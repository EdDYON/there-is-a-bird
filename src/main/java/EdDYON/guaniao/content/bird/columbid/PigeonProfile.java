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
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class PigeonProfile extends BirdSpeciesProfile {
    public static final PigeonProfile INSTANCE = new PigeonProfile();

    private PigeonProfile() {
    }

    @Override
    public float baseBoldness() {
        return 0.58F;
    }

    @Override
    public float baseWariness() {
        return 0.36F;
    }

    @Override
    public float baseActivity() {
        return 0.62F;
    }

    @Override
    public float baseSociability() {
        return 0.72F;
    }

    @Override
    public float baseFlightiness() {
        return 0.35F;
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
        float comfort = 0.38F;
        if (senses.nearCover()) {
            comfort += 0.20F;
        }
        if (senses.nearRoost()) {
            comfort += 0.20F;
        }
        if (senses.hasNearbyThreat()) {
            comfort -= 0.16F;
        }
        return this.clamp(comfort);
    }

    @Override
    public boolean wantsForage(BirdBrain brain) {
        BirdSenses senses = brain.senses();
        return senses.activeTime()
                && senses.isOnGround()
                && brain.motivation().hunger() > 0.32F
                && brain.motivation().fear() < 0.62F
                && brain.computeRiskScore() < 0.72F;
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
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.HAY_BLOCK)
                || state.is(Blocks.COMPOSTER)
                || state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof BedBlock
                || state.getBlock() instanceof ComposterBlock;
    }

    private boolean isRoostBlock(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof FenceGateBlock
                || state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof BedBlock
                || state.is(Blocks.HAY_BLOCK);
    }

    private interface BlockPredicate {
        boolean test(BlockState state);
    }
}
