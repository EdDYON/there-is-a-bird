package EdDYON.guaniao.content.bird.nightheron;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.CleanBirdTemptGoal;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.flock.FlockCompatibleBird;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.BirdSleepWakeable;
import EdDYON.guaniao.content.bird.PollutedFoodReactionUtil;
import EdDYON.guaniao.content.bird.brain.BirdBrain;
import EdDYON.guaniao.content.bird.flight.BirdFlightAware;
import EdDYON.guaniao.content.bird.flight.BirdFlightController;
import EdDYON.guaniao.content.bird.flight.BirdFlightProfile;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import EdDYON.guaniao.content.bath.BirdBathAttraction;
import EdDYON.guaniao.content.bath.BirdBathContentType;
import EdDYON.guaniao.content.bath.BirdBathFeedingAnimatable;
import EdDYON.guaniao.content.bath.BirdBathMountable;
import EdDYON.guaniao.content.bath.BirdBathUseGoal;
import EdDYON.guaniao.content.bird.nightheron.NightHeronAmbientFlightGoal;
import EdDYON.guaniao.content.bird.nightheron.NightHeronBehaviorState;
import EdDYON.guaniao.content.bird.nightheron.NightHeronFlightController;
import EdDYON.guaniao.content.bird.nightheron.NightHeronFlockGoal;
import EdDYON.guaniao.content.bird.nightheron.NightHeronForagingGoal;
import EdDYON.guaniao.content.bird.nightheron.NightHeronFrightGoal;
import EdDYON.guaniao.content.bird.nightheron.NightHeronHighTransitGoal;
import EdDYON.guaniao.content.bird.nightheron.NightHeronIdleGoal;
import EdDYON.guaniao.content.bird.nightheron.NightHeronRoostFlightGoal;
import EdDYON.guaniao.content.bird.nightheron.NightHeronRoostGoal;
import EdDYON.guaniao.content.bird.species.NightHeronProfile;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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

public class NightHeronEntity
extends PathfinderMob
implements GeoEntity, ScalableBirdModel, BirdFlightAware, BirdBathMountable, BirdBathFeedingAnimatable, FlockCompatibleBird, BirdSleepWakeable {
    private static final EntityDataAccessor<Integer> BEHAVIOR_STATE = SynchedEntityData.defineId(NightHeronEntity.class, (EntityDataSerializer)EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MODEL_SCALE = SynchedEntityData.defineId(NightHeronEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<ItemStack> HELD_FISH = SynchedEntityData.defineId(NightHeronEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> HELD_FISH_POSE_SEED = SynchedEntityData.defineId(NightHeronEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EATING_TICKS = SynchedEntityData.defineId(NightHeronEntity.class, EntityDataSerializers.INT);
    static final Ingredient TEMPT_ITEMS = Ingredient.of(BirdTags.NIGHT_HERON_FOODS);
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation IDLE_DIFF_1_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_1").thenLoop("idle");
    private static final RawAnimation IDLE_DIFF_2_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_2").thenLoop("idle");
    private static final RawAnimation IDLE_DIFF_3_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_3").thenLoop("idle");
    private static final RawAnimation IDLE_DIFF_4_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_4").thenLoop("idle");
    private static final RawAnimation IDLE_DIFF_5_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_5").thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN_ANIMATION = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation FLY_LOOP_ANIMATION = RawAnimation.begin().thenLoop("fly_loop");
    private static final RawAnimation FLY_FLAPPING_WING_ANIMATION = RawAnimation.begin().thenPlay("fly_flapping_wing").thenLoop("fly_flapping_wing_loop");
    private static final RawAnimation FLY_FLAPPING_WING_LOOP_ANIMATION = RawAnimation.begin().thenLoop("fly_flapping_wing_loop");
    private static final RawAnimation EAT_ANIMATION = RawAnimation.begin().thenPlay("eat").thenLoop("idle");
    private static final RawAnimation SLEEP_ANIMATION = RawAnimation.begin().thenPlay("sleep").thenLoop("sleep_loop");
    private static final int WATER_SEARCH_RADIUS = 8;
    private static final double RUNNING_SPEED_THRESHOLD = 0.018;
    private static final int MAX_CONTROLLED_FLIGHT_TICKS = 520;
    private static final float FLIGHT_YAW_TURN_RATE = 10.0f;
    private static final float FLIGHT_PITCH_TURN_RATE = 6.0f;
    private static final int POLLUTED_FOOD_REACTION_TICKS = 34;
    private static final int POLLUTED_FOOD_SPIT_TICK = 18;
    private static final int POLLUTED_FOOD_FLEE_TICK = 8;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final BirdBrain birdBrain = new BirdBrain(this, NightHeronProfile.INSTANCE);
    private NightHeronBehaviorState behaviorState = NightHeronBehaviorState.IDLE;
    private IdleAnimationChoice currentIdleAnimation = IdleAnimationChoice.BASE;
    private long nextIdleAnimationSwapTick;
    private int forcedIdleAnimationTicks;
    private int takeoffFlapTicks;
    private int frightMemoryTicks;
    private int recentFrightCount;
    private int externalFrightTicks;
    private int restInterruptionTicks;
    private boolean severeExternalFright;
    private Vec3 externalFrightSource;
    private int preyStrikeCooldown;
    private int thrownFishEatCooldown;
    private int pollutedFoodReactionTicks;
    private Vec3 pollutedFoodSource = Vec3.ZERO;
    private int controlledFlightTicks;
    private int groundedAirborneTicks;
    private int blockedFlightRecoveryActivityTicks;
    private int obstructedFlightTicks;
    private Vec3 lastControlledFlightPosition = Vec3.ZERO;
    private int blockedFlightRecoveryDirectionTicks;
    private Vec3 blockedFlightRecoveryDirection = Vec3.ZERO;
    private BlockPos landingApproachTarget;
    private Vec3 landingApproachDirection = Vec3.ZERO;
    private GuidePreviewAnimation guidePreviewAnimation = GuidePreviewAnimation.NONE;
    private int flybyFlightTicks;
    private Vec3 flybyFlightDirection = Vec3.ZERO;
    private BlockPos flybyLandingTarget;

    public NightHeronEntity(EntityType<? extends NightHeronEntity> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(BlockPathTypes.LEAVES, 0.0f);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0f);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0f);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BEHAVIOR_STATE, NightHeronBehaviorState.IDLE.ordinal());
        this.entityData.define(MODEL_SCALE, BirdModelScale.DEFAULT_INDIVIDUAL_SCALE);
        this.entityData.define(HELD_FISH, ItemStack.EMPTY);
        this.entityData.define(HELD_FISH_POSE_SEED, 0);
        this.entityData.define(EATING_TICKS, 0);
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (BEHAVIOR_STATE.equals(key)) {
            this.behaviorState = NightHeronEntity.decodeBehaviorState((Integer)this.entityData.get(BEHAVIOR_STATE));
        }
        super.onSyncedDataUpdated(key);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FLYING_SPEED, 0.45).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    public static boolean canSpawn(EntityType<NightHeronEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean validGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(Blocks.MUD) || below.is(Blocks.CLAY) || below.is(Blocks.SAND) || below.is(Blocks.RED_SAND);
        return validGround
                && (NightHeronEntity.isWaterEdgeForWorldgen((LevelReader)level, pos)
                || NightHeronEntity.isNearWaterForWorldgen((LevelReader)level, pos, 12));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag compoundTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, compoundTag);
        if (compoundTag == null || !compoundTag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.randomizeModelScale();
        }
        return data;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, (Goal)new FloatGoal((Mob)this));
        this.goalSelector.addGoal(1, (Goal)new NightHeronFrightGoal(this));
        this.goalSelector.addGoal(3, (Goal)new NightHeronEatThrownFishGoal(this));
        this.goalSelector.addGoal(4, (Goal)new CleanBirdTemptGoal((PathfinderMob)this, 1.0, TEMPT_ITEMS, false));
        this.goalSelector.addGoal(5, (Goal)new BirdBathUseGoal(this, 1.0D, 13.0D, 42,
                BirdBathAttraction::isAttractiveToNightHeron,
                this::canUseBirdBath,
                bath -> this.setBehaviorState(NightHeronBehaviorState.FORAGING),
                this::consumeBirdBathServing,
                (bath, consumed) -> {
                    if (!this.isEatingFish() && this.getBehaviorState() == NightHeronBehaviorState.FORAGING) {
                        this.setBehaviorState(NightHeronBehaviorState.IDLE);
                    }
                }));
        this.goalSelector.addGoal(6, (Goal)new NightHeronForagingGoal(this));
        this.goalSelector.addGoal(7, (Goal)new NightHeronSeekWaterGoal(this));
        this.goalSelector.addGoal(8, (Goal)new NightHeronHighTransitGoal(this));
        this.goalSelector.addGoal(9, (Goal)new NightHeronAmbientFlightGoal(this));
        this.goalSelector.addGoal(10, (Goal)new NightHeronRoostFlightGoal(this));
        this.goalSelector.addGoal(11, (Goal)new NightHeronRoostGoal(this));
        this.goalSelector.addGoal(12, (Goal)new NightHeronFlockGoal(this));
        this.goalSelector.addGoal(13, (Goal)new NightHeronIdleGoal(this));
        this.goalSelector.addGoal(14, (Goal)new RandomLookAroundGoal((Mob)this));
    }

    public void aiStep() {
        super.aiStep();
        if (this.isControlledFlightActive()) {
            this.fallDistance = 0.0f;
        }
        if (!this.level().isClientSide) {
            this.birdBrain.tick();
            this.tickStaleFlightRecovery();
            this.tickFlightStateGuard();
            if (this.takeoffFlapTicks > 0) {
                --this.takeoffFlapTicks;
            }
            if (this.frightMemoryTicks > 0) {
                --this.frightMemoryTicks;
            } else {
                this.recentFrightCount = 0;
            }
            if (this.externalFrightTicks > 0) {
                --this.externalFrightTicks;
            } else {
                this.externalFrightSource = null;
                this.severeExternalFright = false;
            }
            if (this.restInterruptionTicks > 0) {
                --this.restInterruptionTicks;
            }
            if (this.preyStrikeCooldown > 0) {
                --this.preyStrikeCooldown;
            }
            if (this.thrownFishEatCooldown > 0) {
                --this.thrownFishEatCooldown;
            }
            this.tickFlybyFlight();
            this.tickPollutedFoodReaction();
            this.tickEatingFish();
            this.tickWaterEscape();
            if (this.blockedFlightRecoveryActivityTicks > 0) {
                --this.blockedFlightRecoveryActivityTicks;
            }
            if (this.blockedFlightRecoveryDirectionTicks > 0) {
                --this.blockedFlightRecoveryDirectionTicks;
                if (this.blockedFlightRecoveryDirectionTicks <= 0) {
                    this.blockedFlightRecoveryDirection = Vec3.ZERO;
                }
            }
            if (this.forcedIdleAnimationTicks > 0) {
                --this.forcedIdleAnimationTicks;
            }
        }
        if (this.getBehaviorState().isAirborne()) {
            this.faceMovementDirection(this.getDeltaMovement());
        } else {
            this.tickGroundMovementFacing();
        }
    }

    public boolean hurt(DamageSource damageSource, float amount) {
        boolean hurt = super.hurt(damageSource, amount);
        if (hurt && !this.level().isClientSide) {
            this.clearEatingFish();
            Entity attacker = damageSource.getEntity();
            Vec3 sourcePos = attacker != null ? attacker.position() : this.position();
            this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 240);
            if (this.isBirdSleeping()) {
                this.getNavigation().stop();
                this.setBehaviorState(NightHeronBehaviorState.ALERT_FREEZE);
            }
            if (BirdConfigManager.aprilFoolsMode() && attacker instanceof Player) {
                return true;
            }
            this.receiveFlockFright(sourcePos, true);
            this.rememberFright(true);
        }
        return hurt;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putBoolean("NoGravity", false);
        this.birdBrain.save(compoundTag);
        BirdModelScale.save(compoundTag, this.getIndividualModelScale(), this.modelScaleProfile());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.clearSerializedFlightState();
        this.birdBrain.load(compoundTag);
        if (compoundTag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.setIndividualModelScale(BirdModelScale.load(compoundTag, this.modelScaleProfile()));
        } else {
            this.randomizeModelScale();
        }
    }

    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        float score = super.getWalkTargetValue(pos, level);
        BlockState below = level.getBlockState(pos.below());
        if (NightHeronEntity.isNearWaterForWorldgen(level, pos, 4)) {
            score += 8.0f;
        }
        if (NightHeronEntity.isWaterEdgeForWorldgen(level, pos)) {
            score += 6.0f;
        }
        if (below.is(Blocks.MUD) || below.is(Blocks.CLAY) || below.is(Blocks.SAND) || below.is(BlockTags.DIRT)) {
            score += 2.0f;
        }
        return score;
    }

    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.NIGHT_HERON_AMBIENT.get();
    }

    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return GuaniaoSoundEvents.NIGHT_HERON_HURT.get();
    }

    protected SoundEvent getDeathSound() {
        return GuaniaoSoundEvents.NIGHT_HERON_DEATH.get();
    }

    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return this.isControlledFlightActive() || super.isNoGravity();
    }

    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.12f, 0.9f);
    }

    public int getAmbientSoundInterval() {
        return BirdFlockSoundLimiter.scaledAmbientInterval(this, 240);
    }

    @Override
    public void playAmbientSound() {
        if (BirdFlockSoundLimiter.allowAmbient(this)) {
            super.playAmbientSound();
        }
    }

    public float getSoundVolume() {
        return 0.5f;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, BirdSoundVolume.apply(this, volume), pitch);
    }

    boolean isActiveTime() {
        return BirdActivitySchedule.NOCTURNAL_CREPUSCULAR.isActiveTime(this.level().getDayTime());
    }

    boolean shouldRoost() {
        return !this.isActiveTime();
    }

    boolean isRoosting() {
        return this.getBehaviorState() == NightHeronBehaviorState.ROOSTING || this.shouldRoost();
    }

    boolean isRestInterrupted() {
        return this.restInterruptionTicks > 0;
    }

    @Override
    public boolean isBirdSleeping() {
        return this.getBehaviorState() == NightHeronBehaviorState.ROOSTING;
    }

    @Override
    public void wakeFromLoudSound(Vec3 soundPosition) {
        if (this.level().isClientSide || !this.isBirdSleeping()) {
            return;
        }
        Vec3 sourcePos = soundPosition == null ? this.position() : soundPosition;
        this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 240);
        this.getNavigation().stop();
        this.setBehaviorState(NightHeronBehaviorState.ALERT_FREEZE);
        this.getLookControl().setLookAt(sourcePos.x, sourcePos.y, sourcePos.z, 35.0F, this.getMaxHeadXRot());
        this.receiveFlockFright(sourcePos, false);
    }

    public ItemStack getHeldFishForRendering() {
        return this.entityData == null ? ItemStack.EMPTY : this.entityData.get(HELD_FISH);
    }

    public int getHeldFishPoseSeed() {
        return this.entityData == null ? 0 : this.entityData.get(HELD_FISH_POSE_SEED);
    }

    public boolean hasHeldFishForRendering() {
        return !this.getHeldFishForRendering().isEmpty();
    }

    public boolean isEatingFish() {
        return this.entityData != null && ((Integer)this.entityData.get(EATING_TICKS) > 0 || this.hasHeldFishForRendering());
    }

    public NightHeronBehaviorState getBehaviorState() {
        if (this.entityData != null) {
            return NightHeronEntity.decodeBehaviorState((Integer)this.entityData.get(BEHAVIOR_STATE));
        }
        return this.behaviorState;
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.NIGHT_HERON;
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

    public BirdBrain birdBrain() {
        return this.birdBrain;
    }

    @Override
    public BirdFlightProfile birdFlightProfile() {
        return BirdFlightProfile.NIGHT_HERON;
    }

    @Override
    public boolean isBirdFlightActive() {
        return this.isControlledFlightActive() || this.getBehaviorState().isAirborne();
    }

    @Override
    public boolean isBirdLanding() {
        return this.getBehaviorState() == NightHeronBehaviorState.LANDING;
    }

    @Override
    public boolean isBirdEscaping() {
        return this.getBehaviorState().isEscape();
    }

    public void startFlybyFlight(Vec3 direction, int ticks) {
        this.startFlybyFlight(direction, null, ticks);
    }

    public void startFlybyFlight(Vec3 direction, BlockPos landingTarget, int ticks) {
        this.clearEatingFish();
        this.clearExternalFright();
        this.flybyFlightDirection = this.normalizeHorizontal(direction);
        this.flybyFlightTicks = Math.max(80, ticks);
        this.flybyLandingTarget = landingTarget == null ? NightHeronLandingSelector.findTransitLanding(this, 22, 74) : landingTarget.immutable();
        this.getNavigation().stop();
        this.markTakeoffFlapping();
        this.setBehaviorState(NightHeronBehaviorState.HIGH_TRANSIT);
        this.setOnGround(false);
        Vec3 movement = this.flybyFlightDirection.scale(0.44).add(0.0, 0.06, 0.0);
        this.setDeltaMovement(movement);
        this.faceMovementDirection(movement);
        this.fallDistance = 0.0f;
        this.hasImpulse = true;
    }

    @Override
    public boolean startBirdBathMountFlight(Vec3 standPosition) {
        if (standPosition == null || this.isControlledFlightActive()) {
            return false;
        }
        Vec3 horizontal = standPosition.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() <= 1.0E-4D) {
            horizontal = Vec3.ZERO;
        } else {
            horizontal = horizontal.normalize().scale(0.24D);
        }
        Vec3 movement = new Vec3(horizontal.x, 0.62D, horizontal.z);
        this.getNavigation().stop();
        this.markTakeoffFlapping();
        this.setOnGround(false);
        this.setDeltaMovement(movement);
        this.faceMovementDirection(movement);
        this.fallDistance = 0.0F;
        this.hasImpulse = true;
        return true;
    }

    @Override
    public void startBirdBathFeedingAnimation(BirdBathContentType contentType, int ticks) {
        this.getNavigation().stop();
        if (contentType == BirdBathContentType.FISH) {
            this.showHeldFoodDuringBirdBathFeeding(new ItemStack(Items.COD), ticks);
            return;
        }
        if (contentType == BirdBathContentType.MEAT) {
            this.showHeldFoodDuringBirdBathFeeding(new ItemStack(Items.CHICKEN), ticks);
            return;
        }
        if (contentType == BirdBathContentType.BREAD) {
            this.showHeldFoodDuringBirdBathFeeding(new ItemStack(Items.BREAD), ticks);
            return;
        }
        this.setBehaviorState(NightHeronBehaviorState.FORAGING);
        this.forcedIdleAnimationTicks = Math.max(this.forcedIdleAnimationTicks, Math.max(24, ticks / 2));
    }

    public void setGuidePreviewAnimation(GuidePreviewAnimation guidePreviewAnimation) {
        this.guidePreviewAnimation = guidePreviewAnimation == null ? GuidePreviewAnimation.NONE : guidePreviewAnimation;
    }

    public GuidePreviewAnimation getGuidePreviewAnimation() {
        return this.guidePreviewAnimation;
    }

    void setBehaviorState(NightHeronBehaviorState behaviorState) {
        this.behaviorState = behaviorState;
        if (this.entityData != null) {
            this.entityData.set(BEHAVIOR_STATE, behaviorState.ordinal());
        }
        if (!behaviorState.isAirborne()) {
            this.controlledFlightTicks = 0;
            this.groundedAirborneTicks = 0;
            this.resetFlightObstructionProbe();
        }
        if (behaviorState != NightHeronBehaviorState.PREEN && behaviorState != NightHeronBehaviorState.NECK_STRETCH && behaviorState != NightHeronBehaviorState.EATING) {
            this.forcedIdleAnimationTicks = 0;
        }
    }

    void triggerPreen() {
        this.setBehaviorState(NightHeronBehaviorState.PREEN);
        this.currentIdleAnimation = IdleAnimationChoice.SCRATCH;
        this.forcedIdleAnimationTicks = 88;
        this.nextIdleAnimationSwapTick = this.level().getGameTime() + (long)this.forcedIdleAnimationTicks;
    }

    void triggerNeckStretch() {
        this.setBehaviorState(NightHeronBehaviorState.NECK_STRETCH);
        this.currentIdleAnimation = switch (this.getRandom().nextInt(4)) {
            case 0 -> IdleAnimationChoice.LONG_NECK_1;
            case 1 -> IdleAnimationChoice.LONG_NECK_2;
            case 2 -> IdleAnimationChoice.LONG_NECK_3;
            default -> IdleAnimationChoice.LONG_NECK_5;
        };
        this.forcedIdleAnimationTicks = 76;
        this.nextIdleAnimationSwapTick = this.level().getGameTime() + (long)this.forcedIdleAnimationTicks;
    }

    void markTakeoffFlapping() {
        this.takeoffFlapTicks = 20;
        this.controlledFlightTicks = 0;
        this.groundedAirborneTicks = 0;
        this.resetFlightObstructionProbe();
        this.clearLandingApproach();
    }

    boolean isTakeoffFlapping() {
        return this.takeoffFlapTicks > 0;
    }

    int getControlledFlightTicks() {
        return this.controlledFlightTicks;
    }

    boolean isControlledFlightActive() {
        if (!this.getBehaviorState().isAirborne()) {
            return false;
        }
        return !this.onGround() || this.getDeltaMovement().y > 0.04;
    }

    void finishFlight(NightHeronBehaviorState nextState) {
        this.flybyFlightTicks = 0;
        this.flybyFlightDirection = Vec3.ZERO;
        this.flybyLandingTarget = null;
        this.takeoffFlapTicks = 0;
        this.controlledFlightTicks = 0;
        this.groundedAirborneTicks = 0;
        this.resetFlightObstructionProbe();
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0f;
        this.hasImpulse = true;
        this.clearLandingApproach();
        this.setBehaviorState(nextState);
    }

    void settleInterruptedFlight(NightHeronBehaviorState groundedState) {
        if (!this.getBehaviorState().isAirborne()) {
            return;
        }
        if (this.onGround()) {
            this.finishFlight(groundedState);
            return;
        }
        this.takeoffFlapTicks = 0;
        this.groundedAirborneTicks = 0;
        this.resetFlightObstructionProbe();
        this.getNavigation().stop();
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.x * 0.45, Math.min(movement.y, -0.045), movement.z * 0.45);
        this.fallDistance = 0.0f;
        this.hasImpulse = true;
        this.setBehaviorState(NightHeronBehaviorState.LANDING);
    }

    Vec3 updateLandingApproachDirection(BlockPos landingTarget, Vec3 preferredDirection, double correctionWeight) {
        boolean newTarget;
        Vec3 preferred = this.normalizeHorizontal(preferredDirection);
        boolean bl = newTarget = this.landingApproachTarget == null || !this.landingApproachTarget.equals((Object)landingTarget);
        if (newTarget || this.landingApproachDirection.lengthSqr() <= 1.0E-4) {
            this.landingApproachTarget = landingTarget.immutable();
            this.landingApproachDirection = preferred;
            return this.landingApproachDirection;
        }
        double dot = this.landingApproachDirection.dot(preferred);
        if (dot < -0.25) {
            preferred = this.landingApproachDirection;
            correctionWeight = 0.0;
        }
        correctionWeight = Mth.clamp((double)correctionWeight, (double)0.0, (double)0.35);
        Vec3 blended = this.landingApproachDirection.scale(1.0 - correctionWeight).add(preferred.scale(correctionWeight));
        this.landingApproachDirection = this.normalizeHorizontal(blended);
        return this.landingApproachDirection;
    }

    void clearLandingApproach() {
        this.landingApproachTarget = null;
        this.landingApproachDirection = Vec3.ZERO;
    }

    void markBlockedFlightRecovery() {
        this.blockedFlightRecoveryActivityTicks = 70;
    }

    boolean hasBlockedFlightRecoveryActivity() {
        return this.blockedFlightRecoveryActivityTicks > 0;
    }

    boolean consumeBlockedFlightRecoveryActivity() {
        if (this.blockedFlightRecoveryActivityTicks <= 0) {
            return false;
        }
        this.blockedFlightRecoveryActivityTicks = 0;
        return true;
    }

    boolean tickFlightObstructionProbe(boolean pathBlocked) {
        boolean stagnant;
        if (!this.getBehaviorState().isAirborne() || this.onGround()) {
            this.resetFlightObstructionProbe();
            return false;
        }
        Vec3 currentPosition = this.position();
        if (this.lastControlledFlightPosition == Vec3.ZERO) {
            this.lastControlledFlightPosition = currentPosition;
            return false;
        }
        double progressSqr = currentPosition.subtract(this.lastControlledFlightPosition).horizontalDistanceSqr();
        boolean bl = stagnant = this.controlledFlightTicks > 8 && progressSqr < 0.0025;
        this.obstructedFlightTicks = pathBlocked || this.horizontalCollision || stagnant ? ++this.obstructedFlightTicks : Math.max(0, this.obstructedFlightTicks - 2);
        this.lastControlledFlightPosition = currentPosition;
        return this.obstructedFlightTicks >= 5;
    }

    void resetFlightObstructionProbe() {
        this.obstructedFlightTicks = 0;
        this.lastControlledFlightPosition = Vec3.ZERO;
        this.blockedFlightRecoveryDirectionTicks = 0;
        this.blockedFlightRecoveryDirection = Vec3.ZERO;
    }

    Vec3 getBlockedFlightRecoveryDirection() {
        return this.blockedFlightRecoveryDirectionTicks > 0 ? this.blockedFlightRecoveryDirection : Vec3.ZERO;
    }

    void lockBlockedFlightRecoveryDirection(Vec3 direction, int ticks) {
        this.blockedFlightRecoveryDirection = this.normalizeHorizontal(direction);
        this.blockedFlightRecoveryDirectionTicks = ticks;
    }

    private Vec3 normalizeHorizontal(Vec3 vector) {
        Vec3 horizontal = new Vec3(vector.x, 0.0, vector.z);
        if (horizontal.lengthSqr() <= 1.0E-4) {
            Vec3 movement = this.getDeltaMovement();
            horizontal = new Vec3(movement.x, 0.0, movement.z);
        }
        if (horizontal.lengthSqr() <= 1.0E-4) {
            Vec3 look = this.getLookAngle();
            horizontal = new Vec3(look.x, 0.0, look.z);
        }
        if (horizontal.lengthSqr() <= 1.0E-4) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        return horizontal.normalize();
    }

    boolean isTemptingPlayer(Player player) {
        return BirdFoodSafety.matchesClean(BirdTags.NIGHT_HERON_FOODS, player.getMainHandItem())
                || BirdFoodSafety.matchesClean(BirdTags.NIGHT_HERON_FOODS, player.getOffhandItem());
    }

    boolean canStrikePrey() {
        return this.preyStrikeCooldown <= 0;
    }

    boolean canEatThrownFish() {
        NightHeronBehaviorState state = this.getBehaviorState();
        return this.pollutedFoodReactionTicks <= 0
                && !this.isEatingFish()
                && this.thrownFishEatCooldown <= 0
                && this.preyStrikeCooldown <= 0
                && this.onGround()
                && !state.isAirborne()
                && !state.isEscape()
                && state != NightHeronBehaviorState.ROOSTING
                && this.getTarget() == null
                && !this.hasExternalFright()
                && this.birdBrain().motivation().fear() < 0.55F;
    }

    private boolean canUseBirdBath() {
        NightHeronBehaviorState state = this.getBehaviorState();
        return this.pollutedFoodReactionTicks <= 0
                && !this.isEatingFish()
                && this.thrownFishEatCooldown <= 0
                && this.onGround()
                && !state.isAirborne()
                && !state.isEscape()
                && state != NightHeronBehaviorState.ROOSTING
                && this.getTarget() == null
                && !this.hasExternalFright()
                && this.birdBrain().motivation().fear() < 0.60F;
    }

    private void consumeBirdBathServing(EdDYON.guaniao.content.bath.BirdBathBlockEntity bath, BirdBathContentType contentType) {
        if (contentType == BirdBathContentType.FISH) {
            this.startEatingFish(new ItemStack(Items.COD), 45 + this.getRandom().nextInt(21));
            return;
        }
        this.birdBrain.onEat(0.18F);
        this.setBehaviorState(NightHeronBehaviorState.FORAGING);
        this.forcedIdleAnimationTicks = Math.max(this.forcedIdleAnimationTicks, 24);
        this.playSound(SoundEvents.GENERIC_DRINK, 0.42F, 0.9F + this.getRandom().nextFloat() * 0.12F);
    }

    public static boolean isEdibleFishItem(ItemStack stack) {
        return BirdFoodSafety.matchesClean(BirdTags.NIGHT_HERON_FOODS, stack);
    }

    static boolean isDroppedFishCandidate(ItemStack stack) {
        return BirdFoodSafety.matchesDroppedFoodCandidate(BirdTags.NIGHT_HERON_FOODS, stack);
    }

    void eatThrownFish(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        if (!NightHeronEntity.isDroppedFishCandidate(stack)) {
            return;
        }
        ItemStack shownStack = stack.copy();
        shownStack.setCount(1);
        stack.shrink(1);
        if (stack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(stack);
        }
        if (BirdFoodSafety.isPollutedFood(shownStack)) {
            this.startPollutedFoodReaction(shownStack, itemEntity.position());
        } else {
            this.startEatingFish(shownStack, 45 + this.getRandom().nextInt(21));
        }
    }

    private void startPollutedFoodReaction(ItemStack foodStack, Vec3 sourcePos) {
        ItemStack copy = foodStack.copy();
        copy.setCount(1);
        this.entityData.set(HELD_FISH, copy);
        this.entityData.set(HELD_FISH_POSE_SEED, this.nextHeldFishPoseSeed());
        this.entityData.set(EATING_TICKS, POLLUTED_FOOD_REACTION_TICKS);
        this.pollutedFoodReactionTicks = POLLUTED_FOOD_REACTION_TICKS;
        this.pollutedFoodSource = sourcePos == null ? this.position() : sourcePos;
        this.getNavigation().stop();
        this.setBehaviorState(NightHeronBehaviorState.EATING);
        this.forcedIdleAnimationTicks = Math.max(this.forcedIdleAnimationTicks, POLLUTED_FOOD_REACTION_TICKS);
    }

    private void tickPollutedFoodReaction() {
        if (this.pollutedFoodReactionTicks <= 0) {
            return;
        }

        --this.pollutedFoodReactionTicks;
        Player witness = PollutedFoodReactionUtil.nearestWitness(this, 16.0D);
        PollutedFoodReactionUtil.lookAtWitness(this, witness);
        this.getNavigation().stop();

        if (this.pollutedFoodReactionTicks == POLLUTED_FOOD_SPIT_TICK && this.hasHeldFishForRendering()) {
            this.playSound(GuaniaoSoundEvents.NIGHT_HERON_AMBIENT.get(), 0.95F, 1.08F + this.getRandom().nextFloat() * 0.18F);
            ItemStack spatFood = this.getHeldFishForRendering().copy();
            this.entityData.set(HELD_FISH, ItemStack.EMPTY);
            this.entityData.set(HELD_FISH_POSE_SEED, 0);
            this.entityData.set(EATING_TICKS, 0);
            this.setBehaviorState(NightHeronBehaviorState.ALERT_FREEZE);
            PollutedFoodReactionUtil.spit(this, spatFood);
        }

        if (this.pollutedFoodReactionTicks == POLLUTED_FOOD_FLEE_TICK) {
            Vec3 source = witness == null ? this.pollutedFoodSource : witness.position();
            Vec3 away = this.position().subtract(source).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() <= 1.0E-4D) {
                away = this.normalizeHorizontal(new Vec3(this.getRandom().nextDouble() - 0.5D, 0.0D, this.getRandom().nextDouble() - 0.5D));
            }
            this.pollutedFoodReactionTicks = 0;
            this.pollutedFoodSource = Vec3.ZERO;
            this.thrownFishEatCooldown = Math.max(this.thrownFishEatCooldown, 300);
            this.startFlybyFlight(away, 120);
        }
    }

    void startEatingFish(ItemStack fishStack, int ticks) {
        ItemStack copy = fishStack.copy();
        copy.setCount(1);
        this.entityData.set(HELD_FISH, copy);
        this.entityData.set(HELD_FISH_POSE_SEED, this.nextHeldFishPoseSeed());
        this.entityData.set(EATING_TICKS, Math.max(1, ticks));
        this.getNavigation().stop();
        this.setBehaviorState(NightHeronBehaviorState.EATING);
        this.forcedIdleAnimationTicks = Math.max(this.forcedIdleAnimationTicks, ticks);
        this.nextIdleAnimationSwapTick = this.level().getGameTime() + (long)ticks;
        this.birdBrain.onEat(0.45F);
        this.playSound(GuaniaoSoundEvents.NIGHT_HERON_ATTACK.get(), 0.55f, 0.9f + this.getRandom().nextFloat() * 0.18f);
    }

    private void showHeldFoodDuringBirdBathFeeding(ItemStack foodStack, int ticks) {
        ItemStack copy = foodStack.copy();
        copy.setCount(1);
        this.entityData.set(HELD_FISH, copy);
        this.entityData.set(HELD_FISH_POSE_SEED, this.nextHeldFishPoseSeed());
        this.entityData.set(EATING_TICKS, Math.max(1, ticks));
        this.getNavigation().stop();
        this.setBehaviorState(NightHeronBehaviorState.EATING);
        this.forcedIdleAnimationTicks = Math.max(this.forcedIdleAnimationTicks, ticks);
        this.nextIdleAnimationSwapTick = this.level().getGameTime() + (long)ticks;
    }

    private void tickEatingFish() {
        if (this.pollutedFoodReactionTicks > 0) {
            return;
        }
        int ticks = this.entityData.get(EATING_TICKS);
        if (ticks <= 0) {
            if (this.hasHeldFishForRendering()) {
                this.clearEatingFish();
            }
            return;
        }
        this.entityData.set(EATING_TICKS, ticks - 1);
        this.getNavigation().stop();
        if (!this.getBehaviorState().isEscape()) {
            this.setBehaviorState(NightHeronBehaviorState.EATING);
        }
        if (ticks - 1 <= 0) {
            this.clearEatingFish();
        }
    }

    private void clearEatingFish() {
        boolean wasEating = this.isEatingFish();
        this.entityData.set(HELD_FISH, ItemStack.EMPTY);
        this.entityData.set(HELD_FISH_POSE_SEED, 0);
        this.entityData.set(EATING_TICKS, 0);
        if (wasEating) {
            this.thrownFishEatCooldown = Math.max(this.thrownFishEatCooldown, 80 + this.getRandom().nextInt(81));
        }
        if (this.getBehaviorState() == NightHeronBehaviorState.EATING) {
            this.setBehaviorState(NightHeronBehaviorState.IDLE);
        }
    }

    private int nextHeldFishPoseSeed() {
        int seed = this.getRandom().nextInt();
        return seed == 0 ? 1 : seed;
    }

    void afterPreyStrike() {
        this.preyStrikeCooldown = 45;
        this.playSound(GuaniaoSoundEvents.NIGHT_HERON_ATTACK.get(), 0.65f, 0.9f + this.getRandom().nextFloat() * 0.18f);
        this.heal(0.5f);
    }

    boolean catchFish(AbstractFish fish) {
        if (fish == null || !fish.isAlive() || this.isEatingFish()) {
            return false;
        }
        ItemStack caughtFish = new ItemStack(this.itemForCaughtFish(fish));
        fish.discard();
        this.preyStrikeCooldown = NightHeronDefinition.FORAGE_ATTACK_COOLDOWN;
        this.startEatingFish(caughtFish, 45 + this.getRandom().nextInt(21));
        this.heal(0.5F);
        return true;
    }

    private net.minecraft.world.item.Item itemForCaughtFish(AbstractFish fish) {
        if (fish.getType() == EntityType.SALMON) {
            return Items.SALMON;
        }
        if (fish.getType() == EntityType.TROPICAL_FISH) {
            return Items.TROPICAL_FISH;
        }
        if (fish.getType() == EntityType.PUFFERFISH) {
            return Items.PUFFERFISH;
        }
        return Items.COD;
    }

    void rememberFright(boolean severe) {
        this.frightMemoryTicks = 220;
        this.recentFrightCount = Math.min(6, this.recentFrightCount + (severe ? 2 : 1));
    }

    int getRecentFrightCount() {
        return this.recentFrightCount;
    }

    void receiveFlockFright(Vec3 source, boolean severe) {
        this.clearEatingFish();
        this.externalFrightSource = source;
        this.externalFrightTicks = severe ? 100 : 55;
        this.severeExternalFright = severe;
        if (severe) {
            this.rememberFright(true);
        }
    }

    boolean hasExternalFright() {
        return this.externalFrightTicks > 0 && this.externalFrightSource != null;
    }

    boolean hasSevereExternalFright() {
        return this.hasExternalFright() && this.severeExternalFright;
    }

    Vec3 getExternalFrightSource() {
        return this.hasExternalFright() ? this.externalFrightSource : null;
    }

    void clearExternalFright() {
        this.externalFrightTicks = 0;
        this.externalFrightSource = null;
        this.severeExternalFright = false;
    }

    private void tickFlybyFlight() {
        if (this.flybyFlightTicks <= 0) {
            return;
        }
        if (!this.getBehaviorState().isAirborne()) {
            this.flybyFlightTicks = 0;
            this.flybyFlightDirection = Vec3.ZERO;
            this.flybyLandingTarget = null;
            return;
        }
        this.getNavigation().stop();
        --this.flybyFlightTicks;
        if (this.flybyLandingTarget != null && NightHeronFlightController.shouldBeginLandingApproach(this, this.flybyLandingTarget, this.flybyFlightTicks, 28.0)) {
            if (NightHeronFlightController.tickLandingApproach(this, this.flybyLandingTarget)) {
                this.flybyFlightTicks = 0;
                this.flybyFlightDirection = Vec3.ZERO;
                this.flybyLandingTarget = null;
            } else if (this.flybyFlightTicks <= 0) {
                this.flybyFlightTicks = 1;
            }
            return;
        }
        if (this.flybyFlightTicks <= 0) {
            if (this.flybyLandingTarget == null) {
                this.flybyLandingTarget = NightHeronLandingSelector.findTransitLanding(this, 8, 36);
            }
            if (this.flybyLandingTarget != null) {
                if (NightHeronFlightController.tickLandingApproach(this, this.flybyLandingTarget)) {
                    this.flybyFlightDirection = Vec3.ZERO;
                    this.flybyLandingTarget = null;
                } else {
                    this.flybyFlightTicks = 1;
                }
            } else {
                NightHeronFlightController.tickOpenLanding(this, this.flybyFlightDirection);
                if (!this.onGround()) {
                    this.flybyFlightTicks = 1;
                }
            }
            return;
        }
        NightHeronFlightController.tickHighTransitFlight(this, this.flybyFlightDirection);
    }

    void faceMovementDirection(Vec3 movement) {
        BirdFlightController.faceMovement(this, movement, BirdFlightProfile.NIGHT_HERON.maxPitchDegrees());
    }

    private void tickGroundMovementFacing() {
        if (!this.shouldFaceGroundMovement()) {
            return;
        }
        BirdFlightController.faceGroundMovement(this, this.getDeltaMovement(), 1.0E-4D);
    }

    private boolean shouldFaceGroundMovement() {
        if (!this.onGround()
                || this.isControlledFlightActive()
                || this.isInWaterOrBubble()
                || this.isPassenger()) {
            return false;
        }
        NightHeronBehaviorState state = this.getBehaviorState();
        if (state.isAirborne()
                || state == NightHeronBehaviorState.EATING
                || state == NightHeronBehaviorState.PREEN
                || state == NightHeronBehaviorState.NECK_STRETCH
                || state == NightHeronBehaviorState.REST_STAND
                || state == NightHeronBehaviorState.LOOK_AROUND
                || state == NightHeronBehaviorState.ALERT_FREEZE
                || state == NightHeronBehaviorState.ROOSTING) {
            return false;
        }
        return BirdGroundAnimation.hasWalkMotion(this);
    }

    double heightAboveSurface() {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int x = Mth.floor((double)this.getX());
        int z = Mth.floor((double)this.getZ());
        int startY = Mth.floor((double)this.getY());
        int minY = this.level().getMinBuildHeight();
        for (int y = startY; y >= minY; --y) {
            mutablePos.set(x, y, z);
            BlockState state = this.level().getBlockState((BlockPos)mutablePos);
            if (state.getCollisionShape((BlockGetter)this.level(), (BlockPos)mutablePos).isEmpty() && !this.level().getFluidState((BlockPos)mutablePos).is(FluidTags.WATER)) continue;
            return Math.max(0.0, this.getY() - ((double)y + 1.0));
        }
        return 18.0;
    }

    boolean isNearWater(BlockPos pos, int radius) {
        return NightHeronEntity.isNearWater((LevelReader)this.level(), pos, radius);
    }

    static boolean isWaterEdge(LevelReader level, BlockPos pos) {
        if (!NightHeronEntity.canReadChunk(level, pos)) {
            return false;
        }
        if (level.getFluidState(pos).is(FluidTags.WATER) && !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacentPos = pos.relative(direction);
            if (!NightHeronEntity.canReadChunk(level, adjacentPos) || !level.getFluidState(adjacentPos).is(FluidTags.WATER) && !level.getFluidState(adjacentPos.below()).is(FluidTags.WATER)) continue;
            return true;
        }
        return false;
    }

    static boolean isNearWater(LevelReader level, BlockPos pos, int radius) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int xOffset = -radius; xOffset <= radius; ++xOffset) {
            for (int zOffset = -radius; zOffset <= radius; ++zOffset) {
                if (xOffset * xOffset + zOffset * zOffset > radius * radius) continue;
                for (int yOffset = -1; yOffset <= 1; ++yOffset) {
                    mutablePos.set(pos.getX() + xOffset, pos.getY() + yOffset, pos.getZ() + zOffset);
                    if (!NightHeronEntity.canReadChunk(level, (BlockPos)mutablePos) || !level.getFluidState((BlockPos)mutablePos).is(FluidTags.WATER)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNearWaterForWorldgen(LevelReader level, BlockPos pos, int radius) {
        if (level instanceof WorldGenRegion) {
            return NightHeronEntity.isNearWaterInSpawnChunk(level, pos, radius);
        }
        return NightHeronEntity.isNearWater(level, pos, radius);
    }

    private static boolean isWaterEdgeForWorldgen(LevelReader level, BlockPos pos) {
        if (level instanceof WorldGenRegion) {
            return NightHeronEntity.isWaterEdgeInSpawnChunk(level, pos);
        }
        return NightHeronEntity.isWaterEdge(level, pos);
    }

    private static boolean isWaterEdgeInSpawnChunk(LevelReader level, BlockPos pos) {
        int spawnChunkX = SectionPos.blockToSectionCoord((int)pos.getX());
        int spawnChunkZ = SectionPos.blockToSectionCoord((int)pos.getZ());
        if (level.getFluidState(pos).is(FluidTags.WATER) && !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacentPos = pos.relative(direction);
            if (!NightHeronEntity.isInChunk(adjacentPos, spawnChunkX, spawnChunkZ) || !level.getFluidState(adjacentPos).is(FluidTags.WATER) && !level.getFluidState(adjacentPos.below()).is(FluidTags.WATER)) continue;
            return true;
        }
        return false;
    }

    private static boolean isNearWaterInSpawnChunk(LevelReader level, BlockPos pos, int radius) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int spawnChunkX = SectionPos.blockToSectionCoord((int)pos.getX());
        int spawnChunkZ = SectionPos.blockToSectionCoord((int)pos.getZ());
        for (int xOffset = -radius; xOffset <= radius; ++xOffset) {
            for (int zOffset = -radius; zOffset <= radius; ++zOffset) {
                int z;
                int x;
                if (xOffset * xOffset + zOffset * zOffset > radius * radius || !NightHeronEntity.isInChunk(x = pos.getX() + xOffset, z = pos.getZ() + zOffset, spawnChunkX, spawnChunkZ)) continue;
                for (int yOffset = -1; yOffset <= 1; ++yOffset) {
                    mutablePos.set(x, pos.getY() + yOffset, z);
                    if (!level.getFluidState((BlockPos)mutablePos).is(FluidTags.WATER)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isInChunk(BlockPos pos, int chunkX, int chunkZ) {
        return NightHeronEntity.isInChunk(pos.getX(), pos.getZ(), chunkX, chunkZ);
    }

    private static boolean isInChunk(int x, int z, int chunkX, int chunkZ) {
        return SectionPos.blockToSectionCoord((int)x) == chunkX && SectionPos.blockToSectionCoord((int)z) == chunkZ;
    }

    static boolean canReadChunk(LevelReader level, BlockPos pos) {
        return level.hasChunk(SectionPos.blockToSectionCoord((int)pos.getX()), SectionPos.blockToSectionCoord((int)pos.getZ()));
    }

    private RawAnimation pickIdleAnimation() {
        if (this.forcedIdleAnimationTicks > 0) {
            return this.currentIdleAnimation.animation;
        }
        if (this.level().getGameTime() >= this.nextIdleAnimationSwapTick) {
            this.currentIdleAnimation = this.chooseIdleAnimation();
            this.nextIdleAnimationSwapTick = this.level().getGameTime() + (long)this.currentIdleAnimation.nextDuration(this.getRandom());
        }
        return this.currentIdleAnimation.animation;
    }

    private IdleAnimationChoice chooseIdleAnimation() {
        int roll = this.getRandom().nextInt(100);
        NightHeronBehaviorState state = this.getBehaviorState();
        if (state == NightHeronBehaviorState.WATER_EDGE_WAIT || state == NightHeronBehaviorState.FORAGING) {
            if (roll < 58) {
                return IdleAnimationChoice.BASE;
            }
            if (roll < 73) {
                return IdleAnimationChoice.LONG_NECK_1;
            }
            if (roll < 86) {
                return IdleAnimationChoice.LONG_NECK_3;
            }
            if (roll < 92) {
                return IdleAnimationChoice.SCRATCH;
            }
            return IdleAnimationChoice.LONG_NECK_5;
        }
        if (this.isRoosting()) {
            if (roll < 48) {
                return IdleAnimationChoice.BASE;
            }
            if (roll < 63) {
                return IdleAnimationChoice.LONG_NECK_1;
            }
            if (roll < 78) {
                return IdleAnimationChoice.LONG_NECK_2;
            }
            if (roll < 90) {
                return IdleAnimationChoice.SCRATCH;
            }
            return IdleAnimationChoice.LONG_NECK_5;
        }
        if (roll < 60) {
            return IdleAnimationChoice.BASE;
        }
        if (roll < 70) {
            return IdleAnimationChoice.LONG_NECK_1;
        }
        if (roll < 80) {
            return IdleAnimationChoice.LONG_NECK_2;
        }
        if (roll < 89) {
            return IdleAnimationChoice.LONG_NECK_3;
        }
        if (roll < 94) {
            return IdleAnimationChoice.SCRATCH;
        }
        return IdleAnimationChoice.LONG_NECK_5;
    }

    private boolean shouldUseFlyingAnimation() {
        if (this.isInWaterOrBubble()) {
            return false;
        }
        return BirdFlightController.shouldPlayFlyAnimation(
                this,
                this.getBehaviorState().isAirborne(),
                this.onGround(),
                this.isNoGravity(),
                this.getDeltaMovement(),
                this.groundedAirborneTicks);
    }

    private boolean shouldPlayWalkAnimation(NightHeronBehaviorState state, boolean animationMoving) {
        if (!BirdGroundAnimation.hasWalkMotion(this, animationMoving)) {
            return false;
        }
        if (animationMoving) {
            return !state.isAirborne();
        }
        return !state.isAirborne()
                && state != NightHeronBehaviorState.EATING
                && state != NightHeronBehaviorState.PREEN
                && state != NightHeronBehaviorState.NECK_STRETCH
                && state != NightHeronBehaviorState.REST_STAND
                && state != NightHeronBehaviorState.LOOK_AROUND
                && state != NightHeronBehaviorState.ALERT_FREEZE
                && state != NightHeronBehaviorState.ROOSTING;
    }

    private RawAnimation chooseFlyingAnimation() {
        NightHeronBehaviorState state = this.getBehaviorState();
        if (state == NightHeronBehaviorState.TAKEOFF || this.takeoffFlapTicks > 0) {
            return FLY_FLAPPING_WING_ANIMATION;
        }
        if (state == NightHeronBehaviorState.LOCAL_FLIGHT || state == NightHeronBehaviorState.LOW_FLAP_ESCAPE || state == NightHeronBehaviorState.CLIMB || state == NightHeronBehaviorState.LANDING) {
            return FLY_FLAPPING_WING_LOOP_ANIMATION;
        }
        boolean highFlight = state == NightHeronBehaviorState.HIGH_TRANSIT || state == NightHeronBehaviorState.LONG_FLIGHT_ESCAPE || state == NightHeronBehaviorState.SOARING || state == NightHeronBehaviorState.GLIDE;
        return highFlight && (NightHeronFlightController.shouldGlide(this) || state == NightHeronBehaviorState.GLIDE) ? FLY_LOOP_ANIMATION : FLY_FLAPPING_WING_LOOP_ANIMATION;
    }

    private <T extends NightHeronEntity> PlayState movementController(AnimationState<T> animationState) {
        RawAnimation guidePreviewRawAnimation = this.guidePreviewAnimation.animation();
        if (guidePreviewRawAnimation != null) {
            return animationState.setAndContinue(guidePreviewRawAnimation);
        }
        if (this.isEatingFish()) {
            return animationState.setAndContinue(EAT_ANIMATION);
        }
        if (this.shouldUseFlyingAnimation()) {
            return animationState.setAndContinue(this.chooseFlyingAnimation());
        }
        NightHeronBehaviorState state = this.getBehaviorState();
        if (state == NightHeronBehaviorState.ROOSTING) {
            return animationState.setAndContinue(SLEEP_ANIMATION);
        }
        double horizontalSpeed = this.getDeltaMovement().horizontalDistanceSqr();
        if (BirdGroundAnimation.canPlayWalk(this)
                && (state == NightHeronBehaviorState.RUN_ESCAPE || horizontalSpeed > RUNNING_SPEED_THRESHOLD)) {
            return animationState.setAndContinue(RUN_ANIMATION);
        }
        if (this.shouldPlayWalkAnimation(state, animationState.isMoving())) {
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        return animationState.setAndContinue(this.pickIdleAnimation());
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable)this, "movement", 4, this::movementController)});
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    private void clearSerializedFlightState() {
        this.takeoffFlapTicks = 0;
        this.controlledFlightTicks = 0;
        this.groundedAirborneTicks = 0;
        this.resetFlightObstructionProbe();
        this.clearLandingApproach();
        if (super.isNoGravity()) {
            this.setNoGravity(false);
        }
        if (this.getBehaviorState().isAirborne()) {
            this.setBehaviorState(NightHeronBehaviorState.IDLE);
        }
        if (!this.onGround() && this.getDeltaMovement().lengthSqr() < 0.0025) {
            this.setDeltaMovement(0.0, -0.08, 0.0);
            this.hasImpulse = true;
            this.fallDistance = 0.0f;
        }
    }

    private void tickStaleFlightRecovery() {
        if (!this.getBehaviorState().isAirborne()) {
            if (super.isNoGravity()) {
                this.setNoGravity(false);
                if (!this.onGround() && this.getDeltaMovement().lengthSqr() < 0.0025) {
                    this.setDeltaMovement(0.0, -0.08, 0.0);
                    this.hasImpulse = true;
                }
            }
            return;
        }
        if (!this.onGround() && this.controlledFlightTicks > 6 && this.getDeltaMovement().lengthSqr() < 0.0025) {
            this.setNoGravity(false);
            this.setBehaviorState(NightHeronBehaviorState.IDLE);
            this.setDeltaMovement(0.0, -0.08, 0.0);
            this.hasImpulse = true;
            this.fallDistance = 0.0f;
        }
    }

    private void tickWaterEscape() {
        if (!this.isInWaterOrBubble()) {
            return;
        }
        this.clearEatingFish();
        this.getNavigation().stop();
        BlockPos dryTarget = this.findNearestDryEscapePosition();
        Vec3 direction = dryTarget == null ? this.getLookAngle().multiply(1.0, 0.0, 1.0) : Vec3.atBottomCenterOf(dryTarget).subtract(this.position()).multiply(1.0, 0.0, 1.0);
        if (direction.lengthSqr() <= 1.0E-4) {
            double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
            direction = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        }
        direction = direction.normalize();
        this.markTakeoffFlapping();
        this.setBehaviorState(NightHeronBehaviorState.TAKEOFF);
        Vec3 movement = direction.scale(0.34).add(0.0, 0.36, 0.0);
        this.setDeltaMovement(movement);
        this.faceMovementDirection(movement);
        this.fallDistance = 0.0f;
        this.hasImpulse = true;
    }

    private BlockPos findNearestDryEscapePosition() {
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        int bestDistanceSqr = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int radius = 2; radius <= 9; ++radius) {
            for (int xOffset = -radius; xOffset <= radius; ++xOffset) {
                for (int zOffset = -radius; zOffset <= radius; ++zOffset) {
                    if (Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                        continue;
                    }
                    for (int yOffset = 3; yOffset >= -3; --yOffset) {
                        mutable.set(origin.getX() + xOffset, origin.getY() + yOffset, origin.getZ() + zOffset);
                        if (!this.isDryEscapePosition(mutable)) {
                            continue;
                        }
                        int distanceSqr = xOffset * xOffset + zOffset * zOffset + yOffset * yOffset;
                        if (distanceSqr < bestDistanceSqr) {
                            bestDistanceSqr = distanceSqr;
                            best = mutable.immutable();
                        }
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private boolean isDryEscapePosition(BlockPos pos) {
        if (!NightHeronEntity.canReadChunk((LevelReader)this.level(), pos)) {
            return false;
        }
        BlockState below = this.level().getBlockState(pos.below());
        BlockState feet = this.level().getBlockState(pos);
        BlockState head = this.level().getBlockState(pos.above());
        if (!feet.getCollisionShape((BlockGetter)this.level(), pos).isEmpty() || !head.getCollisionShape((BlockGetter)this.level(), pos.above()).isEmpty()) {
            return false;
        }
        if (this.level().getFluidState(pos).is(FluidTags.WATER)
                || this.level().getFluidState(pos).is(FluidTags.LAVA)
                || this.level().getFluidState(pos.below()).is(FluidTags.WATER)
                || this.level().getFluidState(pos.below()).is(FluidTags.LAVA)) {
            return false;
        }
        return below.isFaceSturdy((BlockGetter)this.level(), pos.below(), Direction.UP)
                || below.is(BlockTags.LEAVES)
                || below.is(BlockTags.LOGS)
                || below.is(Blocks.MUD)
                || below.is(Blocks.CLAY)
                || below.is(Blocks.SAND)
                || below.is(Blocks.RED_SAND);
    }

    private void tickFlightStateGuard() {
        NightHeronBehaviorState state = this.getBehaviorState();
        if (!state.isAirborne()) {
            this.controlledFlightTicks = 0;
            this.groundedAirborneTicks = 0;
            this.resetFlightObstructionProbe();
            return;
        }
        if (!this.onGround()) {
            ++this.controlledFlightTicks;
            this.groundedAirborneTicks = 0;
            if (this.controlledFlightTicks > MAX_CONTROLLED_FLIGHT_TICKS) {
                this.beginEmergencyDescent();
            }
            return;
        }
        if (this.takeoffFlapTicks > 0 && this.getDeltaMovement().y > 0.04) {
            return;
        }
        ++this.groundedAirborneTicks;
        if (this.groundedAirborneTicks >= 2) {
            this.finishFlight(NightHeronBehaviorState.IDLE);
        }
    }

    private void beginEmergencyDescent() {
        this.flybyFlightTicks = 0;
        this.flybyFlightDirection = Vec3.ZERO;
        this.flybyLandingTarget = null;
        this.takeoffFlapTicks = 0;
        this.controlledFlightTicks = 0;
        this.groundedAirborneTicks = 0;
        this.resetFlightObstructionProbe();
        this.clearLandingApproach();
        this.getNavigation().stop();
        this.setNoGravity(false);
        this.setBehaviorState(NightHeronBehaviorState.IDLE);
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.x * 0.45D, Math.min(movement.y, -0.06D), movement.z * 0.45D);
        this.fallDistance = 0.0F;
        this.hasImpulse = true;
    }

    private static NightHeronBehaviorState decodeBehaviorState(int ordinal) {
        NightHeronBehaviorState[] states = NightHeronBehaviorState.values();
        if (ordinal < 0 || ordinal >= states.length) {
            return NightHeronBehaviorState.IDLE;
        }
        return states[ordinal];
    }

    private void randomizeModelScale() {
        this.setIndividualModelScale(BirdModelScale.randomIndividualScale(this.getRandom(), this.modelScaleProfile()));
    }

    private static enum IdleAnimationChoice {
        BASE(IDLE_ANIMATION, 70, 130),
        LONG_NECK_1(IDLE_DIFF_1_ANIMATION, 62, 72),
        LONG_NECK_2(IDLE_DIFF_2_ANIMATION, 62, 74),
        LONG_NECK_3(IDLE_DIFF_3_ANIMATION, 62, 74),
        SCRATCH(IDLE_DIFF_4_ANIMATION, 78, 90),
        LONG_NECK_5(IDLE_DIFF_5_ANIMATION, 62, 78);

        private final RawAnimation animation;
        private final int minDuration;
        private final int maxDuration;

        private IdleAnimationChoice(RawAnimation animation, int minDuration, int maxDuration) {
            this.animation = animation;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
        }

        private int nextDuration(RandomSource random) {
            return this.minDuration + random.nextInt(this.maxDuration - this.minDuration + 1);
        }
    }

    public static enum GuidePreviewAnimation {
        NONE,
        IDLE,
        LOOK_1,
        LOOK_2,
        LOOK_3,
        SCRATCH,
        LOOK_5,
        WALK,
        RUN,
        FLY_FLAP,
        GLIDE;

        private RawAnimation animation() {
            return switch (this) {
                case NONE -> null;
                case IDLE -> IDLE_ANIMATION;
                case LOOK_1 -> IDLE_DIFF_1_ANIMATION;
                case LOOK_2 -> IDLE_DIFF_2_ANIMATION;
                case LOOK_3 -> IDLE_DIFF_3_ANIMATION;
                case SCRATCH -> IDLE_DIFF_4_ANIMATION;
                case LOOK_5 -> IDLE_DIFF_5_ANIMATION;
                case WALK -> WALK_ANIMATION;
                case RUN -> RUN_ANIMATION;
                case FLY_FLAP -> FLY_FLAPPING_WING_LOOP_ANIMATION;
                case GLIDE -> FLY_LOOP_ANIMATION;
            };
        }
    }
}
