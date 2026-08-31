package EdDYON.guaniao.client.entity.mutation;

import EdDYON.guaniao.content.bird.mutation.BirdMutation;
import EdDYON.guaniao.content.bird.mutation.BirdMutationHolder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Generates real mutation textures at runtime by recoloring the base texture pixel-by-pixel.
 *
 * Unlike a render tint, this writes actual pixel values, so a true white albino is reachable
 * (the byte-wrapping limit only applies to per-vertex colors). Every generated image keeps the
 * base texture's luminance shading and alpha, so feathers keep their detail. Images are cached
 * per (base texture, mutation) pair.
 *
 * The rainbow mutation is a multi-frame texture sheet: an {@link AbstractTexture} whose
 * {@code bind()} uploads a new hue-scrolled frame every tick, so bands of color visibly flow
 * across the bird like streaks of shimmering light.
 */
public final class BirdMutationTextureFactory {
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    private static final int RAINBOW_FRAME_COUNT = 24;

    private BirdMutationTextureFactory() {
    }

    /**
     * Clears the generated-texture cache after a client resource reload (F3+T). The texture
     * manager discards every registered texture on reload, so the cached locations would render
     * as missing texture; clearing lets the next render regenerate and re-register them lazily.
     */
    public static void onResourceReload() {
        CACHE.clear();
    }

    /** Returns the mutation texture for the entity, or the base texture when it is not mutated. */
    public static ResourceLocation textureFor(ResourceLocation base, Entity entity) {
        if (entity instanceof BirdMutationHolder holder) {
            BirdMutation mutation = holder.getBirdMutation();
            if (mutation != BirdMutation.NONE) {
                return getTexture(base, mutation);
            }
        }
        return base;
    }

    private static ResourceLocation getTexture(ResourceLocation base, BirdMutation mutation) {
        if (mutation == BirdMutation.RAINBOW) {
            return getRainbowTexture(base);
        }
        String key = mutation.name() + "|" + base;
        ResourceLocation cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        ResourceLocation generated = generate(base, mutation);
        CACHE.put(key, generated);
        return generated;
    }

    private static ResourceLocation generate(ResourceLocation base, BirdMutation mutation) {
        try {
            NativeImage source = readSource(base);
            int width = source.getWidth();
            int height = source.getHeight();
            NativeImage out = new NativeImage(width, height, true);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    out.setPixelRGBA(x, y, recolor(source.getPixelRGBA(x, y), mutation, x, y));
                }
            }
            source.close();
            DynamicTexture texture = new DynamicTexture(out);
            texture.upload();
            String safePath = base.getPath().replace('/', '_');
            ResourceLocation location = new ResourceLocation(
                    "guaniao", "dynamic/mutations/" + mutation.name().toLowerCase() + "/" + safePath);
            Minecraft.getInstance().getTextureManager().register(location, texture);
            return location;
        } catch (IOException e) {
            return base;
        }
    }

    private static ResourceLocation getRainbowTexture(ResourceLocation base) {
        String key = "RAINBOW|" + base;
        ResourceLocation cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            NativeImage source = readSource(base);
            int width = source.getWidth();
            int height = source.getHeight();
            NativeImage[] frames = new NativeImage[RAINBOW_FRAME_COUNT];
            for (int frame = 0; frame < RAINBOW_FRAME_COUNT; frame++) {
                frames[frame] = rainbowFrame(source, width, height, frame);
            }
            source.close();
            String safePath = base.getPath().replace('/', '_');
            ResourceLocation location = new ResourceLocation("guaniao", "dynamic/mutations/rainbow/" + safePath);
            Minecraft.getInstance().getTextureManager().register(location, new RainbowAnimatedTexture(frames, width, height));
            CACHE.put(key, location);
            return location;
        } catch (IOException e) {
            return base;
        }
    }

    private static NativeImage rainbowFrame(NativeImage source, int width, int height, int frame) {
        NativeImage out = new NativeImage(width, height, true);
        float t = (float) frame / RAINBOW_FRAME_COUNT;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = source.getPixelRGBA(x, y);
                int alpha = (pixel >>> 24) & 0xFF;
                if (alpha == 0) {
                    out.setPixelRGBA(x, y, pixel);
                    continue;
                }
                int blue = (pixel >>> 16) & 0xFF;
                int green = (pixel >>> 8) & 0xFF;
                int red = pixel & 0xFF;
                int luma = (red * 299 + green * 587 + blue * 114) / 1000;
                // Two rainbow bands scroll diagonally across the body, one full cycle per loop.
                float nx = x / (float) width;
                float ny = y / (float) height;
                float phase = nx * 1.6F + ny * 0.8F - t * 2.0F;
                float hue = phase - Mth.floor(phase);
                float brightness = 0.28F + 0.82F * (luma / 255.0F);
                int rgb = Mth.hsvToRgb(hue, 0.9F, Math.min(1.0F, brightness));
                int nr = (rgb >> 16) & 0xFF;
                int ng = (rgb >> 8) & 0xFF;
                int nb = rgb & 0xFF;
                out.setPixelRGBA(x, y, (alpha << 24) | ((nb & 0xFF) << 16) | ((ng & 0xFF) << 8) | (nr & 0xFF));
            }
        }
        return out;
    }

    private static NativeImage readSource(ResourceLocation base) throws IOException {
        try (InputStream in = Minecraft.getInstance().getResourceManager()
                .getResource(base)
                .orElseThrow(() -> new IOException("Missing texture: " + base))
                .open()) {
            return NativeImage.read(in);
        }
    }

    /** Recolors one pixel. The packed int is ABGR: alpha in the top byte, then blue, green, red. */
    private static int recolor(int pixel, BirdMutation mutation, int x, int y) {
        int alpha = (pixel >>> 24) & 0xFF;
        if (alpha == 0) {
            return pixel;
        }
        int blue = (pixel >>> 16) & 0xFF;
        int green = (pixel >>> 8) & 0xFF;
        int red = pixel & 0xFF;
        int luma = (red * 299 + green * 587 + blue * 114) / 1000;

        int nr;
        int ng;
        int nb;
        switch (mutation) {
            case LEUCISTIC -> {
                // White with the base shading kept, so it reads as a clean albino, not a flat blob.
                int v = 190 + luma * 65 / 255;
                nr = v;
                ng = v;
                nb = v * 97 / 100;
            }
            case MELANISTIC -> {
                // Dark, slightly warm gray-black with shading retained.
                int v = 24 + luma * 110 / 255;
                nr = v;
                ng = v * 97 / 100;
                nb = v * 92 / 100;
            }
            case GOLDEN -> {
                // Matte gold: keeps the feather shading but never drops to black, so the whole
                // bird clearly reads as gold rather than brown.
                float f = 0.16F + 0.88F * (luma / 255.0F);
                nr = Math.min(255, (int) (255 * f));
                ng = Math.min(255, (int) (190 * f));
                nb = Math.min(255, (int) (55 * f));
            }
            case GOLDEN_PURE -> {
                // Bright, saturated "pure gold" metal: lighter floor and a hotter highlight range.
                float f = 0.30F + 0.88F * (luma / 255.0F);
                nr = Math.min(255, (int) (255 * f));
                ng = Math.min(255, (int) (208 * f));
                nb = Math.min(255, (int) (28 * f));
            }
            default -> {
                return pixel;
            }
        }
        return (alpha << 24) | ((nb & 0xFF) << 16) | ((ng & 0xFF) << 8) | (nr & 0xFF);
    }

    /**
     * Animated texture that cycles through pre-rendered rainbow frames. Uploading the current
     * frame in {@code bind()} means every render pass picks up the freshest frame for the tick,
     * which is what makes the rainbow bands appear to flow.
     */
    private static final class RainbowAnimatedTexture extends AbstractTexture {
        private final NativeImage[] frames;
        private final int frameWidth;
        private final int frameHeight;
        private boolean allocated;
        private int lastFrame = -1;

        RainbowAnimatedTexture(NativeImage[] frames, int frameWidth, int frameHeight) {
            this.frames = frames;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
        }

        @Override
        public void load(ResourceManager manager) {
        }

        @Override
        public void close() {
            for (NativeImage frame : this.frames) {
                frame.close();
            }
            super.close();
        }

        @Override
        public void bind() {
            super.bind();
            // Storage must be allocated once before any glTexSubImage2D; without this the
            // texture stays uninitialized and samples as pure black.
            if (!this.allocated) {
                TextureUtil.prepareImage(this.getId(), 0, this.frameWidth, this.frameHeight);
                this.allocated = true;
            }
            int frame = currentFrame();
            if (frame != this.lastFrame) {
                this.frames[frame].upload(0, 0, 0, false);
                this.lastFrame = frame;
            }
        }

        private static int currentFrame() {
            Level level = Minecraft.getInstance().level;
            long tick = level == null ? 0L : level.getGameTime();
            return (int) (tick % RAINBOW_FRAME_COUNT);
        }
    }
}
