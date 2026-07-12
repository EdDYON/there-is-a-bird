package EdDYON.guaniao.client.cage;

import EdDYON.guaniao.content.cage.BirdCageBlockEntity;
import EdDYON.guaniao.content.cage.BirdCageVariant;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BirdCageModel extends GeoModel<BirdCageBlockEntity> {
    @Override
    public ResourceLocation getModelResource(BirdCageBlockEntity animatable) {
        return animatable.variant().model();
    }

    @Override
    public ResourceLocation getTextureResource(BirdCageBlockEntity animatable) {
        return animatable.variant().texture();
    }

    @Override
    public ResourceLocation getAnimationResource(BirdCageBlockEntity animatable) {
        return BirdCageVariant.ANIMATION;
    }
}
