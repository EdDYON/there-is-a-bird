package EdDYON.guaniao.content.bird.nightheron;

import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class NightHeronLandingSelector {
    private NightHeronLandingSelector() {
    }

    public static BlockPos findEscapeLanding(NightHeronEntity nightHeron, Vec3 threatPosition, int minRadius, int maxRadius) {
        return NightHeronLandingSelector.findBestLanding(nightHeron, threatPosition, minRadius, maxRadius, false);
    }

    public static BlockPos findTransitLanding(NightHeronEntity nightHeron, int minRadius, int maxRadius) {
        return NightHeronLandingSelector.findBestLanding(nightHeron, null, minRadius, maxRadius, !nightHeron.isActiveTime());
    }

    public static BlockPos findRoostLanding(NightHeronEntity nightHeron, int minRadius, int maxRadius) {
        return NightHeronLandingSelector.findBestLanding(nightHeron, null, minRadius, maxRadius, true);
    }

    public static Vec3 directionTo(BlockPos target, NightHeronEntity nightHeron) {
        Vec3 direction = Vec3.atCenterOf((Vec3i)target).subtract(nightHeron.position());
        Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
        if (horizontal.lengthSqr() <= 1.0E-4) {
            return nightHeron.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        }
        return horizontal.normalize();
    }

    private static BlockPos findBestLanding(NightHeronEntity nightHeron, Vec3 threatPosition, int minRadius, int maxRadius, boolean preferRoost) {
        Level level = nightHeron.level();
        BlockPos origin = nightHeron.blockPosition();
        BlockPos bestPos = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int horizontalStep = maxRadius > 64 ? 2 : 1;
        for (int xOffset = -maxRadius; xOffset <= maxRadius; xOffset += horizontalStep) {
            for (int zOffset = -maxRadius; zOffset <= maxRadius; zOffset += horizontalStep) {
                double score;
                BlockPos landingPos;
                int horizontalDistanceSqr = xOffset * xOffset + zOffset * zOffset;
                if (horizontalDistanceSqr < minRadius * minRadius || horizontalDistanceSqr > maxRadius * maxRadius) continue;
                mutablePos.set(origin.getX() + xOffset, origin.getY(), origin.getZ() + zOffset);
                if (!level.hasChunk(SectionPos.blockToSectionCoord((int)mutablePos.getX()), SectionPos.blockToSectionCoord((int)mutablePos.getZ())) || (landingPos = NightHeronLandingSelector.findSurface(level, (BlockPos)mutablePos, 12)) == null || !NightHeronLandingSelector.isSafeLanding(level, landingPos) || !((score = NightHeronLandingSelector.scoreLanding(nightHeron, landingPos, threatPosition, preferRoost)) > bestScore)) continue;
                bestScore = score;
                bestPos = landingPos.immutable();
            }
        }
        return bestPos;
    }

    private static BlockPos findSurface(Level level, BlockPos center, int verticalRange) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int yOffset = verticalRange; yOffset >= -verticalRange; --yOffset) {
            mutablePos.set(center.getX(), center.getY() + yOffset, center.getZ());
            if (!NightHeronLandingSelector.isSafeLanding(level, (BlockPos)mutablePos)) continue;
            return mutablePos.immutable();
        }
        return null;
    }

    private static boolean isSafeLanding(Level level, BlockPos pos) {
        if (!NightHeronEntity.canReadChunk((LevelReader)level, pos)) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        if (!feet.getCollisionShape((BlockGetter)level, pos).isEmpty() || !head.getCollisionShape((BlockGetter)level, pos.above()).isEmpty()) {
            return false;
        }
        if (level.getFluidState(pos).is(FluidTags.WATER) || level.getFluidState(pos).is(FluidTags.LAVA)) {
            return false;
        }
        if (below.is(Blocks.CACTUS) || below.is(Blocks.MAGMA_BLOCK)) {
            return false;
        }
        return below.isFaceSturdy((BlockGetter)level, pos.below(), Direction.UP) || below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS);
    }

    private static double scoreLanding(NightHeronEntity nightHeron, BlockPos pos, Vec3 threatPosition, boolean preferRoost) {
        double score;
        Level level = nightHeron.level();
        BlockState below = level.getBlockState(pos.below());
        double d = score = nightHeron.isNearWater(pos, 5) ? 18.0 : 0.0;
        if (NightHeronEntity.isWaterEdge((LevelReader)level, pos)) {
            score += 16.0;
        }
        if (below.is(Blocks.MUD) || below.is(Blocks.CLAY) || below.is(Blocks.SAND) || below.is(Blocks.RED_SAND)) {
            score += 5.0;
        }
        if (below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS)) {
            score += preferRoost ? 24.0 : 8.0;
        }
        if (preferRoost) {
            score += NightHeronLandingSelector.roostCoverScore(level, pos) * 5.0;
            score += NightHeronLandingSelector.nearbyRoostingNightHeronScore(nightHeron, pos);
        }
        if (threatPosition != null) {
            score += Math.min(28.0, Vec3.atCenterOf((Vec3i)pos).distanceTo(threatPosition) * 0.45);
        }
        return score -= Math.abs((double)pos.getY() - nightHeron.getY()) * 0.25;
    }

    public static boolean isRoostingSpot(Level level, BlockPos pos) {
        if (!NightHeronEntity.canReadChunk((LevelReader)level, pos)) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        if (below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS)) {
            return true;
        }
        return NightHeronLandingSelector.roostCoverScore(level, pos) >= 2.0 && (NightHeronEntity.isWaterEdge((LevelReader)level, pos) || NightHeronEntity.isNearWater((LevelReader)level, pos, 6));
    }

    public static boolean hasRoostCoverNear(Level level, BlockPos pos, int radius) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int xOffset = -radius; xOffset <= radius; ++xOffset) {
            for (int zOffset = -radius; zOffset <= radius; ++zOffset) {
                if (xOffset * xOffset + zOffset * zOffset > radius * radius) continue;
                for (int yOffset = -2; yOffset <= 4; ++yOffset) {
                    mutablePos.set(pos.getX() + xOffset, pos.getY() + yOffset, pos.getZ() + zOffset);
                    if (!NightHeronEntity.canReadChunk((LevelReader)level, (BlockPos)mutablePos) || !NightHeronLandingSelector.isRoostCoverBlock(level.getBlockState((BlockPos)mutablePos))) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private static double roostCoverScore(Level level, BlockPos pos) {
        double score = 0.0;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int xOffset = -5; xOffset <= 5; ++xOffset) {
            for (int zOffset = -5; zOffset <= 5; ++zOffset) {
                if (xOffset * xOffset + zOffset * zOffset > 25) continue;
                for (int yOffset = -2; yOffset <= 5; ++yOffset) {
                    mutablePos.set(pos.getX() + xOffset, pos.getY() + yOffset, pos.getZ() + zOffset);
                    if (!NightHeronEntity.canReadChunk((LevelReader)level, (BlockPos)mutablePos)) continue;
                    BlockState state = level.getBlockState((BlockPos)mutablePos);
                    if (state.is(BlockTags.LEAVES)) {
                        score += 0.9;
                        continue;
                    }
                    if (state.is(BlockTags.LOGS)) {
                        score += 0.65;
                        continue;
                    }
                    if (!NightHeronLandingSelector.isRoostCoverBlock(state)) continue;
                    score += 0.45;
                }
            }
        }
        return Math.min(score, 8.0);
    }

    private static boolean isRoostCoverBlock(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.VINE) || state.is(Blocks.BAMBOO) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.FERN);
    }

    private static double nearbyRoostingNightHeronScore(NightHeronEntity nightHeron, BlockPos pos) {
        return nightHeron.level().getEntitiesOfClass(NightHeronEntity.class, nightHeron.getBoundingBox().inflate(12.0), other -> other != nightHeron && other.isAlive() && !other.getBehaviorState().isAirborne()).stream().mapToDouble(other -> {
            double distance = Vec3.atCenterOf((Vec3i)pos).distanceTo(other.position());
            if (distance < 2.25) {
                return -6.0;
            }
            if (distance <= 7.0) {
                return 9.0;
            }
            return Math.max(0.0, 7.0 - distance * 0.35);
        }).sum();
    }
}

