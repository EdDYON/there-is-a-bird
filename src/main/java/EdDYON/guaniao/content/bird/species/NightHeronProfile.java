package EdDYON.guaniao.content.bird.species;

import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.brain.BirdBrain;
import EdDYON.guaniao.content.bird.brain.BirdHabitatSnapshot;
import EdDYON.guaniao.content.bird.brain.BirdSenses;
import EdDYON.guaniao.content.bird.brain.BirdSpeciesProfile;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class NightHeronProfile extends BirdSpeciesProfile {
    public static final NightHeronProfile INSTANCE = new NightHeronProfile();

    private NightHeronProfile() {
    }

    @Override
    public float baseBoldness() {
        return 0.34F;
    }

    @Override
    public float baseWariness() {
        return 0.68F;
    }

    @Override
    public float baseActivity() {
        return 0.52F;
    }

    @Override
    public float baseSociability() {
        return 0.38F;
    }

    @Override
    public float baseFlightiness() {
        return 0.58F;
    }

    @Override
    public boolean isActiveTime(BirdSenses senses) {
        return BirdActivitySchedule.NOCTURNAL_CREPUSCULAR.isActiveTime(senses.dayTime());
    }

    @Override
    public boolean isRoostTime(BirdSenses senses) {
        return BirdActivitySchedule.NOCTURNAL_CREPUSCULAR.isRestTime(senses.dayTime());
    }

    @Override
    public boolean isPreferredPrey(LivingEntity entity) {
        return entity != null && entity.getType().is(BirdTags.NIGHT_HERON_PREY);
    }

    @Override
    public LivingEntity findNearestPrey(PathfinderMob bird) {
        if (!(bird.level() instanceof ServerLevel serverLevel)
                || !BirdScanBudget.tryAcquire(serverLevel, bird)) {
            return null;
        }
        return bird.level().getEntitiesOfClass(
                        LivingEntity.class,
                        bird.getBoundingBox().inflate(8.0D),
                        entity -> entity.isAlive() && this.isPreferredPrey(entity)
                ).stream()
                .min((a, b) -> Double.compare(bird.distanceToSqr(a), bird.distanceToSqr(b)))
                .orElse(null);
    }

    @Override
    public boolean isTemptingPlayer(Player player) {
        return NightHeronEntity.isEdibleFishItem(player.getMainHandItem())
                || NightHeronEntity.isEdibleFishItem(player.getOffhandItem());
    }

    @Override
    public boolean isTemptingPlayer(PathfinderMob bird, Player player) {
        return NightHeronEntity.isEdibleFishItem(player.getMainHandItem())
                || NightHeronEntity.isEdibleFishItem(player.getOffhandItem());
    }

    @Override
    public boolean isNearWater(PathfinderMob bird) {
        return this.scanForWater(bird, 6);
    }

    @Override
    public boolean isWaterEdge(PathfinderMob bird) {
        return this.scanForWater(bird, 3) && this.scanForDryGround(bird, 2);
    }

    @Override
    public boolean isNearCover(PathfinderMob bird) {
        Level level = bird.level();
        BlockPos origin = bird.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -1, -4), origin.offset(4, 3, 4))) {
            if (this.isCoverBlock(level.getBlockState(pos))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isNearRoost(PathfinderMob bird) {
        return this.isNearCover(bird);
    }

    @Override
    public int habitatScanCost() {
        return 3;
    }

    @Override
    public BirdHabitatSnapshot scanHabitat(PathfinderMob bird) {
        boolean nearWater = this.scanForWater(bird, 6);
        boolean waterEdge = nearWater && this.scanForWater(bird, 3) && this.scanForDryGround(bird, 2);
        boolean nearCover = this.isNearCover(bird);
        return new BirdHabitatSnapshot(nearWater, waterEdge, nearCover, nearCover);
    }

    @Override
    public float computeComfort(BirdSenses senses) {
        float comfort = 0.25F;

        if (senses.nearWater()) {
            comfort += 0.25F;
        }

        if (senses.waterEdge()) {
            comfort += 0.25F;
        }

        if (senses.nearCover()) {
            comfort += 0.15F;
        }

        if (senses.nearRoost()) {
            comfort += 0.15F;
        }

        if (senses.hasNearbyThreat()) {
            comfort -= 0.28F;
        }

        return this.clamp(comfort);
    }

    @Override
    public boolean wantsForage(BirdBrain brain) {
        BirdSenses senses = brain.senses();

        return senses.activeTime()
                && senses.isOnGround()
                && senses.nearWater()
                && brain.motivation().hunger() > 0.38F
                && brain.motivation().fear() < 0.55F
                && brain.motivation().fatigue() < 0.85F
                && brain.computeRiskScore() < 0.58F;
    }

    private boolean scanForWater(PathfinderMob bird, int radius) {
        Level level = bird.level();
        BlockPos origin = bird.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -2, -radius), origin.offset(radius, 1, radius))) {
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    private boolean scanForDryGround(PathfinderMob bird, int radius) {
        Level level = bird.level();
        BlockPos origin = bird.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -1, -radius), origin.offset(radius, 1, radius))) {
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty() && !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    private boolean isCoverBlock(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(Blocks.MANGROVE_ROOTS)
                || state.is(Blocks.MOSS_CARPET)
                || state.is(Blocks.GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.VINE)
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.BAMBOO);
    }
}
