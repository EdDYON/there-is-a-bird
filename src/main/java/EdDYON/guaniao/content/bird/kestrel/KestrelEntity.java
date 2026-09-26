package EdDYON.guaniao.content.bird.kestrel;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.advancement.BirdAdvancements;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.BirdItemSafety;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.brain.BirdBrain;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.columbid.AbstractColumbidEntity;
import EdDYON.guaniao.content.bird.command.BirdCommandInteraction;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.content.bird.command.CommandableBird;
import EdDYON.guaniao.content.bird.flight.BirdFlightAnimation;
import EdDYON.guaniao.content.bird.flight.BirdFlightAware;
import EdDYON.guaniao.content.bird.flight.BirdFlightProfile;
import EdDYON.guaniao.content.bird.mutation.BirdMutation;
import EdDYON.guaniao.content.bird.mutation.BirdMutationHolder;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.content.bird.species.KestrelProfile;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
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

/** A solitary diurnal falcon with its own perch, hover, dive and recovery loop. */
public class KestrelEntity extends TamableAnimal implements GeoEntity, FlyingAnimal,
        ScalableBirdModel, BirdFlightAware, CommandableBird, BirdMutationHolder {
    private static final EntityDataAccessor<Float> MODEL_SCALE =
            SynchedEntityData.defineId(KestrelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> BEHAVIOR_STATE =
            SynchedEntityData.defineId(KestrelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COMMAND_MODE =
            SynchedEntityData.defineId(KestrelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MUTATION =
            SynchedEntityData.defineId(KestrelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> CARRIED_ITEM =
            SynchedEntityData.defineId(KestrelEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> LEARNING_ITEM =
            SynchedEntityData.defineId(KestrelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> WING_MODE =
            SynchedEntityData.defineId(KestrelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> GRIP_X =
            SynchedEntityData.defineId(KestrelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GRIP_Y =
            SynchedEntityData.defineId(KestrelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GRIP_Z =
            SynchedEntityData.defineId(KestrelEntity.class, EntityDataSerializers.FLOAT);
    private static final int WING_GLIDE = 0;
    private static final int WING_PULSE = 1;
    private static final int WING_CONTINUOUS = 2;

    private static final String MUTATION_NBT_KEY = "BirdMutation";
    private static final String STATE_NBT_KEY = "KestrelState";
    private static final String STATE_TICKS_NBT_KEY = "KestrelStateTicks";
    private static final String HOME_NBT_KEY = "KestrelHome";
    private static final String LEARNED_ITEM_NBT_KEY = "KestrelLearnedItem";
    private static final String CARRIED_ITEM_NBT_KEY = "KestrelCarriedItem";
    private static final String HUNT_COOLDOWN_NBT_KEY = "KestrelHuntCooldown";
    private static final String SCOUT_COOLDOWN_NBT_KEY = "KestrelScoutCooldown";
    private static final byte TAMING_FAILED_EVENT = 6;
    private static final byte TAMING_SUCCEEDED_EVENT = 7;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation IDLE_1_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_1").thenLoop("idle");
    private static final RawAnimation IDLE_2_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_2").thenLoop("idle");
    private static final RawAnimation IDLE_3_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_3").thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SLEEP_ANIMATION = RawAnimation.begin().thenPlay("sleep").thenLoop("sleep_loop");
    private static final RawAnimation GLIDE_ANIMATION = RawAnimation.begin().thenLoop("fly_loop");
    private static final RawAnimation FLAP_THEN_GLIDE_ANIMATION =
            RawAnimation.begin().thenPlay("fly_flapping_wing").thenLoop("fly_loop");
    private static final RawAnimation FLAP_LOOP_ANIMATION = RawAnimation.begin().thenLoop("fly_flapping_wing_loop");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final BirdBrain birdBrain = new BirdBrain(this, KestrelProfile.INSTANCE);
    private final KestrelFlightMotor flightMotor = new KestrelFlightMotor();
    private Vec3 launchHeading = new Vec3(1, 0, 0);
    private Vec3 orbitCenter;
    private double orbitRadius;
    private double orbitAltitude;
    private int orbitDirection;
    private int orbitTicks;
    private int launchTicks;
    private int hoverApproachTicks;
    private int fetchTurnTicks;
    private int diveTicks;
    private int recoveryTicks;
    private Vec3 committedDivePoint;
    private Vec3 diveStart;
    private Vec3 previousTalonPosition;
    private Vec3 recoveryHeading;
    private Vec3 landingPoint;
    private Vec3 landingApproach;
    private boolean finalLanding;
    private int nextFlapTicks = 40;
    private int flapTicks;
    private float bankAngle;
    private float previousBankAngle;
    private GuidePreviewAnimation guidePreviewAnimation = GuidePreviewAnimation.NONE;
    private int stateTicks = 80;
    private int huntCooldown;
    private int scoutCooldown;
    private int assistCooldown;
    private int fetchScanCooldown;
    private int preyFrightenCooldown;
    private int learningTicks;
    private int carriedPreyTicks;
    private int carriedPreyReleaseTicks;
    private int ownerPerchCooldown;
    private double carriedPreyDropHeight;
    private int lastOwnerAttackTimestamp;
    private int patrolTicks;
    private boolean assistDive;
    @Nullable
    private Vec3 flightTarget;
    @Nullable
    private Vec3 hoverAnchor;
    @Nullable
    private Vec3 activityCenter;
    @Nullable
    private BlockPos homePos;
    @Nullable
    private LivingEntity huntTarget;
    @Nullable
    private LivingEntity ownerAttackTarget;
    @Nullable
    private UUID fetchTargetId;
    @Nullable
    private ResourceLocation learnedFetchItem;

    public KestrelEntity(EntityType<? extends KestrelEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 16, false) {
            @Override public void tick() {
                if (KestrelEntity.this.isBirdFlightActive() || KestrelEntity.this.isPassenger()) {
                    KestrelEntity.this.setXxa(0);
                    KestrelEntity.this.setYya(0);
                    KestrelEntity.this.setZza(0);
                    return;
                }
                super.tick();
            }
        };
        this.setPathfindingMalus(BlockPathTypes.LEAVES, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 16.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, KestrelDefinition.MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, KestrelDefinition.ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, KestrelDefinition.WALK_SPEED)
                .add(Attributes.FLYING_SPEED, KestrelDefinition.FLYING_SPEED)
                .add(Attributes.FOLLOW_RANGE, KestrelDefinition.FOLLOW_RANGE);
    }

    public static boolean canSpawn(EntityType<KestrelEntity> type, ServerLevelAccessor level,
                                   MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean openGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(BlockTags.SAND)
                || below.is(BlockTags.BASE_STONE_OVERWORLD);
        if (!openGround || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
                || !level.canSeeSky(pos) || level.getRawBrightness(pos, 0) <= 8) {
            return false;
        }
        int blockedColumns = 0;
        for (int x = -3; x <= 3; x += 3) {
            for (int z = -3; z <= 3; z += 3) {
                BlockPos column = pos.offset(x, 0, z);
                if (!level.canSeeSky(column)) {
                    ++blockedColumns;
                }
            }
        }
        return blockedColumns <= 2;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.50D, 0.0006F) {
            @Override public boolean canUse() { return canGroundIdle() && super.canUse(); }
            @Override public boolean canContinueToUse() { return canGroundIdle() && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 10.0F) {
            @Override public boolean canUse() { return canGroundIdle() && super.canUse(); }
            @Override public boolean canContinueToUse() { return canGroundIdle() && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this) {
            @Override public boolean canUse() { return canGroundIdle() && super.canUse(); }
            @Override public boolean canContinueToUse() { return canGroundIdle() && super.canContinueToUse(); }
        });
    }

    private boolean canGroundIdle() {
        Player nearby = this.level().getNearestPlayer(this, 12.0D);
        return this.onGround() && !this.isPassenger() && !this.isBirdFlightActive()
                && this.getKestrelBehaviorState() == KestrelBehaviorState.PERCHED
                && this.getBirdCommandMode() == BirdCommandMode.FREE
                && !this.isValidHuntTarget(this.huntTarget)
                && (nearby == null || !isTemptingFood(nearby));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(true);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MODEL_SCALE, BirdModelScale.DEFAULT_INDIVIDUAL_SCALE);
        this.entityData.define(BEHAVIOR_STATE, KestrelBehaviorState.PERCHED.ordinal());
        this.entityData.define(COMMAND_MODE, BirdCommandMode.FREE.ordinal());
        this.entityData.define(MUTATION, BirdMutation.NONE.ordinal());
        this.entityData.define(CARRIED_ITEM, ItemStack.EMPTY);
        this.entityData.define(LEARNING_ITEM, false);
        this.entityData.define(WING_MODE, WING_GLIDE);
        this.entityData.define(GRIP_X, 0.0F);
        this.entityData.define(GRIP_Y, 0.0F);
        this.entityData.define(GRIP_Z, 0.0F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
        if (tag == null || !tag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.randomizeModelScale();
        }
        if (tag == null || !tag.contains(MUTATION_NBT_KEY, 3)) {
            this.setBirdMutation(BirdMutation.randomMutation(this.getRandom()));
        }
        this.activityCenter = this.position();
        this.stateTicks = this.perchWaitTicks(80, 300);
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        BirdModelScale.save(tag, this.getIndividualModelScale(), this.modelScaleProfile());
        tag.putInt(MUTATION_NBT_KEY, this.getBirdMutation().ordinal());
        tag.putInt(CommandableBird.COMMAND_MODE_NBT_KEY, this.getBirdCommandMode().ordinal());
        tag.putInt(STATE_NBT_KEY, this.getKestrelBehaviorState().ordinal());
        tag.putInt(STATE_TICKS_NBT_KEY, this.stateTicks);
        tag.putInt(HUNT_COOLDOWN_NBT_KEY, this.huntCooldown);
        tag.putInt(SCOUT_COOLDOWN_NBT_KEY, this.scoutCooldown);
        tag.putFloat("KestrelGripX", this.entityData.get(GRIP_X));
        tag.putFloat("KestrelGripY", this.entityData.get(GRIP_Y));
        tag.putFloat("KestrelGripZ", this.entityData.get(GRIP_Z));
        if (this.homePos != null) {
            tag.putLong(HOME_NBT_KEY, this.homePos.asLong());
        }
        if (this.learnedFetchItem != null) {
            tag.putString(LEARNED_ITEM_NBT_KEY, this.learnedFetchItem.toString());
        }
        ItemStack carried = this.getCarriedItem();
        if (!carried.isEmpty()) {
            tag.put(CARRIED_ITEM_NBT_KEY, carried.save(new CompoundTag()));
        }
        this.birdBrain.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setIndividualModelScale(BirdModelScale.load(tag, this.modelScaleProfile()));
        if (tag.contains(MUTATION_NBT_KEY, 3)) {
            this.setBirdMutation(BirdMutation.byId(tag.getInt(MUTATION_NBT_KEY)));
        }
        this.setBirdCommandMode(tag.contains(CommandableBird.COMMAND_MODE_NBT_KEY, 3)
                ? BirdCommandMode.byId(tag.getInt(CommandableBird.COMMAND_MODE_NBT_KEY))
                : (this.isTame() ? BirdCommandMode.FOLLOW : BirdCommandMode.FREE));
        KestrelBehaviorState loadedState = KestrelBehaviorState.byId(tag.getInt(STATE_NBT_KEY));
        this.setKestrelBehaviorState(loadedState.isAirborne() ? KestrelBehaviorState.PATROL : loadedState);
        this.stateTicks = Math.max(1, tag.getInt(STATE_TICKS_NBT_KEY));
        this.huntCooldown = Math.max(0, tag.getInt(HUNT_COOLDOWN_NBT_KEY));
        this.scoutCooldown = Math.max(0, tag.getInt(SCOUT_COOLDOWN_NBT_KEY));
        this.entityData.set(GRIP_X, tag.getFloat("KestrelGripX"));
        this.entityData.set(GRIP_Y, tag.getFloat("KestrelGripY"));
        this.entityData.set(GRIP_Z, tag.getFloat("KestrelGripZ"));
        if (tag.contains(HOME_NBT_KEY, 4)) {
            this.homePos = BlockPos.of(tag.getLong(HOME_NBT_KEY));
        }
        if (tag.contains(LEARNED_ITEM_NBT_KEY, 8)) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString(LEARNED_ITEM_NBT_KEY));
            if (parsed != null && BuiltInRegistries.ITEM.containsKey(parsed)) {
                this.learnedFetchItem = parsed;
            }
        }
        if (tag.contains(CARRIED_ITEM_NBT_KEY, 10)) {
            ItemStack carried = ItemStack.of(tag.getCompound(CARRIED_ITEM_NBT_KEY));
            if (isValidFetchItem(carried)) {
                carried.setCount(Math.min(16, carried.getCount()));
                this.setCarriedItem(carried);
            }
        }
        this.birdBrain.load(tag);
        this.activityCenter = this.position();
    }

    @Override
    public void aiStep() {
        this.previousTalonPosition = this.talonPosition();
        super.aiStep();
        Player perchedOwner = this.getPerchedOwner();
        if (perchedOwner != null) {
            if (this.level().isClientSide) {
                return;
            }
            this.updateOwnerAttackTarget(perchedOwner);
            if (this.shouldLeaveOwnerPerch(perchedOwner)) {
                this.leaveOwnerPerch(perchedOwner);
            } else {
                this.setKestrelBehaviorState(KestrelBehaviorState.OWNER_PERCH);
                this.getNavigation().stop();
                this.setDeltaMovement(Vec3.ZERO);
                // Repair an existing perch whose mount packet was missed by the owner.
                if (this.tickCount % 100 == 0) {
                    this.syncOwnerPassengers(perchedOwner);
                }
                return;
            }
        }
        if (this.level().isClientSide) {
            this.tickVisualBank();
            return;
        }
        if (this.isNoAi()) {
            return;
        }
        this.flightMotor.beginTick();
        this.tickKestrelBehavior();
        this.flightMotor.tick(this);
        this.tickFlightAnimation();
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isBirdFlightActive() && !this.isPassenger() && !this.isInWaterOrBubble()) {
            // The motor owns air acceleration. Vanilla ground drag would otherwise
            // consume its acceleration allowance and reduce cruise speed every tick.
            if (this.isEffectiveAi()) this.move(MoverType.SELF, this.getDeltaMovement());
            this.calculateEntityAnimation(false);
            return;
        }
        super.travel(input);
    }

    private void tickKestrelBehavior() {
        this.birdBrain.tick();
        this.tickCounters();
        if (this.activityCenter == null) {
            this.activityCenter = this.position();
        }
        LivingEntity carriedPrey = this.getCarriedPrey();
        if (carriedPrey != null) {
            if (this.getKestrelBehaviorState() != KestrelBehaviorState.CARRY_PREY) {
                this.beginCarryingPrey(carriedPrey);
            }
            this.tickCarryingPrey();
            return;
        }
        if (this.isInWaterOrBubble()) {
            this.startFlee(this.position().subtract(0.0D, 1.0D, 0.0D));
        }
        this.tickNearbyPreyFear();
        if (this.tickTemptingPlayer()) {
            return;
        }
        if (this.tickImmediateThreat()) {
            this.tickStateMachine();
            return;
        }
        if (this.tickOwnerAssist()) {
            this.tickStateMachine();
            return;
        }
        if (this.tickCommandBehavior()) {
            return;
        }
        if (this.tickFetchBehavior()) {
            return;
        }
        this.tickStateMachine();
    }

    private void tickCounters() {
        if (this.stateTicks > 0) --this.stateTicks;
        if (this.huntCooldown > 0) --this.huntCooldown;
        if (this.scoutCooldown > 0) --this.scoutCooldown;
        if (this.assistCooldown > 0) --this.assistCooldown;
        if (this.fetchScanCooldown > 0) --this.fetchScanCooldown;
        if (this.preyFrightenCooldown > 0) --this.preyFrightenCooldown;
        if (this.ownerPerchCooldown > 0) --this.ownerPerchCooldown;
        if (this.learningTicks > 0 && --this.learningTicks == 0) {
            this.entityData.set(LEARNING_ITEM, false);
        }
    }

    private boolean tickImmediateThreat() {
        if (BirdConfigManager.aprilFoolsMode() || this.isTame()) {
            return false;
        }
        Player player = this.level().getNearestPlayer(this, 8.0D);
        if (player == null || player.isCreative() || player.isSpectator() || isTemptingFood(player)) {
            return false;
        }
        double distanceSqr = this.distanceToSqr(player);
        if (distanceSqr <= 16.0D && this.getKestrelBehaviorState() != KestrelBehaviorState.FLEE) {
            this.startFlee(player.position());
        } else if (distanceSqr <= 64.0D && !this.isBirdFlightActive()) {
            this.getLookControl().setLookAt(player, 30.0F, this.getMaxHeadXRot());
        }
        return this.getKestrelBehaviorState() == KestrelBehaviorState.FLEE;
    }

    private boolean tickTemptingPlayer() {
        if (this.isTame()) return false;
        Player player = this.level().getNearestPlayer(this, 12.0D);
        if (player == null || player.isSpectator() || !isTemptingFood(player)) return false;
        this.huntTarget = null;
        this.getLookControl().setLookAt(player, 30.0F, this.getMaxHeadXRot());
        if (this.distanceToSqr(player) <= 6.25D) {
            this.getNavigation().stop();
            if (this.onGround()) {
                this.setKestrelBehaviorState(KestrelBehaviorState.PERCHED);
                this.setNoGravity(false);
            } else {
                this.setKestrelBehaviorState(KestrelBehaviorState.FOLLOW_OWNER);
                this.requestArrival(player.position().add(0, 1.5D, 0),
                        0.35D, 5.0D, -0.16D, 0.18D, Vec3.ZERO, player.getEyePosition());
            }
        } else if (this.isBirdFlightActive()) {
            this.setKestrelBehaviorState(KestrelBehaviorState.FOLLOW_OWNER);
            this.requestArrival(player.position().add(0.0D, 1.5D, 0.0D),
                    0.48D, 5.0D, -0.18D, 0.18D, Vec3.ZERO, null);
        } else {
            this.setKestrelBehaviorState(KestrelBehaviorState.PERCHED);
            this.getNavigation().moveTo(player, 0.68D);
        }
        return true;
    }

    private boolean tickOwnerAssist() {
        LivingEntity owner = this.getOwner();
        if (!this.isTame() || owner == null) {
            this.ownerAttackTarget = null;
            return false;
        }
        this.updateOwnerAttackTarget(owner);
        if (!this.isValidHuntTarget(this.ownerAttackTarget)) {
            if (this.huntTarget == this.ownerAttackTarget) {
                this.huntTarget = null;
            }
            this.ownerAttackTarget = null;
            return false;
        }
        this.huntTarget = this.ownerAttackTarget;
        this.assistDive = true;
        KestrelBehaviorState state = this.getKestrelBehaviorState();
        if (state == KestrelBehaviorState.TAKEOFF || state == KestrelBehaviorState.HOVER_SEARCH
                || state == KestrelBehaviorState.TARGET_LOCKED || state == KestrelBehaviorState.ASSIST_ATTACK
                || state == KestrelBehaviorState.DIVE_ATTACK || state == KestrelBehaviorState.CARRY_PREY
                || state == KestrelBehaviorState.RECOVER) {
            return true;
        }
        if (!this.isBirdFlightActive()) {
            this.beginTakeoff();
            return true;
        }
        if (this.assistCooldown <= 0) {
            this.beginTargetLock(true);
        } else if (state != KestrelBehaviorState.PATROL) {
            this.patrolTicks = Math.max(this.patrolTicks, this.assistCooldown + 40);
            this.changeState(KestrelBehaviorState.PATROL, this.randomBetween(70, 120));
        }
        return true;
    }

    private void updateOwnerAttackTarget(LivingEntity owner) {
        LivingEntity target = owner.getLastHurtMob();
        int timestamp = owner.getLastHurtMobTimestamp();
        if (timestamp == this.lastOwnerAttackTimestamp) {
            return;
        }
        this.lastOwnerAttackTimestamp = timestamp;
        if (this.isValidHuntTarget(target)) {
            this.ownerAttackTarget = target;
        }
    }

    private boolean tickCommandBehavior() {
        if (!this.isTame()) {
            return false;
        }
        KestrelBehaviorState state = this.getKestrelBehaviorState();
        if (state == KestrelBehaviorState.SCOUT) {
            this.tickScout();
            return true;
        }
        if (state == KestrelBehaviorState.TARGET_LOCKED || state == KestrelBehaviorState.ASSIST_ATTACK
                || state == KestrelBehaviorState.DIVE_ATTACK || state == KestrelBehaviorState.RECOVER
                || state == KestrelBehaviorState.CARRY_PREY || state == KestrelBehaviorState.FLEE
                || state == KestrelBehaviorState.FETCH) {
            return false;
        }
        BirdCommandMode mode = this.getBirdCommandMode();
        if (mode == BirdCommandMode.STAY) {
            this.getNavigation().stop();
            if (this.isBirdFlightActive()) {
                if (this.getKestrelBehaviorState() != KestrelBehaviorState.LANDING) {
                    this.beginLanding(this.findPerchNear(this.blockPosition(), 7));
                }
                this.tickLanding();
            } else {
                this.setNoGravity(false);
                this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
                this.setKestrelBehaviorState(KestrelBehaviorState.STAY);
            }
            return true;
        }
        if (mode == BirdCommandMode.ROOST) {
            this.tickHomeCommand();
            return true;
        }
        LivingEntity owner = this.getOwner();
        if (mode == BirdCommandMode.FOLLOW && owner != null) {
            double distanceSqr = this.distanceToSqr(owner);
            boolean canApproachPerch = owner instanceof Player player && this.canPerchOnOwner(player)
                    && distanceSqr <= 36.0D;
            if (distanceSqr > 36.0D || canApproachPerch
                    || this.getKestrelBehaviorState() == KestrelBehaviorState.FOLLOW_OWNER) {
                this.tickFollowOwner(owner);
                return true;
            }
        }
        return false;
    }

    private void tickFollowOwner(LivingEntity owner) {
        this.getNavigation().stop();
        // Player deltaMovement does not always contain walking speed on the server.
        Vec3 ownerVelocity = owner.position().subtract(new Vec3(owner.xo, owner.yo, owner.zo));
        if (ownerVelocity.lengthSqr() > 2.25D) ownerVelocity = Vec3.ZERO;
        boolean canMount = owner instanceof Player player && this.canPerchOnOwner(player);
        Vec3 anchor = canMount ? this.ownerPerchPosition((Player)owner) : owner.position().add(0, 2.4D, 0);
        double distance = this.position().distanceTo(anchor);
        double relativeSpeed = this.getDeltaMovement().subtract(ownerVelocity).length();
        if (canMount && distance < 0.8D && relativeSpeed < 0.22D && this.hasLineOfSight(owner)
                && this.tryPerchOnOwner((Player)owner)) return;
        if (canMount || distance > 3.0D) {
            this.setKestrelBehaviorState(KestrelBehaviorState.FOLLOW_OWNER);
            Vec3 predicted = KestrelFlightPaths.ownerApproachTarget(anchor, ownerVelocity, distance);
            this.flightTarget = predicted;
            this.requestArrival(predicted, 0.64D, 6.0D, -0.22D, 0.28D, ownerVelocity, null);
            return;
        }
        if (!this.onGround()) {
            if (this.getKestrelBehaviorState() != KestrelBehaviorState.LANDING) {
                this.beginLanding(this.findPerchNear(owner.blockPosition(), 6));
            }
            this.tickLanding();
        } else {
            this.changeState(KestrelBehaviorState.PERCHED, this.randomBetween(80, 180));
            this.setNoGravity(false);
        }
    }

    private void tickHomeCommand() {
        if (this.homePos == null) {
            this.homePos = this.blockPosition().immutable();
        }
        BlockPos perch = this.findPerchNear(this.homePos, 9);
        if (this.distanceToSqr(Vec3.atBottomCenterOf(perch)) > 1.2D || this.isBirdFlightActive()) {
            if (this.getKestrelBehaviorState() != KestrelBehaviorState.LANDING) {
                this.setKestrelBehaviorState(KestrelBehaviorState.HOME);
                this.beginLanding(perch);
            }
            this.tickLanding();
        } else {
            this.setNoGravity(false);
            this.setDeltaMovement(Vec3.ZERO);
            this.setKestrelBehaviorState(this.isActiveTime() ? KestrelBehaviorState.PERCHED : KestrelBehaviorState.SLEEP);
        }
    }

    private boolean tickFetchBehavior() {
        if (!this.isTame() || this.learnedFetchItem == null
                || (this.getBirdCommandMode() != BirdCommandMode.FREE
                && this.getBirdCommandMode() != BirdCommandMode.FOLLOW)) {
            return false;
        }
        KestrelBehaviorState state = this.getKestrelBehaviorState();
        if (state == KestrelBehaviorState.TARGET_LOCKED || state == KestrelBehaviorState.ASSIST_ATTACK
                || state == KestrelBehaviorState.DIVE_ATTACK || state == KestrelBehaviorState.RECOVER
                || state == KestrelBehaviorState.CARRY_PREY || state == KestrelBehaviorState.FLEE
                || state == KestrelBehaviorState.SCOUT) {
            return false;
        }
        if (!this.getCarriedItem().isEmpty()) {
            this.tickReturnCarriedItem();
            return true;
        }
        ItemEntity target = this.findFetchTarget();
        if (target == null) {
            if (this.getKestrelBehaviorState() == KestrelBehaviorState.FETCH) {
                this.changeState(this.isBirdFlightActive() ? KestrelBehaviorState.PATROL
                        : KestrelBehaviorState.PERCHED, this.randomBetween(50, 100));
            }
            return false;
        }
        this.setKestrelBehaviorState(KestrelBehaviorState.FETCH);
        this.requestArrival(target.position().add(0.0D, 0.20D, 0.0D), 0.62D, 7.0D, -0.22D, 0.22D, Vec3.ZERO, null);
        if (this.distanceToSqr(target) <= 1.3D) {
            ItemStack stack = target.getItem();
            int amount = Math.min(16, stack.getCount());
            ItemStack picked = stack.copy();
            picked.setCount(amount);
            stack.shrink(amount);
            if (stack.isEmpty()) target.discard();
            this.setCarriedItem(picked);
            this.fetchTurnTicks = 10;
            this.fetchTargetId = null;
        }
        return true;
    }

    @Nullable
    private ItemEntity findFetchTarget() {
        if (this.fetchTargetId != null) {
            Entity entity = ((ServerLevel)this.level()).getEntity(this.fetchTargetId);
            if (entity instanceof ItemEntity item && item.isAlive() && this.matchesLearnedItem(item.getItem())) {
                return item;
            }
            this.fetchTargetId = null;
        }
        if (this.fetchScanCooldown > 0 || !BirdScanBudget.tryAcquire((ServerLevel)this.level(), this)) {
            return null;
        }
        this.fetchScanCooldown = this.randomBetween(30, 55);
        List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class,
                this.getBoundingBox().inflate(16.0D, 8.0D, 16.0D),
                item -> item.isAlive() && this.matchesLearnedItem(item.getItem()));
        return items.stream().min(Comparator.comparingDouble(this::distanceToSqr)).map(item -> {
            this.fetchTargetId = item.getUUID();
            return item;
        }).orElse(null);
    }

    private void tickReturnCarriedItem() {
        LivingEntity owner = this.getOwner();
        if (owner == null) {
            this.dropCarriedItem();
            return;
        }
        this.setKestrelBehaviorState(KestrelBehaviorState.FETCH);
        if (this.fetchTurnTicks > 0) {
            --this.fetchTurnTicks;
            this.moveInFlight(this.position().add(this.horizontalHeading().scale(6)).add(0, 2, 0), 0.42D, 0.04D, 0.20D);
            return;
        }
        Vec3 velocity = owner.position().subtract(new Vec3(owner.xo, owner.yo, owner.zo));
        if (velocity.lengthSqr() > 2.25D) velocity = Vec3.ZERO;
        Vec3 delivery = owner.position().add(0, 1.4D, 0);
        this.requestArrival(KestrelFlightPaths.ownerApproachTarget(delivery, velocity, this.position().distanceTo(delivery)),
                0.64D, 6.0D, -0.22D, 0.26D, velocity, null);
        if (this.distanceToSqr(delivery) < 1.0D && this.getDeltaMovement().subtract(velocity).lengthSqr() < 0.04D) {
            ItemStack delivered = this.getCarriedItem().copy();
            this.setCarriedItem(ItemStack.EMPTY);
            ItemEntity dropped = new ItemEntity(this.level(), owner.getX(), owner.getY() + 0.25D, owner.getZ(), delivered);
            dropped.setPickUpDelay(10);
            this.level().addFreshEntity(dropped);
            this.fetchScanCooldown = 100;
            this.beginRecovery();
        }
    }

    private void tickStateMachine() {
        switch (this.getKestrelBehaviorState()) {
            case PERCHED -> this.tickPerched();
            case TAKEOFF -> this.tickTakeoff();
            case PATROL -> this.tickPatrol();
            case HOVER_SEARCH -> this.tickHoverSearch();
            case TARGET_LOCKED, ASSIST_ATTACK -> this.tickTargetLock();
            case DIVE_ATTACK -> this.tickDiveAttack();
            case CARRY_PREY -> this.tickCarryingPrey();
            case RECOVER -> this.tickRecover();
            case LANDING, HOME -> this.tickLanding();
            case SLEEP -> this.tickSleep();
            case FLEE -> this.tickFlee();
            case FOLLOW_OWNER -> {
                LivingEntity owner = this.getOwner();
                if (owner != null) this.tickFollowOwner(owner);
                else this.changeState(KestrelBehaviorState.PATROL, 80);
            }
            case SCOUT -> this.tickScout();
            case FETCH -> this.tickFetchBehavior();
            case OWNER_PERCH -> this.changeState(KestrelBehaviorState.PERCHED, this.randomBetween(60, 120));
            case STAY -> {
                this.setNoGravity(false);
                this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            }
        }
    }

    private void tickPerched() {
        this.setNoGravity(false);
        this.flightTarget = null;
        this.hoverAnchor = null;
        if (!this.isValidHuntTarget(this.huntTarget)) {
            this.huntTarget = null;
        }
        this.birdBrain.onRest(0.004F);
        if (!this.isActiveTime()) {
            this.changeState(KestrelBehaviorState.SLEEP, this.randomBetween(180, 420));
            return;
        }
        LivingEntity prey = this.huntTarget;
        if (!this.isValidHuntTarget(prey) && this.huntCooldown <= 0) {
            prey = this.birdBrain.senses().nearestPrey();
        }
        if (this.huntCooldown <= 0 && this.isValidHuntTarget(prey)) {
            this.huntTarget = prey;
            this.beginTakeoff();
            return;
        }
        if (this.stateTicks <= 0) {
            this.beginTakeoff();
        }
    }

    private void beginTakeoff() {
        this.getNavigation().stop();
        this.launchHeading = this.horizontalHeading();
        this.launchTicks = 0;
        this.flightTarget = this.position().add(this.launchHeading.scale(8.0D)).add(0, 5, 0);
        this.changeState(KestrelBehaviorState.TAKEOFF, 32);
        this.entityData.set(WING_MODE, WING_CONTINUOUS);
        this.moveInFlight(this.flightTarget, 0.48D, 0.12D, 0.30D);
    }

    private void tickTakeoff() {
        ++this.launchTicks;
        if (this.launchTicks <= 10) {
            this.flightTarget = this.position().add(this.launchHeading.scale(8)).add(0, 5, 0);
        } else if (this.isValidHuntTarget(this.huntTarget)) {
            this.flightTarget = this.observationPoint(this.huntTarget);
        } else {
            this.flightTarget = this.choosePatrolTarget();
        }
        this.moveInFlight(this.flightTarget, 0.58D, 0.10D, 0.30D);
        if (this.stateTicks <= 0) {
            if (this.isValidHuntTarget(this.huntTarget)) {
                this.beginHover(this.huntTarget);
            } else {
                this.patrolTicks = this.activeFlightTicks(240, 480);
                this.changeState(KestrelBehaviorState.PATROL, 100);
            }
        }
    }

    private Vec3 horizontalHeading() {
        Vec3 direction = this.getDeltaMovement().multiply(1, 0, 1);
        if (direction.horizontalDistanceSqr() < 0.0025D) {
            double yaw = Math.toRadians(this.getYRot());
            direction = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        return direction.normalize();
    }

    private Vec3 observationPoint(@Nullable LivingEntity prey) {
        if (prey == null) return this.position().add(this.horizontalHeading().scale(5));
        // Leave a horizontal run-in; hovering almost directly above the prey
        // creates a vertical grab regardless of how smoothly velocity is blended.
        return prey.position().subtract(this.horizontalHeading().scale(14.0D)).add(0, 9.5D, 0);
    }

    private void beginHover(@Nullable LivingEntity prey) {
        this.hoverAnchor = this.observationPoint(prey);
        this.hoverApproachTicks = 0;
        this.changeState(KestrelBehaviorState.HOVER_SEARCH, this.randomBetween(50, 100));
    }

    private void tickPatrol() {
        this.flightTarget = this.choosePatrolTarget();
        this.moveInFlight(this.flightTarget, 0.62D, -0.16D, 0.24D);
        if (this.patrolTicks > 0) --this.patrolTicks;
        if (this.birdBrain.motivation().fatigue() > 0.82F || this.patrolTicks <= 0) {
            this.beginLanding(this.findPerchNear(this.activityCenterBlock(), 18));
            return;
        }
        if (!this.isValidHuntTarget(this.huntTarget)) this.huntTarget = null;
        LivingEntity prey = this.huntTarget;
        if (prey == null && this.huntCooldown <= 0) prey = this.birdBrain.senses().nearestPrey();
        if (this.huntCooldown <= 0 && this.isValidHuntTarget(prey) && this.hasLineOfSight(prey)) {
            this.huntTarget = prey;
            this.beginHover(prey);
        } else if (this.getRandom().nextInt(360) == 0) {
            this.beginHover(null);
        }
    }

    private boolean settleHover() {
        ++this.hoverApproachTicks;
        boolean settled = this.hoverAnchor != null && this.distanceToSqr(this.hoverAnchor) < 1.0D
                && this.getDeltaMovement().lengthSqr() < 0.015D;
        if (!settled) this.stateTicks = Math.max(this.stateTicks, 20);
        if (this.hoverApproachTicks > 220) {
            this.beginRecovery();
            return false;
        }
        return settled;
    }

    private void tickHoverSearch() {
        if (this.hoverAnchor == null) this.hoverAnchor = this.observationPoint(this.huntTarget);
        this.hoverAt(this.hoverAnchor, this.huntTarget);
        if (!this.settleHover()) return;
        if (!this.isValidHuntTarget(this.huntTarget) && this.tickCount % 7 == 0) {
            this.huntTarget = KestrelProfile.INSTANCE.findNearestPrey(this);
        }
        if (this.isValidHuntTarget(this.huntTarget) && this.stateTicks <= 16) {
            this.beginTargetLock(this.isOwnerAttackTarget(this.huntTarget));
        } else if (this.stateTicks <= 0) {
            this.patrolTicks = Math.max(this.patrolTicks, 160);
            this.changeState(KestrelBehaviorState.PATROL, 100);
        }
    }

    private void beginTargetLock(boolean assist) {
        this.assistDive = assist;
        if (this.hoverAnchor == null || this.distanceToSqr(this.hoverAnchor) > 25.0D
                || this.huntTarget != null && this.hoverAnchor.subtract(this.huntTarget.position()).horizontalDistance() < 10.0D
                || this.getKestrelBehaviorState() != KestrelBehaviorState.HOVER_SEARCH) {
            this.hoverAnchor = this.observationPoint(this.huntTarget);
        }
        this.hoverApproachTicks = 0;
        this.committedDivePoint = null;
        this.changeState(assist ? KestrelBehaviorState.ASSIST_ATTACK : KestrelBehaviorState.TARGET_LOCKED,
                this.randomBetween(12, 20));
    }

    private Vec3 predictPrey() {
        double lead = Math.min(4.0D, this.distanceTo(this.huntTarget) * 0.5D);
        Vec3 target = this.huntTarget.position().add(this.preyMovement(this.huntTarget).scale(lead));
        if (this.canLiftPrey(this.huntTarget)) {
            return target.add(0, this.huntTarget.getBbHeight() - KestrelTalons.SOLE_Y * this.getModelRenderScale(), 0)
                    .subtract(this.horizontalHeading().scale(KestrelTalons.FORWARD * this.getModelRenderScale()));
        }
        return target.add(0, this.huntTarget.getBbHeight() * 0.65D, 0);
    }

    private Vec3 preyMovement(LivingEntity target) {
        // Grounded mobs retain gravity in deltaMovement even without moving down.
        Vec3 motion = target.getDeltaMovement().multiply(1, target.onGround() ? 0 : 1, 1);
        return motion.lengthSqr() > 0.25D ? motion.normalize().scale(0.5D) : motion;
    }

    private void tickTargetLock() {
        if (!this.isValidHuntTarget(this.huntTarget) || !this.hasLineOfSight(this.huntTarget)) {
            this.beginRecovery();
            return;
        }
        if (this.hoverAnchor == null) this.hoverAnchor = this.observationPoint(this.huntTarget);
        this.hoverAt(this.hoverAnchor, this.huntTarget);
        if (!this.settleHover()) return;
        Vec3 prediction = this.predictPrey();
        this.committedDivePoint = this.committedDivePoint == null ? prediction
                : this.committedDivePoint.lerp(prediction, 0.15D);
        if (this.stateTicks <= 0) {
            if (this.huntTarget.position().subtract(this.position()).horizontalDistance() < 8.0D) {
                this.hoverAnchor = this.observationPoint(this.huntTarget);
                this.hoverApproachTicks = 0;
                this.stateTicks = 20;
                return;
            }
            Vec3 toward = this.huntTarget.position().subtract(this.position());
            float attackYaw = (float)Math.toDegrees(Math.atan2(toward.z, toward.x)) - 90.0F;
            if (Math.abs(Mth.wrapDegrees(attackYaw - this.getYRot())) > 15.0F) return;
            this.diveTicks = 0;
            this.diveStart = this.position();
            this.changeState(KestrelBehaviorState.DIVE_ATTACK, 100);
        }
    }

    private void tickDiveAttack() {
        if (!this.isValidHuntTarget(this.huntTarget) || this.stateTicks <= 0) {
            this.beginRecovery();
            return;
        }
        ++this.diveTicks;
        if (this.committedDivePoint == null) this.committedDivePoint = this.predictPrey();
        if (this.diveStart == null) this.diveStart = this.position();
        // Commit to the initial line before making small terminal corrections.
        LivingEntity target = this.huntTarget;
        Vec3 preyMovement = this.preyMovement(target);
        this.committedDivePoint = KestrelFlightPaths.correctDive(this.committedDivePoint, this.predictPrey(), this.diveTicks, preyMovement);
        boolean liftable = this.canLiftPrey(target);
        if (liftable && KestrelTalons.touching(this.previousTalonPosition == null ? this.talonPosition() : this.previousTalonPosition,
                this.talonPosition(), target.getBoundingBox(), preyMovement)
                && this.hasLineOfSight(target)) {
            // Bind at the actual contact point. Do not recenter the prey on mount.
            Vec3 offset = target.position().subtract(this.talonPosition().add(0, -target.getBbHeight(), 0));
            this.entityData.set(GRIP_X, (float)offset.x);
            this.entityData.set(GRIP_Y, (float)offset.y);
            this.entityData.set(GRIP_Z, (float)offset.z);
            if (target.startRiding(this, true)) {
                this.beginCarryingPrey(target);
                this.tickCarryingPrey();
                return;
            }
        }
        Vec3 toImpact = this.committedDivePoint.subtract(this.position());
        if (this.diveTicks > 10 && toImpact.lengthSqr() < 16
                && toImpact.dot(this.getDeltaMovement()) < 0) {
            this.beginRecovery();
            return;
        }
        Vec3 approach = KestrelFlightPaths.swoopTarget(this.diveStart, this.committedDivePoint, this.position());
        this.requestArrival(approach, KestrelFlightPaths.swoopSpeed(this.committedDivePoint, this.position()),
                0, -0.55D, 0.12D, KestrelFlightPaths.captureMatchingVelocity(preyMovement, this.distanceTo(target)), null);
        if (!liftable && this.getBoundingBox().inflate(0.10D).intersects(target.getBoundingBox())
                && this.hasLineOfSight(target)) {
            if (this.assistDive) target.hurt(this.damageSources().mobAttack(this), 3.0F);
            else this.doHurtTarget(target);
            if (!target.isAlive()) this.birdBrain.onEat(0.65F);
            this.beginRecovery();
        }
    }

    private boolean canLiftPrey(LivingEntity target) {
        if (target == null || target instanceof Player || target == this || target.isPassenger()
                || target.isVehicle() || !target.isAlive() || this.isVehicle()
                || this.hasSameOwner(target) || !this.getCarriedItem().isEmpty()) {
            return false;
        }
        double volume = target.getBbWidth() * target.getBbWidth() * target.getBbHeight();
        boolean lightPrey = target.getType().is(BirdTags.KESTREL_PREY)
                && volume <= KestrelDefinition.MAX_LIFT_VOLUME;
        boolean smallJuvenile = target instanceof AgeableMob ageable && ageable.isBaby()
                && target.getBbWidth() <= KestrelDefinition.MAX_LIFT_WIDTH
                && target.getBbHeight() <= KestrelDefinition.MAX_LIFT_HEIGHT;
        return lightPrey || smallJuvenile;
    }

    private void beginCarryingPrey(LivingEntity prey) {
        this.huntTarget = prey;
        this.assistDive = false;
        this.carriedPreyTicks = 0;
        this.carriedPreyReleaseTicks = this.randomBetween(35, 60);
        this.carriedPreyDropHeight = KestrelDefinition.MIN_PREY_DROP_HEIGHT
                + this.getRandom().nextDouble()
                * (KestrelDefinition.MAX_PREY_DROP_HEIGHT - KestrelDefinition.MIN_PREY_DROP_HEIGHT);
        this.flightTarget = null;
        this.recoveryHeading = this.horizontalHeading();
        this.setNoGravity(true);
        this.changeState(KestrelBehaviorState.CARRY_PREY, 120);
    }

    private void tickCarryingPrey() {
        LivingEntity prey = this.getCarriedPrey();
        if (prey == null) {
            this.beginRecovery();
            return;
        }
        if (!prey.isAlive() || this.isInWaterOrBubble() || this.hurtTime > 0) {
            this.releaseCarriedPrey(prey);
            return;
        }
        ++this.carriedPreyTicks;
        if (this.carriedPreyTicks <= 12) {
            // Keep travelling through the grab, then gradually add lift.
            if (this.recoveryHeading == null) this.recoveryHeading = this.horizontalHeading();
            double lift = this.carriedPreyTicks * 0.012D;
            this.moveInFlight(this.position().add(this.recoveryHeading.scale(10)).add(0, lift / 0.16D, 0),
                    0.60D, 0, lift);
            return;
        }
        int surface = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING,
                this.getBlockX(), this.getBlockZ());
        double heightAboveGround = this.getY() - surface;
        if (this.carriedPreyTicks >= this.carriedPreyReleaseTicks
                && (heightAboveGround >= this.carriedPreyDropHeight || this.stateTicks <= 0)) {
            this.releaseCarriedPrey(prey);
            return;
        }
        if (this.flightTarget == null || this.position().distanceToSqr(this.flightTarget) < 36.0D) {
            Vec3 forward = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
            if (forward.lengthSqr() < 1.0E-4D) {
                forward = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
            }
            if (forward.lengthSqr() < 1.0E-4D) {
                forward = new Vec3(1.0D, 0.0D, 0.0D);
            }
            double targetY = Math.max(surface + this.carriedPreyDropHeight, this.getY() + 3.5D);
            this.flightTarget = new Vec3(this.getX(), targetY, this.getZ())
                    .add(forward.normalize().scale(this.randomBetween(8, 13)));
        }
        this.moveInFlight(this.flightTarget, 0.68D, 0.10D, 0.34D);
    }

    private void releaseCarriedPrey(LivingEntity prey) {
        Vec3 forward = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() > 1.0E-4D) {
            forward = forward.normalize().scale(0.16D);
        }
        prey.stopRiding();
        prey.setNoGravity(false);
        prey.setDeltaMovement(forward.add(0.0D, -0.12D, 0.0D));
        this.carriedPreyTicks = 0;
        this.carriedPreyReleaseTicks = 0;
        this.beginRecovery();
    }

    @Nullable
    private LivingEntity getCarriedPrey() {
        if (this.getPassengers().isEmpty()) {
            return null;
        }
        Entity passenger = this.getPassengers().get(0);
        return passenger instanceof LivingEntity living ? living : null;
    }

    private void beginRecovery() {
        boolean completedOwnerAttack = this.isOwnerAttackTarget(this.huntTarget);
        this.recoveryHeading = this.horizontalHeading();
        this.recoveryTicks = 0;
        this.flightTarget = this.position().add(this.recoveryHeading.scale(14)).add(0, 6, 0);
        this.huntCooldown = this.randomBetween(80, 160);
        if (completedOwnerAttack) this.assistCooldown = this.randomBetween(60, 100);
        this.assistDive = false;
        if (!this.isValidHuntTarget(this.huntTarget)) this.huntTarget = null;
        this.changeState(KestrelBehaviorState.RECOVER, 38);
    }

    private void tickRecover() {
        ++this.recoveryTicks;
        if (this.recoveryHeading == null) this.recoveryHeading = this.horizontalHeading();
        // The pass keeps forward momentum; vertical speed flattens before lift builds.
        double lift = KestrelFlightPaths.recoveryLift(this.recoveryTicks);
        this.flightTarget = this.position().add(this.recoveryHeading.scale(12)).add(0, lift / 0.16D, 0);
        this.moveInFlight(this.flightTarget, this.recoveryTicks <= 7 ? 0.90D : 0.68D, -0.04D, lift);
        if (this.stateTicks <= 0) {
            this.patrolTicks = this.randomBetween(200, 360);
            this.changeState(KestrelBehaviorState.PATROL, 100);
        }
    }

    private void beginLanding(@Nullable BlockPos perch) {
        BlockPos destination = perch == null ? this.blockPosition() : perch;
        this.landingPoint = Vec3.atBottomCenterOf(destination);
        this.landingApproach = this.landingPoint.subtract(this.horizontalHeading().scale(3)).add(0, 2.5D, 0);
        this.finalLanding = false;
        this.flightTarget = this.landingApproach;
        this.changeState(KestrelBehaviorState.LANDING, 220);
    }

    private void tickLanding() {
        if (this.landingPoint == null) {
            this.beginLanding(this.findPerchNear(this.activityCenterBlock(), 16));
        }
        if (!this.finalLanding && this.distanceToSqr(this.landingApproach) < 1.0D
                && this.getDeltaMovement().lengthSqr() < 0.09D) this.finalLanding = true;
        Vec3 target = this.finalLanding ? this.landingPoint : this.landingApproach;
        double distance = this.position().distanceTo(this.landingPoint);
        if (this.finalLanding && distance < 0.28D && this.getDeltaMovement().lengthSqr() < 0.012D) {
            // Do not accept an airborne coordinate as a perch after its support is removed.
            if (!this.level().getBlockState(BlockPos.containing(this.landingPoint).below())
                    .getCollisionShape(this.level(), BlockPos.containing(this.landingPoint).below()).isEmpty()) {
                this.flightMotor.beginTick();
                this.setNoGravity(false);
                this.setDeltaMovement(Vec3.ZERO);
                this.flightTarget = null;
                this.landingPoint = null;
                this.changeState(this.isActiveTime() ? KestrelBehaviorState.PERCHED : KestrelBehaviorState.SLEEP,
                        this.perchWaitTicks(100, 260));
                return;
            }
        }
        this.flightTarget = target;
        this.requestArrival(target, this.finalLanding ? 0.25D : 0.46D,
                this.finalLanding ? 2.5D : 5.0D, -0.18D, 0.16D, Vec3.ZERO, null);
        if (this.stateTicks <= 0) {
            this.landingPoint = null;
            this.patrolTicks = 160;
            this.changeState(KestrelBehaviorState.PATROL, 100);
        }
    }

    private void tickSleep() {
        this.setNoGravity(false);
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        this.birdBrain.onRest(0.010F);
        if (this.isActiveTime() || this.hurtTime > 0) {
            this.changeState(KestrelBehaviorState.PERCHED, this.randomBetween(40, 100));
        }
    }

    private void startFlee(Vec3 threat) {
        Vec3 away = this.position().subtract(threat).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 1.0E-4D) away = new Vec3(1.0D, 0.0D, 0.0D);
        this.flightTarget = this.position().add(away.normalize().scale(this.randomBetween(10, 16)))
                .add(0.0D, this.randomBetween(5, 8), 0.0D);
        this.setNoGravity(true);
        this.changeState(KestrelBehaviorState.FLEE, this.randomBetween(45, 75));
    }

    private void tickFlee() {
        if (this.flightTarget == null) this.flightTarget = this.choosePatrolTarget();
        this.moveInFlight(this.flightTarget, 0.84D, -0.08D, 0.38D);
        if (this.stateTicks <= 0 || this.position().distanceToSqr(this.flightTarget) < 4.0D) {
            this.activityCenter = this.position();
            this.patrolTicks = this.randomBetween(100, 220);
            this.flightTarget = this.choosePatrolTarget();
            this.changeState(KestrelBehaviorState.PATROL, 80);
        }
    }

    private void tickScout() {
        LivingEntity owner = this.getOwner();
        if (owner == null) {
            this.changeState(KestrelBehaviorState.PATROL, 80);
            return;
        }
        Vec3 observation = owner.position().add(0.0D, 10.0D, 0.0D);
        if (this.position().distanceToSqr(observation) > 5.0D) {
            this.moveInFlight(observation, 0.68D, -0.18D, 0.30D);
        } else {
            this.hoverAt(observation, null);
        }
        if (this.tickCount % 10 == 0 && this.level() instanceof ServerLevel serverLevel
                && BirdScanBudget.tryAcquire(serverLevel, this)) {
            serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(24.0D),
                            entity -> entity != this && entity != owner && entity.isAlive()
                                    && (entity instanceof Enemy || entity.getType().is(BirdTags.KESTREL_PREY)))
                    .stream().sorted(Comparator.comparingDouble(this::distanceToSqr)).limit(8)
                    .forEach(entity -> entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false)));
        }
        if (this.stateTicks <= 0) {
            this.scoutCooldown = this.randomBetween(500, 800);
            this.changeState(KestrelBehaviorState.FOLLOW_OWNER, 80);
        }
    }

    private void hoverAt(Vec3 anchor, @Nullable LivingEntity lookTarget) {
        double phase = (this.tickCount + Math.floorMod(this.getUUID().hashCode(), 97)) * 0.035D;
        Vec3 driftingAnchor = anchor.add(Math.sin(phase) * 0.18D,
                Math.sin(phase * 0.63D) * 0.10D, Math.cos(phase) * 0.18D);
        this.requestArrival(driftingAnchor, 0.48D, 5.0D, -0.20D, 0.24D, Vec3.ZERO,
                lookTarget == null ? null : lookTarget.getEyePosition());
    }

    private KestrelFlightMotor.Mode flightMode() {
        return switch (this.getKestrelBehaviorState()) {
            case TAKEOFF -> KestrelFlightMotor.Mode.LAUNCH;
            case HOVER_SEARCH, TARGET_LOCKED, ASSIST_ATTACK, SCOUT -> KestrelFlightMotor.Mode.HOVER;
            case DIVE_ATTACK -> KestrelFlightMotor.Mode.DIVE;
            case RECOVER, CARRY_PREY -> KestrelFlightMotor.Mode.RECOVER;
            case LANDING, HOME -> KestrelFlightMotor.Mode.LAND;
            case FLEE -> KestrelFlightMotor.Mode.FLEE;
            case FOLLOW_OWNER -> KestrelFlightMotor.Mode.FOLLOW;
            case FETCH -> KestrelFlightMotor.Mode.FETCH;
            default -> KestrelFlightMotor.Mode.CRUISE;
        };
    }

    private void moveInFlight(Vec3 target, double speed, double minVertical, double maxVertical) {
        this.requestArrival(target, speed, 0, minVertical, maxVertical, Vec3.ZERO, null);
    }

    private void requestArrival(Vec3 target, double speed, double radius, double minY, double maxY,
                                Vec3 matching, @Nullable Vec3 look) {
        this.flightMotor.request(new KestrelFlightMotor.Intent(this.flightMode(), target, speed,
                minY, maxY, radius, matching, look));
    }

    private Vec3 choosePatrolTarget() {
        Vec3 position = this.position();
        if (this.orbitCenter == null || this.orbitCenter.subtract(position).horizontalDistance() > 100) {
            this.orbitRadius = this.randomBetween(18, 32);
            this.orbitDirection = this.random.nextBoolean() ? 1 : -1;
            Vec3 heading = this.horizontalHeading();
            this.orbitCenter = position.add(-heading.z * this.orbitDirection * this.orbitRadius,
                    0, heading.x * this.orbitDirection * this.orbitRadius);
            this.orbitAltitude = position.y;
            this.orbitTicks = 0;
        }
        ++this.orbitTicks;
        if (this.activityCenter != null) {
            Vec3 drift = this.activityCenter.subtract(this.orbitCenter).multiply(1, 0, 1);
            this.orbitCenter = this.orbitCenter.add(drift.length() > 0.01D ? drift.normalize().scale(0.01D) : drift);
        }
        double radius = this.orbitRadius + Math.sin(this.orbitTicks * 0.003D) * 2.0D;
        Vec3 ahead = KestrelFlightPaths.orbitTarget(this.orbitCenter, position, radius, this.orbitDirection, this.orbitAltitude);
        double x = ahead.x;
        double z = ahead.z;
        BlockPos column = BlockPos.containing(x, position.y, z);
        if (this.level().hasChunkAt(column)) {
            int ground = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
            double altitude = ground + 18.0D + Math.sin(this.orbitTicks * 0.002D) * 5.0D;
            this.orbitAltitude += Mth.clamp(altitude - this.orbitAltitude, -0.045D, 0.065D);
        }
        return new Vec3(x, Mth.clamp(this.orbitAltitude,
                this.level().getMinBuildHeight() + 3, this.level().getMaxBuildHeight() - 4), z);
    }

    private BlockPos findPerchNear(BlockPos center, int radius) {
        BlockPos best = center;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 28; ++i) {
            int x = center.getX() + this.randomBetween(-radius, radius);
            int z = center.getZ() + this.randomBetween(-radius, radius);
            if (!this.level().hasChunkAt(new BlockPos(x, center.getY(), z))) continue;
            int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos air = new BlockPos(x, y, z);
            BlockState support = this.level().getBlockState(air.below());
            if (!this.level().getBlockState(air).isAir() || !this.level().getBlockState(air.above()).isAir()
                    || support.getCollisionShape(this.level(), air.below()).isEmpty()) continue;
            double score = y - center.getY() - air.distSqr(center) * 0.004D;
            if (support.is(BirdTags.BIRD_PERCHES) || support.is(BlockTags.LEAVES)
                    || support.is(BlockTags.LOGS) || support.getBlock() instanceof FenceBlock
                    || support.getBlock() instanceof WallBlock) {
                score += 12.0D;
            }
            if (this.level().canSeeSky(air)) score += 4.0D;
            if (score > bestScore) {
                bestScore = score;
                best = air.immutable();
            }
        }
        if (bestScore == Double.NEGATIVE_INFINITY) {
            int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, center.getX(), center.getZ());
            best = new BlockPos(center.getX(), y, center.getZ());
        }
        return best;
    }

    private void tickNearbyPreyFear() {
        if (this.preyFrightenCooldown > 0 || !this.isAlive()) return;
        this.preyFrightenCooldown = this.randomBetween(10, 20);
        for (LivingEntity bird : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(12.0D),
                entity -> entity != this && entity.isAlive() && entity.getType().is(BirdTags.KESTREL_PREY))) {
            if (this.hasSameOwner(bird)) continue;
            if (bird instanceof SparrowEntity sparrow) {
                sparrow.birdBrain().onFrightened(0.65F);
            } else if (bird instanceof BudgerigarEntity budgerigar) {
                budgerigar.birdBrain().onFrightened(0.65F);
            } else if (bird instanceof AbstractColumbidEntity columbid) {
                columbid.birdBrain().onFrightened(0.65F);
            }
        }
    }

    private boolean hasSameOwner(LivingEntity other) {
        if (!this.isTame() || !(other instanceof TamableAnimal tamable) || !tamable.isTame()) return false;
        UUID ownerId = this.getOwnerUUID();
        return ownerId != null && ownerId.equals(tamable.getOwnerUUID());
    }

    private boolean isValidHuntTarget(@Nullable LivingEntity target) {
        return target != null && target.isAlive() && !target.isRemoved() && target.level() == this.level()
                && target != this && !this.isAlliedTo(target)
                && (!target.getType().is(BirdTags.KESTREL_PREY) || !this.hasSameOwner(target));
    }

    private boolean isOwnerAttackTarget(@Nullable LivingEntity target) {
        return target != null && target == this.ownerAttackTarget && this.isValidHuntTarget(target);
    }

    private boolean canPerchOnOwner(Player owner) {
        return this.isTame() && this.isOwnedBy(owner) && this.getBirdCommandMode() == BirdCommandMode.FOLLOW
                && this.ownerPerchCooldown <= 0 && this.ownerAttackTarget == null
                && this.getCarriedItem().isEmpty() && !this.isVehicle() && !this.isPassenger()
                && owner.isAlive() && !owner.isSpectator() && !owner.isSleeping()
                && !owner.isPassenger() && owner.getPassengers().isEmpty()
                && !owner.isInWaterOrBubble() && !owner.isFallFlying() && !owner.isShiftKeyDown();
    }

    private boolean tryPerchOnOwner(Player owner) {
        if (!this.canPerchOnOwner(owner) || !this.startRiding(owner, true)) {
            return false;
        }
        this.getNavigation().stop();
        this.setNoGravity(false);
        this.setDeltaMovement(Vec3.ZERO);
        this.setKestrelBehaviorState(KestrelBehaviorState.OWNER_PERCH);
        this.positionOnOwner(owner);
        this.syncOwnerPassengers(owner);
        return true;
    }

    private void syncOwnerPassengers(Player owner) {
        // ServerEntity broadcasts passenger changes to tracking players, excluding the
        // vehicle itself. A player carrying a bird must receive this packet explicitly.
        if (owner instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetPassengersPacket(owner));
        }
    }

    @Override
    public void removeVehicle() {
        Entity previousVehicle = this.getVehicle();
        super.removeVehicle();
        if (previousVehicle instanceof ServerPlayer owner && this.getVehicle() != previousVehicle) {
            this.syncOwnerPassengers(owner);
        }
    }

    private boolean shouldLeaveOwnerPerch(Player owner) {
        return !this.isOwnedBy(owner) || this.getBirdCommandMode() != BirdCommandMode.FOLLOW
                || !owner.isAlive() || owner.isSpectator() || owner.isSleeping()
                || owner.isInWaterOrBubble() || owner.isFallFlying() || owner.isShiftKeyDown()
                || this.isValidHuntTarget(this.ownerAttackTarget) || this.hurtTime > 0;
    }

    private void leaveOwnerPerch(Player owner) {
        this.stopRiding();
        this.ownerPerchCooldown = 100;
        this.setPos(owner.getX(), owner.getEyeY(), owner.getZ());
        this.setNoGravity(true);
        // FOLLOW_OWNER submits the departure intent to the motor below.
        this.changeState(KestrelBehaviorState.FOLLOW_OWNER, 80);
    }

    @Nullable
    private Player getPerchedOwner() {
        return this.getVehicle() instanceof Player player && this.isOwnedBy(player) ? player : null;
    }

    public Vec3 ownerHeadOffset(float headYaw, float headPitch) {
        return KestrelHeadPerch.offset(headYaw, headPitch, KestrelTalons.SOLE_Y * this.getModelRenderScale(),
                KestrelTalons.FORWARD * this.getModelRenderScale());
    }

    public boolean hasExtendedTalons() {
        KestrelBehaviorState state = this.getKestrelBehaviorState();
        return this.getPerchedOwner() != null || state == KestrelBehaviorState.OWNER_PERCH
                || state == KestrelBehaviorState.DIVE_ATTACK || state == KestrelBehaviorState.CARRY_PREY;
    }

    private Vec3 talonPosition() {
        return this.position().add(KestrelTalons.offset(this.getYRot(), this.getXRot(), this.getModelRenderScale()));
    }

    @Override
    public boolean canRiderInteract() {
        // Picking normally ignores entities sharing the camera player's root vehicle.
        return this.getPerchedOwner() != null || super.canRiderInteract();
    }

    private Vec3 ownerPerchPosition(Player owner) {
        return owner.position().add(this.ownerHeadOffset(owner.getYHeadRot(), owner.getXRot()));
    }

    @Override
    public void rideTick() {
        super.rideTick();
        Player owner = this.getPerchedOwner();
        if (owner == null) {
            return;
        }
        this.positionOnOwner(owner);
    }

    private void positionOnOwner(Player owner) {
        Vec3 perch = this.ownerPerchPosition(owner);
        this.setPos(perch.x, perch.y, perch.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.resetFallDistance();
        float yaw = owner.getYHeadRot();
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.setYHeadRot(yaw);
        this.setXRot(0.0F);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult command = BirdCommandInteraction.tryHandle(this, this, player, hand);
        if (command.consumesAction()) return command;
        ItemStack stack = player.getItemInHand(hand);
        if (this.isTame() && this.isOwnedBy(player) && stack.is(Items.SPYGLASS)) {
            if (!this.level().isClientSide && this.scoutCooldown <= 0) {
                this.hoverAnchor = player.position().add(0.0D, 10.0D, 0.0D);
                this.changeState(KestrelBehaviorState.SCOUT, this.randomBetween(200, 400));
                player.displayClientMessage(Component.translatable("message.guaniao.kestrel.scout"), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.isTame() && this.isOwnedBy(player) && isValidFetchItem(stack)
                && !BirdFoodSafety.matchesClean(BirdTags.KESTREL_FOODS, stack)) {
            if (!this.level().isClientSide) {
                this.learnedFetchItem = BuiltInRegistries.ITEM.getKey(stack.getItem());
                this.learningTicks = 40;
                this.entityData.set(LEARNING_ITEM, true);
                player.displayClientMessage(Component.translatable("message.guaniao.kestrel.fetch_learned",
                        stack.getHoverName()), true);
                ((ServerLevel)this.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY() + this.getBbHeight() + 0.2D, this.getZ(),
                        4, 0.15D, 0.10D, 0.15D, 0.02D);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!BirdFoodSafety.matchesClean(BirdTags.KESTREL_FOODS, stack)) {
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide) return InteractionResult.sidedSuccess(true);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        this.getNavigation().stop();
        if (this.isTame()) {
            this.heal(4.0F);
            this.level().broadcastEntityEvent(this, TAMING_SUCCEEDED_EVENT);
        } else if (this.getRandom().nextInt(4) == 0) {
            this.tame(player);
            this.setBirdCommandMode(BirdCommandMode.FOLLOW);
            this.setPersistenceRequired();
            BirdAdvancements.awardTamedBird(player, this);
            this.level().broadcastEntityEvent(this, TAMING_SUCCEEDED_EVENT);
        } else {
            this.level().broadcastEntityEvent(this, TAMING_FAILED_EVENT);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean isValidFetchItem(ItemStack stack) {
        return BirdItemSafety.isSafeDisposableItem(stack) && !stack.isEdible();
    }

    private boolean matchesLearnedItem(ItemStack stack) {
        return this.learnedFetchItem != null && isValidFetchItem(stack)
                && this.learnedFetchItem.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static boolean isTemptingFood(Player player) {
        return BirdFoodSafety.matchesClean(BirdTags.KESTREL_FOODS, player.getMainHandItem())
                || BirdFoodSafety.matchesClean(BirdTags.KESTREL_FOODS, player.getOffhandItem());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return BirdFoodSafety.matchesClean(BirdTags.KESTREL_FOODS, stack);
    }

    @Nullable
    @Override
    public KestrelEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
        KestrelEntity child = GuaniaoEntityTypes.KESTREL.get().create(level);
        if (child != null) {
            float mateScale = mate instanceof KestrelEntity other
                    ? other.getIndividualModelScale() : this.getIndividualModelScale();
            child.setIndividualModelScale(BirdModelScale.inheritIndividualScale(child.getRandom(),
                    this.getIndividualModelScale(), mateScale, child.modelScaleProfile()));
        }
        return child;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            LivingEntity carriedPrey = this.getCarriedPrey();
            if (carriedPrey != null) {
                this.releaseCarriedPrey(carriedPrey);
            }
            this.huntTarget = null;
            this.dropCarriedItem();
            Vec3 threat = source.getEntity() == null ? this.position().subtract(this.getLookAngle())
                    : source.getEntity().position();
            this.startFlee(threat);
        }
        return hurt;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof LivingEntity living
                && this.canLiftPrey(living);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (this.hasPassenger(passenger) && passenger instanceof LivingEntity) {
            Vec3 grip = this.talonPosition().add(this.entityData.get(GRIP_X), this.entityData.get(GRIP_Y),
                    this.entityData.get(GRIP_Z)).add(0, -passenger.getBbHeight(), 0);
            moveFunction.accept(passenger, grip.x, grip.y, grip.z);
            return;
        }
        super.positionRider(passenger, moveFunction);
    }

    private void dropCarriedItem() {
        ItemStack carried = this.getCarriedItem();
        if (!carried.isEmpty() && !this.level().isClientSide) {
            this.spawnAtLocation(carried.copy());
            this.setCarriedItem(ItemStack.EMPTY);
        }
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        this.fallDistance = 0.0F;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        float score = super.getWalkTargetValue(pos, level);
        if (level.canSeeSky(pos)) score += 3.0F;
        if (level.getBlockState(pos.below()).is(BlockTags.LEAVES)) score += 5.0F;
        return score;
    }

    public BirdBrain birdBrain() {
        return this.birdBrain;
    }

    public KestrelBehaviorState getKestrelBehaviorState() {
        return KestrelBehaviorState.byId(this.entityData.get(BEHAVIOR_STATE));
    }

    private void setKestrelBehaviorState(KestrelBehaviorState state) {
        this.entityData.set(BEHAVIOR_STATE, state.ordinal());
    }

    private void changeState(KestrelBehaviorState state, int ticks) {
        this.setKestrelBehaviorState(state);
        this.stateTicks = Math.max(0, ticks);
    }

    @Override
    public BirdCommandMode getBirdCommandMode() {
        return BirdCommandMode.byId(this.entityData.get(COMMAND_MODE));
    }

    @Override
    public void setBirdCommandMode(BirdCommandMode mode) {
        BirdCommandMode safe = mode == null ? BirdCommandMode.FREE : mode;
        this.entityData.set(COMMAND_MODE, safe.ordinal());
        if (!this.level().isClientSide && safe == BirdCommandMode.ROOST && this.homePos == null) {
            this.homePos = this.blockPosition().immutable();
        }
    }

    @Override
    public boolean isBirdEmergencyOverrideActive() {
        return this.getKestrelBehaviorState() == KestrelBehaviorState.FLEE || this.hurtTime > 0;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.isTame();
    }

    @Override
    public BirdFlightProfile birdFlightProfile() {
        return BirdFlightProfile.KESTREL;
    }

    @Override
    public boolean isBirdFlightActive() {
        return !this.isPassenger() && (this.getKestrelBehaviorState().isAirborne() || this.isNoGravity());
    }

    @Override
    public boolean isBirdLanding() {
        KestrelBehaviorState state = this.getKestrelBehaviorState();
        return state == KestrelBehaviorState.LANDING || state == KestrelBehaviorState.HOME;
    }

    @Override
    public boolean isBirdEscaping() {
        return this.getKestrelBehaviorState() == KestrelBehaviorState.FLEE;
    }

    @Override
    public boolean isFlying() {
        return !this.isPassenger()
                && (this.isBirdFlightActive() || (!this.onGround() && !this.isInWaterOrBubble()));
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.KESTREL;
    }

    @Override
    public float getIndividualModelScale() {
        return this.entityData.get(MODEL_SCALE);
    }

    @Override
    public void setIndividualModelScale(float scale) {
        this.entityData.set(MODEL_SCALE, BirdModelScale.sanitize(scale, this.modelScaleProfile()));
    }

    private void randomizeModelScale() {
        this.setIndividualModelScale(BirdModelScale.randomIndividualScale(this.getRandom(), this.modelScaleProfile()));
    }

    @Override
    public BirdMutation getBirdMutation() {
        return BirdMutation.byId(this.entityData.get(MUTATION));
    }

    @Override
    public void setBirdMutation(BirdMutation mutation) {
        this.entityData.set(MUTATION, (mutation == null ? BirdMutation.NONE : mutation).ordinal());
    }

    public ItemStack getCarriedItem() {
        return this.entityData.get(CARRIED_ITEM);
    }

    private void setCarriedItem(ItemStack stack) {
        this.entityData.set(CARRIED_ITEM, stack == null ? ItemStack.EMPTY : stack.copy());
    }

    public boolean isLearningFetchItem() {
        return this.entityData.get(LEARNING_ITEM);
    }

    @Override
    public Component getDisplayName() {
        return this.isLearningFetchItem() ? Component.literal("?") : super.getDisplayName();
    }

    private boolean isActiveTime() {
        long time = Math.floorMod(this.level().getDayTime(), 24000L);
        return time >= 23000L || time < 12500L;
    }

    private double activityMultiplier() {
        long time = Math.floorMod(this.level().getDayTime(), 24000L);
        if (time >= 23000L || time < 2500L) return 1.25D;
        if (time >= 10000L && time < 12500L) return 1.35D;
        return 0.82D;
    }

    private int perchWaitTicks(int min, int max) {
        return Math.max(20, (int)Math.round(this.randomBetween(min, max) / this.activityMultiplier()));
    }

    private int activeFlightTicks(int min, int max) {
        return Math.max(40, (int)Math.round(this.randomBetween(min, max) * this.activityMultiplier()));
    }

    private BlockPos activityCenterBlock() {
        return this.activityCenter == null ? this.blockPosition() : BlockPos.containing(this.activityCenter);
    }

    private int randomBetween(int min, int max) {
        return min + this.getRandom().nextInt(Math.max(1, max - min + 1));
    }

    private void tickFlightAnimation() {
        if (!this.isFlying()) {
            this.entityData.set(WING_MODE, WING_GLIDE);
            return;
        }
        if (this.getKestrelBehaviorState() == KestrelBehaviorState.DIVE_ATTACK) {
            this.flapTicks = 0;
            this.entityData.set(WING_MODE, WING_GLIDE);
            return;
        }
        if (this.nextFlapTicks > 0) --this.nextFlapTicks;
        if (this.flapTicks > 0) --this.flapTicks;
        Vec3 velocity = this.getDeltaMovement();
        boolean needsLift = velocity.y > 0.08D || velocity.horizontalDistance() < 0.24D
                || this.getKestrelBehaviorState() == KestrelBehaviorState.TAKEOFF
                || this.getKestrelBehaviorState() == KestrelBehaviorState.LANDING && this.finalLanding;
        if (this.nextFlapTicks <= 0) {
            // The single flap is 12 ticks long; allow the blend and clip to finish.
            this.flapTicks = 18;
            this.nextFlapTicks = this.randomBetween(40, 90);
        }
        this.entityData.set(WING_MODE, needsLift ? WING_CONTINUOUS
                : this.flapTicks > 0 ? WING_PULSE : WING_GLIDE);
    }

    private void tickVisualBank() {
        this.previousBankAngle = this.bankAngle;
        float limit = this.isBirdEscaping() ? 24.0F : 18.0F;
        float yawChange = Mth.wrapDegrees(this.getYRot() - this.yRotO);
        float target = this.isFlying() ? Mth.clamp(-yawChange * 3.0F, -limit, limit) : 0.0F;
        this.bankAngle = Mth.lerp(0.18F, this.bankAngle, target);
    }

    public float flightBank(float partialTick) {
        return Mth.lerp(partialTick, this.previousBankAngle, this.bankAngle);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.KESTREL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return GuaniaoSoundEvents.KESTREL_HURT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return BirdFlockSoundLimiter.scaledAmbientInterval(this, 360);
    }

    @Override
    public void playAmbientSound() {
        KestrelBehaviorState state = this.getKestrelBehaviorState();
        if (state == KestrelBehaviorState.SLEEP
                || state == KestrelBehaviorState.DIVE_ATTACK
                || state == KestrelBehaviorState.CARRY_PREY) {
            return;
        }
        if (BirdFlockSoundLimiter.allowAmbient(this)) {
            super.playAmbientSound();
        }
    }

    @Override
    public float getSoundVolume() {
        return 0.55F;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, BirdSoundVolume.apply(this, volume), pitch);
    }

    private <T extends KestrelEntity> PlayState movementController(AnimationState<T> animationState) {
        animationState.getController().setAnimationSpeed(1.0D);
        animationState.getController().transitionLength(3);
        if (this.guidePreviewAnimation.animation != null) {
            return animationState.setAndContinue(this.guidePreviewAnimation.animation);
        }
        KestrelBehaviorState state = this.getKestrelBehaviorState();
        if (state == KestrelBehaviorState.OWNER_PERCH || this.getPerchedOwner() != null) {
            return this.isActiveTime() ? this.playIdleAnimation(animationState)
                    : animationState.setAndContinue(SLEEP_ANIMATION);
        }
        if (state == KestrelBehaviorState.SLEEP) {
            return animationState.setAndContinue(SLEEP_ANIMATION);
        }
        if (this.isFlying()) {
            if (state == KestrelBehaviorState.DIVE_ATTACK) {
                return BirdFlightAnimation.play(animationState, GLIDE_ANIMATION);
            }
            if (this.entityData.get(WING_MODE) == WING_CONTINUOUS || state == KestrelBehaviorState.TAKEOFF) {
                animationState.getController().setAnimationSpeed(state == KestrelBehaviorState.FLEE ? 1.25D : 1.08D);
                return BirdFlightAnimation.play(animationState, FLAP_LOOP_ANIMATION);
            }
            if (this.entityData.get(WING_MODE) == WING_PULSE) {
                return BirdFlightAnimation.play(animationState, FLAP_THEN_GLIDE_ANIMATION);
            }
            return BirdFlightAnimation.play(animationState, GLIDE_ANIMATION);
        }
        if (BirdGroundAnimation.hasWalkMotion(this, animationState.isMoving())) {
            animationState.getController().setAnimationSpeed(BirdGroundAnimation.walkAnimationSpeed(this, 1.0D));
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        return this.playIdleAnimation(animationState);
    }

    private <T extends KestrelEntity> PlayState playIdleAnimation(AnimationState<T> animationState) {
        int phase = Math.floorMod(this.tickCount + this.getUUID().hashCode(), 420);
        if (phase < 32) return animationState.setAndContinue(IDLE_1_ANIMATION);
        if (phase >= 140 && phase < 180) return animationState.setAndContinue(IDLE_2_ANIMATION);
        // idle_diff_3 lasts 2.57 seconds; allow it to finish, including the transition.
        if (phase >= 285 && phase < 350) return animationState.setAndContinue(IDLE_3_ANIMATION);
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{
                new AnimationController((GeoAnimatable)this, "movement", 3, this::movementController)
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    public void setGuidePreviewAnimation(GuidePreviewAnimation animation) {
        this.guidePreviewAnimation = animation == null ? GuidePreviewAnimation.NONE : animation;
    }

    public enum GuidePreviewAnimation {
        NONE(null),
        IDLE(IDLE_ANIMATION),
        WALK(WALK_ANIMATION),
        FLY(GLIDE_ANIMATION),
        HOVER(FLAP_LOOP_ANIMATION),
        SLEEP(SLEEP_ANIMATION);

        @Nullable
        private final RawAnimation animation;

        GuidePreviewAnimation(@Nullable RawAnimation animation) {
            this.animation = animation;
        }
    }
}
