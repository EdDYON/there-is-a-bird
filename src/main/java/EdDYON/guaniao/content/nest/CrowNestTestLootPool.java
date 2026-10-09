package EdDYON.guaniao.content.nest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Fully weighted test rolls; item quality and footprint are intentionally unrelated. */
public final class CrowNestTestLootPool {
    private static final Item[] JUNK = {
            Items.ROTTEN_FLESH, Items.FEATHER, Items.STICK, Items.BONE, Items.STRING,
            Items.LEATHER, Items.FLINT, Items.CLAY_BALL, Items.COAL
    };
    private static final Item[] COMMON = {
            Items.IRON_NUGGET, Items.COPPER_INGOT, Items.REDSTONE, Items.LAPIS_LAZULI, Items.GLASS_BOTTLE
    };
    private static final Item[] VALUABLE = {
            Items.IRON_INGOT, Items.GOLD_NUGGET, Items.QUARTZ, Items.GLOWSTONE_DUST
    };
    private static final Item[] RARE = {
            Items.GOLD_INGOT, Items.AMETHYST_SHARD, Items.PRISMARINE_CRYSTALS, Items.CLOCK
    };
    private static final Item[] TREASURE = {
            Items.EMERALD, Items.DIAMOND
    };

    private CrowNestTestLootPool() {
    }

    /** Rolls four to six independent finds: no tier or footprint is guaranteed. */
    public static List<ItemStack> roll(RandomSource random) {
        int rewardCount = 4 + random.nextInt(3);
        List<ItemStack> rewards = new ArrayList<>(rewardCount);
        for (int index = 0; index < rewardCount; index++) {
            rewards.add(rollOne(random));
        }
        return rewards;
    }

    private static ItemStack rollOne(RandomSource random) {
        LootBucket bucket = rollBucket(random);
        Item item = switch (bucket) {
            case JUNK -> pick(JUNK, random);
            case COMMON -> pick(COMMON, random);
            case VALUABLE -> pick(VALUABLE, random);
            case RARE -> pick(RARE, random);
            case TREASURE -> pick(TREASURE, random);
        };
        int count = switch (bucket) {
            case JUNK -> 1 + random.nextInt(5);
            case COMMON -> 2 + random.nextInt(11);
            case VALUABLE -> 1 + random.nextInt(5);
            case RARE -> 1 + random.nextInt(3);
            case TREASURE -> 1;
        };
        return new ItemStack(item, count);
    }

    private static LootBucket rollBucket(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 38) {
            return LootBucket.JUNK;
        }
        if (roll < 68) {
            return LootBucket.COMMON;
        }
        if (roll < 86) {
            return LootBucket.VALUABLE;
        }
        if (roll < 97) {
            return LootBucket.RARE;
        }
        return LootBucket.TREASURE;
    }

    private static Item pick(Item[] pool, RandomSource random) {
        return pool[random.nextInt(pool.length)];
    }

    private enum LootBucket {
        JUNK,
        COMMON,
        VALUABLE,
        RARE,
        TREASURE
    }
}
