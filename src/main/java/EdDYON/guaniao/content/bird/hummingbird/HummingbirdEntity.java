package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.config.HummingbirdConfig;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdHeadPerch;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.BirdVisibility;
import EdDYON.guaniao.content.bird.command.BirdCommandInteraction;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.content.bird.command.CommandableBird;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import EdDYON.guaniao.util.ItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import javax.annotation.Nullable;
import java.util.UUID;

public final class HummingbirdEntity extends TamableAnimal implements GeoEntity, FlyingAnimal, CommandableBird {
    // Lowest authored standing foot corners after waist/leg/cube bind rotations, in blocks.
    public static final double HEAD_PERCH_SOLE_Y = 0.002158524900375658D / 16.0D;
    public static final double HEAD_PERCH_SOLE_FORWARD = 0.5828997379737713D / 16.0D;
    public enum Activity {
        HOVER, TRAVEL, NECTAR_ENTER, NECTAR_LOOP, NECTAR_EXIT, TAKEOFF, LAND,
        PERCH, SLEEP_ENTER, SLEEP, WAKE, DROP_SEED, HEAD_PERCH, TERRITORY, PANIC
    }
    public static final TagKey<Item> FOODS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("guaniao", "hummingbird_foods"));
    public static final TagKey<Item> TEMPT = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("guaniao", "hummingbird_tempt_items"));
    private static final EntityDataAccessor<Integer> ACTIVITY = SynchedEntityData.defineId(HummingbirdEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COMMAND = SynchedEntityData.defineId(HummingbirdEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MALE = SynchedEntityData.defineId(HummingbirdEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> NECTAR = SynchedEntityData.defineId(HummingbirdEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BANK = SynchedEntityData.defineId(HummingbirdEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FORWARD = SynchedEntityData.defineId(HummingbirdEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BACKWARD = SynchedEntityData.defineId(HummingbirdEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LATERAL = SynchedEntityData.defineId(HummingbirdEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> IDLE_DETAIL = SynchedEntityData.defineId(HummingbirdEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> CARRY = SynchedEntityData.defineId(HummingbirdEntity.class, EntityDataSerializers.ITEM_STACK);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    final HummingbirdFlightMotor motor = new HummingbirdFlightMotor(this);
    final HummingbirdGardenBehavior behavior = new HummingbirdGardenBehavior(this);
    @Nullable private UUID tamingCandidate;
    private int tamingProgress;
    private long nextFeedTick;
    private long nextAllowedCallTick;
    private long nextInteractionCallTick;
    private float forwardBlend, previousForwardBlend, visualBank, previousVisualBank;
    private float backwardBlend, previousBackwardBlend, lateralBlend, previousLateralBlend;
    private double wingStrokeTime, previousWingStrokeTime;
    private int quietTicks, idleDetailTicks;
    @Nullable private String preview;

    public HummingbirdEntity(EntityType<? extends HummingbirdEntity> type, Level level) {
        super(type, level);
        this.moveControl = new MoveControl(this) { @Override public void tick() { } };
        this.lookControl = new LookControl(this) { @Override public void tick() { } };
        this.setNoGravity(true);
        this.entityData.set(MALE, this.random.nextBoolean());
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4).add(Attributes.MOVEMENT_SPEED, 0.15)
                .add(Attributes.FLYING_SPEED, 0.30).add(Attributes.FOLLOW_RANGE, 32);
    }
    public static boolean canSpawn(EntityType<HummingbirdEntity> type, ServerLevelAccessor level, MobSpawnType reason,
                                   BlockPos pos, RandomSource random) {
        return level.getRawBrightness(pos, 0) > 8 && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTIVITY, Activity.HOVER.ordinal()); builder.define(COMMAND, 0);
        builder.define(MALE, false); builder.define(NECTAR, 0F); builder.define(BANK, 0F);
        builder.define(FORWARD, 0F); builder.define(BACKWARD, 0F); builder.define(LATERAL, 0F);
        builder.define(IDLE_DETAIL, 0);
        builder.define(CARRY, ItemStack.EMPTY);
    }
    @Override protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override public void clientTick() {
                // Body yaw is not sent in movement packets. Use the interpolated entity
                // heading on the client, including when deliberately sidestepping a flower.
                HummingbirdEntity.this.yBodyRot = HummingbirdEntity.this.getYRot();
            }
        };
    }
    @Override protected void registerGoals() { /* All motion is owned by the hover state machine. */ }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (level() instanceof ServerLevel server && isAlive()) {
            behavior.tick(server);
            motor.tick(server);
        }
    }
    @Override public void tick() {
        previousForwardBlend = forwardBlend;
        previousBackwardBlend = backwardBlend;
        previousLateralBlend = lateralBlend;
        previousWingStrokeTime = wingStrokeTime;
        previousVisualBank = visualBank;
        super.tick();
        if (!level().isClientSide) {
            // Client motion packets are intermittent. Send the authoritative pose target,
            // then interpolate it locally instead of repeatedly restarting fly/hover clips.
            double yaw = getYRot() * Mth.DEG_TO_RAD;
            Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            Vec3 right = new Vec3(forward.z, 0, -forward.x);
            boolean flying = isAirborne() && !isNectarFeeding();
            double along = flying ? getDeltaMovement().dot(forward) / .15 : 0;
            setFlightBlend(FORWARD, Mth.clamp(along, 0, 1));
            setFlightBlend(BACKWARD, Mth.clamp(-along, 0, 1));
            setFlightBlend(LATERAL, flying ? Mth.clamp(getDeltaMovement().dot(right) / .18, -1, 1) : 0);
            tickIdleDetail();
        }
        forwardBlend = Mth.lerp(.3F, forwardBlend, entityData.get(FORWARD));
        backwardBlend = Mth.lerp(.3F, backwardBlend, entityData.get(BACKWARD));
        lateralBlend = Mth.lerp(.3F, lateralBlend, entityData.get(LATERAL));
        // Integrate frequency instead of multiplying an age clock by the current blend:
        // direction changes alter wingbeat speed without jumping its phase.
        wingStrokeTime += (1 + backwardBlend * .12) / 20.0;
        visualBank = Mth.lerp(.35F, visualBank, entityData.get(BANK));
    }
    private void setFlightBlend(EntityDataAccessor<Float> accessor, double value) {
        float quantized = Math.round(value * 20) / 20F;
        if (entityData.get(accessor) != quantized) entityData.set(accessor, quantized);
    }
    private void tickIdleDetail() {
        boolean hovering = activity() == Activity.HOVER && getDeltaMovement().lengthSqr() < .0004;
        boolean perched = activity() == Activity.PERCH && !isPassenger();
        int detail = entityData.get(IDLE_DETAIL);
        if ((!hovering && !perched) || (detail == 1 && !hovering) || (detail == 2 && !perched)) {
            quietTicks = idleDetailTicks = 0;
            entityData.set(IDLE_DETAIL, 0);
        } else if (idleDetailTicks > 0) {
            if (--idleDetailTicks == 0) { entityData.set(IDLE_DETAIL, 0); quietTicks = 0; }
        } else if (++quietTicks >= 80 && random.nextInt(240) == 0) {
            entityData.set(IDLE_DETAIL, hovering ? 1 : 2);
            idleDetailTicks = 80;
        }
    }
    @Override public void travel(Vec3 input) {
        if (isPassenger()) { setDeltaMovement(Vec3.ZERO); return; }
        if (isAirborne()) {
            setNoGravity(true);
            if (isEffectiveAi()) move(MoverType.SELF, getDeltaMovement());
            calculateEntityAnimation(false); resetFallDistance();
            return;
        }
        // An anchored plant/vine perch is explicit and revalidated by behavior, not a false grounded flag.
        if (behavior.hasAnchor()) { setNoGravity(true); setDeltaMovement(Vec3.ZERO); return; }
        setNoGravity(false);
        super.travel(Vec3.ZERO);
    }
    @Override public boolean isFlying() { return isAirborne(); }
    public boolean isAirborne() {
        if (preview != null) return !preview.equals("idle") && !preview.equals("sleep");
        return switch (activity()) {
            case PERCH, SLEEP_ENTER, SLEEP, WAKE, HEAD_PERCH -> false;
            default -> true;
        };
    }
    public boolean isSleeping() { return "sleep".equals(preview) || activity() == Activity.SLEEP || activity() == Activity.SLEEP_ENTER; }
    public boolean isWingSoundActive() {
        return preview == null && isAlive() && !isRemoved() && !isSilent() && !isPassenger()
                && isAirborne() && !isSleeping();
    }
    @Nullable @Override protected SoundEvent getAmbientSound() {
        return preview == null && !isSleeping() ? GuaniaoSoundEvents.HUMMINGBIRD_AMBIENT.get() : null;
    }
    @Nullable @Override protected SoundEvent getHurtSound(DamageSource source) {
        return preview == null ? GuaniaoSoundEvents.HUMMINGBIRD_HURT.get() : null;
    }
    @Nullable @Override protected SoundEvent getDeathSound() {
        return preview == null ? GuaniaoSoundEvents.HUMMINGBIRD_DEATH.get() : null;
    }
    @Override public int getAmbientSoundInterval() {
        // The longest call at the lowest voice pitch must finish even at the fastest configured rate.
        return Math.max(60, BirdFlockSoundLimiter.scaledAmbientInterval(this, 240));
    }
    @Override public void playAmbientSound() {
        if (!(level() instanceof ServerLevel server) || preview != null || !isAlive() || isSilent() || isSleeping()
                || BirdSoundVolume.apply(this, getSoundVolume()) <= 0) return;
        long now = server.getGameTime();
        if (now < nextAllowedCallTick || !BirdFlockSoundLimiter.allowAmbient(this)) return;
        nextAllowedCallTick = now + getAmbientSoundInterval();
        super.playAmbientSound();
    }
    @Override protected float getSoundVolume() { return .65F; }
    @Override public float getVoicePitch() { return .95F + random.nextFloat() * .1F; }
    @Override public void playSound(SoundEvent sound, float volume, float pitch) {
        float adjusted = BirdSoundVolume.apply(this, volume);
        if (adjusted > 0) {
            if (level() instanceof ServerLevel server && !isSilent()
                    && (sound == GuaniaoSoundEvents.HUMMINGBIRD_HURT.get()
                    || sound == GuaniaoSoundEvents.HUMMINGBIRD_DEATH.get()
                    || sound == GuaniaoSoundEvents.HUMMINGBIRD_INTERACT.get())) {
                // Responses take priority over the next automatic call, not over player actions.
                nextAllowedCallTick = Math.max(nextAllowedCallTick, server.getGameTime() + 24);
            }
            super.playSound(sound, adjusted, pitch);
        }
    }
    private void playInteractionSound() {
        if (!(level() instanceof ServerLevel server) || preview != null || !isAlive() || isSilent()
                || BirdSoundVolume.apply(this, getSoundVolume()) <= 0) return;
        long now = server.getGameTime();
        if (now < nextInteractionCallTick) return;
        nextInteractionCallTick = now + 24;
        playSound(GuaniaoSoundEvents.HUMMINGBIRD_INTERACT.get(), getSoundVolume(), getVoicePitch());
    }
    public boolean isNectarFeeding() { return "nectar".equals(preview) || activity() == Activity.NECTAR_LOOP || activity() == Activity.NECTAR_ENTER; }
    public double getFlightAnimationTime() { return tickCount / 20.0; }
    public double getForwardFlightBlend() {
        return getForwardFlightBlend(1);
    }
    public double getForwardFlightBlend(float partialTick) {
        if (preview != null) return "fly".equals(preview) ? 1 : 0;
        return Mth.clamp(Mth.lerp(partialTick, previousForwardBlend, forwardBlend), 0, 1);
    }
    public double getBackwardFlightBlend(float partialTick) {
        if (preview != null) return 0;
        return Mth.clamp(Mth.lerp(partialTick, previousBackwardBlend, backwardBlend), 0, 1);
    }
    public double getLateralFlightBlend(float partialTick) {
        if (preview != null) return 0;
        return Mth.clamp(Mth.lerp(partialTick, previousLateralBlend, lateralBlend), -1, 1);
    }
    public double getWingStrokeTime(float partialTick) {
        // Handbook previews advance tickCount without running live entity AI/ticks.
        if (preview != null) return getFlightAnimationTime() + partialTick / 20.0;
        return Mth.lerp(Mth.clamp(partialTick, 0, 1), previousWingStrokeTime, wingStrokeTime);
    }
    public float getNectarProgress() { return "nectar".equals(preview) ? 1F : entityData.get(NECTAR); }
    void setNectarProgress(float value) { entityData.set(NECTAR, value); }
    public float getBankAngle() { return entityData.get(BANK); }
    public float getVisualBankAngle(float partialTick) {
        return Mth.lerp(partialTick, previousVisualBank, visualBank);
    }
    void setBankAngle(float value) { entityData.set(BANK, value); }
    public boolean isMale() { return entityData.get(MALE); }
    public ItemStack getHeldGardenItem() { return entityData.get(CARRY); }
    void setHeldGardenItem(ItemStack stack) { entityData.set(CARRY, stack.copy()); }
    public Activity activity() {
        int index = entityData.get(ACTIVITY);
        return index >= 0 && index < Activity.values().length ? Activity.values()[index] : Activity.HOVER;
    }
    void activity(Activity state) { entityData.set(ACTIVITY, state.ordinal()); }
    public void setGuidePreviewAnimation(@Nullable String name) { preview = name; }
    @Override public BirdCommandMode getBirdCommandMode() { return BirdCommandMode.byId(entityData.get(COMMAND)); }
    @Override public void setBirdCommandMode(BirdCommandMode mode) {
        if (!level().isClientSide) {
            entityData.set(COMMAND, mode.ordinal());
            behavior.commandChanged();
        }
    }
    @Override public boolean isBirdEmergencyOverrideActive() { return activity() == Activity.PANIC; }
    @Override public boolean isFood(ItemStack stack) { return false; } // Repeated feeds tame; they do not start breeding.
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResult command = player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()
                ? BirdCommandInteraction.tryHandle(this, this, player, hand) : InteractionResult.PASS;
        if (command.consumesAction()) {
            playInteractionSound();
            return command;
        }
        if (isTame() && isOwnedBy(player) && player.isShiftKeyDown() && stack.is(net.minecraft.tags.ItemTags.FLOWERS)) {
            if (!level().isClientSide) {
                behavior.bindGarden((ServerLevel)level(), player.blockPosition());
                playInteractionSound();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!BirdFoodSafety.matchesClean(FOODS, stack)) return InteractionResult.PASS;
        if (isTame() && !isOwnedBy(player)) return InteractionResult.PASS;
        if (!level().isClientSide && level().getGameTime() >= nextFeedTick) {
            nextFeedTick = level().getGameTime() + 10;
            if (!isTame()) {
                if (!player.getUUID().equals(tamingCandidate)) { tamingCandidate = player.getUUID(); tamingProgress = 0; }
                tamingProgress++;
                if (tamingProgress >= HummingbirdConfig.tamingFeeds()) {
                    if (!EventHooks.onAnimalTame(this, player)) {
                        tame(player); setBirdCommandMode(BirdCommandMode.FOLLOW);
                        setPersistenceRequired(); level().broadcastEntityEvent(this, (byte)7);
                    } else tamingProgress = Math.max(0, HummingbirdConfig.tamingFeeds() - 1);
                } else level().broadcastEntityEvent(this, (byte)6);
            } else heal(1);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            playInteractionSound();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isAlive()) behavior.panic(source.getEntity());
        return hurt;
    }
    @Override public void rideTick() {
        super.rideTick();
        if (getVehicle() instanceof Player owner && isOwnedBy(owner)) {
            Vec3 at = owner.position().add(ownerHeadOffset(owner.getYHeadRot(), owner.getXRot()));
            setPos(at.x, at.y, at.z); setDeltaMovement(Vec3.ZERO); resetFallDistance();
            setYRot(owner.getYHeadRot()); yBodyRot = getYRot(); setYHeadRot(getYRot());
            setXRot(0);
        }
    }
    public Vec3 ownerHeadOffset(float headYaw, float headPitch) {
        Player owner = getVehicle() instanceof Player p ? p : getOwner() instanceof Player p ? p : null;
        return BirdHeadPerch.offset(headYaw, headPitch, HEAD_PERCH_SOLE_Y, HEAD_PERCH_SOLE_FORWARD,
                owner != null && owner.isCrouching());
    }
    /** Called after the common server interaction has validated the support, reach and permissions. */
    public boolean placeFromOwnerHead(Player owner, BlockPos support, Vec3 feet) {
        if (!(level() instanceof ServerLevel server) || !isAlive() || !isOwnedBy(owner) || getVehicle() != owner) return false;
        HummingbirdSites.Perch perch = HummingbirdSites.at(server, this, support, false);
        if (perch == null) return false;
        stopRiding();
        if (isPassenger() || getVehicle() != null) return false;
        setBirdCommandMode(BirdCommandMode.STAY);
        setPos(feet.x, feet.y, feet.z); setDeltaMovement(Vec3.ZERO); resetFallDistance();
        behavior.placeOnGround(perch);
        return true;
    }
    void syncHeadPassenger(Player owner) {
        if (owner instanceof ServerPlayer player) player.connection.send(new ClientboundSetPassengersPacket(owner));
    }
    @Override public void removeVehicle() {
        Entity old = getVehicle(); super.removeVehicle();
        if (old instanceof Player owner) syncHeadPassenger(owner);
    }
    @Override public boolean canRiderInteract() { return true; }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override protected void checkFallDamage(double distance, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) { }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return BirdVisibility.shouldRender(distance, Entity.getViewScale()); }
    @Override public boolean removeWhenFarAway(double distance) { return !isTame() && distance > BirdVisibility.TRACKING_DISTANCE_SQUARED; }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hit) {
        super.dropCustomDeathLoot(level, source, hit);
        if (!getHeldGardenItem().isEmpty()) { spawnAtLocation(getHeldGardenItem().copy()); setHeldGardenItem(ItemStack.EMPTY); }
    }
    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide) behavior.release();
        super.remove(reason);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HummingbirdCommand", getBirdCommandMode().ordinal()); tag.putBoolean("Male", isMale());
        tag.putInt("TamingProgress", tamingProgress);
        if (tamingCandidate != null) tag.putUUID("TamingCandidate", tamingCandidate);
        if (!getHeldGardenItem().isEmpty()) tag.put("GardenItem", getHeldGardenItem().save(registryAccess()));
        behavior.save(tag);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(COMMAND, BirdCommandMode.byId(tag.getInt("HummingbirdCommand")).ordinal());
        if (tag.contains("Male")) entityData.set(MALE, tag.getBoolean("Male"));
        tamingCandidate = tag.hasUUID("TamingCandidate") ? tag.getUUID("TamingCandidate") : null;
        tamingProgress = Math.max(0, Math.min(tag.getInt("TamingProgress"), HummingbirdConfig.tamingFeeds() - 1));
        ItemStack held = ItemData.load(registryAccess(), tag.getCompound("GardenItem")); held.setCount(Math.min(1, held.getCount()));
        setHeldGardenItem(held); behavior.load(tag);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Explicit enter/exit clips provide transitions; keep all three layer clocks aligned.
        controllers.add(new AnimationController<>(this, "body", 0, state -> {
            int detail = preview == null ? entityData.get(IDLE_DETAIL) : 0;
            String name = detail != 0 ? "idle_diff_" + detail
                    : preview != null ? switch (preview) { case "fly" -> "flight"; case "nectar" -> "nectar_loop"; case "sleep" -> "sleep"; default -> "idle"; }
                    : switch (activity()) {
                case NECTAR_ENTER -> "nectar_enter"; case NECTAR_LOOP -> "nectar_loop"; case NECTAR_EXIT -> "nectar_exit";
                case TAKEOFF -> "takeoff"; case LAND -> "land"; case SLEEP_ENTER -> "sleep_enter"; case SLEEP -> "sleep";
                case WAKE -> "wake"; case DROP_SEED -> "drop_seed"; case PERCH, HEAD_PERCH -> "idle";
                default -> "flight";
            };
            boolean loop = name.equals("idle") || name.equals("flight") || name.equals("nectar_loop") || name.equals("sleep");
            RawAnimation animation = RawAnimation.begin();
            return state.setAndContinue(loop ? animation.thenLoop("animation.body." + name) : animation.thenPlayAndHold("animation.body." + name));
        }));
        controllers.add(new AnimationController<>(this, "wings", 0, state -> {
            int detail = preview == null ? entityData.get(IDLE_DETAIL) : 0;
            return state.setAndContinue(detail != 0
                    ? RawAnimation.begin().thenPlayAndHold("animation.wings.idle_diff_" + detail)
                    : RawAnimation.begin().thenLoop("animation.wings." + (isAirborne() ? "fly" : "idle")));
        }));
        controllers.add(new AnimationController<>(this, "eyes", 0, state -> {
            int detail = preview == null ? entityData.get(IDLE_DETAIL) : 0;
            return state.setAndContinue(detail != 0
                    ? RawAnimation.begin().thenPlayAndHold("animation.eyes.idle_diff_" + detail)
                    : activity() == Activity.WAKE ? RawAnimation.begin().thenPlayAndHold("animation.eyes.wake")
                    : RawAnimation.begin().thenLoop("animation.eyes." + (isSleeping() ? "sleep" : "idle")));
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
}
