package EdDYON.guaniao.content.bird.kiwi;

import EdDYON.guaniao.content.bird.BirdTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class KiwiHabitatUtil {
    private static final int MAX_PATH_CANDIDATES = 12;

    private KiwiHabitatUtil() {
    }

    @Nullable
    static BlockPos findForagePatch(KiwiEntity kiwi) {
        Level level = kiwi.level();
        BlockPos origin = kiwi.blockPosition();
        int radius = KiwiDefinition.FORAGE_SEARCH_RADIUS;
        List<ScoredPos> candidates = new ArrayList<>();

        for (int x = origin.getX() - radius; x <= origin.getX() + radius; ++x) {
            for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; ++z) {
                BlockPos standPos = findStandPosition(level, origin.getY(), x, z);
                if (standPos == null
                        || !kiwi.isWithinTerritory(standPos, KiwiDefinition.MAX_TERRITORY_RADIUS)
                        || kiwi.isNearAvoidedRivalTerritory(standPos, 14)) {
                    continue;
                }
                float score = forageGroundScore(level.getBlockState(standPos.below()));
                if (score <= 0.0F) {
                    continue;
                }
                double distance = Math.sqrt(standPos.distSqr(origin));
                if (distance < 2.0D || distance > radius + 1.0D) {
                    continue;
                }
                if (level.isRainingAt(standPos)) {
                    score += 0.12F;
                }
                score -= (float) distance * 0.018F;
                score += kiwi.getRandom().nextFloat() * 0.08F;
                candidates.add(new ScoredPos(standPos, score));
            }
        }

        return firstReachable(kiwi, candidates);
    }

    @Nullable
    static BlockPos findShelter(KiwiEntity kiwi) {
        BlockPos center = kiwi.getHomeCenter() == null ? kiwi.blockPosition() : kiwi.getHomeCenter();
        Level level = kiwi.level();
        int radius = KiwiDefinition.SHELTER_SEARCH_RADIUS;
        List<ScoredPos> candidates = new ArrayList<>();

        // Roost discovery is infrequent, but each shelter score samples nearby cover.
        // A two-block grid keeps the same useful search area without a large scan spike.
        for (int x = center.getX() - radius; x <= center.getX() + radius; x += 2) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z += 2) {
                BlockPos standPos = findStandPosition(level, center.getY(), x, z);
                if (standPos == null
                        || !kiwi.isWithinTerritory(standPos, KiwiDefinition.MAX_TERRITORY_RADIUS)
                        || kiwi.isNearAvoidedRivalTerritory(standPos, 14)) {
                    continue;
                }
                float score = shelterScore(kiwi, standPos);
                if (score >= 0.58F) {
                    candidates.add(new ScoredPos(standPos, score));
                }
            }
        }

        return firstReachable(kiwi, candidates);
    }

    static boolean isValidRoost(KiwiEntity kiwi, @Nullable BlockPos pos) {
        return pos != null
                && kiwi.level().hasChunkAt(pos)
                && kiwi.isWithinTerritory(pos, KiwiDefinition.MAX_TERRITORY_RADIUS)
                && isWalkable(kiwi.level(), pos)
                && !hasNearbyFire(kiwi.level(), pos)
                && shelterScore(kiwi, pos) >= 0.52F;
    }

    @Nullable
    static Vec3 findEscapeTarget(KiwiEntity kiwi, Vec3 threatPosition) {
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double currentThreatDistance = kiwi.position().distanceTo(threatPosition);

        for (int attempt = 0; attempt < 24; ++attempt) {
            Vec3 candidate = DefaultRandomPos.getPosAway(kiwi, 12, 4, threatPosition);
            if (candidate == null) {
                continue;
            }
            BlockPos standPos = BlockPos.containing(candidate);
            if (!kiwi.isWithinTerritory(standPos, KiwiDefinition.MAX_TERRITORY_RADIUS + 8)
                    || !isWalkable(kiwi.level(), standPos)) {
                continue;
            }
            double score = candidate.distanceTo(threatPosition) - currentThreatDistance;
            score += shelterScore(kiwi, standPos) * 5.0D;
            if (score > bestScore && kiwi.getNavigation().createPath(standPos, 0) != null) {
                bestScore = score;
                best = Vec3.atBottomCenterOf(standPos);
            }
        }
        return best;
    }

    @Nullable
    static Vec3 findTerritoryWanderTarget(KiwiEntity kiwi) {
        BlockPos home = kiwi.getHomeCenter();
        if (home == null) {
            return LandRandomPos.getPos(kiwi, 8, 3);
        }

        double currentHomeDistance = Math.sqrt(home.distSqr(kiwi.blockPosition()));
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int attempt = 0; attempt < 16; ++attempt) {
            Vec3 candidate = LandRandomPos.getPos(kiwi, 10, 4);
            if (candidate == null) {
                continue;
            }
            BlockPos candidatePos = BlockPos.containing(candidate);
            if (!kiwi.isWithinTerritory(candidatePos, KiwiDefinition.MAX_TERRITORY_RADIUS)) {
                continue;
            }
            if (kiwi.isNearAvoidedRivalTerritory(candidatePos, 14)) {
                continue;
            }
            double homeDistance = Math.sqrt(home.distSqr(candidatePos));
            double score = kiwi.getRandom().nextDouble();
            if (currentHomeDistance > KiwiDefinition.NORMAL_TERRITORY_RADIUS) {
                score += (currentHomeDistance - homeDistance) * 0.35D;
            }
            score += shelterScore(kiwi, candidatePos) * 0.35D;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    @Nullable
    static Vec3 findReturnTarget(KiwiEntity kiwi) {
        BlockPos preferred = isValidRoost(kiwi, kiwi.getRoostPos())
                ? kiwi.getRoostPos()
                : kiwi.getHomeCenter();
        if (preferred == null) {
            return null;
        }
        if (isWalkable(kiwi.level(), preferred) && kiwi.getNavigation().createPath(preferred, 0) != null) {
            return Vec3.atBottomCenterOf(preferred);
        }

        for (int radius = 1; radius <= 5; ++radius) {
            for (int x = -radius; x <= radius; ++x) {
                for (int z = -radius; z <= radius; ++z) {
                    BlockPos standPos = findStandPosition(kiwi.level(), preferred.getY(),
                            preferred.getX() + x, preferred.getZ() + z);
                    if (standPos != null && kiwi.getNavigation().createPath(standPos, 0) != null) {
                        return Vec3.atBottomCenterOf(standPos);
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    static Vec3 findSpacingTarget(KiwiEntity kiwi, Vec3 crowdCenter) {
        return DefaultRandomPos.getPosAway(kiwi, 7, 3, crowdCenter);
    }

    static float forageGroundScore(BlockState state) {
        if (!state.is(BirdTags.KIWI_FORAGE_GROUND)) {
            return 0.0F;
        }
        if (state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.PODZOL)) {
            return 1.0F;
        }
        if (state.is(Blocks.ROOTED_DIRT)) {
            return 0.95F;
        }
        if (state.is(Blocks.MUD)) {
            return 0.90F;
        }
        if (state.is(Blocks.GRASS_BLOCK)) {
            return 0.85F;
        }
        if (state.is(Blocks.DIRT)) {
            return 0.75F;
        }
        if (state.is(Blocks.COARSE_DIRT)) {
            return 0.45F;
        }
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
            return 0.15F;
        }
        return 0.25F;
    }

    private static float shelterScore(KiwiEntity kiwi, BlockPos standPos) {
        Level level = kiwi.level();
        if (!isWalkable(level, standPos)) {
            return 0.0F;
        }
        if (hasNearbyFire(level, standPos)) {
            return 0.0F;
        }

        float score = level.canSeeSky(standPos.above()) ? 0.0F : 0.42F;
        int coverBlocks = 0;
        for (BlockPos pos : BlockPos.betweenClosed(standPos.offset(-2, 0, -2), standPos.offset(2, 3, 2))) {
            if (level.hasChunkAt(pos) && isCover(level.getBlockState(pos))) {
                ++coverBlocks;
            }
        }
        score += Math.min(0.38F, coverBlocks * 0.0475F);
        score += Math.max(0.0F, 0.18F - level.getMaxLocalRawBrightness(standPos) / 15.0F * 0.18F);
        score += Math.min(0.14F, forageGroundScore(level.getBlockState(standPos.below())) * 0.14F);
        if (kiwi.birdBrain().senses().nearestPlayer() != null
                && standPos.closerToCenterThan(kiwi.birdBrain().senses().nearestPlayer().position(), 8.0D)) {
            score -= 0.22F;
        }
        return score;
    }

    @Nullable
    private static BlockPos firstReachable(KiwiEntity kiwi, List<ScoredPos> candidates) {
        candidates.sort(Comparator.comparingDouble(ScoredPos::score).reversed());
        int tested = 0;
        for (ScoredPos candidate : candidates) {
            if (++tested > MAX_PATH_CANDIDATES) {
                break;
            }
            if (kiwi.getNavigation().createPath(candidate.pos(), 0) != null) {
                return candidate.pos();
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos findStandPosition(Level level, int originY, int x, int z) {
        for (int y = originY + 4; y >= originY - 5; --y) {
            BlockPos standPos = new BlockPos(x, y, z);
            if (level.hasChunkAt(standPos) && isWalkable(level, standPos)) {
                return standPos;
            }
        }
        return null;
    }

    private static boolean isWalkable(Level level, BlockPos standPos) {
        if (!level.getFluidState(standPos).isEmpty()
                || !level.getFluidState(standPos.above()).isEmpty()) {
            return false;
        }
        BlockState feet = level.getBlockState(standPos);
        BlockState head = level.getBlockState(standPos.above());
        BlockState ground = level.getBlockState(standPos.below());
        return feet.getCollisionShape(level, standPos).isEmpty()
                && head.getCollisionShape(level, standPos.above()).isEmpty()
                && !ground.getCollisionShape(level, standPos.below()).isEmpty();
    }

    private static boolean isCover(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.GRASS)
                || state.is(Blocks.TALL_GRASS);
    }

    private static boolean hasNearbyFire(Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 3, 3))) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(BlockTags.FIRE)) {
                return true;
            }
        }
        return false;
    }

    private record ScoredPos(BlockPos pos, float score) {
    }
}
