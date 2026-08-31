package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.content.camera.PhotographData;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PhotographScreen extends Screen {
    private final ItemStack photograph;

    public PhotographScreen(ItemStack photograph) {
        super(Component.translatable("gui.guaniao.photograph.title"));
        this.photograph = photograph;
    }

    @Override
    protected void init() {
        int buttonY = this.height - 34;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.guaniao.photograph.export"), button -> this.export())
                .bounds(this.width / 2 - 94, buttonY, 88, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.guaniao.photograph.close"), button -> this.onClose())
                .bounds(this.width / 2 + 6, buttonY, 88, 20)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int textureWidth = Math.max(1, PhotographData.width(this.photograph));
        int textureHeight = Math.max(1, PhotographData.height(this.photograph));
        int imageSize = Math.min(PhotographData.IMAGE_SIZE, Math.min(this.width - 48, this.height - 96));
        int x = (this.width - imageSize) / 2;
        int y = 36;

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xE8F5FF);
        graphics.fill(x - 8, y - 8, x + imageSize + 8, y + imageSize + 8, 0xFFE9E1D1);
        graphics.fill(x - 4, y - 4, x + imageSize + 4, y + imageSize + 4, 0xFF2A2A2A);

        ResourceLocation texture = PhotographTextureCache.textureFor(this.photograph);
        blitFullTexture(graphics, texture, x, y, imageSize, textureWidth, textureHeight);

        String photographer = PhotographData.photographer(this.photograph);
        if (!photographer.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("item.guaniao.photograph.tooltip.photographer", photographer), this.width / 2, y + imageSize + 12, 0xB8D7E6);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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
            Path file = PhotographTextureCache.export(this.photograph);
            this.minecraft.player.displayClientMessage(Component.translatable("gui.guaniao.photograph.exported", file.toString()).withStyle(ChatFormatting.GREEN), false);
        } catch (IOException exception) {
            this.minecraft.player.displayClientMessage(Component.translatable("gui.guaniao.photograph.export_failed").withStyle(ChatFormatting.RED), false);
        }
    }
}
