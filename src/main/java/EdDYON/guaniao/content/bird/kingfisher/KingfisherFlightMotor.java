package EdDYON.guaniao.content.bird.kingfisher;

import EdDYON.guaniao.content.bird.flight.BirdFlightLock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded local steering. The entity owns its activities, destinations and landing decisions. */
public final class KingfisherFlightMotor {
    public enum Mode { TRANSIT, HOVER, DIVE, SKIM, EMERGE, ESCAPE, LANDING }
    public enum Result { MOVING, ARRIVED, BLOCKED, UNLOADED, STALLED }
    private enum Clearance { CLEAR, BLOCKED, UNLOADED }

    private static final double MAX_PATH = 32.0D;
    private static final double SAMPLE_LENGTH = 0.20D;
    private static final double EPSILON = 1.0E-6D;
    private static final int DETOUR_TICKS = 16;
    private static final double[] DETOUR_ANGLES = {45, -45, 90, -90, 135, -135, 180};
    private final KingfisherEntity bird;
    private Vec3 destination;
    private Mode previousMode;
    private Vec3 detour;
    private int detourStarted;
    private int preferredSide;
    private Vec3 motionCheckpoint;
    private int motionCheckpointTick;
    private double bestDistance;
    private int progressTick;

    public KingfisherFlightMotor(KingfisherEntity bird) {
        this.bird = bird;
        this.preferredSide = (bird.getId() & 1) == 0 ? 1 : -1;
    }

    /** Clears steering memory only; the caller decides whether to stop or keep its current momentum. */
    public void reset() {
        this.destination = null;
        this.previousMode = null;
        this.detour = null;
        this.motionCheckpoint = null;
    }

    public Result flyTo(Vec3 target, double speed, Mode mode) {
        boolean escapingWater = mode == Mode.EMERGE && this.bird.isInWaterOrBubble();
        if (this.bird.level().isClientSide || this.bird.isPassenger()
                || BirdFlightLock.isFlightDisabled(this.bird) && !escapingWater) {
            this.reset();
            return Result.BLOCKED;
        }
        Vec3 origin = this.bird.position();
        if (!finite(target) || !finite(origin) || mode == null || !Double.isFinite(speed) || speed <= 0
                || origin.distanceToSqr(target) > MAX_PATH * MAX_PATH) return this.stop(Result.BLOCKED);
        double distance = origin.distanceTo(target);
        this.trackDestination(target, mode, distance);
        if (distance < 0.045D) return this.stop(Result.ARRIVED);
        if (!this.loadedBounds(this.boxAt(origin).minmax(this.boxAt(target)))) return this.stop(Result.UNLOADED);
        if (this.isStalled(origin, distance)) return this.stop(Result.STALLED);

        boolean allowWater = mode == Mode.EMERGE;
        boolean canDetour = mode == Mode.TRANSIT || mode == Mode.ESCAPE || mode == Mode.EMERGE;
        double lookAhead = Math.min(distance, Math.min(3.0D, Math.max(0.65D, speed * 4.0D)));
        Vec3 forwardPoint = origin.add(target.subtract(origin).scale(lookAhead / distance));
        Clearance straight = this.pathClearance(origin, forwardPoint, allowWater);
        if (straight == Clearance.UNLOADED) return this.stop(Result.UNLOADED);
        if (this.detour != null && (this.bird.tickCount - this.detourStarted >= DETOUR_TICKS
                || origin.distanceToSqr(this.detour) < 0.08D
                || straight == Clearance.CLEAR && this.bird.tickCount - this.detourStarted >= 8)) this.detour = null;

        Vec3 aim = this.detour == null ? target : this.detour;
        Vec3 velocity = steerVelocity(this.bird.getDeltaMovement(), aim.subtract(origin), this.bird.getYRot(), speed, mode);
        Clearance step = this.pathClearance(origin, origin.add(velocity), allowWater);
        boolean rememberedClear = this.detour != null
                && this.pathClearance(origin, this.detour, allowWater) == Clearance.CLEAR;
        if ((this.detour == null && straight == Clearance.CLEAR || rememberedClear) && step == Clearance.CLEAR) {
            this.bird.setDeltaMovement(velocity);
            return Result.MOVING;
        }
        if (!canDetour) return this.stop(step == Clearance.UNLOADED ? Result.UNLOADED : Result.BLOCKED);

        this.detour = this.findDetour(origin, target, speed, mode);
        if (this.detour == null) return this.stop(Result.BLOCKED);
        this.detourStarted = this.bird.tickCount;
        velocity = steerVelocity(this.bird.getDeltaMovement(), this.detour.subtract(origin), this.bird.getYRot(), speed, mode);
        // findDetour checks this exact steered step as well as the entire short candidate corridor.
        this.bird.setDeltaMovement(velocity);
        return Result.MOVING;
    }

    /** Tests the bird's complete swept volume, without loading any chunk or changing its state. */
    public boolean clearPath(Vec3 from, Vec3 to, boolean allowWater) {
        return this.pathClearance(from, to, allowWater) == Clearance.CLEAR;
    }

    /** Follow one prevalidated fishing-arc segment through ordinary travel, with no teleport or detour. */
    public Result followFishingStep(Vec3 expectedPrevious, Vec3 next) {
        if (this.bird.level().isClientSide || this.bird.isPassenger() || BirdFlightLock.isFlightDisabled(this.bird)
                || !finite(expectedPrevious) || !finite(next)) return this.stop(Result.BLOCKED);
        Vec3 origin = this.bird.position();
        if (!finite(origin) || origin.distanceToSqr(expectedPrevious) > 0.12D * 0.12D)
            return this.stop(Result.STALLED);
        Vec3 velocity = next.subtract(origin);
        if (velocity.lengthSqr() > 0.65D * 0.65D) return this.stop(Result.BLOCKED);
        Clearance clearance = this.pathClearance(origin, next, true);
        if (clearance != Clearance.CLEAR)
            return this.stop(clearance == Clearance.UNLOADED ? Result.UNLOADED : Result.BLOCKED);
        this.bird.setDeltaMovement(velocity);
        return velocity.lengthSqr() < EPSILON * EPSILON ? Result.ARRIVED : Result.MOVING;
    }

    private void trackDestination(Vec3 target, Mode mode, double distance) {
        if (this.destination == null || this.previousMode != mode || this.destination.distanceToSqr(target) > 0.0625D) {
            this.destination = target;
            this.previousMode = mode;
            this.detour = null;
            this.motionCheckpoint = this.bird.position();
            this.motionCheckpointTick = this.bird.tickCount;
            this.bestDistance = distance;
            this.progressTick = this.bird.tickCount;
        }
    }

    private boolean isStalled(Vec3 origin, double distance) {
        if (distance < this.bestDistance - 0.12D) {
            this.bestDistance = distance;
            this.progressTick = this.bird.tickCount;
        }
        if (this.motionCheckpoint == null || this.motionCheckpoint.distanceToSqr(origin) > 0.04D) {
            this.motionCheckpoint = origin;
            this.motionCheckpointTick = this.bird.tickCount;
        }
        return this.bird.tickCount - this.motionCheckpointTick > 24 || this.bird.tickCount - this.progressTick > 70;
    }

    private Vec3 findDetour(Vec3 origin, Vec3 target, double speed, Mode mode) {
        Vec3 forward = target.subtract(origin).multiply(1, 0, 1);
        if (forward.lengthSqr() < 0.001D) forward = heading(this.bird.getYRot());
        else forward = forward.normalize();
        boolean allowWater = mode == Mode.EMERGE;
        Vec3 best = null;
        double bestScore = -Double.MAX_VALUE;
        double targetDistance = origin.distanceTo(target);
        for (int i = 0; i < DETOUR_ANGLES.length + 2; i++) {
            Vec3 direction;
            double angle = 0;
            if (i < DETOUR_ANGLES.length) {
                angle = DETOUR_ANGLES[i] * this.preferredSide;
                direction = forward.yRot((float)Math.toRadians(angle)).add(0, mode == Mode.EMERGE ? 0.12D : 0.04D, 0).normalize();
            } else if (i == DETOUR_ANGLES.length) direction = forward.add(0, 1.4D, 0).normalize();
            else direction = new Vec3(0, 1, 0);
            Vec3 candidate = origin.add(direction.scale(1.4D));
            Vec3 velocity = steerVelocity(this.bird.getDeltaMovement(), candidate.subtract(origin), this.bird.getYRot(), speed, mode);
            if (this.pathClearance(origin, origin.add(velocity), allowWater) != Clearance.CLEAR
                    || this.pathClearance(origin, candidate, allowWater) != Clearance.CLEAR) continue;
            double score = targetDistance - candidate.distanceTo(target);
            if (mode == Mode.EMERGE) score += direction.y * 0.5D;
            if (i < DETOUR_ANGLES.length && angle * this.preferredSide > 0) score += 0.12D;
            if (score > bestScore) { bestScore = score; best = candidate; }
        }
        if (best != null) {
            Vec3 chosen = best.subtract(origin);
            double side = forward.z * chosen.x - forward.x * chosen.z;
            if (Math.abs(side) > 0.1D) this.preferredSide = side > 0 ? 1 : -1;
        }
        return best;
    }

    /** Pure steering calculation, also usable by focused movement tests without a world. */
    public static Vec3 steerVelocity(Vec3 current, Vec3 offset, float facingYaw, double speed, Mode mode) {
        if (!finite(current)) current = Vec3.ZERO;
        if (!finite(offset) || !Float.isFinite(facingYaw) || !Double.isFinite(speed) || speed <= 0 || mode == null) return Vec3.ZERO;
        double distance = offset.length();
        if (distance < 1.0E-5D) return Vec3.ZERO;
        double maximum = Mth.clamp(speed, 0, 0.65D);
        double braking = mode == Mode.LANDING ? 0.055D : 0.085D;
        boolean arriving = mode == Mode.LANDING || mode == Mode.DIVE;
        double stopping = arriving ? stoppingSpeed(distance, braking) : Math.sqrt(2 * braking * distance);
        double wantedSpeed = Math.min(maximum, Math.min(distance * 0.62D, stopping));
        Vec3 desired = offset.scale(wantedSpeed / distance);
        if (arriving) desired = limitArrivalVelocity(desired, offset, braking * 0.7D);
        double yawLimit = mode == Mode.ESCAPE ? 36 : mode == Mode.LANDING || mode == Mode.SKIM ? 22 : 28;
        if (desired.horizontalDistanceSqr() > 1.0E-8D) {
            double yawError = Mth.wrapDegrees(yaw(desired) - facingYaw);
            float nextYaw = facingYaw + (float)Mth.clamp(yawError, -yawLimit, yawLimit);
            double turnSpeed = Math.max(0.18D, Math.cos(Math.toRadians(Math.min(90, Math.abs(yawError)))));
            desired = heading(nextYaw).scale(desired.horizontalDistance() * turnSpeed).add(0, desired.y, 0);
        }
        double acceleration = mode == Mode.ESCAPE ? 0.085D : mode == Mode.LANDING ? 0.055D : 0.065D;
        Vec3 change = desired.subtract(current);
        double limit = desired.lengthSqr() < current.lengthSqr() ? Math.max(acceleration, braking) : acceleration;
        if (change.lengthSqr() > limit * limit) change = change.normalize().scale(limit);
        Vec3 velocity = current.add(change);
        // External pushes must not start a fresh flight backwards relative to the visible body.
        if (velocity.horizontalDistanceSqr() > 1.0E-8D) {
            double yawError = Mth.wrapDegrees(yaw(velocity) - facingYaw);
            if (Math.abs(yawError) > yawLimit) velocity = heading(facingYaw + (float)Mth.clamp(yawError, -yawLimit, yawLimit))
                    .scale(Math.min(velocity.horizontalDistance(), 0.10D)).add(0, velocity.y, 0);
        }
        if (arriving) velocity = limitArrivalVelocity(velocity, offset, braking * 0.7D);
        if (velocity.lengthSqr() > maximum * maximum && maximum > 0) velocity = velocity.normalize().scale(maximum);
        return velocity;
    }

    private static Vec3 limitArrivalVelocity(Vec3 velocity, Vec3 offset, double braking) {
        // The next tick moves before it can brake again. Reserve that entire step,
        // separately for height and horizontal alignment, while retaining heading.
        double horizontal = velocity.horizontalDistance();
        double horizontalLimit = stoppingSpeed(offset.horizontalDistance(), braking);
        double ratio = horizontal > horizontalLimit && horizontal > EPSILON ? horizontalLimit / horizontal : 1;
        double vertical = velocity.y;
        if (vertical * offset.y >= 0) {
            double verticalLimit = stoppingSpeed(Math.abs(offset.y), braking);
            vertical = Mth.clamp(vertical, -verticalLimit, verticalLimit);
        }
        // A carried impulse can exceed the ordinary acceleration budget near a
        // surface; braking here still goes through the same swept collision test.
        return new Vec3(velocity.x * ratio, vertical, velocity.z * ratio);
    }

    private static double stoppingSpeed(double distance, double braking) {
        return Math.max(0, Math.min(distance,
                Math.sqrt(2 * braking * distance + braking * braking * 0.25D) - braking * 0.5D));
    }

    private Clearance pathClearance(Vec3 from, Vec3 to, boolean allowWater) {
        if (!finite(from) || !finite(to) || from.distanceToSqr(to) > MAX_PATH * MAX_PATH) return Clearance.BLOCKED;
        AABB initial = this.boxAt(from);
        AABB total = initial.minmax(this.boxAt(to));
        if (!this.loadedBounds(total)) return Clearance.UNLOADED;
        Vec3 offset = to.subtract(from);
        int samples = Math.max(1, (int)Math.ceil(offset.length() / SAMPLE_LENGTH));
        Vec3 step = offset.scale(1.0D / samples);
        // A bird touching water may leave upwards, but cannot follow the shore down into it.
        boolean leavingWater = !allowWater && step.y > EPSILON && this.fluidKind(initial) == 1;
        AABB previous = initial;
        for (int i = 1; i <= samples; i++) {
            AABB next = initial.move(step.scale(i));
            AABB swept = previous.minmax(next);
            if (!this.bird.level().noCollision(this.bird, swept)) return Clearance.BLOCKED;
            int fluid = this.fluidKind(swept);
            if (fluid == 2 || fluid == 1 && !allowWater && !leavingWater) return Clearance.BLOCKED;
            if (this.fluidKind(next) == 0) leavingWater = false;
            previous = next;
        }
        return Clearance.CLEAR;
    }

    private AABB boxAt(Vec3 position) {
        // No downward inflation: a valid perch is only 0.005 blocks above its supporting shape.
        return this.bird.getBoundingBox().move(position.subtract(this.bird.position()));
    }

    private boolean loadedBounds(AABB box) {
        var border = this.bird.level().getWorldBorder();
        if (box.minX < border.getMinX() || box.maxX > border.getMaxX()
                || box.minZ < border.getMinZ() || box.maxZ > border.getMaxZ()
                || box.minY < this.bird.level().getMinBuildHeight() || box.maxY > this.bird.level().getMaxBuildHeight()
                || box.getXsize() > MAX_PATH + 3 || box.getYsize() > MAX_PATH + 3 || box.getZsize() > MAX_PATH + 3) return false;
        // Collision shapes may extend from adjacent blocks; check those chunks before asking the level.
        int minX = Mth.floor(box.minX - 1) >> 4, maxX = Mth.floor(box.maxX + 1) >> 4;
        int minZ = Mth.floor(box.minZ - 1) >> 4, maxZ = Mth.floor(box.maxZ + 1) >> 4;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if (this.bird.level() instanceof ServerLevel server) {
                if (server.getChunkSource().getChunkNow(x, z) == null) return false;
            } else if (!this.bird.level().hasChunk(x, z)) return false;
        }
        return true;
    }

    /** 0 = dry, 1 = water, 2 = another fluid. Called only after loadedBounds. */
    private int fluidKind(AABB box) {
        int kind = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(box.minX + EPSILON); x <= Mth.floor(box.maxX - EPSILON); x++)
            for (int y = Mth.floor(box.minY + EPSILON); y <= Mth.floor(box.maxY - EPSILON); y++)
                for (int z = Mth.floor(box.minZ + EPSILON); z <= Mth.floor(box.maxZ - EPSILON); z++) {
                    cursor.set(x, y, z);
                    FluidState fluid = this.bird.level().getFluidState(cursor);
                    if (fluid.isEmpty() || box.minY >= y + fluid.getHeight(this.bird.level(), cursor) - EPSILON) continue;
                    if (!fluid.is(FluidTags.WATER)) return 2;
                    kind = 1;
                }
        return kind;
    }

    private Result stop(Result result) {
        this.bird.setDeltaMovement(Vec3.ZERO);
        return result;
    }

    private static Vec3 heading(float yaw) {
        double radians = Math.toRadians(yaw);
        return new Vec3(-Math.sin(radians), 0, Math.cos(radians));
    }

    private static float yaw(Vec3 vector) {
        return (float)(Mth.atan2(vector.z, vector.x) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private static boolean finite(Vec3 vector) {
        return vector != null && Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }
}
