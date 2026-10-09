package EdDYON.guaniao.content.earthworm;

import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/** A small server-controlled crawler; it only burrows while supported by soil. */
public final class EarthwormEntity extends Entity {
    private static final EntityDataAccessor<Integer> BURROW =
            SynchedEntityData.defineId(EarthwormEntity.class, EntityDataSerializers.INT);
    private Vec3 home;
    private int soilAge;
    private int burrowAfter;
    private float desiredYaw;
    private int turnTicks;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw;

    public EarthwormEntity(EntityType<? extends EarthwormEntity> type, Level level) {
        super(type, level);
        this.burrowAfter = 900 + this.random.nextInt(901);
    }

    @Nullable
    public static EarthwormEntity spawn(ServerLevel level, Vec3 position, boolean natural) {
        if (!level.hasChunkAt(BlockPos.containing(position))
                || !level.getFluidState(BlockPos.containing(position)).isEmpty()) {
            return null;
        }
        int cap = natural ? 4 : 12;
        if (level.getEntitiesOfClass(EarthwormEntity.class,
                new AABB(position, position).inflate(16.0D, 4.0D, 16.0D), Entity::isAlive).size() >= cap) {
            return null;
        }
        EarthwormEntity worm = GuaniaoEntityTypes.EARTHWORM.get().create(level);
        if (worm == null) return null;
        worm.setPos(position);
        worm.home = position;
        worm.setYRot(level.random.nextFloat() * 360.0F);
        worm.desiredYaw = worm.getYRot();
        if (!level.noCollision(worm) || !worm.hasSupport(position)) return null;
        if (!level.addFreshEntity(worm)) return null;
        if (natural) {
            BlockPos soil = BlockPos.containing(position.x, position.y - 0.03D, position.z);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(soil)),
                    position.x, position.y, position.z, 3, 0.06D, 0.01D, 0.06D, 0.01D);
        }
        return worm;
    }

    private boolean hasSupport(Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        if (!this.level().hasChunkAt(pos) || !this.level().getFluidState(pos).isEmpty()) return false;
        // A thin footprint accepts partial-height blocks but rejects ledges.
        AABB feet = new AABB(point.x - 0.10D, point.y - 0.06D, point.z - 0.10D,
                point.x + 0.10D, point.y, point.z + 0.10D);
        return this.level().getBlockCollisions(this, feet).iterator().hasNext();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(BURROW, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.lerpSteps > 0) {
                this.setPos(this.getX() + (this.lerpX - this.getX()) / this.lerpSteps,
                        this.getY() + (this.lerpY - this.getY()) / this.lerpSteps,
                        this.getZ() + (this.lerpZ - this.getZ()) / this.lerpSteps);
                this.setYRot(this.getYRot() + Mth.wrapDegrees(this.lerpYaw - this.getYRot()) / this.lerpSteps);
                --this.lerpSteps;
            }
            return;
        }
        if (this.home == null) this.home = this.position();
        BlockPos ground = BlockPos.containing(this.getX(), this.getY() - 0.03D, this.getZ());
        boolean soil = this.hasSupport(this.position())
                && this.level().getBlockState(ground).is(BirdTags.WOODCOCK_FORAGE_GROUND);
        this.soilAge = EarthwormMotion.soilTicks(this.soilAge, soil);
        if (soil && this.soilAge >= this.burrowAfter) {
            int progress = this.entityData.get(BURROW) + 1;
            this.entityData.set(BURROW, progress);
            this.setDeltaMovement(Vec3.ZERO);
            if (progress >= EarthwormMotion.BURROW_TICKS) this.discard();
            return;
        }
        this.entityData.set(BURROW, 0);
        if (--this.turnTicks <= 0 || this.horizontalCollision) {
            this.desiredYaw = this.getYRot() + (this.random.nextFloat() - 0.5F) * 100.0F;
            this.turnTicks = 80 + this.random.nextInt(121);
        }
        Vec3 toHome = this.home.subtract(this.position());
        if (toHome.horizontalDistanceSqr() > EarthwormMotion.HOME_RADIUS * EarthwormMotion.HOME_RADIUS) {
            this.desiredYaw = (float) (Mth.atan2(-toHome.x, toHome.z) * Mth.RAD_TO_DEG);
        }
        this.setYRot(Mth.approachDegrees(this.getYRot(), this.desiredYaw, 1.0F));
        double yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double speed = 0.0025D + 0.0015D * (0.5D + 0.5D * Math.sin(this.tickCount * 0.10D));
        Vec3 heading = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 next = this.position().add(heading.scale(0.22D));
        boolean supported = this.hasSupport(next);
        Vec3 movement = heading.scale(supported ? speed : 0.0D);
        if (!supported) {
            this.desiredYaw = this.getYRot() + 100.0F;
            this.turnTicks = 60;
        }
        this.setDeltaMovement(movement.x, Math.max(-0.30D, this.getDeltaMovement().y - 0.035D), movement.z);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
    }

    public float burrowProgress() {
        return this.entityData.get(BURROW) / (float) EarthwormMotion.BURROW_TICKS;
    }

    public boolean tryEat() {
        if (this.level().isClientSide || !this.isAlive() || this.burrowProgress() > 0.0F) return false;
        this.discard();
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).isEmpty() || !this.isAlive() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide) {
            ItemStack stack = new ItemStack(GuaniaoItems.EARTHWORM.get());
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            this.discard();
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override public boolean isPickable() { return this.isAlive(); }
    @Override public float getPickRadius() { return 0.08F; }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        this.lerpX = x; this.lerpY = y; this.lerpZ = z;
        this.lerpYaw = yaw;
        this.lerpSteps = Math.max(1, steps);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        Vec3 center = this.home == null ? this.position() : this.home;
        tag.putDouble("HomeX", center.x); tag.putDouble("HomeY", center.y); tag.putDouble("HomeZ", center.z);
        tag.putInt("SoilAge", this.soilAge);
        tag.putInt("BurrowAfter", this.burrowAfter);
        tag.putInt("Burrow", this.entityData.get(BURROW));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.home = tag.contains("HomeX") ? new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ")) : this.position();
        this.soilAge = Mth.clamp(tag.getInt("SoilAge"), 0, 1800);
        this.burrowAfter = tag.contains("BurrowAfter") ? Mth.clamp(tag.getInt("BurrowAfter"), 900, 1800) : this.burrowAfter;
        this.entityData.set(BURROW, Mth.clamp(tag.getInt("Burrow"), 0, EarthwormMotion.BURROW_TICKS));
        this.desiredYaw = this.getYRot();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
