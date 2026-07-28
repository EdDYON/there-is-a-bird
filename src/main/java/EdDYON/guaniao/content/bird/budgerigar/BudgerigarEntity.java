package EdDYON.guaniao.content.bird.budgerigar;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdSleepWakeable;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.command.BirdCommandInteraction;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.content.bird.command.BirdRoostGoal;
import EdDYON.guaniao.content.bird.command.BirdStayGoal;
import EdDYON.guaniao.content.bird.command.CommandableBird;
import EdDYON.guaniao.content.bird.flock.FlockCompatibleBird;
import EdDYON.guaniao.content.bird.flock.BirdFlockManager;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.brain.BirdBrain;
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
import EdDYON.guaniao.content.advancement.BirdAdvancements;
import EdDYON.guaniao.content.bird.species.BudgerigarProfile;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.JukeboxBlock;
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

public class BudgerigarEntity extends TamableAnimal implements GeoEntity, FlyingAnimal, ScalableBirdModel, BirdFlightAware, BirdBathMountable, BirdBathFeedingAnimatable, CommandableBird, FlockCompatibleBird, BirdSleepWakeable {
    private static final EntityDataAccessor<Integer> BEHAVIOR_STATE = SynchedEntityData.defineId(BudgerigarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN_VARIANT = SynchedEntityData.defineId(BudgerigarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MODEL_SCALE = SynchedEntityData.defineId(BudgerigarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COMMAND_MODE = SynchedEntityData.defineId(BudgerigarEntity.class, EntityDataSerializers.INT);
    private static final byte TAMING_FAILED_EVENT = 6;
    private static final byte TAMING_SUCCEEDED_EVENT = 7;
    private static final ResourceLocation CHIRPY_PARTNER_ADVANCEMENT = new ResourceLocation("guaniao", "husbandry/chirpy_partner");
    private static final int MUSIC_SCAN_RADIUS = 8;
    private static final int MUSIC_GROUP_RADIUS = 10;
    private static final int AMBIENT_AIR_CRUISE_MIN_TICKS = 110;
    private static final int AMBIENT_AIR_CRUISE_RANDOM_TICKS = 120;
    private static final int ESCAPE_AIR_CRUISE_MIN_TICKS = 80;
    private static final int ESCAPE_AIR_CRUISE_RANDOM_TICKS = 70;
    private static final int MAX_CONTROLLED_FLIGHT_TICKS = 520;
    private static final int MAX_LANDING_RETRIES = 2;
    private static final BirdFlightProfile FLIGHT_PROFILE = BirdFlightProfile.BUDGERIGAR;
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation PREEN_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_1").thenLoop("idle");
    private static final RawAnimation CURIOUS_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_2").thenLoop("idle");
    private static final RawAnimation DANCE_ANIMATION = RawAnimation.begin().thenLoop("idle_diff_3");
    private static final RawAnimation EAT_ANIMATION = RawAnimation.begin().thenPlay("eat").thenLoop("idle");
    private static final RawAnimation SLEEP_ANIMATION = RawAnimation.begin().thenPlay("sleep").thenLoop("sleep_loop");
    private static final RawAnimation SLEEP_LOOP_ANIMATION = RawAnimation.begin().thenLoop("sleep_loop");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation FLY_ANIMATION = RawAnimation.begin().thenLoop("fly_flapping_wing_loop");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final BirdBrain birdBrain = new BirdBrain(this, BudgerigarProfile.INSTANCE);
    private BudgerigarBehaviorState behaviorState = BudgerigarBehaviorState.IDLE;
    private GuidePreviewAnimation guidePreviewAnimation = GuidePreviewAnimation.NONE;
    private int behaviorStateLockTicks;
    private int eatingTicks;
    private int foodCooldown;
    private int trustTicks;
    private int curiousTicks;
    private int nearbyMusicTicks;
    private int musicScanCooldown;
    private int externalFrightTicks;
    private int restInterruptionTicks;
    private int flightTicks;
    private int timeFlying;
    private int flightCooldown;
    private int hoverRetargetTicks;
    private int landingRetryCount;
    private int pendingFrightTicks;
    private int pendingFrightDuration;
    private int idleAnimationTicks;
    private int postTameActionTicks;
    private int postTameActionSwapTicks;
    private boolean escapeFlightActive;
    private boolean landingFlight;
    private RawAnimation currentIdleAnimation = IDLE_ANIMATION;
    private UUID interestedPlayerUUID;
    private BlockPos musicSourcePos;
    private Vec3 frightSource;
    private Vec3 pendingFrightSource;
    private Vec3 flightTarget;

    public BudgerigarEntity(EntityType<? extends BudgerigarEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setPathfindingMalus(BlockPathTypes.LEAVES, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 16.0F);
        this.musicScanCooldown = 10 + this.getRandom().nextInt(20);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BudgerigarDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, BudgerigarDefinition.WALK_SPEED)
                .add(Attributes.FLYING_SPEED, BudgerigarDefinition.FLYING_SPEED)
                .add(Attributes.FOLLOW_RANGE, BudgerigarDefinition.FOLLOW_RANGE);
    }

    public static boolean canSpawn(EntityType<BudgerigarEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean validGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(Blocks.SAND)
                || below.is(Blocks.RED_SAND)
                || below.is(Blocks.HAY_BLOCK);
        return validGround && level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    public BudgerigarEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
        BudgerigarEntity child = GuaniaoEntityTypes.BUDGERIGAR.get().create(level);
        if (child != null) {
            child.setSkinVariant(this.getRandom().nextInt(BudgerigarDefinition.TEXTURE_VARIANTS.length));
            float mateScale = mate instanceof BudgerigarEntity other ? other.getIndividualModelScale() : this.getIndividualModelScale();
            child.setIndividualModelScale(BirdModelScale.inheritIndividualScale(child.getRandom(), this.getIndividualModelScale(), mateScale, child.modelScaleProfile()));
        }
        return child;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, (Goal)new FloatGoal((Mob)this));
        this.goalSelector.addGoal(1, (Goal)new BudgerigarFrightGoal(this));
        this.goalSelector.addGoal(2, new BirdStayGoal<>(this));
        this.goalSelector.addGoal(2, new BirdRoostGoal<>(this));
        this.goalSelector.addGoal(2, (Goal)new BudgerigarMusicDanceGoal(this));
        this.goalSelector.addGoal(3, (Goal)new BudgerigarEatFoodGoal(this));
        this.goalSelector.addGoal(4, (Goal)new BirdBathUseGoal(this, 1.0D, 11.0D, 36,
                BirdBathAttraction::isAttractiveToBudgerigar,
                this::canStartFoodGoal,
                bath -> this.setBehaviorState(BudgerigarBehaviorState.FORAGING),
                this::consumeBirdBathServing,
                (bath, consumed) -> {
                    if (!this.isEating() && this.getBehaviorState() == BudgerigarBehaviorState.FORAGING) {
                        this.setBehaviorState(BudgerigarBehaviorState.IDLE);
                    }
                }));
        this.goalSelector.addGoal(5, (Goal)new BudgerigarSentinelGoal(this));
        this.goalSelector.addGoal(6, (Goal)new BudgerigarRoostGoal(this));
        this.goalSelector.addGoal(7, (Goal)new BudgerigarFollowOwnerGoal(this, 1.0D, 2.5F, 8.5F));
        this.goalSelector.addGoal(8, (Goal)new BudgerigarFlockGoal(this));
        this.goalSelector.addGoal(9, (Goal)new BudgerigarCuriousFollowGoal(this));
        this.goalSelector.addGoal(10, (Goal)new BudgerigarIdleGoal(this));
        this.goalSelector.addGoal(11, (Goal)new RandomLookAroundGoal((Mob)this));
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
        this.entityData.define(BEHAVIOR_STATE, BudgerigarBehaviorState.IDLE.ordinal());
        this.entityData.define(SKIN_VARIANT, 0);
        this.entityData.define(MODEL_SCALE, BirdModelScale.DEFAULT_INDIVIDUAL_SCALE);
        this.entityData.define(COMMAND_MODE, BirdCommandMode.FREE.ordinal());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (BEHAVIOR_STATE.equals(key)) {
            this.behaviorState = BudgerigarEntity.decodeBehaviorState((Integer)this.entityData.get(BEHAVIOR_STATE));
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag compoundTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, compoundTag);
        this.setSkinVariant(this.getRandom().nextInt(BudgerigarDefinition.TEXTURE_VARIANTS.length));
        if (compoundTag == null || !compoundTag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.randomizeModelScale();
        }
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.isPassenger()) {
            this.getNavigation().stop();
            if (this.level().isClientSide) {
                this.tickClientAnimationCounters();
            } else {
                this.tickCounters();
                this.setBehaviorState(BudgerigarBehaviorState.PERCHING);
            }
            return;
        }
        if (this.level().isClientSide) {
            this.tickClientAnimationCounters();
            return;
        }
        this.birdBrain.tick();
        this.tickCounters();
        this.tickMusicAwareness();
        this.tickEating();
        this.tickPostTameAction();
        this.tickWaterEscape();
        this.tickPendingFright();
        this.tickFlight();
        this.tickAmbientAirCruise();
        this.tickBehaviorFallback();
        this.tickGroundMovementFacing();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult commandResult = BirdCommandInteraction.tryHandle(this, this, player, hand);
        if (commandResult.consumesAction()) {
            return commandResult;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (BudgerigarEntity.isCocoa(stack)) {
            if (!this.level().isClientSide) {
                this.setBehaviorStateFor(BudgerigarBehaviorState.CURIOUS, 30);
                this.playInteractionSound();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.isEdibleFoodForThisBird(stack)) {
            if (this.level().isClientSide) {
                return InteractionResult.sidedSuccess(true);
            }
            if (this.isEating()) {
                this.playInteractionSound();
                return InteractionResult.SUCCESS;
            }
            ItemStack eaten = stack.copy();
            eaten.setCount(1);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            boolean wasTame = this.isTame();
            this.interestedPlayerUUID = player.getUUID();
            this.playInteractionSound();
            this.startEatingFood(eaten, true);
            this.addTrust(420);
            this.curiousTicks = Math.max(this.curiousTicks, 260);
            this.shareTrustNearby(120);
            this.updateTrustedOwner(player);
            if (!wasTame && this.isTame()) {
                this.startTameCelebration(player);
                this.awardChirpyPartnerAdvancement(player);
                BirdAdvancements.awardTamedBird(player, this);
                this.level().broadcastEntityEvent(this, TAMING_SUCCEEDED_EVENT);
            } else if (!wasTame) {
                this.level().broadcastEntityEvent(this, TAMING_FAILED_EVENT);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == TAMING_SUCCEEDED_EVENT) {
            this.spawnTamingParticles(true);
            return;
        }
        if (id == TAMING_FAILED_EVENT) {
            this.spawnTamingParticles(false);
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 200);
            this.clearEating();
            Entity attacker = source.getEntity();
            Vec3 sourcePos = attacker == null ? this.position() : attacker.position();
            if (this.isSleepingOrRoosting()) {
                this.wakeFromLoudSound(sourcePos);
            }
            if (BirdConfigManager.aprilFoolsMode() && attacker instanceof Player) {
                return true;
            }
            this.interestedPlayerUUID = null;
            this.nearbyMusicTicks = 0;
            this.birdBrain.onFrightened(attacker instanceof Player ? 0.08F : 0.18F);
            this.getNavigation().stop();
            this.setBehaviorStateFor(BudgerigarBehaviorState.ALERT, attacker instanceof Player ? 35 : 55);
            if (!(attacker instanceof Player)) {
                this.queueFrightFrom(sourcePos, 45, 18 + this.getRandom().nextInt(16));
            }
        }
        return hurt;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        this.birdBrain.save(compoundTag);
        compoundTag.putInt("BudgerigarTrustTicks", this.trustTicks);
        compoundTag.putInt("BudgerigarCuriousTicks", this.curiousTicks);
        compoundTag.putInt("BudgerigarSkinVariant", this.getSkinVariant());
        BirdModelScale.save(compoundTag, this.getIndividualModelScale(), this.modelScaleProfile());
        compoundTag.putInt(CommandableBird.COMMAND_MODE_NBT_KEY, this.getBirdCommandMode().ordinal());
        if (this.interestedPlayerUUID != null) {
            compoundTag.putUUID("BudgerigarInterestedPlayer", this.interestedPlayerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.birdBrain.load(compoundTag);
        this.trustTicks = compoundTag.getInt("BudgerigarTrustTicks");
        this.curiousTicks = compoundTag.getInt("BudgerigarCuriousTicks");
        if (compoundTag.contains("BudgerigarSkinVariant", 3)) {
            this.setSkinVariant(compoundTag.getInt("BudgerigarSkinVariant"));
        }
        if (compoundTag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.setIndividualModelScale(BirdModelScale.load(compoundTag, this.modelScaleProfile()));
        } else {
            this.randomizeModelScale();
        }
        if (compoundTag.hasUUID("BudgerigarInterestedPlayer")) {
            this.interestedPlayerUUID = compoundTag.getUUID("BudgerigarInterestedPlayer");
        }
        if (compoundTag.contains(CommandableBird.COMMAND_MODE_NBT_KEY, 3)) {
            this.setBirdCommandMode(BirdCommandMode.byId(compoundTag.getInt(CommandableBird.COMMAND_MODE_NBT_KEY)));
        } else {
            this.setBirdCommandMode(this.isTame() ? BirdCommandMode.FOLLOW : BirdCommandMode.FREE);
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
        return this.flightTicks > 0
                || this.landingFlight
                || this.getBehaviorState().isAirborne()
                || this.isNoGravity() && !this.onGround();
    }

    @Override
    public boolean isBirdLanding() {
        return this.landingFlight;
    }

    @Override
    public boolean isBirdEscaping() {
        return this.escapeFlightActive;
    }

    public BudgerigarBehaviorState getBehaviorState() {
        if (this.entityData != null) {
            return BudgerigarEntity.decodeBehaviorState((Integer)this.entityData.get(BEHAVIOR_STATE));
        }
        return this.behaviorState;
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
        return this.getBehaviorState() == BudgerigarBehaviorState.FLEEING;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return this.isEdibleFoodForThisBird(stack);
    }

    void setBehaviorState(BudgerigarBehaviorState state) {
        if (state == null) {
            state = BudgerigarBehaviorState.IDLE;
        }
        this.behaviorState = state;
        if (this.entityData != null) {
            this.entityData.set(BEHAVIOR_STATE, state.ordinal());
        }
    }

    void setBehaviorStateFor(BudgerigarBehaviorState state, int ticks) {
        this.setBehaviorState(state);
        this.behaviorStateLockTicks = Math.max(this.behaviorStateLockTicks, ticks);
    }

    public ResourceLocation getTextureResource() {
        return BudgerigarDefinition.textureForVariant(this.getSkinVariant());
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.BUDGERIGAR;
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

    public int getSkinVariant() {
        if (this.entityData == null) {
            return 0;
        }
        return Mth.clamp((Integer)this.entityData.get(SKIN_VARIANT), 0, BudgerigarDefinition.TEXTURE_VARIANTS.length - 1);
    }

    public void setGuidePreviewAnimation(GuidePreviewAnimation guidePreviewAnimation) {
        this.guidePreviewAnimation = guidePreviewAnimation == null ? GuidePreviewAnimation.NONE : guidePreviewAnimation;
    }

    boolean canStartFoodGoal() {
        return this.foodCooldown <= 0
                && (!this.isTame() || this.isBirdCommandMode(BirdCommandMode.FREE))
                && !this.isEating()
                && !this.isFlying()
                && !this.isDancing()
                && !this.isSleepingOrRoosting()
                && !this.getBehaviorState().isEscape();
    }

    boolean canStartSocialGoal() {
        return this.isActiveTime()
                && (!this.isTame() || this.isBirdCommandMode(BirdCommandMode.FREE))
                && !this.isEating()
                && !this.isDancing()
                && !this.isSleepingOrRoosting()
                && !this.getBehaviorState().isEscape();
    }

    boolean isActiveTime() {
        return BirdActivitySchedule.DIURNAL.isActiveTime(this.level().getDayTime());
    }

    boolean isRoostTime() {
        return BirdActivitySchedule.DIURNAL.isRestTime(this.level().getDayTime());
    }

    boolean isRestInterrupted() {
        return this.restInterruptionTicks > 0;
    }

    public boolean isBusyWithMusicOrSleep() {
        return this.isDancing() || this.isSleepingOrRoosting();
    }

    boolean isDancing() {
        return this.nearbyMusicTicks > 0 || this.getBehaviorState() == BudgerigarBehaviorState.DANCING;
    }

    boolean isEating() {
        return this.eatingTicks > 0 || this.getBehaviorState() == BudgerigarBehaviorState.EATING;
    }

    @Override
    public boolean isFlying() {
        return this.shouldPlayFlyAnimation();
    }

    boolean isFlightInProgress() {
        return this.flightTicks > 0 || this.landingFlight;
    }

    boolean isSleepingOrRoosting() {
        BudgerigarBehaviorState state = this.getBehaviorState();
        return state == BudgerigarBehaviorState.SLEEPING || state == BudgerigarBehaviorState.ROOSTING;
    }

    @Override
    public boolean isBirdSleeping() {
        return this.isSleepingOrRoosting();
    }

    @Override
    public void wakeFromLoudSound(Vec3 soundPosition) {
        if (this.level().isClientSide || !this.isSleepingOrRoosting()) {
            return;
        }
        this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 240);
        this.behaviorStateLockTicks = 0;
        this.getNavigation().stop();
        this.setBehaviorStateFor(BudgerigarBehaviorState.ALERT, 45 + this.getRandom().nextInt(25));
        if (soundPosition != null) {
            this.getLookControl().setLookAt(soundPosition.x, soundPosition.y, soundPosition.z, 35.0F, 35.0F);
        }
    }

    public void startFlybyFlight(Vec3 target) {
        Vec3 selectedTarget = target == null ? this.findAirCruiseTarget(false) : this.clampFlightTarget(target);
        if (selectedTarget != null && !BirdFlightTargeting.hasClearFlightPath(this, selectedTarget)) {
            selectedTarget = BirdFlightTargeting.findRecoveryTarget(this, selectedTarget.subtract(this.position()), 5, 8);
        }
        if (selectedTarget == null) {
            return;
        }
        this.escapeFlightActive = false;
        this.landingFlight = false;
        this.landingRetryCount = 0;
        this.flightTarget = selectedTarget;
        this.flightTicks = 150 + this.getRandom().nextInt(91);
        this.timeFlying = 0;
        this.hoverRetargetTicks = 52 + this.getRandom().nextInt(46);
        this.flightCooldown = Math.max(this.flightCooldown, 120);
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.setOnGround(false);
        this.setBehaviorStateFor(BudgerigarBehaviorState.FLYING, 120);
        Vec3 direction = this.flightTarget == null
                ? this.randomHorizontalDirection()
                : this.flightTarget.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() <= 1.0E-4D) {
            direction = this.randomHorizontalDirection();
        }
        Vec3 movement = direction.normalize().scale(0.24D).add(0.0D, 0.07D, 0.0D);
        this.setDeltaMovement(movement);
        this.faceFlightDirection(movement);
        this.fallDistance = 0.0F;
        this.hasImpulse = true;
    }

    @Override
    public boolean startBirdBathMountFlight(Vec3 standPosition) {
        if (standPosition == null || this.isFlightInProgress()) {
            return false;
        }
        Vec3 horizontal = standPosition.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() <= 1.0E-4D) {
            horizontal = Vec3.ZERO;
        } else {
            horizontal = horizontal.normalize().scale(0.27D);
        }
        Vec3 movement = new Vec3(horizontal.x, 0.64D, horizontal.z);
        this.getNavigation().stop();
        this.setNoGravity(false);
        this.setOnGround(false);
        this.setBehaviorStateFor(BudgerigarBehaviorState.FLYING, 32);
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
            this.eatingTicks = Math.max(this.eatingTicks, Math.max(30, ticks));
            this.setBehaviorStateFor(BudgerigarBehaviorState.EATING, this.eatingTicks);
            return;
        }
        this.setBehaviorStateFor(BudgerigarBehaviorState.CURIOUS, Math.max(24, ticks / 2));
    }

    int trustTicks() {
        return this.trustTicks;
    }

    int nearbyMusicTicks() {
        return this.nearbyMusicTicks;
    }

    BlockPos musicSourcePos() {
        return this.musicSourcePos;
    }

    boolean shouldFlee() {
        return this.externalFrightTicks > 0 && this.frightSource != null;
    }

    Vec3 frightSource() {
        return this.frightSource;
    }

    void addTrust(int amount) {
        this.trustTicks = Mth.clamp(this.trustTicks + amount, 0, 6000);
    }

    public void setSkinVariantForRendering(int variant) {
        this.setSkinVariant(variant);
    }

    void shareTrustNearby(int amount) {
        for (BudgerigarEntity budgerigar : BirdFlockManager.nearby(this, BudgerigarEntity.class, 10.0D)) {
            if (budgerigar != this) {
                budgerigar.addTrust(amount);
                budgerigar.curiousTicks = Math.max(budgerigar.curiousTicks, 80);
            }
        }
    }

    void startEatingFood(ItemStack foodStack, boolean trustedFood) {
        this.getNavigation().stop();
        this.eatingTicks = 35 + this.getRandom().nextInt(21);
        this.foodCooldown = 90 + this.getRandom().nextInt(60);
        this.setBehaviorStateFor(BudgerigarBehaviorState.EATING, this.eatingTicks);
        this.birdBrain.onEat(trustedFood ? 0.35F : 0.28F);
        this.playSound(SoundEvents.PARROT_EAT, 0.45F, 1.35F + this.getRandom().nextFloat() * 0.2F);
    }

    private void consumeBirdBathServing(EdDYON.guaniao.content.bath.BirdBathBlockEntity bath, BirdBathContentType contentType) {
        if (contentType == BirdBathContentType.BREAD) {
            this.startEatingFood(new ItemStack(Items.BREAD), true);
            return;
        }
        this.getNavigation().stop();
        this.eatingTicks = 24 + this.getRandom().nextInt(13);
        this.foodCooldown = 70 + this.getRandom().nextInt(45);
        this.setBehaviorStateFor(BudgerigarBehaviorState.EATING, this.eatingTicks);
        this.birdBrain.onEat(0.16F);
        this.playSound(SoundEvents.GENERIC_DRINK, 0.32F, 1.25F + this.getRandom().nextFloat() * 0.18F);
    }

    void consumeItemEntity(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        ItemStack eaten = stack.copy();
        eaten.setCount(1);
        stack.shrink(1);
        if (stack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(stack);
        }
        this.startEatingFood(eaten, true);
        this.addTrust(260);
        this.curiousTicks = Math.max(this.curiousTicks, 180);
        this.shareTrustNearby(80);
    }

    void frightenFrom(Vec3 sourcePos, int ticks) {
        this.frightSource = sourcePos;
        this.externalFrightTicks = Math.max(this.externalFrightTicks, ticks);
        this.pendingFrightTicks = 0;
        this.pendingFrightDuration = 0;
        this.pendingFrightSource = null;
        this.setBehaviorStateFor(BudgerigarBehaviorState.FLEEING, Math.min(90, ticks));
        if (this.flightCooldown <= 0 && this.onGround()) {
            this.startEscapeFlight(sourcePos);
        }
    }

    void alertNearbyBudgerigars(Vec3 sourcePos, int ticks) {
        for (BudgerigarEntity budgerigar : BirdFlockManager.nearby(this, BudgerigarEntity.class, BudgerigarDefinition.SOCIAL_RADIUS)) {
            if (budgerigar != this) {
                budgerigar.birdBrain.onFrightened(0.06F);
                budgerigar.setBehaviorStateFor(BudgerigarBehaviorState.ALERT, 24 + budgerigar.getRandom().nextInt(24));
                budgerigar.curiousTicks = Math.max(budgerigar.curiousTicks, 40);
            }
        }
    }

    void queueFrightFrom(Vec3 sourcePos, int ticks, int delayTicks) {
        if (this.isEating()) {
            this.clearEating();
        }
        this.pendingFrightSource = sourcePos;
        this.pendingFrightDuration = Math.max(this.pendingFrightDuration, ticks);
        if (this.pendingFrightTicks <= 0) {
            this.pendingFrightTicks = Math.max(1, delayTicks);
        } else {
            this.pendingFrightTicks = Math.min(this.pendingFrightTicks, Math.max(1, delayTicks));
        }
        this.setBehaviorStateFor(BudgerigarBehaviorState.ALERT, Math.min(32, this.pendingFrightTicks + 10));
    }

    void triggerMusic(BlockPos sourcePos, int ticks) {
        this.musicSourcePos = sourcePos;
        this.nearbyMusicTicks = Math.max(this.nearbyMusicTicks, ticks);
        if (!this.isEating() && !this.getBehaviorState().isEscape()) {
            this.setBehaviorStateFor(BudgerigarBehaviorState.DANCING, Math.min(ticks, 80));
        }
    }

    void startShortFlight(Vec3 target, boolean fleeing) {
        if (this.flightCooldown > 0 || this.flightTicks > 0 || this.landingFlight) {
            return;
        }
        Vec3 selectedTarget = target == null ? this.findAirCruiseTarget(fleeing) : this.clampFlightTarget(target);
        if (selectedTarget != null && !BirdFlightTargeting.hasClearFlightPath(this, selectedTarget)) {
            selectedTarget = BirdFlightTargeting.findRecoveryTarget(this, selectedTarget.subtract(this.position()), 5, 8);
        }
        if (selectedTarget == null) {
            return;
        }
        this.escapeFlightActive = fleeing;
        this.landingFlight = false;
        this.landingRetryCount = 0;
        this.flightTarget = selectedTarget;
        this.flightTicks = fleeing
                ? ESCAPE_AIR_CRUISE_MIN_TICKS + this.getRandom().nextInt(ESCAPE_AIR_CRUISE_RANDOM_TICKS)
                : AMBIENT_AIR_CRUISE_MIN_TICKS + this.getRandom().nextInt(AMBIENT_AIR_CRUISE_RANDOM_TICKS);
        this.timeFlying = 0;
        this.hoverRetargetTicks = this.nextHoverRetargetDelay();
        this.setNoGravity(true);
        this.setOnGround(false);
        this.getNavigation().stop();
        this.setBehaviorStateFor(fleeing ? BudgerigarBehaviorState.FLEEING : BudgerigarBehaviorState.FLYING, fleeing ? 100 : 90);
    }

    public static boolean isEdibleFood(ItemStack stack) {
        return BirdFoodSafety.matchesClean(BirdTags.BUDGERIGAR_FOODS, stack);
    }

    public boolean isEdibleFoodForThisBird(ItemStack stack) {
        return BirdFoodSafety.matchesClean(this.foodTag(), stack);
    }

    protected TagKey<Item> foodTag() {
        return BirdTags.BUDGERIGAR_FOODS;
    }

    static boolean isCocoa(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.COCOA_BEANS);
    }

    private void setSkinVariant(int variant) {
        int clamped = Mth.clamp(variant, 0, BudgerigarDefinition.TEXTURE_VARIANTS.length - 1);
        if (this.entityData != null) {
            this.entityData.set(SKIN_VARIANT, clamped);
        }
    }

    private void randomizeModelScale() {
        this.setIndividualModelScale(BirdModelScale.randomIndividualScale(this.getRandom(), this.modelScaleProfile()));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.BUDGERIGAR_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return GuaniaoSoundEvents.BUDGERIGAR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuaniaoSoundEvents.BUDGERIGAR_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return BirdFlockSoundLimiter.scaledAmbientInterval(this, 180);
    }

    @Override
    public void playAmbientSound() {
        if (BirdFlockSoundLimiter.allowAmbient(this)) {
            super.playAmbientSound();
        }
    }

    @Override
    public float getSoundVolume() {
        return 0.45F;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, BirdSoundVolume.apply(this, volume), pitch);
    }

    protected SoundEvent getInteractionSound() {
        return GuaniaoSoundEvents.BUDGERIGAR_INTERACT.get();
    }

    protected void playInteractionSound() {
        this.playSound(this.getInteractionSound(), 0.42F, 0.95F + this.getRandom().nextFloat() * 0.18F);
    }

    private static BudgerigarBehaviorState decodeBehaviorState(int ordinal) {
        BudgerigarBehaviorState[] values = BudgerigarBehaviorState.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return BudgerigarBehaviorState.IDLE;
        }
        return values[ordinal];
    }

    private void tickCounters() {
        if (this.behaviorStateLockTicks > 0) {
            --this.behaviorStateLockTicks;
        }
        if (this.foodCooldown > 0) {
            --this.foodCooldown;
        }
        if (this.flightCooldown > 0) {
            --this.flightCooldown;
        }
        if (this.externalFrightTicks > 0) {
            --this.externalFrightTicks;
        }
        if (this.restInterruptionTicks > 0) {
            --this.restInterruptionTicks;
        }
        if (this.curiousTicks > 0) {
            --this.curiousTicks;
        }
        if (this.trustTicks > 0 && this.tickCount % 40 == 0) {
            --this.trustTicks;
        }
        if (this.idleAnimationTicks > 0) {
            --this.idleAnimationTicks;
        }
        if (this.nearbyMusicTicks > 0) {
            --this.nearbyMusicTicks;
        }
        if (this.postTameActionSwapTicks > 0) {
            --this.postTameActionSwapTicks;
        }
    }

    private void tickClientAnimationCounters() {
        if (this.behaviorStateLockTicks > 0) {
            --this.behaviorStateLockTicks;
        }
        if (this.eatingTicks > 0) {
            --this.eatingTicks;
        }
        if (this.idleAnimationTicks > 0) {
            --this.idleAnimationTicks;
        }
        if (this.postTameActionTicks > 0) {
            --this.postTameActionTicks;
        }
        if (this.postTameActionSwapTicks > 0) {
            --this.postTameActionSwapTicks;
        }
    }

    private void tickMusicAwareness() {
        if (this.musicScanCooldown-- > 0) {
            return;
        }
        this.musicScanCooldown = 18 + this.getRandom().nextInt(14);
        BlockPos sourcePos = this.findNearbyJukebox();
        if (sourcePos == null) {
            return;
        }
        this.triggerMusic(sourcePos, 85 + this.getRandom().nextInt(35));
        for (BudgerigarEntity budgerigar : BirdFlockManager.nearby(this, BudgerigarEntity.class, MUSIC_GROUP_RADIUS)) {
            if (budgerigar != this && budgerigar.getRandom().nextFloat() < 0.80F) {
                budgerigar.triggerMusic(sourcePos, 65 + budgerigar.getRandom().nextInt(35));
            }
        }
    }

    private BlockPos findNearbyJukebox() {
        BlockPos origin = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-MUSIC_SCAN_RADIUS, -3, -MUSIC_SCAN_RADIUS), origin.offset(MUSIC_SCAN_RADIUS, 3, MUSIC_SCAN_RADIUS))) {
            BlockState state = this.level().getBlockState(pos);
            if (state.is(Blocks.JUKEBOX) && state.hasProperty(JukeboxBlock.HAS_RECORD) && (Boolean)state.getValue(JukeboxBlock.HAS_RECORD)) {
                return pos.immutable();
            }
        }
        return null;
    }

    private void tickEating() {
        if (this.eatingTicks <= 0) {
            return;
        }
        this.getNavigation().stop();
        this.setBehaviorState(BudgerigarBehaviorState.EATING);
        if (--this.eatingTicks <= 0) {
            this.clearEating();
        }
    }

    private void clearEating() {
        this.eatingTicks = 0;
        if (this.getBehaviorState() == BudgerigarBehaviorState.EATING) {
            this.setBehaviorState(this.birdBrain.wantsForage() ? BudgerigarBehaviorState.FORAGING : BudgerigarBehaviorState.IDLE);
        }
    }

    private void tickPostTameAction() {
        if (this.postTameActionTicks <= 0) {
            return;
        }
        --this.postTameActionTicks;
        if (this.isFlying()) {
            return;
        }
        if (this.isEating()) {
            this.clearEating();
        }
        if (this.getBehaviorState() == BudgerigarBehaviorState.SLEEPING || this.getBehaviorState() == BudgerigarBehaviorState.ROOSTING) {
            this.behaviorStateLockTicks = 0;
            this.setBehaviorState(BudgerigarBehaviorState.CURIOUS);
        }
        if (this.getOwner() != null && this.tickCount % 8 == 0) {
            this.getLookControl().setLookAt(this.getOwner(), 35.0F, 35.0F);
        }
        if (this.postTameActionSwapTicks <= 0 || this.getBehaviorState() == BudgerigarBehaviorState.IDLE) {
            BudgerigarBehaviorState state = this.getRandom().nextBoolean()
                    ? BudgerigarBehaviorState.CURIOUS
                    : BudgerigarBehaviorState.PREENING;
            this.setBehaviorStateFor(state, 32 + this.getRandom().nextInt(32));
            this.postTameActionSwapTicks = 30 + this.getRandom().nextInt(28);
        }
        if (this.postTameActionTicks <= 0 && (this.getBehaviorState() == BudgerigarBehaviorState.CURIOUS || this.getBehaviorState() == BudgerigarBehaviorState.PREENING)) {
            this.behaviorStateLockTicks = 0;
            this.setBehaviorState(BudgerigarBehaviorState.IDLE);
        }
    }

    private void tickWaterEscape() {
        if (!this.isInWaterOrBubble()) {
            return;
        }
        this.getNavigation().stop();
        this.landingFlight = false;
        this.escapeFlightActive = false;
        this.landingRetryCount = 0;
        this.flightTarget = this.findAirCruiseTarget(false);
        this.flightTicks = Math.max(this.flightTicks, 90 + this.getRandom().nextInt(50));
        this.hoverRetargetTicks = Math.min(Math.max(this.hoverRetargetTicks, 1), 12);
        this.setNoGravity(true);
        this.setBehaviorStateFor(BudgerigarBehaviorState.FLYING, 70);
        Vec3 direction = this.flightTarget == null
                ? this.randomHorizontalDirection()
                : this.flightTarget.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() <= 1.0E-4D) {
            direction = this.randomHorizontalDirection();
        }
        Vec3 movement = direction.normalize().scale(0.22D).add(0.0D, 0.28D, 0.0D);
        this.setDeltaMovement(movement);
        this.faceFlightDirection(movement);
        this.fallDistance = 0.0F;
        this.hasImpulse = true;
    }

    private void tickPendingFright() {
        if (this.pendingFrightTicks <= 0) {
            return;
        }
        --this.pendingFrightTicks;
        this.getNavigation().stop();
        if (this.pendingFrightTicks > 0) {
            if (this.pendingFrightSource != null) {
                this.getLookControl().setLookAt(this.pendingFrightSource.x, this.pendingFrightSource.y + 0.6D, this.pendingFrightSource.z, 35.0F, 35.0F);
            }
            return;
        }
        Vec3 sourcePos = this.pendingFrightSource == null ? this.position() : this.pendingFrightSource;
        int duration = Math.max(60, this.pendingFrightDuration);
        this.pendingFrightSource = null;
        this.pendingFrightDuration = 0;
        this.frightenFrom(sourcePos, duration);
    }

    private void tickFlight() {
        if (this.isInWaterOrBubble()) {
            return;
        }
        if (this.flightTicks <= 0 && !this.landingFlight) {
            this.timeFlying = 0;
            BirdFlightController.clearFlightProgress(this);
            this.setNoGravity(false);
            return;
        }
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.fallDistance = 0.0F;
        ++this.timeFlying;
        if (this.timeFlying > MAX_CONTROLLED_FLIGHT_TICKS) {
            this.finishFlight();
            return;
        }
        this.setBehaviorState(this.escapeFlightActive ? BudgerigarBehaviorState.FLEEING : BudgerigarBehaviorState.FLYING);
        if (this.flightTicks > 0) {
            --this.flightTicks;
        }
        if (this.flightTicks <= 0 && !this.landingFlight) {
            this.beginLandingFlight();
        }
        if (this.flightTarget == null) {
            if (this.landingFlight) {
                this.flightTarget = this.findLandingTarget();
                if (this.flightTarget == null) {
                    this.extendCruiseAfterUnsafeLanding();
                    return;
                }
            } else {
                this.retargetAirCruise(this.escapeFlightActive);
                if (this.flightTarget == null) {
                    this.finishFlight();
                    return;
                }
            }
        }
        Vec3 toTarget = this.flightTarget.subtract(this.position());
        double horizontalDistance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        if (this.landingFlight) {
            if (this.onGround()) {
                this.finishFlight();
                return;
            }
            if (this.flightTicks <= 0 && toTarget.lengthSqr() < 0.35D) {
                this.extendCruiseAfterUnsafeLanding();
                return;
            }
        } else if (toTarget.lengthSqr() < 1.85D || --this.hoverRetargetTicks <= 0) {
            this.retargetAirCruise(this.escapeFlightActive);
            if (this.flightTarget == null) {
                this.finishFlight();
                return;
            }
            toTarget = this.flightTarget.subtract(this.position());
            horizontalDistance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        }
        if (!this.landingFlight && BirdFlightController.isFlightProgressStalled(this, this.flightTarget, 12, 12)) {
            Vec3 recovery = BirdFlightTargeting.findRecoveryTarget(this, toTarget, 5, 8);
            this.flightTarget = recovery == null ? this.findAirCruiseTarget(this.escapeFlightActive) : this.clampFlightTarget(recovery);
            if (this.flightTarget == null) {
                this.finishFlight();
                return;
            }
            this.hoverRetargetTicks = this.nextHoverRetargetDelay();
            toTarget = this.flightTarget.subtract(this.position());
            horizontalDistance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        }
        Vec3 direction = toTarget.lengthSqr() > 1.0E-4D ? toTarget.normalize() : this.randomHorizontalDirection();
        Vec3 horizontalDirection = BirdFlightTargeting.normalizeHorizontal(new Vec3(direction.x, 0.0D, direction.z), this.getDeltaMovement());
        if (!this.landingFlight) {
            Vec3 flockHeading = BirdFlightBoids.sameTypeHeading(this, 13.0D, 2.4D, 0.035D, 0.45D, 0.10D, this.escapeFlightActive ? 0.18D : 0.08D);
            if (flockHeading.lengthSqr() > 1.0E-4D) {
                horizontalDirection = BirdFlightTargeting.normalizeHorizontal(horizontalDirection.add(flockHeading), horizontalDirection);
            }
        }
        double speed = this.escapeFlightActive ? 0.34D : (this.landingFlight ? 0.20D : 0.26D);
        if (this.landingFlight) {
            speed = BirdFlightController.decelerateNearLanding(speed, horizontalDistance, 3.4D, 0.42D);
        }
        double hoverBob = this.landingFlight ? -0.035D : Math.sin((this.tickCount + this.getId()) * 0.28D) * 0.025D;
        double vertical = this.landingFlight
                ? Mth.clamp(toTarget.y * 0.11D - 0.035D, -0.13D, 0.055D)
                : Mth.clamp(toTarget.y * 0.12D + hoverBob, -0.075D, 0.16D);
        Vec3 desired = new Vec3(horizontalDirection.x * speed, vertical, horizontalDirection.z * speed);
        Vec3 movement = this.getDeltaMovement().scale(0.32D).add(desired.scale(0.68D));
        if (!this.landingFlight && BirdFlightController.isStalledInAir(this, this.timeFlying, 0.006D)) {
            this.retargetAirCruise(this.escapeFlightActive);
            movement = horizontalDirection.scale(Math.max(speed, 0.18D)).add(0.0D, 0.08D, 0.0D);
        }
        this.setDeltaMovement(movement);
        this.faceFlightDirection(movement);
        this.hasImpulse = true;
    }

    private void finishFlight() {
        boolean wasEscaping = this.escapeFlightActive;
        this.flightTicks = 0;
        this.timeFlying = 0;
        BirdFlightController.clearFlightProgress(this);
        this.flightTarget = null;
        this.hoverRetargetTicks = 0;
        this.escapeFlightActive = false;
        this.landingFlight = false;
        this.landingRetryCount = 0;
        this.setNoGravity(false);
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.x * 0.35D, Math.min(movement.y, -0.04D), movement.z * 0.35D);
        this.flightCooldown = wasEscaping ? 120 + this.getRandom().nextInt(120) : (this.isTame() ? 140 + this.getRandom().nextInt(160) : 160 + this.getRandom().nextInt(180));
        if (this.getBehaviorState().isAirborne()) {
            this.setBehaviorStateFor(BudgerigarBehaviorState.ALERT, 28);
        }
    }

    private void startEscapeFlight(Vec3 sourcePos) {
        Vec3 away = this.position().subtract(sourcePos);
        if (away.horizontalDistanceSqr() < 0.01D) {
            away = new Vec3(this.getRandom().nextDouble() - 0.5D, 0.0D, this.getRandom().nextDouble() - 0.5D);
        }
        Vec3 direction = new Vec3(away.x, 0.0D, away.z).normalize();
        Vec3 target = BirdFlightTargeting.findAirTarget(this, FLIGHT_PROFILE, direction, true);
        if (target == null) {
            target = BirdFlightTargeting.findRecoveryTarget(this, direction, 5, 8);
        }
        this.startShortFlight(target, true);
    }

    private void tickAmbientAirCruise() {
        if (!this.canStartAmbientAirCruise()) {
            return;
        }
        int chance = this.isTame() ? 170 : 150;
        if (this.getRandom().nextInt(chance) == 0) {
            this.startShortFlight(this.findAirCruiseTarget(false), false);
        }
    }

    private boolean canStartAmbientAirCruise() {
        BudgerigarBehaviorState state = this.getBehaviorState();
        return this.flightCooldown <= 0
                && (!this.isTame() || this.isBirdCommandMode(BirdCommandMode.FREE))
                && this.onGround()
                && this.isActiveTime()
                && this.getNavigation().isDone()
                && !this.isEating()
                && !this.isDancing()
                && !this.isSleepingOrRoosting()
                && state != BudgerigarBehaviorState.FORAGING
                && state != BudgerigarBehaviorState.PERCHING
                && state != BudgerigarBehaviorState.FOLLOWING
                && state != BudgerigarBehaviorState.SENTINEL
                && !state.isEscape();
    }

    private void beginLandingFlight() {
        Vec3 landingTarget = this.findLandingTarget();
        if (landingTarget == null || !BirdFlightTargeting.hasClearFlightPath(this, landingTarget)) {
            this.extendCruiseAfterUnsafeLanding();
            return;
        }
        this.landingFlight = true;
        this.escapeFlightActive = false;
        this.flightTicks = 55 + this.getRandom().nextInt(45);
        this.flightTarget = landingTarget;
        this.hoverRetargetTicks = 0;
        this.setBehaviorStateFor(BudgerigarBehaviorState.FLYING, 50);
    }

    private void extendCruiseAfterUnsafeLanding() {
        if (++this.landingRetryCount > MAX_LANDING_RETRIES) {
            this.finishFlight();
            return;
        }
        this.landingFlight = false;
        this.escapeFlightActive = false;
        this.flightTicks = 70 + this.getRandom().nextInt(50);
        this.retargetAirCruise(false);
        this.setNoGravity(true);
        this.setBehaviorStateFor(BudgerigarBehaviorState.FLYING, 60);
    }

    private void retargetAirCruise(boolean fleeing) {
        this.flightTarget = this.findAirCruiseTarget(fleeing);
        this.hoverRetargetTicks = this.nextHoverRetargetDelay();
    }

    private int nextHoverRetargetDelay() {
        return 36 + this.getRandom().nextInt(46);
    }

    private Vec3 findAirCruiseTarget(boolean fleeing) {
        Vec3 direction;
        if (fleeing && this.frightSource != null) {
            Vec3 away = this.position().subtract(this.frightSource);
            direction = away.horizontalDistanceSqr() > 0.01D ? new Vec3(away.x, 0.0D, away.z).normalize() : this.randomHorizontalDirection();
        } else {
            direction = this.getRandom().nextInt(3) == 0 ? this.getLookAngle() : this.randomHorizontalDirection();
        }
        Vec3 target = BirdFlightTargeting.findAirTarget(this, FLIGHT_PROFILE, direction, fleeing);
        if (target != null) {
            return this.clampFlightTarget(target);
        }
        return BirdFlightTargeting.findRecoveryTarget(this, direction, 5, 8);
    }

    private Vec3 findLandingTarget() {
        Vec3 sharedLanding = BirdFlightTargeting.findNearestDryLandingTarget(this, 8, 16);
        if (sharedLanding != null) {
            return this.clampFlightTarget(sharedLanding);
        }
        BlockPos origin = this.blockPosition();
        BlockPos landing = this.findDryLandingSurface(origin, 16);
        if (landing != null) {
            return this.clampFlightTarget(Vec3.atBottomCenterOf(landing));
        }
        for (int attempt = 0; attempt < 24; ++attempt) {
            int x = origin.getX() + this.getRandom().nextInt(13) - 6;
            int z = origin.getZ() + this.getRandom().nextInt(13) - 6;
            BlockPos candidate = this.findDryLandingSurface(new BlockPos(x, origin.getY(), z), 16);
            if (candidate != null) {
                return this.clampFlightTarget(Vec3.atBottomCenterOf(candidate));
            }
        }
        return null;
    }

    private BlockPos findDryLandingSurface(BlockPos center, int verticalRange) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int yOffset = verticalRange; yOffset >= -verticalRange; --yOffset) {
            mutable.set(center.getX(), center.getY() + yOffset, center.getZ());
            if (this.isSafeDryLanding(mutable)) {
                return mutable.immutable();
            }
        }
        return null;
    }

    private boolean isSafeDryLanding(BlockPos pos) {
        return BirdFlightTargeting.isSafeDryLanding(this, pos);
    }

    private Vec3 clampFlightTarget(Vec3 target) {
        double y = Mth.clamp(target.y, this.level().getMinBuildHeight() + 1.5D, this.level().getMaxBuildHeight() - 2.0D);
        return new Vec3(target.x, y, target.z);
    }

    private Vec3 randomHorizontalDirection() {
        return BirdFlightTargeting.randomHorizontalDirection(this.getRandom());
    }

    private void updateTrustedOwner(Player player) {
        if (!this.isTame() && this.trustTicks >= 900) {
            this.tame(player);
            this.setBirdCommandMode(BirdCommandMode.FOLLOW);
        } else if (this.isTame() && this.getOwnerUUID() == null) {
            this.setOwnerUUID(player.getUUID());
        }
    }

    private void awardChirpyPartnerAdvancement(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        var advancement = serverPlayer.server.getAdvancements().getAdvancement(CHIRPY_PARTNER_ADVANCEMENT);
        if (advancement != null) {
            serverPlayer.getAdvancements().award(advancement, "tame_budgerigar");
        }
    }

    @Override
    public boolean isOrderedToSit() {
        return false;
    }

    private void startTameCelebration(Player player) {
        this.clearEating();
        this.getNavigation().stop();
        this.postTameActionTicks = 150 + this.getRandom().nextInt(50);
        this.postTameActionSwapTicks = 42;
        this.curiousTicks = Math.max(this.curiousTicks, 220);
        this.idleAnimationTicks = 0;
        this.foodCooldown = Math.max(this.foodCooldown, 45);
        this.behaviorStateLockTicks = 0;
        this.setBehaviorStateFor(BudgerigarBehaviorState.CURIOUS, 55);
        this.getLookControl().setLookAt(player, 35.0F, 35.0F);
    }

    @Override
    protected void spawnTamingParticles(boolean success) {
        for (int i = 0; i < 9; ++i) {
            double xOffset = this.getRandom().nextGaussian() * 0.03D;
            double yOffset = this.getRandom().nextGaussian() * 0.04D;
            double zOffset = this.getRandom().nextGaussian() * 0.03D;
            if (success) {
                this.level().addParticle(
                        ParticleTypes.NOTE,
                        this.getRandomX(0.7D),
                        this.getRandomY() + 0.22D,
                        this.getRandomZ(0.7D),
                        this.getRandom().nextDouble(),
                        yOffset + 0.035D,
                        zOffset);
            } else {
                this.level().addParticle(
                        ParticleTypes.SMOKE,
                        this.getRandomX(0.7D),
                        this.getRandomY(),
                        this.getRandomZ(0.7D),
                        xOffset,
                        yOffset,
                        zOffset);
            }
        }
    }

    private void tickBehaviorFallback() {
        if (this.behaviorStateLockTicks > 0 || this.postTameActionTicks > 0 || this.isEating() || this.isFlying()) {
            return;
        }
        if (this.nearbyMusicTicks > 0) {
            this.setBehaviorState(BudgerigarBehaviorState.DANCING);
            return;
        }
        if (this.isRoostTime() && !this.isRestInterrupted() && this.onGround() && this.getNavigation().isDone()) {
            this.setBehaviorState(BudgerigarBehaviorState.SLEEPING);
            return;
        }
        BudgerigarBehaviorState state = this.getBehaviorState();
        if (state == BudgerigarBehaviorState.FLEEING || state == BudgerigarBehaviorState.FLYING) {
            this.setBehaviorState(BudgerigarBehaviorState.ALERT);
            return;
        }
        if (this.isTame() && this.getOwner() != null && !this.getNavigation().isDone() && this.distanceToSqr(this.getOwner()) > 9.0D) {
            this.setBehaviorState(BudgerigarBehaviorState.FOLLOWING);
            return;
        }
        if (BirdGroundAnimation.hasWalkMotion(this)) {
            this.setBehaviorState(BudgerigarBehaviorState.WALKING);
            return;
        }
        if (state == BudgerigarBehaviorState.WALKING
                || state == BudgerigarBehaviorState.FORAGING
                || state == BudgerigarBehaviorState.FOLLOWING
                || state == BudgerigarBehaviorState.ALERT) {
            this.setBehaviorState(BudgerigarBehaviorState.IDLE);
        }
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
                || this.isFlying()
                || this.isInWaterOrBubble()
                || this.isPassenger()) {
            return false;
        }
        BudgerigarBehaviorState state = this.getBehaviorState();
        if (state.isAirborne()
                || state == BudgerigarBehaviorState.EATING
                || state == BudgerigarBehaviorState.PREENING
                || state == BudgerigarBehaviorState.DANCING
                || state == BudgerigarBehaviorState.SLEEPING
                || state == BudgerigarBehaviorState.ROOSTING) {
            return false;
        }
        return BirdGroundAnimation.hasWalkMotion(this);
    }

    protected final boolean shouldPlayFlyAnimation() {
        return BirdFlightController.shouldPlayFlyAnimation(
                this,
                this.getBehaviorState().isAirborne(),
                this.onGround(),
                this.isNoGravity(),
                this.getDeltaMovement(),
                0);
    }

    private boolean shouldPlayWalkAnimation(BudgerigarBehaviorState state, boolean animationMoving) {
        if (!BirdGroundAnimation.canPlayWalk(this)) {
            return false;
        }
        if (animationMoving) {
            return true;
        }
        if (state == BudgerigarBehaviorState.EATING
                || state == BudgerigarBehaviorState.PREENING
                || state == BudgerigarBehaviorState.DANCING
                || state == BudgerigarBehaviorState.SLEEPING
                || state == BudgerigarBehaviorState.ROOSTING
                || state.isAirborne()) {
            return false;
        }
        return BirdGroundAnimation.hasWalkMotion(this, animationMoving)
                || state == BudgerigarBehaviorState.WALKING
                || state == BudgerigarBehaviorState.FOLLOWING
                || state == BudgerigarBehaviorState.FORAGING;
    }

    private RawAnimation pickIdleAnimation() {
        if (this.idleAnimationTicks <= 0) {
            int roll = this.getRandom().nextInt(this.trustTicks > 800 || this.curiousTicks > 0 ? 5 : 9);
            if (roll == 0) {
                this.currentIdleAnimation = PREEN_ANIMATION;
                this.idleAnimationTicks = 45 + this.getRandom().nextInt(45);
            } else if (roll <= 2 && (this.trustTicks > 400 || this.curiousTicks > 0)) {
                this.currentIdleAnimation = CURIOUS_ANIMATION;
                this.idleAnimationTicks = 35 + this.getRandom().nextInt(35);
            } else {
                this.currentIdleAnimation = IDLE_ANIMATION;
                this.idleAnimationTicks = 55 + this.getRandom().nextInt(70);
            }
        }
        return this.currentIdleAnimation;
    }

    private <T extends BudgerigarEntity> PlayState movementController(AnimationState<T> animationState) {
        RawAnimation guidePreviewRawAnimation = this.guidePreviewAnimation.animation();
        if (guidePreviewRawAnimation != null) {
            return animationState.setAndContinue(guidePreviewRawAnimation);
        }
        BudgerigarBehaviorState state = this.getBehaviorState();
        if (state == BudgerigarBehaviorState.DANCING || this.nearbyMusicTicks > 0) {
            return animationState.setAndContinue(DANCE_ANIMATION);
        }
        if (state == BudgerigarBehaviorState.EATING || this.eatingTicks > 0) {
            return animationState.setAndContinue(EAT_ANIMATION);
        }
        if (state == BudgerigarBehaviorState.SLEEPING) {
            return animationState.setAndContinue(this.behaviorStateLockTicks > 0 ? SLEEP_ANIMATION : SLEEP_LOOP_ANIMATION);
        }
        if (this.shouldPlayFlyAnimation()) {
            return animationState.setAndContinue(FLY_ANIMATION);
        }
        if (this.shouldPlayWalkAnimation(state, animationState.isMoving())) {
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        if (state == BudgerigarBehaviorState.PREENING) {
            return animationState.setAndContinue(PREEN_ANIMATION);
        }
        if (state == BudgerigarBehaviorState.CURIOUS || state == BudgerigarBehaviorState.ALERT || this.curiousTicks > 0) {
            return animationState.setAndContinue(CURIOUS_ANIMATION);
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

    private static class BudgerigarFollowOwnerGoal extends Goal {
        private final BudgerigarEntity budgerigar;
        private final double speed;
        private final float stopDistance;
        private final float startDistance;
        private LivingEntity owner;
        private int repathTicks;

        BudgerigarFollowOwnerGoal(BudgerigarEntity budgerigar, double speed, float stopDistance, float startDistance) {
            this.budgerigar = budgerigar;
            this.speed = speed;
            this.stopDistance = stopDistance;
            this.startDistance = startDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.budgerigar.isTame()
                    || !this.budgerigar.isBirdCommandMode(BirdCommandMode.FOLLOW)
                    || this.budgerigar.isEating() || this.budgerigar.isDancing()) {
                return false;
            }
            this.owner = this.budgerigar.getOwner();
            return this.owner != null
                    && this.owner.isAlive()
                    && this.budgerigar.isBirdCommandMode(BirdCommandMode.FOLLOW)
                    && this.budgerigar.distanceToSqr(this.owner) > (double)(this.startDistance * this.startDistance);
        }

        @Override
        public boolean canContinueToUse() {
            return this.owner != null
                    && this.owner.isAlive()
                    && !this.budgerigar.isEating()
                    && !this.budgerigar.isDancing()
                    && (this.budgerigar.isFlightInProgress()
                    || this.budgerigar.distanceToSqr(this.owner) > (double)(this.stopDistance * this.stopDistance));
        }

        @Override
        public void start() {
            this.repathTicks = 0;
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.FOLLOWING);
        }

        @Override
        public void tick() {
            this.budgerigar.getLookControl().setLookAt(this.owner, 20.0F, this.budgerigar.getMaxHeadXRot());
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.FOLLOWING);
            double distanceSqr = this.budgerigar.distanceToSqr(this.owner);
            if (this.budgerigar.isFlightInProgress()) {
                return;
            }
            if (distanceSqr > 49.0D && this.budgerigar.onGround() && this.budgerigar.flightCooldown <= 0) {
                Vec3 target = BirdFlightTargeting.findDryLandingTargetNear(this.budgerigar, this.owner.blockPosition(), 5, 10);
                if (target != null) {
                    this.budgerigar.startShortFlight(target, false);
                    return;
                }
            }
            if (--this.repathTicks <= 0) {
                this.repathTicks = 10;
                this.budgerigar.getNavigation().moveTo(this.owner, this.speed);
            }
        }

        @Override
        public void stop() {
            this.owner = null;
            this.repathTicks = 0;
            if (!this.budgerigar.isFlightInProgress()
                    && this.budgerigar.getBehaviorState() == BudgerigarBehaviorState.FOLLOWING) {
                this.budgerigar.setBehaviorState(BudgerigarBehaviorState.IDLE);
            }
        }
    }

    public enum GuidePreviewAnimation {
        NONE(null),
        IDLE(IDLE_ANIMATION),
        PREEN(PREEN_ANIMATION),
        CURIOUS(CURIOUS_ANIMATION),
        DANCE(DANCE_ANIMATION),
        EAT(EAT_ANIMATION),
        SLEEP(SLEEP_ANIMATION),
        WALK(WALK_ANIMATION),
        FLY(FLY_ANIMATION);

        private final RawAnimation animation;

        GuidePreviewAnimation(RawAnimation animation) {
            this.animation = animation;
        }

        private RawAnimation animation() {
            return this.animation;
        }
    }
}
