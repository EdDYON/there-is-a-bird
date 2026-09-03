package EdDYON.guaniao.content.camera;

public enum CameraFocusMode {
    AF_S(0, "gui.guaniao.camera_focus_mode.af_s", "AF-S"),
    AF_C(1, "gui.guaniao.camera_focus_mode.af_c", "AF-C"),
    MANUAL(2, "gui.guaniao.camera_focus_mode.manual", "MF");

    private final int id;
    private final String translationKey;
    private final String shortName;

    CameraFocusMode(int id, String translationKey, String shortName) {
        this.id = id;
        this.translationKey = translationKey;
        this.shortName = shortName;
    }

    public int id() {
        return this.id;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public String shortName() {
        return this.shortName;
    }

    public static CameraFocusMode byId(int id) {
        for (CameraFocusMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return AF_S;
    }
}
