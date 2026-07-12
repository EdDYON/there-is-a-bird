package EdDYON.guaniao.content.dropping;

import EdDYON.guaniao.registry.GuaniaoRecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class BirdDroppingFoodRecipe extends CustomRecipe {
    public BirdDroppingFoodRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, @NotNull Level level) {
        return !this.assemble(container, level.registryAccess()).isEmpty();
    }

    @Override
    public @NotNull ItemStack assemble(CraftingContainer container, @NotNull RegistryAccess registryAccess) {
        ItemStack food = ItemStack.EMPTY;
        ItemStack dropping = ItemStack.EMPTY;
        int occupiedSlots = 0;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            occupiedSlots++;
            if (PrankFoodUtil.isDropping(stack)) {
                if (!dropping.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                dropping = stack;
            } else if (PrankFoodUtil.isEligibleFood(stack)) {
                if (!food.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                food = stack;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (occupiedSlots != 2 || food.isEmpty() || dropping.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return PrankFoodUtil.makePrankFood(food, dropping);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return GuaniaoRecipeSerializers.BIRD_DROPPING_FOOD.get();
    }
}
