package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.registry.GuaniaoItems;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Brief, dismissible instruction card shown before the first viewfinder, or on request. */
public final class CameraHelpScreen extends Screen {
    private final Screen parent;
    private final InteractionHand hand;
    private final LocalPlayer owner;
    private final ClientLevel level;
    private final ItemStack camera;
    private int left;
    private int top;
    private int cardWidth;
    private int cardHeight;

    private CameraHelpScreen(Screen parent, InteractionHand hand) {
        super(Component.translatable("gui.guaniao.camera_help.title"));
        Minecraft minecraft = Minecraft.getInstance();
        this.parent = parent;
        this.hand = hand;
        this.owner = minecraft.player;
        this.level = minecraft.level;
        this.camera = hand != null && this.owner != null ? this.owner.getItemInHand(hand) : ItemStack.EMPTY;
    }

    public static boolean showFirstUse(InteractionHand hand) {
        if (CameraClientPreferences.hasSeenHelp()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || !minecraft.player.getItemInHand(hand).is(GuaniaoItems.NIKON_D750.get())) return false;
        minecraft.setScreen(new CameraHelpScreen(null, hand));
        return true;
    }

    static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new CameraHelpScreen(parent, null));
    }

    @Override
    protected void init() {
        this.cardWidth = Math.min(240, Math.max(1, this.width - 24));
        this.cardHeight = Math.min(128, Math.max(1, this.height - 16));
        this.left = (this.width - this.cardWidth) / 2;
        this.top = (this.height - this.cardHeight) / 2;
        int buttonWidth = Math.min(116, this.cardWidth - 24);
        addRenderableWidget(Button.builder(Component.translatable(this.parent == null
                        ? "gui.guaniao.camera_help.begin" : "gui.back"), button -> finish())
                .bounds((this.width - buttonWidth) / 2, this.top + this.cardHeight - 26, buttonWidth, 20).build());
    }

    @Override
    public void tick() {
        if (!validContext()) Minecraft.getInstance().setScreen(null);
    }

    private boolean validContext() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player == this.owner && minecraft.level == this.level
                && this.owner.isAlive() && (this.parent == null
                ? this.owner.getItemInHand(this.hand) == this.camera && this.camera.is(GuaniaoItems.NIKON_D750.get())
                : CameraClientCapture.isViewfinderOpen());
    }

    private void finish() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!validContext()) {
            minecraft.setScreen(null);
            return;
        }
        if (this.parent != null) {
            minecraft.setScreen(this.parent);
        } else {
            CameraClientPreferences.markHelpSeen();
            minecraft.setScreen(null);
            CameraClientCapture.openViewfinder(this.hand);
        }
    }

    @Override
    public void onClose() {
        boolean valid = validContext();
        if (this.parent == null && valid) CameraClientPreferences.markHelpSeen();
        Minecraft.getInstance().setScreen(this.parent != null && valid ? this.parent : null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x30000000);
        graphics.fill(this.left - 2, this.top - 2, this.left + this.cardWidth + 2, this.top + this.cardHeight + 2, 0xFF373737);
        graphics.fill(this.left, this.top, this.left + this.cardWidth, this.top + this.cardHeight, 0xFFC6C6C6);
        graphics.hLine(this.left, this.left + this.cardWidth - 1, this.top, 0xFFFFFFFF);
        graphics.vLine(this.left, this.top, this.top + this.cardHeight - 1, 0xFFFFFFFF);
        graphics.hLine(this.left, this.left + this.cardWidth, this.top + this.cardHeight, 0xFF555555);
        graphics.vLine(this.left + this.cardWidth, this.top, this.top + this.cardHeight, 0xFF555555);
        graphics.drawString(this.font, this.title, (this.width - this.font.width(this.title)) / 2,
                this.top + 8, 0xFF303030, false);

        List<Component> labels = compactControlLines();
        int columnWidth = (this.cardWidth - 36) / 2;
        int hoveredControl = -1;
        for (int i = 0; i < labels.size(); i++) {
            int x = this.left + 12 + (i / 3) * (columnWidth + 12);
            int y = this.top + 28 + (i % 3) * 16;
            drawTruncated(graphics, labels.get(i), x, y, columnWidth, 0xFF303030);
            if (mouseX >= x && mouseX < x + columnWidth && mouseY >= y - 2 && mouseY < y + 12) {
                hoveredControl = i;
            }
        }
        List<FormattedCharSequence> footer = this.font.split(
                Component.translatable("gui.guaniao.camera_help.resume_short"), this.cardWidth - 24);
        for (int i = 0; i < Math.min(2, footer.size()); i++) {
            graphics.drawString(this.font, footer.get(i), this.left + 12,
                    this.top + 79 + i * this.font.lineHeight, 0xFF555555, false);
        }
        for (var renderable : this.renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
        if (hoveredControl >= 0) {
            graphics.renderTooltip(this.font, controlLines().get(hoveredControl), mouseX, mouseY);
        }
    }

    private static List<Component> compactControlLines() {
        return List.of(
                Component.translatable("gui.guaniao.camera_help.short.capture", mouseName(GLFW.GLFW_MOUSE_BUTTON_LEFT)),
                Component.translatable("gui.guaniao.camera_help.short.close", mouseName(GLFW.GLFW_MOUSE_BUTTON_RIGHT)),
                Component.translatable("gui.guaniao.camera_help.short.zoom"),
                Component.translatable("gui.guaniao.camera_help.short.settings", CameraKeyMappings.OPEN_CREATIVE_CONTROLS.getTranslatedKeyMessage()),
                Component.translatable("gui.guaniao.camera_help.short.filters", CameraKeyMappings.OPEN_FILTER_LIBRARY.getTranslatedKeyMessage()),
                Component.translatable("gui.guaniao.camera_help.short.focus", CameraKeyMappings.FOCUS.getTranslatedKeyMessage())
        );
    }

    static List<Component> controlLines() {
        return List.of(
                Component.translatable("gui.guaniao.camera_help.capture", mouseName(GLFW.GLFW_MOUSE_BUTTON_LEFT)),
                Component.translatable("gui.guaniao.camera_help.close", mouseName(GLFW.GLFW_MOUSE_BUTTON_RIGHT)),
                Component.translatable("gui.guaniao.camera_help.zoom"),
                Component.translatable("gui.guaniao.camera_help.settings", CameraKeyMappings.OPEN_CREATIVE_CONTROLS.getTranslatedKeyMessage()),
                Component.translatable("gui.guaniao.camera_help.filters", CameraKeyMappings.OPEN_FILTER_LIBRARY.getTranslatedKeyMessage()),
                Component.translatable("gui.guaniao.camera_help.focus", CameraKeyMappings.FOCUS.getTranslatedKeyMessage())
        );
    }

    private static Component mouseName(int button) {
        return InputConstants.Type.MOUSE.getOrCreate(button).getDisplayName();
    }

    private void drawTruncated(GuiGraphics graphics, Component text, int x, int y, int availableWidth, int color) {
        String label = text.getString();
        if (this.font.width(label) > availableWidth) {
            label = this.font.plainSubstrByWidth(label, Math.max(1, availableWidth - this.font.width("…"))) + "…";
        }
        graphics.drawString(this.font, label, x, y, color, false);
    }
}
