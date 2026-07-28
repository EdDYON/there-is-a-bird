package EdDYON.guaniao.content.bird;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Data-driven bird behaviour tags. Keeping these keys in one place makes it
 * possible for datapacks to extend diets and interaction targets safely.
 */
public final class BirdTags {
    public static final TagKey<Item> SPARROW_FOODS = item("foods/sparrow");
    public static final TagKey<Item> LONG_TAILED_TIT_FOODS = item("foods/long_tailed_tit");
    public static final TagKey<Item> BUDGERIGAR_FOODS = item("foods/budgerigar");
    public static final TagKey<Item> COCKATIEL_FOODS = item("foods/cockatiel");
    public static final TagKey<Item> MACAW_FOODS = item("foods/macaw");
    public static final TagKey<Item> PIGEON_FOODS = item("foods/pigeon");
    public static final TagKey<Item> SPOTTED_DOVE_FOODS = item("foods/spotted_dove");
    public static final TagKey<Item> CROW_FOODS = item("foods/crow");
    public static final TagKey<Item> SEAGULL_FOODS = item("foods/seagull");
    public static final TagKey<Item> NIGHT_HERON_FOODS = item("foods/night_heron");

    public static final TagKey<Item> SEAGULL_STEALABLE_FOODS = item("seagull_stealable_foods");
    public static final TagKey<Item> CROW_SHINY_ITEMS = item("crow_shiny_items");
    public static final TagKey<Item> CROW_TREASURE_ITEMS = item("crow_treasure_items");
    public static final TagKey<Item> CROW_PROTECTED_ITEMS = item("crow_protected_items");
    public static final TagKey<Item> BIRD_TOXIC_FOODS = item("bird_toxic_foods");
    public static final TagKey<Item> FORGE_ORES = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "ores"));

    public static final TagKey<Block> BIRD_PERCHES = block("bird_perches");
    public static final TagKey<EntityType<?>> NIGHT_HERON_PREY = entityType("night_heron_prey");

    private BirdTags() {
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(GuaniaoMod.MOD_ID, path));
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(GuaniaoMod.MOD_ID, path));
    }

    private static TagKey<EntityType<?>> entityType(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(GuaniaoMod.MOD_ID, path));
    }
}
