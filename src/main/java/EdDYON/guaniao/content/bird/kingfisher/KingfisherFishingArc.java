package EdDYON.guaniao.content.bird.kingfisher;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/** A short dip through the water surface, measured at the bird's feet. */
public final class KingfisherFishingArc {
    private static final int DURATION_TICKS = 14;
    private static final double SPAN = 2.8D;
    private static final double EDGE_HEIGHT = 0.60D;
    private static final double DEPTH = 0.20D;
    private static final int CRUISE_TICKS = 28;
    private static final double CRUISE_LENGTH = 4.8D;
    private final Vec3 surfaceCenter;
    private final Vec3 along;

    private KingfisherFishingArc(Vec3 surfaceCenter, Vec3 along) {
        this.surfaceCenter = surfaceCenter;
        this.along = along;
    }

    /** The supplied center uses the actual fluid height, not the top of its block. */
    @Nullable public static KingfisherFishingArc create(Vec3 surfaceCenter, Vec3 direction) {
        if (!finite(surfaceCenter) || !finite(direction)) return null;
        Vec3 horizontal = direction.multiply(1, 0, 1);
        if (horizontal.lengthSqr() < 1.0E-6D) return null;
        return new KingfisherFishingArc(surfaceCenter, horizontal.normalize());
    }

    public int durationTicks() { return DURATION_TICKS; }
    public double surfaceY() { return this.surfaceCenter.y; }
    public Vec3 start() { return this.pointAt(0); }
    public Vec3 end() { return this.pointAt(1); }
    public int cruiseTicks() { return CRUISE_TICKS; }
    public Vec3 cruiseStart() { return this.cruisePointAt(0); }
    public Vec3 approachStart() { return this.cruiseStart().subtract(this.along.scale(1.8D)); }
    public Vec3 cruiseDirection() { return this.along; }

    public Vec3 cruisePointAt(double progress) {
        double t = Double.isFinite(progress) ? Mth.clamp(progress, 0, 1) : 0;
        return this.start().subtract(this.along.scale(CRUISE_LENGTH * (1 - t)));
    }

    /** A straight, dry run over open water before the short submerged dip. */
    public boolean isCruiseClear(KingfisherEntity bird, KingfisherFlightMotor motor) {
        if (!motor.clearPath(this.approachStart(), this.start(), false)) return false;
        for (int tick = 0; tick <= CRUISE_TICKS; tick += 2)
            if (!this.hasSurfaceAt(bird, this.cruisePointAt(tick / (double)CRUISE_TICKS))) return false;
        return true;
    }

    public Vec3 pointAt(double progress) {
        double t = Double.isFinite(progress) ? Mth.clamp(progress, 0, 1) : 0;
        double height = EDGE_HEIGHT - 4 * (EDGE_HEIGHT + DEPTH) * t * (1 - t);
        return this.surfaceCenter.add(this.along.scale(SPAN * (t - 0.5D))).add(0, height, 0);
    }

    /** Checks all actual movement segments, including the submerged part, without loading chunks. */
    public boolean isClear(KingfisherEntity bird, KingfisherFlightMotor motor) {
        Vec3 previous = this.start();
        // The motor verifies loaded bounds before any block/fluid reads below.
        if (!motor.clearPath(previous, previous, true) || !this.hasSurfaceAt(bird, previous)) return false;
        for (int tick = 1; tick <= DURATION_TICKS; tick++) {
            Vec3 next = this.pointAt(tick / (double)DURATION_TICKS);
            if (!motor.clearPath(previous, next, true) || !this.hasSurfaceAt(bird, next)) return false;
            previous = next;
        }
        return true;
    }

    /** Cheap live check for a fishing step; drained or unloaded water cancels the meal. */
    public boolean hasSurfaceAt(KingfisherEntity bird, Vec3 point) {
        if (!finite(point)) return false;
        BlockPos water = BlockPos.containing(point.x, this.surfaceCenter.y - 1.0E-4D, point.z);
        if (bird.level().isOutsideBuildHeight(water) || bird.level().isOutsideBuildHeight(water.above())) return false;
        int chunkX = water.getX() >> 4, chunkZ = water.getZ() >> 4;
        if (bird.level() instanceof ServerLevel server) {
            if (server.getChunkSource().getChunkNow(chunkX, chunkZ) == null) return false;
        } else if (!bird.level().hasChunk(chunkX, chunkZ)) return false;
        BlockState state = bird.level().getBlockState(water);
        FluidState fluid = state.getFluidState();
        return state.is(Blocks.WATER) && fluid.is(FluidTags.WATER) && fluid.isSource()
                && bird.level().getFluidState(water.above()).isEmpty()
                && Math.abs(water.getY() + fluid.getHeight(bird.level(), water) - this.surfaceCenter.y) < 0.025D;
    }

    private static boolean finite(Vec3 vector) {
        return vector != null && Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }
}
