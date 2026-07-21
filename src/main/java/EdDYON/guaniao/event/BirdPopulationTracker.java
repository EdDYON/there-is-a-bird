package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
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
    private static final Map<ServerLevel, LevelPopulation> LEVELS = new WeakHashMap<>();

    private BirdPopulationTracker() {
    }

    public static int totalAt(ServerLevel level, double x, double z) {
        LevelPopulation population = population(level);
        RegionCount count = population.regions.get(regionKey(x, z, population.regionChunks));
        return count == null ? 0 : count.total;
    }

    public static int speciesAt(ServerLevel level, double x, double z, BirdSpecies species) {
        LevelPopulation population = population(level);
        RegionCount count = population.regions.get(regionKey(x, z, population.regionChunks));
        return count == null || species == null ? 0 : count.bySpecies.getOrDefault(species, 0);
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

    public static void tickBird(Mob mob) {
        if (mob.level() instanceof ServerLevel level && BirdSpecies.from(mob) != null
                && Math.floorMod(mob.tickCount + mob.getId(), 100) == 0) {
            update(level, mob, false);
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
