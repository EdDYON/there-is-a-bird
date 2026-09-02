package EdDYON.guaniao.content.bird.kiwi;

import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.BirdLoudSoundListener;
import EdDYON.guaniao.content.bird.BirdSleepWakeable;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.CleanBirdTemptGoal;
import EdDYON.guaniao.content.bird.brain.BirdBrain;
import EdDYON.guaniao.content.bird.mutation.BirdMutation;
import EdDYON.guaniao.content.bird.mutation.BirdMutationHolder;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import EdDYON.guaniao.content.bird.species.KiwiProfile;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class KiwiEntity extends PathfinderMob
        implements GeoEntity, ScalableBirdModel, BirdSleepWakeable, BirdLoudSoundListener, BirdMutationHolder {
    private static final EntityDataAccessor<Integer> BEHAVIOR_STATE =
            SynchedEntityData.defineId(KiwiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MODEL_SCALE =
            SynchedEntityData.defineId(KiwiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MUTATION =
            SynchedEntityData.defineId(KiwiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CONFLICT_STATE =
            SynchedEntityData.defineId(KiwiEntity.class, EntityDataSerializers.INT);

    private static final String MUTATION_NBT_KEY = "BirdMutation";
    private static final String HOME_X_NBT_KEY = "KiwiHomeX";
    private static final String HOME_Y_NBT_KEY = "KiwiHomeY";
    private static final String HOME_Z_NBT_KEY = "KiwiHomeZ";
    private static final String ROOST_X_NBT_KEY = "KiwiRoostX";
    private static final String ROOST_Y_NBT_KEY = "KiwiRoostY";
    private static final String ROOST_Z_NBT_KEY = "KiwiRoostZ";
    private static final String LAST_RIVAL_NBT_KEY = "KiwiLastRival";
    private static final String RIVAL_AVOID_UNTIL_NBT_KEY = "KiwiRivalAvoidUntil";
    private static final String RIVAL_HOME_X_NBT_KEY = "KiwiRivalHomeX";
    private static final String RIVAL_HOME_Y_NBT_KEY = "KiwiRivalHomeY";
    private static final String RIVAL_HOME_Z_NBT_KEY = "KiwiRivalHomeZ";
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation IDLE_DIFF_1_ANIMATION = RawAnimation.begin().thenPlay("animation.idle_diff_1");
    private static final RawAnimation PECK_ANIMATION = RawAnimation.begin().thenPlay("animation.idle_diff_2");
    private static final RawAnimation SLEEP_ENTER_ANIMATION = RawAnimation.begin().thenPlay("animation.sleep");
    private static final RawAnimation SLEEP_LOOP_ANIMATION = RawAnimation.begin().thenLoop("animation.sleep_loop");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final BirdBrain birdBrain = new BirdBrain(this, KiwiProfile.INSTANCE);
    private int behaviorTicks;
    private int restInterruptionTicks;
    @Nullable
    private BlockPos homeCenter;
    @Nullable
    private BlockPos roostPos;
    @Nullable
    private Vec3 lastHeardSound;
    private long heardSoundUntil;
    private int ambientReplyTicks = -1;
    @Nullable
    private UUID lastRivalUuid;
    @Nullable
    private BlockPos avoidedRivalHome;
    private long rivalAvoidUntil;
    private int conflictRivalId = -1;
    @Nullable
    private UUID conflictRivalUuid;
    private int conflictTicks;
    private long conflictStartTime;
    private float fightMorale;
    private int fightHitsTaken;
    private int fightHitLimit = 5;
    private int fightAttackCooldown;
    private int forcedConflictTicks;
    private boolean returnHomeAfterConflict;

    public KiwiEntity(EntityType<? extends KiwiEntity> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, 4.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_FIRE, 16.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, KiwiDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, KiwiDefinition.WALK_SPEED)
                .add(Attributes.FOLLOW_RANGE, KiwiDefinition.FOLLOW_RANGE);
    }

    public static boolean canSpawn(EntityType<KiwiEntity> entityType, ServerLevelAccessor level,
                                   MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean validGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(Blocks.MOSS_BLOCK)
                || below.is(Blocks.PODZOL);
        return validGround && level.getFluidState(pos).isEmpty() && level.getBlockState(pos).isAir();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new KiwiPanicGoal(this, KiwiDefinition.PANIC_SPEED));
        this.goalSelector.addGoal(1, new KiwiTerritorialFightGoal(this));
        this.goalSelector.addGoal(2, new KiwiGroundEscapeGoal(this));
        this.goalSelector.addGoal(2, new KiwiReturnHomeAfterFightGoal(this));
        this.goalSelector.addGoal(3, new KiwiSeekShelterGoal(this));
        this.goalSelector.addGoal(4, new KiwiSleepGoal(this));
        this.goalSelector.addGoal(5, new KiwiListenGoal(this));
        this.goalSelector.addGoal(6, new KiwiForagePatchGoal(this));
        this.goalSelector.addGoal(7, new CleanBirdTemptGoal(this, 0.9D,
                Ingredient.of(BirdTags.KIWI_FOODS), true));
        this.goalSelector.addGoal(8, new KiwiSpacingGoal(this));
        this.goalSelector.addGoal(9, new KiwiHomeWanderGoal(this));
        this.goalSelector.addGoal(10, new KiwiIdleActionGoal(this));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 7.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BEHAVIOR_STATE, KiwiBehaviorState.AWAKE.ordinal());
        this.entityData.define(MODEL_SCALE, BirdModelScale.DEFAULT_INDIVIDUAL_SCALE);
        this.entityData.define(MUTATION, BirdMutation.NONE.ordinal());
        this.entityData.define(CONFLICT_STATE, KiwiConflictState.NONE.ordinal());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        this.birdBrain.tick();
        if (this.homeCenter == null) {
            this.homeCenter = this.blockPosition().immutable();
        }
        if (this.forcedConflictTicks > 0) {
            --this.forcedConflictTicks;
        }
        if (this.restInterruptionTicks > 0) {
            --this.restInterruptionTicks;
        }
        if (this.ambientReplyTicks > 0) {
            --this.ambientReplyTicks;
        } else if (this.ambientReplyTicks == 0) {
            this.ambientReplyTicks = -1;
            if (this.isActiveTime() && !this.isBirdSleeping() && !this.isConflictActive()
                    && BirdFlockSoundLimiter.allowAmbient(this)) {
                super.playAmbientSound();
            }
        }
        if (this.roostPos != null && this.tickCount % 200 == 0
                && !KiwiHabitatUtil.isValidRoost(this, this.roostPos)) {
            this.roostPos = null;
        }
        if (this.isConflictActive() && this.getConflictRival() == null) {
            this.finishConflict();
        }
        if (this.lastRivalUuid != null && this.level().getGameTime() >= this.rivalAvoidUntil) {
            this.lastRivalUuid = null;
            this.avoidedRivalHome = null;
            this.rivalAvoidUntil = 0L;
        }
        if (this.getBehaviorState() == KiwiBehaviorState.SLEEPING && this.tickCount % 80 == 0) {
            this.birdBrain.onRest(0.03F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            if (source.getEntity() instanceof KiwiEntity rival && this.isFightingWith(rival)) {
                this.fightMorale = Math.max(0.0F,
                        this.fightMorale - (0.12F + this.getRandom().nextFloat() * 0.08F));
                ++this.fightHitsTaken;
            } else {
                if (this.isConflictActive()) {
                    this.endConflictPair(this.getConflictRival());
                }
                this.birdBrain.onFrightened(0.55F);
                Vec3 sourcePosition = source.getEntity() == null ? this.position() : source.getEntity().position();
                this.wakeFromLoudSound(sourcePosition);
            }
        }
        return hurt;
    }

    @Override
    public boolean isPushable() {
        return this.getConflictState() != KiwiConflictState.FIGHTING && super.isPushable();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.KIWI_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return BirdFlockSoundLimiter.scaledAmbientInterval(this, 320);
    }

    @Override
    public void playAmbientSound() {
        if (this.isActiveTime() && !this.isBirdSleeping() && !this.isConflictActive()
                && BirdFlockSoundLimiter.allowAmbient(this)) {
            super.playAmbientSound();
            this.scheduleNearbyReplies();
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        if (tag == null || !tag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.setIndividualModelScale(BirdModelScale.randomIndividualScale(this.getRandom(), this.modelScaleProfile()));
        }
        if (tag == null || !tag.contains(MUTATION_NBT_KEY, 3)) {
            this.setBirdMutation(BirdMutation.randomMutation(this.getRandom()));
        }
        if (this.homeCenter == null) {
            this.homeCenter = this.blockPosition().immutable();
        }
        return data;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.birdBrain.save(tag);
        BirdModelScale.save(tag, this.getIndividualModelScale(), this.modelScaleProfile());
        tag.putInt(MUTATION_NBT_KEY, this.getBirdMutation().ordinal());
        if (this.homeCenter != null) {
            tag.putInt(HOME_X_NBT_KEY, this.homeCenter.getX());
            tag.putInt(HOME_Y_NBT_KEY, this.homeCenter.getY());
            tag.putInt(HOME_Z_NBT_KEY, this.homeCenter.getZ());
        }
        if (this.roostPos != null) {
            tag.putInt(ROOST_X_NBT_KEY, this.roostPos.getX());
            tag.putInt(ROOST_Y_NBT_KEY, this.roostPos.getY());
            tag.putInt(ROOST_Z_NBT_KEY, this.roostPos.getZ());
        }
        if (this.lastRivalUuid != null && this.rivalAvoidUntil > this.level().getGameTime()) {
            tag.putUUID(LAST_RIVAL_NBT_KEY, this.lastRivalUuid);
            tag.putLong(RIVAL_AVOID_UNTIL_NBT_KEY, this.rivalAvoidUntil);
            if (this.avoidedRivalHome != null) {
                tag.putInt(RIVAL_HOME_X_NBT_KEY, this.avoidedRivalHome.getX());
                tag.putInt(RIVAL_HOME_Y_NBT_KEY, this.avoidedRivalHome.getY());
                tag.putInt(RIVAL_HOME_Z_NBT_KEY, this.avoidedRivalHome.getZ());
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.birdBrain.load(tag);
        if (tag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.setIndividualModelScale(BirdModelScale.load(tag, this.modelScaleProfile()));
        }
        if (tag.contains(MUTATION_NBT_KEY, 3)) {
            this.setBirdMutation(BirdMutation.byId(tag.getInt(MUTATION_NBT_KEY)));
        }
        if (tag.contains(HOME_X_NBT_KEY, 3)
                && tag.contains(HOME_Y_NBT_KEY, 3)
                && tag.contains(HOME_Z_NBT_KEY, 3)) {
            this.homeCenter = new BlockPos(
                    tag.getInt(HOME_X_NBT_KEY),
                    tag.getInt(HOME_Y_NBT_KEY),
                    tag.getInt(HOME_Z_NBT_KEY));
        } else {
            this.homeCenter = this.blockPosition().immutable();
        }
        if (tag.contains(ROOST_X_NBT_KEY, 3)
                && tag.contains(ROOST_Y_NBT_KEY, 3)
                && tag.contains(ROOST_Z_NBT_KEY, 3)) {
            this.roostPos = new BlockPos(
                    tag.getInt(ROOST_X_NBT_KEY),
                    tag.getInt(ROOST_Y_NBT_KEY),
                    tag.getInt(ROOST_Z_NBT_KEY));
        }
        if (tag.hasUUID(LAST_RIVAL_NBT_KEY)) {
            this.lastRivalUuid = tag.getUUID(LAST_RIVAL_NBT_KEY);
            this.rivalAvoidUntil = tag.getLong(RIVAL_AVOID_UNTIL_NBT_KEY);
            if (tag.contains(RIVAL_HOME_X_NBT_KEY, 3)
                    && tag.contains(RIVAL_HOME_Y_NBT_KEY, 3)
                    && tag.contains(RIVAL_HOME_Z_NBT_KEY, 3)) {
                this.avoidedRivalHome = new BlockPos(
                        tag.getInt(RIVAL_HOME_X_NBT_KEY),
                        tag.getInt(RIVAL_HOME_Y_NBT_KEY),
                        tag.getInt(RIVAL_HOME_Z_NBT_KEY));
            }
        }
        this.finishConflict();
        this.setBehaviorState(KiwiBehaviorState.AWAKE, 0);
    }

    public BirdBrain birdBrain() {
        return this.birdBrain;
    }

    public KiwiBehaviorState getBehaviorState() {
        return KiwiBehaviorState.byId(this.entityData.get(BEHAVIOR_STATE));
    }

    void setBehaviorState(KiwiBehaviorState state, int ticks) {
        this.entityData.set(BEHAVIOR_STATE, state.ordinal());
        this.behaviorTicks = Math.max(0, ticks);
    }

    int getBehaviorTicks() {
        return this.behaviorTicks;
    }

    void setBehaviorTicks(int ticks) {
        this.behaviorTicks = Math.max(0, ticks);
    }

    int decrementBehaviorTicks() {
        return this.behaviorTicks = Math.max(0, this.behaviorTicks - 1);
    }

    boolean isActiveTime() {
        return BirdActivitySchedule.NOCTURNAL_CREPUSCULAR.isActiveTime(this.level().getDayTime());
    }

    boolean canContinueTerritorialConflict() {
        return this.isActiveTime() || this.forcedConflictTicks > 0;
    }

    boolean canStartCalmBehavior() {
        return this.getBehaviorState() == KiwiBehaviorState.AWAKE
                && this.onGround()
                && !this.isInWaterOrBubble()
                && this.hurtTime <= 0
                && this.getNavigation().isDone()
                && this.birdBrain.computeRiskScore() < 0.60F;
    }

    @Nullable
    BlockPos getHomeCenter() {
        return this.homeCenter;
    }

    boolean isWithinTerritory(BlockPos pos, int radius) {
        return this.homeCenter == null || this.homeCenter.distSqr(pos) <= (double) radius * radius;
    }

    @Nullable
    BlockPos getRoostPos() {
        return this.roostPos;
    }

    void setRoostPos(@Nullable BlockPos pos) {
        this.roostPos = pos == null ? null : pos.immutable();
    }

    boolean isAtRoost() {
        return this.roostPos != null
                && this.distanceToSqr(Vec3.atBottomCenterOf(this.roostPos)) <= 2.25D;
    }

    int getRestInterruptionTicks() {
        return this.restInterruptionTicks;
    }

    void interruptRest(int ticks) {
        this.restInterruptionTicks = Math.max(this.restInterruptionTicks, ticks);
    }

    boolean hasRecentLoudSound() {
        return this.lastHeardSound != null && this.level().getGameTime() <= this.heardSoundUntil;
    }

    @Nullable
    Vec3 getLastHeardSound() {
        return this.lastHeardSound;
    }

    void consumeLoudSound() {
        this.lastHeardSound = null;
        this.heardSoundUntil = 0L;
    }

    @Override
    public boolean isBirdSleeping() {
        KiwiBehaviorState state = this.getBehaviorState();
        return state == KiwiBehaviorState.ENTERING_SLEEP || state == KiwiBehaviorState.SLEEPING;
    }

    @Override
    public void onLoudSound(Vec3 soundPosition, float volume) {
        if (this.level().isClientSide || soundPosition == null) {
            return;
        }
        this.lastHeardSound = soundPosition;
        this.heardSoundUntil = this.level().getGameTime()
                + 40L + Math.min(80L, Math.round(volume * 16.0F));
        this.wakeFromLoudSound(soundPosition);
    }

    @Override
    public void wakeFromLoudSound(Vec3 soundPosition) {
        if (this.level().isClientSide) {
            return;
        }
        if (this.isBirdSleeping()) {
            this.getNavigation().stop();
            if (soundPosition != null) {
                this.getLookControl().setLookAt(soundPosition.x, soundPosition.y, soundPosition.z,
                        35.0F, this.getMaxHeadXRot());
            }
        }
        this.lastHeardSound = soundPosition;
        this.heardSoundUntil = Math.max(this.heardSoundUntil, this.level().getGameTime() + 60L);
        this.interruptRest(240);
        this.setBehaviorState(KiwiBehaviorState.AWAKE, 0);
    }

    public KiwiConflictState getConflictState() {
        return KiwiConflictState.byId(this.entityData.get(CONFLICT_STATE));
    }

    boolean isConflictActive() {
        return this.getConflictState() != KiwiConflictState.NONE;
    }

    @Nullable
    KiwiEntity getConflictRival() {
        if (!this.isConflictActive() || this.conflictRivalId < 0) {
            return null;
        }
        if (this.level().getEntity(this.conflictRivalId) instanceof KiwiEntity rival
                && rival.isAlive()
                && (this.conflictRivalUuid == null || this.conflictRivalUuid.equals(rival.getUUID()))) {
            return rival;
        }
        return null;
    }

    boolean isFightingWith(KiwiEntity rival) {
        KiwiEntity current = this.getConflictRival();
        return current == rival
                && (this.getConflictState() == KiwiConflictState.FIGHTING
                || this.getConflictState() == KiwiConflictState.APPROACH);
    }

    void startConflictWith(KiwiEntity rival, int warningTicks) {
        if (rival == this || this.isConflictActive() || rival.isConflictActive()) {
            return;
        }
        long startedAt = this.level().getGameTime();
        this.bindConflict(rival, warningTicks, startedAt);
        rival.bindConflict(this, warningTicks, startedAt);
        this.playSound(GuaniaoSoundEvents.KIWI_AMBIENT.get(), 0.55F,
                0.82F + this.getRandom().nextFloat() * 0.12F);
        rival.playSound(GuaniaoSoundEvents.KIWI_AMBIENT.get(), 0.55F,
                0.82F + rival.getRandom().nextFloat() * 0.12F);
    }

    /**
     * Starts the normal territorial-conflict state machine for command/debug scenes.
     * The temporary override keeps the scene running during daytime without changing
     * the world's time or weakening the natural spawn rules.
     */
    public boolean startCommandConflict(KiwiEntity rival, BlockPos ownHome, BlockPos rivalHome) {
        if (this.level().isClientSide
                || rival == null
                || rival == this
                || rival.level() != this.level()
                || !this.isAlive()
                || !rival.isAlive()
                || this.isConflictActive()
                || rival.isConflictActive()) {
            return false;
        }
        this.homeCenter = ownHome.immutable();
        rival.homeCenter = rivalHome.immutable();
        this.forcedConflictTicks = 800;
        rival.forcedConflictTicks = 800;
        this.startConflictWith(rival, 16);
        return this.isConflictActive() && rival.isConflictActive();
    }

    private void bindConflict(KiwiEntity rival, int warningTicks, long startedAt) {
        this.getNavigation().stop();
        this.setBehaviorState(KiwiBehaviorState.AWAKE, 0);
        this.returnHomeAfterConflict = false;
        this.conflictRivalId = rival.getId();
        this.conflictRivalUuid = rival.getUUID();
        this.conflictStartTime = startedAt;
        this.fightMorale = Math.min(1.0F,
                0.68F + this.birdBrain.personality().boldness() * 0.25F
                        + this.getRandom().nextFloat() * 0.07F);
        this.fightHitsTaken = 0;
        this.fightHitLimit = 4 + this.getRandom().nextInt(3);
        this.fightAttackCooldown = this.getRandom().nextInt(8);
        this.setConflictState(KiwiConflictState.WARNING, warningTicks);
    }

    void setConflictState(KiwiConflictState state, int ticks) {
        this.entityData.set(CONFLICT_STATE, state.ordinal());
        this.conflictTicks = Math.max(0, ticks);
    }

    int getConflictTicks() {
        return this.conflictTicks;
    }

    int decrementConflictTicks() {
        return this.conflictTicks = Math.max(0, this.conflictTicks - 1);
    }

    long getConflictStartTime() {
        return this.conflictStartTime;
    }

    int getFightAttackCooldown() {
        return this.fightAttackCooldown;
    }

    void setFightAttackCooldown(int ticks) {
        this.fightAttackCooldown = Math.max(0, ticks);
    }

    void tickFightAttackCooldown() {
        if (this.fightAttackCooldown > 0) {
            --this.fightAttackCooldown;
        }
    }

    boolean shouldYieldFight() {
        return this.getHealth() <= this.getMaxHealth() * 0.40F
                || this.fightMorale <= 0.25F
                || this.fightHitsTaken >= this.fightHitLimit;
    }

    float fightResolutionScore() {
        float healthRatio = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
        return this.fightMorale
                + healthRatio * 0.40F
                + this.birdBrain.personality().boldness() * 0.20F;
    }

    float territorialWillingness(KiwiEntity rival) {
        float willingness = this.birdBrain.personality().boldness() * 0.55F
                + this.birdBrain.personality().activity() * 0.25F
                - this.birdBrain.personality().wariness() * 0.20F
                - this.birdBrain.personality().sociability() * 0.15F;
        if (this.homeCenter != null
                && this.homeCenter.distSqr(rival.blockPosition())
                <= (double) KiwiDefinition.CORE_TERRITORY_RADIUS * KiwiDefinition.CORE_TERRITORY_RADIUS) {
            willingness += 0.24F;
        }
        return Math.max(0.0F, Math.min(1.0F, willingness));
    }

    void rememberDefeatBy(KiwiEntity winner) {
        this.lastRivalUuid = winner.getUUID();
        this.avoidedRivalHome = winner.getHomeCenter() == null
                ? winner.blockPosition().immutable()
                : winner.getHomeCenter().immutable();
        this.rivalAvoidUntil = this.level().getGameTime() + 1200L + this.getRandom().nextInt(2401);
    }

    boolean isAvoiding(KiwiEntity rival) {
        return this.lastRivalUuid != null
                && this.level().getGameTime() < this.rivalAvoidUntil
                && this.lastRivalUuid.equals(rival.getUUID());
    }

    boolean isNearAvoidedRivalTerritory(BlockPos pos, int radius) {
        return this.avoidedRivalHome != null
                && this.level().getGameTime() < this.rivalAvoidUntil
                && this.avoidedRivalHome.distSqr(pos) <= (double) radius * radius;
    }

    void endConflictPair(KiwiEntity rival) {
        if (rival != null && rival.getConflictRival() == this) {
            rival.finishConflict();
        }
        this.finishConflict();
    }

    void finishConflict() {
        this.entityData.set(CONFLICT_STATE, KiwiConflictState.NONE.ordinal());
        this.conflictRivalId = -1;
        this.conflictRivalUuid = null;
        this.conflictTicks = 0;
        this.conflictStartTime = 0L;
        this.fightMorale = 0.0F;
        this.fightHitsTaken = 0;
        this.fightAttackCooldown = 0;
        this.forcedConflictTicks = 0;
        if (this.getLastHurtByMob() instanceof KiwiEntity) {
            this.setLastHurtByMob(null);
        }
        if (this.getBehaviorState() == KiwiBehaviorState.GROUND_ESCAPE) {
            this.setBehaviorState(KiwiBehaviorState.AWAKE, 0);
        }
    }

    void requestReturnHomeAfterConflict() {
        this.returnHomeAfterConflict = this.homeCenter != null;
    }

    boolean shouldReturnHomeAfterConflict() {
        return this.returnHomeAfterConflict;
    }

    void finishReturnHomeAfterConflict() {
        this.returnHomeAfterConflict = false;
        if (this.getBehaviorState() == KiwiBehaviorState.RETURNING_HOME) {
            this.setBehaviorState(KiwiBehaviorState.AWAKE, 0);
        }
    }

    private void scheduleNearbyReplies() {
        AABB area = this.getBoundingBox().inflate(28.0D, 8.0D, 28.0D);
        for (KiwiEntity other : this.level().getEntitiesOfClass(KiwiEntity.class, area,
                candidate -> candidate != this
                        && candidate.isAlive()
                        && candidate.isActiveTime()
                        && !candidate.isBirdSleeping()
                        && !candidate.isConflictActive())) {
            double distanceSqr = this.distanceToSqr(other);
            if (distanceSqr >= 12.0D * 12.0D
                    && distanceSqr <= 28.0D * 28.0D
                    && other.ambientReplyTicks < 0
                    && other.getRandom().nextFloat() < 0.32F) {
                other.ambientReplyTicks = 20 + other.getRandom().nextInt(61);
            }
        }
    }

    @Override
    public BirdMutation getBirdMutation() {
        return BirdMutation.byId(this.entityData.get(MUTATION));
    }

    @Override
    public void setBirdMutation(BirdMutation mutation) {
        this.entityData.set(MUTATION, mutation == null ? BirdMutation.NONE.ordinal() : mutation.ordinal());
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.KIWI;
    }

    @Override
    public float getIndividualModelScale() {
        return BirdModelScale.sanitize(this.entityData.get(MODEL_SCALE), this.modelScaleProfile());
    }

    @Override
    public void setIndividualModelScale(float scale) {
        this.entityData.set(MODEL_SCALE, BirdModelScale.sanitize(scale, this.modelScaleProfile()));
    }

    private <T extends KiwiEntity> PlayState movementController(AnimationState<T> animationState) {
        animationState.getController().setAnimationSpeed(1.0D);
        if (this.getConflictState() == KiwiConflictState.FIGHTING) {
            animationState.getController().setAnimationSpeed(this.movementAnimationSpeed());
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        return switch (this.getBehaviorState()) {
            case ENTERING_SLEEP -> animationState.setAndContinue(SLEEP_ENTER_ANIMATION);
            case SLEEPING -> animationState.setAndContinue(SLEEP_LOOP_ANIMATION);
            case PECKING -> animationState.setAndContinue(PECK_ANIMATION);
            case IDLE_VARIATION -> animationState.setAndContinue(IDLE_DIFF_1_ANIMATION);
            case AWAKE, LISTENING, FORAGING, RETURNING_HOME, SEEKING_SHELTER, GROUND_ESCAPE -> {
                if (BirdGroundAnimation.hasWalkMotion(this, animationState.isMoving())) {
                    animationState.getController().setAnimationSpeed(this.movementAnimationSpeed());
                    yield animationState.setAndContinue(WALK_ANIMATION);
                }
                yield animationState.setAndContinue(IDLE_ANIMATION);
            }
        };
    }

    private double movementAnimationSpeed() {
        double measuredSpeed = BirdGroundAnimation.walkAnimationSpeed(this);
        if (this.getBehaviorState() == KiwiBehaviorState.GROUND_ESCAPE
                || this.getConflictState() == KiwiConflictState.CHASING
                || this.getConflictState() == KiwiConflictState.FLEEING) {
            return Math.max(measuredSpeed, 1.55D);
        }
        if (this.getConflictState() == KiwiConflictState.FIGHTING) {
            return 2.25D;
        }
        if (this.getConflictState() == KiwiConflictState.APPROACH) {
            return Math.max(measuredSpeed, 1.25D);
        }
        if (this.getBehaviorState() == KiwiBehaviorState.FORAGING) {
            return Math.min(measuredSpeed, 0.95D);
        }
        return measuredSpeed;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{
                new AnimationController((GeoAnimatable)this, "movement", 4, this::movementController)
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    private static final class KiwiPanicGoal extends PanicGoal {
        private final KiwiEntity kiwi;

        private KiwiPanicGoal(KiwiEntity kiwi, double speedModifier) {
            super(kiwi, speedModifier);
            this.kiwi = kiwi;
        }

        @Override
        public boolean canUse() {
            if (this.kiwi.isConflictActive()) {
                return false;
            }
            boolean canUse = super.canUse();
            if (canUse) {
                this.kiwi.wakeFromLoudSound(this.kiwi.position());
            }
            return canUse;
        }
    }

    private static final class KiwiSleepGoal extends Goal {
        private static final int SLEEP_TRANSITION_TICKS = 10;
        private final KiwiEntity kiwi;

        private KiwiSleepGoal(KiwiEntity kiwi) {
            this.kiwi = kiwi;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.kiwi.canStartCalmBehavior()
                    && !this.kiwi.isActiveTime()
                    && this.kiwi.isAtRoost()
                    && !this.kiwi.isConflictActive()
                    && this.kiwi.restInterruptionTicks <= 0
                    && this.kiwi.getRandom().nextInt(80) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.kiwi.isBirdSleeping()
                    && !this.kiwi.isActiveTime()
                    && this.kiwi.isAtRoost()
                    && !this.kiwi.isConflictActive()
                    && this.kiwi.restInterruptionTicks <= 0
                    && this.kiwi.hurtTime <= 0
                    && this.kiwi.onGround()
                    && !this.kiwi.isInWaterOrBubble();
        }

        @Override
        public void start() {
            this.kiwi.getNavigation().stop();
            this.kiwi.setBehaviorState(KiwiBehaviorState.ENTERING_SLEEP, SLEEP_TRANSITION_TICKS);
        }

        @Override
        public void tick() {
            this.kiwi.getNavigation().stop();
            if (this.kiwi.getBehaviorState() == KiwiBehaviorState.ENTERING_SLEEP
                    && --this.kiwi.behaviorTicks <= 0) {
                this.kiwi.setBehaviorState(KiwiBehaviorState.SLEEPING, 0);
            }
        }

        @Override
        public void stop() {
            this.kiwi.setBehaviorState(KiwiBehaviorState.AWAKE, 0);
        }
    }

    private static final class KiwiIdleActionGoal extends Goal {
        private final KiwiEntity kiwi;
        private int cooldown;

        private KiwiIdleActionGoal(KiwiEntity kiwi) {
            this.kiwi = kiwi;
            this.cooldown = 80 + kiwi.getRandom().nextInt(161);
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown-- > 0) {
                return false;
            }
            return this.kiwi.canStartCalmBehavior() && this.kiwi.isActiveTime();
        }

        @Override
        public boolean canContinueToUse() {
            KiwiBehaviorState state = this.kiwi.getBehaviorState();
            return state == KiwiBehaviorState.IDLE_VARIATION
                    && this.kiwi.behaviorTicks > 0
                    && this.kiwi.hurtTime <= 0;
        }

        @Override
        public void start() {
            this.kiwi.getNavigation().stop();
            this.kiwi.setBehaviorState(KiwiBehaviorState.IDLE_VARIATION, 45);
        }

        @Override
        public void tick() {
            this.kiwi.getNavigation().stop();
            --this.kiwi.behaviorTicks;
        }

        @Override
        public void stop() {
            this.kiwi.setBehaviorState(KiwiBehaviorState.AWAKE, 0);
            this.cooldown = 100 + this.kiwi.getRandom().nextInt(181);
        }
    }
}
