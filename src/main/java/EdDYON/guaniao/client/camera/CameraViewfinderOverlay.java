package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.content.camera.CameraState;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class CameraViewfinderOverlay {
    private static final int BODY = 0xFF181817;
    private static final int EDGE = 0xFF363632;
    private static final int INK = 0xFFEBE8D8;
    private static final int MUTED = 0xFFACA99C;
    private static final int AMBER = 0xFFE3BC73;

    private CameraViewfinderOverlay() {
    }

    static void render(GuiGraphics graphics, CameraState state) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        CameraViewfinderFrame frame = CameraViewfinderFrame.layout(width, height, minecraft.font.lineHeight);
        int left = frame.left();
        int top = frame.top();
        int right = left + frame.size();
        int bottom = top + frame.size();

        CameraPreviewPostEffect.drawFilteredLens(graphics, left, top, right, bottom);
        graphics.fill(0, 0, width, top, BODY);
        graphics.fill(0, bottom, width, height, BODY);
        graphics.fill(0, top, left, bottom, BODY);
        graphics.fill(right, top, width, bottom, BODY);
        drawBezel(graphics, left, top, right, bottom);
        drawFocusMark(graphics, (left + right) / 2, (top + bottom) / 2);
        drawParameters(graphics, minecraft.font, state, left, top, right, bottom);
        if (CameraClientPreferences.showGuides()) {
            drawGuides(graphics, left, top, right, bottom);
        }
    }

    private static void drawBezel(GuiGraphics graphics, int left, int top, int right, int bottom) {
        // The original recessed pixel eyepiece stays outside the photograph.
        border(graphics, left - 6, top - 6, right + 6, bottom + 6, EDGE);
        border(graphics, left - 5, top - 5, right + 5, bottom + 5, 0xFF222221);
        border(graphics, left - 4, top - 4, right + 4, bottom + 4, 0xFF0B0B0B);
        border(graphics, left - 3, top - 3, right + 3, bottom + 3, 0xFF0B0B0B);
        border(graphics, left - 2, top - 2, right + 2, bottom + 2, 0xFF56564F);
        border(graphics, left - 1, top - 1, right + 1, bottom + 1, 0xFF0B0B0B);
        graphics.fill(left - 6, top - 6, left - 3, top - 3, BODY);
        graphics.fill(right + 3, top - 6, right + 6, top - 3, BODY);
        graphics.fill(left - 6, bottom + 3, left - 3, bottom + 6, BODY);
        graphics.fill(right + 3, bottom + 3, right + 6, bottom + 6, BODY);
    }

    private static void drawFocusMark(GuiGraphics graphics, int centerX, int centerY) {
        int radius = 7;
        int color = 0xBBEFEBDD;
        corner(graphics, centerX - radius, centerY - radius, 3, 1, 1, color);
        corner(graphics, centerX + radius, centerY - radius, 3, -1, 1, color);
        corner(graphics, centerX - radius, centerY + radius, 3, 1, -1, color);
        corner(graphics, centerX + radius, centerY + radius, 3, -1, -1, color);
    }

    private static void drawParameters(GuiGraphics graphics, Font font, CameraState state,
                                       int left, int top, int right, int bottom) {
        int size = right - left;
        int half = size / 2;
        int headerY = top - font.lineHeight - 10;
        parameter(graphics, font, Component.translatable(state.shootingMode().translationKey()).getString(),
                left, headerY, half - 4, INK, false);
        parameter(graphics, font, Math.round(state.focalLength()) + " mm",
                left + half, headerY, size - half, AMBER, true);

        String distance = state.hasInfiniteFocus() ? "∞"
                : String.format(Locale.ROOT, "%.1f m", state.focusDistance());
        String settings = state.aperture().label() + "  " + state.focusMode().shortName() + "  " + distance;
        int split = size * 2 / 3;
        parameter(graphics, font, settings, left, bottom + 10, split - 4, AMBER, false);
        parameter(graphics, font, Component.translatable(state.filter().translationKey()).getString(),
                left + split, bottom + 10, size - split, MUTED, true);
    }

    private static void parameter(GuiGraphics graphics, Font font, String value,
                                  int left, int top, int width, int color, boolean alignRight) {
        int availableWidth = Math.max(1, width);
        int textWidth = font.width(value);
        float scale = Math.min(1.0F, availableWidth / (float)Math.max(1, textWidth));
        graphics.pose().pushPose();
        graphics.pose().translate(left + (alignRight ? availableWidth - textWidth * scale : 0), top, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, value, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void border(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top + 1, left + 1, bottom - 1, color);
        graphics.fill(right - 1, top + 1, right, bottom - 1, color);
    }

    private static void drawGuides(GuiGraphics graphics, int left, int top, int right, int bottom) {
        // Four small intersections suggest thirds without drawing lines across the bird.
        for (int x = 1; x <= 2; x++) {
            for (int y = 1; y <= 2; y++) {
                int px = left + (right - left) * x / 3;
                int py = top + (bottom - top) * y / 3;
                graphics.fill(px - 2, py, px + 3, py + 1, 0x55EBE8D8);
                graphics.fill(px, py - 2, px + 1, py + 3, 0x55EBE8D8);
            }
        }
    }

    private static void corner(GuiGraphics graphics, int x, int y, int length, int dx, int dy, int color) {
        graphics.fill(Math.min(x, x + length * dx), y, Math.max(x, x + length * dx) + 1, y + 1, color);
        graphics.fill(x, Math.min(y, y + length * dy), x + 1, Math.max(y, y + length * dy) + 1, color);
    }

}
