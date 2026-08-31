package EdDYON.guaniao.client.entity.kiwi;

import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.content.bird.kiwi.KiwiDefinition;
import EdDYON.guaniao.content.bird.kiwi.KiwiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class KiwiModel extends GeoModel<KiwiEntity> {
    @Override
    public ResourceLocation getModelResource(KiwiEntity animatable) {
        return KiwiDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(KiwiEntity animatable) {
        return BirdMutationTextureFactory.textureFor(KiwiDefinition.TEXTURE, animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(KiwiEntity animatable) {
        return KiwiDefinition.ANIMATION;
    }
}
