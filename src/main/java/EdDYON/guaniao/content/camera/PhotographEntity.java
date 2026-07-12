package EdDYON.guaniao.content.camera;

import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PhotographEntity extends HangingEntity {
    public static final int FRAME_SIZE_PIXELS = 12;
    public static final int PHOTO_SIZE_PIXELS = 10;
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(PhotographEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_ROTATION = SynchedEntityData.defineId(PhotographEntity.class, EntityDataSerializers.INT);

    public PhotographEntity(EntityType<? extends PhotographEntity> entityType, Level level) {
        super(entityType, level);
    }

    public PhotographEntity(Level level, BlockPos pos, Direction direction, ItemStack photograph) {
        super(GuaniaoEntityTypes.PHOTOGRAPH.get(), level, pos);
        this.setDirection(direction);
        this.setItem(photograph);
    }

    @Override
    protected void defineSynchedData() {
        this.getEntityData().define(DATA_ITEM, ItemStack.EMPTY);
        this.getEntityData().define(DATA_ROTATION, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_ITEM.equals(key)) {
            this.onItemChanged(this.getItem());
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDirection(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this, this.direction.get3DDataValue(), this.getPos());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!this.getItem().isEmpty()) {
            tag.put("Item", this.getItem().save(new CompoundTag()));
        }
        tag.putByte("Facing", (byte)this.direction.get3DDataValue());
        tag.putByte("Rotation", (byte)this.getRotation());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ItemStack item = ItemStack.of(tag.getCompound("Item"));
        if (!item.isEmpty()) {
            this.setItem(item);
        }
        this.setDirection(Direction.from3DDataValue(tag.getByte("Facing")));
        this.setRotation(tag.getByte("Rotation"));
    }

    @Override
    protected float getEyeHeight(@NotNull Pose pose, @NotNull EntityDimensions dimensions) {
        return 0.0F;
    }

    @Override
    public int getWidth() {
        return FRAME_SIZE_PIXELS;
    }

    @Override
    public int getHeight() {
        return FRAME_SIZE_PIXELS;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return this.getItem().copy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean survives() {
        if (!this.level().noCollision(this)) {
            return false;
        }
        BlockState state = this.level().getBlockState(this.pos.relative(this.direction.getOpposite()));
        return (state.isSolid() || this.direction.getAxis().isHorizontal() && DiodeBlock.isDiode(state))
                && this.level().getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty();
    }

    @Override
    protected void recalculateBoundingBox() {
        if (this.direction == null) {
            return;
        }

        double hangOffset = 0.46875D;
        double x = (double)this.pos.getX() + 0.5D - (double)this.direction.getStepX() * hangOffset;
        double y = (double)this.pos.getY() + 0.5D - (double)this.direction.getStepY() * hangOffset;
        double z = (double)this.pos.getZ() + 0.5D - (double)this.direction.getStepZ() * hangOffset;
        this.setPosRaw(x, y, z);

        double xSize = this.getWidth();
        double ySize = this.getHeight();
        double zSize = this.getWidth();
        switch (this.direction.getAxis()) {
            case X -> xSize = 1.0D;
            case Y -> ySize = 1.0D;
            case Z -> zSize = 1.0D;
        }

        xSize /= 32.0D;
        ySize /= 32.0D;
        zSize /= 32.0D;
        this.setBoundingBox(new AABB(x - xSize, y - ySize, z - zSize, x + xSize, y + ySize, z + zSize));
    }

    @Override
    protected void setDirection(@NotNull Direction direction) {
        Validate.notNull(direction);
        this.direction = direction;
        if (direction.getAxis().isHorizontal()) {
            this.setXRot(0.0F);
            this.setYRot((float)(direction.get2DDataValue() * 90));
        } else {
            this.setXRot((float)(-90 * direction.getAxisDirection().getStep()));
            this.setYRot(0.0F);
        }

        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.recalculateBoundingBox();
    }

    public ItemStack getItem() {
        return this.getEntityData().get(DATA_ITEM);
    }

    public void setItem(ItemStack stack) {
        this.getEntityData().set(DATA_ITEM, stack);
    }

    private void onItemChanged(ItemStack stack) {
        if (!stack.isEmpty()) {
            stack.setEntityRepresentation(this);
        }
        this.recalculateBoundingBox();
    }

    public int getRotation() {
        return this.getEntityData().get(DATA_ROTATION);
    }

    public void setRotation(int rotation) {
        this.getEntityData().set(DATA_ROTATION, rotation & 3);
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!this.level().isClientSide) {
            this.setRotation(this.getRotation() + 1);
            this.playSound(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 0.8F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (!this.isRemoved() && !this.level().isClientSide) {
            this.dropItem(source.getEntity());
            this.kill();
            this.markHurt();
        }
        return true;
    }

    @Override
    public void dropItem(@Nullable Entity breaker) {
        this.playSound(SoundEvents.ITEM_FRAME_BREAK, 0.8F, 1.0F);
        if (breaker instanceof Player player && player.isCreative()) {
            return;
        }
        ItemStack item = this.getItem();
        if (!item.isEmpty()) {
            ItemStack drop = item.copy();
            drop.setCount(1);
            this.spawnAtLocation(drop);
        }
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.ITEM_FRAME_PLACE, 0.8F, 1.0F);
    }
}
