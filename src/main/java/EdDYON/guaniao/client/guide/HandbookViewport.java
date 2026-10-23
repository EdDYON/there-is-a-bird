package EdDYON.guaniao.client.guide;

/** Pixel-aligned book coordinates, independent of the player's HUD/GUI scale. */
record HandbookViewport(int width, int height, int pixelScale, double guiScale, int pixelHeight) {
    static HandbookViewport of(int pixelWidth, int pixelHeight, double guiScale) {
        // Aim for an 800 x 450 reading area; round to whole pixels to keep the game font crisp.
        int scale = Math.max(1, (int)Math.round(Math.min(pixelWidth / 800.0, pixelHeight / 450.0)));
        return new HandbookViewport((int)Math.ceil(pixelWidth / (double)scale),
                (int)Math.ceil(pixelHeight / (double)scale), scale, guiScale, pixelHeight);
    }

    float renderScale() { return (float)(pixelScale / guiScale); }
    double fromGui(double coordinate) { return coordinate * guiScale / pixelScale; }
}
