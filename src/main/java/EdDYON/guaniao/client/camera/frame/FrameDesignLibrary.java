package EdDYON.guaniao.client.camera.frame;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.frame.FrameDesign;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.TagParser;

/** Local designs contain frame appearance only, never photo identities or image data. */
public final class FrameDesignLibrary {
    public static final int LIMIT = 24;
    private static final int MAX_FILE_BYTES = 262144;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object FILE_WRITE_LOCK = new Object();
    private final List<Entry> entries = new ArrayList<>();
    private final Path file;

    public FrameDesignLibrary() {
        this.file = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("guaniao-frame-designs.json");
        if (!Files.isRegularFile(this.file)) return;
        try {
            if (Files.size(this.file) > MAX_FILE_BYTES) throw new IOException("Frame design library is too large");
            JsonObject root = JsonParser.parseString(Files.readString(this.file, StandardCharsets.UTF_8)).getAsJsonObject();
            int version=root.has("version") ? root.get("version").getAsInt() : 1;
            if(version<1 || version>3) throw new IOException("Unsupported frame design library version");
            JsonArray saved = root.getAsJsonArray("designs");
            if (saved == null) return;
            for (JsonElement element : saved) {
                if (this.entries.size() >= LIMIT) break;
                try {
                    JsonObject entry = element.getAsJsonObject();
                    String name = cleanName(entry.get("name").getAsString());
                    String text = entry.get("design").getAsString();
                    if (name.isBlank() || text.length() > 16384) continue;
                    FrameDesign design = FrameDesign.fromTag(TagParser.parseTag(text));
                    if (design != null && this.entries.stream().noneMatch(old -> old.name.equals(name))) {
                        this.entries.add(new Entry(name, design));
                    }
                } catch (Exception malformedEntry) {
                    GuaniaoMod.LOGGER.warn("Ignored a malformed local frame design: {}", malformedEntry.getMessage());
                }
            }
        } catch (IOException | RuntimeException exception) {
            GuaniaoMod.LOGGER.warn("Could not load local frame designs", exception);
        }
    }

    public List<Entry> entries() { return List.copyOf(this.entries); }

    public void save(String rawName, FrameDesign design) throws IOException {
        String name = cleanName(rawName);
        if (name.isBlank()) throw new IOException("A design name is required");
        List<Entry> updated = new ArrayList<>(this.entries);
        int existing = -1;
        for (int i=0; i<updated.size(); i++) if (updated.get(i).name.equals(name)) existing=i;
        if (existing >= 0) updated.set(existing, new Entry(name,design));
        else {
            if (updated.size() >= LIMIT) throw new IOException("The local design library is full");
            updated.add(new Entry(name,design));
        }
        this.persist(updated);
    }

    public void delete(int index) throws IOException {
        if (index<0 || index>=this.entries.size()) return;
        List<Entry> updated = new ArrayList<>(this.entries);
        updated.remove(index);
        this.persist(updated);
    }

    /** Re-read by identity so a stale page cannot delete a renamed or replaced design. */
    public void delete(Entry expected) throws IOException {
        if (expected == null || expected.name() == null || expected.design() == null
                || !Files.isRegularFile(this.file, LinkOption.NOFOLLOW_LINKS) || Files.size(this.file) > MAX_FILE_BYTES) {
            throw new IOException("Local frame design is no longer available");
        }
        String original = Files.readString(this.file, StandardCharsets.UTF_8);
        try {
            JsonObject root = JsonParser.parseString(original).getAsJsonObject();
            int version = root.has("version") ? root.get("version").getAsInt() : 1;
            JsonArray saved = root.getAsJsonArray("designs");
            if (version < 1 || version > 3 || saved == null) throw new IOException("Invalid local frame design library");
            int selected = -1;
            for (int i = 0; i < saved.size(); i++) {
                if (!saved.get(i).isJsonObject()) continue;
                JsonObject entry = saved.get(i).getAsJsonObject();
                JsonElement name = entry.get("name");
                if (name == null || !name.isJsonPrimitive() || !name.getAsJsonPrimitive().isString()) continue;
                if (!expected.name().equals(cleanName(name.getAsString()))) continue;
                String text = entry.get("design").getAsString();
                if (selected >= 0 || text.length() > 16384
                        || !expected.design().equals(FrameDesign.fromTag(TagParser.parseTag(text)))) {
                    throw new IOException("Local frame design changed; refresh before deleting");
                }
                selected = i;
            }
            if (selected < 0) throw new IOException("Local frame design is no longer available");
            saved.remove(selected);
            this.persistJson(JSON.toJson(root), original);
            FrameDesignLibrary refreshed = new FrameDesignLibrary();
            this.entries.clear();
            this.entries.addAll(refreshed.entries);
        } catch (IOException exception) {
            throw exception;
        } catch (Exception malformed) {
            throw new IOException("Invalid local frame design library", malformed);
        }
    }

    private void persist(List<Entry> updated) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version",3);
        JsonArray saved = new JsonArray();
        for (Entry entry : updated) {
            JsonObject value = new JsonObject();
            value.addProperty("name",entry.name);
            value.addProperty("design",entry.design.toTag().toString());
            saved.add(value);
        }
        root.add("designs",saved);
        String json=JSON.toJson(root);
        this.persistJson(json, null);
        this.entries.clear();
        this.entries.addAll(updated);
    }

    private void persistJson(String json, String expectedContents) throws IOException {
        if(json.getBytes(StandardCharsets.UTF_8).length>MAX_FILE_BYTES) throw new IOException("Frame design library is too large");
        Files.createDirectories(this.file.getParent());
        Path temporary = Files.createTempFile(this.file.getParent(),"guaniao-frame-designs-",".tmp");
        try {
            Files.writeString(temporary,json,StandardCharsets.UTF_8);
            synchronized (FILE_WRITE_LOCK) {
                if (expectedContents != null && (!Files.isRegularFile(this.file, LinkOption.NOFOLLOW_LINKS)
                        || Files.size(this.file) > MAX_FILE_BYTES
                        || !expectedContents.equals(Files.readString(this.file, StandardCharsets.UTF_8)))) {
                    throw new IOException("Local frame design library changed; refresh before deleting");
                }
                try {
                    Files.move(temporary,this.file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary,this.file,StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String cleanName(String name) {
        String cleaned=name.replaceAll("[\\p{Cntrl}]","").strip();
        return cleaned.substring(0,Math.min(32,cleaned.length()));
    }

    public record Entry(String name, FrameDesign design) { }
}
