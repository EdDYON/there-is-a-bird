package EdDYON.guaniao.content.bird.sparrow;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.registry.GuaniaoBlocks;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shares breadcrumb block scans between sparrows occupying the same small area. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BreadcrumbSearchCache {
    private static final int HORIZONTAL_CELL_SIZE = 8;
    private static final int HORIZONTAL_PADDING = HORIZONTAL_CELL_SIZE / 2;
    private static final int MAX_ENTRIES_PER_LEVEL = 1024;
    private static final int MAX_REFRESH_STAGGER_TICKS = 40;
    private static final Map<ServerLevel, LevelCache> LEVEL_CACHES = new WeakHashMap<>();

    private BreadcrumbSearchCache() {
    }

    public static List<BlockPos> nearby(SparrowEntity sparrow, int horizontalRadius, int verticalRadius) {
        if (!(sparrow.level() instanceof ServerLevel level)) {
            return List.of();
        }
        int cellX = Math.floorDiv(sparrow.getBlockX(), HORIZONTAL_CELL_SIZE);
        int cellZ = Math.floorDiv(sparrow.getBlockZ(), HORIZONTAL_CELL_SIZE);
        SearchKey key = new SearchKey(cellX, sparrow.getBlockY(), cellZ, horizontalRadius, verticalRadius);
        LevelCache levelCache = LEVEL_CACHES.computeIfAbsent(level, ignored -> new LevelCache());
        CacheEntry cached = levelCache.entries.get(key);
        long now = level.getGameTime();
        if (cached != null && now < cached.refreshAt) {
            return cached.positions;
        }
        if (!BirdScanBudget.tryAcquire(level, sparrow, 2)) {
            return cached == null ? List.of() : cached.positions;
        }

        List<BlockPos> positions = scan(level, key);
        levelCache.entries.put(key, new CacheEntry(positions, nextRefreshAt(now, key)));
        levelCache.trim();
        return positions;
    }

    public static void invalidate(ServerLevel level) {
        LEVEL_CACHES.remove(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LEVEL_CACHES.remove(level);
        }
    }

    private static List<BlockPos> scan(ServerLevel level, SearchKey key) {
        int centerX = key.cellX * HORIZONTAL_CELL_SIZE + HORIZONTAL_PADDING - 1;
        int centerZ = key.cellZ * HORIZONTAL_CELL_SIZE + HORIZONTAL_PADDING - 1;
        int scanRadius = key.horizontalRadius + HORIZONTAL_PADDING;
        List<BlockPos> positions = new ArrayList<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = centerX - scanRadius; x <= centerX + scanRadius; ++x) {
            for (int z = centerZ - scanRadius; z <= centerZ + scanRadius; ++z) {
                if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                    continue;
                }
                for (int y = key.blockY - key.verticalRadius; y <= key.blockY + key.verticalRadius; ++y) {
                    mutablePos.set(x, y, z);
                    if (level.getBlockState(mutablePos).is(GuaniaoBlocks.BREADCRUMBS.get())) {
                        positions.add(mutablePos.immutable());
                    }
                }
            }
        }
        return List.copyOf(positions);
    }

    private static long nextRefreshAt(long now, SearchKey key) {
        int cacheTicks = Math.max(80, BirdConfigManager.habitatCacheTicks());
        long mixed = ((long)key.hashCode() * 0x9E3779B97F4A7C15L) ^ now;
        int stagger = Math.floorMod(mixed ^ mixed >>> 32, MAX_REFRESH_STAGGER_TICKS);
        return now + cacheTicks + stagger;
    }

    private record SearchKey(int cellX, int blockY, int cellZ, int horizontalRadius, int verticalRadius) {
    }

    private record CacheEntry(List<BlockPos> positions, long refreshAt) {
    }

    private static final class LevelCache {
        private final LinkedHashMap<SearchKey, CacheEntry> entries = new LinkedHashMap<>(64, 0.75F, true);

        private void trim() {
            Iterator<SearchKey> iterator = this.entries.keySet().iterator();
            while (this.entries.size() > MAX_ENTRIES_PER_LEVEL && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }
}
