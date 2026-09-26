package EdDYON.guaniao.content.bird.woodcock;

import EdDYON.guaniao.content.bird.flight.BirdFlightAnimation;
import EdDYON.guaniao.content.earthworm.EarthwormEntity;

import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.BirdLoudSoundListener;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.BirdSleepWakeable;
import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.CleanBirdTemptGoal;
import EdDYON.guaniao.content.bird.brain.BirdIntent;
import EdDYON.guaniao.content.bird.flight.BirdFlightController;
import EdDYON.guaniao.content.bird.flight.BirdFlightProfile;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.content.bird.species.WoodcockProfile;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;

/**
 * A solitary woodland ground bird. Its signature locomotion alternates two
 * short half-steps with rocking pauses, while longer purposeful routes use the
 * ordinary walk cycle inherited from the shared navigation foundation.
 */
public class WoodcockEntity extends SparrowEntity
        implements FlyingAnimal, BirdSleepWakeable, BirdLoudSoundListener {
    private static final EntityDataAccessor<Integer> ACTION_STATE =
            SynchedEntityData.defineId(WoodcockEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SPONTANEOUS_STILL =
            SynchedEntityData.defineId(WoodcockEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation STEP_1_ANIMATION = RawAnimation.begin().thenPlay("animation.walk1");
    private static final RawAnimation ROCK_1_ANIMATION = RawAnimation.begin().thenLoop("animation.walk1_idle");
    private static final RawAnimation STEP_2_ANIMATION = RawAnimation.begin().thenPlay("animation.walk2");
    private static final RawAnimation ROCK_2_ANIMATION = RawAnimation.begin().thenLoop("animation.walk2_idle");
    private static final RawAnimation EAT_ANIMATION = RawAnimation.begin().thenPlay("animation.eat");
    private static final RawAnimation IDLE_VARIATION_ANIMATION =
            RawAnimation.begin().thenPlay("animation.idle_diff_1");
    private static final RawAnimation PROBE_ANIMATION =
            RawAnimation.begin().thenPlay("animation.idle_diff_2");
    private static final RawAnimation FLY_ANIMATION = RawAnimation.begin().thenLoop("animation.fly");
    private static final RawAnimation SLEEP_ENTER_ANIMATION =
            RawAnimation.begin().thenPlay("animation.sleep");
    private static final RawAnimation SLEEP_LOOP_ANIMATION =
            RawAnimation.begin().thenLoop("animation.sleep_loop");

    private static final String TERRITORY_X_TAG = "WoodcockTerritoryX";
    private static final String TERRITORY_Y_TAG = "WoodcockTerritoryY";
    private static final String TERRITORY_Z_TAG = "WoodcockTerritoryZ";
    private static final String FORAGE_COOLDOWN_TAG = "WoodcockForageCooldown";
    private static final String SPAWN_AWAKE_TICKS_TAG = "WoodcockSpawnAwakeTicks";
    private static final String SLEEP_COOLDOWN_TAG = "WoodcockSleepCooldown";
    private static final int IDLE_VARIATION_TICKS = 70;
    private static final int SLEEP_ENTER_TICKS = 15;
    private static final int ROCK_WALK_STEP_TICKS = 4;
    private static final int ROCK_WALK_SWAY_LOOP_TICKS = 8;
    private static final int ROCK_WALK_MIN_SWAYS_PER_STEP = 2;
    private static final int ROCK_WALK_MAX_SWAYS_PER_STEP = 4;
    private static final int ROCK_WALK_MIN_CYCLES = 2;
    private static final int ROCK_WALK_MAX_CYCLES = 4;
    private static final double NORMAL_WALK_SPEED = 0.80D;
    private static final double ROCK_WALK_STEP_SPEED = 0.76D;
    private static final double ROCK_WALK_SWAY_SPEED = 0.0D;
    private static final double ROCK_WALK_ANIMATION_SPEED = 1.25D;
    private static final double NORMAL_WALK_ANIMATION_CADENCE = 1.15D;
    private static final double MIN_INDIVIDUAL_ANIMATION_CADENCE = 0.96D;
    private static final double INDIVIDUAL_ANIMATION_CADENCE_STEP = 0.005D;

    private int actionTicks;
    private int idleCooldown;
    private int alertCooldown;
    private int restInterruptionTicks;
    private int spawnAwakeTicks;
    private int sleepCooldown;
    private int sleepDecisionCooldown;
    private int manualEatTicks;
    private int roamCooldown;
    private int forageCooldown;
    private int forageScanCooldown;
    private int shelterScanCooldown;
    private int daytimeExposureTicks;
    private int daytimeExposureThresholdTicks;
    private int predatorScanCooldown;
    private int flightStateTicks;
    private int postLandingCoverTicks;
    private int nextFlightWeaveTick;
    private double flightWeaveBias;
    private boolean wasFlightActive;
    private boolean forcedRockWalkTest;
    private boolean loopingRockWalkTest;
    private boolean pursuingVisibleWorm;
    private boolean stationaryFacingLocked;
    private float stationaryLockedYaw;
    @Nullable
    private Vec3 forcedRockWalkTarget;
    @Nullable
    private BlockPos territoryCenter;
    @Nullable
    private Vec3 alertLookTarget;
    @Nullable
    private LivingEntity cachedPredator;
    private final WoodcockStillnessController stillnessController;
    private final WoodcockRockWalkSequencer rockWalkSequencer;

    public WoodcockEntity(EntityType<? extends WoodcockEntity> entityType, Level level) {
        super(entityType, level, WoodcockProfile.INSTANCE);
        this.stillnessController = new WoodcockStillnessController(this);
        this.rockWalkSequencer = new WoodcockRockWalkSequencer(this);
        this.idleCooldown = this.randomBetween(80, 180);
        this.roamCooldown = this.randomBetween(5, 25);
        this.forageScanCooldown = this.randomBetween(40, 100);
        this.shelterScanCooldown = this.randomBetween(30, 70);
        this.sleepDecisionCooldown = this.randomBetween(300, 600);
        this.daytimeExposureThresholdTicks = this.randomBetween(300, 600);
        this.predatorScanCooldown = this.randomBetween(12, 28);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, WoodcockDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, WoodcockDefinition.WALK_SPEED)
                .add(Attributes.FLYING_SPEED, WoodcockDefinition.FLYING_SPEED)
                .add(Attributes.FOLLOW_RANGE, WoodcockDefinition.FOLLOW_RANGE);
    }

    public static boolean canWoodcockSpawn(EntityType<WoodcockEntity> entityType, ServerLevelAccessor level,
                                            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean suitableGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(Blocks.PODZOL)
                || below.is(Blocks.MOSS_BLOCK)
                || below.is(Blocks.MUD)
                || below.is(Blocks.ROOTED_DIRT);
        if (!suitableGround || !level.getFluidState(pos).isEmpty()) {
            return false;
        }

        int cover = 0;
        int originChunkX = pos.getX() >> 4;
        int originChunkZ = pos.getZ() >> 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -5; x <= 5; ++x) {
            for (int z = -5; z <= 5; ++z) {
                for (int y = -1; y <= 4; ++y) {
                    cursor.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if ((cursor.getX() >> 4) != originChunkX || (cursor.getZ() >> 4) != originChunkZ) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                        cover += 2;
                    } else if (state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                            || state.is(Blocks.TALL_GRASS)) {
                        ++cover;
                    }
                    if (cover >= 18) {
                        return random.nextFloat() < 0.72F;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void registerGoals() {
        // Woodcock owns its complete goal list. Calling SparrowEntity here would also
        // install its roost/stay/follow goals, which can claim MOVE ahead of ground roaming.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new WoodcockFrightGoal(this));
        this.goalSelector.addGoal(1, new WoodcockSleepGoal(this));
        this.goalSelector.addGoal(2, new WoodcockEatWormGoal(this));
        this.goalSelector.addGoal(3, new WoodcockTemptGoal(this));
        this.goalSelector.addGoal(4, new WoodcockForageGoal(this));
        this.goalSelector.addGoal(5, new WoodcockSeekCoverGoal(this));
        this.goalSelector.addGoal(7, new WoodcockGroundRoamGoal(this));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION_STATE, WoodcockBehaviorState.IDLE.ordinal());
        builder.define(SPONTANEOUS_STILL, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        if (this.territoryCenter == null) {
            this.territoryCenter = this.blockPosition().immutable();
        }
        this.spawnAwakeTicks = Math.max(this.spawnAwakeTicks, this.randomBetween(600, 900));
        this.sleepCooldown = Math.max(this.sleepCooldown, this.randomBetween(600, 900));
        this.sleepDecisionCooldown = this.randomBetween(300, 600);
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.alignFlightFacingToTravel();
        if (this.level().isClientSide) {
            return;
        }
        if (this.territoryCenter == null) {
            this.territoryCenter = this.blockPosition().immutable();
        }
        if (this.idleCooldown > 0) {
            --this.idleCooldown;
        }
        if (this.alertCooldown > 0) {
            --this.alertCooldown;
        }
        if (this.restInterruptionTicks > 0) {
            --this.restInterruptionTicks;
        }
        if (this.spawnAwakeTicks > 0) {
            --this.spawnAwakeTicks;
        }
        if (this.sleepCooldown > 0) {
            --this.sleepCooldown;
        }
        if (this.sleepDecisionCooldown > 0) {
            --this.sleepDecisionCooldown;
        }
        if (this.roamCooldown > 0) {
            --this.roamCooldown;
        }
        if (this.forageCooldown > 0) {
            --this.forageCooldown;
        }
        if (this.forageScanCooldown > 0) {
            --this.forageScanCooldown;
        }
        if (this.shelterScanCooldown > 0) {
            --this.shelterScanCooldown;
        }
        if (this.postLandingCoverTicks > 0) {
            --this.postLandingCoverTicks;
        }
        this.tickDaytimeExposure();
        this.tickPredatorCache();

        WoodcockBehaviorState state = this.getWoodcockBehaviorState();
        if (state == WoodcockBehaviorState.ROCK_1 || state == WoodcockBehaviorState.ROCK_2
                || this.isSpontaneouslyStill()) {
            this.applyStationaryFacingLock();
        } else {
            this.releaseStationaryFacingLock();
        }
        if (this.isBirdFlightActive()) {
            this.cancelSpontaneousStillness();
            this.releaseStationaryFacingLock();
            if (!this.wasFlightActive) {
                this.flightStateTicks = 0;
            }
            ++this.flightStateTicks;
            WoodcockBehaviorState flightState = this.isBirdLanding()
                    ? WoodcockBehaviorState.LANDING
                    : this.flightStateTicks <= 8 ? WoodcockBehaviorState.TAKEOFF : WoodcockBehaviorState.FLYING;
            if (state != flightState) {
                this.setWoodcockBehaviorState(flightState, 0);
            }
            this.wasFlightActive = true;
            this.alertLookTarget = null;
            return;
        }
        if (this.wasFlightActive) {
            this.wasFlightActive = false;
            this.flightStateTicks = 0;
            this.postLandingCoverTicks = this.randomBetween(180, 320);
            this.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
            state = WoodcockBehaviorState.IDLE;
        } else if (state.isAirborne()) {
            this.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
            state = WoodcockBehaviorState.IDLE;
        }
        if (this.manualEatTicks > 0) {
            this.cancelSpontaneousStillness();
            if (state != WoodcockBehaviorState.EATING || this.hurtTime > 0) {
                this.manualEatTicks = 0;
            } else {
                this.getNavigation().stop();
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
                if (--this.manualEatTicks <= 0) {
                    this.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
                    this.roamCooldown = this.randomBetween(10, 30);
                }
                return;
            }
        }
        if ((state == WoodcockBehaviorState.ALERT || state == WoodcockBehaviorState.FREEZE)
                && this.actionTicks > 0) {
            this.cancelSpontaneousStillness();
            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            if (this.alertLookTarget != null) {
                this.getLookControl().setLookAt(
                        this.alertLookTarget.x, this.alertLookTarget.y, this.alertLookTarget.z,
                        35.0F, this.getMaxHeadXRot());
            }
            if (--this.actionTicks <= 0) {
                this.alertLookTarget = null;
                this.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
            }
            return;
        }
        this.stillnessController.tick();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.territoryCenter != null) {
            tag.putInt(TERRITORY_X_TAG, this.territoryCenter.getX());
            tag.putInt(TERRITORY_Y_TAG, this.territoryCenter.getY());
            tag.putInt(TERRITORY_Z_TAG, this.territoryCenter.getZ());
        }
        tag.putInt(FORAGE_COOLDOWN_TAG, this.forageCooldown);
        tag.putInt(SPAWN_AWAKE_TICKS_TAG, this.spawnAwakeTicks);
        tag.putInt(SLEEP_COOLDOWN_TAG, this.sleepCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TERRITORY_X_TAG, 3) && tag.contains(TERRITORY_Y_TAG, 3)
                && tag.contains(TERRITORY_Z_TAG, 3)) {
            this.territoryCenter = new BlockPos(
                    tag.getInt(TERRITORY_X_TAG), tag.getInt(TERRITORY_Y_TAG), tag.getInt(TERRITORY_Z_TAG));
        }
        this.forageCooldown = Math.max(0, tag.getInt(FORAGE_COOLDOWN_TAG));
        this.spawnAwakeTicks = Math.max(0, tag.getInt(SPAWN_AWAKE_TICKS_TAG));
        this.sleepCooldown = Math.max(0, tag.getInt(SLEEP_COOLDOWN_TAG));
        this.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
    }

    public WoodcockBehaviorState getWoodcockBehaviorState() {
        return WoodcockBehaviorState.byId(this.entityData.get(ACTION_STATE));
    }

    public boolean isSpontaneouslyStill() {
        return this.entityData.get(SPONTANEOUS_STILL);
    }

    private void setWoodcockBehaviorState(WoodcockBehaviorState state, int ticks) {
        this.entityData.set(ACTION_STATE, (state == null ? WoodcockBehaviorState.IDLE : state).ordinal());
        this.actionTicks = Math.max(0, ticks);
    }

    private boolean canStartCalmAction() {
        BirdIntent intent = this.birdBrain().currentIntent();
        return this.onGround()
                && this.getNavigation().isDone()
                && !this.isInWaterOrBubble()
                && this.hurtTime <= 0
                && this.getTarget() == null
                && !this.birdBrain().senses().temptingPlayerNearby()
                && this.activityFactor() >= 0.20F
                && (intent == BirdIntent.IDLE || intent == BirdIntent.WATCH)
                && this.birdBrain().computeRiskScore() < 0.45F;
    }

    private boolean canEnterSpontaneousStillness() {
        WoodcockBehaviorState state = this.getWoodcockBehaviorState();
        boolean allowedState = state == WoodcockBehaviorState.IDLE
                || state == WoodcockBehaviorState.SPECIAL_IDLE
                || state == WoodcockBehaviorState.NORMAL_WALK
                || state == WoodcockBehaviorState.FORAGE_APPROACH
                || state == WoodcockBehaviorState.FORAGING
                || state == WoodcockBehaviorState.SEEK_COVER
                || state.isRockWalk();
        return allowedState
                && !this.forcedRockWalkTest
                && !this.pursuingVisibleWorm
                && this.onGround()
                && !this.isBirdFlightActive()
                && !state.isAirborne()
                && !this.isInWaterOrBubble()
                && this.hurtTime <= 0
                && this.getTarget() == null
                && !this.birdBrain().senses().temptingPlayerNearby()
                && this.birdBrain().computeRiskScore() < 0.55F;
    }

    private void cancelSpontaneousStillness() {
        this.stillnessController.cancel();
    }

    private void restoreGroundBehaviorSpeed() {
        if (this.getNavigation().isDone()) {
            return;
        }
        double speed = switch (this.getWoodcockBehaviorState()) {
            case NORMAL_WALK -> NORMAL_WALK_SPEED;
            case FORAGE_APPROACH -> this.pursuingVisibleWorm ? 1.0D : 0.82D;
            case SEEK_COVER -> 0.82D;
            case STEP_1, STEP_2 -> this.rockWalkSequencer.isRunning()
                    ? ROCK_WALK_STEP_SPEED : 0.72D;
            case ROCK_1, ROCK_2 -> ROCK_WALK_SWAY_SPEED;
            default -> 0.0D;
        };
        this.getNavigation().setSpeedModifier(speed);
    }

    @Nullable
    BlockPos territoryCenter() {
        return this.territoryCenter;
    }

    private boolean isInsideTerritory(BlockPos pos, int radius) {
        return this.territoryCenter == null || this.territoryCenter.distSqr(pos) <= (double)(radius * radius);
    }

    private float activityFactor() {
        long time = this.level().getDayTime() % 24000L;
        if (time >= 11000L && time < 15000L) {
            return 1.0F;
        }
        if (time >= 15000L && time < 23000L) {
            return 0.90F;
        }
        if (time >= 23000L || time < 1800L) {
            return 0.82F;
        }
        if (time >= 2500L && time < 10500L) {
            return 0.28F;
        }
        return 0.42F;
    }

    private boolean shouldSeekDayCover() {
        if (this.activityFactor() >= 0.30F || this.restInterruptionTicks > 0
                || this.daytimeExposureTicks < this.daytimeExposureThresholdTicks) {
            return false;
        }
        return this.birdBrain().motivation().fatigue() > 0.42F
                || this.daytimeExposureTicks >= this.daytimeExposureThresholdTicks + 200;
    }

    private boolean shouldConsiderDayRest() {
        return this.activityFactor() < 0.30F
                && this.birdBrain().motivation().fatigue() > 0.46F
                && (this.birdBrain().senses().nearCover()
                || !this.level().canSeeSky(this.blockPosition()));
    }

    private void tickDaytimeExposure() {
        boolean exposedDuringDay = this.activityFactor() < 0.30F
                && this.onGround()
                && !this.isBirdFlightActive()
                && this.level().canSeeSky(this.blockPosition());
        if (exposedDuringDay) {
            this.daytimeExposureTicks = Math.min(1200, this.daytimeExposureTicks + 1);
        } else {
            this.daytimeExposureTicks = Math.max(0, this.daytimeExposureTicks - 3);
        }
    }

    private void onReachedDayCover() {
        this.daytimeExposureTicks = 0;
        this.daytimeExposureThresholdTicks = this.randomBetween(300, 600);
    }

    private void tickPredatorCache() {
        if (this.cachedPredator != null
                && (!this.cachedPredator.isAlive() || this.distanceToSqr(this.cachedPredator) > 196.0D)) {
            this.cachedPredator = null;
        }
        if (this.predatorScanCooldown-- > 0 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.predatorScanCooldown = this.randomBetween(18, 34);
        if (!BirdScanBudget.tryAcquire(serverLevel, this, 1)) {
            return;
        }
        AABB area = this.getBoundingBox().inflate(12.0D, 5.0D, 12.0D);
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Monster monster : serverLevel.getEntitiesOfClass(Monster.class, area,
                entity -> entity.isAlive() && !entity.isSpectator())) {
            double distance = this.distanceToSqr(monster);
            if (distance < nearestDistance) {
                nearest = monster;
                nearestDistance = distance;
            }
        }
        this.cachedPredator = nearest;
    }

    @Nullable
    private LivingEntity currentThreat() {
        Player player = this.birdBrain().senses().nearestPlayer();
        if (player != null && (player.isCreative() || player.isSpectator() || !player.isAlive())) {
            player = null;
        }
        if (player == null) {
            return this.cachedPredator;
        }
        if (this.cachedPredator == null) {
            return player;
        }
        return this.distanceToSqr(player) <= this.distanceToSqr(this.cachedPredator) ? player : this.cachedPredator;
    }

    private boolean startFlush(Vec3 threatPosition) {
        if (threatPosition == null || this.isBirdFlightActive()) {
            return false;
        }
        this.cancelSpontaneousStillness();
        this.getNavigation().stop();
        this.setWoodcockBehaviorState(WoodcockBehaviorState.TAKEOFF, 0);
        boolean started = this.fleeFromFlockThreat(threatPosition);
        if (started) {
            Vec3 motion = this.getDeltaMovement();
            Vec3 horizontal = motion.multiply(1.0D, 0.0D, 1.0D);
            double burstSpeed = 0.46D + this.getRandom().nextDouble() * 0.04D;
            if (horizontal.lengthSqr() > 1.0E-5D) {
                horizontal = horizontal.normalize().scale(burstSpeed);
            }
            this.setDeltaMovement(horizontal.x,
                    Math.max(this.controlledFlightInitialLift(true), motion.y), horizontal.z);
            this.hasImpulse = true;
            if (this.getRandom().nextFloat() < 0.32F && BirdFlockSoundLimiter.allowAmbient(this)) {
                this.playSound(GuaniaoSoundEvents.WOODCOCK_AMBIENT.get(), BirdSoundVolume.apply(this, 0.9F),
                        0.92F + this.getRandom().nextFloat() * 0.16F);
            }
        } else {
            this.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
        }
        return started;
    }

    private boolean isCamouflagedGround() {
        BlockState below = this.level().getBlockState(this.blockPosition().below());
        return below.is(Blocks.PODZOL) || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT)
                || below.is(Blocks.ROOTED_DIRT) || below.is(Blocks.MOSS_BLOCK);
    }

    @Override
    protected boolean usesSparrowTemptGoal() {
        return false;
    }

    @Override
    protected boolean usesSparrowBreedGoal() {
        return false;
    }

    @Override
    protected boolean usesSparrowBreadcrumbGoal() {
        return false;
    }

    @Override
    protected boolean usesSparrowPerchGoal() {
        return false;
    }

    @Override
    protected boolean usesSparrowFlockGoal() {
        return false;
    }

    @Override
    protected boolean usesSparrowSettlementHome() {
        return false;
    }

    @Override
    protected boolean usesSparrowMigrationGoal() {
        return false;
    }

    @Override
    protected boolean usesSparrowPlayerFleeGoal() {
        return false;
    }

    @Override
    protected boolean usesSparrowBirdBathGoal() {
        return false;
    }

    @Override
    protected boolean usesSparrowRandomStrollGoal() {
        return false;
    }

    @Override
    protected boolean blocksSparrowDecorativeBehavior() {
        return true;
    }

    @Override
    protected boolean isCleanTamingItem(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BirdFoodSafety.matchesClean(BirdTags.WOODCOCK_FOODS, stack)) {
            return InteractionResult.PASS;
        }
        WoodcockBehaviorState state = this.getWoodcockBehaviorState();
        boolean calmingPlayerAlert = (state == WoodcockBehaviorState.ALERT
                || state == WoodcockBehaviorState.FREEZE)
                && !player.isSprinting()
                && (this.level().isClientSide || this.currentThreat() == player);
        boolean canHandFeed = state == WoodcockBehaviorState.IDLE
                || state == WoodcockBehaviorState.SPECIAL_IDLE
                || state == WoodcockBehaviorState.STILL
                || state == WoodcockBehaviorState.NORMAL_WALK
                || state.isRockWalk()
                || calmingPlayerAlert;
        if (!canHandFeed || this.isBirdFlightActive() || !this.onGround()
                || this.isInWaterOrBubble() || this.hurtTime > 0) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide) {
            this.cancelSpontaneousStillness();
            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            this.manualEatTicks = 90;
            this.setWoodcockBehaviorState(WoodcockBehaviorState.EATING, this.manualEatTicks);
            this.birdBrain().onEat(0.32F);
            this.forageCooldown = this.randomBetween(240, 480);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private void alignFlightFacingToTravel() {
        if (!this.isFlying()) {
            return;
        }
        Vec3 movement;
        if (this.level().isClientSide) {
            Vec3 actualMovement = new Vec3(
                    this.getX() - this.xo,
                    this.getY() - this.yo,
                    this.getZ() - this.zo);
            movement = actualMovement.horizontalDistanceSqr() > 1.0E-5D
                    ? actualMovement : this.getDeltaMovement();
        } else {
            movement = this.getDeltaMovement();
        }
        if (movement.horizontalDistanceSqr() <= 1.0E-5D) {
            return;
        }
        float yaw = (float)(Mth.atan2(movement.z, movement.x) * 57.29577951308232D) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.setYHeadRot(yaw);
        this.yRotO = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRotO = yaw;
    }

    private void lockStationaryFacing() {
        this.stationaryLockedYaw = this.getYRot();
        this.stationaryFacingLocked = true;
        this.applyStationaryFacingLock();
    }

    private void applyStationaryFacingLock() {
        if (!this.stationaryFacingLocked) {
            return;
        }
        this.setYRot(this.stationaryLockedYaw);
        this.yRotO = this.stationaryLockedYaw;
        this.yBodyRot = this.stationaryLockedYaw;
        this.yBodyRotO = this.stationaryLockedYaw;
        this.setYHeadRot(this.stationaryLockedYaw);
        this.yHeadRotO = this.stationaryLockedYaw;
    }

    private void releaseStationaryFacingLock() {
        this.stationaryFacingLocked = false;
    }

    @Nullable
    @Override
    public WoodcockEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
        return null;
    }

    @Override
    public boolean canFlockWith(Entity other) {
        return false;
    }

    @Override
    public BirdFlightProfile birdFlightProfile() {
        return BirdFlightProfile.WOODCOCK;
    }

    @Override
    protected int escapeFlightMinRadius() {
        return 10;
    }

    @Override
    protected int escapeFlightMaxRadius() {
        return 18;
    }

    @Override
    protected double controlledFlightInitialLift(boolean escapeFlight) {
        return escapeFlight ? 0.56D : super.controlledFlightInitialLift(escapeFlight);
    }

    @Override
    protected int controlledFlightTakeoffTicks(boolean escapeFlight) {
        return escapeFlight ? 13 : super.controlledFlightTakeoffTicks(escapeFlight);
    }

    @Override
    protected double controlledFlightTakeoffLift(boolean escapeFlight) {
        return escapeFlight ? 0.34D : super.controlledFlightTakeoffLift(escapeFlight);
    }

    @Override
    protected Vec3 adjustControlledFlightHorizontalDirection(Vec3 direction, int flightAge,
                                                               boolean escapeFlight) {
        if (!escapeFlight || flightAge < 7 || direction.lengthSqr() <= 1.0E-5D) {
            return direction;
        }
        if (this.tickCount >= this.nextFlightWeaveTick) {
            this.nextFlightWeaveTick = this.tickCount + this.randomBetween(8, 15);
            this.flightWeaveBias = (this.getRandom().nextDouble() * 2.0D - 1.0D) * 0.22D;
        }
        Vec3 sideways = new Vec3(-direction.z, 0.0D, direction.x);
        Vec3 adjusted = direction.add(sideways.scale(this.flightWeaveBias));
        return adjusted.lengthSqr() <= 1.0E-5D ? direction : adjusted.normalize();
    }

    @Override
    protected double scoreShortFlightLanding(BlockPos pos, Vec3 threatPosition, boolean escape) {
        if (!this.level().hasChunkAt(pos)) {
            return Double.NEGATIVE_INFINITY;
        }
        BlockState ground = this.level().getBlockState(pos.below());
        double score = ground.is(BirdTags.WOODCOCK_FORAGE_GROUND) ? 12.0D : -3.0D;
        if (!this.level().canSeeSky(pos)) {
            score += 8.0D;
        } else {
            score -= 6.0D;
        }
        if (ground.is(BlockTags.LEAVES) || ground.is(BlockTags.LOGS)) {
            score -= 10.0D;
        }
        score -= Math.abs(pos.getY() - this.getY()) * 0.55D;
        if (escape && threatPosition != null) {
            score += Math.min(18.0D, Vec3.atBottomCenterOf(pos).distanceTo(threatPosition) * 0.7D);
        }
        return score;
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.WOODCOCK;
    }

    @Override
    public boolean isFlying() {
        return this.getWoodcockBehaviorState().isAirborne()
                || this.isBirdFlightActive() || (!this.onGround() && !this.isInWaterOrBubble());
    }

    @Override
    public boolean isBirdSleeping() {
        return this.getWoodcockBehaviorState().isSleeping();
    }

    @Override
    public void onLoudSound(Vec3 soundPosition, float volume) {
        if (this.level().isClientSide || soundPosition == null || this.forcedRockWalkTest) {
            return;
        }
        this.cancelSpontaneousStillness();
        this.wakeFromLoudSound(soundPosition);
        if (volume >= 2.35F) {
            this.startFlush(soundPosition);
            return;
        }
        if (!this.isBirdFlightActive() && this.onGround() && this.hurtTime <= 0) {
            this.getNavigation().stop();
            this.alertLookTarget = soundPosition;
            int listenTicks = 22 + Math.min(36, Math.round(volume * 8.0F));
            this.setWoodcockBehaviorState(WoodcockBehaviorState.ALERT, Math.min(30, listenTicks));
        }
    }

    @Override
    public void wakeFromLoudSound(Vec3 soundPosition) {
        if (this.level().isClientSide || this.forcedRockWalkTest) {
            return;
        }
        this.cancelSpontaneousStillness();
        if (soundPosition != null) {
            this.alertLookTarget = soundPosition;
            this.getLookControl().setLookAt(soundPosition.x, soundPosition.y, soundPosition.z,
                    35.0F, this.getMaxHeadXRot());
        }
        this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 260);
        this.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            this.cancelSpontaneousStillness();
            Vec3 sourcePosition = source.getEntity() == null ? this.position() : source.getEntity().position();
            this.wakeFromLoudSound(sourcePosition);
            this.startFlush(sourcePosition);
        }
        return hurt;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        BlockState below = level.getBlockState(pos.below());
        float score = super.getWalkTargetValue(pos, level);
        if (below.is(Blocks.MOSS_BLOCK) || below.is(Blocks.PODZOL) || below.is(Blocks.ROOTED_DIRT)) {
            score += 7.0F;
        } else if (below.is(Blocks.MUD) || below.is(Blocks.GRASS_BLOCK)) {
            score += 5.0F;
        } else if (below.is(BlockTags.DIRT)) {
            score += 3.0F;
        }
        return score;
    }

    private boolean shouldPlayFlyAnimation() {
        return BirdFlightController.shouldPlayFlyAnimation(
                this, this.isBirdFlightActive(), this.onGround(), this.isNoGravity(),
                this.getDeltaMovement(), 6);
    }

    private <T extends WoodcockEntity> PlayState movementController(AnimationState<T> animationState) {
        animationState.getController().setAnimationSpeed(1.0D);
        animationState.getController().transitionLength(1);
        if (this.isSpontaneouslyStill()) {
            animationState.getController().setAnimationSpeed(this.individualAnimationCadence());
            return animationState.setAndContinue(IDLE_ANIMATION);
        }
        if (this.getWoodcockBehaviorState() == WoodcockBehaviorState.SLEEP_PREP) {
            return animationState.setAndContinue(SLEEP_ENTER_ANIMATION);
        }
        if (this.getWoodcockBehaviorState() == WoodcockBehaviorState.SLEEPING) {
            animationState.getController().setAnimationSpeed(this.individualAnimationCadence());
            return animationState.setAndContinue(SLEEP_LOOP_ANIMATION);
        }
        if (this.shouldPlayFlyAnimation()) {
            return BirdFlightAnimation.play(animationState, FLY_ANIMATION);
        }
        return switch (this.getWoodcockBehaviorState()) {
            case STEP_1 -> {
                animationState.getController().setAnimationSpeed(ROCK_WALK_ANIMATION_SPEED);
                yield animationState.setAndContinue(STEP_1_ANIMATION);
            }
            case ROCK_1 -> {
                animationState.getController().setAnimationSpeed(ROCK_WALK_ANIMATION_SPEED);
                yield animationState.setAndContinue(ROCK_1_ANIMATION);
            }
            case STEP_2 -> {
                animationState.getController().setAnimationSpeed(ROCK_WALK_ANIMATION_SPEED);
                yield animationState.setAndContinue(STEP_2_ANIMATION);
            }
            case ROCK_2 -> {
                animationState.getController().setAnimationSpeed(ROCK_WALK_ANIMATION_SPEED);
                yield animationState.setAndContinue(ROCK_2_ANIMATION);
            }
            case FORAGING -> animationState.setAndContinue(PROBE_ANIMATION);
            case EATING -> animationState.setAndContinue(EAT_ANIMATION);
            case SPECIAL_IDLE -> animationState.setAndContinue(IDLE_VARIATION_ANIMATION);
            case STILL -> {
                animationState.getController().setAnimationSpeed(this.individualAnimationCadence());
                yield animationState.setAndContinue(IDLE_ANIMATION);
            }
            case NORMAL_WALK, FORAGE_APPROACH, SEEK_COVER -> {
                if (BirdGroundAnimation.hasWalkMotion(this, animationState.isMoving())) {
                    animationState.getController().setAnimationSpeed(
                            BirdGroundAnimation.walkAnimationSpeed(this,
                                    NORMAL_WALK_ANIMATION_CADENCE * this.individualAnimationCadence()));
                    yield animationState.setAndContinue(WALK_ANIMATION);
                }
                animationState.getController().setAnimationSpeed(this.individualAnimationCadence());
                yield animationState.setAndContinue(IDLE_ANIMATION);
            }
            default -> {
                if (BirdGroundAnimation.hasWalkMotion(this, animationState.isMoving())) {
                    animationState.getController().setAnimationSpeed(
                            BirdGroundAnimation.walkAnimationSpeed(this,
                                    NORMAL_WALK_ANIMATION_CADENCE * this.individualAnimationCadence()));
                    yield animationState.setAndContinue(WALK_ANIMATION);
                }
                animationState.getController().setAnimationSpeed(this.individualAnimationCadence());
                yield animationState.setAndContinue(IDLE_ANIMATION);
            }
        };
    }

    /** Stable per bird, so nearby idle and walk loops slowly drift out of lockstep. */
    private double individualAnimationCadence() {
        int bucket = Math.floorMod(this.getUUID().hashCode(), 17);
        return MIN_INDIVIDUAL_ANIMATION_CADENCE + bucket * INDIVIDUAL_ANIMATION_CADENCE_STEP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{
                new AnimationController((GeoAnimatable)this, "movement", 1, this::movementController)
        });
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.WOODCOCK_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        int baseTicks = BirdActivitySchedule.NOCTURNAL_CREPUSCULAR.isActiveTime(this.level().getDayTime())
                ? 300 : 560;
        return BirdFlockSoundLimiter.scaledAmbientInterval(this, baseTicks);
    }

    @Override
    public void playAmbientSound() {
        WoodcockBehaviorState state = this.getWoodcockBehaviorState();
        if (!this.isBirdSleeping() && state != WoodcockBehaviorState.FREEZE
                && state != WoodcockBehaviorState.EATING && state != WoodcockBehaviorState.FORAGING) {
            // SparrowEntity supplies the shared per-flock sound limiter.
            // The interval above already makes daytime calls less frequent; do
            // not suppress them entirely or the registered call seems absent
            // whenever the bird is observed during the day.
            super.playAmbientSound();
        }
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    private int randomBetween(int min, int max) {
        return min + this.getRandom().nextInt(Math.max(1, max - min + 1));
    }

    /** Forces one signature walk, or a continuous one, for the operator-only test command. */
    public void startRockWalkTest(Vec3 target) {
        this.startRockWalkTest(target, false);
    }

    public void startRockWalkTest(Vec3 target, boolean loop) {
        if (this.level().isClientSide || target == null) {
            return;
        }
        this.cancelSpontaneousStillness();
        this.getNavigation().stop();
        this.setTarget(null);
        this.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
        this.forcedRockWalkTarget = target;
        this.forcedRockWalkTest = true;
        this.loopingRockWalkTest = loop;
        this.roamCooldown = 0;
        this.spawnAwakeTicks = Math.max(this.spawnAwakeTicks, 200);
        this.sleepCooldown = Math.max(this.sleepCooldown, 200);
        this.forageCooldown = Math.max(this.forageCooldown, 200);
    }

    private static final class WoodcockTemptGoal extends CleanBirdTemptGoal {
        private final WoodcockEntity woodcock;

        private WoodcockTemptGoal(WoodcockEntity woodcock) {
            // Holding a worm is an intentional way to observe a wild woodcock.
            // Do not inherit the generic "any player movement scares it" rule:
            // that rule used to cancel attraction as soon as the player walked.
            super(woodcock, 0.92D, Ingredient.of(BirdTags.WOODCOCK_FOODS), false);
            this.woodcock = woodcock;
        }

        @Override
        public boolean canUse() {
            if (this.woodcock.forcedRockWalkTest) {
                return false;
            }
            WoodcockBehaviorState state = this.woodcock.getWoodcockBehaviorState();
            boolean interruptibleGroundAction = state == WoodcockBehaviorState.IDLE
                    || state == WoodcockBehaviorState.SPECIAL_IDLE
                    || state == WoodcockBehaviorState.STILL
                    || state == WoodcockBehaviorState.NORMAL_WALK
                    || state == WoodcockBehaviorState.FORAGE_APPROACH
                    || state == WoodcockBehaviorState.FORAGING
                    || state == WoodcockBehaviorState.SEEK_COVER
                    || state.isRockWalk();
            return interruptibleGroundAction
                    && this.woodcock.onGround()
                    && !this.woodcock.isBirdFlightActive()
                    && this.woodcock.hurtTime <= 0
                    && super.canUse();
        }

        @Override
        public void start() {
            // Higher-priority temptation may interrupt wandering/rock-walking.
            // Own the visual state as well as the navigation state.
            this.woodcock.cancelSpontaneousStillness();
            this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            return this.woodcock.getWoodcockBehaviorState() == WoodcockBehaviorState.IDLE
                    && !this.woodcock.isBirdFlightActive()
                    && super.canContinueToUse();
        }

        @Override
        protected double stopDistanceSqr() {
            return 2.56D;
        }
    }

    private static final class WoodcockFrightGoal extends Goal {
        private final WoodcockEntity woodcock;
        @Nullable
        private LivingEntity threat;
        private int phaseTicks;
        private int minimumFreezeTicks;
        private boolean flushImmediately;

        private WoodcockFrightGoal(WoodcockEntity woodcock) {
            this.woodcock = woodcock;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.woodcock.forcedRockWalkTest || this.woodcock.isBirdFlightActive()
                    || this.woodcock.isInWaterOrBubble()) {
                return false;
            }
            LivingEntity nearest = this.woodcock.currentThreat();
            if (nearest == null) {
                return false;
            }
            double distance = this.woodcock.distanceTo(nearest);
            this.flushImmediately = distance <= this.flushDistance(nearest);
            if (nearest instanceof Player player && this.isHoldingWoodcockFood(player)
                    && !player.isSprinting() && !this.flushImmediately) {
                return false;
            }
            if (!this.flushImmediately && (this.woodcock.alertCooldown > 0
                    || distance > this.alertDistance(nearest))) {
                return false;
            }
            this.threat = nearest;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            WoodcockBehaviorState state = this.woodcock.getWoodcockBehaviorState();
            if (this.threat instanceof Player player
                    && this.isHoldingWoodcockFood(player)
                    && this.woodcock.distanceTo(player) > this.flushDistance(player)) {
                return false;
            }
            return this.threat != null && this.threat.isAlive() && this.phaseTicks > 0
                    && (state == WoodcockBehaviorState.ALERT || state == WoodcockBehaviorState.FREEZE)
                    && this.woodcock.distanceToSqr(this.threat) <= 225.0D;
        }

        @Override
        public void start() {
            this.woodcock.cancelSpontaneousStillness();
            this.woodcock.getNavigation().stop();
            if (this.flushImmediately && this.threat != null) {
                this.woodcock.startFlush(this.threat.position());
                this.phaseTicks = 0;
                return;
            }
            this.phaseTicks = this.woodcock.randomBetween(15, 28);
            this.minimumFreezeTicks = this.woodcock.randomBetween(18, 34);
            this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.ALERT, 0);
        }

        @Override
        public void tick() {
            if (this.threat == null) {
                return;
            }
            this.woodcock.getNavigation().stop();
            this.woodcock.setDeltaMovement(this.woodcock.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            this.woodcock.getLookControl().setLookAt(this.threat, 30.0F, this.woodcock.getMaxHeadXRot());
            double distance = this.woodcock.distanceTo(this.threat);
            if (distance <= this.flushDistance(this.threat)) {
                this.woodcock.startFlush(this.threat.position());
                this.phaseTicks = 0;
                return;
            }
            WoodcockBehaviorState state = this.woodcock.getWoodcockBehaviorState();
            if (state == WoodcockBehaviorState.ALERT && --this.phaseTicks <= 0) {
                this.phaseTicks = this.woodcock.randomBetween(60, 110);
                this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.FREEZE, 0);
            } else if (state == WoodcockBehaviorState.FREEZE) {
                if (this.minimumFreezeTicks > 0) {
                    --this.minimumFreezeTicks;
                }
                if (--this.phaseTicks <= 0
                        || this.minimumFreezeTicks <= 0 && distance > this.alertDistance(this.threat) + 1.5D) {
                    this.phaseTicks = 0;
                }
            }
        }

        @Override
        public void stop() {
            WoodcockBehaviorState state = this.woodcock.getWoodcockBehaviorState();
            if (state == WoodcockBehaviorState.ALERT || state == WoodcockBehaviorState.FREEZE) {
                this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
            }
            // A stationary observer should not lock the bird into repeated freeze loops.
            // The close-range flush check still bypasses this cooldown.
            this.woodcock.alertCooldown = this.woodcock.randomBetween(180, 340);
            this.threat = null;
            this.phaseTicks = 0;
            this.minimumFreezeTicks = 0;
            this.flushImmediately = false;
        }

        private double alertDistance(LivingEntity entity) {
            float cautiousness = this.woodcock.birdBrain().personality().wariness() * 0.55F
                    + this.woodcock.birdBrain().personality().flightiness() * 0.45F
                    - this.woodcock.birdBrain().personality().boldness() * 0.35F;
            double distance = 8.0D + cautiousness * 1.8D;
            if (entity instanceof Player player) {
                if (this.isHoldingWoodcockFood(player)) {
                    distance -= 5.2D;
                } else if (player.isShiftKeyDown()) {
                    distance -= 1.7D;
                }
            }
            return Math.max(2.4D, distance);
        }

        private double flushDistance(LivingEntity entity) {
            if (!(entity instanceof Player player)) {
                return 6.0D;
            }
            float cautiousness = this.woodcock.birdBrain().personality().wariness()
                    + this.woodcock.birdBrain().personality().flightiness()
                    - this.woodcock.birdBrain().personality().boldness();
            if (this.isHoldingWoodcockFood(player) && !player.isSprinting()) {
                return player.isShiftKeyDown() ? 0.75D : 0.90D;
            }
            double distance = player.isShiftKeyDown() ? 2.9D : player.isSprinting() ? 7.4D : 4.2D;
            distance += cautiousness * (player.isShiftKeyDown() ? 0.22D : 0.42D);
            if (this.woodcock.isCamouflagedGround() && !player.isSprinting()) {
                distance -= 0.25D;
            }
            return distance;
        }

        private boolean isHoldingWoodcockFood(Player player) {
            return player.getMainHandItem().is(BirdTags.WOODCOCK_FOODS)
                    || player.getOffhandItem().is(BirdTags.WOODCOCK_FOODS);
        }
    }

    private static final class WoodcockSleepGoal extends Goal {
        private final WoodcockEntity woodcock;
        private int minimumSleepTicks;
        private int maximumSleepTicks;

        private WoodcockSleepGoal(WoodcockEntity woodcock) {
            this.woodcock = woodcock;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.woodcock.sleepDecisionCooldown > 0) {
                return false;
            }
            this.woodcock.sleepDecisionCooldown = this.woodcock.randomBetween(300, 600);
            boolean daytimeRest = this.woodcock.shouldConsiderDayRest();
            boolean fatigueRest = this.woodcock.birdBrain().motivation().fatigue() > 0.78F
                    && this.woodcock.activityFactor() < 0.72F;
            return !this.woodcock.forcedRockWalkTest
                    && this.woodcock.spawnAwakeTicks <= 0
                    && this.woodcock.sleepCooldown <= 0
                    && this.woodcock.restInterruptionTicks <= 0
                    && this.woodcock.getWoodcockBehaviorState() == WoodcockBehaviorState.IDLE
                    && this.woodcock.onGround()
                    && this.woodcock.getNavigation().isDone()
                    && !this.woodcock.isInWaterOrBubble()
                    && !this.woodcock.birdBrain().senses().temptingPlayerNearby()
                    && this.woodcock.hurtTime <= 0
                    && (daytimeRest || fatigueRest)
                    && (this.woodcock.birdBrain().senses().nearCover()
                    || !this.woodcock.level().canSeeSky(this.woodcock.blockPosition()))
                    && this.woodcock.birdBrain().computeRiskScore() < 0.48F
                    && this.woodcock.getRandom().nextFloat() < (daytimeRest ? 0.30F : 0.18F);
        }

        @Override
        public boolean canContinueToUse() {
            boolean protectedMinimum = this.minimumSleepTicks > 0;
            boolean plannedDayRest = this.maximumSleepTicks > 0 && this.woodcock.activityFactor() < 0.35F;
            return this.woodcock.isBirdSleeping()
                    && this.woodcock.restInterruptionTicks <= 0
                    && this.woodcock.onGround()
                    && !this.woodcock.isInWaterOrBubble()
                    && this.woodcock.hurtTime <= 0
                    && (protectedMinimum || plannedDayRest)
                    && this.woodcock.birdBrain().computeRiskScore() < 0.62F;
        }

        @Override
        public void start() {
            this.woodcock.cancelSpontaneousStillness();
            this.woodcock.getNavigation().stop();
            this.minimumSleepTicks = this.woodcock.randomBetween(260, 520);
            this.maximumSleepTicks = this.woodcock.randomBetween(700, 1300);
            this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.SLEEP_PREP, SLEEP_ENTER_TICKS);
        }

        @Override
        public void tick() {
            this.woodcock.getNavigation().stop();
            if (this.minimumSleepTicks > 0) {
                --this.minimumSleepTicks;
            }
            if (this.maximumSleepTicks > 0) {
                --this.maximumSleepTicks;
            }
            if (this.woodcock.getWoodcockBehaviorState() == WoodcockBehaviorState.SLEEP_PREP
                    && --this.woodcock.actionTicks <= 0) {
                this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.SLEEPING, 0);
            } else if (this.woodcock.getWoodcockBehaviorState() == WoodcockBehaviorState.SLEEPING
                    && this.woodcock.tickCount % 80 == 0) {
                this.woodcock.birdBrain().onRest(0.035F);
            }
        }

        @Override
        public void stop() {
            if (this.woodcock.isBirdSleeping()) {
                this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
            }
            if (this.maximumSleepTicks <= 0 && this.woodcock.activityFactor() < 0.35F) {
                this.woodcock.restInterruptionTicks = Math.max(this.woodcock.restInterruptionTicks,
                        this.woodcock.randomBetween(360, 760));
                this.woodcock.roamCooldown = Math.min(this.woodcock.roamCooldown,
                        this.woodcock.randomBetween(4, 16));
            }
            this.woodcock.sleepCooldown = this.woodcock.randomBetween(1800, 3600);
            this.woodcock.sleepDecisionCooldown = this.woodcock.randomBetween(300, 600);
            this.minimumSleepTicks = 0;
            this.maximumSleepTicks = 0;
        }
    }

    /** Visible food is worth approaching even outside the normal soil-probing hunger window. */
    private static final class WoodcockEatWormGoal extends Goal {
        private static final double EAT_DISTANCE_SQR = 0.81D;
        private final WoodcockEntity woodcock;
        @Nullable private EarthwormEntity worm;
        @Nullable private Path path;
        private long nextScanAt;
        private long startedAt;
        private long lastProgressAt;
        private long nextPathAt;
        private long peckStartedAt;
        private double bestDistanceSqr;
        private boolean consumed;

        private WoodcockEatWormGoal(WoodcockEntity woodcock) {
            this.woodcock = woodcock;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            long now = this.woodcock.level().getGameTime();
            WoodcockBehaviorState state = this.woodcock.getWoodcockBehaviorState();
            if (now < this.nextScanAt || this.woodcock.forcedRockWalkTest
                    || (state != WoodcockBehaviorState.IDLE && state != WoodcockBehaviorState.NORMAL_WALK
                        && state != WoodcockBehaviorState.SPECIAL_IDLE && state != WoodcockBehaviorState.STILL
                        && state != WoodcockBehaviorState.FORAGE_APPROACH
                        && state != WoodcockBehaviorState.SEEK_COVER && !state.isRockWalk())
                    || !this.woodcock.onGround() || this.woodcock.isBirdFlightActive()
                    || this.woodcock.hurtTime > 0 || this.woodcock.birdBrain().computeRiskScore() >= 0.62F) {
                return false;
            }
            this.nextScanAt = now + this.woodcock.randomBetween(30, 50);
            this.worm = null;
            this.path = null;
            if (!(this.woodcock.level() instanceof ServerLevel serverLevel)
                    || !BirdScanBudget.tryAcquire(serverLevel, this.woodcock, 2)) {
                return false;
            }
            // Bound path searches; an unreachable worm must not hide the other nearby food.
            var candidates = serverLevel.getEntitiesOfClass(EarthwormEntity.class,
                    this.woodcock.getBoundingBox().inflate(8.0D, 2.0D, 8.0D),
                    candidate -> candidate.isAlive() && candidate.burrowProgress() == 0.0F
                            && this.woodcock.hasLineOfSight(candidate));
            candidates.sort(java.util.Comparator.comparingDouble(this.woodcock::distanceToSqr));
            for (int i = 0; i < Math.min(4, candidates.size()); i++) {
                EarthwormEntity candidate = candidates.get(i);
                Path candidatePath = this.woodcock.getNavigation().createPath(candidate.blockPosition(), 0);
                if ((candidatePath != null && candidatePath.canReach())
                        || this.woodcock.distanceToSqr(candidate) <= EAT_DISTANCE_SQR) {
                    this.worm = candidate;
                    this.path = candidatePath;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            this.woodcock.cancelSpontaneousStillness();
            this.woodcock.pursuingVisibleWorm = true;
            this.startedAt = this.woodcock.level().getGameTime();
            this.lastProgressAt = this.startedAt;
            this.nextPathAt = this.startedAt + 20;
            this.peckStartedAt = -1;
            this.consumed = false;
            this.bestDistanceSqr = this.woodcock.distanceToSqr(this.worm);
            this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.FORAGE_APPROACH, 0);
            if (this.path != null) this.woodcock.getNavigation().moveTo(this.path, 1.0D);
        }

        @Override
        public boolean canContinueToUse() {
            long now = this.woodcock.level().getGameTime();
            WoodcockBehaviorState state = this.woodcock.getWoodcockBehaviorState();
            return this.worm != null && !this.woodcock.isBirdFlightActive() && this.woodcock.hurtTime <= 0
                    && (state == WoodcockBehaviorState.FORAGE_APPROACH || state == WoodcockBehaviorState.EATING)
                    && this.woodcock.birdBrain().computeRiskScore() < 0.62F
                    && (this.consumed ? now - this.peckStartedAt < 40
                        : this.worm.isAlive() && this.worm.burrowProgress() == 0.0F
                            && now - this.startedAt < 300 && now - this.lastProgressAt < 80);
        }

        @Override
        public void tick() {
            if (this.worm == null || this.consumed) return;
            long now = this.woodcock.level().getGameTime();
            double distance = this.woodcock.distanceToSqr(this.worm);
            this.woodcock.getLookControl().setLookAt(this.worm, 30.0F, this.woodcock.getMaxHeadXRot());
            if (distance < this.bestDistanceSqr - 0.02D) {
                this.bestDistanceSqr = distance;
                this.lastProgressAt = now;
            }
            if (distance <= EAT_DISTANCE_SQR && this.woodcock.hasLineOfSight(this.worm)) {
                this.lastProgressAt = now;
                this.woodcock.getNavigation().stop();
                if (this.peckStartedAt < 0) {
                    this.peckStartedAt = now;
                    this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.EATING, 40);
                } else if (now - this.peckStartedAt >= 12) {
                    // The server removes the actual worm once, so two birds cannot both eat it.
                    this.consumed = this.worm.tryEat();
                    if (this.consumed) this.woodcock.birdBrain().onEat(0.22F);
                    else this.worm = null;
                }
                return;
            }
            this.peckStartedAt = -1;
            this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.FORAGE_APPROACH, 0);
            if (now >= this.nextPathAt) {
                this.nextPathAt = now + 20;
                // Track the crawling worm instead of walking toward its stale block position.
                if (!this.woodcock.getNavigation().moveTo(this.worm, 1.0D)) this.worm = null;
            }
        }

        @Override
        public void stop() {
            this.woodcock.getNavigation().stop();
            if (this.woodcock.getWoodcockBehaviorState() == WoodcockBehaviorState.FORAGE_APPROACH
                    || this.woodcock.getWoodcockBehaviorState() == WoodcockBehaviorState.EATING) {
                this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
            }
            this.nextScanAt = this.woodcock.level().getGameTime() + (this.consumed ? 100 : 60);
            if (this.consumed) this.woodcock.forageCooldown = Math.max(this.woodcock.forageCooldown, 100);
            this.worm = null;
            this.path = null;
            this.woodcock.pursuingVisibleWorm = false;
        }
    }

    private static final class WoodcockForageGoal extends Goal {
        private static final int PROBE_TICKS = 100;
        private static final int EAT_TICKS = 90;
        private static final int MAX_APPROACH_TICKS = 300;
        private static final int MAX_STALLED_TICKS = 100;
        private final WoodcockEntity woodcock;
        @Nullable
        private BlockPos target;
        @Nullable
        private Path path;
        private int phaseTicks;
        private int probeElapsed;
        private int probeAttemptCount;
        private int probeAttemptsRemaining;
        private int nextProbeTick;
        private boolean foundFood;
        @Nullable
        private EarthwormEntity exposedWorm;
        private long approachStartedAt;
        private long lastProgressAt;
        private double bestApproachDistanceSqr;

        private WoodcockForageGoal(WoodcockEntity woodcock) {
            this.woodcock = woodcock;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.woodcock.forcedRockWalkTest
                    || this.woodcock.getWoodcockBehaviorState() != WoodcockBehaviorState.IDLE
                    || !this.woodcock.onGround() || this.woodcock.isBirdFlightActive()
                    || this.woodcock.forageCooldown > 0
                    || this.woodcock.forageScanCooldown > 0
                    || this.woodcock.birdBrain().senses().temptingPlayerNearby()
                    || !this.shouldForageNow()) {
                return false;
            }
            boolean wetWeather = this.woodcock.level().isRaining();
            int baseMinimum = wetWeather ? 40 : 62;
            int baseMaximum = wetWeather ? 72 : 100;
            float activity = this.woodcock.birdBrain().personality().activity();
            this.woodcock.forageScanCooldown = Math.max(30,
                    this.woodcock.randomBetween(baseMinimum, baseMaximum) - Math.round(activity * 12.0F));
            if (this.woodcock.level() instanceof ServerLevel serverLevel
                    && !BirdScanBudget.tryAcquire(serverLevel, this.woodcock, 2)) {
                return false;
            }
            this.exposedWorm = null;
            this.target = this.findForageTarget();
            if (this.target == null) {
                return false;
            }
            this.path = this.woodcock.getNavigation().createPath(this.target, 0);
            return this.path != null && this.path.canReach();
        }

        @Override
        public boolean canContinueToUse() {
            WoodcockBehaviorState state = this.woodcock.getWoodcockBehaviorState();
            boolean ownedState = state.isForagingSequence() || state.isRockWalk();
            return this.target != null && ownedState
                    && !this.woodcock.isBirdFlightActive()
                    && this.woodcock.hurtTime <= 0
                    && this.woodcock.birdBrain().computeRiskScore() < 0.62F;
        }

        @Override
        public void start() {
            this.phaseTicks = 0;
            this.probeElapsed = 0;
            this.foundFood = this.exposedWorm != null;
            this.approachStartedAt = this.woodcock.level().getGameTime();
            this.lastProgressAt = this.approachStartedAt;
            this.bestApproachDistanceSqr = this.woodcock.position().distanceToSqr(Vec3.atBottomCenterOf(this.target));
            this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.FORAGE_APPROACH, 0);
            this.woodcock.getNavigation().moveTo(this.path, 0.82D);
        }

        @Override
        public void tick() {
            if (this.target == null) {
                return;
            }
            if (this.woodcock.isSpontaneouslyStill()) {
                if (this.woodcock.getWoodcockBehaviorState() == WoodcockBehaviorState.FORAGE_APPROACH
                        || this.woodcock.getWoodcockBehaviorState().isRockWalk()) {
                    ++this.approachStartedAt;
                    ++this.lastProgressAt;
                }
                return;
            }
            WoodcockBehaviorState state = this.woodcock.getWoodcockBehaviorState();
            if (state == WoodcockBehaviorState.FORAGE_APPROACH || state.isRockWalk()) {
                // Deadlines use world ticks, shifted only while the stillness overlay pauses this route.
                long now = this.woodcock.level().getGameTime();
                double distanceSqr = this.woodcock.position().distanceToSqr(Vec3.atBottomCenterOf(this.target));
                if (distanceSqr < this.bestApproachDistanceSqr - 0.04D) {
                    this.bestApproachDistanceSqr = distanceSqr;
                    this.lastProgressAt = now;
                }
                if (distanceSqr > 1.65D && (now - this.approachStartedAt >= MAX_APPROACH_TICKS
                        || now - this.lastProgressAt >= MAX_STALLED_TICKS)) {
                    this.woodcock.getNavigation().stop();
                    this.woodcock.forageCooldown = this.woodcock.randomBetween(60, 100);
                    this.target = null;
                    return;
                }
                this.woodcock.getLookControl().setLookAt(
                        this.target.getX() + 0.5D, this.target.getY(), this.target.getZ() + 0.5D,
                        24.0F, this.woodcock.getMaxHeadXRot());
                if (this.woodcock.position().distanceToSqr(Vec3.atBottomCenterOf(this.target)) <= 1.65D) {
                    this.beginProbe();
                } else if (state == WoodcockBehaviorState.FORAGE_APPROACH
                        && this.woodcock.position().distanceToSqr(Vec3.atBottomCenterOf(this.target)) <= 25.0D) {
                    this.beginApproachStep(WoodcockBehaviorState.STEP_1);
                } else if (state == WoodcockBehaviorState.FORAGE_APPROACH
                        && this.woodcock.getNavigation().isDone()) {
                    this.target = null;
                } else if (state.isRockWalk() && --this.phaseTicks <= 0) {
                    switch (state) {
                        case STEP_1 -> this.beginApproachRock(WoodcockBehaviorState.ROCK_1);
                        case ROCK_1 -> this.beginApproachStep(WoodcockBehaviorState.STEP_2);
                        case STEP_2 -> this.beginApproachRock(WoodcockBehaviorState.ROCK_2);
                        case ROCK_2 -> this.beginApproachStep(WoodcockBehaviorState.STEP_1);
                        default -> {
                        }
                    }
                }
                return;
            }
            this.woodcock.getNavigation().stop();
            if (state == WoodcockBehaviorState.FORAGING) {
                ++this.probeElapsed;
                --this.phaseTicks;
                if (!this.foundFood && this.probeAttemptsRemaining > 0
                        && this.probeElapsed >= this.nextProbeTick) {
                    if (this.forageSucceeds() && this.woodcock.level() instanceof ServerLevel serverLevel) {
                        this.exposedWorm = EarthwormEntity.spawn(serverLevel,
                                Vec3.atBottomCenterOf(this.target).add(0.0D, 0.01D, 0.0D), true);
                        this.foundFood = this.exposedWorm != null;
                    }
                    --this.probeAttemptsRemaining;
                    this.nextProbeTick += 20 + this.woodcock.getRandom().nextInt(9);
                }
                if (this.phaseTicks <= 0) {
                    if (!this.foundFood || this.exposedWorm == null || !this.exposedWorm.isAlive()
                            || this.woodcock.distanceToSqr(this.exposedWorm) > 4.0D
                            || !this.exposedWorm.tryEat()) {
                        this.woodcock.forageCooldown = this.woodcock.randomBetween(45, 95);
                        this.target = null;
                        return;
                    }
                    this.woodcock.birdBrain().onEat(0.22F);
                    this.phaseTicks = EAT_TICKS;
                    this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.EATING, EAT_TICKS);
                }
            } else if (state == WoodcockBehaviorState.EATING && --this.phaseTicks <= 0) {
                int maximum = this.woodcock.level().isRaining() ? 235 : 300;
                this.woodcock.forageCooldown = this.woodcock.randomBetween(120, maximum);
                this.target = null;
            }
        }

        @Override
        public void stop() {
            this.woodcock.getNavigation().stop();
            this.woodcock.releaseStationaryFacingLock();
            if (this.woodcock.getWoodcockBehaviorState().isForagingSequence()
                    || this.woodcock.getWoodcockBehaviorState().isRockWalk()) {
                this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
            }
            this.target = null;
            this.path = null;
            this.phaseTicks = 0;
            this.probeElapsed = 0;
            this.probeAttemptCount = 0;
            this.probeAttemptsRemaining = 0;
            this.foundFood = false;
            this.exposedWorm = null;
        }

        private boolean shouldForageNow() {
            if (this.woodcock.birdBrain().wantsForage()) {
                return true;
            }
            float hunger = this.woodcock.birdBrain().motivation().hunger();
            return hunger > 0.76F && this.woodcock.activityFactor() >= 0.08F
                    && this.woodcock.getRandom().nextFloat() < 0.24F;
        }

        private void beginApproachStep(WoodcockBehaviorState state) {
            this.woodcock.releaseStationaryFacingLock();
            this.phaseTicks = 5;
            this.woodcock.setWoodcockBehaviorState(state, this.phaseTicks);
            if (this.woodcock.getNavigation().isDone()) {
                this.woodcock.getNavigation().moveTo(this.path, 0.72D);
            } else {
                this.woodcock.getNavigation().setSpeedModifier(0.72D);
            }
        }

        private void beginApproachRock(WoodcockBehaviorState state) {
            float activity = this.woodcock.birdBrain().personality().activity();
            int pause = this.woodcock.randomBetween(8, 14) + Math.round((0.5F - activity) * 4.0F);
            this.phaseTicks = Math.max(7, pause);
            this.woodcock.getNavigation().setSpeedModifier(0.0D);
            this.woodcock.setDeltaMovement(this.woodcock.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            this.woodcock.lockStationaryFacing();
            this.woodcock.setWoodcockBehaviorState(state, this.phaseTicks);
        }

        private void beginProbe() {
            this.woodcock.getNavigation().stop();
            this.woodcock.setDeltaMovement(this.woodcock.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            this.phaseTicks = PROBE_TICKS;
            this.probeElapsed = 0;
            this.probeAttemptCount = 1 + this.woodcock.getRandom().nextInt(3);
            this.probeAttemptsRemaining = this.probeAttemptCount;
            this.nextProbeTick = 22 + this.woodcock.getRandom().nextInt(12);
            this.foundFood = this.exposedWorm != null && this.exposedWorm.isAlive();
            this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.FORAGING, PROBE_TICKS);
        }

        @Nullable
        private BlockPos findForageTarget() {
            BlockPos origin = this.woodcock.blockPosition();
            BlockPos best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            int samples = this.woodcock.randomBetween(8, 16);
            for (int attempt = 0; attempt < samples; ++attempt) {
                int x = this.woodcock.getRandom().nextInt(17) - 8;
                int z = this.woodcock.getRandom().nextInt(17) - 8;
                BlockPos stand = this.findStandPosition(origin.offset(x, 0, z));
                if (stand == null) {
                    continue;
                }
                BlockState groundState = this.woodcock.level().getBlockState(stand.below());
                double score = this.groundScore(groundState)
                        - this.woodcock.distanceToSqr(Vec3.atBottomCenterOf(stand)) * 0.025D
                        + (this.woodcock.level().canSeeSky(stand) ? -0.38D : 0.48D)
                        + (this.hasWaterNearby(stand) ? 0.24D : 0.0D)
                        + this.woodcock.getRandom().nextDouble() * 0.35D;
                if (score > bestScore) {
                    best = stand.immutable();
                    bestScore = score;
                }
            }
            return best;
        }

        @Nullable
        private BlockPos findStandPosition(BlockPos column) {
            for (int y = 3; y >= -3; --y) {
                BlockPos stand = column.offset(0, y, 0);
                if (!this.woodcock.level().hasChunkAt(stand)
                        || !this.woodcock.isInsideTerritory(stand, WoodcockDefinition.NORMAL_TERRITORY_RADIUS)) {
                    continue;
                }
                BlockState ground = this.woodcock.level().getBlockState(stand.below());
                if (ground.is(BirdTags.WOODCOCK_FORAGE_GROUND)
                        && this.woodcock.level().getFluidState(stand).isEmpty()
                        && this.woodcock.level().getBlockState(stand).getCollisionShape(this.woodcock.level(), stand).isEmpty()
                        && this.woodcock.level().getBlockState(stand.above())
                        .getCollisionShape(this.woodcock.level(), stand.above()).isEmpty()) {
                    return stand.immutable();
                }
            }
            return null;
        }

        private boolean hasWaterNearby(BlockPos stand) {
            for (int x = -2; x <= 2; x += 2) {
                for (int z = -2; z <= 2; z += 2) {
                    BlockPos sample = stand.offset(x, -1, z);
                    if (this.woodcock.level().hasChunkAt(sample)
                            && !this.woodcock.level().getFluidState(sample).isEmpty()) {
                        return true;
                    }
                }
            }
            return false;
        }

        private double groundScore(BlockState state) {
            if (state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.PODZOL)) {
                return 1.0D;
            }
            if (state.is(Blocks.ROOTED_DIRT)) {
                return 0.95D;
            }
            if (state.is(Blocks.MUD)) {
                return 0.92D;
            }
            if (state.is(Blocks.GRASS_BLOCK)) {
                return 0.84D;
            }
            if (state.is(Blocks.DIRT)) {
                return 0.72D;
            }
            return state.is(Blocks.COARSE_DIRT) ? 0.46D : 0.38D;
        }

        private boolean forageSucceeds() {
            float chance = this.woodcock.level().isThundering()
                    ? 0.35F : this.woodcock.level().isRaining() ? 0.31F : 0.18F;
            if (this.target != null) {
                BlockState ground = this.woodcock.level().getBlockState(this.target.below());
                if (ground.is(Blocks.MUD) || ground.is(Blocks.PODZOL)
                        || ground.is(Blocks.MOSS_BLOCK) || ground.is(Blocks.ROOTED_DIRT)) {
                    chance += 0.025F;
                }
            }
            // Preserve the advertised weather chance for the whole probing sequence,
            // even though one animation may contain several individual bill probes.
            float perProbeChance = (float)(1.0D - Math.pow(1.0D - chance,
                    1.0D / Math.max(1, this.probeAttemptCount)));
            return this.woodcock.getRandom().nextFloat() < perProbeChance;
        }
    }

    private static final class WoodcockSeekCoverGoal extends Goal {
        private final WoodcockEntity woodcock;
        @Nullable
        private BlockPos target;
        @Nullable
        private Path path;
        private int travelTicks;

        private WoodcockSeekCoverGoal(WoodcockEntity woodcock) {
            this.woodcock = woodcock;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            boolean exposed = this.woodcock.level().canSeeSky(this.woodcock.blockPosition());
            boolean needsCover = this.woodcock.shouldSeekDayCover()
                    || this.woodcock.postLandingCoverTicks > 0 && exposed;
            if (this.woodcock.forcedRockWalkTest || !needsCover || this.woodcock.shelterScanCooldown > 0
                    || this.woodcock.birdBrain().senses().temptingPlayerNearby()
                    || this.woodcock.getWoodcockBehaviorState() != WoodcockBehaviorState.IDLE
                    || !this.woodcock.onGround() || this.woodcock.isBirdFlightActive()
                    || this.woodcock.isInWaterOrBubble() || !this.woodcock.getNavigation().isDone()
                    || this.woodcock.hurtTime > 0 || this.woodcock.birdBrain().computeRiskScore() >= 0.70F) {
                return false;
            }
            if (!exposed && this.woodcock.birdBrain().senses().nearCover()) {
                return false;
            }
            this.woodcock.shelterScanCooldown = this.woodcock.randomBetween(80, 150);
            if (this.woodcock.level() instanceof ServerLevel serverLevel
                    && !BirdScanBudget.tryAcquire(serverLevel, this.woodcock, 3)) {
                return false;
            }
            this.path = this.findShelterPath();
            return this.path != null && this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.path != null && this.travelTicks > 0
                    && this.woodcock.getWoodcockBehaviorState() == WoodcockBehaviorState.SEEK_COVER
                    && this.woodcock.onGround() && !this.woodcock.isBirdFlightActive()
                    && !this.woodcock.isInWaterOrBubble() && this.woodcock.hurtTime <= 0
                    && this.woodcock.birdBrain().computeRiskScore() < 0.74F
                    && !this.woodcock.getNavigation().isDone();
        }

        @Override
        public void start() {
            this.travelTicks = 240;
            this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.SEEK_COVER, 0);
            this.woodcock.getNavigation().moveTo(this.path, 0.82D);
        }

        @Override
        public void tick() {
            if (this.woodcock.isSpontaneouslyStill()) {
                return;
            }
            --this.travelTicks;
            if (this.target != null) {
                this.woodcock.getLookControl().setLookAt(
                        this.target.getX() + 0.5D, this.target.getY(), this.target.getZ() + 0.5D,
                        22.0F, this.woodcock.getMaxHeadXRot());
                if (this.woodcock.blockPosition().distSqr(this.target) <= 2.25D) {
                    this.travelTicks = 0;
                }
            }
        }

        @Override
        public void stop() {
            boolean reached = this.target != null && this.woodcock.blockPosition().distSqr(this.target) <= 4.0D;
            this.woodcock.getNavigation().stop();
            if (this.woodcock.getWoodcockBehaviorState() == WoodcockBehaviorState.SEEK_COVER) {
                this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
            }
            if (reached) {
                this.woodcock.postLandingCoverTicks = 0;
                this.woodcock.onReachedDayCover();
                this.woodcock.shelterScanCooldown = this.woodcock.randomBetween(180, 300);
            }
            this.target = null;
            this.path = null;
            this.travelTicks = 0;
        }

        @Nullable
        private Path findShelterPath() {
            BlockPos[] best = new BlockPos[4];
            double[] scores = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
            BlockPos origin = this.woodcock.blockPosition();
            for (int sample = 0; sample < 16; ++sample) {
                int x = this.woodcock.getRandom().nextInt(29) - 14;
                int z = this.woodcock.getRandom().nextInt(29) - 14;
                BlockPos stand = this.findStandPosition(origin.offset(x, 0, z));
                if (stand == null) {
                    continue;
                }
                double score = this.coverScore(stand)
                        - this.woodcock.distanceToSqr(Vec3.atBottomCenterOf(stand)) * 0.012D
                        + this.woodcock.getRandom().nextDouble() * 0.30D;
                for (int index = 0; index < best.length; ++index) {
                    if (score > scores[index]) {
                        for (int shift = best.length - 1; shift > index; --shift) {
                            scores[shift] = scores[shift - 1];
                            best[shift] = best[shift - 1];
                        }
                        scores[index] = score;
                        best[index] = stand;
                        break;
                    }
                }
            }
            for (int index = 0; index < 3 && index < best.length; ++index) {
                if (best[index] == null || scores[index] < 2.0D) {
                    continue;
                }
                Path candidate = this.woodcock.getNavigation().createPath(best[index], 0);
                if (candidate != null) {
                    this.target = best[index];
                    return candidate;
                }
            }
            return null;
        }

        @Nullable
        private BlockPos findStandPosition(BlockPos column) {
            for (int y = 4; y >= -4; --y) {
                BlockPos stand = column.offset(0, y, 0);
                if (!this.woodcock.level().hasChunkAt(stand)
                        || !this.woodcock.isInsideTerritory(stand, WoodcockDefinition.NORMAL_TERRITORY_RADIUS)) {
                    continue;
                }
                BlockState ground = this.woodcock.level().getBlockState(stand.below());
                if (!ground.isFaceSturdy(this.woodcock.level(), stand.below(), Direction.UP)
                        || ground.is(Blocks.MAGMA_BLOCK) || ground.is(Blocks.CAMPFIRE)
                        || ground.is(Blocks.SOUL_CAMPFIRE)
                        || !this.woodcock.level().getFluidState(stand).isEmpty()
                        || !this.woodcock.level().getBlockState(stand)
                        .getCollisionShape(this.woodcock.level(), stand).isEmpty()
                        || !this.woodcock.level().getBlockState(stand.above())
                        .getCollisionShape(this.woodcock.level(), stand.above()).isEmpty()) {
                    continue;
                }
                return stand.immutable();
            }
            return null;
        }

        private double coverScore(BlockPos stand) {
            double score = this.woodcock.level().canSeeSky(stand) ? -2.0D : 5.0D;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int x = -2; x <= 2; x += 2) {
                for (int z = -2; z <= 2; z += 2) {
                    for (int y = 0; y <= 4; ++y) {
                        cursor.set(stand.getX() + x, stand.getY() + y, stand.getZ() + z);
                        if (!this.woodcock.level().hasChunkAt(cursor)) {
                            continue;
                        }
                        BlockState state = this.woodcock.level().getBlockState(cursor);
                        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                            score += 0.45D;
                        } else if (state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                                || state.is(Blocks.TALL_GRASS)) {
                            score += 0.12D;
                        }
                    }
                }
            }
            if (this.woodcock.level().getBlockState(stand.below()).is(BirdTags.WOODCOCK_FORAGE_GROUND)) {
                score += 1.0D;
            }
            return score;
        }
    }

    private enum GroundMoveStyle {
        NORMAL,
        ROCK,
        SPECIAL_IDLE
    }

    /** Owns ordinary ground life so wandering, rocking and special idle no longer compete for MOVE. */
    private static final class WoodcockGroundRoamGoal extends Goal {
        private final WoodcockEntity woodcock;
        @Nullable private Vec3 target;
        @Nullable private Path path;
        private GroundMoveStyle style = GroundMoveStyle.NORMAL;
        private boolean finished;

        private WoodcockGroundRoamGoal(WoodcockEntity woodcock) {
            this.woodcock = woodcock;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            this.finished = false;
            this.target = null;
            this.path = null;
            boolean forced = this.woodcock.forcedRockWalkTest;
            if (this.woodcock.getWoodcockBehaviorState() != WoodcockBehaviorState.IDLE
                    || !this.woodcock.onGround()
                    || this.woodcock.isBirdFlightActive()
                    || this.woodcock.isInWaterOrBubble()
                    || !this.woodcock.getNavigation().isDone()
                    || this.woodcock.hurtTime > 0) {
                return false;
            }
            if (!forced && (this.woodcock.roamCooldown > 0
                    || !this.woodcock.canStartCalmAction())) {
                return false;
            }

            // Decide once per episode. A failed target search receives a short retry delay
            // instead of rerolling every tick and synchronising nearby birds.
            this.woodcock.roamCooldown = this.woodcock.randomBetween(20, 55);
            if (!forced && this.woodcock.idleCooldown <= 0
                    && this.woodcock.getRandom().nextFloat() < 0.20F) {
                this.style = GroundMoveStyle.SPECIAL_IDLE;
                return true;
            }

            this.target = forced ? this.woodcock.forcedRockWalkTarget : this.pickTarget();
            if (forced && this.woodcock.loopingRockWalkTest
                    && (this.target == null || this.woodcock.position().distanceToSqr(this.target) <= 0.85D)) {
                this.target = this.pickTarget();
            }
            if (this.target == null) {
                if (!this.woodcock.loopingRockWalkTest) {
                    this.clearForcedTest();
                }
                return false;
            }
            this.path = this.woodcock.getNavigation().createPath(BlockPos.containing(this.target), 0);
            if (this.path == null || !this.path.canReach()) {
                if (this.woodcock.loopingRockWalkTest) {
                    return this.selectReachableTarget();
                }
                this.clearForcedTest();
                return false;
            }
            this.style = forced ? GroundMoveStyle.ROCK : this.chooseMoveStyle(this.target);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.finished || this.woodcock.isBirdFlightActive()
                    || this.woodcock.isInWaterOrBubble() || this.woodcock.hurtTime > 0
                    || this.woodcock.getTarget() != null) {
                return false;
            }
            return switch (this.style) {
                case SPECIAL_IDLE -> this.woodcock.getWoodcockBehaviorState()
                        == WoodcockBehaviorState.SPECIAL_IDLE && this.woodcock.actionTicks > 0;
                case NORMAL -> this.woodcock.getWoodcockBehaviorState()
                        == WoodcockBehaviorState.NORMAL_WALK && !this.woodcock.getNavigation().isDone();
                case ROCK -> this.woodcock.getWoodcockBehaviorState().isRockWalk()
                        && this.woodcock.rockWalkSequencer.isRunning();
            };
        }

        @Override
        public void start() {
            switch (this.style) {
                case SPECIAL_IDLE -> this.woodcock.setWoodcockBehaviorState(
                        WoodcockBehaviorState.SPECIAL_IDLE, IDLE_VARIATION_TICKS);
                case NORMAL -> {
                    this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.NORMAL_WALK, 0);
                    this.woodcock.getNavigation().moveTo(this.path, NORMAL_WALK_SPEED);
                }
                case ROCK -> this.woodcock.rockWalkSequencer.start(
                        this.target, this.path, this.woodcock.loopingRockWalkTest);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.woodcock.isSpontaneouslyStill()) {
                return;
            }
            switch (this.style) {
                case SPECIAL_IDLE -> {
                    if (--this.woodcock.actionTicks <= 0) {
                        this.finished = true;
                    }
                }
                case NORMAL -> {
                    if (this.target != null) {
                        this.woodcock.getLookControl().setLookAt(
                                this.target.x, this.target.y, this.target.z,
                                18.0F, this.woodcock.getMaxHeadXRot());
                    }
                    if (this.woodcock.getNavigation().isDone()) {
                        this.finished = true;
                    }
                }
                case ROCK -> {
                    this.woodcock.rockWalkSequencer.tick();
                    if (!this.woodcock.rockWalkSequencer.isRunning()) {
                        if (this.woodcock.loopingRockWalkTest && this.selectReachableTarget()) {
                            this.woodcock.rockWalkSequencer.start(this.target, this.path, true);
                        } else {
                            this.finished = true;
                        }
                    }
                }
            }
        }

        @Override
        public void stop() {
            boolean resumeLoop = this.woodcock.loopingRockWalkTest && this.woodcock.isAlive();
            this.woodcock.cancelSpontaneousStillness();
            this.woodcock.rockWalkSequencer.stop();
            this.woodcock.getNavigation().stop();
            WoodcockBehaviorState state = this.woodcock.getWoodcockBehaviorState();
            if (state == WoodcockBehaviorState.NORMAL_WALK || state == WoodcockBehaviorState.SPECIAL_IDLE
                    || state.isRockWalk()) {
                this.woodcock.setWoodcockBehaviorState(WoodcockBehaviorState.IDLE, 0);
            }
            if (this.style == GroundMoveStyle.SPECIAL_IDLE) {
                this.woodcock.idleCooldown = this.woodcock.randomBetween(320, 820);
            }
            this.woodcock.roamCooldown = this.woodcock.randomBetween(20, 70);
            this.target = null;
            this.path = null;
            this.finished = false;
            if (resumeLoop) {
                this.woodcock.forcedRockWalkTest = true;
                this.woodcock.loopingRockWalkTest = true;
                this.woodcock.forcedRockWalkTarget = null;
                this.woodcock.roamCooldown = 0;
            } else {
                this.clearForcedTest();
            }
        }

        private GroundMoveStyle chooseMoveStyle(Vec3 moveTarget) {
            double distanceSqr = this.woodcock.position().distanceToSqr(moveTarget);
            if (distanceSqr > 36.0D) {
                return GroundMoveStyle.NORMAL;
            }
            float activity = this.woodcock.activityFactor();
            float rockChance = activity >= 0.80F ? 0.50F : activity >= 0.40F ? 0.40F : 0.30F;
            return this.woodcock.getRandom().nextFloat() < rockChance
                    ? GroundMoveStyle.ROCK : GroundMoveStyle.NORMAL;
        }

        private boolean selectReachableTarget() {
            for (int attempt = 0; attempt < 8; ++attempt) {
                Vec3 candidate = this.pickTarget();
                if (candidate == null || this.woodcock.position().distanceToSqr(candidate) <= 2.25D) {
                    continue;
                }
                Path candidatePath = this.woodcock.getNavigation().createPath(BlockPos.containing(candidate), 0);
                if (candidatePath != null && candidatePath.canReach()) {
                    this.target = candidate;
                    this.path = candidatePath;
                    this.woodcock.forcedRockWalkTarget = candidate;
                    this.style = GroundMoveStyle.ROCK;
                    return true;
                }
            }
            this.target = null;
            this.path = null;
            return false;
        }

        @Nullable
        private Vec3 pickTarget() {
            BlockPos home = this.woodcock.territoryCenter();
            if (home != null && !this.woodcock.isInsideTerritory(
                    this.woodcock.blockPosition(), WoodcockDefinition.NORMAL_TERRITORY_RADIUS)) {
                return Vec3.atBottomCenterOf(home);
            }
            for (int attempt = 0; attempt < 12; ++attempt) {
                Vec3 candidate = DefaultRandomPos.getPos(this.woodcock, 9, 4);
                if (candidate == null || !this.woodcock.isInsideTerritory(
                        BlockPos.containing(candidate), WoodcockDefinition.NORMAL_TERRITORY_RADIUS)) {
                    continue;
                }
                double x = candidate.x - this.woodcock.getX();
                double z = candidate.z - this.woodcock.getZ();
                double horizontalDistanceSqr = x * x + z * z;
                if (horizontalDistanceSqr >= 9.0D && horizontalDistanceSqr <= 81.0D) {
                    return candidate;
                }
            }
            return null;
        }

        private void clearForcedTest() {
            this.woodcock.forcedRockWalkTest = false;
            this.woodcock.loopingRockWalkTest = false;
            this.woodcock.forcedRockWalkTarget = null;
        }
    }

    /** Runs the signature two-step sequence; it never decides why the bird should move. */
    private static final class WoodcockRockWalkSequencer {
        private final WoodcockEntity woodcock;
        @Nullable private Vec3 target;
        @Nullable private Path path;
        private int phaseTicks;
        private int cyclesRemaining;
        private int swayLoopsRemaining;
        private boolean loop;
        private boolean running;

        private WoodcockRockWalkSequencer(WoodcockEntity woodcock) {
            this.woodcock = woodcock;
        }

        private void start(@Nullable Vec3 target, @Nullable Path path, boolean loop) {
            this.target = target;
            this.path = path;
            this.loop = loop;
            this.cyclesRemaining = this.woodcock.randomBetween(
                    ROCK_WALK_MIN_CYCLES, ROCK_WALK_MAX_CYCLES);
            this.swayLoopsRemaining = 0;
            this.running = target != null && path != null;
            if (!this.running) {
                return;
            }
            this.woodcock.getNavigation().moveTo(path, ROCK_WALK_SWAY_SPEED);
            if (loop) {
                this.beginAtIndividualLoopPhase();
            } else {
                this.beginStep(WoodcockBehaviorState.STEP_1);
            }
        }

        private boolean isRunning() {
            return this.running;
        }

        private void tick() {
            if (!this.running || this.woodcock.isSpontaneouslyStill()) {
                return;
            }
            if (this.target == null || this.woodcock.position().distanceToSqr(this.target) <= 0.85D) {
                this.running = false;
                return;
            }
            if (this.woodcock.getNavigation().isDone()) {
                this.running = false;
                return;
            }
            if (--this.phaseTicks > 0) {
                return;
            }
            switch (this.woodcock.getWoodcockBehaviorState()) {
                case STEP_1 -> this.beginRock(WoodcockBehaviorState.ROCK_1);
                case ROCK_1 -> {
                    if (!this.continueRock(WoodcockBehaviorState.ROCK_1)) {
                        this.beginStep(WoodcockBehaviorState.STEP_2);
                    }
                }
                case STEP_2 -> this.beginRock(WoodcockBehaviorState.ROCK_2);
                case ROCK_2 -> {
                    if (!this.continueRock(WoodcockBehaviorState.ROCK_2)) {
                        if (this.loop || --this.cyclesRemaining > 0) {
                            this.beginStep(WoodcockBehaviorState.STEP_1);
                        } else {
                            this.running = false;
                        }
                    }
                }
                default -> this.running = false;
            }
        }

        private void pause() {
            if (this.running) {
                this.woodcock.getNavigation().setSpeedModifier(0.0D);
            }
        }

        private void resume() {
            if (this.running) {
                this.woodcock.restoreGroundBehaviorSpeed();
            }
        }

        private void stop() {
            this.target = null;
            this.path = null;
            this.phaseTicks = 0;
            this.cyclesRemaining = 0;
            this.swayLoopsRemaining = 0;
            this.loop = false;
            this.running = false;
            this.woodcock.releaseStationaryFacingLock();
        }

        private void beginStep(WoodcockBehaviorState state) {
            this.swayLoopsRemaining = 0;
            this.woodcock.releaseStationaryFacingLock();
            this.phaseTicks = ROCK_WALK_STEP_TICKS;
            this.woodcock.setWoodcockBehaviorState(state, this.phaseTicks);
            if (this.path != null && this.woodcock.getNavigation().isDone()) {
                this.woodcock.getNavigation().moveTo(this.path, ROCK_WALK_STEP_SPEED);
            } else {
                this.woodcock.getNavigation().setSpeedModifier(ROCK_WALK_STEP_SPEED);
            }
        }

        private void beginRock(WoodcockBehaviorState state) {
            this.swayLoopsRemaining = this.woodcock.randomBetween(
                    ROCK_WALK_MIN_SWAYS_PER_STEP, ROCK_WALK_MAX_SWAYS_PER_STEP);
            this.beginRockLoop(state);
        }

        private boolean continueRock(WoodcockBehaviorState state) {
            if (--this.swayLoopsRemaining <= 0) {
                return false;
            }
            this.beginRockLoop(state);
            return true;
        }

        private void beginRockLoop(WoodcockBehaviorState state) {
            this.phaseTicks = ROCK_WALK_SWAY_LOOP_TICKS;
            this.woodcock.getNavigation().setSpeedModifier(ROCK_WALK_SWAY_SPEED);
            Vec3 movement = this.woodcock.getDeltaMovement();
            this.woodcock.setDeltaMovement(0.0D, movement.y, 0.0D);
            this.woodcock.lockStationaryFacing();
            this.woodcock.setWoodcockBehaviorState(state, this.phaseTicks);
        }

        private void beginAtIndividualLoopPhase() {
            switch (Math.floorMod(this.woodcock.getUUID().hashCode(), 4)) {
                case 0 -> this.beginStep(WoodcockBehaviorState.STEP_1);
                case 1 -> this.beginRock(WoodcockBehaviorState.ROCK_1);
                case 2 -> this.beginStep(WoodcockBehaviorState.STEP_2);
                default -> this.beginRock(WoodcockBehaviorState.ROCK_2);
            }
        }
    }

    /** Temporarily freezes eligible ground behavior without owning MOVE or replacing its state. */
    private static final class WoodcockStillnessController {
        private final WoodcockEntity woodcock;
        private int remainingTicks;
        private int nextDecisionTicks;

        private WoodcockStillnessController(WoodcockEntity woodcock) {
            this.woodcock = woodcock;
            this.nextDecisionTicks = woodcock.randomBetween(100, 500);
        }

        private void tick() {
            if (this.woodcock.isSpontaneouslyStill()) {
                if (!this.woodcock.canEnterSpontaneousStillness()) {
                    this.cancel();
                    return;
                }
                this.freezeMovement();
                if (--this.remainingTicks <= 0) {
                    this.cancel();
                }
                return;
            }
            if (--this.nextDecisionTicks > 0) {
                return;
            }
            this.nextDecisionTicks = this.woodcock.randomBetween(120, 600);
            if (this.woodcock.canEnterSpontaneousStillness()
                    && this.woodcock.getRandom().nextFloat() < 0.55F) {
                this.begin();
            }
        }

        private void begin() {
            this.remainingTicks = this.chooseDuration();
            this.woodcock.entityData.set(SPONTANEOUS_STILL, true);
            this.woodcock.rockWalkSequencer.pause();
            this.woodcock.lockStationaryFacing();
            this.freezeMovement();
        }

        private void cancel() {
            if (!this.woodcock.isSpontaneouslyStill()) {
                this.remainingTicks = 0;
                return;
            }
            this.woodcock.entityData.set(SPONTANEOUS_STILL, false);
            this.remainingTicks = 0;
            this.woodcock.rockWalkSequencer.resume();
            this.woodcock.restoreGroundBehaviorSpeed();
            WoodcockBehaviorState state = this.woodcock.getWoodcockBehaviorState();
            if (state != WoodcockBehaviorState.ROCK_1 && state != WoodcockBehaviorState.ROCK_2) {
                this.woodcock.releaseStationaryFacingLock();
            }
        }

        private void freezeMovement() {
            this.woodcock.getNavigation().setSpeedModifier(0.0D);
            Vec3 movement = this.woodcock.getDeltaMovement();
            this.woodcock.setDeltaMovement(0.0D, movement.y, 0.0D);
            this.woodcock.applyStationaryFacingLock();
        }

        private int chooseDuration() {
            float roll = this.woodcock.getRandom().nextFloat();
            if (roll < 0.60F) {
                return this.woodcock.randomBetween(15, 45);
            }
            if (roll < 0.90F) {
                return this.woodcock.randomBetween(46, 90);
            }
            if (roll < 0.99F) {
                return this.woodcock.randomBetween(91, 140);
            }
            return this.woodcock.randomBetween(141, 200);
        }
    }
}
