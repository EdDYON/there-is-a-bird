package EdDYON.guaniao.content.bird.flight;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;

/** Plays flight animations, recovering a missed GeckoLib animation handoff. */
public final class BirdFlightAnimation {
    private BirdFlightAnimation() {
    }

    public static <T extends GeoAnimatable> PlayState play(AnimationState<T> state, RawAnimation animation) {
        AnimationController<T> controller = state.getController();
        // Lift is applied immediately by the flight motor. A transition holds
        // the new clip at time zero while the bird is already moving upward.
        // Ground predicates restore their own blend duration on each frame.
        controller.transitionLength(0);
        // GeckoLib 4.4 can skip polling the replacement when two transitions share
        // a seek time. The requested RawAnimation then matches, but the old clip
        // keeps running. Reload only that mismatch, never a healthy flight loop.
        if (state.isCurrentAnimation(animation)
                && (controller.getAnimationState() == AnimationController.State.STOPPED
                || controller.getAnimationState() == AnimationController.State.RUNNING
                && animation.getAnimationStages().stream().noneMatch(stage ->
                        state.isCurrentAnimationStage(stage.animationName())))) {
            controller.forceAnimationReset();
        }
        // Takeoff-to-cruise sequences may already be in their second stage.
        // Every stage in the requested sequence is valid and must keep its progress.
        return state.setAndContinue(animation);
    }
}
