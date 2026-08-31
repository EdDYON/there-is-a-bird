package EdDYON.guaniao.client.entity.nightheron;

import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.content.bird.nightheron.NightHeronDefinition;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NightHeronModel
extends GeoModel<NightHeronEntity> {
    public ResourceLocation getModelResource(NightHeronEntity animatable) {
        return NightHeronDefinition.MODEL;
    }

    public ResourceLocation getTextureResource(NightHeronEntity animatable) {
        return BirdMutationTextureFactory.textureFor(NightHeronDefinition.TEXTURE, animatable);
    }

    public ResourceLocation getAnimationResource(NightHeronEntity animatable) {
        return NightHeronDefinition.ANIMATION;
    }
}

