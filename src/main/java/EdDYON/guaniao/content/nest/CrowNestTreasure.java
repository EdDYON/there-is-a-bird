package EdDYON.guaniao.content.nest;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CrowNestTreasure {
    private CrowNestTreasure() {
    }

    public static boolean isAccepted(ItemStack stack) {
        return isShiny(stack);
    }

    public static boolean isShiny(ItemStack stack) {
        if (stack.isEmpty() || stack.hasCustomHoverName()) {
            return false;
        }
        return stack.is(Items.GOLD_NUGGET)
                || stack.is(Items.IRON_NUGGET)
                || stack.is(Items.COPPER_INGOT)
                || stack.is(Items.GOLD_INGOT)
                || stack.is(Items.IRON_INGOT)
                || stack.is(Items.AMETHYST_SHARD)
                || stack.is(Items.REDSTONE)
                || stack.is(Items.LAPIS_LAZULI)
                || stack.is(Items.GLOWSTONE_DUST)
                || stack.is(Items.GLASS_BOTTLE)
                || stack.is(Items.CLOCK)
                || stack.is(Items.DIAMOND)
                || stack.is(Items.EMERALD)
                || stack.is(Items.QUARTZ)
                || stack.is(Items.PRISMARINE_CRYSTALS);
    }

}
