package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.phys.AABB;
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

        boolean canAddBirds = BirdConfigManager.maxBirdsNearby() > 0 && !isAtNearbyCap(event);
        for (MobSpawnSettings.SpawnerData original : new ArrayList<>(event.getSpawnerDataList())) {
            BirdSpecies species = BirdSpecies.from(original.type);
            if (species == null) {
                continue;
            }
            event.removeSpawnerData(original);
            if (!canAddBirds) {
                continue;
            }
            double multiplier = BirdConfigManager.spawnMultiplier(species);
            if (multiplier <= 0.0D) {
                continue;
            }
            int weight = Math.max(1, (int)Math.round(original.getWeight().asInt() * multiplier));
            event.addSpawnerData(new MobSpawnSettings.SpawnerData(
                    original.type,
                    weight,
                    BirdConfigManager.minGroup(species),
                    BirdConfigManager.maxGroup(species)
            ));
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
        if (event.getLevel() instanceof ServerLevel level && nearbyBirdCount(level, event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()) >= BirdConfigManager.maxBirdsNearby()) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static boolean isAtNearbyCap(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        return nearbyBirdCount(level, event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()) >= BirdConfigManager.maxBirdsNearby();
    }

    public static int nearbyBirdCount(ServerLevel level, double x, double y, double z) {
        AABB area = new AABB(
                x - BirdConfigManager.BIRD_CAP_HORIZONTAL_RADIUS,
                y - BirdConfigManager.BIRD_CAP_VERTICAL_RADIUS,
                z - BirdConfigManager.BIRD_CAP_HORIZONTAL_RADIUS,
                x + BirdConfigManager.BIRD_CAP_HORIZONTAL_RADIUS,
                y + BirdConfigManager.BIRD_CAP_VERTICAL_RADIUS,
                z + BirdConfigManager.BIRD_CAP_HORIZONTAL_RADIUS
        );
        return level.getEntitiesOfClass(Mob.class, area, mob -> BirdSpecies.from(mob) != null).size();
    }
}
