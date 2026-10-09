package EdDYON.guaniao.content.bird.species;

import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.brain.BirdBrain;
import EdDYON.guaniao.content.bird.brain.BirdSenses;
import EdDYON.guaniao.content.bird.brain.BirdSpeciesProfile;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SparrowProfile extends BirdSpeciesProfile {
    public static final SparrowProfile INSTANCE = new SparrowProfile();

    private SparrowProfile() {
    }

    @Override
    public float baseBoldness() {
        return 0.42F;
    }

    @Override
    public float baseWariness() {
        return 0.55F;
    }

    @Override
    public float baseActivity() {
        return 0.68F;
    }

    @Override
    public float baseSociability() {
        return 0.72F;
    }

    @Override
    public float baseFlightiness() {
        return 0.52F;
    }

    @Override
    public float hungerGainPerTick(BirdSenses senses) {
        return senses.activeTime() ? 0.0002F : 0.00007F;
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
    public LivingEntity findNearestPrey(PathfinderMob bird) {
        return null;
    }

    @Override
    public boolean isTemptingPlayer(Player player) {
        return BirdFoodSafety.matchesClean(BirdTags.SPARROW_FOODS, player.getMainHandItem())
                || BirdFoodSafety.matchesClean(BirdTags.SPARROW_FOODS, player.getOffhandItem());
    }

    @Override
    public boolean isTemptingPlayer(PathfinderMob bird, Player player) {
        return bird instanceof SparrowEntity sparrow
                && (sparrow.isFood(player.getMainHandItem()) || sparrow.isFood(player.getOffhandItem()));
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
        return this.scanNearbyBlocks(bird, 5, 3, this::isCoverBlock);
    }

    @Override
    public boolean isNearRoost(PathfinderMob bird) {
        return this.scanNearbyBlocks(bird, 7, 5, this::isRoostBlock);
    }

    @Override
    public int habitatScanCost() {
        return 3;
    }

    @Override
    public float computeComfort(BirdSenses senses) {
        float comfort = 0.34F;
        if (senses.nearCover()) {
            comfort += 0.24F;
        }
        if (senses.nearRoost()) {
            comfort += 0.22F;
        }
        if (senses.roostTime()) {
            comfort -= 0.16F;
        }
        if (senses.hasNearbyThreat()) {
            comfort -= 0.26F;
        }
        return this.clamp(comfort);
    }

    @Override
    public boolean wantsForage(BirdBrain brain) {
        BirdSenses senses = brain.senses();
        return senses.activeTime()
                && senses.isOnGround()
                && brain.motivation().hunger() > 0.35F
                && brain.motivation().fear() < 0.55F
                && brain.motivation().fatigue() < 0.85F
                && brain.computeRiskScore() < 0.60F;
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
                || state.is(Blocks.LARGE_FERN)
                || state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof FenceBlock
                || state.is(Blocks.HAY_BLOCK)
                || state.getBlock() instanceof ComposterBlock;
    }

    private boolean isRoostBlock(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof FenceGateBlock
                || state.is(Blocks.HAY_BLOCK)
                || state.getBlock() instanceof BedBlock
                || state.getBlock() instanceof DoorBlock;
    }

    private interface BlockPredicate {
        boolean test(BlockState state);
    }
}
