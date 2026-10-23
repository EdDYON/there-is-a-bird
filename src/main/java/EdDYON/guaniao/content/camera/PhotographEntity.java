package EdDYON.guaniao.content.camera;

import EdDYON.guaniao.content.camera.frame.FrameDesign;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoItems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PhotographEntity extends HangingEntity {
    public static final int FRAME_SIZE_PIXELS = 12;
    public static final int PHOTO_SIZE_PIXELS = 10;
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(PhotographEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_ROTATION = SynchedEntityData.defineId(PhotographEntity.class, EntityDataSerializers.INT);
    private PhotographFrameStyle frameStyle = PhotographFrameStyle.DEFAULT;
    @Nullable private FrameDesign frameDesign;
    private List<AABB> occupiedBoxes = List.of();
    private List<BlockPos> supportPositions = List.of();
    @Nullable private AABB designRenderBounds;
    @Nullable private Entity collisionIgnore;

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
    public @NotNull Vec3 trackingPosition() {
        // Match Painting: spawn and movement packets both carry the hanging
        // anchor, not the centre that changes when the frame is resized.
        return Vec3.atLowerCornerOf(this.pos);
    }

    @Override
    public void moveTo(double x, double y, double z, float yaw, float pitch) {
        this.setPos(x, y, z);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        this.setPos(x, y, z);
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
            LegacyPhotoMigration.migrateNow(this.level(), item);
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
        return this.frameDesign == null ? this.getFrameStyle().shape().widthPixels() : this.frameDesign.widthPixels();
    }

    @Override
    public int getHeight() {
        return this.frameDesign == null ? this.getFrameStyle().shape().heightPixels() : this.frameDesign.heightPixels();
    }

    public static boolean canPlace(Level level, BlockPos anchor, Direction face, ItemStack stack) {
        return face.getAxis().isHorizontal() && stack.is(GuaniaoItems.PHOTOGRAPH.get())
                && (PhotographData.hasImage(stack) || FrameDesign.read(stack) != null)
                && new PhotographEntity(level, anchor, face, stack).survives();
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return this.getItem().copy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean survives() {
        if (this.pos == null) {
            return false;
        }
        if (this.frameDesign != null) return this.frameDesign.hasArea() && this.designSurvives();
        // A frame can straddle a chunk boundary. Do not load its neighbour just
        // to check support, or drop an existing frame while that chunk is absent.
        AABB box = this.getBoundingBox();
        BlockPos support = this.pos.relative(this.direction.getOpposite());
        BlockPos min = BlockPos.containing(Math.min(box.minX, support.getX()),
                Math.min(box.minY, support.getY()), Math.min(box.minZ, support.getZ()));
        BlockPos max = BlockPos.containing(Math.max(box.maxX - 1.0E-7D, support.getX()),
                Math.max(box.maxY - 1.0E-7D, support.getY()), Math.max(box.maxZ - 1.0E-7D, support.getZ()));
        if (!this.level().hasChunksAt(min, max)) {
            return this.isAddedToWorld();
        }
        if (!this.level().noCollision(this)) {
            return false;
        }
        int columns = Math.max(1, this.getWidth() / 16);
        int rows = Math.max(1, this.getHeight() / 16);
        Direction horizontal = this.direction.getAxis().isHorizontal() ? this.direction.getCounterClockWise() : Direction.EAST;
        Direction vertical = this.direction.getAxis().isHorizontal() ? Direction.UP : Direction.SOUTH;
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                BlockPos backing = support.relative(horizontal, column - (columns - 1) / 2)
                        .relative(vertical, row - (rows - 1) / 2);
                if (!this.level().hasChunkAt(backing)) {
                    return this.isAddedToWorld();
                }
                BlockState state = this.level().getBlockState(backing);
                if (!Block.canSupportCenter(this.level(), backing, this.direction) && !state.isSolid()
                        && !(this.direction.getAxis().isHorizontal() && DiodeBlock.isDiode(state))) {
                    return false;
                }
            }
        }
        return !this.overlapsHangingEntity(List.of(this.getBoundingBox()));
    }

    @SuppressWarnings("deprecation")
    private boolean designSurvives() {
        AABB bounds = this.getBoundingBox();
        BlockPos support = this.pos.relative(this.direction.getOpposite());
        BlockPos min = BlockPos.containing(Math.min(bounds.minX, support.getX()), Math.min(bounds.minY, support.getY()),
                Math.min(bounds.minZ, support.getZ()));
        BlockPos max = BlockPos.containing(Math.max(bounds.maxX - 1.0E-7D, support.getX()), Math.max(bounds.maxY - 1.0E-7D, support.getY()),
                Math.max(bounds.maxZ - 1.0E-7D, support.getZ()));
        if (!this.level().hasChunksAt(min, max)) return this.isAddedToWorld();
        // The usual clear-space path is one query; detailed silhouettes are needed
        // only when another block intersects the outer bounding rectangle.
        if (!this.level().noCollision(this, bounds)) {
            for (AABB part : this.occupiedBoxes) {
                if (!this.level().noCollision(this, part)) return false;
            }
        }
        for (BlockPos backing : this.supportPositions) {
            if (!this.level().hasChunkAt(backing)) return this.isAddedToWorld();
            BlockState state = this.level().getBlockState(backing);
            if (!Block.canSupportCenter(this.level(), backing, this.direction) && !state.isSolid()
                    && !(this.direction.getAxis().isHorizontal() && DiodeBlock.isDiode(state))) return false;
        }
        return !this.overlapsHangingEntity(this.occupiedBoxes);
    }

    private boolean overlapsHangingEntity(List<AABB> pieces) {
        for (Entity other : this.level().getEntities(this, this.getBoundingBox(), HANGING_ENTITY)) {
            if (other == this.collisionIgnore) continue;
            if (other instanceof PhotographEntity photo && photo.frameDesign != null) {
                for (AABB first : pieces) for (AABB second : photo.occupiedBoxes) {
                    if (first.intersects(second)) return true;
                }
            } else {
                for (AABB part : pieces) if (part.intersects(other.getBoundingBox())) return true;
            }
        }
        return false;
    }

    @Override
    protected void recalculateBoundingBox() {
        if (this.direction == null || this.pos == null) {
            return;
        }

        if (this.frameDesign != null) {
            this.recalculateDesignGeometry();
            return;
        }

        // Vanilla paintings anchor even-width/height frames between block centres.
        // Keep the existing item-frame transform for old floor/ceiling photographs.
        if (this.direction.getAxis().isHorizontal()) {
            super.recalculateBoundingBox();
            return;
        }

        double hangOffset = 0.46875D;
        double x = (double)this.pos.getX() + 0.5D - (double)this.direction.getStepX() * hangOffset;
        double y = (double)this.pos.getY() + 0.5D - (double)this.direction.getStepY() * hangOffset;
        double z = (double)this.pos.getZ() + 0.5D - (double)this.direction.getStepZ() * hangOffset;
        x += this.getWidth() % 32 == 0 ? 0.5D : 0.0D;
        z += this.getHeight() % 32 == 0 ? 0.5D : 0.0D;
        this.setPosRaw(x, y, z);

        double xSize = this.getWidth();
        double ySize = this.getHeight();
        double zSize = this.getHeight();
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

    private void recalculateDesignGeometry() {
        FrameDesign design = this.frameDesign;
        if (design == null) return;
        Direction horizontal = this.direction.getAxis().isHorizontal() ? this.direction.getCounterClockWise() : Direction.EAST;
        Direction vertical = this.direction.getAxis().isHorizontal() ? Direction.UP : Direction.SOUTH;
        double offset = 0.5D - design.depthPixels() / 32.0D;
        double horizontalOffset = ((design.widthPixels() + 15) / 16) % 2 == 0 ? 0.5D : 0.0D;
        double verticalOffset = ((design.heightPixels() + 15) / 16) % 2 == 0 ? 0.5D : 0.0D;
        Vec3 center = Vec3.atCenterOf(this.pos).add(-this.direction.getStepX() * offset, -this.direction.getStepY() * offset,
                -this.direction.getStepZ() * offset).add(horizontal.getStepX() * horizontalOffset + vertical.getStepX() * verticalOffset,
                horizontal.getStepY() * horizontalOffset + vertical.getStepY() * verticalOffset,
                horizontal.getStepZ() * horizontalOffset + vertical.getStepZ() * verticalOffset);
        this.setPosRaw(center.x, center.y, center.z);
        int[] mask = design.mask();
        List<AABB> pieces = new ArrayList<>();
        Set<BlockPos> supports = new HashSet<>();
        double width = Math.max(1, design.widthPixels()) / 16.0D, height = Math.max(1, design.heightPixels()) / 16.0D;
        double halfDepth = design.depthPixels() / 32.0D;
        for (FrameDesign.CellRect rect : FrameDesign.rectangles(mask)) {
            double left = (rect.left() / 32.0D - 0.5D) * width, right = (rect.right() / 32.0D - 0.5D) * width;
            double bottom = (0.5D - rect.bottom() / 32.0D) * height, top = (0.5D - rect.top() / 32.0D) * height;
            Vec3 first = localPoint(center, horizontal, vertical, left, bottom, -halfDepth);
            Vec3 second = localPoint(center, horizontal, vertical, right, top, halfDepth);
            AABB part = new AABB(first, second);
            pieces.add(part);
            // Move the thin occupied rectangle half a block backwards into its backing plane.
            AABB backing = part.move(-this.direction.getStepX() * 0.5D, -this.direction.getStepY() * 0.5D,
                    -this.direction.getStepZ() * 0.5D).deflate(1.0E-7D);
            for (BlockPos block : BlockPos.betweenClosed(BlockPos.containing(backing.minX, backing.minY, backing.minZ),
                    BlockPos.containing(backing.maxX, backing.maxY, backing.maxZ))) supports.add(block.immutable());
        }
        this.occupiedBoxes = List.copyOf(pieces);
        this.supportPositions = List.copyOf(supports);
        AABB bounds = pieces.get(0);
        for (int i = 1; i < pieces.size(); i++) bounds = bounds.minmax(pieces.get(i));
        this.setBoundingBox(bounds);
        double decorationRadius = design.decorations().stream().mapToInt(FrameDesign.Decoration::size).max().orElse(0)
                * Math.min(width, height) / 200.0D;
        // Point decorations may sit beyond a sparse silhouette; render bounds include
        // the design canvas, while physical collision and backing remain mask-only.
        this.designRenderBounds = new AABB(localPoint(center, horizontal, vertical, -width / 2, -height / 2, -halfDepth),
                localPoint(center, horizontal, vertical, width / 2, height / 2, halfDepth)).inflate(decorationRadius + 0.02D);
    }

    private Vec3 localPoint(Vec3 center, Direction horizontal, Direction vertical, double x, double y, double z) {
        return center.add(horizontal.getStepX() * x + vertical.getStepX() * y + this.direction.getStepX() * z,
                horizontal.getStepY() * x + vertical.getStepY() * y + this.direction.getStepY() * z,
                horizontal.getStepZ() * x + vertical.getStepZ() * y + this.direction.getStepZ() * z);
    }

    /** Occupied, disjoint silhouette rectangles in world coordinates for placement previews. */
    public List<AABB> getOccupiedBoxes() {
        return this.frameDesign == null ? List.of(this.getBoundingBox()) : this.occupiedBoxes;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return this.frameDesign != null && this.designRenderBounds != null ? this.designRenderBounds : super.getBoundingBoxForCulling();
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
        ItemStack copy = stack.copy();
        if (!copy.isEmpty()) {
            copy.setCount(1);
        }
        this.getEntityData().set(DATA_ITEM, copy);
        this.recalculateBoundingBox();
    }

    public PhotographFrameStyle getFrameStyle() {
        return this.frameStyle == null ? PhotographFrameStyle.DEFAULT : this.frameStyle;
    }

    @Nullable
    public FrameDesign getFrameDesign() { return this.frameDesign; }

    private void onItemChanged(ItemStack stack) {
        this.frameStyle = PhotographFrameStyle.read(stack);
        this.frameDesign = FrameDesign.read(stack);
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
        if (this.isRemoved() || this.getItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            // A rejected main-hand item must not accidentally rotate via the empty offhand.
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (player.isShiftKeyDown()) {
                if (this.level().isClientSide && PhotographData.hasImage(this.getItem())) {
                    ItemStack copy = this.getItem().copy();
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                        try {
                            Class.forName("EdDYON.guaniao.client.camera.PhotographClientActions")
                                    .getMethod("openScreen", ItemStack.class).invoke(null, copy);
                        } catch (ReflectiveOperationException exception) {
                            throw new IllegalStateException("Failed to open photograph screen", exception);
                        }
                    });
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (!this.mayModify(player, held)) {
                return InteractionResult.FAIL;
            }
            if (!this.level().isClientSide) {
                this.setRotation(this.getRotation() + 1);
                this.playSound(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 0.8F, 1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (this.frameDesign != null && !PhotographData.hasImage(this.getItem())
                && (held.is(GuaniaoItems.PHOTOGRAPH.get()) || held.is(GuaniaoItems.FILM.get())) && PhotographData.hasImage(held)) {
            if (!this.mayModify(player, held)) return InteractionResult.FAIL;
            if (!this.level().isClientSide) {
                // Imported legacy pixels must finish conversion before copying the reference.
                // The queue operates on the real held stack and does not consume it.
                if (PhotographData.hasLegacyPixels(held)) {
                    LegacyPhotoMigration.queue(this.level(), held);
                    return InteractionResult.FAIL;
                }
                ItemStack inserted;
                if (held.is(GuaniaoItems.FILM.get())) {
                    inserted = new ItemStack(GuaniaoItems.PHOTOGRAPH.get());
                    if (held.getTag() != null) inserted.setTag(held.getTag().copy());
                } else {
                    inserted = held.copyWithCount(1);
                }
                FrameDesign.write(inserted, this.frameDesign);
                CompoundTag previousStyle = this.getItem().getTagElement(PhotographFrameStyle.TAG);
                if (previousStyle != null) inserted.addTagElement(PhotographFrameStyle.TAG, previousStyle.copy());
                PhotographEntity candidate = new PhotographEntity(this.level(), this.pos, this.direction, inserted);
                candidate.collisionIgnore = this;
                if (!candidate.survives()) return InteractionResult.FAIL;
                this.setItem(inserted);
                if (!player.getAbilities().instabuild) held.shrink(1);
                this.playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.8F, 1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (this.frameDesign != null) return this.interactWithDesign(player, hand, held);

        PhotographFrameStyle style = this.getFrameStyle();
        PhotographFrameStyle changed;
        boolean shears = held.is(Items.SHEARS);
        if (held.is(Items.OAK_PLANKS)) {
            changed = style.withMaterial(PhotographFrameStyle.Material.OAK);
        } else if (held.is(Items.SPRUCE_PLANKS)) {
            changed = style.withMaterial(PhotographFrameStyle.Material.SPRUCE);
        } else if (held.is(Items.BIRCH_PLANKS)) {
            changed = style.withMaterial(PhotographFrameStyle.Material.BIRCH);
        } else if (held.is(Items.COPPER_INGOT)) {
            changed = style.withMaterial(PhotographFrameStyle.Material.COPPER);
        } else if (held.is(Items.VINE)) {
            changed = style.withOrnament(PhotographFrameStyle.Ornament.LEAVES);
        } else if (isFeather(held)) {
            changed = style.withOrnament(PhotographFrameStyle.Ornament.FEATHER);
        } else if (shears) {
            changed = style.withOrnament(PhotographFrameStyle.Ornament.NONE);
        } else {
            return InteractionResult.PASS;
        }
        if (!this.mayModify(player, held)) {
            return InteractionResult.FAIL;
        }
        if (!this.level().isClientSide && !changed.equals(style)) {
            ItemStack copy = this.getItem().copy();
            PhotographFrameStyle.write(copy, changed);
            this.setItem(copy);
            if (!player.getAbilities().instabuild) {
                if (shears) {
                    held.hurtAndBreak(1, player, user -> user.broadcastBreakEvent(hand));
                } else {
                    held.shrink(1);
                }
            }
            this.playSound(shears ? SoundEvents.SHEEP_SHEAR : SoundEvents.ITEM_FRAME_ADD_ITEM, 0.8F, 1.0F);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private InteractionResult interactWithDesign(Player player, InteractionHand hand, ItemStack held) {
        FrameDesign changed = this.frameDesign;
        FrameDesign.Wood wood = FrameDesign.Wood.fromItem(held);
        FrameDesign.Trim trim = FrameDesign.Trim.fromItem(held);
        boolean shears = held.is(Items.SHEARS);
        if (wood != null) changed = changed.withWood(wood);
        else if (trim != null) changed = changed.withTrim(trim);
        else if (held.is(Items.VINE)) changed = changed.withOrnament(PhotographFrameStyle.Ornament.LEAVES);
        else if (isFeather(held)) changed = changed.withOrnament(PhotographFrameStyle.Ornament.FEATHER);
        else if (shears) changed = changed.withOrnament(PhotographFrameStyle.Ornament.NONE);
        else if (held.getItem() instanceof BlockItem) changed = changed.withFrameMaterial(BuiltInRegistries.ITEM.getKey(held.getItem()));
        else return InteractionResult.PASS;
        if (!this.mayModify(player, held)) return InteractionResult.FAIL;
        if (!this.level().isClientSide && !changed.equals(this.frameDesign)) {
            ItemStack copy = this.getItem().copy();
            FrameDesign.write(copy, changed);
            this.setItem(copy);
            this.playSound(shears ? SoundEvents.SHEEP_SHEAR : SoundEvents.ITEM_FRAME_ADD_ITEM, 0.8F, 1.0F);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private boolean mayModify(Player player, ItemStack held) {
        return player.mayBuild() && this.level().mayInteract(player, this.pos)
                && player.mayUseItemAt(this.pos, this.direction, held);
    }

    private static boolean isFeather(ItemStack stack) {
        if (stack.is(Items.FEATHER)) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("guaniao") && id.getPath().startsWith("feather_");
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
        if (!this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }
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
