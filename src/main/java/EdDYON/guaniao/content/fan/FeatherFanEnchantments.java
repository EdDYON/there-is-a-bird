package EdDYON.guaniao.content.fan;

import EdDYON.guaniao.content.enchantment.GuaniaoEnchantments;
import net.minecraft.world.item.ItemStack;

public final class FeatherFanEnchantments {
    private FeatherFanEnchantments() {
    }

    public static boolean hasBurialPlume(ItemStack stack) {
        return GuaniaoEnchantments.level(stack, GuaniaoEnchantments.BURIAL_PLUME) > 0;
    }

    public static boolean hasRivenPlume(ItemStack stack) {
        return GuaniaoEnchantments.level(stack, GuaniaoEnchantments.RIVEN_PLUME) > 0;
    }

    public static boolean hasHuntingReturn(ItemStack stack) {
        return GuaniaoEnchantments.level(stack, GuaniaoEnchantments.HUNTING_RETURN) > 0;
    }
}
