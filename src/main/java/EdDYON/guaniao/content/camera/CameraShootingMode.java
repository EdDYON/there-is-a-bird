package EdDYON.guaniao.content.camera;

public enum CameraShootingMode {
    AUTO(0, "gui.guaniao.camera_shooting_mode.auto", CameraLens.STANDARD, 50.0D, CameraAperture.F5_6, CameraFocusMode.AF_S, 12.0D),
    BIRD_PORTRAIT(1, "gui.guaniao.camera_shooting_mode.bird_portrait", CameraLens.TELEPHOTO, 200.0D, CameraAperture.F2_8, CameraFocusMode.AF_C, 15.0D),
    BIRD_IN_FLIGHT(2, "gui.guaniao.camera_shooting_mode.bird_in_flight", CameraLens.TELEPHOTO, 200.0D, CameraAperture.F5_6, CameraFocusMode.AF_C, 28.0D),
    MACRO(3, "gui.guaniao.camera_shooting_mode.macro", CameraLens.MACRO, 100.0D, CameraAperture.F2_8, CameraFocusMode.AF_S, 2.4D),
    LANDSCAPE(4, "gui.guaniao.camera_shooting_mode.landscape", CameraLens.WIDE, 24.0D, CameraAperture.F11, CameraFocusMode.AF_S, 64.0D);

    private final int id;
    private final String translationKey;
    private final CameraLens lens;
    private final double focalLength;
    private final CameraAperture aperture;
    private final CameraFocusMode focusMode;
    private final double focusDistance;

    CameraShootingMode(
            int id,
            String translationKey,
            CameraLens lens,
            double focalLength,
            CameraAperture aperture,
            CameraFocusMode focusMode,
            double focusDistance
    ) {
        this.id = id;
        this.translationKey = translationKey;
        this.lens = lens;
        this.focalLength = focalLength;
        this.aperture = aperture;
        this.focusMode = focusMode;
        this.focusDistance = focusDistance;
    }

    public int id() {
        return this.id;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public String descriptionKey() {
        return this.translationKey + ".description";
    }

    public CameraState apply(CameraState current) {
        return new CameraState(
                current.filter(),
                this.lens,
                this,
                this.focalLength,
                this.aperture,
                this.focusMode,
                this.focusDistance
        );
    }

    public static CameraShootingMode byId(int id) {
        for (CameraShootingMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return AUTO;
    }
}
