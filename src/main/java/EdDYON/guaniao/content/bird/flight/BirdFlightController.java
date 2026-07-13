package EdDYON.guaniao.content.bird.flight;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class BirdFlightController {
    private static final Map<Mob, FlightProgress> FLIGHT_PROGRESS = Collections.synchronizedMap(new WeakHashMap<>());

    private BirdFlightController() {
    }

    public static Vec3 blendMovement(Vec3 current, Vec3 desired, double desiredWeight) {
        double weight = Mth.clamp(desiredWeight, 0.0D, 1.0D);
        return current.scale(1.0D - weight).add(desired.scale(weight));
    }

    public static Vec3 steerToward(Mob bird, Vec3 target, double speed, double minVertical, double maxVertical) {
        Vec3 toTarget = target.subtract(bird.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (horizontal.lengthSqr() <= 1.0E-4D) {
            horizontal = bird.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        }
        if (horizontal.lengthSqr() <= 1.0E-4D) {
            horizontal = bird.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (horizontal.lengthSqr() <= 1.0E-4D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        double vertical = Mth.clamp(toTarget.y * 0.12D, minVertical, maxVertical);
        return horizontal.normalize().scale(speed).add(0.0D, vertical, 0.0D);
    }

    public static double decelerateNearLanding(double baseSpeed, double distance, double decelerationDistance, double minFactor) {
        if (decelerationDistance <= 0.0D || distance >= decelerationDistance) {
            return baseSpeed;
        }
        double factor = Mth.clamp(distance / decelerationDistance, minFactor, 1.0D);
        return baseSpeed * factor;
    }

    public static boolean isStalledInAir(Mob bird, int timeFlying, double minMovementSqr) {
        return timeFlying > 15 && !bird.onGround() && bird.getDeltaMovement().lengthSqr() < minMovementSqr;
    }

    /** Detects looping by measuring whether distance to the current target is actually shrinking. */
    public static boolean isFlightProgressStalled(Mob bird, Vec3 target, int graceTicks, int maxStalledTicks) {
        if (target == null || bird.onGround()) {
            clearFlightProgress(bird);
            return false;
        }
        double distanceSqr = bird.position().distanceToSqr(target);
        FlightProgress progress = FLIGHT_PROGRESS.get(bird);
        if (progress == null || progress.target.distanceToSqr(target) > 0.25D) {
            FLIGHT_PROGRESS.put(bird, new FlightProgress(target, distanceSqr));
            return false;
        }
        ++progress.ticks;
        if (distanceSqr + 0.01D < progress.bestDistanceSqr) {
            progress.bestDistanceSqr = distanceSqr;
            progress.stalledTicks = 0;
            return false;
        }
        if (progress.ticks <= graceTicks) {
            return false;
        }
        return ++progress.stalledTicks >= maxStalledTicks;
    }

    public static void clearFlightProgress(Mob bird) {
        FLIGHT_PROGRESS.remove(bird);
    }

    public static void faceMovement(Mob bird, Vec3 movement, float maxPitchDegrees) {
        double horizontalLength = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        if (horizontalLength <= 1.0E-4D) {
            return;
        }
        float yaw = (float)(Mth.atan2(movement.z, movement.x) * 57.29577951308232D) - 90.0F;
        float pitch = Mth.clamp((float)(-(Math.atan2(movement.y, horizontalLength) * 57.29577951308232D)), -maxPitchDegrees, maxPitchDegrees);
        bird.setYRot(yaw);
        bird.setYHeadRot(yaw);
        bird.yBodyRot = yaw;
        bird.yBodyRotO = yaw;
        bird.yHeadRot = yaw;
        bird.yHeadRotO = yaw;
        bird.setXRot(pitch);
        bird.xRotO = pitch;
    }

    public static boolean faceGroundMovement(Mob bird, Vec3 movement, double minHorizontalSpeedSqr) {
        if (movement.horizontalDistanceSqr() <= minHorizontalSpeedSqr) {
            return false;
        }
        float yaw = (float)(Mth.atan2(movement.z, movement.x) * 57.29577951308232D) - 90.0F;
        bird.setYRot(yaw);
        bird.setYHeadRot(yaw);
        bird.yBodyRot = yaw;
        bird.yBodyRotO = yaw;
        bird.yHeadRot = yaw;
        bird.yHeadRotO = yaw;
        bird.setXRot(0.0F);
        bird.xRotO = 0.0F;
        return true;
    }

    public static boolean shouldPlayFlyAnimation(BirdFlightAware bird, boolean airborneState, boolean onGround, boolean noGravity, Vec3 movement, int airborneGraceTicks) {
        if (bird.isBirdFlightActive() || airborneState) {
            return true;
        }
        if (onGround) {
            return false;
        }
        if (airborneGraceTicks > 0) {
            return true;
        }
        if (noGravity || bird.isBirdLanding() || bird.isBirdEscaping()) {
            return true;
        }
        if (movement.y > -0.85D) {
            return true;
        }
        return movement.horizontalDistanceSqr() > 0.001D;
    }

    private static final class FlightProgress {
        private final Vec3 target;
        private double bestDistanceSqr;
        private int ticks;
        private int stalledTicks;

        private FlightProgress(Vec3 target, double bestDistanceSqr) {
            this.target = target;
            this.bestDistanceSqr = bestDistanceSqr;
        }
    }
}
