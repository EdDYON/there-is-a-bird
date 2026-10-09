package EdDYON.guaniao.content.enchantment;

import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** Shared selection and identification for the three feather-fan enchantment books. */
public final class FeatherFanEnchantmentBooks {
    private FeatherFanEnchantmentBooks() {
    }

    public static ItemStack randomBook(RandomSource random) {
        return switch (random.nextInt(3)) {
            case 0 -> new ItemStack(GuaniaoItems.BURIAL_PLUME_BOOK.get());
            case 1 -> new ItemStack(GuaniaoItems.RIVEN_PLUME_BOOK.get());
            default -> new ItemStack(GuaniaoItems.HUNTING_RETURN_BOOK.get());
        };
    }

    public static boolean isBook(ItemStack stack) {
        return stack.is(GuaniaoItems.BURIAL_PLUME_BOOK.get())
                || stack.is(GuaniaoItems.RIVEN_PLUME_BOOK.get())
                || stack.is(GuaniaoItems.HUNTING_RETURN_BOOK.get());
    }
}
