package EdDYON.guaniao.content.nest;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.registry.GuaniaoBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class CrowNestBlockEntity extends BlockEntity implements GeoBlockEntity, Container {
    public static final int TREASURE_SLOTS = 6;
    private static final int MAX_CROW_CLAIMS = 3;
    private static final long CROW_CLAIM_EXPIRY_TICKS = 72000L;
    private static final Map<Level, Set<CrowNestBlockEntity>> LOADED_NESTS = new WeakHashMap<>();
    private static final String TREASURES_TAG = "Treasures";
    private static final String NATURAL_NEST_TAG = "NaturalNest";
    private static final String CROW_CLAIMS_TAG = "CrowClaims";
    private static final String CLAIM_CROW_TAG = "Crow";
    private static final String CLAIM_TIME_TAG = "ClaimTime";
    private static final String RUMMAGED_MASK_TAG = "RummagedMask";
    private static final String CURRENT_SEARCH_SLOT_TAG = "CurrentSearchSlot";
    private static final String SEARCH_TICKS_TAG = "SearchTicks";
    private static final String SEARCH_TOTAL_TICKS_TAG = "SearchTotalTicks";
    private static final String LOOT_LAYOUT_SEED_TAG = "LootLayoutSeed";
    private static final String LOOT_FOOTPRINTS_TAG = "LootFootprints";
    private static final String LOOT_COLUMNS_TAG = "LootColumns";
    private static final String LOOT_ROWS_TAG = "LootRows";

    private final NonNullList<ItemStack> treasures = NonNullList.withSize(TREASURE_SLOTS, ItemStack.EMPTY);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int rummagedMask;
    private int currentSearchSlot = -1;
    private int searchTicks;
    private int searchTotalTicks;
    private long lootLayoutSeed;
    private boolean naturalNest;
    private final Map<UUID, Long> crowClaims = new LinkedHashMap<>();
    private final int[] lootFootprints = new int[TREASURE_SLOTS];
    private final int[] lootColumns = new int[TREASURE_SLOTS];
    private final int[] lootRows = new int[TREASURE_SLOTS];

    public CrowNestBlockEntity(BlockPos pos, BlockState state) {
        super(GuaniaoBlockEntityTypes.CROW_NEST.get(), pos, state);
        this.naturalNest = state.hasProperty(CrowNestBlock.NATURAL_NEST) && state.getValue(CrowNestBlock.NATURAL_NEST);
        Arrays.fill(this.lootColumns, -1);
        Arrays.fill(this.lootRows, -1);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            LOADED_NESTS.computeIfAbsent(this.level, ignored -> new HashSet<>()).add(this);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide) {
            Set<CrowNestBlockEntity> nests = LOADED_NESTS.get(this.level);
            if (nests != null) {
                nests.remove(this);
                if (nests.isEmpty()) {
                    LOADED_NESTS.remove(this.level);
                }
            }
        }
        super.setRemoved();
    }

    public boolean isNaturalNest() {
        return this.naturalNest || (this.getBlockState().hasProperty(CrowNestBlock.NATURAL_NEST)
                && this.getBlockState().getValue(CrowNestBlock.NATURAL_NEST));
    }

    /** Marks a nest placed by world generation. Player-placed nests intentionally default to false. */
    public void setNaturalNest(boolean naturalNest) {
        if (this.naturalNest != naturalNest) {
            this.naturalNest = naturalNest;
            BlockState state = this.getBlockState();
            if (this.level != null && state.hasProperty(CrowNestBlock.NATURAL_NEST)
                    && state.getValue(CrowNestBlock.NATURAL_NEST) != naturalNest) {
                this.level.setBlock(this.worldPosition, state.setValue(CrowNestBlock.NATURAL_NEST, naturalNest), 3);
            }
            this.sync();
        }
    }

    public boolean hasSpace() {
        if (this.getOccupiedTreasureSlots() >= this.getTreasureSlotLimit()) {
            for (ItemStack stored : this.treasures) {
                if (!stored.isEmpty() && stored.getCount() < stored.getMaxStackSize()) {
                    return true;
                }
            }
            return false;
        }
        for (ItemStack stored : this.treasures) {
            if (stored.isEmpty() || stored.getCount() < stored.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSpaceFor(ItemStack stack) {
        if (!CrowNestTreasure.isAccepted(stack)) {
            return false;
        }
        for (ItemStack stored : this.treasures) {
            if (canMerge(stored, stack)) {
                return true;
            }
        }
        return this.getOccupiedTreasureSlots() < this.getTreasureSlotLimit();
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
        if (this.getOccupiedTreasureSlots() >= this.getTreasureSlotLimit()) {
            return false;
        }
        for (int slot = 0; slot < this.treasures.size(); slot++) {
            if (this.treasures.get(slot).isEmpty()) {
                if (!this.hasTreasure()) {
                    this.rerollLootLayout();
                }
                ItemStack inserted = stack.copy();
                inserted.setCount(1);
                this.treasures.set(slot, inserted);
                this.lootFootprints[slot] = this.rollLootFootprint();
                this.setSlotRummaged(slot, false);
                this.rebuildLootLayout();
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

    private int getOccupiedTreasureSlots() {
        int count = 0;
        for (ItemStack stored : this.treasures) {
            if (!stored.isEmpty()) {
                ++count;
            }
        }
        return count;
    }

    private int getTreasureSlotLimit() {
        return Math.min(TREASURE_SLOTS, BirdConfigManager.maxCrowNestTreasures());
    }

    /** Begins or resumes the server-authoritative, one-stack-at-a-time rummage. */
    public boolean beginAutomaticSearch() {
        this.ensureLootLayoutSeed();
        this.ensureLootLayout();
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

    public long getLootLayoutSeed() {
        return this.lootLayoutSeed != 0L ? this.lootLayoutSeed : this.fallbackLayoutSeed();
    }

    public int getLootFootprint(int slot) {
        if (slot < 0 || slot >= TREASURE_SLOTS) {
            return 1;
        }
        int footprint = this.lootFootprints[slot];
        return footprint >= 1 && footprint <= 3 ? footprint : 1;
    }

    public int getLootColumn(int slot) {
        return slot >= 0 && slot < TREASURE_SLOTS ? this.lootColumns[slot] : -1;
    }

    public int getLootRow(int slot) {
        return slot >= 0 && slot < TREASURE_SLOTS ? this.lootRows[slot] : -1;
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
        if (this.level == null || this.level.isClientSide || !this.isSlotRummaged(slot) || slot == this.currentSearchSlot) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ContainerHelper.takeItem(this.treasures, slot);
        if (!result.isEmpty()) {
            this.lootFootprints[slot] = 0;
            this.clearLootPosition(slot);
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
            CrowNestLootProfile profile = CrowNestLootProfile.forStack(nest.treasures.get(foundSlot));
            nest.searchTicks = 0;
            nest.setSlotRummaged(foundSlot, true);
            nest.resetCurrentSearch();
            nest.playRevealSound(level, pos, profile);
            nest.startNextSearch();
            nest.sync();
            return;
        }
        if (nest.searchTicks % 10 == 0) {
            level.playSound(null, pos, SoundEvents.GRASS_STEP, SoundSource.BLOCKS, 0.18F,
                    0.82F + level.getRandom().nextFloat() * 0.12F);
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

    /**
     * Finds a loaded nest without loading chunks. A crow first returns to an already claimed nest,
     * then may claim one of up to three available slots at a nearby suitable nest.
     */
    public static Optional<CrowNestBlockEntity> findNestForCrow(Level level, Vec3 origin, int horizontalRange,
                                                                  int verticalRange, ItemStack stack, UUID crowId) {
        if (level == null || origin == null || crowId == null || horizontalRange < 0 || verticalRange < 0) {
            return Optional.empty();
        }
        CrowNestBlockEntity claimed = null;
        CrowNestBlockEntity available = null;
        double claimedDistance = Double.MAX_VALUE;
        double availableDistance = Double.MAX_VALUE;
        for (CrowNestBlockEntity nest : loadedNests(level)) {
            if (!nest.isWithinSearchRange(level, origin, horizontalRange, verticalRange)
                    || !nest.canAcceptCrow(crowId, stack)) {
                continue;
            }
            double distance = nest.getDepositPosition().distanceToSqr(origin);
            if (nest.isClaimedBy(crowId)) {
                if (distance < claimedDistance) {
                    claimedDistance = distance;
                    claimed = nest;
                }
            } else if (distance < availableDistance) {
                availableDistance = distance;
                available = nest;
            }
        }
        CrowNestBlockEntity result = claimed != null ? claimed : available;
        return result != null && result.claimCrow(crowId) ? Optional.of(result) : Optional.empty();
    }

    private static Optional<CrowNestBlockEntity> findNearestMatching(Level level, Vec3 origin, int horizontalRange,
                                                                      int verticalRange, ItemStack stack) {
        if (level == null || origin == null || horizontalRange < 0 || verticalRange < 0) {
            return Optional.empty();
        }
        CrowNestBlockEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (CrowNestBlockEntity nest : loadedNests(level)) {
            if (!nest.isWithinSearchRange(level, origin, horizontalRange, verticalRange)
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

    private static List<CrowNestBlockEntity> loadedNests(Level level) {
        Set<CrowNestBlockEntity> nests = LOADED_NESTS.get(level);
        return nests == null || nests.isEmpty() ? List.of() : new ArrayList<>(nests);
    }

    private boolean isWithinSearchRange(Level level, Vec3 origin, int horizontalRange, int verticalRange) {
        return !this.isRemoved()
                && this.level == level
                && Math.abs(this.worldPosition.getX() + 0.5D - origin.x) <= horizontalRange
                && Math.abs(this.worldPosition.getZ() + 0.5D - origin.z) <= horizontalRange
                && Math.abs(this.worldPosition.getY() + 0.5D - origin.y) <= verticalRange;
    }

    public boolean canAcceptCrow(UUID crowId, ItemStack stack) {
        if (crowId == null
                || !this.hasSpaceFor(stack)
                || (!this.isNaturalNest() && !BirdConfigManager.crowsClaimPlayerNests())) {
            return false;
        }
        this.pruneExpiredCrowClaims();
        return this.crowClaims.containsKey(crowId) || this.crowClaims.size() < MAX_CROW_CLAIMS;
    }

    private boolean isClaimedBy(UUID crowId) {
        this.pruneExpiredCrowClaims();
        return crowId != null && this.crowClaims.containsKey(crowId);
    }

    private boolean claimCrow(UUID crowId) {
        if (crowId == null) {
            return false;
        }
        this.pruneExpiredCrowClaims();
        if (!this.crowClaims.containsKey(crowId) && this.crowClaims.size() >= MAX_CROW_CLAIMS) {
            return false;
        }
        long gameTime = this.level == null ? 0L : this.level.getGameTime();
        Long previous = this.crowClaims.put(crowId, gameTime);
        if (previous == null || previous.longValue() != gameTime) {
            this.setChanged();
        }
        return true;
    }

    private void pruneExpiredCrowClaims() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        long expiryTime = this.level.getGameTime() - CROW_CLAIM_EXPIRY_TICKS;
        if (this.crowClaims.entrySet().removeIf(entry -> entry.getValue() < expiryTime)) {
            this.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag treasureTag = new CompoundTag();
        ContainerHelper.saveAllItems(treasureTag, this.treasures);
        tag.put(TREASURES_TAG, treasureTag);
        tag.putBoolean(NATURAL_NEST_TAG, this.naturalNest);
        ListTag claimTags = new ListTag();
        for (Map.Entry<UUID, Long> entry : this.crowClaims.entrySet()) {
            CompoundTag claimTag = new CompoundTag();
            claimTag.putUUID(CLAIM_CROW_TAG, entry.getKey());
            claimTag.putLong(CLAIM_TIME_TAG, entry.getValue());
            claimTags.add(claimTag);
        }
        tag.put(CROW_CLAIMS_TAG, claimTags);
        tag.putInt(RUMMAGED_MASK_TAG, this.rummagedMask);
        tag.putInt(CURRENT_SEARCH_SLOT_TAG, this.currentSearchSlot);
        tag.putInt(SEARCH_TICKS_TAG, this.searchTicks);
        tag.putInt(SEARCH_TOTAL_TICKS_TAG, this.searchTotalTicks);
        tag.putLong(LOOT_LAYOUT_SEED_TAG, this.lootLayoutSeed);
        tag.putIntArray(LOOT_FOOTPRINTS_TAG, this.lootFootprints);
        tag.putIntArray(LOOT_COLUMNS_TAG, this.lootColumns);
        tag.putIntArray(LOOT_ROWS_TAG, this.lootRows);
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
        this.naturalNest = tag.getBoolean(NATURAL_NEST_TAG)
                || (this.getBlockState().hasProperty(CrowNestBlock.NATURAL_NEST)
                && this.getBlockState().getValue(CrowNestBlock.NATURAL_NEST));
        this.crowClaims.clear();
        if (tag.contains(CROW_CLAIMS_TAG, CompoundTag.TAG_LIST)) {
            ListTag claimTags = tag.getList(CROW_CLAIMS_TAG, CompoundTag.TAG_COMPOUND);
            for (int index = 0; index < claimTags.size(); ++index) {
                CompoundTag claimTag = claimTags.getCompound(index);
                if (claimTag.hasUUID(CLAIM_CROW_TAG)) {
                    this.crowClaims.put(claimTag.getUUID(CLAIM_CROW_TAG), claimTag.getLong(CLAIM_TIME_TAG));
                }
            }
        }
        this.rummagedMask = tag.getInt(RUMMAGED_MASK_TAG) & ((1 << TREASURE_SLOTS) - 1);
        this.currentSearchSlot = tag.contains(CURRENT_SEARCH_SLOT_TAG) ? tag.getInt(CURRENT_SEARCH_SLOT_TAG) : -1;
        this.searchTicks = Math.max(0, tag.getInt(SEARCH_TICKS_TAG));
        this.searchTotalTicks = Math.max(0, tag.getInt(SEARCH_TOTAL_TICKS_TAG));
        this.lootLayoutSeed = tag.getLong(LOOT_LAYOUT_SEED_TAG);
        Arrays.fill(this.lootFootprints, 0);
        Arrays.fill(this.lootColumns, -1);
        Arrays.fill(this.lootRows, -1);
        int[] savedFootprints = tag.getIntArray(LOOT_FOOTPRINTS_TAG);
        System.arraycopy(savedFootprints, 0, this.lootFootprints, 0, Math.min(savedFootprints.length, TREASURE_SLOTS));
        int[] savedColumns = tag.getIntArray(LOOT_COLUMNS_TAG);
        int[] savedRows = tag.getIntArray(LOOT_ROWS_TAG);
        System.arraycopy(savedColumns, 0, this.lootColumns, 0, Math.min(savedColumns.length, TREASURE_SLOTS));
        System.arraycopy(savedRows, 0, this.lootRows, 0, Math.min(savedRows.length, TREASURE_SLOTS));
        for (int slot = 0; slot < TREASURE_SLOTS; slot++) {
            ItemStack stored = this.sanitizeStoredLoot(this.treasures.get(slot));
            this.treasures.set(slot, stored);
            if (stored.isEmpty()) {
                this.lootFootprints[slot] = 0;
                this.clearLootPosition(slot);
                this.setSlotRummaged(slot, false);
            } else if (this.lootFootprints[slot] < 1 || this.lootFootprints[slot] > 3) {
                this.lootFootprints[slot] = 1;
            }
        }
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
                this.lootFootprints[slot] = 0;
                this.clearLootPosition(slot);
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
            this.lootFootprints[slot] = 0;
            this.clearLootPosition(slot);
            this.setSlotRummaged(slot, false);
        }
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= this.treasures.size()) {
            return;
        }
        boolean hadTreasure = this.hasTreasure();
        if (stack.isEmpty()) {
            this.treasures.set(slot, ItemStack.EMPTY);
            this.lootFootprints[slot] = 0;
            this.clearLootPosition(slot);
            this.setSlotRummaged(slot, false);
        } else if (CrowNestTreasure.isAllowedNestLoot(stack)) {
            if (!hadTreasure) {
                this.rerollLootLayout();
            }
            ItemStack stored = this.sanitizeStoredLoot(stack);
            this.treasures.set(slot, stored);
            this.lootFootprints[slot] = this.rollLootFootprint();
            this.setSlotRummaged(slot, false);
        } else {
            return;
        }
        if (slot == this.currentSearchSlot) {
            this.resetCurrentSearch();
        }
        if (!stack.isEmpty()) {
            this.rebuildLootLayout();
        }
        this.sync();
    }

    @Override
    public boolean stillValid(Player player) {
        return player != null
                && this.level != null
                && this.level.getBlockEntity(this.worldPosition) == this
                && this.level.getBlockState(this.worldPosition).getBlock() instanceof CrowNestBlock
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
            this.lootFootprints[slot] = 0;
            this.clearLootPosition(slot);
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

    private void ensureLootLayoutSeed() {
        if (this.lootLayoutSeed == 0L) {
            this.rerollLootLayout();
        }
    }

    private void ensureLootLayout() {
        if (!this.isLootLayoutValid()) {
            this.rebuildLootLayout();
        }
    }

    private boolean isLootLayoutValid() {
        boolean[][] occupied = new boolean[CrowNestLootLayout.GRID_ROWS][CrowNestLootLayout.GRID_COLUMNS];
        for (int slot = 0; slot < TREASURE_SLOTS; slot++) {
            if (this.treasures.get(slot).isEmpty()) {
                continue;
            }
            int footprint = this.getLootFootprint(slot);
            int column = this.lootColumns[slot];
            int row = this.lootRows[slot];
            if (column < 0 || row < 0 || column + footprint > CrowNestLootLayout.GRID_COLUMNS
                    || row + footprint > CrowNestLootLayout.GRID_ROWS) {
                return false;
            }
            for (int y = row; y < row + footprint; y++) {
                for (int x = column; x < column + footprint; x++) {
                    if (occupied[y][x]) {
                        return false;
                    }
                    occupied[y][x] = true;
                }
            }
        }
        return true;
    }

    private void rebuildLootLayout() {
        List<ItemStack> stacks = new ArrayList<>(TREASURE_SLOTS);
        for (int slot = 0; slot < TREASURE_SLOTS; slot++) {
            stacks.add(this.treasures.get(slot));
        }
        List<CrowNestLootLayout.Placement> placements = CrowNestLootLayout.arrange(stacks, this.getLootLayoutSeed(), this.lootFootprints);
        Arrays.fill(this.lootColumns, -1);
        Arrays.fill(this.lootRows, -1);
        for (CrowNestLootLayout.Placement placement : placements) {
            this.lootColumns[placement.storageSlot()] = placement.column();
            this.lootRows[placement.storageSlot()] = placement.row();
        }
    }

    private void clearLootPosition(int slot) {
        if (slot >= 0 && slot < TREASURE_SLOTS) {
            this.lootColumns[slot] = -1;
            this.lootRows[slot] = -1;
        }
    }

    private void rerollLootLayout() {
        long seed = this.level == null ? this.fallbackLayoutSeed() : this.level.getRandom().nextLong();
        this.lootLayoutSeed = seed != 0L ? seed : 1L;
    }

    private long fallbackLayoutSeed() {
        long seed = this.worldPosition.asLong();
        return seed != 0L ? seed : 1L;
    }

    private int rollLootFootprint() {
        int roll = this.level == null ? Math.floorMod(this.fallbackLayoutSeed(), 100) : this.level.getRandom().nextInt(100);
        if (roll < 56) {
            return 1;
        }
        if (roll < 84) {
            return 2;
        }
        return 3;
    }

    private void playRevealSound(Level level, BlockPos pos, CrowNestLootProfile profile) {
        switch (profile) {
            case COMMON -> level.playSound(null, pos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 0.55F, 1.05F);
            case VALUABLE -> level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.40F, 1.05F);
            case RARE -> level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.65F, 1.20F);
            case TREASURE -> {
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.85F, 1.45F);
                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.35F, 1.55F);
            }
        }
    }

    /** Rejects unsupported or malformed NBT contents before they can become nest loot. */
    private ItemStack sanitizeStoredLoot(ItemStack stack) {
        if (!CrowNestTreasure.isAllowedNestLoot(stack)) {
            return ItemStack.EMPTY;
        }
        ItemStack sanitized = stack.copy();
        sanitized.setCount(Math.min(Math.max(1, sanitized.getCount()), sanitized.getMaxStackSize()));
        return sanitized;
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
