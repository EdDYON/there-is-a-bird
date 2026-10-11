package EdDYON.guaniao.content.bird.hummingbird;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Continuous world-space flight, with either autonomous facing or an independent point of attention. */
final class HummingbirdFlightMotor {
    private static final double SIDE_SPEED = 0.20;
    private static final double BACKWARD_SPEED = 0.18;
    private final HummingbirdEntity bird;
    private Vec3 target, facing;
    private double speed = 0.28;
    HummingbirdFlightMotor(HummingbirdEntity bird) { this.bird = bird; }
    void aim(Vec3 target, Vec3 facing, double speed) {
        this.target = target; this.facing = facing; this.speed = speed;
    }
    void travel(Vec3 target, double speed) { aim(target, null, speed); }
    void clear() { target = facing = null; bird.setDeltaMovement(Vec3.ZERO); bird.setBankAngle(0); }
    void tick(ServerLevel level) {
        if (target == null || !bird.isAirborne() || bird.isPassenger()) {
            bird.setDeltaMovement(Vec3.ZERO); bird.setBankAngle(0); return;
        }
        Vec3 delta = target.subtract(bird.position());
        Vec3 desired = delta.lengthSqr() < 0.0004 ? Vec3.ZERO : delta.scale(0.35);
        boolean independentFacing = facing != null;
        if (desired.length() > speed) desired = desired.normalize().scale(speed);
        Vec3 previousVelocity = bird.getDeltaMovement();
        Vec3 velocity = previousVelocity;
        if (independentFacing) {
            turnToward(facing.subtract(bird.position()), 18);
            desired = limitIndependentFlight(desired);
            velocity = accelerate(velocity, desired);
        } else {
            velocity = forwardStep(desired, velocity);
        }
        // Avoid unloaded chunks and predict the swept collision box, not just a center ray.
        if (!clearStep(level, velocity)) {
            Vec3[] detours = {new Vec3(0, 0.11, 0), new Vec3(0.10, 0.07, 0),
                    new Vec3(-0.10, 0.07, 0), new Vec3(0, 0.07, 0.10), new Vec3(0, 0.07, -0.10)};
            velocity = Vec3.ZERO;
            for (Vec3 candidate : detours) {
                candidate = accelerate(previousVelocity, candidate);
                if (clearStep(level, candidate)) { velocity = candidate; break; }
            }
            if (!independentFacing) turnToward(velocity, 18);
        }
        bird.setDeltaMovement(velocity);
        Vec3 forward = forwardVector(bird.getYRot());
        Vec3 right = new Vec3(forward.z, 0, -forward.x);
        double lateralForce = velocity.subtract(previousVelocity).dot(right) + velocity.dot(right) * .012;
        float bank = Mth.clamp((float)Math.toDegrees(Math.atan2(lateralForce, .08)), -15, 15);
        bird.setBankAngle(Mth.lerp(.25F, bird.getBankAngle(), bank));
        bird.setXRot(0);
        bird.resetFallDistance();
    }
    private Vec3 forwardStep(Vec3 desired, Vec3 velocity) {
        double currentSpeed = velocity.horizontalDistance();
        double wantedSpeed = desired.horizontalDistance();
        boolean reversing = currentSpeed > .05 && wantedSpeed > .01
                && (velocity.x * desired.x + velocity.z * desired.z) < 0;
        boolean turnBeforeAccelerating = currentSpeed <= .05 && wantedSpeed > .01
                && Math.abs(Mth.wrapDegrees(directionYaw(desired) - bird.getYRot())) > 25;
        // Keep physical momentum in world space. A half-turn brakes first; it never rotates velocity by decree.
        Vec3 request = reversing || turnBeforeAccelerating ? new Vec3(0, desired.y, 0) : desired;
        Vec3 next = accelerate(velocity, request);
        turnToward(turnBeforeAccelerating || next.horizontalDistanceSqr() < .0025 ? desired : next, 18);
        return next;
    }
    private Vec3 limitIndependentFlight(Vec3 desired) {
        Vec3 forward = forwardVector(bird.getYRot());
        Vec3 right = new Vec3(forward.z, 0, -forward.x);
        double along = desired.dot(forward), sideways = desired.dot(right);
        double alongLimit = along < 0 ? Math.min(speed, BACKWARD_SPEED) : speed;
        double sideLimit = Math.min(speed, SIDE_SPEED);
        double ratio = Math.hypot(along / Math.max(.001, alongLimit), sideways / Math.max(.001, sideLimit));
        // Bound the local horizontal ellipse without depending on how far away the target happens to be.
        if (ratio > 1) { along /= ratio; sideways /= ratio; }
        return forward.scale(along).add(right.scale(sideways)).add(0, desired.y, 0);
    }
    private static Vec3 accelerate(Vec3 velocity, Vec3 desired) {
        Vec3 change = desired.subtract(velocity);
        if (change.length() > .045) change = change.normalize().scale(.045);
        return velocity.add(change);
    }
    private void turnToward(Vec3 direction, float limit) {
        if (direction.horizontalDistanceSqr() <= 0.0001) return;
        float yaw = Mth.approachDegrees(bird.getYRot(), directionYaw(direction), limit);
        bird.setYRot(yaw); bird.yBodyRot = yaw; bird.setYHeadRot(yaw);
    }
    private static float directionYaw(Vec3 direction) {
        return (float)(Mth.atan2(direction.z, direction.x) * 180 / Math.PI) - 90;
    }
    private static Vec3 forwardVector(float yaw) {
        double radians = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Math.sin(radians), 0, Math.cos(radians));
    }
    private boolean clearStep(ServerLevel level, Vec3 velocity) {
        var swept = bird.getBoundingBox().expandTowards(velocity).inflate(1.0);
        for (int x = Mth.floor(swept.minX) >> 4; x <= Mth.floor(swept.maxX) >> 4; x++)
            for (int z = Mth.floor(swept.minZ) >> 4; z <= Mth.floor(swept.maxZ) >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        return level.noCollision(bird, bird.getBoundingBox().move(velocity));
    }
    static boolean visible(ServerLevel level, Vec3 start, Vec3 end, HummingbirdEntity bird) {
        // Only small, nearby target rays are accepted; never load a chunk while looking for nectar.
        if (start.distanceToSqr(end) > 32 * 32) return false;
        int minX = Mth.floor(Math.min(start.x, end.x) - 1) >> 4, maxX = Mth.floor(Math.max(start.x, end.x) + 1) >> 4;
        int minZ = Mth.floor(Math.min(start.z, end.z) - 1) >> 4, maxZ = Mth.floor(Math.max(start.z, end.z) + 1) >> 4;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bird))
                .getType() == HitResult.Type.MISS;
    }
}
