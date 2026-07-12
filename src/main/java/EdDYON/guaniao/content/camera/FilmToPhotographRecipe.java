package EdDYON.guaniao.content.camera;

import EdDYON.guaniao.registry.GuaniaoItems;
import EdDYON.guaniao.registry.GuaniaoRecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class FilmToPhotographRecipe extends CustomRecipe {
    public FilmToPhotographRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, @NotNull Level level) {
        return container.getWidth() == 3 && container.getHeight() == 3 && !this.findFilm(container).isEmpty();
    }

    @Override
    public @NotNull ItemStack assemble(CraftingContainer container, @NotNull RegistryAccess registryAccess) {
        ItemStack film = this.findFilm(container);
        if (film.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(GuaniaoItems.PHOTOGRAPH.get());
        PhotographData.copyImage(film, result);
        if (film.hasCustomHoverName()) {
            result.setHoverName(Component.translatable("item.guaniao.photograph.named", film.getHoverName()));
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return new ItemStack(GuaniaoItems.PHOTOGRAPH.get());
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return GuaniaoRecipeSerializers.FILM_TO_PHOTOGRAPH.get();
    }

    private ItemStack findFilm(CraftingContainer container) {
        if (container.getWidth() != 3 || container.getHeight() != 3) {
            return ItemStack.EMPTY;
        }

        ItemStack film = ItemStack.EMPTY;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (slot == 4) {
                if (!stack.is(GuaniaoItems.FILM.get()) || !PhotographData.hasImage(stack)) {
                    return ItemStack.EMPTY;
                }
                film = stack;
            } else if (!stack.is(Items.STICK)) {
                return ItemStack.EMPTY;
            }
        }
        return film;
    }
}
