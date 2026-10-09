package EdDYON.guaniao.client.gui.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GuiLayoutLoader {
    private static final String BIRD_GUIDE_LAYOUT_RESOURCE = "/assets/guaniao/gui/bird_guide_layout.json";

    private GuiLayoutLoader() {
    }

    public static GuiLayoutConfig loadBirdGuideLayout() {
        try (InputStream stream = GuiLayoutLoader.class.getResourceAsStream(BIRD_GUIDE_LAYOUT_RESOURCE)) {
            if (stream == null) {
                return null;
            }
            return parseLayout(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean saveBirdGuideLayout(int baseWidth, int baseHeight, Map<String, GuiLayoutRect> rects) {
        return false;
    }

    private static GuiLayoutConfig parseLayout(Reader reader) {
        try {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                return null;
            }

            JsonObject root = rootElement.getAsJsonObject();
            int baseWidth = readInt(root, "baseWidth", 0);
            int baseHeight = readInt(root, "baseHeight", 0);
            if (baseWidth <= 0 || baseHeight <= 0 || !root.has("rects") || !root.get("rects").isJsonObject()) {
                return null;
            }

            Map<String, GuiLayoutRect> rects = new LinkedHashMap<>();
            JsonObject rectRoot = root.getAsJsonObject("rects");
            for (Map.Entry<String, JsonElement> entry : rectRoot.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject rect = entry.getValue().getAsJsonObject();
                GuiLayoutRect parsed = new GuiLayoutRect(
                        readInt(rect, "x", 0),
                        readInt(rect, "y", 0),
                        readInt(rect, "w", 0),
                        readInt(rect, "h", 0));
                if (parsed.isValid()) {
                    rects.put(entry.getKey(), parsed);
                }
            }

            if (rects.isEmpty()) {
                return null;
            }

            String screen = root.has("screen") && root.get("screen").isJsonPrimitive()
                    ? root.get("screen").getAsString()
                    : "";
            return new GuiLayoutConfig(screen, baseWidth, baseHeight, rects);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int readInt(JsonObject object, String key, int fallback) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
