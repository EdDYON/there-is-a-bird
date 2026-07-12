package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.content.camera.NikonD750Item;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class NikonD750ItemRenderer extends GeoItemRenderer<NikonD750Item> {
    private static final float MODEL_SCALE = 1.5F;

    public NikonD750ItemRenderer() {
        super(new NikonD750ItemModel());
    }

    @Override
    public void preRender(PoseStack poseStack, NikonD750Item animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        applyDisplayTransform(poseStack, this.renderPerspective);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static void applyDisplayTransform(PoseStack poseStack, ItemDisplayContext context) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.19F, -0.01F, 0.0F);
            scale(poseStack, 0.37F);
        } else if (context == ItemDisplayContext.GROUND) {
            poseStack.translate(0.16F, 0.02F, 0.0F);
            scale(poseStack, 0.24F);
        } else if (context == ItemDisplayContext.FIXED) {
            poseStack.translate(0.19F, 0.02F, 0.0F);
            scale(poseStack, 0.30F);
        } else if (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseStack.translate(0.1F, 0.16F, 0.18F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-8.0F));
            scale(poseStack, 0.27F);
        } else if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            poseStack.translate(0.1F, -0.07F, -0.08F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-4.0F));
            scale(poseStack, 0.5F);
        } else {
            poseStack.translate(0.19F, 0.08F, 0.0F);
            scale(poseStack, 0.30F);
        }
    }

    private static void scale(PoseStack poseStack, float scale) {
        float finalScale = scale * MODEL_SCALE;
        poseStack.scale(finalScale, finalScale, finalScale);
    }
}
