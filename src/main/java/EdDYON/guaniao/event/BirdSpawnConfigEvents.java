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
        boolean globalCapacity = level == null || isBelowGlobalCaps(level, event.getPos().getX(), event.getPos().getZ());
        for (MobSpawnSettings.SpawnerData original : new ArrayList<>(event.getSpawnerDataList())) {
            BirdSpecies species = BirdSpecies.from(original.type);
            if (species == null) {
                continue;
            }
            event.removeSpawnerData(original);
            double multiplier = BirdConfigManager.spawnMultiplier(species);
            if (!globalCapacity || multiplier <= 0.0D) {
                continue;
            }
            int weight = Math.max(1, (int)Math.round(original.getWeight().asInt() * multiplier));
            if (level != null && BirdPopulationTracker.speciesAt(level, event.getPos().getX(), event.getPos().getZ(), species)
                    >= BirdConfigManager.maxWildNearby(species)) {
                weight = Math.max(1, weight / 4);
            }
            int minGroup = BirdConfigManager.minGroup(species);
            int maxGroup = BirdConfigManager.maxGroup(species);
            if (level != null) {
                BirdColonySpawnRules.GroupSize colonyGroup = BirdColonySpawnRules.groupAt(level, event.getPos(), species);
                if (colonyGroup != null) {
                    minGroup = colonyGroup.min();
                    maxGroup = colonyGroup.max();
                }
            }
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
        if (!BirdConfigManager.allowsNaturalSpawning(species)) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (!event.getLevel().canSeeSky(event.getPos())) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (event.getLevel() instanceof ServerLevel level) {
            if (!isBelowGlobalCaps(level, event.getPos().getX(), event.getPos().getZ())) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    private static boolean isBelowGlobalCaps(ServerLevel level, double x, double z) {
        int count = BirdPopulationTracker.totalAt(level, x, z);
        return BirdConfigManager.maxBirdsNearby() > 0
                && count < BirdConfigManager.maxBirdsNearby()
                && (BirdConfigManager.maxWildBirdsPerRegion() <= 0
                || count < BirdConfigManager.maxWildBirdsPerRegion());
    }

    /** Compatibility entry point used by flyby spawning. */
    public static int nearbyBirdCount(ServerLevel level, double x, double y, double z) {
        return BirdPopulationTracker.totalAt(level, x, z);
    }
}
