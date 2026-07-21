package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.PhotographData;
import com.mojang.blaze3d.platform.NativeImage;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Asynchronous JPEG decoding with a small per-render-tick GPU upload budget. */
public final class PhotographTextureCache {
    private static final int MAX_TEXTURES = 96;
    private static final int MAX_UPLOADS_PER_FRAME = 2;
    private static final long UNUSED_TEXTURE_MILLIS = 60_000L;
    private static final ResourceLocation FALLBACK = new ResourceLocation(GuaniaoMod.MOD_ID, "textures/item/photograph.png");
    private static final Map<String, CachedTexture> TEXTURES = new LinkedHashMap<>(MAX_TEXTURES + 1, 0.75F, true);
    private static final Set<String> DECODING = new HashSet<>();
    private static final ArrayDeque<DecodedImage> READY = new ArrayDeque<>();
    private static final ExecutorService DECODER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Guaniao-Photo-Texture-Decode");
        thread.setDaemon(true);
        return thread;
    });
    private static int generation;

    private PhotographTextureCache() {
    }

    public static ResourceLocation textureFor(ItemStack stack) {
        if (!PhotographData.hasImage(stack)) {
            return FALLBACK;
        }
        String photoId = PhotographData.id(stack);
        String contentHash = PhotographData.contentHash(stack);
        String key = safe(photoId) + "_" + safe(contentHash);
        CachedTexture cached = TEXTURES.get(key);
        if (cached != null) {
            cached.lastUsedMillis = System.currentTimeMillis();
            return cached.location;
        }

        byte[] jpeg = PhotoClientRepository.getOrRequest(photoId, contentHash);
        if (jpeg != null && DECODING.add(key)) {
            int decodeGeneration = generation;
            DECODER.execute(() -> decode(key, jpeg, decodeGeneration));
        }
        return FALLBACK;
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
            DECODING.remove(decoded.key);
            if (decoded.generation != generation || TEXTURES.containsKey(decoded.key)) {
                decoded.image.close();
                continue;
            }
            try {
                DynamicTexture texture = new DynamicTexture(decoded.image);
                texture.upload();
                ResourceLocation location = minecraft.getTextureManager().register("guaniao_photo/" + decoded.key, texture);
                TEXTURES.put(decoded.key, new CachedTexture(location, System.currentTimeMillis()));
            } catch (RuntimeException exception) {
                decoded.image.close();
            }
        }
        evictTextures();
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        for (CachedTexture texture : TEXTURES.values()) {
            minecraft.getTextureManager().release(texture.location);
        }
        TEXTURES.clear();
        DECODING.clear();
        synchronized (READY) {
            while (!READY.isEmpty()) {
                READY.removeFirst().image.close();
            }
        }
        generation++;
        PhotoClientRepository.clear();
    }

    public static Path export(ItemStack stack) throws IOException {
        if (!PhotographData.hasImage(stack)) {
            throw new IOException("Photograph has no pixel data.");
        }
        String photoId = PhotographData.id(stack);
        byte[] jpeg = PhotoClientRepository.cached(photoId);
        if (jpeg == null) {
            PhotoClientRepository.getOrRequest(photoId, PhotographData.contentHash(stack));
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
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(jpeg));
            synchronized (READY) {
                READY.addLast(new DecodedImage(key, image, decodeGeneration));
            }
        } catch (IOException | RuntimeException exception) {
            Minecraft.getInstance().execute(() -> DECODING.remove(key));
        }
    }

    private static void evictTextures() {
        Minecraft minecraft = Minecraft.getInstance();
        long cutoff = System.currentTimeMillis() - UNUSED_TEXTURE_MILLIS;
        Iterator<Map.Entry<String, CachedTexture>> iterator = TEXTURES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CachedTexture> entry = iterator.next();
            if (TEXTURES.size() <= MAX_TEXTURES && entry.getValue().lastUsedMillis >= cutoff) {
                continue;
            }
            iterator.remove();
            minecraft.getTextureManager().release(entry.getValue().location);
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
