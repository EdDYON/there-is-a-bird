package EdDYON.guaniao.content.bird.mutation;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * Rare genetic mutations that alter a bird's appearance. The first three ordinals
 * (NONE, LEUCISTIC, MELANISTIC) must never change: mutation is persisted to NBT and
 * synced by ordinal, so reordering them would corrupt existing birds.
 */
public enum BirdMutation {
    NONE,
    LEUCISTIC,
    MELANISTIC,
    GOLDEN,
    GOLDEN_PURE,
    RAINBOW;

    private static final int LEUCISTIC_WEIGHT = 1000;
    private static final int MELANISTIC_WEIGHT = 1200;
    private static final int GOLDEN_WEIGHT = 900;
    private static final int GOLDEN_PURE_WEIGHT = 350;
    private static final int RAINBOW_WEIGHT = 500;
    private static final int MUTATION_CHANCE_DENOMINATOR = 2000;

    public static BirdMutation byId(int id) {
        BirdMutation[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }

    public static BirdMutation randomMutation(RandomSource random) {
        if (random.nextInt(MUTATION_CHANCE_DENOMINATOR) != 0) {
            return NONE;
        }
        int total = LEUCISTIC_WEIGHT + MELANISTIC_WEIGHT + GOLDEN_WEIGHT + GOLDEN_PURE_WEIGHT + RAINBOW_WEIGHT;
        int roll = random.nextInt(total);
        if (roll < LEUCISTIC_WEIGHT) {
            return LEUCISTIC;
        }
        if (roll < LEUCISTIC_WEIGHT + MELANISTIC_WEIGHT) {
            return MELANISTIC;
        }
        if (roll < LEUCISTIC_WEIGHT + MELANISTIC_WEIGHT + GOLDEN_WEIGHT) {
            return GOLDEN;
        }
        return roll < LEUCISTIC_WEIGHT + MELANISTIC_WEIGHT + GOLDEN_WEIGHT + GOLDEN_PURE_WEIGHT
                ? GOLDEN_PURE : RAINBOW;
    }

    /** Whether this mutation drops gold ingots when the bird is defeated. */
    public boolean isGold() {
        return this == GOLDEN || this == GOLDEN_PURE;
    }

    public static boolean isMutated(Entity entity) {
        return entity instanceof BirdMutationHolder holder && holder.getBirdMutation() != NONE;
    }
}
