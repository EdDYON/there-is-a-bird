package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.PhotographData;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class PhotographTextureCache {
    private static final int MAX_TEXTURES = 64;
    private static final ResourceLocation FALLBACK = new ResourceLocation(GuaniaoMod.MOD_ID, "textures/item/photograph.png");
    private static final Map<String, ResourceLocation> TEXTURES = new LinkedHashMap<>(MAX_TEXTURES + 1, 0.75F, true);

    private PhotographTextureCache() {
    }

    public static ResourceLocation textureFor(ItemStack stack) {
        try {
            if (!PhotographData.hasImage(stack)) {
                return FALLBACK;
            }

            int[] pixels = PhotographData.pixels(stack);
            int width = PhotographData.width(stack);
            int height = PhotographData.height(stack);
            if (width <= 0 || height <= 0 || pixels.length != width * height) {
                return FALLBACK;
            }

            String key = safe(PhotographData.id(stack)) + "_" + width + "x" + height;
            ResourceLocation cached = TEXTURES.get(key);
            if (cached != null) {
                return cached;
            }

            NativeImage image = toNativeImage(pixels, width, height);
            DynamicTexture texture = new DynamicTexture(image);
            texture.upload();
            ResourceLocation registered = Minecraft.getInstance().getTextureManager().register("guaniao_photo/" + key, texture);
            TEXTURES.put(key, registered);
            evictOldestTextures();
            return registered;
        } catch (RuntimeException exception) {
            return FALLBACK;
        }
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        for (ResourceLocation texture : TEXTURES.values()) {
            minecraft.getTextureManager().release(texture);
        }
        TEXTURES.clear();
    }

    public static Path export(ItemStack stack) throws IOException {
        if (!PhotographData.hasImage(stack)) {
            throw new IOException("Photograph has no pixel data.");
        }

        Path directory = Minecraft.getInstance().gameDirectory.toPath().resolve("guaniao_photos");
        Files.createDirectories(directory);
        Path file = directory.resolve(safe(PhotographData.id(stack)) + ".png");
        try (NativeImage image = toNativeImage(PhotographData.pixels(stack), PhotographData.width(stack), PhotographData.height(stack))) {
            image.writeToFile(file);
        }
        return file;
    }

    private static NativeImage toNativeImage(int[] pixels, int width, int height) {
        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixelRGBA(x, y, pixels[y * width + x]);
            }
        }
        return image;
    }

    private static void evictOldestTextures() {
        Iterator<ResourceLocation> iterator = TEXTURES.values().iterator();
        Minecraft minecraft = Minecraft.getInstance();
        while (TEXTURES.size() > MAX_TEXTURES && iterator.hasNext()) {
            ResourceLocation texture = iterator.next();
            iterator.remove();
            minecraft.getTextureManager().release(texture);
        }
    }

    private static String safe(String id) {
        String safe = id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return safe.isEmpty() ? "photograph" : safe;
    }
}
