package EdDYON.guaniao.content.nest;

import EdDYON.guaniao.registry.GuaniaoBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;

public class CrowNestBlockEntity extends BlockEntity implements GeoBlockEntity, Container {
    public static final int TREASURE_SLOTS = 6;
    private static final String TREASURES_TAG = "Treasures";
    private static final String RUMMAGED_MASK_TAG = "RummagedMask";
    private static final String CURRENT_SEARCH_SLOT_TAG = "CurrentSearchSlot";
    private static final String SEARCH_TICKS_TAG = "SearchTicks";
    private static final String SEARCH_TOTAL_TICKS_TAG = "SearchTotalTicks";

    private final NonNullList<ItemStack> treasures = NonNullList.withSize(TREASURE_SLOTS, ItemStack.EMPTY);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int rummagedMask;
    private int currentSearchSlot = -1;
    private int searchTicks;
    private int searchTotalTicks;

    public CrowNestBlockEntity(BlockPos pos, BlockState state) {
        super(GuaniaoBlockEntityTypes.CROW_NEST.get(), pos, state);
    }

    public boolean hasSpace() {
        for (ItemStack stored : this.treasures) {
            if (stored.isEmpty() || stored.getCount() < stored.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSpaceFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (ItemStack stored : this.treasures) {
            if (stored.isEmpty() || canMerge(stored, stack)) {
                return true;
            }
        }
        return false;
    }

    /** Stores exactly one shiny item and shrinks the supplied stack on success. */
    public boolean addTreasure(ItemStack stack) {
        if (!CrowNestTreasure.isAccepted(stack)) {
            return false;
        }
        for (int slot = 0; slot < this.treasures.size(); slot++) {
            ItemStack stored = this.treasures.get(slot);
            if (canMerge(stored, stack)) {
                stored.grow(1);
                stack.shrink(1);
                this.sync();
                return true;
            }
        }
        for (int slot = 0; slot < this.treasures.size(); slot++) {
            if (this.treasures.get(slot).isEmpty()) {
                ItemStack inserted = stack.copy();
                inserted.setCount(1);
                this.treasures.set(slot, inserted);
                this.setSlotRummaged(slot, false);
                stack.shrink(1);
                this.sync();
                return true;
            }
        }
        return false;
    }

    public boolean hasTreasure() {
        return this.getTreasureCount() > 0;
    }

    public int getTreasureCount() {
        int count = 0;
        for (ItemStack stored : this.treasures) {
            count += stored.getCount();
        }
        return count;
    }

    /** Begins or resumes the server-authoritative, one-stack-at-a-time rummage. */
    public boolean beginAutomaticSearch() {
        return this.startNextSearch();
    }

    public boolean hasUnsearchedTreasure() {
        for (int slot = 0; slot < this.treasures.size(); slot++) {
            if (!this.treasures.get(slot).isEmpty() && !this.isSlotRummaged(slot)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSearchComplete() {
        return this.hasTreasure() && !this.hasUnsearchedTreasure() && this.currentSearchSlot < 0;
    }

    public boolean isSlotRummaged(int slot) {
        return slot >= 0 && slot < TREASURE_SLOTS && (this.rummagedMask & 1 << slot) != 0;
    }

    public int getRummagedMask() {
        return this.rummagedMask;
    }

    public int getCurrentSearchSlot() {
        return this.currentSearchSlot;
    }

    public int getSearchTicks() {
        return this.searchTicks;
    }

    public int getSearchTotalTicks() {
        return this.searchTotalTicks;
    }

    /** Removes a full revealed stack. Callers must hand it to the player themselves. */
    public ItemStack takeDiscoveredTreasure(int slot) {
        if (!this.isSlotRummaged(slot) || slot == this.currentSearchSlot) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ContainerHelper.takeItem(this.treasures, slot);
        if (!result.isEmpty()) {
            this.setSlotRummaged(slot, false);
            this.sync();
        }
        return result;
    }

    public int getEggCount() {
        BlockState state = this.getBlockState();
        return state.hasProperty(CrowNestBlock.EGGS) ? state.getValue(CrowNestBlock.EGGS) : 0;
    }

    public boolean takeOneEgg() {
        if (this.level == null || this.level.isClientSide) {
            return false;
        }
        BlockState state = this.getBlockState();
        int eggs = this.getEggCount();
        if (eggs <= 0) {
            return false;
        }
        this.level.setBlock(this.worldPosition, state.setValue(CrowNestBlock.EGGS, eggs - 1), 3);
        this.setChanged();
        return true;
    }

    public Vec3 getDepositPosition() {
        return Vec3.atBottomCenterOf(this.worldPosition).add(0.0D, 0.35D, 0.0D);
    }

    public void dropAllContents() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        Containers.dropContents(this.level, this.worldPosition, this.treasures);
        this.clearTreasures();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrowNestBlockEntity nest) {
        if (nest.currentSearchSlot < 0) {
            return;
        }
        if (nest.currentSearchSlot >= TREASURE_SLOTS
                || nest.treasures.get(nest.currentSearchSlot).isEmpty()
                || nest.isSlotRummaged(nest.currentSearchSlot)) {
            nest.resetCurrentSearch();
            nest.startNextSearch();
            return;
        }
        if (--nest.searchTicks <= 0) {
            int foundSlot = nest.currentSearchSlot;
            nest.searchTicks = 0;
            nest.setSlotRummaged(foundSlot, true);
            nest.resetCurrentSearch();
            level.playSound(null, pos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 0.60F, 1.10F);
            nest.startNextSearch();
            nest.sync();
            return;
        }
        if (nest.searchTicks % 2 == 0) {
            nest.sync();
        }
    }

    public static Optional<CrowNestBlockEntity> findNearest(Level level, Vec3 origin, int horizontalRange, int verticalRange) {
        return findNearestMatching(level, origin, horizontalRange, verticalRange, ItemStack.EMPTY);
    }

    public static Optional<CrowNestBlockEntity> findNearest(Level level, Vec3 origin, int horizontalRange, int verticalRange, ItemStack stack) {
        return findNearestMatching(level, origin, horizontalRange, verticalRange, stack);
    }

    private static Optional<CrowNestBlockEntity> findNearestMatching(Level level, Vec3 origin, int horizontalRange,
                                                                      int verticalRange, ItemStack stack) {
        if (level == null || origin == null || horizontalRange < 0 || verticalRange < 0) {
            return Optional.empty();
        }
        BlockPos center = BlockPos.containing(origin);
        CrowNestBlockEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(
                center.offset(-horizontalRange, -verticalRange, -horizontalRange),
                center.offset(horizontalRange, verticalRange, horizontalRange))) {
            if (!(level.getBlockEntity(candidate) instanceof CrowNestBlockEntity nest)
                    || (stack.isEmpty() ? !nest.hasSpace() : !nest.hasSpaceFor(stack))) {
                continue;
            }
            double distance = nest.getDepositPosition().distanceToSqr(origin);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = nest;
            }
        }
        return Optional.ofNullable(nearest);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag treasureTag = new CompoundTag();
        ContainerHelper.saveAllItems(treasureTag, this.treasures);
        tag.put(TREASURES_TAG, treasureTag);
        tag.putInt(RUMMAGED_MASK_TAG, this.rummagedMask);
        tag.putInt(CURRENT_SEARCH_SLOT_TAG, this.currentSearchSlot);
        tag.putInt(SEARCH_TICKS_TAG, this.searchTicks);
        tag.putInt(SEARCH_TOTAL_TICKS_TAG, this.searchTotalTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int slot = 0; slot < this.treasures.size(); slot++) {
            this.treasures.set(slot, ItemStack.EMPTY);
        }
        if (tag.contains(TREASURES_TAG, CompoundTag.TAG_COMPOUND)) {
            ContainerHelper.loadAllItems(tag.getCompound(TREASURES_TAG), this.treasures);
        }
        this.rummagedMask = tag.getInt(RUMMAGED_MASK_TAG) & ((1 << TREASURE_SLOTS) - 1);
        this.currentSearchSlot = tag.contains(CURRENT_SEARCH_SLOT_TAG) ? tag.getInt(CURRENT_SEARCH_SLOT_TAG) : -1;
        this.searchTicks = Math.max(0, tag.getInt(SEARCH_TICKS_TAG));
        this.searchTotalTicks = Math.max(0, tag.getInt(SEARCH_TOTAL_TICKS_TAG));
        if (this.currentSearchSlot < 0 || this.currentSearchSlot >= TREASURE_SLOTS
                || this.treasures.get(this.currentSearchSlot).isEmpty()) {
            this.resetCurrentSearch();
        }
        if (!this.hasTreasure()) {
            this.rummagedMask = 0;
            this.resetCurrentSearch();
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            this.load(tag);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(0.25D, 0.25D, 0.25D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    @Override
    public int getContainerSize() {
        return TREASURE_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return !this.hasTreasure();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.treasures.size() ? this.treasures.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.treasures, slot, amount);
        if (!result.isEmpty()) {
            if (this.treasures.get(slot).isEmpty()) {
                this.setSlotRummaged(slot, false);
            }
            this.sync();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(this.treasures, slot);
        if (!result.isEmpty()) {
            this.setSlotRummaged(slot, false);
        }
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= this.treasures.size()) {
            return;
        }
        if (stack.isEmpty()) {
            this.treasures.set(slot, ItemStack.EMPTY);
            this.setSlotRummaged(slot, false);
        } else if (CrowNestTreasure.isAccepted(stack)) {
            ItemStack stored = stack.copy();
            stored.setCount(Math.min(stored.getCount(), stored.getMaxStackSize()));
            this.treasures.set(slot, stored);
            this.setSlotRummaged(slot, false);
        } else {
            return;
        }
        if (slot == this.currentSearchSlot) {
            this.resetCurrentSearch();
        }
        this.sync();
    }

    @Override
    public boolean stillValid(Player player) {
        return player != null
                && this.level != null
                && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(Vec3.atCenterOf(this.worldPosition)) <= 64.0D;
    }

    @Override
    public void clearContent() {
        this.clearTreasures();
    }

    private boolean startNextSearch() {
        if (this.currentSearchSlot >= 0) {
            return false;
        }
        for (int slot = 0; slot < this.treasures.size(); slot++) {
            ItemStack stack = this.treasures.get(slot);
            if (stack.isEmpty() || this.isSlotRummaged(slot)) {
                continue;
            }
            this.currentSearchSlot = slot;
            this.searchTotalTicks = CrowNestLootProfile.forStack(stack).searchTicks();
            this.searchTicks = this.searchTotalTicks;
            if (this.level != null) {
                this.level.playSound(null, this.worldPosition, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.55F, 0.85F);
            }
            this.sync();
            return true;
        }
        return false;
    }

    private void clearTreasures() {
        for (int slot = 0; slot < this.treasures.size(); slot++) {
            this.treasures.set(slot, ItemStack.EMPTY);
        }
        this.rummagedMask = 0;
        this.resetCurrentSearch();
        this.sync();
    }

    private void resetCurrentSearch() {
        this.currentSearchSlot = -1;
        this.searchTicks = 0;
        this.searchTotalTicks = 0;
    }

    private void setSlotRummaged(int slot, boolean rummaged) {
        if (slot < 0 || slot >= TREASURE_SLOTS) {
            return;
        }
        if (rummaged) {
            this.rummagedMask |= 1 << slot;
        } else {
            this.rummagedMask &= ~(1 << slot);
        }
    }

    private static boolean canMerge(ItemStack stored, ItemStack incoming) {
        return !stored.isEmpty()
                && stored.getCount() < stored.getMaxStackSize()
                && ItemStack.isSameItemSameTags(stored, incoming);
    }

    private void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }
}
