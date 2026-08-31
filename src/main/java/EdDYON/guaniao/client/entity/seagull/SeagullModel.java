package EdDYON.guaniao.client.entity.seagull;

import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.content.bird.seagull.SeagullDefinition;
import EdDYON.guaniao.content.bird.seagull.SeagullEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SeagullModel extends GeoModel<SeagullEntity> {
    @Override
    public ResourceLocation getModelResource(SeagullEntity animatable) {
        return SeagullDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SeagullEntity animatable) {
        return BirdMutationTextureFactory.textureFor(SeagullDefinition.TEXTURE, animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(SeagullEntity animatable) {
        return SeagullDefinition.ANIMATION;
    }
}
