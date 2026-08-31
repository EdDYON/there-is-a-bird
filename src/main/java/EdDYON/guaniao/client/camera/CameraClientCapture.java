package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.content.camera.CameraFilter;
import EdDYON.guaniao.content.camera.CameraSettingsData;
import EdDYON.guaniao.content.camera.PhotographData;
import EdDYON.guaniao.content.camera.PhotoImageCodec;
import EdDYON.guaniao.network.GuaniaoNetwork;
import EdDYON.guaniao.network.SetCameraFilterPacket;
import EdDYON.guaniao.registry.GuaniaoItems;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ViewportEvent;

public final class CameraClientCapture {
    private static final int CLEAN_CAPTURE_DELAY_FRAMES = 0;
    private static final double MIN_FOCAL_LENGTH = 18.0D;
    private static final double MAX_FOCAL_LENGTH = 200.0D;
    private static final double DEFAULT_FOCAL_LENGTH = 50.0D;
    private static final double FOCAL_LENGTH_SCROLL_STEP = 4.0D;
    private static final double FULL_FRAME_SENSOR_WIDTH = 36.0D;
    private static final double MIN_VIEWFINDER_SENSITIVITY_SCALE = 0.28D;

    private static boolean viewfinderOpen;
    private static InteractionHand viewfinderHand = InteractionHand.MAIN_HAND;
    private static double focalLength = DEFAULT_FOCAL_LENGTH;
    private static CameraFilter currentFilter = CameraFilter.NONE;

    private static boolean cleanCapturePending;
    private static int cleanCaptureDelayFrames;
    private static InteractionHand pendingCaptureHand = InteractionHand.MAIN_HAND;
    private static double pendingCaptureFov = focalLengthToFov(DEFAULT_FOCAL_LENGTH);
    private static CameraFilter pendingCaptureFilter = CameraFilter.NONE;
    private static float pendingCameraYRot;
    private static float pendingCameraXRot;
    private static boolean storedHideGui;
    private static CameraType storedCameraType = CameraType.FIRST_PERSON;
    private static boolean sensitivityAdjusted;
    private static double storedSensitivity;
    private static boolean offscreenCaptureActive;
    private static RenderTarget offscreenCaptureTarget;

    private CameraClientCapture() {
    }

    public static void openViewfinder(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || cleanCapturePending) {
            return;
        }

        if (viewfinderOpen) {
            closeViewfinder();
            return;
        }

        viewfinderHand = hand;
        currentFilter = CameraSettingsData.filter(minecraft.player.getItemInHand(hand));
        focalLength = Mth.clamp(focalLength, MIN_FOCAL_LENGTH, MAX_FOCAL_LENGTH);
        beginSensitivityAdjustment(minecraft);
        applyFocalSensitivity(minecraft);
        viewfinderOpen = true;
        minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 1.5F);
    }

    public static boolean isViewfinderOpen() {
        return viewfinderOpen;
    }

    public static boolean shouldHideHands() {
        return viewfinderOpen || cleanCapturePending;
    }

    /** Used by the client Minecraft mixin while the world is being rendered into the camera canvas. */
    public static RenderTarget redirectedRenderTarget() {
        return offscreenCaptureActive ? offscreenCaptureTarget : null;
    }

    /**
     * Some world render passes temporarily bind their own framebuffer. Rebind the camera canvas
     * at Forge's stage boundaries so the following entity/translucent passes return to it.
     */
    public static void rebindOffscreenCaptureTarget() {
        if (offscreenCaptureActive && offscreenCaptureTarget != null) {
            offscreenCaptureTarget.bindWrite(true);
        }
    }

    public static void closeViewfinder() {
        viewfinderOpen = false;
        restoreSensitivity(Minecraft.getInstance());
    }

    public static void tickViewfinder() {
        if (!viewfinderOpen) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !isCameraStillHeld(minecraft, viewfinderHand)) {
            closeViewfinder();
        }
    }

    public static boolean handleMouseScroll(double delta) {
        if (!viewfinderOpen || cleanCapturePending) {
            return false;
        }

        double scrollAmount = Math.max(1.0D, Math.abs(delta));
        double next = Mth.clamp(focalLength + (delta > 0.0D ? FOCAL_LENGTH_SCROLL_STEP : -FOCAL_LENGTH_SCROLL_STEP) * scrollAmount,
                MIN_FOCAL_LENGTH,
                MAX_FOCAL_LENGTH);
        if (Math.abs(next - focalLength) > 0.001D) {
            focalLength = next;
            Minecraft minecraft = Minecraft.getInstance();
            applyFocalSensitivity(minecraft);
            if (minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.18F, delta > 0.0D ? 1.65F : 1.1F);
            }
        }
        return true;
    }

    public static boolean handleMouseButton(int button, int action) {
        if (!viewfinderOpen || cleanCapturePending || action != 1) {
            return false;
        }

        if (button == 0) {
            beginCleanCapture(viewfinderHand, currentFov());
            return true;
        }
        if (button == 1) {
            closeViewfinder();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.25F, 0.85F);
            }
            return true;
        }

        return false;
    }

    public static boolean cycleFilter() {
        if (!viewfinderOpen || cleanCapturePending) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !isCameraStillHeld(minecraft, viewfinderHand)) {
            closeViewfinder();
            return true;
        }

        currentFilter = currentFilter.next();
        CameraSettingsData.setFilter(minecraft.player.getItemInHand(viewfinderHand), currentFilter);
        GuaniaoNetwork.sendToServer(new SetCameraFilterPacket(viewfinderHand, currentFilter));
        minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.28F, 1.2F + currentFilter.ordinal() * 0.08F);
        return true;
    }

    public static void renderViewfinder(GuiGraphics graphics, float partialTick) {
        if (!viewfinderOpen || cleanCapturePending) {
            return;
        }
        CameraViewfinderOverlay.render(graphics, focalLength, currentFov(), currentFilter);
    }

    public static void modifyFov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) {
            return;
        }

        if (viewfinderOpen) {
            event.setFOV(currentFov());
        } else if (cleanCapturePending) {
            event.setFOV(pendingCaptureFov);
        }
    }

    public static void onRenderTickEnd() {
        if (!cleanCapturePending) {
            return;
        }

        if (cleanCaptureDelayFrames-- > 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            restoreAfterCleanCapture();
            return;
        }

        // Re-render from the exact shutter orientation, as Exposure's background capture does.
        minecraft.player.setYRot(pendingCameraYRot);
        minecraft.player.yRotO = pendingCameraYRot;
        minecraft.player.setXRot(pendingCameraXRot);
        minecraft.player.xRotO = pendingCameraXRot;

        try {
            captureAndSend(pendingCaptureHand);
        } finally {
            restoreAfterCleanCapture();
        }
    }

    public static void captureAndSend(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        try {
            beginOffscreenCaptureFrame(minecraft);
            minecraft.gameRenderer.renderLevel(minecraft.getFrameTime(), 0L, new PoseStack());

            try (NativeImage image = Screenshot.takeScreenshot(offscreenCaptureTarget)) {
                int[] pixels = cropSquare(image, PhotographData.IMAGE_SIZE);
                CameraImageFilters.apply(pixels, PhotographData.IMAGE_SIZE, PhotographData.IMAGE_SIZE, pendingCaptureFilter, System.nanoTime());
                byte[] jpeg = PhotoImageCodec.encodeJpeg(pixels, PhotographData.IMAGE_SIZE, PhotographData.IMAGE_SIZE);
                PhotoClientRepository.upload(hand, jpeg);
            }
        } catch (Exception exception) {
            minecraft.player.displayClientMessage(Component.translatable("item.guaniao.nikon_d750.capture_failed"), true);
        } finally {
            releaseOffscreenCaptureFrame(minecraft);
        }
    }

    private static void beginOffscreenCaptureFrame(Minecraft minecraft) {
        int windowWidth = Math.max(1, minecraft.getWindow().getWidth());
        int windowHeight = Math.max(1, minecraft.getWindow().getHeight());
        RenderTarget target = new TextureTarget(windowWidth, windowHeight, true, Minecraft.ON_OSX);
        offscreenCaptureTarget = target;
        offscreenCaptureActive = true;

        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.clear(Minecraft.ON_OSX);
        minecraft.gameRenderer.setRenderBlockOutline(false);
        minecraft.levelRenderer.graphicsChanged();
        target.bindWrite(false);
    }

    private static void releaseOffscreenCaptureFrame(Minecraft minecraft) {
        RenderTarget target = offscreenCaptureTarget;
        offscreenCaptureActive = false;
        offscreenCaptureTarget = null;

        try {
            if (target != null) {
                target.unbindWrite();
                target.destroyBuffers();
            }
        } finally {
            minecraft.gameRenderer.setRenderBlockOutline(true);
            try {
                minecraft.levelRenderer.graphicsChanged();
            } finally {
                minecraft.getMainRenderTarget().bindWrite(true);
            }
        }
    }

    private static int[] cropSquare(NativeImage image, int size) {
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        int sourceSize = CameraViewfinderOverlay.apertureSize(sourceWidth, sourceHeight);
        int offsetX = (sourceWidth - sourceSize) / 2;
        int offsetY = (sourceHeight - sourceSize) / 2;
        int[] pixels = new int[size * size];

        for (int y = 0; y < size; y++) {
            int sampleY = offsetY + Math.min(sourceSize - 1, (int)((y + 0.5D) * sourceSize / size));
            for (int x = 0; x < size; x++) {
                int sampleX = offsetX + Math.min(sourceSize - 1, (int)((x + 0.5D) * sourceSize / size));
                pixels[y * size + x] = image.getPixelRGBA(sampleX, sampleY);
            }
        }

        return pixels;
    }

    private static void beginCleanCapture(InteractionHand hand, double fov) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !isCameraStillHeld(minecraft, hand)) {
            closeViewfinder();
            return;
        }

        viewfinderOpen = false;
        cleanCapturePending = true;
        cleanCaptureDelayFrames = CLEAN_CAPTURE_DELAY_FRAMES;
        pendingCaptureHand = hand;
        pendingCaptureFov = fov;
        pendingCaptureFilter = currentFilter;
        pendingCameraYRot = minecraft.player.getViewYRot(1.0F);
        pendingCameraXRot = minecraft.player.getViewXRot(1.0F);
        storedHideGui = minecraft.options.hideGui;
        storedCameraType = minecraft.options.getCameraType();
        minecraft.options.hideGui = true;
        if (storedCameraType != CameraType.THIRD_PERSON_FRONT) {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        }
        minecraft.player.playSound(SoundEvents.SPYGLASS_USE, 0.45F, 1.4F);
    }

    private static void restoreAfterCleanCapture() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.hideGui = storedHideGui;
        minecraft.options.setCameraType(storedCameraType);
        restoreSensitivity(minecraft);
        cleanCapturePending = false;
    }

    private static boolean isCameraStillHeld(Minecraft minecraft, InteractionHand hand) {
        if (minecraft.player == null) {
            return false;
        }
        ItemStack stack = minecraft.player.getItemInHand(hand);
        return stack.is(GuaniaoItems.NIKON_D750.get());
    }

    private static double currentFov() {
        return focalLengthToFov(focalLength);
    }

    private static double focalLengthToFov(double focalLength) {
        double fov = Math.toDegrees(2.0D * Math.atan(FULL_FRAME_SENSOR_WIDTH / (2.0D * focalLength)));
        return Mth.clamp(fov, 14.0D, 92.0D);
    }

    private static void beginSensitivityAdjustment(Minecraft minecraft) {
        if (!sensitivityAdjusted) {
            storedSensitivity = minecraft.options.sensitivity().get();
            sensitivityAdjusted = true;
        }
    }

    private static void applyFocalSensitivity(Minecraft minecraft) {
        if (!sensitivityAdjusted) {
            return;
        }
        minecraft.options.sensitivity().set(storedSensitivity * focalSensitivityScale());
    }

    private static void restoreSensitivity(Minecraft minecraft) {
        if (!sensitivityAdjusted) {
            return;
        }
        minecraft.options.sensitivity().set(storedSensitivity);
        sensitivityAdjusted = false;
    }

    private static double focalSensitivityScale() {
        double normalized = Mth.clamp((focalLength - MIN_FOCAL_LENGTH) / (MAX_FOCAL_LENGTH - MIN_FOCAL_LENGTH), 0.0D, 1.0D);
        double smooth = normalized * normalized * (3.0D - 2.0D * normalized);
        return Mth.lerp(smooth, 1.0D, MIN_VIEWFINDER_SENSITIVITY_SCALE);
    }
}
