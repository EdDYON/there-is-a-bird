package EdDYON.guaniao.content.camera;

public record CameraState(
        CameraFilter filter,
        CameraLens lens,
        CameraShootingMode shootingMode,
        double focalLength,
        CameraAperture aperture,
        CameraFocusMode focusMode,
        double focusDistance
) {
    public static final double MIN_FOCAL_LENGTH = 8.0D;
    public static final double MAX_FOCAL_LENGTH = 200.0D;
    public static final double MIN_FOCUS_DISTANCE = 0.3D;
    public static final double MAX_FOCUS_DISTANCE = 128.0D;

    public CameraState {
        filter = filter == null ? CameraFilter.NONE : filter;
        lens = lens == null ? CameraLens.STANDARD : lens;
        shootingMode = shootingMode == null ? CameraShootingMode.AUTO : shootingMode;
        focalLength = clampFinite(focalLength, MIN_FOCAL_LENGTH, MAX_FOCAL_LENGTH, 50.0D);
        aperture = aperture == null ? CameraAperture.F5_6 : aperture;
        focusMode = focusMode == null ? CameraFocusMode.AF_S : focusMode;
        focusDistance = clampFinite(focusDistance, lens.minimumFocusDistance(), MAX_FOCUS_DISTANCE, 12.0D);
    }

    public static CameraState defaults() {
        return new CameraState(
                CameraFilter.NONE,
                CameraLens.STANDARD,
                CameraShootingMode.AUTO,
                50.0D,
                CameraAperture.F5_6,
                CameraFocusMode.AF_S,
                12.0D
        );
    }

    public CameraState withFilter(CameraFilter next) {
        return new CameraState(next, this.lens, this.shootingMode, this.focalLength, this.aperture, this.focusMode, this.focusDistance);
    }

    public CameraState withLens(CameraLens next) {
        CameraLens resolved = next == null ? CameraLens.STANDARD : next;
        return new CameraState(this.filter, resolved, this.shootingMode, resolved.defaultFocalLength(), this.aperture, this.focusMode, this.focusDistance);
    }

    public CameraState withShootingMode(CameraShootingMode next) {
        return (next == null ? CameraShootingMode.AUTO : next).apply(this);
    }

    public CameraState withFocalLength(double next) {
        return new CameraState(this.filter, this.lens, this.shootingMode, next, this.aperture, this.focusMode, this.focusDistance);
    }

    public CameraState withAperture(CameraAperture next) {
        return new CameraState(this.filter, this.lens, this.shootingMode, this.focalLength, next, this.focusMode, this.focusDistance);
    }

    public CameraState withFocusMode(CameraFocusMode next) {
        return new CameraState(this.filter, this.lens, this.shootingMode, this.focalLength, this.aperture, next, this.focusDistance);
    }

    public CameraState withFocusDistance(double next) {
        return new CameraState(this.filter, this.lens, this.shootingMode, this.focalLength, this.aperture, this.focusMode, next);
    }

    public boolean hasInfiniteFocus() {
        return this.focusDistance >= MAX_FOCUS_DISTANCE - 0.5D;
    }

    private static double clampFinite(double value, double minimum, double maximum, double fallback) {
        if (!Double.isFinite(value)) {
            value = fallback;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
