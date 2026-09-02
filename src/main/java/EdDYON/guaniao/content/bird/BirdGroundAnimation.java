package EdDYON.guaniao.content.bird;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class BirdGroundAnimation {
    private static final double WALK_MOTION_THRESHOLD_SQR = 1.0E-5D;
    private static final double CLIENT_POSITION_THRESHOLD_SQR = 1.0E-6D;
    private static final int CLIENT_MOTION_GRACE_TICKS = 3;
    private static final double NORMAL_WALK_VELOCITY = 0.12D;
    private static final Map<PathfinderMob, ClientMotionSample> CLIENT_MOTION_SAMPLES = Collections.synchronizedMap(new WeakHashMap<>());

    private BirdGroundAnimation() {
    }

    public static boolean canPlayWalk(PathfinderMob bird) {
        return bird.onGround()
                && !bird.isPassenger()
                && !bird.isInWaterOrBubble();
    }

    public static boolean hasWalkMotion(PathfinderMob bird) {
        return hasWalkMotion(bird, false);
    }

    public static boolean hasWalkMotion(PathfinderMob bird, boolean animationMoving) {
        boolean clientPositionMoving = bird.level().isClientSide && hasClientPositionMotion(bird);
        return canPlayWalk(bird)
                && (animationMoving
                || clientPositionMoving
                || bird.getDeltaMovement().horizontalDistanceSqr() > WALK_MOTION_THRESHOLD_SQR
                || (!bird.level().isClientSide && !bird.getNavigation().isDone()));
    }

    /** Keeps walk cycles tied to measured horizontal movement instead of a fixed 1x rate. */
    public static double walkAnimationSpeed(PathfinderMob bird) {
        if (!bird.level().isClientSide) {
            double speed = Math.sqrt(bird.getDeltaMovement().horizontalDistanceSqr());
            return Mth.clamp(speed / NORMAL_WALK_VELOCITY, 0.65D, 1.55D);
        }
        synchronized (CLIENT_MOTION_SAMPLES) {
            ClientMotionSample sample = updateClientMotion(bird);
            float target = Mth.clamp(
                    (float)(sample.horizontalSpeed / NORMAL_WALK_VELOCITY), 0.65F, 1.55F);
            sample.animationSpeed = Mth.lerp(0.24F, sample.animationSpeed, target);
            return sample.animationSpeed;
        }
    }

    private static boolean hasClientPositionMotion(PathfinderMob bird) {
        synchronized (CLIENT_MOTION_SAMPLES) {
            ClientMotionSample sample = updateClientMotion(bird);
            return sample.motionGraceTicks > 0;
        }
    }

    private static ClientMotionSample updateClientMotion(PathfinderMob bird) {
        ClientMotionSample sample = CLIENT_MOTION_SAMPLES.get(bird);
        if (sample == null) {
            sample = new ClientMotionSample(bird.tickCount, bird.getX(), bird.getZ());
            CLIENT_MOTION_SAMPLES.put(bird, sample);
            return sample;
        }
        if (sample.tick == bird.tickCount) {
            return sample;
        }
        int elapsedTicks = Math.max(1, bird.tickCount - sample.tick);
        double deltaX = bird.getX() - sample.x;
        double deltaZ = bird.getZ() - sample.z;
        double distanceSqr = deltaX * deltaX + deltaZ * deltaZ;
        if (elapsedTicks <= CLIENT_MOTION_GRACE_TICKS + 1
                && distanceSqr > CLIENT_POSITION_THRESHOLD_SQR) {
            sample.motionGraceTicks = CLIENT_MOTION_GRACE_TICKS;
            sample.horizontalSpeed = Math.sqrt(distanceSqr) / elapsedTicks;
        } else {
            sample.motionGraceTicks = Math.max(0, sample.motionGraceTicks - elapsedTicks);
            sample.horizontalSpeed *= 0.65D;
        }
        sample.tick = bird.tickCount;
        sample.x = bird.getX();
        sample.z = bird.getZ();
        return sample;
    }

    private static final class ClientMotionSample {
        private int tick;
        private double x;
        private double z;
        private int motionGraceTicks;
        private double horizontalSpeed;
        private float animationSpeed = 1.0F;

        private ClientMotionSample(int tick, double x, double z) {
            this.tick = tick;
            this.x = x;
            this.z = z;
        }
    }
}
