package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.config.HummingbirdConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Local candidate weights only. Biome restrictions and population caps remain with the spawning caller. */
public final class GardenAttraction {
    private GardenAttraction() { }

    public static double hummingbirdSpawnMultiplier(ServerLevel level, BlockPos pos) {
        return flowerMultiplier(FlowerHabitatIndex.countFlowers(level, pos, 16));
    }

    public static double flowerMultiplier(int plants) {
        if (plants < 5) return 0;
        if (plants < 15) return 1;
        if (plants < 30) return 1.5;
        if (plants < 60) return 2;
        return 3;
    }

    public static double songbirdMultiplier(ServerLevel level, BlockPos pos, BirdSpecies species) {
        if (species != BirdSpecies.SPARROW && species != BirdSpecies.LONG_TAILED_TIT && species != BirdSpecies.MYNA) return 1;
        int radius = HummingbirdConfig.gardenRadius();
        int flowers = FlowerHabitatIndex.countFlowers(level, pos, radius);
        if (flowers < 5) return 1;
        int birds = Math.min(4, HummingbirdPollinationService.activeSourcesNear(level, Vec3.atCenterOf(pos), radius));
        return 1 + Math.max(0, Math.min(0.15, HummingbirdConfig.gardenBonus()))
                * Math.min(1, flowers / 15.0) * birds / 4.0;
    }
}
