package EdDYON.guaniao.content.dropping;

import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.content.bird.kingfisher.KingfisherEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class BirdDroppingProjectileEntity extends ThrowableItemProjectile implements GeoEntity {
    public static final int STRIP_STREAM_TICKS = 4;
    private static final int MAX_FLIGHT_LIFETIME_TICKS = 20 * 10;
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(BirdDroppingProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_NATURAL_DROPPING = SynchedEntityData.defineId(BirdDroppingProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_STRIP_DROPPING = SynchedEntityData.defineId(BirdDroppingProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SOURCE_BIRD_ID = SynchedEntityData.defineId(BirdDroppingProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_STRIP_STARTED_AT = SynchedEntityData.defineId(BirdDroppingProjectileEntity.class, EntityDataSerializers.LONG);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private int lifeTicks;
    private boolean impactResolved;
    private boolean laxativeDropping;
    private UUID sourceBirdUuid;
    private Vec3[] stripTrail;
    private int stripTrailCount;

    public BirdDroppingProjectileEntity(EntityType<? extends BirdDroppingProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public BirdDroppingProjectileEntity(Level level, LivingEntity owner, BirdDroppingVariant variant) {
        super(GuaniaoEntityTypes.BIRD_DROPPING_PROJECTILE.get(), owner, level);
        this.setVariant(variant);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, BirdDroppingVariant.ONE.id());
        this.entityData.define(DATA_NATURAL_DROPPING, false);
        this.entityData.define(DATA_STRIP_DROPPING, false);
        this.entityData.define(DATA_SOURCE_BIRD_ID, -1);
        this.entityData.define(DATA_STRIP_STARTED_AT, 0L);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return this.getVariant().item();
    }

    @Override
    protected float getGravity() {
        return this.isStripDropping() ? 0.025F : 0.06F;
    }

    @Override
    public void tick() {
        if (this.level().isClientSide && this.isStripDropping()) {
            this.rememberStripPosition();
        }
        super.tick();
        if (this.level().isClientSide || this.isRemoved()) {
            return;
        }
        ++this.lifeTicks;
        if (this.isStripDropping() && this.entityData.get(DATA_SOURCE_BIRD_ID) < 0 && this.getOwner() != null) {
            this.entityData.set(DATA_SOURCE_BIRD_ID, this.getOwner().getId());
        }
        if (this.isInWaterOrBubble() || this.lifeTicks >= MAX_FLIGHT_LIFETIME_TICKS) {
            this.impactResolved = true;
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide || !this.beginImpactResolution()) {
            return;
        }

        Entity hit = result.getEntity();
        if (BirdDroppingInteractionHandler.handleEntityHit(this, hit)) {
            this.splat();
            this.discard();
            return;
        }

        if (hit instanceof Player player) {
            if (this.isNaturalDropping()) {
                BirdDroppingPrankHandler.handleNaturalHitPlayer(this, player);
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
            }
        } else if (hit instanceof Villager villager) {
            BirdDroppingPrankHandler.handleVillagerHit(this, villager);
        } else if (hit instanceof IronGolem golem) {
            BirdDroppingPrankHandler.handleIronGolemHit(this, golem);
        } else if (hit instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
        }

        this.attachStickySplat(hit);
        this.splat();
        this.discard();
    }

    /** Spawns the sticky splat variant that rides the victim; the splat follows its
     * host every tick until it ages out. A helmet keeps the mess off, consistent
     * with the prank handler. */
    private void attachStickySplat(Entity hit) {
        if (!(hit instanceof LivingEntity living)
                || !(this.level() instanceof ServerLevel serverLevel)
                || !this.canAddImpactSplatAt(serverLevel, living.position())) {
            return;
        }
        if (hit instanceof Player player && !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return;
        }
        Vec3 anchor = living.position().add(0.0D, living.getBbHeight() * 0.92D, 0.0D);
        BirdDroppingSplatEntity splat = BirdDroppingSplatEntity.onEntity(
                serverLevel, living, anchor, Direction.UP);
        splat.setStripDropping(this.isStripDropping());
        serverLevel.addFreshEntity(splat);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide || !this.beginImpactResolution()) {
            return;
        }

        if (BirdDroppingPrankHandler.handleCakeHit(this, result.getBlockPos())) {
            this.discard();
            return;
        }

        if (BirdDroppingInteractionHandler.handleNearbyFoodHit(this, result.getLocation())) {
            this.splat();
            this.discard();
            return;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            BirdDroppingPressurePlatePulse.tryTrigger(serverLevel, result.getBlockPos(), this);
        }
        this.spawnBlockSplat(result);
        this.splat();
        this.discard();
    }

    private boolean beginImpactResolution() {
        if (this.impactResolved) {
            return false;
        }
        this.impactResolved = true;
        return true;
    }

    private void spawnBlockSplat(BlockHitResult result) {
        Level level = this.level();
        if (level.getFluidState(result.getBlockPos().relative(result.getDirection())).isSource()) {
            return;
        }
        if (!this.canAddImpactSplatAt(level, result.getLocation())) {
            return;
        }
        BirdDroppingSplatEntity splat = BirdDroppingSplatEntity.onBlock(level, result.getLocation(), result.getDirection(), result.getBlockPos());
        splat.setStripDropping(this.isStripDropping());
        if (this.isStripDropping()) {
            splat.alignStripWithImpact(this.getDeltaMovement());
        }
        level.addFreshEntity(splat);
    }

    private void splat() {
        Level level = this.level();
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_BLOCK_HIT, SoundSource.NEUTRAL, 0.65F, 0.85F + level.random.nextFloat() * 0.25F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), 4, 0.08D, 0.04D, 0.08D, 0.004D);
        }
    }

    public BirdDroppingVariant getVariant() {
        return BirdDroppingVariant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(BirdDroppingVariant variant) {
        if (variant == null) {
            variant = BirdDroppingVariant.ONE;
        }
        this.entityData.set(DATA_VARIANT, variant.id());
    }

    public boolean isNaturalDropping() {
        return this.entityData.get(DATA_NATURAL_DROPPING);
    }

    public boolean isStripDropping() {
        return this.entityData.get(DATA_STRIP_DROPPING);
    }

    public void setStripDropping(boolean strip) {
        if (strip && !this.isStripDropping()) {
            this.entityData.set(DATA_STRIP_STARTED_AT, this.level().getGameTime());
        }
        this.entityData.set(DATA_STRIP_DROPPING, strip);
    }

    public float getStripAge(float partialTick) {
        return Mth.clamp(this.level().getGameTime() - this.entityData.get(DATA_STRIP_STARTED_AT) + partialTick,
                0.0F, MAX_FLIGHT_LIFETIME_TICKS);
    }

    private void rememberStripPosition() {
        if (this.stripTrail == null) this.stripTrail = new Vec3[STRIP_STREAM_TICKS + 1];
        if (this.stripTrailCount > 0 && this.position().distanceToSqr(this.stripTrail[0]) > 16.0D) {
            this.stripTrailCount = 0;
        }
        System.arraycopy(this.stripTrail, 0, this.stripTrail, 1, this.stripTrail.length - 1);
        this.stripTrail[0] = this.position();
        this.stripTrailCount = Math.min(this.stripTrailCount + 1, this.stripTrail.length);
    }

    private Vec3 stripHistoryPoint(float ticksBack, float partialTick) {
        if (this.stripTrailCount == 0) return this.getPosition(partialTick);
        float sample = Mth.clamp(1.0F - partialTick + ticksBack, 0.0F, this.stripTrailCount);
        int first = Mth.floor(sample);
        Vec3 a = first == 0 ? this.position() : this.stripTrail[first - 1];
        Vec3 b = first >= this.stripTrailCount ? a : this.stripTrail[first];
        return a.lerp(b, sample - first);
    }

    /** Visual-only fluid trail; one projectile still owns all collisions and limits. */
    public Vec3 getStripStreamPoint(float fraction, float partialTick) {
        float span = Mth.clamp(this.stripTrailCount - 1 + partialTick, 0.0F, STRIP_STREAM_TICKS);
        Vec3 point = this.stripHistoryPoint(fraction * span, partialTick);
        float attachment = Mth.clamp(STRIP_STREAM_TICKS - this.getStripAge(partialTick), 0.0F, 1.0F);
        if (attachment > 0 && this.level().getEntity(this.entityData.get(DATA_SOURCE_BIRD_ID)) instanceof KingfisherEntity bird
                && bird.isAlive() && bird.isDefecating()) {
            Vec3 tail = this.stripHistoryPoint(span, partialTick);
            Vec3 origin = bird.getDroppingRenderOrigin(partialTick);
            Vec3 correction = origin.subtract(tail);
            if (correction.lengthSqr() < 0.36D) {
                point = point.add(correction.scale(fraction * fraction * attachment));
            }
        }
        return point;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // The spray can remain visible while its tiny leading projectile leaves the view.
        return this.isStripDropping() ? super.getBoundingBoxForCulling().inflate(2.0D) : super.getBoundingBoxForCulling();
    }

    public void markNaturalDropping(UUID birdUuid) {
        this.entityData.set(DATA_NATURAL_DROPPING, true);
        this.sourceBirdUuid = birdUuid;
        if (this.getOwner() != null) this.entityData.set(DATA_SOURCE_BIRD_ID, this.getOwner().getId());
    }

    public void markLaxativeDropping(UUID birdUuid) {
        this.markNaturalDropping(birdUuid);
        this.laxativeDropping = true;
    }

    public boolean isLaxativeDropping() {
        return this.laxativeDropping;
    }

    private boolean canAddImpactSplatAt(Level level, Vec3 position) {
        return this.isLaxativeDropping()
                ? BirdDroppingSplatEntity.canAddHardCappedSplatAt(level, position)
                : BirdDroppingSplatEntity.canAddSplatAt(level, position);
    }

    public UUID getSourceBirdUuid() {
        return this.sourceBirdUuid;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant().id());
        tag.putBoolean("NaturalDropping", this.isNaturalDropping());
        tag.putBoolean("StripDropping", this.isStripDropping());
        tag.putLong("StripStartedAt", this.entityData.get(DATA_STRIP_STARTED_AT));
        tag.putBoolean("LaxativeDropping", this.isLaxativeDropping());
        tag.putInt("LifeTicks", this.lifeTicks);
        if (this.sourceBirdUuid != null) {
            tag.putUUID("SourceBirdUuid", this.sourceBirdUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(BirdDroppingVariant.byId(tag.getInt("Variant")));
        this.entityData.set(DATA_NATURAL_DROPPING, tag.getBoolean("NaturalDropping"));
        this.setStripDropping(tag.getBoolean("StripDropping"));
        this.laxativeDropping = tag.getBoolean("LaxativeDropping");
        this.lifeTicks = Math.max(0, tag.getInt("LifeTicks"));
        this.entityData.set(DATA_SOURCE_BIRD_ID, -1);
        this.entityData.set(DATA_STRIP_STARTED_AT, tag.contains("StripStartedAt")
                ? tag.getLong("StripStartedAt") : this.level().getGameTime() - this.lifeTicks);
        this.stripTrail = null;
        this.stripTrailCount = 0;
        if (tag.hasUUID("SourceBirdUuid")) {
            this.sourceBirdUuid = tag.getUUID("SourceBirdUuid");
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }
}
