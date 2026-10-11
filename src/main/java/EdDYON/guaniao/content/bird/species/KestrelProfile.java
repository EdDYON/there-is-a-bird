package EdDYON.guaniao.content.bird.species;

import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.brain.BirdHabitatSnapshot;
import EdDYON.guaniao.content.bird.brain.BirdSenses;
import EdDYON.guaniao.content.bird.brain.BirdSpeciesProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class KestrelProfile extends BirdSpeciesProfile {
    public static final KestrelProfile INSTANCE = new KestrelProfile();

    private KestrelProfile() {
    }

    @Override
    public double playerSenseRadius() {
        return 12.0D;
    }

    @Override
    public float baseBoldness() {
        return 0.46F;
    }

    @Override
    public float baseWariness() {
        return 0.64F;
    }

    @Override
    public float baseActivity() {
        return 0.66F;
    }

    @Override
    public float baseSociability() {
        return 0.12F;
    }

    @Override
    public float baseFlightiness() {
        return 0.72F;
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
        return entity != null && entity.isAlive() && entity.getType().is(BirdTags.KESTREL_PREY);
    }

    @Override
    public LivingEntity findNearestPrey(PathfinderMob bird) {
        if (!(bird.level() instanceof ServerLevel serverLevel)
                || !BirdScanBudget.tryAcquire(serverLevel, bird)) {
            return null;
        }
        AABB searchBox = new AABB(
                bird.getX() - 16.0D, bird.getY() - 20.0D, bird.getZ() - 16.0D,
                bird.getX() + 16.0D, bird.getY() + 5.0D, bird.getZ() + 16.0D);
        return serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox,
                        entity -> entity != bird && this.isPreferredPrey(entity) && !bird.isAlliedTo(entity)
                                && !(bird instanceof TamableAnimal hunter && hunter.isTame()
                                && entity instanceof TamableAnimal pet && pet.isTame()
                                && java.util.Objects.equals(hunter.getOwnerUUID(), pet.getOwnerUUID()))
                                && bird.hasLineOfSight(entity))
                .stream()
                .max(java.util.Comparator.comparingDouble(prey -> this.attackOpportunity(bird, prey)))
                .orElse(null);
    }

    private double attackOpportunity(PathfinderMob bird, LivingEntity prey) {
        net.minecraft.world.phys.Vec3 delta = prey.position().subtract(bird.position());
        net.minecraft.world.phys.Vec3 heading = bird.getDeltaMovement().multiply(1, 0, 1);
        if (heading.lengthSqr() < 0.0025D) heading = bird.getLookAngle().multiply(1, 0, 1);
        double forward = heading.normalize().dot(delta.multiply(1, 0, 1).normalize());
        double drop = bird.getY() - prey.getY();
        double heightBonus = drop >= 5 && drop <= 16 ? 12.0D : -Math.abs(drop - 9) * 0.5D;
        double openBonus = bird.level().canSeeSky(prey.blockPosition().above()) ? 5.0D : -4.0D;
        return heightBonus + openBonus + forward * 8.0D - delta.length() * 0.35D;
    }

    @Override
    public boolean isTemptingPlayer(Player player) {
        return BirdFoodSafety.matchesClean(BirdTags.KESTREL_FOODS, player.getMainHandItem())
                || BirdFoodSafety.matchesClean(BirdTags.KESTREL_FOODS, player.getOffhandItem());
    }

    @Override
    public boolean isNearCover(PathfinderMob bird) {
        Level level = bird.level();
        BlockPos origin = bird.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -2, -4), origin.offset(4, 5, 4))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isNearRoost(PathfinderMob bird) {
        Level level = bird.level();
        BlockPos origin = bird.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5, -2, -5), origin.offset(5, 7, 5))) {
            if ((level.getBlockState(pos).is(BirdTags.BIRD_PERCHES)
                    || level.getBlockState(pos).is(BlockTags.LEAVES))
                    && level.getBlockState(pos.above()).isAir()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int habitatScanCost() {
        return 2;
    }

    @Override
    public BirdHabitatSnapshot scanHabitat(PathfinderMob bird) {
        boolean cover = this.isNearCover(bird);
        boolean roost = this.isNearRoost(bird);
        return new BirdHabitatSnapshot(false, false, cover, roost);
    }
}
