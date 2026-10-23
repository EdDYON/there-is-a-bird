package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.PhotographFrameStyle;
import EdDYON.guaniao.content.camera.frame.FrameDesign;
import EdDYON.guaniao.client.camera.frame.FrameDesignRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class PhotographItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation CARD_TEXTURE = ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, "textures/item/photograph_card.png");
    private static final ResourceLocation ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, "textures/item/photograph_icon.png");
    private static final float CARD_WIDTH = 0.82F;
    private static final float CARD_HEIGHT = 0.62F;

    public PhotographItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poses.pushPose();
        try {
            // ItemRenderer has already moved the model origin by (-.5,-.5,-.5).
            poses.translate(0.5F, 0.5F, 0.5F);
            int light = context == ItemDisplayContext.GUI ? LightTexture.FULL_BRIGHT : packedLight;
            FrameDesign design = FrameDesignRenderer.designFor(stack);
            PhotographFrameStyle style = design == null ? PhotographFrameStyle.read(stack) : PhotographFrameStyle.DEFAULT;
            boolean plainLegacy = design == null && style.shape() == PhotographFrameStyle.Shape.LEGACY
                    && style.material() == PhotographFrameStyle.Material.OAK
                    && style.ornament() == PhotographFrameStyle.Ornament.NONE;
            if (!plainLegacy || context != ItemDisplayContext.GUI) {
                applyDisplayTransform(poses, context);
            }
            if (design != null) {
                if (design.widthPixels() <= 0 || design.heightPixels() <= 0) return;
                float maxSide = Math.max(design.widthPixels(), design.heightPixels()) / 16.0F;
                float scale = 0.88F / maxSide;
                poses.scale(scale, scale, scale);
                FrameDesignRenderer.render(stack, design, 0, poses, buffers, light);
            } else if (plainLegacy) {
                boolean inventory = context == ItemDisplayContext.GUI;
                VertexConsumer out = buffers.getBuffer(RenderType.text(inventory ? ICON_TEXTURE : CARD_TEXTURE));
                renderCard(out, poses.last().pose(), inventory ? 1.0F : CARD_WIDTH,
                        inventory ? 1.0F : CARD_HEIGHT, light);
            } else {
                float maxSide = Math.max(style.shape().widthPixels(), style.shape().heightPixels()) / 16.0F;
                float scale = 0.88F / maxSide;
                poses.scale(scale, scale, scale);
                PhotographFrameRenderer.render(stack, style, 0, poses, buffers, light);
            }
        } finally {
            poses.popPose();
        }
    }

    private static void applyDisplayTransform(PoseStack poses, ItemDisplayContext context) {
        if (context == ItemDisplayContext.GUI) {
            poses.mulPose(Axis.ZP.rotationDegrees(-6.0F));
            poses.scale(0.96F, 0.96F, 0.96F);
        } else if (context == ItemDisplayContext.GROUND) {
            poses.translate(0.0F, 0.05F, 0.0F);
            poses.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poses.scale(0.62F, 0.62F, 0.62F);
        } else if (context == ItemDisplayContext.FIXED) {
            poses.scale(0.78F, 0.78F, 0.78F);
        } else {
            poses.translate(0.0F, 0.02F, 0.0F);
            poses.mulPose(Axis.ZP.rotationDegrees(-6.0F));
            poses.scale(0.72F, 0.72F, 0.72F);
        }
    }

    private static void renderCard(VertexConsumer out, Matrix4f matrix, float width, float height, int light) {
        float x0=-width/2, x1=width/2, y0=-height/2, y1=height/2;
        out.addVertex(matrix,x0,y0,0).setColor(255,255,255,255).setUv(0,1).setLight(light);
        out.addVertex(matrix,x1,y0,0).setColor(255,255,255,255).setUv(1,1).setLight(light);
        out.addVertex(matrix,x1,y1,0).setColor(255,255,255,255).setUv(1,0).setLight(light);
        out.addVertex(matrix,x0,y1,0).setColor(255,255,255,255).setUv(0,0).setLight(light);
    }
}
