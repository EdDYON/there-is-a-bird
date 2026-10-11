package EdDYON.guaniao.content.bird.species;

import EdDYON.guaniao.content.bird.BirdActivitySchedule;
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

/** Solitary, cautious woodland profile for a crepuscular woodcock. */
public final class WoodcockProfile extends BirdSpeciesProfile {
    public static final WoodcockProfile INSTANCE = new WoodcockProfile();

    private WoodcockProfile() {
    }

    @Override
    public double playerSenseRadius() {
        return 18.0D;
    }

    @Override
    public float baseBoldness() {
        return 0.18F;
    }

    @Override
    public float baseWariness() {
        return 0.82F;
    }

    @Override
    public float baseActivity() {
        return 0.48F;
    }

    @Override
    public float baseSociability() {
        return 0.16F;
    }

    @Override
    public float baseFlightiness() {
        return 0.76F;
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
    public boolean isNearCover(PathfinderMob bird) {
        return this.scanWoodlandCover(bird, 6, 4);
    }

    @Override
    public boolean isNearRoost(PathfinderMob bird) {
        return this.scanWoodlandCover(bird, 8, 5);
    }

    @Override
    public int habitatScanCost() {
        return 3;
    }

    @Override
    public float computeComfort(BirdSenses senses) {
        float comfort = 0.24F;
        if (senses.nearCover()) {
            comfort += 0.40F;
        }
        if (senses.nearRoost()) {
            comfort += 0.18F;
        }
        if (senses.nearWater()) {
            comfort += 0.12F;
        }
        if (!senses.nearCover()) {
            comfort -= 0.12F;
        }
        if (senses.hasNearbyThreat() && this.playerCountsAsRisk(senses.nearestPlayer())) {
            comfort -= 0.34F;
        }
        return this.clamp(comfort);
    }

    @Override
    public boolean isTemptingPlayer(PathfinderMob bird, Player player) {
        return player.getMainHandItem().is(BirdTags.WOODCOCK_FOODS)
                || player.getOffhandItem().is(BirdTags.WOODCOCK_FOODS);
    }

    @Override
    protected boolean playerCountsAsRisk(Player player) {
        return super.playerCountsAsRisk(player) && !player.isCreative();
    }

    @Override
    public boolean wantsForage(BirdBrain brain) {
        return brain.senses().activeTime()
                && brain.senses().isOnGround()
                && brain.motivation().hunger() > 0.32F
                && brain.motivation().fear() < 0.55F
                && brain.computeRiskScore() < 0.52F;
    }

    @Override
    public boolean wantsShortEscape(BirdBrain brain) {
        float risk = brain.computeRiskScore();
        return risk >= 0.62F && risk < 0.80F;
    }

    @Override
    public boolean wantsLongEscape(BirdBrain brain) {
        return brain.computeRiskScore() >= 0.80F;
    }

    @Override
    public boolean wantsMigrate(BirdBrain brain) {
        return false;
    }

    private boolean scanWoodlandCover(PathfinderMob bird, int horizontalRadius, int verticalRadius) {
        Level level = bird.level();
        BlockPos origin = bird.blockPosition();
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -horizontalRadius; x <= horizontalRadius; ++x) {
            for (int z = -horizontalRadius; z <= horizontalRadius; ++z) {
                for (int y = -1; y <= verticalRadius; ++y) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if ((cursor.getX() >> 4) != originChunkX || (cursor.getZ() >> 4) != originChunkZ) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)
                            || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                            || state.is(Blocks.TALL_GRASS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
