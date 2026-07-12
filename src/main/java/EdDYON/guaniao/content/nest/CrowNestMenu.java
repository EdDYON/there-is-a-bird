package EdDYON.guaniao.content.nest;

import EdDYON.guaniao.registry.GuaniaoMenuTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The nest only exposes egg slots to the vanilla container. Crow treasures are
 * rendered as a custom masked rummage grid and are claimed through validated menu buttons.
 */
public class CrowNestMenu extends AbstractContainerMenu {
    public static final int TAKE_TREASURE_BUTTON_BASE = 32;
    public static final int TREASURE_SLOT_COUNT = CrowNestBlockEntity.TREASURE_SLOTS;
    public static final int EGG_SLOT_COUNT = 3;
    public static final int EGG_START_X = 8;
    public static final int EGG_START_Y = 18;
    public static final int PLAYER_START_X = 8;
    public static final int PLAYER_START_Y = 140;

    private final Container lootContainer;
    @Nullable
    private final CrowNestBlockEntity nest;
    private final boolean clientSide;
    private int syncedRummagedMask;
    private int syncedCurrentSearchSlot = -1;
    private int syncedSearchTicks;
    private int syncedSearchTotalTicks;
    private int syncedTreasureCount;
    private int syncedEggCount;

    public CrowNestMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, findNest(inventory, buffer.readBlockPos()), true);
    }

    public CrowNestMenu(int containerId, Inventory inventory, CrowNestBlockEntity nest) {
        this(containerId, inventory, nest, false);
    }

    private CrowNestMenu(int containerId, Inventory inventory, @Nullable CrowNestBlockEntity nest, boolean clientSide) {
        super(GuaniaoMenuTypes.CROW_NEST.get(), containerId);
        this.nest = nest;
        this.lootContainer = nest == null ? new SimpleContainer(TREASURE_SLOT_COUNT) : nest;
        this.clientSide = clientSide;

        for (int slot = 0; slot < EGG_SLOT_COUNT; slot++) {
            this.addSlot(new EggSlot(this, slot, EGG_START_X + slot * 21, EGG_START_Y));
        }
        this.addPlayerInventory(inventory, PLAYER_START_X, PLAYER_START_Y);
        this.addRummageDataSlots();
        if (!clientSide && this.nest != null) {
            this.nest.beginAutomaticSearch();
        }
    }

    private void addRummageDataSlots() {
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return CrowNestMenu.this.clientSide ? CrowNestMenu.this.syncedRummagedMask : CrowNestMenu.this.getServerRummagedMask();
            }

            @Override
            public void set(int value) {
                CrowNestMenu.this.syncedRummagedMask = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return CrowNestMenu.this.clientSide ? CrowNestMenu.this.syncedCurrentSearchSlot : CrowNestMenu.this.getServerCurrentSearchSlot();
            }

            @Override
            public void set(int value) {
                CrowNestMenu.this.syncedCurrentSearchSlot = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return CrowNestMenu.this.clientSide ? CrowNestMenu.this.syncedSearchTicks : CrowNestMenu.this.getServerSearchTicks();
            }

            @Override
            public void set(int value) {
                CrowNestMenu.this.syncedSearchTicks = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return CrowNestMenu.this.clientSide ? CrowNestMenu.this.syncedSearchTotalTicks : CrowNestMenu.this.getServerSearchTotalTicks();
            }

            @Override
            public void set(int value) {
                CrowNestMenu.this.syncedSearchTotalTicks = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return CrowNestMenu.this.clientSide ? CrowNestMenu.this.syncedTreasureCount : CrowNestMenu.this.getServerTreasureCount();
            }

            @Override
            public void set(int value) {
                CrowNestMenu.this.syncedTreasureCount = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return CrowNestMenu.this.clientSide ? CrowNestMenu.this.syncedEggCount : CrowNestMenu.this.getServerEggCount();
            }

            @Override
            public void set(int value) {
                CrowNestMenu.this.syncedEggCount = value;
            }
        });
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        int treasureSlot = buttonId - TAKE_TREASURE_BUTTON_BASE;
        if (treasureSlot < 0 || treasureSlot >= TREASURE_SLOT_COUNT || this.clientSide || this.nest == null) {
            return false;
        }
        ItemStack claimed = this.nest.takeDiscoveredTreasure(treasureSlot);
        if (claimed.isEmpty()) {
            return false;
        }
        if (!player.getInventory().add(claimed)) {
            player.drop(claimed, false);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem() || index >= EGG_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack egg = new ItemStack(Items.EGG);
        if (!this.moveItemStackTo(egg, EGG_SLOT_COUNT, this.slots.size(), true) || !this.removeOneEgg()) {
            return ItemStack.EMPTY;
        }
        return egg;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.nest != null && this.nest.stillValid(player);
    }

    public boolean isSlotRummaged(int slot) {
        int mask = this.clientSide ? this.syncedRummagedMask : this.getServerRummagedMask();
        return slot >= 0 && slot < TREASURE_SLOT_COUNT && (mask & 1 << slot) != 0;
    }

    public boolean isSearching() {
        return this.getCurrentSearchSlot() >= 0 && this.getSearchTicks() > 0;
    }

    public boolean isSearchComplete() {
        return this.getTreasureCount() > 0 && !this.isSearching() && !this.hasUnsearchedTreasure();
    }

    public int getCurrentSearchSlot() {
        return this.clientSide ? this.syncedCurrentSearchSlot : this.getServerCurrentSearchSlot();
    }

    public int getSearchTicks() {
        return this.clientSide ? this.syncedSearchTicks : this.getServerSearchTicks();
    }

    public int getSearchTotalTicks() {
        return this.clientSide ? this.syncedSearchTotalTicks : this.getServerSearchTotalTicks();
    }

    public int getTreasureCount() {
        return this.clientSide ? this.syncedTreasureCount : this.getServerTreasureCount();
    }

    public int getEggCount() {
        return this.clientSide ? this.syncedEggCount : this.getServerEggCount();
    }

    public ItemStack getTreasureStack(int slot) {
        if (slot < 0 || slot >= TREASURE_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return this.lootContainer.getItem(slot);
    }

    public List<CrowNestLootLayout.Placement> getLootLayout() {
        List<ItemStack> stacks = new ArrayList<>(TREASURE_SLOT_COUNT);
        for (int slot = 0; slot < TREASURE_SLOT_COUNT; slot++) {
            stacks.add(this.getTreasureStack(slot));
        }
        return CrowNestLootLayout.arrange(stacks);
    }

    private boolean hasUnsearchedTreasure() {
        for (int slot = 0; slot < TREASURE_SLOT_COUNT; slot++) {
            if (!this.getTreasureStack(slot).isEmpty() && !this.isSlotRummaged(slot)) {
                return true;
            }
        }
        return false;
    }

    private boolean removeOneEgg() {
        return !this.clientSide && this.nest != null && this.nest.takeOneEgg();
    }

    private int getServerRummagedMask() {
        return this.nest == null ? 0 : this.nest.getRummagedMask();
    }

    private int getServerCurrentSearchSlot() {
        return this.nest == null ? -1 : this.nest.getCurrentSearchSlot();
    }

    private int getServerSearchTicks() {
        return this.nest == null ? 0 : this.nest.getSearchTicks();
    }

    private int getServerSearchTotalTicks() {
        return this.nest == null ? 0 : this.nest.getSearchTotalTicks();
    }

    private int getServerTreasureCount() {
        return this.nest == null ? 0 : this.nest.getTreasureCount();
    }

    private int getServerEggCount() {
        return this.nest == null ? 0 : this.nest.getEggCount();
    }

    private void addPlayerInventory(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, x + column * 18, y + row * 18));
            }
        }
        int hotbarY = y + 58;
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, x + column * 18, hotbarY));
        }
    }

    @Nullable
    private static CrowNestBlockEntity findNest(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof CrowNestBlockEntity nest ? nest : null;
    }

    private static final class EggSlot extends Slot {
        private final CrowNestMenu menu;
        private final int eggIndex;

        private EggSlot(CrowNestMenu menu, int eggIndex, int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
            this.menu = menu;
            this.eggIndex = eggIndex;
        }

        @Override
        public ItemStack getItem() {
            return this.menu.getEggCount() > this.eggIndex ? new ItemStack(Items.EGG) : ItemStack.EMPTY;
        }

        @Override
        public boolean hasItem() {
            return this.menu.getEggCount() > this.eggIndex;
        }

        @Override
        public ItemStack remove(int amount) {
            return amount > 0 && this.menu.removeOneEgg() ? new ItemStack(Items.EGG) : ItemStack.EMPTY;
        }

        @Override
        public boolean mayPickup(Player player) {
            return this.menu.getEggCount() > this.eggIndex;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
