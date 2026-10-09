package EdDYON.guaniao.client.guide;

import EdDYON.guaniao.content.enchantment.GuaniaoEnchantments;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;

/** Display stacks for the existing custom anvil operation (not a crafting recipe). */
record HandbookAnvilRecipe(ItemStack input, ItemStack book, ItemStack output, String outputPage) {
    static HandbookAnvilRecipe fromBook(String id, HolderLookup.Provider registries) {
        ItemStack fan = new ItemStack(GuaniaoItems.WIND_FEATHER_FAN.get());
        ItemStack output = fan.copy();
        ItemStack book;
        String suffix;
        switch (id) {
            case "guaniao:burial_plume_book" -> {
                book = new ItemStack(GuaniaoItems.BURIAL_PLUME_BOOK.get());
                output.enchant(GuaniaoEnchantments.holder(registries, GuaniaoEnchantments.BURIAL_PLUME), 1);
                suffix = "burial";
            }
            case "guaniao:riven_plume_book" -> {
                book = new ItemStack(GuaniaoItems.RIVEN_PLUME_BOOK.get());
                output.enchant(GuaniaoEnchantments.holder(registries, GuaniaoEnchantments.RIVEN_PLUME), 1);
                suffix = "riven";
            }
            case "guaniao:hunting_return_book" -> {
                book = new ItemStack(GuaniaoItems.HUNTING_RETURN_BOOK.get());
                output.enchant(GuaniaoEnchantments.holder(registries, GuaniaoEnchantments.HUNTING_RETURN), 1);
                suffix = "hunting";
            }
            default -> throw new IllegalArgumentException("Unknown handbook anvil book: " + id);
        }
        return new HandbookAnvilRecipe(fan, book, output, "item/wind_feather_fan_" + suffix);
    }
}
