package EdDYON.guaniao.client.entity.cockatiel;

import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.content.bird.cockatiel.CockatielDefinition;
import EdDYON.guaniao.content.bird.cockatiel.CockatielEntity;
import EdDYON.guaniao.content.bird.cockatiel.CockatielCrestState;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class CockatielModel extends GeoModel<CockatielEntity> {
    @Override
    public ResourceLocation getModelResource(CockatielEntity animatable) {
        return CockatielDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CockatielEntity animatable) {
        return BirdMutationTextureFactory.textureFor(animatable.getTextureResource(), animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(CockatielEntity animatable) {
        return CockatielDefinition.ANIMATION;
    }

    @Override
    public void setCustomAnimations(CockatielEntity animatable, long instanceId, AnimationState<CockatielEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        CockatielCrestState state = animatable.getCrestState();
        float offset = switch (state) {
            case RELAXED -> 0.0F;
            case HAPPY -> -0.12F;
            case CURIOUS -> -0.22F;
            case ALERT -> -0.38F;
            case AFRAID -> -0.56F;
        };
        this.getBone("crest").ifPresent(crest -> crest.setRotX(crest.getRotX() + offset));
    }
}
