package EdDYON.guaniao.registry;

import EdDYON.guaniao.content.bath.BirdBathBlock;
import EdDYON.guaniao.content.bath.BirdBathVariant;
import EdDYON.guaniao.content.dropping.BirdDroppingStainBlock;
import EdDYON.guaniao.content.feed.BreadcrumbPileBlock;
import EdDYON.guaniao.content.food.BaggedFriesBlock;
import EdDYON.guaniao.content.cage.BirdCageBlock;
import EdDYON.guaniao.content.cage.BirdCageVariant;
import EdDYON.guaniao.content.nest.CrowNestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Supplier;

public final class GuaniaoBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, "guaniao");
    public static final Supplier<Block> BREADCRUMBS = BLOCKS.register("breadcrumbs", () ->
            new BreadcrumbPileBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.SAND)
                    .randomTicks()));
    public static final Supplier<Block> BAGGED_FRIES = BLOCKS.register("bagged_fries", () ->
            new BaggedFriesBlock(BlockBehaviour.Properties.of()
                    .strength(0.2F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));
    public static final Supplier<Block> SMALL_BIRD_CAGE = registerBirdCage(BirdCageVariant.SMALL);
    public static final Supplier<Block> MEDIUM_BIRD_CAGE = registerBirdCage(BirdCageVariant.MEDIUM);
    public static final Supplier<Block> LARGE_BIRD_CAGE = registerBirdCage(BirdCageVariant.LARGE);
    public static final Supplier<Block> WOODEN_BIRD_BATH = registerBirdBath(BirdBathVariant.WOODEN_BIRD_BATH);
    public static final Supplier<Block> STONE_BIRD_BATH = registerBirdBath(BirdBathVariant.STONE_BIRD_BATH);
    public static final Supplier<Block> BIRD_BATH = registerBirdBath(BirdBathVariant.BIRD_BATH);
    public static final Supplier<Block> WOODEN_BIRD_BATH_2 = registerBirdBath(BirdBathVariant.WOODEN_BIRD_BATH_2);
    public static final Supplier<Block> STONE_BIRD_BATH_2 = registerBirdBath(BirdBathVariant.STONE_BIRD_BATH_2);
    public static final Supplier<Block> BIRD_BATH_2 = registerBirdBath(BirdBathVariant.BIRD_BATH_2);
    public static final Supplier<Block> CROW_NEST = BLOCKS.register("crow_nest", () ->
            new CrowNestBlock(BlockBehaviour.Properties.of()
                    .strength(0.7F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()));
    public static final Supplier<Block> BIRD_DROPPING_STAIN_LIGHT = BLOCKS.register("bird_dropping_stain_light", () ->
            new BirdDroppingStainBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.SLIME_BLOCK)
                    .randomTicks()
                    .noOcclusion()));
    public static final Supplier<Block> BIRD_DROPPING_STAIN_DARK = BLOCKS.register("bird_dropping_stain_dark", () ->
            new BirdDroppingStainBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.SLIME_BLOCK)
                    .randomTicks()
                    .noOcclusion()));

    private GuaniaoBlocks() {
    }

    private static Supplier<Block> registerBirdCage(BirdCageVariant variant) {
        return BLOCKS.register(variant.id(), () -> new BirdCageBlock(variant, BlockBehaviour.Properties.of()
                .strength(1.5f)
                .sound(SoundType.WOOD)
                .noOcclusion()));
    }

    private static Supplier<Block> registerBirdBath(BirdBathVariant variant) {
        return BLOCKS.register(variant.id(), () -> new BirdBathBlock(variant, BlockBehaviour.Properties.of()
                .strength(1.8F)
                .sound(variant.soundType())
                .randomTicks()
                .noOcclusion()));
    }
}
