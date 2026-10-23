package EdDYON.guaniao.client.camera.frame;

import EdDYON.guaniao.GuaniaoMod;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import java.nio.FloatBuffer;
import java.util.LinkedHashMap;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

/** Resource-pack-aware surfaces and bounded snapshots of the real inventory icons. */
@Mod.EventBusSubscriber(modid=GuaniaoMod.MOD_ID, value=Dist.CLIENT)
public final class FrameVisualResources {
    public static final ResourceLocation ATLAS=TextureAtlas.LOCATION_BLOCKS;
    public static final ResourceLocation AIR=new ResourceLocation("minecraft", "air");
    private static final ResourceLocation PAPER=new ResourceLocation("minecraft", "paper");
    private static final int MAX_CACHED=128;
    private static final int ICON_PIXELS=64;
    private static final Logger LOGGER=LogUtils.getLogger();
    private static final Map<ResourceLocation,Material> MATERIALS=new LinkedHashMap<>(16,0.75F,true);
    private static final Map<ResourceLocation,ResourceLocation> STICKERS=new LinkedHashMap<>(16,0.75F,true);
    private static final Set<ResourceLocation> PENDING=new LinkedHashSet<>();
    private static final Set<ResourceLocation> FAILED=new LinkedHashSet<>();
    private static TextureTarget target;
    private static StickerBuffers iconBuffers;
    private static boolean capturing;
    // RenderType itself memoizes by texture ID, so reuse IDs as well as bounding GPU textures.
    private static final List<ResourceLocation> TEXTURE_SLOTS=java.util.stream.IntStream.range(0,MAX_CACHED)
            .mapToObj(slot->new ResourceLocation(GuaniaoMod.MOD_ID,"dynamic/frame_sticker/"+slot)).toList();
    private static final ArrayDeque<ResourceLocation> AVAILABLE_SLOTS=new ArrayDeque<>(TEXTURE_SLOTS);

    private FrameVisualResources() { }

    public record Material(TextureAtlasSprite sprite,float red,float green,float blue,boolean translucent) { }

    /** Glass linings include blocks that borrow a glass sprite, such as beacons. */
    public static boolean isGlass(ResourceLocation id) {
        Item item=ForgeRegistries.ITEMS.getValue(id);
        if(!(item instanceof BlockItem block))return false;
        if(block.getBlock() instanceof AbstractGlassBlock || block.getBlock() instanceof StainedGlassPaneBlock
                || block.getBlock()==Blocks.GLASS_PANE)return true;
        // Class alone misses other blocks whose chosen surface is the same vanilla pane.
        // Inspect the resolved sprite, preserving ordinary tiled wood/stone/metal textures.
        ResourceLocation texture=material(id).sprite().contents().name();
        if(!texture.getNamespace().equals("minecraft"))return false;
        String path=texture.getPath();
        if(path.equals("block/glass") || path.equals("block/tinted_glass"))return true;
        for(DyeColor color:DyeColor.values()) {
            if(path.equals("block/"+color.getName()+"_stained_glass"))return true;
        }
        return false;
    }

    public static Material material(ResourceLocation id) {
        Material existing=MATERIALS.get(id);
        if(existing!=null) return existing;
        Material material;
        try {
            material=resolveMaterial(id);
        } catch(RuntimeException exception) {
            // A third-party model may need block-entity data unavailable to a frame.
            LOGGER.warn("Using oak texture for unsupported photo-frame material {}",id,exception);
            material=resolveMaterial(new ResourceLocation("minecraft","oak_planks"));
        }
        MATERIALS.put(id,material);
        while(MATERIALS.size()>MAX_CACHED) MATERIALS.remove(MATERIALS.keySet().iterator().next());
        return material;
    }

    private static Material resolveMaterial(ResourceLocation id) {
        Minecraft mc=Minecraft.getInstance();
        boolean paper=id.equals(PAPER)||id.equals(AIR);
        Item item=ForgeRegistries.ITEMS.getValue(id);
        BlockState state=paper ? Blocks.WHITE_WOOL.defaultBlockState()
                : item instanceof BlockItem block ? block.getBlock().defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
        BakedModel model=mc.getBlockRenderer().getBlockModel(state);
        TextureAtlasSprite sprite=model.getParticleIcon(ModelData.EMPTY);
        if(sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            sprite=mc.getBlockRenderer().getBlockModel(Blocks.OAK_PLANKS.defaultBlockState()).getParticleIcon(ModelData.EMPTY);
        }
        int tint=0xFFFFFF;
        if(!paper && tintedParticle(model,state,sprite)) {
            // Match vanilla inventory tint rather than changing an already chosen material
            // with the viewer's current biome. Leaves and grass keep their native colours.
            tint=mc.getBlockColors().getColor(state,null,null,0);
            if(tint==-1) tint=0xFFFFFF;
        }
        // Honour the model's render layer: stained glass carries partial texture alpha,
        // which the cutout shader alone cannot blend with the scene behind the frame.
        boolean translucent=model.getRenderTypes(state,RandomSource.create(42),ModelData.EMPTY)
                .contains(RenderType.translucent());
        return new Material(sprite,((tint>>16)&255)/255.0F,((tint>>8)&255)/255.0F*(paper?0.96F:1),
                (tint&255)/255.0F*(paper?0.86F:1),translucent);
    }

    private static boolean tintedParticle(BakedModel model,BlockState state,TextureAtlasSprite sprite) {
        for(int side=-1;side<Direction.values().length;side++) {
            var faces=model.getQuads(state,side<0?null:Direction.values()[side],RandomSource.create(42),ModelData.EMPTY,null);
            for(var face:faces) if(face.isTinted()&&face.getSprite()==sprite) return true;
        }
        return false;
    }

    /** Rendering never changes framebuffer or flushes another renderer's pending vertices. */
    @Nullable
    public static ResourceLocation sticker(ResourceLocation itemId) {
        ResourceLocation texture=STICKERS.get(itemId);
        if(texture!=null) return texture;
        if(!capturing && !FAILED.contains(itemId) && !itemId.equals(AIR)
                && ForgeRegistries.ITEMS.containsKey(itemId) && PENDING.size()<MAX_CACHED*2) PENDING.add(itemId);
        return null;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc=Minecraft.getInstance();
        if(event.phase!=TickEvent.Phase.START || PENDING.isEmpty() || mc.getOverlay()!=null
                || GameRenderer.getRendertypeEntityCutoutNoCullShader()==null) return;
        for(int count=0;count<2&&!PENDING.isEmpty();count++) {
            ResourceLocation itemId=PENDING.iterator().next();
            PENDING.remove(itemId);
            if(STICKERS.containsKey(itemId)) continue;
            ResourceLocation slot=reserveSlot();
            try {
                ResourceLocation texture=capture(itemId,slot);
                if(texture!=null) STICKERS.put(itemId,texture);
                else AVAILABLE_SLOTS.addLast(slot);
            } catch(RuntimeException exception) {
                mc.getTextureManager().release(slot);
                AVAILABLE_SLOTS.addLast(slot);
                if(iconBuffers!=null) iconBuffers.abort();
                FAILED.add(itemId);
                while(FAILED.size()>MAX_CACHED) FAILED.remove(FAILED.iterator().next());
                LOGGER.warn("Could not render photo-frame sticker {}",itemId,exception);
            }
        }
    }

    private static ResourceLocation reserveSlot() {
        if(!AVAILABLE_SLOTS.isEmpty()) return AVAILABLE_SLOTS.removeFirst();
        ResourceLocation oldest=STICKERS.keySet().iterator().next();
        ResourceLocation slot=STICKERS.remove(oldest);
        Minecraft.getInstance().getTextureManager().release(slot);
        return slot;
    }

    @Nullable
    private static ResourceLocation capture(ResourceLocation itemId,ResourceLocation slot) {
        Minecraft mc=Minecraft.getInstance();
        Item item=ForgeRegistries.ITEMS.getValue(itemId);
        if(item==null) return null;
        RenderState state=new RenderState();
        PoseStack modelView=RenderSystem.getModelViewStack();
        modelView.pushPose();
        capturing=true;
        NativeImage image=null;
        try {
            if(target==null) {
                target=new TextureTarget(ICON_PIXELS,ICON_PIXELS,true,Minecraft.ON_OSX);
                target.setClearColor(0,0,0,0);
            }
            if(iconBuffers==null) iconBuffers=new StickerBuffers();
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true,true,true,true);
            RenderSystem.depthMask(true);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,16,16,0,1000,3000),VertexSorting.ORTHOGRAPHIC_Z);
            modelView.setIdentity();
            modelView.translate(0,0,-2000);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.setShaderFogStart(Float.MAX_VALUE);
            RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            Lighting.setupFor3DItems();
            GuiGraphics graphics=new GuiGraphics(mc,iconBuffers);
            graphics.renderItem(new ItemStack(item),0,0);
            graphics.flush();
            // Screenshot.takeScreenshot forces all alpha to 255; preserve transparency here.
            image=new NativeImage(ICON_PIXELS,ICON_PIXELS,false);
            RenderSystem.bindTexture(target.getColorTextureId());
            image.downloadTexture(0,false);
            image.flipY();
            DynamicTexture texture=new DynamicTexture(image);
            image=null; // DynamicTexture now owns and closes the pixels.
            texture.setFilter(false,false);
            mc.getTextureManager().register(slot,texture);
            return slot;
        } finally {
            if(image!=null) image.close();
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            capturing=false;
            state.restore();
        }
    }

    public static void onResourceReload() {
        Minecraft mc=Minecraft.getInstance();
        mc.execute(()-> {
            for(ResourceLocation texture:STICKERS.values()) mc.getTextureManager().release(texture);
            STICKERS.clear();PENDING.clear();FAILED.clear();MATERIALS.clear();
            AVAILABLE_SLOTS.clear();AVAILABLE_SLOTS.addAll(TEXTURE_SLOTS);
            FrameDesignRenderer.onResourceReload();
            // The scratch framebuffer is texture-pack independent; reusing it also avoids
            // rebinding framebuffers in the middle of a resource-reload callback.
        });
    }

    @Mod.EventBusSubscriber(modid=GuaniaoMod.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class ReloadListener {
        private ReloadListener() { }
        @SubscribeEvent
        public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) resources->onResourceReload());
        }
    }

    /** Mirrors vanilla RenderBuffers: glint and base geometry must have distinct consumers. */
    private static final class StickerBuffers extends MultiBufferSource.BufferSource {
        private StickerBuffers() {super(new BufferBuilder(262144),glintBuffers());}
        private static Map<RenderType,BufferBuilder> glintBuffers() {
            Map<RenderType,BufferBuilder> result=new LinkedHashMap<>();
            for(RenderType type:List.of(RenderType.armorGlint(),RenderType.armorEntityGlint(),RenderType.glint(),
                    RenderType.glintDirect(),RenderType.glintTranslucent(),RenderType.entityGlint(),RenderType.entityGlintDirect())) {
                result.put(type,new BufferBuilder(type.bufferSize()));
            }
            return result;
        }
        private void abort() {
            this.builder.discard();
            for(BufferBuilder fixed:this.fixedBuffers.values()) fixed.discard();
            this.startedBuffers.clear();
            this.lastState=Optional.empty();
        }
    }

    /** Capture runs between frames, but restores state even if a custom item renderer fails. */
    private static final class RenderState {
        private final Matrix4f projection=new Matrix4f(RenderSystem.getProjectionMatrix());
        private final VertexSorting sorting=RenderSystem.getVertexSorting();
        private final ShaderInstance shader=RenderSystem.getShader();
        private final float[] shaderColor=RenderSystem.getShaderColor().clone();
        private final float fogStart=RenderSystem.getShaderFogStart(),fogEnd=RenderSystem.getShaderFogEnd();
        private final int[] shaderTextures=new int[12];
        private final int[] viewport=new int[4],scissor=new int[4];
        private final int drawFbo=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        private final int readFbo=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        private final int activeTexture=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final int boundTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        private final boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),scissored=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        private final boolean depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final int depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        private final int srcRgb=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dstRgb=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final Vector3f light0,light1;
        private final float[] clearColor=new float[4];
        private final boolean[] colorMask=new boolean[4];
        private final double clearDepth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        private RenderState() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,scissor);
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clearColor);
            try(MemoryStack memory=MemoryStack.stackPush()) {
                var colors=memory.malloc(4);
                GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK,colors);
                for(int i=0;i<4;i++) colorMask[i]=colors.get(i)!=0;
            }
            for(int i=0;i<shaderTextures.length;i++) shaderTextures[i]=RenderSystem.getShaderTexture(i);
            ShaderInstance lit=GameRenderer.getRendertypeEntityCutoutNoCullShader();
            RenderSystem.setupShaderLights(lit);
            light0=vector(lit.LIGHT0_DIRECTION.getFloatBuffer());
            light1=vector(lit.LIGHT1_DIRECTION.getFloatBuffer());
        }
        private static Vector3f vector(FloatBuffer values) {return new Vector3f(values.get(0),values.get(1),values.get(2));}
        private void restore() {
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,drawFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,readFbo);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            RenderSystem.setProjectionMatrix(projection,sorting);
            RenderSystem.setShader(()->shader);
            RenderSystem.setShaderColor(shaderColor[0],shaderColor[1],shaderColor[2],shaderColor[3]);
            RenderSystem.setShaderLights(light0,light1);
            RenderSystem.setShaderFogStart(fogStart);RenderSystem.setShaderFogEnd(fogEnd);
            for(int i=0;i<shaderTextures.length;i++) RenderSystem.setShaderTexture(i,shaderTextures[i]);
            RenderSystem.activeTexture(activeTexture);RenderSystem.bindTexture(boundTexture);
            if(blend) RenderSystem.enableBlend();else RenderSystem.disableBlend();
            RenderSystem.blendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);
            if(depth) RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            RenderSystem.depthMask(depthMask);RenderSystem.depthFunc(depthFunc);
            if(cull) RenderSystem.enableCull();else RenderSystem.disableCull();
            if(scissored) RenderSystem.enableScissor(scissor[0],scissor[1],scissor[2],scissor[3]);else RenderSystem.disableScissor();
            RenderSystem.clearColor(clearColor[0],clearColor[1],clearColor[2],clearColor[3]);
            RenderSystem.clearDepth(clearDepth);
            RenderSystem.colorMask(colorMask[0],colorMask[1],colorMask[2],colorMask[3]);
        }
    }
}
