package EdDYON.guaniao.client.cage;

import EdDYON.guaniao.content.cage.BirdCageItem;
import EdDYON.guaniao.content.cage.BirdCageVariant;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BirdCageItemModel extends GeoModel<BirdCageItem> {
    @Override
    public ResourceLocation getModelResource(BirdCageItem animatable) {
        return animatable.variant().model();
    }

    @Override
    public ResourceLocation getTextureResource(BirdCageItem animatable) {
        return animatable.variant().texture();
    }

    @Override
    public ResourceLocation getAnimationResource(BirdCageItem animatable) {
        return BirdCageVariant.ANIMATION;
    }
}
