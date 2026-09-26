package EdDYON.guaniao.registry;

import EdDYON.guaniao.content.camera.FilmToPhotographRecipe;
import EdDYON.guaniao.content.dropping.BirdDroppingFoodRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Supplier;

public final class GuaniaoRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, "guaniao");
    public static final Supplier<RecipeSerializer<FilmToPhotographRecipe>> FILM_TO_PHOTOGRAPH = RECIPE_SERIALIZERS.register("film_to_photograph",
            () -> new SimpleCraftingRecipeSerializer<>(FilmToPhotographRecipe::new));
    public static final Supplier<RecipeSerializer<BirdDroppingFoodRecipe>> BIRD_DROPPING_FOOD = RECIPE_SERIALIZERS.register("bird_dropping_food",
            () -> new SimpleCraftingRecipeSerializer<>(BirdDroppingFoodRecipe::new));

    private GuaniaoRecipeSerializers() {
    }
}
