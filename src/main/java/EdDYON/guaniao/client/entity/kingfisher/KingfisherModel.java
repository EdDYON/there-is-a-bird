package EdDYON.guaniao.client.entity.kingfisher;

import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.content.bird.kingfisher.KingfisherDefinition;
import EdDYON.guaniao.content.bird.kingfisher.KingfisherEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class KingfisherModel extends GeoModel<KingfisherEntity> {
    @Override
    public void setCustomAnimations(KingfisherEntity bird, long instanceId,
                                    AnimationState<KingfisherEntity> animationState) {
        super.setCustomAnimations(bird, instanceId, animationState);
        if (!bird.isEating()) return;
        float tick = bird.getEatingTicks(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        // Keep the author's perched pose. Two brief bill dips handle the catch;
        // the final lift accompanies swallowing, never the spray or shake clips.
        float handling = pulse(tick, 11, 5) + pulse(tick, 22, 5);
        float swallow = pulse(tick, 38, 11);
        this.getBone("head").ifPresent(head ->
                head.setRotX(head.getRotX() + (-26 * handling + 14 * swallow) * Mth.DEG_TO_RAD));
        this.getBone("upbody").ifPresent(body ->
                body.setRotX(body.getRotX() - 5 * handling * Mth.DEG_TO_RAD));
    }

    private static float pulse(float tick, float center, float radius) {
        float value = Mth.clamp(1 - Math.abs(tick - center) / radius, 0, 1);
        return value * value * (3 - 2 * value);
    }

    @Override
    public ResourceLocation getModelResource(KingfisherEntity bird) {
        return KingfisherDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(KingfisherEntity bird) {
        return BirdMutationTextureFactory.textureFor(KingfisherDefinition.TEXTURE, bird);
    }

    @Override
    public ResourceLocation getAnimationResource(KingfisherEntity bird) {
        return KingfisherDefinition.ANIMATION;
    }
}
