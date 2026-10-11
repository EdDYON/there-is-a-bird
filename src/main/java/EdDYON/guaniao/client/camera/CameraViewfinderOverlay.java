package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.content.camera.CameraState;
import EdDYON.guaniao.content.camera.PhotographData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class CameraViewfinderOverlay {
    private static final int APERTURE_PERCENT = 72;
    private static final int MASK_COLOR = 0x9A02070A;
    private static final int FRAME_COLOR = 0xDDE8F6FF;
    private static final int SOFT_FRAME_COLOR = 0x6699BCD0;
    private static final int TEXT_COLOR = 0xE6F6FFFF;

    private CameraViewfinderOverlay() {
    }

    static void render(GuiGraphics graphics, CameraState state, double fov) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int topTextReserve = font.lineHeight + 8;
        int bottomTextReserve = font.lineHeight * 3 + 12;
        int apertureSize = Math.min(apertureSize(width, height), Math.max(1, height - topTextReserve - bottomTextReserve));
        int left = (width - apertureSize) / 2;
        int availableHeight = Math.max(1, height - topTextReserve - bottomTextReserve);
        int top = topTextReserve + Math.max(0, (availableHeight - apertureSize) / 2);
        int right = left + apertureSize;
        int bottom = top + apertureSize;

        CameraPreviewPostEffect.drawFilteredLens(graphics, left, top, right, bottom);

        graphics.fill(0, 0, width, top, MASK_COLOR);
        graphics.fill(0, bottom, width, height, MASK_COLOR);
        graphics.fill(0, top, left, bottom, MASK_COLOR);
        graphics.fill(right, top, width, bottom, MASK_COLOR);

        drawFrame(graphics, left, top, right, bottom);
        drawGuides(graphics, left, top, right, bottom);

        Component modeLine = Component.translatable(
                "gui.guaniao.camera_viewfinder.focal_line",
                (int)Math.round(state.focalLength()),
                (int)Math.round(fov));
        drawCenteredFitted(graphics, font, modeLine, width, Math.max(2, top - font.lineHeight - 5), TEXT_COLOR);

        Component filterLine = Component.translatable(
                "gui.guaniao.camera_viewfinder.filter_line",
                Component.translatable(state.filter().translationKey()));
        int filterY = bottom + 5;
        drawCenteredFitted(graphics, font, filterLine, width, filterY, TEXT_COLOR);
        String focusDistance = state.hasInfiniteFocus()
                ? "∞"
                : String.format(java.util.Locale.ROOT, "%.1fm", state.focusDistance());
        Component settingsLine = Component.translatable(
                "gui.guaniao.camera_viewfinder.settings_line",
                Component.translatable(state.shootingMode().translationKey()),
                Component.translatable(state.lens().translationKey()),
                state.aperture().label(),
                state.focusMode().shortName(),
                focusDistance
        );
        int settingsY = filterY + font.lineHeight + 2;
        drawCenteredFitted(graphics, font, settingsLine, width, settingsY, 0xCCDAE7EE);
        Component hint = Component.translatable("gui.guaniao.camera_viewfinder.hint");
        int hintY = settingsY + font.lineHeight + 2;
        drawCenteredFitted(graphics, font, hint, width, hintY, 0xAABBD4DF);
    }

    static int apertureSize(int width, int height) {
        return Math.max(1, Math.min(width, height) * APERTURE_PERCENT / 100);
    }

    private static void drawCenteredFitted(
            GuiGraphics graphics,
            Font font,
            Component text,
            int screenWidth,
            int y,
            int color
    ) {
        int availableWidth = Math.max(1, screenWidth - 12);
        int textWidth = font.width(text);
        float scale = textWidth <= availableWidth ? 1.0F : Math.max(0.55F, availableWidth / (float)Math.max(1, textWidth));
        float scaledWidth = textWidth * scale;
        graphics.pose().pushPose();
        graphics.pose().translate((screenWidth - scaledWidth) / 2.0F, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void drawFrame(GuiGraphics graphics, int left, int top, int right, int bottom) {
        graphics.fill(left - 1, top - 1, right + 1, top + 1, SOFT_FRAME_COLOR);
        graphics.fill(left - 1, bottom - 1, right + 1, bottom + 1, SOFT_FRAME_COLOR);
        graphics.fill(left - 1, top - 1, left + 1, bottom + 1, SOFT_FRAME_COLOR);
        graphics.fill(right - 1, top - 1, right + 1, bottom + 1, SOFT_FRAME_COLOR);

        int corner = Math.max(24, (right - left) / 10);
        graphics.fill(left - 2, top - 2, left + corner, top + 2, FRAME_COLOR);
        graphics.fill(left - 2, top - 2, left + 2, top + corner, FRAME_COLOR);
        graphics.fill(right - corner, top - 2, right + 2, top + 2, FRAME_COLOR);
        graphics.fill(right - 2, top - 2, right + 2, top + corner, FRAME_COLOR);
        graphics.fill(left - 2, bottom - 2, left + corner, bottom + 2, FRAME_COLOR);
        graphics.fill(left - 2, bottom - corner, left + 2, bottom + 2, FRAME_COLOR);
        graphics.fill(right - corner, bottom - 2, right + 2, bottom + 2, FRAME_COLOR);
        graphics.fill(right - 2, bottom - corner, right + 2, bottom + 2, FRAME_COLOR);
    }

    private static void drawGuides(GuiGraphics graphics, int left, int top, int right, int bottom) {
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;
        graphics.fill(centerX - 10, centerY, centerX - 3, centerY + 1, FRAME_COLOR);
        graphics.fill(centerX + 3, centerY, centerX + 10, centerY + 1, FRAME_COLOR);
        graphics.fill(centerX, centerY - 10, centerX + 1, centerY - 3, FRAME_COLOR);
        graphics.fill(centerX, centerY + 3, centerX + 1, centerY + 10, FRAME_COLOR);

        int third = (right - left) / 3;
        int lineColor = 0x3399BCD0;
        graphics.fill(left + third, top, left + third + 1, bottom, lineColor);
        graphics.fill(right - third, top, right - third + 1, bottom, lineColor);
        graphics.fill(left, top + third, right, top + third + 1, lineColor);
        graphics.fill(left, bottom - third, right, bottom - third + 1, lineColor);

        int photoSize = PhotographData.IMAGE_SIZE;
        String marker = photoSize + "x" + photoSize;
        graphics.drawString(Minecraft.getInstance().font, marker, right - Minecraft.getInstance().font.width(marker) - 6, bottom - 12, 0x7799BCD0, false);
    }
}
