package EdDYON.guaniao.world;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.enchantment.FeatherFanEnchantmentBooks;
import EdDYON.guaniao.content.nest.CrowNestBlock;
import EdDYON.guaniao.content.nest.CrowNestBlockEntity;
import EdDYON.guaniao.registry.GuaniaoBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.function.BiPredicate;
import java.util.function.IntBinaryOperator;

/** Places a sparse crow nest with guaranteed starter loot on a dense, open tree canopy. */
public final class CrowNestFeature extends Feature<NoneFeatureConfiguration> {
    private static final double BASE_CHANCE_PER_SUITABLE_CHUNK = 1.0D / 8.0D;
    private static final int TREE_SCAN_RADIUS = 12;
    private static final int TREE_SCAN_ATTEMPTS = 36;
    private static final int RUNTIME_TREE_SCAN_RADIUS = 18;
    private static final int RUNTIME_TREE_SCAN_ATTEMPTS = 40;
    private static final int ENCHANTMENT_BOOK_CHANCE = 12;

    public CrowNestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!BirdConfigManager.naturalCrowNests()) {
            return false;
        }
        RandomSource random = context.random();
        double chance = Math.min(1.0D, BASE_CHANCE_PER_SUITABLE_CHUNK * BirdConfigManager.crowNestGenerationMultiplier());
        if (chance <= 0.0D || random.nextDouble() >= chance) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos nestPos = findNestPosition(
                level,
                level.getMinBuildHeight(),
                level.getMaxBuildHeight(),
                context.origin(),
                random,
                TREE_SCAN_RADIUS,
                TREE_SCAN_ATTEMPTS,
                (x, z) -> level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z),
                (x, z) -> true
        );
        if (nestPos == null) {
            return false;
        }

        return placeNaturalNest(level, nestPos, random, true);
    }

    /** Lets a wild crow backfill a nest in an already generated, currently loaded area. */
    public static boolean tryPlaceRuntimeNest(ServerLevel level, BlockPos origin, RandomSource random) {
        if (!BirdConfigManager.naturalCrowNests() || BirdConfigManager.crowNestGenerationMultiplier() <= 0.0D) {
            return false;
        }
        BlockPos nestPos = findNestPosition(
                level,
                level.getMinBuildHeight(),
                level.getMaxBuildHeight(),
                origin,
                random,
                RUNTIME_TREE_SCAN_RADIUS,
                RUNTIME_TREE_SCAN_ATTEMPTS,
                (x, z) -> level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z),
                (x, z) -> isRuntimeColumnLoaded(level, x, z)
        );
        if (nestPos == null || !level.canSeeSky(nestPos)) {
            return false;
        }
        return placeNaturalNest(level, nestPos, random, false);
    }

    private static boolean placeNaturalNest(LevelAccessor level, BlockPos nestPos, RandomSource random, boolean allowEggs) {
        BlockState nestState = GuaniaoBlocks.CROW_NEST.get().defaultBlockState().setValue(CrowNestBlock.NATURAL_NEST, true);
        if (allowEggs && random.nextInt(10) == 0) {
            nestState = nestState.setValue(CrowNestBlock.EGGS, 1 + random.nextInt(2));
        }
        if (!level.setBlock(nestPos, nestState, 2)) {
            return false;
        }
        if (level.getBlockEntity(nestPos) instanceof CrowNestBlockEntity nest) {
            nest.setNaturalNest(true);
            addNaturalNestContents(nest, random);
        }
        return true;
    }

    private static BlockPos findNestPosition(BlockGetter level, int minBuildHeight, int maxBuildHeight,
                                             BlockPos origin, RandomSource random, int scanRadius, int scanAttempts,
                                             IntBinaryOperator surfaceHeight,
                                             BiPredicate<Integer, Integer> columnAvailable) {
        BlockPos.MutableBlockPos supportPos = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < scanAttempts; ++attempt) {
            int x = origin.getX() + random.nextInt(scanRadius * 2 + 1) - scanRadius;
            int z = origin.getZ() + random.nextInt(scanRadius * 2 + 1) - scanRadius;
            if (!columnAvailable.test(x, z)) {
                continue;
            }
            int topY = surfaceHeight.applyAsInt(x, z) - 1;
            int bottomY = Math.max(minBuildHeight + 2, topY - 20);
            for (int y = topY; y >= bottomY; --y) {
                supportPos.set(x, y, z);
                BlockState support = level.getBlockState(supportPos);
                if ((!support.is(BlockTags.LEAVES) && !support.is(BlockTags.LOGS))
                        || !hasDenseTreeCanopy(level, supportPos)) {
                    continue;
                }
                BlockPos nestPos = supportPos.above();
                if (nestPos.getY() >= maxBuildHeight - 2
                        || !level.getBlockState(nestPos).isAir()
                        || !level.getBlockState(nestPos.above()).isAir()) {
                    continue;
                }
                return nestPos.immutable();
            }
        }
        return null;
    }

    private static boolean isRuntimeColumnLoaded(ServerLevel level, int x, int z) {
        return level.hasChunk(x >> 4, z >> 4)
                && level.hasChunk((x - 2) >> 4, (z - 2) >> 4)
                && level.hasChunk((x - 2) >> 4, (z + 2) >> 4)
                && level.hasChunk((x + 2) >> 4, (z - 2) >> 4)
                && level.hasChunk((x + 2) >> 4, (z + 2) >> 4);
    }

    private static boolean hasDenseTreeCanopy(BlockGetter level, BlockPos anchor) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int treeBlocks = 0;
        for (int xOffset = -2; xOffset <= 2; ++xOffset) {
            for (int zOffset = -2; zOffset <= 2; ++zOffset) {
                for (int yOffset = -1; yOffset <= 2; ++yOffset) {
                    cursor.set(anchor.getX() + xOffset, anchor.getY() + yOffset, anchor.getZ() + zOffset);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                        if (++treeBlocks >= 7) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void addNaturalNestContents(CrowNestBlockEntity nest, RandomSource random) {
        int count = 1 + random.nextInt(3);
        for (int slot = 0; slot < count; ++slot) {
            nest.setItem(slot, randomNaturalNestItem(random));
        }
        if (random.nextInt(ENCHANTMENT_BOOK_CHANCE) == 0) {
            nest.setItem(0, FeatherFanEnchantmentBooks.randomBook(random));
        }
        if (!nest.hasTreasure()) {
            nest.setItem(0, new ItemStack(Items.FEATHER));
        }
    }

    private static ItemStack randomNaturalNestItem(RandomSource random) {
        if (random.nextInt(40) == 0) {
            return randomPristineGoldenEquipment(random);
        }
        return switch (random.nextInt(16)) {
            case 0 -> new ItemStack(Items.FEATHER, 1 + random.nextInt(3));
            case 1 -> new ItemStack(Items.STICK, 1 + random.nextInt(2));
            case 2 -> new ItemStack(Items.STRING);
            case 3 -> new ItemStack(Items.FLINT);
            case 4 -> new ItemStack(Items.COAL);
            case 5 -> new ItemStack(Items.IRON_NUGGET, 1 + random.nextInt(2));
            case 6 -> new ItemStack(Items.GOLD_NUGGET);
            case 7 -> new ItemStack(Items.BONE);
            case 8 -> new ItemStack(Items.RAW_COPPER);
            case 9 -> new ItemStack(Items.RAW_IRON);
            case 10 -> new ItemStack(Items.RAW_GOLD);
            case 11 -> new ItemStack(Items.QUARTZ);
            case 12 -> new ItemStack(Items.AMETHYST_SHARD);
            case 13 -> new ItemStack(Items.WHEAT_SEEDS, 1 + random.nextInt(3));
            case 14 -> new ItemStack(Items.APPLE);
            default -> new ItemStack(Items.BREAD);
        };
    }

    private static ItemStack randomPristineGoldenEquipment(RandomSource random) {
        return switch (random.nextInt(10)) {
            case 0 -> new ItemStack(Items.GOLDEN_HELMET);
            case 1 -> new ItemStack(Items.GOLDEN_CHESTPLATE);
            case 2 -> new ItemStack(Items.GOLDEN_LEGGINGS);
            case 3 -> new ItemStack(Items.GOLDEN_BOOTS);
            case 4 -> new ItemStack(Items.GOLDEN_SWORD);
            case 5 -> new ItemStack(Items.GOLDEN_PICKAXE);
            case 6 -> new ItemStack(Items.GOLDEN_AXE);
            case 7 -> new ItemStack(Items.GOLDEN_SHOVEL);
            case 8 -> new ItemStack(Items.GOLDEN_HOE);
            default -> new ItemStack(Items.GOLDEN_HORSE_ARMOR);
        };
    }
}
