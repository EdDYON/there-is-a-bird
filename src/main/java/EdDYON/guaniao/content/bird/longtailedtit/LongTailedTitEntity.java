package EdDYON.guaniao.content.bird.longtailedtit;

import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.CleanBirdTemptGoal;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.advancement.BirdAdvancements;
import EdDYON.guaniao.content.bird.sparrow.SparrowBehaviorState;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.content.bird.crow.CrowEntity;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class LongTailedTitEntity extends SparrowEntity {
    private static final Ingredient TRUST_FOODS = Ingredient.of((ItemLike[]) new ItemLike[]{
            Items.WHEAT_SEEDS,
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.TORCHFLOWER_SEEDS,
            Items.PITCHER_POD,
            Items.SWEET_BERRIES,
            Items.GLOW_BERRIES,
            Items.SPIDER_EYE,
            Items.APPLE
    });
    private static final Ingredient EXTRA_TRUST_FOODS = Ingredient.of(
            Items.SWEET_BERRIES,
            Items.GLOW_BERRIES,
            Items.SPIDER_EYE,
            Items.APPLE
    );
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation IDLE_ONE_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_1").thenLoop("idle");
    private static final RawAnimation IDLE_TWO_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_2").thenLoop("idle");
    private static final RawAnimation IDLE_THREE_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_3").thenLoop("idle");
    private static final RawAnimation EAT_ANIMATION = RawAnimation.begin().thenPlay("eat").thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation FLY_ANIMATION = RawAnimation.begin().thenLoop("fly_loop");
    private RawAnimation currentIdleAnimation = IDLE_ANIMATION;
    private long nextIdleAnimationTick;
    private GuidePreviewAnimation guidePreviewAnimation = GuidePreviewAnimation.NONE;
    private int flockFlightCooldown;
    private int threatScanCooldown;

    public LongTailedTitEntity(EntityType<? extends LongTailedTitEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static boolean canLongTailedTitSpawn(EntityType<LongTailedTitEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean forestFloor = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(BlockTags.LEAVES)
                || below.is(Blocks.PODZOL)
                || below.is(Blocks.MOSS_BLOCK);
        return forestFloor && level.getRawBrightness(pos, 0) > 7;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new CleanBirdTemptGoal(this, 0.92D, EXTRA_TRUST_FOODS, false));
        this.goalSelector.addGoal(6, new RoostLineGoal(this));
        this.goalSelector.addGoal(8, new TightFlockGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.flockFlightCooldown > 0) {
            --this.flockFlightCooldown;
        }
        if (this.threatScanCooldown > 0) {
            --this.threatScanCooldown;
        }
        if (!this.level().isClientSide) {
            this.tickFlockFlightResponse();
            this.tickLargeBirdResponse();
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean trustFood = isTrustFood(stack);
        InteractionResult result;
        if (isExtraTrustFood(stack)) {
            result = this.feedExtraTrustFood(player, stack);
        } else {
            result = super.mobInteract(player, hand);
        }
        if (trustFood && !this.level().isClientSide && result.consumesAction()) {
            this.shareTrustWithFlock();
        }
        return result;
    }

    private InteractionResult feedExtraTrustFood(Player player, ItemStack stack) {
        if (!this.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            this.birdBrain().onEat(0.24F);
            this.acceptFlockTrust(3600, 700);
            if (this.isTame()) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(2.0F);
                }
                if (!this.isBaby() && !this.isInLove()) {
                    this.setInLove(player);
                }
                this.level().broadcastEntityEvent(this, (byte) 7);
            } else if (this.getRandom().nextInt(3) == 0) {
                this.tame(player);
                this.getNavigation().stop();
                BirdAdvancements.awardTamedBird(player, this);
                this.level().broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private void shareTrustWithFlock() {
        for (LongTailedTitEntity tit : this.nearbyFlock(12.0D)) {
            tit.acceptFlockTrust(3000, 600);
            tit.birdBrain().onRest(0.12F);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isTrustFood(stack);
    }

    private static boolean isTrustFood(ItemStack stack) {
        return BirdFoodSafety.matchesClean(TRUST_FOODS, stack);
    }

    private static boolean isExtraTrustFood(ItemStack stack) {
        return BirdFoodSafety.matchesClean(EXTRA_TRUST_FOODS, stack);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.LONG_TAILED_TIT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return GuaniaoSoundEvents.LONG_TAILED_TIT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuaniaoSoundEvents.LONG_TAILED_TIT_DEATH.get();
    }

    @Nullable
    @Override
    public LongTailedTitEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
        LongTailedTitEntity child = GuaniaoEntityTypes.LONG_TAILED_TIT.get().create(level);
        if (child != null) {
            float mateScale = mate instanceof SparrowEntity other ? other.getIndividualModelScale() : this.getIndividualModelScale();
            child.setIndividualModelScale(EdDYON.guaniao.content.bird.scale.BirdModelScale.inheritIndividualScale(
                    child.getRandom(), this.getIndividualModelScale(), mateScale, child.modelScaleProfile()));
        }
        return child;
    }

    @Override
    public void setGuidePreviewAnimation(SparrowEntity.GuidePreviewAnimation animation) {
        this.guidePreviewAnimation = animation == null ? GuidePreviewAnimation.NONE : switch (animation) {
            case NONE -> GuidePreviewAnimation.NONE;
            case IDLE -> GuidePreviewAnimation.IDLE;
            case TAIL -> GuidePreviewAnimation.IDLE_ONE;
            case PECK -> GuidePreviewAnimation.EAT;
            case LOOK_AROUND -> GuidePreviewAnimation.IDLE_TWO;
            case WALK -> GuidePreviewAnimation.WALK;
            case FLY -> GuidePreviewAnimation.FLY;
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable) this, "movement", 4, this::movementController)});
    }

    private <T extends LongTailedTitEntity> PlayState movementController(AnimationState<T> state) {
        RawAnimation preview = this.guidePreviewAnimation.animation;
        if (preview != null) {
            return state.setAndContinue(preview);
        }
        if (this.isBirdFlightActive() || !this.onGround()) {
            return state.setAndContinue(FLY_ANIMATION);
        }
        SparrowBehaviorState behavior = this.getBehaviorState();
        if (BirdGroundAnimation.hasWalkMotion(this)
                && behavior != SparrowBehaviorState.PECKING
                && behavior != SparrowBehaviorState.ROOSTING
                && behavior != SparrowBehaviorState.PERCHING) {
            return state.setAndContinue(WALK_ANIMATION);
        }
        if (behavior == SparrowBehaviorState.PECKING) {
            return state.setAndContinue(EAT_ANIMATION);
        }
        if (behavior == SparrowBehaviorState.LOOK_AROUND || behavior == SparrowBehaviorState.ALERT) {
            return state.setAndContinue(IDLE_TWO_ANIMATION);
        }
        return state.setAndContinue(this.pickIdleAnimation());
    }

    private RawAnimation pickIdleAnimation() {
        if (this.level().getGameTime() >= this.nextIdleAnimationTick) {
            int roll = this.getRandom().nextInt(10);
            this.currentIdleAnimation = roll < 5 ? IDLE_ANIMATION
                    : roll < 7 ? IDLE_ONE_ANIMATION
                    : roll < 9 ? IDLE_TWO_ANIMATION
                    : IDLE_THREE_ANIMATION;
            this.nextIdleAnimationTick = this.level().getGameTime() + 55L + this.getRandom().nextInt(90);
        }
        return this.currentIdleAnimation;
    }

    private void tickFlockFlightResponse() {
        if (this.flockFlightCooldown > 0 || this.isTame() || this.isBirdFlightActive() || this.isRestTime() || this.tickCount % 5 != 0) {
            return;
        }
        LongTailedTitEntity flyingLeader = this.nearbyFlock(11.0D).stream()
                .filter(tit -> tit != this && tit.isBirdFlightActive())
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
        if (flyingLeader == null) {
            return;
        }
        Vec3 motion = flyingLeader.getDeltaMovement();
        Vec3 direction = motion.horizontalDistanceSqr() > 1.0E-4D
                ? new Vec3(motion.x, 0.0D, motion.z).normalize()
                : flyingLeader.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        double distance = 5.0D + this.getRandom().nextDouble() * 4.0D;
        int x = (int) Math.floor(flyingLeader.getX() + direction.x * distance + this.getRandom().nextDouble() * 2.0D - 1.0D);
        int z = (int) Math.floor(flyingLeader.getZ() + direction.z * distance + this.getRandom().nextDouble() * 2.0D - 1.0D);
        int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (this.startFlybyFlight(new Vec3(x + 0.5D, y, z + 0.5D))) {
            this.flockFlightCooldown = 80;
        }
    }

    private void tickLargeBirdResponse() {
        if (this.threatScanCooldown > 0 || this.isTame() || this.tickCount % 10 != 0) {
            return;
        }
        CrowEntity threat = this.level().getNearestEntity(
                this.level().getEntitiesOfClass(CrowEntity.class, this.getBoundingBox().inflate(11.0D), crow -> crow.isAlive()),
                net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat(),
                this,
                this.getX(), this.getY(), this.getZ());
        if (threat == null) {
            return;
        }
        this.birdBrain().onFrightened(0.72F);
        this.fleeFromFlockThreat(threat.position());
        this.threatScanCooldown = 100;
    }

    private List<LongTailedTitEntity> nearbyFlock(double radius) {
        return this.level().getEntitiesOfClass(LongTailedTitEntity.class, this.getBoundingBox().inflate(radius), tit -> tit.isAlive());
    }

    private boolean isRestTime() {
        return BirdActivitySchedule.DIURNAL.isRestTime(this.level().getDayTime()) || this.level().isRaining();
    }

    private enum GuidePreviewAnimation {
        NONE(null),
        IDLE(IDLE_ANIMATION),
        IDLE_ONE(IDLE_ONE_ANIMATION),
        IDLE_TWO(IDLE_TWO_ANIMATION),
        EAT(EAT_ANIMATION),
        WALK(WALK_ANIMATION),
        FLY(FLY_ANIMATION);

        private final RawAnimation animation;

        GuidePreviewAnimation(RawAnimation animation) {
            this.animation = animation;
        }
    }

    private static final class TightFlockGoal extends Goal {
        private final LongTailedTitEntity bird;
        private Vec3 target;

        private TightFlockGoal(LongTailedTitEntity bird) {
            this.bird = bird;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.bird.isTame() || this.bird.isBirdFlightActive() || this.bird.isRestTime() || this.bird.getRandom().nextInt(12) != 0) {
                return false;
            }
            List<LongTailedTitEntity> flock = this.bird.nearbyFlock(12.0D);
            if (flock.size() < 2) {
                return false;
            }
            double x = 0.0D;
            double y = 0.0D;
            double z = 0.0D;
            for (LongTailedTitEntity tit : flock) {
                x += tit.getX();
                y += tit.getY();
                z += tit.getZ();
            }
            this.target = new Vec3(x / flock.size(), y / flock.size(), z / flock.size());
            return this.bird.position().distanceToSqr(this.target) > 6.25D;
        }

        @Override
        public void start() {
            this.bird.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, 0.9D);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.bird.getNavigation().isDone() && !this.bird.isRestTime();
        }
    }

    private static final class RoostLineGoal extends Goal {
        private final LongTailedTitEntity bird;
        private Vec3 linePosition;
        private int remainingTicks;
        private int recalculateTicks;

        private RoostLineGoal(LongTailedTitEntity bird) {
            this.bird = bird;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.bird.isRestTime() || this.bird.isBirdFlightActive() || this.bird.isInWaterOrBubble()) {
                return false;
            }
            return this.findLinePosition();
        }

        @Override
        public void start() {
            this.remainingTicks = 180 + this.bird.getRandom().nextInt(260);
            this.recalculateTicks = 0;
            this.moveIntoLine();
        }

        @Override
        public boolean canContinueToUse() {
            return this.remainingTicks > 0 && this.bird.isRestTime() && !this.bird.isBirdFlightActive() && !this.bird.isInWaterOrBubble();
        }

        @Override
        public void tick() {
            --this.remainingTicks;
            if (--this.recalculateTicks <= 0) {
                this.recalculateTicks = 30;
                if (this.findLinePosition()) {
                    this.moveIntoLine();
                }
            }
            if (this.linePosition == null) {
                return;
            }
            double distance = this.bird.position().distanceToSqr(this.linePosition);
            if (distance <= 0.12D) {
                this.bird.getNavigation().stop();
                List<LongTailedTitEntity> flock = this.bird.nearbyFlock(6.0D);
                if (!flock.isEmpty()) {
                    LongTailedTitEntity anchor = flock.stream().min(Comparator.comparingInt(LongTailedTitEntity::getId)).orElse(this.bird);
                    this.bird.setYRot(anchor.getYRot());
                    this.bird.yBodyRot = anchor.getYRot();
                }
            }
        }

        @Override
        public void stop() {
            this.linePosition = null;
            this.remainingTicks = 0;
        }

        private boolean findLinePosition() {
            List<LongTailedTitEntity> flock = new ArrayList<>(this.bird.nearbyFlock(8.0D));
            if (!flock.contains(this.bird)) {
                flock.add(this.bird);
            }
            flock.removeIf(tit -> tit.isBirdFlightActive() || !tit.onGround());
            if (flock.size() < 2) {
                return false;
            }
            flock.sort(Comparator.comparingInt(LongTailedTitEntity::getId));
            LongTailedTitEntity anchor = flock.get(0);
            int index = flock.indexOf(this.bird);
            if (index < 0) {
                return false;
            }
            double yaw = Math.toRadians(anchor.getYRot());
            Vec3 side = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
            double centeredIndex = index - (flock.size() - 1) * 0.5D;
            this.linePosition = anchor.position().add(side.scale(centeredIndex * 0.38D));
            return true;
        }

        private void moveIntoLine() {
            if (this.linePosition != null && this.bird.position().distanceToSqr(this.linePosition) > 0.12D) {
                this.bird.getNavigation().moveTo(this.linePosition.x, this.linePosition.y, this.linePosition.z, 0.78D);
            }
        }
    }
}
