package EdDYON.guaniao.content.bird;

/** Per-controller path length in model pixels; repeated renders never add travel twice. */
final class BirdWalkDistance {
    private double lastTime = Double.NaN;
    private double lastX;
    private double lastZ;
    private double pixels;
    private boolean wasGrounded;

    void sample(double time, double x, double z, boolean grounded, double renderScale) {
        sample(time, x, z, grounded, renderScale, Double.POSITIVE_INFINITY);
    }

    void sample(double time, double x, double z, boolean grounded, double renderScale,
                double maximumPixelsPerTick) {
        if (time == lastTime) return;
        double elapsed = time - lastTime;
        double dx = x - lastX, dz = z - lastZ;
        double distanceSqr = dx * dx + dz * dz;
        if (grounded && wasGrounded && elapsed > 0 && elapsed <= 4
                && Double.isFinite(distanceSqr) && distanceSqr <= 4
                && Double.isFinite(renderScale) && renderScale > 0) {
            // Discard excess travel immediately: slowing or stopping must never
            // replay a backlog of fast steps. Gaits without a cadence limit keep all travel.
            pixels += Math.min(Math.sqrt(distanceSqr) * 16 / renderScale, maximumPixelsPerTick * elapsed);
        }
        lastTime = time;
        lastX = x;
        lastZ = z;
        wasGrounded = grounded;
    }

    double pixels() { return pixels; }
}
