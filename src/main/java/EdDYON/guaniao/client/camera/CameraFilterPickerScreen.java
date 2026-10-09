package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.content.camera.CameraFilter;
import EdDYON.guaniao.content.camera.CameraFilterCategory;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Non-pausing filter library with a large live preview and a compact scrolling list. */
public final class CameraFilterPickerScreen extends Screen {
    private static final int OUTER_GAP = 8;
    private static final int COLUMN_GAP = 9;
    private static final int SECTION_GAP = 5;
    private static final int FOOTER_HEIGHT = 30;
    private static final int HEADER_HEIGHT = 27;
    private static final int TAB_HEIGHT = 24;
    private static final int ORIGINAL_HEIGHT = 21;
    private static final int INFO_HEIGHT = 54;
    private static final int ROW_GAP = 3;
    private static final int MAX_VISIBLE_ROWS = 7;

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

    private final CameraFilter original;
    private CameraFilter highlighted;
    private CameraFilterCategory category;
    private int highlightedIndex;
    private int scrollOffset;
    private int visibleRows;
    private int rowHeight;

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
    private int originalTop;
    private int listTop;
    private int listBottom;
    private boolean finished;

    private CameraFilterPickerScreen(CameraFilter original) {
        super(Component.translatable("gui.guaniao.camera_filter_picker.title"));
        this.original = original;
        this.highlighted = original;
        this.category = original == CameraFilter.NONE ? CameraFilterCategory.NATURAL : original.category();
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!CameraClientCapture.isViewfinderOpen() || CameraClientCapture.isCleanCapturePending()) {
            return;
        }
        if (minecraft.screen instanceof CameraFilterPickerScreen picker) {
            picker.confirmAndClose();
            return;
        }
        minecraft.setScreen(new CameraFilterPickerScreen(CameraClientCapture.currentFilter()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        if (this.highlighted != CameraFilter.NONE) {
            syncIndexToHighlighted();
        }
        recomputeLayout();
        ensureSelectionVisible();
        CameraClientCapture.previewFilter(this.highlighted);
    }

    @Override
    public void tick() {
        super.tick();
        if (!CameraClientCapture.isViewfinderOpen() || CameraClientCapture.isCleanCapturePending()) {
            this.finished = true;
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public void removed() {
        if (!this.finished) {
            CameraClientCapture.restorePreviewFilter(this.original);
        }
        super.removed();
    }

    private void recomputeLayout() {
        int maximumWidth = Math.max(1, this.width - 16);
        int maximumHeight = Math.max(1, this.height - 12);
        int minimumWidth = Math.min(520, maximumWidth);
        int minimumHeight = Math.min(280, maximumHeight);
        int panelWidth = Mth.clamp(Math.round(this.width * 0.88F), minimumWidth, maximumWidth);
        int panelHeight = Mth.clamp(Math.round(this.height * 0.82F), minimumHeight, maximumHeight);

        this.panelLeft = (this.width - panelWidth) / 2;
        this.panelTop = (this.height - panelHeight) / 2;
        this.panelRight = this.panelLeft + panelWidth;
        this.panelBottom = this.panelTop + panelHeight;
        this.footerTop = this.panelBottom - FOOTER_HEIGHT;

        int contentLeft = this.panelLeft + OUTER_GAP;
        int contentRight = this.panelRight - OUTER_GAP;
        int contentTop = this.panelTop + OUTER_GAP;
        int contentBottom = this.footerTop - OUTER_GAP;
        int usableWidth = contentRight - contentLeft - COLUMN_GAP;
        int leftWidth = Math.round(usableWidth * 0.58F);

        this.leftLeft = contentLeft;
        this.leftRight = contentLeft + leftWidth;
        this.rightLeft = this.leftRight + COLUMN_GAP;
        this.rightRight = contentRight;
        this.infoTop = Math.max(contentTop + 60, contentBottom - INFO_HEIGHT);
        this.previewTop = contentTop;
        this.previewBottom = this.infoTop - SECTION_GAP;

        this.tabsTop = contentTop + HEADER_HEIGHT + SECTION_GAP;
        this.originalTop = this.tabsTop + TAB_HEIGHT + SECTION_GAP;
        this.listTop = this.originalTop + ORIGINAL_HEIGHT + SECTION_GAP;
        this.listBottom = contentBottom;

        int listHeight = Math.max(1, this.listBottom - this.listTop);
        this.visibleRows = Mth.clamp((listHeight + ROW_GAP) / (32 + ROW_GAP), 1, MAX_VISIBLE_ROWS);
        this.rowHeight = Math.max(24, (listHeight - (this.visibleRows - 1) * ROW_GAP) / this.visibleRows);
    }

    private List<CameraFilter> currentFilters() {
        return CameraFilter.inCategory(this.category);
    }

    CameraFilterCategory previewCategory() {
        return this.category;
    }

    private void syncIndexToHighlighted() {
        List<CameraFilter> filters = currentFilters();
        int index = filters.indexOf(this.highlighted);
        this.highlightedIndex = index >= 0 ? index : 0;
        if (!filters.isEmpty() && index < 0) {
            this.highlighted = filters.get(this.highlightedIndex);
        }
    }

    private void ensureSelectionVisible() {
        List<CameraFilter> filters = currentFilters();
        int maximum = Math.max(0, filters.size() - this.visibleRows);
        if (this.highlighted != CameraFilter.NONE) {
            if (this.highlightedIndex < this.scrollOffset) {
                this.scrollOffset = this.highlightedIndex;
            } else if (this.highlightedIndex >= this.scrollOffset + this.visibleRows) {
                this.scrollOffset = this.highlightedIndex - this.visibleRows + 1;
            }
        }
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maximum);
    }

    private void setCategory(CameraFilterCategory next) {
        if (next == null || next == this.category) {
            return;
        }
        this.category = next;
        this.highlightedIndex = 0;
        this.scrollOffset = 0;
        List<CameraFilter> filters = currentFilters();
        if (!filters.isEmpty()) {
            select(filters.get(0));
        }
    }

    private void select(CameraFilter filter) {
        this.highlighted = filter;
        CameraClientCapture.previewFilter(filter);
        ensureSelectionVisible();
    }

    private void selectOriginal() {
        select(CameraFilter.NONE);
    }

    private void moveSelection(int delta) {
        List<CameraFilter> filters = currentFilters();
        if (filters.isEmpty()) {
            return;
        }
        if (this.highlighted == CameraFilter.NONE) {
            this.highlightedIndex = delta < 0 ? filters.size() - 1 : 0;
        } else {
            this.highlightedIndex = Math.floorMod(this.highlightedIndex + delta, filters.size());
        }
        select(filters.get(this.highlightedIndex));
    }

    private void randomFilter() {
        this.highlighted = CameraFilter.byId(ThreadLocalRandom.current().nextInt(1, CameraFilter.MAX_ID + 1));
        this.category = this.highlighted.category();
        this.scrollOffset = 0;
        syncIndexToHighlighted();
        select(this.highlighted);
    }

    private void confirmAndClose() {
        this.finished = true;
        CameraClientCapture.commitFilter(this.highlighted);
        Minecraft.getInstance().setScreen(null);
    }

    private void cancelAndClose() {
        this.finished = true;
        CameraClientCapture.restorePreviewFilter(this.original);
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, SCREEN_SHADE);
        drawPanel(graphics, this.panelLeft, this.panelTop, this.panelRight, this.panelBottom, PANEL_BACKGROUND);
        renderLeftPreview(graphics);
        renderRightLibrary(graphics, mouseX, mouseY);
        renderFooter(graphics);
    }

    private void renderLeftPreview(GuiGraphics graphics) {
        drawPanel(graphics, this.leftLeft, this.previewTop, this.leftRight, this.previewBottom, 0xFF090B0D);
        CameraPreviewPostEffect.drawPreview(
                graphics,
                this.leftLeft + 3,
                this.previewTop + 3,
                this.leftRight - 3,
                this.previewBottom - 3
        );
        drawFocusMarks(
                graphics,
                this.leftLeft + 3,
                this.previewTop + 3,
                this.leftRight - 3,
                this.previewBottom - 3
        );

        drawPanel(graphics, this.leftLeft, this.infoTop, this.leftRight, this.listBottom, SECTION_BACKGROUND);
        String filterName = Component.translatable(this.highlighted.translationKey()).getString();
        String selected = String.format(Locale.ROOT, "#%02d  %s", categoryDisplayNumber(this.highlighted), filterName);
        graphics.drawString(this.font, selected, this.leftLeft + 10, this.infoTop + 11, ACCENT, false);
        graphics.drawString(
                this.font,
                Component.translatable(this.category.translationKey()),
                this.leftLeft + 10,
                this.infoTop + 30,
                TEXT_SECONDARY,
                false
        );

        int dividerX = Math.min(this.leftRight - 120, this.leftLeft + (this.leftRight - this.leftLeft) * 2 / 5);
        graphics.vLine(dividerX, this.infoTop + 7, this.listBottom - 7, BORDER_LIGHT);
        int noteX = dividerX + 9;
        int noteWidth = Math.max(32, this.leftRight - noteX - 9);
        graphics.drawString(
                this.font,
                Component.translatable("gui.guaniao.camera_filter_picker.lens_note"),
                noteX,
                this.infoTop + 8,
                ACCENT,
                false
        );
        List<FormattedCharSequence> noteLines = this.font.split(
                (FormattedText)Component.translatable(this.highlighted.descriptionKey()),
                noteWidth
        );
        int noteY = this.infoTop + 24;
        for (int i = 0; i < Math.min(2, noteLines.size()); i++) {
            graphics.drawString(this.font, noteLines.get(i), noteX, noteY + i * 11, TEXT_SECONDARY, false);
        }
    }

    private void renderRightLibrary(GuiGraphics graphics, int mouseX, int mouseY) {
        int contentTop = this.panelTop + OUTER_GAP;
        drawPanel(graphics, this.rightLeft, contentTop, this.rightRight, this.listBottom, SECTION_BACKGROUND);

        graphics.drawString(this.font, this.title, this.rightLeft + 8, contentTop + 9, ACCENT, false);
        String count = "50 / 50";
        graphics.drawString(
                this.font,
                count,
                this.rightRight - 8 - this.font.width(count),
                contentTop + 9,
                TEXT_SECONDARY,
                false
        );

        renderTabs(graphics, mouseX, mouseY);
        renderOriginalButton(graphics, mouseX, mouseY);
        renderFilterList(graphics, mouseX, mouseY);
        renderScrollBar(graphics);
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        CameraFilterCategory[] categories = CameraFilterCategory.values();
        int tabWidth = Math.max(1, (this.rightRight - this.rightLeft - 4) / categories.length);
        for (int i = 0; i < categories.length; i++) {
            int x0 = this.rightLeft + 2 + i * tabWidth;
            int x1 = i == categories.length - 1 ? this.rightRight - 2 : x0 + tabWidth;
            CameraFilterCategory candidate = categories[i];
            boolean selected = candidate == this.category;
            boolean hovered = contains(mouseX, mouseY, x0, this.tabsTop, x1, this.tabsTop + TAB_HEIGHT);
            int background = selected ? 0xFF384233 : hovered ? 0xFF30363B : 0xFF24292D;
            graphics.fill(x0, this.tabsTop, x1, this.tabsTop + TAB_HEIGHT, background);
            drawBorder(graphics, x0, this.tabsTop, x1, this.tabsTop + TAB_HEIGHT, selected ? ACCENT_DARK : BORDER_LIGHT);
            drawCenteredFittingString(
                    graphics,
                    Component.translatable(candidate.translationKey()),
                    x0 + 2,
                    this.tabsTop,
                    x1 - x0 - 4,
                    TAB_HEIGHT,
                    selected ? ACCENT : TEXT_SECONDARY
            );
        }
    }

    private void renderOriginalButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x0 = this.rightLeft + 3;
        int x1 = this.rightRight - 3;
        boolean selected = this.highlighted == CameraFilter.NONE;
        boolean hovered = contains(mouseX, mouseY, x0, this.originalTop, x1, this.originalTop + ORIGINAL_HEIGHT);
        graphics.fill(
                x0,
                this.originalTop,
                x1,
                this.originalTop + ORIGINAL_HEIGHT,
                selected ? ITEM_SELECTED : hovered ? ITEM_HOVER : ITEM_BACKGROUND
        );
        if (selected) {
            graphics.fill(x0, this.originalTop, x0 + 3, this.originalTop + ORIGINAL_HEIGHT, ACCENT);
        }
        drawBorder(graphics, x0, this.originalTop, x1, this.originalTop + ORIGINAL_HEIGHT, selected ? ACCENT_DARK : BORDER_LIGHT);
        String label = "#00  " + Component.translatable(CameraFilter.NONE.translationKey()).getString();
        graphics.drawString(
                this.font,
                (selected ? "> " : "  ") + label,
                x0 + 7,
                this.originalTop + (ORIGINAL_HEIGHT - 8) / 2,
                selected ? ACCENT : TEXT_PRIMARY,
                false
        );
    }

    private void renderFilterList(GuiGraphics graphics, int mouseX, int mouseY) {
        List<CameraFilter> filters = currentFilters();
        int contentRight = this.rightRight - 12;
        graphics.enableScissor(this.rightLeft + 2, this.listTop, this.rightRight - 2, this.listBottom);
        for (int row = 0; row < this.visibleRows; row++) {
            int filterIndex = this.scrollOffset + row;
            if (filterIndex >= filters.size()) {
                break;
            }
            CameraFilter filter = filters.get(filterIndex);
            int y0 = this.listTop + row * (this.rowHeight + ROW_GAP);
            int y1 = Math.min(this.listBottom, y0 + this.rowHeight);
            boolean selected = filter == this.highlighted;
            boolean hovered = contains(mouseX, mouseY, this.rightLeft + 3, y0, contentRight, y1);
            graphics.fill(
                    this.rightLeft + 3,
                    y0,
                    contentRight,
                    y1,
                    selected ? ITEM_SELECTED : hovered ? ITEM_HOVER : ITEM_BACKGROUND
            );
            drawBorder(
                    graphics,
                    this.rightLeft + 3,
                    y0,
                    contentRight,
                    y1,
                    selected ? ACCENT : BORDER_LIGHT
            );
            if (selected) {
                graphics.fill(this.rightLeft + 3, y0, this.rightLeft + 6, y1, ACCENT);
            }

            int thumbnailHeight = Math.max(12, y1 - y0 - 8);
            int thumbnailWidth = Math.min(72, Math.max(24, Math.round(thumbnailHeight * 1.6F)));
            int thumbnailX = this.rightLeft + 10;
            int thumbnailY = y0 + (y1 - y0 - thumbnailHeight) / 2;
            boolean thumbnailDrawn = CameraPreviewPostEffect.drawFilterThumbnail(
                    graphics,
                    filter,
                    thumbnailX,
                    thumbnailY,
                    thumbnailX + thumbnailWidth,
                    thumbnailY + thumbnailHeight
            );
            if (!thumbnailDrawn) {
                renderPaletteThumbnail(graphics, filter, thumbnailX, thumbnailY, thumbnailWidth, thumbnailHeight);
            }
            drawBorder(
                    graphics,
                    thumbnailX,
                    thumbnailY,
                    thumbnailX + thumbnailWidth,
                    thumbnailY + thumbnailHeight,
                    BORDER_DARK
            );

            int textX = thumbnailX + thumbnailWidth + 9;
            int textWidth = Math.max(8, contentRight - textX - 6);
            String name = (selected ? "> " : "  ") + String.format(
                    Locale.ROOT,
                    "%02d  %s",
                    filterIndex + 1,
                    Component.translatable(filter.translationKey()).getString()
            );
            if (this.font.width(name) > textWidth) {
                name = this.font.plainSubstrByWidth(name, Math.max(6, textWidth - 6)) + "…";
            }
            graphics.drawString(
                    this.font,
                    name,
                    textX,
                    y0 + Math.max(4, (y1 - y0 - 8) / 2),
                    selected ? ACCENT : TEXT_PRIMARY,
                    false
            );
        }
        graphics.disableScissor();
    }

    private static int categoryDisplayNumber(CameraFilter filter) {
        if (filter == CameraFilter.NONE) {
            return 0;
        }
        int index = CameraFilter.inCategory(filter.category()).indexOf(filter);
        return index < 0 ? 0 : index + 1;
    }

    private void renderPaletteThumbnail(
            GuiGraphics graphics,
            CameraFilter filter,
            int x,
            int y,
            int width,
            int height
    ) {
        float position = (filter.id() % 10) / 10.0F;
        float hue;
        float saturation;
        float brightness;
        switch (filter.category()) {
            case NATURAL -> {
                hue = 0.28F + position * 0.30F;
                saturation = 0.58F;
                brightness = 0.82F;
            }
            case FILM -> {
                hue = 0.06F + position * 0.10F;
                saturation = 0.52F;
                brightness = 0.73F;
            }
            case MONO -> {
                hue = 0.0F;
                saturation = 0.04F;
                brightness = 0.42F + position * 0.42F;
            }
            case MOOD -> {
                hue = 0.72F + position * 0.34F;
                saturation = 0.46F;
                brightness = 0.86F;
            }
            case CREATIVE -> {
                hue = 0.48F + position * 0.58F;
                saturation = 0.88F;
                brightness = 0.88F;
            }
            default -> throw new IllegalStateException("Unexpected filter category");
        }
        hue -= (float)Math.floor(hue);
        int sky = 0xFF000000 | Mth.hsvToRgb(hue, saturation * 0.72F, brightness);
        int groundHueOffset = Mth.hsvToRgb(
                hue + 0.08F >= 1.0F ? hue - 0.92F : hue + 0.08F,
                saturation,
                brightness * 0.58F
        );
        int highlightHueOffset = Mth.hsvToRgb(
                hue + 0.16F >= 1.0F ? hue - 0.84F : hue + 0.16F,
                Math.max(0.0F, saturation - 0.12F),
                Math.min(1.0F, brightness + 0.10F)
        );
        int ground = 0xFF000000 | groundHueOffset;
        int highlight = 0xFF000000 | highlightHueOffset;

        int horizon = y + Math.max(4, height * 5 / 9);
        graphics.fill(x, y, x + width, horizon, sky);
        graphics.fill(x, horizon, x + width, y + height, ground);
        graphics.fill(x + width / 7, horizon - height / 5, x + width / 3, horizon, highlight);
        graphics.fill(x + width / 3, horizon - height / 3, x + width * 3 / 5, horizon, highlight);
        graphics.fill(x + width * 3 / 5, horizon - height / 6, x + width * 6 / 7, horizon, highlight);
        drawBorder(graphics, x, y, x + width, y + height, BORDER_DARK);
    }

    private void renderScrollBar(GuiGraphics graphics) {
        List<CameraFilter> filters = currentFilters();
        int x0 = this.rightRight - 8;
        int x1 = this.rightRight - 4;
        graphics.fill(x0, this.listTop, x1, this.listBottom, 0xFF111416);
        int maximum = Math.max(0, filters.size() - this.visibleRows);
        if (maximum <= 0) {
            graphics.fill(x0 + 1, this.listTop + 1, x1 - 1, this.listBottom - 1, BORDER_LIGHT);
            return;
        }
        int trackHeight = Math.max(1, this.listBottom - this.listTop);
        int thumbHeight = Math.max(16, trackHeight * this.visibleRows / filters.size());
        int thumbTravel = trackHeight - thumbHeight;
        int thumbTop = this.listTop + Math.round(thumbTravel * (this.scrollOffset / (float)maximum));
        graphics.fill(x0, thumbTop, x1, thumbTop + thumbHeight, ACCENT_DARK);
        graphics.fill(x0 + 1, thumbTop + 1, x1 - 1, thumbTop + thumbHeight - 1, ACCENT);
    }

    private void renderFooter(GuiGraphics graphics) {
        graphics.fill(this.panelLeft, this.footerTop, this.panelRight, this.panelBottom, 0xF3121518);
        graphics.hLine(this.panelLeft, this.panelRight, this.footerTop, BORDER_LIGHT);
        drawCenteredFittingString(
                graphics,
                Component.translatable("gui.guaniao.camera_filter_picker.help"),
                this.panelLeft + 8,
                this.footerTop,
                this.panelRight - this.panelLeft - 16,
                FOOTER_HEIGHT,
                TEXT_SECONDARY
        );
    }

    private void drawFocusMarks(GuiGraphics graphics, int left, int top, int right, int bottom) {
        int length = Mth.clamp(Math.min(right - left, bottom - top) / 9, 9, 22);
        int inset = 12;
        int x0 = left + inset;
        int y0 = top + inset;
        int x1 = right - inset;
        int y1 = bottom - inset;
        int color = 0xDDE4E6E7;

        graphics.fill(x0, y0, x0 + length, y0 + 2, color);
        graphics.fill(x0, y0, x0 + 2, y0 + length, color);
        graphics.fill(x1 - length, y0, x1, y0 + 2, color);
        graphics.fill(x1 - 2, y0, x1, y0 + length, color);
        graphics.fill(x0, y1 - 2, x0 + length, y1, color);
        graphics.fill(x0, y1 - length, x0 + 2, y1, color);
        graphics.fill(x1 - length, y1 - 2, x1, y1, color);
        graphics.fill(x1 - 2, y1 - length, x1, y1, color);

        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;
        graphics.fill(centerX - 8, centerY, centerX - 2, centerY + 1, color);
        graphics.fill(centerX + 3, centerY, centerX + 9, centerY + 1, color);
        graphics.fill(centerX, centerY - 8, centerX + 1, centerY - 2, color);
        graphics.fill(centerX, centerY + 3, centerX + 1, centerY + 9, color);
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

    private void drawCenteredFittingString(
            GuiGraphics graphics,
            Component text,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        int textWidth = this.font.width(text);
        if (textWidth <= 0 || width <= 0) {
            return;
        }
        float scale = Math.min(1.0F, Math.max(0.42F, (width - 6) / (float)textWidth));
        int scaledWidth = Math.round(textWidth * scale);
        int scaledHeight = Math.round(8.0F * scale);
        graphics.pose().pushPose();
        graphics.pose().translate(x + (width - scaledWidth) / 2.0F, y + (height - scaledHeight) / 2.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static boolean contains(double x, double y, int left, int top, int right, int bottom) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        CameraFilterCategory[] categories = CameraFilterCategory.values();
        int tabWidth = Math.max(1, (this.rightRight - this.rightLeft - 4) / categories.length);
        for (int i = 0; i < categories.length; i++) {
            int x0 = this.rightLeft + 2 + i * tabWidth;
            int x1 = i == categories.length - 1 ? this.rightRight - 2 : x0 + tabWidth;
            if (contains(mouseX, mouseY, x0, this.tabsTop, x1, this.tabsTop + TAB_HEIGHT)) {
                setCategory(categories[i]);
                return true;
            }
        }

        if (contains(
                mouseX,
                mouseY,
                this.rightLeft + 3,
                this.originalTop,
                this.rightRight - 3,
                this.originalTop + ORIGINAL_HEIGHT
        )) {
            selectOriginal();
            return true;
        }

        if (contains(mouseX, mouseY, this.rightRight - 10, this.listTop, this.rightRight, this.listBottom)) {
            scrollFromTrack(mouseY);
            return true;
        }

        List<CameraFilter> filters = currentFilters();
        for (int row = 0; row < this.visibleRows; row++) {
            int filterIndex = this.scrollOffset + row;
            if (filterIndex >= filters.size()) {
                break;
            }
            int y0 = this.listTop + row * (this.rowHeight + ROW_GAP);
            int y1 = Math.min(this.listBottom, y0 + this.rowHeight);
            if (contains(mouseX, mouseY, this.rightLeft + 3, y0, this.rightRight - 12, y1)) {
                this.highlightedIndex = filterIndex;
                select(filters.get(filterIndex));
                return true;
            }
        }
        return true;
    }

    private void scrollFromTrack(double mouseY) {
        List<CameraFilter> filters = currentFilters();
        int maximum = Math.max(0, filters.size() - this.visibleRows);
        if (maximum <= 0) {
            return;
        }
        double progress = Mth.clamp(
                (mouseY - this.listTop) / Math.max(1.0D, this.listBottom - this.listTop),
                0.0D,
                1.0D
        );
        this.scrollOffset = Mth.clamp((int)Math.round(progress * maximum), 0, maximum);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0.0D) {
            return true;
        }
        moveSelection(delta > 0.0D ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancelAndClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_V || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirmAndClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            randomFilter();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_Q) {
            setCategory(this.category.previous());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_E) {
            setCategory(this.category.next());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_0 || keyCode == GLFW.GLFW_KEY_KP_0) {
            selectOriginal();
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_5) {
            setCategory(CameraFilterCategory.values()[keyCode - GLFW.GLFW_KEY_1]);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            this.highlightedIndex = 0;
            select(currentFilters().get(0));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            List<CameraFilter> filters = currentFilters();
            this.highlightedIndex = filters.size() - 1;
            select(filters.get(this.highlightedIndex));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_UP) {
            moveSelection(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_DOWN) {
            moveSelection(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
