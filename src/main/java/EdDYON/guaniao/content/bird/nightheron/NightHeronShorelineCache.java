package EdDYON.guaniao.content.bird.nightheron;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shares heightmap-assisted shoreline probes between night herons in the same section. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class NightHeronShorelineCache {
    private static final int MAX_ENTRIES_PER_LEVEL = 2048;
    private static final int MAX_CANDIDATES = 24;
    private static final int SCAN_BUDGET_COST = 3;
    private static final Map<ServerLevel, LevelCache> LEVEL_CACHES = new WeakHashMap<>();

    private NightHeronShorelineCache() {
    }

    @Nullable
    public static BlockPos find(NightHeronEntity nightHeron) {
        if (!(nightHeron.level() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos origin = nightHeron.blockPosition();
        CacheKey key = new CacheKey(SectionPos.asLong(
                SectionPos.blockToSectionCoord(origin.getX()),
                SectionPos.blockToSectionCoord(origin.getY()),
                SectionPos.blockToSectionCoord(origin.getZ())));
        LevelCache levelCache = LEVEL_CACHES.computeIfAbsent(level, ignored -> new LevelCache());
        CacheEntry cached = levelCache.entries.get(key);
        long now = level.getGameTime();
        if (cached == null || now >= cached.refreshAt) {
            if (BirdScanBudget.tryAcquire(level, nightHeron, SCAN_BUDGET_COST)) {
                cached = new CacheEntry(scan(level, origin), nextRefreshAt(now, key));
                levelCache.entries.put(key, cached);
                levelCache.trim();
            } else if (cached == null) {
                return null;
            }
        }
        return selectNearestValid(level, origin, cached.positions);
    }

    static boolean isSafeShorelinePosition(Level level, BlockPos pos) {
        if (!NightHeronEntity.canReadChunk(level, pos)
                || !NightHeronEntity.isWaterEdge(level, pos)
                || !level.getFluidState(pos).isEmpty()
                || !level.getFluidState(pos.above()).isEmpty()) {
            return false;
        }
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        if (!feet.getCollisionShape((BlockGetter)level, pos).isEmpty()
                || !head.getCollisionShape((BlockGetter)level, pos.above()).isEmpty()) {
            return false;
        }
        return below.isFaceSturdy((BlockGetter)level, pos.below(), Direction.UP)
                || isPreferredShoreBlock(below);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LEVEL_CACHES.remove(level);
        }
    }

    private static List<BlockPos> scan(ServerLevel level, BlockPos origin) {
        int radius = NightHeronDefinition.WATER_SEEK_SCAN_RADIUS;
        List<ScoredPosition> scored = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int xOffset = -radius; xOffset <= radius; ++xOffset) {
            for (int zOffset = -radius; zOffset <= radius; ++zOffset) {
                int horizontalDistanceSqr = xOffset * xOffset + zOffset * zOffset;
                if (horizontalDistanceSqr > radius * radius) {
                    continue;
                }
                int x = origin.getX() + xOffset;
                int z = origin.getZ() + zOffset;
                if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                    continue;
                }
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(surfaceY - origin.getY()) > 5) {
                    continue;
                }
                mutable.set(x, surfaceY, z);
                if (!isSafeShorelinePosition(level, mutable)) {
                    continue;
                }
                BlockState below = level.getBlockState(mutable.below());
                double score = -horizontalDistanceSqr - Math.abs(surfaceY - origin.getY()) * 3.0D;
                if (isPreferredShoreBlock(below)) {
                    score += 10.0D;
                }
                scored.add(new ScoredPosition(mutable.immutable(), score));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredPosition::score).reversed());
        return scored.stream().limit(MAX_CANDIDATES).map(ScoredPosition::pos).toList();
    }

    @Nullable
    private static BlockPos selectNearestValid(ServerLevel level, BlockPos origin, List<BlockPos> candidates) {
        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (BlockPos candidate : candidates) {
            if (!isSafeShorelinePosition(level, candidate)) {
                continue;
            }
            double score = -candidate.distSqr(origin);
            if (isPreferredShoreBlock(level.getBlockState(candidate.below()))) {
                score += 10.0D;
            }
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean isPreferredShoreBlock(BlockState state) {
        return state.is(Blocks.MUD) || state.is(Blocks.CLAY)
                || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND);
    }

    private static long nextRefreshAt(long now, CacheKey key) {
        int cacheTicks = BirdConfigManager.habitatCacheTicks();
        long mixed = key.section ^ key.section >>> 33;
        return now + cacheTicks + Math.floorMod(mixed, Math.max(8, Math.min(40, cacheTicks / 5)));
    }

    private record CacheKey(long section) {
    }

    private record CacheEntry(List<BlockPos> positions, long refreshAt) {
    }

    private record ScoredPosition(BlockPos pos, double score) {
    }

    private static final class LevelCache {
        private final LinkedHashMap<CacheKey, CacheEntry> entries = new LinkedHashMap<>(64, 0.75F, true);

        private void trim() {
            Iterator<CacheKey> iterator = this.entries.keySet().iterator();
            while (this.entries.size() > MAX_ENTRIES_PER_LEVEL && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }
}
