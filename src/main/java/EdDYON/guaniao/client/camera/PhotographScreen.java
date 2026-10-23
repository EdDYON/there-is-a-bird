package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.PhotographData;
import EdDYON.guaniao.content.camera.PhotoAuthorText;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PhotographScreen extends Screen {
    private final ItemStack photograph;
    private int imageX;
    private int imageY;
    private int imageSize;
    private int titleY;
    private int captionY;
    private int buttonY;

    public PhotographScreen(ItemStack photograph) {
        super(Component.translatable("gui.guaniao.photograph.title"));
        this.photograph = photograph;
    }

    @Override
    protected void init() {
        PhotographTextureCache.preload(this.photograph);
        // Fit the picture and its controls as one group, including after GUI-scale changes.
        int controlsHeight = this.font.lineHeight * 2 + 7 + 10 + 9 + 20;
        this.imageSize = Math.max(1, Math.min(PhotographData.IMAGE_SIZE,
                Math.min(this.width - 32, this.height - controlsHeight - 24)));
        this.imageX = (this.width - this.imageSize) / 2;
        this.titleY = (this.height - this.imageSize - controlsHeight) / 2;
        this.imageY = this.titleY + this.font.lineHeight + 7;
        this.captionY = this.imageY + this.imageSize + 10;
        this.buttonY = this.captionY + this.font.lineHeight + 9;
        int buttonWidth = Math.min(88, Math.max(40, (this.imageSize - 8) / 2));
        this.addRenderableWidget(Button.builder(Component.translatable("gui.guaniao.photograph.export"), button -> this.export())
                .bounds(this.width / 2 - buttonWidth - 4, this.buttonY, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.guaniao.photograph.close"), button -> this.onClose())
                .bounds(this.width / 2 + 4, this.buttonY, buttonWidth, 20)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int textureWidth = Math.max(1, PhotographData.width(this.photograph));
        int textureHeight = Math.max(1, PhotographData.height(this.photograph));
        int x = this.imageX;
        int y = this.imageY;
        int size = this.imageSize;

        drawCenteredLabel(graphics, this.title, this.titleY, 0xD4D8DB);
        // Two small pixel steps lift the photograph off the world without a heavy mat.
        graphics.fill(x + 2, y + 2, x + size + 4, y + size + 4, 0x20000000);
        graphics.fill(x + 1, y + 1, x + size + 2, y + size + 2, 0x50000000);
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF1A1D20);
        graphics.fill(x, y, x + size, y + size, 0xFF2A2A2A);

        ResourceLocation texture = PhotographTextureCache.readyTextureFor(this.photograph);
        if (texture != null) {
            blitFullTexture(graphics, texture, x, y, size, textureWidth, textureHeight);
        }

        String photographer = PhotographData.photographer(this.photograph);
        if (!photographer.isEmpty()) {
            drawCenteredLabel(graphics, Component.translatable("item.guaniao.photograph.tooltip.photographer",
                    PhotoAuthorText.component(photographer)),
                    this.captionY, 0xB4C1C6);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawCenteredLabel(GuiGraphics graphics, Component label, int y, int color) {
        FormattedText text = label;
        if (this.font.width(text) > this.imageSize) {
            String suffix = "…";
            text = FormattedText.composite(this.font.substrByWidth(text, Math.max(0, this.imageSize - this.font.width(suffix))),
                    Component.literal(suffix));
        }
        graphics.drawString(this.font, Language.getInstance().getVisualOrder(text),
                this.width / 2 - this.font.width(text) / 2, y, color, false);
    }

    private static void blitFullTexture(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int imageSize,
            int textureWidth,
            int textureHeight
    ) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0.0F);
            graphics.pose().scale(
                    imageSize / (float) textureWidth,
                    imageSize / (float) textureHeight,
                    1.0F
            );
            graphics.blit(texture, 0, 0, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
        } finally {
            graphics.pose().popPose();
        }
    }

    private void export() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        try {
            PhotographTextureCache.export(this.photograph);
        } catch (IOException exception) {
            GuaniaoMod.LOGGER.warn("Failed to export photograph", exception);
        }
    }
}
