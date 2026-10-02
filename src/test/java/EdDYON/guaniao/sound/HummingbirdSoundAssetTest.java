package EdDYON.guaniao.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Checks only the newly registered hummingbird audio; no sound device or whole-mod scan. */
public final class HummingbirdSoundAssetTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/guaniao");
    private static final String[] EVENTS = {"entity.hummingbird.ambient", "entity.hummingbird.wing",
            "entity.hummingbird.hurt", "entity.hummingbird.death", "entity.hummingbird.interact"};
    private static int checks;

    public static void main(String[] args) throws Exception {
        JsonObject sounds = JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject();
        Set<String> paths = new HashSet<>();
        Map<String, Double> durations = new HashMap<>();
        double longestCall = 0;
        for (String event : EVENTS) {
            require(sounds.has(event), "Missing registered hummingbird event " + event);
            JsonObject definition = sounds.getAsJsonObject(event);
            require(definition.has("subtitle"), "Hummingbird audio has a subtitle: " + event);
            require(definition.get("subtitle").getAsString().equals("subtitles.guaniao." + event),
                    "Each event uses its own subtitle: " + event);
            boolean ambient = event.endsWith("ambient");
            boolean wing = event.endsWith("wing");
            require(definition.getAsJsonArray("sounds").size() == (ambient ? 5 : 1),
                    "Five ambient calls; one wing or short feedback clip per event");
            int index = 0;
            for (JsonElement entry : definition.getAsJsonArray("sounds")) {
                JsonObject clip = entry.getAsJsonObject();
                String name = clip.get("name").getAsString();
                require(name.startsWith("guaniao:entity/hummingbird/") && !name.contains(".."),
                        "Clip resolves inside the hummingbird sound directory: " + name);
                String expected = "guaniao:entity/hummingbird/" + (ambient ? "call_" + ++index : wing ? "wing_loop" : "call_5");
                require(name.equals(expected), "Ambient calls and wing loop remain intact; feedback reuses call_5: " + event);
                require(!clip.has("type") || clip.get("type").getAsString().equals("file"), "Direct Ogg clip, not an unresolved event alias");
                require(!clip.has("stream") || !clip.get("stream").getAsBoolean(), "Short nearby sounds do not need streaming");
                require(!clip.has("pitch") || clip.get("pitch").getAsDouble() == 1,
                        "Asset pitch must not silently extend the server's overlap guard");
                require(clip.has("attenuation_distance") && clip.get("attenuation_distance").getAsInt() == (wing ? 16 : 32),
                        "Calls cover the garden while wing beats remain close-range positional audio");
                boolean unique = paths.add(name);
                require(ambient || wing ? unique : !unique,
                        "Only feedback events reuse the existing short call_5 recording");
                if (unique) {
                    Path path = ASSETS.resolve("sounds/" + name.substring("guaniao:".length()) + ".ogg");
                    durations.put(name, monoVorbisSeconds(Files.readAllBytes(path), name));
                }
                double seconds = durations.get(name);
                if (ambient) {
                    require(seconds >= 0.5 && seconds <= 2.5, "Ambient recording is a short complete call: " + name + " / " + seconds);
                    require(Math.ceil(seconds / .95 * 20) < 60,
                            "Even the slowest allowed pitch finishes before the 60-tick anti-overlap floor");
                    longestCall = Math.max(longestCall, seconds);
                } else if (wing) {
                    require(seconds >= 1 && seconds <= 1.5, "Wing recording is one compact loop: " + seconds);
                } else {
                    require(Math.ceil(seconds / .95 * 20) <= 24,
                            "Feedback finishes within 24 ticks at the slowest allowed pitch: " + event);
                }
                System.out.println(name + ": " + seconds + " seconds, mono Vorbis");
            }
        }
        require(paths.size() == 6, "Five events share exactly six unique Ogg recordings");
        for (String locale : new String[]{"zh_cn", "en_us"}) {
            JsonObject language = JsonParser.parseString(Files.readString(ASSETS.resolve("lang/" + locale + ".json"))).getAsJsonObject();
            for (String event : EVENTS) {
                String key = sounds.getAsJsonObject(event).get("subtitle").getAsString();
                require(language.has(key) && !language.get(key).getAsString().isBlank(), "Localized subtitle exists: " + locale + " / " + key);
            }
        }
        System.out.println("HummingbirdSoundAssetTest passed: " + checks + " checks, five events, six unique clips, longest call " + longestCall + " seconds");
    }

    /** Duration comes from Ogg page granules, independently of an exporter's metadata report. */
    private static double monoVorbisSeconds(byte[] bytes, String name) {
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int offset = 0, rate = 0, serial = 0, sequence = 0;
        long lastGranule = -1;
        boolean ended = false;
        while (offset < bytes.length) {
            require(offset + 27 <= bytes.length && bytes[offset] == 'O' && bytes[offset + 1] == 'g'
                    && bytes[offset + 2] == 'g' && bytes[offset + 3] == 'S' && bytes[offset + 4] == 0,
                    "Complete Ogg page header: " + name);
            int segments = Byte.toUnsignedInt(bytes[offset + 26]);
            require(offset + 27 + segments <= bytes.length, "Complete Ogg segment table");
            int body = offset + 27 + segments, payload = 0;
            for (int i = 0; i < segments; i++) payload += Byte.toUnsignedInt(bytes[offset + 27 + i]);
            require(body + payload <= bytes.length, "Ogg payload is not truncated");
            if (offset == 0) {
                require((bytes[offset + 5] & 2) != 0 && payload >= 30, "Vorbis beginning-of-stream page");
                byte[] signature = {1, 'v', 'o', 'r', 'b', 'i', 's'};
                for (int i = 0; i < signature.length; i++) require(bytes[body + i] == signature[i], "Vorbis identification packet");
                require(bytes[body + 11] == 1, "OpenAL spatial audio requires mono: " + name);
                rate = data.getInt(body + 12);
                require(rate >= 22050 && rate <= 96000, "Usable Vorbis sample rate: " + rate);
                serial = data.getInt(offset + 14);
            }
            require(data.getInt(offset + 14) == serial && data.getInt(offset + 18) == sequence++, "One ordered Ogg logical stream");
            long granule = data.getLong(offset + 6);
            if (granule >= 0) lastGranule = Math.max(lastGranule, granule);
            ended = (bytes[offset + 5] & 4) != 0;
            offset = body + payload;
            require(!ended || offset == bytes.length, "No hidden concatenated recording after the end marker");
        }
        require(ended && lastGranule > 0 && rate > 0, "Complete audible recording with a final sample count");
        return lastGranule / (double)rate;
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
