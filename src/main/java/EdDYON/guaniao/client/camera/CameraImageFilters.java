package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.content.camera.CameraFilter;
import net.minecraft.util.Mth;

/** CPU counterpart of guaniao_camera_universal.fsh used for the saved JPEG. */
final class CameraImageFilters {
    private static final float SHARPEN_STRENGTH = 0.16F;

    private CameraImageFilters() {
    }

    static void apply(int[] pixels, int width, int height, CameraFilter filter, long seed) {
        if (filter == CameraFilter.COLOR_BALANCE) {
            autoColorBalance(pixels);
        } else if (filter != CameraFilter.NONE) {
            applyUniversal(pixels, width, height, filter.id(), seed);
        }
        sharpen(pixels, width, height);
    }

    private static void applyUniversal(int[] pixels, int width, int height, int mode, long seed) {
        boolean samplesNeighbours = mode == CameraFilter.GLITCH_RGB.id()
                || mode == CameraFilter.CHROMATIC_ABERRATION.id();
        int[] source = samplesNeighbours ? pixels.clone() : pixels;

        for (int index = 0; index < pixels.length; index++) {
            int x = index % width;
            int yPixel = index / width;
            int pixel = source[index];
            double u = (x + 0.5D) / width;
            double v = (yPixel + 0.5D) / height;
            double red = normalized(red(pixel));
            double green = normalized(green(pixel));
            double blue = normalized(blue(pixel));
            double sourceRed = red;
            double sourceGreen = green;
            double sourceBlue = blue;
            double luminance = luminance(red, green, blue);
            double amount;
            double grey;
            double mask;
            double grain;

            switch (mode) {
                case 1 -> {
                    grey = contrast(luminance, 1.18D);
                    red = green = blue = grey;
                }
                case 2 -> {
                    red *= 1.03D;
                    blue *= 0.95D;
                    double tintedLuminance = luminance(red, green, blue);
                    red = saturate(red, tintedLuminance, 0.95D);
                    green = saturate(green, tintedLuminance, 0.95D);
                    blue = saturate(blue, tintedLuminance, 0.95D);
                    grain = grainOffset(red, green, blue, x, yPixel, seed, 0.075D);
                    amount = vignette(u, v, width, height, 0.30D, 0.66D, 0.22D);
                    red = (red + grain) * amount;
                    green = (green + grain) * amount;
                    blue = (blue + grain) * amount;
                }
                case 3 -> {
                    red = expose(red, 1.52D);
                    green = expose(green, 1.52D);
                    blue = expose(blue, 1.52D);
                    double exposedLuminance = luminance(red, green, blue);
                    red = saturate(red, exposedLuminance, 1.08D);
                    green = saturate(green, exposedLuminance, 1.08D);
                    blue = saturate(blue, exposedLuminance, 1.08D);
                }
                case 5 -> {
                    grey = smoothstep(0.025D, 0.975D, contrast(luminance, 1.70D));
                    red = green = blue = grey;
                }
                case 6 -> {
                    red = contrast(saturate(red, luminance, 1.90D), 1.25D);
                    green = contrast(saturate(green, luminance, 1.90D), 1.25D);
                    blue = contrast(saturate(blue, luminance, 1.90D), 1.25D);
                }
                case 7 -> {
                    red *= 1.18D;
                    green *= 1.04D;
                    blue *= 0.76D;
                    double tintedLuminance = luminance(red, green, blue);
                    red = contrast(saturate(red, tintedLuminance, 0.82D), 1.07D) * 0.88D + 0.075D;
                    green = contrast(saturate(green, tintedLuminance, 0.82D), 1.07D) * 0.88D + 0.052D;
                    blue = contrast(saturate(blue, tintedLuminance, 0.82D), 1.07D) * 0.88D + 0.025D;
                    amount = vignette(u, v, width, height, 0.31D, 0.68D, 0.28D);
                    red *= amount;
                    green *= amount;
                    blue *= amount;
                }
                case 8 -> {
                    amount = smoothstep(0.26D, 0.80D, luminance);
                    red = lerp(amount, red * 0.68D, red * 1.23D);
                    green = lerp(amount, green * 1.04D, green * 1.01D);
                    blue = lerp(amount, blue * 1.22D, blue * 0.68D);
                    double mixedLuminance = luminance(red, green, blue);
                    red = contrast(saturate(red, mixedLuminance, 1.23D), 1.18D);
                    green = contrast(saturate(green, mixedLuminance, 1.23D), 1.18D);
                    blue = contrast(saturate(blue, mixedLuminance, 1.23D), 1.18D);
                }
                case 9 -> {
                    red = expose(red, 1.48D) * 1.0576D;
                    green = expose(green, 1.48D) * 0.9856D;
                    blue = expose(blue, 1.48D) * 1.0792D;
                    amount = smoothstep(0.55D, 1.0D, luminance(red, green, blue)) * 0.22D;
                    red = lerp(amount, red, 1.0D);
                    green = lerp(amount, green, 0.94D);
                    blue = lerp(amount, blue, 0.985D);
                    double dreamyLuminance = luminance(red, green, blue);
                    red = saturate(red, dreamyLuminance, 0.90D);
                    green = saturate(green, dreamyLuminance, 0.90D);
                    blue = saturate(blue, dreamyLuminance, 0.90D);
                }
                case 10 -> {
                    red = contrast(saturate(red, luminance, 1.75D), 1.30D) * 1.06D;
                    green = contrast(saturate(green, luminance, 1.75D), 1.30D) * 0.98D;
                    blue = contrast(saturate(blue, luminance, 1.75D), 1.30D) * 1.02D;
                    amount = vignette(u, v, width, height, 0.26D, 0.62D, 0.58D);
                    red *= amount;
                    green *= amount;
                    blue *= amount;
                }
                case 11 -> {
                    red = lerp(0.48D, red, expose(red, 1.35D));
                    green = lerp(0.48D, green, expose(green, 1.35D));
                    blue = lerp(0.48D, blue, expose(blue, 1.35D));
                    double softLuminance = luminance(red, green, blue);
                    red = contrast(saturate(red, softLuminance, 0.92D), 0.92D);
                    green = contrast(saturate(green, softLuminance, 0.92D), 0.92D);
                    blue = contrast(saturate(blue, softLuminance, 0.92D), 0.92D);
                }
                case 12 -> {
                    red = Math.pow(contrast(saturate(red, luminance, 1.12D), 1.52D), 1.05D);
                    green = Math.pow(contrast(saturate(green, luminance, 1.12D), 1.52D), 1.05D);
                    blue = Math.pow(contrast(saturate(blue, luminance, 1.12D), 1.52D), 1.05D);
                }
                case 13 -> {
                    red = expose(red, 1.30D) * 1.16D;
                    green = expose(green, 1.30D) * 1.03D;
                    blue = expose(blue, 1.30D) * 0.83D;
                    double warmLuminance = luminance(red, green, blue);
                    red = saturate(red, warmLuminance, 1.12D);
                    green = saturate(green, warmLuminance, 1.12D);
                    blue = saturate(blue, warmLuminance, 1.12D);
                }
                case 14 -> {
                    red = contrast(red * 0.88D, 1.17D);
                    green = contrast(green * 1.01D, 1.17D);
                    blue = contrast(blue * 1.18D, 1.17D);
                    double coolLuminance = luminance(red, green, blue);
                    red = saturate(red, coolLuminance, 1.13D);
                    green = saturate(green, coolLuminance, 1.13D);
                    blue = saturate(blue, coolLuminance, 1.13D);
                }
                case 15 -> {
                    mask = smoothstep(0.02D, 0.35D, blue - Math.max(red, green) * 0.72D) * 0.82D;
                    red = lerp(mask, red, red * 0.92D);
                    green = lerp(mask, green, green * 1.05D);
                    blue = lerp(mask, blue, blue * 1.38D);
                    double skyLuminance = luminance(red, green, blue);
                    red = saturate(red, skyLuminance, 1.20D);
                    green = saturate(green, skyLuminance, 1.20D);
                    blue = saturate(blue, skyLuminance, 1.20D);
                }
                case 16 -> {
                    mask = smoothstep(0.0D, 0.30D, green - Math.max(red, blue) * 0.78D) * 0.75D;
                    red = lerp(mask, red, red * 0.90D);
                    green = lerp(mask, green, green * 1.30D);
                    blue = lerp(mask, blue, blue * 0.89D);
                    double forestLuminance = luminance(red, green, blue);
                    red = contrast(saturate(red, forestLuminance, 1.20D), 1.10D);
                    green = contrast(saturate(green, forestLuminance, 1.20D), 1.10D);
                    blue = contrast(saturate(blue, forestLuminance, 1.20D), 1.10D);
                }
                case 17 -> {
                    red *= 1.27D;
                    green *= 0.96D;
                    blue *= 0.72D;
                    amount = smoothstep(0.25D, 0.75D, luminance);
                    green = lerp(amount, green, green * 0.86D);
                    blue = lerp(amount, blue, blue * 1.05D);
                    double sunsetLuminance = luminance(red, green, blue);
                    red = contrast(saturate(red, sunsetLuminance, 1.28D), 1.12D);
                    green = contrast(saturate(green, sunsetLuminance, 1.28D), 1.12D);
                    blue = contrast(saturate(blue, sunsetLuminance, 1.28D), 1.12D);
                }
                case 18 -> {
                    red = red * 0.88D * 0.83D + 0.055D;
                    green = green * 1.02D * 0.83D + 0.066D;
                    blue = blue * 1.13D * 0.83D + 0.082D;
                    double vintageLuminance = luminance(red, green, blue);
                    red = saturate(red, vintageLuminance, 0.78D);
                    green = saturate(green, vintageLuminance, 0.78D);
                    blue = saturate(blue, vintageLuminance, 0.78D);
                    grain = grainOffset(red, green, blue, x, yPixel, seed, 0.045D);
                    red += grain;
                    green += grain;
                    blue += grain;
                }
                case 19 -> {
                    red = contrast(saturate(red, luminance, 0.68D), 0.78D);
                    green = contrast(saturate(green, luminance, 0.68D), 0.78D);
                    blue = contrast(saturate(blue, luminance, 0.68D), 0.78D);
                    red = lerp(0.10D, red, 0.90D) * 0.82D + 0.09D;
                    green = lerp(0.10D, green, 0.83D) * 0.82D + 0.09D;
                    blue = lerp(0.10D, blue, 0.70D) * 0.82D + 0.09D;
                    grain = grainOffset(red, green, blue, x, yPixel, seed, 0.035D);
                    red += grain;
                    green += grain;
                    blue += grain;
                }
                case 20 -> {
                    red = lerp(0.58D, red, luminance);
                    green = lerp(0.58D, green, luminance);
                    blue = lerp(0.58D, blue, luminance);
                    red = contrast(red, 1.62D);
                    green = contrast(green, 1.62D);
                    blue = contrast(blue, 1.62D);
                    double bleachLuminance = luminance(red, green, blue);
                    red = saturate(red, bleachLuminance, 0.72D);
                    green = saturate(green, bleachLuminance, 0.72D);
                    blue = saturate(blue, bleachLuminance, 0.72D);
                }
                case 21 -> {
                    red = contrast(sourceRed * 0.393D + sourceGreen * 0.769D + sourceBlue * 0.189D, 1.16D);
                    green = contrast(sourceRed * 0.349D + sourceGreen * 0.686D + sourceBlue * 0.168D, 1.16D);
                    blue = contrast(sourceRed * 0.272D + sourceGreen * 0.534D + sourceBlue * 0.131D, 1.16D);
                    amount = vignette(u, v, width, height, 0.31D, 0.69D, 0.25D);
                    red *= amount;
                    green *= amount;
                    blue *= amount;
                }
                case 22 -> {
                    amount = smoothstep(0.36D, 0.74D, luminance);
                    red = lerp(amount, red * 0.62D, red * 1.30D);
                    green = lerp(amount, green * 0.86D, green * 1.09D);
                    blue = lerp(amount, blue * 1.26D, blue * 0.62D);
                    double cinemaLuminance = luminance(red, green, blue);
                    red = contrast(saturate(red, cinemaLuminance, 1.15D), 1.18D);
                    green = contrast(saturate(green, cinemaLuminance, 1.15D), 1.18D);
                    blue = contrast(saturate(blue, cinemaLuminance, 1.15D), 1.18D);
                }
                case 23 -> {
                    red = smoothstep(0.02D, 0.86D, red);
                    green = Math.pow(green, 0.82D);
                    blue = Math.pow(blue, 1.23D) * 1.12D;
                    double crossLuminance = luminance(red, green, blue);
                    red = contrast(saturate(red, crossLuminance, 1.35D), 1.12D);
                    green = contrast(saturate(green, crossLuminance, 1.35D), 1.12D);
                    blue = contrast(saturate(blue, crossLuminance, 1.35D), 1.12D);
                }
                case 24 -> {
                    red = contrast(saturate(red, luminance, 1.48D), 1.32D) * 1.08D;
                    green = contrast(saturate(green, luminance, 1.48D), 1.32D) * 0.94D;
                    blue = contrast(saturate(blue, luminance, 1.48D), 1.32D) * 1.03D;
                    amount = vignette(u, v, width, height, 0.20D, 0.58D, 0.68D);
                    red *= amount;
                    green *= amount;
                    blue *= amount;
                    grain = grainOffset(red, green, blue, x, yPixel, seed, 0.055D);
                    red += grain;
                    green += grain;
                    blue += grain;
                }
                case 25 -> {
                    grey = clamp01((luminance - 0.5D) * 0.83D + 0.55D);
                    red = green = blue = grey;
                }
                case 26 -> {
                    grey = contrast(expose(luminance, 1.70D), 1.05D);
                    red = green = blue = grey;
                }
                case 27 -> {
                    grey = contrast(Math.pow(luminance, 1.55D), 1.45D);
                    amount = vignette(u, v, width, height, 0.27D, 0.66D, 0.32D);
                    red = green = blue = grey * amount;
                }
                case 28 -> {
                    red = contrast(luminance * 1.08D, 1.22D);
                    green = contrast(luminance, 1.22D);
                    blue = contrast(luminance * 0.86D, 1.22D);
                }
                case 29 -> {
                    red = contrast(luminance * 0.84D, 1.24D);
                    green = contrast(luminance * 0.96D, 1.24D);
                    blue = contrast(luminance * 1.13D, 1.24D);
                }
                case 30 -> {
                    grey = contrast(luminance, 1.32D);
                    red = grey * 0.92D;
                    green = grey * 0.98D;
                    blue = grey * 1.05D;
                    grain = grainOffset(red, green, blue, x, yPixel, seed, 0.040D);
                    red += grain;
                    green += grain;
                    blue += grain;
                }
                case 31 -> {
                    grey = Math.floor(clamp01(contrast(luminance, 1.85D)) * 7.0D) / 7.0D;
                    red = green = blue = grey;
                }
                case 32 -> {
                    grey = contrast(luminance, 1.68D);
                    red = green = blue = grey;
                    grain = grainOffset(red, green, blue, x, yPixel, seed, 0.105D);
                    amount = vignette(u, v, width, height, 0.24D, 0.62D, 0.52D);
                    red = (red + grain) * amount;
                    green = (green + grain) * amount;
                    blue = (blue + grain) * amount;
                }
                case 33 -> {
                    red = lerp(0.18D, contrast(saturate(red, luminance, 0.72D), 0.78D), 0.80D);
                    green = lerp(0.18D, contrast(saturate(green, luminance, 0.72D), 0.78D), 0.88D);
                    blue = lerp(0.18D, contrast(saturate(blue, luminance, 0.72D), 0.78D), 0.92D);
                    red = expose(red, 1.22D);
                    green = expose(green, 1.22D);
                    blue = expose(blue, 1.22D);
                }
                case 34 -> {
                    red *= 1.17D;
                    green *= 0.96D;
                    blue *= 1.06D;
                    amount = smoothstep(0.50D, 1.0D, luminance) * 0.18D;
                    red = expose(lerp(amount, red, 1.0D), 1.18D);
                    green = expose(lerp(amount, green, 0.70D), 1.18D);
                    blue = expose(lerp(amount, blue, 0.76D), 1.18D);
                }
                case 35 -> {
                    red = Math.pow(red, 1.18D) * 0.64D;
                    green = Math.pow(green, 1.18D) * 0.82D;
                    blue = Math.pow(blue, 1.18D) * 1.24D;
                    double moonLuminance = luminance(red, green, blue);
                    red = contrast(saturate(red, moonLuminance, 0.72D), 1.18D);
                    green = contrast(saturate(green, moonLuminance, 0.72D), 1.18D);
                    blue = contrast(saturate(blue, moonLuminance, 0.72D), 1.18D);
                }
                case 36 -> {
                    red = expose(red, 1.27D) * 1.16D;
                    green = expose(green, 1.27D) * 0.91D;
                    blue = expose(blue, 1.27D) * 1.05D;
                    double pinkLuminance = luminance(red, green, blue);
                    red = saturate(red, pinkLuminance, 1.08D);
                    green = saturate(green, pinkLuminance, 1.08D);
                    blue = saturate(blue, pinkLuminance, 1.08D);
                }
                case 37 -> {
                    red = red * 1.20D + sourceGreen * 0.08D;
                    green = green * 1.04D * 0.94D;
                    blue *= 0.72D;
                    double autumnLuminance = luminance(red, green, blue);
                    red = contrast(saturate(red, autumnLuminance, 1.30D), 1.10D);
                    green = contrast(saturate(green, autumnLuminance, 1.30D), 1.10D);
                    blue = contrast(saturate(blue, autumnLuminance, 1.30D), 1.10D);
                }
                case 38 -> {
                    red = expose(red * 1.06D, 1.22D);
                    green = expose(green * 1.17D, 1.22D);
                    blue = expose(blue * 0.98D, 1.22D);
                    double springLuminance = luminance(red, green, blue);
                    red = saturate(red, springLuminance, 1.16D);
                    green = saturate(green, springLuminance, 1.16D);
                    blue = saturate(blue, springLuminance, 1.16D);
                }
                case 39 -> {
                    red *= 0.82D;
                    green *= 0.98D;
                    blue *= 1.22D;
                    double winterLuminance = luminance(red, green, blue);
                    red = expose(saturate(red, winterLuminance, 0.78D), 1.15D);
                    green = expose(saturate(green, winterLuminance, 0.78D), 1.15D);
                    blue = expose(saturate(blue, winterLuminance, 0.78D), 1.15D);
                }
                case 40 -> {
                    red *= 1.13D;
                    green *= 1.07D;
                    blue *= 0.88D;
                    double summerLuminance = luminance(red, green, blue);
                    red = contrast(saturate(red, summerLuminance, 1.42D), 1.14D);
                    green = contrast(saturate(green, summerLuminance, 1.42D), 1.14D);
                    blue = contrast(saturate(blue, summerLuminance, 1.42D), 1.14D);
                }
                case 41 -> {
                    red = contrast(saturate(red, luminance, 1.80D), 1.32D) * 1.16D;
                    green = contrast(saturate(green, luminance, 1.80D), 1.32D) * 0.78D
                            + smoothstep(0.45D, 1.0D, sourceBlue) * 0.16D;
                    blue = contrast(saturate(blue, luminance, 1.80D), 1.32D) * 1.30D;
                }
                case 42 -> {
                    red = contrast(red, 1.38D) * 0.66D;
                    green = contrast(green, 1.38D) * 1.22D;
                    blue = contrast(blue, 1.38D) * 0.60D;
                    double horrorLuminance = luminance(red, green, blue);
                    red = saturate(red, horrorLuminance, 0.72D);
                    green = saturate(green, horrorLuminance, 0.72D);
                    blue = saturate(blue, horrorLuminance, 0.72D);
                    amount = vignette(u, v, width, height, 0.25D, 0.62D, 0.52D);
                    red *= amount;
                    green *= amount;
                    blue *= amount;
                }
                case 43 -> {
                    red = contrast(saturate(red, luminance, 0.58D) * 1.26D, 1.42D);
                    green = contrast(saturate(green, luminance, 0.58D) * 0.82D, 1.42D);
                    blue = contrast(saturate(blue, luminance, 0.58D) * 0.53D, 1.42D);
                    grain = grainOffset(red, green, blue, x, yPixel, seed, 0.060D);
                    amount = vignette(u, v, width, height, 0.29D, 0.66D, 0.30D);
                    red = (red + grain) * amount;
                    green = (green + grain) * amount;
                    blue = (blue + grain) * amount;
                }
                case 44 -> {
                    int band = (int)Math.floor(v * 90.0D);
                    double kick = noise01(band, 0, seed) >= 0.84D ? 1.0D : 0.0D;
                    int shift = (int)Math.round((noise01(band * 23, 7, seed ^ 0x5DEECE66DL) - 0.5D)
                            * 0.045D * kick * width);
                    int colorOffset = Math.max(1, (int)Math.round(width * 0.004D));
                    red = sampleChannel(source, width, height, x + shift + colorOffset, yPixel, 0);
                    green = sampleChannel(source, width, height, x + shift, yPixel, 1);
                    blue = sampleChannel(source, width, height, x + shift - colorOffset, yPixel, 2);
                    double glitchLuminance = luminance(red, green, blue);
                    red = contrast(saturate(red, glitchLuminance, 1.35D), 1.18D);
                    green = contrast(saturate(green, glitchLuminance, 1.35D), 1.18D);
                    blue = contrast(saturate(blue, glitchLuminance, 1.35D), 1.18D);
                }
                case 45 -> {
                    int offsetX = (int)Math.round((u - 0.5D) * 0.018D * width);
                    int offsetY = (int)Math.round((v - 0.5D) * 0.018D * height);
                    red = sampleChannel(source, width, height, x + offsetX, yPixel + offsetY, 0);
                    green = sourceGreen;
                    blue = sampleChannel(source, width, height, x - offsetX, yPixel - offsetY, 2);
                    red = contrast(red, 1.12D);
                    green = contrast(green, 1.12D);
                    blue = contrast(blue, 1.12D);
                }
                case 46 -> {
                    red = Math.pow(contrast(saturate(red, luminance, 2.35D), 1.48D), 0.82D) * 1.08D;
                    green = Math.pow(contrast(saturate(green, luminance, 2.35D), 1.48D), 0.82D) * 0.92D;
                    blue = Math.pow(contrast(saturate(blue, luminance, 2.35D), 1.48D), 0.82D) * 1.18D;
                }
                case 47 -> {
                    red = contrast(Math.floor(saturate(red, luminance, 1.45D) * 6.0D + 0.5D) / 6.0D, 1.26D);
                    green = contrast(Math.floor(saturate(green, luminance, 1.45D) * 6.0D + 0.5D) / 6.0D, 1.26D);
                    blue = contrast(Math.floor(saturate(blue, luminance, 1.45D) * 6.0D + 0.5D) / 6.0D, 1.26D);
                }
                case 48 -> {
                    amount = smoothstep(0.08D, 0.93D, luminance);
                    red = lerp(amount, 0.015D, 0.55D);
                    green = lerp(amount, 0.035D, 0.88D);
                    blue = lerp(amount, 0.13D, 1.0D);
                }
                case 49 -> {
                    double boosted = expose(luminance, 2.25D);
                    red = boosted * 0.25D;
                    green = boosted;
                    blue = boosted * 0.18D;
                    grain = grainOffset(red, green, blue, x, yPixel, seed, 0.07D);
                    amount = vignette(u, v, width, height, 0.28D, 0.65D, 0.48D);
                    red = (red + grain) * amount;
                    green = (green + grain) * amount;
                    blue = (blue + grain) * amount;
                }
                case 50 -> {
                    grey = contrast(luminance, 1.32D);
                    if (grey < 0.25D) {
                        amount = grey / 0.25D;
                        red = lerp(amount, 0.02D, 0.10D);
                        green = lerp(amount, 0.00D, 0.05D);
                        blue = lerp(amount, 0.12D, 0.75D);
                    } else if (grey < 0.50D) {
                        amount = (grey - 0.25D) / 0.25D;
                        red = lerp(amount, 0.10D, 0.85D);
                        green = lerp(amount, 0.05D, 0.05D);
                        blue = lerp(amount, 0.75D, 0.35D);
                    } else if (grey < 0.75D) {
                        amount = (grey - 0.50D) / 0.25D;
                        red = lerp(amount, 0.85D, 1.0D);
                        green = lerp(amount, 0.05D, 0.65D);
                        blue = lerp(amount, 0.35D, 0.05D);
                    } else {
                        amount = (grey - 0.75D) / 0.25D;
                        red = 1.0D;
                        green = lerp(amount, 0.65D, 1.0D);
                        blue = lerp(amount, 0.05D, 0.75D);
                    }
                }
                default -> {
                }
            }

            pixels[index] = abgr(alpha(pixel), channel(red), channel(green), channel(blue));
        }
    }

    private static void autoColorBalance(int[] pixels) {
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
        double correction = Mth.clamp(neutral / channelAverage, 0.82D, 1.18D);
        return lerp(0.48D, 1.0D, correction);
    }

    private static double sampleChannel(int[] pixels, int width, int height, int x, int y, int channel) {
        int clampedX = Mth.clamp(x, 0, width - 1);
        int clampedY = Mth.clamp(y, 0, height - 1);
        int pixel = pixels[clampedY * width + clampedX];
        return normalized(switch (channel) {
            case 0 -> red(pixel);
            case 1 -> green(pixel);
            default -> blue(pixel);
        });
    }

    private static double grainOffset(double red, double green, double blue, int x, int y, long seed, double amount) {
        double midtone = 1.0D - Math.abs(luminance(red, green, blue) * 2.0D - 1.0D);
        return (noise01(x, y, seed) - 0.5D) * amount * (0.35D + 0.65D * midtone);
    }

    private static double noise01(int x, int y, long seed) {
        long hash = seed ^ x * 0x632BE59BD9B4E019L ^ y * 0x9E3779B97F4A7C15L;
        hash ^= hash >>> 30;
        hash *= 0xBF58476D1CE4E5B9L;
        hash ^= hash >>> 27;
        hash *= 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return (hash >>> 11) * 0x1.0p-53;
    }

    private static double vignette(double u, double v, int width, int height, double start, double end, double amount) {
        double aspectY = height / (double)Math.max(1, width);
        double x = u - 0.5D;
        double y = (v - 0.5D) * aspectY;
        double distance = Math.sqrt(x * x + y * y);
        return 1.0D - smoothstep(start, end, distance) * amount;
    }

    private static double luminance(double red, double green, double blue) {
        return red * 0.2126D + green * 0.7152D + blue * 0.0722D;
    }

    private static double saturate(double channel, double luminance, double amount) {
        return clamp01(lerp(amount, luminance, channel));
    }

    private static double contrast(double channel, double amount) {
        return clamp01((channel - 0.5D) * amount + 0.5D);
    }

    private static double expose(double channel, double power) {
        return 1.0D - Math.pow(1.0D - clamp01(channel), power);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double progress = clamp01((value - edge0) / (edge1 - edge0));
        return progress * progress * (3.0D - 2.0D * progress);
    }

    private static double lerp(double amount, double start, double end) {
        return start + amount * (end - start);
    }

    private static double normalized(int channel) {
        return channel / 255.0D;
    }

    private static int channel(double normalized) {
        return clamp((int)Math.round(clamp01(normalized) * 255.0D));
    }

    private static double clamp01(double value) {
        return Mth.clamp(value, 0.0D, 1.0D);
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
