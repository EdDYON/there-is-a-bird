package EdDYON.guaniao.client.nest;

import EdDYON.guaniao.content.nest.CrowNestLootLayout;
import EdDYON.guaniao.content.nest.CrowNestMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** A vanilla chest screen with masked custom loot areas. */
public final class CrowNestScreen extends AbstractContainerScreen<CrowNestMenu> {
    private static final ResourceLocation GENERIC_54 = new ResourceLocation("textures/gui/container/generic_54.png");
    private static final int SCREEN_WIDTH = 176;
    private static final int SCREEN_HEIGHT = 222;
    private static final int LOOT_GRID_X = 62;
    private static final int LOOT_GRID_Y = 18;
    private static final int LOOT_CELL = 18;

    public CrowNestScreen(CrowNestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = SCREEN_WIDTH;
        this.imageHeight = SCREEN_HEIGHT;
        this.inventoryLabelY = 128;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        List<CrowNestLootLayout.Placement> placements = this.menu.getLootLayout();
        this.renderLootGrid(graphics, placements, mouseX, mouseY);
        this.renderLootTooltip(graphics, placements, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(GENERIC_54, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            CrowNestLootLayout.Placement placement = this.getLootAt(mouseX, mouseY, this.menu.getLootLayout());
            if (placement != null && this.menu.isSlotRummaged(placement.storageSlot())) {
                this.minecraft.gameMode.handleInventoryButtonClick(
                        this.menu.containerId, CrowNestMenu.TAKE_TREASURE_BUTTON_BASE + placement.storageSlot());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderLootGrid(GuiGraphics graphics, List<CrowNestLootLayout.Placement> placements, int mouseX, int mouseY) {
        for (CrowNestLootLayout.Placement placement : placements) {
            int x = this.leftPos + LOOT_GRID_X + placement.column() * LOOT_CELL;
            int y = this.topPos + LOOT_GRID_Y + placement.row() * LOOT_CELL;
            int width = placement.profile().width() * LOOT_CELL;
            int height = placement.profile().height() * LOOT_CELL;
            if (this.menu.isSlotRummaged(placement.storageSlot())) {
                this.renderDiscoveredLoot(graphics, placement, x, y, width, height);
            } else {
                this.renderCoveredLoot(graphics, placement, x, y, width, height);
            }
        }
    }

    private void renderDiscoveredLoot(GuiGraphics graphics, CrowNestLootLayout.Placement placement, int x, int y, int width, int height) {
        ItemStack stack = this.menu.getTreasureStack(placement.storageSlot());
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 120.0F);
        graphics.renderItem(stack, -8, -8);
        graphics.pose().popPose();
    }

    private void renderCoveredLoot(GuiGraphics graphics, CrowNestLootLayout.Placement placement, int x, int y, int width, int height) {
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF5D5D5D);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF2B2B2B);
        if (placement.storageSlot() == this.menu.getCurrentSearchSlot()) {
            int total = Math.max(1, this.menu.getSearchTotalTicks());
            int filled = Mth.floor((width - 4) * Mth.clamp(1.0F - this.menu.getSearchTicks() / (float) total, 0.0F, 1.0F));
            graphics.fill(x + 2, y + height - 4, x + 2 + filled, y + height - 2, 0xFFBFAF6D);
        }
    }

    private void renderLootTooltip(GuiGraphics graphics, List<CrowNestLootLayout.Placement> placements, int mouseX, int mouseY) {
        CrowNestLootLayout.Placement hovered = this.getLootAt(mouseX, mouseY, placements);
        if (hovered != null && this.menu.isSlotRummaged(hovered.storageSlot())) {
            ItemStack stack = this.menu.getTreasureStack(hovered.storageSlot());
            if (!stack.isEmpty()) {
                graphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
        }
    }

    private CrowNestLootLayout.Placement getLootAt(double mouseX, double mouseY, List<CrowNestLootLayout.Placement> placements) {
        for (CrowNestLootLayout.Placement placement : placements) {
            int x = this.leftPos + LOOT_GRID_X + placement.column() * LOOT_CELL;
            int y = this.topPos + LOOT_GRID_Y + placement.row() * LOOT_CELL;
            if (mouseX >= x && mouseX < x + placement.profile().width() * LOOT_CELL
                    && mouseY >= y && mouseY < y + placement.profile().height() * LOOT_CELL) {
                return placement;
            }
        }
        return null;
    }
}
