package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdSpawnConfigEvents {
    private BirdSpawnConfigEvents() {
    }

    @SubscribeEvent
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory() != MobCategory.CREATURE) {
            return;
        }
        ServerLevel level = event.getLevel() instanceof ServerLevel serverLevel ? serverLevel : null;
        for (MobSpawnSettings.SpawnerData original : new ArrayList<>(event.getSpawnerDataList())) {
            BirdSpecies species = BirdSpecies.from(original.type);
            if (species == null) {
                continue;
            }
            event.removeSpawnerData(original);
            double multiplier = BirdConfigManager.spawnMultiplier(species);
            int available = level == null ? Integer.MAX_VALUE
                    : remainingCapacity(level, event.getPos().getX(), event.getPos().getZ(), species);
            if (multiplier <= 0.0D || available <= 0) {
                continue;
            }
            int weight = Math.max(1, (int)Math.round(original.getWeight().asInt() * multiplier));
            int minGroup = BirdConfigManager.minGroup(species);
            int maxGroup = BirdConfigManager.maxGroup(species);
            if (level != null) {
                BirdColonySpawnRules.GroupSize colonyGroup = BirdColonySpawnRules.groupAt(level, event.getPos(), species);
                if (colonyGroup != null) {
                    minGroup = colonyGroup.min();
                    maxGroup = colonyGroup.max();
                }
            }
            maxGroup = Math.min(maxGroup, available);
            minGroup = Math.min(minGroup, maxGroup);
            event.addSpawnerData(new MobSpawnSettings.SpawnerData(original.type, weight, minGroup, maxGroup));
        }
    }

    @SubscribeEvent
    public static void onSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION) {
            return;
        }
        BirdSpecies species = BirdSpecies.from(event.getEntityType());
        if (species == null) {
            return;
        }
        // Chunk-generation spawning can run against a WorldGenRegion rather than
        // a ServerLevel, which bypasses the live population tracker. Runtime
        // natural spawning repopulates the same habitats under the configured caps.
        if (event.getSpawnType() == MobSpawnType.CHUNK_GENERATION) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (!BirdConfigManager.allowsNaturalSpawning(species)) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (species.requiresOpenSkyForNaturalSpawn() && !event.getLevel().canSeeSky(event.getPos())) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (event.getLevel() instanceof ServerLevel level) {
            if (!isBelowGlobalCaps(level, event.getPos().getX(), event.getPos().getZ())
                    || !isBelowSpeciesCap(level, event.getPos().getX(), event.getPos().getZ(), species)) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    private static boolean isBelowGlobalCaps(ServerLevel level, double x, double z) {
        int nearbyCount = BirdPopulationTracker.totalAt(level, x, z);
        int regionalCount = BirdPopulationTracker.totalInRegionAt(level, x, z);
        return BirdConfigManager.maxBirdsNearby() > 0
                && nearbyCount < BirdConfigManager.maxBirdsNearby()
                && (BirdConfigManager.maxWildBirdsPerRegion() <= 0
                || regionalCount < BirdConfigManager.maxWildBirdsPerRegion());
    }

    private static boolean isBelowSpeciesCap(ServerLevel level, double x, double z, BirdSpecies species) {
        int limit = BirdConfigManager.maxWildNearby(species);
        return limit > 0 && BirdPopulationTracker.speciesAt(level, x, z, species) < limit;
    }

    private static int remainingCapacity(ServerLevel level, double x, double z, BirdSpecies species) {
        int nearbyLimit = BirdConfigManager.maxBirdsNearby();
        int speciesLimit = BirdConfigManager.maxWildNearby(species);
        if (nearbyLimit <= 0 || speciesLimit <= 0) {
            return 0;
        }
        int remaining = Math.min(
                nearbyLimit - BirdPopulationTracker.totalAt(level, x, z),
                speciesLimit - BirdPopulationTracker.speciesAt(level, x, z, species)
        );
        int regionalLimit = BirdConfigManager.maxWildBirdsPerRegion();
        if (regionalLimit > 0) {
            remaining = Math.min(remaining,
                    regionalLimit - BirdPopulationTracker.totalInRegionAt(level, x, z));
        }
        return Math.max(0, remaining);
    }

    /** Compatibility entry point used by flyby spawning. */
    public static int nearbyBirdCount(ServerLevel level, double x, double y, double z) {
        return BirdPopulationTracker.totalAt(level, x, z);
    }
}
