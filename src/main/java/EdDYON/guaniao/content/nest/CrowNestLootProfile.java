package EdDYON.guaniao.content.nest;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Search duration and reveal sound tier. This is deliberately independent of grid footprint. */
public enum CrowNestLootProfile {
    COMMON(12),
    VALUABLE(24),
    RARE(42),
    TREASURE(68);

    private final int searchTicks;

    CrowNestLootProfile(int searchTicks) {
        this.searchTicks = searchTicks;
    }

    public int searchTicks() {
        return this.searchTicks;
    }

    public static CrowNestLootProfile forStack(ItemStack stack) {
        if (stack.is(Items.DIAMOND) || stack.is(Items.EMERALD)) {
            return TREASURE;
        }
        if (stack.is(Items.GOLD_INGOT)
                || stack.is(Items.CLOCK)
                || stack.is(Items.AMETHYST_SHARD)
                || stack.is(Items.PRISMARINE_CRYSTALS)) {
            return RARE;
        }
        if (stack.is(Items.IRON_INGOT)
                || stack.is(Items.GOLD_NUGGET)
                || stack.is(Items.QUARTZ)
                || stack.is(Items.GLOWSTONE_DUST)) {
            return VALUABLE;
        }
        return COMMON;
    }
}
