package EdDYON.guaniao.content.bird.sparrow;

import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.CleanBirdTemptGoal;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.command.BirdCommandInteraction;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.content.bird.command.BirdRoostGoal;
import EdDYON.guaniao.content.bird.command.BirdStayGoal;
import EdDYON.guaniao.content.bird.command.CommandableBird;
import EdDYON.guaniao.content.bird.flock.FlockCompatibleBird;
import EdDYON.guaniao.content.bird.flock.BirdFlockManager;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.brain.BirdBrain;
import EdDYON.guaniao.content.bird.brain.BirdIntent;
import EdDYON.guaniao.content.bird.flight.BirdFlightAware;
import EdDYON.guaniao.content.bird.flight.BirdFlightBoids;
import EdDYON.guaniao.content.bird.flight.BirdFlightController;
import EdDYON.guaniao.content.bird.flight.BirdFlightProfile;
import EdDYON.guaniao.content.bird.flight.BirdFlightTargeting;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bath.BirdBathAttraction;
import EdDYON.guaniao.content.bath.BirdBathContentType;
import EdDYON.guaniao.content.bath.BirdBathFeedingAnimatable;
import EdDYON.guaniao.content.bath.BirdBathMountable;
import EdDYON.guaniao.content.bath.BirdBathUseGoal;
import EdDYON.guaniao.content.advancement.BirdAdvancements;
import EdDYON.guaniao.content.bird.species.SparrowProfile;
import EdDYON.guaniao.event.BirdColonySpawnRules;
import EdDYON.guaniao.content.feed.BreadcrumbPileBlock;
import net.minecraft.core.Direction;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoBlocks;
import EdDYON.guaniao.registry.GuaniaoItems;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
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

public class SparrowEntity extends TamableAnimal implements GeoEntity, ScalableBirdModel, BirdFlightAware, BirdBathMountable, BirdBathFeedingAnimatable, CommandableBird, FlockCompatibleBird {
    private static final EntityDataAccessor<Integer> BEHAVIOR_STATE = SynchedEntityData.defineId(SparrowEntity.class, (EntityDataSerializer)EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MODEL_SCALE = SynchedEntityData.defineId(SparrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COMMAND_MODE = SynchedEntityData.defineId(SparrowEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation TAIL_ANIMATION = RawAnimation.begin().thenPlay("animation.idle_diff_1").thenLoop("animation.idle");
    private static final RawAnimation PECK_ANIMATION = RawAnimation.begin().thenPlay("animation.idle_diff_2").thenLoop("animation.idle");
    private static final RawAnimation LOOK_AROUND_ANIMATION = RawAnimation.begin().thenPlay("animation.idle_diff_3").thenLoop("animation.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation FLY_ANIMATION = RawAnimation.begin().thenLoop("animation.fly");
    private static final double SHORT_FLIGHT_SPEED = 0.24;
    private static final double ESCAPE_FLIGHT_SPEED = 0.42;
    private static final BirdFlightProfile FLIGHT_PROFILE = BirdFlightProfile.SPARROW;
    private static final int MAX_CONTROLLED_FLIGHT_TICKS = 320;
    private static final int MAX_LANDING_TICKS = 140;
    private static final float FLIGHT_YAW_TURN_RATE = 22.0f;
    private static final float FLIGHT_PITCH_TURN_RATE = 10.0f;
    private static final int MAX_FAMILIAR_TICKS = 7200;
    private static final int ATTACK_DISTRUST_TICKS = 48000;
    private static final int FULL_SATIATION_TICKS = 2400;
    private static final int MAX_SATIATION_TICKS = 4800;
    private static final int HOME_RADIUS = 36;
    private static final int SETTLEMENT_SCAN_RADIUS = 14;
    private static final int PERCH_SCAN_BUDGET_COST = 3;
    private static final int MAX_SCORED_ROOST_CANDIDATES = 18;
    private static final int MAX_SCORED_DAY_CANDIDATES = 12;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final BirdBrain birdBrain = new BirdBrain(this, SparrowProfile.INSTANCE);
    private SparrowBehaviorState behaviorState = SparrowBehaviorState.IDLE;
    private IdleAnimationChoice currentIdleAnimation = IdleAnimationChoice.BASE;
    private long nextIdleAnimationSwapTick;
    private int forcedIdleAnimationTicks;
    private int familiarTicks;
    private int calmAroundPlayerTicks;
    private int satiatedTicks;
    private int flightTicks;
    private int flightDuration;
    private int timeFlying;
    private int flightCooldown;
    private int blockedFlightTicks;
    private int flightLandingTicks;
    private int airborneFlightAnimationTicks;
    private Vec3 flightTarget;
    private double flightSpeed = SHORT_FLIGHT_SPEED;
    private boolean escapeFlight;
    private UUID distrustedPlayer;
    private int distrustTicks;
    private Vec3 pendingScareSource;
    private int pendingScareTicks;
    private ScareReaction pendingScareReaction = ScareReaction.ESCAPE_FLIGHT;
    private BlockPos noticedBreadcrumbPos;
    private int breadcrumbInterestTicks;
    private BlockPos homePos;
    private int perchCooldown;
    private long nextPerchSearchTick;
    private int behaviorStateLockTicks;
    private int ownerFollowSuppressedTicks;
    private int restInterruptionTicks;
    private GuidePreviewAnimation guidePreviewAnimation = GuidePreviewAnimation.NONE;

    public SparrowEntity(EntityType<? extends SparrowEntity> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(BlockPathTypes.LEAVES, 0.0f);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0f);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16.0f);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 16.0f);
        this.satiatedTicks = this.getRandom().nextFloat() < 0.62f ? 0 : 300 + this.getRandom().nextInt(1800);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, SparrowDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, SparrowDefinition.WALK_SPEED)
                .add(Attributes.FOLLOW_RANGE, SparrowDefinition.FOLLOW_RANGE);
    }

    public static boolean canSpawn(EntityType<SparrowEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean validGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(Blocks.FARMLAND)
                || below.is(BlockTags.DIRT)
                || below.is(Blocks.SAND)
                || below.is(Blocks.RED_SAND);
        if (!validGround || level.getRawBrightness(pos, 0) <= 8) {
            return false;
        }
        int settlementScore = level instanceof ServerLevel serverLevel
                ? BirdColonySpawnRules.settlementScore(serverLevel, pos)
                : SparrowEntity.settlementScore(level, pos, SETTLEMENT_SCAN_RADIUS, 4);
        if (settlementScore >= 14) {
            return true;
        }
        if (settlementScore >= 6) {
            return random.nextFloat() < 0.82f;
        }
        return random.nextFloat() < 0.48f;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, (Goal)new FloatGoal((Mob)this));
        this.goalSelector.addGoal(1, (Goal)new SparrowFleePlayerGoal(this));
        this.goalSelector.addGoal(2, new BirdStayGoal<>(this));
        this.goalSelector.addGoal(2, new BirdRoostGoal<>(this));
        this.goalSelector.addGoal(2, (Goal)new SparrowFollowOwnerGoal(this, 1.02, 3.0f, 10.0f));
        if (this.usesSparrowTemptGoal()) {
            this.goalSelector.addGoal(3, new CleanBirdTemptGoal(this, 0.9, Ingredient.of(this.foodTag()), false));
        }
        if (this.usesSparrowBreedGoal()) {
            this.goalSelector.addGoal(4, (Goal)new BreedGoal(this, 0.8));
        }
        if (this.usesSparrowBreadcrumbGoal()) {
            this.goalSelector.addGoal(5, new SparrowEatBreadcrumbGoal(this));
        }
        this.goalSelector.addGoal(6, (Goal)new BirdBathUseGoal(this, 0.82D, 9.0D, 42,
                BirdBathAttraction::isAttractiveToSmallSeedBird,
                this::canUseBirdBath,
                bath -> this.setBehaviorState(SparrowBehaviorState.FORAGING),
                this::consumeBirdBathServing,
                (bath, consumed) -> {
                    if (this.getBehaviorState() == SparrowBehaviorState.FORAGING) {
                        this.setBehaviorStateFor(SparrowBehaviorState.LOOK_AROUND, consumed ? 24 : 8);
                    }
                }));
        if (this.usesSparrowPerchGoal()) {
            this.goalSelector.addGoal(7, new SparrowPerchGoal(this));
        }
        if (this.usesSparrowFlockGoal()) {
            this.goalSelector.addGoal(8, new SparrowFlockGoal(this));
        }
        this.goalSelector.addGoal(9, (Goal)new RandomStrollGoal(this, 0.72));
        this.goalSelector.addGoal(10, (Goal)new LookAtPlayerGoal((Mob)this, Player.class, 6.0f));
        this.goalSelector.addGoal(11, (Goal)new RandomLookAroundGoal((Mob)this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BEHAVIOR_STATE, SparrowBehaviorState.IDLE.ordinal());
        this.entityData.define(MODEL_SCALE, BirdModelScale.DEFAULT_INDIVIDUAL_SCALE);
        this.entityData.define(COMMAND_MODE, BirdCommandMode.FREE.ordinal());
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
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (BEHAVIOR_STATE.equals(key)) {
            this.behaviorState = SparrowEntity.decodeBehaviorState((Integer)this.entityData.get(BEHAVIOR_STATE));
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.familiarTicks > 0) {
            --this.familiarTicks;
        }
        if (this.calmAroundPlayerTicks > 0) {
            --this.calmAroundPlayerTicks;
        }
        if (this.satiatedTicks > 0) {
            --this.satiatedTicks;
        }
        if (this.breadcrumbInterestTicks > 0) {
            --this.breadcrumbInterestTicks;
            if (this.breadcrumbInterestTicks <= 0) {
                this.noticedBreadcrumbPos = null;
            }
        }
        if (this.distrustTicks > 0) {
            --this.distrustTicks;
            if (this.distrustTicks <= 0) {
                this.distrustedPlayer = null;
            }
        }
        if (this.flightCooldown > 0) {
            --this.flightCooldown;
        }
        if (this.airborneFlightAnimationTicks > 0) {
            if (this.onGround()) {
                this.airborneFlightAnimationTicks = 0;
            } else {
                --this.airborneFlightAnimationTicks;
            }
        }
        if (this.ownerFollowSuppressedTicks > 0) {
            --this.ownerFollowSuppressedTicks;
        }
        if (this.restInterruptionTicks > 0) {
            --this.restInterruptionTicks;
        }
        if (this.perchCooldown > 0) {
            --this.perchCooldown;
        }
        if (this.forcedIdleAnimationTicks > 0) {
            --this.forcedIdleAnimationTicks;
        }
        if (!this.level().isClientSide) {
            this.birdBrain.tick();
            if (this.usesSparrowSettlementHome()) {
                this.ensureHomePos();
            }
            this.tickStaleFlightRecovery();
            this.tickWaterEscape();
        }
        if (!this.level().isClientSide && this.pendingScareTicks > 0) {
            --this.pendingScareTicks;
            if (this.pendingScareTicks <= 0) {
                this.releasePendingScare();
            }
        }
        if (!this.level().isClientSide && this.isControlledFlightActive()) {
            this.tickControlledFlight();
        }
        if (!this.level().isClientSide && this.onGround() && !this.isInWaterOrBubble()
                && (!this.isTame() || this.isBirdCommandMode(BirdCommandMode.FREE))) {
            if (this.getNavigation().isDone() && this.getRandom().nextInt(160) == 0) {
                this.triggerPeck();
            }
            int ambientFlightChance = this.level().isDay() ? (this.isTame() ? 360 : 260) : (this.isTame() ? 720 : 520);
            if (this.canStartAmbientShortFlight() && this.getRandom().nextInt(ambientFlightChance) == 0) {
                this.startAmbientShortFlight();
            }
            int shortHopChance = this.isTame() ? 900 : 520;
            if (this.canRandomShortHop() && this.getRandom().nextInt(shortHopChance) == 0) {
                this.shortHop();
            }
        }
        if (!this.onGround() || this.isControlledFlightActive()) {
            this.fallDistance = 0.0f;
        }
        if (!this.level().isClientSide) {
            this.tickBehaviorStateFallback();
            this.tickGroundMovementFacing();
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult commandResult = BirdCommandInteraction.tryHandle(this, this, player, hand);
        if (commandResult.consumesAction()) {
            return commandResult;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (isCleanTamingItem(stack)) {
            if (this.isDistrusted(player)) {
                if (!this.level().isClientSide) {
                    this.startEscapeFlight(player.position());
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            this.familiarTicks = Math.max(this.familiarTicks, 3600);
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.birdBrain.onEat(0.20F);
                this.satiatedTicks = Math.max(this.satiatedTicks, 900);
                this.calmAroundPlayerTicks = Math.max(this.calmAroundPlayerTicks, 600);
                if (this.isTame()) {
                    if (this.getHealth() < this.getMaxHealth()) {
                        this.heal(2.0F);
                    }
                    if (!this.isBaby() && !this.isInLove()) {
                        this.setInLove(player);
                    }
                    this.birdBrain.onRest(0.05F);
                    this.level().broadcastEntityEvent(this, (byte)7);
                } else if (this.getRandom().nextInt(3) == 0) {
                    this.tame(player);
                    this.setBirdCommandMode(BirdCommandMode.FOLLOW);
                    this.getNavigation().stop();
                    this.birdBrain.onRest(0.10F);
                    BirdAdvancements.awardTamedBird(player, this);
                    this.level().broadcastEntityEvent(this, (byte)7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte)6);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        boolean hurt = super.hurt(damageSource, amount);
        if (hurt) {
            this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 200);
            Entity attacker = damageSource.getEntity();
            if (BirdConfigManager.aprilFoolsMode() && attacker instanceof Player) {
                return true;
            }
            this.familiarTicks = 0;
            this.calmAroundPlayerTicks = 0;
            if (attacker instanceof Player player && this.isOwnedBy(player)) {
                this.birdBrain.onFrightened(0.25F);
                this.suppressOwnerFollow(120);
                this.getNavigation().stop();
                this.getLookControl().setLookAt(player, 30.0f, 30.0f);
                this.setBehaviorStateFor(SparrowBehaviorState.ALERT, 60);
                return hurt;
            }
            this.birdBrain.onFrightened(0.65F);
            if (attacker != null) {
                if (attacker instanceof Player player) {
                    this.rememberDistrustedPlayer(player);
                }
                if (this.isTame()) {
                    this.suppressOwnerFollow(160);
                }
                if (!this.isControlledFlightActive()) {
                    this.startEscapeFlight(attacker.position());
                }
                this.alertNearbySparrows(attacker);
            }
        }
        return hurt;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putBoolean("NoGravity", false);
        if (this.distrustedPlayer != null && this.distrustTicks > 0) {
            compoundTag.putUUID("DistrustedPlayer", this.distrustedPlayer);
            compoundTag.putInt("DistrustTicks", this.distrustTicks);
        }
        compoundTag.putInt("SatiatedTicks", this.satiatedTicks);
        if (this.homePos != null) {
            compoundTag.putInt("HomeX", this.homePos.getX());
            compoundTag.putInt("HomeY", this.homePos.getY());
            compoundTag.putInt("HomeZ", this.homePos.getZ());
        }
        BirdModelScale.save(compoundTag, this.getIndividualModelScale(), this.modelScaleProfile());
        compoundTag.putInt(CommandableBird.COMMAND_MODE_NBT_KEY, this.getBirdCommandMode().ordinal());
        this.birdBrain.save(compoundTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.hasUUID("DistrustedPlayer")) {
            this.distrustedPlayer = compoundTag.getUUID("DistrustedPlayer");
            this.distrustTicks = compoundTag.getInt("DistrustTicks");
            if (this.distrustTicks <= 0) {
                this.distrustedPlayer = null;
            }
        }
        this.satiatedTicks = Math.max(0, compoundTag.getInt("SatiatedTicks"));
        if (compoundTag.contains("HomeX") && compoundTag.contains("HomeY") && compoundTag.contains("HomeZ")) {
            this.homePos = new BlockPos(compoundTag.getInt("HomeX"), compoundTag.getInt("HomeY"), compoundTag.getInt("HomeZ"));
        }
        if (compoundTag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.setIndividualModelScale(BirdModelScale.load(compoundTag, this.modelScaleProfile()));
        } else {
            this.randomizeModelScale();
        }
        if (compoundTag.contains(CommandableBird.COMMAND_MODE_NBT_KEY, 3)) {
            this.setBirdCommandMode(BirdCommandMode.byId(compoundTag.getInt(CommandableBird.COMMAND_MODE_NBT_KEY)));
        } else {
            this.setBirdCommandMode(this.isTame() ? BirdCommandMode.FOLLOW : BirdCommandMode.FREE);
        }
        this.birdBrain.load(compoundTag);
        this.clearSerializedFlightState();
    }

    public BirdBrain birdBrain() {
        return this.birdBrain;
    }

    public void acceptFlockTrust(int familiarTicks, int calmTicks) {
        this.familiarTicks = Math.max(this.familiarTicks, familiarTicks);
        this.calmAroundPlayerTicks = Math.max(this.calmAroundPlayerTicks, calmTicks);
    }

    public boolean fleeFromFlockThreat(Vec3 threatPosition) {
        if (threatPosition == null || this.isControlledFlightActive()) {
            return false;
        }
        return this.startEscapeFlight(threatPosition);
    }

    @Override
    public BirdFlightProfile birdFlightProfile() {
        return FLIGHT_PROFILE;
    }

    @Override
    public boolean isBirdFlightActive() {
        return this.isControlledFlightActive()
                || this.getBehaviorState().isAirborne()
                || this.airborneFlightAnimationTicks > 0 && !this.onGround();
    }

    @Override
    public boolean isBirdLanding() {
        return this.flightLandingTicks > 0;
    }

    @Override
    public boolean isBirdEscaping() {
        return this.escapeFlight;
    }

    public boolean startFlybyFlight(Vec3 landingTarget) {
        if (landingTarget == null) {
            return false;
        }
        return this.startControlledFlight(landingTarget, this.randomBetween(78, 122), SHORT_FLIGHT_SPEED + 0.05 + this.getRandom().nextDouble() * 0.04, false);
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
            horizontal = horizontal.normalize().scale(0.27D);
        }
        Vec3 movement = new Vec3(horizontal.x, 0.64D, horizontal.z);
        this.getNavigation().stop();
        this.setOnGround(false);
        this.airborneFlightAnimationTicks = Math.max(this.airborneFlightAnimationTicks, 32);
        this.setBehaviorStateFor(SparrowBehaviorState.SHORT_FLIGHT, 32);
        this.setDeltaMovement(movement);
        this.faceMovement(movement);
        this.fallDistance = 0.0F;
        this.hasImpulse = true;
        return true;
    }

    @Override
    public void startBirdBathFeedingAnimation(BirdBathContentType contentType, int ticks) {
        this.getNavigation().stop();
        if (contentType.isFood()) {
            this.setBehaviorStateFor(SparrowBehaviorState.PECKING, Math.max(28, ticks));
            return;
        }
        this.setBehaviorStateFor(SparrowBehaviorState.LOOK_AROUND, Math.max(24, ticks / 2));
    }

    public SparrowBehaviorState getBehaviorState() {
        if (this.entityData != null) {
            return SparrowEntity.decodeBehaviorState((Integer)this.entityData.get(BEHAVIOR_STATE));
        }
        return this.behaviorState;
    }

    void setBehaviorState(SparrowBehaviorState behaviorState) {
        if (behaviorState == null) {
            behaviorState = SparrowBehaviorState.IDLE;
        }
        this.behaviorState = behaviorState;
        if (this.entityData != null) {
            this.entityData.set(BEHAVIOR_STATE, behaviorState.ordinal());
        }
    }

    void setBehaviorStateFor(SparrowBehaviorState behaviorState, int ticks) {
        this.setBehaviorState(behaviorState);
        this.behaviorStateLockTicks = Math.max(this.behaviorStateLockTicks, ticks);
    }

    private static SparrowBehaviorState decodeBehaviorState(int ordinal) {
        SparrowBehaviorState[] values = SparrowBehaviorState.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return SparrowBehaviorState.IDLE;
        }
        return values[ordinal];
    }

    private boolean brainWantsForage() {
        BirdIntent intent = this.birdBrain.currentIntent();
        return this.birdBrain.wantsForage() || intent == BirdIntent.FORAGE;
    }

    private boolean brainWantsRoost() {
        if (this.isRestInterrupted()) {
            return false;
        }
        BirdIntent intent = this.birdBrain.currentIntent();
        return this.birdBrain.wantsRoost() || intent == BirdIntent.ROOST;
    }

    private boolean brainWantsEscapeOrAlert() {
        BirdIntent intent = this.birdBrain.currentIntent();
        return this.birdBrain.wantsShortEscape()
                || this.birdBrain.wantsLongEscape()
                || intent == BirdIntent.ALERT
                || intent == BirdIntent.SHORT_FLIGHT
                || intent == BirdIntent.LONG_FLIGHT;
    }

    private boolean canUseBirdBath() {
        SparrowBehaviorState state = this.getBehaviorState();
        return this.onGround()
                && !this.isControlledFlightActive()
                && this.flightCooldown <= 0
                && this.pendingScareTicks <= 0
                && !this.hasBreadcrumbInterest()
                && !this.brainWantsRoost()
                && this.getTarget() == null
                && state != SparrowBehaviorState.PERCHING
                && state != SparrowBehaviorState.ROOSTING
                && !state.isEscape();
    }

    private void consumeBirdBathServing(EdDYON.guaniao.content.bath.BirdBathBlockEntity bath, BirdBathContentType contentType) {
        if (contentType == BirdBathContentType.BREAD) {
            this.satiatedTicks = Math.max(this.satiatedTicks, 700);
            this.birdBrain.onEat(0.24F);
            this.triggerPeck();
            return;
        }
        this.satiatedTicks = Math.max(this.satiatedTicks, 420);
        this.birdBrain.onEat(0.12F);
        this.setBehaviorStateFor(SparrowBehaviorState.LOOK_AROUND, 26);
        this.playSound(SoundEvents.GENERIC_DRINK, 0.18F, 1.35F + this.getRandom().nextFloat() * 0.18F);
    }

    private boolean canStartAmbientShortFlight() {
        if (this.isControlledFlightActive()
                || !this.onGround()
                || this.flightCooldown > 0
                || !this.getNavigation().isDone()
                || this.behaviorStateLockTicks > 0
                || this.hasBreadcrumbInterest()
                || this.brainWantsForage()
                || this.brainWantsRoost()) {
            return false;
        }
        SparrowBehaviorState state = this.getBehaviorState();
        if (state == SparrowBehaviorState.FORAGING
                || state == SparrowBehaviorState.PECKING
                || state == SparrowBehaviorState.PERCHING
                || state == SparrowBehaviorState.ROOSTING
                || state == SparrowBehaviorState.FOLLOWING_OWNER
                || state == SparrowBehaviorState.ALERT
                || state.isEscape()) {
            return false;
        }
        if (!this.isTame()) {
            return true;
        }
        LivingEntity owner = this.getOwner();
        return owner == null || this.distanceToSqr(owner) > 49.0;
    }

    private boolean canRandomShortHop() {
        if (this.isControlledFlightActive()
                || !this.onGround()
                || !this.getNavigation().isDone()
                || this.behaviorStateLockTicks > 0
                || this.hasBreadcrumbInterest()
                || this.brainWantsForage()
                || this.brainWantsRoost()) {
            return false;
        }
        SparrowBehaviorState state = this.getBehaviorState();
        return state == SparrowBehaviorState.IDLE || state == SparrowBehaviorState.LOOK_AROUND;
    }

    private void suppressOwnerFollow(int ticks) {
        if (this.isTame()) {
            this.ownerFollowSuppressedTicks = Math.max(this.ownerFollowSuppressedTicks, ticks);
        }
    }

    private void tickBehaviorStateFallback() {
        if (this.behaviorStateLockTicks > 0) {
            --this.behaviorStateLockTicks;
        }
        if (this.isControlledFlightActive()) {
            this.setBehaviorState(this.escapeFlight ? SparrowBehaviorState.FLEEING : SparrowBehaviorState.SHORT_FLIGHT);
            return;
        }
        if (this.updateFollowingOwnerBehaviorState()) {
            return;
        }
        SparrowBehaviorState state = this.getBehaviorState();
        BirdIntent intent = this.birdBrain.currentIntent();
        if (this.behaviorStateLockTicks <= 0
                && this.forcedIdleAnimationTicks <= 0
                && this.getNavigation().isDone()
                && state != SparrowBehaviorState.PERCHING
                && state != SparrowBehaviorState.ROOSTING
                && state != SparrowBehaviorState.FORAGING
                && (intent == BirdIntent.ALERT || intent == BirdIntent.WATCH)) {
            this.setBehaviorState(SparrowBehaviorState.ALERT);
            return;
        }
        if (this.behaviorStateLockTicks <= 0
                && this.forcedIdleAnimationTicks <= 0
                && state != SparrowBehaviorState.PERCHING
                && state != SparrowBehaviorState.ROOSTING
                && state != SparrowBehaviorState.FORAGING) {
            this.setBehaviorState(SparrowBehaviorState.IDLE);
        }
    }

    private boolean updateFollowingOwnerBehaviorState() {
        LivingEntity owner = this.getOwner();
        if (this.isTame()
                && owner != null
                && owner.isAlive()
                && this.ownerFollowSuppressedTicks <= 0
                && this.distanceToSqr(owner) > 9.0
                && !this.getNavigation().isDone()) {
            this.setBehaviorState(SparrowBehaviorState.FOLLOWING_OWNER);
            return true;
        }
        return false;
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
        SparrowBehaviorState state = this.getBehaviorState();
        if (state == SparrowBehaviorState.PECKING
                || state == SparrowBehaviorState.LOOK_AROUND
                || state == SparrowBehaviorState.PERCHING
                || state == SparrowBehaviorState.ROOSTING
                || state.isEscape()) {
            return false;
        }
        return BirdGroundAnimation.hasWalkMotion(this);
    }

    private boolean isOwnedBy(Player player) {
        return this.isTame() && this.getOwnerUUID() != null && this.getOwnerUUID().equals(player.getUUID());
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.SPARROW;
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

    private void randomizeModelScale() {
        this.setIndividualModelScale(BirdModelScale.randomIndividualScale(this.getRandom(), this.modelScaleProfile()));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isCleanTamingItem(stack);
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
        return this.pendingScareTicks > 0 || this.getBehaviorState() == SparrowBehaviorState.FLEEING;
    }

    @Override
    public boolean isOrderedToSit() {
        return false;
    }

    @Override
    public SparrowEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
        SparrowEntity child = GuaniaoEntityTypes.SPARROW.get().create(level);
        if (child != null) {
            float mateScale = mate instanceof SparrowEntity other ? other.getIndividualModelScale() : this.getIndividualModelScale();
            child.setIndividualModelScale(BirdModelScale.inheritIndividualScale(child.getRandom(), this.getIndividualModelScale(), mateScale, child.modelScaleProfile()));
        }
        return child;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        BlockState below = level.getBlockState(pos.below());
        float score = super.getWalkTargetValue(pos, level);
        if (below.is(Blocks.FARMLAND)) {
            score += 5.0f;
        }
        if (below.is(BlockTags.DIRT) || below.is(BlockTags.ANIMALS_SPAWNABLE_ON)) {
            score += 2.0f;
        }
        return score;
    }

    private static int settlementScore(ServerLevelAccessor level, BlockPos origin, int horizontalRadius, int verticalRadius) {
        int score = 0;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int xOffset = -horizontalRadius; xOffset <= horizontalRadius; ++xOffset) {
            for (int zOffset = -horizontalRadius; zOffset <= horizontalRadius; ++zOffset) {
                if (xOffset * xOffset + zOffset * zOffset > horizontalRadius * horizontalRadius) {
                    continue;
                }
                for (int yOffset = -verticalRadius; yOffset <= verticalRadius; ++yOffset) {
                    mutablePos.set(origin.getX() + xOffset, origin.getY() + yOffset, origin.getZ() + zOffset);
                    if (!SparrowEntity.canReadSpawnScan(level, origin, mutablePos)) {
                        continue;
                    }
                    score += SparrowEntity.settlementBlockScore(level.getBlockState(mutablePos));
                    if (score >= 42) {
                        return score;
                    }
                }
            }
        }
        return score;
    }

    private static boolean canReadSpawnScan(ServerLevelAccessor level, BlockPos origin, BlockPos pos) {
        if (level instanceof WorldGenRegion) {
            return SectionPos.blockToSectionCoord(origin.getX()) == SectionPos.blockToSectionCoord(pos.getX())
                    && SectionPos.blockToSectionCoord(origin.getZ()) == SectionPos.blockToSectionCoord(pos.getZ());
        }
        return true;
    }

    private static int settlementBlockScore(BlockState state) {
        if (state.is(Blocks.FARMLAND)) {
            return 4;
        }
        if (state.getBlock() instanceof CropBlock) {
            return 3;
        }
        if (state.is(Blocks.HAY_BLOCK) || state.is(Blocks.COMPOSTER)) {
            return 5;
        }
        if (state.getBlock() instanceof BedBlock || state.getBlock() instanceof DoorBlock) {
            return 6;
        }
        if (state.getBlock() instanceof FenceBlock || state.getBlock() instanceof FenceGateBlock) {
            return 3;
        }
        return 0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.SPARROW_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return GuaniaoSoundEvents.SPARROW_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuaniaoSoundEvents.SPARROW_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.08f, 1.55f);
    }

    @Override
    public int getAmbientSoundInterval() {
        return BirdFlockSoundLimiter.scaledAmbientInterval(this, this.level().isDay() ? 120 : 260);
    }

    @Override
    public void playAmbientSound() {
        if (BirdFlockSoundLimiter.allowAmbient(this)) {
            super.playAmbientSound();
        }
    }

    @Override
    public float getSoundVolume() {
        return 0.38f;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, BirdSoundVolume.apply(this, volume), pitch);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    public void setGuidePreviewAnimation(GuidePreviewAnimation guidePreviewAnimation) {
        this.guidePreviewAnimation = guidePreviewAnimation == null ? GuidePreviewAnimation.NONE : guidePreviewAnimation;
    }

    public GuidePreviewAnimation getGuidePreviewAnimation() {
        return this.guidePreviewAnimation;
    }

    private void triggerPeck() {
        this.currentIdleAnimation = IdleAnimationChoice.PECK;
        this.forcedIdleAnimationTicks = 34;
        this.nextIdleAnimationSwapTick = this.level().getGameTime() + (long)this.forcedIdleAnimationTicks;
        this.setBehaviorStateFor(SparrowBehaviorState.PECKING, 30);
    }

    private void triggerLookAround() {
        this.currentIdleAnimation = IdleAnimationChoice.LOOK_AROUND;
        this.forcedIdleAnimationTicks = 42;
        this.nextIdleAnimationSwapTick = this.level().getGameTime() + (long)this.forcedIdleAnimationTicks;
        this.setBehaviorStateFor(SparrowBehaviorState.LOOK_AROUND, 40);
    }

    private void triggerTailFlick() {
        this.currentIdleAnimation = IdleAnimationChoice.TAIL;
        this.forcedIdleAnimationTicks = 42;
        this.nextIdleAnimationSwapTick = this.level().getGameTime() + (long)this.forcedIdleAnimationTicks;
    }

    private void ensureHomePos() {
        if (this.homePos == null) {
            this.homePos = this.blockPosition().immutable();
        }
    }

    private boolean shouldSeekNightRoost() {
        return !this.isRestInterrupted()
                && (BirdActivitySchedule.DIURNAL.isRestTime(this.level().getDayTime()) || this.level().isRaining());
    }

    private boolean isRestInterrupted() {
        return this.restInterruptionTicks > 0;
    }

    private BlockPos findPerchTarget(boolean roosting) {
        this.ensureHomePos();
        BlockPos center = roosting ? this.homePos : this.blockPosition();
        int radius = roosting ? HOME_RADIUS : 18;
        int minY = roosting ? -5 : -3;
        int maxY = roosting ? 13 : 8;
        int attempts = roosting ? 130 : 64;
        BlockPos bestPos = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int scoredCandidates = 0;
        int maxScoredCandidates = roosting ? MAX_SCORED_ROOST_CANDIDATES : MAX_SCORED_DAY_CANDIDATES;
        List<SparrowEntity> nearbyRoostingSparrows = roosting
                ? BirdFlockManager.nearby(this, SparrowEntity.class, 12.0D)
                : List.of();
        Map<Long, Boolean> coverBySection = new HashMap<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        perchSearch:
        for (int attempt = 0; attempt < attempts; ++attempt) {
            int xOffset = this.randomBetween(-radius, radius);
            int zOffset = this.randomBetween(-radius, radius);
            if (xOffset * xOffset + zOffset * zOffset > radius * radius) {
                continue;
            }
            for (int yOffset = maxY; yOffset >= minY; --yOffset) {
                mutablePos.set(center.getX() + xOffset, center.getY() + yOffset, center.getZ() + zOffset);
                BlockPos perchPos = mutablePos.above();
                if (!this.isSafePerchPosition(perchPos)) {
                    continue;
                }
                double score = this.scorePerchPosition(
                        perchPos, roosting, nearbyRoostingSparrows, coverBySection);
                if (score > bestScore) {
                    bestScore = score;
                    bestPos = perchPos.immutable();
                }
                if (++scoredCandidates >= maxScoredCandidates) {
                    break perchSearch;
                }
            }
        }
        return bestPos;
    }

    private boolean isSafePerchPosition(BlockPos pos) {
        if (!this.canReadChunk(pos)) {
            return false;
        }
        BlockState below = this.level().getBlockState(pos.below());
        BlockState feet = this.level().getBlockState(pos);
        BlockState head = this.level().getBlockState(pos.above());
        if (!feet.getCollisionShape((BlockGetter)this.level(), pos).isEmpty() || !head.getCollisionShape((BlockGetter)this.level(), pos.above()).isEmpty()) {
            return false;
        }
        if (this.level().getFluidState(pos).is(FluidTags.WATER) || this.level().getFluidState(pos).is(FluidTags.LAVA)) {
            return false;
        }
        if (below.isAir() || below.is(Blocks.CACTUS) || below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.FIRE) || below.is(Blocks.SOUL_FIRE) || below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE)) {
            return false;
        }
        return this.isPreferredPerchBase(below) || below.isFaceSturdy((BlockGetter)this.level(), pos.below(), Direction.UP);
    }

    private boolean isPreferredPerchBase(BlockState state) {
        return state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof FenceGateBlock
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(Blocks.HAY_BLOCK)
                || state.is(Blocks.COMPOSTER);
    }

    private double scorePerchPosition(
            BlockPos pos,
            boolean roosting,
            List<SparrowEntity> nearbyRoostingSparrows,
            Map<Long, Boolean> coverBySection
    ) {
        BlockState below = this.level().getBlockState(pos.below());
        double score = 0.0;
        if (below.getBlock() instanceof FenceBlock || below.getBlock() instanceof FenceGateBlock) {
            score += 24.0;
        }
        if (below.is(BlockTags.LEAVES)) {
            score += roosting ? 25.0 : 15.0;
        }
        if (below.is(BlockTags.LOGS)) {
            score += roosting ? 20.0 : 13.0;
        }
        if (below.is(Blocks.HAY_BLOCK) || below.is(Blocks.COMPOSTER)) {
            score += 17.0;
        }
        if (below.isFaceSturdy((BlockGetter)this.level(), pos.below(), Direction.UP)) {
            score += 6.0;
        }
        score += Math.max(0.0, Math.min(10.0, (double)(pos.getY() - this.blockPosition().getY()) * 1.2));
        if (roosting) {
            long section = SectionPos.asLong(
                    SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getY()),
                    SectionPos.blockToSectionCoord(pos.getZ()));
            score += coverBySection.computeIfAbsent(section, ignored -> this.hasPerchCoverNear(pos, 4))
                    ? 10.0 : 0.0;
            score += this.nearbyRoostingSparrowScore(pos, nearbyRoostingSparrows);
        }
        int settlementScore = this.level() instanceof ServerLevel serverLevel
                ? BirdColonySpawnRules.settlementScore(serverLevel, pos)
                : this.localSettlementScore(pos, 6);
        score += Math.min(12.0, settlementScore * 0.5);
        if (this.homePos != null) {
            score -= Math.sqrt(this.blockDistanceSqr(pos, this.homePos)) * 0.08;
        }
        score -= Math.sqrt(this.blockDistanceSqr(pos, this.blockPosition())) * (roosting ? 0.03 : 0.08);
        return score;
    }

    private double blockDistanceSqr(BlockPos first, BlockPos second) {
        double x = first.getX() - second.getX();
        double y = first.getY() - second.getY();
        double z = first.getZ() - second.getZ();
        return x * x + y * y + z * z;
    }

    private boolean hasPerchCoverNear(BlockPos pos, int radius) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int horizontalStep = radius >= 4 ? 2 : 1;
        for (int xOffset = -radius; xOffset <= radius; xOffset += horizontalStep) {
            for (int zOffset = -radius; zOffset <= radius; zOffset += horizontalStep) {
                for (int yOffset = 0; yOffset <= 4; ++yOffset) {
                    mutablePos.set(pos.getX() + xOffset, pos.getY() + yOffset, pos.getZ() + zOffset);
                    if (!this.canReadChunk(mutablePos)) {
                        continue;
                    }
                    BlockState state = this.level().getBlockState(mutablePos);
                    if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.getBlock() instanceof FenceBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private double nearbyRoostingSparrowScore(BlockPos pos, List<SparrowEntity> nearbyRoostingSparrows) {
        return nearbyRoostingSparrows.stream()
                .filter(other -> other != this && other.onGround()).mapToDouble(other -> {
            double distance = Vec3.atCenterOf(pos).distanceTo(other.position());
            if (distance < 0.9) {
                return -10.0;
            }
            if (distance <= 5.5) {
                return 6.0;
            }
            return 0.0;
        }).sum();
    }

    private int localSettlementScore(BlockPos origin, int radius) {
        int score = 0;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int xOffset = -radius; xOffset <= radius; ++xOffset) {
            for (int zOffset = -radius; zOffset <= radius; ++zOffset) {
                for (int yOffset = -2; yOffset <= 2; ++yOffset) {
                    mutablePos.set(origin.getX() + xOffset, origin.getY() + yOffset, origin.getZ() + zOffset);
                    if (!this.canReadChunk(mutablePos)) {
                        continue;
                    }
                    score += SparrowEntity.settlementBlockScore(this.level().getBlockState(mutablePos));
                    if (score >= 28) {
                        return score;
                    }
                }
            }
        }
        return score;
    }

    private void shortHop() {
        float yaw = this.getYRot() * ((float)Math.PI / 180.0f);
        double side = (this.getRandom().nextDouble() - 0.5) * 0.12;
        this.setDeltaMovement(-Math.sin(yaw) * 0.13 + side, 0.23, Math.cos(yaw) * 0.13 - side);
        this.hasImpulse = true;
        if (this.getBehaviorState() == SparrowBehaviorState.FLEEING) {
            this.setBehaviorStateFor(SparrowBehaviorState.FLEEING, 24);
        } else {
            this.setBehaviorStateFor(SparrowBehaviorState.LOOK_AROUND, 18);
        }
    }

    private boolean isControlledFlightActive() {
        return this.flightTarget != null && (this.flightTicks > 0 || !this.onGround());
    }

    private void clearSerializedFlightState() {
        this.flightTarget = null;
        this.flightTicks = 0;
        this.flightDuration = 0;
        this.timeFlying = 0;
        this.blockedFlightTicks = 0;
        this.flightLandingTicks = 0;
        this.airborneFlightAnimationTicks = 0;
        this.escapeFlight = false;
        if (this.isNoGravity()) {
            this.setNoGravity(false);
        }
        if (!this.onGround() && this.getDeltaMovement().lengthSqr() < 0.0025) {
            this.setDeltaMovement(0.0, -0.08, 0.0);
            this.hasImpulse = true;
            this.fallDistance = 0.0f;
        }
    }

    private void tickStaleFlightRecovery() {
        if (!this.isControlledFlightActive() && this.isNoGravity()) {
            this.setNoGravity(false);
            if (!this.onGround() && this.getDeltaMovement().lengthSqr() < 0.0025) {
                this.setDeltaMovement(0.0, -0.08, 0.0);
                this.hasImpulse = true;
            }
        }
    }

    private boolean startAmbientShortFlight() {
        Vec3 target = this.findShortFlightTarget(null, false, 4, 11);
        if (target == null) {
            return false;
        }
        return this.startControlledFlight(target, this.randomBetween(22, 42), SHORT_FLIGHT_SPEED + this.getRandom().nextDouble() * 0.04, false);
    }

    private boolean startEscapeFlight(Vec3 threatPosition) {
        if (this.isControlledFlightActive()) {
            return false;
        }
        Vec3 target = this.findShortFlightTarget(threatPosition, true, 12, 22);
        if (target == null) {
            Vec3 away = this.position().subtract(threatPosition).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() <= 1.0E-4) {
                double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
                away = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            }
            target = BirdFlightTargeting.findRecoveryTarget(this, away, 5, 10);
            if (target == null) {
                return false;
            }
        }
        this.suppressOwnerFollow(160);
        return this.startControlledFlight(target, this.randomBetween(48, 86), ESCAPE_FLIGHT_SPEED, true);
    }

    private boolean startControlledFlight(Vec3 target, int duration, double speed, boolean escapeFlight) {
        if (target == null) {
            return false;
        }
        if (!BirdFlightTargeting.hasClearFlightPath(this, target)) {
            target = BirdFlightTargeting.findRecoveryTarget(this, target.subtract(this.position()), 5, 8);
            if (target == null) {
                return false;
            }
        }
        this.pendingScareSource = null;
        this.pendingScareTicks = 0;
        this.flightTarget = target;
        this.flightTicks = duration;
        this.flightDuration = duration;
        this.timeFlying = 0;
        this.flightSpeed = speed;
        this.escapeFlight = escapeFlight;
        this.blockedFlightTicks = 0;
        this.flightLandingTicks = 0;
        this.airborneFlightAnimationTicks = duration + 12;
        this.setBehaviorStateFor(escapeFlight ? SparrowBehaviorState.FLEEING : SparrowBehaviorState.SHORT_FLIGHT, escapeFlight ? 70 : 32);
        this.flightCooldown = escapeFlight ? 45 + this.getRandom().nextInt(65) : (this.isTame() ? 90 + this.getRandom().nextInt(110) : 70 + this.getRandom().nextInt(90));
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.setOnGround(false);
        Vec3 direction = target.subtract(this.position()).multiply(1.0, 0.0, 1.0);
        if (direction.lengthSqr() <= 1.0E-4) {
            direction = this.getLookAngle().multiply(1.0, 0.0, 1.0);
        }
        direction = direction.normalize();
        this.setDeltaMovement(direction.scale(speed * 0.75).add(0.0, escapeFlight ? 0.48 : 0.28, 0.0));
        this.faceMovement(this.getDeltaMovement());
        this.hasImpulse = true;
        return true;
    }

    private void tickControlledFlight() {
        if (this.flightTarget == null) {
            this.finishControlledFlight(false);
            return;
        }
        this.getNavigation().stop();
        this.setNoGravity(true);
        --this.flightTicks;
        ++this.timeFlying;
        if (this.timeFlying > MAX_CONTROLLED_FLIGHT_TICKS) {
            this.finishControlledFlight(false);
            return;
        }
        if (this.flightTicks <= 0 && !this.onGround()) {
            ++this.flightLandingTicks;
            if (this.flightLandingTicks > MAX_LANDING_TICKS) {
                this.finishControlledFlight(false);
                return;
            }
            this.flightTicks = 1;
            if (this.flightLandingTicks == 1 || this.flightLandingTicks % 14 == 0) {
                Vec3 landing = this.findNearestShortFlightLandingTarget(this.flightLandingTicks > 70 ? 18 : 11);
                if (landing != null) {
                    this.flightTarget = landing;
                }
            }
        }
        Vec3 toTarget = this.flightTarget.subtract(this.position());
        double distance = toTarget.length();
        double horizontalDistance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        boolean progressStalled = BirdFlightController.isFlightProgressStalled(this, this.flightTarget, 12, 12);
        int flightAge = this.flightDuration - this.flightTicks;
        boolean closeToTarget = distance < (this.escapeFlight ? 0.65 : 0.9);
        boolean groundedNearTarget = this.onGround() && flightAge > 8 && horizontalDistance < (this.escapeFlight ? 1.3 : 1.8);
        if (groundedNearTarget || (closeToTarget && this.onGround())) {
            this.finishControlledFlight(true);
            return;
        }
        Vec3 horizontalDirection = new Vec3(toTarget.x, 0.0, toTarget.z);
        if (horizontalDirection.lengthSqr() <= 1.0E-4) {
            horizontalDirection = this.getDeltaMovement().multiply(1.0, 0.0, 1.0);
        }
        if (horizontalDirection.lengthSqr() <= 1.0E-4) {
            horizontalDirection = this.getLookAngle().multiply(1.0, 0.0, 1.0);
        }
        double heightAboveTarget = this.getY() - this.flightTarget.y;
        if (this.flightLandingTicks > 0 && horizontalDistance < 1.5 && heightAboveTarget > 2.4) {
            Vec3 drift = this.getDeltaMovement().multiply(1.0, 0.0, 1.0);
            if (drift.lengthSqr() > 1.0E-4) {
                horizontalDirection = drift;
            }
        }
        horizontalDirection = horizontalDirection.normalize();
        Vec3 flockHeading = BirdFlightBoids.sameTypeHeading(
                this,
                this.escapeFlight ? 14.0D : 10.0D,
                1.9D,
                this.escapeFlight ? 0.0D : 0.035D,
                this.escapeFlight ? 0.10D : 0.34D,
                this.escapeFlight ? 0.22D : 0.10D,
                this.escapeFlight ? 0.20D : 0.06D);
        if (flockHeading.lengthSqr() > 1.0E-4D) {
            horizontalDirection = BirdFlightTargeting.normalizeHorizontal(horizontalDirection.add(flockHeading), horizontalDirection);
        }
        double speed = BirdFlightController.decelerateNearLanding(this.flightSpeed, horizontalDistance, this.escapeFlight ? 3.0D : 2.4D, 0.50D);
        double lift = Mth.clamp(toTarget.y * 0.16, -0.11, 0.16);
        if (flightAge < 8) {
            lift += this.escapeFlight ? 0.24 : 0.11;
        }
        if (horizontalDistance < 1.6) {
            lift = Mth.clamp(toTarget.y * 0.22 - 0.05, -0.14, 0.06);
        }
        if (this.flightLandingTicks > 0) {
            speed = Math.max(speed, this.flightSpeed * 0.46);
            lift = Math.min(lift, this.flightLandingTicks > 70 ? -0.05 : -0.032);
            if (heightAboveTarget < 1.15) {
                lift = Math.max(lift, -0.026);
            }
        }
        Vec3 desired = horizontalDirection.scale(speed).add(0.0, lift, 0.0);
        Vec3 movement = this.getDeltaMovement().scale(0.32).add(desired.scale(0.68));
        if (this.flightLandingTicks <= 0 && BirdFlightController.isStalledInAir(this, this.timeFlying, 0.006D)) {
            Vec3 newTarget = this.findShortFlightTarget(null, this.escapeFlight, this.escapeFlight ? 8 : 4, this.escapeFlight ? 16 : 10);
            if (newTarget != null) {
                this.flightTarget = newTarget;
                this.flightTicks = Math.max(this.flightTicks, 18);
                this.flightLandingTicks = 0;
            }
            movement = horizontalDirection.scale(Math.max(speed, this.flightSpeed * 0.65D)).add(0.0D, this.escapeFlight ? 0.12D : 0.08D, 0.0D);
        }
        if (this.horizontalCollision || (this.verticalCollision && flightAge > 6) || progressStalled) {
            ++this.blockedFlightTicks;
            movement = movement.add(0.0, 0.08, 0.0);
        } else {
            this.blockedFlightTicks = Math.max(0, this.blockedFlightTicks - 1);
        }
        if (this.blockedFlightTicks > 5) {
            Vec3 newTarget = BirdFlightTargeting.findRecoveryTarget(this, toTarget, 5, 8);
            if (newTarget == null) {
                newTarget = this.findShortFlightTarget(null, false, 3, 8);
            }
            if (newTarget == null) {
                this.finishControlledFlight(false);
                return;
            }
            this.flightTarget = newTarget;
            this.blockedFlightTicks = 0;
        }
        if (this.flightTicks <= 0) {
            if (this.onGround()) {
                this.finishControlledFlight(true);
                return;
            }
            this.flightTicks = 1;
        }
        this.setDeltaMovement(movement);
        this.faceMovement(movement);
        this.fallDistance = 0.0f;
        this.hasImpulse = true;
    }

    private void tickWaterEscape() {
        if (!this.isInWaterOrBubble()) {
            return;
        }
        this.getNavigation().stop();
        Vec3 target = this.findShortFlightTarget(null, true, 7, 16);
        if (target != null && !this.isControlledFlightActive()) {
            this.startControlledFlight(target, this.randomBetween(42, 72), ESCAPE_FLIGHT_SPEED, true);
            return;
        }
        Vec3 movement = this.getDeltaMovement().multiply(0.45, 0.0, 0.45).add(0.0, 0.32, 0.0);
        this.setNoGravity(true);
        this.setBehaviorStateFor(SparrowBehaviorState.SHORT_FLIGHT, 35);
        this.setDeltaMovement(movement);
        this.faceMovement(movement);
        this.fallDistance = 0.0f;
        this.hasImpulse = true;
    }

    private void finishControlledFlight(boolean landed) {
        boolean wasEscapeFlight = this.escapeFlight;
        BirdFlightController.clearFlightProgress(this);
        this.flightTarget = null;
        this.flightTicks = 0;
        this.flightDuration = 0;
        this.timeFlying = 0;
        this.blockedFlightTicks = 0;
        this.flightLandingTicks = 0;
        this.escapeFlight = false;
        this.setNoGravity(false);
        Vec3 movement = this.getDeltaMovement();
        if (landed) {
            this.setDeltaMovement(movement.x * 0.35, 0.0, movement.z * 0.35);
            this.airborneFlightAnimationTicks = 0;
        } else {
            this.setDeltaMovement(movement.x * 0.55, Math.min(movement.y * 0.35, -0.04), movement.z * 0.55);
            this.airborneFlightAnimationTicks = Math.max(this.airborneFlightAnimationTicks, 18);
        }
        int cooldown = wasEscapeFlight ? 55 + this.getRandom().nextInt(75) : (this.isTame() ? 100 + this.getRandom().nextInt(120) : 85 + this.getRandom().nextInt(95));
        this.flightCooldown = Math.max(this.flightCooldown, cooldown);
        if (wasEscapeFlight) {
            this.suppressOwnerFollow(140);
        }
        if (this.getBehaviorState().isEscape()) {
            this.behaviorStateLockTicks = 0;
            if (landed || this.onGround()) {
                this.setBehaviorState(SparrowBehaviorState.IDLE);
            } else {
                this.setBehaviorStateFor(wasEscapeFlight ? SparrowBehaviorState.FLEEING : SparrowBehaviorState.SHORT_FLIGHT, 18);
            }
        }
    }

    private Vec3 findShortFlightTarget(Vec3 threatPosition, boolean escape, int minRadius, int maxRadius) {
        BlockPos origin = this.blockPosition();
        Vec3 bestTarget = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int attempt = 0; attempt < 28; ++attempt) {
            double angle;
            if (escape && threatPosition != null) {
                Vec3 away = this.position().subtract(threatPosition).multiply(1.0, 0.0, 1.0);
                angle = away.lengthSqr() > 1.0E-4 ? Math.atan2(away.z, away.x) + this.randomSigned(0.75) : this.getRandom().nextDouble() * Math.PI * 2.0;
            } else {
                Vec3 forward = BirdFlightTargeting.normalizeHorizontal(this.getViewVector(1.0F), this.getLookAngle());
                angle = attempt < 20
                        ? Math.atan2(forward.z, forward.x) + this.randomSigned(Math.toRadians(15.0D))
                        : this.getRandom().nextDouble() * Math.PI * 2.0;
            }
            double radius = minRadius + this.getRandom().nextDouble() * (double)(maxRadius - minRadius);
            int x = origin.getX() + Mth.floor(Math.cos(angle) * radius);
            int z = origin.getZ() + Mth.floor(Math.sin(angle) * radius);
            int y = origin.getY() + (escape ? this.randomBetween(2, 8) : this.randomBetween(-1, 4));
            BlockPos landing = this.findLandingSurface(new BlockPos(x, y, z), escape ? 9 : 6);
            if (landing == null) {
                continue;
            }
            double score = this.scoreShortFlightLanding(landing, threatPosition, escape);
            Vec3 candidate = new Vec3((double)landing.getX() + 0.5, (double)landing.getY() + 0.05, (double)landing.getZ() + 0.5);
            if (!BirdFlightTargeting.hasClearFlightPath(this, candidate)) {
                continue;
            }
            if (score > bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }
        return bestTarget;
    }

    private Vec3 findNearestShortFlightLandingTarget(int radius) {
        BlockPos origin = this.blockPosition();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int r = 1; r <= radius; ++r) {
            for (int xOffset = -r; xOffset <= r; ++xOffset) {
                for (int zOffset = -r; zOffset <= r; ++zOffset) {
                    if (Math.abs(xOffset) != r && Math.abs(zOffset) != r) {
                        continue;
                    }
                    mutable.set(origin.getX() + xOffset, origin.getY(), origin.getZ() + zOffset);
                    BlockPos landing = this.findLandingSurface(mutable, 10);
                    if (landing != null) {
                        return new Vec3((double)landing.getX() + 0.5, (double)landing.getY() + 0.05, (double)landing.getZ() + 0.5);
                    }
                }
            }
        }
        return null;
    }

    private BlockPos findLandingSurface(BlockPos center, int verticalRange) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int yOffset = verticalRange; yOffset >= -verticalRange; --yOffset) {
            mutable.set(center.getX(), center.getY() + yOffset, center.getZ());
            if (this.isSafeShortFlightLanding(mutable)) {
                return mutable.immutable();
            }
        }
        return null;
    }

    private boolean isSafeShortFlightLanding(BlockPos pos) {
        return BirdFlightTargeting.isSafeDryLanding(this, pos);
    }

    private double scoreShortFlightLanding(BlockPos pos, Vec3 threatPosition, boolean escape) {
        BlockState below = this.level().getBlockState(pos.below());
        double score = 0.0;
        if (below.is(Blocks.FARMLAND)) {
            score += 6.0;
        }
        if (below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS)) {
            score += 11.0;
        }
        if (below.getBlock() instanceof FenceBlock || below.getBlock() instanceof FenceGateBlock) {
            score += 12.0;
        }
        if (below.is(Blocks.HAY_BLOCK) || below.is(Blocks.COMPOSTER)) {
            score += 9.0;
        }
        if (below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(BlockTags.DIRT)) {
            score += 2.5;
        }
        score -= Math.abs((double)pos.getY() - this.getY()) * 0.35;
        if (escape && threatPosition != null) {
            score += Math.min(16.0, new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5).distanceTo(threatPosition) * 0.6);
        }
        return score;
    }

    private boolean canReadChunk(BlockPos pos) {
        return this.level().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    private boolean tryTeleportNearOwner(LivingEntity owner) {
        BlockPos ownerPos = owner.blockPosition();
        for (int attempt = 0; attempt < 12; ++attempt) {
            int xOffset = this.randomBetween(-4, 4);
            int zOffset = this.randomBetween(-4, 4);
            if (Math.abs(xOffset) < 2 && Math.abs(zOffset) < 2) {
                continue;
            }
            BlockPos candidate = ownerPos.offset(xOffset, 0, zOffset);
            for (int yOffset = 2; yOffset >= -3; --yOffset) {
                BlockPos landing = candidate.offset(0, yOffset, 0);
                if (!this.isSafeOwnerTeleportPosition(landing)) {
                    continue;
                }
                this.moveTo((double)landing.getX() + 0.5, landing.getY(), (double)landing.getZ() + 0.5, this.getYRot(), this.getXRot());
                this.setDeltaMovement(Vec3.ZERO);
                this.getNavigation().stop();
                this.fallDistance = 0.0f;
                this.setBehaviorStateFor(SparrowBehaviorState.FOLLOWING_OWNER, 20);
                return true;
            }
        }
        return false;
    }

    private boolean isSafeOwnerTeleportPosition(BlockPos pos) {
        if (!this.canReadChunk(pos) || !this.canReadChunk(pos.above())) {
            return false;
        }
        Level level = this.level();
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        if ((!below.isFaceSturdy(level, belowPos, Direction.UP) && !below.is(BlockTags.LEAVES)) || below.is(Blocks.CACTUS) || below.is(Blocks.MAGMA_BLOCK)) {
            return false;
        }
        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) {
            return false;
        }
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    private void faceMovement(Vec3 movement) {
        BirdFlightController.faceMovement(this, movement, FLIGHT_PROFILE.maxPitchDegrees());
    }

    private int randomBetween(int min, int max) {
        return min + this.getRandom().nextInt(max - min + 1);
    }

    private double randomSigned(double range) {
        return (this.getRandom().nextDouble() * 2.0 - 1.0) * range;
    }

    private boolean isComfortableNear(Player player) {
        if (BirdConfigManager.aprilFoolsMode()) {
            return true;
        }
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        if (this.isDistrusted(player)) {
            return false;
        }
        if (this.isTame()) {
            return true;
        }
        if (isCleanTamingItem(player.getMainHandItem()) || isCleanTamingItem(player.getOffhandItem())) {
            return true;
        }
        if (player.getMainHandItem().is(GuaniaoItems.BREADCRUMBS.get()) || player.getOffhandItem().is(GuaniaoItems.BREADCRUMBS.get())) {
            return true;
        }
        if (this.calmAroundPlayerTicks > 0) {
            return true;
        }
        float calmChance = 0.7f + Mth.clamp((float)this.familiarTicks / 3600.0f, 0.0f, 0.3f);
        if (this.getRandom().nextFloat() < calmChance) {
            this.calmAroundPlayerTicks = 80 + this.getRandom().nextInt(100);
            return true;
        }
        return false;
    }

    protected TagKey<Item> foodTag() {
        return BirdTags.SPARROW_FOODS;
    }

    protected boolean isCleanTamingItem(ItemStack stack) {
        return BirdFoodSafety.matchesClean(this.foodTag(), stack);
    }

    protected boolean usesSparrowTemptGoal() {
        return true;
    }

    protected boolean usesSparrowBreedGoal() {
        return true;
    }

    protected boolean usesSparrowBreadcrumbGoal() {
        return true;
    }

    protected boolean usesSparrowPerchGoal() {
        return true;
    }

    protected boolean usesSparrowFlockGoal() {
        return true;
    }

    protected boolean usesSparrowSettlementHome() {
        return true;
    }

    private boolean shouldAvoidBreadcrumbs() {
        if (this.pendingScareTicks > 0 || this.isControlledFlightActive()) {
            return true;
        }
        Player player = this.level().getNearestPlayer(this, this.hasDistrustMemory() ? 12.0 : 7.0);
        return player != null && !this.isComfortableNear(player);
    }

    private void gainBreadcrumbConfidence() {
        this.familiarTicks = Math.min(MAX_FAMILIAR_TICKS, this.familiarTicks + 160);
        this.calmAroundPlayerTicks = Math.max(this.calmAroundPlayerTicks, 160);
    }

    private boolean isHungry() {
        return this.satiatedTicks <= 0;
    }

    private void restoreBreadcrumbSatiation() {
        this.satiatedTicks = Math.min(MAX_SATIATION_TICKS, this.satiatedTicks + FULL_SATIATION_TICKS);
    }

    private boolean hasBreadcrumbInterest() {
        return this.breadcrumbInterestTicks > 0 && this.noticedBreadcrumbPos != null;
    }

    private void noticeBreadcrumbs(BlockPos pos, Player player) {
        if (player != null && this.isDistrusted(player)) {
            return;
        }
        this.noticedBreadcrumbPos = pos.immutable();
        this.breadcrumbInterestTicks = Math.max(this.breadcrumbInterestTicks, 160 + this.getRandom().nextInt(120));
        this.satiatedTicks = Math.min(this.satiatedTicks, 700);
        this.calmAroundPlayerTicks = Math.max(this.calmAroundPlayerTicks, 60);
    }

    public static void alertNearbyBreadcrumbs(ServerLevel level, BlockPos pos, Player player) {
        Vec3 center = Vec3.atCenterOf(pos);
        List<SparrowEntity> flock = level.getEntitiesOfClass(SparrowEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(20.0),
                sparrow -> sparrow.getType() == GuaniaoEntityTypes.SPARROW.get());
        for (SparrowEntity sparrow : flock) {
            if (sparrow.isRemoved() || sparrow.isInWaterOrBubble()) {
                continue;
            }
            double distance = sparrow.position().distanceTo(center);
            float chance = distance <= 6.0 ? 1.0f : distance <= 12.0 ? 0.82f : 0.58f;
            if (sparrow.getRandom().nextFloat() > chance) {
                continue;
            }
            sparrow.noticeBreadcrumbs(pos, player);
        }
    }

    private BlockPos findNearbyBreadcrumbs(int horizontalRadius, int verticalRadius) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        BlockPos bestPos = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        BlockPos origin = this.blockPosition();
        for (BlockPos candidate : BreadcrumbSearchCache.nearby(this, horizontalRadius, verticalRadius)) {
            if (Math.abs(candidate.getY() - origin.getY()) > verticalRadius
                    || Math.abs(candidate.getX() - origin.getX()) > horizontalRadius
                    || Math.abs(candidate.getZ() - origin.getZ()) > horizontalRadius) {
                continue;
            }
            BlockState state = this.level().getBlockState(candidate);
            if (!state.is(GuaniaoBlocks.BREADCRUMBS.get())) {
                BreadcrumbSearchCache.remove(serverLevel, candidate);
                continue;
            }
            double distanceSqr = this.position().distanceToSqr(Vec3.atCenterOf(candidate));
            int layers = state.getValue(BreadcrumbPileBlock.LAYERS);
            int crowded = BreadcrumbSearchCache.userCount(serverLevel, candidate, this.getUUID());
            double crowdedPenalty = crowded * 1.35D;
            double score = (double)(layers * 6) - distanceSqr * 0.14 - crowdedPenalty;
            if (bestPos == null || score > bestScore) {
                bestPos = candidate;
                bestScore = score;
            }
        }
        return bestPos;
    }

    private Vec3 breadcrumbStandPosition(BlockPos pilePos) {
        double angle = ((double)(this.getId() & 7) / 8.0) * Math.PI * 2.0;
        double radius = 0.45 + (double)(this.getId() % 3) * 0.08;
        return new Vec3((double)pilePos.getX() + 0.5 + Math.cos(angle) * radius, (double)pilePos.getY(), (double)pilePos.getZ() + 0.5 + Math.sin(angle) * radius);
    }

    private boolean isDistrusted(Player player) {
        if (this.isOwnedBy(player)) {
            return false;
        }
        return this.distrustedPlayer != null && this.distrustTicks > 0 && this.distrustedPlayer.equals(player.getUUID());
    }

    private boolean hasDistrustMemory() {
        return this.distrustedPlayer != null && this.distrustTicks > 0;
    }

    private void rememberDistrustedPlayer(Player player) {
        this.distrustedPlayer = player.getUUID();
        this.distrustTicks = ATTACK_DISTRUST_TICKS;
        this.familiarTicks = 0;
        this.calmAroundPlayerTicks = 0;
    }

    private void queueScareReaction(Vec3 sourcePosition, int delayTicks, ScareReaction reaction) {
        if (delayTicks <= 0) {
            this.pendingScareSource = sourcePosition;
            this.pendingScareTicks = 1;
            this.pendingScareReaction = reaction;
            return;
        }
        if (this.pendingScareTicks == 0 || delayTicks < this.pendingScareTicks) {
            this.pendingScareSource = sourcePosition;
            this.pendingScareTicks = delayTicks;
            this.pendingScareReaction = reaction;
        }
    }

    private void releasePendingScare() {
        Vec3 sourcePosition = this.pendingScareSource;
        ScareReaction reaction = this.pendingScareReaction;
        this.pendingScareSource = null;
        this.pendingScareTicks = 0;
        this.pendingScareReaction = ScareReaction.ESCAPE_FLIGHT;
        if (sourcePosition == null || this.isRemoved()) {
            return;
        }
        this.getNavigation().stop();
        if (this.isTame()) {
            this.triggerLookAround();
            return;
        }
        if (reaction == ScareReaction.LOOK_AROUND) {
            this.getLookControl().setLookAt(sourcePosition.x, sourcePosition.y + 0.4, sourcePosition.z, 30.0f, 30.0f);
            this.triggerLookAround();
            return;
        }
        if (reaction == ScareReaction.SHORT_HOP) {
            this.triggerLookAround();
            Vec3 away = DefaultRandomPos.getPosAway(this, 5, 3, sourcePosition);
            if (away != null) {
                this.getNavigation().moveTo(away.x, away.y, away.z, 1.05);
            }
            if (this.onGround()) {
                this.shortHop();
            }
            return;
        }
        if (!this.startEscapeFlight(sourcePosition)) {
            Vec3 away = DefaultRandomPos.getPosAway(this, 12, 7, sourcePosition);
            if (away != null) {
                this.setBehaviorStateFor(SparrowBehaviorState.FLEEING, 50);
                this.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
                if (this.onGround() && this.getRandom().nextBoolean()) {
                    this.shortHop();
                }
            }
        }
    }

    private void alertNearbySparrows(Entity source) {
        Vec3 sourcePosition = source.position();
        Player attacker = source instanceof Player player ? player : null;
        List<SparrowEntity> flock = BirdFlockManager.nearby(this, SparrowEntity.class, 18.0);
        for (SparrowEntity sparrow : flock) {
            if (sparrow != this && !sparrow.isTame()) {
                sparrow.familiarTicks = 0;
                sparrow.calmAroundPlayerTicks = 0;
                if (attacker != null) {
                    sparrow.rememberDistrustedPlayer(attacker);
                }
                sparrow.birdBrain.onFrightened(attacker != null ? 0.40F : 0.25F);
                sparrow.getNavigation().stop();
                double distance = sparrow.position().distanceTo(sourcePosition);
                int delay = SparrowEntity.scareDelayForDistance(sparrow, distance);
                ScareReaction reaction = SparrowEntity.scareReactionForDistance(sparrow, distance, attacker != null);
                sparrow.queueScareReaction(sourcePosition, delay, reaction);
            }
        }
    }

    private static int scareDelayForDistance(SparrowEntity sparrow, double distance) {
        int delay = 3 + Mth.floor(distance * 0.62);
        delay += sparrow.getRandom().nextInt(7);
        if (distance < 3.0) {
            delay = 1 + sparrow.getRandom().nextInt(3);
        }
        return Mth.clamp(delay, 1, 22);
    }

    private static ScareReaction scareReactionForDistance(SparrowEntity sparrow, double distance, boolean severe) {
        float familiar = Mth.clamp((float)sparrow.familiarTicks / 3600.0f, 0.0f, 1.0f);
        float escapeChance;
        if (distance < 3.0) {
            escapeChance = severe ? 1.0f : 0.86f;
        } else if (distance < 8.0) {
            escapeChance = severe ? 0.82f : 0.55f;
        } else {
            escapeChance = severe ? 0.52f : 0.24f;
        }
        escapeChance -= familiar * 0.22f;
        float roll = sparrow.getRandom().nextFloat();
        if (roll < escapeChance) {
            return ScareReaction.ESCAPE_FLIGHT;
        }
        if (roll < escapeChance + (distance < 8.0 ? 0.35f : 0.28f)) {
            return ScareReaction.SHORT_HOP;
        }
        return ScareReaction.LOOK_AROUND;
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
        BirdIntent intent = this.birdBrain.currentIntent();
        float fear = this.birdBrain.motivation().fear();
        float alertness = this.birdBrain.motivation().alertness();
        int baseWeight = 2;
        int tailWeight = 1;
        int peckWeight = 1;
        int lookWeight = 1;
        if (intent == BirdIntent.FORAGE) {
            peckWeight += 3;
            lookWeight += 1;
        } else if (intent == BirdIntent.WATCH || intent == BirdIntent.ALERT) {
            lookWeight += 3;
            peckWeight = Math.max(0, peckWeight - 1);
        } else if (intent == BirdIntent.ROOST) {
            baseWeight += 3;
            tailWeight += 2;
            peckWeight = 0;
        }
        if (fear > 0.45F || alertness > 0.45F) {
            lookWeight += 2;
            peckWeight = Math.max(0, peckWeight - 1);
        }
        int total = baseWeight + tailWeight + peckWeight + lookWeight;
        int roll = this.getRandom().nextInt(Math.max(1, total));
        if ((roll -= baseWeight) < 0) {
            return IdleAnimationChoice.BASE;
        }
        if ((roll -= tailWeight) < 0) {
            return IdleAnimationChoice.TAIL;
        }
        if ((roll -= lookWeight) < 0) {
            return IdleAnimationChoice.LOOK_AROUND;
        }
        return IdleAnimationChoice.PECK;
    }

    private boolean shouldPlayFlyAnimation() {
        return BirdFlightController.shouldPlayFlyAnimation(
                this,
                this.getBehaviorState().isAirborne(),
                this.onGround(),
                this.isNoGravity(),
                this.getDeltaMovement(),
                this.airborneFlightAnimationTicks);
    }

    private boolean shouldPlayWalkAnimation(SparrowBehaviorState state, boolean animationMoving) {
        if (!BirdGroundAnimation.hasWalkMotion(this, animationMoving)) {
            return false;
        }
        return !state.isAirborne()
                && state != SparrowBehaviorState.PERCHING
                && state != SparrowBehaviorState.ROOSTING
                && state != SparrowBehaviorState.SHORT_FLIGHT;
    }

    private <T extends SparrowEntity> PlayState movementController(AnimationState<T> animationState) {
        RawAnimation guidePreviewRawAnimation = this.guidePreviewAnimation.animation();
        if (guidePreviewRawAnimation != null) {
            return animationState.setAndContinue(guidePreviewRawAnimation);
        }
        SparrowBehaviorState state = this.getBehaviorState();
        if (this.shouldPlayFlyAnimation()) {
            return animationState.setAndContinue(FLY_ANIMATION);
        }
        if (this.shouldPlayWalkAnimation(state, animationState.isMoving())) {
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        if (state == SparrowBehaviorState.PECKING) {
            return animationState.setAndContinue(PECK_ANIMATION);
        }
        if (state == SparrowBehaviorState.LOOK_AROUND || state == SparrowBehaviorState.ALERT) {
            return animationState.setAndContinue(LOOK_AROUND_ANIMATION);
        }
        if (state == SparrowBehaviorState.PERCHING || state == SparrowBehaviorState.ROOSTING) {
            return animationState.setAndContinue(this.currentIdleAnimation == IdleAnimationChoice.TAIL ? TAIL_ANIMATION : IDLE_ANIMATION);
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

    private static final class SparrowFollowOwnerGoal extends Goal {
        private final SparrowEntity sparrow;
        private final double speedModifier;
        private final float startDistance;
        private final float stopDistance;
        private LivingEntity owner;
        private int teleportAttemptTicks;

        private SparrowFollowOwnerGoal(SparrowEntity sparrow, double speedModifier, float stopDistance, float startDistance) {
            this.sparrow = sparrow;
            this.speedModifier = speedModifier;
            this.stopDistance = stopDistance;
            this.startDistance = startDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = this.sparrow.getOwner();
            if (!this.canFollowOwner(owner)) {
                return false;
            }
            this.owner = owner;
            return this.sparrow.distanceToSqr(owner) > (double)(this.startDistance * this.startDistance);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canFollowOwner(this.owner)
                    && this.sparrow.distanceToSqr(this.owner) > (double)(this.stopDistance * this.stopDistance);
        }

        @Override
        public void start() {
            this.teleportAttemptTicks = 0;
            this.sparrow.setBehaviorState(SparrowBehaviorState.FOLLOWING_OWNER);
        }

        @Override
        public void tick() {
            if (this.owner == null) {
                return;
            }
            this.sparrow.getLookControl().setLookAt(this.owner, 10.0f, this.sparrow.getMaxHeadXRot());
            this.sparrow.setBehaviorState(SparrowBehaviorState.FOLLOWING_OWNER);
            double distanceSqr = this.sparrow.distanceToSqr(this.owner);
            if (distanceSqr > 64.0 && this.sparrow.onGround() && this.sparrow.flightCooldown <= 0) {
                Vec3 target = BirdFlightTargeting.findDryLandingTargetNear(this.sparrow, this.owner.blockPosition(), 4, 8);
                if (target != null && this.sparrow.startControlledFlight(target, this.sparrow.randomBetween(46, 72), SHORT_FLIGHT_SPEED + 0.04D, false)) {
                    return;
                }
            }
            double teleportDistance = BirdConfigManager.ownerTeleportDistance(BirdSpecies.from(this.sparrow));
            if (distanceSqr > teleportDistance * teleportDistance && this.sparrow.ownerFollowSuppressedTicks <= 0) {
                if (--this.teleportAttemptTicks <= 0) {
                    this.teleportAttemptTicks = 20;
                    if (this.sparrow.tryTeleportNearOwner(this.owner)) {
                        return;
                    }
                }
            }
            this.sparrow.getNavigation().moveTo(this.owner, this.speedModifier);
        }

        @Override
        public void stop() {
            this.owner = null;
            this.teleportAttemptTicks = 0;
            if (this.sparrow.getBehaviorState() == SparrowBehaviorState.FOLLOWING_OWNER) {
                this.sparrow.setBehaviorState(SparrowBehaviorState.IDLE);
            }
        }

        private boolean canFollowOwner(LivingEntity owner) {
            if (!this.sparrow.isTame()
                    || owner == null
                    || !owner.isAlive()
                    || !this.sparrow.isBirdCommandMode(BirdCommandMode.FOLLOW)
                    || this.sparrow.ownerFollowSuppressedTicks > 0
                    || this.sparrow.isControlledFlightActive()
                    || this.sparrow.pendingScareTicks > 0) {
                return false;
            }
            if (owner instanceof Player player && player.isSpectator()) {
                return false;
            }
            SparrowBehaviorState state = this.sparrow.getBehaviorState();
            return state != SparrowBehaviorState.PERCHING
                    && state != SparrowBehaviorState.ROOSTING
                    && !state.isEscape();
        }
    }

    private static final class SparrowFleePlayerGoal extends Goal {
        private final SparrowEntity sparrow;
        private Player player;
        private Vec3 fleeTarget;

        private SparrowFleePlayerGoal(SparrowEntity sparrow) {
            this.sparrow = sparrow;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.sparrow.pendingScareTicks > 0) {
                return false;
            }
            double senseRange = this.sparrow.hasDistrustMemory() ? 11.0 : 6.5;
            if (this.sparrow.brainWantsEscapeOrAlert()) {
                senseRange += 2.0;
            }
            this.player = this.sparrow.level().getNearestPlayer(this.sparrow, senseRange);
            if (this.player == null || this.sparrow.isComfortableNear(this.player)) {
                return false;
            }
            boolean strongFlee = this.sparrow.isDistrusted(this.player) || this.sparrow.birdBrain().wantsLongEscape();
            this.fleeTarget = DefaultRandomPos.getPosAway(this.sparrow, strongFlee ? 14 : 9, strongFlee ? 7 : 5, this.player.position());
            return this.fleeTarget != null;
        }

        @Override
        public void start() {
            this.sparrow.setBehaviorStateFor(SparrowBehaviorState.FLEEING, 60);
            if (this.sparrow.isDistrusted(this.player)) {
                this.sparrow.alertNearbySparrows(this.player);
            }
            if (!this.sparrow.startEscapeFlight(this.player.position())) {
                this.sparrow.getNavigation().moveTo(this.fleeTarget.x, this.fleeTarget.y, this.fleeTarget.z, 1.18);
                this.sparrow.shortHop();
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !BirdConfigManager.aprilFoolsMode()
                    && (this.sparrow.isControlledFlightActive() || !this.sparrow.getNavigation().isDone());
        }

        @Override
        public void stop() {
            if (this.sparrow.getBehaviorState().isEscape() && !this.sparrow.isControlledFlightActive()) {
                this.sparrow.behaviorStateLockTicks = 0;
                this.sparrow.setBehaviorState(SparrowBehaviorState.IDLE);
            }
        }
    }

    private static final class SparrowPerchGoal extends Goal {
        private final SparrowEntity sparrow;
        private BlockPos perchPos;
        private boolean roosting;
        private int remainingTicks;
        private int repositionTicks;

        private SparrowPerchGoal(SparrowEntity sparrow) {
            this.sparrow = sparrow;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.sparrow.pendingScareTicks > 0 || this.sparrow.isControlledFlightActive() || this.sparrow.isInWaterOrBubble() || this.sparrow.getTarget() != null) {
                return false;
            }
            boolean brainRoost = this.sparrow.brainWantsRoost();
            this.roosting = this.sparrow.shouldSeekNightRoost() || brainRoost;
            if (!this.roosting) {
                int chance = brainRoost ? 80 : (this.sparrow.isTame() ? 280 : 190);
                if (!this.sparrow.onGround() || this.sparrow.perchCooldown > 0 || !this.sparrow.getNavigation().isDone() || this.sparrow.getRandom().nextInt(chance) != 0) {
                    return false;
                }
            } else if (this.sparrow.perchCooldown > 0 && this.sparrow.getRandom().nextInt(4) != 0) {
                return false;
            }
            long now = this.sparrow.level().getGameTime();
            if (now < this.sparrow.nextPerchSearchTick) {
                return false;
            }
            this.sparrow.nextPerchSearchTick = now + 40L + this.sparrow.getRandom().nextInt(41);
            if (this.sparrow.level() instanceof ServerLevel serverLevel
                    && !BirdScanBudget.tryAcquire(serverLevel, this.sparrow, PERCH_SCAN_BUDGET_COST)) {
                this.sparrow.nextPerchSearchTick = now + 6L + Math.floorMod(this.sparrow.getId(), 13);
                return false;
            }
            this.perchPos = this.sparrow.findPerchTarget(this.roosting);
            if (this.perchPos == null) {
                this.sparrow.nextPerchSearchTick = now + 80L + this.sparrow.getRandom().nextInt(81);
            }
            return this.perchPos != null;
        }

        @Override
        public void start() {
            this.remainingTicks = this.roosting ? this.sparrow.randomBetween(360, 760) : this.sparrow.randomBetween(90, 190);
            this.repositionTicks = 0;
            this.sparrow.setBehaviorState(this.roosting ? SparrowBehaviorState.ROOSTING : SparrowBehaviorState.PERCHING);
            this.moveToPerch();
        }

        @Override
        public boolean canContinueToUse() {
            if (this.perchPos == null || this.remainingTicks <= 0 || this.sparrow.pendingScareTicks > 0 || this.sparrow.isInWaterOrBubble()) {
                return false;
            }
            if (!this.sparrow.isSafePerchPosition(this.perchPos)) {
                return false;
            }
            return this.roosting ? (this.sparrow.shouldSeekNightRoost() || this.sparrow.brainWantsRoost()) : !this.sparrow.shouldSeekNightRoost();
        }

        @Override
        public void tick() {
            --this.remainingTicks;
            if (this.perchPos == null) {
                return;
            }
            this.sparrow.setBehaviorState(this.roosting ? SparrowBehaviorState.ROOSTING : SparrowBehaviorState.PERCHING);
            if (this.sparrow.isControlledFlightActive()) {
                this.sparrow.getLookControl().setLookAt((double)this.perchPos.getX() + 0.5, (double)this.perchPos.getY() + 0.2, (double)this.perchPos.getZ() + 0.5, 20.0f, 20.0f);
                return;
            }
            double distanceSqr = this.sparrow.position().distanceToSqr(Vec3.atBottomCenterOf(this.perchPos));
            if (distanceSqr > 1.35) {
                if (--this.repositionTicks <= 0 || this.sparrow.getNavigation().isDone()) {
                    this.moveToPerch();
                }
                return;
            }
            this.sparrow.getNavigation().stop();
            this.sparrow.getLookControl().setLookAt((double)this.perchPos.getX() + 0.5 + this.sparrow.randomSigned(0.8), (double)this.perchPos.getY() + 0.4, (double)this.perchPos.getZ() + 0.5 + this.sparrow.randomSigned(0.8), 12.0f, 12.0f);
            if (this.remainingTicks % 80 == 0 && this.sparrow.getRandom().nextInt(3) == 0) {
                this.sparrow.triggerTailFlick();
            } else if (this.remainingTicks % 95 == 0 && this.sparrow.getRandom().nextInt(4) == 0) {
                this.sparrow.triggerLookAround();
            }
            if (this.remainingTicks % 80 == 0) {
                this.sparrow.birdBrain().onRest(0.02F);
            }
        }

        @Override
        public void stop() {
            this.perchPos = null;
            this.remainingTicks = 0;
            this.repositionTicks = 0;
            this.sparrow.perchCooldown = this.roosting ? 60 + this.sparrow.getRandom().nextInt(80) : 420 + this.sparrow.getRandom().nextInt(360);
            if (this.sparrow.getBehaviorState() == SparrowBehaviorState.PERCHING || this.sparrow.getBehaviorState() == SparrowBehaviorState.ROOSTING) {
                this.sparrow.setBehaviorState(SparrowBehaviorState.IDLE);
            }
        }

        private void moveToPerch() {
            if (this.perchPos == null) {
                return;
            }
            this.repositionTicks = 24;
            Vec3 target = Vec3.atBottomCenterOf(this.perchPos);
            double distanceSqr = this.sparrow.position().distanceToSqr(target);
            boolean highTarget = this.perchPos.getY() > this.sparrow.blockPosition().getY() + 2;
            boolean shouldFlyToPerch = this.roosting
                    && this.sparrow.onGround()
                    && this.sparrow.flightCooldown <= 0
                    && (distanceSqr > 144.0 || highTarget);
            if (shouldFlyToPerch) {
                if (this.sparrow.startControlledFlight(new Vec3(target.x, target.y + 0.04, target.z), this.sparrow.randomBetween(26, 48), SHORT_FLIGHT_SPEED + (this.roosting ? 0.05 : 0.02), false)) {
                    return;
                }
            }
            this.sparrow.getNavigation().moveTo(target.x, target.y, target.z, this.roosting ? 0.96 : 0.84);
        }
    }

    private static final class SparrowFlockGoal extends Goal {
        private final SparrowEntity sparrow;
        private Vec3 target;

        private SparrowFlockGoal(SparrowEntity sparrow) {
            this.sparrow = sparrow;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            float sociability = this.sparrow.birdBrain().personality().sociability();
            int chance = Mth.clamp((int)(70.0F - sociability * 55.0F), 12, 70);
            if (this.sparrow.isTame() || this.sparrow.getRandom().nextInt(chance) != 0) {
                return false;
            }
            List<SparrowEntity> flock = BirdFlockManager.nearby(this.sparrow, SparrowEntity.class, SparrowDefinition.SOCIAL_RADIUS)
                    .stream().filter(other -> other != this.sparrow && !other.isTame()).toList();
            if (flock.isEmpty()) {
                return false;
            }
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;
            for (SparrowEntity other : flock) {
                x += other.getX();
                y += other.getY();
                z += other.getZ();
            }
            Vec3 center = new Vec3(x / (double)flock.size(), y / (double)flock.size(), z / (double)flock.size());
            if (this.sparrow.position().distanceToSqr(center) < 9.0) {
                return false;
            }
            if (sociability < 0.4F && this.sparrow.position().distanceToSqr(center) < 25.0 && this.sparrow.getRandom().nextBoolean()) {
                return false;
            }
            this.target = center;
            return true;
        }

        @Override
        public void start() {
            this.sparrow.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, 0.82);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.sparrow.getNavigation().isDone();
        }
    }

    private static final class SparrowEatBreadcrumbGoal extends Goal {
        private final SparrowEntity sparrow;
        private BlockPos pilePos;
        private BlockPos cachedPilePos;
        private Vec3 standPos;
        private int nextPeckTicks;
        private long cachedPileUntilTick;
        private long nextClaimRefreshTick;
        private long nextSearchTick;
        private boolean searchStaggerInitialized;
        private boolean claimedPile;

        private SparrowEatBreadcrumbGoal(SparrowEntity sparrow) {
            this.sparrow = sparrow;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if ((!this.sparrow.isHungry() && !this.sparrow.hasBreadcrumbInterest()) || this.sparrow.shouldAvoidBreadcrumbs() || this.sparrow.isInWaterOrBubble()) {
                return false;
            }
            long now = this.sparrow.level().getGameTime();
            if (!this.searchStaggerInitialized) {
                this.searchStaggerInitialized = true;
                this.nextSearchTick = now + this.sparrow.getRandom().nextInt(41);
            }
            this.pilePos = null;
            if (this.sparrow.hasBreadcrumbInterest() && this.sparrow.noticedBreadcrumbPos != null
                    && this.isValidPile(this.sparrow.noticedBreadcrumbPos)) {
                this.pilePos = this.sparrow.noticedBreadcrumbPos.immutable();
            }
            if (this.pilePos == null && !this.sparrow.hasBreadcrumbInterest() && now < this.nextSearchTick) {
                return false;
            }
            if (this.pilePos == null && this.cachedPilePos != null && now <= this.cachedPileUntilTick
                    && this.isValidPile(this.cachedPilePos)) {
                this.pilePos = this.cachedPilePos;
            }
            if (this.pilePos == null) {
                this.pilePos = this.sparrow.findNearbyBreadcrumbs(this.sparrow.hasBreadcrumbInterest() ? 20 : 14, 3);
            }
            if (this.pilePos == null) {
                this.nextSearchTick = now + 40L + this.sparrow.getRandom().nextInt(41);
                return false;
            }
            this.cachedPilePos = this.pilePos;
            this.cachedPileUntilTick = now + 120L;
            if (!this.sparrow.hasBreadcrumbInterest() && !this.sparrow.brainWantsForage() && this.sparrow.getRandom().nextInt(3) != 0) {
                this.nextSearchTick = now + 20L + this.sparrow.getRandom().nextInt(21);
                return false;
            }
            this.standPos = this.sparrow.breadcrumbStandPosition(this.pilePos);
            if (this.sparrow.level() instanceof ServerLevel serverLevel) {
                BreadcrumbSearchCache.register(serverLevel, this.pilePos);
                BreadcrumbSearchCache.claim(serverLevel, this.pilePos, this.sparrow.getUUID());
                this.nextClaimRefreshTick = now + 40L;
                this.claimedPile = true;
            }
            return true;
        }

        @Override
        public void start() {
            this.nextPeckTicks = this.sparrow.randomBetween(8, 14);
            this.sparrow.setBehaviorState(SparrowBehaviorState.FORAGING);
            this.moveTowardsPile();
        }

        @Override
        public void tick() {
            if (this.pilePos == null) {
                return;
            }
            long now = this.sparrow.level().getGameTime();
            if (now >= this.nextClaimRefreshTick && this.sparrow.level() instanceof ServerLevel serverLevel) {
                BreadcrumbSearchCache.claim(serverLevel, this.pilePos, this.sparrow.getUUID());
                this.nextClaimRefreshTick = now + 40L;
                this.claimedPile = true;
            }
            if (!this.sparrow.isControlledFlightActive() && this.sparrow.getBehaviorState() != SparrowBehaviorState.PECKING) {
                this.sparrow.setBehaviorState(SparrowBehaviorState.FORAGING);
            }
            BlockState state = this.sparrow.level().getBlockState(this.pilePos);
            if (!state.is(GuaniaoBlocks.BREADCRUMBS.get())) {
                this.forgetInvalidPile();
                this.stop();
                return;
            }
            if (this.sparrow.shouldAvoidBreadcrumbs()) {
                this.stop();
                return;
            }

            this.sparrow.getLookControl().setLookAt((double)this.pilePos.getX() + 0.5, (double)this.pilePos.getY() + 0.2, (double)this.pilePos.getZ() + 0.5, 20.0f, 20.0f);
            double distanceToPile = this.sparrow.position().distanceToSqr(Vec3.atCenterOf(this.pilePos));
            if (distanceToPile > 3.1) {
                this.moveTowardsPile();
                return;
            }

            this.sparrow.getNavigation().stop();
            if (this.sparrow.isControlledFlightActive()) {
                return;
            }

            if (--this.nextPeckTicks > 0) {
                return;
            }

            if (this.sparrow.getRandom().nextFloat() < 0.28f) {
                this.sparrow.triggerLookAround();
                this.nextPeckTicks = this.sparrow.randomBetween(12, 20);
                return;
            }

            this.sparrow.triggerPeck();
            this.sparrow.gainBreadcrumbConfidence();
            if (state.getBlock() instanceof BreadcrumbPileBlock breadcrumbPileBlock) {
                if (breadcrumbPileBlock.consumeOneServing(this.sparrow.level(), this.pilePos, state)) {
                    this.sparrow.restoreBreadcrumbSatiation();
                    this.sparrow.birdBrain().onEat(0.35F);
                }
                if (!this.sparrow.level().getBlockState(this.pilePos).is(GuaniaoBlocks.BREADCRUMBS.get())) {
                    this.forgetInvalidPile();
                    this.stop();
                    return;
                }
            }
            this.nextPeckTicks = this.sparrow.randomBetween(10, 16);
        }

        private void moveTowardsPile() {
            if (this.standPos == null) {
                this.standPos = this.sparrow.breadcrumbStandPosition(this.pilePos);
            }
            if (this.sparrow.getBehaviorState() != SparrowBehaviorState.PECKING) {
                this.sparrow.setBehaviorState(SparrowBehaviorState.FORAGING);
            }
            this.sparrow.flightCooldown = Math.max(this.sparrow.flightCooldown, 80);
            this.sparrow.getNavigation().moveTo(this.standPos.x, this.standPos.y, this.standPos.z, 0.98);
        }

        @Override
        public boolean canContinueToUse() {
            return this.pilePos != null && !this.sparrow.shouldAvoidBreadcrumbs();
        }

        @Override
        public void stop() {
            if (this.claimedPile && this.pilePos != null && this.sparrow.level() instanceof ServerLevel serverLevel) {
                BreadcrumbSearchCache.release(serverLevel, this.pilePos, this.sparrow.getUUID());
            }
            this.claimedPile = false;
            this.nextClaimRefreshTick = 0L;
            this.pilePos = null;
            this.standPos = null;
            this.nextPeckTicks = 0;
            if (this.sparrow.breadcrumbInterestTicks <= 0) {
                this.sparrow.noticedBreadcrumbPos = null;
            }
            this.sparrow.getNavigation().stop();
            this.sparrow.flightCooldown = Math.max(this.sparrow.flightCooldown, 20 + this.sparrow.getRandom().nextInt(41));
            this.nextSearchTick = Math.max(this.nextSearchTick,
                    this.sparrow.level().getGameTime() + 20L + this.sparrow.getRandom().nextInt(21));
            if (this.sparrow.getBehaviorState() == SparrowBehaviorState.FORAGING) {
                this.sparrow.setBehaviorStateFor(SparrowBehaviorState.LOOK_AROUND, 24);
            }
        }

        private boolean isValidPile(BlockPos pos) {
            if (pos == null || !(this.sparrow.level() instanceof ServerLevel serverLevel)) {
                return false;
            }
            if (!serverLevel.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                this.forgetKnownPile(pos);
                return false;
            }
            boolean valid = serverLevel.getBlockState(pos).is(GuaniaoBlocks.BREADCRUMBS.get());
            if (valid) {
                BreadcrumbSearchCache.register(serverLevel, pos);
            } else {
                BreadcrumbSearchCache.remove(serverLevel, pos);
                this.forgetKnownPile(pos);
            }
            return valid;
        }

        private void forgetInvalidPile() {
            if (this.pilePos != null && this.sparrow.level() instanceof ServerLevel serverLevel) {
                BreadcrumbSearchCache.remove(serverLevel, this.pilePos);
            }
            this.forgetKnownPile(this.pilePos);
        }

        private void forgetKnownPile(BlockPos pos) {
            if (pos != null && pos.equals(this.cachedPilePos)) {
                this.cachedPilePos = null;
                this.cachedPileUntilTick = 0L;
            }
            if (pos != null && pos.equals(this.sparrow.noticedBreadcrumbPos)) {
                this.sparrow.noticedBreadcrumbPos = null;
                this.sparrow.breadcrumbInterestTicks = 0;
            }
        }
    }

    private enum IdleAnimationChoice {
        BASE(IDLE_ANIMATION, 55, 115),
        TAIL(TAIL_ANIMATION, 48, 78),
        PECK(PECK_ANIMATION, 34, 58),
        LOOK_AROUND(LOOK_AROUND_ANIMATION, 58, 88);

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

    private enum ScareReaction {
        LOOK_AROUND,
        SHORT_HOP,
        ESCAPE_FLIGHT
    }

    public enum GuidePreviewAnimation {
        NONE,
        IDLE,
        TAIL,
        PECK,
        LOOK_AROUND,
        WALK,
        FLY;

        private RawAnimation animation() {
            return switch (this) {
                case NONE -> null;
                case IDLE -> IDLE_ANIMATION;
                case TAIL -> TAIL_ANIMATION;
                case PECK -> PECK_ANIMATION;
                case LOOK_AROUND -> LOOK_AROUND_ANIMATION;
                case WALK -> WALK_ANIMATION;
                case FLY -> FLY_ANIMATION;
            };
        }
    }
}
