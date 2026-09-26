package EdDYON.guaniao.client.guide;

import EdDYON.guaniao.client.config.BirdConfigClient;
import EdDYON.guaniao.client.gui.layout.GuiLayoutLoader;
import EdDYON.guaniao.client.gui.layout.GuiLayoutRect;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.columbid.AbstractColumbidEntity;
import EdDYON.guaniao.content.bird.crow.CrowEntity;
import EdDYON.guaniao.content.bird.kiwi.KiwiEntity;
import EdDYON.guaniao.content.bird.myna.MynaEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.seagull.SeagullEntity;
import EdDYON.guaniao.content.bird.kestrel.KestrelEntity;
import EdDYON.guaniao.content.bird.cassowary.CassowaryEntity;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.Items;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import net.minecraft.client.gui.screens.Screen;
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
import org.lwjgl.glfw.GLFW;

public class BirdGuideScreen extends Screen {
    private static final ResourceLocation RECIPE_BOOK = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/recipe_book.png");
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int LINE_HEIGHT = 11;
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
            new BirdGuideEntry("seagull", List.of("intro")),
            new BirdGuideEntry("kiwi", List.of("intro")),
            new BirdGuideEntry("myna", List.of("intro")),
            new BirdGuideEntry("woodcock", List.of("intro")),
            new BirdGuideEntry("kestrel", List.of("intro")),
            new BirdGuideEntry("cassowary", List.of("intro")),
            new BirdGuideEntry("umbrella_cockatoo", List.of("intro"))
    );
    private static final PoseKind[] POSES = PoseKind.values();

    private int selectedIndex;
    private int selectedPoseIndex;
    private int textScroll;
    private final BirdGuideIndex catalogue = new BirdGuideIndex();
    private final Map<Integer, LivingEntity> thumbnails = new HashMap<>();
    private final List<AbstractWidget> mainWidgets = new ArrayList<>();
    private final List<AbstractWidget> catalogueWidgets = new ArrayList<>();
    private final List<SpeciesButton> speciesButtons = new ArrayList<>();
    private final List<PoseButton> poseButtons = new ArrayList<>();
    private final List<NoteLine> noteLines = new ArrayList<>();
    private BirdGuideLayout layout;
    private EditBox searchBox;
    private Button previousPage;
    private Button nextPage;
    private Button catalogueBack;
    private String searchText = "";
    private boolean catalogueOpen;
    private boolean draggingNotes;
    private int noteHeight;
    private LivingEntity previewEntity;
    private final RandomSource previewRandom = RandomSource.create();
    private float previewDragX = 16.0F;
    private float previewDragY = -8.0F;
    private float previewZoom = 1.0F;
    private boolean draggingPreview;
    private boolean manualPoseLocked;
    private int manualLookTicks;
    private int motionTicks;
    private int motionDuration = 90;
    private PreviewMotion previewMotion = PreviewMotion.PERCH;
    private GuidePreviewAnimation previewAnimation = GuidePreviewAnimation.IDLE;
    private float birdX;
    private float birdY;

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
        this.mainWidgets.clear();
        this.catalogueWidgets.clear();
        this.speciesButtons.clear();
        this.poseButtons.clear();
        this.draggingPreview = false;
        this.draggingNotes = false;
        this.layout = new BirdGuideLayout(this.width, this.height, GuiLayoutLoader.loadBirdGuideLayout());
        this.catalogue.setPageSize(this.layout.pageSize());
        GuiLayoutRect search = this.layout.rect("search");
        this.searchBox = new EditBox(this.font, search.x(), search.y(), search.w(), search.h(),
                Component.translatable("gui.guaniao.bird_guide.search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setHint(Component.translatable("gui.guaniao.bird_guide.search"));
        this.searchBox.setValue(this.searchText);
        this.searchBox.setResponder(value -> {
            this.searchText = value;
            this.filterCatalogue();
        });
        this.addRenderableWidget(this.searchBox);
        this.catalogueWidgets.add(this.searchBox);
        this.previousPage = this.addButton("previous_page", Component.literal("<"),
                button -> this.moveCataloguePage(-1), true);
        this.previousPage.setTooltip(Tooltip.create(Component.translatable("gui.guaniao.bird_guide.previous_page")));
        this.nextPage = this.addButton("next_page", Component.literal(">"),
                button -> this.moveCataloguePage(1), true);
        this.nextPage.setTooltip(Tooltip.create(Component.translatable("gui.guaniao.bird_guide.next_page")));
        this.catalogueBack = this.addButton("catalogue_back", Component.literal("<"),
                button -> this.closeCatalogue(), true);
        this.catalogueBack.setTooltip(Tooltip.create(Component.translatable("gui.guaniao.bird_guide.back")));
        this.addButton("config_button", Component.translatable("gui.guaniao.bird_guide.config"),
                button -> BirdConfigClient.requestOpen(), false);
        this.addButton("close_button", Component.translatable("gui.guaniao.bird_guide.close"),
                button -> this.onClose(), false);
        Button index = new Button(this.layout.rect("catalogue_toggle").x(), this.layout.rect("catalogue_toggle").y(),
                20, 20, Component.translatable("gui.guaniao.bird_guide.species"), button -> {
                    this.catalogueOpen = true;
                    this.updateVisibility();
                    this.setFocused(this.searchBox);
                    this.searchBox.setFocused(true);
                }, message -> message.get()) {
            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                Component label = this.getMessage();
                this.setMessage(Component.empty());
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
                this.setMessage(label);
                graphics.renderItem(Items.BOOK.getDefaultInstance(), this.getX() + 2, this.getY() + 2);
            }
        };
        index.setTooltip(Tooltip.create(Component.translatable("gui.guaniao.bird_guide.species")));
        this.addRenderableWidget(index);
        this.mainWidgets.add(index);
        for (int i = 0; i < POSES.length; i++) {
            PoseButton button = new PoseButton(i, this.layout.pose(i));
            this.addRenderableWidget(button);
            this.poseButtons.add(button);
            this.mainWidgets.add(button);
        }
        this.filterCatalogue();
        this.rebuildNotes();
        this.updatePoseButtons();
        this.lockPreviewModelPosition();
    }

    private Button addButton(String rectId, Component label, Button.OnPress action, boolean inCatalogue) {
        GuiLayoutRect rect = this.layout.rect(rectId);
        Button button = Button.builder(label, action).bounds(rect.x(), rect.y(), rect.w(), rect.h()).build();
        this.addRenderableWidget(button);
        (inCatalogue ? this.catalogueWidgets : this.mainWidgets).add(button);
        return button;
    }

    private boolean showCatalogue() {
        return this.layout.wide || this.catalogueOpen;
    }

    private boolean showDetails() {
        return this.layout.wide || !this.catalogueOpen;
    }

    private void closeCatalogue() {
        this.catalogueOpen = false;
        this.searchBox.setFocused(false);
        this.setFocused(null);
        this.updateVisibility();
    }

    private void updateVisibility() {
        for (AbstractWidget widget : this.mainWidgets) widget.visible = this.showDetails();
        for (AbstractWidget widget : this.catalogueWidgets) widget.visible = this.showCatalogue();
        for (SpeciesButton button : this.speciesButtons) button.visible = this.showCatalogue();
        this.catalogueBack.visible = this.showCatalogue() && !this.layout.wide;
        this.previousPage.active = this.catalogue.page() > 0;
        this.nextPage.active = this.catalogue.page() + 1 < this.catalogue.pageCount();
        if (!this.showCatalogue()) this.searchBox.setFocused(false);
    }

    private void filterCatalogue() {
        this.catalogue.search(ENTRIES.size(), this.searchText, index -> {
            BirdGuideEntry entry = ENTRIES.get(index);
            StringBuilder text = new StringBuilder(entry.id().replace('_', ' '))
                    .append(' ').append(entry.title().getString()).append(' ').append(entry.subtitle().getString());
            for (String tag : this.tagsFor(entry)) {
                text.append(' ').append(Component.translatable("gui.guaniao.bird_guide.tag." + tag).getString());
            }
            return text.toString();
        });
        this.rebuildSpeciesButtons();
    }

    private void moveCataloguePage(int direction) {
        this.catalogue.movePage(direction);
        this.rebuildSpeciesButtons();
    }

    private void rebuildSpeciesButtons() {
        for (SpeciesButton button : this.speciesButtons) this.removeWidget(button);
        this.speciesButtons.clear();
        List<Integer> visible = this.catalogue.visible();
        for (int slot = 0; slot < visible.size(); slot++) {
            SpeciesButton button = new SpeciesButton(visible.get(slot), this.layout.slot(slot));
            this.addRenderableWidget(button);
            this.speciesButtons.add(button);
        }
        this.updateVisibility();
    }

    private void selectEntry(int index) {
        if (this.selectedIndex != index) {
            this.previewEntity = null;
            this.previewZoom = 1.0F;
        }
        this.selectedIndex = index;
        this.textScroll = 0;
        this.selectedPoseIndex = 0;
        this.manualPoseLocked = false;
        this.draggingPreview = false;
        this.draggingNotes = false;
        this.resetPreviewMotion();
        this.rebuildNotes();
        this.updatePoseButtons();
        this.closeCatalogue();
    }

    private void updatePoseButtons() {
        for (int i = 0; i < this.poseButtons.size(); i++) {
            PoseButton button = this.poseButtons.get(i);
            boolean flightless = POSES[i] == PoseKind.FLY && this.isKiwiSelected();
            button.active = !flightless;
            button.setMessage(Component.translatable(flightless
                    ? "gui.guaniao.bird_guide.pose.flightless" : POSES[i].translationKey()));
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Catalogue birds still need an animation clock to finish folding their wings.
        for (LivingEntity thumbnail : this.thumbnails.values()) ++thumbnail.tickCount;
        this.tickPreviewMotion();
        if (this.previewEntity != null) {
            ++this.previewEntity.tickCount;
            this.applyPreviewAnimation(this.previewEntity);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderTransparentBackground(graphics);
        if (this.showCatalogue()) this.renderCatalogue(graphics);
        if (this.showDetails()) {
            this.renderDetails(graphics);
            this.renderPreview(graphics);
        }
        for (var renderable : this.renderables) renderable.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderCatalogue(GuiGraphics graphics) {
        GuiLayoutRect panel = this.layout.rect("catalogue_panel");
        // The real vanilla recipe-book surface, stretched only through its empty centre.
        EdDYON.guaniao.client.gui.BirdGuiRendering.blitNineSliced(graphics, RECIPE_BOOK, panel.x(), panel.y(), panel.w(), panel.h(), 5, 147, 166, 1, 1);
        GuiLayoutRect header = this.layout.rect("species_header");
        graphics.drawString(this.font, Component.translatable("gui.guaniao.bird_guide.species"),
                header.x(), header.y(), 0xFFFFFFFF, false);
        GuiLayoutRect grid = this.layout.rect("species_list");
        if (this.catalogue.count() == 0) {
            graphics.drawWordWrap(this.font, Component.translatable("gui.guaniao.bird_guide.no_results"),
                    grid.x() + 4, grid.y() + 8, grid.w() - 8, 0xFFAAAAAA);
        }
        GuiLayoutRect previous = this.layout.rect("previous_page");
        Component count = this.catalogue.pageCount() > 1
                ? Component.literal((this.catalogue.page() + 1) + " / " + this.catalogue.pageCount())
                : Component.translatable("gui.guaniao.bird_guide.species_count", this.catalogue.count());
        graphics.drawCenteredString(this.font, count, panel.centerX(), previous.y() + 6, 0xFFFFFFFF);
    }

    private void renderDetails(GuiGraphics graphics) {
        GuiLayoutRect panel = this.layout.rect("main_panel");
        drawContainer(graphics, panel);
        GuiLayoutRect header = this.layout.rect("header");
        graphics.drawString(this.font, this.title, header.x(), header.y(), TEXT_COLOR, false);
        GuiLayoutRect title = this.layout.rect("detail_header");
        graphics.drawString(this.font, this.selectedEntry(this.selectedIndex).title(), title.x(), title.y(), TEXT_COLOR, false);
        GuiLayoutRect notes = this.layout.rect("info_card");
        this.textScroll = Mth.clamp(this.textScroll, 0, this.maxTextScroll());
        graphics.enableScissor(notes.x(), notes.y(), notes.right() - 7, notes.bottom());
        for (NoteLine line : this.noteLines) {
            int y = notes.y() + line.y() - this.textScroll;
            if (y + LINE_HEIGHT > notes.y() && y < notes.bottom()) {
                graphics.drawString(this.font, line.text(), notes.x(), y, line.color(), false);
            }
        }
        graphics.disableScissor();
        if (this.maxTextScroll() > 0) {
            int barX = notes.right() - 5;
            graphics.fill(barX, notes.y(), barX + 4, notes.bottom(), 0xFF8B8B8B);
            int thumb = this.scrollThumbHeight();
            int top = notes.y() + (notes.h() - thumb) * this.textScroll / this.maxTextScroll();
            graphics.fill(barX, top, barX + 4, top + thumb, 0xFF555555);
            graphics.fill(barX, top, barX + 3, top + thumb - 1, 0xFFFFFFFF);
            graphics.fill(barX + 1, top + 1, barX + 3, top + thumb - 1, 0xFFC6C6C6);
        }
    }

    private void rebuildNotes() {
        this.noteLines.clear();
        this.noteHeight = 0;
        BirdGuideEntry entry = this.selectedEntry(this.selectedIndex);
        MutableComponent tags = Component.empty();
        for (String tag : this.tagsFor(entry)) {
            if (!tags.getSiblings().isEmpty()) tags.append(" · ");
            tags.append(Component.translatable("gui.guaniao.bird_guide.tag." + tag));
        }
        this.addNote(tags, 0xFF555555, 8);
        for (String fact : List.of("activity", "diet", "environment", "behavior")) {
            String key = "gui.guaniao.bird_guide.entry." + entry.id() + ".info." + fact;
            if (I18n.exists(key)) {
                this.addNote(Component.translatable("gui.guaniao.bird_guide.fact",
                        Component.translatable("gui.guaniao.bird_guide.info." + fact), Component.translatable(key)), TEXT_COLOR, 3);
            }
        }
        this.noteHeight += 5;
        this.addNote(Component.translatable("gui.guaniao.bird_guide.entry." + entry.id() + ".intro.title"), 0xFF202020, 5);
        for (String section : entry.sections()) {
            this.addNote(Component.translatable("gui.guaniao.bird_guide.entry." + entry.id() + "." + section + ".body"), TEXT_COLOR, 6);
        }
        this.textScroll = Math.min(this.textScroll, this.maxTextScroll());
    }

    private void addNote(Component component, int color, int gap) {
        int width = this.layout.rect("info_card").w() - 10;
        for (FormattedCharSequence line : this.font.split((FormattedText)component, width)) {
            this.noteLines.add(new NoteLine(line, this.noteHeight, color));
            this.noteHeight += LINE_HEIGHT;
        }
        this.noteHeight += gap;
    }

    private int maxTextScroll() {
        return Math.max(0, this.noteHeight - this.layout.rect("info_card").h());
    }

    private int scrollThumbHeight() {
        int height = this.layout.rect("info_card").h();
        return Math.max(12, height * height / Math.max(height, this.noteHeight));
    }

    private void scrollNotesTo(double mouseY) {
        GuiLayoutRect notes = this.layout.rect("info_card");
        int thumb = this.scrollThumbHeight();
        double progress = (mouseY - notes.y() - thumb / 2.0D) / Math.max(1, notes.h() - thumb);
        this.textScroll = Mth.clamp((int)Math.round(progress * this.maxTextScroll()), 0, this.maxTextScroll());
    }

    private void renderPreview(GuiGraphics graphics) {
        GuiLayoutRect preview = this.layout.rect("preview_box");
        graphics.fill(preview.x() - 1, preview.y() - 1, preview.right() + 1, preview.bottom() + 1, 0xFF8B8B8B);
        graphics.fill(preview.x(), preview.y(), preview.right(), preview.bottom(), 0xFFB0B0B0);
        LivingEntity entity = this.previewEntity();
        if (entity != null) {
            graphics.enableScissor(preview.x(), preview.y(), preview.right(), preview.bottom());
            EdDYON.guaniao.client.gui.BirdGuiRendering.renderEntity(graphics, Math.round(this.birdX), Math.round(this.birdY),
                    this.previewRenderScale(preview), this.previewDragX, this.previewDragY, entity);
            graphics.disableScissor();
        }
        Component hint = Component.translatable("gui.guaniao.bird_guide.drag_hint");
        graphics.drawString(this.font, hint, preview.centerX() - this.font.width(hint) / 2,
                preview.bottom() + 3, TEXT_COLOR, false);
    }

    private static void drawContainer(GuiGraphics graphics, GuiLayoutRect r) {
        // Vanilla container's stepped corners and flat 1-pixel bevel, without an HD atlas.
        graphics.fill(r.x() + 2, r.y(), r.right() - 2, r.bottom(), 0xFF000000);
        graphics.fill(r.x(), r.y() + 2, r.right(), r.bottom() - 2, 0xFF000000);
        graphics.fill(r.x() + 1, r.y() + 2, r.right() - 1, r.bottom() - 2, 0xFF555555);
        graphics.fill(r.x() + 2, r.y() + 1, r.right() - 2, r.bottom() - 1, 0xFF555555);
        graphics.fill(r.x() + 2, r.y() + 2, r.right() - 3, r.bottom() - 3, 0xFFFFFFFF);
        graphics.fill(r.x() + 4, r.y() + 4, r.right() - 4, r.bottom() - 4, 0xFFC6C6C6);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            if (!this.showCatalogue() && this.getFocused() instanceof SpeciesButton) this.setFocused(null);
            return true;
        }
        if (button != 0 || !this.showDetails()) return false;
        if (this.layout.rect("preview_box").contains(mouseX, mouseY)) {
            this.draggingPreview = true;
            return true;
        }
        GuiLayoutRect notes = this.layout.rect("info_card");
        if (notes.contains(mouseX, mouseY) && mouseX >= notes.right() - 7 && this.maxTextScroll() > 0) {
            this.draggingNotes = true;
            this.scrollNotesTo(mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingNotes) {
            this.scrollNotesTo(mouseY);
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
        if (button == 0 && (this.draggingPreview || this.draggingNotes)) {
            this.draggingPreview = false;
            this.draggingNotes = false;
            this.manualLookTicks = 50;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double delta) {
        if (this.showDetails() && this.layout.rect("preview_box").contains(mouseX, mouseY)) {
            // Framing is controlled by the player, not reset whenever a pose changes.
            this.previewZoom = Mth.clamp(this.previewZoom + (float)Math.signum(delta) * 0.1F, 0.3F, 1.5F);
            return true;
        }
        if (this.showCatalogue() && this.layout.rect("catalogue_panel").contains(mouseX, mouseY)) {
            this.moveCataloguePage(delta > 0 ? -1 : 1);
            return true;
        }
        if (this.showDetails() && this.layout.rect("info_card").contains(mouseX, mouseY)) {
            this.textScroll = Mth.clamp(this.textScroll - (int)Math.signum(delta) * 22, 0, this.maxTextScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalDelta, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !this.layout.wide && this.catalogueOpen) {
            this.closeCatalogue();
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && this.searchBox.isFocused() && !this.catalogue.visible().isEmpty()) {
            this.selectEntry(this.catalogue.visible().get(0));
            return true;
        }
        if (this.showCatalogue() && !this.searchBox.isFocused()
                && (keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN)) {
            this.moveCataloguePage(keyCode == GLFW.GLFW_KEY_PAGE_UP ? -1 : 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private final class PoseButton extends Button {
        private final int poseIndex;

        private PoseButton(int index, GuiLayoutRect rect) {
            super(rect.x(), rect.y(), rect.w(), rect.h(), Component.translatable(POSES[index].translationKey()),
                    button -> BirdGuideScreen.this.selectPose(index), DEFAULT_NARRATION);
            this.poseIndex = index;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            if (BirdGuideScreen.this.manualPoseLocked && BirdGuideScreen.this.selectedPoseIndex == this.poseIndex) {
                graphics.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0xFFFFFFFF);
            }
        }
    }

    private final class SpeciesButton extends Button {
        private final int entryIndex;

        private SpeciesButton(int index, GuiLayoutRect rect) {
            super(rect.x(), rect.y(), rect.w(), rect.h(), ENTRIES.get(index).title(),
                    button -> BirdGuideScreen.this.selectEntry(index), DEFAULT_NARRATION);
            this.entryIndex = index;
            this.setTooltip(Tooltip.create(ENTRIES.get(index).title().copy().append("\n").append(ENTRIES.get(index).subtitle())));
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            GuiLayoutRect slot = new GuiLayoutRect(this.getX(), this.getY(), this.getWidth(), this.getHeight());
            EdDYON.guaniao.client.gui.BirdGuiRendering.blitNineSliced(graphics, ResourceLocation.withDefaultNamespace("textures/gui/sprites/recipe_book/slot_craftable.png"), slot.x(), slot.y(), slot.w(), slot.h(), 2, 25, 25, 0, 0, 25, 25);
            LivingEntity entity = BirdGuideScreen.this.thumbnail(this.entryIndex);
            if (entity != null) {
                GuiLayoutRect inside = slot.inset(3);
                graphics.enableScissor(inside.x(), inside.y(), inside.right(), inside.bottom());
                EdDYON.guaniao.client.gui.BirdGuiRendering.renderEntity(graphics, inside.centerX(), inside.bottom() - 2,
                        modelScale(entity, inside), -22.0F, -5.0F, entity);
                graphics.disableScissor();
            }
            if (this.isHoveredOrFocused() || this.entryIndex == BirdGuideScreen.this.selectedIndex) {
                graphics.renderOutline(slot.x(), slot.y(), slot.w(), slot.h(), 0xFFFFFFFF);
            }
        }
    }

    private LivingEntity thumbnail(int index) {
        if (this.minecraft == null || this.minecraft.level == null) return null;
        return this.thumbnails.computeIfAbsent(index, key -> {
            EntityType<? extends LivingEntity> type = ENTRIES.get(key).entityType();
            LivingEntity entity = type == null ? null : type.create(this.minecraft.level);
            if (entity != null) {
                if (entity instanceof Mob mob) mob.setNoAi(true);
                if (entity instanceof ScalableBirdModel bird) bird.setIndividualModelScale(1.0F);
                entity.setNoGravity(true);
                entity.setSilent(true);
                entity.setOnGround(true);
                this.applyPreviewAnimation(entity, GuidePreviewAnimation.IDLE);
            }
            return entity;
        });
    }

    private static int modelScale(LivingEntity entity, GuiLayoutRect box) {
        float height = entity instanceof ScalableBirdModel bird
                ? bird.modelScaleProfile().targetHeightBlocks() * bird.getIndividualModelScale()
                : Math.max(0.2F, entity.getBbHeight());
        float pixels = Math.min(box.h() * 0.72F, box.w() * 0.78F);
        return BirdModelScale.fitPreviewScale(Math.max(1, Math.round(pixels / height)));
    }

    private int previewRenderScale(GuiLayoutRect preview) {
        int scale = this.previewEntity == null ? 34 : modelScale(this.previewEntity, preview);
        return Math.max(1, Math.round(scale * this.previewZoom));
    }

    private float defaultStageX(GuiLayoutRect preview, int scale) { return preview.centerX(); }
    private float defaultStageY(GuiLayoutRect preview, int scale) { return preview.bottom() - 9; }
    private GuiLayoutRect layoutRect(String id) { return this.layout.rect(id); }
    private boolean isKiwiSelected() { return "kiwi".equals(this.selectedEntry(this.selectedIndex).id()); }
    private boolean isNightHeronSelected() { return "night_heron".equals(this.selectedEntry(this.selectedIndex).id()); }
    private BirdGuideEntry selectedEntry(int index) { return ENTRIES.get(Mth.clamp(index, 0, ENTRIES.size() - 1)); }
    private record NoteLine(FormattedCharSequence text, int y, int color) {}

    private LivingEntity previewEntity() {
        if (this.previewEntity == null && this.minecraft != null && this.minecraft.level != null) {
            EntityType<? extends LivingEntity> type = this.selectedEntry(this.selectedIndex).entityType();
            this.previewEntity = type == null ? null : type.create(this.minecraft.level);
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
        if (this.isKiwiSelected()) {
            if (roll < 0.55F) {
                this.planPerch();
            } else {
                this.planWalk();
            }
        } else if (this.isNightHeronSelected()) {
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
        if (this.isKiwiSelected() && POSES[Mth.clamp(poseIndex, 0, POSES.length - 1)] == PoseKind.FLY) {
            return;
        }
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
        int scale = this.previewRenderScale(preview);
        this.birdX = this.defaultStageX(preview, scale);
        this.birdY = this.defaultStageY(preview, scale);
    }

    private void applyPreviewAnimation(LivingEntity entity) {
        this.applyPreviewAnimation(entity, this.previewAnimation);
    }

    private void applyPreviewAnimation(LivingEntity entity, GuidePreviewAnimation animation) {
        if (entity instanceof NightHeronEntity nightHeron) {
            nightHeron.setGuidePreviewAnimation(this.toNightHeronPreviewAnimation(animation));
        } else if (entity instanceof MynaEntity myna) {
            myna.setGuidePreviewAnimation(switch (animation) {
                case WALK, RUN -> MynaEntity.GuidePreviewAnimation.WALK;
                case FLY_FLAP, GLIDE -> MynaEntity.GuidePreviewAnimation.FLY;
                case LOOK_2, SCRATCH -> MynaEntity.GuidePreviewAnimation.IDLE_2;
                case LOOK_1, LOOK_3, LOOK_5 -> MynaEntity.GuidePreviewAnimation.IDLE_1;
                default -> MynaEntity.GuidePreviewAnimation.IDLE;
            });
        } else if (entity instanceof KiwiEntity kiwi) {
            kiwi.setGuidePreviewAnimation(switch (animation) {
                case WALK, RUN -> KiwiEntity.GuidePreviewAnimation.WALK;
                case LOOK_2, SCRATCH -> KiwiEntity.GuidePreviewAnimation.FORAGE;
                case LOOK_1, LOOK_3, LOOK_5 -> KiwiEntity.GuidePreviewAnimation.ALERT;
                default -> KiwiEntity.GuidePreviewAnimation.IDLE;
            });
        } else if (entity instanceof SparrowEntity sparrow) {
            sparrow.setGuidePreviewAnimation(this.toSparrowPreviewAnimation(animation));
        } else if (entity instanceof BudgerigarEntity budgerigar) {
            // Subclasses with a richer crest (the umbrella cockatoo) override this method
            // and re-map the budgerigar poses onto their own overlay animations.
            budgerigar.setGuidePreviewAnimation(this.toBudgerigarPreviewAnimation(animation));
        } else if (entity instanceof AbstractColumbidEntity columbid) {
            columbid.setGuidePreviewAnimation(this.toColumbidPreviewAnimation(animation));
        } else if (entity instanceof CrowEntity crow) {
            crow.setGuidePreviewAnimation(this.toCrowPreviewAnimation(animation));
        } else if (entity instanceof SeagullEntity seagull) {
            seagull.setGuidePreviewAnimation(this.toSeagullPreviewAnimation(animation));
        } else if (entity instanceof KestrelEntity kestrel) {
            kestrel.setGuidePreviewAnimation(switch (animation) {
                case WALK, RUN -> KestrelEntity.GuidePreviewAnimation.WALK;
                case FLY_FLAP -> KestrelEntity.GuidePreviewAnimation.HOVER;
                case GLIDE -> KestrelEntity.GuidePreviewAnimation.FLY;
                default -> KestrelEntity.GuidePreviewAnimation.IDLE;
            });
        } else if (entity instanceof CassowaryEntity cassowary) {
            cassowary.setGuidePreviewAnimation(switch (animation) {
                case WALK -> CassowaryEntity.GuidePreviewAnimation.WALK;
                case RUN -> CassowaryEntity.GuidePreviewAnimation.SPRINT;
                case LOOK_1, LOOK_3, LOOK_5 -> CassowaryEntity.GuidePreviewAnimation.LOOK;
                case LOOK_2, SCRATCH -> CassowaryEntity.GuidePreviewAnimation.ALERT;
                case FLY_FLAP -> CassowaryEntity.GuidePreviewAnimation.WARNING;
                case GLIDE -> CassowaryEntity.GuidePreviewAnimation.REST;
                default -> CassowaryEntity.GuidePreviewAnimation.IDLE;
            });
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
            case "kiwi" -> List.of("nocturnal", "forest", "insect_eater", "alert", "solitary");
            case "myna" -> List.of("diurnal", "village", "omnivore", "social", "tameable");
            case "woodcock" -> List.of("nocturnal", "forest", "insect_eater", "alert", "solitary");
            case "kestrel" -> List.of("diurnal", "farmland", "predator", "alert", "solitary", "tameable");
            case "cassowary" -> List.of("diurnal", "forest", "omnivore", "alert", "solitary");
            case "umbrella_cockatoo" -> List.of("diurnal", "jungle", "fruit_eater", "social", "tameable", "mimic");
            default -> List.of();
        };
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
                case "kiwi" -> GuaniaoEntityTypes.KIWI.get();
                case "myna" -> GuaniaoEntityTypes.MYNA.get();
                case "woodcock" -> GuaniaoEntityTypes.WOODCOCK.get();
                case "kestrel" -> GuaniaoEntityTypes.KESTREL.get();
                case "cassowary" -> GuaniaoEntityTypes.CASSOWARY.get();
                case "umbrella_cockatoo" -> GuaniaoEntityTypes.UMBRELLA_COCKATOO.get();
                // Explicit so a mistyped entry id surfaces as null (skipped preview)
                // instead of silently rendering a night heron.
                case "night_heron" -> GuaniaoEntityTypes.NIGHT_HERON.get();
                default -> null;
            };
        }
    }

}
