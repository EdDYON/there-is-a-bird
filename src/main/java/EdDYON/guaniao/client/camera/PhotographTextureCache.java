package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.PhotographData;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.jetbrains.annotations.Nullable;

/** Asynchronous JPEG decoding with a small per-render-tick GPU upload budget. */
public final class PhotographTextureCache {
    // At full capture resolution this bounds GPU storage to 1 GiB. Uploaded
    // photos do not keep a second uncompressed copy in native system memory.
    private static final int MAX_TEXTURES = 256;
    private static final int MAX_PENDING_DECODES = 8;
    private static final int MAX_UPLOADS_PER_FRAME = 2;
    private static final long UNUSED_TEXTURE_MILLIS = 60_000L;
    private static final ResourceLocation FALLBACK = new ResourceLocation(GuaniaoMod.MOD_ID, "textures/item/photograph.png");
    private static final Map<String, CachedTexture> TEXTURES = new LinkedHashMap<>(MAX_TEXTURES + 1, 0.75F, true);
    private static final Set<String> DECODING = new HashSet<>();
    /** Keys whose jpeg could not be decoded; retried only after a logout clears it. */
    private static final Set<String> FAILED = new HashSet<>();
    private static final ArrayDeque<DecodedImage> READY = new ArrayDeque<>();
    private static final ArrayDeque<ResourceLocation> AVAILABLE_SLOTS = new ArrayDeque<>();
    private static final ThreadPoolExecutor DECODER = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_PENDING_DECODES), runnable -> {
        Thread thread = new Thread(runnable, "Guaniao-Photo-Texture-Decode");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile int generation;

    static {
        resetSlots();
    }

    private PhotographTextureCache() {
    }

    public static ResourceLocation textureFor(ItemStack stack) {
        ResourceLocation texture = readyTextureFor(stack);
        return texture == null ? FALLBACK : texture;
    }

    /** Requests the original image without substituting an unrelated item icon. */
    @Nullable
    public static ResourceLocation readyTextureFor(ItemStack stack) {
        if (!PhotographData.hasImage(stack)) {
            return null;
        }
        String photoId = PhotographData.id(stack);
        String contentHash = PhotographData.contentHash(stack);
        // This key never becomes a resource path. Avoid two regex replacements
        // per draw, and keep case-sensitive photo identities distinct.
        String key = photoId + ":" + contentHash;
        CachedTexture cached = TEXTURES.get(key);
        if (cached != null) {
            cached.lastUsedMillis = System.currentTimeMillis();
            return cached.location;
        }

        byte[] jpeg = PhotoClientRepository.getOrRequest(photoId, contentHash);
        if (jpeg != null) imageAvailable(photoId, contentHash, jpeg);
        return null;
    }

    public static void preload(ItemStack stack) {
        readyTextureFor(stack);
    }

    public static void tickHeldPhotographs() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        // Include exposed film, whose held/inventory renderer uses a fixed icon.
        // Never download the entire inventory or world just to open one photo.
        preload(player.getMainHandItem());
        preload(player.getOffhandItem());
    }

    static void imageAvailable(String photoId, String contentHash, byte[] jpeg) {
        String key = photoId + ":" + contentHash;
        if (!TEXTURES.containsKey(key) && DECODING.size() < MAX_PENDING_DECODES
                && !FAILED.contains(key) && DECODING.add(key)) {
            int decodeGeneration = generation;
            DECODER.execute(() -> decode(key, jpeg, decodeGeneration));
        }
    }

    public static void pumpUploads() {
        Minecraft minecraft = Minecraft.getInstance();
        for (int uploaded = 0; uploaded < MAX_UPLOADS_PER_FRAME; uploaded++) {
            DecodedImage decoded;
            synchronized (READY) {
                decoded = READY.pollFirst();
            }
            if (decoded == null) {
                break;
            }
            if (decoded.generation != generation) {
                decoded.image.close();
                continue;
            }
            DECODING.remove(decoded.key);
            if (TEXTURES.containsKey(decoded.key)) {
                decoded.image.close();
                continue;
            }
            ResourceLocation location = reserveSlot();
            PhotoTexture texture = null;
            try {
                texture = new PhotoTexture(decoded.image);
                minecraft.getTextureManager().register(location, texture);
                TEXTURES.put(decoded.key, new CachedTexture(location, System.currentTimeMillis()));
            } catch (RuntimeException exception) {
                if (texture != null) texture.close();
                AVAILABLE_SLOTS.addLast(location);
                FAILED.add(decoded.key);
                GuaniaoMod.LOGGER.warn("Could not upload photograph texture {}", decoded.key, exception);
            } finally {
                decoded.image.close();
            }
        }
        evictTextures();
    }

    public static void clear() {
        generation++;
        DECODER.getQueue().clear();
        Minecraft minecraft = Minecraft.getInstance();
        for (CachedTexture texture : TEXTURES.values()) {
            minecraft.getTextureManager().release(texture.location);
        }
        TEXTURES.clear();
        resetSlots();
        DECODING.clear();
        FAILED.clear();
        synchronized (READY) {
            while (!READY.isEmpty()) {
                READY.removeFirst().image.close();
            }
        }
        PhotoClientRepository.clear();
    }

    public static Path export(ItemStack stack) throws IOException {
        if (!PhotographData.hasImage(stack)) {
            throw new IOException("Photograph has no pixel data.");
        }
        String photoId = PhotographData.id(stack);
        String contentHash = PhotographData.contentHash(stack);
        byte[] jpeg = PhotoClientRepository.getOrRequest(photoId, contentHash);
        if (jpeg == null) {
            throw new IOException("Photograph is still downloading.");
        }
        Path directory = Minecraft.getInstance().gameDirectory.toPath().resolve("guaniao_photos");
        Files.createDirectories(directory);
        Path file = directory.resolve(safe(photoId) + ".png");
        try (NativeImage image = NativeImage.read(new ByteArrayInputStream(jpeg))) {
            image.writeToFile(file);
        }
        return file;
    }

    private static void decode(String key, byte[] jpeg, int decodeGeneration) {
        if (decodeGeneration != generation) return;
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(jpeg));
            synchronized (READY) {
                if (decodeGeneration == generation) READY.addLast(new DecodedImage(key, image, decodeGeneration));
                else image.close();
            }
        } catch (IOException | RuntimeException exception) {
            // Mark the key failed so a broken photo cannot resubmit itself to the
            // decoder every frame.
            Minecraft.getInstance().execute(() -> finishDecodeFailure(key, decodeGeneration));
        }
    }

    private static void finishDecodeFailure(String key, int decodeGeneration) {
        if (decodeGeneration != generation) {
            return;
        }
        DECODING.remove(key);
        FAILED.add(key);
    }

    private static void evictTextures() {
        Minecraft minecraft = Minecraft.getInstance();
        long cutoff = System.currentTimeMillis() - UNUSED_TEXTURE_MILLIS;
        Iterator<Map.Entry<String, CachedTexture>> iterator = TEXTURES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CachedTexture> entry = iterator.next();
            if (entry.getValue().lastUsedMillis >= cutoff) {
                continue;
            }
            iterator.remove();
            minecraft.getTextureManager().release(entry.getValue().location);
            AVAILABLE_SLOTS.addLast(entry.getValue().location);
        }
    }

    private static ResourceLocation reserveSlot() {
        if (!AVAILABLE_SLOTS.isEmpty()) return AVAILABLE_SLOTS.removeFirst();
        Iterator<CachedTexture> iterator = TEXTURES.values().iterator();
        CachedTexture oldest = iterator.next();
        iterator.remove();
        Minecraft.getInstance().getTextureManager().release(oldest.location);
        return oldest.location;
    }

    private static void resetSlots() {
        AVAILABLE_SLOTS.clear();
        for (int slot = 0; slot < MAX_TEXTURES; slot++) {
            AVAILABLE_SLOTS.addLast(new ResourceLocation(GuaniaoMod.MOD_ID, "dynamic/guaniao_photo/" + slot));
        }
    }

    /** Immutable pixels live on the GPU; export continues to use the original JPEG. */
    private static final class PhotoTexture extends AbstractTexture implements Dumpable {
        private final int width;
        private final int height;

        private PhotoTexture(NativeImage image) {
            width = image.getWidth();
            height = image.getHeight();
            try {
                TextureUtil.prepareImage(getId(), width, height);
                bind();
                image.upload(0, 0, 0, false);
                setFilter(false, false);
            } catch (RuntimeException exception) {
                releaseId();
                throw exception;
            }
        }

        @Override
        public void load(ResourceManager resources) {
            // Like DynamicTexture, retain the uploaded image across resource reloads.
        }

        @Override
        public void close() {
            releaseId();
        }

        @Override
        public void dumpContents(ResourceLocation location, Path directory) throws IOException {
            int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            try (NativeImage image = new NativeImage(width, height, false)) {
                bind();
                image.downloadTexture(0, false);
                image.writeToFile(directory.resolve(location.toDebugFileName() + ".png"));
            } finally {
                RenderSystem.bindTexture(previous);
            }
        }
    }

    private static String safe(String id) {
        String safe = id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return safe.isEmpty() ? "photograph" : safe;
    }

    private static final class CachedTexture {
        private final ResourceLocation location;
        private long lastUsedMillis;

        private CachedTexture(ResourceLocation location, long lastUsedMillis) {
            this.location = location;
            this.lastUsedMillis = lastUsedMillis;
        }
    }

    private record DecodedImage(String key, NativeImage image, int generation) {
    }
}
