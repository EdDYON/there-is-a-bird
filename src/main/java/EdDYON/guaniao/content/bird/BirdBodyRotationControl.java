package EdDYON.guaniao.content.bird;

import java.util.function.Predicate;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

/** Client presentation only: keep the rendered body facing the path actually being interpolated. */
public final class BirdBodyRotationControl extends BodyRotationControl {
    private static final double MIN_MOTION_SQR = 1.0E-8D;
    private static final double MAX_MOTION_SQR = 4.0D;
    private static final float MAX_TURN_PER_TICK = 90.0F;

    private final Mob bird;
    private final Predicate<Mob> preserveFacing;

    public BirdBodyRotationControl(Mob bird) {
        this(bird, ignored -> false);
    }

    /** Allows a species to keep intentional strafing, such as a kestrel facing prey while hovering. */
    public BirdBodyRotationControl(Mob bird, Predicate<Mob> preserveFacing) {
        super(bird);
        this.bird = bird;
        this.preserveFacing = preserveFacing;
    }

    @Override
    public void clientTick() {
        // This method also runs on the server. Keep vanilla control and all
        // navigation/motor rotations there, and retain vanilla idle head turning.
        super.clientTick();
        boolean eligible = this.bird.level().isClientSide
                && this.bird.isAlive()
                && !this.bird.isNoAi()
                && !this.bird.isPassenger()
                && !this.bird.isInWaterOrBubble()
                && this.bird.hurtTime == 0
                && !this.preserveFacing.test(this.bird);
        this.bird.yBodyRot = bodyYawForMotion(
                this.bird.yBodyRotO, this.bird.yBodyRot,
                this.bird.getX() - this.bird.xo,
                this.bird.getY() - this.bird.yo,
                this.bird.getZ() - this.bird.zo, eligible);
        // Do not rewrite yRot, head rotation, pitch, or yBodyRotO. GeckoLib's
        // normal partial-tick interpolation provides the single visual transition.
    }

    static float bodyYawForMotion(float previousBodyYaw, float vanillaBodyYaw,
                                  double deltaX, double deltaY, double deltaZ,
                                  boolean eligible) {
        double horizontalSqr = deltaX * deltaX + deltaZ * deltaZ;
        double distanceSqr = horizontalSqr + deltaY * deltaY;
        if (!eligible || !Double.isFinite(distanceSqr)
                || horizontalSqr <= MIN_MOTION_SQR || distanceSqr > MAX_MOTION_SQR) {
            return vanillaBodyYaw;
        }

        float motionYaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        float delta = Mth.wrapDegrees(motionYaw - previousBodyYaw);
        // Ordinary turns reach the measured heading in this tick. A sudden
        // reversal takes at most two ticks, using the shortest angular path.
        // Starting from last tick's visible body avoids resetting this limit
        // from the independently interpolated entity yaw every tick.
        return previousBodyYaw + Mth.clamp(delta, -MAX_TURN_PER_TICK, MAX_TURN_PER_TICK);
    }
}
