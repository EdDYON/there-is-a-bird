package EdDYON.guaniao.content.bird;

import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import net.minecraft.world.entity.PathfinderMob;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public final class BirdGroundAnimation {
    private BirdGroundAnimation() { }

    public static boolean canPlayWalk(PathfinderMob bird) {
        return bird.onGround() && !bird.isPassenger() && !bird.isInWaterOrBubble();
    }

    public static boolean hasWalkMotion(PathfinderMob bird) { return hasWalkMotion(bird, false); }

    public static boolean hasWalkMotion(PathfinderMob bird, boolean animationMoving) {
        if (!bird.level().isClientSide) {
            // Server behavior code also uses this helper. Preserve its navigation-intent
            // semantics; only the client presentation follows measured displacement.
            return canPlayWalk(bird) && (animationMoving
                    || bird.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5
                    || !bird.getNavigation().isDone());
        }
        // Navigation intent and stale client velocity are not evidence of travel (e.g. against a wall).
        return canPlayWalk(bird) && horizontalSpeed(bird) > 0.0001;
    }

    public static double horizontalSpeed(PathfinderMob bird) {
        double dx = bird.getX() - bird.xo, dz = bird.getZ() - bird.zo;
        double distanceSqr = dx * dx + dz * dz;
        return Double.isFinite(distanceSqr) && distanceSqr <= 4 ? Math.sqrt(distanceSqr) : 0;
    }

    static double renderScale(PathfinderMob bird) {
        return bird instanceof ScalableBirdModel scalable ? scalable.getModelRenderScale() : 1;
    }

    public static <T extends GeoAnimatable> PlayState play(AnimationState<T> state, PathfinderMob bird, RawAnimation clip) {
        return play(state, bird, clip, BirdWalkStride.Gait.WALK);
    }

    public static <T extends GeoAnimatable> PlayState play(AnimationState<T> state, PathfinderMob bird,
                                                          RawAnimation clip, BirdWalkStride.Gait gait) {
        if (!(state.getController() instanceof BirdMovementAnimationController<?> controller)) {
            throw new IllegalStateException("Bird locomotion requires a distance-driven movement controller");
        }
        controller.useGroundMovement(BirdWalkStride.pixelsPerTick(bird, gait),
                BirdWalkStride.maximumAnimationTicksPerTick(bird, gait));
        return state.setAndContinue(clip);
    }
}
