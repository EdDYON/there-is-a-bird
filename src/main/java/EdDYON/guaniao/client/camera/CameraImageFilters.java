package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.content.camera.CameraFilter;
import net.minecraft.util.Mth;

final class CameraImageFilters {
    private static final float SHARPEN_STRENGTH = 0.16F;

    private CameraImageFilters() {
    }

    static void apply(int[] pixels, int width, int height, CameraFilter filter, long seed) {
        switch (filter) {
            case BLACK_AND_WHITE -> blackAndWhite(pixels);
            case FILM_GRAIN -> filmGrain(pixels, width, seed);
            case EXPOSURE -> brightenExposure(pixels);
            case COLOR_BALANCE -> colorBalance(pixels);
            case NONE -> {
            }
        }
        sharpen(pixels, width, height);
    }

    private static void blackAndWhite(int[] pixels) {
        for (int index = 0; index < pixels.length; index++) {
            int pixel = pixels[index];
            int luminance = clamp(Math.round((red(pixel) * 0.2126F + green(pixel) * 0.7152F + blue(pixel) * 0.0722F - 128.0F) * 1.06F + 128.0F));
            pixels[index] = abgr(alpha(pixel), luminance, luminance, luminance);
        }
    }

    private static void filmGrain(int[] pixels, int width, long seed) {
        int seedBits = (int)(seed ^ seed >>> 32);
        for (int index = 0; index < pixels.length; index++) {
            int x = index % width;
            int y = index / width;
            int hash = x * 0x1f123bb5 ^ y * 0x5f356495 ^ seedBits;
            hash ^= hash >>> 15;
            hash *= 0x2c1b3c6d;
            hash ^= hash >>> 12;
            int noise = (hash & 15) - 7;
            int pixel = pixels[index];
            int luminance = (red(pixel) + green(pixel) + blue(pixel)) / 3;
            float midtone = 1.0F - Math.abs(luminance - 128) / 128.0F;
            int amount = Math.round(noise * (0.35F + 0.65F * midtone));
            pixels[index] = abgr(alpha(pixel), red(pixel) + amount, green(pixel) + amount, blue(pixel) + amount);
        }
    }

    private static void brightenExposure(int[] pixels) {
        for (int index = 0; index < pixels.length; index++) {
            int pixel = pixels[index];
            pixels[index] = abgr(
                    alpha(pixel),
                    expose(red(pixel)),
                    expose(green(pixel)),
                    expose(blue(pixel))
            );
        }
    }

    private static int expose(int channel) {
        double normalized = channel / 255.0D;
        return clamp((int)Math.round((1.0D - Math.pow(1.0D - normalized, 1.28D)) * 255.0D));
    }

    private static void colorBalance(int[] pixels) {
        long redTotal = 0L;
        long greenTotal = 0L;
        long blueTotal = 0L;
        for (int pixel : pixels) {
            redTotal += red(pixel);
            greenTotal += green(pixel);
            blueTotal += blue(pixel);
        }
        double count = Math.max(1, pixels.length);
        double redAverage = redTotal / count;
        double greenAverage = greenTotal / count;
        double blueAverage = blueTotal / count;
        double neutral = (redAverage + greenAverage + blueAverage) / 3.0D;
        double redScale = balancedScale(neutral, redAverage);
        double greenScale = balancedScale(neutral, greenAverage);
        double blueScale = balancedScale(neutral, blueAverage);

        for (int index = 0; index < pixels.length; index++) {
            int pixel = pixels[index];
            pixels[index] = abgr(
                    alpha(pixel),
                    (int)Math.round(red(pixel) * redScale),
                    (int)Math.round(green(pixel) * greenScale),
                    (int)Math.round(blue(pixel) * blueScale)
            );
        }
    }

    private static double balancedScale(double neutral, double channelAverage) {
        if (channelAverage < 1.0D) {
            return 1.0D;
        }
        double correction = Mth.clamp(neutral / channelAverage, 0.85D, 1.15D);
        return Mth.lerp(0.35D, 1.0D, correction);
    }

    private static void sharpen(int[] pixels, int width, int height) {
        if (width < 3 || height < 3) {
            return;
        }
        int[] source = pixels.clone();
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int index = y * width + x;
                int center = source[index];
                int left = source[index - 1];
                int right = source[index + 1];
                int up = source[index - width];
                int down = source[index + width];
                pixels[index] = abgr(
                        alpha(center),
                        sharpenChannel(red(center), red(left), red(right), red(up), red(down)),
                        sharpenChannel(green(center), green(left), green(right), green(up), green(down)),
                        sharpenChannel(blue(center), blue(left), blue(right), blue(up), blue(down))
                );
            }
        }
    }

    private static int sharpenChannel(int center, int left, int right, int up, int down) {
        float neighborAverage = (left + right + up + down) * 0.25F;
        return clamp(Math.round(center + (center - neighborAverage) * SHARPEN_STRENGTH));
    }

    private static int alpha(int abgr) {
        return abgr >>> 24 & 255;
    }

    private static int red(int abgr) {
        return abgr & 255;
    }

    private static int green(int abgr) {
        return abgr >>> 8 & 255;
    }

    private static int blue(int abgr) {
        return abgr >>> 16 & 255;
    }

    private static int abgr(int alpha, int red, int green, int blue) {
        return clamp(alpha) << 24 | clamp(blue) << 16 | clamp(green) << 8 | clamp(red);
    }

    private static int clamp(int channel) {
        return Mth.clamp(channel, 0, 255);
    }
}
