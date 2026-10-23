package EdDYON.guaniao.content.bird.kingfisher;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.BirdSleepWakeable;
import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.BirdVisibility;
import EdDYON.guaniao.content.bird.flight.BirdFlightAware;
import EdDYON.guaniao.content.bird.flight.BirdFlightLock;
import EdDYON.guaniao.content.bird.flight.BirdFlightProfile;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import EdDYON.guaniao.event.BirdDroppingEvents;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
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
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A wild shore hunter: trust changes its tolerance, never its ownership. */
public final class KingfisherEntity extends Animal
        implements GeoEntity, FlyingAnimal, BirdFlightAware, BirdSleepWakeable, ScalableBirdModel {
    public enum Activity { OBSERVE, TRAVEL, DIVE, EMERGE, RETURN, EAT, SLEEP, FLEE, HOVER, ALERT, SHAKE, DEFECATE, SKIM, FISH_APPROACH, FISH_CRUISE }
    private enum PerchSearchStatus { FOUND, DEFERRED, NO_VALID_PERCH }
    private record PerchCandidate(BlockPos support, double score) { }
    private static final Activity[] ACTIVITIES = Activity.values();

    private static final EntityDataAccessor<Integer> ACTIVITY = SynchedEntityData.defineId(KingfisherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> HELD_FISH = SynchedEntityData.defineId(KingfisherEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(KingfisherEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FLIGHT_PITCH = SynchedEntityData.defineId(KingfisherEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> HOVER_MODE = SynchedEntityData.defineId(KingfisherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PERCHED = SynchedEntityData.defineId(KingfisherEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.fly");
    private static final RawAnimation SWOOP = RawAnimation.begin().thenLoop("animation.swoop");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.sleep");
    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("animation.fly_idle");
    private static final RawAnimation HOVER_LOOK = RawAnimation.begin().thenPlay("animation.idle_diff_1");
    private static final RawAnimation ALERT = RawAnimation.begin().thenLoop("animation.fight_idle");
    private static final RawAnimation SHAKE = RawAnimation.begin().thenPlay("animation.idle_diff_2");
    private static final RawAnimation DEFECATE = RawAnimation.begin().thenPlay("animation.idle_diff_3");
    private static final int MAX_TRUSTED_PLAYERS = 16;
    private static final int MAX_TRUST = 8;
    private static final double HOME_RADIUS_SQR = 24.0D * 24.0D;
    private static final double HOME_SPACING_SQR = 8.0D * 8.0D;
    private static final double PERCH_SPACING_SQR = 5.0D * 5.0D;
    private static final float FISHING_CATCH_CHANCE = 0.65F;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final KingfisherFlightMotor flightMotor = new KingfisherFlightMotor(this);
    private final Map<BlockPos, Integer> failedPerches = new LinkedHashMap<>();
    private final Map<BlockPos, Integer> failedWaterExits = new LinkedHashMap<>();
    private final Map<UUID, Integer> trust = new LinkedHashMap<>();
    private final Map<UUID, Integer> witnessedFeedings = new LinkedHashMap<>();
    private List<KingfisherEntity> nearbyKingfishers = List.of();
    @Nullable private BlockPos home;
    @Nullable private BlockPos shoreWater;
    @Nullable private BlockPos perch;
    @Nullable private BlockPos landingPerch;
    @Nullable private Vec3 targetPoint;
    @Nullable private Vec3 approachPoint;
    @Nullable private Vec3 landingPoint;
    @Nullable private Vec3 perchWaitAnchor;
    @Nullable private Vec3 recoveryPoint;
    @Nullable private Vec3 waterExitPoint;
    @Nullable private Vec3 waterWaypoint;
    @Nullable private BlockPos fishingWater;
    @Nullable private BlockPos mealPerch;
    @Nullable private KingfisherFishingArc fishingArc;
    @Nullable private ItemEntity offeredFood;
    @Nullable private Vec3 hoverAnchor;
    @Nullable private Vec3 alertSource;
    @Nullable private Vec3 territoryAvoid;
    @Nullable private Vec3 skimStart;
    @Nullable private Vec3 skimEnd;
    @Nullable private Vec3 fishingApproach;
    @Nullable private Vec3 fishingCruiseOrigin;
    @Nullable private Vec3 droppingRenderOffset;
    @Nullable private String guidePreview;
    private int guidePreviewStartedTick;
    private long nextAllowedCallTick;
    private int stateTicks;
    private int fishingStep;
    private int fishingCruiseStep;
    private boolean fishingRunIn;
    private boolean fishingCatchResolved;
    private int perchWaitTicks;
    private int recoveryTicks;
    private int recoverySequence;
    private int landingAlignTicks;
    private int waterExitSearchDelay;
    private int waterExitTicks;
    private int dryExitTicks;
    private int perchSearchDelay;
    private int foodSearchDelay;
    private int huntDelay;
    private int feedDelay;
    private int threatSearchDelay;
    private int alertTicks;
    private int hoverLookDelay;
    private int hoverHoldTicks;
    private int waterTicks;
    private int separationDelay;
    private int witnessDelay;
    private int clientEatingTicks;
    private int clientDefecatingTicks;
    private int droppingRenderTick = -1;
    private float previousFlightPitch;
    private boolean nearbyGull;
    private boolean hoveringToFish;
    private boolean waterShakePending;
    private boolean droppingReleased;
    private boolean movementAnimationDirty;
    private boolean territorialAlert;
    private boolean watersidePerch;

    public KingfisherEntity(EntityType<? extends KingfisherEntity> type, Level level) {
        super(type, level);
        this.moveControl = new MoveControl(this) { @Override public void tick() { } };
        this.lookControl = new LookControl(this) { @Override public void tick() { } };
        if (!level.isClientSide)
            this.setIndividualModelScale(BirdModelScale.randomIndividualScale(this.random, this.modelScaleProfile()));
        this.huntDelay = 80 + this.random.nextInt(100);
        this.perchSearchDelay = this.random.nextInt(20);
        this.threatSearchDelay = this.random.nextInt(10);
        this.hoverLookDelay = 600 + this.random.nextInt(600);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, KingfisherDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.18D).add(Attributes.FLYING_SPEED, 0.46D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    public static boolean canSpawn(EntityType<KingfisherEntity> type, ServerLevelAccessor level,
                                   MobSpawnType reason, BlockPos pos, RandomSource random) {
        return KingfisherHabitat.canSpawn(level, reason, pos);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACTIVITY, Activity.OBSERVE.ordinal());
        this.entityData.define(HELD_FISH, ItemStack.EMPTY);
        this.entityData.define(SCALE, 1.0F);
        this.entityData.define(FLIGHT_PITCH, 0.0F);
        this.entityData.define(HOVER_MODE, 0);
        this.entityData.define(PERCHED, false);
    }

    @Override protected void registerGoals() { /* One owner for quick flight and dive movement. */ }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (this.level().isClientSide && (ACTIVITY.equals(key) || HOVER_MODE.equals(key)))
            this.movementAnimationDirty = true;
        if (this.level().isClientSide && ACTIVITY.equals(key)) {
            this.clientEatingTicks = 0;
            this.clientDefecatingTicks = 0;
            this.droppingRenderOffset = null;
        }
    }

    @Override public void tick() {
        this.previousFlightPitch = this.getFlightPitch();
        super.tick();
        if (this.level().isClientSide) {
            this.clientEatingTicks = this.isEating() ? this.clientEatingTicks + 1 : 0;
            this.clientDefecatingTicks = this.getActivity() == Activity.DEFECATE ? this.clientDefecatingTicks + 1 : 0;
        }
    }

    @Override protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override public void clientTick() { KingfisherEntity.this.yBodyRot = KingfisherEntity.this.getYRot(); }
        };
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel server) || !this.isAlive()) return;
        if (this.isPassenger()) {
            this.clearTravelPlan();
            this.clearFishingRoute();
            this.perch = null;
            this.setActivity(Activity.OBSERVE);
            this.entityData.set(PERCHED, false);
            return;
        }
        if (BirdFlightLock.isFlightDisabled(this) && !this.isInWaterOrBubble()) {
            this.clearTravelPlan();
            this.clearFishingRoute();
            if (this.perch != null && (!this.validPerch(this.perch)
                    || this.position().distanceToSqr(this.perchPoint(this.perch)) > 0.01D)) this.perch = null;
            if (this.perch != null || this.canRestOnGround()) {
                this.setActivity(this.waterShakePending ? Activity.SHAKE : !this.getHeldFish().isEmpty() ? Activity.EAT
                        : this.level().isNight() ? Activity.SLEEP : Activity.OBSERVE);
                this.stateTicks++;
                if (this.getActivity() == Activity.EAT) this.eatMeal();
                else if (this.getActivity() == Activity.SHAKE) this.shakeOffWater(server);
            } else this.setActivity(Activity.OBSERVE);
            this.entityData.set(PERCHED, this.perch != null);
            this.setNoGravity(false);
            return;
        }
        this.stateTicks++;
        this.waterExitSearchDelay = Math.max(0, this.waterExitSearchDelay - 1);
        this.perchSearchDelay = Math.max(0, this.perchSearchDelay - 1);
        this.foodSearchDelay = Math.max(0, this.foodSearchDelay - 1);
        this.huntDelay = Math.max(0, this.huntDelay - 1);
        this.feedDelay = Math.max(0, this.feedDelay - 1);
        this.alertTicks = Math.max(0, this.alertTicks - 1);
        this.hoverLookDelay = Math.max(0, this.hoverLookDelay - 1);
        this.witnessDelay = Math.max(0, this.witnessDelay - 1);
        this.separationDelay = Math.max(0, this.separationDelay - 1);
        if (this.separationDelay == 0) this.territoryAvoid = null;
        this.waterTicks = this.isInWater() ? this.waterTicks + 1 : 0;
        if (this.isInWater()) this.waterShakePending = true;
        // A home is a real shore perch, not the airborne position at spawn.
        if (this.home != null && this.tickCount % 100 == 0 && this.loaded(this.home)
                && !this.validPerch(this.home)) this.home = null;
        if (--this.threatSearchDelay <= 0) {
            int interval = Math.max(5, BirdConfigManager.threatScanInterval(BirdSpecies.KINGFISHER));
            this.threatSearchDelay = interval + this.random.nextInt(Math.max(1, interval / 4));
            if (BirdScanBudget.tryAcquire(server, this)) this.detectThreat(server);
        }
        if (this.isInWater() && this.getActivity() != Activity.SKIM && this.getActivity() != Activity.EMERGE) this.beginEmerge();
        if (this.perch != null && (this.tickCount % 20 == 0) && (!this.validPerch(this.perch)
                || this.position().distanceToSqr(this.perchPoint(this.perch)) > 0.36D)) {
            this.perch = null;
            this.watersidePerch = false;
            this.beginReturn(server);
        }
        if (this.perch != null && this.watersidePerch && this.tickCount % 80 == 0
                && !this.hasWaterView(this.perchPoint(this.perch))) {
            this.watersidePerch = false;
            this.shoreWater = null;
            this.perchSearchDelay = 0;
            if (!this.level().isNight() && this.getActivity() == Activity.OBSERVE) this.seekPerch(server);
        }
        switch (this.getActivity()) {
            case OBSERVE -> this.observe(server);
            case SLEEP -> this.sleepAtPerch(server);
            case EAT -> this.eatMeal();
            case HOVER -> this.hoverOverWater(server);
            case ALERT -> this.alertAtPerch(server);
            case SHAKE -> this.shakeOffWater(server);
            case DEFECATE -> this.defecate(server);
            case DIVE -> this.dive(server);
            case FISH_APPROACH -> this.approachFishingWater(server);
            case FISH_CRUISE -> this.cruiseFishingWater(server);
            case SKIM -> this.skimWater(server);
            case EMERGE -> this.emerge(server);
            case FLEE -> this.fleeFlight(server);
            case TRAVEL, RETURN -> this.travelToPerch(server);
        }
        this.entityData.set(PERCHED, this.perch != null);
    }

    private void observe(ServerLevel server) {
        if (this.perch == null && !this.onGround()) {
            this.beginReturn(server);
            return;
        }
        this.setDeltaMovement(Vec3.ZERO);
        if (this.perch != null && (this.home == null || !this.watersidePerch) && this.tickCount % 80 == 0)
            this.refreshPerchHabitat(server);
        if (this.waterShakePending) { this.setActivity(Activity.SHAKE); return; }
        if (!this.getHeldFish().isEmpty()) { this.setActivity(Activity.EAT); return; }
        if (this.level().isNight() && this.alertTicks == 0) {
            if (this.perch != null || this.canRestOnGround()) this.setActivity(Activity.SLEEP);
            else this.seekPerch(server);
            return;
        }
        // A budget-deferred territorial move must be retried; do not resume
        // fishing indefinitely from the branch that this bird just yielded.
        if (this.perch != null && (this.territoryAvoid != null || this.home == null)
                && !this.availablePerch(this.perchPoint(this.perch))) {
            this.seekPerch(server);
            return;
        }

        if (this.offeredFood != null) { this.approachFood(server); return; }
        if (this.foodSearchDelay == 0 && this.feedDelay == 0 && BirdScanBudget.tryAcquire(server, this)) {
            int interval = Math.max(5, BirdConfigManager.foodScanInterval(BirdSpecies.KINGFISHER));
            this.foodSearchDelay = interval + this.random.nextInt(Math.max(1, interval / 4));
            this.offeredFood = server.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(10, 4, 10),
                    item -> item.isAlive() && !item.hasPickUpDelay() && isTrustFood(item.getItem())
                            && this.loadedLine(item.position()) && this.clearLine(item.position().add(0, 0.1, 0)))
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (this.offeredFood != null) { this.approachFood(server); return; }
        }
        if (this.huntDelay == 0 && this.alertTicks == 0 && BirdScanBudget.tryAcquire(server, this, 2)) {
            this.huntDelay = Math.max(100, BirdConfigManager.foodScanInterval(BirdSpecies.KINGFISHER)) + this.random.nextInt(100);
            if (this.planWaterSkim()) {
                if (this.random.nextFloat() >= 0.15F || !this.beginHover(true)) this.beginDive(server);
                return;
            }
        }
        if (this.perch != null && this.alertTicks == 0 && this.hoverLookDelay == 0 && this.stateTicks > 100) {
            this.hoverLookDelay = 800 + this.random.nextInt(800);
            if (this.hasWaterView(this.position()) && this.beginHover(false)) return;
        }
        if (this.perch == null || this.stateTicks > 360 + (this.getId() & 255)) this.seekPerch(server);
    }

    private boolean beginHover(boolean toFish) {
        Vec3 anchor = this.position().add(0, 1.0D, 0);
        // idle_diff_1 contains small authored translations. Give the complete
        // model room before using it instead of playing it through a branch.
        if (!this.clearHoverSpace(anchor) || !this.clearLine(anchor)) return false;
        this.hoverAnchor = anchor;
        this.hoveringToFish = toFish;
        this.hoverHoldTicks = 0;
        this.perch = null;
        this.landingPerch = null;
        this.targetPoint = anchor;
        this.setActivity(Activity.HOVER);
        return true;
    }

    private boolean clearHoverSpace(Vec3 anchor) {
        if (!this.loadedLine(anchor.add(-1, -0.5D, -1)) || !this.loadedLine(anchor.add(1, 1, 1))) return false;
        return this.level().getFluidState(BlockPos.containing(anchor)).isEmpty()
                && this.level().noCollision(this, this.getBoundingBox().move(anchor.subtract(this.position())).inflate(0.65D, 0.35D, 0.65D));
    }

    private void hoverOverWater(ServerLevel server) {
        if (this.hoverAnchor == null || this.stateTicks > 140 || this.level().isNight()
                || this.hoveringToFish && (this.stateTicks == 1 || this.tickCount % 5 == 0) && !this.validFishingRoute()
                || this.tickCount % 5 == 0 && !this.clearHoverSpace(this.hoverAnchor)) {
            this.beginReturn(server);
            return;
        }
        if (this.position().distanceToSqr(this.hoverAnchor) > 0.015D) {
            this.entityData.set(HOVER_MODE, 0);
            if (!this.flyToward(this.hoverAnchor, 0.26D)) this.beginReturn(server);
            return;
        }
        this.setDeltaMovement(Vec3.ZERO);
        this.entityData.set(FLIGHT_PITCH, 0.0F);
        this.entityData.set(HOVER_MODE, this.hoveringToFish ? 1 : 2);
        if (++this.hoverHoldTicks < (this.hoveringToFish ? 30 : 84)) return;
        if (this.hoveringToFish && this.validFishingRoute()) this.beginDive(server);
        else this.beginReturn(server);
    }

    private void beginDive(ServerLevel server) {
        if (!this.validFishingRoute()) { this.cancelFishing(server); return; }
        Vec3 entry = this.fishingArc.approachStart();
        if (this.flightMotor.clearPath(this.position(), entry, false)) {
            this.perch = null;
            this.fishingApproach = null;
            this.targetPoint = entry;
            this.setActivity(Activity.FISH_APPROACH);
            return;
        }
        // Leave the supporting branch before joining the ordinary low flight.
        Vec3 towardWater = entry.subtract(this.position()).multiply(1, 0, 1).normalize();
        for (double distance : new double[]{1.1D, 1.8D}) {
            for (double rise : new double[]{0.45D, 1.0D}) {
                Vec3 departure = this.position().add(towardWater.scale(distance)).add(0, rise, 0);
                if (!this.flightMotor.clearPath(this.position(), departure, false)
                        || !this.flightMotor.clearPath(departure, entry, false)) continue;
                this.perch = null;
                this.fishingApproach = departure;
                this.targetPoint = departure;
                this.setActivity(Activity.FISH_APPROACH);
                return;
            }
        }
        this.cancelFishing(server);
    }

    private void approachFishingWater(ServerLevel server) {
        if (this.fishingArc == null || this.level().isNight() || this.isInWaterOrBubble()
                || this.stateTicks > 140 || (this.stateTicks == 1 || this.tickCount % 5 == 0) && !this.validFishingRoute()) {
            this.cancelFishing(server);
            return;
        }
        Vec3 entry = this.fishingArc.cruiseStart();
        Vec3 alignment = this.fishingArc.approachStart();
        if (this.fishingApproach != null && this.position().distanceToSqr(this.fishingApproach) < 0.016D) {
            if (!this.flightMotor.clearPath(this.position(), alignment, false)) { this.cancelFishing(server); return; }
            this.fishingApproach = null;
        }
        if (this.fishingApproach == null && !this.fishingRunIn
                && this.position().distanceToSqr(alignment) < 0.0256D
                && this.getDeltaMovement().lengthSqr() < 0.0144D) this.fishingRunIn = true;
        Vec3 heading = this.fishingArc.cruiseDirection();
        Vec3 motion = this.getDeltaMovement().multiply(1, 0, 1);
        if (this.fishingRunIn && this.position().distanceToSqr(entry) < 0.16D
                && Math.abs(this.getY() - entry.y) < 0.10D
                && motion.lengthSqr() > 0.0025D && motion.normalize().dot(heading) > 0.96D
                && this.fishingArc.hasSurfaceAt(this, this.position())) {
            // Join from the current position with the same heading, instead of snapping
            // back to the planned start after approaching it from the opposite bank.
            this.fishingCruiseOrigin = this.position();
            this.setActivity(Activity.FISH_CRUISE);
            this.fishingCruiseStep = 0;
            this.targetPoint = this.skimStart;
            this.cruiseFishingWater(server);
            return;
        }
        if (this.fishingRunIn && this.position().subtract(entry).dot(heading) > 0.6D) {
            this.cancelFishing(server);
            return;
        }
        this.targetPoint = this.fishingApproach != null ? this.fishingApproach
                : this.fishingRunIn ? entry.add(heading.scale(1.2D)) : alignment;
        KingfisherFlightMotor.Result result = this.moveFlight(this.targetPoint,
                this.fishingRunIn ? 0.20D : 0.34D, KingfisherFlightMotor.Mode.TRANSIT);
        if (result == KingfisherFlightMotor.Result.BLOCKED || result == KingfisherFlightMotor.Result.STALLED
                || result == KingfisherFlightMotor.Result.UNLOADED) this.cancelFishing(server);
    }

    private void cruiseFishingWater(ServerLevel server) {
        KingfisherFishingArc arc = this.fishingArc;
        if (arc == null || this.fishingCruiseOrigin == null || this.level().isNight() || this.isInWaterOrBubble()
                || this.stateTicks > arc.cruiseTicks() + 3 || !this.getHeldFish().isEmpty()) {
            this.cancelFishing(server);
            return;
        }
        if (this.fishingCruiseStep >= arc.cruiseTicks()) {
            // Move the first dip segment on this same tick: no stop at the water-entry point.
            this.setActivity(Activity.SKIM);
            this.fishingStep = 0;
            this.targetPoint = this.skimEnd;
            this.skimWater(server);
            return;
        }
        Vec3 previous = this.fishingCruiseOrigin.lerp(this.skimStart, this.fishingCruiseStep / (double)arc.cruiseTicks());
        Vec3 next = this.fishingCruiseOrigin.lerp(this.skimStart, (this.fishingCruiseStep + 1) / (double)arc.cruiseTicks());
        if (!arc.hasSurfaceAt(this, previous) || !arc.hasSurfaceAt(this, next)
                || !this.flightMotor.clearPath(this.position(), next, false)) {
            this.cancelFishing(server);
            return;
        }
        KingfisherFlightMotor.Result result = this.flightMotor.followFishingStep(previous, next);
        if (result != KingfisherFlightMotor.Result.MOVING && result != KingfisherFlightMotor.Result.ARRIVED) {
            this.cancelFishing(server);
            return;
        }
        this.faceTravel(this.getDeltaMovement());
        this.fishingCruiseStep++;
    }

    private void alertAtPerch(ServerLevel server) {
        this.setDeltaMovement(Vec3.ZERO);
        if (this.alertSource == null) { this.setActivity(Activity.OBSERVE); return; }
        if (this.territorialAlert && this.stateTicks >= 16) {
            this.setActivity(Activity.OBSERVE);
            this.perchSearchDelay = 0;
            this.seekPerch(server);
            return;
        }
        if (this.stateTicks >= 16) this.startFlee(this.alertSource);
    }

    private void shakeOffWater(ServerLevel server) {
        this.setDeltaMovement(Vec3.ZERO);
        if (this.perch == null && !this.onGround()) { this.beginReturn(server); return; }
        // idle_diff_2 shakes its head and wings between 0.25 and 0.75 seconds.
        if (this.stateTicks >= 6 && this.stateTicks <= 15 && this.stateTicks % 3 == 0) {
            server.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + this.getBbHeight() * 0.65D, this.getZ(),
                    4, this.getBbWidth() * 0.45D, this.getBbHeight() * 0.12D, this.getBbWidth() * 0.45D, 0.035D);
        }
        if (this.stateTicks >= 28) {
            this.waterShakePending = false;
            this.resumePerchedActivity();
        }
    }

    private void resumePerchedActivity() {
        this.setActivity(this.waterShakePending ? Activity.SHAKE : !this.getHeldFish().isEmpty() ? Activity.EAT
                : this.level().isNight() && this.alertTicks == 0 && (this.perch != null || this.canRestOnGround())
                ? Activity.SLEEP : Activity.OBSERVE);
    }

    public boolean tryStartDefecation() {
        if (this.level().isClientSide || !this.isAlive() || this.isInWaterOrBubble() || this.isPassenger()
                || this.getActivity() != Activity.OBSERVE || this.perch == null && !this.onGround()
                || this.alertTicks > 0 || this.waterShakePending || !this.getHeldFish().isEmpty()) return false;
        this.droppingReleased = false;
        this.setDeltaMovement(Vec3.ZERO);
        this.setActivity(Activity.DEFECATE);
        return true;
    }

    private void defecate(ServerLevel server) {
        this.setDeltaMovement(Vec3.ZERO);
        if (this.perch == null && !this.onGround()) { this.beginReturn(server); return; }
        if (this.stateTicks >= 13 && !this.droppingReleased) {
            this.droppingReleased = true;
            BirdDroppingEvents.releaseKingfisherDropping(server, this);
        }
        if (this.stateTicks >= 23) this.resumePerchedActivity();
    }

    private void sleepAtPerch(ServerLevel server) {
        this.setDeltaMovement(Vec3.ZERO);
        if (this.perch == null && !this.canRestOnGround()) { this.beginReturn(server); return; }
        if (!this.level().isNight() || this.alertTicks > 0) this.setActivity(Activity.OBSERVE);
    }

    private boolean canRestOnGround() {
        return this.onGround() && !this.isInWaterOrBubble()
                && this.validPerch(BlockPos.containing(this.getX(), this.getY() - 0.05D, this.getZ()));
    }

    private boolean planGroundTouchdown() {
        if (this.isInWaterOrBubble() || this.isPassenger() || this.getDeltaMovement().lengthSqr() > 0.0225D) return false;
        // Flight uses no gravity and stops just above support, so onGround can
        // remain false. Inspect only the column under the feet, not a new scan.
        BlockPos column = this.blockPosition();
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos support = column.below(dy);
            if (!this.validPerch(support)) continue;
            Vec3 landing = this.perchPoint(support);
            double gap = this.getY() - landing.y;
            boolean nearSurface = gap >= -0.015D && gap <= 0.35D;
            // On a stair's lower tread the normal perch is half a block higher.
            // Reach that safe top via the verified landing route, not an upward snap.
            boolean lowerStep = gap >= -0.6D && gap < -0.015D
                    && !this.flightMotor.clearPath(this.position(), this.position().add(0, -0.35D, 0), false);
            if ((!nearSurface && !lowerStep) || nearSurface
                    && !this.flightMotor.clearPath(this.position(), landing, false)) continue;
            // Use the normal landing motor and post-move confirmation. Merely
            // finding a nearby block must not make a hovering bird "perched".
            if (nearSurface) this.failedPerches.remove(support);
            if (this.planLanding(support)) return true;
        }
        return false;
    }

    private void eatMeal() {
        this.setDeltaMovement(Vec3.ZERO);
        if (this.isInWaterOrBubble() || (this.perch == null ? !this.canRestOnGround()
                : !this.validPerch(this.perch) || this.position().distanceToSqr(this.perchPoint(this.perch)) > 0.04D)) {
            if (this.level() instanceof ServerLevel server) this.beginReturn(server);
            return;
        }
        if (this.stateTicks >= 50) {
            this.entityData.set(HELD_FISH, ItemStack.EMPTY);
            this.mealPerch = null;
            this.heal(1.0F);
            this.huntDelay = this.isPeakFeedingTime() ? 600 + this.random.nextInt(301)
                    : 900 + this.random.nextInt(601);
            this.huntDelay = Math.max(this.huntDelay, BirdConfigManager.foodScanInterval(BirdSpecies.KINGFISHER));
            this.setActivity(Activity.OBSERVE);
        }
    }

    private boolean isPeakFeedingTime() {
        long time = Math.floorMod(this.level().getDayTime(), 24000L);
        return time < 2000L || time >= 10000L && time < 13000L;
    }

    private void approachFood(ServerLevel server) {
        if (this.offeredFood == null || !this.offeredFood.isAlive() || !isTrustFood(this.offeredFood.getItem())
                || this.distanceToSqr(this.offeredFood) > 196 || this.feedDelay > 0
                || this.getActivity() == Activity.TRAVEL && this.stateTicks > 120) {
            this.offeredFood = null;
            this.targetPoint = null;
            this.foodSearchDelay = 80;
            this.beginReturn(server);
            return;
        }
        if (this.distanceToSqr(this.offeredFood) <= 0.8D) {
            // The item may have moved behind a wall since the budgeted search.
            if (!this.canSeePoint(this.offeredFood.position().add(0, 0.1D, 0))) {
                this.offeredFood = null;
                this.foodSearchDelay = Math.max(80, BirdConfigManager.foodScanInterval(BirdSpecies.KINGFISHER));
                this.beginReturn(server);
                return;
            }
            ItemEntity food = this.offeredFood;
            ItemStack held = food.getItem().copy();
            held.setCount(1);
            this.entityData.set(HELD_FISH, held);
            ItemStack remaining = food.getItem().copy();
            remaining.shrink(1);
            if (remaining.isEmpty()) food.discard(); else food.setItem(remaining);
            if (food.getOwner() instanceof Player player && !player.isSprinting() && this.distanceToSqr(player) <= 400) {
                this.addTrust(player.getUUID(), 1);
                this.nearbyKingfishers.stream().filter(bird -> bird.isAlive() && !bird.isSleeping()
                                && bird.witnessDelay == 0 && bird.alertTicks == 0 && bird.distanceToSqr(this) < 64
                                && bird.canSeePoint(food.position().add(0, 0.1D, 0)) && bird.canSeePoint(player.getEyePosition()))
                        .sorted(Comparator.comparingDouble(this::distanceToSqr)).limit(2)
                        .forEach(bird -> bird.witnessFeeding(player.getUUID()));
            }
            this.feedDelay = 200;
            this.offeredFood = null;
            this.beginReturn(server);
            return;
        }
        this.perch = null;
        this.setActivity(Activity.TRAVEL);
        this.targetPoint = this.offeredFood.position().add(0, 0.15D, 0);
        if (!this.flyToward(this.targetPoint, 0.30D)) {
            this.offeredFood = null;
            this.foodSearchDelay = Math.max(this.foodSearchDelay, 80);
            this.beginReturn(server);
        }
    }

    /** Reserve an open-water dip and a dry return perch. No fish entities are queried. */
    private boolean planWaterSkim() {
        this.clearFishingRoute();
        if (!this.getHeldFish().isEmpty()) return false;
        BlockPos returnPerch = this.perch != null && this.validPerch(this.perch) ? this.perch : this.home;
        if (returnPerch == null || !this.validPerch(returnPerch)) return false;
        if (this.shoreWater == null || !KingfisherHabitat.isOpenWater(this.level(), this.shoreWater)
                || this.blockPosition().distSqr(this.shoreWater) > 144)
            this.shoreWater = KingfisherHabitat.findWater(this.level(), this.blockPosition());
        if (this.shoreWater == null) return false;
        int first = this.random.nextInt(81);
        for (int attempt = 0; attempt < 36; attempt++) {
            int index = (first + attempt * 17) % 81;
            BlockPos water = this.shoreWater.offset(index % 9 - 4, 0, index / 9 - 4);
            if (!KingfisherHabitat.isOpenWater(this.level(), water)
                    || !this.availableFishingWater(water)
                    || !KingfisherHabitat.hasWaterView(this.level(), this.getEyePosition(), water)) continue;
            Vec3 center = Vec3.atBottomCenterOf(water).add(0,
                    this.level().getFluidState(water).getHeight(this.level(), water), 0);
            Vec3 approach = center.subtract(this.position()).multiply(1, 0, 1);
            if (approach.lengthSqr() < 0.04D) approach = this.getLookAngle().multiply(1, 0, 1);
            if (approach.lengthSqr() < 0.04D) approach = new Vec3(1, 0, 0);
            approach = approach.normalize();
            for (int direction = 0; direction < 4; direction++) {
                Vec3 along = switch (direction) {
                    case 1 -> new Vec3(-approach.z, 0, approach.x);
                    case 2 -> new Vec3(approach.z, 0, -approach.x);
                    case 3 -> approach.scale(-1);
                    default -> approach;
                };
                KingfisherFishingArc arc = KingfisherFishingArc.create(center, along);
                if (arc == null) continue;
                Vec3 start = arc.start();
                Vec3 end = arc.end();
                if (!this.canSeePoint(start.add(0, 0.15D, 0))) continue;
                if (!arc.isCruiseClear(this, this.flightMotor) || !arc.isClear(this, this.flightMotor)) continue;
                this.fishingWater = water.immutable();
                this.fishingArc = arc;
                this.fishingStep = 0;
                this.mealPerch = returnPerch.immutable();
                this.skimStart = start;
                this.skimEnd = end;
                return true;
            }
        }
        return false;
    }

    private boolean availableFishingWater(BlockPos water) {
        for (KingfisherEntity bird : this.nearbyKingfishers) {
            if (!bird.isAlive() || bird.isRemoved() || bird.fishingWater == null) continue;
            if (water.distSqr(bird.fishingWater) < 25.0D) return false;
        }
        return true;
    }

    private boolean validFishingRoute() {
        return this.fishingWater != null && this.fishingArc != null && this.skimStart != null && this.skimEnd != null
                && this.loadedLine(this.skimStart) && this.loadedLine(this.skimEnd)
                && this.fishingArc.isCruiseClear(this, this.flightMotor) && this.fishingArc.isClear(this, this.flightMotor);
    }

    private void clearFishingRoute() {
        this.fishingWater = null;
        this.fishingArc = null;
        this.fishingStep = 0;
        this.fishingCruiseStep = 0;
        this.fishingCruiseOrigin = null;
        this.fishingRunIn = false;
        this.fishingCatchResolved = false;
        this.skimStart = null;
        this.skimEnd = null;
        this.fishingApproach = null;
        if (this.getHeldFish().isEmpty()) this.mealPerch = null;
    }

    private void cancelFishing(ServerLevel server) {
        this.clearFishingRoute();
        if (this.getHeldFish().isEmpty()) this.mealPerch = null;
        this.huntDelay = Math.max(this.huntDelay, 100);
        if (this.isInWater()) this.beginEmerge(); else this.beginReturn(server);
    }

    private void dive(ServerLevel server) {
        if (this.skimStart == null || this.level().isNight() || this.isInWater() || this.stateTicks > 90
                || (this.stateTicks == 1 || this.tickCount % 5 == 0) && !this.validFishingRoute()) {
            this.cancelFishing(server);
            return;
        }
        if (this.position().distanceToSqr(this.skimStart) < 0.0064D
                && Math.abs(this.getY() - this.skimStart.y) < 0.05D && Math.abs(this.getDeltaMovement().y) < 0.045D) {
            if (!this.validFishingRoute()) { this.cancelFishing(server); return; }
            this.setActivity(Activity.SKIM);
            this.setDeltaMovement(Vec3.ZERO);
            this.fishingStep = 0;
            this.targetPoint = this.skimEnd;
            return;
        }
        if (!this.flyToward(this.skimStart, 0.56D)) this.cancelFishing(server);
    }

    private void skimWater(ServerLevel server) {
        KingfisherFishingArc arc = this.fishingArc;
        if (arc == null || this.level().isNight() || this.stateTicks > arc.durationTicks() + 6
                || !this.fishingCatchResolved && !this.getHeldFish().isEmpty()) {
            this.cancelFishing(server);
            return;
        }
        Vec3 previous = arc.pointAt(this.fishingStep / (double)arc.durationTicks());
        if (this.position().distanceToSqr(previous) > 0.0144D || !arc.hasSurfaceAt(this, previous)) {
            this.cancelFishing(server);
            return;
        }
        if (this.fishingStep >= arc.durationTicks()) {
            if (this.isInWaterOrBubble()) { this.cancelFishing(server); return; }
            if (this.getHeldFish().isEmpty()) {
                this.huntDelay = Math.max(80, BirdConfigManager.foodScanInterval(BirdSpecies.KINGFISHER)) + this.random.nextInt(41);
            }
            this.waterShakePending = true;
            this.beginReturn(server);
            return;
        }
        Vec3 next = arc.pointAt((this.fishingStep + 1) / (double)arc.durationTicks());
        if (!arc.hasSurfaceAt(this, next)) { this.cancelFishing(server); return; }
        KingfisherFlightMotor.Result result = this.flightMotor.followFishingStep(previous, next);
        if (result != KingfisherFlightMotor.Result.MOVING && result != KingfisherFlightMotor.Result.ARRIVED) {
            this.cancelFishing(server);
            return;
        }
        // Roll once at the submerged low point, allowing the tracked client's
        // interpolated dive to enter the water before the catch becomes visible.
        // A miss still completes the full dip and return.
        if (!this.fishingCatchResolved && this.fishingStep >= arc.durationTicks() / 2
                && this.isInWater() && this.getY() < arc.surfaceY() - 0.04D) {
            this.fishingCatchResolved = true;
            if (this.random.nextFloat() < FISHING_CATCH_CHANCE) {
                // A visual meal only; no fish entity is harmed and no item is spawned.
                this.entityData.set(HELD_FISH, new ItemStack(this.random.nextBoolean() ? Items.COD : Items.SALMON));
            }
        }
        this.faceTravel(this.getDeltaMovement());
        // Keep the bill dipped through the catch, then lift it smoothly on exit.
        // The shallow arc alone levels the body while its bill is still above water.
        float exitProgress = Mth.clamp((this.fishingStep - (arc.durationTicks() - 4)) / 4.0F, 0.0F, 1.0F);
        exitProgress = exitProgress * exitProgress * (3.0F - 2.0F * exitProgress);
        float travelPitch = this.entityData.get(FLIGHT_PITCH);
        this.entityData.set(FLIGHT_PITCH, Math.max(travelPitch, Mth.lerp(exitProgress, 30.0F, travelPitch)));
        if ((previous.y > arc.surfaceY()) != (next.y > arc.surfaceY())) {
            server.sendParticles(ParticleTypes.SPLASH, next.x, arc.surfaceY() + 0.02D, next.z,
                    5, 0.12D, 0.025D, 0.12D, 0.02D);
            server.sendParticles(ParticleTypes.BUBBLE_POP, next.x, arc.surfaceY(), next.z,
                    3, 0.10D, 0.02D, 0.10D, 0.01D);
        }
        this.fishingStep++;
    }

    private void beginEmerge() {
        this.clearFishingRoute();
        // An unreachable thrown meal must not hijack the return flight and send
        // the bird straight back into the water after every escape attempt.
        this.offeredFood = null;
        this.foodSearchDelay = Math.max(this.foodSearchDelay,
                Math.max(100, BirdConfigManager.foodScanInterval(BirdSpecies.KINGFISHER)));
        this.perch = null;
        this.clearTravelPlan();
        this.setActivity(Activity.EMERGE);
        this.waterExitPoint = null;
        this.waterWaypoint = null;
        this.waterExitSearchDelay = 0;
        this.waterExitTicks = 0;
        this.dryExitTicks = 0;
        if (!this.isInWater() && this.flightMotor.clearPath(this.position(), this.position().add(0, 0.9D, 0), false))
            this.waterExitPoint = this.position().add(0, 0.9D, 0);
    }

    private void emerge(ServerLevel server) {
        boolean clearAir = !this.isInWaterOrBubble()
                && this.flightMotor.clearPath(this.position(), this.position().add(0, 0.25D, 0), false);
        this.dryExitTicks = clearAir ? this.dryExitTicks + 1 : 0;
        if (this.dryExitTicks >= 8 && (this.waterExitPoint == null || this.getY() >= this.waterExitPoint.y - 0.15D)) {
            this.beginReturn(server);
            return;
        }
        if (this.waterExitPoint == null && this.waterExitSearchDelay == 0 && BirdScanBudget.tryAcquire(server, this, 2)) {
            this.waterExitSearchDelay = 30;
            this.planWaterExit();
        }
        if (this.waterWaypoint != null && this.position().distanceToSqr(this.waterWaypoint) < 0.06D) {
            this.waterWaypoint = null;
            this.flightMotor.reset();
        }
        Vec3 goal = this.waterWaypoint != null ? this.waterWaypoint : this.waterExitPoint;
        if (goal != null) {
            this.targetPoint = goal;
            KingfisherFlightMotor.Result result = this.moveFlight(goal, 0.30D, KingfisherFlightMotor.Mode.EMERGE);
            if (++this.waterExitTicks > 100 || result == KingfisherFlightMotor.Result.BLOCKED
                    || result == KingfisherFlightMotor.Result.STALLED || result == KingfisherFlightMotor.Result.UNLOADED) {
                if (this.waterExitPoint != null) this.rememberFailure(this.failedWaterExits, BlockPos.containing(this.waterExitPoint), 160);
                this.waterExitPoint = null;
                this.waterWaypoint = null;
                this.targetPoint = null;
                this.waterExitTicks = 0;
                this.waterExitSearchDelay = Math.max(this.waterExitSearchDelay, 10);
                this.flightMotor.reset();
            }
            return;
        }
        // A covered water column is not an exit. Wait for the next bounded
        // search without restarting EMERGE or repeatedly pushing into its roof.
        Vec3 rise = new Vec3(0, 0.10D, 0);
        this.setDeltaMovement(this.flightMotor.clearPath(this.position(), this.position().add(rise), true) ? rise : Vec3.ZERO);
        this.faceTravel(this.getDeltaMovement());
    }

    private void planWaterExit() {
        BlockPos origin = this.blockPosition();
        for (int radius : new int[]{0, 2, 4, 6, 8}) {
            for (int direction = 0; direction < (radius == 0 ? 1 : 8); direction++) {
                double angle = direction * Math.PI / 4;
                int x = origin.getX() + (int)Math.round(Math.cos(angle) * radius);
                int z = origin.getZ() + (int)Math.round(Math.sin(angle) * radius);
                for (int dy = 0; dy <= 12; dy++) {
                    BlockPos water = new BlockPos(x, origin.getY() + dy, z);
                    if (!this.loaded(water) || !this.loaded(water.above(2))) break;
                    if (!this.level().getFluidState(water).is(FluidTags.WATER)
                            || !this.level().getFluidState(water.above()).isEmpty()) continue;
                    Vec3 exit = Vec3.atBottomCenterOf(water).add(0, 1.4D, 0);
                    if (this.failedWaterExits.getOrDefault(BlockPos.containing(exit), 0) > this.tickCount
                            || !this.flightMotor.clearPath(exit, exit.add(0, 0.25D, 0), false)) continue;
                    Vec3 under = new Vec3(exit.x, Math.min(this.getY(), water.getY() + 0.1D), exit.z);
                    boolean direct = this.flightMotor.clearPath(this.position(), exit, true);
                    if (!direct && (!this.flightMotor.clearPath(this.position(), under, true)
                            || !this.flightMotor.clearPath(under, exit, true))) continue;
                    this.waterExitPoint = exit;
                    this.waterWaypoint = direct ? null : under;
                    this.waterExitTicks = 0;
                    this.flightMotor.reset();
                    return;
                }
            }
        }
        Vec3 upward = this.position().add(0, 2.5D, 0);
        if (this.flightMotor.clearPath(this.position(), upward, true)) {
            this.waterWaypoint = upward;
            this.waterExitTicks = 0;
            this.flightMotor.reset();
            return;
        }
        // If every nearby opening is covered, explore a short clear underwater
        // corridor before the next search. No new chunk or unbounded flood fill.
        double baseAngle = this.getId() * 2.399963D + this.recoverySequence++ * 0.72D;
        for (int i = 0; i < 8; i++) {
            double angle = baseAngle + i * Math.PI / 4;
            Vec3 candidate = this.position().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3);
            if (this.flightMotor.clearPath(this.position(), candidate, true)) {
                this.waterWaypoint = candidate;
                this.waterExitTicks = 0;
                this.flightMotor.reset();
                return;
            }
        }
    }

    private void beginReturn(ServerLevel server) {
        this.setActivity(Activity.RETURN);
        this.clearTravelPlan();
        if (this.alertTicks == 0 && !this.getHeldFish().isEmpty() && this.mealPerch != null
                && this.planLanding(this.mealPerch)) return;
        if (this.alertTicks == 0 && this.home != null && this.validPerch(this.home)) {
            Vec3 point = this.perchPoint(this.home);
            if ((this.hasWaterView(point) || this.level().isNight() || !this.getHeldFish().isEmpty())
                    && this.planLanding(this.home)) return;
        }
        if (this.planGroundTouchdown()) return;
        if (this.seekPerch(server) != PerchSearchStatus.FOUND && this.canRestOnGround()) this.setActivity(Activity.OBSERVE);
    }

    private void travelToPerch(ServerLevel server) {
        if (this.offeredFood != null && this.getActivity() == Activity.TRAVEL) { this.approachFood(server); return; }
        if (this.landingPerch != null && (this.landingPoint == null || this.tickCount % 5 == 0
                && (!this.validPerch(this.landingPerch) || !this.availablePerch(this.landingPoint)))) this.failLanding();
        if (this.landingPerch == null || this.landingPoint == null) {
            if (!this.planGroundTouchdown()) {
                PerchSearchStatus search = this.seekPerch(server);
                if (search != PerchSearchStatus.FOUND) { this.waitForPerch(search); return; }
            }
        }
        if (this.approachPoint != null) {
            this.targetPoint = this.approachPoint;
            if (this.position().distanceToSqr(this.approachPoint) < 0.025D
                    && this.getDeltaMovement().lengthSqr() < 0.0225D) {
                this.approachPoint = null;
                this.targetPoint = this.landingPoint;
                this.flightMotor.reset();
            }
        } else this.targetPoint = this.landingPoint;
        if (this.approachPoint == null && this.position().distanceToSqr(this.landingPoint) < 0.0121D
                && this.getDeltaMovement().lengthSqr() < 0.0121D) {
            Vec3 before = this.position();
            this.move(MoverType.SELF, this.landingPoint.subtract(before));
            // move can be shortened by collision. Confirm the actual feet and
            // support instead of assuming the requested last step succeeded.
            if (this.validPerch(this.landingPerch) && this.availablePerch(this.landingPoint)
                    && this.position().distanceToSqr(this.landingPoint) < 0.0016D
                    && Math.abs(this.getY() - this.landingPoint.y) < 0.035D) {
                this.perch = this.landingPerch;
                this.refreshPerchHabitat(server);
                this.clearTravelPlan();
                this.setDeltaMovement(Vec3.ZERO);
                this.resumePerchedActivity();
                return;
            }
            if (++this.landingAlignTicks >= 3) { this.failLanding(); return; }
        }
        KingfisherFlightMotor.Result result = this.moveFlight(this.targetPoint,
                this.approachPoint == null ? 0.36D : 0.42D,
                this.approachPoint == null ? KingfisherFlightMotor.Mode.LANDING : KingfisherFlightMotor.Mode.TRANSIT);
        if (result == KingfisherFlightMotor.Result.BLOCKED || result == KingfisherFlightMotor.Result.STALLED
                || result == KingfisherFlightMotor.Result.UNLOADED || this.stateTicks > 240) this.failLanding();
    }

    private void clearTravelPlan() {
        this.landingPerch = null;
        this.landingPoint = null;
        this.approachPoint = null;
        this.targetPoint = null;
        this.recoveryPoint = null;
        this.perchWaitAnchor = null;
        this.perchWaitTicks = 0;
        this.recoveryTicks = 0;
        this.landingAlignTicks = 0;
        this.flightMotor.reset();
    }

    private void failLanding() {
        if (this.landingPerch != null) this.rememberFailure(this.failedPerches, this.landingPerch, 240);
        this.clearTravelPlan();
        this.perchSearchDelay = Math.max(this.perchSearchDelay, 20);
        this.stateTicks = 0;
    }

    private void rememberFailure(Map<BlockPos, Integer> failures, BlockPos pos, int duration) {
        failures.entrySet().removeIf(entry -> entry.getValue() <= this.tickCount);
        failures.remove(pos);
        if (failures.size() >= 8) failures.remove(failures.keySet().iterator().next());
        failures.put(pos.immutable(), this.tickCount + duration);
    }

    private boolean planLanding(BlockPos support) {
        if (!this.validPerch(support) || this.failedPerches.getOrDefault(support, 0) > this.tickCount) return false;
        Vec3 landing = this.perchPoint(support);
        if (!this.availablePerch(landing)) return false;
        Vec3 approach = null;
        boolean needsTakeoffClearance = landing.subtract(this.position()).horizontalDistanceSqr() > 2.25D
                && landing.y < this.getY() + 0.75D
                && !this.flightMotor.clearPath(this.position(), this.position().add(0, -0.4D, 0), false);
        boolean needsStepClearance = landing.y > this.getY() + 0.15D
                && landing.subtract(this.position()).horizontalDistanceSqr() <= 2.25D
                && !this.flightMotor.clearPath(this.position(), this.position().add(0, -0.4D, 0), false);
        if (needsTakeoffClearance || needsStepClearance) {
            for (double height : new double[]{1.0D, 1.5D}) {
                Vec3 candidate = this.position().add(0, height, 0);
                if (this.flightMotor.clearPath(this.position(), candidate, false)
                        && this.flightMotor.clearPath(candidate, landing, false)) { approach = candidate; break; }
            }
        }
        if (approach == null && (needsTakeoffClearance || needsStepClearance
                || !this.flightMotor.clearPath(this.position(), landing, false))) {
            for (double height : new double[]{1.25D, 2.5D}) {
                Vec3 candidate = landing.add(0, height, 0);
                if (this.flightMotor.clearPath(this.position(), candidate, false)
                        && this.flightMotor.clearPath(candidate, landing, false)) { approach = candidate; break; }
            }
            if (approach == null) {
                this.rememberFailure(this.failedPerches, support, 160);
                return false;
            }
        }
        this.clearTravelPlan();
        this.perch = null;
        this.landingPerch = support.immutable();
        this.landingPoint = landing;
        this.approachPoint = approach;
        this.targetPoint = approach == null ? landing : approach;
        this.stateTicks = 0;
        return true;
    }

    private void waitForPerch(PerchSearchStatus search) {
        if (this.canRestOnGround()) {
            this.clearTravelPlan();
            this.setDeltaMovement(Vec3.ZERO);
            this.setActivity(Activity.OBSERVE);
            return;
        }
        if (this.perchWaitAnchor == null) this.perchWaitAnchor = this.position();
        this.perchWaitTicks++;
        // A denied scan says nothing about the ground below. Hold briefly, then
        // use bounded, dry local legs while the next budgeted perch search waits.
        if (this.recoveryPoint == null && this.perchWaitTicks < (search == PerchSearchStatus.DEFERRED ? 40 : 15)) {
            this.moveFlight(this.perchWaitAnchor, 0.18D, KingfisherFlightMotor.Mode.HOVER);
            return;
        }
        if (this.recoveryPoint == null && this.recoveryTicks-- <= 0) {
            this.recoveryTicks = 25;
            double baseAngle = this.getId() * 2.399963D + this.recoverySequence++ * 0.72D;
            // Over water, gain clearance and leave the crowded patch. Searching
            // a wider bank is useless if recovery only circles at surface height.
            BlockPos below = this.blockPosition().below();
            boolean overWater = this.loaded(below) && this.level().getFluidState(below).is(FluidTags.WATER);
            double descent = overWater ? 1.2D : (this.recoverySequence & 1) == 0 ? -1.0D : 0;
            BlockPos refuge = !this.getHeldFish().isEmpty() && this.mealPerch != null ? this.mealPerch : this.home;
            if (refuge != null) {
                Vec3 toward = Vec3.atCenterOf(refuge).subtract(this.position());
                baseAngle = Math.atan2(toward.z, toward.x);
            } else if (this.territoryAvoid != null) {
                Vec3 away = this.position().subtract(this.territoryAvoid);
                baseAngle = Math.atan2(away.z, away.x);
            }
            for (int i = 0; i < 8; i++) {
                double angle = baseAngle + i * Math.PI / 4;
                Vec3 candidate = this.position().add(Math.cos(angle) * 5, descent, Math.sin(angle) * 5);
                if (this.flightMotor.clearPath(this.position(), candidate, false)) {
                    this.recoveryPoint = candidate;
                    this.flightMotor.reset();
                    this.recoveryTicks = 0;
                    break;
                }
            }
        }
        if (this.recoveryPoint != null) {
            KingfisherFlightMotor.Result result = this.moveFlight(this.recoveryPoint, 0.28D, KingfisherFlightMotor.Mode.TRANSIT);
            if (++this.recoveryTicks > 45 || result != KingfisherFlightMotor.Result.MOVING
                    || this.position().distanceToSqr(this.recoveryPoint) < 0.08D) {
                this.recoveryPoint = null;
                this.recoveryTicks = 10;
                this.flightMotor.reset();
            }
        } else this.setDeltaMovement(Vec3.ZERO);
    }

    private void fleeFlight(ServerLevel server) {
        if (this.targetPoint == null || this.stateTicks > 65 || this.position().distanceToSqr(this.targetPoint) < 1.0D) {
            this.beginReturn(server);
            return;
        }
        if (!this.flyToward(this.targetPoint, 0.55D)) this.beginReturn(server);
    }

    private PerchSearchStatus seekPerch(ServerLevel server) {
        if (this.perchSearchDelay > 0 || !BirdScanBudget.tryAcquire(server, this, 3)) return PerchSearchStatus.DEFERRED;
        this.perchSearchDelay = this.perch == null ? 35 + this.random.nextInt(25) : 100 + this.random.nextInt(80);
        if (this.shoreWater == null || this.blockPosition().distSqr(this.shoreWater) > 196
                || !KingfisherHabitat.isOpenWater(this.level(), this.shoreWater))
            this.shoreWater = KingfisherHabitat.findWater(this.level(), this.blockPosition());
        List<PerchCandidate> candidates = new ArrayList<>(8);
        BlockPos center = this.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        double phase = this.getId() * 2.399963D + this.random.nextDouble() * 0.4D;
        int[] searchRadii = {3, 8, 15, 23};
        // A territory guides ordinary perched roaming. It must not prevent an
        // airborne or displaced bird from accepting a safe local shore.
        boolean stayInTerritory = this.perch != null && this.home != null && this.loaded(this.home)
                && this.separationDelay == 0
                && horizontalDistanceSqr(this.position(), Vec3.atCenterOf(this.home)) <= HOME_RADIUS_SQR;
        for (int attempt = 0; attempt < 80; attempt++) {
            // Keep the same bounded number of columns while sampling both the
            // nearby footholds and the opposite/wider banks, rather than +/-9.
            double radius = attempt < 32 ? searchRadii[attempt / 8]
                    : 6 + this.random.nextDouble() * 18;
            double angle = attempt < 32 ? phase + (attempt % 8) * Math.PI / 4
                    : phase + attempt * 2.399963D;
            int x = center.getX() + (int)Math.round(Math.cos(angle) * radius);
            int z = center.getZ() + (int)Math.round(Math.sin(angle) * radius);
            for (int dy = 5; dy >= -6; dy--) {
                cursor.set(x, center.getY() + dy, z);
                if (!this.validPerch(cursor) || this.failedPerches.getOrDefault(cursor, 0) > this.tickCount) continue;
                Vec3 point = this.perchPoint(cursor);
                if (!this.availablePerch(point) || !this.loadedLine(point)) continue;
                if (stayInTerritory
                        && horizontalDistanceSqr(point, Vec3.atCenterOf(this.home)) > HOME_RADIUS_SQR) continue;
                BlockState support = this.level().getBlockState(cursor);
                double score = isNaturalPerch(support) ? 20.0D : 0.0D;
                // A dry emergency foothold is allowed, but it is not treated as
                // a feeding territory when no open water can be seen from it.
                score += this.hasWaterView(point) ? 35.0D : -12.0D;
                score -= this.position().distanceToSqr(point) * 0.035D;
                if (this.shoreWater != null) {
                    double distance = cursor.distSqr(this.shoreWater);
                    score += 12 - Math.sqrt(distance) * 0.5D;
                }
                if (this.home != null) score -= Math.min(12, cursor.distSqr(this.home) * 0.008D);
                if (this.perch != null) {
                    double moved = horizontalDistanceSqr(point, this.position());
                    score += moved >= 36 ? 12 : -18;
                    if (this.perch.equals(cursor)) score -= 25;
                }
                for (KingfisherEntity neighbor : this.nearbyKingfishers) {
                    if (neighbor.isAlive() && !neighbor.isRemoved())
                        score -= Math.max(0, 12 - Math.sqrt(horizontalDistanceSqr(point, neighbor.position())));
                }
                BlockPos candidateSupport = cursor.immutable();
                if (candidates.stream().noneMatch(candidate -> candidate.support().equals(candidateSupport))) {
                    candidates.add(new PerchCandidate(candidateSupport, score));
                    candidates.sort(Comparator.comparingDouble(PerchCandidate::score).reversed());
                    if (candidates.size() > 8) candidates.remove(8);
                }
                break;
            }
        }
        for (PerchCandidate candidate : candidates) {
            if (!this.planLanding(candidate.support())) continue;
            this.setActivity(this.getHeldFish().isEmpty() ? Activity.TRAVEL : Activity.RETURN);
            return PerchSearchStatus.FOUND;
        }
        return PerchSearchStatus.NO_VALID_PERCH;
    }

    private boolean hasWaterView(Vec3 feet) {
        return this.shoreWater != null && KingfisherHabitat.isOpenWater(this.level(), this.shoreWater)
                && KingfisherHabitat.hasWaterView(this.level(), feet.add(0, this.getEyeHeight(), 0), this.shoreWater);
    }

    private void refreshPerchHabitat(ServerLevel server) {
        if (this.perch == null) return;
        Vec3 point = this.perchPoint(this.perch);
        this.watersidePerch = this.hasWaterView(point);
        // Refresh once after landing, not once for every sampled shore column.
        // A bird crossing a wide river must discover the water at its new bank.
        if (!this.watersidePerch && BirdScanBudget.tryAcquire(server, this, 2)) {
            BlockPos water = KingfisherHabitat.findWater(this.level(), this.perch.above());
            if (water != null && KingfisherHabitat.hasWaterView(this.level(),
                    point.add(0, this.getEyeHeight(), 0), water)) {
                this.shoreWater = water;
                this.watersidePerch = true;
            }
        }
        if (this.watersidePerch && this.availablePerch(point) && (this.home == null
                || horizontalDistanceSqr(point, Vec3.atCenterOf(this.home)) > HOME_RADIUS_SQR))
            this.home = this.perch.immutable();
    }

    private boolean availablePerch(Vec3 point) {
        if (this.territoryAvoid != null && horizontalDistanceSqr(point, this.territoryAvoid) < HOME_SPACING_SQR) return false;
        for (KingfisherEntity bird : this.nearbyKingfishers) {
            if (!bird.isAlive() || bird.isRemoved() || bird.level() != this.level()) continue;
            if (bird.perch != null && point.distanceToSqr(bird.position()) < PERCH_SPACING_SQR) return false;
            if (bird.landingPerch != null && bird.loaded(bird.landingPerch) && bird.getUUID().compareTo(this.getUUID()) < 0
                    && point.distanceToSqr(bird.perchPoint(bird.landingPerch)) < PERCH_SPACING_SQR) return false;
            if (bird.home != null && bird.getUUID().compareTo(this.getUUID()) < 0
                    && horizontalDistanceSqr(point, Vec3.atCenterOf(bird.home)) < HOME_SPACING_SQR) return false;
            if (bird.mealPerch != null && (!bird.getHeldFish().isEmpty() || bird.fishingArc != null)
                    && bird.getUUID().compareTo(this.getUUID()) < 0
                    && horizontalDistanceSqr(point, Vec3.atCenterOf(bird.mealPerch)) < PERCH_SPACING_SQR) return false;
        }
        return true;
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double x = a.x - b.x, z = a.z - b.z;
        return x * x + z * z;
    }

    private static boolean isNaturalPerch(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.FENCES)
                || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO);
    }

    private boolean validPerch(BlockPos support) {
        if (!this.loaded(support) || !this.loaded(support.above(2))) return false;
        BlockState state = this.level().getBlockState(support);
        if (!this.level().getFluidState(support).isEmpty() || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.CACTUS)) return false;
        Vec3 point = this.perchPoint(support);
        if (!isNaturalPerch(state) && !state.isFaceSturdy(this.level(), support, Direction.UP)) {
            // Slabs, stairs and farmland can support a bird below a full block's
            // top face. Check the real contact rather than rejecting their shape.
            if (state.getCollisionShape(this.level(), support).isEmpty()
                    || this.level().noCollision(this, this.getBoundingBox()
                    .move(point.subtract(this.position())).move(0, -0.02D, 0))) return false;
        }
        BlockPos feet = BlockPos.containing(point);
        BlockState feetState = this.level().getBlockState(feet);
        return !feetState.is(Blocks.FIRE) && !feetState.is(Blocks.SOUL_FIRE)
                && !feetState.is(Blocks.POWDER_SNOW) && !feetState.is(Blocks.SWEET_BERRY_BUSH)
                && !feetState.is(Blocks.WITHER_ROSE) && this.level().getFluidState(feet).isEmpty()
                && this.level().noCollision(this, this.getBoundingBox().move(point.subtract(this.position())));
    }

    private Vec3 perchPoint(BlockPos support) {
        BlockState state = this.level().getBlockState(support);
        var shape = state.getCollisionShape(this.level(), support);
        double top = state.is(BlockTags.LEAVES) && BirdConfigManager.birdsPassThroughLeaves()
                ? 0.75D : shape.isEmpty() ? 1.0D : shape.max(Direction.Axis.Y);
        return new Vec3(support.getX() + 0.5D, support.getY() + top + 0.005D, support.getZ() + 0.5D);
    }

    private void detectThreat(ServerLevel server) {
        LivingEntity danger = null;
        double bestDistance = Double.MAX_VALUE;
        var neighbors = server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(24, 9, 24),
                other -> other != this && other.isAlive() && (other instanceof Player || other instanceof KingfisherEntity
                        || other.getType().is(BirdTags.KINGFISHER_PREDATORS) || other.getType().is(BirdTags.KINGFISHER_DISTURBANCE)));
        this.nearbyKingfishers = neighbors.stream().filter(KingfisherEntity.class::isInstance)
                .map(KingfisherEntity.class::cast).toList();
        this.nearbyGull = neighbors.stream().anyMatch(entity -> this.distanceToSqr(entity) < 64
                && entity.getType().is(BirdTags.KINGFISHER_DISTURBANCE) && this.canSeePoint(entity.getEyePosition()));
        for (LivingEntity entity : neighbors) {
            double distance = this.distanceToSqr(entity);
            if (entity instanceof Player player) {
                if (player.isSpectator()) continue;
                int familiar = this.trustLevel(player.getUUID());
                double tolerance = Math.max(1.1D, 6.0D - familiar * 0.62D);
                if (player.isShiftKeyDown()) tolerance *= 0.6D;
                if (player.isSprinting() || player.getDeltaMovement().horizontalDistanceSqr() > 0.06D) tolerance += 3.0D;
                if (this.nearbyGull) tolerance += 1.5D;
                if (distance < tolerance * tolerance && distance < bestDistance && this.canSeePoint(player.getEyePosition())) {
                    danger = player;
                    bestDistance = distance;
                }
            } else if (entity.getType().is(BirdTags.KINGFISHER_PREDATORS)) {
                if (distance < 144 && distance < bestDistance && this.canSeePoint(entity.getEyePosition())) {
                    danger = entity;
                    bestDistance = distance;
                }
            }
        }
        if (danger != null && !this.isInWaterOrBubble() && this.getActivity() != Activity.EMERGE
                && (this.getActivity() != Activity.FLEE || this.stateTicks > 25)) {
            boolean approachingSlowly = danger instanceof Player player && !player.isSprinting()
                    && player.getDeltaMovement().horizontalDistanceSqr() < 0.025D && bestDistance > 9.0D;
            if (approachingSlowly && this.getActivity() == Activity.ALERT) return;
            if (approachingSlowly && this.getActivity() == Activity.OBSERVE && this.perch != null && this.alertTicks == 0) {
                this.alertSource = danger.position();
                this.territorialAlert = false;
                this.alertTicks = 100;
                this.setDeltaMovement(Vec3.ZERO);
                this.setActivity(Activity.ALERT);
            } else this.startFlee(danger.position());
        } else if (danger == null && this.getActivity() == Activity.OBSERVE && this.perch != null
                && !this.level().isNight() && this.separationDelay == 0 && this.alertTicks == 0) {
            // Stable UUID priority makes only one neighbour yield, preventing two
            // birds from endlessly chasing each other away from the same branch.
            KingfisherEntity resident = this.nearbyKingfishers.stream()
                    .filter(bird -> bird.getUUID().compareTo(this.getUUID()) < 0
                            && (bird.perch != null && this.distanceToSqr(bird) < PERCH_SPACING_SQR
                            || bird.home != null && this.home != null && this.home.distSqr(bird.home) < HOME_SPACING_SQR))
                    .min(Comparator.comparing(KingfisherEntity::getUUID)).orElse(null);
            if (resident != null) {
                this.territoryAvoid = resident.home == null ? resident.position() : Vec3.atCenterOf(resident.home);
                this.separationDelay = 600;
                this.home = null;
                this.alertSource = resident.position();
                this.territorialAlert = true;
                this.alertTicks = 40;
                this.setDeltaMovement(Vec3.ZERO);
                this.setActivity(Activity.ALERT);
            }
        }
    }

    private void startFlee(Vec3 source) {
        this.alertTicks = 100;
        this.clearFishingRoute();
        this.clearTravelPlan();
        this.offeredFood = null;
        this.perch = null;
        this.landingPerch = null;
        Vec3 away = this.position().subtract(source).multiply(1, 0, 1);
        if (away.lengthSqr() < 0.01D) away = new Vec3(Math.cos(this.getId()), 0, Math.sin(this.getId()));
        away = away.normalize().scale(8);
        this.targetPoint = this.position().add(away).add(0, 1.8D, 0);
        this.setActivity(Activity.FLEE);
        this.perchSearchDelay = Math.min(this.perchSearchDelay, 30);
    }

    private boolean flyToward(Vec3 target, double speed) {
        KingfisherFlightMotor.Mode mode = switch (this.getActivity()) {
            case DIVE -> KingfisherFlightMotor.Mode.DIVE;
            case SKIM -> KingfisherFlightMotor.Mode.SKIM;
            case EMERGE -> KingfisherFlightMotor.Mode.EMERGE;
            case HOVER -> KingfisherFlightMotor.Mode.HOVER;
            case FLEE -> KingfisherFlightMotor.Mode.ESCAPE;
            default -> KingfisherFlightMotor.Mode.TRANSIT;
        };
        KingfisherFlightMotor.Result result = this.moveFlight(target, speed, mode);
        return result == KingfisherFlightMotor.Result.MOVING || result == KingfisherFlightMotor.Result.ARRIVED;
    }

    private KingfisherFlightMotor.Result moveFlight(Vec3 target, double speed, KingfisherFlightMotor.Mode mode) {
        KingfisherFlightMotor.Result result = this.flightMotor.flyTo(target, speed, mode);
        this.faceTravel(this.getDeltaMovement());
        return result;
    }

    private void faceTravel(Vec3 movement) {
        if (movement.horizontalDistanceSqr() > 0.000025D) {
            float yaw = (float)(Mth.atan2(movement.z, movement.x) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(this.getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - this.getYRot()), -40, 40));
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
        }
        float pitch = (float)(Mth.atan2(-movement.y, Math.max(0.06D, movement.horizontalDistance())) * Mth.RAD_TO_DEG);
        this.entityData.set(FLIGHT_PITCH, Math.round(Mth.clamp(pitch, -55, 65) * 2) * 0.5F);
    }

    private boolean loaded(BlockPos pos) {
        return !this.level().isOutsideBuildHeight(pos)
                && (this.level() instanceof ServerLevel server
                ? server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
                : this.level().hasChunk(pos.getX() >> 4, pos.getZ() >> 4));
    }

    private boolean loadedLine(Vec3 target) {
        if (!Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)) return false;
        if (this.position().distanceToSqr(target) > 1024) return false;
        BlockPos a = this.blockPosition(), b = BlockPos.containing(target);
        for (int x = Math.min(a.getX(), b.getX()) >> 4; x <= (Math.max(a.getX(), b.getX()) >> 4); x++)
            for (int z = Math.min(a.getZ(), b.getZ()) >> 4; z <= (Math.max(a.getZ(), b.getZ()) >> 4); z++)
                if (!this.loaded(new BlockPos(x << 4, b.getY(), z << 4))) return false;
        return true;
    }

    private boolean clearLine(Vec3 target) {
        return this.level().clip(new ClipContext(this.getEyePosition(), target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private boolean canSeePoint(Vec3 point) { return this.loadedLine(point) && this.clearLine(point); }

    private void setActivity(Activity activity) {
        if (this.getActivity() == activity) return;

        this.entityData.set(ACTIVITY, activity.ordinal());
        this.stateTicks = 0;
        this.flightMotor.reset();
        if (activity != Activity.HOVER) {
            this.entityData.set(HOVER_MODE, 0);
            this.hoverAnchor = null;
            this.hoverHoldTicks = 0;
        }
        if (activity != Activity.ALERT) {
            this.alertSource = null;
            this.territorialAlert = false;
        }
        if (activity != Activity.DIVE && activity != Activity.SKIM && activity != Activity.HOVER
                && activity != Activity.FISH_APPROACH && activity != Activity.FISH_CRUISE) this.clearFishingRoute();
        if (activity != Activity.EMERGE) {
            this.waterExitPoint = null;
            this.waterWaypoint = null;
            this.waterExitTicks = 0;
            this.dryExitTicks = 0;
        }
        if (!this.shouldPlayFlyAnimation()) this.entityData.set(FLIGHT_PITCH, 0.0F);
    }

    @Override public void travel(Vec3 input) {
        if (this.isPassenger()) { this.setDeltaMovement(Vec3.ZERO); return; }
        if (BirdFlightLock.isFlightDisabled(this) && !this.isInWaterOrBubble()) {
            this.setNoGravity(false);
            super.travel(Vec3.ZERO);
            return;
        }
        if (this.shouldPlayFlyAnimation()) {
            this.setNoGravity(true);
            if (this.isEffectiveAi()) this.move(MoverType.SELF, this.getDeltaMovement());
            this.calculateEntityAnimation(false);
            this.resetFallDistance();
            return;
        }
        if (this.entityData.get(PERCHED)) { this.setNoGravity(true); this.setDeltaMovement(Vec3.ZERO); return; }
        this.setNoGravity(false);
        super.travel(Vec3.ZERO);
    }

    @Override public boolean hurt(DamageSource source, float damage) {
        boolean hurt = super.hurt(source, damage);
        if (hurt && !this.level().isClientSide && this.isAlive()) {
            if (source.getEntity() instanceof Player player) {
                this.trust.remove(player.getUUID());
                this.witnessedFeedings.remove(player.getUUID());
            }
            this.startFlee(source.getSourcePosition() == null ? this.position().add(1, 0, 0) : source.getSourcePosition());
        }
        return hurt;
    }

    private void addTrust(UUID player, int amount) {
        int level = Math.min(MAX_TRUST, this.trustLevel(player) + amount);
        this.trust.remove(player);
        if (this.trust.size() >= MAX_TRUSTED_PLAYERS) this.trust.remove(this.trust.keySet().iterator().next());
        this.trust.put(player, level);
    }

    private void witnessFeeding(UUID player) {
        this.witnessDelay = 200;
        int observations = this.witnessedFeedings.getOrDefault(player, 0) + 1;
        this.witnessedFeedings.remove(player);
        if (observations >= 3) {
            this.addTrust(player, 1);
        } else {
            if (this.witnessedFeedings.size() >= MAX_TRUSTED_PLAYERS)
                this.witnessedFeedings.remove(this.witnessedFeedings.keySet().iterator().next());
            this.witnessedFeedings.put(player, observations);
        }
    }

    public int trustLevel(UUID player) { return this.trust.getOrDefault(player, 0); }

    public static boolean isTrustFood(ItemStack stack) { return BirdFoodSafety.matchesClean(BirdTags.KINGFISHER_FOODS, stack); }

    @Nullable @Override protected SoundEvent getAmbientSound() {
        return this.guidePreview == null && !this.isSleeping() && !this.isEating() && !this.isDiving()
                && this.getActivity() != Activity.EMERGE && !this.isInWaterOrBubble() && this.getHeldFish().isEmpty()
                ? GuaniaoSoundEvents.KINGFISHER_AMBIENT.get() : null;
    }

    @Override public int getAmbientSoundInterval() {
        // The longest clip is 1.2 seconds; even the fastest rate must let it finish.
        return Math.max(26, BirdFlockSoundLimiter.scaledAmbientInterval(this, 240));
    }

    @Override public void playAmbientSound() {
        if (!(this.level() instanceof ServerLevel server) || !this.isAlive() || this.isSilent()) return;
        SoundEvent sound = this.getAmbientSound();
        float volume = BirdSoundVolume.apply(this, 0.55F);
        long now = server.getGameTime();
        if (sound == null || volume <= 0 || now < this.nextAllowedCallTick
                || !BirdFlockSoundLimiter.allowAmbient(this)) return;
        this.nextAllowedCallTick = now + this.getAmbientSoundInterval();
        // Preserve the recording's pitch without changing inherited hurt/death sounds.
        this.playSound(sound, volume, 1.0F);
    }

    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) { return InteractionResult.PASS; }
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }
    @Override public boolean requiresCustomPersistence() { return super.requiresCustomPersistence() || !this.trust.isEmpty(); }
    @Override public boolean removeWhenFarAway(double distance) { return this.trust.isEmpty() && distance > BirdVisibility.TRACKING_DISTANCE_SQUARED; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return BirdVisibility.shouldRender(distance, Entity.getViewScale()); }
    @Override protected void checkFallDamage(double distance, boolean grounded, BlockState state, BlockPos pos) { }
    public Activity getActivity() { return ACTIVITIES[Mth.clamp(this.entityData.get(ACTIVITY), 0, ACTIVITIES.length - 1)]; }
    public ItemStack getHeldFish() { return this.entityData.get(HELD_FISH); }
    public boolean isEating() { return "eat".equals(this.guidePreview) || this.guidePreview == null && this.getActivity() == Activity.EAT; }
    public float getEatingTicks(float partialTick) {
        // Handbook previews advance tickCount without running the entity AI/tick.
        if ("eat".equals(this.guidePreview))
            return Mth.clamp(Math.floorMod(this.tickCount - this.guidePreviewStartedTick, 70) + partialTick, 0, 50);
        return this.isEating() ? Mth.clamp((this.level().isClientSide ? this.clientEatingTicks : this.stateTicks) + partialTick, 0, 50) : 0;
    }
    public boolean isSleeping() { return "sleep".equals(this.guidePreview) || this.guidePreview == null && this.getActivity() == Activity.SLEEP; }
    public boolean isDiving() { return "dive".equals(this.guidePreview) || "swoop".equals(this.guidePreview)
            || this.guidePreview == null && (this.getActivity() == Activity.DIVE || this.getActivity() == Activity.SKIM); }
    public boolean isHovering() { return "hover".equals(this.guidePreview) || "hover_look".equals(this.guidePreview)
            || this.guidePreview == null && this.getActivity() == Activity.HOVER && this.entityData.get(HOVER_MODE) != 0; }
    public boolean isLookingWhileHovering() { return "hover_look".equals(this.guidePreview)
            || this.guidePreview == null && this.getActivity() == Activity.HOVER && this.entityData.get(HOVER_MODE) == 2; }
    public boolean isAlert() { return "alert".equals(this.guidePreview)
            || this.guidePreview == null && this.getActivity() == Activity.ALERT; }
    public boolean isShaking() { return "shake".equals(this.guidePreview)
            || this.guidePreview == null && this.getActivity() == Activity.SHAKE; }
    public boolean isDefecating() { return "defecate".equals(this.guidePreview)
            || this.guidePreview == null && this.getActivity() == Activity.DEFECATE; }

    /** Approximate the raised tail root without depending on client animation code. */
    public Vec3 getDroppingLaunchPosition() {
        return this.position().add(this.droppingLaunchOffset(this.getYRot(), this.stateTicks));
    }

    private Vec3 droppingLaunchOffset(float yaw, float ticks) {
        // The 3-tick transition puts the raised-tail .5s keyframe at server tick 13.
        float progress = Mth.clamp((ticks - 13.0F) / 5.0F, 0, 1);
        double scale = this.getModelRenderScale() / this.modelScaleProfile().baseRenderScale();
        double rear = Mth.lerp(progress, 0.126D, 0.163D) * scale;
        double height = Mth.lerp(progress, 0.311D, 0.258D) * scale;
        return Vec3.directionFromRotation(0, yaw).scale(-rear).add(0, height, 0);
    }

    public Vec3 getDroppingRenderOrigin(float partialTick) {
        Vec3 position = new Vec3(Mth.lerp(partialTick, this.xOld, this.getX()),
                Mth.lerp(partialTick, this.yOld, this.getY()), Mth.lerp(partialTick, this.zOld, this.getZ()));
        int age = this.tickCount - this.droppingRenderTick;
        if (this.guidePreview == null && this.getActivity() == Activity.DEFECATE
                && this.droppingRenderOffset != null && age >= 0 && age <= 1)
            return position.add(this.droppingRenderOffset);
        float yaw = Mth.rotLerp(partialTick, this.yBodyRotO, this.yBodyRot);
        float ticks = (this.level().isClientSide ? this.clientDefecatingTicks : this.stateTicks) + partialTick;
        return position.add(this.droppingLaunchOffset(yaw, ticks));
    }

    /** Only a rendered world entity may supply this short-lived visual anchor. */
    public void setDroppingRenderOrigin(Vec3 origin, float partialTick) {
        if (!this.level().isClientSide || this.guidePreview != null || this.getActivity() != Activity.DEFECATE
                || this.level().getEntity(this.getId()) != this) return;
        Vec3 position = new Vec3(Mth.lerp(partialTick, this.xOld, this.getX()),
                Mth.lerp(partialTick, this.yOld, this.getY()), Mth.lerp(partialTick, this.zOld, this.getZ()));
        Vec3 offset = origin.subtract(position);
        double limit = Math.max(1.0D, this.getModelRenderScale());
        if (!Double.isFinite(offset.x) || !Double.isFinite(offset.y) || !Double.isFinite(offset.z)
                || offset.lengthSqr() > limit * limit) return;
        this.droppingRenderOffset = offset;
        this.droppingRenderTick = this.tickCount;
    }
    public boolean shouldPlayFlyAnimation() {
        if (this.guidePreview != null) return this.guidePreview.equals("fly") || this.guidePreview.equals("glide")
                || this.isDiving() || this.isHovering();
        if (this.isPassenger()) return false;
        return switch (this.getActivity()) { case TRAVEL, DIVE, SKIM, EMERGE, RETURN, FLEE, HOVER, FISH_APPROACH, FISH_CRUISE -> true; default -> false; };
    }
    public float getFlightPitch() { return this.guidePreview == null ? this.entityData.get(FLIGHT_PITCH) : 0; }
    public float getFlightPitch(float partialTick) { return Mth.lerp(partialTick, this.previousFlightPitch, this.getFlightPitch()); }
    public void setGuidePreviewAnimation(@Nullable String animation) {
        if (!Objects.equals(this.guidePreview, animation)) {
            this.movementAnimationDirty = true;
            this.clientEatingTicks = 0;
            this.guidePreviewStartedTick = this.tickCount;
            this.droppingRenderOffset = null;
        }
        this.guidePreview = animation;
    }
    @Override public boolean isFlying() { return this.shouldPlayFlyAnimation(); }
    @Override public BirdFlightProfile birdFlightProfile() { return BirdFlightProfile.SPARROW; }
    @Override public boolean isBirdFlightActive() { return this.shouldPlayFlyAnimation(); }
    @Override public boolean isBirdLanding() { return this.landingPerch != null && this.landingPoint != null
            && this.approachPoint == null && this.position().distanceToSqr(this.landingPoint) < 4; }
    @Override public boolean isBirdEscaping() { return this.getActivity() == Activity.FLEE || this.getActivity() == Activity.EMERGE; }
    @Override public boolean isBirdSleeping() { return this.isSleeping(); }
    @Override public void wakeFromLoudSound(Vec3 soundPosition) { if (!this.level().isClientSide && this.isSleeping()) this.startFlee(soundPosition); }
    @Override public BirdModelScaleProfile modelScaleProfile() { return BirdModelScaleProfile.KINGFISHER; }
    @Override public float getIndividualModelScale() { return this.entityData.get(SCALE); }
    @Override public void setIndividualModelScale(float scale) { this.entityData.set(SCALE, BirdModelScale.sanitize(scale, this.modelScaleProfile())); }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag entries = new ListTag();
        this.trust.forEach((uuid, value) -> { CompoundTag entry = new CompoundTag(); entry.putUUID("Player", uuid); entry.putInt("Level", value); entries.add(entry); });
        tag.put("KingfisherTrust", entries);
        ListTag witnesses = new ListTag();
        this.witnessedFeedings.forEach((uuid, value) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", uuid);
            entry.putInt("Observations", value);
            witnesses.add(entry);
        });
        tag.put("KingfisherWitnesses", witnesses);
        tag.putInt("KingfisherWitnessDelay", this.witnessDelay);
        if (this.home != null) tag.putLong("KingfisherHome", this.home.asLong());
        if (this.mealPerch != null && !this.getHeldFish().isEmpty()) tag.putLong("KingfisherMealPerch", this.mealPerch.asLong());
        if (this.perch != null) tag.putLong("KingfisherPerch", this.perch.asLong());
        tag.putBoolean("KingfisherWatersidePerch", this.watersidePerch);
        if (this.shoreWater != null) tag.putLong("KingfisherWater", this.shoreWater.asLong());
        if (!this.getHeldFish().isEmpty()) tag.put("KingfisherMeal", this.getHeldFish().save(new CompoundTag()));
        tag.putInt("KingfisherHuntDelay", this.huntDelay);
        tag.putInt("KingfisherFeedDelay", this.feedDelay);
        tag.putBoolean("KingfisherWaterShakePending", this.waterShakePending);
        tag.putInt("KingfisherHoverLookDelay", this.hoverLookDelay);
        if (this.getActivity() == Activity.DEFECATE) {
            tag.putInt("KingfisherDefecationTicks", this.stateTicks);
            tag.putBoolean("KingfisherDroppingReleased", this.droppingReleased);
        }
        BirdModelScale.save(tag, this.getIndividualModelScale(), this.modelScaleProfile());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.trust.clear();
        this.witnessedFeedings.clear();
        ListTag entries = tag.getList("KingfisherTrust", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(entries.size(), MAX_TRUSTED_PLAYERS); i++) {
            CompoundTag entry = entries.getCompound(i);
            int value = Mth.clamp(entry.getInt("Level"), 0, MAX_TRUST);
            if (entry.hasUUID("Player") && value > 0) this.trust.put(entry.getUUID("Player"), value);
        }
        ListTag witnesses = tag.getList("KingfisherWitnesses", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(witnesses.size(), MAX_TRUSTED_PLAYERS); i++) {
            CompoundTag entry = witnesses.getCompound(i);
            int value = Mth.clamp(entry.getInt("Observations"), 0, 2);
            if (entry.hasUUID("Player") && value > 0) this.witnessedFeedings.put(entry.getUUID("Player"), value);
        }
        this.witnessDelay = Mth.clamp(tag.getInt("KingfisherWitnessDelay"), 0, 200);
        this.home = tag.contains("KingfisherHome", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("KingfisherHome")) : null;
        this.perch = tag.contains("KingfisherPerch", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("KingfisherPerch")) : null;
        this.watersidePerch = tag.contains("KingfisherWatersidePerch") ? tag.getBoolean("KingfisherWatersidePerch") : this.perch != null;
        this.shoreWater = tag.contains("KingfisherWater", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("KingfisherWater")) : null;
        ItemStack meal = tag.contains("KingfisherMeal", Tag.TAG_COMPOUND) ? ItemStack.of(tag.getCompound("KingfisherMeal")) : ItemStack.EMPTY;
        if (!meal.isEmpty()) meal.setCount(1);
        this.entityData.set(HELD_FISH, meal);
        this.mealPerch = tag.contains("KingfisherMealPerch", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("KingfisherMealPerch")) : null;
        this.huntDelay = Mth.clamp(tag.getInt("KingfisherHuntDelay"), 0, 1500);
        this.feedDelay = Mth.clamp(tag.getInt("KingfisherFeedDelay"), 0, 200);
        this.waterShakePending = tag.getBoolean("KingfisherWaterShakePending");
        if (tag.contains("KingfisherHoverLookDelay", Tag.TAG_INT))
            this.hoverLookDelay = Mth.clamp(tag.getInt("KingfisherHoverLookDelay"), 0, 1600);
        this.setIndividualModelScale(BirdModelScale.load(tag, this.modelScaleProfile()));
        this.clearFishingRoute();
        this.clearTravelPlan();
        this.failedPerches.clear();
        this.failedWaterExits.clear();
        this.waterExitPoint = null;
        this.waterWaypoint = null;
        this.waterExitSearchDelay = 0;
        this.waterExitTicks = 0;
        this.dryExitTicks = 0;
        this.offeredFood = null;
        this.landingPerch = null;
        this.targetPoint = null;
        this.territoryAvoid = null;
        this.separationDelay = 0;
        this.nearbyKingfishers = List.of();
        this.clientEatingTicks = 0;
        this.clientDefecatingTicks = 0;
        this.droppingRenderOffset = null;
        this.droppingRenderTick = -1;
        this.setActivity(this.perch == null ? Activity.RETURN : Activity.OBSERVE);
        if (tag.contains("KingfisherDefecationTicks", Tag.TAG_INT)) {
            this.setActivity(Activity.DEFECATE);
            this.stateTicks = Mth.clamp(tag.getInt("KingfisherDefecationTicks"), 0, 23);
            this.droppingReleased = tag.getBoolean("KingfisherDroppingReleased");
        }
        this.entityData.set(PERCHED, this.perch != null);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 3, state -> {
            // An off-screen bird can finish and start the same short action before
            // its renderer runs again. Consume data changes once, never each frame.
            if (this.movementAnimationDirty) {
                state.resetCurrentAnimation();
                this.movementAnimationDirty = false;
            }
            if (this.isSleeping()) return state.setAndContinue(SLEEP);
            if (this.isDiving()) return state.setAndContinue(SWOOP);
            if (this.isHovering()) return state.setAndContinue(this.isLookingWhileHovering() ? HOVER_LOOK : HOVER);
            if (this.shouldPlayFlyAnimation()) return state.setAndContinue(FLY);
            if (this.isShaking()) return state.setAndContinue(SHAKE);
            if (this.isDefecating()) return state.setAndContinue(DEFECATE);
            if (this.isAlert()) return state.setAndContinue(ALERT);
            // The supplied file has no feeding clip; idle_diff_3 is defecation.
            return state.setAndContinue(IDLE);
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.animationCache; }
}
