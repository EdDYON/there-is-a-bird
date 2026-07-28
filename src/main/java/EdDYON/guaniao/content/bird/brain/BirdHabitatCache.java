package EdDYON.guaniao.content.bird.brain;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Shares expensive habitat probes between birds occupying the same world section. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdHabitatCache {
    private static final int MAX_CACHE_ENTRIES_PER_LEVEL = 4096;
    private static final int MAX_REFRESH_STAGGER_TICKS = 40;
    private static final Map<ServerLevel, LevelCache> LEVEL_CACHES = new WeakHashMap<>();

    private BirdHabitatCache() {
    }

    @Nullable
    public static BirdHabitatSnapshot sample(PathfinderMob bird, BirdSpeciesProfile profile) {
        if (!(bird.level() instanceof ServerLevel serverLevel)) {
            return profile.scanHabitat(bird);
        }
        int scanCost = profile.habitatScanCost();
        if (scanCost <= 0) {
            return profile.scanHabitat(bird);
        }

        CacheKey key = new CacheKey(
                SectionPos.asLong(
                        SectionPos.blockToSectionCoord(bird.getBlockX()),
                        SectionPos.blockToSectionCoord(bird.getBlockY()),
                        SectionPos.blockToSectionCoord(bird.getBlockZ())),
                profile.getClass());
        LevelCache levelCache = LEVEL_CACHES.computeIfAbsent(serverLevel, ignored -> new LevelCache());
        CacheEntry cached = levelCache.entries.get(key);
        long now = serverLevel.getGameTime();
        if (cached != null && now < cached.refreshAt) {
            return cached.snapshot;
        }
        if (!BirdScanBudget.tryAcquire(serverLevel, bird, scanCost)) {
            return cached == null ? null : cached.snapshot;
        }

        BirdHabitatSnapshot snapshot = profile.scanHabitat(bird);
        levelCache.entries.put(key, new CacheEntry(snapshot, nextRefreshAt(now, key)));
        levelCache.trim();
        return snapshot;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            LEVEL_CACHES.remove(serverLevel);
        }
    }

    private static long nextRefreshAt(long now, CacheKey key) {
        int cacheTicks = BirdConfigManager.habitatCacheTicks();
        int staggerWindow = Math.min(MAX_REFRESH_STAGGER_TICKS, Math.max(4, cacheTicks / 5));
        long mixed = key.section ^ (long)key.profileType.getName().hashCode() * 0x9E3779B97F4A7C15L;
        int stagger = Math.floorMod(mixed ^ mixed >>> 32, staggerWindow);
        return now + cacheTicks + stagger;
    }

    private record CacheKey(long section, Class<? extends BirdSpeciesProfile> profileType) {
    }

    private record CacheEntry(BirdHabitatSnapshot snapshot, long refreshAt) {
    }

    private static final class LevelCache {
        private final LinkedHashMap<CacheKey, CacheEntry> entries = new LinkedHashMap<>(128, 0.75F, true);

        private void trim() {
            Iterator<CacheKey> iterator = this.entries.keySet().iterator();
            while (this.entries.size() > MAX_CACHE_ENTRIES_PER_LEVEL && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }
}
