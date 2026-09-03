package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.content.camera.CameraAperture;
import EdDYON.guaniao.content.camera.CameraFocusMode;
import EdDYON.guaniao.content.camera.CameraLens;
import EdDYON.guaniao.content.camera.CameraShootingMode;
import EdDYON.guaniao.content.camera.CameraState;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Live lens, shooting-mode, and exposure-geometry controls for a held camera. */
public final class CameraCreativeControlsScreen extends Screen {
    private static final int GAP = 8;
    private static final int FOOTER_HEIGHT = 28;
    private static final int HEADER_HEIGHT = 27;
    private static final int TAB_HEIGHT = 24;
    private static final int INFO_HEIGHT = 58;
    private static final int CHOICE_ROW_GAP = 4;
    private static final int MAX_VISIBLE_CHOICE_ROWS = 4;
    private static final int MIN_CHOICE_ROW_HEIGHT = 24;
    private static final int SCREEN_SHADE = 0x78000000;
    private static final int PANEL_BACKGROUND = 0xF2181B1E;
    private static final int SECTION_BACKGROUND = 0xE8202428;
    private static final int ITEM_BACKGROUND = 0xE6262B30;
    private static final int ITEM_HOVER = 0xEE30363C;
    private static final int ITEM_SELECTED = 0xF2333B36;
    private static final int BORDER_DARK = 0xFF0D0F11;
    private static final int BORDER_LIGHT = 0xFF3B4248;
    private static final int TEXT_PRIMARY = 0xFFE4E6E7;
    private static final int TEXT_SECONDARY = 0xFFADB3B8;
    private static final int ACCENT = 0xFFA4D36C;
    private static final int ACCENT_DARK = 0xFF56753B;

    private final CameraState original;
    private CameraState working;
    private Page page = Page.LENS;
    private boolean finished;

    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;
    private int footerTop;
    private int leftLeft;
    private int leftRight;
    private int previewTop;
    private int previewBottom;
    private int infoTop;
    private int rightLeft;
    private int rightRight;
    private int tabsTop;
    private int controlsTop;
    private int controlsBottom;

    private CameraCreativeControlsScreen(CameraState original) {
        super(Component.translatable("gui.guaniao.camera_creative.title"));
        this.original = original;
        this.working = original;
    }

    public static void open() {
        if (!CameraClientCapture.isViewfinderOpen() || CameraClientCapture.isCleanCapturePending()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CameraCreativeControlsScreen controls) {
            controls.confirmAndClose();
            return;
        }
        minecraft.setScreen(new CameraCreativeControlsScreen(CameraClientCapture.currentState()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        recomputeLayout();
        CameraClientCapture.previewState(this.working);
    }

    @Override
    public void tick() {
        super.tick();
        if (!CameraClientCapture.isViewfinderOpen() || CameraClientCapture.isCleanCapturePending()) {
            this.finished = true;
            Minecraft.getInstance().setScreen(null);
            return;
        }
        if (this.working.focusMode() == CameraFocusMode.AF_C) {
            this.working = this.working.withFocusDistance(CameraClientCapture.currentState().focusDistance());
        }
    }

    @Override
    public void removed() {
        if (!this.finished) {
            CameraClientCapture.restorePreviewState(this.original);
        }
        super.removed();
    }

    private void recomputeLayout() {
        int maximumWidth = Math.max(1, this.width - 16);
        int maximumHeight = Math.max(1, this.height - 12);
        int panelWidth = Mth.clamp(Math.round(this.width * 0.88F), Math.min(520, maximumWidth), maximumWidth);
        int panelHeight = Mth.clamp(Math.round(this.height * 0.82F), Math.min(280, maximumHeight), maximumHeight);
        this.panelLeft = (this.width - panelWidth) / 2;
        this.panelTop = (this.height - panelHeight) / 2;
        this.panelRight = this.panelLeft + panelWidth;
        this.panelBottom = this.panelTop + panelHeight;
        this.footerTop = this.panelBottom - FOOTER_HEIGHT;

        int contentLeft = this.panelLeft + GAP;
        int contentRight = this.panelRight - GAP;
        int contentTop = this.panelTop + GAP;
        int contentBottom = this.footerTop - GAP;
        int usableWidth = contentRight - contentLeft - GAP;
        int leftWidth = Math.round(usableWidth * 0.58F);
        this.leftLeft = contentLeft;
        this.leftRight = contentLeft + leftWidth;
        this.rightLeft = this.leftRight + GAP;
        this.rightRight = contentRight;
        this.infoTop = Math.max(contentTop + 64, contentBottom - INFO_HEIGHT);
        this.previewTop = contentTop;
        this.previewBottom = this.infoTop - 5;
        this.tabsTop = contentTop + HEADER_HEIGHT + 5;
        this.controlsTop = this.tabsTop + TAB_HEIGHT + 5;
        this.controlsBottom = contentBottom;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, SCREEN_SHADE);
        drawPanel(graphics, this.panelLeft, this.panelTop, this.panelRight, this.panelBottom, PANEL_BACKGROUND);
        renderPreview(graphics);
        renderControls(graphics, mouseX, mouseY);
        renderFooter(graphics);
    }

    private void renderPreview(GuiGraphics graphics) {
        drawPanel(graphics, this.leftLeft, this.previewTop, this.leftRight, this.previewBottom, 0xFF090B0D);
        CameraPreviewPostEffect.drawPreview(
                graphics,
                this.leftLeft + 3,
                this.previewTop + 3,
                this.leftRight - 3,
                this.previewBottom - 3
        );
        renderPreviewReticle(graphics);

        drawPanel(graphics, this.leftLeft, this.infoTop, this.leftRight, this.controlsBottom, SECTION_BACKGROUND);
        graphics.drawString(
                this.font,
                Component.translatable(this.working.shootingMode().translationKey()),
                this.leftLeft + 10,
                this.infoTop + 10,
                ACCENT,
                false
        );
        graphics.drawString(
                this.font,
                Component.translatable(this.working.lens().translationKey()),
                this.leftLeft + 10,
                this.infoTop + 31,
                TEXT_SECONDARY,
                false
        );

        int dividerX = this.leftLeft + (this.leftRight - this.leftLeft) * 2 / 5;
        graphics.vLine(dividerX, this.infoTop + 7, this.controlsBottom - 7, BORDER_LIGHT);
        int summaryX = dividerX + 9;
        graphics.drawString(
                this.font,
                Component.translatable("gui.guaniao.camera_creative.current_setup"),
                summaryX,
                this.infoTop + 8,
                TEXT_PRIMARY,
                false
        );
        String focus = this.working.hasInfiniteFocus()
                ? "∞"
                : String.format(Locale.ROOT, "%.1fm", this.working.focusDistance());
        String firstLine = String.format(
                Locale.ROOT,
                "%dmm · %s · %s",
                Math.round(this.working.focalLength()),
                this.working.aperture().label(),
                this.working.focusMode().shortName()
        );
        graphics.drawString(this.font, firstLine, summaryX, this.infoTop + 25, ACCENT, false);
        graphics.drawString(
                this.font,
                Component.translatable("gui.guaniao.camera_creative.focus_value", focus),
                summaryX,
                this.infoTop + 39,
                TEXT_SECONDARY,
                false
        );
    }

    private void renderPreviewReticle(GuiGraphics graphics) {
        int inset = Math.max(10, (this.previewBottom - this.previewTop) / 12);
        int left = this.leftLeft + inset;
        int right = this.leftRight - inset;
        int top = this.previewTop + inset;
        int bottom = this.previewBottom - inset;
        int corner = Math.max(12, Math.min(right - left, bottom - top) / 9);
        int color = 0xCDE8F6FF;
        graphics.fill(left, top, left + corner, top + 2, color);
        graphics.fill(left, top, left + 2, top + corner, color);
        graphics.fill(right - corner, top, right, top + 2, color);
        graphics.fill(right - 2, top, right, top + corner, color);
        graphics.fill(left, bottom - 2, left + corner, bottom, color);
        graphics.fill(left, bottom - corner, left + 2, bottom, color);
        graphics.fill(right - corner, bottom - 2, right, bottom, color);
        graphics.fill(right - 2, bottom - corner, right, bottom, color);

        int centerX = (this.leftLeft + this.leftRight) / 2;
        int centerY = (this.previewTop + this.previewBottom) / 2;
        graphics.fill(centerX - 9, centerY, centerX - 3, centerY + 1, color);
        graphics.fill(centerX + 3, centerY, centerX + 9, centerY + 1, color);
        graphics.fill(centerX, centerY - 9, centerX + 1, centerY - 3, color);
        graphics.fill(centerX, centerY + 3, centerX + 1, centerY + 9, color);
    }

    private void renderControls(GuiGraphics graphics, int mouseX, int mouseY) {
        drawPanel(graphics, this.rightLeft, this.panelTop + GAP, this.rightRight, this.controlsBottom, SECTION_BACKGROUND);
        graphics.drawString(this.font, this.title, this.rightLeft + 8, this.panelTop + GAP + 9, ACCENT, false);
        renderTabs(graphics, mouseX, mouseY);
        switch (this.page) {
            case LENS -> renderLensList(graphics, mouseX, mouseY);
            case MODE -> renderModeList(graphics, mouseX, mouseY);
            case PARAMETERS -> renderParameters(graphics, mouseX, mouseY);
        }
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        Page[] pages = Page.values();
        int tabWidth = Math.max(1, (this.rightRight - this.rightLeft - 4) / pages.length);
        for (int i = 0; i < pages.length; i++) {
            int x0 = this.rightLeft + 2 + i * tabWidth;
            int x1 = i == pages.length - 1 ? this.rightRight - 2 : x0 + tabWidth;
            boolean selected = pages[i] == this.page;
            boolean hovered = contains(mouseX, mouseY, x0, this.tabsTop, x1, this.tabsTop + TAB_HEIGHT);
            graphics.fill(x0, this.tabsTop, x1, this.tabsTop + TAB_HEIGHT,
                    selected ? 0xFF384233 : hovered ? 0xFF30363B : 0xFF24292D);
            drawBorder(graphics, x0, this.tabsTop, x1, this.tabsTop + TAB_HEIGHT, selected ? ACCENT_DARK : BORDER_LIGHT);
            drawCentered(graphics, Component.translatable(pages[i].translationKey), x0, this.tabsTop, x1 - x0, TAB_HEIGHT,
                    selected ? ACCENT : TEXT_SECONDARY);
        }
    }

    private void renderLensList(GuiGraphics graphics, int mouseX, int mouseY) {
        CameraLens[] lenses = CameraLens.values();
        int visibleRows = visibleChoiceRows(lenses.length);
        int rowHeight = choiceRowHeight(visibleRows);
        int scrollOffset = choiceScrollOffset(this.working.lens().ordinal(), lenses.length, visibleRows);
        int contentRight = lenses.length > visibleRows ? this.rightRight - 12 : this.rightRight - 3;
        graphics.enableScissor(this.rightLeft + 2, this.controlsTop, this.rightRight - 2, this.controlsBottom);
        for (int row = 0; row < visibleRows; row++) {
            int lensIndex = scrollOffset + row;
            if (lensIndex >= lenses.length) {
                break;
            }
            CameraLens lens = lenses[lensIndex];
            int y0 = this.controlsTop + row * (rowHeight + CHOICE_ROW_GAP);
            int y1 = Math.min(this.controlsBottom, y0 + rowHeight);
            boolean selected = this.working.lens() == lens;
            renderChoiceRow(graphics, mouseX, mouseY, y0, y1, selected, contentRight);
            String focal = Math.round(lens.defaultFocalLength()) + "mm";
            graphics.drawCenteredString(this.font, focal, this.rightLeft + 32, y0 + Math.max(5, (y1 - y0 - 8) / 2),
                    selected ? ACCENT : TEXT_SECONDARY);
            renderNamedDescription(
                    graphics,
                    lens.translationKey(),
                    lens.descriptionKey(),
                    this.rightLeft + 59,
                    y0,
                    contentRight - this.rightLeft - 65,
                    y1 - y0,
                    selected
            );
        }
        graphics.disableScissor();
        renderChoiceScrollbar(graphics, lenses.length, visibleRows, scrollOffset);
    }

    private void renderModeList(GuiGraphics graphics, int mouseX, int mouseY) {
        CameraShootingMode[] modes = CameraShootingMode.values();
        int visibleRows = visibleChoiceRows(modes.length);
        int rowHeight = choiceRowHeight(visibleRows);
        int scrollOffset = choiceScrollOffset(this.working.shootingMode().ordinal(), modes.length, visibleRows);
        int contentRight = modes.length > visibleRows ? this.rightRight - 12 : this.rightRight - 3;
        graphics.enableScissor(this.rightLeft + 2, this.controlsTop, this.rightRight - 2, this.controlsBottom);
        for (int row = 0; row < visibleRows; row++) {
            int modeIndex = scrollOffset + row;
            if (modeIndex >= modes.length) {
                break;
            }
            CameraShootingMode mode = modes[modeIndex];
            int y0 = this.controlsTop + row * (rowHeight + CHOICE_ROW_GAP);
            int y1 = Math.min(this.controlsBottom, y0 + rowHeight);
            boolean selected = this.working.shootingMode() == mode;
            renderChoiceRow(graphics, mouseX, mouseY, y0, y1, selected, contentRight);
            graphics.drawCenteredString(this.font, String.format(Locale.ROOT, "%02d", modeIndex + 1), this.rightLeft + 25,
                    y0 + Math.max(5, (y1 - y0 - 8) / 2), selected ? ACCENT : TEXT_SECONDARY);
            renderNamedDescription(
                    graphics,
                    mode.translationKey(),
                    mode.descriptionKey(),
                    this.rightLeft + 48,
                    y0,
                    contentRight - this.rightLeft - 54,
                    y1 - y0,
                    selected
            );
        }
        graphics.disableScissor();
        renderChoiceScrollbar(graphics, modes.length, visibleRows, scrollOffset);
    }

    private void renderChoiceRow(GuiGraphics graphics, int mouseX, int mouseY, int y0, int y1, boolean selected) {
        renderChoiceRow(graphics, mouseX, mouseY, y0, y1, selected, this.rightRight - 3);
    }

    private void renderChoiceRow(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            int y0,
            int y1,
            boolean selected,
            int x1
    ) {
        int x0 = this.rightLeft + 3;
        boolean hovered = contains(mouseX, mouseY, x0, y0, x1, y1);
        graphics.fill(x0, y0, x1, y1, selected ? ITEM_SELECTED : hovered ? ITEM_HOVER : ITEM_BACKGROUND);
        drawBorder(graphics, x0, y0, x1, y1, selected ? ACCENT : BORDER_LIGHT);
        if (selected) {
            graphics.fill(x0, y0, x0 + 3, y1, ACCENT);
        }
    }

    private void renderChoiceScrollbar(GuiGraphics graphics, int totalRows, int visibleRows, int scrollOffset) {
        if (totalRows <= visibleRows) {
            return;
        }
        int trackLeft = this.rightRight - 9;
        int trackRight = this.rightRight - 4;
        graphics.fill(trackLeft, this.controlsTop, trackRight, this.controlsBottom, 0xFF14181B);
        int trackHeight = this.controlsBottom - this.controlsTop;
        int thumbHeight = Math.max(12, Math.round(trackHeight * (visibleRows / (float)totalRows)));
        int maximum = totalRows - visibleRows;
        int travel = Math.max(0, trackHeight - thumbHeight);
        int thumbTop = this.controlsTop + Math.round(travel * (scrollOffset / (float)maximum));
        graphics.fill(trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, ACCENT);
    }

    private int visibleChoiceRows(int totalRows) {
        int available = Math.max(1, this.controlsBottom - this.controlsTop);
        int fitByHeight = Math.max(1, (available + CHOICE_ROW_GAP) / (MIN_CHOICE_ROW_HEIGHT + CHOICE_ROW_GAP));
        return Math.min(totalRows, Math.min(MAX_VISIBLE_CHOICE_ROWS, fitByHeight));
    }

    private int choiceRowHeight(int visibleRows) {
        int available = Math.max(1, this.controlsBottom - this.controlsTop);
        return Math.max(1, (available - (visibleRows - 1) * CHOICE_ROW_GAP) / visibleRows);
    }

    private static int choiceScrollOffset(int selectedIndex, int totalRows, int visibleRows) {
        return Mth.clamp(selectedIndex - visibleRows + 1, 0, Math.max(0, totalRows - visibleRows));
    }

    private void renderNamedDescription(
            GuiGraphics graphics,
            String nameKey,
            String descriptionKey,
            int x,
            int y,
            int width,
            int height,
            boolean selected
    ) {
        drawLeftFitted(
                graphics,
                Component.translatable(nameKey),
                x,
                y + 7,
                width,
                selected ? ACCENT : TEXT_PRIMARY
        );
        if (height < 38) {
            return;
        }
        List<FormattedCharSequence> lines = this.font.split((FormattedText)Component.translatable(descriptionKey), width);
        if (!lines.isEmpty()) {
            graphics.drawString(this.font, lines.get(0), x, y + 22, TEXT_SECONDARY, false);
        }
    }

    private void renderParameters(GuiGraphics graphics, int mouseX, int mouseY) {
        int rowHeight = parameterRowHeight();
        graphics.enableScissor(this.rightLeft + 2, this.controlsTop, this.rightRight - 2, this.controlsBottom);
        renderSliderRow(
                graphics,
                mouseX,
                mouseY,
                0,
                Component.translatable("gui.guaniao.camera_parameter.focal_length"),
                Math.round(this.working.focalLength()) + "mm",
                (this.working.focalLength() - CameraState.MIN_FOCAL_LENGTH)
                        / (CameraState.MAX_FOCAL_LENGTH - CameraState.MIN_FOCAL_LENGTH),
                rowHeight
        );
        renderApertureRow(graphics, mouseX, mouseY, rowHeight);
        renderFocusModeRow(graphics, mouseX, mouseY, rowHeight);
        double focusProgress = Math.log(this.working.focusDistance() / CameraState.MIN_FOCUS_DISTANCE)
                / Math.log(CameraState.MAX_FOCUS_DISTANCE / CameraState.MIN_FOCUS_DISTANCE);
        String focusValue = this.working.hasInfiniteFocus()
                ? "∞"
                : String.format(Locale.ROOT, "%.1fm", this.working.focusDistance());
        renderSliderRow(
                graphics,
                mouseX,
                mouseY,
                3,
                Component.translatable("gui.guaniao.camera_parameter.focus_distance"),
                focusValue,
                focusProgress,
                rowHeight
        );
        graphics.disableScissor();
    }

    private int parameterRowHeight() {
        int available = Math.max(1, this.controlsBottom - this.controlsTop);
        return Math.max(1, (available - 3 * CHOICE_ROW_GAP) / 4);
    }

    private void renderSliderRow(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            int index,
            Component label,
            String value,
            double progress,
            int rowHeight
    ) {
        int y0 = this.controlsTop + index * (rowHeight + 4);
        int y1 = Math.min(this.controlsBottom, y0 + rowHeight);
        renderChoiceRow(graphics, mouseX, mouseY, y0, y1, false);
        graphics.drawString(this.font, label, this.rightLeft + 10, y0 + 7, TEXT_PRIMARY, false);
        graphics.drawString(this.font, value, this.rightRight - 10 - this.font.width(value), y0 + 7, ACCENT, false);
        int barLeft = this.rightLeft + 10;
        int barRight = this.rightRight - 10;
        int barY = y1 - 13;
        graphics.fill(barLeft, barY, barRight, barY + 3, 0xFF15191C);
        int knobX = Mth.clamp(barLeft + (int)Math.round((barRight - barLeft) * progress), barLeft, barRight);
        graphics.fill(barLeft, barY, knobX, barY + 3, ACCENT_DARK);
        graphics.fill(knobX - 2, barY - 2, knobX + 2, barY + 5, ACCENT);
    }

    private void renderApertureRow(GuiGraphics graphics, int mouseX, int mouseY, int rowHeight) {
        int y0 = this.controlsTop + rowHeight + 4;
        int y1 = Math.min(this.controlsBottom, y0 + rowHeight);
        renderChoiceRow(graphics, mouseX, mouseY, y0, y1, false);
        graphics.drawString(this.font, Component.translatable("gui.guaniao.camera_parameter.aperture"),
                this.rightLeft + 10, y0 + 6, TEXT_PRIMARY, false);
        CameraAperture[] apertures = CameraAperture.values();
        int x0 = this.rightLeft + 7;
        int totalWidth = this.rightRight - this.rightLeft - 14;
        int buttonWidth = Math.max(1, totalWidth / apertures.length);
        int buttonTop = y0 + 19;
        for (int i = 0; i < apertures.length; i++) {
            int left = x0 + i * buttonWidth;
            int right = i == apertures.length - 1 ? this.rightRight - 7 : left + buttonWidth;
            boolean selected = apertures[i] == this.working.aperture();
            graphics.fill(left, buttonTop, right, y1 - 5, selected ? 0xFF384233 : 0xFF202529);
            drawCentered(graphics, Component.literal(apertures[i].label()), left, buttonTop, right - left, y1 - 5 - buttonTop,
                    selected ? ACCENT : TEXT_SECONDARY);
        }
    }

    private void renderFocusModeRow(GuiGraphics graphics, int mouseX, int mouseY, int rowHeight) {
        int y0 = this.controlsTop + 2 * (rowHeight + 4);
        int y1 = Math.min(this.controlsBottom, y0 + rowHeight);
        renderChoiceRow(graphics, mouseX, mouseY, y0, y1, false);
        graphics.drawString(this.font, Component.translatable("gui.guaniao.camera_parameter.focus_mode"),
                this.rightLeft + 10, y0 + 6, TEXT_PRIMARY, false);
        CameraFocusMode[] modes = CameraFocusMode.values();
        int x0 = this.rightLeft + 7;
        int totalWidth = this.rightRight - this.rightLeft - 14;
        int buttonWidth = Math.max(1, totalWidth / modes.length);
        int buttonTop = y0 + 19;
        for (int i = 0; i < modes.length; i++) {
            int left = x0 + i * buttonWidth;
            int right = i == modes.length - 1 ? this.rightRight - 7 : left + buttonWidth;
            boolean selected = modes[i] == this.working.focusMode();
            graphics.fill(left, buttonTop, right, y1 - 5, selected ? 0xFF384233 : 0xFF202529);
            drawCentered(graphics, Component.translatable(modes[i].translationKey()), left, buttonTop,
                    right - left, y1 - 5 - buttonTop, selected ? ACCENT : TEXT_SECONDARY);
        }
    }

    private void renderFooter(GuiGraphics graphics) {
        graphics.fill(this.panelLeft, this.footerTop, this.panelRight, this.panelBottom, 0xF3121518);
        graphics.hLine(this.panelLeft, this.panelRight, this.footerTop, BORDER_LIGHT);
        drawCentered(
                graphics,
                Component.translatable("gui.guaniao.camera_creative.help"),
                this.panelLeft + 8,
                this.footerTop,
                this.panelRight - this.panelLeft - 16,
                FOOTER_HEIGHT,
                TEXT_SECONDARY
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        Page[] pages = Page.values();
        int tabWidth = Math.max(1, (this.rightRight - this.rightLeft - 4) / pages.length);
        for (int i = 0; i < pages.length; i++) {
            int x0 = this.rightLeft + 2 + i * tabWidth;
            int x1 = i == pages.length - 1 ? this.rightRight - 2 : x0 + tabWidth;
            if (contains(mouseX, mouseY, x0, this.tabsTop, x1, this.tabsTop + TAB_HEIGHT)) {
                this.page = pages[i];
                return true;
            }
        }

        if (this.page == Page.LENS) {
            return clickLens(mouseX, mouseY);
        }
        if (this.page == Page.MODE) {
            return clickMode(mouseX, mouseY);
        }
        return clickParameter(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.page == Page.PARAMETERS) {
            int rowHeight = parameterRowHeight();
            int row = (int)((mouseY - this.controlsTop) / (rowHeight + CHOICE_ROW_GAP));
            if (row == 0 || row == 3) {
                return clickParameter(mouseX, mouseY);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean clickLens(double mouseX, double mouseY) {
        CameraLens[] lenses = CameraLens.values();
        int visibleRows = visibleChoiceRows(lenses.length);
        int rowHeight = choiceRowHeight(visibleRows);
        int scrollOffset = choiceScrollOffset(this.working.lens().ordinal(), lenses.length, visibleRows);
        int contentRight = lenses.length > visibleRows ? this.rightRight - 12 : this.rightRight - 3;
        for (int row = 0; row < visibleRows; row++) {
            int lensIndex = scrollOffset + row;
            int y0 = this.controlsTop + row * (rowHeight + CHOICE_ROW_GAP);
            if (lensIndex < lenses.length
                    && contains(mouseX, mouseY, this.rightLeft + 3, y0, contentRight, y0 + rowHeight)) {
                setWorking(this.working.withLens(lenses[lensIndex]));
                return true;
            }
        }
        return true;
    }

    private boolean clickMode(double mouseX, double mouseY) {
        CameraShootingMode[] modes = CameraShootingMode.values();
        int visibleRows = visibleChoiceRows(modes.length);
        int rowHeight = choiceRowHeight(visibleRows);
        int scrollOffset = choiceScrollOffset(this.working.shootingMode().ordinal(), modes.length, visibleRows);
        int contentRight = modes.length > visibleRows ? this.rightRight - 12 : this.rightRight - 3;
        for (int row = 0; row < visibleRows; row++) {
            int modeIndex = scrollOffset + row;
            int y0 = this.controlsTop + row * (rowHeight + CHOICE_ROW_GAP);
            if (modeIndex < modes.length
                    && contains(mouseX, mouseY, this.rightLeft + 3, y0, contentRight, y0 + rowHeight)) {
                setWorking(this.working.withShootingMode(modes[modeIndex]));
                return true;
            }
        }
        return true;
    }

    private boolean clickParameter(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY, this.rightLeft + 3, this.controlsTop, this.rightRight - 3, this.controlsBottom)) {
            return false;
        }
        int rowHeight = parameterRowHeight();
        int row = (int)((mouseY - this.controlsTop) / (rowHeight + CHOICE_ROW_GAP));
        if (row < 0 || row > 3) {
            return true;
        }
        int rowTop = this.controlsTop + row * (rowHeight + 4);
        if (mouseY >= Math.min(this.controlsBottom, rowTop + rowHeight)) {
            return true;
        }
        if (row == 0 || row == 3) {
            double progress = Mth.clamp(
                    (mouseX - (this.rightLeft + 10)) / Math.max(1.0D, this.rightRight - this.rightLeft - 20.0D),
                    0.0D,
                    1.0D
            );
            if (row == 0) {
                double focal = Mth.lerp(progress, CameraState.MIN_FOCAL_LENGTH, CameraState.MAX_FOCAL_LENGTH);
                setWorking(this.working.withFocalLength(Math.round(focal)));
            } else {
                double distance = CameraState.MIN_FOCUS_DISTANCE * Math.pow(
                        CameraState.MAX_FOCUS_DISTANCE / CameraState.MIN_FOCUS_DISTANCE,
                        progress
                );
                setWorking(this.working.withFocusDistance(distance));
            }
            return true;
        }

        int x0 = this.rightLeft + 7;
        int totalWidth = this.rightRight - this.rightLeft - 14;
        if (row == 1) {
            CameraAperture[] apertures = CameraAperture.values();
            int index = Mth.clamp((int)((mouseX - x0) * apertures.length / Math.max(1, totalWidth)), 0, apertures.length - 1);
            setWorking(this.working.withAperture(apertures[index]));
        } else {
            CameraFocusMode[] modes = CameraFocusMode.values();
            int index = Mth.clamp((int)((mouseX - x0) * modes.length / Math.max(1, totalWidth)), 0, modes.length - 1);
            setWorking(this.working.withFocusMode(modes[index]));
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0.0D) {
            return true;
        }
        int direction = delta > 0.0D ? -1 : 1;
        if (this.page == Page.LENS) {
            CameraLens[] values = CameraLens.values();
            int index = Math.floorMod(this.working.lens().ordinal() + direction, values.length);
            setWorking(this.working.withLens(values[index]));
        } else if (this.page == Page.MODE) {
            CameraShootingMode[] values = CameraShootingMode.values();
            int index = Math.floorMod(this.working.shootingMode().ordinal() + direction, values.length);
            setWorking(this.working.withShootingMode(values[index]));
        } else {
            CameraAperture aperture = direction > 0 ? this.working.aperture().next() : this.working.aperture().previous();
            setWorking(this.working.withAperture(aperture));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancelAndClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_C || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirmAndClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_Q) {
            this.page = this.page.previous();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_E) {
            this.page = this.page.next();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            CameraClientCapture.focusAtCrosshair();
            this.working = CameraClientCapture.currentState();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_UP) {
            return mouseScrolled(0.0D, 0.0D, 1.0D);
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_DOWN) {
            return mouseScrolled(0.0D, 0.0D, -1.0D);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void setWorking(CameraState next) {
        this.working = next;
        CameraClientCapture.previewState(next);
    }

    private void confirmAndClose() {
        this.finished = true;
        CameraClientCapture.commitState(this.working);
        Minecraft.getInstance().setScreen(null);
    }

    private void cancelAndClose() {
        this.finished = true;
        CameraClientCapture.restorePreviewState(this.original);
        Minecraft.getInstance().setScreen(null);
    }

    private void drawPanel(GuiGraphics graphics, int left, int top, int right, int bottom, int fill) {
        graphics.fill(left, top, right, bottom, fill);
        drawBorder(graphics, left, top, right, bottom, BORDER_DARK);
        if (right - left > 4 && bottom - top > 4) {
            drawBorder(graphics, left + 1, top + 1, right - 1, bottom - 1, BORDER_LIGHT);
        }
    }

    private static void drawBorder(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.hLine(left, right, top, color);
        graphics.hLine(left, right, bottom, color);
        graphics.vLine(left, top, bottom, color);
        graphics.vLine(right, top, bottom, color);
    }

    private void drawCentered(GuiGraphics graphics, Component text, int x, int y, int width, int height, int color) {
        int textWidth = this.font.width(text);
        float scale = textWidth <= width - 4 ? 1.0F : Math.max(0.5F, (width - 4) / (float)Math.max(1, textWidth));
        int scaledWidth = Math.round(textWidth * scale);
        graphics.pose().pushPose();
        graphics.pose().translate(x + (width - scaledWidth) / 2.0F, y + (height - 8.0F * scale) / 2.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void drawLeftFitted(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        int textWidth = this.font.width(text);
        float scale = textWidth <= width ? 1.0F : Math.max(0.58F, width / (float)Math.max(1, textWidth));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static boolean contains(double x, double y, int left, int top, int right, int bottom) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    private enum Page {
        LENS("gui.guaniao.camera_creative.tab.lens"),
        MODE("gui.guaniao.camera_creative.tab.mode"),
        PARAMETERS("gui.guaniao.camera_creative.tab.parameters");

        private final String translationKey;

        Page(String translationKey) {
            this.translationKey = translationKey;
        }

        private Page next() {
            Page[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        private Page previous() {
            Page[] values = values();
            return values[(this.ordinal() - 1 + values.length) % values.length];
        }
    }
}
