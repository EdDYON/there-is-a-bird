package EdDYON.guaniao.content.camera;

public enum CameraLens {
    STANDARD(0, "gui.guaniao.camera_lens.standard", 50.0D, 0.0F, 1.0F, 0.5D),
    WIDE(1, "gui.guaniao.camera_lens.wide", 24.0D, 0.12F, 0.72F, 0.5D),
    TELEPHOTO(2, "gui.guaniao.camera_lens.telephoto", 200.0D, -0.015F, 1.55F, 4.0D),
    MACRO(3, "gui.guaniao.camera_lens.macro", 100.0D, 0.0F, 1.85F, 0.3D),
    FISHEYE(4, "gui.guaniao.camera_lens.fisheye", 8.0D, 0.78F, 0.68F, 0.5D);

    private final int id;
    private final String translationKey;
    private final double defaultFocalLength;
    private final float distortion;
    private final float depthOfFieldMultiplier;
    private final double minimumFocusDistance;

    CameraLens(
            int id,
            String translationKey,
            double defaultFocalLength,
            float distortion,
            float depthOfFieldMultiplier,
            double minimumFocusDistance
    ) {
        this.id = id;
        this.translationKey = translationKey;
        this.defaultFocalLength = defaultFocalLength;
        this.distortion = distortion;
        this.depthOfFieldMultiplier = depthOfFieldMultiplier;
        this.minimumFocusDistance = minimumFocusDistance;
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

    public double defaultFocalLength() {
        return this.defaultFocalLength;
    }

    public float distortion() {
        return this.distortion;
    }

    public float depthOfFieldMultiplier() {
        return this.depthOfFieldMultiplier;
    }

    public double minimumFocusDistance() {
        return this.minimumFocusDistance;
    }

    public static CameraLens byId(int id) {
        for (CameraLens lens : values()) {
            if (lens.id == id) {
                return lens;
            }
        }
        return STANDARD;
    }
}
