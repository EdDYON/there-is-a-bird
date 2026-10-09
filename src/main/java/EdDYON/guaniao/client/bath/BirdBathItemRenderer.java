package EdDYON.guaniao.client.bath;

import EdDYON.guaniao.content.bath.BirdBathContentType;
import EdDYON.guaniao.content.bath.BirdBathCleanliness;
import EdDYON.guaniao.content.bath.BirdBathItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class BirdBathItemRenderer extends GeoItemRenderer<BirdBathItem> {
    public BirdBathItemRenderer() {
        super(new BirdBathItemModel());
    }

    @Override
    public void preRender(PoseStack poseStack, BirdBathItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int renderColor) {
        if (this.renderPerspective == ItemDisplayContext.GUI) {
            // Fill the slot with the basin's top, without changing the held model.
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            // Leave room for the second design's small birds along the rim.
            poseStack.scale(0.8F, 0.8F, 0.8F);
            poseStack.translate(0.0F, -1.4F, 0.0F);
            if (!isReRender) {
                // GeoItemRenderer adds this model-origin offset in preRender.
                poseStack.translate(-0.5F, -0.51F, -0.5F);
            }
        } else {
            poseStack.translate(itemOffsetX(), itemOffsetY(), itemOffsetZ());
            float scale = itemScale();
            poseStack.scale(scale, scale, scale);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, renderColor);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, BirdBathItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int renderColor) {
        BirdBathBoneVisibility.apply(BirdBathContentType.EMPTY, 0, BirdBathCleanliness.CLEAN, BirdBathContentType.EMPTY, bone);
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, renderColor);
    }

    private static float itemOffsetX() {
        return 0.28F;
    }

    private static float itemOffsetY() {
        return -0.12F;
    }

    private static float itemOffsetZ() {
        return 0.0F;
    }

    private static float itemScale() {
        return 0.5F;
    }
}
