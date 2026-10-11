package EdDYON.guaniao.client.guide;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.FormattedCharSequence;

/** Keeps drawing, clipping and tooltip placement in the same book coordinate system. */
final class HandbookGraphics extends GuiGraphics {
    private final HandbookViewport viewport;
    private final ArrayDeque<ScreenRectangle> clips = new ArrayDeque<>();

    HandbookGraphics(Minecraft client, GuiGraphics parent, HandbookViewport viewport) {
        super(client, parent.bufferSource());
        this.viewport = viewport;
        pose().mulPoseMatrix(parent.pose().last().pose());
        pose().scale(viewport.renderScale(), viewport.renderScale(), 1);
    }

    @Override public int guiWidth() { return viewport.width(); }
    @Override public int guiHeight() { return viewport.height(); }

    // EditBox uses these same overloads for the hint, typed text and caret.
    // Retain vanilla editing/selection/IME behavior while removing its dark drop shadow.
    @Override public int drawString(Font font, String text, float x, float y, int color, boolean shadow) {
        return super.drawString(font, text, x, y, color, false);
    }
    @Override public int drawString(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow) {
        return super.drawString(font, text, x, y, color, false);
    }

    @Override public void enableScissor(int left, int top, int right, int bottom) {
        ScreenRectangle next = new ScreenRectangle(left, top, Math.max(0, right-left), Math.max(0, bottom-top));
        if (!clips.isEmpty()) {
            next = next.intersection(clips.peek());
            if (next == null) next = new ScreenRectangle(0, 0, 0, 0);
        }
        clips.push(next);
        applyClip();
    }

    @Override public void disableScissor() {
        clips.pop();
        applyClip();
    }

    private void applyClip() {
        flush();
        if (clips.isEmpty()) {
            RenderSystem.disableScissor();
        } else {
            ScreenRectangle r = clips.peek();
            int scale = viewport.pixelScale();
            RenderSystem.enableScissor(r.left()*scale, viewport.pixelHeight()-r.bottom()*scale,
                    r.width()*scale, r.height()*scale);
        }
    }
}
