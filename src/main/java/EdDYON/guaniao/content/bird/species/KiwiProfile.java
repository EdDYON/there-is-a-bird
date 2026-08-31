package EdDYON.guaniao.content.bird.species;

import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdTags;
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
import net.minecraft.world.level.block.state.BlockState;

/** Ground-only, cautious forest-bird profile. Flight and aerial migration are deliberately disabled. */
public final class KiwiProfile extends BirdSpeciesProfile {
    public static final KiwiProfile INSTANCE = new KiwiProfile();

    private KiwiProfile() {
    }

    @Override
    public double playerSenseRadius() {
        return 15.0D;
    }

    @Override
    public float baseBoldness() {
        return 0.18F;
    }

    @Override
    public float baseWariness() {
        return 0.78F;
    }

    @Override
    public float baseActivity() {
        return 0.55F;
    }

    @Override
    public float baseSociability() {
        return 0.18F;
    }

    @Override
    public float baseFlightiness() {
        return 0.0F;
    }

    @Override
    public boolean isActiveTime(BirdSenses senses) {
        return BirdActivitySchedule.NOCTURNAL_CREPUSCULAR.isActiveTime(senses.dayTime());
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
        return BirdFoodSafety.matchesClean(BirdTags.KIWI_FOODS, player.getMainHandItem())
                || BirdFoodSafety.matchesClean(BirdTags.KIWI_FOODS, player.getOffhandItem());
    }

    @Override
    public boolean isNearCover(PathfinderMob bird) {
        return this.scanCover(bird, 5, 3, false);
    }

    @Override
    public boolean isNearRoost(PathfinderMob bird) {
        return this.scanCover(bird, 4, 2, true);
    }

    @Override
    public int habitatScanCost() {
        return 2;
    }

    @Override
    public float computeComfort(BirdSenses senses) {
        float comfort = 0.28F;
        if (senses.nearCover()) {
            comfort += 0.34F;
        }
        if (senses.nearRoost()) {
            comfort += 0.18F;
        }
        if (senses.hasNearbyThreat()) {
            comfort -= 0.30F;
        }
        return this.clamp(comfort);
    }

    @Override
    public boolean wantsForage(BirdBrain brain) {
        return brain.senses().activeTime()
                && brain.senses().isOnGround()
                && brain.motivation().hunger() > 0.32F
                && brain.motivation().fear() < 0.58F
                && brain.computeRiskScore() < 0.58F;
    }

    @Override
    public boolean wantsShortEscape(BirdBrain brain) {
        return false;
    }

    @Override
    public boolean wantsLongEscape(BirdBrain brain) {
        return false;
    }

    @Override
    public boolean wantsMigrate(BirdBrain brain) {
        return false;
    }

    @Override
    public float computeRisk(BirdBrain brain) {
        float risk = super.computeRisk(brain);
        Player player = brain.senses().nearestPlayer();
        if (player == null) {
            return risk;
        }

        double distance = brain.senses().nearestPlayerDistance();
        if (player.isCrouching() && !player.isSprinting()) {
            risk -= 0.12F;
        }
        if (player.isSprinting() && distance < 10.0D) {
            risk += 0.10F;
        }
        if (distance < 5.0D) {
            risk += 0.18F;
        }
        return this.clamp(risk);
    }

    private boolean scanCover(PathfinderMob bird, int horizontalRadius, int verticalRadius, boolean denseOnly) {
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
                    || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.PODZOL)) {
                return true;
            }
            if (!denseOnly && (state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                    || state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS))) {
                return true;
            }
        }
        return false;
    }
}
