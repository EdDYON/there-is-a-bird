package EdDYON.guaniao.content.bird;

/** Shared visibility limits, independent of a bird's small collision box or current pose. */
public final class BirdVisibility {
    public static final double BASE_RENDER_DISTANCE = 192.0D;
    public static final int TRACKING_RANGE_CHUNKS = 16;
    public static final double TRACKING_DISTANCE = TRACKING_RANGE_CHUNKS * 16.0D;
    public static final double TRACKING_DISTANCE_SQUARED = TRACKING_DISTANCE * TRACKING_DISTANCE;

    private BirdVisibility() { }

    public static boolean shouldRender(double distanceSquared, double viewScale) {
        if (!Double.isFinite(distanceSquared) || distanceSquared < 0
                || !Double.isFinite(viewScale) || viewScale <= 0) return false;
        double distance = BASE_RENDER_DISTANCE * viewScale;
        return distanceSquared < distance * distance;
    }
}
