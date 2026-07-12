package EdDYON.guaniao.client.guide;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.client.gui.layout.GuiLayoutConfig;
import EdDYON.guaniao.client.gui.layout.GuiLayoutLoader;
import EdDYON.guaniao.client.gui.layout.GuiLayoutRect;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.columbid.AbstractColumbidEntity;
import EdDYON.guaniao.content.bird.crow.CrowEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.seagull.SeagullEntity;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

public class BirdGuideScreen extends Screen {
    private static final ResourceLocation UI_ATLAS = new ResourceLocation(GuaniaoMod.MOD_ID, "textures/gui/bird_guide_ui.png");
    private static final int UI_ATLAS_W = 1448;
    private static final int UI_ATLAS_H = 1086;
    private static final int TEXT_COLOR = 0xFF3E2B1E;
    private static final int LIGHT_TEXT_COLOR = 0xFFFFF2D0;
    private static final int MUTED_TEXT_COLOR = 0xFF7C6748;
    private static final int ACCENT_TEXT_COLOR = 0xFF8A5C1F;
    private static final int NOTE_TITLE_COLOR = 0xFF2D1D14;
    private static final int PAPER = 0xF0D8BE84;
    private static final int PAPER_LIGHT = 0xFFE9D2A0;
    private static final int PAPER_DARK = 0xFFC59A5D;
    private static final int PAPER_SOFT = 0xD8CFA96D;
    private static final int INK_SHADOW = 0x660F0B08;
    private static final int PANEL_DARK = 0xEA151B22;
    private static final int PANEL_FAINT = 0x88B89055;
    private static final int BLUE_HIGHLIGHT = 0xF0E4BD62;
    private static final int BLUE_HOVER = 0xC8D1AD66;
    private static final int BORDER = 0xFF2B2119;
    private static final int BORDER_SOFT = 0xFF6C5637;
    private static final int DIVIDER = 0xFF806443;
    private static final int EDIT_BORDER = 0xFFB7F0FF;
    private static final int EDIT_ACTIVE = 0xFFFFFFFF;
    private static final int EDIT_HANDLE = 0xFFB7F0FF;
    private static final int EDIT_MIN_SIZE = 24;
    private static final boolean LAYOUT_EDITING_ENABLED = false;
    private static final List<BirdGuideEntry> ENTRIES = List.of(
            new BirdGuideEntry("night_heron", List.of("intro")),
            new BirdGuideEntry("sparrow", List.of("intro")),
            new BirdGuideEntry("long_tailed_tit", List.of("intro")),
            new BirdGuideEntry("cockatiel", List.of("intro")),
            new BirdGuideEntry("macaw", List.of("intro")),
            new BirdGuideEntry("budgerigar", List.of("intro")),
            new BirdGuideEntry("spotted_dove", List.of("intro")),
            new BirdGuideEntry("pigeon", List.of("intro")),
            new BirdGuideEntry("crow", List.of("intro")),
            new BirdGuideEntry("seagull", List.of("intro"))
    );
    private static final PoseKind[] POSES = PoseKind.values();
    private static final List<String> LAYOUT_RECT_IDS = List.of(
            "header",
            "main_panel",
            "species_header",
            "species_list",
            "detail_header",
            "tag_area",
            "info_card",
            "preview_box",
            "pose_buttons",
            "close_button");

    private int selectedIndex;
    private int selectedPoseIndex;
    private int listScrollIndex;
    private int textScroll;
    private LivingEntity previewEntity;
    private final RandomSource previewRandom = RandomSource.create();
    private float previewDragX = 16.0F;
    private float previewDragY = -8.0F;
    private boolean draggingPreview;
    private boolean manualPoseLocked;
    private int manualLookTicks;
    private int motionTicks;
    private int motionDuration = 90;
    private PreviewMotion previewMotion = PreviewMotion.PERCH;
    private GuidePreviewAnimation previewAnimation = GuidePreviewAnimation.IDLE;
    private float birdX;
    private float birdY;
    private float birdScale = 1.0F;
    private GuiLayoutConfig externalLayout;
    private boolean debugLayout;
    private boolean layoutEditMode;
    private final Map<String, GuiLayoutRect> editedRects = new LinkedHashMap<>();
    private String activeLayoutRectId;
    private EditDragMode editDragMode = EditDragMode.NONE;
    private GuiLayoutRect editDragStartRect;
    private int editDragStartMouseX;
    private int editDragStartMouseY;
    private Component editMessage = Component.empty();
    private int editMessageTicks;

    public BirdGuideScreen() {
        super(Component.translatable("gui.guaniao.bird_guide.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.externalLayout = GuiLayoutLoader.loadBirdGuideLayout();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.editMessageTicks > 0) {
            --this.editMessageTicks;
        }
        this.tickPreviewMotion();
        if (this.previewEntity != null) {
            ++this.previewEntity.tickCount;
            this.applyPreviewAnimation(this.previewEntity);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        this.renderShell(graphics);
        this.renderEntryList(graphics, mouseX, mouseY);
        BirdGuideEntry entry = this.selectedEntry(this.selectedIndex);
        this.renderCenterDetails(graphics, entry);
        this.renderPreviewPanel(graphics, mouseX, mouseY);
        this.renderCloseButton(graphics, mouseX, mouseY);
        if (LAYOUT_EDITING_ENABLED && (this.debugLayout || this.layoutEditMode)) {
            this.renderLayoutDebug(graphics);
        }
        if (LAYOUT_EDITING_ENABLED && this.layoutEditMode) {
            this.renderLayoutEditHelp(graphics);
        }
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        if (this.minecraft != null && this.minecraft.level != null) {
            graphics.fill(0, 0, this.width, this.height, 0xC9080E12);
            graphics.fill(0, 0, this.width, this.height, 0x40000000);
            this.drawBackgroundDither(graphics);
        } else {
            this.renderDirtBackground(graphics);
            graphics.fill(0, 0, this.width, this.height, 0xD00A0E12);
            this.drawBackgroundDither(graphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (LAYOUT_EDITING_ENABLED && this.layoutEditMode && button == 0 && this.startLayoutEditDrag(mouseX, mouseY)) {
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (this.closeButtonRect().contains(mouseX, mouseY)) {
            this.onClose();
            return true;
        }
        int pose = this.poseButtonIndexAt(mouseX, mouseY);
        if (pose >= 0) {
            this.selectPose(pose);
            return true;
        }
        if (this.isInPreview(mouseX, mouseY)) {
            this.draggingPreview = true;
            return true;
        }
        GuiLayoutRect list = this.layoutRect("species_list");
        int listX = this.listContentX(list);
        int listY = this.listRowsY(list);
        int stride = this.listRowStride(list);
        if (list.contains(mouseX, mouseY)) {
            int localY = (int)mouseY - listY;
            int row = localY / stride;
            int entryIndex = this.listScrollIndex + row;
            if (entryIndex >= 0 && entryIndex < ENTRIES.size() && row >= 0 && row < this.visibleListRows(list) && localY >= 0 && localY % stride < this.listRowH(list)) {
                if (this.selectedIndex != entryIndex) {
                    this.previewEntity = null;
                }
                this.selectedIndex = entryIndex;
                this.textScroll = 0;
                this.selectedPoseIndex = 0;
                this.manualPoseLocked = false;
                this.resetPreviewMotion();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && LAYOUT_EDITING_ENABLED && this.layoutEditMode && this.editDragMode != EditDragMode.NONE) {
            this.updateLayoutEditDrag(mouseX, mouseY);
            return true;
        }
        if (button == 0 && this.draggingPreview) {
            this.previewDragX = Mth.clamp(this.previewDragX + (float)dragX * 1.7F, -85.0F, 85.0F);
            this.previewDragY = Mth.clamp(this.previewDragY + (float)dragY * 1.25F, -45.0F, 45.0F);
            this.manualLookTicks = 60;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && LAYOUT_EDITING_ENABLED && this.layoutEditMode && this.editDragMode != EditDragMode.NONE) {
            this.editDragMode = EditDragMode.NONE;
            this.editDragStartRect = null;
            return true;
        }
        if (button == 0 && this.draggingPreview) {
            this.draggingPreview = false;
            this.manualLookTicks = 50;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        GuiLayoutRect list = this.layoutRect("species_list");
        if (list.contains(mouseX, mouseY)) {
            int maxScroll = this.maxListScroll(list);
            if (maxScroll > 0) {
                this.listScrollIndex = Mth.clamp(this.listScrollIndex - (int)Math.signum(delta), 0, maxScroll);
                return true;
            }
        }
        if (this.isInNotes(mouseX, mouseY)) {
            int maxScroll = this.maxTextScroll(this.selectedEntry(this.selectedIndex));
            if (maxScroll > 0) {
                this.textScroll = Mth.clamp(this.textScroll - (int)Math.signum(delta) * 18, 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!LAYOUT_EDITING_ENABLED) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_E && Screen.hasControlDown()) {
            this.toggleLayoutEditMode();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_S && Screen.hasControlDown() && this.layoutEditMode) {
            this.saveEditedLayout();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.layoutEditMode) {
            this.layoutEditMode = false;
            this.editDragMode = EditDragMode.NONE;
            this.showEditMessage(Component.literal("Layout edit mode off"));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_L) {
            this.debugLayout = !this.debugLayout;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R && Screen.hasControlDown()) {
            this.editedRects.clear();
            this.layoutEditMode = false;
            this.editDragMode = EditDragMode.NONE;
            this.clearWidgets();
            this.init();
            this.resetPreviewMotion();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderShell(GuiGraphics graphics) {
        GuiLayoutRect header = this.layoutRect("header");
        GuiLayoutRect main = this.layoutRect("main_panel");
        GuiLayoutRect speciesList = this.layoutRect("species_list");
        GuiLayoutRect detailHeader = this.layoutRect("detail_header");
        GuiLayoutRect preview = this.layoutRect("preview_box");

        this.drawPixelBookPanel(graphics, main.x(), main.y(), main.w(), main.h());
        int titleH = Mth.clamp(header.h(), 28, 62);
        int titleW = Math.min(header.w(), Math.round(titleH * 270.0F / 76.0F));
        this.drawAtlas(graphics, header.x(), header.y() - 2, titleW, titleH, 226, 82, 270, 76);

        int firstDivider = speciesList.right() + Math.max(8, (detailHeader.x() - speciesList.right()) / 2);
        int secondDivider = preview.x() - Math.max(8, (preview.x() - detailHeader.right()) / 2);
        int dividerTop = main.y() + 18;
        int dividerBottom = main.bottom() - 22;
        if (firstDivider > main.x() && firstDivider < main.right()) {
            graphics.vLine(firstDivider, dividerTop, dividerBottom, 0x362B2119);
        }
        if (secondDivider > main.x() && secondDivider < main.right()) {
            graphics.vLine(secondDivider, dividerTop, dividerBottom, 0x362B2119);
        }
        this.drawGuideStickers(graphics, main);
    }

    private void renderEntryList(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiLayoutRect header = this.layoutRect("species_header");
        GuiLayoutRect list = this.layoutRect("species_list");
        int panelX = Math.max(0, header.x() - 5);
        int panelY = Math.max(0, header.y() - 6);
        int panelW = list.right() - panelX + 5;
        int panelH = list.bottom() - panelY + 6;
        this.drawDarkGuidePanel(graphics, panelX, panelY, panelW, panelH);
        int x = header.x() + 8;
        int y = header.y() + Math.max(2, (header.h() - 8) / 2);
        graphics.drawString(this.font, Component.translatable("gui.guaniao.bird_guide.species"), x, y, LIGHT_TEXT_COLOR, false);
        String count = String.format("%02d/%02d", this.selectedIndex + 1, ENTRIES.size());
        graphics.fill(header.right() - 47, y - 1, header.right() - 3, y + 10, 0xAA1E2529);
        graphics.drawString(this.font, count, header.right() - 7 - this.font.width(count), y, 0xFFAEE8F4, false);

        graphics.enableScissor(list.x() + 3, list.y() + 3, list.right() - 3, list.bottom() - 3);
        int listX = this.listContentX(list);
        int listW = this.listContentW(list);
        int rowH = this.listRowH(list);
        int stride = this.listRowStride(list);
        int visibleRows = this.visibleListRows(list);
        this.listScrollIndex = Mth.clamp(this.listScrollIndex, 0, this.maxListScroll(list));
        for (int row = 0; row < visibleRows; ++row) {
            int entryIndex = this.listScrollIndex + row;
            if (entryIndex >= ENTRIES.size()) {
                break;
            }
            BirdGuideEntry entry = this.selectedEntry(entryIndex);
            int rowY = this.listRowsY(list) + row * stride;
            boolean selected = this.selectedIndex == entryIndex;
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= rowY && mouseY < rowY + rowH;
            if (selected || hovered) {
                this.drawAtlas(graphics, listX, rowY, listW, rowH, 302, 664, 193, 42);
            } else {
                this.drawAtlas(graphics, listX, rowY, listW, rowH, 86, 618, 194, 42);
            }
            if (selected) {
                this.drawFootprints(graphics, listX + 5, rowY + Math.max(5, rowH / 2 - 5), 0xAA5F4327);
            }
            this.drawColorDot(graphics, listX + 18, rowY + rowH / 2 - 2, this.speciesColor(entry));
            graphics.drawString(this.font, entry.title(), listX + 32, rowY + rowH / 2 - 4, selected ? NOTE_TITLE_COLOR : 0xFFE1D8C5, false);
        }
        graphics.disableScissor();
        this.renderSpeciesScrollBar(graphics, list);
    }

    private void renderCenterDetails(GuiGraphics graphics, BirdGuideEntry entry) {
        GuiLayoutRect detailHeader = this.layoutRect("detail_header");
        GuiLayoutRect tagArea = this.layoutRect("tag_area");
        GuiLayoutRect infoCard = this.infoCardRect();

        this.drawAtlas(graphics, detailHeader.x() - 3, detailHeader.y() - 3, detailHeader.w() + 6, detailHeader.h() + 8, 529, 158, 350, 173);
        graphics.enableScissor(detailHeader.x(), detailHeader.y(), detailHeader.right(), detailHeader.bottom());
        int x = detailHeader.x() + 10;
        int w = detailHeader.w() - 20;
        int titleY = detailHeader.y() + Math.max(5, (detailHeader.h() - 28) / 2);
        this.drawScaledString(graphics, entry.title(), x, titleY, 1.0F, TEXT_COLOR);
        graphics.drawString(this.font, entry.subtitle(), x, titleY + 16, ACCENT_TEXT_COLOR, false);
        this.drawPixelRule(graphics, x, x + w, detailHeader.bottom() - 5);
        graphics.disableScissor();

        graphics.enableScissor(tagArea.x(), tagArea.y(), tagArea.right(), tagArea.bottom());
        this.renderTagChips(graphics, entry, tagArea.x() + 6, tagArea.y() + 5, tagArea.w() - 12);
        graphics.disableScissor();

        this.renderNotes(graphics, entry, infoCard);
    }

    private void renderTagChips(GuiGraphics graphics, BirdGuideEntry entry, int x, int y, int w) {
        int chipX = x;
        int chipY = y;
        int row = 0;
        for (String key : this.tagsFor(entry)) {
            Component text = Component.translatable("gui.guaniao.bird_guide.tag." + key);
            int chipW = this.font.width(text) + 14;
            if (chipX + chipW > x + w) {
                chipX = x;
                chipY += 21;
                row++;
            }
            if (row >= 2) {
                break;
            }
            this.drawPixelButton(graphics, chipX, chipY - 1, chipW, 18, false, false);
            graphics.drawString(this.font, text, chipX + 7, chipY + 4, TEXT_COLOR, false);
            chipX += chipW + 5;
        }
    }

    private void renderNotes(GuiGraphics graphics, BirdGuideEntry entry, GuiLayoutRect rect) {
        int x = rect.x();
        int y = rect.y();
        int w = rect.w();
        int h = rect.h();
        this.drawAtlas(graphics, x - 3, y - 5, w + 6, h + 9, 529, 158, 350, 173);
        MutableComponent title = Component.translatable("gui.guaniao.bird_guide.entry." + entry.id() + ".intro.title");
        int titleX = x + 12;
        int titleY = y + 11;
        graphics.drawString(this.font, title, titleX, titleY, NOTE_TITLE_COLOR, false);
        this.drawPixelRule(graphics, titleX, x + w - 12, titleY + 15);

        int textX = x + 12;
        int textY = titleY + 24;
        int textW = w - 24;
        int textBottom = y + h - 12;
        int maxScroll = this.maxTextScroll(entry);
        this.textScroll = Mth.clamp(this.textScroll, 0, maxScroll);
        graphics.enableScissor(textX, textY, textX + textW, textBottom);
        int lineY = textY - this.textScroll;
        for (String section : entry.sections()) {
            MutableComponent body = Component.translatable("gui.guaniao.bird_guide.entry." + entry.id() + "." + section + ".body");
            for (FormattedCharSequence line : this.font.split((FormattedText)body, textW)) {
                if (lineY >= textY - 10 && lineY < textBottom) {
                    graphics.drawString(this.font, line, textX, lineY, TEXT_COLOR, false);
                }
                lineY += 12;
            }
            lineY += 7;
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int barX = x + w - 9;
            int barTop = textY;
            int barBottom = textBottom;
            int totalHeight = this.detailTextHeight(entry, textW);
            int thumbH = Math.max(16, (barBottom - barTop) * (barBottom - barTop) / Math.max(barBottom - barTop, totalHeight));
            int thumbY = barTop + (barBottom - barTop - thumbH) * this.textScroll / maxScroll;
            graphics.fill(barX, barTop, barX + 1, barBottom, 0x665F4327);
            graphics.fill(barX - 1, thumbY, barX + 2, thumbY + thumbH, 0xFFE0B65C);
        }
    }

    private void renderPreviewPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiLayoutRect main = this.layoutRect("main_panel");
        GuiLayoutRect preview = this.layoutRect("preview_box");
        int titleY = Math.max(main.y() + 8, preview.y() - 28);
        int titleW = Math.min(92, Math.max(64, preview.w() - 42));
        int titleH = 20;
        int titleX = preview.centerX() - titleW / 2;
        this.drawPixelButton(graphics, titleX, titleY - 4, titleW, titleH, false, false);
        this.drawCenteredFittingString(graphics, Component.translatable("gui.guaniao.bird_guide.observation_pose"), titleX, titleY - 4, titleW, titleH, TEXT_COLOR);

        this.drawAtlas(graphics, preview.x(), preview.y(), preview.w(), preview.h(), 925, 92, 421, 387);
        graphics.enableScissor(preview.x() + 4, preview.y() + 4, preview.right() - 4, preview.bottom() - 4);
        LivingEntity entity = this.previewEntity();
        if (entity != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, Math.round(this.birdX), Math.round(this.birdY), this.previewRenderScale(preview), this.previewDragX, this.previewDragY, entity);
        }
        graphics.disableScissor();
        this.renderPoseButtons(graphics, mouseX, mouseY);
    }

    private void renderHabitatStage(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xFF26343A);
        graphics.fill(x + 5, y + 5, x + w - 5, y + h / 2, 0xFF2E4850);
        graphics.fill(x + 5, y + h / 2, x + w - 5, y + h - 5, 0xFF1D2B24);
        for (int px = x + 9; px < x + w - 9; px += 13) {
            int py = y + h - 17 + ((px / 13) % 2);
            graphics.fill(px, py, px + 7, py + 2, 0xFF547747);
        }
        graphics.fill(x + w / 2 - 24, y + h - 20, x + w / 2 + 24, y + h - 17, 0xFF7B5532);
        graphics.fill(x + w / 2 - 18, y + h - 16, x + w / 2 + 18, y + h - 15, 0x77311D12);
        this.drawLeafStamp(graphics, x + w - 25, y + 11, 0x773D6641);
    }

    private void renderPoseButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiLayoutRect poseButtons = this.layoutRect("pose_buttons");
        int y = poseButtons.y();
        int h = this.poseButtonH(poseButtons);
        for (int i = 0; i < POSES.length; i++) {
            int x = this.poseButtonX(poseButtons, i);
            int w = this.poseButtonW(poseButtons);
            boolean selected = this.selectedPoseIndex == i && this.manualPoseLocked;
            boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            this.drawPixelButton(graphics, x, y, w, h, selected, hovered);
            this.drawCenteredFittingString(graphics, Component.translatable(POSES[i].translationKey()), x, y, w, h, selected ? NOTE_TITLE_COLOR : TEXT_COLOR);
        }
    }

    private void renderCloseButton(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiLayoutRect rect = this.closeButtonRect();
        boolean hovered = rect.contains(mouseX, mouseY);
        if (hovered) {
            this.drawAtlas(graphics, rect.x(), rect.y(), rect.w(), rect.h(), 1119, 803, 158, 56);
        } else {
            this.drawAtlas(graphics, rect.x(), rect.y(), rect.w(), rect.h(), 936, 803, 158, 56);
        }
    }

    private LivingEntity previewEntity() {
        if (this.previewEntity == null && this.minecraft != null && this.minecraft.level != null) {
            this.previewEntity = this.selectedEntry(this.selectedIndex).entityType().create((Level)this.minecraft.level);
            if (this.previewEntity != null) {
                if (this.previewEntity instanceof Mob mob) {
                    mob.setNoAi(true);
                }
                this.previewEntity.setNoGravity(true);
                this.previewEntity.setSilent(true);
                this.previewEntity.setOnGround(true);
                this.resetPreviewMotion();
            }
        }
        return this.previewEntity;
    }

    private void resetPreviewMotion() {
        this.manualLookTicks = 0;
        this.motionTicks = 0;
        this.motionDuration = 1;
        this.previewMotion = PreviewMotion.PERCH;
        this.previewAnimation = GuidePreviewAnimation.IDLE;
        GuiLayoutRect preview = this.layoutRect("preview_box");
        this.birdScale = this.basePreviewScale();
        int scale = this.previewRenderScale(preview);
        this.birdX = this.defaultStageX(preview, scale);
        this.birdY = this.defaultStageY(preview, scale);
        if (this.previewEntity != null) {
            this.applyPreviewAnimation(this.previewEntity);
        }
        if (!this.manualPoseLocked) {
            this.chooseNextPreviewMotion();
        } else {
            this.applySelectedPose();
        }
    }

    private void tickPreviewMotion() {
        if (this.manualLookTicks > 0) {
            --this.manualLookTicks;
        }
        if (this.manualPoseLocked) {
            ++this.motionTicks;
            this.applyPreviewMotion();
            return;
        }
        if (++this.motionTicks >= this.motionDuration) {
            this.chooseNextPreviewMotion();
        }
        this.applyPreviewMotion();
    }

    private void chooseNextPreviewMotion() {
        float roll = this.previewRandom.nextFloat();
        if (this.isNightHeronSelected()) {
            if (roll < 0.34F) {
                this.planPerch();
            } else if (roll < 0.48F) {
                this.planWalk();
            } else if (roll < 0.58F) {
                this.planRun();
            } else if (roll < 0.78F) {
                this.planTakeoff();
            } else {
                this.planGlide();
            }
        } else if (roll < 0.46F) {
            this.planPerch();
        } else if (roll < 0.72F) {
            this.planWalk();
        } else if (roll < 0.83F) {
            this.planRun();
        } else if (roll < 0.92F) {
            this.planTakeoff();
        } else {
            this.planGlide();
        }
    }

    private void planPerch() {
        this.setPreviewMotion(PreviewMotion.PERCH, this.randomBetween(72, 118), this.randomIdleGuideAnimation());
    }

    private void planWalk() {
        this.setPreviewMotion(PreviewMotion.WALK, this.randomBetween(50, 88), GuidePreviewAnimation.WALK);
    }

    private void planRun() {
        this.setPreviewMotion(PreviewMotion.RUN, this.randomBetween(28, 48), GuidePreviewAnimation.RUN);
    }

    private void planTakeoff() {
        this.setPreviewMotion(PreviewMotion.TAKEOFF, this.randomBetween(28, 42), GuidePreviewAnimation.FLY_FLAP);
    }

    private void planGlide() {
        this.setPreviewMotion(PreviewMotion.GLIDE, this.randomBetween(54, 84), GuidePreviewAnimation.GLIDE);
    }

    private void setPreviewMotion(PreviewMotion motion, int duration, GuidePreviewAnimation animation) {
        this.previewMotion = motion;
        this.previewAnimation = animation;
        this.motionTicks = 0;
        this.motionDuration = Math.max(1, duration);
        this.lockPreviewModelPosition();
        if (this.previewEntity != null) {
            this.applyPreviewAnimation(this.previewEntity);
        }
    }

    private void selectPose(int poseIndex) {
        this.selectedPoseIndex = Mth.clamp(poseIndex, 0, POSES.length - 1);
        this.manualPoseLocked = true;
        this.applySelectedPose();
    }

    private void applySelectedPose() {
        PoseKind pose = POSES[this.selectedPoseIndex];
        switch (pose) {
            case IDLE -> this.setPreviewMotion(PreviewMotion.PERCH, 120, GuidePreviewAnimation.IDLE);
            case FORAGE -> this.setPreviewMotion(PreviewMotion.PERCH, 120, this.forageAnimationForSelected());
            case FLY -> this.setPreviewMotion(PreviewMotion.GLIDE, 120, GuidePreviewAnimation.GLIDE);
            case ALERT -> this.setPreviewMotion(PreviewMotion.PERCH, 120, GuidePreviewAnimation.LOOK_3);
        }
    }

    private GuidePreviewAnimation forageAnimationForSelected() {
        String id = this.selectedEntry(this.selectedIndex).id();
        if ("budgerigar".equals(id)) {
            return GuidePreviewAnimation.LOOK_2;
        }
        if ("night_heron".equals(id)) {
            return GuidePreviewAnimation.SCRATCH;
        }
        return GuidePreviewAnimation.LOOK_2;
    }

    private void applyPreviewMotion() {
        this.lockPreviewModelPosition();
        if (!this.draggingPreview && this.manualLookTicks <= 0) {
            float targetDragX = switch (this.previewMotion) {
                case GLIDE -> 26.0F;
                case TAKEOFF -> 20.0F;
                case RUN -> 12.0F;
                default -> 9.0F;
            };
            float targetDragY = switch (this.previewMotion) {
                case GLIDE -> -18.0F;
                case TAKEOFF -> -12.0F;
                case RUN -> -7.0F;
                default -> -4.0F;
            };
            this.previewDragX = Mth.lerp(0.12F, this.previewDragX, targetDragX);
            this.previewDragY = Mth.lerp(0.12F, this.previewDragY, targetDragY);
        }
    }

    private void lockPreviewModelPosition() {
        GuiLayoutRect preview = this.layoutRect("preview_box");
        this.birdScale = this.basePreviewScale();
        int scale = this.previewRenderScale(preview);
        this.birdX = this.defaultStageX(preview, scale);
        this.birdY = this.defaultStageY(preview, scale);
    }

    private void applyPreviewAnimation(LivingEntity entity) {
        if (entity instanceof NightHeronEntity nightHeron) {
            nightHeron.setGuidePreviewAnimation(this.toNightHeronPreviewAnimation(this.previewAnimation));
        } else if (entity instanceof SparrowEntity sparrow) {
            sparrow.setGuidePreviewAnimation(this.toSparrowPreviewAnimation(this.previewAnimation));
        } else if (entity instanceof BudgerigarEntity budgerigar) {
            budgerigar.setGuidePreviewAnimation(this.toBudgerigarPreviewAnimation(this.previewAnimation));
        } else if (entity instanceof AbstractColumbidEntity columbid) {
            columbid.setGuidePreviewAnimation(this.toColumbidPreviewAnimation(this.previewAnimation));
        } else if (entity instanceof CrowEntity crow) {
            crow.setGuidePreviewAnimation(this.toCrowPreviewAnimation(this.previewAnimation));
        } else if (entity instanceof SeagullEntity seagull) {
            seagull.setGuidePreviewAnimation(this.toSeagullPreviewAnimation(this.previewAnimation));
        }
    }

    private NightHeronEntity.GuidePreviewAnimation toNightHeronPreviewAnimation(GuidePreviewAnimation animation) {
        return switch (animation) {
            case IDLE -> NightHeronEntity.GuidePreviewAnimation.IDLE;
            case LOOK_1 -> NightHeronEntity.GuidePreviewAnimation.LOOK_1;
            case LOOK_2 -> NightHeronEntity.GuidePreviewAnimation.LOOK_2;
            case LOOK_3 -> NightHeronEntity.GuidePreviewAnimation.LOOK_3;
            case SCRATCH -> NightHeronEntity.GuidePreviewAnimation.SCRATCH;
            case LOOK_5 -> NightHeronEntity.GuidePreviewAnimation.LOOK_5;
            case WALK -> NightHeronEntity.GuidePreviewAnimation.WALK;
            case RUN -> NightHeronEntity.GuidePreviewAnimation.RUN;
            case FLY_FLAP -> NightHeronEntity.GuidePreviewAnimation.FLY_FLAP;
            case GLIDE -> NightHeronEntity.GuidePreviewAnimation.GLIDE;
        };
    }

    private SparrowEntity.GuidePreviewAnimation toSparrowPreviewAnimation(GuidePreviewAnimation animation) {
        return switch (animation) {
            case IDLE -> SparrowEntity.GuidePreviewAnimation.IDLE;
            case LOOK_1, LOOK_5 -> SparrowEntity.GuidePreviewAnimation.TAIL;
            case LOOK_2, SCRATCH -> SparrowEntity.GuidePreviewAnimation.PECK;
            case LOOK_3 -> SparrowEntity.GuidePreviewAnimation.LOOK_AROUND;
            case WALK, RUN -> SparrowEntity.GuidePreviewAnimation.WALK;
            case FLY_FLAP, GLIDE -> SparrowEntity.GuidePreviewAnimation.FLY;
        };
    }

    private BudgerigarEntity.GuidePreviewAnimation toBudgerigarPreviewAnimation(GuidePreviewAnimation animation) {
        return switch (animation) {
            case IDLE -> BudgerigarEntity.GuidePreviewAnimation.IDLE;
            case LOOK_1, SCRATCH -> BudgerigarEntity.GuidePreviewAnimation.PREEN;
            case LOOK_2, LOOK_5 -> BudgerigarEntity.GuidePreviewAnimation.CURIOUS;
            case LOOK_3 -> BudgerigarEntity.GuidePreviewAnimation.DANCE;
            case WALK, RUN -> BudgerigarEntity.GuidePreviewAnimation.WALK;
            case FLY_FLAP, GLIDE -> BudgerigarEntity.GuidePreviewAnimation.FLY;
        };
    }

    private AbstractColumbidEntity.GuidePreviewAnimation toColumbidPreviewAnimation(GuidePreviewAnimation animation) {
        return switch (animation) {
            case IDLE -> AbstractColumbidEntity.GuidePreviewAnimation.IDLE;
            case LOOK_1, SCRATCH -> AbstractColumbidEntity.GuidePreviewAnimation.LOOK_1;
            case LOOK_2 -> AbstractColumbidEntity.GuidePreviewAnimation.LOOK_2;
            case LOOK_3, LOOK_5 -> AbstractColumbidEntity.GuidePreviewAnimation.LOOK_3;
            case WALK, RUN -> AbstractColumbidEntity.GuidePreviewAnimation.WALK;
            case FLY_FLAP -> AbstractColumbidEntity.GuidePreviewAnimation.FLY_FLAP;
            case GLIDE -> AbstractColumbidEntity.GuidePreviewAnimation.GLIDE;
        };
    }

    private CrowEntity.GuidePreviewAnimation toCrowPreviewAnimation(GuidePreviewAnimation animation) {
        return switch (animation) {
            case IDLE -> CrowEntity.GuidePreviewAnimation.IDLE;
            case LOOK_1, SCRATCH -> CrowEntity.GuidePreviewAnimation.LOOK_1;
            case LOOK_2, LOOK_3, LOOK_5 -> CrowEntity.GuidePreviewAnimation.LOOK_2;
            case WALK, RUN -> CrowEntity.GuidePreviewAnimation.WALK;
            case FLY_FLAP, GLIDE -> CrowEntity.GuidePreviewAnimation.FLY;
        };
    }

    private SeagullEntity.GuidePreviewAnimation toSeagullPreviewAnimation(GuidePreviewAnimation animation) {
        return switch (animation) {
            case IDLE -> SeagullEntity.GuidePreviewAnimation.IDLE;
            case LOOK_1, SCRATCH -> SeagullEntity.GuidePreviewAnimation.MOUTH_SCRATCH;
            case LOOK_2 -> SeagullEntity.GuidePreviewAnimation.LAUGH_1;
            case LOOK_3 -> SeagullEntity.GuidePreviewAnimation.IDLE_VARIATION;
            case LOOK_5 -> SeagullEntity.GuidePreviewAnimation.BIG_LAUGH;
            case WALK, RUN -> SeagullEntity.GuidePreviewAnimation.WALK;
            case FLY_FLAP -> SeagullEntity.GuidePreviewAnimation.FLY_FLAP;
            case GLIDE -> SeagullEntity.GuidePreviewAnimation.GLIDE_BOOST;
        };
    }

    private GuidePreviewAnimation randomIdleGuideAnimation() {
        return switch (this.previewRandom.nextInt(6)) {
            case 0 -> GuidePreviewAnimation.IDLE;
            case 1 -> GuidePreviewAnimation.LOOK_1;
            case 2 -> GuidePreviewAnimation.LOOK_2;
            case 3 -> GuidePreviewAnimation.LOOK_3;
            case 4 -> GuidePreviewAnimation.LOOK_5;
            default -> GuidePreviewAnimation.SCRATCH;
        };
    }

    private int randomBetween(int min, int max) {
        return min + this.previewRandom.nextInt(max - min + 1);
    }

    private List<String> tagsFor(BirdGuideEntry entry) {
        return switch (entry.id()) {
            case "night_heron" -> List.of("nocturnal", "wetland", "fish_eater", "alert");
            case "sparrow" -> List.of("diurnal", "village", "seed_eater", "social", "tameable");
            case "long_tailed_tit" -> List.of("diurnal", "forest", "seed_eater", "social", "tameable");
            case "cockatiel" -> List.of("diurnal", "savanna", "seed_eater", "social", "tameable");
            case "macaw" -> List.of("diurnal", "jungle", "fruit_eater", "social", "tameable");
            case "budgerigar" -> List.of("diurnal", "social", "music", "seed_eater", "curious");
            case "spotted_dove" -> List.of("diurnal", "farmland", "pair_bond", "weather_sense", "calm");
            case "pigeon" -> List.of("diurnal", "urban", "social", "seed_eater");
            case "crow" -> List.of("diurnal", "scavenger", "omnivore", "shiny", "alert");
            case "seagull" -> List.of("diurnal", "coast", "omnivore", "scavenger", "bold");
            default -> List.of();
        };
    }

    private int speciesColor(BirdGuideEntry entry) {
        return switch (entry.id()) {
            case "night_heron" -> 0xFF8FCBE6;
            case "sparrow" -> 0xFFD1A065;
            case "long_tailed_tit" -> 0xFFE7D9DC;
            case "cockatiel" -> 0xFFF1D467;
            case "macaw" -> 0xFFE64C45;
            case "budgerigar" -> 0xFFD6DA62;
            case "spotted_dove" -> 0xFF9B8AAE;
            case "pigeon" -> 0xFF9AB3C4;
            case "crow" -> 0xFF7E8798;
            case "seagull" -> 0xFFE7E2D7;
            default -> ACCENT_TEXT_COLOR;
        };
    }

    private int previewRenderScale(GuiLayoutRect preview) {
        float baseScale = Math.min((float)preview.w() * 0.072F, (float)preview.h() * 0.18F);
        return Math.max(34, Math.round(baseScale * this.birdScale));
    }

    private float basePreviewScale() {
        return this.isNightHeronSelected() ? 0.86F : 0.96F;
    }

    private float defaultStageX(GuiLayoutRect preview, int scale) {
        return this.clampStageX(preview, (float)preview.centerX(), scale);
    }

    private float defaultStageY(GuiLayoutRect preview, int scale) {
        float top = this.stageSafeTop(preview, scale);
        float bottom = this.stageSafeBottom(preview, scale);
        if (top > bottom) {
            return preview.y() + preview.h() * 0.58F;
        }
        return Mth.lerp(0.52F, top, bottom);
    }

    private float stageSafeLeft(GuiLayoutRect preview, int scale) {
        return preview.x() + 22.0F + (float)scale * 0.7F;
    }

    private float stageSafeRight(GuiLayoutRect preview, int scale) {
        return preview.right() - 22.0F - (float)scale * 0.7F;
    }

    private float stageSafeTop(GuiLayoutRect preview, int scale) {
        return preview.y() + 20.0F + (float)scale * 0.94F;
    }

    private float stageSafeBottom(GuiLayoutRect preview, int scale) {
        return preview.bottom() - 24.0F - (float)scale * 0.08F;
    }

    private float clampStageX(GuiLayoutRect preview, float x, int scale) {
        float left = this.stageSafeLeft(preview, scale);
        float right = this.stageSafeRight(preview, scale);
        if (left > right) {
            return preview.centerX();
        }
        return Mth.clamp(x, left, right);
    }

    private boolean isInPreview(double mouseX, double mouseY) {
        return this.layoutRect("preview_box").contains(mouseX, mouseY);
    }

    private boolean isInNotes(double mouseX, double mouseY) {
        return this.infoCardRect().contains(mouseX, mouseY);
    }

    private BirdGuideEntry selectedEntry(int index) {
        return ENTRIES.get(Mth.clamp(index, 0, ENTRIES.size() - 1));
    }

    private void toggleLayoutEditMode() {
        if (!this.layoutEditMode) {
            this.captureEditableLayout();
            this.layoutEditMode = true;
            this.debugLayout = false;
            this.showEditMessage(Component.literal("Layout edit mode on"));
        } else {
            this.layoutEditMode = false;
            this.editDragMode = EditDragMode.NONE;
            this.showEditMessage(Component.literal("Layout edit mode off"));
        }
    }

    private void captureEditableLayout() {
        this.editedRects.clear();
        for (String id : LAYOUT_RECT_IDS) {
            GuiLayoutRect rect = "info_card".equals(id) ? this.infoCardRect() : this.layoutRect(id);
            this.editedRects.put(id, rect);
        }
    }

    private boolean startLayoutEditDrag(double mouseX, double mouseY) {
        for (int i = LAYOUT_RECT_IDS.size() - 1; i >= 0; --i) {
            String id = LAYOUT_RECT_IDS.get(i);
            GuiLayoutRect rect = this.editorRect(id);
            EditDragMode mode = this.editModeAt(rect, mouseX, mouseY);
            if (mode != EditDragMode.NONE) {
                this.activeLayoutRectId = id;
                this.editDragMode = mode;
                this.editDragStartRect = rect;
                this.editDragStartMouseX = (int)Math.round(mouseX);
                this.editDragStartMouseY = (int)Math.round(mouseY);
                return true;
            }
        }
        this.activeLayoutRectId = null;
        return false;
    }

    private void updateLayoutEditDrag(double mouseX, double mouseY) {
        if (this.activeLayoutRectId == null || this.editDragStartRect == null || this.editDragMode == EditDragMode.NONE) {
            return;
        }

        int dx = (int)Math.round(mouseX) - this.editDragStartMouseX;
        int dy = (int)Math.round(mouseY) - this.editDragStartMouseY;
        GuiLayoutRect next = this.editDragMode == EditDragMode.MOVE
                ? this.moveEditedRect(this.editDragStartRect, dx, dy)
                : this.resizeEditedRect(this.editDragStartRect, dx, dy, this.editDragMode);
        this.editedRects.put(this.activeLayoutRectId, next);
        if ("preview_box".equals(this.activeLayoutRectId)) {
            this.lockPreviewModelPosition();
        }
    }

    private GuiLayoutRect moveEditedRect(GuiLayoutRect rect, int dx, int dy) {
        int x = Mth.clamp(rect.x() + dx, 0, Math.max(0, this.width - rect.w()));
        int y = Mth.clamp(rect.y() + dy, 0, Math.max(0, this.height - rect.h()));
        return new GuiLayoutRect(x, y, rect.w(), rect.h());
    }

    private GuiLayoutRect resizeEditedRect(GuiLayoutRect rect, int dx, int dy, EditDragMode mode) {
        int left = rect.x();
        int right = rect.right();
        int top = rect.y();
        int bottom = rect.bottom();

        if (mode.left) {
            left += dx;
        }
        if (mode.right) {
            right += dx;
        }
        if (mode.top) {
            top += dy;
        }
        if (mode.bottom) {
            bottom += dy;
        }

        left = Mth.clamp(left, 0, Math.max(0, this.width - EDIT_MIN_SIZE));
        right = Mth.clamp(right, EDIT_MIN_SIZE, this.width);
        top = Mth.clamp(top, 0, Math.max(0, this.height - EDIT_MIN_SIZE));
        bottom = Mth.clamp(bottom, EDIT_MIN_SIZE, this.height);

        if (right - left < EDIT_MIN_SIZE) {
            if (mode.left) {
                left = Math.max(0, right - EDIT_MIN_SIZE);
            } else {
                right = Math.min(this.width, left + EDIT_MIN_SIZE);
            }
        }
        if (bottom - top < EDIT_MIN_SIZE) {
            if (mode.top) {
                top = Math.max(0, bottom - EDIT_MIN_SIZE);
            } else {
                bottom = Math.min(this.height, top + EDIT_MIN_SIZE);
            }
        }

        return new GuiLayoutRect(left, top, right - left, bottom - top);
    }

    private EditDragMode editModeAt(GuiLayoutRect rect, double mouseX, double mouseY) {
        int handle = 5;
        boolean inExpanded = mouseX >= rect.x() - handle && mouseX <= rect.right() + handle
                && mouseY >= rect.y() - handle && mouseY <= rect.bottom() + handle;
        if (!inExpanded) {
            return EditDragMode.NONE;
        }

        boolean left = Math.abs(mouseX - rect.x()) <= handle;
        boolean right = Math.abs(mouseX - rect.right()) <= handle;
        boolean top = Math.abs(mouseY - rect.y()) <= handle;
        boolean bottom = Math.abs(mouseY - rect.bottom()) <= handle;
        if (left && top) {
            return EditDragMode.RESIZE_TOP_LEFT;
        }
        if (right && top) {
            return EditDragMode.RESIZE_TOP_RIGHT;
        }
        if (left && bottom) {
            return EditDragMode.RESIZE_BOTTOM_LEFT;
        }
        if (right && bottom) {
            return EditDragMode.RESIZE_BOTTOM_RIGHT;
        }
        if (left) {
            return EditDragMode.RESIZE_LEFT;
        }
        if (right) {
            return EditDragMode.RESIZE_RIGHT;
        }
        if (top) {
            return EditDragMode.RESIZE_TOP;
        }
        if (bottom) {
            return EditDragMode.RESIZE_BOTTOM;
        }
        return rect.contains(mouseX, mouseY) ? EditDragMode.MOVE : EditDragMode.NONE;
    }

    private void saveEditedLayout() {
        if (this.editedRects.isEmpty()) {
            this.captureEditableLayout();
        }

        Map<String, GuiLayoutRect> rects = new LinkedHashMap<>();
        for (String id : LAYOUT_RECT_IDS) {
            rects.put(id, this.editorRect(id));
        }

        boolean saved = GuiLayoutLoader.saveBirdGuideLayout(this.width, this.height, rects);
        this.externalLayout = GuiLayoutLoader.loadBirdGuideLayout();
        this.editedRects.clear();
        this.editedRects.putAll(rects);
        this.showEditMessage(Component.literal(saved ? "Layout saved" : "Layout save failed"));
    }

    private GuiLayoutRect editorRect(String id) {
        GuiLayoutRect edited = this.editedRects.get(id);
        if (edited != null) {
            return "info_card".equals(id) ? this.infoCardRectFrom(edited) : edited;
        }
        return "info_card".equals(id) ? this.infoCardRect() : this.layoutRect(id);
    }

    private void showEditMessage(Component message) {
        this.editMessage = message;
        this.editMessageTicks = 80;
    }

    private GuiLayoutRect layoutRect(String id) {
        return this.layoutRect(id, this.fallbackRect(id));
    }

    private GuiLayoutRect layoutRect(String id, GuiLayoutRect fallback) {
        GuiLayoutRect edited = this.editedRects.get(id);
        if (edited != null) {
            return edited;
        }
        if (this.externalLayout == null) {
            return fallback;
        }
        return this.externalLayout.rect(id, fallback, this.width, this.height);
    }

    private GuiLayoutRect infoCardRect() {
        return this.infoCardRectFrom(this.layoutRect("info_card"));
    }

    private GuiLayoutRect infoCardRectFrom(GuiLayoutRect raw) {
        GuiLayoutRect tagArea = this.layoutRect("tag_area");
        GuiLayoutRect main = this.layoutRect("main_panel");
        int targetY = Math.max(raw.y(), tagArea.bottom() + 20);
        int maxBottom = main.bottom() - 24;
        int shifted = Math.max(0, targetY - raw.y());
        int h = Math.max(72, raw.h() - shifted);
        if (targetY + h > maxBottom) {
            h = Math.max(72, maxBottom - targetY);
        }
        return new GuiLayoutRect(raw.x(), targetY, raw.w(), h);
    }

    private GuiLayoutRect closeButtonRect() {
        GuiLayoutRect raw = this.layoutRect("close_button");
        int minW = Math.max(48, this.font.width(Component.translatable("gui.guaniao.bird_guide.close")) + 16);
        int minH = 20;
        int w = Mth.clamp(raw.w(), minW, minW + 18);
        int h = Mth.clamp(raw.h(), minH, minH + 6);
        int x = Mth.clamp(raw.centerX() - w / 2, 0, Math.max(0, this.width - w));
        int y = Mth.clamp(raw.centerY() - h / 2, 0, Math.max(0, this.height - h));
        return new GuiLayoutRect(x, y, w, h);
    }

    private GuiLayoutRect fallbackRect(String id) {
        return switch (id) {
            case "header" -> this.scaleBaseRect(64, 25, 480, 65);
            case "main_panel" -> this.scaleBaseRect(38, 87, 1525, 740);
            case "species_header" -> this.scaleBaseRect(82, 126, 338, 54);
            case "species_list" -> this.scaleBaseRect(72, 195, 352, 592);
            case "detail_header" -> this.scaleBaseRect(468, 126, 461, 130);
            case "tag_area" -> this.scaleBaseRect(464, 278, 472, 144);
            case "info_card" -> this.scaleBaseRect(456, 448, 480, 295);
            case "preview_box" -> this.scaleBaseRect(993, 159, 525, 434);
            case "pose_buttons" -> this.scaleBaseRect(985, 622, 528, 83);
            case "close_button" -> this.scaleBaseRect(1360, 750, 158, 51);
            default -> new GuiLayoutRect(0, 0, Math.max(1, this.width), Math.max(1, this.height));
        };
    }

    private GuiLayoutRect scaleBaseRect(int x, int y, int w, int h) {
        return new GuiLayoutRect(x, y, w, h).scale(this.width / 1600.0F, this.height / 900.0F);
    }

    private int listContentX(GuiLayoutRect rect) {
        return rect.x() + 10;
    }

    private int listContentW(GuiLayoutRect rect) {
        return Math.max(20, rect.w() - 20);
    }

    private int listRowsY(GuiLayoutRect rect) {
        return rect.y() + 4;
    }

    private int listRowH(GuiLayoutRect rect) {
        if (ENTRIES.isEmpty()) {
            return 28;
        }
        int gap = 4;
        return Mth.clamp((rect.h() - gap * Math.max(0, ENTRIES.size() - 1)) / ENTRIES.size(), 26, 42);
    }

    private int listRowStride(GuiLayoutRect rect) {
        return this.listRowH(rect) + 4;
    }

    private int visibleListRows(GuiLayoutRect rect) {
        int stride = this.listRowStride(rect);
        if (stride <= 0) {
            return ENTRIES.size();
        }
        return Math.max(1, Math.min(ENTRIES.size(), (rect.h() - 6) / stride));
    }

    private int maxListScroll(GuiLayoutRect rect) {
        return Math.max(0, ENTRIES.size() - this.visibleListRows(rect));
    }

    private void renderSpeciesScrollBar(GuiGraphics graphics, GuiLayoutRect list) {
        int maxScroll = this.maxListScroll(list);
        if (maxScroll <= 0) {
            return;
        }
        int barX = list.right() - 6;
        int barTop = this.listRowsY(list);
        int barBottom = list.bottom() - 6;
        int barH = Math.max(1, barBottom - barTop);
        int visibleRows = this.visibleListRows(list);
        int thumbH = Mth.clamp(barH * visibleRows / ENTRIES.size(), 14, barH);
        int thumbY = barTop + (barH - thumbH) * this.listScrollIndex / maxScroll;
        graphics.fill(barX, barTop, barX + 2, barBottom, 0xAA0B1013);
        graphics.fill(barX - 1, thumbY, barX + 3, thumbY + thumbH, 0xFFB98B4B);
        graphics.fill(barX, thumbY + 1, barX + 2, thumbY + thumbH - 1, 0xFFE8C36D);
    }

    private int detailTextHeight(BirdGuideEntry entry, int textW) {
        int height = 0;
        for (String section : entry.sections()) {
            MutableComponent body = Component.translatable("gui.guaniao.bird_guide.entry." + entry.id() + "." + section + ".body");
            height += this.font.split((FormattedText)body, textW).size() * 12 + 7;
        }
        return height;
    }

    private int maxTextScroll(BirdGuideEntry entry) {
        GuiLayoutRect note = this.infoCardRect();
        int visibleHeight = note.h() - 52;
        return Math.max(0, this.detailTextHeight(entry, note.w() - 28) - visibleHeight + 8);
    }

    private int poseButtonH(GuiLayoutRect rect) {
        return Math.max(24, Math.min(34, rect.h()));
    }

    private int poseButtonGap() {
        return 5;
    }

    private int poseButtonW(GuiLayoutRect rect) {
        int natural = (rect.w() - this.poseButtonGap() * (POSES.length - 1)) / POSES.length;
        return natural < 28 ? Math.max(1, natural) : Mth.clamp(natural, 28, 72);
    }

    private int poseButtonX(GuiLayoutRect rect, int index) {
        int buttonW = this.poseButtonW(rect);
        int totalW = buttonW * POSES.length + this.poseButtonGap() * (POSES.length - 1);
        int startX = rect.x() + Math.max(0, (rect.w() - totalW) / 2);
        return startX + index * (buttonW + this.poseButtonGap());
    }

    private int poseButtonIndexAt(double mouseX, double mouseY) {
        GuiLayoutRect rect = this.layoutRect("pose_buttons");
        int buttonH = this.poseButtonH(rect);
        if (mouseY < rect.y() || mouseY > rect.y() + buttonH) {
            return -1;
        }
        int buttonW = this.poseButtonW(rect);
        for (int i = 0; i < POSES.length; i++) {
            int x = this.poseButtonX(rect, i);
            if (mouseX >= x && mouseX <= x + buttonW) {
                return i;
            }
        }
        return -1;
    }

    private boolean isNightHeronSelected() {
        return "night_heron".equals(this.selectedEntry(this.selectedIndex).id());
    }

    private void renderLayoutDebug(GuiGraphics graphics) {
        for (String id : LAYOUT_RECT_IDS) {
            GuiLayoutRect rect = "close_button".equals(id) ? this.closeButtonRect() : this.editorRect(id);
            boolean active = id.equals(this.activeLayoutRectId);
            int color = active ? EDIT_ACTIVE : "main_panel".equals(id) ? 0xAAB7F0FF : 0xAA9DD6E8;
            this.drawThinBorder(graphics, rect.x(), rect.y(), rect.w(), rect.h(), color);
            this.drawFittingString(graphics, Component.literal(id), rect.x() + 3, rect.y() + 3, rect.w() - 6, 0.55F, color);
            if (this.layoutEditMode) {
                this.drawEditHandles(graphics, rect, active ? EDIT_ACTIVE : EDIT_HANDLE);
            }
        }
    }

    private void renderLayoutEditHelp(GuiGraphics graphics) {
        Component help = Component.literal("Layout Edit  Ctrl+E exit  Drag move  Drag edge resize  Ctrl+S save  Ctrl+R reload");
        int x = 8;
        int y = this.height - 19;
        int w = Math.min(this.width - 16, this.font.width(help) + 14);
        graphics.fill(x, y, x + w, y + 14, 0xAA06131B);
        this.drawThinBorder(graphics, x, y, w, 14, BORDER_SOFT);
        this.drawFittingString(graphics, help, x + 7, y + 3, w - 14, 1.0F, ACCENT_TEXT_COLOR);

        if (this.editMessageTicks > 0) {
            int messageW = Math.min(this.width - 16, this.font.width(this.editMessage) + 14);
            int messageX = this.width - messageW - 8;
            graphics.fill(messageX, y - 17, messageX + messageW, y - 3, 0xAA06131B);
            this.drawThinBorder(graphics, messageX, y - 17, messageW, 14, BORDER_SOFT);
            this.drawFittingString(graphics, this.editMessage, messageX + 7, y - 14, messageW - 14, 1.0F, TEXT_COLOR);
        }
    }

    private void drawEditHandles(GuiGraphics graphics, GuiLayoutRect rect, int color) {
        int size = 4;
        this.drawHandle(graphics, rect.x(), rect.y(), size, color);
        this.drawHandle(graphics, rect.centerX(), rect.y(), size, color);
        this.drawHandle(graphics, rect.right(), rect.y(), size, color);
        this.drawHandle(graphics, rect.x(), rect.centerY(), size, color);
        this.drawHandle(graphics, rect.right(), rect.centerY(), size, color);
        this.drawHandle(graphics, rect.x(), rect.bottom(), size, color);
        this.drawHandle(graphics, rect.centerX(), rect.bottom(), size, color);
        this.drawHandle(graphics, rect.right(), rect.bottom(), size, color);
    }

    private void drawHandle(GuiGraphics graphics, int centerX, int centerY, int size, int color) {
        graphics.fill(centerX - size / 2, centerY - size / 2, centerX + size / 2 + 1, centerY + size / 2 + 1, color);
    }

    private void drawAtlas(GuiGraphics graphics, int x, int y, int w, int h, int u, int v, int uw, int vh) {
        if (w <= 0 || h <= 0 || uw <= 0 || vh <= 0) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate((float)x, (float)y, 0.0F);
        graphics.pose().scale(w / (float)uw, h / (float)vh, 1.0F);
        graphics.blit(UI_ATLAS, 0, 0, u, v, uw, vh, UI_ATLAS_W, UI_ATLAS_H);
        graphics.pose().popPose();
    }

    private void drawPoseButtonFromAtlas(GuiGraphics graphics, int index, int x, int y, int w, int h, boolean highlighted) {
        int[] normalX = {930, 1030, 1131, 1231};
        int[] selectedX = {929, 1030, 1131, 1231};
        int safeIndex = Mth.clamp(index, 0, POSES.length - 1);
        int u = highlighted ? selectedX[safeIndex] : normalX[safeIndex];
        int v = highlighted ? 681 : 617;
        this.drawAtlas(graphics, x, y, w, h, u, v, 96, 50);
    }

    private void drawDarkGuidePanel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x + 4, y + 5, x + w + 4, y + h + 5, 0x77000000);
        graphics.fill(x, y, x + w, y + h, 0xFF6D4D2B);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF1B252A);
        graphics.fill(x + 5, y + 5, x + w - 5, y + h - 5, 0xEE121A1E);
        graphics.fill(x + 5, y + 5, x + w - 5, y + 7, 0xFF45535A);
        graphics.fill(x + 5, y + h - 7, x + w - 5, y + h - 5, 0xFF0B1013);
        graphics.fill(x, y, x + 8, y + 8, 0xFFB98B4B);
        graphics.fill(x + w - 8, y, x + w, y + 8, 0xFFB98B4B);
        graphics.fill(x, y + h - 8, x + 8, y + h, 0xFFB98B4B);
        graphics.fill(x + w - 8, y + h - 8, x + w, y + h, 0xFFB98B4B);
        this.drawPixelRule(graphics, x + 14, x + w - 14, y + 35);
    }

    private void drawBackgroundDither(GuiGraphics graphics) {
        for (int y = 0; y < this.height; y += 18) {
            for (int x = (y / 18) % 2 == 0 ? 0 : 9; x < this.width; x += 36) {
                graphics.fill(x, y, x + 2, y + 2, 0x18000000);
            }
        }
    }

    private void drawPixelBookPanel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x + 4, y + 5, x + w + 4, y + h + 5, 0x88000000);
        graphics.fill(x, y, x + w, y + h, 0xFF4B341F);
        graphics.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xFF0A0F12);
        graphics.fill(x + 8, y + 8, x + w - 8, y + h - 8, 0xD7121B20);
        graphics.fill(x + 10, y + 10, x + w - 10, y + 12, 0xFF775331);
        graphics.fill(x + 10, y + h - 12, x + w - 10, y + h - 10, 0xFF775331);
        graphics.fill(x + 9, y + 9, x + 11, y + h - 9, 0xFF775331);
        graphics.fill(x + w - 11, y + 9, x + w - 9, y + h - 9, 0xFF775331);
    }

    private void drawTitleTab(GuiGraphics graphics, int x, int y, int w, int h) {
        int tabW = Math.min(w, 118);
        graphics.fill(x + 2, y + 2, x + tabW + 2, y + h + 7, 0x66000000);
        graphics.fill(x, y, x + tabW, y + h + 5, BORDER);
        graphics.fill(x + 2, y + 2, x + tabW - 2, y + h + 3, 0xFF26313A);
        graphics.fill(x + 3, y + 3, x + tabW - 3, y + 5, 0xFF52616B);
    }

    private void drawGuideStickers(GuiGraphics graphics, GuiLayoutRect main) {
        this.drawTapeSticker(graphics, main.x() + 28, main.y() + 10, 28, 8, 0xEEDBB476);
        this.drawTapeSticker(graphics, main.right() - 68, main.y() + 12, 34, 8, 0xE7CDA36E);
        this.drawLeafStamp(graphics, main.right() - 36, main.bottom() - 38, 0x884D7548);
        this.drawFootprints(graphics, main.x() + 30, main.bottom() - 38, 0x705F4327);
        this.drawPixelFeather(graphics, main.x() + 112, main.y() + 13, 0xAFFFF2D0, 0x8A8B6D4E);
    }

    private void drawPaperPanel(GuiGraphics graphics, int x, int y, int w, int h, boolean shadow) {
        if (shadow) {
            graphics.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x4A000000);
        }
        graphics.fill(x, y, x + w, y + h, BORDER);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, PAPER_DARK);
        graphics.fill(x + 3, y + 3, x + w - 3, y + h - 3, PAPER_SOFT);
        graphics.fill(x + 4, y + 4, x + w - 4, y + 5, PAPER_LIGHT);
        this.drawPaperNoise(graphics, x + 4, y + 5, w - 8, h - 9);
    }

    private void drawPaperNoise(GuiGraphics graphics, int x, int y, int w, int h) {
        if (w <= 6 || h <= 6) {
            return;
        }
        for (int py = y + 4; py < y + h - 4; py += 17) {
            for (int px = x + 5 + (py % 3); px < x + w - 5; px += 23) {
                graphics.fill(px, py, px + 1, py + 1, 0x26976F3F);
            }
        }
    }

    private void drawPaperLabel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x + 1, y + 2, x + w + 1, y + h + 2, 0x55000000);
        graphics.fill(x, y, x + w, y + h, BORDER);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, PAPER_LIGHT);
    }

    private void drawDisplayCase(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x66000000);
        graphics.fill(x, y, x + w, y + h, BORDER);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF70543A);
        graphics.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xFF172127);
        graphics.fill(x + 5, y + 5, x + w - 5, y + 7, 0xFF3B5962);
        graphics.fill(x + 5, y + 5, x + 7, y + h - 5, 0xFF3B5962);
        this.drawTapeSticker(graphics, x + w - 31, y - 4, 24, 8, 0xEAD7A66C);
    }

    private void drawPixelButton(GuiGraphics graphics, int x, int y, int w, int h, boolean selected, boolean hovered) {
        int inner = selected ? BLUE_HIGHLIGHT : hovered ? BLUE_HOVER : PAPER_LIGHT;
        graphics.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x4A000000);
        graphics.fill(x, y, x + w, y + h, BORDER);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, PAPER_DARK);
        graphics.fill(x + 3, y + 3, x + w - 3, y + h - 3, inner);
        if (selected) {
            graphics.fill(x + 4, y + 4, x + w - 4, y + 6, 0xFFFFE89B);
        }
    }

    private void drawPixelListRow(GuiGraphics graphics, int x, int y, int w, int h, boolean selected, boolean hovered) {
        if (selected || hovered) {
            int fill = selected ? BLUE_HIGHLIGHT : BLUE_HOVER;
            graphics.fill(x + 1, y + 2, x + w + 1, y + h + 2, 0x33000000);
            graphics.fill(x, y, x + w, y + h, BORDER);
            graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, fill);
        } else {
            graphics.fill(x, y, x + w, y + h, PANEL_FAINT);
            graphics.fill(x, y + h - 1, x + w, y + h, 0x336E5636);
        }
    }

    private void drawPixelRule(GuiGraphics graphics, int x1, int x2, int y) {
        graphics.hLine(x1, x2, y, 0x7B6D4F32);
        for (int x = x1; x < x2; x += 9) {
            graphics.fill(x, y + 1, Math.min(x + 3, x2), y + 2, 0x44F3D99C);
        }
    }

    private void drawTapeSticker(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + h, color);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0x44FFFFFF);
        for (int px = x + 3; px < x + w - 2; px += 7) {
            graphics.fill(px, y + 2, px + 1, y + h - 1, 0x24805D3A);
        }
    }

    private void drawFootprints(GuiGraphics graphics, int x, int y, int color) {
        for (int i = 0; i < 3; i++) {
            int ox = x + i * 7;
            int oy = y + (i % 2) * 4;
            graphics.fill(ox, oy + 3, ox + 2, oy + 5, color);
            graphics.fill(ox + 2, oy, ox + 3, oy + 1, color);
            graphics.fill(ox + 3, oy + 2, ox + 4, oy + 3, color);
        }
    }

    private void drawLeafStamp(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 5, y, x + 8, y + 2, color);
        graphics.fill(x + 3, y + 2, x + 10, y + 5, color);
        graphics.fill(x + 1, y + 5, x + 8, y + 8, color);
        graphics.fill(x + 8, y + 5, x + 12, y + 7, color);
        graphics.fill(x + 5, y + 8, x + 7, y + 13, color);
        graphics.fill(x + 7, y + 10, x + 11, y + 11, color);
    }

    private void drawPixelFeather(GuiGraphics graphics, int x, int y, int fill, int shade) {
        graphics.fill(x + 8, y, x + 12, y + 2, fill);
        graphics.fill(x + 6, y + 2, x + 14, y + 4, fill);
        graphics.fill(x + 4, y + 4, x + 15, y + 6, fill);
        graphics.fill(x + 3, y + 6, x + 13, y + 8, fill);
        graphics.fill(x + 2, y + 8, x + 10, y + 10, fill);
        graphics.fill(x, y + 10, x + 7, y + 12, fill);
        graphics.fill(x + 9, y + 2, x + 11, y + 11, shade);
        graphics.fill(x + 4, y + 12, x + 6, y + 15, shade);
        graphics.fill(x + 1, y + 7, x + 3, y + 8, shade);
    }

    private void drawSoftRect(GuiGraphics graphics, int x, int y, int w, int h, int fill, int border) {
        graphics.fill(x, y, x + w, y + h, fill);
        this.drawThinBorder(graphics, x, y, w, h, border);
    }

    private void drawThinBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.hLine(x, x + w, y, color);
        graphics.hLine(x, x + w, y + h, color);
        graphics.vLine(x, y, y + h, color);
        graphics.vLine(x + w, y, y + h, color);
    }

    private void drawColorDot(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y + 1, x + 4, y + 3, color);
        graphics.fill(x + 1, y, x + 3, y + 4, color);
    }

    private void drawCenteredFittingString(GuiGraphics graphics, Component component, int x, int y, int w, int h, int color) {
        int textW = this.font.width(component);
        if (textW <= 0) {
            return;
        }
        float scale = Math.min(1.0F, Math.max(1.0F, w - 10) / (float)textW);
        int scaledW = Math.round(textW * scale);
        int scaledH = Math.round(8.0F * scale);
        int drawX = x + Math.max(0, (w - scaledW) / 2);
        int drawY = y + Math.max(0, (h - scaledH) / 2);
        graphics.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        this.drawScaledString(graphics, component, drawX, drawY, scale, color);
        graphics.disableScissor();
    }

    private void drawFittingString(GuiGraphics graphics, Component component, int x, int y, int maxW, float maxScale, int color) {
        int textW = this.font.width(component);
        if (textW <= 0 || maxW <= 0) {
            return;
        }
        float scale = Math.min(maxScale, maxW / (float)textW);
        this.drawScaledString(graphics, component, x, y, scale, color);
    }

    private void drawScaledString(GuiGraphics graphics, Component component, int x, int y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate((float)x, (float)y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, component, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private enum PreviewMotion {
        PERCH,
        WALK,
        RUN,
        TAKEOFF,
        GLIDE
    }

    private enum GuidePreviewAnimation {
        IDLE,
        LOOK_1,
        LOOK_2,
        LOOK_3,
        SCRATCH,
        LOOK_5,
        WALK,
        RUN,
        FLY_FLAP,
        GLIDE
    }

    private enum PoseKind {
        IDLE("idle"),
        FORAGE("forage"),
        FLY("fly"),
        ALERT("alert");

        private final String key;

        PoseKind(String key) {
            this.key = key;
        }

        private String translationKey() {
            return "gui.guaniao.bird_guide.pose." + this.key;
        }
    }

    private enum EditDragMode {
        NONE(false, false, false, false),
        MOVE(false, false, false, false),
        RESIZE_LEFT(true, false, false, false),
        RESIZE_RIGHT(false, true, false, false),
        RESIZE_TOP(false, false, true, false),
        RESIZE_BOTTOM(false, false, false, true),
        RESIZE_TOP_LEFT(true, false, true, false),
        RESIZE_TOP_RIGHT(false, true, true, false),
        RESIZE_BOTTOM_LEFT(true, false, false, true),
        RESIZE_BOTTOM_RIGHT(false, true, false, true);

        private final boolean left;
        private final boolean right;
        private final boolean top;
        private final boolean bottom;

        EditDragMode(boolean left, boolean right, boolean top, boolean bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }

    private record BirdGuideEntry(String id, List<String> sections) {
        private Component title() {
            return Component.translatable("gui.guaniao.bird_guide.entry." + this.id + ".title");
        }

        private Component subtitle() {
            return Component.translatable("gui.guaniao.bird_guide.entry." + this.id + ".subtitle");
        }

        private EntityType<? extends LivingEntity> entityType() {
            return switch (this.id) {
                case "budgerigar" -> GuaniaoEntityTypes.BUDGERIGAR.get();
                case "sparrow" -> GuaniaoEntityTypes.SPARROW.get();
                case "long_tailed_tit" -> GuaniaoEntityTypes.LONG_TAILED_TIT.get();
                case "cockatiel" -> GuaniaoEntityTypes.COCKATIEL.get();
                case "macaw" -> GuaniaoEntityTypes.MACAW.get();
                case "spotted_dove" -> GuaniaoEntityTypes.SPOTTED_DOVE.get();
                case "pigeon" -> GuaniaoEntityTypes.PIGEON.get();
                case "crow" -> GuaniaoEntityTypes.CROW.get();
                case "seagull" -> GuaniaoEntityTypes.SEAGULL.get();
                default -> GuaniaoEntityTypes.NIGHT_HERON.get();
            };
        }
    }

}
