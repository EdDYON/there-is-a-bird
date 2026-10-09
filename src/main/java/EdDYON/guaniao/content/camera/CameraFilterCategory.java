package EdDYON.guaniao.content.camera;

public enum CameraFilterCategory {
    NATURAL("gui.guaniao.camera_filter_category.natural"),
    FILM("gui.guaniao.camera_filter_category.film"),
    MONO("gui.guaniao.camera_filter_category.mono"),
    MOOD("gui.guaniao.camera_filter_category.mood"),
    CREATIVE("gui.guaniao.camera_filter_category.creative");

    private final String translationKey;

    CameraFilterCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public CameraFilterCategory next() {
        CameraFilterCategory[] categories = values();
        return categories[(this.ordinal() + 1) % categories.length];
    }

    public CameraFilterCategory previous() {
        CameraFilterCategory[] categories = values();
        return categories[(this.ordinal() - 1 + categories.length) % categories.length];
    }
}
