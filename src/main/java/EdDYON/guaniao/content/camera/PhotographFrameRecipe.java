package EdDYON.guaniao.content.camera;

import EdDYON.guaniao.registry.GuaniaoItems;
import EdDYON.guaniao.registry.GuaniaoRecipeSerializers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/** A normal shaped recipe whose output retains the input photograph and its finish. */
public final class PhotographFrameRecipe extends ShapedRecipe {
    private final PhotographFrameStyle.Shape shape;

    private PhotographFrameRecipe(ShapedRecipe recipe, PhotographFrameStyle.Shape shape) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, preview(shape), recipe.showNotification());
        this.shape = shape;
    }

    private static ItemStack preview(PhotographFrameStyle.Shape shape) {
        ItemStack stack = new ItemStack(GuaniaoItems.PHOTOGRAPH.get());
        PhotographFrameStyle.write(stack, PhotographFrameStyle.read(stack).withShape(shape));
        return stack;
    }

    private ItemStack photograph(CraftingInput container) {
        ItemStack photograph = ItemStack.EMPTY;
        for (int slot = 0; slot < container.size(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.is(GuaniaoItems.PHOTOGRAPH.get())) continue;
            if (!photograph.isEmpty() || !PhotographData.hasImage(stack)) return ItemStack.EMPTY;
            photograph = stack;
        }
        return photograph;
    }

    @Override
    public boolean matches(@NotNull CraftingInput container, @NotNull Level level) {
        ItemStack photo = photograph(container);
        return !photo.isEmpty() && PhotographFrameStyle.read(photo).shape() != shape
                && super.matches(container, level);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput container, @NotNull HolderLookup.Provider access) {
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
        private static final Codec<PhotographFrameStyle.Shape> SHAPE_CODEC = Codec.STRING.comapFlatMap(id -> {
            for (PhotographFrameStyle.Shape shape : PhotographFrameStyle.Shape.values()) {
                if (shape.id().equals(id)) return DataResult.success(shape);
            }
            return DataResult.error(() -> "Unknown photograph frame shape: " + id);
        }, PhotographFrameStyle.Shape::id);
        private static final MapCodec<PhotographFrameRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ShapedRecipe.Serializer.CODEC.forGetter((PhotographFrameRecipe recipe) -> recipe),
                SHAPE_CODEC.fieldOf("frame_shape").forGetter(recipe -> recipe.shape)
        ).apply(instance, PhotographFrameRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, PhotographFrameRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {
                    ShapedRecipe.Serializer.STREAM_CODEC.encode(buffer, recipe);
                    buffer.writeUtf(recipe.shape.id(), 32);
                }, buffer -> {
                    ShapedRecipe recipe = ShapedRecipe.Serializer.STREAM_CODEC.decode(buffer);
                    String id = buffer.readUtf(32);
                    return new PhotographFrameRecipe(recipe, SHAPE_CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                            new com.google.gson.JsonPrimitive(id)).getOrThrow());
                });

        @Override
        public @NotNull MapCodec<PhotographFrameRecipe> codec() { return CODEC; }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, PhotographFrameRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
