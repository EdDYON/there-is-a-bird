package EdDYON.guaniao.content.bird.umbrellacockatoo;

import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.advancement.BirdAdvancements;
import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.BirdLoudSoundListener;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarBehaviorState;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarControl;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.cockatiel.CockatielEntity;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.content.bird.flight.BirdFlightAnimation;
import EdDYON.guaniao.content.bird.flight.BirdFlightProfile;
import EdDYON.guaniao.content.bird.macaw.MacawEntity;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A large, loud, intensely social cockatoo.
 *
 * <p>One GeckoLib controller owns the base pose. The client samples the authored
 * emotion tracks separately and adds their local deltas only while drawing.</p>
 *
 * <p>Taming is progressive: no single feed is ever a coin flip. Trust is tracked
 * per player, so two people can both make progress on the same bird.</p>
 */
public class UmbrellaCockatooEntity extends BudgerigarEntity implements BirdLoudSoundListener {
    private static final EntityDataAccessor<Integer> EMOTION =
            SynchedEntityData.defineId(UmbrellaCockatooEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEAD_ROLL =
            SynchedEntityData.defineId(UmbrellaCockatooEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> PERCH_SLOT =
            SynchedEntityData.defineId(UmbrellaCockatooEntity.class, EntityDataSerializers.INT);

    /** Mirrors the private taming feedback events on {@code BudgerigarEntity}. */
    private static final byte TAMING_FAILED_EVENT = 6;
    private static final byte TAMING_SUCCEEDED_EVENT = 7;

    private static final String TRUST_NBT_KEY = "UmbrellaCockatooTrust";
    private static final String EMOTION_NBT_KEY = "UmbrellaCockatooEmotion";
    private static final String PERCH_NBT_KEY = "UmbrellaCockatooPerch";

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation IDLE_DIFF_1_ANIMATION =
            RawAnimation.begin().thenPlay("animation.idle_diff_1").thenLoop("animation.idle");
    private static final RawAnimation IDLE_DIFF_2_ANIMATION =
            RawAnimation.begin().thenLoop("animation.idle_diff_2_residual");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation FLY_ANIMATION = RawAnimation.begin().thenLoop("animation.fly");
    private static final RawAnimation EAT_ANIMATION =
            RawAnimation.begin().thenPlay("animation.eat").thenLoop("animation.idle");
    private static final RawAnimation SLEEP_ANIMATION =
            RawAnimation.begin().thenPlay("animation.sleep").thenLoop("animation.sleep_loop");

    private static final int GROUND_AVERSION_TICKS = 170;
    private static final int PERCH_SLOT_NONE = 0;
    private static final int PERCH_SLOT_LEFT_SHOULDER = 1;
    private static final int PERCH_SLOT_RIGHT_SHOULDER = 2;
    private static final int PERCH_SLOT_HEAD = 3;

    // Same order as CockatooCallSequence.Clip: short calls and phrases share one voice.
    private static final List<Supplier<SoundEvent>> NATIVE_CALLS = List.of(
            GuaniaoSoundEvents.UMBRELLA_COCKATOO_CALL_01,
            GuaniaoSoundEvents.UMBRELLA_COCKATOO_CALL_02,
            GuaniaoSoundEvents.UMBRELLA_COCKATOO_CALL_03,
            GuaniaoSoundEvents.UMBRELLA_COCKATOO_CALL_04,
            GuaniaoSoundEvents.UMBRELLA_COCKATOO_CALL_05,
            GuaniaoSoundEvents.UMBRELLA_COCKATOO_CALL_07,
            GuaniaoSoundEvents.UMBRELLA_COCKATOO_CALL_08,
            GuaniaoSoundEvents.UMBRELLA_COCKATOO_PHRASE_01,
            GuaniaoSoundEvents.UMBRELLA_COCKATOO_PHRASE_02,
            GuaniaoSoundEvents.UMBRELLA_COCKATOO_PHRASE_03,
            GuaniaoSoundEvents.UMBRELLA_COCKATOO_PHRASE_04
    );

    private final Map<UUID, Integer> trustByPlayer = new HashMap<>();
    private int curiousTicks;
    private int alertTicks;
    private int excitedTicks;
    private int startledTicks;
    private int soundFocusTicks;
    private int wildTrustDecayTicks;
    private int headTiltHoldTicks;
    private int headTiltCooldown;
    private int observeScanCooldown;
    private int inspectCooldown;
    private int socialCooldown;
    private int mimicCooldown;
    private final CockatooCallSequence callSequence = new CockatooCallSequence();
    private float callSequenceVolume;
    private int groundTicks;
    private int ownerPerchCooldown;
    private int ownerPerchSwapCooldown;
    private boolean pendingHighPerch;
    private float clientHeadRoll;
    private GuidePreviewAnimation previewAnimation = GuidePreviewAnimation.NONE;
    private RawAnimation currentIdleAnimation = IDLE_ANIMATION;
    private long nextIdleAnimationTick;
    private final CockatooDisplayState displayAnimation = new CockatooDisplayState();
    private double cockatooAnimationTick;
    @Nullable
    private Vec3 soundFocus;
    @Nullable
    private UUID observedPlayerId;

    public UmbrellaCockatooEntity(EntityType<? extends UmbrellaCockatooEntity> entityType, Level level) {
        super(entityType, level);
        this.mimicCooldown = 300 + this.getRandom().nextInt(400);
    }

    public static AttributeSupplier.Builder createUmbrellaCockatooAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, UmbrellaCockatooDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, UmbrellaCockatooDefinition.WALK_SPEED)
                .add(Attributes.FLYING_SPEED, UmbrellaCockatooDefinition.FLYING_SPEED)
                .add(Attributes.FOLLOW_RANGE, UmbrellaCockatooDefinition.FOLLOW_RANGE);
    }

    /**
     * This bird lives inside the canopy, so the shared "must see the sky" check
     * would reject exactly the trees it belongs in. Leaves and logs count as footing.
     */
    public static boolean canUmbrellaCockatooSpawn(EntityType<UmbrellaCockatooEntity> entityType,
                                                   ServerLevelAccessor level, MobSpawnType spawnType,
                                                   BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean canopyOrFloor = below.is(BlockTags.LEAVES)
                || below.is(BlockTags.LOGS)
                || below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(Blocks.MOSS_BLOCK);
        return canopyOrFloor && level.getRawBrightness(pos, 0) > 7;
    }

    @Override
    protected void registerGoals() {
        // Registered before the shared parrot goals so this species' signature
        // behaviours win same-priority ties. Lower priority numbers (fright, stay,
        // roost, eat, owner follow) still preempt them.
        this.goalSelector.addGoal(9, new UmbrellaCockatooObserveGoal(this));
        this.goalSelector.addGoal(10, new UmbrellaCockatooInspectGoal(this));
        this.goalSelector.addGoal(10, new UmbrellaCockatooSocialGoal(this));
        super.registerGoals();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(EMOTION, UmbrellaCockatooEmotion.RELAXED.ordinal());
        this.entityData.define(HEAD_ROLL, 0.0F);
        this.entityData.define(PERCH_SLOT, PERCH_SLOT_NONE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag trust = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : this.trustByPlayer.entrySet()) {
            trust.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put(TRUST_NBT_KEY, trust);
        tag.putInt(EMOTION_NBT_KEY, this.getEmotion().ordinal());
        tag.putInt(PERCH_NBT_KEY, this.getPerchSlot());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.trustByPlayer.clear();
        if (tag.contains(TRUST_NBT_KEY, 10)) {
            CompoundTag trust = tag.getCompound(TRUST_NBT_KEY);
            for (String key : trust.getAllKeys()) {
                try {
                    UUID player = UUID.fromString(key);
                    this.trustByPlayer.put(player,
                            Mth.clamp(trust.getInt(key), 0, UmbrellaCockatooDefinition.TAMING_TRUST_MAX));
                } catch (IllegalArgumentException ignored) {
                    // A malformed key from a hand-edited save is dropped instead of failing the load.
                }
            }
        }
        this.setEmotion(UmbrellaCockatooEmotion.byId(tag.getInt(EMOTION_NBT_KEY)));
        this.entityData.set(PERCH_SLOT, tag.getInt(PERCH_NBT_KEY));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            this.tickClientHeadRoll();
            return;
        }
        this.tickScanCooldowns();
        this.tickEmotionState();
        this.tickSoundFocus();
        this.tickHeadTilt();
        this.tickWildTrustDecay();
        this.tickOwnerPerch();
        this.tickOwnerProximity();
        this.tickHighPerchPreference();
        this.tickNativeCalls();
        this.tickMimicry();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Wild birds use progressive trust. Once tamed the shared parrot feeding
        // path takes over, so flock trust, animations and particles stay in charge.
        if (!this.isTame() && this.isEdibleFoodForThisBird(stack)) {
            return this.handleProgressiveFeeding(player, stack);
        }
        return super.mobInteract(player, hand);
    }

    private InteractionResult handleProgressiveFeeding(Player player, ItemStack stack) {
        if (this.level().isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (BudgerigarControl.isEating(this)) {
            this.playInteractionSound();
            return InteractionResult.SUCCESS;
        }
        ItemStack eaten = stack.copy();
        eaten.setCount(1);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        int grant = UmbrellaCockatooDefinition.TRUST_PER_FEED_MIN
                + this.getRandom().nextInt(UmbrellaCockatooDefinition.TRUST_PER_FEED_MAX
                        - UmbrellaCockatooDefinition.TRUST_PER_FEED_MIN + 1);
        this.playInteractionSound();
        BudgerigarControl.startEatingFood(this, eaten, true);
        // Keep the shared parrot trust counter moving so flocking and curiosity
        // goals keep their established tuning, without letting it finish taming.
        BudgerigarControl.addTrust(this, 300);
        this.excitedTicks = Math.max(this.excitedTicks, 140);
        int trust = this.addTrustFor(player, grant);
        if (trust >= UmbrellaCockatooDefinition.TAMING_TRUST_MAX) {
            this.completeTaming(player);
        } else {
            this.curiousTicks = Math.max(this.curiousTicks, 180);
            this.level().broadcastEntityEvent(this, TAMING_FAILED_EVENT);
        }
        return InteractionResult.SUCCESS;
    }

    private void completeTaming(Player player) {
        boolean wasTame = this.isTame();
        this.setTame(true);
        this.setOwnerUUID(player.getUUID());
        this.setBirdCommandMode(BirdCommandMode.FOLLOW);
        this.trustByPlayer.put(player.getUUID(), UmbrellaCockatooDefinition.TAMING_TRUST_MAX);
        this.playInteractionSound();
        BudgerigarControl.setBehaviorStateFor(this, BudgerigarBehaviorState.DANCING, 80);
        this.excitedTicks = Math.max(this.excitedTicks, 200);
        // The synced mood is re-derived from the tick counters every frame by
        // tickEmotionState(); writing EMOTION directly here was overwritten on
        // the next tick, which is why the taming celebration never showed.
        if (!wasTame) {
            BirdAdvancements.awardTamedBird(player, this);
            this.level().broadcastEntityEvent(this, TAMING_SUCCEEDED_EVENT);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float healthBefore = this.getHealth();
        boolean hurt = super.hurt(source, amount);
        if (!hurt || this.level().isClientSide) {
            return hurt;
        }
        this.feel(UmbrellaCockatooEmotion.STARTLED, 60);
        this.clearHeadTilt();
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player && !this.isTame()) {
            int penalty = amount >= healthBefore
                    ? UmbrellaCockatooDefinition.TRUST_LOST_ON_FATAL_ATTACK
                    : UmbrellaCockatooDefinition.TRUST_PENALTY_ON_ATTACK;
            this.addTrustFor(player, -penalty);
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Emotion
    // ------------------------------------------------------------------

    public UmbrellaCockatooEmotion getEmotion() {
        return UmbrellaCockatooEmotion.byId(this.entityData.get(EMOTION));
    }

    private void setEmotion(UmbrellaCockatooEmotion emotion) {
        this.entityData.set(EMOTION, (emotion == null ? UmbrellaCockatooEmotion.RELAXED : emotion).ordinal());
    }

    /** Raises a mood for a while. The client animation controller reads the synced value. */
    public void feel(UmbrellaCockatooEmotion emotion, int ticks) {
        if (this.level().isClientSide || emotion == null || emotion == UmbrellaCockatooEmotion.RELAXED) {
            return;
        }
        int clamped = Math.max(10, ticks);
        switch (emotion) {
            case STARTLED -> this.startledTicks = Math.max(this.startledTicks, clamped);
            case EXCITED -> this.excitedTicks = Math.max(this.excitedTicks, clamped);
            case ALERT -> this.alertTicks = Math.max(this.alertTicks, clamped);
            case CURIOUS -> this.curiousTicks = Math.max(this.curiousTicks, clamped);
            default -> {
            }
        }
        this.setEmotion(emotion);
    }

    private void tickEmotionState() {
        if (this.startledTicks > 0) {
            --this.startledTicks;
        }
        if (this.excitedTicks > 0) {
            --this.excitedTicks;
        }
        if (this.alertTicks > 0) {
            --this.alertTicks;
        }
        if (this.curiousTicks > 0) {
            --this.curiousTicks;
        }
        // Sleep always wins: a sleeping bird is calm no matter what it was feeling.
        if (this.isBirdSleeping() || BudgerigarControl.isSleepingOrRoosting(this)) {
            this.curiousTicks = 0;
            this.alertTicks = 0;
            this.excitedTicks = 0;
            this.startledTicks = 0;
            this.setEmotion(UmbrellaCockatooEmotion.RELAXED);
            return;
        }
        UmbrellaCockatooEmotion next;
        if (this.startledTicks > 0) {
            next = UmbrellaCockatooEmotion.STARTLED;
        } else if (this.excitedTicks > 0) {
            next = UmbrellaCockatooEmotion.EXCITED;
        } else if (this.alertTicks > 0) {
            next = UmbrellaCockatooEmotion.ALERT;
        } else if (this.curiousTicks > 0) {
            next = UmbrellaCockatooEmotion.CURIOUS;
        } else {
            next = UmbrellaCockatooEmotion.RELAXED;
        }
        if (next != this.getEmotion()) {
            this.setEmotion(next);
        }
    }

    /** Single place where scan throttles drain, so goals cannot deadlock themselves. */
    private void tickScanCooldowns() {
        if (this.observeScanCooldown > 0) {
            --this.observeScanCooldown;
        }
        if (this.inspectCooldown > 0) {
            --this.inspectCooldown;
        }
        if (this.socialCooldown > 0) {
            --this.socialCooldown;
        }
    }

    // ------------------------------------------------------------------
    // Head tilt
    // ------------------------------------------------------------------

    public float getHeadRoll() {
        return this.level().isClientSide ? this.clientHeadRoll : this.entityData.get(HEAD_ROLL);
    }

    private void tickClientHeadRoll() {
        float target = this.entityData.get(HEAD_ROLL);
        this.clientHeadRoll = Mth.lerp(0.22F, this.clientHeadRoll, target);
        if (Math.abs(this.clientHeadRoll - target) < 0.05F) {
            this.clientHeadRoll = target;
        }
    }

    /** Picks a side and holds it. Deliberately slow: no mechanical left-right twitching. */
    public void beginHeadTilt() {
        if (this.level().isClientSide || this.headTiltHoldTicks > 0 || this.headTiltCooldown > 0) {
            return;
        }
        int magnitude = 12 + this.getRandom().nextInt(14);
        this.headTiltHoldTicks = 15 + this.getRandom().nextInt(31);
        this.entityData.set(HEAD_ROLL, (float) ((this.getRandom().nextBoolean() ? 1 : -1) * magnitude));
    }

    public boolean isHeadTilting() {
        return this.headTiltHoldTicks > 0;
    }

    private void clearHeadTilt() {
        this.headTiltHoldTicks = 0;
        this.headTiltCooldown = 60;
        this.entityData.set(HEAD_ROLL, 0.0F);
    }

    private void tickHeadTilt() {
        if (this.headTiltCooldown > 0) {
            --this.headTiltCooldown;
        }
        if (this.headTiltHoldTicks > 0 && --this.headTiltHoldTicks <= 0) {
            this.entityData.set(HEAD_ROLL, 0.0F);
            this.headTiltCooldown = 40 + this.getRandom().nextInt(80);
        }
    }

    // ------------------------------------------------------------------
    // Calm/awake gate shared by the three custom goals
    // ------------------------------------------------------------------

    /** Awake, on the ground, not busy eating, not in an emergency, free to be curious. */
    public boolean isCalmAndAwake() {
        return BudgerigarControl.canStartSocialGoal(this)
                && !this.isFlying()
                && !this.isPassenger()
                && !BudgerigarControl.isEating(this);
    }

    public boolean isDayActive() {
        return BirdActivitySchedule.DIURNAL.isActiveTime(this.level().getDayTime());
    }

    @Nullable
    public UUID getObservedPlayerId() {
        return this.observedPlayerId;
    }

    public void setObservedPlayer(@Nullable UUID playerId) {
        this.observedPlayerId = playerId;
    }

    public boolean isObserveCooldownReady() {
        return this.observeScanCooldown <= 0;
    }

    void markObserveScan() {
        this.observeScanCooldown = 8 + this.getRandom().nextInt(5);
    }

    public boolean isInspectCooldownReady() {
        return this.inspectCooldown <= 0;
    }

    void markInspect(long ticks) {
        this.inspectCooldown = (int) Math.max(40L, ticks);
    }

    public boolean isSocialCooldownReady() {
        return this.socialCooldown <= 0;
    }

    void markSocial(int ticks) {
        this.socialCooldown = Math.max(20, ticks);
    }

    // ------------------------------------------------------------------
    // Loud sounds
    // ------------------------------------------------------------------

    private void tickSoundFocus() {
        if (this.soundFocusTicks > 0) {
            --this.soundFocusTicks;
        }
        if (this.soundFocus != null) {
            this.getLookControl().setLookAt(this.soundFocus.x, this.soundFocus.y, this.soundFocus.z, 45.0F, 45.0F);
            if (this.soundFocusTicks <= 0) {
                this.soundFocus = null;
            }
        }
    }

    @Override
    public void onLoudSound(Vec3 soundPosition, float volume) {
        if (this.level().isClientSide || soundPosition == null) {
            return;
        }
        boolean wasSleeping = this.isBirdSleeping() || BudgerigarControl.isSleepingOrRoosting(this);
        if (wasSleeping) {
            this.wakeFromLoudSound(soundPosition);
        }
        this.soundFocus = soundPosition;
        this.soundFocusTicks = 20 + this.getRandom().nextInt(41);
        this.clearHeadTilt();
        // This bird does not panic at every noise. It stops, looks, and decides.
        if (volume >= 4.0F) {
            this.feel(UmbrellaCockatooEmotion.STARTLED, 40 + this.getRandom().nextInt(40));
            this.pendingHighPerch = true;
        } else if (volume >= 2.5F) {
            this.feel(UmbrellaCockatooEmotion.ALERT, 60 + this.getRandom().nextInt(60));
        } else {
            this.feel(UmbrellaCockatooEmotion.CURIOUS, 60 + this.getRandom().nextInt(80));
        }
        if (!wasSleeping && !this.isFlying()) {
            BudgerigarControl.setBehaviorStateFor(this, BudgerigarBehaviorState.ALERT, 40);
        }
    }

    // ------------------------------------------------------------------
    // Progressive trust
    // ------------------------------------------------------------------

    public int trustFor(Player player) {
        return this.trustByPlayer.getOrDefault(player.getUUID(), 0);
    }

    private int addTrustFor(Player player, int amount) {
        int updated = Mth.clamp(this.trustByPlayer.getOrDefault(player.getUUID(), 0) + amount,
                0, UmbrellaCockatooDefinition.TAMING_TRUST_MAX);
        this.trustByPlayer.put(player.getUUID(), updated);
        return updated;
    }

    private void tickWildTrustDecay() {
        if (this.isTame() || this.trustByPlayer.isEmpty()) {
            return;
        }
        if (++this.wildTrustDecayTicks < UmbrellaCockatooDefinition.WILD_TRUST_DECAY_INTERVAL) {
            return;
        }
        this.wildTrustDecayTicks = 0;
        this.trustByPlayer.replaceAll((uuid, value) -> Math.max(0, value - 1));
        // Zero means "no relationship": drop the entry so the map does not grow
        // for every player who ever fed or hit this bird.
        this.trustByPlayer.values().removeIf(value -> value <= 0);
    }

    // ------------------------------------------------------------------
    // Owner perch: left shoulder, right shoulder or the owner's head
    // ------------------------------------------------------------------

    public int getPerchSlot() {
        return this.entityData.get(PERCH_SLOT);
    }

    private void tickOwnerPerch() {
        if (this.ownerPerchCooldown > 0) {
            --this.ownerPerchCooldown;
        }
        if (this.ownerPerchSwapCooldown > 0) {
            --this.ownerPerchSwapCooldown;
        }
        Player owner = this.getPerchedOwner();
        if (owner == null) {
            return;
        }
        if (this.shouldLeaveOwnerPerch(owner)) {
            this.leaveOwnerPerch(owner);
            return;
        }
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.positionOnOwner(owner);
        if (this.tickCount % 100 == 0) {
            this.syncOwnerPassengers(owner);
        }
        if (this.ownerPerchSwapCooldown <= 0) {
            this.ownerPerchSwapCooldown = 120 + this.getRandom().nextInt(241);
            if (this.getRandom().nextFloat() < 0.35F) {
                this.entityData.set(PERCH_SLOT, this.pickPerchSlot(this.getPerchSlot()));
            }
        }
    }

    private boolean canPerchOnOwner(Player owner) {
        BirdCommandMode mode = this.getBirdCommandMode();
        return this.isTame() && this.isOwnedBy(owner)
                && (mode == BirdCommandMode.FREE || mode == BirdCommandMode.FOLLOW)
                && this.ownerPerchCooldown <= 0
                && !this.isVehicle() && !this.isPassenger()
                && owner.isAlive() && !owner.isSpectator() && !owner.isSleeping()
                && !owner.isPassenger() && owner.getPassengers().isEmpty()
                && !owner.isInWaterOrBubble() && !owner.isFallFlying()
                && !owner.isShiftKeyDown() && !owner.isSprinting()
                && !this.isBirdSleeping()
                && this.startledTicks <= 0
                && !BudgerigarControl.isEating(this);
    }

    /** Called by the custom goals when the bird has drifted close to its owner. */
    public boolean tryPerchOnOwner(Player owner) {
        if (!this.canPerchOnOwner(owner)) {
            return false;
        }
        if (this.getPerchSlot() == PERCH_SLOT_NONE) {
            this.entityData.set(PERCH_SLOT, this.pickPerchSlot(PERCH_SLOT_NONE));
        }
        if (!this.startRiding(owner, true)) {
            return false;
        }
        this.getNavigation().stop();
        this.setNoGravity(false);
        this.setDeltaMovement(Vec3.ZERO);
        this.positionOnOwner(owner);
        this.syncOwnerPassengers(owner);
        BudgerigarControl.setBehaviorState(this, BudgerigarBehaviorState.PERCHING);
        this.feel(UmbrellaCockatooEmotion.CURIOUS, 60);
        return true;
    }

    /** Picks a different perch slot than the one it is standing on now. */
    private int pickPerchSlot(int previous) {
        int[] options = {PERCH_SLOT_LEFT_SHOULDER, PERCH_SLOT_RIGHT_SHOULDER, PERCH_SLOT_HEAD};
        int[] candidates = new int[options.length];
        int count = 0;
        for (int option : options) {
            if (option != previous) {
                candidates[count++] = option;
            }
        }
        if (count == 0) {
            return options[0];
        }
        return candidates[this.getRandom().nextInt(count)];
    }

    @Nullable
    private Player getPerchedOwner() {
        return this.getVehicle() instanceof Player player && this.isOwnedBy(player) ? player : null;
    }

    private boolean shouldLeaveOwnerPerch(Player owner) {
        BirdCommandMode mode = this.getBirdCommandMode();
        return !this.isOwnedBy(owner)
                || (mode != BirdCommandMode.FREE && mode != BirdCommandMode.FOLLOW)
                || !owner.isAlive() || owner.isSpectator() || owner.isSleeping()
                || owner.isInWaterOrBubble() || owner.isFallFlying() || owner.isShiftKeyDown()
                || owner.isSprinting() || owner.hurtTime > 0
                || this.hurtTime > 0
                || this.startledTicks > 0;
    }

    private void leaveOwnerPerch(Player owner) {
        this.stopRiding();
        this.ownerPerchCooldown = 120;
        this.setPos(owner.getX(), owner.getEyeY() + 0.25D, owner.getZ());
        this.setNoGravity(true);
        Vec3 perch = this.findHighPerchTarget();
        this.startFlybyFlight(perch != null ? perch : owner.position().add(0.0D, 1.4D, 0.0D));
    }

    @Override
    public void removeVehicle() {
        Entity previous = this.getVehicle();
        super.removeVehicle();
        if (previous instanceof ServerPlayer serverOwner && this.getVehicle() != previous) {
            this.syncOwnerPassengers(serverOwner);
        }
    }

    @Override
    public void rideTick() {
        super.rideTick();
        Player owner = this.getPerchedOwner();
        if (owner != null) {
            this.positionOnOwner(owner);
        }
    }

    private void positionOnOwner(Player owner) {
        Vec3 perch = this.ownerPerchPosition(owner);
        this.setPos(perch.x, perch.y, perch.z);
        this.setDeltaMovement(Vec3.ZERO);
    }

    private Vec3 ownerPerchPosition(Player owner) {
        double yaw = Math.toRadians(owner.getYHeadRot());
        double sideways = switch (this.getPerchSlot()) {
            case PERCH_SLOT_LEFT_SHOULDER -> -0.26D;
            case PERCH_SLOT_RIGHT_SHOULDER -> 0.26D;
            default -> 0.0D;
        };
        double height = this.getPerchSlot() == PERCH_SLOT_HEAD ? 1.60D : 1.44D;
        double forward = this.getPerchSlot() == PERCH_SLOT_HEAD ? 0.02D : 0.07D;
        double dx = Math.cos(yaw) * sideways - Math.sin(yaw) * forward;
        double dz = -Math.sin(yaw) * sideways - Math.cos(yaw) * forward;
        return owner.position().add(dx, height, dz);
    }

    private void syncOwnerPassengers(Player owner) {
        // ServerEntity broadcasts passenger changes to tracking players but not to
        // the vehicle itself; a player carrying a bird must be told explicitly.
        if (owner instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetPassengersPacket(owner));
        }
    }

    /** A tamed bird in FREE mode roams; it only comes back when it has drifted far. */
    private void tickOwnerProximity() {
        if (!this.isTame() || this.isPassenger() || this.getBirdCommandMode() != BirdCommandMode.FREE) {
            return;
        }
        LivingEntity owner = this.getOwner();
        if (owner == null || !owner.isAlive()) {
            return;
        }
        double distance = this.distanceTo(owner);
        if (distance > 20.0D && this.tickCount % 60 == 0 && !this.isFlying()) {
            this.startFlybyFlight(owner.position().add(0.0D, 1.6D, 0.0D));
            return;
        }
        if (distance < 3.4D && owner instanceof Player player && !this.isVehicle()
                && this.tickCount % 40 == 0 && this.getRandom().nextFloat() < 0.12F) {
            this.tryPerchOnOwner(player);
        }
    }

    // ------------------------------------------------------------------
    // High perches
    // ------------------------------------------------------------------

    private void tickHighPerchPreference() {
        // A loud fright asks for an immediate retreat to the canopy instead of
        // waiting out the ordinary ground-aversion timer.
        boolean urgent = this.pendingHighPerch && this.onGround() && !this.isFlying();
        if (this.pendingHighPerch && !this.onGround()) {
            this.pendingHighPerch = false;
        }
        boolean shouldLookUp = this.onGround() && !this.isFlying() && !this.isInWaterOrBubble()
                && !this.isPassenger()
                && BirdActivitySchedule.DIURNAL.isActiveTime(this.level().getDayTime())
                && !BudgerigarControl.isEating(this);
        if (!shouldLookUp) {
            this.groundTicks = 0;
            return;
        }
        if (urgent && this.getNavigation().isDone()) {
            this.pendingHighPerch = false;
            this.groundTicks = 0;
            Vec3 urgentTarget = this.findHighPerchTarget();
            if (urgentTarget != null) {
                this.startFlybyFlight(urgentTarget);
            }
            return;
        }
        if (++this.groundTicks < GROUND_AVERSION_TICKS || !this.getNavigation().isDone()) {
            return;
        }
        this.groundTicks = 0;
        Vec3 target = this.findHighPerchTarget();
        if (target != null) {
            this.startFlybyFlight(target);
        }
    }

    /**
     * Looks for leaves, log tops or perch blocks at least two blocks up around the
     * bird, skipping spots another bird already took. Gated by the shared scan
     * budget so a group of cockatoos cannot flood the server.
     */
    @Nullable
    public Vec3 findHighPerchTarget() {
        if (!(this.level() instanceof ServerLevel serverLevel)
                || !BirdScanBudget.tryAcquire(serverLevel, this, 2)) {
            return null;
        }
        BlockPos origin = this.blockPosition();
        int radius = 10 + this.getRandom().nextInt(7);
        for (int attempt = 0; attempt < 24; ++attempt) {
            int x = origin.getX() + this.getRandom().nextInt(radius * 2 + 1) - radius;
            int z = origin.getZ() + this.getRandom().nextInt(radius * 2 + 1) - radius;
            int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (y < origin.getY() + 2 || y > origin.getY() + 16) {
                continue;
            }
            BlockPos stand = new BlockPos(x, y, z);
            BlockState below = this.level().getBlockState(stand.below());
            if (!(below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS) || below.is(BirdTags.BIRD_PERCHES))) {
                continue;
            }
            if (!this.level().getBlockState(stand).isAir() || !this.level().getBlockState(stand.above()).isAir()) {
                continue;
            }
            if (this.isPerchOccupied(stand)) {
                continue;
            }
            return Vec3.atBottomCenterOf(stand);
        }
        return null;
    }

    /** Large parrots should not all fold onto the same block. */
    private boolean isPerchOccupied(BlockPos stand) {
        List<LivingEntity> occupants = this.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(stand).inflate(1.2D),
                entity -> entity != this && entity.isAlive() && entity.getType().is(BirdTags.BIRDS));
        return !occupants.isEmpty();
    }

    // ------------------------------------------------------------------
    // Mimicry
    // ------------------------------------------------------------------

    private void tickMimicry() {
        if (--this.mimicCooldown > 0) {
            return;
        }
        if (!this.canVocalize()
                || BudgerigarControl.isEating(this) || this.isFlying() || this.startledTicks > 0
                || this.callSequence.isBusy(this.tickCount)) {
            return;
        }
        // A tame bird chatters at its owner more readily than a wild one does.
        boolean ownerNearby = this.getOwner() instanceof Player owner && this.distanceTo(owner) < 14.0D;
        this.mimicCooldown = (ownerNearby ? 260 : 340) + this.getRandom().nextInt(ownerNearby ? 300 : 380);
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(16.0D),
                entity -> entity != this && entity.isAlive() && BirdSpecies.from(entity) != null);
        if (nearby.isEmpty()) {
            // No registered voice to borrow: stay quiet rather than fake one.
            return;
        }
        LivingEntity source = nearby.get(this.getRandom().nextInt(nearby.size()));
        SoundEvent imitation = UmbrellaCockatooMimicry.imitationFor(BirdSpecies.from(source));
        if (imitation != null) {
            this.playSound(imitation, 0.7F, 0.98F + this.getRandom().nextFloat() * 0.3F);
            this.feel(UmbrellaCockatooEmotion.CURIOUS, 60);
        }
    }

    // ------------------------------------------------------------------
    // Flight, food, rendering
    // ------------------------------------------------------------------

    @Override
    public BirdFlightProfile birdFlightProfile() {
        return BirdFlightProfile.UMBRELLA_COCKATOO;
    }

    @Override
    protected double flybyInitialLift() {
        return 0.09D;
    }

    @Override
    protected TagKey<Item> foodTag() {
        return BirdTags.UMBRELLA_COCKATOO_FOODS;
    }

    @Override
    public ResourceLocation getTextureResource() {
        return UmbrellaCockatooDefinition.TEXTURE;
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.UMBRELLA_COCKATOO;
    }

    @Override
    public double getBoneResetTime() {
        // Must match the display director's residual exit hold.
        return 5.0D;
    }

    // ------------------------------------------------------------------
    // Native bouts mix short calls and phrases. Neighbour mimicry stays separate.
    // ------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        if (!this.canVocalize() || this.callSequence.isBusy(this.tickCount)) {
            return null;
        }
        return NATIVE_CALLS.get(this.getRandom().nextInt(NATIVE_CALLS.size())).get();
    }

    @Override
    protected SoundEvent getInteractionSound() {
        return NATIVE_CALLS.get(this.getRandom().nextInt(NATIVE_CALLS.size())).get();
    }

    @Override
    public void playAmbientSound() {
        if (this.canVocalize() && !this.callSequence.isBusy(this.tickCount)
                && BirdFlockSoundLimiter.allowAmbient(this)) {
            this.startNativeCalls(false, this.getSoundVolume());
        }
    }

    @Override
    protected void playInteractionSound() {
        this.startNativeCalls(true, 0.7F);
    }

    private boolean canVocalize() {
        return !this.level().isClientSide && this.isAlive() && !this.isRemoved() && !this.isSilent()
                && !this.isBirdSleeping() && !BudgerigarControl.isSleepingOrRoosting(this);
    }

    private void startNativeCalls(boolean interaction, float volume) {
        if (this.canVocalize() && this.callSequence.start(this.tickCount, interaction, this.getRandom())) {
            this.callSequenceVolume = volume;
            this.tickNativeCalls();
        }
    }

    private void tickNativeCalls() {
        if (!this.canVocalize()) {
            this.callSequence.cancel();
            return;
        }
        CockatooCallSequence.Playback playback = this.callSequence.poll(this.tickCount, this.getRandom());
        if (playback != null) {
            this.playSound(NATIVE_CALLS.get(playback.clip().ordinal()).get(),
                    this.callSequenceVolume, playback.pitch());
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        // The supplied pack contains calls, not dedicated hurt recordings.
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public float getSoundVolume() {
        return 0.9F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return BirdFlockSoundLimiter.scaledAmbientInterval(this, 150);
    }

    @Override
    public boolean canFlockWith(Entity other) {
        return other instanceof UmbrellaCockatooEntity
                || other instanceof CockatielEntity
                || other instanceof MacawEntity
                || super.canFlockWith(other);
    }

    @Nullable
    @Override
    public UmbrellaCockatooEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
        UmbrellaCockatooEntity child = GuaniaoEntityTypes.UMBRELLA_COCKATOO.get().create(level);
        if (child != null) {
            float mateScale = mate instanceof BudgerigarEntity other
                    ? other.getIndividualModelScale() : this.getIndividualModelScale();
            child.setIndividualModelScale(BirdModelScale.inheritIndividualScale(
                    child.getRandom(), this.getIndividualModelScale(), mateScale, child.modelScaleProfile()));
        }
        return child;
    }

    // ------------------------------------------------------------------
    // GeckoLib base pose; emotion deltas are sampled separately by the client.
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, this::movementController));
    }

    private <T extends UmbrellaCockatooEntity> PlayState movementController(AnimationState<T> animationState) {
        this.cockatooAnimationTick = animationState.getAnimationTick();
        animationState.getController().setAnimationSpeed(1.0D);
        animationState.getController().transitionLength(5);
        BudgerigarBehaviorState state = this.getBehaviorState();
        boolean flying = this.shouldPlayFlyAnimation();
        boolean sleeping = this.isBirdSleeping() || state == BudgerigarBehaviorState.SLEEPING
                || state == BudgerigarBehaviorState.ROOSTING;
        boolean walking = this.shouldWalk(state, animationState.isMoving());
        boolean preview = this.previewAnimation != GuidePreviewAnimation.NONE;
        boolean fullPreview = this.previewAnimation == GuidePreviewAnimation.FULL_DISPLAY;
        boolean displayAllowed = !preview && !flying && !sleeping && !walking
                && state != BudgerigarBehaviorState.EATING && !this.isPassenger()
                && this.getEmotion() != UmbrellaCockatooEmotion.ALERT
                && this.getEmotion() != UmbrellaCockatooEmotion.STARTLED;
        this.displayAnimation.update(this.cockatooAnimationTick, displayAllowed || fullPreview, fullPreview);
        if (preview) {
            return animationState.setAndContinue(fullPreview && this.displayAnimation.playsBody(this.cockatooAnimationTick)
                    ? IDLE_DIFF_2_ANIMATION : this.previewAnimation.animation);
        }
        if (flying) {
            animationState.getController().transitionLength(0);
            animationState.getController().setAnimationSpeed(this.flightAnimationSpeed());
            return BirdFlightAnimation.play(animationState, FLY_ANIMATION);
        }
        if (state == BudgerigarBehaviorState.EATING) {
            return animationState.setAndContinue(EAT_ANIMATION);
        }
        if (sleeping) {
            return animationState.setAndContinue(SLEEP_ANIMATION);
        }
        if (walking) {
            animationState.getController().setAnimationSpeed(BirdGroundAnimation.walkAnimationSpeed(this));
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        RawAnimation idle = this.pickIdleAnimation(displayAllowed);
        return animationState.setAndContinue(this.displayAnimation.playsBody(this.cockatooAnimationTick)
                ? IDLE_DIFF_2_ANIMATION : idle);
    }

    /** Visual targets are alternatives; the display reference is never added to a mood twice. */
    public int getExpressionLevel() {
        if (this.displayAnimation.holdsFullDisplay(this.cockatooAnimationTick)) return 2;
        if (this.previewAnimation != GuidePreviewAnimation.NONE) {
            return this.previewAnimation.expressionLevel;
        }
        if (this.isBirdSleeping() || BudgerigarControl.isSleepingOrRoosting(this)) {
            return 0;
        }
        return this.getEmotion().overlayLevel();
    }

    public double getCockatooAnimationTick() {
        return this.cockatooAnimationTick;
    }

    public boolean needsDisplayReference() {
        return this.displayAnimation.needsFullReference(this.cockatooAnimationTick);
    }

    private boolean shouldWalk(BudgerigarBehaviorState state, boolean animationMoving) {
        if (!BirdGroundAnimation.canPlayWalk(this)) {
            return false;
        }
        return BirdGroundAnimation.hasWalkMotion(this, animationMoving)
                || state == BudgerigarBehaviorState.WALKING
                || state == BudgerigarBehaviorState.FOLLOWING
                || state == BudgerigarBehaviorState.FORAGING;
    }

    private RawAnimation pickIdleAnimation(boolean displayAllowed) {
        if (this.level().getGameTime() >= this.nextIdleAnimationTick) {
            int roll = this.getRandom().nextInt(10);
            if (roll < 3) {
                this.currentIdleAnimation = IDLE_DIFF_1_ANIMATION;
                this.nextIdleAnimationTick = this.level().getGameTime() + 80L;
            } else if (roll < 5) {
                this.currentIdleAnimation = IDLE_ANIMATION;
                if (displayAllowed) this.displayAnimation.request(this.cockatooAnimationTick);
                this.nextIdleAnimationTick = this.level().getGameTime() + 90L;
            } else {
                this.currentIdleAnimation = IDLE_ANIMATION;
                this.nextIdleAnimationTick = this.level().getGameTime() + 70L + this.getRandom().nextInt(110);
            }
        }
        return this.currentIdleAnimation;
    }

    @Override
    public void setGuidePreviewAnimation(BudgerigarEntity.GuidePreviewAnimation animation) {
        this.previewAnimation = animation == null
                ? GuidePreviewAnimation.NONE
                : switch (animation) {
                    case NONE -> GuidePreviewAnimation.NONE;
                    case IDLE, CURIOUS -> GuidePreviewAnimation.IDLE;
                    case PREEN -> GuidePreviewAnimation.CREST_UP;
                    case DANCE -> GuidePreviewAnimation.FULL_DISPLAY;
                    case EAT -> GuidePreviewAnimation.EAT;
                    case SLEEP -> GuidePreviewAnimation.SLEEP;
                    case WALK -> GuidePreviewAnimation.WALK;
                    case FLY -> GuidePreviewAnimation.FLY;
                };
    }

    private enum GuidePreviewAnimation {
        NONE(null, 0),
        IDLE(IDLE_ANIMATION, 0),
        CREST_UP(IDLE_ANIMATION, 1),
        FULL_DISPLAY(IDLE_ANIMATION, 2),
        EAT(EAT_ANIMATION, 0),
        SLEEP(SLEEP_ANIMATION, 0),
        WALK(WALK_ANIMATION, 0),
        FLY(FLY_ANIMATION, 0);

        private final RawAnimation animation;
        private final int expressionLevel;

        GuidePreviewAnimation(RawAnimation animation, int expressionLevel) {
            this.animation = animation;
            this.expressionLevel = expressionLevel;
        }
    }
}
