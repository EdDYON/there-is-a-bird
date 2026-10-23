package EdDYON.guaniao.content.fan;

import EdDYON.guaniao.content.enchantment.GuaniaoEnchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class FeatherFanEnchantments {
    private FeatherFanEnchantments() {
    }

    public static boolean hasBurialPlume(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                GuaniaoEnchantments.BURIAL_PLUME.get(), stack) > 0;
    }

    public static boolean hasRivenPlume(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                GuaniaoEnchantments.RIVEN_PLUME.get(), stack) > 0;
    }

    public static boolean hasHuntingReturn(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                GuaniaoEnchantments.HUNTING_RETURN.get(), stack) > 0;
    }
}
