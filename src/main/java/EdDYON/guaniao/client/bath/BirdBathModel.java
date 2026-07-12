package EdDYON.guaniao.client.bath;

import EdDYON.guaniao.content.bath.BirdBathBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BirdBathModel extends GeoModel<BirdBathBlockEntity> {
    @Override
    public ResourceLocation getModelResource(BirdBathBlockEntity animatable) {
        return animatable.variant().model();
    }

    @Override
    public ResourceLocation getTextureResource(BirdBathBlockEntity animatable) {
        return animatable.variant().texture();
    }

    @Override
    public ResourceLocation getAnimationResource(BirdBathBlockEntity animatable) {
        return animatable.variant().animation();
    }
}
