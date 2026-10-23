package EdDYON.guaniao.content.camera.frame;

import EdDYON.guaniao.config.FrameWorkbenchConfig;
import EdDYON.guaniao.content.camera.LegacyPhotoMigration;
import EdDYON.guaniao.content.camera.PhotographData;
import EdDYON.guaniao.network.FrameWorkSnapshotPacket;
import EdDYON.guaniao.network.GuaniaoNetwork;
import EdDYON.guaniao.registry.GuaniaoBlocks;
import EdDYON.guaniao.registry.GuaniaoItems;
import EdDYON.guaniao.registry.GuaniaoMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class FrameWorkbenchMenu extends AbstractContainerMenu {
    public static final int INPUT_COUNT = 1;
    public static final int OUTPUT_SLOT = 1;
    public static final int PLAYER_START = 2;
    public static final int HOTBAR_START = 29;
    public static final int PLAYER_END = 38;
    public static final int[][] INPUT_POSITIONS = {{18, 54}};
    public static final int OUTPUT_X = 338;
    public static final int OUTPUT_Y = 237;
    public static final int INVENTORY_X = 18;
    public static final int INVENTORY_Y = 212;
    public static final int HOTBAR_Y = 270;
    public static final int CRAFT_DURATION_TICKS = 300;
    private static final int MAX_SOURCE_NBT_BYTES = 65536;

    private final SimpleContainer inputs;
    private final SimpleContainer result;
    private final ContainerLevelAccess access;
    private final Player owner;
    private final BlockPos workbenchPos;
    @Nullable private final FrameWorkbenchBlockEntity bench;
    @Nullable private final FrameWorkbenchBlockEntity.Work work;
    private final DataSlot craftRevision = DataSlot.standalone();
    private final DataSlot minSizePixels = DataSlot.standalone();
    private final DataSlot maxSizePixels = DataSlot.standalone();
    private final DataSlot craftTicksRemaining = DataSlot.standalone();
    private final DataSlot craftResponse = DataSlot.standalone();
    private ItemStack workPreview = ItemStack.EMPTY;
    private long workSeed;
    private long sentSnapshotVersion = Long.MIN_VALUE;
    private boolean closed;

    public FrameWorkbenchMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(), null);
    }

    public FrameWorkbenchMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        this(containerId, inventory, access.evaluate((level, pos) -> pos, BlockPos.ZERO),
                findWorkbench(access));
    }

    @Nullable
    private static FrameWorkbenchBlockEntity findWorkbench(ContainerLevelAccess access) {
        FrameWorkbenchBlockEntity[] found = new FrameWorkbenchBlockEntity[1];
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof FrameWorkbenchBlockEntity bench) found[0] = bench;
        });
        return found[0];
    }

    public FrameWorkbenchMenu(int containerId, Inventory inventory, FrameWorkbenchBlockEntity bench) {
        this(containerId, inventory, bench.getBlockPos(), bench);
    }

    private FrameWorkbenchMenu(int containerId, Inventory inventory, BlockPos pos, @Nullable FrameWorkbenchBlockEntity bench) {
        super(GuaniaoMenuTypes.FRAME_WORKBENCH.get(), containerId);
        this.owner = inventory.player;
        this.workbenchPos = pos.immutable();
        this.access = owner.level().isClientSide ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(owner.level(), pos);
        this.bench = owner.level().isClientSide ? null : bench;
        this.work = this.bench == null ? null : this.bench.open(owner.getUUID(), this);
        this.inputs = work == null ? new SimpleContainer(INPUT_COUNT) : work.input;
        this.result = work == null ? new SimpleContainer(1) : work.result;
        for (int i = 0; i < INPUT_COUNT; i++) {
            this.addSlot(new Slot(inputs, i, INPUT_POSITIONS[i][0], INPUT_POSITIONS[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isUsable() && !isCrafting() && isSource(stack);
                }

                @Override
                public boolean mayPickup(Player player) { return isUsable() && !isCrafting(); }

                @Override
                public int getMaxStackSize() { return 1; }
            });
        }
        this.addSlot(new Slot(result, 0, OUTPUT_X, OUTPUT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return isUsable() && !isCrafting(); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9,
                        INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
        // Send the running flag before the accepted revision. The screen can
        // acknowledge its request without briefly unlocking between packets.
        this.addDataSlot(craftTicksRemaining);
        this.addDataSlot(craftRevision);
        this.minSizePixels.set(FrameWorkbenchConfig.DEFAULT_MIN_PIXELS);
        this.maxSizePixels.set(FrameWorkbenchConfig.DEFAULT_MAX_PIXELS);
        this.addDataSlot(minSizePixels);
        this.addDataSlot(maxSizePixels);
        // A request is acknowledged only after all of its authoritative state.
        this.addDataSlot(craftResponse);
        updateSizeLimits();
        updateWorkState();
    }

    public ItemStack getPhotograph() { return inputs.getItem(0); }
    public int getCraftRevision() { return craftRevision.get(); }
    public int getCraftResponse() { return craftResponse.get(); }
    public int getMinSizePixels() { return minSizePixels.get(); }
    public int getMaxSizePixels() { return maxSizePixels.get(); }
    public int getMinPixels() { return getMinSizePixels(); }
    public int getMaxPixels() { return getMaxSizePixels(); }
    public int getCraftTicksRemaining() { return craftTicksRemaining.get(); }
    public boolean isCrafting() { return work != null ? work.busy() : getCraftTicksRemaining() > 0; }
    public ItemStack getWorkPreview() { return (work == null ? workPreview : work.preview()).copy(); }
    public long getWorkSeed() { return work == null ? workSeed : work.seed; }
    @Nullable public FrameDesign getWorkDesign() { return FrameDesign.read(getWorkPreview()); }

    private boolean isUsable() {
        return !closed && (owner.level().isClientSide
                || (bench != null && bench.owns(owner.getUUID(), work, this)));
    }

    public void acceptWorkSnapshot(BlockPos pos, ItemStack preview, long seed, int remaining, int revision) {
        if (!owner.level().isClientSide || closed || !workbenchPos.equals(pos)) return;
        workPreview = preview.copy();
        workSeed = seed;
        craftTicksRemaining.set(Math.max(0, Math.min(CRAFT_DURATION_TICKS, remaining)));
        craftRevision.set(revision & 0x7fff);
    }

    private void updateSizeLimits() {
        if (!owner.level().isClientSide) {
            minSizePixels.set(FrameWorkbenchConfig.minSizePixels());
            maxSizePixels.set(FrameWorkbenchConfig.maxSizePixels());
        }
    }

    @Override
    public void broadcastChanges() {
        updateSizeLimits();
        updateWorkState();
        sendWorkSnapshot();
        if (PhotographData.hasLegacyPixels(getPhotograph())) LegacyPhotoMigration.queue(owner.level(), getPhotograph());
        super.broadcastChanges();
    }

    private void updateWorkState() {
        if (work == null || owner.level().isClientSide) return;
        craftTicksRemaining.set(work.remaining);
        craftRevision.set(work.revision);
    }

    private void sendWorkSnapshot() {
        if (!closed && work != null && owner instanceof ServerPlayer player
                && player.containerMenu == this && sentSnapshotVersion != work.snapshotVersion) {
            sentSnapshotVersion = work.snapshotVersion;
            GuaniaoNetwork.sendToPlayer(new FrameWorkSnapshotPacket(containerId, workbenchPos,
                    work.preview().copy(), work.seed, work.remaining, work.revision), player);
        }
    }

    private static boolean isSource(ItemStack stack) {
        return stack.is(GuaniaoItems.PHOTOGRAPH.get())
                || (stack.is(GuaniaoItems.FILM.get()) && PhotographData.hasImage(stack));
    }

    /** Only an availability hint on the client; the server repeats every check. */
    public boolean canCraft(FrameDesign design) {
        if (!isUsable() || isCrafting() || design == null || !result.isEmpty() || !design.hasArea() || !design.hasAvailableMaterials()) return false;
        // The server reads current settings, even when a config reload happened
        // after the last menu data synchronization.
        if (!owner.level().isClientSide) {
            if (!FrameWorkbenchConfig.allows(design)) return false;
        } else if (design.widthPixels() < getMinSizePixels() || design.heightPixels() < getMinSizePixels()
                || design.widthPixels() > getMaxSizePixels() || design.heightPixels() > getMaxSizePixels()) return false;
        ItemStack source = getPhotograph();
        if (!source.isEmpty() && (!isSource(source)
                || (EdDYON.guaniao.util.ItemData.copy(source).contains(PhotographData.TAG_PIXELS))
                || (((net.minecraft.nbt.CompoundTag)source.saveOptional(owner.registryAccess())).getCompound("components").sizeInBytes() > MAX_SOURCE_NBT_BYTES))) return false;
        return true;
    }

    /** One accepted request reserves one output in the block entity, never in a hidden slot. */
    public boolean craft(Player player, int expectedRevision, FrameDesign design) {
        if (owner != player || player.level().isClientSide || closed || player.containerMenu != this
                || expectedRevision != getCraftRevision()) return false;
        try {
            if (!stillValid(player) || design == null) return false;
            ItemStack source = getPhotograph();
            if (PhotographData.hasLegacyPixels(source)) {
                // This queues asynchronous migration; never copy the embedded pixel payload.
                LegacyPhotoMigration.migrateNow(player.level(), source);
                return false;
            }
            if (!canCraft(design)) return false;
            ItemStack output;
            if (source.is(GuaniaoItems.PHOTOGRAPH.get())) {
                output = source.copy();
                output.setCount(1);
            } else {
                output = new ItemStack(GuaniaoItems.PHOTOGRAPH.get());
                output.applyComponents(source.getComponentsPatch());
            }
            FrameDesign.write(output, design);
            // Framing is free. In survival the source moves into the result; its
            // photo reference and other data are retained rather than duplicated.
            work.pending = output;
            work.reservedSource = player.getAbilities().instabuild ? ItemStack.EMPTY : source.copy();
            if (!work.reservedSource.isEmpty()) work.reservedSource.setCount(1);
            work.remaining = CRAFT_DURATION_TICKS;
            work.startedTick = player.level().getGameTime();
            work.seed = player.getRandom().nextLong();
            work.revision = (work.revision + 1) & 0x7fff;
            work.snapshotVersion++;
            if (!player.getAbilities().instabuild && !source.isEmpty()) inputs.removeItem(0, 1);
            bench.setChanged();
            return true;
        } finally {
            // Business rejections also release the screen's pending request.
            // Network latency must never be mistaken for a rejected operation.
            craftResponse.set((getCraftResponse() + 1) & 0x7fff);
            broadcastChanges();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return isUsable() && owner == player && stillValid(access, player, GuaniaoBlocks.FRAME_WORKBENCH.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!isUsable() || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < PLAYER_START) {
            moved = moveItemStackTo(stack, PLAYER_START, PLAYER_END, index == OUTPUT_SLOT);
        } else {
            int input = findInputSlot(stack);
            moved = input >= 0 && moveItemStackTo(stack, input, input + 1, false);
            if (!moved) moved = index < HOTBAR_START
                    ? moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false)
                    : moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false);
        }
        if (!moved || stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        broadcastChanges();
        return original;
    }

    private static int findInputSlot(ItemStack stack) {
        if (isSource(stack)) return 0;
        return -1;
    }

    @Override
    public void removed(Player player) {
        if (closed) return;
        closed = true;
        super.removed(player);
        if (!player.level().isClientSide && bench != null && bench.owns(player.getUUID(), work, this)) {
            // Work and ready results belong to the block, not the screen. An idle
            // visit gives its unused source back using vanilla overflow handling.
            if (!work.busy() && result.isEmpty()) clearContainer(player, inputs);
            bench.release(player.getUUID(), work, this);
        }
    }
}
