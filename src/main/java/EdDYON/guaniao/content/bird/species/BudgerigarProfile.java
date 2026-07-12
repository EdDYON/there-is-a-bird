package EdDYON.guaniao.content.bird.species;

import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.brain.BirdBrain;
import EdDYON.guaniao.content.bird.brain.BirdSenses;
import EdDYON.guaniao.content.bird.brain.BirdSpeciesProfile;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BudgerigarProfile extends BirdSpeciesProfile {
    public static final BudgerigarProfile INSTANCE = new BudgerigarProfile();

    private BudgerigarProfile() {
    }

    @Override
    public float baseBoldness() {
        return 0.82F;
    }

    @Override
    public float baseWariness() {
        return 0.16F;
    }

    @Override
    public float baseActivity() {
        return 0.76F;
    }

    @Override
    public float baseSociability() {
        return 0.88F;
    }

    @Override
    public float baseFlightiness() {
        return 0.24F;
    }

    @Override
    public float hungerGainPerTick(BirdSenses senses) {
        return senses.activeTime() ? 0.00022F : 0.00006F;
    }

    @Override
    public boolean isActiveTime(BirdSenses senses) {
        return BirdActivitySchedule.DIURNAL.isActiveTime(senses.dayTime());
    }

    @Override
    public boolean isRoostTime(BirdSenses senses) {
        return BirdActivitySchedule.DIURNAL.isRestTime(senses.dayTime());
    }

    @Override
    public boolean isPreferredPrey(LivingEntity entity) {
        return false;
    }

    @Override
    public LivingEntity findNearestPrey(PathfinderMob bird) {
        return null;
    }

    @Override
    public boolean isTemptingPlayer(Player player) {
        return BudgerigarEntity.isEdibleFood(player.getMainHandItem()) || BudgerigarEntity.isEdibleFood(player.getOffhandItem());
    }

    @Override
    public boolean isNearWater(PathfinderMob bird) {
        return false;
    }

    @Override
    public boolean isWaterEdge(PathfinderMob bird) {
        return false;
    }

    @Override
    public boolean isNearCover(PathfinderMob bird) {
        return this.scanNearbyBlocks(bird, 6, 3, this::isComfortBlock);
    }

    @Override
    public boolean isNearRoost(PathfinderMob bird) {
        return this.scanNearbyBlocks(bird, 7, 5, this::isRoostBlock);
    }

    @Override
    public float computeComfort(BirdSenses senses) {
        float comfort = 0.36F;
        if (senses.nearCover()) {
            comfort += 0.24F;
        }
        if (senses.nearRoost()) {
            comfort += 0.18F;
        }
        if (senses.activeTime()) {
            comfort += 0.08F;
        }
        if (senses.roostTime()) {
            comfort -= 0.14F;
        }
        if (senses.temptingPlayerNearby() && senses.nearestPlayerDistance() > 3.0D) {
            comfort += 0.05F;
        }
        if (senses.hasNearbyThreat()) {
            comfort -= 0.06F;
        }
        return this.clamp(comfort);
    }

    @Override
    public boolean wantsForage(BirdBrain brain) {
        BirdSenses senses = brain.senses();
        if (brain.bird() instanceof BudgerigarEntity budgerigar && budgerigar.isBusyWithMusicOrSleep()) {
            return false;
        }
        return senses.activeTime()
                && senses.isOnGround()
                && brain.motivation().hunger() > 0.30F
                && brain.motivation().fear() < 0.72F
                && brain.computeRiskScore() < 0.78F;
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

    private boolean isComfortBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(Blocks.HAY_BLOCK)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof BushBlock;
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
