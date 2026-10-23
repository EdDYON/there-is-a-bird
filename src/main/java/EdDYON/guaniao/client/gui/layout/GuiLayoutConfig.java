package EdDYON.guaniao.client.gui.layout;

import java.util.Map;

public final class GuiLayoutConfig {
    private final String screen;
    private final int baseWidth;
    private final int baseHeight;
    private final Map<String, GuiLayoutRect> rects;

    public GuiLayoutConfig(String screen, int baseWidth, int baseHeight, Map<String, GuiLayoutRect> rects) {
        this.screen = screen;
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.rects = Map.copyOf(rects);
    }

    public String screen() {
        return this.screen;
    }

    public int baseWidth() {
        return this.baseWidth;
    }

    public int baseHeight() {
        return this.baseHeight;
    }

    public Map<String, GuiLayoutRect> rects() {
        return this.rects;
    }

    public GuiLayoutRect rect(String id, GuiLayoutRect fallback, int screenWidth, int screenHeight) {
        if (this.baseWidth <= 0 || this.baseHeight <= 0) {
            return fallback;
        }

        GuiLayoutRect raw = this.rects.get(id);
        if (raw == null || !raw.isValid()) {
            return fallback;
        }

        float scaleX = screenWidth / (float)this.baseWidth;
        float scaleY = screenHeight / (float)this.baseHeight;
        GuiLayoutRect scaled = raw.scale(scaleX, scaleY);
        return scaled.isValid() ? scaled : fallback;
    }
}
