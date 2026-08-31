package EdDYON.guaniao.content.camera;

import net.minecraft.util.Mth;

public enum CameraFilter {
    NONE("gui.guaniao.camera_filter.none"),
    BLACK_AND_WHITE("gui.guaniao.camera_filter.black_and_white"),
    FILM_GRAIN("gui.guaniao.camera_filter.film_grain"),
    EXPOSURE("gui.guaniao.camera_filter.exposure"),
    COLOR_BALANCE("gui.guaniao.camera_filter.color_balance");

    private static final CameraFilter[] VALUES = values();
    private final String translationKey;

    CameraFilter(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public CameraFilter next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public static CameraFilter byId(int id) {
        return VALUES[Mth.clamp(id, 0, VALUES.length - 1)];
    }
}
