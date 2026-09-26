package EdDYON.guaniao.registry;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bath.BirdBathBlockEntity;
import EdDYON.guaniao.content.cage.BirdCageBlockEntity;
import EdDYON.guaniao.content.nest.CrowNestBlockEntity;
import EdDYON.guaniao.content.food.BaggedFriesBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Supplier;

public final class GuaniaoBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, GuaniaoMod.MOD_ID);

    public static final Supplier<BlockEntityType<BirdCageBlockEntity>> BIRD_CAGE = BLOCK_ENTITY_TYPES.register("bird_cage", () ->
            BlockEntityType.Builder.of(BirdCageBlockEntity::new,
                    GuaniaoBlocks.SMALL_BIRD_CAGE.get(),
                    GuaniaoBlocks.MEDIUM_BIRD_CAGE.get(),
                    GuaniaoBlocks.LARGE_BIRD_CAGE.get()).build(null));

    public static final Supplier<BlockEntityType<BirdBathBlockEntity>> BIRD_BATH = BLOCK_ENTITY_TYPES.register("bird_bath", () ->
            BlockEntityType.Builder.of(BirdBathBlockEntity::new,
                    GuaniaoBlocks.WOODEN_BIRD_BATH.get(),
                    GuaniaoBlocks.STONE_BIRD_BATH.get(),
                    GuaniaoBlocks.BIRD_BATH.get(),
                    GuaniaoBlocks.WOODEN_BIRD_BATH_2.get(),
                    GuaniaoBlocks.STONE_BIRD_BATH_2.get(),
                    GuaniaoBlocks.BIRD_BATH_2.get()).build(null));

    public static final Supplier<BlockEntityType<CrowNestBlockEntity>> CROW_NEST = BLOCK_ENTITY_TYPES.register("crow_nest", () ->
            BlockEntityType.Builder.of(CrowNestBlockEntity::new, GuaniaoBlocks.CROW_NEST.get()).build(null));

    public static final Supplier<BlockEntityType<BaggedFriesBlockEntity>> BAGGED_FRIES = BLOCK_ENTITY_TYPES.register("bagged_fries", () ->
            BlockEntityType.Builder.of(BaggedFriesBlockEntity::new, GuaniaoBlocks.BAGGED_FRIES.get()).build(null));

    private GuaniaoBlockEntityTypes() {
    }
}
