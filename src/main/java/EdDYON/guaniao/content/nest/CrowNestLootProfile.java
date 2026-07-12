package EdDYON.guaniao.content.nest;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The rummage footprint and search duration for each level of crow treasure. */
public enum CrowNestLootProfile {
    COMMON(1, 1, 14, 0xFF70836E),
    VALUABLE(2, 1, 24, 0xFF7F9FB4),
    RARE(1, 2, 38, 0xFFC6954B),
    TREASURE(2, 2, 64, 0xFFD8B65C);

    private final int width;
    private final int height;
    private final int searchTicks;
    private final int color;

    CrowNestLootProfile(int width, int height, int searchTicks, int color) {
        this.width = width;
        this.height = height;
        this.searchTicks = searchTicks;
        this.color = color;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public int searchTicks() {
        return this.searchTicks;
    }

    public int color() {
        return this.color;
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
