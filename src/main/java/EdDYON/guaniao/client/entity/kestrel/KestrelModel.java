package EdDYON.guaniao.client.entity.kestrel;

import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.content.bird.kestrel.KestrelDefinition;
import EdDYON.guaniao.content.bird.kestrel.KestrelEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;

public final class KestrelModel extends GeoModel<KestrelEntity> {
    @Override
    public void setCustomAnimations(KestrelEntity bird, long instanceId, AnimationState<KestrelEntity> state) {
        super.setCustomAnimations(bird, instanceId, state);
        if (bird.hasExtendedTalons()) {
            // Flight clips retract the legs by 64-94 degrees. Contact needs the
            // same extended-foot anchor used by the server, including blends.
            for (String name : new String[]{"leg_left", "leg_right"}) {
                this.getBone(name).ifPresent(bone -> {
                    bone.setRotX(0);
                    bone.setPosY(0);
                });
            }
            this.getBone("root").ifPresent(bone -> {
                bone.setPosX(0);
                bone.setPosY(0);
                bone.setPosZ(0);
                bone.setRotX(0);
                bone.setScaleY(1);
            });
        }
    }

    @Override
    public ResourceLocation getModelResource(KestrelEntity animatable) {
        return KestrelDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(KestrelEntity animatable) {
        return BirdMutationTextureFactory.textureFor(KestrelDefinition.TEXTURE, animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(KestrelEntity animatable) {
        return KestrelDefinition.ANIMATION;
    }
}
