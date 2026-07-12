package EdDYON.guaniao.content.bird.columbid;

import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.brain.BirdBrain;
import EdDYON.guaniao.content.bird.brain.BirdSenses;
import EdDYON.guaniao.content.bird.brain.BirdSpeciesProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SpottedDoveProfile extends BirdSpeciesProfile {
    public static final SpottedDoveProfile INSTANCE = new SpottedDoveProfile();

    private SpottedDoveProfile() {
    }

    @Override
    public float baseBoldness() {
        return 0.46F;
    }

    @Override
    public float baseWariness() {
        return 0.48F;
    }

    @Override
    public float baseActivity() {
        return 0.58F;
    }

    @Override
    public float baseSociability() {
        return 0.62F;
    }

    @Override
    public float baseFlightiness() {
        return 0.38F;
    }

    @Override
    public boolean isActiveTime(BirdSenses senses) {
        return BirdActivitySchedule.DIURNAL.isActiveTime(senses.dayTime());
    }

    @Override
    public boolean isRoostTime(BirdSenses senses) {
        return !this.isActiveTime(senses);
    }

    @Override
    public boolean isPreferredPrey(LivingEntity entity) {
        return false;
    }

    @Override
    public boolean isTemptingPlayer(Player player) {
        return AbstractColumbidEntity.isSeedFood(player.getMainHandItem()) || AbstractColumbidEntity.isSeedFood(player.getOffhandItem());
    }

    @Override
    public boolean isNearCover(PathfinderMob bird) {
        return this.scanNearbyBlocks(bird, 7, 4, this::isCoverBlock);
    }

    @Override
    public boolean isNearRoost(PathfinderMob bird) {
        return this.scanNearbyBlocks(bird, 9, 6, this::isRoostBlock);
    }

    @Override
    public float computeComfort(BirdSenses senses) {
        float comfort = 0.36F;
        if (senses.nearCover()) {
            comfort += 0.23F;
        }
        if (senses.nearRoost()) {
            comfort += 0.20F;
        }
        if (senses.roostTime()) {
            comfort -= 0.12F;
        }
        if (senses.hasNearbyThreat()) {
            comfort -= 0.24F;
        }
        return this.clamp(comfort);
    }

    @Override
    public boolean wantsForage(BirdBrain brain) {
        BirdSenses senses = brain.senses();
        return senses.activeTime()
                && senses.isOnGround()
                && brain.motivation().hunger() > 0.30F
                && brain.motivation().fear() < 0.55F
                && brain.computeRiskScore() < 0.65F;
    }

    private boolean scanNearbyBlocks(PathfinderMob bird, int horizontalRadius, int verticalRadius, BlockPredicate predicate) {
        Level level = bird.level();
        BlockPos origin = bird.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-horizontalRadius, -1, -horizontalRadius), origin.offset(horizontalRadius, verticalRadius, horizontalRadius))) {
            if (predicate.test(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    private boolean isCoverBlock(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(Blocks.GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.SUNFLOWER)
                || state.is(Blocks.WHEAT)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.HAY_BLOCK)
                || state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof ComposterBlock;
    }

    private boolean isRoostBlock(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof FenceGateBlock
                || state.is(Blocks.HAY_BLOCK);
    }

    private interface BlockPredicate {
        boolean test(BlockState state);
    }
}
