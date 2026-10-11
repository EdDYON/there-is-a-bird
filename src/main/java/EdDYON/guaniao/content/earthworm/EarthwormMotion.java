package EdDYON.guaniao.content.earthworm;

/** Shared geometry and timing, independent of client rendering and world access. */
public final class EarthwormMotion {
    public static final int SEGMENTS = 12;
    public static final float LENGTH = 0.30F;
    public static final double HOME_RADIUS = 2.0D;
    public static final int BURROW_TICKS = 40;

    private EarthwormMotion() { }

    public static float bend(float along, float time) {
        return (float) Math.sin(along * Math.PI * 3.0 - time * 0.10) * 0.022F
                * (float) Math.sin(along * Math.PI);
    }

    public static float radius(float along) {
        // Discrete widths give the ends and body a stepped pixel silhouette.
        return along < 0.10F || along > 0.90F ? 0.005F
                : along < 0.25F || along > 0.80F ? 0.010F : 0.015F;
    }

    public static int soilTicks(int previous, boolean onSoil) {
        return onSoil ? previous + 1 : 0;
    }
}
