package EdDYON.guaniao.content.bird.cassowary;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.mutation.BirdMutation;
import EdDYON.guaniao.content.bird.mutation.BirdMutationHolder;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A solitary, ground-only cassowary. Goals own movement and interaction while
 * this entity owns shared intent, threat memory, trust, gaze and animation data.
 */
public class CassowaryEntity extends PathfinderMob
        implements GeoEntity, ScalableBirdModel, BirdMutationHolder {
    private static final EntityDataAccessor<Integer> BEHAVIOR_STATE =
            SynchedEntityData.defineId(CassowaryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MODEL_SCALE =
            SynchedEntityData.defineId(CassowaryEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MUTATION =
            SynchedEntityData.defineId(CassowaryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GAZE_TARGET_ID =
            SynchedEntityData.defineId(CassowaryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> GAZE_YAW =
            SynchedEntityData.defineId(CassowaryEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GAZE_PITCH =
            SynchedEntityData.defineId(CassowaryEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GAZE_WEIGHT =
            SynchedEntityData.defineId(CassowaryEntity.class, EntityDataSerializers.FLOAT);

    private static final String MUTATION_NBT_KEY = "BirdMutation";
    private static final String HOME_NBT_KEY = "CassowaryHome";
    private static final String TRUST_NBT_KEY = "CassowaryTrust";
    private static final int TRUST_DURATION_TICKS = 20 * 60 * 10;
    private static final double TRUST_SHARE_RADIUS = 48.0D;
    private static final int GAZE_LAST_SEEN_TICKS = 30;
    private static final double GAZE_SWITCH_MARGIN = 64.0D;
    private static final double TERRITORY_PURSUIT_LIMIT = 32.0D;
    static final int WARNING_TICKS = 57;
    static final int EATING_TICKS = 75;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation LOOKING_ANIMATION = RawAnimation.begin().thenPlay("idle_looking");
    private static final RawAnimation ALERT_ANIMATION = RawAnimation.begin()
            .thenPlay("idle_alerting").thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("running");
    private static final RawAnimation TROT_ANIMATION = RawAnimation.begin().thenLoop("trotting");
    private static final RawAnimation SPRINT_ANIMATION = RawAnimation.begin().thenLoop("sprinting");
    private static final RawAnimation WARNING_ANIMATION = RawAnimation.begin().thenPlay("warning");
    private static final RawAnimation HURT_ANIMATION = RawAnimation.begin().thenPlay("hurt");
    private static final RawAnimation SWOOP_ANIMATION = RawAnimation.begin().thenPlay("swoop");
    private static final RawAnimation PECK_ANIMATION = RawAnimation.begin().thenPlay("peck");
    private static final RawAnimation EAT_ANIMATION = RawAnimation.begin().thenPlay("sawllow");
    private static final RawAnimation SLEEP_ENTER_ANIMATION = RawAnimation.begin().thenPlay("sleeping_ready");
    private static final RawAnimation SLEEP_ANIMATION = RawAnimation.begin().thenLoop("sleeping_idle");
    private static final RawAnimation SLEEP_EXIT_ANIMATION = RawAnimation.begin().thenPlay("sleeping_finish");
    private static final RawAnimation REST_ENTER_ANIMATION = RawAnimation.begin().thenPlay("rest_ready");
    private static final RawAnimation REST_ANIMATION = RawAnimation.begin().thenLoop("rest_idle");
    private static final RawAnimation REST_EXIT_ANIMATION = RawAnimation.begin().thenPlay("rest_finish");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final Map<UUID, Long> trustedPlayers = new HashMap<>();

    @Nullable private BlockPos homeCenter;
    @Nullable private UUID threatTargetUuid;
    @Nullable private UUID gazeTargetUuid;
    @Nullable private Vec3 lastSeenThreatPos;
    @Nullable private Vec3 lastSeenGazePos;
    @Nullable private Vec3 calmGazePosition;
    @Nullable private ItemEntity foodTarget;
    private int behaviorTicks;
    private int lostSightTicks;
    private int attackCooldown;
    private int warningCooldown;
    private int swoopCooldown;
    private int provokedTicks;
    private int threatScanCooldown;
    private int gazeScanCooldown;
    private int gazeTargetHoldTicks;
    private int lastSeenGazeTicks;
    private int foodScanCooldown;
    private int calmGazeTicks;
    private int glanceAwayTicks;
    // Selects which lateral eye leads the current observation; eye bones never rotate.
    private int gazeSideSign = 1;
    private float threatLevel;
    private boolean foodConsumed;

    private float clientHeadYaw;
    private float clientHeadPitch;
    private float clientUpperNeckYaw;
    private float clientUpperNeckPitch;
    private float clientLowerNeckYaw;
    private float clientLowerNeckPitch;
    private float clientGazeWeight;
    private GuidePreviewAnimation guidePreviewAnimation = GuidePreviewAnimation.NONE;

    public CassowaryEntity(EntityType<? extends CassowaryEntity> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, 8.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DAMAGE_FIRE, 16.0F);
        this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.1D);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, CassowaryDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, CassowaryDefinition.WALK_SPEED)
                .add(Attributes.ATTACK_DAMAGE, CassowaryDefinition.ATTACK_DAMAGE)
                .add(Attributes.ATTACK_KNOCKBACK, CassowaryDefinition.ATTACK_KNOCKBACK)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D)
                .add(Attributes.FOLLOW_RANGE, CassowaryDefinition.FOLLOW_RANGE);
    }

    public static boolean canSpawn(EntityType<CassowaryEntity> entityType, ServerLevelAccessor level,
                                   MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean validGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(Blocks.MOSS_BLOCK)
                || below.is(Blocks.PODZOL)
                || below.is(Blocks.ROOTED_DIRT);
        if (!validGround || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        for (int y = 0; y <= 2; y++) {
            BlockPos check = pos.above(y);
            if (!level.getFluidState(check).isEmpty()
                    || !level.getBlockState(check).getCollisionShape(level, check).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CassowaryTerritoryGoal(this));
        this.goalSelector.addGoal(2, new CassowarySleepGoal(this));
        this.goalSelector.addGoal(3, new CassowaryRestGoal(this));
        this.goalSelector.addGoal(4, new CassowaryForageGoal(this));
        this.goalSelector.addGoal(5, new CassowaryPersonalSpaceGoal(this));
        this.goalSelector.addGoal(6, new CassowaryPatrolGoal(this));
        this.goalSelector.addGoal(7, new CassowaryIdleGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation navigation = new GroundPathNavigation(this, level);
        navigation.setCanFloat(false);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BEHAVIOR_STATE, CassowaryBehaviorState.CALM.ordinal());
        builder.define(MODEL_SCALE, BirdModelScale.DEFAULT_INDIVIDUAL_SCALE);
        builder.define(MUTATION, BirdMutation.NONE.ordinal());
        builder.define(GAZE_TARGET_ID, -1);
        builder.define(GAZE_YAW, 0.0F);
        builder.define(GAZE_PITCH, 0.0F);
        builder.define(GAZE_WEIGHT, 0.0F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        this.setIndividualModelScale(BirdModelScale.randomIndividualScale(this.getRandom(), this.modelScaleProfile()));
        this.setBirdMutation(BirdMutation.randomMutation(this.getRandom()));
        if (this.homeCenter == null) {
            this.homeCenter = this.blockPosition().immutable();
        }
        this.threatScanCooldown = this.getRandom().nextInt(10);
        this.gazeScanCooldown = this.getRandom().nextInt(5);
        this.foodScanCooldown = this.getRandom().nextInt(20);
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            this.tickClientGaze();
            return;
        }
        if (this.homeCenter == null) {
            this.homeCenter = this.blockPosition().immutable();
        }
        ++this.behaviorTicks;
        if (this.attackCooldown > 0) --this.attackCooldown;
        if (this.warningCooldown > 0) --this.warningCooldown;
        if (this.swoopCooldown > 0) --this.swoopCooldown;
        if (this.provokedTicks > 0) --this.provokedTicks;
        if (this.threatScanCooldown > 0) --this.threatScanCooldown;
        if (this.gazeScanCooldown > 0) --this.gazeScanCooldown;
        if (this.gazeTargetHoldTicks > 0) --this.gazeTargetHoldTicks;
        if (this.lastSeenGazeTicks > 0) --this.lastSeenGazeTicks;
        if (this.foodScanCooldown > 0) --this.foodScanCooldown;
        this.trustedPlayers.entrySet().removeIf(entry -> entry.getValue() <= this.level().getGameTime());
        this.scanGazeTarget();
        this.updateServerGaze();
    }

    @Nullable
    LivingEntity acquireTerritoryTarget() {
        if (this.isOutsideTerritoryLimit() && !this.isTerritoryState()) {
            if (this.threatTargetUuid != null) {
                this.clearThreatTarget();
            }
            return null;
        }
        LivingEntity current = this.resolveThreatTarget();
        if (current != null && current.isAlive() && this.distanceToSqr(current) <= 32.0D * 32.0D
                && (!(current instanceof Player player) || (!player.isCreative() && !this.isTrusted(player)))) {
            return current;
        }
        if (this.threatTargetUuid != null) {
            this.clearThreatTarget();
        }
        if (this.threatScanCooldown > 0
                || !(this.level() instanceof ServerLevel serverLevel)
                || !BirdScanBudget.tryAcquire(serverLevel, this)) {
            return null;
        }
        this.threatScanCooldown = Math.max(5, BirdConfigManager.threatScanInterval(BirdSpecies.CASSOWARY));
        Player nearest = this.level().getEntitiesOfClass(Player.class,
                        this.getBoundingBox().inflate(16.0D, 8.0D, 16.0D),
                        player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
                                && !this.isTrusted(player) && this.getSensing().hasLineOfSight(player))
                .stream().min(Comparator.comparingDouble(player ->
                        this.distanceToSqr(player) - (this.isClosingThreat(player) ? 18.0D : 0.0D)))
                .orElse(null);
        if (nearest != null) {
            this.rememberThreat(nearest);
        }
        return nearest;
    }

    void rememberThreat(LivingEntity target) {
        this.threatTargetUuid = target.getUUID();
        this.setGazeTarget(target);
        if (this.getSensing().hasLineOfSight(target)) {
            this.recordVisibleThreat(target);
        }
    }

    void recordVisibleThreat(LivingEntity target) {
        this.lastSeenThreatPos = target.getEyePosition();
        this.lastSeenGazePos = this.lastSeenThreatPos;
        this.lastSeenGazeTicks = GAZE_LAST_SEEN_TICKS;
        this.lostSightTicks = 0;
    }

    void incrementLostSightTicks() {
        ++this.lostSightTicks;
    }

    int getLostSightTicks() {
        return this.lostSightTicks;
    }

    @Nullable
    LivingEntity resolveThreatTarget() {
        if (this.threatTargetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.threatTargetUuid);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    void clearThreatTarget() {
        this.threatTargetUuid = null;
        this.lastSeenThreatPos = null;
        this.lostSightTicks = 0;
        this.threatLevel = 0.0F;
    }

    boolean hasImmediateThreat() {
        LivingEntity target = this.acquireTerritoryTarget();
        return target != null && this.distanceToSqr(target) <= 18.0D * 18.0D;
    }

    boolean hasThreatWithin(double distance) {
        LivingEntity target = this.acquireTerritoryTarget();
        return target != null && this.distanceToSqr(target) <= distance * distance;
    }

    void updateThreatLevel(LivingEntity target, double distance, boolean visible) {
        if (target instanceof Player player) {
            if (player.isCreative() || this.isTrusted(player)) {
                this.threatLevel = Mth.clamp(this.threatLevel - 0.04F, 0.0F, 1.0F);
                return;
            }
            float delta = 0.001F;
            if (distance < 10.0D) delta += 0.002F;
            if (distance < 6.0D) delta += 0.004F;
            if (distance < 4.0D) delta += 0.008F;
            if (this.isClosingThreat(player)) delta += 0.010F;
            if (visible && this.isPlayerLookingAtBird(player)) delta += 0.003F;
            if (!visible) delta -= 0.008F;
            if (distance > 11.5D || this.isMovingAway(player)) delta -= 0.010F;
            this.threatLevel = Mth.clamp(this.threatLevel + delta, 0.0F, 1.0F);
        } else {
            this.threatLevel = Mth.clamp(this.threatLevel + (visible ? 0.02F : -0.006F), 0.0F, 1.0F);
        }
    }

    float getThreatLevel() {
        return this.threatLevel;
    }

    void setThreatLevel(float value) {
        this.threatLevel = Mth.clamp(value, 0.0F, 1.0F);
    }

    boolean wasRecentlyProvoked() {
        return this.provokedTicks > 0;
    }

    boolean isClosingThreat(LivingEntity entity) {
        Vec3 toBird = this.position().subtract(entity.position()).multiply(1.0D, 0.0D, 1.0D);
        Vec3 motion = entity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        if (toBird.lengthSqr() <= 1.0E-5D || motion.lengthSqr() <= 1.0E-5D) {
            return false;
        }
        double toward = motion.normalize().dot(toBird.normalize());
        return entity instanceof Player player ? player.isSprinting() && toward > 0.65D : toward > 0.65D;
    }

    private boolean isMovingAway(LivingEntity entity) {
        Vec3 toBird = this.position().subtract(entity.position()).multiply(1.0D, 0.0D, 1.0D);
        Vec3 motion = entity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        return toBird.lengthSqr() > 1.0E-5D && motion.lengthSqr() > 1.0E-5D
                && motion.normalize().dot(toBird.normalize()) < -0.35D;
    }

    private boolean isPlayerLookingAtBird(Player player) {
        Vec3 toBird = this.getEyePosition().subtract(player.getEyePosition());
        return toBird.lengthSqr() > 1.0E-5D
                && player.getViewVector(1.0F).normalize().dot(toBird.normalize()) > 0.82D;
    }

    void performTerritorialHit(LivingEntity target) {
        if (this.doHurtTarget(target)) {
            Vec3 away = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() > 1.0E-5D) {
                away = away.normalize();
                target.knockback(1.15D, -away.x, -away.z);
            }
        }
        this.attackCooldown = 52;
    }

    @Nullable
    Vec3 findWithdrawPosition(LivingEntity target) {
        Vec3 result = DefaultRandomPos.getPosAway(this, 8, 4, target.position());
        if (result != null) {
            return result;
        }
        Vec3 away = this.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() <= 1.0E-5D) {
            away = directionFromYaw(this.getYRot() + 180.0F);
        }
        float side = this.getRandom().nextBoolean() ? 12.0F : -12.0F;
        Vec3 direction = directionFromYaw(yawTo(this.position().add(away)) + side);
        return this.position().add(direction.scale(this.randomBetween(4, 7)));
    }

    void trySwoopAction() {
        if (this.swoopCooldown > 0 || !this.onGround()
                || this.getDeltaMovement().horizontalDistanceSqr() < 0.075D) {
            return;
        }
        Vec3 forward = directionFromYaw(this.getYRot());
        BlockPos ahead = BlockPos.containing(this.position().add(forward.scale(1.25D)));
        boolean lowObstacle = !this.level().getBlockState(ahead).getCollisionShape(this.level(), ahead).isEmpty()
                && this.level().getBlockState(ahead.above()).getCollisionShape(this.level(), ahead.above()).isEmpty();
        if (!this.horizontalCollision && !lowObstacle && this.getRandom().nextInt(240) != 0) {
            return;
        }
        this.getJumpControl().jump();
        this.triggerAnim("action", "swoop");
        this.swoopCooldown = this.randomBetween(120, 300);
    }

    int getAttackCooldown() {
        return this.attackCooldown;
    }

    int getWarningCooldown() {
        return this.warningCooldown;
    }

    void setWarningCooldown(int ticks) {
        this.warningCooldown = Math.max(0, ticks);
    }

    private void scanGazeTarget() {
        LivingEntity threat = this.resolveThreatTarget();
        if (threat != null && threat.isAlive()
                && this.distanceToSqr(threat) <= 32.0D * 32.0D) {
            this.setGazeTarget(threat);
            return;
        }
        if (this.gazeScanCooldown > 0) {
            return;
        }
        this.gazeScanCooldown = 5;

        LivingEntity current = this.resolveGazeTarget();
        if (current != null) {
            if (this.distanceToSqr(current) > 26.0D * 26.0D
                    || current instanceof Player player && player.isSpectator()) {
                this.setGazeTarget(null);
                current = null;
            } else {
                boolean visible = this.getSensing().hasLineOfSight(current);
                if (visible) {
                    this.lastSeenGazePos = current.getEyePosition();
                    this.lastSeenGazeTicks = GAZE_LAST_SEEN_TICKS;
                }
                if (this.gazeTargetHoldTicks > 0
                        || !visible && this.lastSeenGazeTicks > 0) {
                    return;
                }
            }
        }

        List<Player> players = this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(24.0D, 10.0D, 24.0D),
                player -> player.isAlive() && !player.isSpectator()
                        && this.getSensing().hasLineOfSight(player));
        Player selected = players.stream()
                .min(Comparator.comparingDouble(this::gazeTargetScore))
                .orElse(null);

        if (current instanceof Player currentPlayer
                && currentPlayer.isAlive()
                && this.getSensing().hasLineOfSight(currentPlayer)) {
            if (selected == null) {
                return;
            }
            if (selected != currentPlayer
                    && this.gazeTargetScore(selected)
                    >= this.gazeTargetScore(currentPlayer) - GAZE_SWITCH_MARGIN) {
                return;
            }
        }

        if (selected != null) {
            this.setGazeTarget(selected);
        } else if (this.lastSeenGazeTicks <= 0) {
            this.setGazeTarget(null);
        }
    }

    private double gazeTargetScore(Player player) {
        double score = this.distanceToSqr(player);
        if (!this.isTrusted(player) && !player.isCreative()) {
            score -= 180.0D;
        }
        if (!this.isTrusted(player) && this.isClosingThreat(player)) {
            score -= 80.0D;
        }
        return score;
    }

    private void setGazeTarget(@Nullable LivingEntity target) {
        if (target == null) {
            if (this.gazeTargetUuid != null && this.lastSeenGazePos != null) {
                this.lastSeenGazeTicks = Math.max(this.lastSeenGazeTicks, 16);
            }
            this.gazeTargetUuid = null;
            this.gazeTargetHoldTicks = 0;
            this.entityData.set(GAZE_TARGET_ID, -1);
            return;
        }
        UUID targetUuid = target.getUUID();
        boolean changed = !targetUuid.equals(this.gazeTargetUuid);
        this.gazeTargetUuid = targetUuid;
        if (changed) {
            this.gazeTargetHoldTicks = this.randomBetween(28, 46);
            float relativeYaw = Mth.wrapDegrees(this.yawTo(target.position()) - this.getYRot());
            if (Math.abs(relativeYaw) > 7.0F) {
                this.gazeSideSign = relativeYaw > 0.0F ? 1 : -1;
            } else {
                this.gazeSideSign = this.getRandom().nextBoolean() ? 1 : -1;
            }
        }
        this.entityData.set(GAZE_TARGET_ID, target.getId());
        if (this.getSensing().hasLineOfSight(target)) {
            this.lastSeenGazePos = target.getEyePosition();
            this.lastSeenGazeTicks = GAZE_LAST_SEEN_TICKS;
        }
    }

    @Nullable
    private LivingEntity resolveGazeTarget() {
        if (this.gazeTargetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.gazeTargetUuid);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private void updateServerGaze() {
        LivingEntity target = this.resolveGazeTarget();
        CassowaryBehaviorState state = this.getCassowaryBehaviorState();
        boolean targetVisible = target != null && this.getSensing().hasLineOfSight(target);
        if (targetVisible) {
            this.lastSeenGazePos = target.getEyePosition();
            this.lastSeenGazeTicks = GAZE_LAST_SEEN_TICKS;
        }

        boolean hasActiveThreat = this.resolveThreatTarget() != null;
        if (hasActiveThreat) {
            this.glanceAwayTicks = 0;
        }
        boolean mayGlanceAway = !hasActiveThreat
                && target instanceof Player
                && (state == CassowaryBehaviorState.CALM
                || state == CassowaryBehaviorState.WATCHING);
        if (mayGlanceAway
                && this.glanceAwayTicks <= 0 && this.getRandom().nextInt(190) == 0) {
            this.glanceAwayTicks = this.randomBetween(24, 58);
            this.chooseCalmGaze();
        }
        if (this.glanceAwayTicks > 0) --this.glanceAwayTicks;

        Vec3 gazePosition = null;
        if (this.glanceAwayTicks > 0) {
            gazePosition = this.calmGazePosition;
        } else if (targetVisible) {
            gazePosition = target.getEyePosition();
        } else if (this.lastSeenThreatPos != null && this.isTerritoryState()) {
            gazePosition = this.lastSeenThreatPos;
        } else if (this.lastSeenGazePos != null && this.lastSeenGazeTicks > 0) {
            gazePosition = this.lastSeenGazePos;
        } else if (state == CassowaryBehaviorState.CALM || state == CassowaryBehaviorState.WATCHING) {
            if (--this.calmGazeTicks <= 0) this.chooseCalmGaze();
            gazePosition = this.calmGazePosition;
        }

        float weight = gazeWeightForState(state);
        if (gazePosition == null || weight <= 0.0F) {
            this.entityData.set(GAZE_YAW, 0.0F);
            this.entityData.set(GAZE_PITCH, 0.0F);
            this.entityData.set(GAZE_WEIGHT, 0.0F);
            return;
        }
        Vec3 delta = gazePosition.subtract(this.getX(), this.getEyeY(), this.getZ());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float)(Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
        float relativeYaw = Mth.wrapDegrees(targetYaw - this.getYRot());
        if (targetVisible) {
            relativeYaw = this.applyLateralEyeBias(relativeYaw, state);
        }
        relativeYaw = Mth.clamp(relativeYaw, -82.0F, 82.0F);
        float pitch = Mth.clamp((float)(-Mth.atan2(delta.y, Math.max(0.001D, horizontal)) * Mth.RAD_TO_DEG),
                -32.0F, 32.0F);
        this.entityData.set(GAZE_YAW, relativeYaw);
        this.entityData.set(GAZE_PITCH, pitch);
        this.entityData.set(GAZE_WEIGHT, weight);

        if (target != null && (state == CassowaryBehaviorState.CALM
                || state == CassowaryBehaviorState.WATCHING)) {
            this.turnBodyToward(gazePosition, 0.55F, 68.0F);
        }
    }

    private float applyLateralEyeBias(float relativeYaw, CassowaryBehaviorState state) {
        float bias = switch (state) {
            case CALM -> 9.0F;
            case WATCHING -> 8.0F;
            case ALERT -> 5.0F;
            case WARNING -> 3.0F;
            default -> 0.0F;
        };
        if (bias <= 0.0F) {
            return relativeYaw;
        }

        float absoluteYaw = Math.abs(relativeYaw);
        if (absoluteYaw >= 9.0F) {
            float side = relativeYaw > 0.0F ? 1.0F : -1.0F;
            return relativeYaw - side * Math.min(bias, absoluteYaw * 0.35F);
        }
        return relativeYaw + this.gazeSideSign * bias * 0.55F;
    }

    private void chooseCalmGaze() {
        float yaw = this.getYRot() + this.getRandom().nextFloat() * 120.0F - 60.0F;
        this.calmGazePosition = this.getEyePosition().add(directionFromYaw(yaw).scale(this.randomBetween(4, 9)))
                .add(0.0D, this.getRandom().nextDouble() * 2.0D - 1.0D, 0.0D);
        this.calmGazeTicks = this.randomBetween(35, 110);
    }

    private void tickClientGaze() {
        float desiredYaw = this.entityData.get(GAZE_YAW);
        float desiredPitch = this.entityData.get(GAZE_PITCH);
        CassowaryBehaviorState state = this.getCassowaryBehaviorState();
        boolean urgent = state == CassowaryBehaviorState.ALERT || state == CassowaryBehaviorState.WARNING;

        float headYawShare = urgent ? 0.56F : 0.64F;
        float upperYawShare = urgent ? 0.28F : 0.24F;
        float lowerYawShare = urgent ? 0.16F : 0.12F;
        float headPitchShare = urgent ? 0.60F : 0.70F;
        float upperPitchShare = urgent ? 0.26F : 0.20F;
        float lowerPitchShare = urgent ? 0.14F : 0.10F;

        float headYawTarget = Mth.clamp(desiredYaw * headYawShare, -38.0F, 38.0F);
        float upperYawTarget = Mth.clamp(desiredYaw * upperYawShare, -22.0F, 22.0F);
        float lowerYawTarget = Mth.clamp(desiredYaw * lowerYawShare, -14.0F, 14.0F);
        float headPitchTarget = Mth.clamp(desiredPitch * headPitchShare, -18.0F, 18.0F);
        float upperPitchTarget = Mth.clamp(desiredPitch * upperPitchShare, -9.0F, 9.0F);
        float lowerPitchTarget = Mth.clamp(desiredPitch * lowerPitchShare, -5.0F, 5.0F);

        this.clientHeadYaw = Mth.approachDegrees(this.clientHeadYaw, headYawTarget, urgent ? 3.4F : 1.9F);
        this.clientUpperNeckYaw = Mth.approachDegrees(this.clientUpperNeckYaw, upperYawTarget,
                urgent ? 1.55F : 0.78F);
        this.clientLowerNeckYaw = Mth.approachDegrees(this.clientLowerNeckYaw, lowerYawTarget,
                urgent ? 0.80F : 0.38F);
        this.clientHeadPitch = Mth.approach(this.clientHeadPitch, headPitchTarget, urgent ? 2.2F : 1.15F);
        this.clientUpperNeckPitch = Mth.approach(this.clientUpperNeckPitch, upperPitchTarget,
                urgent ? 1.1F : 0.52F);
        this.clientLowerNeckPitch = Mth.approach(this.clientLowerNeckPitch, lowerPitchTarget,
                urgent ? 0.60F : 0.28F);
        this.clientGazeWeight = Mth.lerp(urgent ? 0.30F : 0.18F,
                this.clientGazeWeight, this.entityData.get(GAZE_WEIGHT));
    }

    private static float gazeWeightForState(CassowaryBehaviorState state) {
        return switch (state) {
            case CALM -> 0.55F;
            case WATCHING -> 0.85F;
            case ALERT, WARNING -> 1.0F;
            case CHARGING -> 0.40F;
            case ATTACKING -> 0.20F;
            case WITHDRAWING -> 0.55F;
            case FORAGING -> 0.25F;
            case REST_ENTER, RESTING, REST_EXIT -> 0.20F;
            case LOOKING -> 0.45F;
            case EATING, PECKING, SLEEP_ENTER, SLEEPING, SLEEP_EXIT -> 0.0F;
        };
    }

    void turnBodyToward(Vec3 target, float maxTurn, float threshold) {
        float desired = yawTo(target);
        float difference = Mth.wrapDegrees(desired - this.getYRot());
        if (Math.abs(difference) < threshold) {
            return;
        }
        this.setHeavyBodyYaw(Mth.approachDegrees(this.getYRot(), desired, maxTurn));
    }

    private void setHeavyBodyYaw(float yaw) {
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private float yawTo(Vec3 target) {
        Vec3 delta = target.subtract(this.position());
        return (float)(Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private static Vec3 directionFromYaw(float yaw) {
        float radians = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
    }

    @Nullable
    Vec3 findPatrolPosition() {
        if (this.homeCenter != null && this.distanceToSqr(Vec3.atCenterOf(this.homeCenter)) > 24.0D * 24.0D) {
            return Vec3.atBottomCenterOf(this.homeCenter);
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            Vec3 candidate = LandRandomPos.getPos(this, 14, 5);
            if (candidate != null && (this.homeCenter == null
                    || candidate.distanceToSqr(Vec3.atCenterOf(this.homeCenter)) <= 24.0D * 24.0D)) {
                return candidate;
            }
        }
        return this.homeCenter == null ? null : Vec3.atBottomCenterOf(this.homeCenter);
    }

    boolean isOutsideTerritoryLimit() {
        if (this.homeCenter == null) {
            return false;
        }
        Vec3 home = Vec3.atCenterOf(this.homeCenter);
        double dx = this.getX() - home.x;
        double dz = this.getZ() - home.z;
        return dx * dx + dz * dz > TERRITORY_PURSUIT_LIMIT * TERRITORY_PURSUIT_LIMIT;
    }

    @Nullable
    Vec3 findSleepPosition() {
        BlockPos center = this.homeCenter == null ? this.blockPosition() : this.homeCenter;
        return this.findSafeRestingPosition(center, 4, 16, 24);
    }

    @Nullable
    Vec3 findRestPosition() {
        return this.findSafeRestingPosition(this.blockPosition(), 2, 8, 14);
    }

    @Nullable
    private Vec3 findSafeRestingPosition(BlockPos center, int minRadius, int maxRadius, int attempts) {
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int attempt = 0; attempt < attempts; attempt++) {
            double angle = this.getRandom().nextDouble() * Mth.TWO_PI;
            double radius = minRadius + this.getRandom().nextDouble() * (maxRadius - minRadius);
            int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
            Vec3 candidate = this.safeRestingPositionAt(x, z);
            if (candidate == null) {
                continue;
            }
            BlockPos stand = BlockPos.containing(candidate);
            double score = this.canopyScore(stand) * 2.0D
                    - candidate.distanceToSqr(Vec3.atCenterOf(center)) * 0.015D
                    - Math.abs(candidate.y - center.getY()) * 0.25D;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best != null) {
            return best;
        }
        return this.safeRestingPositionAt(center.getX(), center.getZ());
    }

    @Nullable
    private Vec3 safeRestingPositionAt(int x, int z) {
        if (!this.level().hasChunkAt(new BlockPos(x, this.blockPosition().getY(), z))) {
            return null;
        }
        int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos stand = new BlockPos(x, y, z);
        if (!this.isSafeRestingPosition(stand)) {
            return null;
        }
        Vec3 target = Vec3.atBottomCenterOf(stand);
        if (this.distanceToSqr(target) > 2.25D
                && this.getNavigation().createPath(stand, 0) == null) {
            return null;
        }
        return target;
    }

    private boolean isSafeRestingPosition(BlockPos stand) {
        BlockPos floorPos = stand.below();
        BlockState floor = this.level().getBlockState(floorPos);
        boolean naturalGround = floor.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || floor.is(BlockTags.DIRT)
                || floor.is(Blocks.MOSS_BLOCK)
                || floor.is(Blocks.PODZOL)
                || floor.is(Blocks.ROOTED_DIRT);
        if (!naturalGround || !floor.isFaceSturdy(this.level(), floorPos, Direction.UP)) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacentFloor = stand.relative(direction).below();
            if (!this.level().getBlockState(adjacentFloor)
                    .isFaceSturdy(this.level(), adjacentFloor, Direction.UP)) {
                return false;
            }
        }
        for (int y = 0; y <= 2; y++) {
            BlockPos open = stand.above(y);
            if (!this.level().getFluidState(open).isEmpty()
                    || !this.level().getBlockState(open)
                    .getCollisionShape(this.level(), open).isEmpty()) {
                return false;
            }
        }
        Vec3 target = Vec3.atBottomCenterOf(stand);
        AABB targetBox = this.getBoundingBox().move(
                target.x - this.getX(), target.y - this.getY(), target.z - this.getZ());
        return this.level().noCollision(this, targetBox);
    }

    private int canopyScore(BlockPos stand) {
        int coveredColumns = 0;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 3; y <= 7; y++) {
                    if (this.level().getBlockState(stand.offset(x, y, z)).is(BlockTags.LEAVES)) {
                        ++coveredColumns;
                        break;
                    }
                }
            }
        }
        return coveredColumns;
    }

    @Nullable
    Vec3 findPersonalSpacePosition(Vec3 crowdedFrom) {
        for (int attempt = 0; attempt < 5; attempt++) {
            Vec3 candidate = DefaultRandomPos.getPosAway(this, 8, 3, crowdedFrom);
            if (candidate != null && this.isWithinHomeRange(candidate, 24.0D)) {
                return candidate;
            }
        }
        Vec3 away = this.position().subtract(crowdedFrom).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() <= 1.0E-5D) {
            away = directionFromYaw(this.getYRot() + 90.0F);
        }
        Vec3 fallback = this.position().add(away.normalize().scale(5.0D));
        if (this.isWithinHomeRange(fallback, 24.0D)
                && this.getNavigation().createPath(BlockPos.containing(fallback), 0) != null) {
            return fallback;
        }
        return this.findPatrolPosition();
    }

    private boolean isWithinHomeRange(Vec3 position, double radius) {
        return this.homeCenter == null
                || position.distanceToSqr(Vec3.atCenterOf(this.homeCenter)) <= radius * radius;
    }

    boolean isDayActivityTime() {
        return this.level().getDayTime() % 24000L < 12500L;
    }

    boolean isHotMidday() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 5000L && time <= 9000L;
    }

    int randomBetween(int min, int max) {
        return min + this.getRandom().nextInt(max - min + 1);
    }

    void setCassowaryBehaviorState(CassowaryBehaviorState state) {
        if (this.getCassowaryBehaviorState() == state) {
            return;
        }
        this.entityData.set(BEHAVIOR_STATE, state.ordinal());
        this.behaviorTicks = 0;
        if (!this.level().isClientSide && state == CassowaryBehaviorState.WARNING) {
            this.playSound(GuaniaoSoundEvents.CASSOWARY_WARNING.get(),
                    1.15F, 0.94F + this.getRandom().nextFloat() * 0.12F);
        }
    }

    public CassowaryBehaviorState getCassowaryBehaviorState() {
        return CassowaryBehaviorState.byId(this.entityData.get(BEHAVIOR_STATE));
    }

    int getBehaviorTicks() {
        return this.behaviorTicks;
    }

    boolean isTerritoryState() {
        return switch (this.getCassowaryBehaviorState()) {
            case WATCHING, ALERT, WARNING, CHARGING, ATTACKING, WITHDRAWING -> true;
            default -> false;
        };
    }

    boolean isRestState() {
        return switch (this.getCassowaryBehaviorState()) {
            case REST_ENTER, RESTING, REST_EXIT -> true;
            default -> false;
        };
    }

    boolean isSleepState() {
        return switch (this.getCassowaryBehaviorState()) {
            case SLEEP_ENTER, SLEEPING, SLEEP_EXIT -> true;
            default -> false;
        };
    }

    boolean isRestOrSleepState() {
        return this.isRestState() || this.isSleepState();
    }

    void beginExitForThreat() {
        if (this.getCassowaryBehaviorState() == CassowaryBehaviorState.REST_ENTER
                || this.getCassowaryBehaviorState() == CassowaryBehaviorState.RESTING) {
            this.setCassowaryBehaviorState(CassowaryBehaviorState.REST_EXIT);
        } else if (this.getCassowaryBehaviorState() == CassowaryBehaviorState.SLEEP_ENTER
                || this.getCassowaryBehaviorState() == CassowaryBehaviorState.SLEEPING) {
            this.setCassowaryBehaviorState(CassowaryBehaviorState.SLEEP_EXIT);
        }
    }

    int foodScanInterval() {
        return Math.max(20, BirdConfigManager.foodScanInterval(BirdSpecies.CASSOWARY));
    }

    @Nullable
    ItemEntity findFoodTarget() {
        if (this.foodTarget != null && this.foodTarget.isAlive()
                && BirdFoodSafety.matchesClean(BirdTags.CASSOWARY_FOODS, this.foodTarget.getItem())
                && this.distanceToSqr(this.foodTarget) <= 14.0D * 14.0D) {
            return this.foodTarget;
        }
        this.foodTarget = null;
        if (this.foodScanCooldown > 0
                || !(this.level() instanceof ServerLevel serverLevel)
                || !BirdScanBudget.tryAcquire(serverLevel, this)) {
            return null;
        }
        this.foodScanCooldown = this.foodScanInterval();
        this.foodTarget = this.level().getEntitiesOfClass(ItemEntity.class,
                        this.getBoundingBox().inflate(12.0D, 4.0D, 12.0D),
                        item -> item.isAlive()
                                && BirdFoodSafety.matchesClean(BirdTags.CASSOWARY_FOODS, item.getItem()))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        return this.foodTarget;
    }

    @Nullable
    ItemEntity getFoodTarget() {
        return this.foodTarget;
    }

    void clearFoodTarget() {
        this.foodTarget = null;
        this.foodConsumed = false;
    }

    boolean isFoodConsumed() {
        return this.foodConsumed;
    }

    void setFoodConsumed(boolean consumed) {
        this.foodConsumed = consumed;
    }

    void consumeFoodTarget() {
        this.foodConsumed = true;
        if (this.foodTarget == null || !this.foodTarget.isAlive()) {
            return;
        }
        Entity thrower = this.foodTarget.getOwner();
        ItemStack stack = this.foodTarget.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            this.foodTarget.discard();
        } else {
            this.foodTarget.setItem(stack);
        }
        this.heal(2.0F);
        if (thrower instanceof Player player) {
            this.grantAreaTrust(player);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    this.getX(), this.getEyeY(), this.getZ(), 5, 0.3D, 0.25D, 0.3D, 0.02D);
        }
    }

    public boolean isTrusted(Player player) {
        Long expires = this.trustedPlayers.get(player.getUUID());
        return expires != null && expires > this.level().getGameTime();
    }

    private void grantAreaTrust(Player player) {
        long expires = this.level().getGameTime() + TRUST_DURATION_TICKS;
        AABB area = this.getBoundingBox().inflate(TRUST_SHARE_RADIUS, 14.0D, TRUST_SHARE_RADIUS);
        for (CassowaryEntity cassowary : this.level().getEntitiesOfClass(CassowaryEntity.class, area,
                bird -> bird.isAlive())) {
            cassowary.trustedPlayers.put(player.getUUID(), expires);
            cassowary.setGazeTarget(player);
            LivingEntity threat = cassowary.resolveThreatTarget();
            if (threat != null && threat.getUUID().equals(player.getUUID())) {
                cassowary.clearThreatTarget();
                cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.CALM);
            }
        }
    }

    private void revokeAreaTrust(UUID playerUuid) {
        AABB area = this.getBoundingBox().inflate(TRUST_SHARE_RADIUS, 14.0D, TRUST_SHARE_RADIUS);
        for (CassowaryEntity cassowary : this.level().getEntitiesOfClass(CassowaryEntity.class, area,
                bird -> bird.isAlive())) {
            cassowary.trustedPlayers.remove(playerUuid);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BirdFoodSafety.matchesClean(BirdTags.CASSOWARY_FOODS, stack)) {
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.getNavigation().stop();
        this.foodTarget = null;
        this.foodConsumed = true;
        this.heal(2.0F);
        this.grantAreaTrust(player);
        this.setCassowaryBehaviorState(CassowaryBehaviorState.EATING);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    this.getX(), this.getEyeY(), this.getZ(), 7, 0.35D, 0.3D, 0.35D, 0.025D);
        }
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            this.triggerAnim("action", "hurt");
            if (source.getEntity() instanceof LivingEntity attacker) {
                this.rememberThreat(attacker);
                this.threatLevel = 1.0F;
                this.provokedTicks = 60;
                if (attacker instanceof Player player) {
                    this.revokeAreaTrust(player.getUUID());
                }
                this.foodTarget = null;
                this.beginExitForThreat();
            }
        }
        return hurt;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        BirdModelScale.save(tag, this.getIndividualModelScale(), this.modelScaleProfile());
        tag.putInt(MUTATION_NBT_KEY, this.getBirdMutation().ordinal());
        if (this.homeCenter != null) {
            tag.putLong(HOME_NBT_KEY, this.homeCenter.asLong());
        }
        ListTag trustList = new ListTag();
        long now = this.level().getGameTime();
        this.trustedPlayers.forEach((uuid, expires) -> {
            if (expires > now) {
                CompoundTag trust = new CompoundTag();
                trust.putUUID("Player", uuid);
                trust.putLong("Until", expires);
                trustList.add(trust);
            }
        });
        tag.put(TRUST_NBT_KEY, trustList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(BirdModelScale.NBT_KEY, Tag.TAG_FLOAT)) {
            this.setIndividualModelScale(BirdModelScale.load(tag, this.modelScaleProfile()));
        }
        if (tag.contains(MUTATION_NBT_KEY, Tag.TAG_INT)) {
            this.setBirdMutation(BirdMutation.byId(tag.getInt(MUTATION_NBT_KEY)));
        }
        if (tag.contains(HOME_NBT_KEY, Tag.TAG_LONG)) {
            this.homeCenter = BlockPos.of(tag.getLong(HOME_NBT_KEY));
        }
        this.trustedPlayers.clear();
        ListTag trustList = tag.getList(TRUST_NBT_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < trustList.size(); index++) {
            CompoundTag trust = trustList.getCompound(index);
            if (trust.hasUUID("Player")) {
                this.trustedPlayers.put(trust.getUUID("Player"), trust.getLong("Until"));
            }
        }
        this.setCassowaryBehaviorState(CassowaryBehaviorState.CALM);
    }

    @Override
    public BirdMutation getBirdMutation() {
        return BirdMutation.byId(this.entityData.get(MUTATION));
    }

    @Override
    public void setBirdMutation(BirdMutation mutation) {
        this.entityData.set(MUTATION, (mutation == null ? BirdMutation.NONE : mutation).ordinal());
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.CASSOWARY;
    }

    @Override
    public float getIndividualModelScale() {
        return BirdModelScale.sanitize(this.entityData.get(MODEL_SCALE), this.modelScaleProfile());
    }

    @Override
    public void setIndividualModelScale(float scale) {
        this.entityData.set(MODEL_SCALE, BirdModelScale.sanitize(scale, this.modelScaleProfile()));
    }

    public float getClientHeadYaw() { return this.clientHeadYaw; }
    public float getClientHeadPitch() { return this.clientHeadPitch; }
    public float getClientUpperNeckYaw() { return this.clientUpperNeckYaw; }
    public float getClientUpperNeckPitch() { return this.clientUpperNeckPitch; }
    public float getClientLowerNeckYaw() { return this.clientLowerNeckYaw; }
    public float getClientLowerNeckPitch() { return this.clientLowerNeckPitch; }
    public float getClientGazeWeight() { return this.clientGazeWeight; }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.CASSOWARY_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return GuaniaoSoundEvents.CASSOWARY_HURT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return BirdFlockSoundLimiter.scaledAmbientInterval(this, 520);
    }

    @Override
    public void playAmbientSound() {
        CassowaryBehaviorState state = this.getCassowaryBehaviorState();
        if (state == CassowaryBehaviorState.SLEEP_ENTER
                || state == CassowaryBehaviorState.SLEEPING
                || state == CassowaryBehaviorState.SLEEP_EXIT
                || state == CassowaryBehaviorState.WARNING
                || state == CassowaryBehaviorState.CHARGING
                || state == CassowaryBehaviorState.ATTACKING) {
            return;
        }
        if (BirdFlockSoundLimiter.allowAmbient(this)) {
            super.playAmbientSound();
        }
    }

    @Override
    public float getSoundVolume() {
        return 0.85F;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, BirdSoundVolume.apply(this, volume), pitch);
    }

    public void setGuidePreviewAnimation(GuidePreviewAnimation animation) {
        this.guidePreviewAnimation = animation == null ? GuidePreviewAnimation.NONE : animation;
    }

    private <T extends CassowaryEntity> PlayState movementController(AnimationState<T> animationState) {
        animationState.getController().setAnimationSpeed(1.0D);
        if (this.guidePreviewAnimation.animation != null) {
            return animationState.setAndContinue(this.guidePreviewAnimation.animation);
        }
        return switch (this.getCassowaryBehaviorState()) {
            case WATCHING -> BirdGroundAnimation.hasWalkMotion(this, animationState.isMoving())
                    ? this.movementAnimation(animationState, false)
                    : animationState.setAndContinue(IDLE_ANIMATION);
            case ALERT -> animationState.setAndContinue(ALERT_ANIMATION);
            case WARNING -> animationState.setAndContinue(WARNING_ANIMATION);
            case CHARGING, ATTACKING -> this.movementAnimation(animationState, true);
            case WITHDRAWING, FORAGING -> this.movementAnimation(animationState, false);
            case EATING -> animationState.setAndContinue(EAT_ANIMATION);
            case PECKING -> animationState.setAndContinue(PECK_ANIMATION);
            case LOOKING -> animationState.setAndContinue(LOOKING_ANIMATION);
            case REST_ENTER -> animationState.setAndContinue(REST_ENTER_ANIMATION);
            case RESTING -> animationState.setAndContinue(REST_ANIMATION);
            case REST_EXIT -> animationState.setAndContinue(REST_EXIT_ANIMATION);
            case SLEEP_ENTER -> animationState.setAndContinue(SLEEP_ENTER_ANIMATION);
            case SLEEPING -> animationState.setAndContinue(SLEEP_ANIMATION);
            case SLEEP_EXIT -> {
                if (this.wasRecentlyProvoked()) animationState.getController().setAnimationSpeed(1.35D);
                yield animationState.setAndContinue(SLEEP_EXIT_ANIMATION);
            }
            case CALM -> BirdGroundAnimation.hasWalkMotion(this, animationState.isMoving())
                    ? this.movementAnimation(animationState, false)
                    : animationState.setAndContinue(IDLE_ANIMATION);
        };
    }

    private <T extends CassowaryEntity> PlayState movementAnimation(AnimationState<T> animationState,
                                                                     boolean forceFast) {
        double speed = Math.sqrt(this.getDeltaMovement().horizontalDistanceSqr());
        if (forceFast || speed >= 0.30D) {
            animationState.getController().setAnimationSpeed(Mth.clamp(speed / 0.43D, 0.82D, 1.38D));
            return animationState.setAndContinue(SPRINT_ANIMATION);
        }
        if (speed >= 0.17D) {
            animationState.getController().setAnimationSpeed(Mth.clamp(speed / 0.22D, 0.72D, 1.40D));
            return animationState.setAndContinue(TROT_ANIMATION);
        }
        animationState.getController().setAnimationSpeed(Mth.clamp(
                BirdGroundAnimation.walkAnimationSpeed(this, 0.90D), 0.62D, 1.45D));
        return animationState.setAndContinue(WALK_ANIMATION);
    }

    public enum GuidePreviewAnimation {
        NONE(null), IDLE(IDLE_ANIMATION), LOOK(LOOKING_ANIMATION), ALERT(ALERT_ANIMATION),
        WALK(WALK_ANIMATION), TROT(TROT_ANIMATION), SPRINT(SPRINT_ANIMATION),
        WARNING(WARNING_ANIMATION), EAT(EAT_ANIMATION), REST(REST_ANIMATION), SLEEP(SLEEP_ANIMATION);

        private final RawAnimation animation;

        GuidePreviewAnimation(RawAnimation animation) {
            this.animation = animation;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController movement = new AnimationController(
                (GeoAnimatable)this, "movement", 4, this::movementController);
        AnimationController action = new AnimationController(
                (GeoAnimatable)this, "action", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", HURT_ANIMATION)
                .triggerableAnim("swoop", SWOOP_ANIMATION);
        controllers.add(new AnimationController[]{movement, action});
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }
}
