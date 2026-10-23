package EdDYON.guaniao.content.bird;

import EdDYON.guaniao.content.dropping.PrankFoodUtil;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class BirdFoodSafety {
    private BirdFoodSafety() {
    }

    public static boolean isCleanFoodCandidate(ItemStack stack) {
        return !stack.isEmpty() && !PrankFoodUtil.isPrankFood(stack);
    }

    public static boolean isPollutedFood(ItemStack stack) {
        return !stack.isEmpty() && PrankFoodUtil.isPrankFood(stack);
    }

    public static boolean matchesClean(Ingredient ingredient, ItemStack stack) {
        return isCleanFoodCandidate(stack) && ingredient.test(stack);
    }

    public static boolean matchesClean(TagKey<Item> tag, ItemStack stack) {
        return isCleanFoodCandidate(stack) && !stack.is(BirdTags.BIRD_TOXIC_FOODS) && stack.is(tag);
    }

    public static boolean matchesDroppedFoodCandidate(Ingredient ingredient, ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack);
    }

    public static boolean matchesDroppedFoodCandidate(TagKey<Item> tag, ItemStack stack) {
        return !stack.isEmpty() && stack.is(tag);
    }
}
