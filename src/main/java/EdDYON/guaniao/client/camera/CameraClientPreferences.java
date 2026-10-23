package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.client.Minecraft;

/** Presentation preferences stay local to this client, never in a camera item or server packet. */
public final class CameraClientPreferences {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long MAX_FILE_BYTES = 16384;
    private static JsonObject values;
    private static Path file;
    private static boolean writable = true;

    private CameraClientPreferences() {
    }

    public static boolean depthOfFieldEnabled() {
        return value("depthOfField");
    }

    public static boolean showGuides() {
        return value("showGuides");
    }

    public static boolean hasSeenHelp() {
        return value("helpSeen");
    }

    public static void setDepthOfFieldEnabled(boolean enabled) {
        set("depthOfField", enabled);
    }

    public static void setShowGuides(boolean enabled) {
        set("showGuides", enabled);
    }

    public static void markHelpSeen() {
        set("helpSeen", true);
    }

    static boolean isFocusKeyMigrationPending() {
        load();
        // An unreadable file must not lose either preferences or the migration marker.
        return writable && !value("focusTabMigrated");
    }

    static void markFocusKeyMigrated() {
        set("focusTabMigrated", true);
    }

    private static boolean value(String key) {
        load();
        JsonElement element = values.get(key);
        return element != null && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isBoolean() && element.getAsBoolean();
    }

    private static void load() {
        if (values != null) return;
        values = new JsonObject();
        file = Minecraft.getInstance().gameDirectory.toPath().resolve("config/guaniao-camera-client.json");
        if (!Files.exists(file)) return;
        try {
            if (Files.size(file) > MAX_FILE_BYTES) throw new IOException("Camera preferences are too large");
            values = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException exception) {
            // Retain an unreadable file for recovery; session preferences can still be changed.
            writable = false;
            GuaniaoMod.LOGGER.warn("Could not load local camera preferences; keeping the original file", exception);
        }
    }

    private static void set(String key, boolean value) {
        load();
        values.addProperty(key, value);
        if (!writable) return;
        try {
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), "guaniao-camera-", ".tmp");
            try {
                Files.writeString(temporary, JSON.toJson(values), StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            GuaniaoMod.LOGGER.warn("Could not save local camera preferences", exception);
        }
    }
}
