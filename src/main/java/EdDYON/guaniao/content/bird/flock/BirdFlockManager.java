package EdDYON.guaniao.content.bird.flock;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Short-lived, section-local candidate cache shared by flock goals. The final
 * radius and compatibility checks always run against live entities.
 */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdFlockManager {
    private static final int MAX_CACHE_ENTRIES = 4096;
    private static final Map<ServerLevel, LevelCache> LEVEL_CACHES = new WeakHashMap<>();

    private BirdFlockManager() {
    }

    public static <T extends Entity> List<T> nearby(T source, Class<T> entityClass, double radius) {
        return nearby(source, entityClass, radius, 32);
    }

    public static <T extends Entity> List<T> nearby(T source, Class<T> entityClass, double radius, int maxResults) {
        BirdSpecies species = BirdSpecies.from(source);
        if (species != null) {
            radius = Math.min(radius, BirdConfigManager.flockRadius(species));
            maxResults = Math.min(maxResults, BirdConfigManager.flockMaxMembers(species));
        }
        if (!(source.level() instanceof ServerLevel serverLevel)) {
            return source.level().getEntitiesOfClass(entityClass, source.getBoundingBox().inflate(radius),
                    other -> other.isAlive() && other.getType() == source.getType());
        }
        int sectionX = SectionPos.blockToSectionCoord(source.getBlockX());
        int sectionY = SectionPos.blockToSectionCoord(source.getBlockY());
        int sectionZ = SectionPos.blockToSectionCoord(source.getBlockZ());
        int radiusBucket = Math.max(1, (int)Math.ceil(radius / 4.0D));
        CacheKey key = new CacheKey(SectionPos.asLong(sectionX, sectionY, sectionZ), source.getType(), radiusBucket);
        LevelCache levelCache = LEVEL_CACHES.computeIfAbsent(serverLevel, ignored -> new LevelCache());
        CacheEntry entry = levelCache.entries.get(key);
        long now = serverLevel.getGameTime();
        if (entry == null || now - entry.createdAt >= BirdConfigManager.flockRefreshTicks()) {
            if (!BirdScanBudget.tryAcquire(serverLevel, source)) {
                if (entry == null) {
                    return List.of();
                }
            } else {
                double scanRadius = radiusBucket * 4.0D + 14.0D;
                Vec3 sectionCenter = new Vec3(sectionX * 16.0D + 8.0D, sectionY * 16.0D + 8.0D, sectionZ * 16.0D + 8.0D);
                AABB scanBox = AABB.ofSize(sectionCenter, scanRadius * 2.0D, scanRadius * 2.0D, scanRadius * 2.0D);
                List<Integer> ids = serverLevel.getEntitiesOfClass(entityClass, scanBox,
                                entity -> entity.isAlive() && entity.getType() == source.getType())
                        .stream().map(Entity::getId).toList();
                entry = new CacheEntry(now, ids);
                levelCache.entries.put(key, entry);
                levelCache.trim();
            }
        }

        double radiusSqr = radius * radius;
        List<T> result = new ArrayList<>();
        for (int id : entry.entityIds) {
            Entity entity = serverLevel.getEntity(id);
            if (!entityClass.isInstance(entity) || !entity.isAlive() || entity.getType() != source.getType()
                    || source.distanceToSqr(entity) > radiusSqr) {
                continue;
            }
            result.add(entityClass.cast(entity));
        }
        result.sort(Comparator.comparingDouble(source::distanceToSqr));
        if (result.size() > maxResults) {
            return new ArrayList<>(result.subList(0, maxResults));
        }
        return result;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            LEVEL_CACHES.remove(serverLevel);
        }
    }

    private record CacheKey(long section, EntityType<?> type, int radiusBucket) {
    }

    private record CacheEntry(long createdAt, List<Integer> entityIds) {
    }

    private static final class LevelCache {
        private final LinkedHashMap<CacheKey, CacheEntry> entries = new LinkedHashMap<>(128, 0.75F, true);

        private void trim() {
            while (this.entries.size() > MAX_CACHE_ENTRIES) {
                CacheKey eldest = this.entries.keySet().iterator().next();
                this.entries.remove(eldest);
            }
        }
    }
}
