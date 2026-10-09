package EdDYON.guaniao.client.entity.woodcock;

import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.content.bird.woodcock.WoodcockDefinition;
import EdDYON.guaniao.content.bird.woodcock.WoodcockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;

public class WoodcockModel extends GeoModel<WoodcockEntity> {
    @Override
    public ResourceLocation getModelResource(WoodcockEntity animatable) {
        return WoodcockDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(WoodcockEntity animatable) {
        return BirdMutationTextureFactory.textureFor(WoodcockDefinition.TEXTURE, animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(WoodcockEntity animatable) {
        return WoodcockDefinition.ANIMATION;
    }

    @Override
    public void setCustomAnimations(WoodcockEntity animatable, long instanceId,
                                    AnimationState<WoodcockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // The source model stores its permanent 0.45 scale in every animation
        // instead of in the geo bone. During controller transitions GeckoLib can
        // briefly restore the bone's authored default of 1.0, making one bird
        // appear more than twice its normal size. Keep this model transform
        // constant after animation blending has finished for the current frame.
        this.getBone("root_shape").ifPresent(bone -> {
            bone.setScaleX(WoodcockDefinition.MODEL_ROOT_SCALE);
            bone.setScaleY(WoodcockDefinition.MODEL_ROOT_SCALE);
            bone.setScaleZ(WoodcockDefinition.MODEL_ROOT_SCALE);
        });

        boolean flying = animatable.isFlying();
        this.getBone("wing_left").ifPresent(bone -> {
            bone.setHidden(flying);
            bone.setChildrenHidden(flying);
        });
        this.getBone("wing_right").ifPresent(bone -> {
            bone.setHidden(flying);
            bone.setChildrenHidden(flying);
        });
        this.getBone("fly").ifPresent(bone -> {
            bone.setHidden(!flying);
            bone.setChildrenHidden(!flying);
        });
    }
}
