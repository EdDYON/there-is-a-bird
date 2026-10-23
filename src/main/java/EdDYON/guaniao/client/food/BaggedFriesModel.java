package EdDYON.guaniao.client.food;

import EdDYON.guaniao.content.food.BaggedFriesBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class BaggedFriesModel extends GeoModel<BaggedFriesBlockEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("guaniao", "geo/bagged_fries.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("guaniao", "textures/block/bagged_fries.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("guaniao", "animations/bagged_fries.animation.json");

    @Override
    public ResourceLocation getModelResource(BaggedFriesBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BaggedFriesBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BaggedFriesBlockEntity animatable) {
        return ANIMATION;
    }
}
