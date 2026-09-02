package EdDYON.guaniao.content.bird.seagull;

import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.BirdItemSafety;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.BirdSleepWakeable;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.crow.CrowEntity;
import EdDYON.guaniao.content.advancement.BirdAdvancements;
import EdDYON.guaniao.content.bird.command.BirdCommandInteraction;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.content.bird.command.BirdRoostGoal;
import EdDYON.guaniao.content.bird.command.BirdStayGoal;
import EdDYON.guaniao.content.bird.command.CommandableBird;
import EdDYON.guaniao.content.bird.flight.BirdFlightAware;
import EdDYON.guaniao.content.bird.flight.BirdFlightController;
import EdDYON.guaniao.content.bird.flight.BirdFlightProfile;
import EdDYON.guaniao.content.bird.flight.BirdFlightTargeting;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import EdDYON.guaniao.content.bird.flock.FlockCompatibleBird;
import EdDYON.guaniao.content.bird.mutation.BirdMutation;
import EdDYON.guaniao.content.bird.mutation.BirdMutationHolder;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
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

public class SeagullEntity extends TamableAnimal implements GeoEntity, FlyingAnimal, ScalableBirdModel, BirdFlightAware, CommandableBird, FlockCompatibleBird, BirdSleepWakeable, BirdMutationHolder {
    private static final Map<UUID, Set<UUID>> ACTIVE_THEFT_TARGETS = new HashMap<>();

    public static void clearTheftTargetsFor(UUID playerId) {
        ACTIVE_THEFT_TARGETS.remove(playerId);
    }
    private static final EntityDataAccessor<Float> MODEL_SCALE = SynchedEntityData.defineId(SeagullEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(SeagullEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> EATING = SynchedEntityData.defineId(SeagullEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> HELD_FOOD = SynchedEntityData.defineId(SeagullEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> COMMAND_MODE = SynchedEntityData.defineId(SeagullEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MUTATION = SynchedEntityData.defineId(SeagullEntity.class, EntityDataSerializers.INT);
    public static final String MUTATION_NBT_KEY = "BirdMutation";
    private static final BirdFlightProfile FLIGHT_PROFILE = BirdFlightProfile.SEAGULL;
    private static final byte TAMING_FAILED_EVENT = 6;
    private static final byte TAMING_SUCCEEDED_EVENT = 7;
    private static final String PLAYER_STEAL_COOLDOWN_KEY = "guaniaoSeagullStealCooldownUntil";
    private static final String NBT_HELD_FOOD = "SeagullHeldFood";
    private static final String NBT_HELD_FOOD_TICKS = "SeagullHeldFoodTicks";
    private static final String NBT_STEAL_COOLDOWN = "SeagullStealCooldown";
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
    private int eatingTicks;
    private int heldFoodTicks;
    private int stealCooldownTicks;
    private int emergencyTicks;
    private Vec3 flightTarget;
    @Nullable
    private Player theftTarget;

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

    private static boolean hasNearbyWater(LevelReader level, BlockPos pos, int radius) {
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
        this.goalSelector.addGoal(2, new BirdStayGoal<>(this));
        this.goalSelector.addGoal(2, new BirdRoostGoal<>(this));
        this.goalSelector.addGoal(3, new SeagullFollowOwnerGoal(this));
        this.goalSelector.addGoal(4, new SeagullStealFoodGoal(this));
        this.goalSelector.addGoal(4, new SeagullStealFromBirdGoal(this));
        this.goalSelector.addGoal(4, new SeagullScavengeFoodGoal(this));
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
        this.entityData.define(EATING, false);
        this.entityData.define(HELD_FOOD, ItemStack.EMPTY);
        this.entityData.define(COMMAND_MODE, BirdCommandMode.FREE.ordinal());
        this.entityData.define(MUTATION, BirdMutation.NONE.ordinal());
    }

    @Override
    public BirdMutation getBirdMutation() {
        return BirdMutation.byId(this.entityData.get(MUTATION));
    }

    @Override
    public void setBirdMutation(BirdMutation mutation) {
        this.entityData.set(MUTATION, mutation.ordinal());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag compoundTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, compoundTag);
        if (compoundTag == null || !compoundTag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.randomizeModelScale();
        }
        if (compoundTag == null || !compoundTag.contains(MUTATION_NBT_KEY, 3)) {
            this.setBirdMutation(BirdMutation.randomMutation(this.getRandom()));
        }
        return data;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        BirdModelScale.save(compoundTag, this.getIndividualModelScale(), this.modelScaleProfile());
        compoundTag.putInt(CommandableBird.COMMAND_MODE_NBT_KEY, this.getBirdCommandMode().ordinal());
        compoundTag.putInt(MUTATION_NBT_KEY, this.getBirdMutation().ordinal());
        ItemStack heldFood = this.getHeldFoodForRendering();
        if (!heldFood.isEmpty()) {
            compoundTag.put(NBT_HELD_FOOD, heldFood.save(new CompoundTag()));
            compoundTag.putInt(NBT_HELD_FOOD_TICKS, this.heldFoodTicks);
        }
        compoundTag.putInt(NBT_STEAL_COOLDOWN, this.stealCooldownTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
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
        if (compoundTag.contains(MUTATION_NBT_KEY, 3)) {
            this.setBirdMutation(BirdMutation.byId(compoundTag.getInt(MUTATION_NBT_KEY)));
        }
        if (compoundTag.contains(NBT_HELD_FOOD, 10)) {
            ItemStack heldFood = ItemStack.of(compoundTag.getCompound(NBT_HELD_FOOD));
            if (BirdItemSafety.isSeagullStealableFood(heldFood)) {
                heldFood.setCount(1);
                this.entityData.set(HELD_FOOD, heldFood);
                this.heldFoodTicks = Math.max(1, compoundTag.getInt(NBT_HELD_FOOD_TICKS));
            }
        }
        this.stealCooldownTicks = Math.max(0, compoundTag.getInt(NBT_STEAL_COOLDOWN));
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
        if (this.eatingTicks > 0 && --this.eatingTicks == 0) {
            this.entityData.set(EATING, false);
        }
        if (this.stealCooldownTicks > 0) {
            --this.stealCooldownTicks;
        }
        if (this.emergencyTicks > 0) {
            --this.emergencyTicks;
        }
        if (this.isInWaterOrBubble() && this.flightTicks <= 0) {
            this.setNoGravity(true);
            this.setOnGround(false);
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.32D, 0.0D));
            this.fallDistance = 0.0F;
            this.hasImpulse = true;
            this.flightTicks = 40;
            this.airborneGraceTicks = 80;
        }
        if (!this.getHeldFoodForRendering().isEmpty()) {
            if (this.heldFoodTicks > 0) {
                --this.heldFoodTicks;
            }
            if (this.heldFoodTicks <= 0 && this.onGround() && this.flightTicks <= 0) {
                this.consumeHeldFood();
            }
        }
        this.tickAirCruise();
        this.tickCommandedRest();
        this.tickGroundMovementFacing();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult commandResult = BirdCommandInteraction.tryHandle(this, this, player, hand);
        if (commandResult.consumesAction()) {
            return commandResult;
        }
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
        this.playSound(GuaniaoSoundEvents.SEAGULL_AMBIENT.get(), 0.48F, 0.9F + this.getRandom().nextFloat() * 0.14F);
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
            this.setBirdCommandMode(BirdCommandMode.FOLLOW);
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
        return BirdFoodSafety.matchesClean(BirdTags.SEAGULL_FOODS, stack);
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
        return this.emergencyTicks > 0;
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
    public BirdFlightProfile birdFlightProfile() {
        return FLIGHT_PROFILE;
    }

    @Override
    public boolean isBirdFlightActive() {
        return this.flightTicks > 0
                || this.isNoGravity()
                || (!this.onGround() && this.airborneGraceTicks > 0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            this.theftTarget = null;
            Vec3 sourcePos = source.getEntity() == null ? this.position() : source.getEntity().position();
            this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 240);
            if (this.isBirdSleeping()) {
                this.wakeFromLoudSound(sourcePos);
            }
            if (BirdConfigManager.aprilFoolsMode() && source.getEntity() instanceof Player) {
                return true;
            }
            this.emergencyTicks = Math.max(this.emergencyTicks, 100);
            this.setSleeping(false);
            this.flightCooldown = 0;
            if (this.onGround()) {
                this.startAirCruise();
            }
        }
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        ItemStack heldFood = this.getHeldFoodForRendering();
        if (!heldFood.isEmpty()) {
            this.spawnAtLocation(heldFood.copy());
            this.clearHeldFood();
        }
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
        animationState.getController().setAnimationSpeed(1.0D);
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
        if (this.entityData.get(EATING)) {
            return animationState.setAndContinue(EAT_ANIMATION);
        }
        if (this.shouldPlayWalkAnimation(animationState.isMoving())) {
            animationState.getController().setAnimationSpeed(BirdGroundAnimation.walkAnimationSpeed(this));
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    private boolean shouldPlayFlyAnimation() {
        return !this.isInWaterOrBubble()
                && !this.onGround()
                && (this.isNoGravity() || this.airborneGraceTicks > 0 || !this.isNearGroundForAnimation());
    }

    private boolean shouldPlayWalkAnimation(boolean animationMoving) {
        return !this.isNightResting()
                && !this.isInWaterOrBubble()
                && (BirdGroundAnimation.hasWalkMotion(this, animationMoving)
                || ((this.onGround() || this.isNearGroundForAnimation())
                && (animationMoving
                || this.getDeltaMovement().horizontalDistanceSqr() > WALK_ANIMATION_MOTION_THRESHOLD
                || (!this.level().isClientSide && !this.getNavigation().isDone()))));
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

    @Override
    public boolean isBirdSleeping() {
        return this.isNightResting();
    }

    @Override
    public void wakeFromLoudSound(Vec3 soundPosition) {
        if (this.level().isClientSide || !this.isBirdSleeping()) {
            return;
        }
        this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 240);
        this.emergencyTicks = Math.max(this.emergencyTicks, 60);
        this.setSleeping(false);
        this.getNavigation().stop();
        if (soundPosition != null) {
            this.getLookControl().setLookAt(soundPosition.x, soundPosition.y, soundPosition.z, 35.0F, this.getMaxHeadXRot());
        }
    }

    private void setSleeping(boolean sleeping) {
        if (this.entityData != null && this.entityData.get(SLEEPING) != sleeping) {
            this.entityData.set(SLEEPING, sleeping);
        }
    }

    private void tickCommandedRest() {
        if (!this.isTame()) {
            return;
        }
        BirdCommandMode mode = this.getBirdCommandMode();
        boolean maySleepInPlace = mode == BirdCommandMode.FOLLOW
                || mode == BirdCommandMode.STAY
                || mode == BirdCommandMode.ROOST;
        if (!this.isRestTime() || this.restInterruptionTicks > 0 || !maySleepInPlace) {
            if (this.isBirdSleeping()) {
                this.setSleeping(false);
            }
            return;
        }
        if (this.onGround()
                && this.getNavigation().isDone()
                && this.flightTicks <= 0
                && !this.isInWaterOrBubble()
                && !this.entityData.get(EATING)) {
            this.setSleeping(true);
        } else if (this.isBirdSleeping()) {
            this.setSleeping(false);
        }
    }

    private void startEating() {
        this.eatingTicks = 36;
        this.entityData.set(EATING, true);
        this.setSleeping(false);
        this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 100);
        this.playSound(SoundEvents.GENERIC_EAT, 0.42F, 0.92F + this.getRandom().nextFloat() * 0.16F);
    }

    public ItemStack getHeldFoodForRendering() {
        return this.entityData == null ? ItemStack.EMPTY : this.entityData.get(HELD_FOOD);
    }

    private boolean takeFood(ItemStack source, int holdTicks, @Nullable Vec3 fleeFrom) {
        if (source.isEmpty() || !this.getHeldFoodForRendering().isEmpty()) {
            return false;
        }
        ItemStack held = source.copy();
        held.setCount(1);
        this.entityData.set(HELD_FOOD, held);
        this.heldFoodTicks = Math.max(20, holdTicks);
        if (fleeFrom != null) {
            this.startAirCruiseAwayFrom(fleeFrom);
        }
        return true;
    }

    private void consumeHeldFood() {
        if (this.getHeldFoodForRendering().isEmpty()) {
            return;
        }
        this.clearHeldFood();
        this.startEating();
    }

    private void clearHeldFood() {
        this.entityData.set(HELD_FOOD, ItemStack.EMPTY);
        this.heldFoodTicks = 0;
    }

    private boolean isBusyWithCommand() {
        return this.isTame() && this.getBirdCommandMode() != BirdCommandMode.FREE;
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
        if (BirdFlightController.isFlightProgressStalled(this, this.flightTarget, 12, 12)) {
            Vec3 recovery = BirdFlightTargeting.findRecoveryTarget(this, this.flightTarget.subtract(this.position()), 8, 12);
            if (recovery == null) {
                this.finishAirCruise();
                return;
            }
            this.flightTarget = recovery;
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

    private boolean startAirCruiseAwayFrom(Vec3 source) {
        if (source == null || this.flightTicks > 0 || this.isInWaterOrBubble()) {
            return false;
        }
        this.flightCooldown = 0;
        Vec3 away = this.position().subtract(source).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() <= 1.0E-4D) {
            away = this.randomHorizontalDirection();
        }
        this.flightTarget = BirdFlightTargeting.findAirTarget(this, FLIGHT_PROFILE, away.normalize(), false);
        if (this.flightTarget == null) {
            return this.startAirCruise();
        }
        this.flightTicks = FLIGHT_PROFILE.minFlightTicks() + this.getRandom().nextInt(FLIGHT_PROFILE.maxFlightTicks() - FLIGHT_PROFILE.minFlightTicks() + 1);
        this.airborneGraceTicks = 80;
        this.setNoGravity(true);
        this.setOnGround(false);
        this.getNavigation().stop();
        Vec3 movement = away.normalize().scale(0.52D).add(0.0D, 0.22D, 0.0D);
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
        return BirdFlightTargeting.findRecoveryTarget(this, preferredDirection, 8, 12);
    }

    private void finishAirCruise() {
        this.flightTicks = 0;
        BirdFlightController.clearFlightProgress(this);
        this.flightTarget = null;
        this.setNoGravity(false);
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.x * 0.82D, Math.min(movement.y, -0.04D), movement.z * 0.82D);
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
        return GuaniaoSoundEvents.SEAGULL_AMBIENT.get();
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
        return BirdFlockSoundLimiter.scaledAmbientInterval(this, 190);
    }

    @Override
    public void playAmbientSound() {
        if (BirdFlockSoundLimiter.allowAmbient(this)) {
            super.playAmbientSound();
        }
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
                    && !this.seagull.isBusyWithCommand()
                    && this.seagull.getNavigation().isDone()
                    && this.seagull.getRandom().nextInt(65) == 0;
        }

        @Override
        public void start() {
            this.seagull.startAirCruise();
        }
    }

    private static final class SeagullFollowOwnerGoal extends Goal {
        private final SeagullEntity seagull;
        @Nullable
        private LivingEntity owner;
        private int recalcTicks;

        private SeagullFollowOwnerGoal(SeagullEntity seagull) {
            this.seagull = seagull;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = this.seagull.getOwner();
            if (!this.canFollow(owner) || this.seagull.distanceToSqr(owner) <= 100.0D) {
                return false;
            }
            this.owner = owner;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canFollow(this.owner) && this.seagull.distanceToSqr(this.owner) > 9.0D;
        }

        @Override
        public void tick() {
            if (this.owner == null) {
                return;
            }
            this.seagull.getLookControl().setLookAt(this.owner, 10.0F, this.seagull.getMaxHeadXRot());
            if (--this.recalcTicks <= 0) {
                this.recalcTicks = 10;
                double teleportDistance = BirdConfigManager.ownerTeleportDistance(BirdSpecies.SEAGULL);
                if (this.seagull.distanceToSqr(this.owner) > teleportDistance * teleportDistance && this.tryTeleportNearOwner()) {
                    return;
                }
                this.seagull.getNavigation().moveTo(this.owner, 1.12D);
            }
        }

        @Override
        public void stop() {
            this.owner = null;
            this.seagull.getNavigation().stop();
        }

        private boolean canFollow(@Nullable LivingEntity owner) {
            return owner != null && !owner.isSpectator() && this.seagull.isTame()
                    && this.seagull.isBirdCommandMode(BirdCommandMode.FOLLOW);
        }

        private boolean tryTeleportNearOwner() {
            BlockPos ownerPos = this.owner.blockPosition();
            Vec3 origin = this.seagull.position();
            for (int attempt = 0; attempt < 10; ++attempt) {
                int x = ownerPos.getX() + this.seagull.getRandom().nextInt(7) - 3;
                int y = ownerPos.getY() + this.seagull.getRandom().nextInt(5) - 2;
                int z = ownerPos.getZ() + this.seagull.getRandom().nextInt(7) - 3;
                if (Math.abs(x - ownerPos.getX()) < 2 && Math.abs(z - ownerPos.getZ()) < 2) {
                    continue;
                }
                this.seagull.moveTo(x + 0.5D, y, z + 0.5D, this.seagull.getYRot(), this.seagull.getXRot());
                if (this.seagull.level().noCollision(this.seagull)) {
                    this.seagull.getNavigation().stop();
                    return true;
                }
                this.seagull.moveTo(origin.x, origin.y, origin.z, this.seagull.getYRot(), this.seagull.getXRot());
            }
            return false;
        }
    }

    private static final class SeagullScavengeFoodGoal extends Goal {
        private final SeagullEntity seagull;
        @Nullable
        private ItemEntity target;
        private int recalcTicks;

        private SeagullScavengeFoodGoal(SeagullEntity seagull) {
            this.seagull = seagull;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.seagull.isBusyWithCommand() || this.seagull.entityData.get(EATING)
                    || !this.seagull.getHeldFoodForRendering().isEmpty()
                    || this.seagull.getRandom().nextInt(BirdConfigManager.foodScanInterval(BirdSpecies.SEAGULL)) != 0) {
                return false;
            }
            if (!(this.seagull.level() instanceof ServerLevel serverLevel) || !BirdScanBudget.tryAcquire(serverLevel, this.seagull)) {
                return false;
            }
            this.target = this.seagull.level().getEntitiesOfClass(ItemEntity.class,
                            this.seagull.getBoundingBox().inflate(12.0D, 5.0D, 12.0D),
                            item -> item.isAlive() && BirdItemSafety.isSeagullStealableFood(item.getItem()))
                    .stream().min(java.util.Comparator.comparingDouble(this.seagull::distanceToSqr)).orElse(null);
            return this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.target.isAlive()
                    && BirdItemSafety.isSeagullStealableFood(this.target.getItem())
                    && this.seagull.distanceToSqr(this.target) < 256.0D;
        }

        @Override
        public void tick() {
            if (this.target == null) {
                return;
            }
            this.seagull.getLookControl().setLookAt(this.target, 18.0F, 18.0F);
            if (this.seagull.distanceToSqr(this.target) <= 1.7D) {
                ItemStack stack = this.target.getItem();
                ItemStack food = stack.copy();
                food.setCount(1);
                if (!this.seagull.takeFood(food, 42 + this.seagull.getRandom().nextInt(28), this.target.position())) {
                    return;
                }
                stack.shrink(1);
                if (stack.isEmpty()) {
                    this.target.discard();
                } else {
                    this.target.setItem(stack);
                }
                this.target = null;
                return;
            }
            if (--this.recalcTicks <= 0) {
                this.recalcTicks = 10;
                this.seagull.getNavigation().moveTo(this.target, 1.08D);
            }
        }

        @Override
        public void stop() {
            this.target = null;
            this.seagull.getNavigation().stop();
        }
    }

    private static final class SeagullStealFoodGoal extends Goal {
        private final SeagullEntity seagull;
        @Nullable
        private Player player;
        private InteractionHand hand;
        private int recalcTicks;
        private int observeTicks;
        private boolean bold;
        private boolean cancelled;
        @Nullable
        private UUID claimedPlayerId;

        private SeagullStealFoodGoal(SeagullEntity seagull) {
            this.seagull = seagull;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!BirdConfigManager.seagullStealingEnabled()
                    || BirdConfigManager.maxConcurrentSeagullTargetsPerPlayer() <= 0
                    || this.seagull.isTame() || this.seagull.entityData.get(EATING)
                    || !this.seagull.getHeldFoodForRendering().isEmpty()
                    || this.seagull.stealCooldownTicks > 0
                    || this.seagull.getRandom().nextInt(80) != 0) {
                return false;
            }
            if (!(this.seagull.level() instanceof ServerLevel serverLevel) || !BirdScanBudget.tryAcquire(serverLevel, this.seagull)) {
                return false;
            }
            long now = this.seagull.level().getGameTime();
            this.player = this.seagull.level().getEntitiesOfClass(Player.class,
                            this.seagull.getBoundingBox().inflate(11.0D, 5.0D, 11.0D),
                    player -> !player.isSpectator() && !player.isCreative()
                                    && player.getPersistentData().getLong(PLAYER_STEAL_COOLDOWN_KEY) <= now
                                    && this.findStealableHand(player) != null
                                    && !this.hasCompetingSeagull(player))
                    .stream().min(java.util.Comparator.comparingDouble(this.seagull::distanceToSqr)).orElse(null);
            if (this.player == null) {
                return false;
            }
            this.hand = this.findStealableHand(this.player);
            if (this.hand == null || !this.claimTarget(this.player)) {
                this.player = null;
                this.hand = null;
                return false;
            }
            this.seagull.theftTarget = this.player;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.player != null && this.player.isAlive() && this.hand != null
                    && !this.cancelled
                    && this.seagull.theftTarget == this.player
                    && this.isStealable(this.player.getItemInHand(this.hand))
                    && this.seagull.distanceToSqr(this.player) < 196.0D;
        }

        @Override
        public void start() {
            this.recalcTicks = 0;
            this.observeTicks = 30 + this.seagull.getRandom().nextInt(31);
            this.bold = BirdConfigManager.aprilFoolsMode() || this.seagull.getRandom().nextInt(3) == 0;
            this.cancelled = false;
        }

        @Override
        public void tick() {
            if (this.player == null || this.hand == null) {
                return;
            }
            this.seagull.getLookControl().setLookAt(this.player, 20.0F, 20.0F);
            if (!BirdConfigManager.aprilFoolsMode() && !this.bold && this.isPlayerLookingAtSeagull()) {
                this.cancelled = true;
                this.seagull.getNavigation().stop();
                return;
            }
            double distanceSqr = this.seagull.distanceToSqr(this.player);
            if (this.observeTicks > 0 && distanceSqr >= 25.0D && distanceSqr <= 64.0D) {
                --this.observeTicks;
                this.seagull.getNavigation().stop();
                return;
            }
            if (distanceSqr <= 2.5D) {
                ItemStack stack = this.player.getItemInHand(this.hand);
                if (this.isStealable(stack)) {
                    ItemStack stolen = stack.copy();
                    stolen.setCount(1);
                    if (!this.seagull.takeFood(stolen, 55 + this.seagull.getRandom().nextInt(36), this.player.position())) {
                        this.player = null;
                        return;
                    }
                    stack.shrink(1);
                    this.player.getPersistentData().putLong(PLAYER_STEAL_COOLDOWN_KEY,
                            this.seagull.level().getGameTime() + BirdConfigManager.seagullPlayerCooldownTicks());
                    this.seagull.stealCooldownTicks = 1200 + this.seagull.getRandom().nextInt(1201);
                    BirdAdvancements.awardSeagullStoleFood(this.player);
                }
                this.player = null;
                return;
            }
            if (--this.recalcTicks <= 0) {
                this.recalcTicks = 8;
                this.seagull.getNavigation().moveTo(this.player, 1.18D);
            }
        }

        @Override
        public void stop() {
            this.releaseClaim();
            this.player = null;
            this.hand = null;
            this.seagull.theftTarget = null;
            this.observeTicks = 0;
            this.cancelled = false;
            this.seagull.getNavigation().stop();
        }

        @Nullable
        private InteractionHand findStealableHand(Player player) {
            if (this.isStealable(player.getMainHandItem())) {
                return InteractionHand.MAIN_HAND;
            }
            return this.isStealable(player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
        }

        private boolean isStealable(ItemStack stack) {
            return stack.getCount() > 1 && BirdItemSafety.isSeagullStealableFood(stack);
        }

        private boolean hasCompetingSeagull(Player player) {
            UUID playerId = player.getUUID();
            Set<UUID> active = ACTIVE_THEFT_TARGETS.get(playerId);
            if (active == null) {
                return false;
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                active.removeIf(seagullId -> !(serverLevel.getEntity(seagullId) instanceof SeagullEntity other)
                        || other.theftTarget != player);
            }
            if (active.isEmpty()) {
                ACTIVE_THEFT_TARGETS.remove(playerId);
                return false;
            }
            return active.size() >= BirdConfigManager.maxConcurrentSeagullTargetsPerPlayer();
        }

        private boolean claimTarget(Player player) {
            if (player == null || this.claimedPlayerId != null || this.hasCompetingSeagull(player)) {
                return false;
            }
            this.claimedPlayerId = player.getUUID();
            ACTIVE_THEFT_TARGETS.computeIfAbsent(this.claimedPlayerId, ignored -> new HashSet<>())
                    .add(this.seagull.getUUID());
            return true;
        }

        private void releaseClaim() {
            if (this.claimedPlayerId == null) {
                return;
            }
            ACTIVE_THEFT_TARGETS.computeIfPresent(this.claimedPlayerId, (ignored, active) -> {
                active.remove(this.seagull.getUUID());
                return active.isEmpty() ? null : active;
            });
            this.claimedPlayerId = null;
        }

        private boolean isPlayerLookingAtSeagull() {
            Vec3 toSeagull = this.seagull.getEyePosition().subtract(this.player.getEyePosition());
            return toSeagull.lengthSqr() <= 1.0E-4D
                    || this.player.getLookAngle().normalize().dot(toSeagull.normalize()) >= 0.78D;
        }
    }

    private static final class SeagullStealFromBirdGoal extends Goal {
        private final SeagullEntity seagull;
        @Nullable
        private CrowEntity crowTarget;
        @Nullable
        private ItemEntity itemTarget;
        private int recalcTicks;
        private boolean itemDropped;

        private SeagullStealFromBirdGoal(SeagullEntity seagull) {
            this.seagull = seagull;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.seagull.isBusyWithCommand() || this.seagull.entityData.get(EATING)
                    || !this.seagull.getHeldFoodForRendering().isEmpty()
                    || this.seagull.stealCooldownTicks > 0
                    || this.seagull.getRandom().nextInt(120) != 0) {
                return false;
            }
            if (!(this.seagull.level() instanceof ServerLevel serverLevel) || !BirdScanBudget.tryAcquire(serverLevel, this.seagull)) {
                return false;
            }
            this.crowTarget = this.seagull.level().getEntitiesOfClass(CrowEntity.class,
                            this.seagull.getBoundingBox().inflate(10.0D, 5.0D, 10.0D),
                            crow -> crow.isAlive() && !crow.getHeldFoodForRendering().isEmpty()
                                    && CrowEntity.isCrowFood(crow.getHeldFoodForRendering()))
                    .stream().min(java.util.Comparator.comparingDouble(this.seagull::distanceToSqr)).orElse(null);
            return this.crowTarget != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.itemDropped) {
                return this.itemTarget != null && this.itemTarget.isAlive()
                        && BirdItemSafety.isSeagullStealableFood(this.itemTarget.getItem())
                        && this.seagull.distanceToSqr(this.itemTarget) < 256.0D;
            }
            return this.crowTarget != null && this.crowTarget.isAlive()
                    && !this.crowTarget.getHeldFoodForRendering().isEmpty()
                    && this.seagull.distanceToSqr(this.crowTarget) < 144.0D;
        }

        @Override
        public void start() {
            this.recalcTicks = 0;
            this.itemDropped = false;
            this.itemTarget = null;
        }

        @Override
        public void tick() {
            if (this.itemDropped) {
                this.tickPickupItem();
                return;
            }
            if (this.crowTarget == null) {
                return;
            }
            this.seagull.getLookControl().setLookAt(this.crowTarget, 18.0F, 18.0F);
            if (this.seagull.distanceToSqr(this.crowTarget) <= 2.5D) {
                this.crowTarget.dropHeldItem();
                this.itemDropped = true;
                this.findDroppedItem();
                return;
            }
            if (--this.recalcTicks <= 0) {
                this.recalcTicks = 10;
                this.seagull.getNavigation().moveTo(this.crowTarget, 1.08D);
            }
        }

        private void tickPickupItem() {
            if (this.itemTarget == null) {
                this.findDroppedItem();
                return;
            }
            this.seagull.getLookControl().setLookAt(this.itemTarget, 18.0F, 18.0F);
            if (this.seagull.distanceToSqr(this.itemTarget) <= 1.7D) {
                ItemStack stack = this.itemTarget.getItem();
                ItemStack food = stack.copy();
                food.setCount(1);
                if (!this.seagull.takeFood(food, 42 + this.seagull.getRandom().nextInt(28), this.itemTarget.position())) {
                    return;
                }
                stack.shrink(1);
                if (stack.isEmpty()) {
                    this.itemTarget.discard();
                } else {
                    this.itemTarget.setItem(stack);
                }
                this.itemTarget = null;
                return;
            }
            if (--this.recalcTicks <= 0) {
                this.recalcTicks = 10;
                this.seagull.getNavigation().moveTo(this.itemTarget, 1.08D);
            }
        }

        private void findDroppedItem() {
            this.itemTarget = this.seagull.level().getEntitiesOfClass(ItemEntity.class,
                            this.seagull.getBoundingBox().inflate(8.0D, 4.0D, 8.0D),
                            item -> item.isAlive() && BirdItemSafety.isSeagullStealableFood(item.getItem()))
                    .stream().min(java.util.Comparator.comparingDouble(this.seagull::distanceToSqr)).orElse(null);
        }

        @Override
        public void stop() {
            this.crowTarget = null;
            this.itemTarget = null;
            this.itemDropped = false;
            this.seagull.stealCooldownTicks = 600 + this.seagull.getRandom().nextInt(600);
            this.seagull.getNavigation().stop();
        }
    }

    private static final class SeagullSleepGoal extends Goal {
        private final SeagullEntity seagull;
        private BlockPos sleepTarget;
        private int nextSearchTick;
        private int travelTicks;

        private SeagullSleepGoal(SeagullEntity seagull) {
            this.seagull = seagull;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.canSleepNow() || !this.seagull.onGround() || this.seagull.tickCount < this.nextSearchTick) {
                return false;
            }
            this.nextSearchTick = this.seagull.tickCount + 80 + this.seagull.getRandom().nextInt(40);
            BlockPos currentPos = this.seagull.blockPosition();
            if (this.scoreSleepSite(currentPos, currentPos) >= 20) {
                this.sleepTarget = currentPos.immutable();
            } else {
                this.sleepTarget = this.findCoastalSleepSite(currentPos);
                if (this.sleepTarget == null && BirdFlightTargeting.isSafeDryLanding(this.seagull, currentPos)) {
                    this.sleepTarget = currentPos.immutable();
                }
            }
            return this.sleepTarget != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.sleepTarget != null && this.travelTicks < 240 && this.canSleepNow();
        }

        @Override
        public void start() {
            this.travelTicks = 0;
            this.moveOrSleep();
        }

        @Override
        public void tick() {
            if (!this.seagull.isBirdSleeping()) {
                ++this.travelTicks;
            }
            this.moveOrSleep();
        }

        @Override
        public void stop() {
            this.sleepTarget = null;
            this.travelTicks = 0;
            this.seagull.setSleeping(false);
        }

        private boolean canSleepNow() {
            return this.seagull.isRestTime()
                    && (!this.seagull.isTame()
                    || this.seagull.isBirdCommandMode(BirdCommandMode.FREE)
                    || this.seagull.isBirdCommandMode(BirdCommandMode.STAY))
                    && this.seagull.restInterruptionTicks <= 0
                    && this.seagull.flightTicks <= 0
                    && !this.seagull.isInWaterOrBubble()
                    && !this.seagull.entityData.get(EATING);
        }

        private void moveOrSleep() {
            if (this.sleepTarget == null) {
                return;
            }
            Vec3 target = Vec3.atBottomCenterOf(this.sleepTarget);
            if (this.seagull.distanceToSqr(target) > 1.75D || !this.seagull.onGround()) {
                this.seagull.setSleeping(false);
                if (this.seagull.tickCount % 20 == 0 || this.seagull.getNavigation().isDone()) {
                    this.seagull.getNavigation().moveTo(target.x, target.y, target.z, 0.92D);
                }
                return;
            }
            this.seagull.getNavigation().stop();
            Vec3 movement = this.seagull.getDeltaMovement();
            this.seagull.setDeltaMovement(0.0D, movement.y, 0.0D);
            this.seagull.setSleeping(true);
        }

        private BlockPos findCoastalSleepSite(BlockPos origin) {
            BlockPos best = null;
            int bestScore = Integer.MIN_VALUE;
            for (int attempt = 0; attempt < 36; ++attempt) {
                int x = origin.getX() + this.seagull.getRandom().nextInt(25) - 12;
                int z = origin.getZ() + this.seagull.getRandom().nextInt(25) - 12;
                if (!this.seagull.level().hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int y = this.seagull.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - origin.getY()) > 10) {
                    continue;
                }
                BlockPos candidate = new BlockPos(x, y, z);
                int score = this.scoreSleepSite(candidate, origin);
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
            return bestScore >= 12 ? best : null;
        }

        private int scoreSleepSite(BlockPos candidate, BlockPos origin) {
            if (!BirdFlightTargeting.isSafeDryLanding(this.seagull, candidate)) {
                return Integer.MIN_VALUE;
            }
            BlockState below = this.seagull.level().getBlockState(candidate.below());
            int score = 0;
            if (below.is(Blocks.SAND) || below.is(Blocks.RED_SAND)) {
                score += 24;
            } else if (below.is(Blocks.GRAVEL)
                    || below.is(Blocks.STONE)
                    || below.is(Blocks.COBBLESTONE)
                    || below.is(Blocks.SANDSTONE)
                    || below.is(Blocks.RED_SANDSTONE)) {
                score += 18;
            } else if (below.is(BlockTags.LOGS)) {
                score += 14;
            } else if (below.is(BlockTags.DIRT) || below.is(BlockTags.ANIMALS_SPAWNABLE_ON)) {
                score += 6;
            }
            if (this.seagull.level().getBiome(candidate).is(BiomeTags.IS_OCEAN)
                    || this.seagull.level().getBiome(candidate).is(BiomeTags.IS_BEACH)
                    || this.seagull.level().getBiome(candidate).is(BiomeTags.IS_RIVER)) {
                score += 18;
            }
            if (this.seagull.level().canSeeSky(candidate)) {
                score += 5;
            }
            score -= (Math.abs(candidate.getX() - origin.getX()) + Math.abs(candidate.getZ() - origin.getZ())) / 4;
            return score;
        }
    }
}
