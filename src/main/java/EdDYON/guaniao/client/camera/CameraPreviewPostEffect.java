package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.CameraFilter;
import EdDYON.guaniao.content.camera.CameraFilterCategory;
import EdDYON.guaniao.content.camera.CameraState;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/** Keeps a filtered world copy for the viewfinder without modifying Minecraft's main framebuffer. */
public final class CameraPreviewPostEffect {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int THUMBNAIL_WIDTH = 160;
    private static final int THUMBNAIL_HEIGHT = 90;

    private static TextureTarget previewTarget;
    private static TextureTarget opticsTarget;
    private static PostChain chain;
    private static CameraFilter loadedFilter = CameraFilter.NONE;
    private static int targetWidth = -1;
    private static int targetHeight = -1;
    private static boolean preparedThisFrame;
    private static boolean cleanCapturePrepared;
    private static boolean opticsPassFailed;
    private static final Map<CameraFilter, ThumbnailSlot> THUMBNAILS = new EnumMap<>(CameraFilter.class);
    private static CameraFilterCategory thumbnailCategory;

    private CameraPreviewPostEffect() {
    }

    public static void prepare(float partialTick) {
        preparedThisFrame = false;
        cleanCapturePrepared = false;

        Minecraft minecraft = Minecraft.getInstance();
        CameraFilterPickerScreen picker = minecraft.screen instanceof CameraFilterPickerScreen screen ? screen : null;
        if (picker == null) {
            closeThumbnails();
        }
        boolean cleanCapture = CameraClientCapture.isCleanCapturePending();
        if (minecraft.level == null || (!CameraClientCapture.isViewfinderOpen() && !cleanCapture)) {
            return;
        }

        CameraState state = CameraClientCapture.renderState();
        CameraFilter filter = state.filter();

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        int width = mainTarget.width;
        int height = mainTarget.height;
        if (width <= 0 || height <= 0) {
            return;
        }

        ensureTarget(width, height);
        if (previewTarget == null || opticsTarget == null) {
            return;
        }

        try {
            boolean opticsRendered = false;
            if (!opticsPassFailed) {
                try {
                    opticsRendered = CameraOpticsShader.process(mainTarget, opticsTarget, state);
                } catch (RuntimeException exception) {
                    opticsPassFailed = true;
                    LOGGER.error("Failed to process camera depth-of-field pass; using the unprocessed scene", exception);
                }
            }
            if (!opticsRendered) {
                blitColor(mainTarget, opticsTarget);
            }

            if (cleanCapture) {
                cleanCapturePrepared = true;
                return;
            }

            blitColor(opticsTarget, previewTarget);
            preparedThisFrame = true;
            if (filter == CameraFilter.NONE) {
                closeChain();
            } else {
                if (loadedFilter != filter) {
                    loadChain(filter);
                }
                if (chain != null) {
                    try {
                        chain.process(partialTick);
                    } catch (RuntimeException exception) {
                        LOGGER.error("Failed to process camera preview filter {}", filter, exception);
                        closeChain();
                        loadedFilter = filter;
                    }
                }
            }

            if (picker != null) {
                prepareThumbnails(minecraft, opticsTarget, picker.previewCategory(), partialTick);
            }
        } finally {
            // Small post chains change the viewport; restore both the main framebuffer and its full viewport.
            mainTarget.bindWrite(true);
        }
    }

    public static void drawFilteredLens(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (!preparedThisFrame || previewTarget == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        if (guiWidth <= 0 || guiHeight <= 0) {
            return;
        }

        float u0 = (float)left / guiWidth;
        float u1 = (float)right / guiWidth;
        float vTop = 1.0F - (float)top / guiHeight;
        float vBottom = 1.0F - (float)bottom / guiHeight;

        drawPreviewTarget(graphics, previewTarget, left, top, right, bottom, u0, vBottom, u1, vTop);
    }

    /** Draws a center-cropped copy of the current filtered world into an arbitrary UI rectangle. */
    public static void drawPreview(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (!preparedThisFrame || previewTarget == null || right <= left || bottom <= top) {
            return;
        }

        float sourceAspect = previewTarget.width / (float)Math.max(1, previewTarget.height);
        float destinationAspect = (right - left) / (float)Math.max(1, bottom - top);
        float u0 = 0.0F;
        float u1 = 1.0F;
        float vBottom = 0.0F;
        float vTop = 1.0F;

        if (sourceAspect > destinationAspect) {
            float visibleWidth = destinationAspect / sourceAspect;
            float crop = (1.0F - visibleWidth) * 0.5F;
            u0 = crop;
            u1 = 1.0F - crop;
        } else if (sourceAspect < destinationAspect) {
            float visibleHeight = sourceAspect / destinationAspect;
            float crop = (1.0F - visibleHeight) * 0.5F;
            vBottom = crop;
            vTop = 1.0F - crop;
        }

        drawPreviewTarget(graphics, previewTarget, left, top, right, bottom, u0, vBottom, u1, vTop);
    }

    /** Draws the real filtered world thumbnail prepared for one item in the active picker category. */
    public static boolean drawFilterThumbnail(
            GuiGraphics graphics,
            CameraFilter filter,
            int left,
            int top,
            int right,
            int bottom
    ) {
        ThumbnailSlot slot = THUMBNAILS.get(filter);
        if (slot == null || !slot.ready || right <= left || bottom <= top) {
            return false;
        }

        drawPreviewTarget(graphics, slot.target, left, top, right, bottom, 0.0F, 0.0F, 1.0F, 1.0F);
        return true;
    }

    private static void drawPreviewTarget(
            GuiGraphics graphics,
            RenderTarget target,
            int left,
            int top,
            int right,
            int bottom,
            float u0,
            float vBottom,
            float u1,
            float vTop
    ) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(matrix, left, bottom, 0.0F).uv(u0, vBottom).endVertex();
        builder.vertex(matrix, right, bottom, 0.0F).uv(u1, vBottom).endVertex();
        builder.vertex(matrix, right, top, 0.0F).uv(u1, vTop).endVertex();
        builder.vertex(matrix, left, top, 0.0F).uv(u0, vTop).endVertex();
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void ensureTarget(int width, int height) {
        if (previewTarget != null && opticsTarget != null && width == targetWidth && height == targetHeight) {
            return;
        }

        closeChain();
        if (previewTarget != null) {
            previewTarget.destroyBuffers();
        }
        if (opticsTarget != null) {
            opticsTarget.destroyBuffers();
        }

        previewTarget = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        previewTarget.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
        opticsTarget = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        opticsTarget.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
        targetWidth = width;
        targetHeight = height;
    }

    private static void blitColor(RenderTarget source, RenderTarget destination) {
        RenderSystem.assertOnRenderThread();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.frameBufferId);
        GL30.glBlitFramebuffer(
                0, 0, source.width, source.height,
                0, 0, destination.width, destination.height,
                GL11.GL_COLOR_BUFFER_BIT,
                GL11.GL_NEAREST
        );
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, destination.frameBufferId);
    }

    private static void blitCenterCropped(RenderTarget source, RenderTarget destination) {
        RenderSystem.assertOnRenderThread();
        float sourceAspect = source.width / (float)Math.max(1, source.height);
        float destinationAspect = destination.width / (float)Math.max(1, destination.height);
        int sourceLeft = 0;
        int sourceBottom = 0;
        int sourceRight = source.width;
        int sourceTop = source.height;

        if (sourceAspect > destinationAspect) {
            int croppedWidth = Math.max(1, Math.round(source.height * destinationAspect));
            sourceLeft = (source.width - croppedWidth) / 2;
            sourceRight = sourceLeft + croppedWidth;
        } else if (sourceAspect < destinationAspect) {
            int croppedHeight = Math.max(1, Math.round(source.width / destinationAspect));
            sourceBottom = (source.height - croppedHeight) / 2;
            sourceTop = sourceBottom + croppedHeight;
        }

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.frameBufferId);
        GL30.glBlitFramebuffer(
                sourceLeft, sourceBottom, sourceRight, sourceTop,
                0, 0, destination.width, destination.height,
                GL11.GL_COLOR_BUFFER_BIT,
                GL11.GL_LINEAR
        );
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, destination.frameBufferId);
    }

    private static void prepareThumbnails(
            Minecraft minecraft,
            RenderTarget mainTarget,
            CameraFilterCategory category,
            float partialTick
    ) {
        ensureThumbnailCategory(minecraft, category);
        for (ThumbnailSlot slot : THUMBNAILS.values()) {
            slot.ready = false;
            if (slot.failed || slot.chain == null) {
                continue;
            }

            blitCenterCropped(mainTarget, slot.target);
            try {
                slot.chain.process(partialTick);
                slot.ready = true;
            } catch (RuntimeException exception) {
                slot.failed = true;
                LOGGER.error("Failed to process camera filter thumbnail {}", slot.filter, exception);
                slot.closeChain();
            }
        }
    }

    private static void ensureThumbnailCategory(Minecraft minecraft, CameraFilterCategory category) {
        if (thumbnailCategory == category && THUMBNAILS.size() == CameraFilter.inCategory(category).size()) {
            return;
        }

        closeThumbnails();
        thumbnailCategory = category;
        for (CameraFilter filter : CameraFilter.inCategory(category)) {
            TextureTarget target = new TextureTarget(
                    THUMBNAIL_WIDTH,
                    THUMBNAIL_HEIGHT,
                    false,
                    Minecraft.ON_OSX
            );
            target.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            ThumbnailSlot slot = new ThumbnailSlot(filter, target);
            THUMBNAILS.put(filter, slot);

            ResourceLocation postEffect = postEffectFor(filter);
            try {
                slot.chain = new PostChain(
                        minecraft.getTextureManager(),
                        minecraft.getResourceManager(),
                        target,
                        postEffect
                );
                slot.chain.resize(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
            } catch (Exception exception) {
                slot.failed = true;
                LOGGER.error("Failed to load camera filter thumbnail effect {}", postEffect, exception);
                slot.closeChain();
            }
        }
    }

    private static void closeThumbnails() {
        for (ThumbnailSlot slot : THUMBNAILS.values()) {
            slot.close();
        }
        THUMBNAILS.clear();
        thumbnailCategory = null;
    }

    private static void loadChain(CameraFilter filter) {
        closeChain();
        ResourceLocation postEffect = postEffectFor(filter);
        if (postEffect == null || previewTarget == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        try {
            chain = new PostChain(
                    minecraft.getTextureManager(),
                    minecraft.getResourceManager(),
                    previewTarget,
                    postEffect
            );
            chain.resize(targetWidth, targetHeight);
            loadedFilter = filter;
        } catch (Exception exception) {
            LOGGER.error("Failed to load camera preview post effect {}", postEffect, exception);
            closeChain();
            loadedFilter = filter;
        }
    }

    private static ResourceLocation postEffectFor(CameraFilter filter) {
        if (filter == null || filter == CameraFilter.NONE) {
            return null;
        }
        String path = String.format(Locale.ROOT, "shaders/post/camera_filter_%02d.json", filter.id());
        return new ResourceLocation(GuaniaoMod.MOD_ID, path);
    }

    public static void close() {
        preparedThisFrame = false;
        cleanCapturePrepared = false;
        opticsPassFailed = false;
        closeChain();
        closeThumbnails();
        if (previewTarget != null) {
            previewTarget.destroyBuffers();
            previewTarget = null;
        }
        if (opticsTarget != null) {
            opticsTarget.destroyBuffers();
            opticsTarget = null;
        }
        targetWidth = -1;
        targetHeight = -1;
    }

    public static RenderTarget cleanCaptureTarget(RenderTarget fallback) {
        return cleanCapturePrepared && opticsTarget != null ? opticsTarget : fallback;
    }

    private static void closeChain() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
        loadedFilter = CameraFilter.NONE;
    }

    private static final class ThumbnailSlot {
        private final CameraFilter filter;
        private final TextureTarget target;
        private PostChain chain;
        private boolean ready;
        private boolean failed;

        private ThumbnailSlot(CameraFilter filter, TextureTarget target) {
            this.filter = filter;
            this.target = target;
        }

        private void closeChain() {
            if (this.chain != null) {
                this.chain.close();
                this.chain = null;
            }
        }

        private void close() {
            closeChain();
            this.target.destroyBuffers();
        }
    }
}
