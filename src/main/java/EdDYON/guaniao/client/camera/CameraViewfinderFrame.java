package EdDYON.guaniao.client.camera;

/** The visible lens and saved photograph share the same GUI-to-framebuffer rectangle. */
record CameraViewfinderFrame(int left, int top, int size) {
    static CameraViewfinderFrame layout(int width, int height, int lineHeight) {
        // Keep the original eyepiece proportions, with parameters above and below it.
        int verticalMargin = lineHeight * 2 + 18;
        int availableHeight = Math.max(1, height - verticalMargin * 2);
        int size = Math.max(1, Math.min(Math.min(width - 24, height * 80 / 100), availableHeight));
        // The focus ray is the centre of the world viewport, so centre the photo there too.
        return new CameraViewfinderFrame((width - size) / 2, (height - size) / 2, size);
    }

    Pixels pixels(int guiWidth, int guiHeight, int pixelWidth, int pixelHeight) {
        double scaleX = pixelWidth / (double)Math.max(1, guiWidth);
        double scaleY = pixelHeight / (double)Math.max(1, guiHeight);
        int x = clamp((int)Math.round(left * scaleX), 0, pixelWidth - 1);
        int y = clamp((int)Math.round(top * scaleY), 0, pixelHeight - 1);
        int right = clamp((int)Math.round((left + size) * scaleX), x + 1, pixelWidth);
        int bottom = clamp((int)Math.round((top + size) * scaleY), y + 1, pixelHeight);
        return new Pixels(x, y, right - x, bottom - y);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    record Pixels(int left, int top, int width, int height) { }
}
