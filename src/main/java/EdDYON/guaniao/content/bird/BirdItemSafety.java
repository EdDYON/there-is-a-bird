package EdDYON.guaniao.content.bird;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.EntityBlock;

/** Server-authoritative guardrails for items picked up or stolen by birds. */
public final class BirdItemSafety {
    private BirdItemSafety() {
    }

    public static boolean isSafeDisposableItem(ItemStack stack) {
        if (stack.isEmpty() || stack.hasCustomHoverName() || stack.hasTag()) {
            return false;
        }
        if (stack.getMaxStackSize() <= 1 || stack.getItem() instanceof MapItem) {
            return false;
        }
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof EntityBlock) {
            return false;
        }
        Rarity rarity = stack.getRarity();
        return rarity != Rarity.RARE && rarity != Rarity.EPIC;
    }

    public static boolean isSeagullStealableFood(ItemStack stack) {
        return isSafeDisposableItem(stack)
                && BirdFoodSafety.matchesClean(BirdTags.SEAGULL_STEALABLE_FOODS, stack)
                && !stack.is(BirdTags.BIRD_TOXIC_FOODS);
    }

    public static boolean isCrowShiny(ItemStack stack) {
        return isSafeDisposableItem(stack)
                && stack.is(BirdTags.CROW_SHINY_ITEMS)
                && !stack.is(BirdTags.CROW_PROTECTED_ITEMS);
    }
}
