package EdDYON.guaniao.content.bird.columbid;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.command.BirdCommandInteraction;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.content.bird.command.BirdRoostGoal;
import EdDYON.guaniao.content.bird.command.BirdStayGoal;
import EdDYON.guaniao.content.bird.command.CommandableBird;
import EdDYON.guaniao.content.bird.flock.FlockCompatibleBird;
import EdDYON.guaniao.content.bird.flock.BirdFlockManager;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.brain.BirdBrain;
import EdDYON.guaniao.content.bird.brain.BirdIntent;
import EdDYON.guaniao.content.bird.brain.BirdSpeciesProfile;
import EdDYON.guaniao.content.bird.flight.BirdFlightAware;
import EdDYON.guaniao.content.bird.flight.BirdFlightBoids;
import EdDYON.guaniao.content.bird.flight.BirdFlightController;
import EdDYON.guaniao.content.bird.flight.BirdFlightProfile;
import EdDYON.guaniao.content.bird.flight.BirdFlightTargeting;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import EdDYON.guaniao.content.bath.BirdBathAttraction;
import EdDYON.guaniao.content.bath.BirdBathContentType;
import EdDYON.guaniao.content.bath.BirdBathFeedingAnimatable;
import EdDYON.guaniao.content.bath.BirdBathMountable;
import EdDYON.guaniao.content.bath.BirdBathUseGoal;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.event.BirdColonySpawnRules;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
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

public abstract class AbstractColumbidEntity extends TamableAnimal implements GeoEntity, FlyingAnimal, ScalableBirdModel, BirdFlightAware, BirdBathMountable, BirdBathFeedingAnimatable, CommandableBird, FlockCompatibleBird {
    private static final EntityDataAccessor<Integer> BEHAVIOR_STATE = SynchedEntityData.defineId(AbstractColumbidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MODEL_SCALE = SynchedEntityData.defineId(AbstractColumbidEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COMMAND_MODE = SynchedEntityData.defineId(AbstractColumbidEntity.class, EntityDataSerializers.INT);
    private static final double FLIGHT_SPEED = 0.34D;
    private static final double HIGH_FLIGHT_SPEED = 0.38D;
    private static final double ESCAPE_FLIGHT_SPEED = 0.44D;
    private static final int AUTONOMOUS_FLIGHT_MIN_TICKS = 520;
    private static final int AUTONOMOUS_FLIGHT_RANDOM_TICKS = 300;
    private static final BirdFlightProfile FLIGHT_PROFILE = BirdFlightProfile.COLUMBID;
    protected static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation IDLE_DIFF_1_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_1").thenLoop("idle");
    protected static final RawAnimation IDLE_DIFF_2_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_2").thenLoop("idle");
    protected static final RawAnimation IDLE_DIFF_3_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_3").thenLoop("idle");
    protected static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation FLY_LOOP_ANIMATION = RawAnimation.begin().thenLoop("fly_loop");
    protected static final RawAnimation FLY_FLAP_ONCE_ANIMATION = RawAnimation.begin().thenPlay("fly_flapping_wing").thenLoop("fly_loop");
    protected static final RawAnimation FLY_FLAPPING_LOOP_ANIMATION = RawAnimation.begin().thenLoop("fly_flapping_wing_loop");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final BirdBrain birdBrain;
    private ColumbidBehaviorState behaviorState = ColumbidBehaviorState.IDLE;
    private GuidePreviewAnimation guidePreviewAnimation = GuidePreviewAnimation.NONE;
    private IdleAnimationChoice currentIdleAnimation = IdleAnimationChoice.BASE;
    private long nextIdleAnimationSwapTick;
    protected int behaviorStateLockTicks;
    protected int eatingTicks;
    protected int seedTrustTicks;
    protected int foodCooldown;
    protected int flightCooldown;
    protected int flightTicks;
    protected int flightDuration;
    protected int timeFlying;
    protected int flightLandingTicks;
    protected int landingSettleTicks;
    protected int flapOnceTicks;
    protected int pairScanCooldown;
    protected int pairLostTicks;
    protected int courtshipCooldown;
    protected int chaseCooldown;
    protected int restInterruptionTicks;
    protected boolean escapeFlight;
    protected Vec3 flightTarget;
    protected double flightSpeed = FLIGHT_SPEED;
    protected double flightCruiseY;
    protected boolean highCruiseFlight;
    protected boolean autonomousCruiseFlight;
    protected Vec3 flightWaypoint;
    protected int flightWaypointTicks;
    protected BlockPos homePos;
    protected UUID pairPartnerUUID;

    protected AbstractColumbidEntity(EntityType<? extends AbstractColumbidEntity> entityType, Level level, BirdSpeciesProfile profile) {
        super(entityType, level);
        this.birdBrain = new BirdBrain(this, profile);
        this.setPathfindingMalus(BlockPathTypes.LEAVES, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 16.0F);
    }

    public static AttributeSupplier.Builder createColumbidAttributes(double maxHealth, double walkSpeed, double flyingSpeed, double followRange) {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, maxHealth)
                .add(Attributes.MOVEMENT_SPEED, walkSpeed)
                .add(Attributes.FLYING_SPEED, flyingSpeed)
                .add(Attributes.FOLLOW_RANGE, followRange);
    }

    public static boolean isSeedFood(ItemStack stack) {
        return BirdFoodSafety.matchesClean(BirdTags.PIGEON_FOODS, stack);
    }

    public static boolean isPreferredTamingSeed(ItemStack stack) {
        return BirdFoodSafety.isCleanFoodCandidate(stack) && (stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS));
    }

    protected static boolean canColumbidSpawn(ServerLevelAccessor level, BlockPos pos, RandomSource random, boolean urbanBias) {
        BlockState below = level.getBlockState(pos.below());
        boolean validGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(Blocks.GRASS_BLOCK)
                || below.is(BlockTags.DIRT)
                || below.is(Blocks.FARMLAND)
                || below.is(Blocks.SAND)
                || below.is(Blocks.RED_SAND);
        if (!validGround || level.getRawBrightness(pos, 0) <= 8) {
            return false;
        }
        int score = level instanceof ServerLevel serverLevel
                ? BirdColonySpawnRules.columbidHabitatScore(serverLevel, pos, urbanBias)
                : habitatScore(level, pos, urbanBias);
        if (score >= (urbanBias ? 12 : 16)) {
            return true;
        }
        if (score >= (urbanBias ? 7 : 9)) {
            return random.nextFloat() < 0.76F;
        }
        return random.nextFloat() < (urbanBias ? 0.34F : 0.28F);
    }

    private static int habitatScore(LevelReader level, BlockPos origin, boolean urbanBias) {
        int score = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-7, -2, -7), origin.offset(7, 5, 7))) {
            if (!canReadChunk(level, pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FARMLAND) || state.is(Blocks.WHEAT) || state.getBlock() instanceof CropBlock) {
                score += urbanBias ? 2 : 4;
            } else if (state.is(Blocks.SUNFLOWER) || state.is(Blocks.HAY_BLOCK) || state.getBlock() instanceof ComposterBlock) {
                score += 3;
            } else if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                score += urbanBias ? 1 : 3;
            } else if (state.getBlock() instanceof FenceBlock || state.getBlock() instanceof FenceGateBlock) {
                score += 2;
            } else if (urbanBias && (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof BedBlock)) {
                score += 4;
            }
            if (score >= 24) {
                return score;
            }
        }
        return score;
    }

    private static boolean canReadChunk(LevelReader level, BlockPos pos) {
        return level.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal((Mob)this));
        this.goalSelector.addGoal(1, new ColumbidEatSeedGoal(this));
        this.goalSelector.addGoal(2, new BirdStayGoal<>(this));
        this.goalSelector.addGoal(2, new BirdRoostGoal<>(this));
        this.goalSelector.addGoal(2, new ColumbidFollowOwnerGoal(this, 1.0D, 3.0F, 10.0F));
        this.goalSelector.addGoal(3, new ColumbidRoostGoal(this));
        this.goalSelector.addGoal(4, new ColumbidChaseSmallBirdGoal(this));
        this.goalSelector.addGoal(5, new ColumbidPairBondGoal(this));
        this.goalSelector.addGoal(6, new ColumbidFlockOrPairGoal(this));
        this.goalSelector.addGoal(7, new ColumbidCourtshipGoal(this));
        this.goalSelector.addGoal(8, new ColumbidAmbientFlightGoal(this));
        this.goalSelector.addGoal(9, new BirdBathUseGoal(this, 0.92D, 11.0D, 38,
                BirdBathAttraction::isAttractiveToColumbid,
                this::canStartSeedGoal,
                bath -> this.setBehaviorState(ColumbidBehaviorState.FORAGING),
                this::consumeBirdBathServing,
                (bath, consumed) -> {
                    if (this.getBehaviorState() == ColumbidBehaviorState.FORAGING) {
                        this.setBehaviorState(ColumbidBehaviorState.IDLE);
                    }
                }));
        this.goalSelector.addGoal(10, new ColumbidGroundForagingGoal(this));
        this.goalSelector.addGoal(11, new ColumbidIdleGoal(this));
        this.goalSelector.addGoal(12, new LookAtPlayerGoal((Mob)this, Player.class, 6.0F));
        this.goalSelector.addGoal(13, new RandomLookAroundGoal((Mob)this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation navigation = new GroundPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BEHAVIOR_STATE, ColumbidBehaviorState.IDLE.ordinal());
        this.entityData.define(MODEL_SCALE, BirdModelScale.DEFAULT_INDIVIDUAL_SCALE);
        this.entityData.define(COMMAND_MODE, BirdCommandMode.FREE.ordinal());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (BEHAVIOR_STATE.equals(key)) {
            this.behaviorState = decodeBehaviorState(this.entityData.get(BEHAVIOR_STATE));
        }
        super.onSyncedDataUpdated(key);
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
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        this.birdBrain.tick();
        this.tickCounters();
        this.tickFlight();
        this.tickWaterEscape();
        this.tickBehaviorFallback();
        this.tickGroundMovementFacing();
    }

    private void tickCounters() {
        if (this.behaviorStateLockTicks > 0) {
            --this.behaviorStateLockTicks;
        }
        if (this.eatingTicks > 0) {
            --this.eatingTicks;
            if (this.eatingTicks <= 0 && this.getBehaviorState() == ColumbidBehaviorState.EATING) {
                this.setBehaviorState(ColumbidBehaviorState.FORAGING);
            }
        }
        if (this.seedTrustTicks > 0) {
            --this.seedTrustTicks;
        }
        if (this.foodCooldown > 0) {
            --this.foodCooldown;
        }
        if (this.flightCooldown > 0) {
            --this.flightCooldown;
        }
        if (this.landingSettleTicks > 0) {
            --this.landingSettleTicks;
        }
        if (this.flapOnceTicks > 0) {
            --this.flapOnceTicks;
        }
        if (this.pairScanCooldown > 0) {
            --this.pairScanCooldown;
        }
        if (this.courtshipCooldown > 0) {
            --this.courtshipCooldown;
        }
        if (this.chaseCooldown > 0) {
            --this.chaseCooldown;
        }
        if (this.restInterruptionTicks > 0) {
            --this.restInterruptionTicks;
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult commandResult = BirdCommandInteraction.tryHandle(this, this, player, hand);
        if (commandResult.consumesAction()) {
            return commandResult;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!this.isSeedFoodForThisBird(stack)) {
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        ItemStack offeredStack = stack.copy();
        float chance = this.tamingChance(offeredStack);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.birdBrain.onEat(0.18F);
        this.triggerEatingAnimation(28);
        this.playInteractionSound();
        if (this.isTame()) {
            this.seedTrustTicks = Math.max(this.seedTrustTicks, 700);
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(2.0F);
            }
            this.spawnTrustParticles(true);
            return InteractionResult.SUCCESS;
        }
        if (this.getRandom().nextFloat() < chance) {
            this.tame(player);
            this.setBirdCommandMode(BirdCommandMode.FOLLOW);
            this.setPersistenceRequired();
            this.homePos = this.blockPosition();
            this.spawnTrustParticles(true);
        } else {
            this.spawnTrustParticles(false);
        }
        this.seedTrustTicks = Math.max(this.seedTrustTicks, 700);
        return InteractionResult.SUCCESS;
    }

    protected float tamingChance(ItemStack stack) {
        float chance = isPreferredTamingSeed(stack) ? 0.22F : 0.12F;
        if (this.seedTrustTicks > 0) {
            chance += 0.22F;
        }
        return this.supportsPairBond() ? chance : chance * 0.65F;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        boolean hurt = super.hurt(damageSource, amount);
        if (hurt && !this.level().isClientSide) {
            this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 200);
            Entity source = damageSource.getEntity();
            if (BirdConfigManager.aprilFoolsMode() && source instanceof Player) {
                return true;
            }
            this.birdBrain.onFrightened(0.45F);
            if (source instanceof Player player && this.isOwnedBy(player)) {
                this.getNavigation().stop();
                this.setBehaviorStateFor(ColumbidBehaviorState.ALERT, 45);
            } else {
                this.startEscapeFlight(source == null ? this.position() : source.position());
            }
        }
        return hurt;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return this.isSeedFoodForThisBird(stack);
    }

    public boolean isSeedFoodForThisBird(ItemStack stack) {
        return BirdFoodSafety.matchesClean(this.foodTag(), stack);
    }

    protected TagKey<Item> foodTag() {
        return BirdTags.PIGEON_FOODS;
    }

    @Override
    public BirdCommandMode getBirdCommandMode() {
        return BirdCommandMode.byId(this.entityData.get(COMMAND_MODE));
    }

    @Override
    public void setBirdCommandMode(BirdCommandMode mode) {
        this.entityData.set(COMMAND_MODE, (mode == null ? BirdCommandMode.FREE : mode).ordinal());
    }

    @Override
    public boolean isBirdEmergencyOverrideActive() {
        return this.escapeFlight || this.getBehaviorState() == ColumbidBehaviorState.FLEEING;
    }

    @Override
    public boolean isOrderedToSit() {
        return false;
    }

    @Override
    public boolean isFlying() {
        return this.shouldPlayFlyAnimation();
    }

    @Override
    public boolean isNoGravity() {
        return this.isControlledFlightActive() || super.isNoGravity();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        this.fallDistance = 0.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        this.birdBrain.save(compoundTag);
        BirdModelScale.save(compoundTag, this.getIndividualModelScale(), this.modelScaleProfile());
        if (this.homePos != null) {
            compoundTag.putInt("HomeX", this.homePos.getX());
            compoundTag.putInt("HomeY", this.homePos.getY());
            compoundTag.putInt("HomeZ", this.homePos.getZ());
        }
        if (this.pairPartnerUUID != null) {
            compoundTag.putUUID("PairPartner", this.pairPartnerUUID);
        }
        compoundTag.putInt("SeedTrustTicks", this.seedTrustTicks);
        compoundTag.putInt(CommandableBird.COMMAND_MODE_NBT_KEY, this.getBirdCommandMode().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.birdBrain.load(compoundTag);
        if (compoundTag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.setIndividualModelScale(BirdModelScale.load(compoundTag, this.modelScaleProfile()));
        } else {
            this.randomizeModelScale();
        }
        if (compoundTag.contains("HomeX", 99) && compoundTag.contains("HomeY", 99) && compoundTag.contains("HomeZ", 99)) {
            this.homePos = new BlockPos(compoundTag.getInt("HomeX"), compoundTag.getInt("HomeY"), compoundTag.getInt("HomeZ"));
        }
        this.pairPartnerUUID = compoundTag.hasUUID("PairPartner") ? compoundTag.getUUID("PairPartner") : null;
        this.seedTrustTicks = compoundTag.getInt("SeedTrustTicks");
        if (compoundTag.contains(CommandableBird.COMMAND_MODE_NBT_KEY, 3)) {
            this.setBirdCommandMode(BirdCommandMode.byId(compoundTag.getInt(CommandableBird.COMMAND_MODE_NBT_KEY)));
        } else {
            this.setBirdCommandMode(this.isTame() ? BirdCommandMode.FOLLOW : BirdCommandMode.FREE);
        }
        this.clearFlightState();
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.COLUMBID;
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
        return FLIGHT_PROFILE;
    }

    @Override
    public boolean isBirdFlightActive() {
        ColumbidBehaviorState state = this.getBehaviorState();
        return this.isControlledFlightActive()
                || state.isAirborne() && (!this.onGround() || super.isNoGravity());
    }

    @Override
    public boolean isBirdLanding() {
        return this.flightLandingTicks > 0;
    }

    @Override
    public boolean isBirdEscaping() {
        return this.escapeFlight;
    }

    public ColumbidBehaviorState getBehaviorState() {
        if (this.entityData != null) {
            return decodeBehaviorState(this.entityData.get(BEHAVIOR_STATE));
        }
        return this.behaviorState;
    }

    void setBehaviorState(ColumbidBehaviorState state) {
        if (state == null) {
            state = ColumbidBehaviorState.IDLE;
        }
        this.behaviorState = state;
        if (this.entityData != null) {
            this.entityData.set(BEHAVIOR_STATE, state.ordinal());
        }
    }

    void setBehaviorStateFor(ColumbidBehaviorState state, int ticks) {
        this.setBehaviorState(state);
        this.behaviorStateLockTicks = Math.max(this.behaviorStateLockTicks, ticks);
    }

    public void setGuidePreviewAnimation(GuidePreviewAnimation guidePreviewAnimation) {
        this.guidePreviewAnimation = guidePreviewAnimation == null ? GuidePreviewAnimation.NONE : guidePreviewAnimation;
    }

    public ColumbidVariant getColumbidVariant() {
        return ColumbidVariant.SPOTTED_DOVE;
    }

    public net.minecraft.resources.ResourceLocation getTextureResource() {
        return this.getColumbidVariant().texture();
    }

    public boolean sensesIncomingBadWeather() {
        return this.usesWeatherSense() && (this.level().isRaining() || this.level().isThundering());
    }

    protected boolean usesWeatherSense() {
        return false;
    }

    protected boolean supportsPairBond() {
        return false;
    }

    protected boolean supportsChasing() {
        return false;
    }

    protected boolean prefersHumanSettlements() {
        return false;
    }

    protected int ambientFlightChance() {
        int chance = this.prefersHumanSettlements() ? 70 : 92;
        if (this.isTame()) {
            chance += 180;
        }
        if (this.sensesIncomingBadWeather()) {
            chance += 260;
        }
        if (this.birdBrain.wantsForage()) {
            chance += 120;
        }
        return chance;
    }

    protected boolean isActiveTime() {
        return BirdActivitySchedule.DIURNAL.isActiveTime(this.level().getDayTime());
    }

    public boolean startFlybyFlight(Vec3 landingTarget, int ticks) {
        if (landingTarget == null) {
            return false;
        }
        int duration = Math.max(ticks, AUTONOMOUS_FLIGHT_MIN_TICKS + this.getRandom().nextInt(AUTONOMOUS_FLIGHT_RANDOM_TICKS));
        return this.startAutonomousCruiseFlight(landingTarget, duration);
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
            horizontal = horizontal.normalize().scale(0.28D);
        }
        Vec3 movement = new Vec3(horizontal.x, 0.66D, horizontal.z);
        this.getNavigation().stop();
        this.setNoGravity(false);
        this.setOnGround(false);
        this.flapOnceTicks = Math.max(this.flapOnceTicks, 24);
        this.setBehaviorStateFor(ColumbidBehaviorState.FLAP_FLYING, 34);
        this.setDeltaMovement(movement);
        this.faceFlightDirection(movement);
        this.fallDistance = 0.0F;
        this.hasImpulse = true;
        return true;
    }

    @Override
    public void startBirdBathFeedingAnimation(BirdBathContentType contentType, int ticks) {
        this.getNavigation().stop();
        if (contentType.isFood()) {
            this.triggerEatingAnimation(Math.max(32, ticks));
            return;
        }
        this.triggerPeckAnimation(Math.max(24, ticks / 2));
    }

    private boolean startAutonomousCruiseFlight(Vec3 landingTarget, int duration) {
        return this.startControlledFlight(landingTarget, duration, HIGH_FLIGHT_SPEED, false, true, true);
    }

    protected boolean isRoostTime() {
        return BirdActivitySchedule.DIURNAL.isRestTime(this.level().getDayTime());
    }

    protected boolean isRestInterrupted() {
        return this.restInterruptionTicks > 0;
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    protected SoundEvent getInteractionSound() {
        return this.getAmbientSound();
    }

    protected void playInteractionSound() {
        this.playSound(this.getInteractionSound(), 0.38F, 0.92F + this.getRandom().nextFloat() * 0.14F);
    }

    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PARROT_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public int getAmbientSoundInterval() {
        if (this.sensesIncomingBadWeather()) {
            return BirdFlockSoundLimiter.scaledAmbientInterval(this, 360);
        }
        return BirdFlockSoundLimiter.scaledAmbientInterval(this, this.level().isDay() ? 190 : 320);
    }

    @Override
    public void playAmbientSound() {
        if (BirdFlockSoundLimiter.allowAmbient(this)) {
            super.playAmbientSound();
        }
    }

    @Override
    public float getSoundVolume() {
        return 0.32F;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, BirdSoundVolume.apply(this, volume), pitch);
    }

    @Override
    public float getVoicePitch() {
        return 0.78F + this.getRandom().nextFloat() * 0.12F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.09F, 0.86F);
    }

    protected abstract AbstractColumbidEntity createChildEntity(ServerLevel level);

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        AbstractColumbidEntity child = this.createChildEntity(level);
        if (child != null) {
            float mateScale = mate instanceof AbstractColumbidEntity other ? other.getIndividualModelScale() : this.getIndividualModelScale();
            child.setIndividualModelScale(BirdModelScale.inheritIndividualScale(child.getRandom(), this.getIndividualModelScale(), mateScale, child.modelScaleProfile()));
        }
        return child;
    }

    private static ColumbidBehaviorState decodeBehaviorState(int ordinal) {
        ColumbidBehaviorState[] values = ColumbidBehaviorState.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return ColumbidBehaviorState.IDLE;
        }
        return values[ordinal];
    }

    private void randomizeModelScale() {
        this.setIndividualModelScale(BirdModelScale.randomIndividualScale(this.getRandom(), this.modelScaleProfile()));
    }

    private boolean isOwnedBy(Player player) {
        return this.isTame() && this.getOwnerUUID() != null && this.getOwnerUUID().equals(player.getUUID());
    }

    private void spawnTrustParticles(boolean success) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(success ? ParticleTypes.HEART : ParticleTypes.SMOKE, this.getX(), this.getY() + 0.6D, this.getZ(), 6, 0.25D, 0.25D, 0.25D, 0.02D);
    }

    private void spawnCourtshipParticles(int count) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.7D, this.getZ(), count, 0.25D, 0.25D, 0.25D, 0.01D);
        }
    }

    private void triggerEatingAnimation(int ticks) {
        this.eatingTicks = Math.max(this.eatingTicks, ticks);
        this.currentIdleAnimation = this.getRandom().nextBoolean() ? IdleAnimationChoice.PECK_1 : IdleAnimationChoice.PECK_2;
        this.nextIdleAnimationSwapTick = this.level().getGameTime() + ticks;
        this.setBehaviorStateFor(ColumbidBehaviorState.EATING, ticks);
        this.foodCooldown = Math.max(this.foodCooldown, 45 + this.getRandom().nextInt(45));
    }

    private void consumeBirdBathServing(EdDYON.guaniao.content.bath.BirdBathBlockEntity bath, BirdBathContentType contentType) {
        if (contentType == BirdBathContentType.BREAD) {
            this.birdBrain().onEat(0.24F);
            this.triggerEatingAnimation(30 + this.getRandom().nextInt(14));
            return;
        }
        this.birdBrain().onEat(0.12F);
        this.eatingTicks = Math.max(this.eatingTicks, 22 + this.getRandom().nextInt(12));
        this.currentIdleAnimation = IdleAnimationChoice.PECK_1;
        this.nextIdleAnimationSwapTick = this.level().getGameTime() + this.eatingTicks;
        this.setBehaviorStateFor(ColumbidBehaviorState.EATING, this.eatingTicks);
        this.foodCooldown = Math.max(this.foodCooldown, 55 + this.getRandom().nextInt(45));
        this.playSound(SoundEvents.GENERIC_DRINK, 0.28F, 0.9F + this.getRandom().nextFloat() * 0.12F);
    }

    private void triggerPeckAnimation(int ticks) {
        this.currentIdleAnimation = this.getRandom().nextBoolean() ? IdleAnimationChoice.PECK_1 : IdleAnimationChoice.PECK_2;
        this.nextIdleAnimationSwapTick = this.level().getGameTime() + ticks;
        this.setBehaviorStateFor(ColumbidBehaviorState.FORAGING, ticks);
    }

    private boolean canStartSeedGoal() {
        return this.foodCooldown <= 0
                && (!this.isTame() || this.isBirdCommandMode(BirdCommandMode.FREE))
                && !this.isControlledFlightActive()
                && !this.isRoostTime()
                && !this.isInWaterOrBubble()
                && this.getTarget() == null
                && !this.getBehaviorState().isEscape();
    }

    private boolean canStartGroundSocialGoal() {
        return this.isActiveTime()
                && (!this.isTame() || this.isBirdCommandMode(BirdCommandMode.FREE))
                && this.onGround()
                && !this.isControlledFlightActive()
                && this.landingSettleTicks <= 0
                && !this.isInWaterOrBubble()
                && !this.getBehaviorState().isEscape()
                && this.getTarget() == null;
    }

    private void tickBehaviorFallback() {
        if (this.isControlledFlightActive()) {
            return;
        }
        if (this.behaviorStateLockTicks > 0) {
            return;
        }
        if (!this.onGround() && !this.isInWaterOrBubble()) {
            this.setBehaviorState(ColumbidBehaviorState.FLAP_FLYING);
            return;
        }
        ColumbidBehaviorState state = this.getBehaviorState();
        if (state == ColumbidBehaviorState.ROOSTING || state == ColumbidBehaviorState.SLEEPING) {
            if (!this.isRoostTime()) {
                this.setBehaviorState(ColumbidBehaviorState.IDLE);
            }
            return;
        }
        if (this.isRoostTime() && !this.isRestInterrupted() && this.getNavigation().isDone()) {
            this.setBehaviorState(ColumbidBehaviorState.SLEEPING);
            if (this.tickCount % 80 == 0) {
                this.birdBrain.onRest(0.02F);
            }
            return;
        }
        BirdIntent intent = this.birdBrain.currentIntent();
        if (this.getNavigation().isDone() && (intent == BirdIntent.ALERT || intent == BirdIntent.WATCH)) {
            this.setBehaviorState(ColumbidBehaviorState.ALERT);
            return;
        }
        if (!this.getNavigation().isDone()) {
            this.setBehaviorState(this.birdBrain.wantsForage() ? ColumbidBehaviorState.FORAGING : ColumbidBehaviorState.WALKING);
            return;
        }
        this.setBehaviorState(this.birdBrain.wantsForage() ? ColumbidBehaviorState.FORAGING : ColumbidBehaviorState.IDLE);
    }

    private void startEscapeFlight(Vec3 threatPosition) {
        if (this.isControlledFlightActive() || this.flightCooldown > 0) {
            return;
        }
        Vec3 away = this.position().subtract(threatPosition).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() <= 1.0E-4D) {
            away = this.randomHorizontalDirection();
        }
        Vec3 target = this.findFlightLandingTarget(away.normalize(), 18, 34, true);
        if (target == null) {
            target = this.position().add(away.normalize().scale(22.0D)).add(0.0D, 4.0D, 0.0D);
        }
        this.startControlledFlight(target, 105 + this.getRandom().nextInt(65), ESCAPE_FLIGHT_SPEED, true, true);
    }

    private boolean startControlledFlight(Vec3 target, int duration, double speed, boolean escape) {
        return this.startControlledFlight(target, duration, speed, escape, escape);
    }

    private boolean startControlledFlight(Vec3 target, int duration, double speed, boolean escape, boolean highCruise) {
        return this.startControlledFlight(target, duration, speed, escape, highCruise, false);
    }

    private boolean startControlledFlight(Vec3 target, int duration, double speed, boolean escape, boolean highCruise, boolean autonomousCruise) {
        if (target == null) {
            return false;
        }
        this.flightTarget = target;
        this.flightTicks = duration;
        this.flightDuration = duration;
        this.timeFlying = 0;
        this.flightLandingTicks = 0;
        this.flightSpeed = speed;
        this.escapeFlight = escape;
        this.highCruiseFlight = highCruise;
        this.autonomousCruiseFlight = autonomousCruise;
        double horizontalDistance = target.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).length();
        double clearance = highCruise
                ? Mth.clamp(FLIGHT_PROFILE.minCruiseHeight() + horizontalDistance * 0.18D, FLIGHT_PROFILE.minCruiseHeight(), escape ? 22.0D : FLIGHT_PROFILE.maxCruiseHeight())
                : Mth.clamp(5.0D + horizontalDistance * 0.10D, 6.0D, 10.0D);
        double minimumCruiseY = this.getY() + (highCruise ? (this.onGround() ? 7.0D : 2.0D) : 3.5D);
        this.flightCruiseY = Math.max(target.y + clearance, minimumCruiseY);
        Vec3 initialDirection = target.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (initialDirection.lengthSqr() <= 1.0E-4D) {
            initialDirection = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (initialDirection.lengthSqr() <= 1.0E-4D) {
            initialDirection = this.randomHorizontalDirection();
        }
        this.flightWaypoint = autonomousCruise ? this.chooseAutonomousFlightWaypoint(initialDirection.normalize()) : null;
        this.flightWaypointTicks = autonomousCruise ? 48 + this.getRandom().nextInt(76) : 0;
        this.flapOnceTicks = 0;
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.setOnGround(false);
        this.setBehaviorStateFor(escape ? ColumbidBehaviorState.FLEEING : ColumbidBehaviorState.FLAP_FLYING, escape ? 120 : (autonomousCruise ? 140 : (highCruise ? 105 : 70)));
        this.flightCooldown = Math.max(this.flightCooldown, escape ? 160 : (autonomousCruise ? 520 : (highCruise ? 260 : 180)));
        Vec3 movement = initialDirection.normalize().scale(speed * (highCruise ? 0.9D : 0.7D)).add(0.0D, escape ? 0.64D : (highCruise ? 0.52D : 0.28D), 0.0D);
        this.setDeltaMovement(movement);
        this.faceFlightDirection(movement);
        this.fallDistance = 0.0F;
        this.hasImpulse = true;
        return true;
    }

    private void tickFlight() {
        if (!this.isControlledFlightActive()) {
            this.timeFlying = 0;
            BirdFlightController.clearFlightProgress(this);
            if (this.isNoGravity()) {
                this.setNoGravity(false);
            }
            return;
        }
        this.getNavigation().stop();
        this.setNoGravity(true);
        ++this.timeFlying;
        --this.flightTicks;
        Vec3 toLanding = this.flightTarget.subtract(this.position());
        double landingDistance = toLanding.length();
        double horizontalDistance = Math.sqrt(toLanding.x * toLanding.x + toLanding.z * toLanding.z);
        int flightAge = this.flightDuration - this.flightTicks;
        if (this.onGround() && flightAge > 8) {
            this.finishControlledFlight(true);
            return;
        }
        if ((this.onGround() && flightAge > 14 && horizontalDistance < 1.6D) || (landingDistance < 0.95D && this.onGround())) {
            this.finishControlledFlight(true);
            return;
        }
        if (this.onGround() && flightAge > 18 && this.getDeltaMovement().y <= 0.03D) {
            this.finishControlledFlight(false);
            return;
        }
        int landingWindow = this.autonomousCruiseFlight
                ? Mth.clamp(this.flightDuration / 7, 64, 100)
                : (this.highCruiseFlight ? Math.max(44, this.flightDuration / 3) : Math.max(24, this.flightDuration / 4));
        boolean landingPhase = this.flightTicks <= landingWindow
                || (!this.autonomousCruiseFlight && horizontalDistance < (this.highCruiseFlight ? 7.0D : 3.2D));
        if (this.autonomousCruiseFlight && !landingPhase) {
            this.updateAutonomousFlightWaypoint();
        }
        if (!landingPhase && !this.autonomousCruiseFlight && this.flightWaypoint != null
                && (--this.flightWaypointTicks <= 0 || this.flightWaypoint.distanceToSqr(this.position()) < 4.0D)) {
            this.flightWaypoint = null;
        }
        Vec3 steeringTarget = landingPhase
                ? this.flightTarget
                : (this.flightWaypoint != null ? this.flightWaypoint : new Vec3(this.flightTarget.x, this.flightCruiseY, this.flightTarget.z));
        if (!landingPhase && BirdFlightController.isFlightProgressStalled(this, steeringTarget, 12, 12)) {
            Vec3 recovery = BirdFlightTargeting.findRecoveryTarget(this, steeringTarget.subtract(this.position()), 8, 12);
            if (recovery != null) {
                this.flightWaypoint = recovery;
                this.flightWaypointTicks = 36;
                steeringTarget = recovery;
            }
        }
        Vec3 toSteeringTarget = steeringTarget.subtract(this.position());
        Vec3 horizontal = new Vec3(toSteeringTarget.x, 0.0D, toSteeringTarget.z);
        double heightAboveLanding = this.getY() - this.flightTarget.y;
        boolean closeLandingApproach = landingPhase && horizontalDistance < 1.25D;
        if (horizontal.lengthSqr() <= 1.0E-4D) {
            horizontal = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        }
        if (landingPhase && horizontalDistance < 2.6D && heightAboveLanding > 4.0D) {
            Vec3 drift = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
            if (drift.lengthSqr() > 1.0E-4D) {
                horizontal = drift;
            }
        }
        if (closeLandingApproach && horizontalDistance < 0.55D) {
            horizontal = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        }
        if (horizontal.lengthSqr() <= 1.0E-4D) {
            horizontal = closeLandingApproach ? Vec3.ZERO : this.randomHorizontalDirection();
        }
        boolean hasHorizontalSteering = horizontal.lengthSqr() > 1.0E-4D;
        if (hasHorizontalSteering) {
            horizontal = horizontal.normalize();
        }
        if (!landingPhase) {
            Vec3 flockHeading = BirdFlightBoids.sameTypeHeading(this, 30.0D, 4.6D, 0.030D, 0.52D, 0.11D, this.escapeFlight ? 0.20D : 0.09D);
            if (flockHeading.lengthSqr() > 1.0E-4D) {
                horizontal = BirdFlightTargeting.normalizeHorizontal(horizontal.add(flockHeading), horizontal);
            }
        }
        double speed = landingPhase
                ? BirdFlightController.decelerateNearLanding(this.flightSpeed, horizontalDistance, this.highCruiseFlight ? 7.0D : 4.0D, 0.22D)
                : this.flightSpeed;
        if (closeLandingApproach) {
            speed = Math.min(speed, 0.035D + horizontalDistance * 0.06D);
        }
        if (this.highCruiseFlight && !landingPhase) {
            speed = this.autonomousCruiseFlight ? Math.min(speed, HIGH_FLIGHT_SPEED) : Math.min(speed + 0.015D, ESCAPE_FLIGHT_SPEED);
        }
        double lift = Mth.clamp(toSteeringTarget.y * (landingPhase ? 0.10D : 0.12D), landingPhase ? -0.16D : (this.autonomousCruiseFlight ? -0.045D : -0.025D), this.highCruiseFlight ? 0.24D : 0.17D);
        if (!landingPhase && this.getY() < this.flightCruiseY - 1.0D) {
            lift = Math.max(lift, Mth.clamp((this.flightCruiseY - this.getY()) * 0.08D, 0.10D, this.escapeFlight ? 0.25D : 0.21D));
        }
        if (landingPhase && heightAboveLanding > 2.5D && horizontalDistance > 1.6D) {
            speed = Math.max(speed, this.flightSpeed * (heightAboveLanding > 5.0D ? 0.66D : 0.48D));
            lift = Math.min(lift, this.highCruiseFlight ? -0.066D : -0.048D);
        } else if (landingPhase && heightAboveLanding > 2.5D) {
            lift = Math.min(lift, this.highCruiseFlight ? -0.055D : -0.042D);
        }
        if (landingPhase && heightAboveLanding < 1.25D) {
            lift = Math.max(lift, -0.024D);
        }
        if (flightAge < 10) {
            lift += this.escapeFlight ? 0.22D : 0.14D;
        }
        if (this.flightTicks <= 0) {
            if (!this.onGround()) {
                ++this.flightLandingTicks;
                this.flightTicks = 1;
                if (this.flightLandingTicks == 1 || this.flightLandingTicks % 24 == 0 || this.flightTarget == null) {
                    Vec3 landing = this.findNearestDryLandingTarget(this.flightLandingTicks > 80 ? 24 : 14);
                    if (landing != null) {
                        this.flightTarget = landing;
                    }
                }
                lift = Math.min(lift, this.flightLandingTicks > 90 ? -0.065D : -0.045D);
                if (this.getY() < this.flightTarget.y + 1.35D) {
                    lift = Math.max(lift, -0.026D);
                }
            } else {
                this.finishControlledFlight(true);
                return;
            }
        }
        Vec3 desired = (hasHorizontalSteering ? horizontal.scale(speed) : Vec3.ZERO).add(0.0D, lift, 0.0D);
        Vec3 movement = this.getDeltaMovement().scale(0.42D).add(desired.scale(0.58D));
        if (!landingPhase && BirdFlightController.isStalledInAir(this, this.timeFlying, 0.008D)) {
            Vec3 nextWaypoint = this.chooseAutonomousFlightWaypoint(horizontal);
            if (nextWaypoint != null) {
                this.flightWaypoint = nextWaypoint;
                this.flightWaypointTicks = 44 + this.getRandom().nextInt(48);
                this.flightCruiseY = Mth.clamp(nextWaypoint.y, this.level().getMinBuildHeight() + 8.0D, this.level().getMaxBuildHeight() - 8.0D);
            }
            movement = horizontal.scale(Math.max(speed, this.flightSpeed * 0.72D)).add(0.0D, this.highCruiseFlight ? 0.16D : 0.10D, 0.0D);
        }
        if (!this.escapeFlight && flightAge > 22 && movement.y <= 0.04D && this.getRandom().nextInt(34) == 0) {
            this.flapOnceTicks = 18;
        }
        if (!this.escapeFlight && movement.y < 0.03D && movement.horizontalDistanceSqr() > 0.035D) {
            this.setBehaviorState(this.flapOnceTicks > 0 ? ColumbidBehaviorState.FLAP_FLYING : ColumbidBehaviorState.GLIDING);
        } else {
            this.setBehaviorState(this.escapeFlight ? ColumbidBehaviorState.FLEEING : ColumbidBehaviorState.FLAP_FLYING);
        }
        this.setDeltaMovement(movement);
        this.faceFlightDirection(movement);
        this.fallDistance = 0.0F;
        this.hasImpulse = true;
    }

    private boolean isControlledFlightActive() {
        return this.flightTarget != null && (this.flightTicks > 0 || !this.onGround());
    }

    private void finishControlledFlight(boolean landed) {
        BirdFlightController.clearFlightProgress(this);
        this.flightTarget = null;
        this.flightTicks = 0;
        this.flightDuration = 0;
        this.timeFlying = 0;
        this.flightLandingTicks = 0;
        this.escapeFlight = false;
        this.highCruiseFlight = false;
        this.autonomousCruiseFlight = false;
        this.flightWaypoint = null;
        this.flightWaypointTicks = 0;
        this.flightCruiseY = 0.0D;
        this.flapOnceTicks = 0;
        this.setNoGravity(false);
        this.getNavigation().stop();
        Vec3 movement = this.getDeltaMovement();
        if (landed) {
            this.landingSettleTicks = Math.max(this.landingSettleTicks, 18);
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(movement.x * 0.35D, Math.max(movement.y * 0.3D, -0.04D), movement.z * 0.35D);
        }
        if (this.getBehaviorState().isAirborne()) {
            this.setBehaviorStateFor(ColumbidBehaviorState.ALERT, 24);
        }
    }

    private void clearFlightState() {
        BirdFlightController.clearFlightProgress(this);
        this.flightTarget = null;
        this.flightTicks = 0;
        this.flightDuration = 0;
        this.timeFlying = 0;
        this.flightLandingTicks = 0;
        this.escapeFlight = false;
        this.highCruiseFlight = false;
        this.autonomousCruiseFlight = false;
        this.flightWaypoint = null;
        this.flightWaypointTicks = 0;
        this.flightCruiseY = 0.0D;
        this.flapOnceTicks = 0;
        this.landingSettleTicks = 0;
        this.setNoGravity(false);
    }

    private void tickWaterEscape() {
        if (!this.isInWaterOrBubble()) {
            return;
        }
        this.getNavigation().stop();
        Vec3 target = this.findNearestDryLandingTarget(14);
        if (target != null) {
            this.startControlledFlight(target, 85 + this.getRandom().nextInt(45), ESCAPE_FLIGHT_SPEED, true, true);
            return;
        }
        Vec3 movement = this.getDeltaMovement().multiply(0.4D, 0.0D, 0.4D).add(0.0D, 0.32D, 0.0D);
        this.setNoGravity(true);
        this.setBehaviorStateFor(ColumbidBehaviorState.FLAP_FLYING, 35);
        this.setDeltaMovement(movement);
        this.faceFlightDirection(movement);
        this.hasImpulse = true;
    }

    private void updateAutonomousFlightWaypoint() {
        Vec3 waypoint = this.flightWaypoint;
        boolean shouldPickNewWaypoint = waypoint == null || --this.flightWaypointTicks <= 0;
        if (!shouldPickNewWaypoint && waypoint != null) {
            Vec3 toWaypoint = waypoint.subtract(this.position());
            double horizontalDistance = Math.sqrt(toWaypoint.x * toWaypoint.x + toWaypoint.z * toWaypoint.z);
            shouldPickNewWaypoint = horizontalDistance < (this.autonomousCruiseFlight ? 8.0D : 5.0D) || Math.abs(toWaypoint.y) > FLIGHT_PROFILE.maxVerticalStep() + 3.0D;
        }
        if (!shouldPickNewWaypoint) {
            return;
        }
        Vec3 direction = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() <= 1.0E-4D) {
            direction = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (direction.lengthSqr() <= 1.0E-4D) {
            direction = this.randomHorizontalDirection();
        }
        Vec3 nextWaypoint = this.chooseAutonomousFlightWaypoint(direction.normalize());
        if (nextWaypoint != null) {
            this.flightWaypoint = nextWaypoint;
            this.flightWaypointTicks = 54 + this.getRandom().nextInt(86);
            this.flightCruiseY = Mth.clamp(nextWaypoint.y, this.level().getMinBuildHeight() + 8.0D, this.level().getMaxBuildHeight() - 8.0D);
        } else {
            this.flightWaypointTicks = 24;
        }
    }

    private Vec3 chooseAutonomousFlightWaypoint(Vec3 preferredDirection) {
        return BirdFlightTargeting.findAirTarget(this, FLIGHT_PROFILE, preferredDirection, this.escapeFlight);
    }

    private Vec3 findFlightLandingTarget(Vec3 direction, int minRadius, int maxRadius, boolean high) {
        return BirdFlightTargeting.findLandingInDirection(this, direction, minRadius, maxRadius, high ? 8 : 5, high ? 22 : 12);
    }

    private Vec3 findNearestDryLandingTarget(int radius) {
        return BirdFlightTargeting.findNearestDryLandingTarget(this, radius, 16);
    }

    private Vec3 findDryLandingTarget(BlockPos center, int verticalRange) {
        return BirdFlightTargeting.findDryLandingTarget(this, center, verticalRange);
    }

    private Vec3 findDryLandingTargetNear(BlockPos center, int horizontalRange, int verticalRange) {
        return BirdFlightTargeting.findDryLandingTargetNear(this, center, horizontalRange, verticalRange);
    }

    private Vec3 findGroundStrollTarget(int horizontalRange, int verticalRange) {
        for (int attempt = 0; attempt < 10; ++attempt) {
            Vec3 candidate = LandRandomPos.getPos(this, horizontalRange, verticalRange);
            if (candidate == null) {
                continue;
            }
            BlockPos pos = BlockPos.containing(candidate);
            if (this.isSafeDryLanding(pos)) {
                return Vec3.atBottomCenterOf(pos).add(0.0D, 0.05D, 0.0D);
            }
        }
        return null;
    }

    private boolean isSafeDryLanding(BlockPos pos) {
        return BirdFlightTargeting.isSafeDryLanding(this, pos);
    }

    private Optional<AbstractColumbidEntity> pairPartner() {
        if (this.pairPartnerUUID == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        Entity entity = serverLevel.getEntity(this.pairPartnerUUID);
        if (entity instanceof AbstractColumbidEntity columbid && columbid.isAlive() && columbid.getClass() == this.getClass()) {
            return Optional.of(columbid);
        }
        return Optional.empty();
    }

    private boolean isPairedWith(Entity entity) {
        return entity != null && this.pairPartnerUUID != null && this.pairPartnerUUID.equals(entity.getUUID());
    }

    private boolean hasReciprocalPairWith(AbstractColumbidEntity other) {
        return other != null
                && this.pairPartnerUUID != null
                && this.pairPartnerUUID.equals(other.getUUID())
                && other.pairPartnerUUID != null
                && other.pairPartnerUUID.equals(this.getUUID());
    }

    private void faceFlightDirection(Vec3 movement) {
        BirdFlightController.faceMovement(this, movement, FLIGHT_PROFILE.maxPitchDegrees());
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
        ColumbidBehaviorState state = this.getBehaviorState();
        if (state.isAirborne()
                || state == ColumbidBehaviorState.EATING
                || state == ColumbidBehaviorState.PREENING
                || state == ColumbidBehaviorState.COURTING
                || state == ColumbidBehaviorState.ROOSTING
                || state == ColumbidBehaviorState.SLEEPING) {
            return false;
        }
        return BirdGroundAnimation.hasWalkMotion(this);
    }

    private Vec3 randomHorizontalDirection() {
        return BirdFlightTargeting.randomHorizontalDirection(this.getRandom());
    }

    private double randomSigned(double value) {
        return (this.getRandom().nextDouble() * 2.0D - 1.0D) * value;
    }

    private static Vec3 rotateHorizontal(Vec3 direction, double angle) {
        return BirdFlightTargeting.rotateHorizontal(direction, angle);
    }

    private RawAnimation pickIdleAnimation() {
        ColumbidBehaviorState state = this.getBehaviorState();
        if (state == ColumbidBehaviorState.EATING || this.eatingTicks > 0) {
            if (this.currentIdleAnimation != IdleAnimationChoice.PECK_1 && this.currentIdleAnimation != IdleAnimationChoice.PECK_2) {
                this.currentIdleAnimation = this.getRandom().nextBoolean() ? IdleAnimationChoice.PECK_1 : IdleAnimationChoice.PECK_2;
            }
            return this.currentIdleAnimation.animation;
        }
        if (state == ColumbidBehaviorState.PREENING) {
            return IDLE_DIFF_1_ANIMATION;
        }
        if (state == ColumbidBehaviorState.CURIOUS || state == ColumbidBehaviorState.ALERT) {
            return IDLE_DIFF_3_ANIMATION;
        }
        if (state == ColumbidBehaviorState.COURTING) {
            return IDLE_DIFF_3_ANIMATION;
        }
        if (state == ColumbidBehaviorState.ROOSTING || state == ColumbidBehaviorState.SLEEPING) {
            return IDLE_ANIMATION;
        }
        if (this.level().getGameTime() >= this.nextIdleAnimationSwapTick) {
            this.currentIdleAnimation = this.chooseIdleAnimation();
            this.nextIdleAnimationSwapTick = this.level().getGameTime() + this.currentIdleAnimation.nextDuration(this.getRandom());
        }
        return this.currentIdleAnimation.animation;
    }

    private boolean shouldPlayFlyAnimation() {
        boolean airborneState = this.getBehaviorState().isAirborne()
                && (!this.onGround() || this.isControlledFlightActive() || super.isNoGravity());
        return BirdFlightController.shouldPlayFlyAnimation(
                this,
                airborneState,
                this.onGround(),
                this.isNoGravity(),
                this.getDeltaMovement(),
                0);
    }

    private boolean shouldPlayWalkAnimation() {
        ColumbidBehaviorState state = this.getBehaviorState();
        if (!BirdGroundAnimation.canPlayWalk(this)) {
            return false;
        }
        if (state.isAirborne()
                || state == ColumbidBehaviorState.EATING
                || state == ColumbidBehaviorState.PREENING
                || state == ColumbidBehaviorState.COURTING
                || state == ColumbidBehaviorState.ROOSTING
                || state == ColumbidBehaviorState.SLEEPING) {
            return false;
        }
        if (state == ColumbidBehaviorState.WALKING
                || state == ColumbidBehaviorState.FOLLOWING_OWNER
                || state == ColumbidBehaviorState.PAIR_FOLLOWING
                || state == ColumbidBehaviorState.CHASING) {
            return true;
        }
        return BirdGroundAnimation.hasWalkMotion(this);
    }

    private IdleAnimationChoice chooseIdleAnimation() {
        ColumbidBehaviorState state = this.getBehaviorState();
        int roll = this.getRandom().nextInt(100);
        if (state == ColumbidBehaviorState.FORAGING || state == ColumbidBehaviorState.EATING) {
            if (roll < 46) {
                return IdleAnimationChoice.PECK_1;
            }
            if (roll < 76) {
                return IdleAnimationChoice.PECK_2;
            }
            return IdleAnimationChoice.BASE;
        }
        if (state == ColumbidBehaviorState.COURTING) {
            return IdleAnimationChoice.DISPLAY;
        }
        if (roll < 58) {
            return IdleAnimationChoice.BASE;
        }
        if (roll < 74) {
            return IdleAnimationChoice.PECK_1;
        }
        if (roll < 90) {
            return IdleAnimationChoice.PECK_2;
        }
        return IdleAnimationChoice.DISPLAY;
    }

    private <T extends AbstractColumbidEntity> PlayState movementController(AnimationState<T> animationState) {
        RawAnimation preview = this.guidePreviewAnimation.animation();
        if (preview != null) {
            return animationState.setAndContinue(preview);
        }
        if (this.shouldPlayFlyAnimation()) {
            if (this.flapOnceTicks > 0) {
                return animationState.setAndContinue(FLY_FLAP_ONCE_ANIMATION);
            }
            if (this.getBehaviorState() == ColumbidBehaviorState.GLIDING) {
                return animationState.setAndContinue(FLY_LOOP_ANIMATION);
            }
            return animationState.setAndContinue(FLY_FLAPPING_LOOP_ANIMATION);
        }
        if (this.shouldPlayWalkAnimation()) {
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        return animationState.setAndContinue(this.pickIdleAnimation());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable)this, "movement", 4, this::movementController)});
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    private enum IdleAnimationChoice {
        BASE(IDLE_ANIMATION, 70, 130),
        PECK_1(IDLE_DIFF_1_ANIMATION, 45, 70),
        PECK_2(IDLE_DIFF_2_ANIMATION, 48, 76),
        DISPLAY(IDLE_DIFF_3_ANIMATION, 70, 95);

        private final RawAnimation animation;
        private final int minDuration;
        private final int maxDuration;

        IdleAnimationChoice(RawAnimation animation, int minDuration, int maxDuration) {
            this.animation = animation;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
        }

        private int nextDuration(RandomSource random) {
            return this.minDuration + random.nextInt(this.maxDuration - this.minDuration + 1);
        }
    }

    public enum GuidePreviewAnimation {
        NONE(null),
        IDLE(IDLE_ANIMATION),
        LOOK_1(IDLE_DIFF_1_ANIMATION),
        LOOK_2(IDLE_DIFF_2_ANIMATION),
        LOOK_3(IDLE_DIFF_3_ANIMATION),
        WALK(WALK_ANIMATION),
        FLY_FLAP(FLY_FLAPPING_LOOP_ANIMATION),
        GLIDE(FLY_LOOP_ANIMATION);

        private final RawAnimation animation;

        GuidePreviewAnimation(RawAnimation animation) {
            this.animation = animation;
        }

        private RawAnimation animation() {
            return this.animation;
        }
    }

    private static class ColumbidEatSeedGoal extends Goal {
        private final AbstractColumbidEntity columbid;
        private ItemEntity targetItem;
        private int repathTicks;
        private int peckTicks;
        private int scanCooldown;

        ColumbidEatSeedGoal(AbstractColumbidEntity columbid) {
            this.columbid = columbid;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.columbid.canStartSeedGoal()) {
                return false;
            }
            if (this.scanCooldown-- > 0) {
                return false;
            }
            BirdSpecies species = BirdSpecies.from(this.columbid);
            int interval = species == null ? 20 : BirdConfigManager.foodScanInterval(species);
            this.scanCooldown = interval + this.columbid.getRandom().nextInt(Math.max(1, interval / 2));
            this.targetItem = this.findSeed();
            return this.targetItem != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetItem != null
                    && this.targetItem.isAlive()
                    && this.columbid.isSeedFoodForThisBird(this.targetItem.getItem())
                    && this.columbid.canStartSeedGoal()
                    && this.columbid.distanceToSqr(this.targetItem) < 196.0D;
        }

        @Override
        public void start() {
            this.repathTicks = 0;
            this.peckTicks = 8;
            this.columbid.setBehaviorState(ColumbidBehaviorState.FORAGING);
        }

        @Override
        public void tick() {
            this.columbid.getLookControl().setLookAt(this.targetItem, 20.0F, 20.0F);
            double distanceSqr = this.columbid.distanceToSqr(this.targetItem);
            if (distanceSqr > 1.7D) {
                if (--this.repathTicks <= 0) {
                    this.repathTicks = 10;
                    this.columbid.getNavigation().moveTo(this.targetItem, 0.88D);
                    this.columbid.setBehaviorState(ColumbidBehaviorState.FORAGING);
                }
                return;
            }
            this.columbid.getNavigation().stop();
            if (--this.peckTicks > 0) {
                return;
            }
            ItemStack stack = this.targetItem.getItem();
            if (this.columbid.isSeedFoodForThisBird(stack)) {
                boolean preferredSeed = isPreferredTamingSeed(stack);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    this.targetItem.discard();
                } else {
                    this.targetItem.setItem(stack);
                }
                this.columbid.seedTrustTicks = Math.max(this.columbid.seedTrustTicks, 900);
                this.columbid.birdBrain().onEat(preferredSeed ? 0.28F : 0.18F);
                this.columbid.triggerEatingAnimation(30);
            }
            this.peckTicks = 18 + this.columbid.getRandom().nextInt(16);
        }

        @Override
        public void stop() {
            this.targetItem = null;
            if (this.columbid.getBehaviorState() == ColumbidBehaviorState.FORAGING) {
                this.columbid.setBehaviorState(ColumbidBehaviorState.IDLE);
            }
        }

        private ItemEntity findSeed() {
            if (!(this.columbid.level() instanceof ServerLevel serverLevel)
                    || !BirdScanBudget.tryAcquire(serverLevel, this.columbid)) {
                return null;
            }
            List<ItemEntity> items = this.columbid.level().getEntitiesOfClass(ItemEntity.class,
                    this.columbid.getBoundingBox().inflate(10.0D, 3.0D, 10.0D),
                    item -> item.isAlive() && this.columbid.isSeedFoodForThisBird(item.getItem()));
            ItemEntity best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (ItemEntity item : items) {
                double score = -this.columbid.distanceToSqr(item);
                if (isPreferredTamingSeed(item.getItem())) {
                    score += 18.0D;
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = item;
                }
            }
            return best;
        }
    }

    private static class ColumbidFollowOwnerGoal extends Goal {
        private final AbstractColumbidEntity columbid;
        private final double speed;
        private final float stopDistance;
        private final float startDistance;
        private LivingEntity owner;
        private int repathTicks;

        ColumbidFollowOwnerGoal(AbstractColumbidEntity columbid, double speed, float stopDistance, float startDistance) {
            this.columbid = columbid;
            this.speed = speed;
            this.stopDistance = stopDistance;
            this.startDistance = startDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.columbid.isTame()
                    || !this.columbid.isBirdCommandMode(BirdCommandMode.FOLLOW)
                    || this.columbid.isControlledFlightActive()) {
                return false;
            }
            this.owner = this.columbid.getOwner();
            return this.owner != null && this.owner.isAlive() && this.columbid.distanceToSqr(this.owner) > (double)(this.startDistance * this.startDistance);
        }

        @Override
        public boolean canContinueToUse() {
            return this.owner != null
                    && this.owner.isAlive()
                    && this.columbid.isBirdCommandMode(BirdCommandMode.FOLLOW)
                    && !this.columbid.isControlledFlightActive()
                    && this.columbid.distanceToSqr(this.owner) > (double)(this.stopDistance * this.stopDistance)
                    && !this.columbid.getNavigation().isDone();
        }

        @Override
        public void start() {
            this.repathTicks = 0;
            this.columbid.setBehaviorState(ColumbidBehaviorState.FOLLOWING_OWNER);
        }

        @Override
        public void tick() {
            this.columbid.getLookControl().setLookAt(this.owner, 10.0F, this.columbid.getMaxHeadXRot());
            this.columbid.setBehaviorState(ColumbidBehaviorState.FOLLOWING_OWNER);
            if (this.columbid.distanceToSqr(this.owner) > 64.0D && this.columbid.onGround() && this.columbid.flightCooldown <= 0) {
                Vec3 target = this.columbid.findDryLandingTargetNear(this.owner.blockPosition(), 5, 10);
                if (target != null) {
                    this.columbid.startControlledFlight(target, 120, HIGH_FLIGHT_SPEED, false, true);
                    return;
                }
            }
            if (--this.repathTicks <= 0) {
                this.repathTicks = 12;
                this.columbid.getNavigation().moveTo(this.owner, this.speed);
            }
        }

        @Override
        public void stop() {
            this.owner = null;
            this.columbid.getNavigation().stop();
            if (this.columbid.getBehaviorState() == ColumbidBehaviorState.FOLLOWING_OWNER) {
                this.columbid.setBehaviorState(ColumbidBehaviorState.IDLE);
            }
        }
    }

    private static class ColumbidRoostGoal extends Goal {
        private final AbstractColumbidEntity columbid;
        private BlockPos roostPos;
        private int roostTicks;
        private int repathTicks;

        ColumbidRoostGoal(AbstractColumbidEntity columbid) {
            this.columbid = columbid;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            boolean weatherShelter = this.columbid.sensesIncomingBadWeather();
            int chance = weatherShelter ? 28 : 80;
            if ((this.columbid.isTame() && !this.columbid.isBirdCommandMode(BirdCommandMode.FREE))
                    || (!this.columbid.isRoostTime() && !weatherShelter)
                    || this.columbid.isRestInterrupted()
                    || this.columbid.isControlledFlightActive()
                    || this.columbid.getRandom().nextInt(chance) != 0) {
                return false;
            }
            this.roostPos = this.findRoost();
            return this.roostPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.roostTicks > 0
                    && this.roostPos != null
                    && (!this.columbid.isTame() || this.columbid.isBirdCommandMode(BirdCommandMode.FREE))
                    && (this.columbid.isRoostTime() || this.columbid.sensesIncomingBadWeather())
                    && !this.columbid.isRestInterrupted()
                    && !this.columbid.isInWaterOrBubble();
        }

        @Override
        public void start() {
            this.roostTicks = 260 + this.columbid.getRandom().nextInt(360);
            this.repathTicks = 0;
            this.columbid.setBehaviorState(ColumbidBehaviorState.ROOSTING);
        }

        @Override
        public void tick() {
            --this.roostTicks;
            double distanceSqr = this.columbid.distanceToSqr(Vec3.atCenterOf(this.roostPos));
            if (distanceSqr > 2.5D) {
                Vec3 target = Vec3.atBottomCenterOf(this.roostPos);
                if (distanceSqr > 64.0D && this.columbid.onGround() && this.columbid.flightCooldown <= 0) {
                    this.columbid.startControlledFlight(target, 95, HIGH_FLIGHT_SPEED, false, true);
                    return;
                }
                if (--this.repathTicks <= 0 || this.columbid.getNavigation().isDone()) {
                    this.repathTicks = 18 + this.columbid.getRandom().nextInt(16);
                    this.columbid.getNavigation().moveTo(target.x, target.y, target.z, 0.9D);
                }
                return;
            }
            this.columbid.getNavigation().stop();
            this.columbid.setBehaviorState(ColumbidBehaviorState.ROOSTING);
            if (this.roostTicks % 80 == 0) {
                this.columbid.birdBrain().onRest(0.03F);
            }
        }

        @Override
        public void stop() {
            this.roostPos = null;
            this.repathTicks = 0;
            if (this.columbid.getBehaviorState() == ColumbidBehaviorState.ROOSTING) {
                this.columbid.setBehaviorState(ColumbidBehaviorState.IDLE);
            }
        }

        private BlockPos findRoost() {
            BlockPos origin = this.columbid.homePos != null && this.columbid.isTame() ? this.columbid.homePos : this.columbid.blockPosition();
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            for (int attempt = 0; attempt < 48; ++attempt) {
                int x = origin.getX() + this.columbid.getRandom().nextInt(19) - 9;
                int z = origin.getZ() + this.columbid.getRandom().nextInt(19) - 9;
                int y = origin.getY() + this.columbid.getRandom().nextInt(9) + 1;
                mutable.set(x, y, z);
                if (this.columbid.isSafeDryLanding(mutable)
                        && this.isRoostBlock(this.columbid.level().getBlockState(mutable.below()))
                        && (!this.columbid.sensesIncomingBadWeather() || !this.columbid.level().canSeeSky(mutable))) {
                    return mutable.immutable();
                }
            }
            return null;
        }

        private boolean isRoostBlock(BlockState state) {
            return state.is(BlockTags.LEAVES)
                    || state.is(BlockTags.LOGS)
                    || state.getBlock() instanceof FenceBlock
                    || state.getBlock() instanceof FenceGateBlock
                    || state.is(Blocks.HAY_BLOCK);
        }
    }

    private static class ColumbidPairBondGoal extends Goal {
        private final AbstractColumbidEntity columbid;

        ColumbidPairBondGoal(AbstractColumbidEntity columbid) {
            this.columbid = columbid;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.columbid.supportsPairBond()
                    && !this.columbid.isTame()
                    && this.columbid.canStartGroundSocialGoal()
                    && this.columbid.pairScanCooldown <= 0;
        }

        @Override
        public void start() {
            this.columbid.pairScanCooldown = 180 + this.columbid.getRandom().nextInt(160);
            Optional<AbstractColumbidEntity> current = this.columbid.pairPartner();
            if (current.isEmpty() && this.columbid.pairPartnerUUID != null) {
                this.columbid.pairPartnerUUID = null;
                this.columbid.pairLostTicks = 0;
            }
            if (current.isPresent()) {
                if (!this.columbid.hasReciprocalPairWith(current.get())) {
                    this.columbid.pairPartnerUUID = null;
                    this.columbid.pairLostTicks = 0;
                } else if (this.columbid.distanceToSqr(current.get()) > 625.0D) {
                    if (++this.columbid.pairLostTicks > 8) {
                        this.columbid.pairPartnerUUID = null;
                        this.columbid.pairLostTicks = 0;
                    }
                } else {
                    this.columbid.pairLostTicks = 0;
                }
                if (this.columbid.pairPartnerUUID != null) {
                    return;
                }
            }
            List<AbstractColumbidEntity> nearby = BirdFlockManager.nearby(this.columbid, AbstractColumbidEntity.class, 16.0D)
                    .stream().filter(other -> other != this.columbid && !other.isTame() && other.pairPartnerUUID == null).toList();
            if (nearby.isEmpty()) {
                return;
            }
            AbstractColumbidEntity partner = nearby.get(this.columbid.getRandom().nextInt(nearby.size()));
            if (this.columbid.pairPartnerUUID != null || partner.pairPartnerUUID != null) {
                return;
            }
            this.columbid.pairPartnerUUID = partner.getUUID();
            partner.pairPartnerUUID = this.columbid.getUUID();
            partner.pairScanCooldown = Math.max(partner.pairScanCooldown, 180);
            this.columbid.setBehaviorStateFor(ColumbidBehaviorState.COURTING, 45);
            partner.setBehaviorStateFor(ColumbidBehaviorState.COURTING, 45);
            this.columbid.spawnCourtshipParticles(3);
            partner.spawnCourtshipParticles(3);
        }
    }

    private static class ColumbidFlockOrPairGoal extends Goal {
        private final AbstractColumbidEntity columbid;
        private AbstractColumbidEntity socialTarget;
        private Vec3 target;
        private int moveTicks;
        private boolean followingPartner;

        ColumbidFlockOrPairGoal(AbstractColumbidEntity columbid) {
            this.columbid = columbid;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.columbid.canStartGroundSocialGoal() || this.columbid.getRandom().nextInt(34) != 0) {
                return false;
            }
            Optional<AbstractColumbidEntity> partner = this.columbid.pairPartner();
            if (partner.isPresent()) {
                double distance = this.columbid.distanceToSqr(partner.get());
                if (distance > 18.0D && distance < 196.0D) {
                    this.socialTarget = partner.get();
                    this.followingPartner = true;
                    this.target = partner.get().position().add(this.columbid.randomSigned(1.2D), 0.0D, this.columbid.randomSigned(1.2D));
                    return true;
                }
            }
            List<AbstractColumbidEntity> flock = BirdFlockManager.nearby(this.columbid, AbstractColumbidEntity.class, 12.0D)
                    .stream().filter(other -> other != this.columbid).toList();
            if (flock.isEmpty()) {
                return false;
            }
            AbstractColumbidEntity other = flock.get(this.columbid.getRandom().nextInt(flock.size()));
            Vec3 away = this.columbid.position().subtract(other.position()).multiply(1.0D, 0.0D, 1.0D);
            if (this.columbid.distanceToSqr(other) < 2.6D && away.lengthSqr() > 1.0E-4D) {
                this.socialTarget = other;
                this.followingPartner = false;
                this.target = this.columbid.position().add(away.normalize().scale(2.6D));
                return true;
            }
            if (this.columbid.distanceToSqr(other) > 36.0D) {
                this.socialTarget = other;
                this.followingPartner = false;
                this.target = other.position();
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.moveTicks > 0
                    && this.target != null
                    && this.socialTarget != null
                    && this.socialTarget.isAlive()
                    && !this.columbid.isControlledFlightActive()
                    && this.columbid.canStartGroundSocialGoal()
                    && (!this.followingPartner || this.columbid.hasReciprocalPairWith(this.socialTarget));
        }

        @Override
        public void start() {
            this.moveTicks = 35 + this.columbid.getRandom().nextInt(35);
            this.columbid.setBehaviorState(ColumbidBehaviorState.PAIR_FOLLOWING);
            if (this.columbid.onGround()
                    && this.columbid.flightCooldown <= 0
                    && this.target != null
                    && this.columbid.distanceToSqr(this.target) > 64.0D) {
                Vec3 landing = this.columbid.findDryLandingTargetNear(BlockPos.containing(this.target), 4, 7);
                if (landing != null && this.columbid.startControlledFlight(landing, 100 + this.columbid.getRandom().nextInt(45), HIGH_FLIGHT_SPEED, false, true)) {
                    return;
                }
            }
            this.columbid.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, 0.78D);
        }

        @Override
        public void tick() {
            --this.moveTicks;
            this.columbid.getLookControl().setLookAt(this.socialTarget, 18.0F, 18.0F);
            if (this.followingPartner && this.moveTicks % 18 == 0) {
                double distanceSqr = this.columbid.distanceToSqr(this.socialTarget);
                if (distanceSqr > 20.0D) {
                    this.target = this.socialTarget.position().add(this.columbid.randomSigned(1.4D), 0.0D, this.columbid.randomSigned(1.4D));
                    this.columbid.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, 0.78D);
                } else if (distanceSqr < 2.4D) {
                    Vec3 away = this.columbid.position().subtract(this.socialTarget.position()).multiply(1.0D, 0.0D, 1.0D);
                    if (away.lengthSqr() > 1.0E-4D) {
                        this.target = this.columbid.position().add(away.normalize().scale(1.8D));
                        this.columbid.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, 0.62D);
                    }
                }
            }
        }

        @Override
        public void stop() {
            this.socialTarget = null;
            this.target = null;
            this.followingPartner = false;
            if (this.columbid.getBehaviorState() == ColumbidBehaviorState.PAIR_FOLLOWING) {
                this.columbid.setBehaviorState(ColumbidBehaviorState.IDLE);
            }
        }
    }

    private static class ColumbidCourtshipGoal extends Goal {
        private final AbstractColumbidEntity columbid;
        private AbstractColumbidEntity partner;
        private int courtshipTicks;
        private int stepCooldown;

        ColumbidCourtshipGoal(AbstractColumbidEntity columbid) {
            this.columbid = columbid;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.columbid.supportsPairBond()
                    || this.columbid.isTame()
                    || this.columbid.courtshipCooldown > 0
                    || !this.columbid.canStartGroundSocialGoal()
                    || this.columbid.getRandom().nextInt(220) != 0) {
                return false;
            }
            Optional<AbstractColumbidEntity> currentPartner = this.columbid.pairPartner();
            if (currentPartner.isEmpty() || !this.columbid.hasReciprocalPairWith(currentPartner.get())) {
                return false;
            }
            double distanceSqr = this.columbid.distanceToSqr(currentPartner.get());
            if (distanceSqr < 1.8D || distanceSqr > 81.0D) {
                return false;
            }
            this.partner = currentPartner.get();
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.courtshipTicks > 0
                    && this.partner != null
                    && this.partner.isAlive()
                    && this.columbid.canStartGroundSocialGoal()
                    && this.columbid.hasReciprocalPairWith(this.partner)
                    && this.columbid.distanceToSqr(this.partner) < 100.0D;
        }

        @Override
        public void start() {
            this.courtshipTicks = 80 + this.columbid.getRandom().nextInt(81);
            this.stepCooldown = 0;
            this.columbid.courtshipCooldown = 760 + this.columbid.getRandom().nextInt(760);
            this.columbid.setBehaviorStateFor(ColumbidBehaviorState.COURTING, Math.min(this.courtshipTicks, 90));
            this.partner.setBehaviorStateFor(ColumbidBehaviorState.COURTING, 45);
            this.columbid.spawnCourtshipParticles(3);
        }

        @Override
        public void tick() {
            --this.courtshipTicks;
            if (this.stepCooldown > 0) {
                --this.stepCooldown;
            }
            this.columbid.getLookControl().setLookAt(this.partner, 26.0F, 26.0F);
            this.partner.getLookControl().setLookAt(this.columbid, 22.0F, 22.0F);
            if (this.courtshipTicks % 42 == 0) {
                this.columbid.spawnCourtshipParticles(2);
            }
            double distanceSqr = this.columbid.distanceToSqr(this.partner);
            if (distanceSqr > 20.0D) {
                if (this.stepCooldown <= 0 || this.columbid.getNavigation().isDone()) {
                    this.stepCooldown = 12 + this.columbid.getRandom().nextInt(10);
                    this.columbid.getNavigation().moveTo(this.partner, 0.68D);
                }
                return;
            }
            if (distanceSqr < 2.2D) {
                Vec3 away = this.columbid.position().subtract(this.partner.position()).multiply(1.0D, 0.0D, 1.0D);
                if (away.lengthSqr() > 1.0E-4D) {
                    Vec3 target = this.columbid.position().add(away.normalize().scale(1.6D));
                    this.columbid.getNavigation().moveTo(target.x, target.y, target.z, 0.55D);
                }
                return;
            }
            if (this.stepCooldown <= 0) {
                this.stepCooldown = 18 + this.columbid.getRandom().nextInt(18);
                Vec3 direction = this.columbid.position().subtract(this.partner.position()).multiply(1.0D, 0.0D, 1.0D);
                if (direction.lengthSqr() <= 1.0E-4D) {
                    direction = this.columbid.randomHorizontalDirection();
                }
                Vec3 orbit = rotateHorizontal(direction.normalize(), this.columbid.randomSigned(0.9D)).scale(2.0D + this.columbid.getRandom().nextDouble() * 0.8D);
                Vec3 target = this.columbid.findDryLandingTarget(BlockPos.containing(this.partner.position().add(orbit)), 2);
                if (target != null) {
                    this.columbid.getNavigation().moveTo(target.x, target.y, target.z, 0.56D);
                } else {
                    this.columbid.getNavigation().stop();
                }
            }
        }

        @Override
        public void stop() {
            this.columbid.getNavigation().stop();
            if (this.columbid.getBehaviorState() == ColumbidBehaviorState.COURTING) {
                this.columbid.setBehaviorState(ColumbidBehaviorState.IDLE);
            }
            if (this.partner != null && this.partner.getBehaviorState() == ColumbidBehaviorState.COURTING) {
                this.partner.setBehaviorState(ColumbidBehaviorState.IDLE);
            }
            this.partner = null;
        }
    }

    private static class ColumbidChaseSmallBirdGoal extends Goal {
        private final AbstractColumbidEntity columbid;
        private LivingEntity target;
        private int chaseTicks;
        private int repathTicks;

        ColumbidChaseSmallBirdGoal(AbstractColumbidEntity columbid) {
            this.columbid = columbid;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.columbid.supportsChasing()
                    || this.columbid.isTame()
                    || this.columbid.getHealth() < this.columbid.getMaxHealth() * 0.5F
                    || this.columbid.chaseCooldown > 0
                    || !this.columbid.canStartGroundSocialGoal()
                    || this.columbid.getRandom().nextInt(420) != 0) {
                return false;
            }
            if (!(this.columbid.level() instanceof ServerLevel serverLevel)
                    || !BirdScanBudget.tryAcquire(serverLevel, this.columbid)) {
                return false;
            }
            List<LivingEntity> candidates = this.columbid.level().getEntitiesOfClass(LivingEntity.class, this.columbid.getBoundingBox().inflate(8.0D, 3.0D, 8.0D), entity -> {
                if (entity instanceof SparrowEntity sparrow) {
                    return !sparrow.isTame() && sparrow.isAlive();
                }
                if (entity instanceof AbstractColumbidEntity other) {
                    return other.getClass() == this.columbid.getClass()
                            && other != this.columbid
                            && other.isAlive()
                            && !other.isTame()
                            && other.pairPartnerUUID == null
                            && !this.columbid.isPairedWith(other);
                }
                return false;
            });
            if (candidates.isEmpty()) {
                return false;
            }
            this.target = candidates.get(this.columbid.getRandom().nextInt(candidates.size()));
            return this.target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.chaseTicks > 0
                    && this.target != null
                    && this.target.isAlive()
                    && this.columbid.distanceToSqr(this.target) < 196.0D
                    && !this.columbid.isControlledFlightActive();
        }

        @Override
        public void start() {
            this.chaseTicks = 30 + this.columbid.getRandom().nextInt(31);
            this.repathTicks = 0;
            this.columbid.chaseCooldown = 1200 + this.columbid.getRandom().nextInt(1201);
            this.columbid.setBehaviorState(ColumbidBehaviorState.CHASING);
        }

        @Override
        public void tick() {
            --this.chaseTicks;
            this.columbid.getLookControl().setLookAt(this.target, 25.0F, 25.0F);
            if (this.columbid.distanceToSqr(this.target) < 3.2D) {
                this.columbid.getNavigation().stop();
                this.chaseTicks = Math.min(this.chaseTicks, 12);
                return;
            }
            if (--this.repathTicks <= 0 || this.columbid.getNavigation().isDone()) {
                this.repathTicks = 8 + this.columbid.getRandom().nextInt(8);
                this.columbid.getNavigation().moveTo(this.target, 0.98D);
            }
        }

        @Override
        public void stop() {
            this.target = null;
            this.repathTicks = 0;
            if (this.columbid.getBehaviorState() == ColumbidBehaviorState.CHASING) {
                this.columbid.setBehaviorState(ColumbidBehaviorState.IDLE);
            }
        }
    }

    private static class ColumbidAmbientFlightGoal extends Goal {
        private final AbstractColumbidEntity columbid;

        ColumbidAmbientFlightGoal(AbstractColumbidEntity columbid) {
            this.columbid = columbid;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if ((this.columbid.isTame() && !this.columbid.isBirdCommandMode(BirdCommandMode.FREE))
                    || !this.columbid.canStartGroundSocialGoal()
                    || !this.columbid.getNavigation().isDone()
                    || this.columbid.flightCooldown > 0
                    || this.columbid.getBehaviorState() == ColumbidBehaviorState.FORAGING
                    || this.columbid.getBehaviorState() == ColumbidBehaviorState.EATING) {
                return false;
            }
            return this.columbid.getRandom().nextInt(Math.max(80, this.columbid.ambientFlightChance())) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            Vec3 direction = this.columbid.randomHorizontalDirection();
            if (this.columbid.getRandom().nextInt(4) == 0) {
                direction = this.columbid.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
            }
            if (direction.lengthSqr() <= 1.0E-4D) {
                direction = this.columbid.randomHorizontalDirection();
            }
            boolean badWeather = this.columbid.sensesIncomingBadWeather();
            Vec3 target = this.columbid.findFlightLandingTarget(direction, badWeather ? 18 : 52, badWeather ? 38 : 96, true);
            if (target == null && !badWeather) {
                target = this.columbid.findFlightLandingTarget(direction, 28, 54, true);
            }
            if (target != null) {
                int duration = AUTONOMOUS_FLIGHT_MIN_TICKS + this.columbid.getRandom().nextInt(AUTONOMOUS_FLIGHT_RANDOM_TICKS);
                this.columbid.startAutonomousCruiseFlight(target, duration);
            }
        }
    }

    private static class ColumbidGroundForagingGoal extends Goal {
        private final AbstractColumbidEntity columbid;
        private Vec3 strollTarget;
        private int remainingTicks;
        private int repathTicks;
        private int peckCooldown;
        private int lookCooldown;

        ColumbidGroundForagingGoal(AbstractColumbidEntity columbid) {
            this.columbid = columbid;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.columbid.canStartGroundSocialGoal() || !this.columbid.getNavigation().isDone()) {
                return false;
            }
            int chance = this.columbid.birdBrain().wantsForage() ? 5 : (this.columbid.prefersHumanSettlements() ? 12 : 16);
            if (this.columbid.sensesIncomingBadWeather()) {
                chance += 8;
            }
            return this.columbid.getRandom().nextInt(chance) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.remainingTicks > 0
                    && this.columbid.canStartGroundSocialGoal()
                    && !this.columbid.isRoostTime();
        }

        @Override
        public void start() {
            this.remainingTicks = 120 + this.columbid.getRandom().nextInt(141);
            this.repathTicks = 0;
            this.peckCooldown = 8 + this.columbid.getRandom().nextInt(18);
            this.lookCooldown = 20 + this.columbid.getRandom().nextInt(35);
            this.columbid.setBehaviorState(ColumbidBehaviorState.FORAGING);
        }

        @Override
        public void tick() {
            --this.remainingTicks;
            if (this.repathTicks > 0) {
                --this.repathTicks;
            }
            if (this.peckCooldown > 0) {
                --this.peckCooldown;
            }
            if (this.lookCooldown > 0) {
                --this.lookCooldown;
            }
            if (this.columbid.behaviorStateLockTicks > 0 && this.columbid.getNavigation().isDone()) {
                return;
            }
            if (this.peckCooldown <= 0 && this.columbid.getNavigation().isDone()) {
                this.peckAtGround();
                this.peckCooldown = 22 + this.columbid.getRandom().nextInt(36);
                return;
            }
            if (this.lookCooldown <= 0 && this.columbid.getNavigation().isDone()) {
                this.lookAround();
                this.lookCooldown = 32 + this.columbid.getRandom().nextInt(46);
            }
            if (this.repathTicks <= 0 && (this.columbid.getNavigation().isDone() || this.strollTarget == null || this.columbid.distanceToSqr(this.strollTarget) < 1.8D)) {
                this.chooseStrollTarget();
            }
            if (this.strollTarget != null && this.columbid.getNavigation().isDone()) {
                this.columbid.setBehaviorState(ColumbidBehaviorState.FORAGING);
                this.columbid.getNavigation().moveTo(this.strollTarget.x, this.strollTarget.y, this.strollTarget.z, this.columbid.prefersHumanSettlements() ? 0.78D : 0.70D);
            }
        }

        @Override
        public void stop() {
            this.strollTarget = null;
            this.remainingTicks = 0;
            if (!this.columbid.isControlledFlightActive()
                    && this.columbid.getBehaviorState() != ColumbidBehaviorState.EATING
                    && this.columbid.getBehaviorState() != ColumbidBehaviorState.ROOSTING
                    && this.columbid.getBehaviorState() != ColumbidBehaviorState.SLEEPING) {
                this.columbid.setBehaviorState(ColumbidBehaviorState.IDLE);
            }
        }

        private void chooseStrollTarget() {
            this.repathTicks = 24 + this.columbid.getRandom().nextInt(28);
            this.strollTarget = this.columbid.findGroundStrollTarget(this.columbid.prefersHumanSettlements() ? 7 : 5, 2);
        }

        private void peckAtGround() {
            this.columbid.getNavigation().stop();
            double x = this.columbid.getX() + this.columbid.randomSigned(0.45D);
            double z = this.columbid.getZ() + this.columbid.randomSigned(0.45D);
            this.columbid.getLookControl().setLookAt(x, this.columbid.getY() + 0.1D, z, 18.0F, 18.0F);
            this.columbid.triggerPeckAnimation(18 + this.columbid.getRandom().nextInt(10));
        }

        private void lookAround() {
            double x = this.columbid.getX() + this.columbid.randomSigned(3.5D);
            double z = this.columbid.getZ() + this.columbid.randomSigned(3.5D);
            this.columbid.getLookControl().setLookAt(x, this.columbid.getEyeY(), z, 16.0F, 16.0F);
            this.columbid.setBehaviorStateFor(ColumbidBehaviorState.CURIOUS, 18 + this.columbid.getRandom().nextInt(18));
        }
    }

    private static class ColumbidIdleGoal extends Goal {
        private final AbstractColumbidEntity columbid;
        private Vec3 strollTarget;
        private int idleTicks;

        ColumbidIdleGoal(AbstractColumbidEntity columbid) {
            this.columbid = columbid;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.columbid.canStartGroundSocialGoal()
                    && this.columbid.getNavigation().isDone()
                    && this.columbid.getRandom().nextInt(18) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.idleTicks > 0 && !this.columbid.isControlledFlightActive();
        }

        @Override
        public void start() {
            this.idleTicks = 45 + this.columbid.getRandom().nextInt(85);
            int roll = this.columbid.getRandom().nextInt(100);
            if (this.columbid.supportsPairBond() && this.columbid.pairPartner().isPresent() && this.columbid.courtshipCooldown <= 0 && roll < 8) {
                this.columbid.courtshipCooldown = 900 + this.columbid.getRandom().nextInt(800);
                this.columbid.setBehaviorStateFor(ColumbidBehaviorState.COURTING, Math.min(this.idleTicks, 90));
                this.columbid.spawnCourtshipParticles(3);
                return;
            }
            if (roll < 20) {
                this.columbid.getNavigation().stop();
                this.columbid.setBehaviorStateFor(ColumbidBehaviorState.PREENING, Math.min(this.idleTicks, 80));
            } else if (roll < 38) {
                this.columbid.getNavigation().stop();
                this.columbid.setBehaviorStateFor(ColumbidBehaviorState.CURIOUS, Math.min(this.idleTicks, 64));
            } else {
                this.strollTarget = this.columbid.findGroundStrollTarget(5, 2);
                if (this.strollTarget != null) {
                    this.columbid.setBehaviorState(this.columbid.birdBrain().wantsForage() ? ColumbidBehaviorState.FORAGING : ColumbidBehaviorState.WALKING);
                    this.columbid.getNavigation().moveTo(this.strollTarget.x, this.strollTarget.y, this.strollTarget.z, 0.70D);
                } else {
                    this.columbid.setBehaviorState(ColumbidBehaviorState.IDLE);
                }
            }
        }

        @Override
        public void tick() {
            --this.idleTicks;
            if (this.columbid.getBehaviorState() == ColumbidBehaviorState.COURTING) {
                this.columbid.pairPartner().ifPresent(partner -> this.columbid.getLookControl().setLookAt(partner, 25.0F, 25.0F));
            } else if (this.strollTarget == null && this.columbid.getRandom().nextInt(28) == 0) {
                this.columbid.getLookControl().setLookAt(this.columbid.getX() + this.columbid.randomSigned(3.0D), this.columbid.getEyeY(), this.columbid.getZ() + this.columbid.randomSigned(3.0D), 16.0F, 16.0F);
            }
        }

        @Override
        public void stop() {
            this.strollTarget = null;
            if (!this.columbid.isControlledFlightActive()
                    && this.columbid.getBehaviorState() != ColumbidBehaviorState.EATING
                    && this.columbid.getBehaviorState() != ColumbidBehaviorState.ROOSTING) {
                this.columbid.setBehaviorState(ColumbidBehaviorState.IDLE);
            }
        }

    }
}
