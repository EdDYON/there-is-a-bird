package EdDYON.guaniao.content.bird.nightheron;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.flock.BirdFlockManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class NightHeronLandingSelector {
    private static final int MAX_LANDING_CACHE_ENTRIES = 1024;
    private static final int MAX_COVER_CACHE_ENTRIES = 1024;
    private static final int MAX_REFRESH_STAGGER_TICKS = 40;
    private static final int LOCAL_CANDIDATE_ATTEMPTS = 64;
    private static final int LONG_DISTANCE_CANDIDATE_ATTEMPTS = 96;
    private static final Map<ServerLevel, LevelCache> LEVEL_CACHES = new WeakHashMap<>();

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
        if (!(nightHeron.level() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos origin = nightHeron.blockPosition();
        LandingKey key = new LandingKey(
                SectionPos.asLong(
                        SectionPos.blockToSectionCoord(origin.getX()),
                        SectionPos.blockToSectionCoord(origin.getY()),
                        SectionPos.blockToSectionCoord(origin.getZ())),
                minRadius,
                maxRadius,
                preferRoost);
        LevelCache levelCache = LEVEL_CACHES.computeIfAbsent(level, ignored -> new LevelCache());
        LandingEntry cached = levelCache.landings.get(key);
        long now = level.getGameTime();
        if ((cached == null || now >= cached.refreshAt) && BirdScanBudget.tryAcquire(level, nightHeron, 4)) {
            cached = new LandingEntry(buildCandidates(nightHeron, key, now), nextRefreshAt(now, key.hashCode()));
            levelCache.landings.put(key, cached);
            levelCache.trimLandings();
        }
        if (cached == null) {
            return null;
        }

        BlockPos bestPos = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int minDistanceSqr = minRadius * minRadius;
        int maxDistanceSqr = maxRadius * maxRadius;
        for (LandingCandidate candidate : cached.candidates) {
            BlockPos landingPos = candidate.pos;
            int xOffset = landingPos.getX() - origin.getX();
            int zOffset = landingPos.getZ() - origin.getZ();
            int horizontalDistanceSqr = xOffset * xOffset + zOffset * zOffset;
            if (horizontalDistanceSqr < minDistanceSqr || horizontalDistanceSqr > maxDistanceSqr
                    || !isSafeLanding(level, landingPos)) {
                continue;
            }
            double score = scoreLanding(nightHeron, candidate, threatPosition, preferRoost);
            if (score <= bestScore) {
                continue;
            }
            bestScore = score;
            bestPos = landingPos;
        }
        return bestPos;
    }

    private static List<LandingCandidate> buildCandidates(NightHeronEntity nightHeron, LandingKey key, long now) {
        ServerLevel level = (ServerLevel)nightHeron.level();
        BlockPos origin = nightHeron.blockPosition();
        long seed = key.section ^ (long)key.hashCode() * 0x9E3779B97F4A7C15L
                ^ Math.floorDiv(now, Math.max(20, BirdConfigManager.habitatCacheTicks()));
        RandomSource random = RandomSource.create(seed);
        int attempts = key.maxRadius > 64 ? LONG_DISTANCE_CANDIDATE_ATTEMPTS : LOCAL_CANDIDATE_ATTEMPTS;
        double minDistanceSqr = (double)key.minRadius * key.minRadius;
        double maxDistanceSqr = (double)key.maxRadius * key.maxRadius;
        Set<Long> sampledColumns = new HashSet<>(attempts);
        List<LandingCandidate> candidates = new ArrayList<>(attempts);
        BlockPos.MutableBlockPos column = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < attempts; ++attempt) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(minDistanceSqr + random.nextDouble() * (maxDistanceSqr - minDistanceSqr));
            int x = origin.getX() + (int)Math.round(Math.cos(angle) * distance);
            int z = origin.getZ() + (int)Math.round(Math.sin(angle) * distance);
            long columnKey = BlockPos.asLong(x, 0, z);
            if (!sampledColumns.add(columnKey)
                    || !level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                continue;
            }
            column.set(x, origin.getY(), z);
            BlockPos landingPos = findSurface(level, column, 12);
            if (landingPos == null || !isSafeLanding(level, landingPos)) {
                continue;
            }
            candidates.add(new LandingCandidate(
                    landingPos,
                    baseLandingScore(nightHeron, landingPos, key.preferRoost)));
        }
        return List.copyOf(candidates);
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

    private static double baseLandingScore(NightHeronEntity nightHeron, BlockPos pos, boolean preferRoost) {
        Level level = nightHeron.level();
        BlockState below = level.getBlockState(pos.below());
        double score = nightHeron.isNearWater(pos, 5) ? 18.0 : 0.0;
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
        }
        return score;
    }

    private static double scoreLanding(NightHeronEntity nightHeron, LandingCandidate candidate, Vec3 threatPosition, boolean preferRoost) {
        BlockPos pos = candidate.pos;
        double score = candidate.baseScore;
        if (preferRoost) {
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
        return NightHeronEntity.isWaterEdge((LevelReader)level, pos)
                || NightHeronLandingSelector.roostCoverScore(level, pos) >= 2.0
                && NightHeronEntity.isNearWater((LevelReader)level, pos, 6);
    }

    public static boolean hasRoostCoverNear(NightHeronEntity nightHeron, int radius) {
        Level level = nightHeron.level();
        BlockPos pos = nightHeron.blockPosition();
        if (!(level instanceof ServerLevel serverLevel)) {
            return scanRoostCoverNear(level, pos, radius);
        }
        CoverKey key = new CoverKey(
                SectionPos.asLong(
                        SectionPos.blockToSectionCoord(pos.getX()),
                        SectionPos.blockToSectionCoord(pos.getY()),
                        SectionPos.blockToSectionCoord(pos.getZ())),
                radius);
        LevelCache levelCache = LEVEL_CACHES.computeIfAbsent(serverLevel, ignored -> new LevelCache());
        CoverEntry cached = levelCache.cover.get(key);
        long now = serverLevel.getGameTime();
        if (cached != null && now < cached.refreshAt) {
            return cached.hasCover;
        }
        if (!BirdScanBudget.tryAcquire(serverLevel, nightHeron)) {
            return cached == null || cached.hasCover;
        }
        boolean hasCover = scanRoostCoverNear(level, pos, radius);
        levelCache.cover.put(key, new CoverEntry(hasCover, nextRefreshAt(now, key.hashCode())));
        levelCache.trimCover();
        return hasCover;
    }

    private static boolean scanRoostCoverNear(Level level, BlockPos pos, int radius) {
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
        for (int xOffset = -5; xOffset <= 5; xOffset += 2) {
            for (int zOffset = -5; zOffset <= 5; zOffset += 2) {
                if (xOffset * xOffset + zOffset * zOffset > 25) continue;
                for (int yOffset = -2; yOffset <= 5; yOffset += 2) {
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
        return BirdFlockManager.nearby(nightHeron, NightHeronEntity.class, 12.0D).stream()
                .filter(other -> other != nightHeron && !other.getBehaviorState().isAirborne()).mapToDouble(other -> {
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

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LEVEL_CACHES.remove(level);
        }
    }

    private static long nextRefreshAt(long now, int hash) {
        int cacheTicks = Math.max(80, BirdConfigManager.habitatCacheTicks());
        int stagger = Math.floorMod(hash, MAX_REFRESH_STAGGER_TICKS);
        return now + cacheTicks + stagger;
    }

    private record LandingKey(long section, int minRadius, int maxRadius, boolean preferRoost) {
    }

    private record LandingCandidate(BlockPos pos, double baseScore) {
    }

    private record LandingEntry(List<LandingCandidate> candidates, long refreshAt) {
    }

    private record CoverKey(long section, int radius) {
    }

    private record CoverEntry(boolean hasCover, long refreshAt) {
    }

    private static final class LevelCache {
        private final LinkedHashMap<LandingKey, LandingEntry> landings = new LinkedHashMap<>(64, 0.75F, true);
        private final LinkedHashMap<CoverKey, CoverEntry> cover = new LinkedHashMap<>(64, 0.75F, true);

        private void trimLandings() {
            Iterator<LandingKey> iterator = this.landings.keySet().iterator();
            while (this.landings.size() > MAX_LANDING_CACHE_ENTRIES && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }

        private void trimCover() {
            Iterator<CoverKey> iterator = this.cover.keySet().iterator();
            while (this.cover.size() > MAX_COVER_CACHE_ENTRIES && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }
}
