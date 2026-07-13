package EdDYON.guaniao.world;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.nest.CrowNestBlock;
import EdDYON.guaniao.content.nest.CrowNestBlockEntity;
import EdDYON.guaniao.registry.GuaniaoBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Places a rare, empty-by-default crow nest on a dense, open tree canopy. */
public final class CrowNestFeature extends Feature<NoneFeatureConfiguration> {
    private static final double BASE_CHANCE_PER_SUITABLE_CHUNK = 1.0D / 14.0D;
    private static final int TREE_SCAN_RADIUS = 8;
    private static final int TREE_SCAN_ATTEMPTS = 24;

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
        BlockPos nestPos = this.findNestPosition(level, context.origin(), random);
        if (nestPos == null) {
            return false;
        }

        BlockState nestState = GuaniaoBlocks.CROW_NEST.get().defaultBlockState().setValue(CrowNestBlock.NATURAL_NEST, true);
        if (random.nextInt(12) == 0) {
            nestState = nestState.setValue(CrowNestBlock.EGGS, 1 + random.nextInt(2));
        }
        if (!level.setBlock(nestPos, nestState, 2)) {
            return false;
        }
        if (level.getBlockEntity(nestPos) instanceof CrowNestBlockEntity nest) {
            nest.setNaturalNest(true);
            if (random.nextInt(10) == 0) {
                this.addAbandonedNestContents(nest, random);
            }
        }
        return true;
    }

    private BlockPos findNestPosition(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos.MutableBlockPos supportPos = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < TREE_SCAN_ATTEMPTS; ++attempt) {
            int x = origin.getX() + random.nextInt(TREE_SCAN_RADIUS * 2 + 1) - TREE_SCAN_RADIUS;
            int z = origin.getZ() + random.nextInt(TREE_SCAN_RADIUS * 2 + 1) - TREE_SCAN_RADIUS;
            int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            int bottomY = Math.max(level.getMinBuildHeight() + 2, topY - 18);
            for (int y = topY; y >= bottomY; --y) {
                supportPos.set(x, y, z);
                BlockState support = level.getBlockState(supportPos);
                if ((!support.is(BlockTags.LEAVES) && !support.is(BlockTags.LOGS))
                        || !this.hasDenseTreeCanopy(level, supportPos)) {
                    continue;
                }
                BlockPos nestPos = supportPos.above();
                if (nestPos.getY() >= level.getMaxBuildHeight() - 2
                        || !level.isEmptyBlock(nestPos)
                        || !level.isEmptyBlock(nestPos.above())) {
                    continue;
                }
                return nestPos.immutable();
            }
        }
        return null;
    }

    private boolean hasDenseTreeCanopy(WorldGenLevel level, BlockPos anchor) {
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

    private void addAbandonedNestContents(CrowNestBlockEntity nest, RandomSource random) {
        int count = 1 + random.nextInt(2);
        for (int slot = 0; slot < count; ++slot) {
            nest.setItem(slot, this.randomAbandonedItem(random));
        }
    }

    private ItemStack randomAbandonedItem(RandomSource random) {
        return switch (random.nextInt(8)) {
            case 0 -> new ItemStack(Items.FEATHER, 1 + random.nextInt(3));
            case 1 -> new ItemStack(Items.STICK, 1 + random.nextInt(2));
            case 2 -> new ItemStack(Items.STRING);
            case 3 -> new ItemStack(Items.FLINT);
            case 4 -> new ItemStack(Items.COAL);
            case 5 -> new ItemStack(Items.IRON_NUGGET, 1 + random.nextInt(2));
            case 6 -> new ItemStack(Items.GOLD_NUGGET);
            default -> new ItemStack(Items.BONE);
        };
    }
}
