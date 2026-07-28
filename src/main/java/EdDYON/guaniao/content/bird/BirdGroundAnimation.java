package EdDYON.guaniao.content.bird;

import net.minecraft.world.entity.PathfinderMob;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class BirdGroundAnimation {
    private static final double WALK_MOTION_THRESHOLD_SQR = 1.0E-5D;
    private static final double CLIENT_POSITION_THRESHOLD_SQR = 1.0E-6D;
    private static final int CLIENT_MOTION_GRACE_TICKS = 3;
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

    private static boolean hasClientPositionMotion(PathfinderMob bird) {
        synchronized (CLIENT_MOTION_SAMPLES) {
            ClientMotionSample sample = CLIENT_MOTION_SAMPLES.get(bird);
            if (sample == null) {
                CLIENT_MOTION_SAMPLES.put(bird, new ClientMotionSample(bird.tickCount, bird.getX(), bird.getZ()));
                return false;
            }
            if (sample.tick == bird.tickCount) {
                return sample.motionGraceTicks > 0;
            }
            int elapsedTicks = Math.max(1, bird.tickCount - sample.tick);
            double deltaX = bird.getX() - sample.x;
            double deltaZ = bird.getZ() - sample.z;
            if (elapsedTicks <= CLIENT_MOTION_GRACE_TICKS + 1
                    && deltaX * deltaX + deltaZ * deltaZ > CLIENT_POSITION_THRESHOLD_SQR) {
                sample.motionGraceTicks = CLIENT_MOTION_GRACE_TICKS;
            } else {
                sample.motionGraceTicks = Math.max(0, sample.motionGraceTicks - elapsedTicks);
            }
            sample.tick = bird.tickCount;
            sample.x = bird.getX();
            sample.z = bird.getZ();
            return sample.motionGraceTicks > 0;
        }
    }

    private static final class ClientMotionSample {
        private int tick;
        private double x;
        private double z;
        private int motionGraceTicks;

        private ClientMotionSample(int tick, double x, double z) {
            this.tick = tick;
            this.x = x;
            this.z = z;
        }
    }
}
