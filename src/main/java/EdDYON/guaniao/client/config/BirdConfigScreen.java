package EdDYON.guaniao.client.config;

import EdDYON.guaniao.config.BirdConfigData;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdConfigScope;
import EdDYON.guaniao.config.BirdGlobalConfig;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.config.BirdSpeciesConfig;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.columbid.AbstractColumbidEntity;
import EdDYON.guaniao.content.bird.crow.CrowEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.myna.MynaEntity;
import EdDYON.guaniao.content.bird.seagull.SeagullEntity;
import EdDYON.guaniao.content.bird.kestrel.KestrelEntity;
import EdDYON.guaniao.content.bird.cassowary.CassowaryEntity;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.network.GuaniaoNetwork;
import EdDYON.guaniao.network.SaveBirdConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.ResourceLocation;
import EdDYON.guaniao.client.gui.layout.GuiLayoutRect;
import EdDYON.guaniao.content.bird.kiwi.KiwiEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class BirdConfigScreen extends Screen {
    private static final ResourceLocation RECIPE_BOOK = new ResourceLocation("minecraft", "textures/gui/recipe_book.png");
    private static final int TEXT = 0xFF404040;
    private static final int MUTED = 0xFF606060;
    private BirdConfigData data;
    private final List<NumericInput> numericInputs = new ArrayList<>();
    private final List<SettingSpec> visibleSettings = new ArrayList<>();
    private BirdConfigLayout layout;
    private boolean speciesMode;
    private BirdConfigCategory category = BirdConfigCategory.GENERAL;
    private int selectedIndex = 1;
    private int listScroll;
    private int fieldScroll;
    private int settingCount;
    private Component status = Component.empty();
    private LivingEntity previewEntity;
    private float previewLookX = 18.0F;
    private float previewLookY = -8.0F;
    private boolean draggingPreview;
    private boolean draggingFields;
    private double fieldDragOffset;

    public BirdConfigScreen(BirdConfigData data) {
        super(Component.translatable("gui.guaniao.bird_config.title"));
        this.data = data.copy();
    }

    void acceptServerConfig(BirdConfigData data) {
        this.data = data.copy();
        this.status = Component.translatable("message.guaniao.bird_config.saved");
        refreshWidgets();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        this.draggingFields = false;
        this.layout = new BirdConfigLayout(this.width, this.height);
        refreshWidgets();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        // Valid edits survive GUI scale/window changes, as they do category changes.
        captureNumericValues();
        super.resize(minecraft, width, height);
    }

    private Button button(GuiLayoutRect r, Component message, Button.OnPress action) {
        return this.addRenderableWidget(Button.builder(message, action).bounds(r.x(), r.y(), r.w(), r.h()).build());
    }

    private void choice(GuiLayoutRect r, Component message, boolean selected, Button.OnPress action) {
        Button button = new Button(r.x(), r.y(), r.w(), r.h(), fit(message, r.w() - 8), action, value -> value.get()) {
            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
                if (selected) graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
            }
        };
        button.setTooltip(Tooltip.create(message));
        this.addRenderableWidget(button);
    }

    private void refreshWidgets() {
        this.clearWidgets();
        this.numericInputs.clear();
        this.visibleSettings.clear();
        choice(this.layout.mode(0), Component.translatable("gui.guaniao.bird_config.mode.categories"), !this.speciesMode,
                b -> changeMode(false));
        choice(this.layout.mode(1), Component.translatable("gui.guaniao.bird_config.mode.species"), this.speciesMode,
                b -> changeMode(true));
        int subjects = this.speciesMode ? BirdSpecies.values().length : BirdConfigCategory.values().length;
        this.listScroll = Mth.clamp(this.listScroll, 0, Math.max(0, subjects - this.layout.listRows()));
        for (int row = 0; row < this.layout.listRows() && row + this.listScroll < subjects; row++) {
            int index = row + this.listScroll;
            Component name = this.speciesMode ? Component.translatable(BirdSpecies.values()[index].translationKey())
                    : Component.translatable(BirdConfigCategory.values()[index].translationKey());
            boolean selected = this.speciesMode ? this.selectedIndex == index + 1 : this.category.ordinal() == index;
            choice(this.layout.subject(row), name, selected, b -> selectSubject(index));
        }
        button(this.layout.listArrow(false), Component.literal("<"), b -> scrollSubjects(-this.layout.listRows())).active = this.listScroll > 0;
        button(this.layout.listArrow(true), Component.literal(">"), b -> scrollSubjects(this.layout.listRows())).active = this.listScroll + this.layout.listRows() < subjects;
        if (this.speciesMode) {
            for (int i = 0; i < BirdConfigCategory.SPECIES.size(); i++) {
                BirdConfigCategory target = BirdConfigCategory.SPECIES.get(i);
                choice(this.layout.tab(i), Component.translatable(target.translationKey() + ".short"), this.category == target,
                        b -> selectCategory(target));
            }
        }
        List<SettingSpec> settings = settingsForSelection();
        this.settingCount = settings.size();
        int rows = this.layout.fieldRows(this.speciesMode);
        this.fieldScroll = Mth.clamp(this.fieldScroll, 0, Math.max(0, settings.size() - rows));
        for (int row = 0; row < rows && this.fieldScroll + row < settings.size(); row++) {
            SettingSpec setting = settings.get(this.fieldScroll + row);
            this.visibleSettings.add(setting);
            GuiLayoutRect r = this.layout.control(row, this.speciesMode);
            if (setting.storageScopeRow()) {
                Button scope = button(r, scopeLabel(), this::toggleStorageScope);
                scope.active = this.data.worldScopeAllowed;
            } else if (setting.toggle()) {
                button(r, toggleLabel(setting.booleanGetter().getAsBoolean()), b -> {
                    boolean next = !setting.booleanGetter().getAsBoolean();
                    setting.booleanSetter().accept(next);
                    b.setMessage(toggleLabel(next));
                    this.status = Component.translatable("gui.guaniao.bird_config.unsaved");
                });
            } else {
                EditBox box = new EditBox(this.font, r.x(), r.y(), r.w(), r.h(), setting.label());
                box.setMaxLength(16);
                box.setFilter(BirdConfigScreen::isNumericText);
                box.setValue(format(setting.numberGetter().getAsDouble(), setting.integer()));
                box.setResponder(value -> this.status = Component.translatable("gui.guaniao.bird_config.unsaved"));
                this.numericInputs.add(new NumericInput(setting, box));
                this.addRenderableWidget(box);
            }
        }
        button(this.layout.footer(0), Component.translatable("gui.guaniao.bird_config.save"), b -> save());
        button(this.layout.footer(1), Component.translatable("gui.guaniao.bird_config.reset_all"), b -> resetDefaults())
                .setTooltip(Tooltip.create(Component.translatable("gui.guaniao.bird_config.reset_all.description")));
        button(this.layout.footer(2), Component.translatable("gui.guaniao.bird_config.close"), b -> onClose());
    }

    private List<SettingSpec> settingsForSelection() {
        return allSettingsForSelection().stream().filter(setting -> this.category.contains(setting.key())).toList();
    }

    private void changeMode(boolean species) {
        if (this.speciesMode == species || !captureNumericValues()) return;
        this.speciesMode = species;
        this.category = species ? BirdConfigCategory.ECOLOGY : BirdConfigCategory.GENERAL;
        this.listScroll = 0;
        this.fieldScroll = 0;
        this.previewEntity = null;
        refreshWidgets();
    }

    private void selectSubject(int index) {
        if (!captureNumericValues()) return;
        if (this.speciesMode) {
            this.selectedIndex = index + 1;
            this.previewEntity = null;
        } else {
            this.category = BirdConfigCategory.values()[index];
        }
        this.fieldScroll = 0;
        refreshWidgets();
    }

    private void selectCategory(BirdConfigCategory category) {
        if (!captureNumericValues()) return;
        this.category = category;
        this.fieldScroll = 0;
        refreshWidgets();
    }

    private void scrollSubjects(int amount) {
        if (!captureNumericValues()) return;
        this.listScroll += amount;
        refreshWidgets();
    }

    private void scrollFields(int amount) {
        scrollFieldsTo(this.fieldScroll + amount);
    }

    private void scrollFieldsTo(int offset) {
        int next = Mth.clamp(offset, 0, this.layout.maxFieldScroll(this.speciesMode, this.settingCount));
        if (next == this.fieldScroll) return;
        if (!captureNumericValues()) return;
        this.fieldScroll = next;
        refreshWidgets();
    }

    private void dragFieldsTo(double mouseY) {
        scrollFieldsTo(this.layout.fieldScrollAt(this.speciesMode, this.settingCount, mouseY - this.fieldDragOffset));
    }

    private void save() {
        if (!captureNumericValues()) {
            return;
        }
        GuaniaoNetwork.sendToServer(new SaveBirdConfigPacket(this.data));
        this.status = Component.translatable("gui.guaniao.bird_config.saving");
    }

    private void resetDefaults() {
        BirdConfigScope scope = BirdConfigScope.sanitize(this.data.storageScope);
        boolean worldScopeAllowed = this.data.worldScopeAllowed;
        this.data = BirdConfigManager.defaultConfig();
        this.data.storageScope = scope;
        this.data.worldScopeAllowed = worldScopeAllowed;
        this.selectedIndex = Mth.clamp(this.selectedIndex, 0, BirdSpecies.values().length);
        this.fieldScroll = 0;
        this.previewEntity = null;
        this.status = Component.translatable("gui.guaniao.bird_config.reset_notice");
        refreshWidgets();
    }

    private boolean captureNumericValues() {
        for (NumericInput input : this.numericInputs) {
            String raw = input.box().getValue().trim();
            if (raw.isEmpty()) {
                this.status = Component.translatable("gui.guaniao.bird_config.invalid_number");
                input.box().setFocused(true);
                return false;
            }
            try {
                double value = Double.parseDouble(raw);
                if (!Double.isFinite(value) || value < input.setting().min() || value > input.setting().max()) {
                    throw new NumberFormatException();
                }
                if (input.setting().integer()) {
                    value = Math.rint(value);
                }
                input.setting().numberSetter().accept(value);
            } catch (NumberFormatException exception) {
                this.status = Component.translatable(
                        "gui.guaniao.bird_config.number_range",
                        format(input.setting().min(), input.setting().integer()),
                        format(input.setting().max(), input.setting().integer())
                );
                input.box().setFocused(true);
                return false;
            }
        }

        BirdSpecies species = selectedSpecies();
        if (species != null) {
            BirdSpeciesConfig bird = this.data.birds.get(species.id());
            if (bird.minGroup > bird.maxGroup) {
                this.status = Component.translatable("gui.guaniao.bird_config.invalid_group");
                return false;
            }
        }
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        GuiLayoutRect left = this.layout.left;
        GuiLayoutRect right = this.layout.right;
        graphics.blitNineSliced(RECIPE_BOOK, left.x(), left.y(), left.w(), left.h(), 5, 147, 166, 1, 1);
        drawContainer(graphics, right);
        graphics.drawString(this.font, fit(this.title, left.w() - 14), left.x() + 7, left.y() + 10, 0xFFFFFFFF, false);
        Component heading = this.speciesMode ? Component.translatable(selectedSpecies().translationKey())
                : Component.translatable(this.category.translationKey());
        graphics.drawString(this.font, heading, right.x() + 8, right.y() + 10, TEXT, false);
        if (!this.speciesMode) graphics.drawString(this.font,
                fit(Component.translatable("gui.guaniao.bird_config.browse_hint"), right.w() - 16), right.x() + 8, right.y() + 25, MUTED, false);
        for (int row = 0; row < this.visibleSettings.size(); row++) {
            GuiLayoutRect r = this.layout.row(row, this.speciesMode);
            GuiLayoutRect control = this.layout.control(row, this.speciesMode);
            graphics.fill(r.x(), r.y(), r.right(), r.bottom(), r.contains(mouseX, mouseY) ? 0xFFB8B8B8 : 0xFFBEBEBE);
            var lines = this.font.split(this.visibleSettings.get(row).label(), control.x() - r.x() - 9);
            int lineY = r.y() + (r.h() - Math.min(2, lines.size()) * 9) / 2;
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                graphics.drawString(this.font, lines.get(i), r.x() + 4, lineY + i * 9, TEXT, false);
            }
        }
        int subjects = this.speciesMode ? BirdSpecies.values().length : BirdConfigCategory.values().length;
        int listRows = this.layout.listRows();
        int pages = (subjects + listRows - 1) / listRows;
        int page = Math.min(pages, (this.listScroll + listRows * 2 - 1) / listRows);
        drawCentered(graphics, Component.literal(page + "/" + pages),
                left.centerX(), left.bottom() - 21, 0xFFFFFFFF);
        renderFieldScrollbar(graphics, mouseX, mouseY);
        GuiLayoutRect statusBox = this.layout.status();
        graphics.drawString(this.font, fit(this.status, statusBox.w()), statusBox.x(), statusBox.y(), MUTED, false);
        renderPreview(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        for (int row = 0; row < this.visibleSettings.size(); row++) {
            if (this.layout.row(row, this.speciesMode).contains(mouseX, mouseY)) {
                SettingSpec setting = this.visibleSettings.get(row);
                Component text = setting.label().copy().append("\n").append(setting.description());
                if (!setting.toggle() && !setting.storageScopeRow()) text = text.copy().append("\n").append(Component.translatable(
                        "gui.guaniao.bird_config.number_range", format(setting.min(), setting.integer()), format(setting.max(), setting.integer())));
                graphics.renderTooltip(this.font, this.font.split(text, 220), mouseX, mouseY);
            }
        }
        if (statusBox.contains(mouseX, mouseY) && !this.status.getString().isBlank()) {
            graphics.renderTooltip(this.font, this.font.split(this.status, 220), mouseX, mouseY);
        }
    }

    private void drawCentered(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.drawString(this.font, text, x - this.font.width(text) / 2, y, color, false);
    }

    private void renderFieldScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiLayoutRect track = this.layout.fieldScrollbar(this.speciesMode);
        GuiLayoutRect thumb = this.layout.fieldThumb(this.speciesMode, this.settingCount, this.fieldScroll);
        graphics.fill(track.x(), track.y(), track.right(), track.bottom(), 0xFF555555);
        boolean enabled = this.layout.maxFieldScroll(this.speciesMode, this.settingCount) > 0;
        boolean hovered = enabled && (this.draggingFields || thumb.contains(mouseX, mouseY));
        graphics.fill(thumb.x(), thumb.y(), thumb.right(), thumb.bottom(), enabled ? 0xFF373737 : 0xFF777777);
        graphics.fill(thumb.x(), thumb.y(), thumb.right() - 1, thumb.bottom() - 1, enabled ? 0xFFFFFFFF : 0xFFAAAAAA);
        graphics.fill(thumb.x() + 1, thumb.y() + 1, thumb.right() - 1, thumb.bottom() - 1,
                hovered ? 0xFFDDDDDD : enabled ? 0xFFC6C6C6 : 0xFF999999);
    }

    private static void drawContainer(GuiGraphics graphics, GuiLayoutRect r) {
        graphics.fill(r.x() + 2, r.y(), r.right() - 2, r.bottom(), 0xFF000000);
        graphics.fill(r.x(), r.y() + 2, r.right(), r.bottom() - 2, 0xFF000000);
        graphics.fill(r.x() + 1, r.y() + 2, r.right() - 1, r.bottom() - 2, 0xFF555555);
        graphics.fill(r.x() + 2, r.y() + 1, r.right() - 2, r.bottom() - 1, 0xFF555555);
        graphics.fill(r.x() + 2, r.y() + 2, r.right() - 3, r.bottom() - 3, 0xFFFFFFFF);
        graphics.fill(r.x() + 4, r.y() + 4, r.right() - 4, r.bottom() - 4, 0xFFC6C6C6);
    }

    private void renderPreview(GuiGraphics graphics) {
        if (!this.layout.hasPreview(this.speciesMode)) return;
        GuiLayoutRect r = this.layout.preview();
        graphics.fill(r.x(), r.y(), r.right(), r.bottom(), 0xFFB0B0B0);
        graphics.renderOutline(r.x(), r.y(), r.w(), r.h(), 0xFF8B8B8B);
        LivingEntity entity = previewEntity();
        if (entity != null) {
            float height = entity instanceof EdDYON.guaniao.content.bird.scale.ScalableBirdModel bird
                    ? bird.modelScaleProfile().targetHeightBlocks() * bird.getIndividualModelScale() : entity.getBbHeight();
            int scale = BirdModelScale.fitPreviewScale(Math.max(1, Math.round(66 / Math.max(0.2F, height))));
            graphics.enableScissor(r.x(), r.y(), r.right(), r.bottom());
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, r.centerX(), r.bottom() - 8,
                    scale, this.previewLookX, this.previewLookY, entity);
            graphics.disableScissor();
        }
        drawCentered(graphics, Component.translatable("gui.guaniao.bird_config.preview_hint"), r.centerX(), r.bottom() + 4, MUTED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.layout.fieldScrollbar(this.speciesMode).contains(mouseX, mouseY)) {
            if (this.layout.maxFieldScroll(this.speciesMode, this.settingCount) == 0 || !captureNumericValues()) return true;
            GuiLayoutRect thumb = this.layout.fieldThumb(this.speciesMode, this.settingCount, this.fieldScroll);
            this.fieldDragOffset = thumb.contains(mouseX, mouseY) ? mouseY - thumb.y() : thumb.h() / 2.0D;
            this.draggingFields = true;
            this.setFocused(null);
            dragFieldsTo(mouseY);
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && this.layout.hasPreview(this.speciesMode) && this.layout.preview().contains(mouseX, mouseY)) {
            this.draggingPreview = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingFields) {
            dragFieldsTo(mouseY);
            return true;
        }
        if (button == 0 && this.draggingPreview) {
            this.previewLookX = Mth.clamp(this.previewLookX + (float)dragX * 1.4F, -80.0F, 80.0F);
            this.previewLookY = Mth.clamp(this.previewLookY + (float)dragY, -40.0F, 40.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingFields) {
            this.draggingFields = false;
            return true;
        }
        if (button == 0 && this.draggingPreview) {
            this.draggingPreview = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.layout.left.contains(mouseX, mouseY)) {
            scrollSubjects(-(int)Math.signum(delta));
            return true;
        }
        if (this.layout.fields(this.speciesMode).contains(mouseX, mouseY)
                || this.layout.fieldScrollbar(this.speciesMode).contains(mouseX, mouseY)) {
            scrollFields(-(int)Math.signum(delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        super.tick();
        for (NumericInput input : this.numericInputs) input.box().tick();
        if (this.previewEntity != null) {
            ++this.previewEntity.tickCount;
            applyPreviewIdle(this.previewEntity);
        }
    }

    private List<SettingSpec> allSettingsForSelection() {
        List<SettingSpec> settings = new ArrayList<>();
        BirdSpecies species = selectedSpecies();
        if (species == null) {
            BirdGlobalConfig global = this.data.global;
            settings.add(SettingSpec.storageScope());
            settings.add(SettingSpec.toggle("april_fools_mode", () -> global.aprilFoolsMode, value -> global.aprilFoolsMode = value));
            settings.add(SettingSpec.toggle("sparrow_tide_mode", () -> global.sparrowTideMode, value -> global.sparrowTideMode = value));
            settings.add(SettingSpec.toggle("sky_bird_ecology", () -> global.skyBirdEcologyEnabled, value -> global.skyBirdEcologyEnabled = value));
            settings.add(SettingSpec.toggle("bird_death_guilt_messages", () -> global.birdDeathGuiltMessagesEnabled, value -> global.birdDeathGuiltMessagesEnabled = value));
            settings.add(SettingSpec.toggle("natural_spawning", () -> global.naturalSpawning, value -> global.naturalSpawning = value));
            settings.add(SettingSpec.toggle("colonial_mode", () -> global.colonialMode, value -> global.colonialMode = value));
            settings.add(SettingSpec.toggle("natural_crow_nests", () -> global.naturalCrowNests, value -> global.naturalCrowNests = value));
            settings.add(SettingSpec.number("crow_nest_generation_multiplier", () -> global.crowNestGenerationMultiplier, value -> global.crowNestGenerationMultiplier = value, 0.0D, 5.0D, false));
            settings.add(SettingSpec.toggle("crows_store_treasures", () -> global.crowsStoreTreasures, value -> global.crowsStoreTreasures = value));
            settings.add(SettingSpec.number("crow_nest_search_distance", () -> global.crowNestSearchDistance, value -> global.crowNestSearchDistance = (int)value, 16.0D, 128.0D, true));
            settings.add(SettingSpec.number("max_crow_nest_treasures", () -> global.maxCrowNestTreasures, value -> global.maxCrowNestTreasures = (int)value, 1.0D, 6.0D, true));
            settings.add(SettingSpec.toggle("crows_claim_player_nests", () -> global.crowsClaimPlayerNests, value -> global.crowsClaimPlayerNests = value));
            settings.add(SettingSpec.toggle("pet_bird_commands", () -> global.enablePetBirdCommands, value -> global.enablePetBirdCommands = value));
            settings.add(SettingSpec.toggle("seagull_stealing", () -> global.enableSeagullStealing, value -> global.enableSeagullStealing = value));
            settings.add(SettingSpec.toggle("crow_item_safety", () -> global.crowItemSafety, value -> global.crowItemSafety = value));
            settings.add(SettingSpec.toggle("birds_pass_through_leaves", () -> global.birdsPassThroughLeaves, value -> global.birdsPassThroughLeaves = value));
            settings.add(SettingSpec.toggle("dropping_pressure_plate_pulse", () -> global.droppingPressurePlatePulseEnabled, value -> global.droppingPressurePlatePulseEnabled = value));
            settings.add(SettingSpec.number("dropping_pressure_plate_pulse_ticks", () -> global.droppingPressurePlatePulseTicks, value -> global.droppingPressurePlatePulseTicks = (int)value, 5.0D, 100.0D, true));
            settings.add(SettingSpec.toggle("photo_uploads", () -> global.photoUploadsEnabled, value -> global.photoUploadsEnabled = value));
            settings.add(SettingSpec.toggle("photo_uploads_operator_only", () -> global.photoUploadsOperatorOnly, value -> global.photoUploadsOperatorOnly = value));
            settings.add(SettingSpec.toggle("photo_uploads_whitelist_only", () -> global.photoUploadsWhitelistedOnly, value -> global.photoUploadsWhitelistedOnly = value));
            settings.add(SettingSpec.number("max_photos_per_player", () -> global.maxPhotosPerPlayer, value -> global.maxPhotosPerPlayer = (int)value, 1.0D, 10000.0D, true));
            settings.add(SettingSpec.number("max_photo_storage_player", () -> global.maxPhotoStorageMiBPerPlayer, value -> global.maxPhotoStorageMiBPerPlayer = (int)value, 1.0D, 4096.0D, true));
            settings.add(SettingSpec.number("max_photos_per_world", () -> global.maxPhotosPerWorld, value -> global.maxPhotosPerWorld = (int)value, 1.0D, 100000.0D, true));
            settings.add(SettingSpec.number("max_photo_storage_world", () -> global.maxPhotoStorageMiBPerWorld, value -> global.maxPhotoStorageMiBPerWorld = (int)value, 1.0D, 65536.0D, true));
            settings.add(SettingSpec.number("photo_trash_retention_days", () -> global.photoTrashRetentionDays, value -> global.photoTrashRetentionDays = (int)value, 1.0D, 90.0D, true));
            settings.add(SettingSpec.number("max_photo_downloads", () -> global.maxConcurrentPhotoDownloads, value -> global.maxConcurrentPhotoDownloads = (int)value, 1.0D, 128.0D, true));
            settings.add(SettingSpec.number("photo_download_kib_tick", () -> global.photoDownloadKiBPerTick, value -> global.photoDownloadKiBPerTick = (int)value, 24.0D, 2048.0D, true));
            settings.add(SettingSpec.number("spawn_multiplier", () -> global.spawnMultiplier, value -> global.spawnMultiplier = value, 0.0D, 10.0D, false));
            settings.add(SettingSpec.number("dropping_multiplier", () -> global.droppingFrequencyMultiplier, value -> global.droppingFrequencyMultiplier = value, 0.0D, 10.0D, false));
            settings.add(SettingSpec.number("sound_multiplier", () -> global.soundVolumeMultiplier, value -> global.soundVolumeMultiplier = value, 0.0D, 4.0D, false));
            settings.add(SettingSpec.number("max_birds", () -> global.maxBirdsNearby, value -> global.maxBirdsNearby = (int)value, 0.0D, 256.0D, true));
            settings.add(SettingSpec.number("max_wild_birds_region", () -> global.maxWildBirdsPerRegion, value -> global.maxWildBirdsPerRegion = (int)value, 0.0D, 1024.0D, true));
            settings.add(SettingSpec.number("population_region_chunks", () -> global.populationRegionChunks, value -> global.populationRegionChunks = (int)value, 1.0D, 16.0D, true));
            settings.add(SettingSpec.number("wild_bird_despawn_ticks", () -> global.wildBirdDespawnTicks, value -> global.wildBirdDespawnTicks = (int)value, 200.0D, 1728000.0D, true));
            settings.add(SettingSpec.number("flyby_bird_lifetime_ticks", () -> global.flybyBirdLifetimeTicks, value -> global.flybyBirdLifetimeTicks = (int)value, 200.0D, 72000.0D, true));
            settings.add(SettingSpec.number("flock_refresh_ticks", () -> global.flockRefreshTicks, value -> global.flockRefreshTicks = (int)value, 5.0D, 200.0D, true));
            settings.add(SettingSpec.number("habitat_cache_ticks", () -> global.habitatCacheTicks, value -> global.habitatCacheTicks = (int)value, 20.0D, 2400.0D, true));
            settings.add(SettingSpec.number("seagull_steal_cooldown", () -> global.seagullPlayerCooldownTicks, value -> global.seagullPlayerCooldownTicks = (int)value, 0.0D, 72000.0D, true));
            settings.add(SettingSpec.number("seagull_concurrent_targets", () -> global.maxConcurrentSeagullTargetsPerPlayer, value -> global.maxConcurrentSeagullTargetsPerPlayer = (int)value, 0.0D, 8.0D, true));
            settings.add(SettingSpec.number("bird_scan_budget", () -> global.birdScanBudgetPerTick, value -> global.birdScanBudgetPerTick = (int)value, 1.0D, 128.0D, true));
            settings.add(SettingSpec.number("max_droppings", () -> global.maxGroundDroppingsNearby, value -> global.maxGroundDroppingsNearby = (int)value, 0.0D, 16.0D, true));
            settings.add(SettingSpec.toggle("natural_droppings", () -> global.naturalDroppingsEnabled, value -> global.naturalDroppingsEnabled = value));
            settings.add(SettingSpec.number("dropping_radius", () -> global.droppingNearbyRadius, value -> global.droppingNearbyRadius = (int)value, 4.0D, 32.0D, true));
            settings.add(SettingSpec.number("dropping_area_min_seconds", () -> global.droppingAreaCooldownMinSeconds, value -> global.droppingAreaCooldownMinSeconds = (int)value, 1.0D, 300.0D, true));
            settings.add(SettingSpec.number("dropping_area_max_seconds", () -> global.droppingAreaCooldownMaxSeconds, value -> global.droppingAreaCooldownMaxSeconds = (int)value, 1.0D, 300.0D, true));
            settings.add(SettingSpec.number("dropping_lifetime_min_minutes", () -> global.droppingLifetimeMinMinutes, value -> global.droppingLifetimeMinMinutes = (int)value, 1.0D, 30.0D, true));
            settings.add(SettingSpec.number("dropping_lifetime_max_minutes", () -> global.droppingLifetimeMaxMinutes, value -> global.droppingLifetimeMaxMinutes = (int)value, 1.0D, 30.0D, true));
            settings.add(SettingSpec.toggle("enable_migration", () -> global.enableMigration, value -> global.enableMigration = value));
            settings.add(SettingSpec.number("migration_interval_ticks", () -> global.migrationIntervalTicks, value -> global.migrationIntervalTicks = (int)value, 200.0D, 72000.0D, true));
            settings.add(SettingSpec.number("migration_radius", () -> global.migrationRadius, value -> global.migrationRadius = (int)value, 32.0D, 512.0D, true));
            return settings;
        }

        BirdSpeciesConfig bird = this.data.birds.computeIfAbsent(species.id(), id -> new BirdSpeciesConfig(species));
        settings.add(SettingSpec.toggle("enabled", () -> bird.enabled, value -> bird.enabled = value));
        settings.add(SettingSpec.toggle("natural_spawning", () -> bird.naturalSpawning, value -> bird.naturalSpawning = value));
        settings.add(SettingSpec.number("spawn_multiplier", () -> bird.spawnMultiplier, value -> bird.spawnMultiplier = value, 0.0D, 10.0D, false));
        settings.add(SettingSpec.number("min_group", () -> bird.minGroup, value -> bird.minGroup = (int)value, 1.0D, 32.0D, true));
        settings.add(SettingSpec.number("max_group", () -> bird.maxGroup, value -> bird.maxGroup = (int)value, 1.0D, 32.0D, true));
        settings.add(SettingSpec.number("dropping_multiplier", () -> bird.droppingFrequencyMultiplier, value -> bird.droppingFrequencyMultiplier = value, 0.0D, 10.0D, false));
        settings.add(SettingSpec.number("sound_multiplier", () -> bird.soundVolumeMultiplier, value -> bird.soundVolumeMultiplier = value, 0.0D, 4.0D, false));
        settings.add(SettingSpec.number("max_wild_nearby", () -> bird.maxWildNearby, value -> bird.maxWildNearby = (int)value, 0.0D, 256.0D, true));
        settings.add(SettingSpec.number("flock_radius", () -> bird.flockRadius, value -> bird.flockRadius = value, 2.0D, 48.0D, false));
        settings.add(SettingSpec.number("flock_max_members", () -> bird.flockMaxMembers, value -> bird.flockMaxMembers = (int)value, 2.0D, 64.0D, true));
        settings.add(SettingSpec.number("food_scan_interval", () -> bird.foodScanInterval, value -> bird.foodScanInterval = (int)value, 5.0D, 1200.0D, true));
        settings.add(SettingSpec.number("threat_scan_interval", () -> bird.threatScanInterval, value -> bird.threatScanInterval = (int)value, 5.0D, 1200.0D, true));
        settings.add(SettingSpec.number("owner_teleport_distance", () -> bird.ownerTeleportDistance, value -> bird.ownerTeleportDistance = value, 8.0D, 128.0D, false));
        settings.add(SettingSpec.number("ambient_sound_cooldown", () -> bird.ambientSoundCooldownMultiplier, value -> bird.ambientSoundCooldownMultiplier = value, 0.25D, 8.0D, false));
        return settings;
    }

    private LivingEntity previewEntity() {
        BirdSpecies species = selectedSpecies();
        if (this.previewEntity == null && species != null && this.minecraft != null && this.minecraft.level != null) {
            EntityType<?> type = species.entityType();
            if (type != null && type.create((Level)this.minecraft.level) instanceof LivingEntity living) {
                this.previewEntity = living;
                if (living instanceof Mob mob) {
                    mob.setNoAi(true);
                }
                living.setSilent(true);
                living.setNoGravity(true);
                living.setOnGround(true);
                applyPreviewIdle(living);
            }
        }
        return this.previewEntity;
    }

    private static void applyPreviewIdle(LivingEntity entity) {
        if (entity instanceof KiwiEntity bird) {
            bird.setGuidePreviewAnimation(KiwiEntity.GuidePreviewAnimation.IDLE);
        } else if (entity instanceof MynaEntity bird) {
            bird.setGuidePreviewAnimation(MynaEntity.GuidePreviewAnimation.IDLE);
        } else if (entity instanceof NightHeronEntity bird) {
            bird.setGuidePreviewAnimation(NightHeronEntity.GuidePreviewAnimation.IDLE);
        } else if (entity instanceof SparrowEntity bird) {
            bird.setGuidePreviewAnimation(SparrowEntity.GuidePreviewAnimation.IDLE);
        } else if (entity instanceof BudgerigarEntity bird) {
            bird.setGuidePreviewAnimation(BudgerigarEntity.GuidePreviewAnimation.IDLE);
        } else if (entity instanceof AbstractColumbidEntity bird) {
            bird.setGuidePreviewAnimation(AbstractColumbidEntity.GuidePreviewAnimation.IDLE);
        } else if (entity instanceof CrowEntity bird) {
            bird.setGuidePreviewAnimation(CrowEntity.GuidePreviewAnimation.IDLE);
        } else if (entity instanceof SeagullEntity bird) {
            bird.setGuidePreviewAnimation(SeagullEntity.GuidePreviewAnimation.IDLE);
        } else if (entity instanceof KestrelEntity bird) {
            bird.setGuidePreviewAnimation(KestrelEntity.GuidePreviewAnimation.IDLE);
        } else if (entity instanceof CassowaryEntity bird) {
            bird.setGuidePreviewAnimation(CassowaryEntity.GuidePreviewAnimation.IDLE);
        }
    }

    private BirdSpecies selectedSpecies() {
        return !this.speciesMode || this.selectedIndex <= 0 ? null : BirdSpecies.values()[this.selectedIndex - 1];
    }

    private static Component toggleLabel(boolean enabled) {
        return Component.translatable(enabled ? "gui.guaniao.bird_config.on" : "gui.guaniao.bird_config.off");
    }

    private Component scopeLabel() {
        if (!this.data.worldScopeAllowed) {
            return Component.translatable("gui.guaniao.bird_config.scope.server");
        }
        return Component.translatable(BirdConfigScope.sanitize(this.data.storageScope) == BirdConfigScope.WORLD
                ? "gui.guaniao.bird_config.scope.world"
                : "gui.guaniao.bird_config.scope.global");
    }

    private void toggleStorageScope(Button button) {
        if (!this.data.worldScopeAllowed) {
            return;
        }
        this.data.storageScope = BirdConfigScope.sanitize(this.data.storageScope) == BirdConfigScope.WORLD
                ? BirdConfigScope.GLOBAL
                : BirdConfigScope.WORLD;
        button.setMessage(scopeLabel());
        this.status = Component.translatable("gui.guaniao.bird_config.unsaved");
    }

    private static boolean isNumericText(String value) {
        return value.isEmpty() || value.matches("\\d*(\\.\\d*)?");
    }

    private static String format(double value, boolean integer) {
        if (integer || Math.abs(value - Math.rint(value)) < 1.0E-6D) {
            return String.valueOf((long)Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private Component fit(Component text, int maxWidth) {
        return Component.literal(this.font.plainSubstrByWidth(text.getString(), Math.max(12, maxWidth)));
    }

    private record NumericInput(SettingSpec setting, EditBox box) {
    }

    private record SettingSpec(
            String key,
            Component label,
            Component description,
            boolean storageScopeRow,
            boolean toggle,
            BooleanSupplier booleanGetter,
            Consumer<Boolean> booleanSetter,
            DoubleSupplier numberGetter,
            DoubleConsumer numberSetter,
            double min,
            double max,
            boolean integer
    ) {
        private static SettingSpec storageScope() {
            return new SettingSpec(
                    "scope", Component.translatable("gui.guaniao.bird_config.setting.scope"),
                    Component.translatable("gui.guaniao.bird_config.setting.scope.description"),
                    true, false, null, null, null, null, 0.0D, 1.0D, false
            );
        }

        private static SettingSpec toggle(String key, BooleanSupplier getter, Consumer<Boolean> setter) {
            return new SettingSpec(
                    key, Component.translatable("gui.guaniao.bird_config.setting." + key),
                    Component.translatable("gui.guaniao.bird_config.setting." + key + ".description"),
                    false, true, getter, setter, null, null, 0.0D, 1.0D, false
            );
        }

        private static SettingSpec number(String key, DoubleSupplier getter, DoubleConsumer setter, double min, double max, boolean integer) {
            return new SettingSpec(
                    key, Component.translatable("gui.guaniao.bird_config.setting." + key),
                    Component.translatable("gui.guaniao.bird_config.setting." + key + ".description"),
                    false, false, null, null, getter, setter, min, max, integer
            );
        }
    }
}
