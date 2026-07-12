package EdDYON.guaniao.content.bird.seagull;

import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.advancement.BirdAdvancements;
import EdDYON.guaniao.content.bird.flight.BirdFlightController;
import EdDYON.guaniao.content.bird.flight.BirdFlightProfile;
import EdDYON.guaniao.content.bird.flight.BirdFlightTargeting;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SeagullEntity extends TamableAnimal implements GeoEntity, FlyingAnimal, ScalableBirdModel {
    private static final EntityDataAccessor<Float> MODEL_SCALE = SynchedEntityData.defineId(SeagullEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(SeagullEntity.class, EntityDataSerializers.BOOLEAN);
    private static final BirdFlightProfile FLIGHT_PROFILE = BirdFlightProfile.SEAGULL;
    private static final byte TAMING_FAILED_EVENT = 6;
    private static final byte TAMING_SUCCEEDED_EVENT = 7;
    private static final Ingredient TAMING_FOODS = Ingredient.of((ItemLike[]) new ItemLike[]{
            Items.COD,
            Items.COOKED_COD,
            Items.SALMON,
            Items.COOKED_SALMON,
            Items.TROPICAL_FISH,
            Items.PUFFERFISH,
            Items.BREAD,
            Items.WHEAT_SEEDS,
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.ROTTEN_FLESH
    });
    private static final double WALK_ANIMATION_MOTION_THRESHOLD = 1.0E-5D;
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation FLY_GLIDE_ANIMATION = RawAnimation.begin().thenLoop("fly_loop");
    private static final RawAnimation FLY_GLIDE_BOOST_ANIMATION = RawAnimation.begin().thenPlay("fly_flapping_wing").thenLoop("fly_loop");
    private static final RawAnimation FLY_FLAP_LOOP_ANIMATION = RawAnimation.begin().thenLoop("fly_flapping_wing_loop");
    private static final RawAnimation MOUTH_SCRATCH_IDLE_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_1").thenLoop("idle");
    private static final RawAnimation LAUGH_IDLE_1_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_2").thenLoop("idle");
    private static final RawAnimation EXTRA_IDLE_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_3").thenLoop("idle");
    private static final RawAnimation LAUGH_IDLE_2_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_4").thenLoop("idle");
    private static final RawAnimation BIG_LAUGH_IDLE_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_5").thenLoop("idle");
    private static final RawAnimation EAT_ANIMATION = RawAnimation.begin().thenPlay("eat").thenLoop("idle");
    private static final RawAnimation SLEEP_ANIMATION = RawAnimation.begin().thenPlay("sleep").thenLoop("sleep_loop");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private GuidePreviewAnimation guidePreviewAnimation = GuidePreviewAnimation.NONE;
    private int flightTicks;
    private int flightCooldown;
    private int airborneGraceTicks;
    private int restInterruptionTicks;
    private Vec3 flightTarget;

    public SeagullEntity(EntityType<? extends SeagullEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 12, true);
        this.setPathfindingMalus(BlockPathTypes.LEAVES, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 16.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, SeagullDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, SeagullDefinition.WALK_SPEED)
                .add(Attributes.FLYING_SPEED, SeagullDefinition.FLYING_SPEED)
                .add(Attributes.FOLLOW_RANGE, SeagullDefinition.FOLLOW_RANGE);
    }

    @Nullable
    @Override
    public SeagullEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
        SeagullEntity child = GuaniaoEntityTypes.SEAGULL.get().create(level);
        if (child != null) {
            float mateScale = mate instanceof SeagullEntity other ? other.getIndividualModelScale() : this.getIndividualModelScale();
            child.setIndividualModelScale(BirdModelScale.inheritIndividualScale(child.getRandom(), this.getIndividualModelScale(), mateScale, child.modelScaleProfile()));
        }
        return child;
    }

    public static boolean canSpawn(EntityType<SeagullEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean validGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(Blocks.SAND)
                || below.is(Blocks.RED_SAND)
                || below.is(Blocks.GRAVEL)
                || below.is(Blocks.STONE)
                || below.is(Blocks.COBBLESTONE)
                || below.is(Blocks.SANDSTONE)
                || below.is(Blocks.RED_SANDSTONE)
                || below.is(BlockTags.LOGS);
        return validGround
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getRawBrightness(pos, 0) > 5
                && SeagullEntity.hasNearbyWater(level, pos, 9);
    }

    private static boolean hasNearbyWater(ServerLevelAccessor level, BlockPos pos, int radius) {
        if (level.getBiome(pos).is(BiomeTags.IS_OCEAN)
                || level.getBiome(pos).is(BiomeTags.IS_BEACH)
                || level.getBiome(pos).is(BiomeTags.IS_RIVER)) {
            return true;
        }
        int originChunkX = pos.getX() >> 4;
        int originChunkZ = pos.getZ() >> 4;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; ++x) {
            for (int y = -2; y <= 2; ++y) {
                for (int z = -radius; z <= radius; ++z) {
                    mutable.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if ((mutable.getX() >> 4) != originChunkX || (mutable.getZ() >> 4) != originChunkZ) {
                        continue;
                    }
                    if (level.getFluidState(mutable).is(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SeagullSleepGoal(this));
        this.goalSelector.addGoal(5, new SeagullAirCruiseGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.92D, 0.001F));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        this.fallDistance = 0.0F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MODEL_SCALE, BirdModelScale.DEFAULT_INDIVIDUAL_SCALE);
        this.entityData.define(SLEEPING, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag compoundTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, compoundTag);
        if (compoundTag == null || !compoundTag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.randomizeModelScale();
        }
        return data;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        BirdModelScale.save(compoundTag, this.getIndividualModelScale(), this.modelScaleProfile());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.setIndividualModelScale(BirdModelScale.load(compoundTag, this.modelScaleProfile()));
        } else {
            this.randomizeModelScale();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        this.tickFlightCounters();
        if (this.restInterruptionTicks > 0) {
            --this.restInterruptionTicks;
        }
        this.tickAirCruise();
        this.tickGroundMovementFacing();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isCleanTamingFood(stack)) {
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 180);
        this.setSleeping(false);
        this.getNavigation().stop();
        if (this.isTame()) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(2.0F);
            }
            if (!this.isBaby() && !this.isInLove()) {
                this.setInLove(player);
            }
            this.level().broadcastEntityEvent(this, TAMING_SUCCEEDED_EVENT);
        } else if (this.getRandom().nextInt(3) == 0) {
            this.tame(player);
            this.setPersistenceRequired();
            BirdAdvancements.awardTamedBird(player, this);
            this.level().broadcastEntityEvent(this, TAMING_SUCCEEDED_EVENT);
        } else {
            this.level().broadcastEntityEvent(this, TAMING_FAILED_EVENT);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isCleanTamingFood(stack);
    }

    private static boolean isCleanTamingFood(ItemStack stack) {
        return BirdFoodSafety.matchesClean(TAMING_FOODS, stack);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.isTame();
    }

    @Override
    public boolean isFlying() {
        return this.shouldPlayFlyAnimation();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 240);
            this.setSleeping(false);
            this.flightCooldown = 0;
            if (this.onGround()) {
                this.startAirCruise();
            }
        }
        return hurt;
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.SEAGULL;
    }

    @Override
    public float getIndividualModelScale() {
        if (this.entityData == null) {
            return BirdModelScale.DEFAULT_INDIVIDUAL_SCALE;
        }
        return BirdModelScale.sanitize(this.entityData.get(MODEL_SCALE), this.modelScaleProfile());
    }

    @Override
    public void setIndividualModelScale(float scale) {
        if (this.entityData != null) {
            this.entityData.set(MODEL_SCALE, BirdModelScale.sanitize(scale, this.modelScaleProfile()));
        }
    }

    public void setGuidePreviewAnimation(GuidePreviewAnimation guidePreviewAnimation) {
        this.guidePreviewAnimation = guidePreviewAnimation == null ? GuidePreviewAnimation.NONE : guidePreviewAnimation;
    }

    private <T extends SeagullEntity> PlayState movementController(AnimationState<T> animationState) {
        RawAnimation guidePreviewRawAnimation = this.guidePreviewAnimation.animation();
        if (guidePreviewRawAnimation != null) {
            return animationState.setAndContinue(guidePreviewRawAnimation);
        }
        if (this.shouldPlayFlyAnimation()) {
            return animationState.setAndContinue(this.flightAnimation());
        }
        if (this.isNightResting()) {
            return animationState.setAndContinue(SLEEP_ANIMATION);
        }
        if (this.shouldPlayWalkAnimation()) {
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    private boolean shouldPlayFlyAnimation() {
        return !this.isInWaterOrBubble()
                && !this.onGround()
                && (this.isNoGravity() || this.airborneGraceTicks > 0 || !this.isNearGroundForAnimation());
    }

    private boolean shouldPlayWalkAnimation() {
        return !this.isNightResting()
                && !this.isInWaterOrBubble()
                && (BirdGroundAnimation.hasWalkMotion(this)
                || ((this.onGround() || this.isNearGroundForAnimation())
                && (this.getDeltaMovement().horizontalDistanceSqr() > WALK_ANIMATION_MOTION_THRESHOLD || !this.getNavigation().isDone())));
    }

    private RawAnimation flightAnimation() {
        if (this.shouldFlapInFlight()) {
            return FLY_FLAP_LOOP_ANIMATION;
        }
        return this.shouldBoostWhileGliding() ? FLY_GLIDE_BOOST_ANIMATION : FLY_GLIDE_ANIMATION;
    }

    private boolean shouldFlapInFlight() {
        Vec3 movement = this.getDeltaMovement();
        return movement.y > 0.035D || movement.horizontalDistanceSqr() > 0.055D;
    }

    private boolean shouldBoostWhileGliding() {
        return Math.floorMod(this.tickCount + this.getId() * 11, 92) < 14;
    }

    private void tickFlightCounters() {
        if (this.flightCooldown > 0) {
            --this.flightCooldown;
        }
        if (this.airborneGraceTicks > 0) {
            --this.airborneGraceTicks;
        }
    }

    private boolean isActiveTime() {
        return BirdActivitySchedule.COASTAL_DIURNAL.isActiveTime(this.level().getDayTime());
    }

    private boolean isRestTime() {
        return BirdActivitySchedule.COASTAL_DIURNAL.isRestTime(this.level().getDayTime());
    }

    private boolean isNightResting() {
        return this.entityData != null && this.entityData.get(SLEEPING);
    }

    private void setSleeping(boolean sleeping) {
        if (this.entityData != null && this.entityData.get(SLEEPING) != sleeping) {
            this.entityData.set(SLEEPING, sleeping);
        }
    }

    private void tickAirCruise() {
        if (this.flightTicks <= 0) {
            if (this.isNoGravity()) {
                this.setNoGravity(false);
            }
            return;
        }
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.fallDistance = 0.0F;
        --this.flightTicks;
        if (this.flightTarget == null || this.flightTarget.distanceToSqr(this.position()) < 4.0D || this.tickCount % 70 == 0) {
            this.retargetAirCruise();
        }
        if (this.flightTarget == null) {
            this.finishAirCruise();
            return;
        }
        Vec3 toTarget = this.flightTarget.subtract(this.position());
        Vec3 horizontal = BirdFlightTargeting.normalizeHorizontal(new Vec3(toTarget.x, 0.0D, toTarget.z), this.getDeltaMovement());
        double vertical = Mth.clamp(toTarget.y * 0.11D + Math.sin((this.tickCount + this.getId()) * 0.20D) * 0.016D, -0.085D, 0.18D);
        Vec3 desired = horizontal.scale(FLIGHT_PROFILE.cruiseSpeed()).add(0.0D, vertical, 0.0D);
        Vec3 movement = BirdFlightController.blendMovement(this.getDeltaMovement(), desired, 0.74D);
        this.setDeltaMovement(movement);
        BirdFlightController.faceMovement(this, movement, FLIGHT_PROFILE.maxPitchDegrees());
        this.hasImpulse = true;
        if (this.flightTicks <= 0) {
            this.finishAirCruise();
        }
    }

    private boolean startAirCruise() {
        if (this.flightCooldown > 0 || this.flightTicks > 0 || this.isInWaterOrBubble()) {
            return false;
        }
        this.flightTarget = this.findAirCruiseTarget();
        if (this.flightTarget == null) {
            return false;
        }
        this.flightTicks = FLIGHT_PROFILE.minFlightTicks() + this.getRandom().nextInt(FLIGHT_PROFILE.maxFlightTicks() - FLIGHT_PROFILE.minFlightTicks() + 1);
        this.airborneGraceTicks = 80;
        this.setNoGravity(true);
        this.setOnGround(false);
        this.getNavigation().stop();
        Vec3 direction = this.flightTarget.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() <= 1.0E-4D) {
            direction = this.randomHorizontalDirection();
        }
        Vec3 movement = direction.normalize().scale(0.48D).add(0.0D, 0.18D, 0.0D);
        this.setDeltaMovement(movement);
        BirdFlightController.faceMovement(this, movement, FLIGHT_PROFILE.maxPitchDegrees());
        this.fallDistance = 0.0F;
        this.hasImpulse = true;
        return true;
    }

    private void retargetAirCruise() {
        this.flightTarget = this.findAirCruiseTarget();
    }

    private Vec3 findAirCruiseTarget() {
        Vec3 preferredDirection = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D ? this.getDeltaMovement() : this.randomHorizontalDirection();
        Vec3 target = BirdFlightTargeting.findAirTarget(this, FLIGHT_PROFILE, preferredDirection, false);
        if (target != null) {
            return target;
        }
        Vec3 direction = this.randomHorizontalDirection();
        double y = Mth.clamp(
                this.getY() + 8.0D + this.getRandom().nextDouble() * 8.0D,
                this.level().getMinBuildHeight() + 3.0D,
                this.level().getMaxBuildHeight() - 3.0D);
        return new Vec3(
                this.getX() + direction.x * (16.0D + this.getRandom().nextDouble() * 14.0D),
                y,
                this.getZ() + direction.z * (16.0D + this.getRandom().nextDouble() * 14.0D));
    }

    private void finishAirCruise() {
        this.flightTicks = 0;
        this.flightTarget = null;
        this.setNoGravity(false);
        this.airborneGraceTicks = 50;
        this.flightCooldown = 80 + this.getRandom().nextInt(120);
    }

    private Vec3 randomHorizontalDirection() {
        return BirdFlightTargeting.randomHorizontalDirection(this.getRandom());
    }

    private void tickGroundMovementFacing() {
        if (!this.isNoGravity()
                && !this.isInWaterOrBubble()
                && (this.onGround() || this.isNearGroundForAnimation())) {
            BirdFlightController.faceGroundMovement(this, this.getDeltaMovement(), 1.0E-4D);
        }
    }

    private boolean isNearGroundForAnimation() {
        return this.onGround() || !this.level().noCollision(this, this.getBoundingBox().move(0.0D, -0.35D, 0.0D));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable)this, "movement", 4, this::movementController)});
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    private void randomizeModelScale() {
        this.setIndividualModelScale(BirdModelScale.randomIndividualScale(this.getRandom(), this.modelScaleProfile()));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 190;
    }

    @Override
    public float getSoundVolume() {
        return 0.58F;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, BirdSoundVolume.apply(this, volume), pitch);
    }

    public enum GuidePreviewAnimation {
        NONE(null),
        IDLE(IDLE_ANIMATION),
        MOUTH_SCRATCH(MOUTH_SCRATCH_IDLE_ANIMATION),
        LAUGH_1(LAUGH_IDLE_1_ANIMATION),
        IDLE_VARIATION(EXTRA_IDLE_ANIMATION),
        LAUGH_2(LAUGH_IDLE_2_ANIMATION),
        BIG_LAUGH(BIG_LAUGH_IDLE_ANIMATION),
        WALK(WALK_ANIMATION),
        FLY_FLAP(FLY_FLAP_LOOP_ANIMATION),
        GLIDE_BOOST(FLY_GLIDE_BOOST_ANIMATION),
        GLIDE(FLY_GLIDE_ANIMATION),
        EAT(EAT_ANIMATION),
        SLEEP(SLEEP_ANIMATION);

        private final RawAnimation animation;

        GuidePreviewAnimation(RawAnimation animation) {
            this.animation = animation;
        }

        private RawAnimation animation() {
            return this.animation;
        }
    }

    private static final class SeagullAirCruiseGoal extends Goal {
        private final SeagullEntity seagull;

        private SeagullAirCruiseGoal(SeagullEntity seagull) {
            this.seagull = seagull;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.seagull.flightCooldown <= 0
                    && this.seagull.onGround()
                    && this.seagull.isActiveTime()
                    && this.seagull.getNavigation().isDone()
                    && this.seagull.getRandom().nextInt(65) == 0;
        }

        @Override
        public void start() {
            this.seagull.startAirCruise();
        }
    }

    private static final class SeagullSleepGoal extends Goal {
        private final SeagullEntity seagull;

        private SeagullSleepGoal(SeagullEntity seagull) {
            this.seagull = seagull;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.seagull.isRestTime()
                    && this.seagull.restInterruptionTicks <= 0
                    && this.seagull.flightTicks <= 0
                    && this.seagull.onGround()
                    && !this.seagull.isInWaterOrBubble();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.setSleepingState();
        }

        @Override
        public void tick() {
            this.setSleepingState();
        }

        @Override
        public void stop() {
            this.seagull.setSleeping(false);
        }

        private void setSleepingState() {
            this.seagull.getNavigation().stop();
            Vec3 movement = this.seagull.getDeltaMovement();
            this.seagull.setDeltaMovement(0.0D, movement.y, 0.0D);
            this.seagull.setSleeping(true);
        }
    }
}
