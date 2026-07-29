package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Incremental wild-bird counts; spawn checks never scan a large entity AABB. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdPopulationTracker {
    private static final String LAST_NEARBY_PLAYER_TIME = "GuaniaoLastNearbyPlayerTime";
    private static final String EXCESS_POPULATION_SINCE = "GuaniaoExcessPopulationSince";
    private static final String TRANSIENT_FLYBY_SPAWN_TIME = "GuaniaoTransientFlybySpawnTime";
    private static final int TRACK_INTERVAL_TICKS = 100;
    private static final long EXCESS_POPULATION_GRACE_TICKS = 1200L;
    private static final long UNOBSERVED_LIFETIME_TICKS = 24000L;
    private static final long TRANSIENT_FLYBY_LIFETIME_TICKS = 3600L;
    private static final double EXCESS_CULL_DISTANCE_SQR = 32.0D * 32.0D;
    private static final double NEARBY_PLAYER_DISTANCE_SQR = 32.0D * 32.0D;
    private static final double UNOBSERVED_CULL_DISTANCE_SQR = 48.0D * 48.0D;
    private static final double VISIBLE_CULL_GUARD_DISTANCE_SQR = 64.0D * 64.0D;
    private static final Map<ServerLevel, LevelPopulation> LEVELS = new WeakHashMap<>();

    private BirdPopulationTracker() {
    }

    public static int totalAt(ServerLevel level, double x, double z) {
        LevelPopulation population = population(level);
        return population.totalNear(regionKey(x, z, population.regionChunks));
    }

    public static int totalInRegionAt(ServerLevel level, double x, double z) {
        LevelPopulation population = population(level);
        RegionCount count = population.regions.get(regionKey(x, z, population.regionChunks));
        return count == null ? 0 : count.total;
    }

    public static int speciesAt(ServerLevel level, double x, double z, BirdSpecies species) {
        LevelPopulation population = population(level);
        return species == null ? 0 : population.speciesNear(regionKey(x, z, population.regionChunks), species);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Mob mob
                && BirdSpecies.from(mob) != null) {
            update(level, mob, true);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LevelPopulation population = LEVELS.get(level);
        if (population != null) {
            TrackedBird tracked = population.tracked.remove(event.getEntity().getUUID());
            if (tracked != null && tracked.counted) {
                population.adjust(tracked.region, tracked.species, -1);
            }
        }
    }

    public static boolean tickBird(Mob mob) {
        if (mob.level() instanceof ServerLevel level && BirdSpecies.from(mob) != null
                && Math.floorMod(mob.tickCount + mob.getId(), TRACK_INTERVAL_TICKS) == 0) {
            update(level, mob, false);
            return shouldCullWildBird(level, mob);
        }
        return false;
    }

    public static void markTransientFlyby(Mob mob) {
        if (mob.level() instanceof ServerLevel level) {
            mob.getPersistentData().putLong(TRANSIENT_FLYBY_SPAWN_TIME, level.getGameTime());
        }
    }

    public static void rebuild(net.minecraft.server.MinecraftServer server) {
        LEVELS.clear();
        for (ServerLevel level : server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof Mob mob && BirdSpecies.from(mob) != null) {
                    update(level, mob, true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LEVELS.remove(level);
        }
    }

    private static void update(ServerLevel level, Mob mob, boolean force) {
        LevelPopulation population = population(level);
        BirdSpecies species = BirdSpecies.from(mob);
        if (species == null) {
            return;
        }
        RegionKey region = regionKey(mob.getX(), mob.getZ(), population.regionChunks);
        boolean counted = isCountedWildBird(mob);
        TrackedBird previous = population.tracked.get(mob.getUUID());
        if (!force && previous != null && previous.region.equals(region)
                && previous.species == species && previous.counted == counted) {
            return;
        }
        if (previous != null && previous.counted) {
            population.adjust(previous.region, previous.species, -1);
        }
        population.tracked.put(mob.getUUID(), new TrackedBird(region, species, counted));
        if (counted) {
            population.adjust(region, species, 1);
        }
    }

    private static boolean isCountedWildBird(Mob mob) {
        return mob.isAlive() && !mob.isNoAi() && !mob.hasCustomName()
                && (!(mob instanceof TamableAnimal tamable) || !tamable.isTame());
    }

    private static boolean shouldCullWildBird(ServerLevel level, Mob mob) {
        if (!isSafeToCull(mob)) {
            clearLifecycleTimers(mob);
            return false;
        }

        long now = level.getGameTime();
        CompoundTag data = mob.getPersistentData();
        double nearestPlayerDistanceSqr = nearestPlayerDistanceSqr(level, mob);
        if (!data.contains(LAST_NEARBY_PLAYER_TIME, Tag.TAG_LONG)) {
            data.putLong(LAST_NEARBY_PLAYER_TIME, now);
        }
        if (nearestPlayerDistanceSqr <= NEARBY_PLAYER_DISTANCE_SQR) {
            data.putLong(LAST_NEARBY_PLAYER_TIME, now);
        }

        if (data.contains(TRANSIENT_FLYBY_SPAWN_TIME, Tag.TAG_LONG)
                && elapsed(now, data.getLong(TRANSIENT_FLYBY_SPAWN_TIME)) >= TRANSIENT_FLYBY_LIFETIME_TICKS
                && canCullQuietly(level, mob, nearestPlayerDistanceSqr)) {
            return true;
        }

        BirdSpecies species = BirdSpecies.from(mob);
        if (isOverCapacity(level, mob, species)) {
            if (!data.contains(EXCESS_POPULATION_SINCE, Tag.TAG_LONG)) {
                data.putLong(EXCESS_POPULATION_SINCE, now);
            } else if (elapsed(now, data.getLong(EXCESS_POPULATION_SINCE)) >= EXCESS_POPULATION_GRACE_TICKS
                    && canCullQuietly(level, mob, nearestPlayerDistanceSqr)) {
                return true;
            }
        } else {
            data.remove(EXCESS_POPULATION_SINCE);
        }

        return nearestPlayerDistanceSqr > UNOBSERVED_CULL_DISTANCE_SQR
                && elapsed(now, data.getLong(LAST_NEARBY_PLAYER_TIME)) >= UNOBSERVED_LIFETIME_TICKS
                && canCullQuietly(level, mob, nearestPlayerDistanceSqr);
    }

    private static boolean isSafeToCull(Mob mob) {
        if (!isCountedWildBird(mob) || mob.isPersistenceRequired() || mob.requiresCustomPersistence()
                || mob.isLeashed() || mob.isPassenger() || mob.isVehicle() || mob.getTarget() != null) {
            return false;
        }
        if (mob instanceof AgeableMob ageable && ageable.isBaby()) {
            return false;
        }
        return !(mob instanceof Animal animal) || !animal.isInLove();
    }

    private static void clearLifecycleTimers(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        data.remove(LAST_NEARBY_PLAYER_TIME);
        data.remove(EXCESS_POPULATION_SINCE);
    }

    private static boolean isOverCapacity(ServerLevel level, Mob mob, BirdSpecies species) {
        int nearbyLimit = BirdConfigManager.maxBirdsNearby();
        int regionalLimit = BirdConfigManager.maxWildBirdsPerRegion();
        int speciesLimit = BirdConfigManager.maxWildNearby(species);
        return nearbyLimit <= 0 || totalAt(level, mob.getX(), mob.getZ()) > nearbyLimit
                || (regionalLimit > 0 && totalInRegionAt(level, mob.getX(), mob.getZ()) > regionalLimit)
                || speciesLimit <= 0
                || speciesAt(level, mob.getX(), mob.getZ(), species) > speciesLimit;
    }

    private static double nearestPlayerDistanceSqr(ServerLevel level, Mob mob) {
        double nearest = Double.POSITIVE_INFINITY;
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            nearest = Math.min(nearest, player.distanceToSqr(mob));
        }
        return nearest;
    }

    private static boolean canCullQuietly(ServerLevel level, Mob mob, double nearestPlayerDistanceSqr) {
        if (nearestPlayerDistanceSqr <= EXCESS_CULL_DISTANCE_SQR) {
            return false;
        }
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (player.distanceToSqr(mob) <= VISIBLE_CULL_GUARD_DISTANCE_SQR
                    && player.hasLineOfSight(mob)) {
                return false;
            }
        }
        return true;
    }

    private static long elapsed(long now, long since) {
        return now >= since ? now - since : 0L;
    }

    private static LevelPopulation population(ServerLevel level) {
        int regionChunks = BirdConfigManager.populationRegionChunks();
        LevelPopulation population = LEVELS.computeIfAbsent(level, ignored -> new LevelPopulation(regionChunks));
        if (population.regionChunks != regionChunks) {
            population = new LevelPopulation(regionChunks);
            LEVELS.put(level, population);
        }
        return population;
    }

    private static RegionKey regionKey(double x, double z, int regionChunks) {
        int chunkX = ((int)Math.floor(x)) >> 4;
        int chunkZ = ((int)Math.floor(z)) >> 4;
        return new RegionKey(Math.floorDiv(chunkX, regionChunks), Math.floorDiv(chunkZ, regionChunks));
    }

    private record RegionKey(int x, int z) {
    }

    private record TrackedBird(RegionKey region, BirdSpecies species, boolean counted) {
    }

    private static final class RegionCount {
        private int total;
        private final EnumMap<BirdSpecies, Integer> bySpecies = new EnumMap<>(BirdSpecies.class);
    }

    private static final class LevelPopulation {
        private final int regionChunks;
        private final Map<RegionKey, RegionCount> regions = new HashMap<>();
        private final Map<UUID, TrackedBird> tracked = new HashMap<>();

        private LevelPopulation(int regionChunks) {
            this.regionChunks = regionChunks;
        }

        private int totalNear(RegionKey center) {
            int total = 0;
            for (int xOffset = -1; xOffset <= 1; ++xOffset) {
                for (int zOffset = -1; zOffset <= 1; ++zOffset) {
                    RegionCount count = this.regions.get(new RegionKey(center.x + xOffset, center.z + zOffset));
                    if (count != null) {
                        total += count.total;
                    }
                }
            }
            return total;
        }

        private int speciesNear(RegionKey center, BirdSpecies species) {
            int total = 0;
            for (int xOffset = -1; xOffset <= 1; ++xOffset) {
                for (int zOffset = -1; zOffset <= 1; ++zOffset) {
                    RegionCount count = this.regions.get(new RegionKey(center.x + xOffset, center.z + zOffset));
                    if (count != null) {
                        total += count.bySpecies.getOrDefault(species, 0);
                    }
                }
            }
            return total;
        }

        private void adjust(RegionKey key, BirdSpecies species, int delta) {
            RegionCount count = this.regions.computeIfAbsent(key, ignored -> new RegionCount());
            count.total = Math.max(0, count.total + delta);
            int speciesCount = Math.max(0, count.bySpecies.getOrDefault(species, 0) + delta);
            if (speciesCount == 0) {
                count.bySpecies.remove(species);
            } else {
                count.bySpecies.put(species, speciesCount);
            }
            if (count.total == 0) {
                this.regions.remove(key);
            }
        }
    }
}
