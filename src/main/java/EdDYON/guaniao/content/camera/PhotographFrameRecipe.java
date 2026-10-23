package EdDYON.guaniao.content.camera;

import EdDYON.guaniao.registry.GuaniaoItems;
import EdDYON.guaniao.registry.GuaniaoRecipeSerializers;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/** A normal shaped recipe whose output retains the input photograph and its finish. */
public final class PhotographFrameRecipe extends ShapedRecipe {
    private final PhotographFrameStyle.Shape shape;

    private PhotographFrameRecipe(ShapedRecipe recipe, PhotographFrameStyle.Shape shape) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(),
                recipe.getIngredients(), preview(shape), recipe.showNotification());
        this.shape = shape;
    }

    private static ItemStack preview(PhotographFrameStyle.Shape shape) {
        ItemStack stack = new ItemStack(GuaniaoItems.PHOTOGRAPH.get());
        PhotographFrameStyle.write(stack, PhotographFrameStyle.read(stack).withShape(shape));
        return stack;
    }

    private ItemStack photograph(CraftingContainer container) {
        ItemStack photograph = ItemStack.EMPTY;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.is(GuaniaoItems.PHOTOGRAPH.get())) continue;
            if (!photograph.isEmpty() || !PhotographData.hasImage(stack)) return ItemStack.EMPTY;
            photograph = stack;
        }
        return photograph;
    }

    @Override
    public boolean matches(@NotNull CraftingContainer container, @NotNull Level level) {
        ItemStack photo = photograph(container);
        return !photo.isEmpty() && PhotographFrameStyle.read(photo).shape() != shape
                && super.matches(container, level);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer container, @NotNull RegistryAccess access) {
        ItemStack source = photograph(container);
        if (source.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = source.copy();
        result.setCount(1);
        PhotographFrameStyle.write(result, PhotographFrameStyle.read(source).withShape(shape));
        return result;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return GuaniaoRecipeSerializers.PHOTOGRAPH_FRAME.get();
    }

    public static final class Serializer implements RecipeSerializer<PhotographFrameRecipe> {
        private final ShapedRecipe.Serializer shaped = new ShapedRecipe.Serializer();

        private static PhotographFrameStyle.Shape shape(String id) {
            for (PhotographFrameStyle.Shape shape : PhotographFrameStyle.Shape.values()) {
                if (shape.id().equals(id)) return shape;
            }
            throw new JsonSyntaxException("Unknown photograph frame shape: " + id);
        }

        @Override
        public @NotNull PhotographFrameRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            return new PhotographFrameRecipe(shaped.fromJson(id, json), shape(GsonHelper.getAsString(json, "frame_shape")));
        }

        @Override
        public @NotNull PhotographFrameRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buffer) {
            ShapedRecipe recipe = shaped.fromNetwork(id, buffer);
            return new PhotographFrameRecipe(recipe, shape(buffer.readUtf(32)));
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull PhotographFrameRecipe recipe) {
            shaped.toNetwork(buffer, recipe);
            buffer.writeUtf(recipe.shape.id(), 32);
        }
    }
}
