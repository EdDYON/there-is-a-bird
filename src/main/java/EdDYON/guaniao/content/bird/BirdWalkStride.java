package EdDYON.guaniao.content.bird;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.core.registries.BuiltInRegistries;

/** Walking foot travel or hopping stride, in model pixels per animation tick, before render scale. */
public final class BirdWalkStride {
    public enum Gait { WALK, HOP, RUN, TROT, SPRINT }
    private BirdWalkStride() { }

    public static double pixelsPerTick(PathfinderMob bird, Gait gait) {
        return pixelsPerTick(BuiltInRegistries.ENTITY_TYPE.getKey(bird.getType()).getPath(), gait);
    }

    public static double maximumAnimationTicksPerTick(PathfinderMob bird, Gait gait) {
        return maximumAnimationTicksPerTick(BuiltInRegistries.ENTITY_TYPE.getKey(bird.getType()).getPath(), gait);
    }

    static double maximumAnimationTicksPerTick(String species, Gait gait) {
        // Each authored gait includes coordinated feet, head and body motion.
        // Short strides or small models must not accelerate that whole clip without
        // a limit. Slow travel still uses the distance clock; faster travel reaches
        // the authored rate without building up steps to replay after stopping.
        if (gait != Gait.HOP) return 1.0;
        // At most four hops per second: sparrow has two hops in ten ticks,
        // long-tailed tit has one hop in four ticks. Preview clips keep authored timing.
        return switch (species) {
            case "sparrow" -> 1.0;
            case "long_tailed_tit" -> 0.8;
            default -> throw new IllegalArgumentException("Missing hop cadence for bird: " + species);
        };
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
            // Both feet leave the ground together; tiny backwards foot motion is
            // not a walking stance. Each hop covers 6.4 model pixels instead.
            case "sparrow" -> 1.28;
            case "long_tailed_tit" -> 1.6;
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
