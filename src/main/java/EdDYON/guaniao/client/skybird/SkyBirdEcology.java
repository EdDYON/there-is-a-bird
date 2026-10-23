package EdDYON.guaniao.client.skybird;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Biome, time and weather rules for lightweight client sky flocks. */
public final class SkyBirdEcology {
    private static final int HABITAT_SAMPLE_RADIUS = 64;
    private static final int[][] HABITAT_SAMPLE_OFFSETS = {
            {0, 0},
            {HABITAT_SAMPLE_RADIUS, 0},
            {-HABITAT_SAMPLE_RADIUS, 0},
            {0, HABITAT_SAMPLE_RADIUS},
            {0, -HABITAT_SAMPLE_RADIUS},
            {HABITAT_SAMPLE_RADIUS, HABITAT_SAMPLE_RADIUS},
            {-HABITAT_SAMPLE_RADIUS, HABITAT_SAMPLE_RADIUS},
            {HABITAT_SAMPLE_RADIUS, -HABITAT_SAMPLE_RADIUS},
            {-HABITAT_SAMPLE_RADIUS, -HABITAT_SAMPLE_RADIUS}
    };
    private static final Map<SkyBirdSpecies, Profile> PROFILES = new EnumMap<>(SkyBirdSpecies.class);

    static {
        register(SkyBirdSpecies.GOOSE, "goose_sky_habitat", 18, 1, Activity.MIGRATION);
        register(SkyBirdSpecies.EAGLE, "eagle_sky_habitat", 8, 1, Activity.THERMAL);
        register(SkyBirdSpecies.SEAGULL, "seagull_habitat", 32, 2, Activity.DAYTIME);
        register(SkyBirdSpecies.SWALLOW, "swallow_sky_habitat", 26, 2, Activity.AERIAL);
        register(SkyBirdSpecies.CRANE, "crane_sky_habitat", 14, 1, Activity.MIGRATION);
        register(SkyBirdSpecies.VULTURE, "vulture_sky_habitat", 10, 1, Activity.THERMAL);
        register(SkyBirdSpecies.STARLING, "starling_sky_habitat", 90, 1, Activity.DUSK_FLOCK);
    }

    private SkyBirdEcology() {
    }

    public static EnumSet<SkyBirdSpecies> availableSpecies(ClientLevel level, BlockPos position,
                                                            Set<SkyBirdSpecies> textureReady) {
        EnumSet<SkyBirdSpecies> result = EnumSet.noneOf(SkyBirdSpecies.class);
        for (SkyBirdSpecies species : SkyBirdSpecies.values()) {
            Profile profile = PROFILES.get(species);
            if (profile != null
                    && textureReady.contains(species)
                    && spawnWeight(species, level) > 0
                    && matchesNearbyHabitat(level, position, profile.habitat())) {
                result.add(species);
            }
        }
        return result;
    }

    /**
     * High-altitude birds use the surrounding landscape as their habitat,
     * rather than the single biome cell directly below the player. Unloaded
     * sample chunks are ignored so this lookup never forces chunk loading.
     */
    private static boolean matchesNearbyHabitat(ClientLevel level, BlockPos origin,
                                                TagKey<Biome> habitat) {
        for (int[] offset : HABITAT_SAMPLE_OFFSETS) {
            BlockPos sample = origin.offset(offset[0], 0, offset[1]);
            if (!level.hasChunk(sample.getX() >> 4, sample.getZ() >> 4)) {
                continue;
            }
            Holder<Biome> biome = level.getBiome(sample);
            if (biome.is(habitat)) {
                return true;
            }
        }
        return false;
    }

    public static int spawnWeight(SkyBirdSpecies species, ClientLevel level) {
        Profile profile = PROFILES.get(species);
        if (profile == null || level.isThundering()) {
            return 0;
        }

        long time = Math.floorMod(level.getDayTime(), 24000L);
        if (time >= 13000L && time <= 23000L) {
            return 0;
        }

        boolean dawn = time <= 3000L || time >= 23000L;
        boolean midday = time >= 4500L && time <= 8500L;
        boolean dusk = time >= 10500L && time < 13000L;
        double activityMultiplier = switch (profile.activity()) {
            case MIGRATION -> dawn || dusk ? 1.8D : midday ? 0.65D : 1.0D;
            case THERMAL -> midday ? 1.75D : dawn || dusk ? 0.55D : 1.0D;
            case AERIAL -> dawn ? 1.35D : dusk ? 1.20D : 1.0D;
            case DUSK_FLOCK -> dusk ? 3.25D : dawn ? 1.45D : midday ? 0.55D : 0.85D;
            case DAYTIME -> dusk ? 0.80D : 1.0D;
        };

        double weatherMultiplier = 1.0D;
        if (level.isRaining()) {
            weatherMultiplier = switch (species) {
                case SEAGULL -> 0.72D;
                case GOOSE, CRANE -> 0.50D;
                case SWALLOW, STARLING -> 0.32D;
                case EAGLE -> 0.24D;
                case VULTURE -> 0.16D;
            };
        }
        return Math.max(0, (int)Math.round(profile.baseWeight() * activityMultiplier * weatherMultiplier));
    }

    public static int maxFlocks(SkyBirdSpecies species) {
        Profile profile = PROFILES.get(species);
        return profile == null ? 0 : profile.maxFlocks();
    }

    private static void register(SkyBirdSpecies species, String habitatPath,
                                 int baseWeight, int maxFlocks, Activity activity) {
        TagKey<Biome> habitat = TagKey.create(
                Registries.BIOME,
                new ResourceLocation(GuaniaoMod.MOD_ID, habitatPath)
        );
        PROFILES.put(species, new Profile(habitat, baseWeight, maxFlocks, activity));
    }

    private record Profile(TagKey<Biome> habitat, int baseWeight, int maxFlocks, Activity activity) {
    }

    private enum Activity {
        DAYTIME,
        AERIAL,
        MIGRATION,
        THERMAL,
        DUSK_FLOCK
    }
}
