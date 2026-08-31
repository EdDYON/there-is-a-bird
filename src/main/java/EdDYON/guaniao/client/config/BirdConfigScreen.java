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
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.network.GuaniaoNetwork;
import EdDYON.guaniao.network.SaveBirdConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
    private static final int MARGIN = 14;
    private static final int GAP = 10;
    private static final int LIST_ROW_HEIGHT = 25;
    private static final int SETTING_ROW_HEIGHT = 31;
    private static final int FIELD_TOP = 72;
    private static final int BOTTOM_HEIGHT = 42;
    private static final int PANEL = 0xD9182026;
    private static final int PANEL_ALT = 0xD91D272E;
    private static final int ROW = 0x88263239;
    private static final int ROW_HOVER = 0xAA31424B;
    private static final int SELECTED = 0xAA3B474E;
    private static final int OUTLINE = 0xAA59666C;
    private static final int TEXT = 0xFFF1F5F5;
    private static final int MUTED = 0xFFB8C5C7;

    private BirdConfigData data;
    private final List<NumericInput> numericInputs = new ArrayList<>();
    private final List<SettingSpec> visibleSettings = new ArrayList<>();
    private int selectedIndex;
    private int listScroll;
    private int fieldScroll;
    private Component status = Component.empty();
    private LivingEntity previewEntity;
    private float previewLookX = 18.0F;
    private float previewLookY = -8.0F;
    private boolean draggingPreview;

    public BirdConfigScreen(BirdConfigData data) {
        super(Component.translatable("gui.guaniao.bird_config.title"));
        this.data = data.copy();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        refreshWidgets();
    }

    private void refreshWidgets() {
        this.clearWidgets();
        this.numericInputs.clear();
        this.visibleSettings.clear();

        int bottomY = this.height - MARGIN - 22;
        int buttonX = rightX() + 8;
        int buttonGap = 6;
        int buttonW = Math.max(34, (rightW() - 16 - buttonGap * 2) / 3);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.guaniao.bird_config.save"), button -> save())
                .bounds(buttonX, bottomY, buttonW, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.guaniao.bird_config.reset"), button -> resetDefaults())
                .bounds(buttonX + buttonW + buttonGap, bottomY, buttonW, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.guaniao.bird_config.close"), button -> onClose())
                .bounds(buttonX + (buttonW + buttonGap) * 2, bottomY, buttonW, 20).build());

        List<SettingSpec> settings = settingsForSelection();
        int visibleRows = visibleSettingRows();
        this.fieldScroll = Mth.clamp(this.fieldScroll, 0, Math.max(0, settings.size() - visibleRows));
        int controlX = controlX();
        int controlW = 96;

        for (int row = 0; row < visibleRows && this.fieldScroll + row < settings.size(); row++) {
            SettingSpec setting = settings.get(this.fieldScroll + row);
            this.visibleSettings.add(setting);
            int y = FIELD_TOP + row * SETTING_ROW_HEIGHT + 5;
            if (setting.storageScopeRow()) {
                Button scope = Button.builder(scopeLabel(), this::toggleStorageScope)
                        .bounds(controlX, y, controlW, 20)
                        .build();
                scope.active = this.data.worldScopeAllowed;
                this.addRenderableWidget(scope);
            } else if (setting.toggle()) {
                Button toggle = Button.builder(toggleLabel(setting.booleanGetter().getAsBoolean()), button -> {
                            boolean next = !setting.booleanGetter().getAsBoolean();
                            setting.booleanSetter().accept(next);
                            button.setMessage(toggleLabel(next));
                            this.status = Component.translatable("gui.guaniao.bird_config.unsaved");
                        })
                        .bounds(controlX, y, controlW, 20)
                        .build();
                this.addRenderableWidget(toggle);
            } else {
                EditBox box = new EditBox(this.font, controlX, y, controlW, 20, setting.label());
                box.setMaxLength(16);
                box.setFilter(BirdConfigScreen::isNumericText);
                box.setValue(format(setting.numberGetter().getAsDouble(), setting.integer()));
                this.numericInputs.add(new NumericInput(setting, box));
                this.addRenderableWidget(box);
            }
        }
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
        renderPanels(graphics);
        graphics.drawString(this.font, this.title, MARGIN + 3, 16, TEXT, false);
        renderCategoryList(graphics, mouseX, mouseY);
        renderSettings(graphics, mouseX, mouseY);
        renderPreview(graphics);
        if (!this.status.getString().isBlank()) {
            graphics.drawString(this.font, fit(this.status, rightW() - 28), rightX() + 14, this.height - MARGIN - 36, MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderHoveredDescription(graphics, mouseX, mouseY);
    }

    private void renderPanels(GuiGraphics graphics) {
        int top = 52;
        int bottom = this.height - MARGIN;
        graphics.fill(MARGIN, top, MARGIN + leftW(), bottom, PANEL);
        graphics.fill(rightX(), top, rightX() + rightW(), bottom, PANEL_ALT);
    }

    private void renderCategoryList(GuiGraphics graphics, int mouseX, int mouseY) {
        int top = 66;
        int count = BirdSpecies.values().length + 1;
        int visibleRows = visibleListRows();
        this.listScroll = Mth.clamp(this.listScroll, 0, Math.max(0, count - visibleRows));

        for (int row = 0; row < visibleRows && this.listScroll + row < count; row++) {
            int index = this.listScroll + row;
            int y = top + row * LIST_ROW_HEIGHT;
            boolean selected = index == this.selectedIndex;
            boolean hovered = mouseX >= MARGIN + 7 && mouseX <= MARGIN + leftW() - 7 && mouseY >= y && mouseY < y + LIST_ROW_HEIGHT - 3;
            graphics.fill(MARGIN + 7, y, MARGIN + leftW() - 7, y + LIST_ROW_HEIGHT - 3, selected ? SELECTED : hovered ? ROW_HOVER : ROW);
            Component name = index == 0
                    ? Component.translatable("gui.guaniao.bird_config.global")
                    : Component.translatable(BirdSpecies.values()[index - 1].translationKey());
            graphics.drawString(this.font, fit(name, leftW() - 30), MARGIN + 15, y + 7, selected ? TEXT : MUTED, false);
        }
    }

    private void renderSettings(GuiGraphics graphics, int mouseX, int mouseY) {
        int labelX = rightX() + 15;
        int rowRight = previewVisible() ? previewX() - 10 : rightX() + rightW() - 12;
        Component heading = selectedSpecies() == null
                ? Component.translatable("gui.guaniao.bird_config.global_heading")
                : Component.translatable(selectedSpecies().translationKey());
        graphics.drawString(this.font, heading, labelX, 59, TEXT, false);

        for (int row = 0; row < this.visibleSettings.size(); row++) {
            int y = FIELD_TOP + row * SETTING_ROW_HEIGHT;
            boolean hovered = mouseX >= labelX - 5 && mouseX <= rowRight && mouseY >= y && mouseY < y + SETTING_ROW_HEIGHT - 2;
            graphics.fill(labelX - 5, y, rowRight, y + SETTING_ROW_HEIGHT - 2, hovered ? ROW_HOVER : ROW);
            graphics.drawString(this.font, fit(this.visibleSettings.get(row).label(), Math.max(40, controlX() - labelX - 10)), labelX, y + 11, TEXT, false);
        }
    }

    private void renderPreview(GuiGraphics graphics) {
        if (!previewVisible()) {
            return;
        }
        int x = previewX();
        int y = 67;
        int w = 132;
        int h = 112;
        graphics.fill(x, y, x + w, y + h, 0xAA10171C);
        graphics.renderOutline(x, y, w, h, OUTLINE);
        LivingEntity entity = previewEntity();
        if (entity != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    x + w / 2,
                    y + h - 8,
                    previewScale(selectedSpecies()),
                    this.previewLookX,
                    this.previewLookY,
                    entity
            );
        }
        graphics.drawCenteredString(this.font, Component.translatable("gui.guaniao.bird_config.preview_hint"), x + w / 2, y + h + 4, MUTED);
    }

    private void renderHoveredDescription(GuiGraphics graphics, int mouseX, int mouseY) {
        int labelX = rightX() + 10;
        int rowRight = previewVisible() ? previewX() - 10 : rightX() + rightW() - 12;
        if (mouseX < labelX || mouseX > rowRight || mouseY < FIELD_TOP) {
            return;
        }
        int row = (mouseY - FIELD_TOP) / SETTING_ROW_HEIGHT;
        if (row >= 0 && row < this.visibleSettings.size()) {
            graphics.renderTooltip(this.font, this.visibleSettings.get(row).description(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && previewVisible() && isInPreview(mouseX, mouseY)) {
            this.draggingPreview = true;
            return true;
        }
        int top = 66;
        if (mouseX >= MARGIN + 7 && mouseX <= MARGIN + leftW() - 7 && mouseY >= top) {
            int row = ((int)mouseY - top) / LIST_ROW_HEIGHT;
            int index = this.listScroll + row;
            if (row >= 0 && row < visibleListRows() && index >= 0 && index <= BirdSpecies.values().length) {
                if (!captureNumericValues()) {
                    return true;
                }
                this.selectedIndex = index;
                this.fieldScroll = 0;
                this.previewEntity = null;
                this.previewLookX = 18.0F;
                this.previewLookY = -8.0F;
                refreshWidgets();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingPreview) {
            this.previewLookX = Mth.clamp(this.previewLookX + (float)dragX * 1.4F, -80.0F, 80.0F);
            this.previewLookY = Mth.clamp(this.previewLookY + (float)dragY, -40.0F, 40.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingPreview) {
            this.draggingPreview = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!captureNumericValues()) {
            return true;
        }
        int direction = (int)Math.signum(delta);
        if (mouseX < rightX()) {
            int max = Math.max(0, BirdSpecies.values().length + 1 - visibleListRows());
            this.listScroll = Mth.clamp(this.listScroll - direction, 0, max);
        } else {
            int max = Math.max(0, settingsForSelection().size() - visibleSettingRows());
            this.fieldScroll = Mth.clamp(this.fieldScroll - direction, 0, max);
        }
        refreshWidgets();
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.previewEntity != null) {
            ++this.previewEntity.tickCount;
            applyPreviewIdle(this.previewEntity);
        }
    }

    private List<SettingSpec> settingsForSelection() {
        List<SettingSpec> settings = new ArrayList<>();
        BirdSpecies species = selectedSpecies();
        if (species == null) {
            BirdGlobalConfig global = this.data.global;
            settings.add(SettingSpec.storageScope());
            settings.add(SettingSpec.toggle("april_fools_mode", () -> global.aprilFoolsMode, value -> global.aprilFoolsMode = value));
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
        if (entity instanceof MynaEntity bird) {
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
        }
    }

    private BirdSpecies selectedSpecies() {
        return this.selectedIndex <= 0 ? null : BirdSpecies.values()[this.selectedIndex - 1];
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

    private int leftW() {
        return Mth.clamp(this.width / 4, 126, 184);
    }

    private int rightX() {
        return MARGIN + leftW() + GAP;
    }

    private int rightW() {
        return Math.max(1, this.width - rightX() - MARGIN);
    }

    private int visibleListRows() {
        return Math.max(1, (this.height - 66 - BOTTOM_HEIGHT - MARGIN) / LIST_ROW_HEIGHT);
    }

    private int visibleSettingRows() {
        return Math.max(1, (this.height - FIELD_TOP - BOTTOM_HEIGHT - MARGIN) / SETTING_ROW_HEIGHT);
    }

    private boolean previewVisible() {
        return selectedSpecies() != null && rightW() >= 430;
    }

    private int previewX() {
        return rightX() + rightW() - 144;
    }

    private int controlX() {
        return previewVisible() ? previewX() - 106 : rightX() + rightW() - 110;
    }

    private boolean isInPreview(double mouseX, double mouseY) {
        return mouseX >= previewX() && mouseX <= previewX() + 132 && mouseY >= 67 && mouseY <= 179;
    }

    private int previewScale(BirdSpecies species) {
        if (species == null) {
            return BirdModelScale.fitPreviewScale(40);
        }
        int scale = switch (species) {
            case NIGHT_HERON -> 27;
            case SPARROW -> 54;
            case LONG_TAILED_TIT -> 51;
            case COCKATIEL -> 45;
            case MACAW -> 34;
            case BUDGERIGAR -> 48;
            case SPOTTED_DOVE, PIGEON -> 42;
            case CROW, SEAGULL -> 37;
            case KIWI -> 43;
            case MYNA -> 47;
        };
        return BirdModelScale.fitPreviewScale(scale);
    }

    private Component fit(Component text, int maxWidth) {
        return Component.literal(this.font.plainSubstrByWidth(text.getString(), Math.max(12, maxWidth)));
    }

    private record NumericInput(SettingSpec setting, EditBox box) {
    }

    private record SettingSpec(
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
                    Component.translatable("gui.guaniao.bird_config.setting.scope"),
                    Component.translatable("gui.guaniao.bird_config.setting.scope.description"),
                    true, false, null, null, null, null, 0.0D, 1.0D, false
            );
        }

        private static SettingSpec toggle(String key, BooleanSupplier getter, Consumer<Boolean> setter) {
            return new SettingSpec(
                    Component.translatable("gui.guaniao.bird_config.setting." + key),
                    Component.translatable("gui.guaniao.bird_config.setting." + key + ".description"),
                    false, true, getter, setter, null, null, 0.0D, 1.0D, false
            );
        }

        private static SettingSpec number(String key, DoubleSupplier getter, DoubleConsumer setter, double min, double max, boolean integer) {
            return new SettingSpec(
                    Component.translatable("gui.guaniao.bird_config.setting." + key),
                    Component.translatable("gui.guaniao.bird_config.setting." + key + ".description"),
                    false, false, null, null, getter, setter, min, max, integer
            );
        }
    }
}
