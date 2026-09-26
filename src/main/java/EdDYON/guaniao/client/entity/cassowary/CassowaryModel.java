package EdDYON.guaniao.client.entity.cassowary;

import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.content.bird.cassowary.CassowaryDefinition;
import EdDYON.guaniao.content.bird.cassowary.CassowaryEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class CassowaryModel extends GeoModel<CassowaryEntity> {
    @Override
    public void setCustomAnimations(CassowaryEntity bird, long instanceId,
                                    AnimationState<CassowaryEntity> animationState) {
        super.setCustomAnimations(bird, instanceId, animationState);
        float weight = bird.getClientGazeWeight();
        if (weight <= 0.001F) {
            return;
        }

        // Cassowary gaze is intentionally head-led. Its laterally positioned eyes
        // make head and neck orientation more natural than human-like independent
        // eye tracking. Authored animations retain full control of the eye bones;
        // procedural gaze is layered through head -> upper neck -> lower neck.
        addRotation("head", bird.getClientHeadPitch(), bird.getClientHeadYaw(), weight);
        addRotation("neckUpper", bird.getClientUpperNeckPitch(), bird.getClientUpperNeckYaw(), weight);
        addRotation("neckLower", bird.getClientLowerNeckPitch(), bird.getClientLowerNeckYaw(), weight);
    }

    private void addRotation(String boneName, float pitchDegrees, float yawDegrees, float weight) {
        this.getBone(boneName).ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + pitchDegrees * weight * Mth.DEG_TO_RAD);
            bone.setRotY(bone.getRotY() + yawDegrees * weight * Mth.DEG_TO_RAD);
        });
    }

    @Override
    public ResourceLocation getModelResource(CassowaryEntity animatable) {
        return CassowaryDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CassowaryEntity animatable) {
        return BirdMutationTextureFactory.textureFor(CassowaryDefinition.TEXTURE, animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(CassowaryEntity animatable) {
        return CassowaryDefinition.ANIMATION;
    }
}
