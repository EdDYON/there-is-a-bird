package EdDYON.guaniao.content.bird.species;

import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.brain.BirdBrain;
import EdDYON.guaniao.content.bird.brain.BirdSenses;
import EdDYON.guaniao.content.bird.brain.BirdSpeciesProfile;
import EdDYON.guaniao.content.bird.myna.MynaEntity;
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
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Active, social ground-foraging profile for crested mynas. */
public final class MynaProfile extends BirdSpeciesProfile {
    public static final MynaProfile INSTANCE = new MynaProfile();

    private MynaProfile() {
    }

    @Override
    public double playerSenseRadius() {
        return 20.0D;
    }

    @Override
    public float baseBoldness() {
        return 0.52F;
    }

    @Override
    public float baseWariness() {
        return 0.48F;
    }

    @Override
    public float baseActivity() {
        return 0.72F;
    }

    @Override
    public float baseSociability() {
        return 0.78F;
    }

    @Override
    public float baseFlightiness() {
        return 0.48F;
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
        return BirdFoodSafety.matchesClean(BirdTags.MYNA_FOODS, player.getMainHandItem())
                || BirdFoodSafety.matchesClean(BirdTags.MYNA_FOODS, player.getOffhandItem());
    }

    @Override
    public boolean isTemptingPlayer(PathfinderMob bird, Player player) {
        return bird instanceof MynaEntity myna
                && (myna.isFood(player.getMainHandItem()) || myna.isFood(player.getOffhandItem()));
    }

    @Override
    public boolean isNearCover(PathfinderMob bird) {
        return this.scanNearbyBlocks(bird, 6, 4, false);
    }

    @Override
    public boolean isNearRoost(PathfinderMob bird) {
        return this.scanNearbyBlocks(bird, 8, 6, true);
    }

    @Override
    public int habitatScanCost() {
        return 3;
    }

    @Override
    public float computeComfort(BirdSenses senses) {
        float comfort = 0.38F;
        if (senses.nearCover()) {
            comfort += 0.23F;
        }
        if (senses.nearRoost()) {
            comfort += 0.22F;
        }
        if (senses.hasNearbyThreat()) {
            comfort -= 0.27F;
        }
        return this.clamp(comfort);
    }

    @Override
    public boolean wantsForage(BirdBrain brain) {
        return brain.senses().activeTime()
                && brain.senses().isOnGround()
                && brain.motivation().hunger() > 0.34F
                && brain.motivation().fear() < 0.62F
                && brain.computeRiskScore() < 0.62F;
    }

    private boolean scanNearbyBlocks(PathfinderMob bird, int horizontalRadius, int verticalRadius, boolean roostOnly) {
        Level level = bird.level();
        BlockPos origin = bird.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-horizontalRadius, -1, -horizontalRadius),
                origin.offset(horizontalRadius, verticalRadius, horizontalRadius))) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)
                    || state.getBlock() instanceof FenceBlock
                    || state.getBlock() instanceof FenceGateBlock
                    || state.getBlock() instanceof WallBlock
                    || state.getBlock() instanceof SignBlock
                    || state.getBlock() instanceof StairBlock
                    || state.getBlock() instanceof SlabBlock) {
                return true;
            }
            if (roostOnly && (state.is(Blocks.HAY_BLOCK)
                    || state.getBlock() instanceof BedBlock
                    || state.getBlock() instanceof DoorBlock)) {
                return true;
            }
            if (!roostOnly && (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)
                    || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                    || state.is(Blocks.FARMLAND) || state.is(Blocks.HAY_BLOCK)
                    || state.is(Blocks.SWEET_BERRY_BUSH)
                    || state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)
                    || state.getBlock() instanceof CropBlock
                    || state.getBlock() instanceof ComposterBlock)) {
                return true;
            }
        }
        return false;
    }
}
