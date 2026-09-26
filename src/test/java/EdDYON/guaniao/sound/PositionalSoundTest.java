package EdDYON.guaniao.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Entity sounds must be mono for OpenAL positional audio. No audio device required. */
public final class PositionalSoundTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/guaniao");

    public static void main(String[] args) throws Exception {
        JsonObject events = JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject();
        Set<String> checkedFiles = new HashSet<>();
        for (String event : events.keySet()) {
            if (event.startsWith("entity.")) {
                checkEvent(events, event, new HashSet<>(), checkedFiles);
            }
        }
        require(!checkedFiles.isEmpty(), "No entity sounds were checked");
        System.out.println("PositionalSoundTest passed: " + checkedFiles.size() + " registered entity clips are mono Vorbis");
    }

    private static void checkEvent(JsonObject events, String event, Set<String> visiting, Set<String> checkedFiles) throws Exception {
        require(visiting.add(event), "Cyclic sound event: " + event);
        require(events.has(event), "Missing sound event: " + event);
        for (JsonElement entry : events.getAsJsonObject(event).getAsJsonArray("sounds")) {
            JsonObject options = entry.isJsonObject() ? entry.getAsJsonObject() : null;
            String name = options == null ? entry.getAsString() : options.get("name").getAsString();
            // Unqualified sound names belong to Minecraft, not this mod.
            if (!name.startsWith("guaniao:")) continue;
            String path = name.substring("guaniao:".length());
            if (options != null && options.has("type") && options.get("type").getAsString().equals("event")) {
                checkEvent(events, path, visiting, checkedFiles);
            } else if (checkedFiles.add(path)) {
                checkClip(path);
            }
        }
        visiting.remove(event);
    }

    private static void checkClip(String path) throws Exception {
        byte[] header;
        try (var input = Files.newInputStream(ASSETS.resolve("sounds/" + path + ".ogg"))) {
            header = input.readNBytes(512);
        }
        require(header.length >= 58 && header[0] == 'O' && header[1] == 'g' && header[2] == 'g'
                && header[3] == 'S' && header[4] == 0, "Invalid Ogg header: " + path);
        int packet = 27 + Byte.toUnsignedInt(header[26]);
        require(packet + 30 <= header.length, "Missing Vorbis identification packet: " + path);
        byte[] signature = {1, 'v', 'o', 'r', 'b', 'i', 's'};
        for (int i = 0; i < signature.length; i++) {
            require(header[packet + i] == signature[i], "Expected Vorbis audio: " + path);
        }
        require(header[packet + 11] == 1, "Entity clip must be mono for positional audio: " + path);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
