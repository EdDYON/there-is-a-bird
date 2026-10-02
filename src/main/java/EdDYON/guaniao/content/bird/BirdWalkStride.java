package EdDYON.guaniao.content.bird;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraftforge.registries.ForgeRegistries;

/** Authored foot travel, in model pixels per animation tick, before the entity render scale. */
public final class BirdWalkStride {
    public enum Gait { WALK, RUN, TROT, SPRINT }
    private BirdWalkStride() { }

    public static double pixelsPerTick(PathfinderMob bird, Gait gait) {
        return pixelsPerTick(ForgeRegistries.ENTITY_TYPES.getKey(bird.getType()).getPath(), gait);
    }

    static double pixelsPerTick(String species, Gait gait) {
        // Measured from the baked walk clips and geometry: average backwards foot travel
        // during stance, including parent transforms and Woodcock's authored 0.45 scale.
        // Body size (including individual/April Fools scale) is applied by the distance clock.
        if (species.equals("night_heron") && gait == Gait.RUN) return 1.4175;
        if (species.equals("cassowary")) {
            if (gait == Gait.TROT) return 0.6432;
            if (gait == Gait.SPRINT) return 1.9515;
        }
        return switch (species) {
            case "night_heron" -> 0.4887;
            case "sparrow" -> 0.2169;
            case "long_tailed_tit" -> 0.4685;
            case "cockatiel" -> 0.7136;
            case "macaw" -> 0.6667;
            case "budgerigar" -> 0.7754;
            case "pigeon", "spotted_dove" -> 0.3285;
            case "crow" -> 0.4787;
            case "seagull" -> 0.5091;
            case "myna" -> 0.4706;
            case "woodcock" -> 0.3549;
            case "kestrel" -> 0.4328;
            case "umbrella_cockatoo" -> 0.35;
            case "kiwi" -> 3.0174;
            case "cassowary" -> 0.857;
            default -> throw new IllegalArgumentException("Missing walk stride for bird: " + species);
        };
    }
}
