package EdDYON.guaniao.content.dropping;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.bird.BirdAmbientDropControl;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class BirdDroppingSplatEntity extends Entity implements GeoEntity {
    public static final int MIN_AGE_TICKS = BirdAmbientDropControl.MIN_LIFETIME_TICKS;
    public static final int MAX_AGE_TICKS = BirdAmbientDropControl.MAX_LIFETIME_TICKS;
    private static final int FADE_TICKS = 20 * 20;
    private static final double ENTITY_HORIZONTAL_SURFACE_INSET = 0.16D;
    private static final double ENTITY_VERTICAL_SURFACE_INSET = 0.055D;
    private static final double ENTITY_RENDER_NUDGE = 0.006D;
    private static final float RAIN_WASH_CHANCE_PER_TICK = 0.003F;
    private static final EntityDataAccessor<Integer> DATA_SURFACE_DIRECTION = SynchedEntityData.defineId(BirdDroppingSplatEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACHED_ENTITY_ID = SynchedEntityData.defineId(BirdDroppingSplatEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACHMENT_PART = SynchedEntityData.defineId(BirdDroppingSplatEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LOCAL_DIRECTION = SynchedEntityData.defineId(BirdDroppingSplatEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_LOCAL_X = SynchedEntityData.defineId(BirdDroppingSplatEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LOCAL_Y = SynchedEntityData.defineId(BirdDroppingSplatEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LOCAL_Z = SynchedEntityData.defineId(BirdDroppingSplatEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_MAX_AGE_TICKS = SynchedEntityData.defineId(BirdDroppingSplatEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private int ageTicks;
    private BlockPos anchorBlock = BlockPos.ZERO;
    private UUID attachedEntityUuid;

    public BirdDroppingSplatEntity(EntityType<? extends BirdDroppingSplatEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static boolean canAddSplatAt(Level level, Vec3 position) {
        double radius = BirdConfigManager.DROPPING_CAP_RADIUS;
        AABB area = new AABB(
                position.x - radius,
                position.y - radius,
                position.z - radius,
                position.x + radius,
                position.y + radius,
                position.z + radius
        );
        int cap = Math.min(
                BirdConfigManager.maxGroundDroppingsNearby(),
                BirdAmbientDropControl.HARD_MAX_DROPPINGS_NEARBY
        );
        return cap > 0 && level.getEntitiesOfClass(BirdDroppingSplatEntity.class, area).size() < cap;
    }

    public static BirdDroppingSplatEntity onBlock(Level level, Vec3 position, Direction direction, BlockPos anchorBlock) {
        BirdDroppingSplatEntity splat = new BirdDroppingSplatEntity(GuaniaoEntityTypes.BIRD_DROPPING_SPLAT.get(), level);
        Vec3 offset = Vec3.atLowerCornerOf(direction.getNormal()).scale(0.0125D);
        splat.setPos(position.add(offset));
        splat.setSurfaceDirection(direction);
        splat.anchorBlock = anchorBlock.immutable();
        splat.setMaxAgeTicks(BirdAmbientDropControl.randomLifetime(level.random));
        return splat;
    }

    public static BirdDroppingSplatEntity onEntity(Level level, Entity target, Vec3 position, Direction direction) {
        BirdDroppingSplatEntity splat = new BirdDroppingSplatEntity(GuaniaoEntityTypes.BIRD_DROPPING_SPLAT.get(), level);
        splat.setPos(position);
        splat.setSurfaceDirection(direction);
        splat.setAttachedEntity(target, position, direction);
        splat.setMaxAgeTicks(BirdAmbientDropControl.randomLifetime(level.random));
        return splat;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SURFACE_DIRECTION, Direction.UP.get3DDataValue());
        this.entityData.define(DATA_ATTACHED_ENTITY_ID, -1);
        this.entityData.define(DATA_ATTACHMENT_PART, AttachmentPart.BODY.id);
        this.entityData.define(DATA_LOCAL_DIRECTION, Direction.SOUTH.get3DDataValue());
        this.entityData.define(DATA_LOCAL_X, 0.0F);
        this.entityData.define(DATA_LOCAL_Y, 0.0F);
        this.entityData.define(DATA_LOCAL_Z, 0.0F);
        this.entityData.define(DATA_MAX_AGE_TICKS, MIN_AGE_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);
        ++this.ageTicks;

        Entity attached = this.getAttachedEntity();
        if (attached != null) {
            if (!attached.isAlive()) {
                this.discard();
                return;
            }
            this.updateAttachedPosition(attached);
        } else if (!this.anchorBlock.equals(BlockPos.ZERO) && !this.hasValidAnchor()) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            if (this.isTouchingWater()) {
                this.washAway(serverLevel, ParticleTypes.SPLASH);
                return;
            }
            if (serverLevel.isRainingAt(this.blockPosition().above()) && this.random.nextFloat() < RAIN_WASH_CHANCE_PER_TICK) {
                this.washAway(serverLevel, ParticleTypes.SPLASH);
                return;
            }
        }

        if (!this.level().isClientSide && this.ageTicks >= this.getMaxAgeTicks()) {
            this.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public float getPickRadius() {
        return 0.25F;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (!this.level().isClientSide) {
            Entity attacker = source.getEntity();
            if (!(attacker instanceof Player player) || !player.isCreative()) {
                this.dropCollectible(BirdDroppingUtil.randomDroppingStack(this.level().random));
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.NEUTRAL, 0.5F, 0.9F + this.level().random.nextFloat() * 0.2F);
            this.discard();
        }
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.BRUSH)) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            this.cleanWithBrush(serverLevel, player, hand, stack);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public Direction getSurfaceDirection() {
        return Direction.from3DDataValue(this.entityData.get(DATA_SURFACE_DIRECTION));
    }

    public void setSurfaceDirection(Direction direction) {
        if (direction == null) {
            direction = Direction.UP;
        }
        this.entityData.set(DATA_SURFACE_DIRECTION, direction.get3DDataValue());
    }

    public float getFadeAlpha() {
        int maxAge = this.getMaxAgeTicks();
        if (this.ageTicks <= maxAge - FADE_TICKS) {
            return 1.0F;
        }
        return Math.max(0.0F, (maxAge - this.ageTicks) / (float)FADE_TICKS);
    }

    private int getMaxAgeTicks() {
        return this.entityData.get(DATA_MAX_AGE_TICKS);
    }

    private void setMaxAgeTicks(int ticks) {
        this.entityData.set(DATA_MAX_AGE_TICKS, Mth.clamp(ticks, MIN_AGE_TICKS, MAX_AGE_TICKS));
    }

    private void setAttachedEntity(Entity target, Vec3 hitPosition, Direction hitDirection) {
        this.entityData.set(DATA_ATTACHED_ENTITY_ID, target.getId());
        this.attachedEntityUuid = target.getUUID();
        AttachmentPart part = AttachmentPart.fromHit(target, hitPosition);
        float yaw = attachmentYaw(target, part);
        Vec3 anchor = attachmentAnchor(target, part);
        Vec3 offset = hitPosition.subtract(anchor);
        LocalOffset localOffset = toLocal(offset, yaw);
        this.entityData.set(DATA_ATTACHMENT_PART, part.id);
        this.entityData.set(DATA_LOCAL_X, (float)localOffset.x);
        this.entityData.set(DATA_LOCAL_Y, (float)localOffset.y);
        this.entityData.set(DATA_LOCAL_Z, (float)localOffset.z);
        this.entityData.set(DATA_LOCAL_DIRECTION, toLocalDirection(hitDirection, yaw).get3DDataValue());
        this.updateAttachedPosition(target);
    }

    private Entity getAttachedEntity() {
        int entityId = this.entityData.get(DATA_ATTACHED_ENTITY_ID);
        if (entityId >= 0) {
            Entity entity = this.level().getEntity(entityId);
            if (entity != null) {
                return entity;
            }
        }
        if (this.attachedEntityUuid != null && this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(this.attachedEntityUuid);
        }
        return null;
    }

    private void updateAttachedPosition(Entity target) {
        AttachmentPart part = AttachmentPart.byId(this.entityData.get(DATA_ATTACHMENT_PART));
        float yaw = attachmentYaw(target, part);
        Vec3 anchor = attachmentAnchor(target, part);
        Vec3 local = new Vec3(this.entityData.get(DATA_LOCAL_X), this.entityData.get(DATA_LOCAL_Y), this.entityData.get(DATA_LOCAL_Z));
        Vec3 worldOffset = toWorld(local, yaw);
        Direction worldDirection = toWorldDirection(Direction.from3DDataValue(this.entityData.get(DATA_LOCAL_DIRECTION)), yaw);
        double inset = worldDirection.getAxis().isVertical() ? ENTITY_VERTICAL_SURFACE_INSET : ENTITY_HORIZONTAL_SURFACE_INSET;
        Vec3 normal = Vec3.atLowerCornerOf(worldDirection.getNormal());
        Vec3 worldPosition = anchor.add(worldOffset).subtract(normal.scale(inset)).add(normal.scale(ENTITY_RENDER_NUDGE));
        this.setSurfaceDirection(worldDirection);
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setPos(worldPosition);
    }

    private void cleanWithBrush(ServerLevel level, Player player, InteractionHand hand, ItemStack brush) {
        this.dropCollectible(BirdDroppingUtil.randomDroppingStack(level.random));
        level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.05D, this.getZ(), 8, 0.22D, 0.06D, 0.22D, 0.01D);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.NEUTRAL, 0.6F, 0.85F + level.random.nextFloat() * 0.25F);
        if (!player.getAbilities().instabuild) {
            brush.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
        }
        this.discard();
    }

    private void washAway(ServerLevel level, ParticleOptions particles) {
        level.sendParticles(particles, this.getX(), this.getY() + 0.05D, this.getZ(), 5, 0.2D, 0.05D, 0.2D, 0.01D);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.NEUTRAL, 0.35F, 1.1F + level.random.nextFloat() * 0.25F);
        this.discard();
    }

    private void dropCollectible(ItemStack stack) {
        ItemEntity itemEntity = this.spawnAtLocation(stack);
        if (itemEntity != null) {
            BirdAmbientDropControl.applyRandomLifetime(itemEntity);
        }
    }

    private boolean isTouchingWater() {
        BlockPos pos = this.blockPosition();
        for (Direction direction : Direction.values()) {
            if (this.level().getFluidState(pos.relative(direction)).is(Fluids.WATER)) {
                return true;
            }
        }
        return this.level().getFluidState(pos).is(Fluids.WATER);
    }

    private boolean hasValidAnchor() {
        net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(this.anchorBlock);
        return state.isSolid() || state.getBlock() instanceof BasePressurePlateBlock;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.ageTicks);
        tag.putInt("MaxAge", this.getMaxAgeTicks());
        tag.putInt("SurfaceDirection", this.getSurfaceDirection().get3DDataValue());
        tag.putLong("AnchorPos", this.anchorBlock.asLong());
        tag.putInt("AttachmentPart", this.entityData.get(DATA_ATTACHMENT_PART));
        tag.putInt("LocalDirection", this.entityData.get(DATA_LOCAL_DIRECTION));
        tag.putFloat("LocalX", this.entityData.get(DATA_LOCAL_X));
        tag.putFloat("LocalY", this.entityData.get(DATA_LOCAL_Y));
        tag.putFloat("LocalZ", this.entityData.get(DATA_LOCAL_Z));
        if (this.attachedEntityUuid != null) {
            tag.putUUID("AttachedEntity", this.attachedEntityUuid);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.ageTicks = tag.getInt("Age");
        if (tag.contains("MaxAge")) {
            this.setMaxAgeTicks(tag.getInt("MaxAge"));
        }
        this.setSurfaceDirection(Direction.from3DDataValue(tag.getInt("SurfaceDirection")));
        this.anchorBlock = BlockPos.of(tag.getLong("AnchorPos"));
        this.entityData.set(DATA_ATTACHMENT_PART, tag.getInt("AttachmentPart"));
        this.entityData.set(DATA_LOCAL_DIRECTION, tag.getInt("LocalDirection"));
        this.entityData.set(DATA_LOCAL_X, tag.getFloat("LocalX"));
        this.entityData.set(DATA_LOCAL_Y, tag.getFloat("LocalY"));
        this.entityData.set(DATA_LOCAL_Z, tag.getFloat("LocalZ"));
        if (tag.hasUUID("AttachedEntity")) {
            this.attachedEntityUuid = tag.getUUID("AttachedEntity");
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

    private static Vec3 attachmentAnchor(Entity target, AttachmentPart part) {
        double height = target.getBbHeight();
        double y = switch (part) {
            case HEAD -> target instanceof LivingEntity living ? living.getEyeY() - height * 0.08D : target.getY() + height * 0.82D;
            case LOWER_BODY -> target.getY() + height * 0.25D;
            default -> target.getY() + height * 0.55D;
        };
        return new Vec3(target.getX(), y, target.getZ());
    }

    private static float attachmentYaw(Entity target, AttachmentPart part) {
        if (target instanceof LivingEntity living && part == AttachmentPart.HEAD) {
            return living.getYHeadRot();
        }
        return target.getYRot();
    }

    private static LocalOffset toLocal(Vec3 worldOffset, float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double localX = worldOffset.x * cos + worldOffset.z * sin;
        double localZ = worldOffset.z * cos - worldOffset.x * sin;
        return new LocalOffset(localX, worldOffset.y, localZ);
    }

    private static Vec3 toWorld(Vec3 local, float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double x = -sin * local.z + cos * local.x;
        double z = cos * local.z + sin * local.x;
        return new Vec3(x, local.y, z);
    }

    private static Direction toLocalDirection(Direction worldDirection, float yawDegrees) {
        if (worldDirection.getAxis().isVertical()) {
            return worldDirection;
        }
        LocalOffset local = toLocal(Vec3.atLowerCornerOf(worldDirection.getNormal()), yawDegrees);
        return Direction.getNearest(local.x, local.y, local.z);
    }

    private static Direction toWorldDirection(Direction localDirection, float yawDegrees) {
        if (localDirection.getAxis().isVertical()) {
            return localDirection;
        }
        Vec3 world = toWorld(Vec3.atLowerCornerOf(localDirection.getNormal()), yawDegrees);
        return Direction.getNearest(world.x, world.y, world.z);
    }

    private record LocalOffset(double x, double y, double z) {
    }

    private enum AttachmentPart {
        LOWER_BODY(0),
        BODY(1),
        HEAD(2);

        private static final AttachmentPart[] VALUES = values();
        private final int id;

        AttachmentPart(int id) {
            this.id = id;
        }

        static AttachmentPart byId(int id) {
            if (id < 0 || id >= VALUES.length) {
                return BODY;
            }
            return VALUES[id];
        }

        static AttachmentPart fromHit(Entity target, Vec3 hitPosition) {
            double height = Math.max(0.1D, target.getBbHeight());
            double relativeY = Mth.clamp((hitPosition.y - target.getY()) / height, 0.0D, 1.0D);
            if (relativeY >= 0.72D) {
                return HEAD;
            }
            if (relativeY <= 0.34D) {
                return LOWER_BODY;
            }
            return BODY;
        }
    }
}
