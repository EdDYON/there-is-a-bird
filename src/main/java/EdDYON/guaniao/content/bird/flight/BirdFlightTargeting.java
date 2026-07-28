package EdDYON.guaniao.content.bird.flight;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.registry.GuaniaoBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BirdFlightTargeting {
    private static final double FORWARD_CONE_RADIANS = Math.toRadians(15.0D);
    private static final double FORWARD_FALLBACK_RADIANS = 0.62D;
    private static final double WIDE_FALLBACK_RADIANS = 1.35D;
    private static final double FLIGHT_PATH_SAMPLE_SPACING = 0.75D;
    private static final int MAX_AIR_TARGET_ATTEMPTS = 8;
    private static final int MAX_RECOVERY_PATH_CHECKS = 8;
    private static final int MAX_HORIZONTAL_RECOVERY_PATH_CHECKS = 6;

    private BirdFlightTargeting() {
    }

    public static Vec3 randomHorizontalDirection(RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
    }

    public static Vec3 normalizeHorizontal(Vec3 vector, Vec3 fallback) {
        Vec3 horizontal = vector.multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() <= 1.0E-4D) {
            horizontal = fallback.multiply(1.0D, 0.0D, 1.0D);
        }
        if (horizontal.lengthSqr() <= 1.0E-4D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        return horizontal.normalize();
    }

    public static Vec3 rotateHorizontal(Vec3 direction, double angle) {
        Vec3 normalized = normalizeHorizontal(direction, new Vec3(1.0D, 0.0D, 0.0D));
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(normalized.x * cos - normalized.z * sin, 0.0D, normalized.x * sin + normalized.z * cos).normalize();
    }

    public static Vec3 findAirTarget(PathfinderMob bird, BirdFlightProfile profile, Vec3 preferredDirection, boolean fleeing) {
        Vec3 viewDirection = normalizeHorizontal(bird.getViewVector(1.0F), bird.getLookAngle());
        Vec3 preferred = normalizeHorizontal(preferredDirection, bird.getDeltaMovement());
        Vec3 baseDirection = fleeing ? preferred : normalizeHorizontal(viewDirection.scale(0.78D).add(preferred.scale(0.22D)), viewDirection);
        RandomSource random = bird.getRandom();
        for (int attempt = 0; attempt < MAX_AIR_TARGET_ATTEMPTS; ++attempt) {
            double turnLimit = fleeing
                    ? (attempt < 5 ? 0.78D : 1.12D)
                    : (attempt < 4 ? FORWARD_CONE_RADIANS : (attempt < 7 ? FORWARD_FALLBACK_RADIANS : WIDE_FALLBACK_RADIANS));
            Vec3 direction = rotateHorizontal(baseDirection, randomSigned(random, turnLimit));
            double distance = profile.minAirTargetDistance() + Math.sqrt(random.nextDouble()) * (profile.maxAirTargetDistance() - profile.minAirTargetDistance());
            Vec3 horizontalTarget = bird.position().add(direction.scale(distance));
            int blockX = Mth.floor(horizontalTarget.x);
            int blockZ = Mth.floor(horizontalTarget.z);
            Level level = bird.level();
            if (!level.hasChunk(blockX >> 4, blockZ >> 4)) {
                continue;
            }
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
            if (surfaceY <= level.getMinBuildHeight() || surfaceY >= level.getMaxBuildHeight() - 6) {
                continue;
            }
            double targetY = surfaceY + profile.minCruiseHeight() + random.nextDouble() * Math.max(0.0D, profile.maxCruiseHeight() - profile.minCruiseHeight());
            targetY = Mth.clamp(targetY, bird.getY() - profile.maxVerticalStep(), bird.getY() + profile.maxVerticalStep());
            targetY = Mth.clamp(targetY, level.getMinBuildHeight() + 3.0D, level.getMaxBuildHeight() - 3.0D);
            BlockPos airPos = BlockPos.containing(horizontalTarget.x, targetY, horizontalTarget.z);
            Vec3 airTarget = new Vec3(blockX + 0.5D, targetY, blockZ + 0.5D);
            // Flight safety is correctness-critical and must not compete with optional habitat/perception scans.
            // The local attempt caps bound the work without turning a depleted shared budget into a failed escape.
            if (isOpenAir(bird, airPos) && hasClearFlightPath(bird, airTarget)) {
                return airTarget;
            }
        }
        return null;
    }

    /**
     * Checks the entire direct flight corridor, rather than validating only the
     * destination block. This keeps birds from selecting air beyond walls,
     * cave ceilings, or closed doors.
     */
    public static boolean hasClearFlightPath(Entity bird, Vec3 target) {
        Vec3 start = bird.position();
        Vec3 path = target.subtract(start);
        double distance = path.length();
        if (distance <= 1.0E-4D) {
            return true;
        }
        int samples = Math.max(1, Mth.ceil(distance / FLIGHT_PATH_SAMPLE_SPACING));
        for (int sample = 1; sample <= samples; ++sample) {
            Vec3 point = start.add(path.scale((double)sample / (double)samples));
            if (!isOpenAir(bird, BlockPos.containing(point))) {
                return false;
            }
        }
        return true;
    }

    /** Chooses a short, visible climb or reverse corridor after blocked flight. */
    public static Vec3 findRecoveryTarget(PathfinderMob bird, Vec3 blockedDirection, int maxClimb, int maxHorizontalDistance) {
        Vec3 reverse = normalizeHorizontal(blockedDirection.scale(-1.0D), bird.getDeltaMovement());
        double[] angles = {0.0D, 0.45D, -0.45D, 0.9D, -0.9D, 1.35D, -1.35D};
        int pathChecks = 0;
        horizontalSearch:
        for (double angle : angles) {
            Vec3 direction = rotateHorizontal(reverse, angle);
            for (int distance = Math.max(3, maxHorizontalDistance); distance >= 3; --distance) {
                Vec3 point = bird.position().add(direction.scale(distance));
                BlockPos airPos = BlockPos.containing(point);
                Vec3 target = Vec3.atBottomCenterOf(airPos);
                if (isOpenAir(bird, airPos)) {
                    if (pathChecks >= MAX_HORIZONTAL_RECOVERY_PATH_CHECKS) {
                        break horizontalSearch;
                    }
                    ++pathChecks;
                    if (hasClearFlightPath(bird, target)) {
                        return target;
                    }
                }
            }
        }

        int blockX = Mth.floor(bird.getX());
        int blockZ = Mth.floor(bird.getZ());
        for (int climb = Math.max(2, maxClimb); climb >= 2 && pathChecks < MAX_RECOVERY_PATH_CHECKS; --climb) {
            BlockPos airPos = new BlockPos(blockX, Mth.floor(bird.getY() + (double)climb), blockZ);
            Vec3 target = Vec3.atBottomCenterOf(airPos);
            if (isOpenAir(bird, airPos)) {
                ++pathChecks;
                if (hasClearFlightPath(bird, target)) {
                    return target;
                }
            }
        }
        return null;
    }

    public static Vec3 findLandingInDirection(PathfinderMob bird, Vec3 direction, int minRadius, int maxRadius, int horizontalRange, int verticalRange) {
        Vec3 horizontal = normalizeHorizontal(direction, bird.getLookAngle());
        RandomSource random = bird.getRandom();
        for (int attempt = 0; attempt < 18; ++attempt) {
            double radius = minRadius + random.nextDouble() * (double)Math.max(1, maxRadius - minRadius);
            Vec3 rotated = rotateHorizontal(horizontal, randomSigned(random, 0.85D));
            BlockPos center = BlockPos.containing(bird.position().add(rotated.scale(radius)).add(0.0D, 3.5D, 0.0D));
            Vec3 landing = findDryLandingTargetNear(bird, center, horizontalRange, verticalRange);
            if (landing != null) {
                return landing;
            }
        }
        return null;
    }

    public static Vec3 findNearestDryLandingTarget(PathfinderMob bird, int radius, int verticalRange) {
        BlockPos origin = bird.blockPosition();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int r = 2; r <= radius; ++r) {
            for (int xOffset = -r; xOffset <= r; ++xOffset) {
                for (int zOffset = -r; zOffset <= r; ++zOffset) {
                    if (Math.abs(xOffset) != r && Math.abs(zOffset) != r) {
                        continue;
                    }
                    mutable.set(origin.getX() + xOffset, origin.getY(), origin.getZ() + zOffset);
                    Vec3 landing = findDryLandingTarget(bird, mutable, verticalRange);
                    if (landing != null) {
                        return landing;
                    }
                }
            }
        }
        return null;
    }

    public static Vec3 findDryLandingTargetNear(PathfinderMob bird, BlockPos center, int horizontalRange, int verticalRange) {
        Vec3 direct = findDryLandingTarget(bird, center, verticalRange);
        if (direct != null) {
            return direct;
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int radius = 1; radius <= horizontalRange; ++radius) {
            for (int xOffset = -radius; xOffset <= radius; ++xOffset) {
                for (int zOffset = -radius; zOffset <= radius; ++zOffset) {
                    if (Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                        continue;
                    }
                    mutable.set(center.getX() + xOffset, center.getY(), center.getZ() + zOffset);
                    Vec3 landing = findDryLandingTarget(bird, mutable, verticalRange);
                    if (landing != null) {
                        return landing;
                    }
                }
            }
        }
        return null;
    }

    public static Vec3 findDryLandingTarget(PathfinderMob bird, BlockPos center, int verticalRange) {
        BlockPos landing = findDryLandingSurface(bird, center, verticalRange);
        return landing == null ? null : Vec3.atBottomCenterOf(landing).add(0.0D, 0.05D, 0.0D);
    }

    public static BlockPos findDryLandingSurface(PathfinderMob bird, BlockPos center, int verticalRange) {
        if (!bird.level().hasChunk(center.getX() >> 4, center.getZ() >> 4)) {
            return null;
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int yOffset = verticalRange; yOffset >= -verticalRange; --yOffset) {
            mutable.set(center.getX(), center.getY() + yOffset, center.getZ());
            if (isSafeDryLanding(bird, mutable)) {
                return mutable.immutable();
            }
        }
        return null;
    }

    public static boolean isOpenAir(Entity entity, BlockPos pos) {
        Level level = entity.level();
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }
        if (pos.getY() <= level.getMinBuildHeight() + 1 || pos.getY() >= level.getMaxBuildHeight() - 1) {
            return false;
        }
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        if (hasBlockingFlightCollision(entity, level, pos, feet)
                || hasBlockingFlightCollision(entity, level, pos.above(), head)) {
            return false;
        }
        AABB box = entity.getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(entity.position())).inflate(-0.04D, -0.02D, -0.04D);
        return level.noCollision(entity, box)
                && !level.getFluidState(pos).is(FluidTags.WATER)
                && !level.getFluidState(pos).is(FluidTags.LAVA)
                && !level.getFluidState(pos.above()).is(FluidTags.WATER)
                && !level.getFluidState(pos.above()).is(FluidTags.LAVA);
    }

    private static boolean hasBlockingFlightCollision(Entity entity, Level level, BlockPos pos, BlockState state) {
        if (state.is(BlockTags.LEAVES)
                && BirdConfigManager.birdsPassThroughLeaves()
                && entity instanceof BirdFlightAware bird
                && (bird.isBirdFlightActive() || bird.shouldPassThroughLeaves())) {
            return false;
        }
        return !state.getCollisionShape((BlockGetter)level, pos).isEmpty();
    }

    public static boolean isSafeDryLanding(PathfinderMob bird, BlockPos pos) {
        Level level = bird.level();
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        if (!feet.getCollisionShape((BlockGetter)level, pos).isEmpty() || !head.getCollisionShape((BlockGetter)level, pos.above()).isEmpty()) {
            return false;
        }
        if (level.getFluidState(pos).is(FluidTags.WATER)
                || level.getFluidState(pos).is(FluidTags.LAVA)
                || level.getFluidState(pos.below()).is(FluidTags.WATER)
                || level.getFluidState(pos.below()).is(FluidTags.LAVA)) {
            return false;
        }
        if (below.isAir() || below.is(Blocks.CACTUS) || below.is(Blocks.MAGMA_BLOCK)) {
            return false;
        }
        return below.isFaceSturdy((BlockGetter)level, pos.below(), Direction.UP)
                || below.is(GuaniaoBlockTags.BIRD_PERCHES)
                || below.is(BlockTags.LEAVES)
                || below.is(BlockTags.LOGS)
                || below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(Blocks.FARMLAND)
                || below.is(Blocks.HAY_BLOCK)
                || below.is(Blocks.COMPOSTER)
                || below.getBlock() instanceof FenceBlock
                || below.getBlock() instanceof FenceGateBlock;
    }

    private static double randomSigned(RandomSource random, double value) {
        return (random.nextDouble() * 2.0D - 1.0D) * value;
    }
}
