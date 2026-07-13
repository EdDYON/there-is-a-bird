package EdDYON.guaniao.content.nest;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CrowNestTreasure {
    private CrowNestTreasure() {
    }

    public static boolean isAccepted(ItemStack stack) {
        return isShiny(stack);
    }

    /** Items that may be present in a rummage nest, including harmless bits of crow junk. */
    public static boolean isAllowedNestLoot(ItemStack stack) {
        return !stack.isEmpty() && !stack.hasTag() && (isShiny(stack) || isJunk(stack));
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

    private static boolean isJunk(ItemStack stack) {
        if (stack.isEmpty() || stack.hasCustomHoverName()) {
            return false;
        }
        return stack.is(Items.ROTTEN_FLESH)
                || stack.is(Items.FEATHER)
                || stack.is(Items.STICK)
                || stack.is(Items.BONE)
                || stack.is(Items.STRING)
                || stack.is(Items.LEATHER)
                || stack.is(Items.FLINT)
                || stack.is(Items.CLAY_BALL)
                || stack.is(Items.COAL);
    }

}
