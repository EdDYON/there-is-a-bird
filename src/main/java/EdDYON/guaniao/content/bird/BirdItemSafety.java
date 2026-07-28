package EdDYON.guaniao.content.bird;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    /**
     * Dropped items a crow may carry to a nest. Named, modified, protected and
     * container-like stacks remain excluded; pristine golden gear is the one
     * deliberate non-stackable exception.
     */
    public static boolean isCrowTreasure(ItemStack stack) {
        if (stack.isEmpty()
                || stack.hasCustomHoverName()
                || stack.hasTag()
                || stack.is(BirdTags.CROW_PROTECTED_ITEMS)
                || stack.is(BirdTags.BIRD_TOXIC_FOODS)
                || BirdFoodSafety.isPollutedFood(stack)) {
            return false;
        }
        if (isPristineGoldenEquipment(stack)) {
            return true;
        }
        boolean cleanFood = stack.isEdible() || stack.is(BirdTags.CROW_FOODS);
        if (stack.is(BirdTags.FORGE_ORES)) {
            return isPlainCarryableItem(stack);
        }
        if (cleanFood) {
            Rarity rarity = stack.getRarity();
            return isPlainCarryableItem(stack) && rarity != Rarity.RARE && rarity != Rarity.EPIC;
        }
        return isSafeDisposableItem(stack)
                && stack.is(BirdTags.CROW_TREASURE_ITEMS);
    }

    private static boolean isPlainCarryableItem(ItemStack stack) {
        return !(stack.getItem() instanceof MapItem)
                && (!(stack.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof EntityBlock));
    }

    private static boolean isPristineGoldenEquipment(ItemStack stack) {
        return stack.is(Items.GOLDEN_HELMET)
                || stack.is(Items.GOLDEN_CHESTPLATE)
                || stack.is(Items.GOLDEN_LEGGINGS)
                || stack.is(Items.GOLDEN_BOOTS)
                || stack.is(Items.GOLDEN_SWORD)
                || stack.is(Items.GOLDEN_PICKAXE)
                || stack.is(Items.GOLDEN_AXE)
                || stack.is(Items.GOLDEN_SHOVEL)
                || stack.is(Items.GOLDEN_HOE)
                || stack.is(Items.GOLDEN_HORSE_ARMOR);
    }
}
