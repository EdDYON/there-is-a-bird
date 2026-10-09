package EdDYON.guaniao.content.bird;

import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.state.BoneSnapshot;

/** Advances ground locomotion by travel, without retiming an already playing GeckoLib clip. */
public class BirdMovementAnimationController<T extends GeoAnimatable> extends AnimationController<T> {
    private final BirdWalkDistance distance = new BirdWalkDistance();
    private double groundPixelsPerTick;
    private double maximumGroundPixelsPerTick = Double.POSITIVE_INFINITY;
    private double distanceAtLoopStart;
    private boolean groundClockRunning;

    public BirdMovementAnimationController(T bird, String name, int transitionTicks, AnimationStateHandler<T> handler) {
        super(bird, name, transitionTicks, handler);
    }

    void useGroundMovement(double pixelsPerTick) {
        useGroundMovement(pixelsPerTick, Double.POSITIVE_INFINITY);
    }

    void useGroundMovement(double pixelsPerTick, double maximumAnimationTicksPerTick) {
        this.groundPixelsPerTick = pixelsPerTick;
        this.maximumGroundPixelsPerTick = pixelsPerTick * maximumAnimationTicksPerTick;
        // The clock supplies the speed. Transitions retain their real-time duration.
        setAnimationSpeed(1);
    }

    @Override
    protected PlayState handleAnimationState(AnimationState<T> state) {
        this.groundPixelsPerTick = 0;
        this.maximumGroundPixelsPerTick = Double.POSITIVE_INFINITY;
        return super.handleAnimationState(state);
    }

    @Override
    public void process(CoreGeoModel<T> model, AnimationState<T> state, Map<String, CoreGeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
        if (this.animatable instanceof PathfinderMob bird) {
            float partial = state.getPartialTick();
            sampleTravel(seekTime, Mth.lerp(partial, bird.xo, bird.getX()),
                    Mth.lerp(partial, bird.zo, bird.getZ()), BirdGroundAnimation.canPlayWalk(bird),
                    BirdGroundAnimation.renderScale(bird));
        }
        super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
    }

    /** Separate from entity access so real animation playback can be regression-tested. */
    protected final void sampleTravel(double time, double x, double z, boolean grounded, double scale) {
        this.distance.sample(time, x, z, grounded, scale, this.maximumGroundPixelsPerTick);
    }

    @Override
    protected double adjustTick(double tick) {
        if (this.groundPixelsPerTick <= 0 || getAnimationState() != State.RUNNING) {
            this.groundClockRunning = false;
            this.distanceAtLoopStart = this.distance.pixels();
            return super.adjustTick(tick);
        }
        double phase = (this.distance.pixels() - this.distanceAtLoopStart) / this.groundPixelsPerTick;
        if (this.shouldResetTick) {
            double length = this.currentAnimation == null ? 0 : this.currentAnimation.animation().length();
            boolean looping = this.groundClockRunning && length > 0 && phase >= length;
            super.adjustTick(tick);
            // Keep the fractional step at a loop boundary, including at low frame rates.
            phase = looping ? phase % length : 0;
            this.distanceAtLoopStart = this.distance.pixels() - phase * this.groundPixelsPerTick;
        } else if (!this.groundClockRunning) {
            this.distanceAtLoopStart = this.distance.pixels();
            phase = 0;
        }
        this.groundClockRunning = true;
        return Math.max(0, phase);
    }
}
