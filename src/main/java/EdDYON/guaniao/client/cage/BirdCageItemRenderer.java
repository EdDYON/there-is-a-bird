package EdDYON.guaniao.client.cage;

import EdDYON.guaniao.content.cage.BirdCageItem;
import EdDYON.guaniao.content.cage.BirdCageVariant;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class BirdCageItemRenderer extends GeoItemRenderer<BirdCageItem> {
    public BirdCageItemRenderer() {
        super(new BirdCageItemModel());
    }

    @Override
    public void preRender(PoseStack poseStack, BirdCageItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        BirdCageVariant variant = animatable.variant();
        poseStack.translate(itemOffsetX(variant), itemOffsetY(variant), 0.0F);
        float scale = itemScale(variant);
        poseStack.scale(scale, scale, scale);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static float itemScale(BirdCageVariant variant) {
        return switch (variant) {
            case SMALL -> 0.78F;
            case MEDIUM -> 0.30F;
            case LARGE -> 0.22F;
        };
    }

    private static float itemOffsetX(BirdCageVariant variant) {
        return switch (variant) {
            case SMALL -> 0.08F;
            case MEDIUM -> 0.30F;
            case LARGE -> 0.37F;
        };
    }

    private static float itemOffsetY(BirdCageVariant variant) {
        return switch (variant) {
            case SMALL -> -0.40F;
            case MEDIUM -> -0.10F;
            case LARGE -> -0.10F;
        };
    }
}
