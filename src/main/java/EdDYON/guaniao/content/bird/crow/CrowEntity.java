package EdDYON.guaniao.content.bird.crow;

import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bath.BirdBathAttraction;
import EdDYON.guaniao.content.bath.BirdBathContentType;
import EdDYON.guaniao.content.bath.BirdBathFeedingAnimatable;
import EdDYON.guaniao.content.bath.BirdBathMountable;
import EdDYON.guaniao.content.bath.BirdBathUseGoal;
import EdDYON.guaniao.content.advancement.BirdAdvancements;
import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.PollutedFoodReactionUtil;
import EdDYON.guaniao.content.bird.flight.BirdFlightAware;
import EdDYON.guaniao.content.bird.flight.BirdFlightBoids;
import EdDYON.guaniao.content.bird.flight.BirdFlightController;
import EdDYON.guaniao.content.bird.flight.BirdFlightProfile;
import EdDYON.guaniao.content.bird.flight.BirdFlightTargeting;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import EdDYON.guaniao.content.nest.CrowNestBlockEntity;
import EdDYON.guaniao.content.nest.CrowNestTreasure;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
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

public class CrowEntity extends TamableAnimal implements GeoEntity, FlyingAnimal, ScalableBirdModel, BirdFlightAware, BirdBathMountable, BirdBathFeedingAnimatable {
    private static final EntityDataAccessor<Integer> BEHAVIOR_STATE = SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MODEL_SCALE = SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FLYING_ANIMATION_ACTIVE = SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> HELD_FOOD = SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> HELD_FOOD_POSE_SEED = SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.INT);
    private static final BirdFlightProfile FLIGHT_PROFILE = BirdFlightProfile.CROW;
    private static final int EAT_ANIMATION_TICKS = 82;
    private static final int EAT_BITE_DELAY_TICKS = 18;
    private static final int POLLUTED_FOOD_REACTION_TICKS = 34;
    private static final int POLLUTED_FOOD_SPIT_TICK = 18;
    private static final int POLLUTED_FOOD_FLEE_TICK = 8;
    private static final int MAX_AFFINITY = 3000;
    private static final int TAME_AFFINITY_THRESHOLD = 900;
    private static final int FED_AFFINITY = 260;
    private static final int ANGRY_FED_AFFINITY = 90;
    private static final int GROUP_FEED_AFFINITY = 70;
    private static final int ATTACK_AFFINITY_PENALTY = 520;
    private static final int ATTACK_ANGER_TICKS = 12000;
    private static final int PERCH_SCAN_HORIZONTAL = 13;
    private static final int PERCH_SCAN_VERTICAL = 16;
    private static final double PLAYER_LOOKING_DOT = 0.72D;
    private static final byte TAMING_FAILED_EVENT = 6;
    private static final byte TAMING_SUCCEEDED_EVENT = 7;
    private static final String NBT_HELD_ITEM = "CrowHeldItem";
    private static final String NBT_HELD_ITEM_POSE_SEED = "CrowHeldItemPoseSeed";
    private static final String NBT_CARRYING_HELD_ITEM = "CrowCarryingHeldItem";
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation IDLE_DIFF_1_ANIMATION = RawAnimation.begin().thenPlay("animation.idle_diff_1").thenLoop("animation.idle");
    private static final RawAnimation IDLE_DIFF_2_ANIMATION = RawAnimation.begin().thenPlay("animation.idle_diff_2").thenLoop("animation.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation FLY_ANIMATION = RawAnimation.begin().thenLoop("animation.fly");
    private static final RawAnimation GLIDE_ANIMATION = RawAnimation.begin().thenLoop("animation.fly_loop");
    private static final RawAnimation EAT_ANIMATION = RawAnimation.begin().thenPlay("animation.eat").thenLoop("animation.idle");
    private static final RawAnimation SLEEP_ANIMATION = RawAnimation.begin().thenLoop("animation.sleep");
    private static final RawAnimation SLEEP_2_ANIMATION = RawAnimation.begin().thenLoop("animation.sleep2");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private CrowBehaviorState behaviorState = CrowBehaviorState.IDLE;
    private GuidePreviewAnimation guidePreviewAnimation = GuidePreviewAnimation.NONE;
    private RawAnimation currentIdleAnimation = IDLE_ANIMATION;
    private int behaviorStateLockTicks;
    private int eatingTicks;
    private int foodCooldown;
    private int calmTicks;
    private int affinity;
    private int angerMemoryTicks;
    private int flightTicks;
    private int timeFlying;
    private int flightCooldown;
    private int hoverRetargetTicks;
    private int airborneGraceTicks;
    private int shinyCooldown;
    private int groupAlertCooldown;
    private int pollutedFoodReactionTicks;
    private boolean escapeFlightActive;
    private boolean landingFlight;
    private boolean carryingHeldItem;
    private Vec3 flightTarget;
    private Vec3 frightSource;
    private UUID rememberedPlayerUUID;

    public CrowEntity(EntityType<? extends CrowEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 12, true);
        this.setPathfindingMalus(BlockPathTypes.LEAVES, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 16.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, CrowDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, CrowDefinition.WALK_SPEED)
                .add(Attributes.FLYING_SPEED, CrowDefinition.FLYING_SPEED)
                .add(Attributes.FOLLOW_RANGE, CrowDefinition.FOLLOW_RANGE);
    }

    public static boolean canSpawn(EntityType<CrowEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean validGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(BlockTags.LEAVES)
                || below.is(BlockTags.LOGS)
                || below.is(Blocks.FARMLAND)
                || below.is(Blocks.HAY_BLOCK)
                || below.getBlock() instanceof FenceBlock
                || below.getBlock() instanceof FenceGateBlock;
        return validGround && level.getRawBrightness(pos, 0) > 7;
    }

    @Override
    public CrowEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
        CrowEntity child = GuaniaoEntityTypes.CROW.get().create(level);
        if (child != null) {
            float mateScale = mate instanceof CrowEntity other ? other.getIndividualModelScale() : this.getIndividualModelScale();
            child.setIndividualModelScale(BirdModelScale.inheritIndividualScale(child.getRandom(), this.getIndividualModelScale(), mateScale, child.modelScaleProfile()));
        }
        return child;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CrowFleeGoal(this));
        this.goalSelector.addGoal(2, new CrowEatDroppedFoodGoal(this));
        this.goalSelector.addGoal(2, new CrowDepositTreasureGoal(this));
        this.goalSelector.addGoal(3, new BirdBathUseGoal(this, 1.02D, 12.0D, 42,
                BirdBathAttraction::isAttractiveToCrow,
                this::canStartForagingGoal,
                bath -> this.setBehaviorState(CrowBehaviorState.FORAGING),
                this::consumeBirdBathServing,
                (bath, consumed) -> {
                    if (!this.isEating() && this.getBehaviorState() == CrowBehaviorState.FORAGING) {
                        this.setBehaviorState(CrowBehaviorState.IDLE);
                    }
                }));
        this.goalSelector.addGoal(4, new CrowSleepGoal(this));
        this.goalSelector.addGoal(5, new CrowInvestigateShinyGoal(this));
        this.goalSelector.addGoal(6, new CrowFollowOwnerGoal(this, 1.0D, 3.2F, 11.0F));
        this.goalSelector.addGoal(7, new CrowWatchPlayerGoal(this));
        this.goalSelector.addGoal(8, new CrowAmbientFlightGoal(this));
        this.goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 0.92D, 0.001F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
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
        this.entityData.define(BEHAVIOR_STATE, CrowBehaviorState.IDLE.ordinal());
        this.entityData.define(MODEL_SCALE, BirdModelScale.DEFAULT_INDIVIDUAL_SCALE);
        this.entityData.define(FLYING_ANIMATION_ACTIVE, false);
        this.entityData.define(HELD_FOOD, ItemStack.EMPTY);
        this.entityData.define(HELD_FOOD_POSE_SEED, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (BEHAVIOR_STATE.equals(key)) {
            this.behaviorState = CrowEntity.decodeBehaviorState(this.entityData.get(BEHAVIOR_STATE));
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
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putInt("CrowCalmTicks", this.calmTicks);
        compoundTag.putInt("CrowAffinity", this.affinity);
        compoundTag.putInt("CrowAngerMemoryTicks", this.angerMemoryTicks);
        if (this.rememberedPlayerUUID != null) {
            compoundTag.putUUID("CrowRememberedPlayer", this.rememberedPlayerUUID);
        }
        ItemStack heldItem = this.getHeldFoodForRendering();
        if (!heldItem.isEmpty()) {
            compoundTag.put(NBT_HELD_ITEM, heldItem.save(new CompoundTag()));
            compoundTag.putInt(NBT_HELD_ITEM_POSE_SEED, this.getHeldFoodPoseSeed());
            compoundTag.putBoolean(NBT_CARRYING_HELD_ITEM, this.carryingHeldItem);
        }
        BirdModelScale.save(compoundTag, this.getIndividualModelScale(), this.modelScaleProfile());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.calmTicks = compoundTag.getInt("CrowCalmTicks");
        this.affinity = Mth.clamp(compoundTag.getInt("CrowAffinity"), 0, MAX_AFFINITY);
        this.angerMemoryTicks = compoundTag.getInt("CrowAngerMemoryTicks");
        if (compoundTag.hasUUID("CrowRememberedPlayer")) {
            this.rememberedPlayerUUID = compoundTag.getUUID("CrowRememberedPlayer");
        }
        if (compoundTag.contains(BirdModelScale.NBT_KEY, 5)) {
            this.setIndividualModelScale(BirdModelScale.load(compoundTag, this.modelScaleProfile()));
        } else {
            this.randomizeModelScale();
        }
        if (compoundTag.contains(NBT_HELD_ITEM, 10)) {
            this.setHeldFoodForRendering(
                    ItemStack.of(compoundTag.getCompound(NBT_HELD_ITEM)),
                    compoundTag.getBoolean(NBT_CARRYING_HELD_ITEM),
                    compoundTag.getInt(NBT_HELD_ITEM_POSE_SEED));
        } else {
            this.clearHeldFoodForRendering();
        }
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.isTame() || this.hasHeldItem();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        this.tickCounters();
        this.tickPollutedFoodReaction();
        this.tickEating();
        this.tickWaterEscape();
        this.tickFlight();
        this.tickBehaviorFallback();
        this.tickGroundMovementFacing();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && isCrowFood(stack)) {
            if (!this.level().isClientSide) {
                boolean wasTame = this.isTame();
                boolean feedingRememberedAggressor = this.isRememberingPlayer(player);
                ItemStack eaten = stack.copy();
                eaten.setCount(1);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                if (this.isCarryingHeldItem()) {
                    this.dropHeldItem();
                }
                this.startEatingFood(eaten, true);
                this.calmTicks = Math.max(this.calmTicks, 2400 + this.getRandom().nextInt(3600));
                if (feedingRememberedAggressor) {
                    this.angerMemoryTicks = Math.max(0, this.angerMemoryTicks - 2200);
                    if (this.angerMemoryTicks <= 0) {
                        this.rememberedPlayerUUID = null;
                    }
                }
                this.addAffinity(feedingRememberedAggressor ? ANGRY_FED_AFFINITY : FED_AFFINITY);
                this.shareFriendlyFeeding(player, GROUP_FEED_AFFINITY);
                this.updateTrustedOwner(player);
                if (!wasTame && this.isTame()) {
                    BirdAdvancements.awardTamedBird(player, this);
                    this.level().broadcastEntityEvent(this, TAMING_SUCCEEDED_EVENT);
                } else if (!wasTame) {
                    this.level().broadcastEntityEvent(this, TAMING_FAILED_EVENT);
                }
                this.heal(1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isCrowFood(stack);
    }

    @Override
    public boolean isOrderedToSit() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            if (!this.dropHeldItem()) {
                this.clearEating();
            }
            Entity attacker = source.getEntity();
            Vec3 sourcePos = attacker == null ? this.position() : attacker.position();
            this.rememberAggressor(attacker);
            this.frightenFrom(sourcePos, attacker instanceof Player ? 90 : 60);
            this.alertNearbyCrows(attacker, sourcePos);
        }
        return hurt;
    }

    @Override
    public boolean isFlying() {
        return this.isBirdFlightActive() || (!this.onGround() && this.getDeltaMovement().y > -0.65D);
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
                || (this.isNoGravity() && !this.onGround());
    }

    @Override
    public boolean isBirdLanding() {
        return this.landingFlight;
    }

    @Override
    public boolean isBirdEscaping() {
        return this.escapeFlightActive;
    }

    @Override
    public boolean startBirdBathMountFlight(Vec3 standPosition) {
        if (standPosition == null || this.isFlightInProgress()) {
            return false;
        }
        Vec3 horizontal = standPosition.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() > 1.0E-4D) {
            horizontal = horizontal.normalize().scale(0.30D);
        }
        this.getNavigation().stop();
        this.setNoGravity(false);
        this.setOnGround(false);
        this.setDeltaMovement(horizontal.x, 0.68D, horizontal.z);
        this.airborneGraceTicks = Math.max(this.airborneGraceTicks, 32);
        this.setFlyingAnimationActive(true);
        this.setBehaviorStateFor(CrowBehaviorState.FLYING, 34);
        this.faceFlightDirection(this.getDeltaMovement());
        this.fallDistance = 0.0F;
        this.hasImpulse = true;
        return true;
    }

    @Override
    public void startBirdBathFeedingAnimation(BirdBathContentType contentType, int ticks) {
        this.getNavigation().stop();
        if (contentType.isFood()) {
            this.eatingTicks = Math.max(this.eatingTicks, Math.max(EAT_ANIMATION_TICKS, ticks));
            this.setBehaviorStateFor(CrowBehaviorState.EATING, this.eatingTicks);
            return;
        }
        this.setBehaviorStateFor(CrowBehaviorState.WATCHING, Math.max(24, ticks / 2));
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.CROW;
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

    public ResourceLocation getTextureResource() {
        return CrowDefinition.TEXTURE;
    }

    public ItemStack getHeldFoodForRendering() {
        return this.entityData == null ? ItemStack.EMPTY : this.entityData.get(HELD_FOOD);
    }

    public int getHeldFoodPoseSeed() {
        return this.entityData == null ? 0 : this.entityData.get(HELD_FOOD_POSE_SEED);
    }

    private boolean hasHeldItem() {
        return !this.getHeldFoodForRendering().isEmpty();
    }

    private boolean isCarryingHeldItem() {
        return this.carryingHeldItem && this.hasHeldItem();
    }

    public void setGuidePreviewAnimation(GuidePreviewAnimation guidePreviewAnimation) {
        this.guidePreviewAnimation = guidePreviewAnimation == null ? GuidePreviewAnimation.NONE : guidePreviewAnimation;
    }

    public CrowBehaviorState getBehaviorState() {
        if (this.entityData != null) {
            return CrowEntity.decodeBehaviorState(this.entityData.get(BEHAVIOR_STATE));
        }
        return this.behaviorState;
    }

    void setBehaviorState(CrowBehaviorState state) {
        if (state == null) {
            state = CrowBehaviorState.IDLE;
        }
        this.behaviorState = state;
        if (this.entityData != null) {
            this.entityData.set(BEHAVIOR_STATE, state.ordinal());
        }
    }

    void setBehaviorStateFor(CrowBehaviorState state, int ticks) {
        this.setBehaviorState(state);
        this.behaviorStateLockTicks = Math.max(this.behaviorStateLockTicks, ticks);
    }

    boolean canStartForagingGoal() {
        return this.foodCooldown <= 0
                && this.pollutedFoodReactionTicks <= 0
                && !this.hasHeldItem()
                && !this.isEating()
                && !this.isFlightInProgress()
                && !this.getBehaviorState().isEscape();
    }

    boolean isEating() {
        return this.eatingTicks > 0 || this.getBehaviorState() == CrowBehaviorState.EATING;
    }

    boolean isFlightInProgress() {
        return this.flightTicks > 0 || this.landingFlight;
    }

    boolean isActiveTime() {
        return BirdActivitySchedule.DIURNAL.isActiveTime(this.level().getDayTime());
    }

    void startEatingFood(ItemStack foodStack, boolean trustedFood) {
        this.beginEatingAnimation(trustedFood);
        this.setHeldFoodForRendering(foodStack);
        this.playEatingSound();
    }

    private void beginEatingAnimation(boolean trustedFood) {
        this.getNavigation().stop();
        if (this.eatingTicks <= 0) {
            this.eatingTicks = EAT_ANIMATION_TICKS;
        }
        this.foodCooldown = trustedFood ? 90 + this.getRandom().nextInt(70) : 120 + this.getRandom().nextInt(80);
        this.setBehaviorStateFor(CrowBehaviorState.EATING, this.eatingTicks);
    }

    private void playEatingSound() {
        this.playSound(SoundEvents.GENERIC_EAT, 0.42F, 0.78F + this.getRandom().nextFloat() * 0.16F);
    }

    private void setHeldFoodForRendering(ItemStack foodStack) {
        this.setHeldFoodForRendering(foodStack, false);
    }

    private void setHeldFoodForRendering(ItemStack foodStack, boolean carried) {
        this.setHeldFoodForRendering(foodStack, carried, 0);
    }

    private void setHeldFoodForRendering(ItemStack foodStack, boolean carried, int poseSeed) {
        if (this.entityData == null) {
            return;
        }
        if (foodStack == null || foodStack.isEmpty()) {
            this.entityData.set(HELD_FOOD, ItemStack.EMPTY);
            this.entityData.set(HELD_FOOD_POSE_SEED, 0);
            this.carryingHeldItem = false;
            return;
        }
        ItemStack copy = foodStack.copy();
        copy.setCount(1);
        this.entityData.set(HELD_FOOD, copy);
        this.entityData.set(HELD_FOOD_POSE_SEED, poseSeed == 0 ? this.nextHeldFoodPoseSeed() : poseSeed);
        this.carryingHeldItem = carried;
    }

    private void clearHeldFoodForRendering() {
        if (this.entityData != null) {
            if (!this.entityData.get(HELD_FOOD).isEmpty()) {
                this.entityData.set(HELD_FOOD, ItemStack.EMPTY);
            }
            if (this.entityData.get(HELD_FOOD_POSE_SEED) != 0) {
                this.entityData.set(HELD_FOOD_POSE_SEED, 0);
            }
        }
        this.carryingHeldItem = false;
    }

    private int nextHeldFoodPoseSeed() {
        int seed = this.getRandom().nextInt();
        return seed == 0 ? 1 : seed;
    }

    boolean pickUpItemEntity(ItemEntity itemEntity) {
        if (itemEntity == null || !itemEntity.isAlive() || this.hasHeldItem()) {
            return false;
        }
        ItemStack stack = itemEntity.getItem();
        if (!isCrowDroppedFoodCandidate(stack) && !isLowValueShiny(stack)) {
            return false;
        }
        Vec3 sourcePos = itemEntity.position();
        ItemStack heldItem = stack.copy();
        heldItem.setCount(1);
        stack.shrink(1);
        if (stack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(stack);
        }
        this.setHeldFoodForRendering(heldItem, true);
        this.foodCooldown = Math.max(this.foodCooldown, 220 + this.getRandom().nextInt(180));
        this.shinyCooldown = Math.max(this.shinyCooldown, 260 + this.getRandom().nextInt(220));
        if (BirdFoodSafety.isPollutedFood(heldItem)) {
            this.beginPollutedFoodReaction();
            return true;
        }
        if (isCrowFood(heldItem)) {
            this.calmTicks = Math.max(this.calmTicks, 1200 + this.getRandom().nextInt(1800));
            this.playEatingSound();
        } else {
            this.playSound(SoundEvents.ITEM_PICKUP, 0.36F, 0.88F + this.getRandom().nextFloat() * 0.18F);
        }
        this.notifyNearbyCrowsOfPickup(sourcePos, isCrowFood(heldItem));
        if (!this.hasNearbyAvailableNest()) {
            this.startCarryAwayFlight(sourcePos);
        }
        return true;
    }

    private boolean hasNearbyAvailableNest() {
        ItemStack heldItem = this.getHeldFoodForRendering();
        return !heldItem.isEmpty()
                && CrowNestTreasure.isShiny(heldItem)
                && CrowNestBlockEntity.findNearest(this.level(), this.position(), 18, 8, heldItem).isPresent();
    }

    private boolean dropHeldItem() {
        ItemStack heldItem = this.getHeldFoodForRendering();
        if (heldItem.isEmpty()) {
            return false;
        }
        ItemStack dropStack = heldItem.copy();
        this.eatingTicks = 0;
        this.clearHeldFoodForRendering();
        this.pollutedFoodReactionTicks = 0;
        if (this.getBehaviorState() == CrowBehaviorState.EATING) {
            this.behaviorStateLockTicks = 0;
            this.setBehaviorState(CrowBehaviorState.ALERT);
        }
        ItemEntity droppedItem = this.spawnAtLocation(dropStack, 0.25F);
        if (droppedItem != null) {
            Vec3 toss = this.randomHorizontalDirection().scale(0.08D).add(0.0D, 0.08D, 0.0D);
            droppedItem.setDeltaMovement(this.getDeltaMovement().scale(0.15D).add(toss));
        }
        return true;
    }

    private void beginPollutedFoodReaction() {
        this.eatingTicks = 0;
        this.pollutedFoodReactionTicks = POLLUTED_FOOD_REACTION_TICKS;
        this.getNavigation().stop();
        this.setBehaviorStateFor(CrowBehaviorState.ALERT, POLLUTED_FOOD_REACTION_TICKS);
    }

    private void tickPollutedFoodReaction() {
        if (this.pollutedFoodReactionTicks <= 0) {
            return;
        }

        --this.pollutedFoodReactionTicks;
        Player witness = PollutedFoodReactionUtil.nearestWitness(this, 16.0D);
        PollutedFoodReactionUtil.lookAtWitness(this, witness);
        this.getNavigation().stop();

        if (this.pollutedFoodReactionTicks == POLLUTED_FOOD_SPIT_TICK && this.hasHeldItem()) {
            this.playSound(this.getAmbientSound(), 0.9F, 1.12F + this.getRandom().nextFloat() * 0.18F);
            ItemStack spatFood = this.getHeldFoodForRendering().copy();
            this.eatingTicks = 0;
            this.clearHeldFoodForRendering();
            PollutedFoodReactionUtil.spit(this, spatFood);
        }

        if (this.pollutedFoodReactionTicks == POLLUTED_FOOD_FLEE_TICK) {
            Vec3 source = witness == null ? this.position().subtract(this.randomHorizontalDirection()) : witness.position();
            this.flightCooldown = 0;
            this.startEscapeFlight(source);
        }
    }

    private void startCarryAwayFlight(Vec3 sourcePos) {
        Vec3 away = sourcePos == null ? this.randomHorizontalDirection() : this.position().subtract(sourcePos).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() <= 1.0E-4D) {
            away = this.randomHorizontalDirection();
        }
        Vec3 target = this.findPreferredPerchTarget(BlockPos.containing(this.position().add(away.normalize().scale(9.0D))), 8, 14);
        if (target == null) {
            target = BirdFlightTargeting.findAirTarget(this, FLIGHT_PROFILE, away, false);
        }
        if (target == null) {
            target = this.position()
                    .add(away.normalize().scale(8.0D + this.getRandom().nextDouble() * 6.0D))
                    .add(0.0D, 3.0D + this.getRandom().nextDouble() * 2.0D, 0.0D);
        }
        this.flightCooldown = 0;
        this.startShortFlight(target, false);
    }

    private void cancelEatingAnimation() {
        this.eatingTicks = 0;
        this.clearHeldFoodForRendering();
        if (this.getBehaviorState() == CrowBehaviorState.EATING) {
            this.behaviorStateLockTicks = 0;
            this.setBehaviorState(CrowBehaviorState.IDLE);
        }
    }

    void frightenFrom(Vec3 sourcePos, int ticks) {
        this.frightSource = sourcePos;
        this.setBehaviorStateFor(CrowBehaviorState.FLEEING, Math.min(90, ticks));
        if (this.flightCooldown <= 0 && !this.isFlightInProgress()) {
            this.startEscapeFlight(sourcePos);
        }
    }

    void receiveGroupAlert(Entity attacker, Vec3 sourcePos, int delayRange) {
        if (this.groupAlertCooldown > 0) {
            return;
        }
        this.groupAlertCooldown = 80 + this.getRandom().nextInt(80);
        this.rememberSharedAggressor(attacker);
        this.setBehaviorStateFor(CrowBehaviorState.ALERT, 35 + this.getRandom().nextInt(35));
        if (this.getRandom().nextInt(Math.max(1, delayRange)) == 0 && this.flightCooldown <= 0) {
            this.startEscapeFlight(sourcePos);
        }
    }

    private void consumeBirdBathServing(EdDYON.guaniao.content.bath.BirdBathBlockEntity bath, BirdBathContentType contentType) {
        if (contentType == BirdBathContentType.FISH) {
            this.startEatingFood(new ItemStack(Items.COD), true);
            return;
        }
        if (contentType == BirdBathContentType.MEAT) {
            this.startEatingFood(new ItemStack(Items.CHICKEN), true);
            return;
        }
        if (contentType == BirdBathContentType.BREAD) {
            this.startEatingFood(new ItemStack(Items.BREAD), true);
            return;
        }
        this.eatingTicks = 22 + this.getRandom().nextInt(12);
        this.foodCooldown = 70 + this.getRandom().nextInt(45);
        this.setBehaviorStateFor(CrowBehaviorState.EATING, this.eatingTicks);
        this.playSound(SoundEvents.GENERIC_DRINK, 0.32F, 0.86F + this.getRandom().nextFloat() * 0.16F);
    }

    public static boolean isCrowFood(ItemStack stack) {
        return BirdFoodSafety.isCleanFoodCandidate(stack) && matchesCrowDiet(stack);
    }

    private static boolean isCrowDroppedFoodCandidate(ItemStack stack) {
        return matchesCrowDiet(stack);
    }

    private static boolean matchesCrowDiet(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(Items.ROTTEN_FLESH)
                || stack.is(Items.SPIDER_EYE)
                || stack.is(Items.BEEF)
                || stack.is(Items.PORKCHOP)
                || stack.is(Items.CHICKEN)
                || stack.is(Items.MUTTON)
                || stack.is(Items.RABBIT)
                || stack.is(Items.COD)
                || stack.is(Items.SALMON)
                || stack.is(Items.TROPICAL_FISH)
                || stack.is(Items.PUFFERFISH)
                || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES)
                || stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.MELON_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS)
                || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.TORCHFLOWER_SEEDS)
                || stack.is(Items.PITCHER_POD)
                || stack.is(Items.WHEAT)
                || stack.is(Items.BREAD)
                || stack.is(Items.COOKED_BEEF)
                || stack.is(Items.COOKED_PORKCHOP)
                || stack.is(Items.COOKED_CHICKEN)
                || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.COOKED_RABBIT)
                || stack.is(Items.COOKED_COD)
                || stack.is(Items.COOKED_SALMON)
                || stack.is(Items.APPLE)
                || stack.is(Items.BEETROOT));
    }

    static boolean isLowValueShiny(ItemStack stack) {
        return !stack.isEmpty()
                && !stack.hasCustomHoverName()
                && !stack.hasTag()
                && (stack.is(Items.GOLD_NUGGET)
                || stack.is(Items.IRON_NUGGET)
                || stack.is(Items.COPPER_INGOT)
                || stack.is(Items.AMETHYST_SHARD)
                || stack.is(Items.REDSTONE)
                || stack.is(Items.LAPIS_LAZULI)
                || stack.is(Items.GLOWSTONE_DUST)
                || stack.is(Items.GLASS_BOTTLE)
                || stack.is(Items.CLOCK));
    }

    private static boolean isThreateningHeldItem(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(Items.WOODEN_SWORD)
                || stack.is(Items.STONE_SWORD)
                || stack.is(Items.IRON_SWORD)
                || stack.is(Items.GOLDEN_SWORD)
                || stack.is(Items.DIAMOND_SWORD)
                || stack.is(Items.NETHERITE_SWORD)
                || stack.is(Items.WOODEN_AXE)
                || stack.is(Items.STONE_AXE)
                || stack.is(Items.IRON_AXE)
                || stack.is(Items.GOLDEN_AXE)
                || stack.is(Items.DIAMOND_AXE)
                || stack.is(Items.NETHERITE_AXE)
                || stack.is(Items.BOW)
                || stack.is(Items.CROSSBOW)
                || stack.is(Items.TRIDENT));
    }

    private boolean isHoldingThreateningItem(Player player) {
        return player != null
                && (isThreateningHeldItem(player.getMainHandItem())
                || isThreateningHeldItem(player.getOffhandItem()));
    }

    private boolean isHoldingCrowFood(Player player) {
        return player != null
                && (isCrowFood(player.getMainHandItem())
                || isCrowFood(player.getOffhandItem()));
    }

    private boolean isHoldingLowValueShiny(Player player) {
        return player != null
                && (isLowValueShiny(player.getMainHandItem())
                || isLowValueShiny(player.getOffhandItem()));
    }

    private InteractionHand findStealableHand(Player player, boolean preferShiny) {
        if (player == null) {
            return null;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack offhand = player.getOffhandItem();
        if (preferShiny) {
            if (isLowValueShiny(main)) {
                return InteractionHand.MAIN_HAND;
            }
            if (isLowValueShiny(offhand)) {
                return InteractionHand.OFF_HAND;
            }
        }
        if (isCrowFood(main)) {
            return InteractionHand.MAIN_HAND;
        }
        if (isCrowFood(offhand)) {
            return InteractionHand.OFF_HAND;
        }
        if (isLowValueShiny(main)) {
            return InteractionHand.MAIN_HAND;
        }
        if (isLowValueShiny(offhand)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private boolean tryStealHeldItem(Player player, boolean preferShiny) {
        if (player == null || this.hasHeldItem() || this.isOwnedBy(player) || this.isHoldingThreateningItem(player)) {
            return false;
        }
        InteractionHand hand = this.findStealableHand(player, preferShiny);
        if (hand == null) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || (!isCrowFood(stack) && !isLowValueShiny(stack))) {
            return false;
        }
        ItemStack stolen = stack.copy();
        stolen.setCount(1);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.setHeldFoodForRendering(stolen, true);
        this.foodCooldown = Math.max(this.foodCooldown, 240 + this.getRandom().nextInt(180));
        this.shinyCooldown = Math.max(this.shinyCooldown, 260 + this.getRandom().nextInt(240));
        this.playSound(SoundEvents.ITEM_PICKUP, 0.42F, 0.92F + this.getRandom().nextFloat() * 0.18F);
        this.notifyNearbyCrowsOfPickup(player.position(), isCrowFood(stolen));
        this.startCarryAwayFlight(player.position());
        return true;
    }

    private void rememberAggressor(Entity attacker) {
        this.rememberAggressor(attacker, ATTACK_AFFINITY_PENALTY, ATTACK_ANGER_TICKS);
    }

    private void rememberSharedAggressor(Entity attacker) {
        this.rememberAggressor(attacker, 160, 6000);
    }

    private void rememberAggressor(Entity attacker, int affinityPenalty, int baseAngerTicks) {
        if (attacker instanceof Player player) {
            this.rememberedPlayerUUID = player.getUUID();
            this.angerMemoryTicks = Math.max(this.angerMemoryTicks, baseAngerTicks + this.getRandom().nextInt(4001));
            this.addAffinity(-affinityPenalty);
        }
    }

    private Player rememberedPlayer() {
        if (this.rememberedPlayerUUID == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getPlayerByUUID(this.rememberedPlayerUUID);
    }

    private void alertNearbyCrows(Entity attacker, Vec3 sourcePos) {
        if (this.groupAlertCooldown > 0) {
            return;
        }
        this.groupAlertCooldown = 120;
        for (CrowEntity crow : this.level().getEntitiesOfClass(CrowEntity.class, this.getBoundingBox().inflate(16.0D))) {
            if (crow != this) {
                crow.receiveGroupAlert(attacker, sourcePos, 3 + crow.getRandom().nextInt(4));
            }
        }
    }

    private boolean isRememberingPlayer(Player player) {
        return player != null
                && this.rememberedPlayerUUID != null
                && this.angerMemoryTicks > 0
                && this.rememberedPlayerUUID.equals(player.getUUID());
    }

    private boolean isOwnedBy(Player player) {
        return player != null
                && this.isTame()
                && this.getOwnerUUID() != null
                && this.getOwnerUUID().equals(player.getUUID());
    }

    private boolean isTrustedPlayer(Player player) {
        if (player == null || this.isRememberingPlayer(player)) {
            return false;
        }
        return this.isOwnedBy(player) || (!this.isTame() && this.calmTicks > 0);
    }

    private void addAffinity(int amount) {
        this.affinity = Mth.clamp(this.affinity + amount, 0, MAX_AFFINITY);
    }

    private void updateTrustedOwner(Player player) {
        if (player == null) {
            return;
        }
        if (!this.isTame() && this.affinity >= TAME_AFFINITY_THRESHOLD && !this.isRememberingPlayer(player)) {
            this.tame(player);
        } else if (this.isTame() && this.getOwnerUUID() == null) {
            this.setOwnerUUID(player.getUUID());
        }
    }

    private void shareFriendlyFeeding(Player player, int amount) {
        for (CrowEntity crow : this.level().getEntitiesOfClass(CrowEntity.class, this.getBoundingBox().inflate(12.0D))) {
            if (crow != this) {
                crow.receiveFriendlyFeeding(player, amount);
            }
        }
    }

    private void receiveFriendlyFeeding(Player player, int amount) {
        if (player != null && this.isRememberingPlayer(player)) {
            this.angerMemoryTicks = Math.max(0, this.angerMemoryTicks - 520);
            if (this.angerMemoryTicks <= 0) {
                this.rememberedPlayerUUID = null;
            }
        } else {
            this.addAffinity(amount);
        }
        this.calmTicks = Math.max(this.calmTicks, 700 + this.getRandom().nextInt(700));
        if (!this.isEating() && !this.isFlightInProgress() && this.getBehaviorState() != CrowBehaviorState.SLEEPING) {
            this.setBehaviorStateFor(CrowBehaviorState.WATCHING, 28 + this.getRandom().nextInt(28));
            if (player != null) {
                this.getLookControl().setLookAt(player, 35.0F, this.getMaxHeadXRot());
                if (this.onGround() && this.getNavigation().isDone() && this.distanceToSqr(player) > 6.0D * 6.0D && this.getRandom().nextInt(3) == 0) {
                    this.getNavigation().moveTo(player, 0.86D);
                }
            }
        }
    }

    private void signalNearbyCrowsToForage(ItemEntity target, boolean shiny) {
        if (target == null) {
            return;
        }
        for (CrowEntity crow : this.level().getEntitiesOfClass(CrowEntity.class, this.getBoundingBox().inflate(10.0D, 4.0D, 10.0D))) {
            if (crow != this) {
                crow.receiveForagingSignal(target, shiny);
            }
        }
    }

    private void receiveForagingSignal(ItemEntity target, boolean shiny) {
        if (target == null
                || !target.isAlive()
                || this.hasHeldItem()
                || this.isEating()
                || this.isFlightInProgress()
                || this.getBehaviorState() == CrowBehaviorState.SLEEPING) {
            return;
        }
        if (shiny) {
            this.shinyCooldown = Math.min(this.shinyCooldown, 20 + this.getRandom().nextInt(30));
            this.setBehaviorStateFor(CrowBehaviorState.WATCHING, 30 + this.getRandom().nextInt(30));
        } else {
            this.foodCooldown = Math.min(this.foodCooldown, 12 + this.getRandom().nextInt(24));
            this.setBehaviorStateFor(CrowBehaviorState.FORAGING, 26 + this.getRandom().nextInt(28));
        }
        this.getLookControl().setLookAt(target, 35.0F, this.getMaxHeadXRot());
        if (this.onGround() && this.getRandom().nextInt(2) == 0) {
            this.getNavigation().moveTo(target, shiny ? 0.84D : 0.96D);
        }
    }

    private void notifyNearbyCrowsOfPickup(Vec3 sourcePos, boolean food) {
        for (CrowEntity crow : this.level().getEntitiesOfClass(CrowEntity.class, this.getBoundingBox().inflate(9.0D, 4.0D, 9.0D))) {
            if (crow != this) {
                crow.receivePickupSignal(this, sourcePos, food);
            }
        }
    }

    private void receivePickupSignal(CrowEntity carrier, Vec3 sourcePos, boolean food) {
        if (carrier == null || this.isEating() || this.isFlightInProgress() || this.getBehaviorState() == CrowBehaviorState.SLEEPING) {
            return;
        }
        if (!this.hasHeldItem()) {
            this.foodCooldown = Math.min(this.foodCooldown, food ? 28 : 70);
            this.shinyCooldown = Math.min(this.shinyCooldown, food ? 90 : 36);
        }
        this.getLookControl().setLookAt(carrier, 40.0F, this.getMaxHeadXRot());
        this.setBehaviorStateFor(food ? CrowBehaviorState.FORAGING : CrowBehaviorState.WATCHING, 24 + this.getRandom().nextInt(36));
        if (this.onGround() && this.getNavigation().isDone() && this.getRandom().nextInt(3) == 0) {
            Vec3 target = sourcePos == null ? carrier.position() : sourcePos;
            this.getNavigation().moveTo(target.x, target.y, target.z, food ? 0.98D : 0.9D);
        }
    }

    private Player nearestWatchingPlayer(Vec3 watchedPos, double range) {
        Player best = null;
        double bestDistance = range * range;
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(range + 2.0D), player -> player.isAlive() && !player.isSpectator())) {
            double distance = player.distanceToSqr(watchedPos.x, watchedPos.y, watchedPos.z);
            if (distance < bestDistance && this.isPlayerLookingAt(player, watchedPos, PLAYER_LOOKING_DOT)) {
                best = player;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean isPlayerLookingAt(Player player, Vec3 watchedPos, double dotThreshold) {
        if (player == null) {
            return false;
        }
        Vec3 toTarget = watchedPos.subtract(player.getEyePosition());
        if (toTarget.lengthSqr() <= 1.0E-4D) {
            return true;
        }
        return player.getLookAngle().normalize().dot(toTarget.normalize()) >= dotThreshold;
    }

    private boolean isPlayerLookingAtMe(Player player) {
        return this.isPlayerLookingAt(player, this.position().add(0.0D, 0.35D, 0.0D), PLAYER_LOOKING_DOT);
    }

    private void sidestepAround(Vec3 focusPos, double distance, double speed) {
        Vec3 toFocus = focusPos.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (toFocus.lengthSqr() <= 1.0E-4D) {
            toFocus = this.randomHorizontalDirection();
        } else {
            toFocus = toFocus.normalize();
        }
        double sideSign = this.getRandom().nextBoolean() ? 1.0D : -1.0D;
        Vec3 sideways = new Vec3(-toFocus.z, 0.0D, toFocus.x).scale(distance * sideSign);
        Vec3 back = toFocus.scale(-0.45D);
        Vec3 move = this.position().add(sideways).add(back);
        this.getNavigation().moveTo(move.x, move.y, move.z, speed);
    }

    private void tickCounters() {
        if (this.behaviorStateLockTicks > 0) {
            --this.behaviorStateLockTicks;
        }
        if (this.eatingTicks > 0) {
            --this.eatingTicks;
        }
        if (this.foodCooldown > 0) {
            --this.foodCooldown;
        }
        if (this.calmTicks > 0) {
            --this.calmTicks;
        }
        if (this.angerMemoryTicks > 0) {
            --this.angerMemoryTicks;
            if (this.angerMemoryTicks <= 0) {
                this.rememberedPlayerUUID = null;
            }
        }
        if (this.flightCooldown > 0) {
            --this.flightCooldown;
        }
        if (this.airborneGraceTicks > 0) {
            --this.airborneGraceTicks;
        }
        if (this.shinyCooldown > 0) {
            --this.shinyCooldown;
        }
        if (this.groupAlertCooldown > 0) {
            --this.groupAlertCooldown;
        }
        if (this.airborneGraceTicks <= 0 && !this.isFlightInProgress() && this.onGround()) {
            this.setFlyingAnimationActive(false);
        }
    }

    private void tickEating() {
        if (this.eatingTicks > 0 && this.onGround() && !this.isFlightInProgress()) {
            this.getNavigation().stop();
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, movement.y, 0.0D);
        }
        if (this.eatingTicks <= 0) {
            if (!this.isCarryingHeldItem()) {
                this.clearHeldFoodForRendering();
            }
            if (this.getBehaviorState() == CrowBehaviorState.EATING) {
                this.setBehaviorState(CrowBehaviorState.IDLE);
            }
        }
    }

    private void clearEating() {
        this.eatingTicks = 0;
        if (!this.isCarryingHeldItem()) {
            this.clearHeldFoodForRendering();
        }
        if (this.getBehaviorState() == CrowBehaviorState.EATING) {
            this.setBehaviorState(CrowBehaviorState.ALERT);
        }
    }

    private void tickWaterEscape() {
        if (this.isInWaterOrBubble() && this.flightCooldown <= 0 && !this.isFlightInProgress()) {
            this.startShortFlight(this.position().add(this.randomHorizontalDirection().scale(5.0D)).add(0.0D, 3.0D, 0.0D), true);
        }
    }

    private void tickFlight() {
        if (this.flightTicks <= 0 && !this.landingFlight) {
            this.timeFlying = 0;
            this.setNoGravity(false);
            if (this.onGround() && this.airborneGraceTicks <= 0) {
                this.setFlyingAnimationActive(false);
            }
            return;
        }
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.setFlyingAnimationActive(true);
        this.fallDistance = 0.0F;
        ++this.timeFlying;
        this.setBehaviorState(this.escapeFlightActive ? CrowBehaviorState.FLEEING : CrowBehaviorState.FLYING);
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
        } else if (toTarget.lengthSqr() < 2.2D || --this.hoverRetargetTicks <= 0) {
            this.retargetAirCruise(this.escapeFlightActive);
            toTarget = this.flightTarget.subtract(this.position());
            horizontalDistance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        }
        Vec3 direction = toTarget.lengthSqr() > 1.0E-4D ? toTarget.normalize() : this.randomHorizontalDirection();
        Vec3 horizontalDirection = BirdFlightTargeting.normalizeHorizontal(new Vec3(direction.x, 0.0D, direction.z), this.getDeltaMovement());
        if (!this.landingFlight) {
            Vec3 flockHeading = BirdFlightBoids.sameTypeHeading(this, 14.0D, 2.8D, 0.035D, 0.36D, 0.12D, this.escapeFlightActive ? 0.14D : 0.08D);
            if (flockHeading.lengthSqr() > 1.0E-4D) {
                horizontalDirection = BirdFlightTargeting.normalizeHorizontal(horizontalDirection.add(flockHeading), horizontalDirection);
            }
        }
        double speed = this.escapeFlightActive ? FLIGHT_PROFILE.escapeSpeed() : (this.landingFlight ? FLIGHT_PROFILE.landingSpeed() : FLIGHT_PROFILE.cruiseSpeed());
        if (this.landingFlight) {
            speed = BirdFlightController.decelerateNearLanding(speed, horizontalDistance, 4.0D, 0.45D);
        }
        double vertical = this.landingFlight
                ? Mth.clamp(toTarget.y * 0.11D - 0.035D, -0.13D, 0.055D)
                : Mth.clamp(toTarget.y * 0.11D + Math.sin((this.tickCount + this.getId()) * 0.22D) * 0.018D, -0.08D, 0.16D);
        Vec3 desired = new Vec3(horizontalDirection.x * speed, vertical, horizontalDirection.z * speed);
        Vec3 movement = BirdFlightController.blendMovement(this.getDeltaMovement(), desired, 0.68D);
        if (!this.landingFlight && BirdFlightController.isStalledInAir(this, this.timeFlying, 0.006D)) {
            this.retargetAirCruise(this.escapeFlightActive);
            movement = horizontalDirection.scale(Math.max(speed, 0.22D)).add(0.0D, 0.08D, 0.0D);
        }
        this.setDeltaMovement(movement);
        this.faceFlightDirection(movement);
        this.hasImpulse = true;
    }

    private void tickBehaviorFallback() {
        if (this.behaviorStateLockTicks > 0 || this.isEating() || this.isFlightInProgress()) {
            return;
        }
        CrowBehaviorState state = this.getBehaviorState();
        if (BirdGroundAnimation.hasWalkMotion(this)) {
            this.setBehaviorState(CrowBehaviorState.WALKING);
            return;
        }
        if (state == CrowBehaviorState.WALKING
                || state == CrowBehaviorState.FORAGING
                || state == CrowBehaviorState.WATCHING
                || state == CrowBehaviorState.ALERT
                || state == CrowBehaviorState.FOLLOWING_OWNER
                || state.isAirborne()) {
            this.setBehaviorState(CrowBehaviorState.IDLE);
        }
    }

    private void tickGroundMovementFacing() {
        if (this.onGround()
                && !this.isFlightInProgress()
                && !this.isInWaterOrBubble()
                && !this.isPassenger()
                && !this.getBehaviorState().isAirborne()) {
            BirdFlightController.faceGroundMovement(this, this.getDeltaMovement(), 1.0E-4D);
        }
    }

    private void startShortFlight(Vec3 target, boolean fleeing) {
        if (this.flightCooldown > 0 || this.flightTicks > 0 || this.landingFlight) {
            return;
        }
        this.escapeFlightActive = fleeing;
        this.landingFlight = false;
        this.flightTarget = target == null ? this.findAirCruiseTarget(fleeing) : this.clampFlightTarget(target);
        this.flightTicks = fleeing
                ? 100 + this.getRandom().nextInt(80)
                : FLIGHT_PROFILE.minFlightTicks() + this.getRandom().nextInt(FLIGHT_PROFILE.maxFlightTicks() - FLIGHT_PROFILE.minFlightTicks() + 1);
        this.timeFlying = 0;
        this.hoverRetargetTicks = this.nextHoverRetargetDelay();
        this.setNoGravity(true);
        this.setFlyingAnimationActive(true);
        this.setOnGround(false);
        this.getNavigation().stop();
        this.setBehaviorStateFor(fleeing ? CrowBehaviorState.FLEEING : CrowBehaviorState.FLYING, fleeing ? 100 : 90);
        Vec3 direction = this.flightTarget.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() <= 1.0E-4D) {
            direction = this.randomHorizontalDirection();
        }
        Vec3 movement = direction.normalize().scale(fleeing ? 0.34D : 0.24D).add(0.0D, 0.12D, 0.0D);
        this.setDeltaMovement(movement);
        this.faceFlightDirection(movement);
        this.fallDistance = 0.0F;
        this.hasImpulse = true;
    }

    private void startEscapeFlight(Vec3 sourcePos) {
        Vec3 away = this.position().subtract(sourcePos).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() <= 1.0E-4D) {
            away = this.randomHorizontalDirection();
        }
        Vec3 target = BirdFlightTargeting.findAirTarget(this, FLIGHT_PROFILE, away, true);
        if (target == null) {
            target = this.position().add(away.normalize().scale(8.0D + this.getRandom().nextDouble() * 6.0D)).add(0.0D, 3.0D, 0.0D);
        }
        this.startShortFlight(target, true);
    }

    private void beginLandingFlight() {
        Vec3 landingTarget = this.findLandingTarget();
        if (landingTarget == null) {
            this.extendCruiseAfterUnsafeLanding();
            return;
        }
        this.landingFlight = true;
        this.escapeFlightActive = false;
        this.flightTicks = 55 + this.getRandom().nextInt(45);
        this.flightTarget = landingTarget;
        this.hoverRetargetTicks = 0;
        this.setBehaviorStateFor(CrowBehaviorState.FLYING, 55);
    }

    private void extendCruiseAfterUnsafeLanding() {
        this.landingFlight = false;
        this.escapeFlightActive = false;
        this.flightTicks = 70 + this.getRandom().nextInt(55);
        this.retargetAirCruise(false);
        this.setNoGravity(true);
        this.setBehaviorStateFor(CrowBehaviorState.FLYING, 70);
    }

    private void finishFlight() {
        boolean wasEscaping = this.escapeFlightActive;
        this.flightTicks = 0;
        this.timeFlying = 0;
        this.flightTarget = null;
        this.hoverRetargetTicks = 0;
        this.escapeFlightActive = false;
        this.landingFlight = false;
        this.setNoGravity(false);
        this.setFlyingAnimationActive(false);
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.42D, 0.0D, 0.42D));
        this.flightCooldown = wasEscaping ? 110 + this.getRandom().nextInt(100) : 160 + this.getRandom().nextInt(160);
        this.setBehaviorStateFor(wasEscaping ? CrowBehaviorState.ALERT : CrowBehaviorState.IDLE, wasEscaping ? 28 : 14);
    }

    private void retargetAirCruise(boolean fleeing) {
        this.flightTarget = this.findAirCruiseTarget(fleeing);
        this.hoverRetargetTicks = this.nextHoverRetargetDelay();
    }

    private int nextHoverRetargetDelay() {
        return 44 + this.getRandom().nextInt(54);
    }

    private Vec3 findAirCruiseTarget(boolean fleeing) {
        Vec3 direction;
        if (fleeing && this.frightSource != null) {
            Vec3 away = this.position().subtract(this.frightSource);
            direction = away.horizontalDistanceSqr() > 0.01D ? new Vec3(away.x, 0.0D, away.z).normalize() : this.randomHorizontalDirection();
        } else {
            direction = this.getRandom().nextInt(4) == 0 ? this.randomHorizontalDirection() : this.getLookAngle();
        }
        Vec3 target = BirdFlightTargeting.findAirTarget(this, FLIGHT_PROFILE, direction, fleeing);
        if (target != null) {
            return this.clampFlightTarget(target);
        }
        return this.clampFlightTarget(this.position().add(this.randomHorizontalDirection().scale(10.0D)).add(0.0D, this.onGround() ? 4.0D : 1.0D, 0.0D));
    }

    private Vec3 findLandingTarget() {
        Vec3 preferredPerch = this.findPreferredPerchTarget(this.blockPosition(), PERCH_SCAN_HORIZONTAL, PERCH_SCAN_VERTICAL);
        if (preferredPerch != null && (this.isCarryingHeldItem() || this.getRandom().nextInt(4) != 0)) {
            return this.clampFlightTarget(preferredPerch);
        }
        Vec3 sharedLanding = BirdFlightTargeting.findNearestDryLandingTarget(this, 10, 18);
        if (sharedLanding != null) {
            return this.clampFlightTarget(sharedLanding);
        }
        Vec3 forwardLanding = BirdFlightTargeting.findLandingInDirection(this, this.getDeltaMovement(), 4, 12, 8, 18);
        return forwardLanding == null ? null : this.clampFlightTarget(forwardLanding);
    }

    private Vec3 findPreferredPerchTarget(BlockPos center, int horizontalRange, int verticalRange) {
        if (center == null) {
            return null;
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        Vec3 best = null;
        int bestScore = 24;
        int minY = Math.max(this.level().getMinBuildHeight() + 1, center.getY() - 4);
        int maxY = Math.min(this.level().getMaxBuildHeight() - 2, center.getY() + verticalRange);
        for (int xOffset = -horizontalRange; xOffset <= horizontalRange; ++xOffset) {
            for (int zOffset = -horizontalRange; zOffset <= horizontalRange; ++zOffset) {
                if (xOffset * xOffset + zOffset * zOffset > horizontalRange * horizontalRange) {
                    continue;
                }
                for (int y = maxY; y >= minY; --y) {
                    mutable.set(center.getX() + xOffset, y, center.getZ() + zOffset);
                    if (!this.level().hasChunk(mutable.getX() >> 4, mutable.getZ() >> 4)
                            || !BirdFlightTargeting.isSafeDryLanding(this, mutable)) {
                        continue;
                    }
                    int score = this.scoreCrowPerch(mutable, center);
                    if (score > bestScore) {
                        bestScore = score;
                        best = Vec3.atBottomCenterOf(mutable).add(0.0D, 0.05D, 0.0D);
                    }
                    break;
                }
            }
        }
        return best;
    }

    private int scoreCrowPerch(BlockPos landingPos, BlockPos center) {
        BlockState below = this.level().getBlockState(landingPos.below());
        int score = 0;
        if (below.is(BlockTags.LEAVES)) {
            score += 78;
        }
        if (below.getBlock() instanceof FenceBlock || below.getBlock() instanceof FenceGateBlock) {
            score += 70;
        }
        if (below.is(BlockTags.LOGS)) {
            score += 58;
        }
        if (isStonePerchBlock(below)) {
            score += 46;
        }
        if (isWoodenPlatformBlock(below)) {
            score += 42;
        }
        if (below.is(BlockTags.ANIMALS_SPAWNABLE_ON)) {
            score += 12;
        }
        int heightBonus = Mth.clamp(landingPos.getY() - center.getY(), 0, 12);
        int horizontalPenalty = (Math.abs(landingPos.getX() - center.getX()) + Math.abs(landingPos.getZ() - center.getZ())) / 3;
        return score + heightBonus * 5 - horizontalPenalty;
    }

    private static boolean isStonePerchBlock(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.MOSSY_STONE_BRICKS)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.BRICKS);
    }

    private static boolean isWoodenPlatformBlock(BlockState state) {
        return state.is(Blocks.OAK_PLANKS)
                || state.is(Blocks.SPRUCE_PLANKS)
                || state.is(Blocks.BIRCH_PLANKS)
                || state.is(Blocks.JUNGLE_PLANKS)
                || state.is(Blocks.ACACIA_PLANKS)
                || state.is(Blocks.DARK_OAK_PLANKS)
                || state.is(Blocks.MANGROVE_PLANKS)
                || state.is(Blocks.CHERRY_PLANKS)
                || state.is(Blocks.BAMBOO_PLANKS)
                || state.is(Blocks.CRIMSON_PLANKS)
                || state.is(Blocks.WARPED_PLANKS);
    }

    private Vec3 clampFlightTarget(Vec3 target) {
        double y = Mth.clamp(target.y, this.level().getMinBuildHeight() + 1.5D, this.level().getMaxBuildHeight() - 2.0D);
        return new Vec3(target.x, y, target.z);
    }

    private Vec3 randomHorizontalDirection() {
        return BirdFlightTargeting.randomHorizontalDirection(this.getRandom());
    }

    private void faceFlightDirection(Vec3 movement) {
        BirdFlightController.faceMovement(this, movement, FLIGHT_PROFILE.maxPitchDegrees());
    }

    private boolean isFlyingAnimationActive() {
        return this.entityData != null && this.entityData.get(FLYING_ANIMATION_ACTIVE);
    }

    private void setFlyingAnimationActive(boolean active) {
        if (this.entityData != null) {
            this.entityData.set(FLYING_ANIMATION_ACTIVE, active);
        }
    }

    private boolean shouldPlayFlyAnimation() {
        return BirdFlightController.shouldPlayFlyAnimation(
                this,
                this.getBehaviorState().isAirborne() || this.isFlyingAnimationActive(),
                this.onGround(),
                this.isNoGravity(),
                this.getDeltaMovement(),
                this.airborneGraceTicks);
    }

    private boolean shouldPlayWalkAnimation(CrowBehaviorState state) {
        if (!BirdGroundAnimation.canPlayWalk(this) || state.isAirborne() || state == CrowBehaviorState.EATING) {
            return false;
        }
        return BirdGroundAnimation.hasWalkMotion(this)
                || state == CrowBehaviorState.WALKING
                || state == CrowBehaviorState.FORAGING
                || state == CrowBehaviorState.FOLLOWING_OWNER;
    }

    private RawAnimation pickIdleAnimation() {
        if (this.tickCount % (100 + this.getId() % 80) == 0) {
            int roll = this.getRandom().nextInt(4);
            this.currentIdleAnimation = roll == 0 ? IDLE_DIFF_1_ANIMATION : (roll == 1 ? IDLE_DIFF_2_ANIMATION : IDLE_ANIMATION);
        }
        return this.currentIdleAnimation;
    }

    private boolean shouldPlayGlideAnimation() {
        if (this.getBehaviorState() == CrowBehaviorState.FLEEING) {
            return false;
        }
        Vec3 movement = this.getDeltaMovement();
        if (movement.horizontalDistanceSqr() < 0.02D || movement.y > 0.055D) {
            return false;
        }
        int phase = Math.floorMod(this.tickCount + this.getId() * 7, 52);
        return phase >= 18;
    }

    private RawAnimation sleepAnimation() {
        long sleepCycle = this.level().getDayTime() / 24000L;
        return ((this.getId() + sleepCycle) & 1L) == 0L ? SLEEP_ANIMATION : SLEEP_2_ANIMATION;
    }

    private <T extends CrowEntity> PlayState movementController(AnimationState<T> animationState) {
        RawAnimation guidePreviewRawAnimation = this.guidePreviewAnimation.animation();
        if (guidePreviewRawAnimation != null) {
            return animationState.setAndContinue(guidePreviewRawAnimation);
        }
        CrowBehaviorState state = this.getBehaviorState();
        if (this.shouldPlayFlyAnimation()) {
            return animationState.setAndContinue(this.shouldPlayGlideAnimation() ? GLIDE_ANIMATION : FLY_ANIMATION);
        }
        if (state == CrowBehaviorState.SLEEPING) {
            return animationState.setAndContinue(this.sleepAnimation());
        }
        if (this.shouldPlayWalkAnimation(state)) {
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        if (state == CrowBehaviorState.WATCHING || state == CrowBehaviorState.ALERT) {
            return animationState.setAndContinue(IDLE_DIFF_2_ANIMATION);
        }
        if (state == CrowBehaviorState.EATING || this.eatingTicks > 0) {
            return animationState.setAndContinue(EAT_ANIMATION);
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
        return 210;
    }

    @Override
    public float getSoundVolume() {
        return 0.56F;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, BirdSoundVolume.apply(this, volume), pitch);
    }

    private static CrowBehaviorState decodeBehaviorState(int ordinal) {
        CrowBehaviorState[] values = CrowBehaviorState.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return CrowBehaviorState.IDLE;
        }
        return values[ordinal];
    }

    private static final class CrowDepositTreasureGoal extends Goal {
        private static final int SEARCH_HORIZONTAL = 18;
        private static final int SEARCH_VERTICAL = 8;
        private static final double DEPOSIT_REACH_SQR = 1.45D * 1.45D;
        private final CrowEntity crow;
        private CrowNestBlockEntity targetNest;
        private int searchCooldown;
        private int repathTicks;
        private int launchCooldown;
        private int totalTicks;
        private boolean deposited;

        private CrowDepositTreasureGoal(CrowEntity crow) {
            this.crow = crow;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.searchCooldown > 0) {
                --this.searchCooldown;
                return false;
            }
            if (!this.canCarryToNest() || this.crow.isFlightInProgress()) {
                return false;
            }
            ItemStack carried = this.crow.getHeldFoodForRendering();
            this.targetNest = CrowNestBlockEntity.findNearest(
                    this.crow.level(), this.crow.position(), SEARCH_HORIZONTAL, SEARCH_VERTICAL, carried).orElse(null);
            this.searchCooldown = this.targetNest == null
                    ? 55 + this.crow.getRandom().nextInt(45)
                    : 24;
            return this.targetNest != null;
        }

        @Override
        public boolean canContinueToUse() {
            ItemStack carried = this.crow.getHeldFoodForRendering();
            return !this.deposited
                    && this.totalTicks < 260
                    && this.canCarryToNest()
                    && this.isValidTarget(this.targetNest, carried);
        }

        @Override
        public void start() {
            this.repathTicks = 0;
            this.launchCooldown = 0;
            this.totalTicks = 0;
            this.deposited = false;
            this.crow.setBehaviorStateFor(CrowBehaviorState.WATCHING, 24);
            this.moveTowardNest();
        }

        @Override
        public void tick() {
            if (this.targetNest == null) {
                return;
            }
            ++this.totalTicks;
            if (this.launchCooldown > 0) {
                --this.launchCooldown;
            }
            Vec3 depositPosition = this.targetNest.getDepositPosition();
            this.crow.getLookControl().setLookAt(depositPosition.x, depositPosition.y, depositPosition.z, 35.0F, 30.0F);
            if (this.crow.position().distanceToSqr(depositPosition) <= DEPOSIT_REACH_SQR) {
                this.depositCarriedTreasure();
                return;
            }
            if (this.crow.onGround()
                    && this.launchCooldown <= 0
                    && this.crow.position().distanceToSqr(depositPosition) > 3.5D * 3.5D
                    && this.crow.startBirdBathMountFlight(depositPosition.add(0.0D, 0.45D, 0.0D))) {
                this.launchCooldown = 30;
            }
            if (--this.repathTicks <= 0 || this.crow.getNavigation().isDone()) {
                this.moveTowardNest();
            }
        }

        @Override
        public void stop() {
            this.crow.getNavigation().stop();
            this.targetNest = null;
            this.repathTicks = 0;
            this.launchCooldown = 0;
            this.totalTicks = 0;
            if (!this.deposited && this.crow.getBehaviorState() == CrowBehaviorState.WATCHING) {
                this.crow.setBehaviorState(CrowBehaviorState.IDLE);
            }
            this.deposited = false;
        }

        private boolean canCarryToNest() {
            ItemStack carried = this.crow.getHeldFoodForRendering();
            return !this.crow.level().isClientSide
                    && this.crow.isActiveTime()
                    && this.crow.isCarryingHeldItem()
                    && CrowNestTreasure.isShiny(carried)
                    && !this.crow.isEating()
                    && !this.crow.isInWaterOrBubble()
                    && this.crow.getBehaviorState() != CrowBehaviorState.SLEEPING
                    && !this.crow.getBehaviorState().isEscape();
        }

        private boolean isValidTarget(CrowNestBlockEntity nest, ItemStack carried) {
            return nest != null
                    && !nest.isRemoved()
                    && nest.getLevel() == this.crow.level()
                    && nest.hasSpaceFor(carried);
        }

        private void moveTowardNest() {
            if (this.targetNest == null) {
                return;
            }
            Vec3 target = this.targetNest.getDepositPosition();
            this.crow.getNavigation().moveTo(target.x, target.y, target.z, 1.08D);
            this.repathTicks = 14 + this.crow.getRandom().nextInt(10);
        }

        private void depositCarriedTreasure() {
            if (this.targetNest == null || this.deposited) {
                return;
            }
            ItemStack carried = this.crow.getHeldFoodForRendering().copy();
            if (!CrowNestTreasure.isShiny(carried)) {
                return;
            }
            int poseSeed = this.crow.getHeldFoodPoseSeed();
            if (!this.targetNest.addTreasure(carried)) {
                return;
            }
            if (carried.isEmpty()) {
                this.crow.clearHeldFoodForRendering();
            } else {
                this.crow.setHeldFoodForRendering(carried, true, poseSeed);
            }
            this.crow.getNavigation().stop();
            this.crow.playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.45F, 1.08F + this.crow.getRandom().nextFloat() * 0.12F);
            this.crow.setBehaviorStateFor(CrowBehaviorState.WATCHING, 30 + this.crow.getRandom().nextInt(24));
            this.deposited = true;
        }
    }

    private static final class CrowSleepGoal extends Goal {
        private final CrowEntity crow;

        private CrowSleepGoal(CrowEntity crow) {
            this.crow = crow;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !this.crow.isActiveTime()
                    && this.crow.onGround()
                    && !this.crow.isInWaterOrBubble()
                    && !this.crow.isEating()
                    && !this.crow.isFlightInProgress()
                    && !this.crow.getBehaviorState().isEscape();
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
            if (this.crow.getBehaviorState() == CrowBehaviorState.SLEEPING) {
                this.crow.setBehaviorState(CrowBehaviorState.IDLE);
            }
        }

        private void setSleepingState() {
            this.crow.getNavigation().stop();
            Vec3 movement = this.crow.getDeltaMovement();
            this.crow.setDeltaMovement(0.0D, movement.y, 0.0D);
            this.crow.setBehaviorState(CrowBehaviorState.SLEEPING);
        }
    }

    private static final class CrowEatDroppedFoodGoal extends Goal {
        private final CrowEntity crow;
        private ItemEntity target;
        private int repathTicks;
        private int observeTicks;
        private int biteDelayTicks;
        private boolean eatingStarted;
        private boolean consumed;
        private boolean sidestepped;

        private CrowEatDroppedFoodGoal(CrowEntity crow) {
            this.crow = crow;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.crow.canStartForagingGoal() || this.crow.getRandom().nextInt(6) != 0) {
                return false;
            }
            this.target = this.findNearestFood();
            return this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.eatingStarted) {
                return this.biteDelayTicks > 0
                        && this.target != null
                        && this.target.isAlive()
                        && isCrowDroppedFoodCandidate(this.target.getItem())
                        && this.crow.distanceToSqr(this.target) <= 2.25D
                        && this.crow.getBehaviorState() == CrowBehaviorState.EATING
                        && !this.crow.isFlightInProgress();
            }
            return this.target != null
                    && this.target.isAlive()
                    && isCrowDroppedFoodCandidate(this.target.getItem())
                    && this.crow.distanceToSqr(this.target) < 18.0D * 18.0D
                    && !this.crow.isEating()
                    && !this.crow.isFlightInProgress();
        }

        @Override
        public void start() {
            this.repathTicks = 0;
            this.observeTicks = 28 + this.crow.getRandom().nextInt(28);
            this.biteDelayTicks = 0;
            this.eatingStarted = false;
            this.consumed = false;
            this.sidestepped = false;
            this.crow.setBehaviorState(CrowBehaviorState.FORAGING);
            this.crow.signalNearbyCrowsToForage(this.target, false);
        }

        @Override
        public void tick() {
            if (this.target == null) {
                return;
            }
            this.crow.getLookControl().setLookAt(this.target, 25.0F, this.crow.getMaxHeadXRot());
            if (this.eatingStarted) {
                this.crow.getNavigation().stop();
                if (--this.biteDelayTicks <= 0) {
                    this.consumed = this.crow.pickUpItemEntity(this.target);
                    if (!this.consumed) {
                        this.crow.cancelEatingAnimation();
                    }
                }
                return;
            }
            double distanceSqr = this.crow.distanceToSqr(this.target);
            Player watcher = this.crow.nearestWatchingPlayer(this.target.position().add(0.0D, 0.2D, 0.0D), 7.5D);
            boolean watched = watcher != null && distanceSqr > 2.25D;
            if (this.observeTicks > 0 || watched) {
                this.crow.setBehaviorStateFor(CrowBehaviorState.WATCHING, 14);
                if (!this.sidestepped && distanceSqr <= 10.0D * 10.0D) {
                    this.sidestepped = true;
                    this.crow.sidestepAround(this.target.position(), 1.25D + this.crow.getRandom().nextDouble() * 0.55D, 0.78D);
                } else if (watched) {
                    this.crow.getNavigation().stop();
                }
                if (watched) {
                    this.observeTicks = Math.max(this.observeTicks, 8);
                } else {
                    --this.observeTicks;
                }
                return;
            }
            if (distanceSqr <= 1.65D) {
                this.crow.getNavigation().stop();
                this.crow.beginEatingAnimation(false);
                this.biteDelayTicks = EAT_BITE_DELAY_TICKS;
                this.eatingStarted = true;
                return;
            }
            if (--this.repathTicks <= 0 || this.crow.getNavigation().isDone()) {
                this.repathTicks = 7;
                this.crow.getNavigation().moveTo(this.target, 1.18D);
            }
        }

        @Override
        public void stop() {
            if (this.eatingStarted && !this.consumed) {
                this.crow.cancelEatingAnimation();
            }
            this.target = null;
            this.repathTicks = 0;
            this.observeTicks = 0;
            this.biteDelayTicks = 0;
            this.eatingStarted = false;
            this.consumed = false;
            this.sidestepped = false;
            if (!this.crow.isEating() && this.crow.getBehaviorState() == CrowBehaviorState.FORAGING) {
                this.crow.setBehaviorState(CrowBehaviorState.IDLE);
            }
        }

        private ItemEntity findNearestFood() {
            ItemEntity best = null;
            double bestDistance = 12.0D * 12.0D;
            for (ItemEntity item : this.crow.level().getEntitiesOfClass(ItemEntity.class, this.crow.getBoundingBox().inflate(12.0D, 4.0D, 12.0D), entity -> entity.isAlive() && isCrowDroppedFoodCandidate(entity.getItem()))) {
                double distance = this.crow.distanceToSqr(item);
                if (distance < bestDistance) {
                    best = item;
                    bestDistance = distance;
                }
            }
            return best;
        }
    }

    private static final class CrowInvestigateShinyGoal extends Goal {
        private final CrowEntity crow;
        private ItemEntity target;
        private int observeTicks;
        private int repathTicks;
        private boolean pickedUp;

        private CrowInvestigateShinyGoal(CrowEntity crow) {
            this.crow = crow;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.crow.canStartForagingGoal() || this.crow.shinyCooldown > 0 || this.crow.getRandom().nextInt(22) != 0) {
                return false;
            }
            this.target = this.findNearestShiny();
            return this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null
                    && this.target.isAlive()
                    && isLowValueShiny(this.target.getItem())
                    && !this.pickedUp
                    && this.observeTicks > 0
                    && !this.crow.isEating()
                    && !this.crow.isFlightInProgress();
        }

        @Override
        public void start() {
            this.observeTicks = 90 + this.crow.getRandom().nextInt(80);
            this.repathTicks = 0;
            this.pickedUp = false;
            this.crow.setBehaviorState(CrowBehaviorState.WATCHING);
            this.crow.signalNearbyCrowsToForage(this.target, true);
        }

        @Override
        public void tick() {
            --this.observeTicks;
            if (this.target == null) {
                return;
            }
            this.crow.getLookControl().setLookAt(this.target, 35.0F, this.crow.getMaxHeadXRot());
            double distanceSqr = this.crow.distanceToSqr(this.target);
            if (distanceSqr <= 1.65D) {
                this.crow.getNavigation().stop();
                this.pickedUp = this.crow.pickUpItemEntity(this.target);
                if (this.pickedUp) {
                    this.observeTicks = 0;
                }
                return;
            }
            if (distanceSqr > 4.0D && (--this.repathTicks <= 0 || this.crow.getNavigation().isDone())) {
                this.repathTicks = 12;
                this.crow.getNavigation().moveTo(this.target, 0.88D);
            } else if (distanceSqr <= 4.0D) {
                this.crow.getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            this.target = null;
            this.observeTicks = 0;
            this.repathTicks = 0;
            this.pickedUp = false;
            this.crow.shinyCooldown = 220 + this.crow.getRandom().nextInt(180);
            if (this.crow.getBehaviorState() == CrowBehaviorState.WATCHING) {
                this.crow.setBehaviorState(CrowBehaviorState.IDLE);
            }
        }

        private ItemEntity findNearestShiny() {
            ItemEntity best = null;
            double bestDistance = 10.0D * 10.0D;
            for (ItemEntity item : this.crow.level().getEntitiesOfClass(ItemEntity.class, this.crow.getBoundingBox().inflate(10.0D, 4.0D, 10.0D), entity -> entity.isAlive() && isLowValueShiny(entity.getItem()))) {
                double distance = this.crow.distanceToSqr(item);
                if (distance < bestDistance) {
                    best = item;
                    bestDistance = distance;
                }
            }
            return best;
        }
    }

    private static final class CrowWatchPlayerGoal extends Goal {
        private final CrowEntity crow;
        private Player target;
        private int repathTicks;

        private CrowWatchPlayerGoal(CrowEntity crow) {
            this.crow = crow;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.crow.isEating() || this.crow.isFlightInProgress() || this.crow.getRandom().nextInt(4) != 0) {
                return false;
            }
            this.target = this.findNearestPlayer();
            return this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null
                    && this.target.isAlive()
                    && !this.target.isSpectator()
                    && this.crow.distanceToSqr(this.target) <= 13.0D * 13.0D
                    && !this.crow.isEating()
                    && !this.crow.isFlightInProgress();
        }

        @Override
        public void start() {
            this.repathTicks = 0;
        }

        @Override
        public void tick() {
            double distanceSqr = this.crow.distanceToSqr(this.target);
            boolean trustedPlayer = this.crow.isTrustedPlayer(this.target);
            boolean holdingThreat = this.crow.isHoldingThreateningItem(this.target);
            boolean holdingFood = this.crow.isHoldingCrowFood(this.target);
            boolean holdingShiny = this.crow.isHoldingLowValueShiny(this.target);
            this.crow.getLookControl().setLookAt(this.target, 35.0F, this.crow.getMaxHeadXRot());
            if (holdingThreat) {
                this.crow.setBehaviorStateFor(CrowBehaviorState.ALERT, 24);
                if (distanceSqr <= 8.5D * 8.5D && this.crow.flightCooldown <= 0 && this.crow.onGround()) {
                    Vec3 perch = this.crow.findPreferredPerchTarget(this.target.blockPosition(), 8, 12);
                    if (perch != null) {
                        this.crow.startShortFlight(perch, false);
                    } else {
                        this.crow.frightenFrom(this.target.position(), trustedPlayer ? 40 : 80);
                    }
                    return;
                }
                if (distanceSqr <= 10.0D * 10.0D && --this.repathTicks <= 0) {
                    this.repathTicks = 16;
                    Vec3 away = this.crow.position().subtract(this.target.position()).multiply(1.0D, 0.0D, 1.0D);
                    if (away.lengthSqr() > 1.0E-4D) {
                        Vec3 move = this.crow.position().add(away.normalize().scale(4.0D));
                        this.crow.getNavigation().moveTo(move.x, move.y, move.z, 1.02D);
                    }
                }
                return;
            }
            if (holdingShiny && !trustedPlayer) {
                this.crow.setBehaviorStateFor(CrowBehaviorState.WATCHING, 24);
                if (distanceSqr <= 2.7D * 2.7D
                        && !this.crow.isPlayerLookingAtMe(this.target)
                        && this.crow.getRandom().nextInt(16) == 0
                        && this.crow.tryStealHeldItem(this.target, true)) {
                    return;
                }
                if (distanceSqr > 4.8D * 4.8D) {
                    if (--this.repathTicks <= 0 || this.crow.getNavigation().isDone()) {
                        this.repathTicks = 12;
                        this.crow.getNavigation().moveTo(this.target, 0.78D);
                    }
                } else {
                    this.crow.getNavigation().stop();
                    if (--this.repathTicks <= 0) {
                        this.repathTicks = 22;
                        this.crow.sidestepAround(this.target.position(), 1.35D, 0.72D);
                    }
                }
                return;
            }
            if (holdingFood && !trustedPlayer) {
                this.crow.setBehaviorStateFor(CrowBehaviorState.WATCHING, 20);
                if (distanceSqr <= 2.45D * 2.45D
                        && !this.crow.isPlayerLookingAtMe(this.target)
                        && this.crow.getRandom().nextInt(42) == 0
                        && this.crow.tryStealHeldItem(this.target, false)) {
                    return;
                }
                if (distanceSqr > 5.5D * 5.5D) {
                    if (--this.repathTicks <= 0 || this.crow.getNavigation().isDone()) {
                        this.repathTicks = 14;
                        this.crow.getNavigation().moveTo(this.target, 0.68D);
                    }
                } else {
                    this.crow.getNavigation().stop();
                }
                return;
            }
            if (distanceSqr <= 4.0D * 4.0D && !trustedPlayer && this.crow.flightCooldown <= 0) {
                this.crow.frightenFrom(this.target.position(), this.crow.calmTicks > 0 ? 40 : 70);
                return;
            }
            if (trustedPlayer && distanceSqr <= 5.0D * 5.0D) {
                this.crow.getNavigation().stop();
                this.crow.setBehaviorStateFor(CrowBehaviorState.WATCHING, 18);
                return;
            }
            if (distanceSqr <= 6.0D * 6.0D) {
                this.crow.setBehaviorStateFor(CrowBehaviorState.ALERT, 18);
                if (--this.repathTicks <= 0) {
                    this.repathTicks = 12;
                    Vec3 away = this.crow.position().subtract(this.target.position()).multiply(1.0D, 0.0D, 1.0D);
                    if (away.lengthSqr() > 1.0E-4D) {
                        Vec3 move = this.crow.position().add(away.normalize().scale(3.2D));
                        this.crow.getNavigation().moveTo(move.x, move.y, move.z, 1.03D);
                    }
                }
                return;
            }
            if (distanceSqr <= 5.0D * 5.0D && --this.repathTicks <= 0) {
                this.repathTicks = 20;
                this.crow.sidestepAround(this.target.position(), 1.15D, 0.72D);
                this.crow.setBehaviorStateFor(CrowBehaviorState.WATCHING, 16);
                return;
            }
            this.crow.getNavigation().stop();
            this.crow.setBehaviorStateFor(CrowBehaviorState.WATCHING, 14);
        }

        @Override
        public void stop() {
            this.target = null;
            this.repathTicks = 0;
            if (this.crow.getBehaviorState() == CrowBehaviorState.WATCHING || this.crow.getBehaviorState() == CrowBehaviorState.ALERT) {
                this.crow.setBehaviorState(CrowBehaviorState.IDLE);
            }
        }

        private Player findNearestPlayer() {
            Player best = null;
            double bestDistance = 12.0D * 12.0D;
            for (Player player : this.crow.level().getEntitiesOfClass(Player.class, this.crow.getBoundingBox().inflate(12.0D), player -> player.isAlive() && !player.isSpectator())) {
                double distance = this.crow.distanceToSqr(player);
                if (distance < bestDistance) {
                    best = player;
                    bestDistance = distance;
                }
            }
            return best;
        }
    }

    private static final class CrowFleeGoal extends Goal {
        private final CrowEntity crow;
        private Player remembered;
        private int repathTicks;

        private CrowFleeGoal(CrowEntity crow) {
            this.crow = crow;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.crow.isEating() || this.crow.isFlightInProgress() || this.crow.angerMemoryTicks <= 0) {
                return false;
            }
            this.remembered = this.crow.rememberedPlayer();
            return this.remembered != null
                    && this.remembered.isAlive()
                    && this.crow.distanceToSqr(this.remembered) <= 14.0D * 14.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return this.remembered != null
                    && this.remembered.isAlive()
                    && this.crow.angerMemoryTicks > 0
                    && !this.crow.isEating()
                    && !this.crow.isFlightInProgress()
                    && this.crow.distanceToSqr(this.remembered) <= 16.0D * 16.0D;
        }

        @Override
        public void start() {
            this.repathTicks = 0;
            this.crow.setBehaviorStateFor(CrowBehaviorState.ALERT, 40);
            if (this.crow.distanceToSqr(this.remembered) <= 8.0D * 8.0D) {
                this.crow.frightenFrom(this.remembered.position(), 90);
            }
        }

        @Override
        public void tick() {
            this.crow.getLookControl().setLookAt(this.remembered, 35.0F, this.crow.getMaxHeadXRot());
            if (this.crow.distanceToSqr(this.remembered) <= 7.0D * 7.0D && this.crow.flightCooldown <= 0) {
                this.crow.frightenFrom(this.remembered.position(), 90);
                return;
            }
            if (--this.repathTicks <= 0) {
                this.repathTicks = 12;
                Vec3 away = this.crow.position().subtract(this.remembered.position()).multiply(1.0D, 0.0D, 1.0D);
                if (away.lengthSqr() > 1.0E-4D) {
                    Vec3 move = this.crow.position().add(away.normalize().scale(5.0D));
                    this.crow.getNavigation().moveTo(move.x, move.y, move.z, 1.08D);
                }
            }
        }

        @Override
        public void stop() {
            this.remembered = null;
            this.repathTicks = 0;
        }
    }

    private static final class CrowFollowOwnerGoal extends Goal {
        private final CrowEntity crow;
        private final double speed;
        private final float stopDistance;
        private final float startDistance;
        private LivingEntity owner;
        private int repathTicks;

        private CrowFollowOwnerGoal(CrowEntity crow, double speed, float stopDistance, float startDistance) {
            this.crow = crow;
            this.speed = speed;
            this.stopDistance = stopDistance;
            this.startDistance = startDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = this.crow.getOwner();
            if (!this.canFollowOwner(owner)) {
                return false;
            }
            this.owner = owner;
            return this.crow.distanceToSqr(owner) > (double)(this.startDistance * this.startDistance);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canFollowOwner(this.owner)
                    && (this.crow.isFlightInProgress()
                    || this.crow.distanceToSqr(this.owner) > (double)(this.stopDistance * this.stopDistance));
        }

        @Override
        public void start() {
            this.repathTicks = 0;
            this.crow.setBehaviorState(CrowBehaviorState.FOLLOWING_OWNER);
        }

        @Override
        public void tick() {
            if (this.owner == null) {
                return;
            }
            this.crow.getLookControl().setLookAt(this.owner, 25.0F, this.crow.getMaxHeadXRot());
            this.crow.setBehaviorState(CrowBehaviorState.FOLLOWING_OWNER);
            if (this.crow.isFlightInProgress()) {
                return;
            }
            double distanceSqr = this.crow.distanceToSqr(this.owner);
            if (distanceSqr > 64.0D && this.crow.onGround() && this.crow.flightCooldown <= 0) {
                Vec3 target = this.crow.findPreferredPerchTarget(this.owner.blockPosition(), 7, 10);
                if (target == null) {
                    target = BirdFlightTargeting.findDryLandingTargetNear(this.crow, this.owner.blockPosition(), 5, 10);
                }
                if (target != null) {
                    this.crow.startShortFlight(target, false);
                    return;
                }
            }
            if (--this.repathTicks <= 0) {
                this.repathTicks = 10;
                this.crow.getNavigation().moveTo(this.owner, this.speed);
            }
        }

        @Override
        public void stop() {
            this.owner = null;
            this.repathTicks = 0;
            if (!this.crow.isFlightInProgress() && this.crow.getBehaviorState() == CrowBehaviorState.FOLLOWING_OWNER) {
                this.crow.setBehaviorState(CrowBehaviorState.IDLE);
            }
        }

        private boolean canFollowOwner(LivingEntity owner) {
            if (!this.crow.isTame()
                    || owner == null
                    || !owner.isAlive()
                    || this.crow.isEating()
                    || this.crow.getBehaviorState() == CrowBehaviorState.SLEEPING) {
                return false;
            }
            if (owner instanceof Player player) {
                return !player.isSpectator() && !this.crow.isRememberingPlayer(player);
            }
            return true;
        }
    }

    private static final class CrowAmbientFlightGoal extends Goal {
        private final CrowEntity crow;

        private CrowAmbientFlightGoal(CrowEntity crow) {
            this.crow = crow;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.crow.flightCooldown <= 0
                    && this.crow.onGround()
                    && this.crow.isActiveTime()
                    && this.crow.getNavigation().isDone()
                    && !this.crow.isEating()
                    && !this.crow.getBehaviorState().isEscape()
                    && this.crow.getRandom().nextInt(170) == 0;
        }

        @Override
        public void start() {
            Vec3 target = this.crow.findPreferredPerchTarget(this.crow.blockPosition(), 10, 12);
            if (target != null && target.distanceToSqr(this.crow.position()) < 6.25D) {
                target = null;
            }
            this.crow.startShortFlight(target == null ? this.crow.findAirCruiseTarget(false) : target, false);
        }
    }

    public enum GuidePreviewAnimation {
        NONE(null),
        IDLE(IDLE_ANIMATION),
        LOOK_1(IDLE_DIFF_1_ANIMATION),
        LOOK_2(IDLE_DIFF_2_ANIMATION),
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
