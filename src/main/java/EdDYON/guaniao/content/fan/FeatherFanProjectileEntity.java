package EdDYON.guaniao.content.fan;

import EdDYON.guaniao.content.enchantment.GuaniaoEnchantments;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoItems;
import EdDYON.guaniao.registry.GuaniaoParticleTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FeatherFanProjectileEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(FeatherFanProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_CHARGE = SynchedEntityData.defineId(FeatherFanProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_RIVEN_TICKS = SynchedEntityData.defineId(FeatherFanProjectileEntity.class, EntityDataSerializers.INT);
    private static final int MAX_OWNER_MISSING_TICKS = 200;
    private static final int MAX_TOTAL_LIFE_TICKS = 400;
    private static final int RETURN_COOLDOWN_TICKS = 12;
    private static final int ENTITY_STUCK_DURATION_TICKS = 100;
    private static final int BLOCK_STUCK_DURATION_TICKS = 70;
    private static final int STUCK_DAMAGE_DURATION_TICKS = 50;
    private static final double ENTITY_STUCK_DEPTH = 0.06D;
    private static final int PULLOUT_DURATION_TICKS = 5;
    private static final int STUCK_DAMAGE_INTERVAL = 10;
    private static final float STUCK_DAMAGE = 1.0F;
    private static final double STUCK_SLOW_AMOUNT = -0.25D;
    private static final int BURIAL_DURATION_TICKS = 40;
    private static final double BURIAL_PULL_RADIUS = 5.0D;
    private static final int BURIAL_DAMAGE_INTERVAL_TICKS = 10;
    private static final float BURIAL_DAMAGE = 0.5F;
    private static final double BURIAL_SLASH_RADIUS = 4.0D;
    private static final float BURIAL_SLASH_DAMAGE = 6.0F;
    public static final int RIVEN_PREPARE_END = 5;
    public static final int RIVEN_SPLIT_END = 14;
    public static final int RIVEN_LOCK_END = 19;
    public static final int RIVEN_CONVERGE_END = 26;
    public static final int RIVEN_END = 36;
    private static final double RIVEN_BURST_RADIUS = 3.75D;
    private static final float RIVEN_MAIN_DAMAGE = 8.0F;
    private static final float RIVEN_MIN_SPLASH_DAMAGE = 3.0F;
    private static final float RIVEN_MAX_SPLASH_DAMAGE = 5.0F;
    private static final float RIVEN_REFORM_DAMAGE = 16.0F;
    private static final int HUNT_MAX_TARGETS = 7;
    public static final float HUNT_SPEED = 1.75F;
    private static final double HUNT_TURN_RATE = 0.24D;
    private static final double HUNT_HIT_RANGE = 0.85D;
    private static final double HUNT_ABANDON_RANGE = 20.0D;

    public enum FanState {
        OUTBOUND_SPIN,
        PIERCING,
        STUCK_ENTITY,
        STUCK_BLOCK,
        RETURNING,
        BURIAL_VORTEX,
        RIVEN_SEQUENCE,
        HUNTING;

        private static FanState fromId(int id) {
            FanState[] values = values();
            return values[Mth.clamp(id, 0, values.length - 1)];
        }
    }

    private enum PiercingArt {
        NORMAL,
        BURIAL,
        RIVEN
    }

    private final Set<UUID> outboundHits = new HashSet<>();
    private final Set<UUID> returnHits = new HashSet<>();
    private final Set<UUID> huntedTargets = new HashSet<>();
    private final Set<UUID> huntingLockedTargets = new LinkedHashSet<>();
    private final Map<UUID, Boolean> burialTargetPhysics = new HashMap<>();
    private Vec3 throwOrigin = Vec3.ZERO;
    private float maxDistance = 6.0F;
    private float attackDamage = 3.0F;
    private float returnSpeed = 1.45F;
    private InteractionHand returnHand = InteractionHand.MAIN_HAND;
    private UUID ownerUuid;
    private UUID stuckEntityUuid;
    private UUID huntingTargetUuid;
    private BlockPos stuckBlockPos = BlockPos.ZERO;
    private Direction stuckFace = Direction.UP;
    private Vec3 stuckPosition = Vec3.ZERO;
    private Vec3 stuckOffset = Vec3.ZERO;
    private Vec3 stuckForward = new Vec3(0.0D, 0.0D, 1.0D);
    private Vec3 stuckLocalForward = new Vec3(0.0D, 0.0D, 1.0D);
    private Vec3 rivenAnchor = Vec3.ZERO;
    private int ownerMissingTicks;
    private int lifeTicks;
    private int returningTicks;
    private int stuckTicks;
    private int pulloutTicks;
    private int rivenTicks;
    private int huntingHop;
    private int clientStuckEntityId = -1;
    private int clientStuckFollowTicks;
    private boolean rivenDamageDone;
    private boolean rivenReformDamageDone;

    public FeatherFanProjectileEntity(EntityType<? extends FeatherFanProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public FeatherFanProjectileEntity(Level level, LivingEntity owner) {
        super(GuaniaoEntityTypes.FEATHER_FAN_PROJECTILE.get(), owner, level);
        this.ownerUuid = owner.getUUID();
        this.throwOrigin = this.position();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STATE, FanState.OUTBOUND_SPIN.ordinal());
        this.entityData.define(DATA_CHARGE, 0.0F);
        this.entityData.define(DATA_RIVEN_TICKS, 0);
    }

    public void configureThrow(ItemStack fanStack, InteractionHand hand, float charge) {
        float clampedCharge = Mth.clamp(charge, 0.0F, 1.0F);
        this.setItem(fanStack);
        this.returnHand = hand;
        this.setFanState(FanState.OUTBOUND_SPIN);
        this.entityData.set(DATA_CHARGE, clampedCharge);
        this.throwOrigin = this.position();
        this.maxDistance = Mth.lerp(clampedCharge, 6.0F, 16.0F);
        this.attackDamage = Mth.lerp(clampedCharge, 3.0F, 7.0F);
        this.returnSpeed = Mth.lerp(clampedCharge, 1.45F, 1.85F);
    }

    public void configurePiercing(ItemStack fanStack, InteractionHand hand) {
        this.setItem(fanStack);
        this.returnHand = hand;
        this.setFanState(FanState.PIERCING);
        this.entityData.set(DATA_CHARGE, 1.0F);
        this.throwOrigin = this.position();
        this.maxDistance = 20.0F;
        this.attackDamage = 7.0F;
        this.returnSpeed = 1.95F;
    }

    public void configureHunting(ItemStack fanStack, InteractionHand hand, float charge,
                                 List<LivingEntity> targets) {
        float clampedCharge = Mth.clamp(charge, 0.0F, 1.0F);
        this.setItem(fanStack);
        this.returnHand = hand;
        this.setFanState(FanState.HUNTING);
        this.entityData.set(DATA_CHARGE, clampedCharge);
        this.throwOrigin = this.position();
        this.maxDistance = 64.0F;
        this.attackDamage = 6.0F;
        this.returnSpeed = Mth.lerp(clampedCharge, 1.55F, 1.95F);
        this.huntingLockedTargets.clear();
        targets.stream()
                .limit(HUNT_MAX_TARGETS)
                .map(Entity::getUUID)
                .forEach(this.huntingLockedTargets::add);
        this.huntingTargetUuid = this.huntingLockedTargets.stream().findFirst().orElse(null);
        this.huntingHop = 0;
        this.huntedTargets.clear();
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return GuaniaoItems.WIND_FEATHER_FAN.get();
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        this.noPhysics = this.isNonCollidingState();
        if (!this.level().isClientSide) {
            if (++this.lifeTicks > MAX_TOTAL_LIFE_TICKS) {
                this.dropFanAndDiscard();
                return;
            }

            ServerPlayer owner = this.findServerOwner();
            if (owner == null) {
                this.releaseBurialTargets();
                this.removeStuckSlowdown();
                this.setFanState(FanState.RETURNING);
                this.noPhysics = true;
                this.setDeltaMovement(Vec3.ZERO);
                if (++this.ownerMissingTicks > MAX_OWNER_MISSING_TICKS) {
                    this.dropFanAndDiscard();
                    return;
                }
            } else {
                this.ownerMissingTicks = 0;
                if (!owner.isAlive() || owner.level() != this.level()) {
                    this.dropFanAndDiscard();
                    return;
                }
                this.setOwner(owner);
                switch (this.getFanState()) {
                    case RETURNING -> {
                        if (this.tickReturning(owner)) {
                            return;
                        }
                    }
                    case STUCK_ENTITY -> this.tickStuckEntity(owner);
                    case STUCK_BLOCK -> this.tickStuckBlock(owner);
                    case BURIAL_VORTEX -> this.tickBurialVortex(owner);
                    case RIVEN_SEQUENCE -> this.tickRivenSequence(owner);
                    case HUNTING -> this.tickHunting(owner);
                    case OUTBOUND_SPIN, PIERCING -> this.hitEntitiesAlongMotion();
                }
                if (this.level() instanceof ServerLevel serverLevel) {
                    FanState currentState = this.getFanState();
                    if (currentState == FanState.OUTBOUND_SPIN
                            || currentState == FanState.PIERCING
                            || currentState == FanState.HUNTING
                            || currentState == FanState.RETURNING) {
                        this.spawnFlightTrail(serverLevel);
                    }
                }
            }
        }

        this.noPhysics = this.isNonCollidingState();
        super.tick();

        if (this.level().isClientSide) {
            this.tickClientStuckAttachment();
        }

        if (!this.level().isClientSide) {
            ServerPlayer owner = this.findServerOwner();
            if (this.isFlyingOutbound()
                    && this.position().distanceToSqr(this.throwOrigin) >= this.maxDistance * this.maxDistance) {
                this.beginReturn(owner);
            }
            if (this.isReturning() && owner != null && this.position().distanceToSqr(returnTarget(owner)) <= 1.5D * 1.5D) {
                this.returnFanToOwner(owner);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        FanState state = this.getFanState();
        if (state == FanState.STUCK_ENTITY || state == FanState.STUCK_BLOCK
                || state == FanState.BURIAL_VORTEX || state == FanState.RIVEN_SEQUENCE
                || !(target instanceof LivingEntity) || target == this.getOwner() || !super.canHitEntity(target)) {
            return false;
        }
        if (state == FanState.HUNTING) {
            return this.huntingTargetUuid != null
                    && this.huntingTargetUuid.equals(target.getUUID())
                    && !this.huntedTargets.contains(target.getUUID());
        }
        Set<UUID> hits = state == FanState.RETURNING ? this.returnHits : this.outboundHits;
        return !hits.contains(target.getUUID());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        if (hit instanceof LivingEntity living) {
            this.hitLivingEntity(living, result.getLocation());
        }
    }

    private void hitLivingEntity(LivingEntity living, Vec3 hitLocation) {
        if (this.getFanState() == FanState.HUNTING) {
            this.hitHuntingTarget(living);
            return;
        }
        if (this.getFanState() == FanState.PIERCING) {
            this.hitPiercingTarget(living, hitLocation);
            return;
        }

        Set<UUID> hits = this.isReturning() ? this.returnHits : this.outboundHits;
        if (!hits.add(living.getUUID()) || this.level().isClientSide) {
            return;
        }

        float damage = this.isReturning() ? this.attackDamage * 0.85F : this.attackDamage;
        int previousInvulnerableTime = living.invulnerableTime;
        if (this.isReturning()) {
            living.invulnerableTime = 0;
        }
        boolean damaged = living.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
        if (this.isReturning()) {
            living.invulnerableTime = Math.max(living.invulnerableTime, previousInvulnerableTime);
        }
        if (!damaged) {
            return;
        }

        Vec3 movement = this.getDeltaMovement();
        living.knockback(0.25D, -movement.x, -movement.z);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F, this.isReturning() ? 0.85F : 1.1F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.28F, 1.45F);
        if (this.level() instanceof ServerLevel serverLevel) {
            double hitY = living.getY(0.55D);
            serverLevel.sendParticles(ParticleTypes.CRIT, living.getX(), hitY, living.getZ(), 6, 0.24D, 0.24D, 0.24D, 0.14D);
            serverLevel.sendParticles(ParticleTypes.POOF, living.getX(), hitY, living.getZ(), 2, 0.12D, 0.12D, 0.12D, 0.025D);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, living.getX(), hitY, living.getZ(), 4, 0.20D, 0.20D, 0.20D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, living.getX(), hitY, living.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void hitPiercingTarget(LivingEntity living, Vec3 hitLocation) {
        if (!this.outboundHits.add(living.getUUID())) {
            return;
        }

        Vec3 surfaceHit = this.findTargetSurface(living, hitLocation);
        if (this.level().isClientSide) {
            // Predict the same local-space attachment as the server so target knockback cannot
            // visually separate the fan while the authoritative state packet is in flight.
            this.clientStuckEntityId = living.getId();
            this.clientStuckFollowTicks = 3;
            this.positionOnEntitySurface(living, surfaceHit);
            return;
        }

        boolean damaged = living.hurt(this.damageSources().thrown(this, this.getOwner()), this.attackDamage);
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() > 1.0E-6D) {
            living.knockback(0.18D, -movement.x, -movement.z);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            this.spawnPiercingHitEffects(serverLevel, living, damaged);
        }

        if (living.isAlive()) {
            this.stickToEntity(living, surfaceHit);
        } else {
            this.setPos(surfaceHit.x, surfaceHit.y, surfaceHit.z);
            this.beginReturn(this.findServerOwner());
        }
    }

    private void stickToEntity(LivingEntity target, Vec3 hitLocation) {
        this.stuckEntityUuid = target.getUUID();
        this.positionOnEntitySurface(target, hitLocation);
        PiercingArt art = this.getPiercingArt();
        this.setFanState(switch (art) {
            case BURIAL -> FanState.BURIAL_VORTEX;
            case RIVEN -> FanState.RIVEN_SEQUENCE;
            case NORMAL -> FanState.STUCK_ENTITY;
        });
        this.stuckTicks = 0;
        this.pulloutTicks = 0;
        this.rivenTicks = 0;
        this.rivenDamageDone = false;
        this.rivenReformDamageDone = false;
        this.entityData.set(DATA_RIVEN_TICKS, 0);
        this.rivenAnchor = this.getBurialCenter(target);
        if (art == PiercingArt.NORMAL) {
            this.applyStuckSlowdown(target);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            if (art == PiercingArt.RIVEN) {
                this.spawnRivenStartEffects(serverLevel, this.rivenAnchor);
            } else {
                this.spawnPinEffects(serverLevel);
            }
            if (art == PiercingArt.BURIAL) {
                this.spawnBurialStartEffects(serverLevel, this.getBurialCenter(target));
            }
        }
    }

    private void positionOnEntitySurface(LivingEntity target, Vec3 hitLocation) {
        this.captureStuckDirection();
        Vec3 attachedPosition = hitLocation.add(this.stuckForward.scale(ENTITY_STUCK_DEPTH));
        float bodyYaw = target.yBodyRot * Mth.DEG_TO_RAD;
        this.stuckOffset = attachedPosition.subtract(target.position()).yRot(bodyYaw);
        this.stuckLocalForward = this.stuckForward.yRot(bodyYaw);
        this.setPos(attachedPosition.x, attachedPosition.y, attachedPosition.z);
        this.setDeltaMovement(Vec3.ZERO);
    }

    private Vec3 findTargetSurface(LivingEntity target, Vec3 fallback) {
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-6D) {
            return fallback;
        }

        Vec3 forward = movement.normalize();
        AABB surfaceBox = target.getBoundingBox().inflate(0.02D);
        Vec3 rayStart = fallback.subtract(forward.scale(4.0D));
        Vec3 rayEnd = fallback.add(forward.scale(4.0D));
        return surfaceBox.clip(rayStart, rayEnd).orElseGet(() -> new Vec3(
                Mth.clamp(fallback.x, surfaceBox.minX, surfaceBox.maxX),
                Mth.clamp(fallback.y, surfaceBox.minY, surfaceBox.maxY),
                Mth.clamp(fallback.z, surfaceBox.minZ, surfaceBox.maxZ)
        ));
    }

    private void stickToBlock(BlockHitResult result) {
        this.captureStuckDirection();
        this.stuckBlockPos = result.getBlockPos();
        this.stuckFace = result.getDirection();
        Vec3 surfaceOffset = Vec3.atLowerCornerOf(this.stuckFace.getNormal()).scale(0.035D);
        this.stuckPosition = result.getLocation().add(surfaceOffset);
        this.setPos(this.stuckPosition.x, this.stuckPosition.y, this.stuckPosition.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setFanState(FanState.STUCK_BLOCK);
        this.stuckTicks = 0;
        this.pulloutTicks = 0;
        if (this.level() instanceof ServerLevel serverLevel) {
            this.spawnPinEffects(serverLevel);
        }
    }

    private void tickStuckEntity(ServerPlayer owner) {
        if (this.pulloutTicks > 0) {
            this.tickPullout(owner);
            return;
        }

        LivingEntity target = this.findStuckEntity();
        if (target == null || !target.isAlive()) {
            this.beginPullout();
            return;
        }

        this.updateStuckAttachment(target);
        this.applyStuckSlowdown(target);
        this.stuckTicks++;

        if (this.stuckTicks <= STUCK_DAMAGE_DURATION_TICKS
                && this.stuckTicks % STUCK_DAMAGE_INTERVAL == 0) {
            Vec3 movementBeforeDamage = target.getDeltaMovement();
            int previousInvulnerableTime = target.invulnerableTime;
            target.invulnerableTime = 0;
            boolean damaged = target.hurt(this.damageSources().thrown(this, this.getOwner()), STUCK_DAMAGE);
            target.invulnerableTime = Math.max(target.invulnerableTime, previousInvulnerableTime);
            target.setDeltaMovement(movementBeforeDamage);
            if (damaged && this.level() instanceof ServerLevel serverLevel) {
                this.spawnStuckPulse(serverLevel, target);
            }
        }

        if (!target.isAlive() || this.stuckTicks >= ENTITY_STUCK_DURATION_TICKS) {
            this.beginPullout();
        }
    }

    private void tickBurialVortex(ServerPlayer owner) {
        if (this.pulloutTicks > 0) {
            this.tickPullout(owner);
            return;
        }

        LivingEntity anchor = this.findStuckEntity();
        if (anchor == null || !anchor.isAlive()) {
            this.beginPullout();
            return;
        }

        this.updateStuckAttachment(anchor);
        Vec3 center = this.getBurialCenter(anchor);
        Vec3 collectionPoint = anchor.position();
        this.stuckTicks++;
        this.captureBurialTargets(center, anchor);
        this.moveBurialTargets(collectionPoint);
        if (this.stuckTicks % BURIAL_DAMAGE_INTERVAL_TICKS == 0) {
            this.damageBurialTargets(anchor);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            this.spawnBurialVortexEffects(serverLevel, center);
        }

        if (this.stuckTicks >= BURIAL_DURATION_TICKS) {
            this.performBurialSlash(center);
            this.beginPullout();
        }
    }

    private void tickRivenSequence(ServerPlayer owner) {
        LivingEntity target = this.findStuckEntity();
        if (target != null && target.isAlive()) {
            this.rivenAnchor = target.getBoundingBox().getCenter();
            if (this.rivenTicks + 1 < RIVEN_PREPARE_END) {
                this.updateStuckAttachment(target);
            } else {
                this.setPos(this.rivenAnchor.x, this.rivenAnchor.y, this.rivenAnchor.z);
            }
        } else {
            this.setPos(this.rivenAnchor.x, this.rivenAnchor.y, this.rivenAnchor.z);
        }
        this.setDeltaMovement(Vec3.ZERO);

        this.rivenTicks++;
        this.entityData.set(DATA_RIVEN_TICKS, this.rivenTicks);
        if (this.level() instanceof ServerLevel serverLevel) {
            this.spawnRivenSequenceEffects(serverLevel, this.rivenAnchor);
        }

        if (this.rivenTicks >= RIVEN_CONVERGE_END && !this.rivenDamageDone) {
            this.rivenDamageDone = true;
            this.performRivenBurst(this.rivenAnchor);
        }

        if (this.rivenTicks >= RIVEN_END) {
            if (!this.rivenReformDamageDone) {
                this.rivenReformDamageDone = true;
                this.performRivenReformStrike(target);
                if (this.level() instanceof ServerLevel serverLevel) {
                    this.spawnRivenReformEffects(serverLevel, this.rivenAnchor);
                }
            }
            this.beginReturn(owner);
        }
    }

    private void tickHunting(ServerPlayer owner) {
        LivingEntity target = this.findHuntingTarget();
        if (target == null || !target.isAlive() || !owner.canAttack(target)) {
            if (this.huntingTargetUuid != null) {
                this.huntedTargets.add(this.huntingTargetUuid);
            }
            Vec3 nextSearchOrigin = target == null
                    ? this.position()
                    : target.getBoundingBox().getCenter();
            this.chooseNextHuntingTargetOrReturn(nextSearchOrigin, owner);
            return;
        }

        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 toTarget = targetCenter.subtract(this.position());
        double distance = toTarget.length();
        if (distance > HUNT_ABANDON_RANGE || !this.hasClearHuntingPath(this.position(), target)) {
            this.huntedTargets.add(target.getUUID());
            this.chooseNextHuntingTargetOrReturn(this.position(), owner);
            return;
        }

        if (distance <= HUNT_HIT_RANGE) {
            this.hitHuntingTarget(target);
            return;
        }

        this.updateHuntingVelocity(targetCenter, distance);
        this.hitEntitiesAlongMotion();
        if (this.getFanState() != FanState.HUNTING
                || !target.getUUID().equals(this.huntingTargetUuid)) {
            return;
        }

        Vec3 nextPosition = this.position().add(this.getDeltaMovement());
        if (distance < 2.5D && nextPosition.distanceTo(targetCenter) > distance) {
            this.hitHuntingTarget(target);
        }
    }

    private void updateHuntingVelocity(Vec3 targetPosition, double distance) {
        Vec3 desiredDirection = targetPosition.subtract(this.position()).normalize();
        Vec3 current = this.getDeltaMovement();
        if (current.lengthSqr() < 1.0E-3D) {
            current = desiredDirection.scale(HUNT_SPEED);
        }

        Vec3 desiredVelocity = desiredDirection.scale(HUNT_SPEED);
        double steering = distance < 3.0D ? 0.48D : HUNT_TURN_RATE;
        Vec3 newVelocity = current.scale(1.0D - steering)
                .add(desiredVelocity.scale(steering));
        if (newVelocity.lengthSqr() > 1.0E-3D) {
            newVelocity = newVelocity.normalize().scale(HUNT_SPEED);
        } else {
            newVelocity = desiredVelocity;
        }
        this.setDeltaMovement(newVelocity);
        this.hasImpulse = true;
    }

    private void hitHuntingTarget(LivingEntity target) {
        if (this.level().isClientSide || this.getFanState() != FanState.HUNTING
                || this.huntingTargetUuid == null
                || !this.huntingTargetUuid.equals(target.getUUID())) {
            return;
        }

        UUID targetUuid = target.getUUID();
        if (!this.huntedTargets.add(targetUuid)) {
            this.chooseNextHuntingTargetOrReturn(target.getBoundingBox().getCenter(),
                    this.findServerOwner());
            return;
        }

        float damage = Math.max(3.5F, 6.0F * (1.0F - this.huntingHop * 0.10F));
        boolean damaged = target.hurt(
                this.damageSources().thrown(this, this.getOwner()),
                damage
        );
        if (damaged) {
            Vec3 movement = this.getDeltaMovement();
            target.knockback(0.18D, -movement.x, -movement.z);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            this.spawnHuntingHitEffects(serverLevel, target, this.huntingHop);
        }

        this.huntingHop++;
        this.chooseNextHuntingTargetOrReturn(target.getBoundingBox().getCenter(),
                this.findServerOwner());
    }

    private void chooseNextHuntingTargetOrReturn(Vec3 origin, ServerPlayer owner) {
        if (owner == null || this.huntingHop >= HUNT_MAX_TARGETS) {
            this.beginReturn(owner);
            return;
        }

        LivingEntity next = this.findNextHuntingTarget(origin, owner);
        if (next == null) {
            this.beginReturn(owner);
            return;
        }
        this.setHuntingTarget(next);
    }

    private LivingEntity findNextHuntingTarget(Vec3 origin, ServerPlayer owner) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        for (UUID targetUuid : this.huntingLockedTargets) {
            if (this.huntedTargets.contains(targetUuid)
                    || targetUuid.equals(this.huntingTargetUuid)) {
                continue;
            }
            Entity entity = serverLevel.getEntity(targetUuid);
            if (!(entity instanceof LivingEntity candidate)
                    || !candidate.isAlive()
                    || candidate.isSpectator()
                    || candidate == owner
                    || !owner.canAttack(candidate)) {
                continue;
            }
            double distance = candidate.getBoundingBox().getCenter().distanceToSqr(origin);
            if (distance > HUNT_ABANDON_RANGE * HUNT_ABANDON_RANGE
                    || !this.hasClearHuntingPath(this.position(), candidate)) {
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private void setHuntingTarget(LivingEntity target) {
        this.huntingTargetUuid = target.getUUID();
        Vec3 direction = target.getBoundingBox().getCenter().subtract(this.position());
        if (direction.lengthSqr() > 1.0E-3D) {
            Vec3 desired = direction.normalize().scale(HUNT_SPEED);
            Vec3 redirected = this.getDeltaMovement().scale(0.35D)
                    .add(desired.scale(0.65D));
            this.setDeltaMovement(redirected.lengthSqr() > 1.0E-3D
                    ? redirected.normalize().scale(HUNT_SPEED)
                    : desired);
            this.hasImpulse = true;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            this.spawnHuntingTurnEffects(serverLevel, target);
        }
    }

    private LivingEntity findHuntingTarget() {
        if (this.huntingTargetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.huntingTargetUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private boolean hasClearHuntingPath(Vec3 start, LivingEntity target) {
        Vec3 end = target.getBoundingBox().getCenter();
        HitResult hit = this.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    private void tickClientStuckAttachment() {
        if (this.clientStuckEntityId < 0 || this.clientStuckFollowTicks <= 0) {
            this.clientStuckEntityId = -1;
            return;
        }

        FanState state = this.getFanState();
        boolean followsTarget = state == FanState.PIERCING
                || state == FanState.STUCK_ENTITY
                || state == FanState.BURIAL_VORTEX
                || state == FanState.RIVEN_SEQUENCE;
        Entity target = this.level().getEntity(this.clientStuckEntityId);
        if (!followsTarget || !(target instanceof LivingEntity living) || !living.isAlive()) {
            this.clientStuckEntityId = -1;
            this.clientStuckFollowTicks = 0;
            return;
        }

        this.updateStuckAttachment(living);
        if (--this.clientStuckFollowTicks <= 0) {
            this.clientStuckEntityId = -1;
        }
    }

    private void updateStuckAttachment(LivingEntity target) {
        float bodyYaw = target.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 rotatedForward = this.stuckLocalForward.yRot(-bodyYaw);
        if (rotatedForward.lengthSqr() > 1.0E-6D) {
            this.stuckForward = rotatedForward.normalize();
            this.updateRotationFromDirection();
        }
        Vec3 position = target.position().add(this.stuckOffset.yRot(-bodyYaw));
        this.setPos(position.x, position.y, position.z);
        this.setDeltaMovement(Vec3.ZERO);
    }

    private Vec3 getBurialCenter(LivingEntity anchor) {
        return anchor.getBoundingBox().getCenter();
    }

    private void captureBurialTargets(Vec3 center, LivingEntity anchor) {
        Entity owner = this.getOwner();
        AABB area = new AABB(center, center).inflate(BURIAL_PULL_RADIUS);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> target.isAlive()
                        && !target.isSpectator()
                        && !target.isPassenger()
                        && !target.isVehicle()
                        && target != owner
                        && target != anchor
        );

        for (LivingEntity target : targets) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            if (targetCenter.distanceToSqr(center) > BURIAL_PULL_RADIUS * BURIAL_PULL_RADIUS
                    || !this.hasBurialLineOfSight(center, target)) {
                continue;
            }
            this.burialTargetPhysics.putIfAbsent(target.getUUID(), target.noPhysics);
            target.noPhysics = true;
        }
    }

    private void moveBurialTargets(Vec3 collectionPoint) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Iterator<Map.Entry<UUID, Boolean>> iterator = this.burialTargetPhysics.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Boolean> entry = iterator.next();
            Entity entity = serverLevel.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                if (entity != null) {
                    entity.noPhysics = entry.getValue();
                }
                iterator.remove();
                continue;
            }

            target.noPhysics = true;
            Vec3 toPoint = collectionPoint.subtract(target.position());
            double distance = toPoint.length();
            if (distance <= 0.10D) {
                target.setPos(collectionPoint.x, collectionPoint.y, collectionPoint.z);
            } else {
                double step = Math.min(distance, Mth.clamp(distance * 0.18D, 0.10D, 0.40D));
                Vec3 next = target.position().add(toPoint.scale(step / distance));
                target.setPos(next.x, next.y, next.z);
            }
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0.0F;
            target.hurtMarked = true;
        }
    }

    private void damageBurialTargets(LivingEntity anchor) {
        this.damageBurialTarget(anchor);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (UUID targetUuid : this.burialTargetPhysics.keySet()) {
            Entity entity = serverLevel.getEntity(targetUuid);
            if (entity instanceof LivingEntity target && target.isAlive()) {
                this.damageBurialTarget(target);
            }
        }
    }

    private void damageBurialTarget(LivingEntity target) {
        Vec3 movementBeforeDamage = target.getDeltaMovement();
        int previousInvulnerableTime = target.invulnerableTime;
        target.invulnerableTime = 0;
        target.hurt(this.damageSources().thrown(this, this.getOwner()), BURIAL_DAMAGE);
        target.invulnerableTime = Math.max(target.invulnerableTime, previousInvulnerableTime);
        target.setDeltaMovement(movementBeforeDamage);
        target.hurtMarked = true;
    }

    private void releaseBurialTargets() {
        if (this.burialTargetPhysics.isEmpty()) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            for (Map.Entry<UUID, Boolean> entry : this.burialTargetPhysics.entrySet()) {
                Entity entity = serverLevel.getEntity(entry.getKey());
                if (entity != null) {
                    entity.noPhysics = entry.getValue();
                    entity.hurtMarked = true;
                }
            }
        }
        this.burialTargetPhysics.clear();
    }

    private boolean hasBurialLineOfSight(Vec3 center, LivingEntity target) {
        HitResult blockHit = this.level().clip(new ClipContext(
                center,
                target.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        return blockHit.getType() == HitResult.Type.MISS;
    }

    private void performBurialSlash(Vec3 center) {
        Entity owner = this.getOwner();
        AABB area = new AABB(center, center).inflate(BURIAL_SLASH_RADIUS);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> target.isAlive() && !target.isSpectator() && target != owner
        );

        for (LivingEntity target : targets) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 away = targetCenter.subtract(center);
            if (away.lengthSqr() > BURIAL_SLASH_RADIUS * BURIAL_SLASH_RADIUS
                    || !this.hasBurialLineOfSight(center, target)) {
                continue;
            }

            Vec3 movementBeforeDamage = target.getDeltaMovement();
            int previousInvulnerableTime = target.invulnerableTime;
            target.invulnerableTime = 0;
            boolean damaged = target.hurt(
                    this.damageSources().thrown(this, this.getOwner()),
                    BURIAL_SLASH_DAMAGE
            );
            target.invulnerableTime = Math.max(target.invulnerableTime, previousInvulnerableTime);
            target.setDeltaMovement(movementBeforeDamage);
            if (damaged && target.isAlive()) {
                Vec3 horizontalAway = new Vec3(away.x, 0.0D, away.z);
                if (horizontalAway.lengthSqr() > 1.0E-6D) {
                    horizontalAway = horizontalAway.normalize();
                }
                target.setDeltaMovement(movementBeforeDamage.add(
                        horizontalAway.x * 0.45D,
                        0.18D,
                        horizontalAway.z * 0.45D
                ));
                target.hurtMarked = true;
            }
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            this.spawnBurialSlashEffects(serverLevel, center);
        }
    }

    private PiercingArt getPiercingArt() {
        ItemStack fan = this.getItem();
        if (EnchantmentHelper.getItemEnchantmentLevel(
                GuaniaoEnchantments.RIVEN_PLUME.get(), fan) > 0) {
            return PiercingArt.RIVEN;
        }
        if (EnchantmentHelper.getItemEnchantmentLevel(
                GuaniaoEnchantments.BURIAL_PLUME.get(), fan) > 0) {
            return PiercingArt.BURIAL;
        }
        return PiercingArt.NORMAL;
    }

    private void performRivenBurst(Vec3 center) {
        Entity owner = this.getOwner();
        LivingEntity mainTarget = this.findStuckEntity();
        AABB area = new AABB(center, center).inflate(RIVEN_BURST_RADIUS);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> target.isAlive() && !target.isSpectator() && target != owner
        );

        for (LivingEntity target : targets) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 away = targetCenter.subtract(center);
            double distance = away.length();
            if (distance > RIVEN_BURST_RADIUS
                    || !this.hasBurialLineOfSight(center, target)) {
                continue;
            }

            float damage;
            if (target == mainTarget) {
                damage = RIVEN_MAIN_DAMAGE;
            } else {
                float factor = 1.0F - (float)(distance / RIVEN_BURST_RADIUS);
                damage = Mth.lerp(factor, RIVEN_MIN_SPLASH_DAMAGE, RIVEN_MAX_SPLASH_DAMAGE);
            }

            Vec3 movementBeforeDamage = target.getDeltaMovement();
            int previousInvulnerableTime = target.invulnerableTime;
            target.invulnerableTime = 0;
            boolean damaged = target.hurt(
                    this.damageSources().thrown(this, this.getOwner()),
                    damage
            );
            target.invulnerableTime = Math.max(target.invulnerableTime, previousInvulnerableTime);
            target.setDeltaMovement(movementBeforeDamage);
            if (!damaged || !target.isAlive()) {
                continue;
            }

            Vec3 horizontalAway = new Vec3(away.x, 0.0D, away.z);
            if (horizontalAway.lengthSqr() < 1.0E-6D) {
                target.setDeltaMovement(movementBeforeDamage.add(0.0D, 0.30D, 0.0D));
            } else {
                horizontalAway = horizontalAway.normalize();
                target.setDeltaMovement(movementBeforeDamage.add(
                        horizontalAway.x * 0.55D,
                        0.22D,
                        horizontalAway.z * 0.55D
                ));
            }
            target.hurtMarked = true;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            this.spawnRivenBurstEffects(serverLevel, center);
        }
    }

    private void performRivenReformStrike(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        Vec3 movementBeforeDamage = target.getDeltaMovement();
        int previousInvulnerableTime = target.invulnerableTime;
        target.invulnerableTime = 0;
        boolean damaged = target.hurt(
                this.damageSources().thrown(this, this.getOwner()),
                RIVEN_REFORM_DAMAGE
        );
        target.invulnerableTime = Math.max(target.invulnerableTime, previousInvulnerableTime);
        target.setDeltaMovement(movementBeforeDamage);
        target.hurtMarked = true;

        if (damaged && this.level() instanceof ServerLevel serverLevel) {
            Vec3 hitCenter = target.getBoundingBox().getCenter();
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    hitCenter.x, hitCenter.y, hitCenter.z,
                    20, 0.42D, 0.42D, 0.42D, 0.20D);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    hitCenter.x, hitCenter.y, hitCenter.z,
                    14, 0.34D, 0.34D, 0.34D, 0.15D);
            this.level().playSound(null, hitCenter.x, hitCenter.y, hitCenter.z,
                    SoundEvents.PLAYER_ATTACK_CRIT,
                    SoundSource.PLAYERS, 1.15F, 0.92F);
        }
    }

    private void tickStuckBlock(ServerPlayer owner) {
        if (this.pulloutTicks > 0) {
            this.tickPullout(owner);
            return;
        }
        if (this.level().getBlockState(this.stuckBlockPos).isAir()) {
            this.beginPullout();
            return;
        }

        this.setPos(this.stuckPosition.x, this.stuckPosition.y, this.stuckPosition.z);
        this.setDeltaMovement(Vec3.ZERO);
        if (++this.stuckTicks >= BLOCK_STUCK_DURATION_TICKS) {
            this.beginPullout();
        }
    }

    private void beginPullout() {
        if (this.pulloutTicks > 0) {
            return;
        }
        this.releaseBurialTargets();
        this.removeStuckSlowdown();
        this.pulloutTicks = 1;
        this.setPos(this.position().subtract(this.stuckForward.scale(0.05D)));
        if (this.level() instanceof ServerLevel serverLevel) {
            this.spawnPulloutEffects(serverLevel);
        }
    }

    private void tickPullout(ServerPlayer owner) {
        this.setPos(this.position().subtract(this.stuckForward.scale(0.05D)));
        this.setDeltaMovement(Vec3.ZERO);
        if (++this.pulloutTicks >= PULLOUT_DURATION_TICKS) {
            this.beginReturn(owner);
        }
    }

    private void captureStuckDirection() {
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() > 1.0E-6D) {
            this.stuckForward = movement.normalize();
        }
        this.updateRotationFromDirection();
    }

    private void updateRotationFromDirection() {
        double horizontal = Math.sqrt(this.stuckForward.x * this.stuckForward.x
                + this.stuckForward.z * this.stuckForward.z);
        double radiansToDegrees = 180.0D / Math.PI;
        this.setYRot((float)(Mth.atan2(this.stuckForward.x, this.stuckForward.z) * radiansToDegrees));
        this.setXRot((float)(Mth.atan2(this.stuckForward.y, horizontal) * radiansToDegrees));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private LivingEntity findStuckEntity() {
        if (this.stuckEntityUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity target = serverLevel.getEntity(this.stuckEntityUuid);
        return target instanceof LivingEntity living ? living : null;
    }

    private void applyStuckSlowdown(LivingEntity target) {
        AttributeInstance movementSpeed = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null && movementSpeed.getModifier(this.getUUID()) == null) {
            movementSpeed.addTransientModifier(new AttributeModifier(
                    this.getUUID(), "Feather fan pin slowdown", STUCK_SLOW_AMOUNT,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private void removeStuckSlowdown() {
        LivingEntity target = this.findStuckEntity();
        if (target == null) {
            return;
        }
        AttributeInstance movementSpeed = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(this.getUUID());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.level().isClientSide || this.isNonCollidingState()) {
            return;
        }

        if (this.getFanState() == FanState.PIERCING) {
            this.stickToBlock(result);
            return;
        }

        if (this.getFanState() == FanState.HUNTING) {
            Vec3 movement = this.getDeltaMovement();
            if (movement.lengthSqr() > 1.0E-6D) {
                this.setPos(result.getLocation().subtract(movement.normalize().scale(0.05D)));
            }
            if (this.huntingTargetUuid != null) {
                this.huntedTargets.add(this.huntingTargetUuid);
            }
            this.chooseNextHuntingTargetOrReturn(this.position(), this.findServerOwner());
            return;
        }

        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() > 1.0E-6D) {
            this.setPos(result.getLocation().subtract(movement.normalize().scale(0.05D)));
        }
        this.beginReturn(this.findServerOwner());
    }

    private void beginReturn(ServerPlayer owner) {
        if (this.isReturning()) {
            return;
        }

        FanState previousState = this.getFanState();
        this.releaseBurialTargets();
        this.removeStuckSlowdown();
        this.setFanState(FanState.RETURNING);
        this.noPhysics = true;
        this.returningTicks = 0;
        Vec3 current = this.getDeltaMovement();
        if (current.lengthSqr() < 1.0E-6D) {
            current = this.stuckForward.scale(-1.0D);
        }
        current = current.normalize();
        Vec3 desired = owner == null ? current.scale(-1.0D) : returnTarget(owner).subtract(this.position()).normalize();
        Vec3 perpendicular = new Vec3(-current.z, 0.0D, current.x);
        if ((this.getId() & 1) == 0) {
            perpendicular = perpendicular.scale(-1.0D);
        }
        Vec3 turn = current.scale(0.45D).add(perpendicular.scale(0.65D)).add(desired.scale(0.40D));
        if (turn.lengthSqr() < 1.0E-6D) {
            turn = desired;
        }
        this.setDeltaMovement(turn.normalize().scale(this.returnSpeed));
        this.hasImpulse = true;
        if (this.level() instanceof ServerLevel serverLevel) {
            if (previousState == FanState.OUTBOUND_SPIN) {
                this.spawnTurnEffects(serverLevel);
            } else if (previousState == FanState.HUNTING) {
                this.spawnHuntingReturnEffects(serverLevel);
            } else {
                this.spawnPiercingReturnEffects(serverLevel);
            }
        }
        this.huntingTargetUuid = null;
        this.huntedTargets.clear();
        this.huntingLockedTargets.clear();
    }

    private boolean tickReturning(ServerPlayer owner) {
        this.returningTicks++;
        Vec3 target = returnTarget(owner);
        Vec3 toOwner = target.subtract(this.position());
        double distance = toOwner.length();
        if (distance <= 1.6D || this.returningTicks > 100) {
            this.returnFanToOwner(owner);
            return true;
        }

        Vec3 desiredDirection = toOwner.normalize();
        Vec3 current = this.getDeltaMovement();
        if (distance > 5.0D) {
            Vec3 desiredVelocity = desiredDirection.scale(this.returnSpeed);
            double alignment = current.lengthSqr() < 1.0E-6D ? 1.0D : current.normalize().dot(desiredDirection);
            double desiredWeight = alignment < -0.25D ? 0.65D : 0.45D;
            Vec3 steering = current.scale(1.0D - desiredWeight).add(desiredVelocity.scale(desiredWeight));
            if (steering.lengthSqr() > 1.0E-6D) {
                this.setDeltaMovement(steering.normalize().scale(this.returnSpeed));
            } else {
                this.setDeltaMovement(desiredVelocity);
            }
        } else {
            double speed = Mth.clamp(distance * 0.38D, 0.35D, this.returnSpeed);
            this.setDeltaMovement(desiredDirection.scale(speed));
        }
        this.hasImpulse = true;

        Vec3 next = this.position().add(this.getDeltaMovement());
        if (distance < 3.0D && next.distanceTo(target) > distance) {
            this.returnFanToOwner(owner);
            return true;
        }
        return false;
    }

    private void spawnFlightTrail(ServerLevel serverLevel) {
        Entity owner = this.getOwner();
        if (owner != null && this.distanceToSqr(owner) <= 2.5D * 2.5D) {
            return;
        }

        if (this.getFanState() == FanState.PIERCING) {
            this.spawnPiercingTrail(serverLevel);
            return;
        }
        if (this.getFanState() == FanState.HUNTING) {
            this.spawnHuntingTrail(serverLevel);
            return;
        }

        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() < 0.01D) {
            return;
        }

        Vec3 forward = movement.normalize();
        Vec3 side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-4D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        Vec3 up = side.cross(forward).normalize();
        double phase = this.tickCount * 0.9D;
        double radius = this.isReturning() ? 0.15D : 0.12D;
        Vec3 center = this.position().subtract(forward.scale(0.18D));
        Vec3 offset = side.scale(Math.cos(phase) * radius).add(up.scale(Math.sin(phase) * radius));
        Vec3 opposite = offset.scale(-1.0D);

        if (this.isReturning()) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.x + offset.x, center.y + offset.y, center.z + offset.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.x + opposite.x, center.y + opposite.y, center.z + opposite.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        } else {
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, center.x + offset.x, center.y + offset.y, center.z + offset.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, center.x + opposite.x, center.y + opposite.y, center.z + opposite.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (this.tickCount % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.06D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (this.tickCount % 6 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.14F, this.isReturning() ? 1.55F : 1.8F);
        }
    }

    private void spawnPiercingTrail(ServerLevel serverLevel) {
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() < 0.01D) {
            return;
        }

        Vec3 forward = movement.normalize();
        Vec3 side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-4D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        int segments = Mth.clamp((int)Math.ceil(movement.length() * 2.0D), 3, 6);
        double spacing = movement.length() / segments;
        for (int segment = 1; segment <= segments; segment++) {
            Vec3 center = this.position().subtract(forward.scale(segment * spacing));
            Vec3 lineOffset = side.scale(0.085D);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                    center.x + lineOffset.x, center.y + lineOffset.y, center.z + lineOffset.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                    center.x - lineOffset.x, center.y - lineOffset.y, center.z - lineOffset.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void spawnHuntingTrail(ServerLevel serverLevel) {
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() < 0.01D) {
            return;
        }

        Vec3 forward = movement.normalize();
        Vec3 center = this.position().subtract(forward.scale(0.24D));
        serverLevel.sendParticles(GuaniaoParticleTypes.HUNTING_STREAK.get(),
                center.x, center.y, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        if ((this.tickCount & 1) == 0) {
            serverLevel.sendParticles(ParticleTypes.WAX_ON,
                    center.x, center.y, center.z,
                    2, 0.10D, 0.10D, 0.10D, 0.018D);
        }
    }

    private void spawnHuntingHitEffects(ServerLevel serverLevel, LivingEntity target, int hop) {
        Vec3 center = target.getBoundingBox().getCenter();
        this.level().playSound(null, center.x, center.y, center.z,
                GuaniaoSoundEvents.FEATHER_FAN_HUNT_HIT.get(),
                SoundSource.PLAYERS, 0.92F, 1.05F + hop * 0.08F);
        serverLevel.sendParticles(GuaniaoParticleTypes.HUNTING_MARK.get(),
                center.x, center.y, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                center.x, center.y, center.z,
                6, 0.22D, 0.25D, 0.22D, 0.13D);
        serverLevel.sendParticles(ParticleTypes.WAX_ON,
                center.x, center.y, center.z,
                8, 0.28D, 0.30D, 0.28D, 0.08D);
    }

    private void spawnHuntingTurnEffects(ServerLevel serverLevel, LivingEntity nextTarget) {
        Vec3 nextCenter = nextTarget.getBoundingBox().getCenter();
        Vec3 direction = nextCenter.subtract(this.position());
        if (direction.lengthSqr() > 1.0E-4D) {
            direction = direction.normalize();
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                GuaniaoSoundEvents.FEATHER_FAN_HUNT_TURN.get(),
                SoundSource.PLAYERS, 0.72F, 1.0F + this.huntingHop * 0.08F);
        for (int i = -1; i <= 1; i++) {
            double ySpeed = direction.y * 0.08D + i * 0.018D;
            serverLevel.sendParticles(GuaniaoParticleTypes.HUNTING_STREAK.get(),
                    this.getX(), this.getY(), this.getZ(),
                    0,
                    direction.x * 0.15D,
                    ySpeed,
                    direction.z * 0.15D,
                    1.0D);
        }
        serverLevel.sendParticles(GuaniaoParticleTypes.HUNTING_MARK.get(),
                nextCenter.x, nextCenter.y, nextCenter.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private void spawnHuntingReturnEffects(ServerLevel serverLevel) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                GuaniaoSoundEvents.FEATHER_FAN_HUNT_TURN.get(),
                SoundSource.PLAYERS, 0.78F, 0.82F);
        serverLevel.sendParticles(GuaniaoParticleTypes.HUNTING_MARK.get(),
                this.getX(), this.getY(), this.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.WAX_ON,
                this.getX(), this.getY(), this.getZ(),
                7, 0.24D, 0.18D, 0.24D, 0.055D);
    }

    private void spawnTurnEffects(ServerLevel serverLevel) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 0.72F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.65F, 0.8F);
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0D * i / 12.0D;
            double radius = 0.55D;
            double particleX = this.getX() + Math.cos(angle) * radius;
            double particleZ = this.getZ() + Math.sin(angle) * radius;
            if ((i & 1) == 0) {
                serverLevel.sendParticles(ParticleTypes.WHITE_ASH, particleX, this.getY(), particleZ, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            } else {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, particleX, this.getY(), particleZ, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 4, 0.22D, 0.08D, 0.22D, 0.012D);
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private void spawnPiercingHitEffects(ServerLevel serverLevel, LivingEntity target, boolean damaged) {
        double hitY = target.getY(0.55D);
        this.level().playSound(null, target.getX(), hitY, target.getZ(),
                SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, damaged ? 0.65F : 0.35F, 1.55F);
        serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), hitY, target.getZ(),
                4, 0.14D, 0.14D, 0.14D, 0.10D);
        serverLevel.sendParticles(ParticleTypes.WHITE_ASH, target.getX(), hitY, target.getZ(),
                3, 0.13D, 0.13D, 0.13D, 0.018D);
    }

    private void spawnPinEffects(ServerLevel serverLevel) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.9F, 0.72F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.55F, 1.35F);
        serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(),
                3, 0.10D, 0.10D, 0.10D, 0.025D);
        serverLevel.sendParticles(ParticleTypes.WHITE_ASH, this.getX(), this.getY(), this.getZ(),
                5, 0.18D, 0.18D, 0.18D, 0.025D);
    }

    private void spawnBurialStartEffects(ServerLevel serverLevel, Vec3 center) {
        this.level().playSound(null, center.x, center.y, center.z,
                GuaniaoSoundEvents.FEATHER_FAN_BURIAL_VORTEX.get(),
                SoundSource.PLAYERS, 0.72F, 0.88F);
        this.level().playSound(null, center.x, center.y, center.z,
                SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.28F, 0.72F);
        for (int layer = 0; layer < 3; layer++) {
            double radius = 1.15D + layer * 0.72D;
            double y = center.y - 0.32D + layer * 0.36D;
            int segments = 8 + layer * 3;
            for (int i = 0; i < segments; i++) {
                double angle = Math.PI * 2.0D * i / segments + layer * 0.38D;
                double tangentialSpeed = 0.045D + layer * 0.008D;
                double xSpeed = -Math.sin(angle) * tangentialSpeed - Math.cos(angle) * 0.018D;
                double zSpeed = Math.cos(angle) * tangentialSpeed - Math.sin(angle) * 0.018D;
                serverLevel.sendParticles(
                        GuaniaoParticleTypes.BURIAL_CYCLONE.get(),
                        center.x + Math.cos(angle) * radius,
                        y + Math.sin(angle * 2.0D) * 0.10D,
                        center.z + Math.sin(angle) * radius,
                        0,
                        xSpeed,
                        0.018D + layer * 0.006D,
                        zSpeed,
                        1.0D
                );
            }
        }
        serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y - 0.25D, center.z,
                12, 1.15D, 0.12D, 1.15D, 0.035D);
        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z,
                12, 1.25D, 0.55D, 1.25D, 0.025D);
    }

    private void spawnBurialVortexEffects(ServerLevel serverLevel, Vec3 center) {
        if ((this.stuckTicks & 1) != 0) {
            return;
        }

        double progress = Mth.clamp(this.stuckTicks / (double)BURIAL_DURATION_TICKS, 0.0D, 1.0D);
        double baseAngle = this.stuckTicks * (0.48D + progress * 0.20D);
        for (int layer = 0; layer < 5; layer++) {
            double layerProgress = layer / 4.0D;
            double radius = Mth.lerp(layerProgress, 0.72D, 2.55D) * Mth.lerp(progress, 1.0D, 0.86D);
            double y = center.y - 0.48D + layerProgress * 2.10D;
            for (int arm = 0; arm < 3; arm++) {
                double angle = baseAngle
                        + Math.PI * 2.0D * arm / 3.0D
                        + layer * 0.54D;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                double tangentialSpeed = 0.075D + layerProgress * 0.045D;
                double inwardSpeed = 0.018D + progress * 0.012D;
                double xSpeed = -Math.sin(angle) * tangentialSpeed - Math.cos(angle) * inwardSpeed;
                double zSpeed = Math.cos(angle) * tangentialSpeed - Math.sin(angle) * inwardSpeed;
                serverLevel.sendParticles(GuaniaoParticleTypes.BURIAL_CYCLONE.get(), x, y, z,
                        0, xSpeed, 0.028D + layerProgress * 0.018D, zSpeed, 1.0D);
            }
        }

        for (int stream = 0; stream < 6; stream++) {
            double inwardProgress = (this.stuckTicks * 0.095D + stream / 6.0D) % 1.0D;
            double radius = Mth.lerp(inwardProgress, 4.8D, 0.55D);
            double angle = -this.stuckTicks * 0.22D
                    + stream * Mth.TWO_PI / 6.0D
                    + inwardProgress * 2.2D;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y - 0.34D + inwardProgress * 0.70D;
            double xSpeed = -Math.cos(angle) * 0.095D - Math.sin(angle) * 0.045D;
            double zSpeed = -Math.sin(angle) * 0.095D + Math.cos(angle) * 0.045D;
            serverLevel.sendParticles(GuaniaoParticleTypes.BURIAL_CYCLONE.get(), x, y, z,
                    0, xSpeed, 0.022D, zSpeed, 1.0D);
        }

        serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y - 0.30D, center.z,
                4, 1.15D, 0.08D, 1.15D, 0.025D);
        serverLevel.sendParticles(ParticleTypes.WHITE_ASH, center.x, center.y + 0.45D, center.z,
                3, 1.35D, 0.70D, 1.35D, 0.018D);
        if (this.stuckTicks % 10 == 0 && this.stuckTicks < BURIAL_DURATION_TICKS) {
            this.level().playSound(null, center.x, center.y, center.z,
                    GuaniaoSoundEvents.FEATHER_FAN_BURIAL_VORTEX.get(),
                    SoundSource.PLAYERS, 0.64F,
                    (float)(0.92D + progress * 0.14D));
        }
    }

    private void spawnBurialSlashEffects(ServerLevel serverLevel, Vec3 center) {
        this.level().playSound(null, center.x, center.y, center.z,
                GuaniaoSoundEvents.FEATHER_FAN_BURIAL_SLASH.get(),
                SoundSource.PLAYERS, 1.20F, 1.0F);
        this.level().playSound(null, center.x, center.y, center.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.38F, 1.28F);
        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.20D + ring * 1.25D;
            int segments = 16 + ring * 8;
            for (int i = 0; i < segments; i++) {
                double angle = Math.PI * 2.0D * i / segments + ring * 0.22D;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                serverLevel.sendParticles(GuaniaoParticleTypes.BURIAL_WIND.get(),
                        x, center.y - 0.08D + ring * 0.11D, z,
                        1, Math.cos(angle) * 0.11D, 0.035D, Math.sin(angle) * 0.11D, 0.0D);
                if (ring == 2 && i % 4 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            x, center.y + 0.14D, z,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
        serverLevel.sendParticles(ParticleTypes.POOF, center.x, center.y - 0.12D, center.z,
                18, 1.15D, 0.24D, 1.15D, 0.12D);
        serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
                24, 1.45D, 0.38D, 1.45D, 0.16D);
        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.20D, center.z,
                32, 2.65D, 0.48D, 2.65D, 0.15D);
        serverLevel.sendParticles(ParticleTypes.WHITE_ASH, center.x, center.y + 0.20D, center.z,
                38, 3.45D, 0.55D, 3.45D, 0.20D);
    }

    private void spawnRivenStartEffects(ServerLevel serverLevel, Vec3 center) {
        this.level().playSound(null, center.x, center.y, center.z,
                GuaniaoSoundEvents.FEATHER_FAN_RIVEN_PIN.get(),
                SoundSource.PLAYERS, 0.90F, 1.0F);
        serverLevel.sendParticles(GuaniaoParticleTypes.RIVEN_SPLIT.get(),
                center.x, center.y, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                8, 0.24D, 0.24D, 0.24D, 0.055D);
        serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                4, 0.14D, 0.14D, 0.14D, 0.025D);
    }

    private void spawnRivenSequenceEffects(ServerLevel serverLevel, Vec3 center) {
        if (this.rivenTicks == RIVEN_PREPARE_END) {
            this.level().playSound(null, center.x, center.y, center.z,
                    GuaniaoSoundEvents.FEATHER_FAN_RIVEN_SPLIT.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                    18, 0.34D, 0.34D, 0.34D, 0.12D);
            for (int i = 0; i < 8; i++) {
                double angle = Mth.TWO_PI * i / 8.0D;
                double ySpeed = Math.sin(angle * 2.0D) * 0.055D;
                serverLevel.sendParticles(GuaniaoParticleTypes.RIVEN_SPLIT.get(),
                        center.x, center.y, center.z,
                        0,
                        Math.cos(angle) * 0.21D,
                        ySpeed,
                        Math.sin(angle) * 0.21D,
                        1.0D);
                serverLevel.sendParticles(GuaniaoParticleTypes.RIVEN_SPLIT.get(),
                        center.x, center.y, center.z,
                        0,
                        Math.cos(angle + 0.12D) * 0.14D,
                        -ySpeed * 0.65D,
                        Math.sin(angle + 0.12D) * 0.14D,
                        1.0D);
            }
            return;
        }

        if (this.rivenTicks > RIVEN_PREPARE_END && this.rivenTicks < RIVEN_SPLIT_END) {
            if ((this.rivenTicks & 1) == 0) {
                float radius = getRivenArrayRadius(this.rivenTicks);
                double rotation = getRivenRingRotation(this.rivenTicks);
                for (int i = 0; i < 8; i++) {
                    double baseAngle = Mth.TWO_PI * i / 8.0D;
                    double angle = baseAngle + rotation;
                    double y = Math.sin(baseAngle * 2.0D) * 0.85D * radius / 3.8D;
                    double outward = 0.075D + radius * 0.012D;
                    double tangent = 0.028D;
                    serverLevel.sendParticles(GuaniaoParticleTypes.RIVEN_SPLIT.get(),
                            center.x + Math.cos(angle) * radius,
                            center.y + y,
                            center.z + Math.sin(angle) * radius,
                            0,
                            Math.cos(angle) * outward - Math.sin(angle) * tangent,
                            Math.sin(baseAngle * 2.0D) * 0.018D,
                            Math.sin(angle) * outward + Math.cos(angle) * tangent,
                            1.0D);
                }
            }
            return;
        }

        if (this.rivenTicks == RIVEN_SPLIT_END) {
            this.level().playSound(null, center.x, center.y, center.z,
                    GuaniaoSoundEvents.FEATHER_FAN_RIVEN_LOCK.get(),
                    SoundSource.PLAYERS, 0.95F, 1.0F);
            double rotation = getRivenRingRotation(this.rivenTicks);
            for (int i = 0; i < 8; i++) {
                double baseAngle = Mth.TWO_PI * i / 8.0D;
                double angle = baseAngle + rotation;
                double y = Math.sin(baseAngle * 2.0D) * 0.85D;
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        center.x + Math.cos(angle) * 3.8D,
                        center.y + y,
                        center.z + Math.sin(angle) * 3.8D,
                        3, 0.08D, 0.08D, 0.08D, 0.015D);
                serverLevel.sendParticles(GuaniaoParticleTypes.RIVEN_SPLIT.get(),
                        center.x + Math.cos(angle) * 3.8D,
                        center.y + y,
                        center.z + Math.sin(angle) * 3.8D,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            return;
        }

        if (this.rivenTicks > RIVEN_SPLIT_END && this.rivenTicks < RIVEN_LOCK_END) {
            if ((this.rivenTicks & 1) == 0) {
                double rotation = getRivenRingRotation(this.rivenTicks);
                for (int i = 0; i < 8; i++) {
                    double baseAngle = Mth.TWO_PI * i / 8.0D;
                    double angle = baseAngle + rotation;
                    double y = Math.sin(baseAngle * 2.0D) * 0.85D;
                    serverLevel.sendParticles(GuaniaoParticleTypes.RIVEN_SPLIT.get(),
                            center.x + Math.cos(angle) * 3.8D,
                            center.y + y,
                            center.z + Math.sin(angle) * 3.8D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            return;
        }

        if (this.rivenTicks >= RIVEN_LOCK_END && this.rivenTicks < RIVEN_CONVERGE_END) {
            float radius = getRivenArrayRadius(this.rivenTicks);
            double heightScale = radius / 3.8D;
            double rotation = getRivenRingRotation(this.rivenTicks);
            for (int i = 0; i < 8; i++) {
                double baseAngle = Mth.TWO_PI * i / 8.0D;
                double angle = baseAngle + rotation;
                double y = Math.sin(baseAngle * 2.0D) * 0.85D * heightScale;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                serverLevel.sendParticles(GuaniaoParticleTypes.RIVEN_STREAK.get(),
                        x, center.y + y, z,
                        0,
                        -Math.cos(angle) * 0.21D,
                        -y * 0.065D,
                        -Math.sin(angle) * 0.21D,
                        1.0D);
            }
            return;
        }

        if (this.rivenTicks > RIVEN_CONVERGE_END && this.rivenTicks < RIVEN_END) {
            if ((this.rivenTicks & 1) == 0) {
                double stormProgress = (this.rivenTicks - RIVEN_CONVERGE_END)
                        / (double)(RIVEN_END - RIVEN_CONVERGE_END);
                for (int i = 0; i < 8; i++) {
                    double angle = this.rivenTicks * 0.58D + Mth.TWO_PI * i / 8.0D;
                    double radius = Mth.lerp(stormProgress, 2.15D, 0.52D);
                    double y = Math.sin(angle * 1.7D) * 0.72D;
                    double tangential = 0.10D + stormProgress * 0.05D;
                    serverLevel.sendParticles(GuaniaoParticleTypes.RIVEN_SPLIT.get(),
                            center.x + Math.cos(angle) * radius,
                            center.y + y,
                            center.z + Math.sin(angle) * radius,
                            0,
                            -Math.sin(angle) * tangential - Math.cos(angle) * 0.04D,
                            0.025D - y * 0.018D,
                            Math.cos(angle) * tangential - Math.sin(angle) * 0.04D,
                            1.0D);
                }
            }
            if (this.rivenTicks == RIVEN_CONVERGE_END + 4) {
                for (int i = 0; i < 8; i++) {
                    double angle = Mth.TWO_PI * i / 8.0D + 0.22D;
                    serverLevel.sendParticles(GuaniaoParticleTypes.RIVEN_SPLIT.get(),
                            center.x, center.y + 0.18D, center.z,
                            0,
                            Math.cos(angle) * 0.13D,
                            Math.sin(angle * 2.0D) * 0.035D,
                            Math.sin(angle) * 0.13D,
                            1.0D);
                }
            }
        }
    }

    private void spawnRivenBurstEffects(ServerLevel serverLevel, Vec3 center) {
        this.level().playSound(null, center.x, center.y, center.z,
                GuaniaoSoundEvents.FEATHER_FAN_RIVEN_BURST.get(),
                SoundSource.PLAYERS, 1.25F, 1.0F);
        this.level().playSound(null, center.x, center.y, center.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.38F, 1.35F);

        for (int ray = 0; ray < 8; ray++) {
            double angle = Mth.TWO_PI * ray / 8.0D + 0.18D;
            for (int step = 1; step <= 7; step++) {
                double radius = step * 0.48D;
                double y = Math.sin(ray * Mth.HALF_PI) * 0.18D * (1.0D - step / 8.0D);
                serverLevel.sendParticles(GuaniaoParticleTypes.RIVEN_STREAK.get(),
                        center.x + Math.cos(angle) * radius,
                        center.y + y,
                        center.z + Math.sin(angle) * radius,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            serverLevel.sendParticles(GuaniaoParticleTypes.RIVEN_SPLIT.get(),
                    center.x + Math.cos(angle) * 2.45D,
                    center.y,
                    center.z + Math.sin(angle) * 2.45D,
                    0,
                    Math.cos(angle) * 0.12D,
                    Math.sin(ray * Mth.HALF_PI) * 0.025D,
                    Math.sin(angle) * 0.12D,
                    1.0D);
        }

        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                36, 1.05D, 0.82D, 1.05D, 0.23D);
        serverLevel.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z,
                28, 1.45D, 0.92D, 1.45D, 0.20D);
        serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                16, 0.82D, 0.62D, 0.82D, 0.13D);
        serverLevel.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z,
                24, 1.75D, 1.05D, 1.75D, 0.28D);
    }

    private void spawnRivenReformEffects(ServerLevel serverLevel, Vec3 center) {
        this.level().playSound(null, center.x, center.y, center.z,
                SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.72F, 1.45F);
        serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                8, 0.30D, 0.30D, 0.30D, 0.06D);
        for (int i = 0; i < 8; i++) {
            double angle = Mth.TWO_PI * i / 8.0D;
            serverLevel.sendParticles(GuaniaoParticleTypes.RIVEN_SPLIT.get(),
                    center.x + Math.cos(angle) * 1.35D,
                    center.y + Math.sin(angle * 2.0D) * 0.28D,
                    center.z + Math.sin(angle) * 1.35D,
                    0,
                    -Math.cos(angle) * 0.16D,
                    -Math.sin(angle * 2.0D) * 0.025D,
                    -Math.sin(angle) * 0.16D,
                    1.0D);
        }
    }

    public static float getRivenArrayRadius(float age) {
        if (age < RIVEN_PREPARE_END) {
            return 0.0F;
        }
        if (age < RIVEN_SPLIT_END) {
            float progress = (age - RIVEN_PREPARE_END)
                    / (RIVEN_SPLIT_END - RIVEN_PREPARE_END);
            float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
            return Mth.lerp(eased, 0.30F, 3.80F);
        }
        if (age < RIVEN_LOCK_END) {
            return 3.80F;
        }
        if (age < RIVEN_CONVERGE_END) {
            float progress = (age - RIVEN_LOCK_END)
                    / (RIVEN_CONVERGE_END - RIVEN_LOCK_END);
            float eased = progress * progress * progress;
            return Mth.lerp(eased, 3.80F, 0.15F);
        }
        return 0.0F;
    }

    public static float getRivenRingRotation(float age) {
        float progress = Mth.clamp(
                (age - RIVEN_PREPARE_END) / (RIVEN_SPLIT_END - RIVEN_PREPARE_END),
                0.0F,
                1.0F
        );
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress) * (1.0F - progress);
        return eased * 0.95F;
    }

    private void spawnStuckPulse(ServerLevel serverLevel, LivingEntity target) {
        double y = target.getY(0.55D);
        this.level().playSound(null, target.getX(), y, target.getZ(),
                SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.22F, 1.75F);
        serverLevel.sendParticles(ParticleTypes.WHITE_ASH, this.getX(), this.getY(), this.getZ(),
                2, 0.08D, 0.08D, 0.08D, 0.014D);
        serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(),
                1, 0.04D, 0.04D, 0.04D, 0.008D);
    }

    private void spawnPulloutEffects(ServerLevel serverLevel) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.75F, 1.35F);
        serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(),
                2, 0.09D, 0.09D, 0.09D, 0.018D);
        serverLevel.sendParticles(ParticleTypes.WHITE_ASH, this.getX(), this.getY(), this.getZ(),
                4, 0.14D, 0.14D, 0.14D, 0.018D);
    }

    private void spawnPiercingReturnEffects(ServerLevel serverLevel) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.55F, 1.45F);
        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(),
                4, 0.12D, 0.08D, 0.12D, 0.012D);
    }

    private void hitEntitiesAlongMotion() {
        FanState motionState = this.getFanState();
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 start = this.position();
        Vec3 end = start.add(movement);
        if (!this.isReturning()) {
            HitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != HitResult.Type.MISS) {
                end = blockHit.getLocation();
            }
        }
        AABB searchBox = this.getBoundingBox().expandTowards(movement).inflate(0.35D);
        List<Entity> targets = this.level().getEntities(this, searchBox, this::canHitEntity);
        targets.sort(Comparator.comparingDouble(target -> target.position().distanceToSqr(start)));
        for (Entity target : targets) {
            AABB targetBox = target.getBoundingBox().inflate(0.3D);
            Vec3 hitLocation;
            if (targetBox.contains(start)) {
                hitLocation = start;
            } else {
                hitLocation = targetBox.clip(start, end).orElse(null);
            }
            if (hitLocation != null) {
                this.hitLivingEntity((LivingEntity)target, hitLocation);
                if (motionState == FanState.PIERCING && this.getFanState() != FanState.PIERCING) {
                    return;
                }
            }
        }
    }

    private ServerPlayer findServerOwner() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        if (this.ownerUuid == null && this.getOwner() instanceof ServerPlayer owner) {
            this.ownerUuid = owner.getUUID();
        }
        return this.ownerUuid == null ? null : serverLevel.getServer().getPlayerList().getPlayer(this.ownerUuid);
    }

    private void returnFanToOwner(ServerPlayer owner) {
        if (this.isRemoved()) {
            return;
        }
        this.releaseBurialTargets();
        this.removeStuckSlowdown();
        ItemStack fan = this.getItem().copy();
        fan.setCount(1);
        if (owner.getItemInHand(this.returnHand).isEmpty()) {
            owner.setItemInHand(this.returnHand, fan);
        } else if (!owner.getInventory().add(fan)) {
            owner.drop(fan, false);
        }
        owner.getCooldowns().addCooldown(GuaniaoItems.WIND_FEATHER_FAN.get(), RETURN_COOLDOWN_TICKS);
        this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7F, 1.35F);
        this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.22F, 1.7F);
        this.discard();
    }

    private void dropFanAndDiscard() {
        if (this.isRemoved() || this.level().isClientSide) {
            return;
        }
        this.releaseBurialTargets();
        this.removeStuckSlowdown();
        ItemStack fan = this.getItem().copy();
        fan.setCount(1);
        this.spawnAtLocation(fan, 0.1F);
        this.discard();
    }

    private static Vec3 returnTarget(ServerPlayer owner) {
        return owner.getEyePosition().subtract(0.0D, 0.25D, 0.0D);
    }

    public boolean isReturning() {
        return this.getFanState() == FanState.RETURNING;
    }

    public boolean isPiercing() {
        return this.getFanState() == FanState.PIERCING;
    }

    public boolean isHunting() {
        return this.getFanState() == FanState.HUNTING;
    }

    public boolean isStuck() {
        FanState state = this.getFanState();
        return state == FanState.STUCK_ENTITY
                || state == FanState.STUCK_BLOCK
                || state == FanState.BURIAL_VORTEX
                || state == FanState.RIVEN_SEQUENCE;
    }

    public boolean isRivenActive() {
        return this.getFanState() == FanState.RIVEN_SEQUENCE;
    }

    public int getRivenTicks() {
        return this.entityData.get(DATA_RIVEN_TICKS);
    }

    public FanState getFanState() {
        return FanState.fromId(this.entityData.get(DATA_STATE));
    }

    private void setFanState(FanState state) {
        this.entityData.set(DATA_STATE, state.ordinal());
    }

    private boolean isFlyingOutbound() {
        FanState state = this.getFanState();
        return state == FanState.OUTBOUND_SPIN || state == FanState.PIERCING;
    }

    private boolean isNonCollidingState() {
        FanState state = this.getFanState();
        return state == FanState.RETURNING
                || state == FanState.STUCK_ENTITY
                || state == FanState.STUCK_BLOCK
                || state == FanState.BURIAL_VORTEX
                || state == FanState.RIVEN_SEQUENCE;
    }

    public float getCharge() {
        return this.entityData.get(DATA_CHARGE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Returning", this.isReturning());
        tag.putInt("FanState", this.getFanState().ordinal());
        tag.putFloat("Charge", this.getCharge());
        tag.putDouble("ThrowOriginX", this.throwOrigin.x);
        tag.putDouble("ThrowOriginY", this.throwOrigin.y);
        tag.putDouble("ThrowOriginZ", this.throwOrigin.z);
        tag.putFloat("MaxDistance", this.maxDistance);
        tag.putFloat("AttackDamage", this.attackDamage);
        tag.putFloat("ReturnSpeed", this.returnSpeed);
        tag.putBoolean("ReturnOffhand", this.returnHand == InteractionHand.OFF_HAND);
        tag.putInt("OwnerMissingTicks", this.ownerMissingTicks);
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putInt("ReturningTicks", this.returningTicks);
        tag.putInt("StuckTicks", this.stuckTicks);
        tag.putInt("PulloutTicks", this.pulloutTicks);
        tag.putInt("RivenTicks", this.rivenTicks);
        tag.putBoolean("RivenDamageDone", this.rivenDamageDone);
        tag.putBoolean("RivenReformDamageDone", this.rivenReformDamageDone);
        tag.putInt("HuntingHop", this.huntingHop);
        tag.putInt("StuckBlockX", this.stuckBlockPos.getX());
        tag.putInt("StuckBlockY", this.stuckBlockPos.getY());
        tag.putInt("StuckBlockZ", this.stuckBlockPos.getZ());
        tag.putInt("StuckFace", this.stuckFace.get3DDataValue());
        putVec3(tag, "StuckPosition", this.stuckPosition);
        putVec3(tag, "StuckOffset", this.stuckOffset);
        putVec3(tag, "StuckForward", this.stuckForward);
        putVec3(tag, "StuckLocalForward", this.stuckLocalForward);
        putVec3(tag, "RivenAnchor", this.rivenAnchor);
        tag.put("OutboundHits", saveHitSet(this.outboundHits));
        tag.put("ReturnHits", saveHitSet(this.returnHits));
        tag.put("HuntedTargets", saveHitSet(this.huntedTargets));
        tag.put("HuntingLockedTargets", saveHitSet(this.huntingLockedTargets));
        if (this.ownerUuid != null) {
            tag.putUUID("FanOwner", this.ownerUuid);
        }
        if (this.stuckEntityUuid != null) {
            tag.putUUID("StuckEntity", this.stuckEntityUuid);
        }
        if (this.huntingTargetUuid != null) {
            tag.putUUID("HuntingTarget", this.huntingTargetUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FanState", Tag.TAG_INT)) {
            int savedState = tag.getInt("FanState");
            boolean temporaryReturningState = savedState == FanState.STUCK_BLOCK.ordinal()
                    && !tag.contains("StuckBlockX", Tag.TAG_INT);
            this.setFanState(temporaryReturningState ? FanState.RETURNING : FanState.fromId(savedState));
        } else {
            this.setFanState(tag.getBoolean("Returning") ? FanState.RETURNING : FanState.OUTBOUND_SPIN);
        }
        this.entityData.set(DATA_CHARGE, tag.getFloat("Charge"));
        this.throwOrigin = new Vec3(tag.getDouble("ThrowOriginX"), tag.getDouble("ThrowOriginY"), tag.getDouble("ThrowOriginZ"));
        this.maxDistance = tag.getFloat("MaxDistance");
        this.attackDamage = tag.getFloat("AttackDamage");
        this.returnSpeed = tag.getFloat("ReturnSpeed");
        this.returnHand = tag.getBoolean("ReturnOffhand") ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        this.ownerMissingTicks = tag.getInt("OwnerMissingTicks");
        this.lifeTicks = tag.getInt("LifeTicks");
        this.returningTicks = tag.getInt("ReturningTicks");
        this.stuckTicks = tag.getInt("StuckTicks");
        this.pulloutTicks = tag.getInt("PulloutTicks");
        this.rivenTicks = tag.getInt("RivenTicks");
        this.rivenDamageDone = tag.getBoolean("RivenDamageDone");
        this.rivenReformDamageDone = tag.getBoolean("RivenReformDamageDone");
        this.huntingHop = tag.getInt("HuntingHop");
        this.entityData.set(DATA_RIVEN_TICKS, this.rivenTicks);
        this.stuckBlockPos = new BlockPos(tag.getInt("StuckBlockX"), tag.getInt("StuckBlockY"), tag.getInt("StuckBlockZ"));
        this.stuckFace = Direction.from3DDataValue(tag.getInt("StuckFace"));
        this.stuckPosition = getVec3(tag, "StuckPosition");
        this.stuckOffset = getVec3(tag, "StuckOffset");
        this.rivenAnchor = getVec3(tag, "RivenAnchor");
        Vec3 savedForward = getVec3(tag, "StuckForward");
        if (savedForward.lengthSqr() > 1.0E-6D) {
            this.stuckForward = savedForward.normalize();
        }
        Vec3 savedLocalForward = getVec3(tag, "StuckLocalForward");
        this.stuckLocalForward = savedLocalForward.lengthSqr() > 1.0E-6D
                ? savedLocalForward.normalize()
                : this.stuckForward;
        loadHitSet(tag.getList("OutboundHits", Tag.TAG_STRING), this.outboundHits);
        loadHitSet(tag.getList("ReturnHits", Tag.TAG_STRING), this.returnHits);
        loadHitSet(tag.getList("HuntedTargets", Tag.TAG_STRING), this.huntedTargets);
        loadHitSet(tag.getList("HuntingLockedTargets", Tag.TAG_STRING), this.huntingLockedTargets);
        if (tag.hasUUID("FanOwner")) {
            this.ownerUuid = tag.getUUID("FanOwner");
        }
        if (tag.hasUUID("StuckEntity")) {
            this.stuckEntityUuid = tag.getUUID("StuckEntity");
        }
        if (tag.hasUUID("HuntingTarget")) {
            this.huntingTargetUuid = tag.getUUID("HuntingTarget");
        }
        this.noPhysics = this.isNonCollidingState();
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private static ListTag saveHitSet(Set<UUID> hits) {
        ListTag tag = new ListTag();
        for (UUID uuid : hits) {
            tag.add(StringTag.valueOf(uuid.toString()));
        }
        return tag;
    }

    private static void loadHitSet(ListTag tag, Set<UUID> hits) {
        hits.clear();
        for (int i = 0; i < tag.size(); i++) {
            hits.add(UUID.fromString(tag.getString(i)));
        }
    }

    private static void putVec3(CompoundTag tag, String key, Vec3 value) {
        tag.putDouble(key + "X", value.x);
        tag.putDouble(key + "Y", value.y);
        tag.putDouble(key + "Z", value.z);
    }

    private static Vec3 getVec3(CompoundTag tag, String key) {
        return new Vec3(tag.getDouble(key + "X"), tag.getDouble(key + "Y"), tag.getDouble(key + "Z"));
    }
}
