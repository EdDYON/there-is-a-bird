package EdDYON.guaniao.content.earthworm;

/** Standalone checks for the connected block silhouette, bend, and soil timer resets. */
public final class EarthwormMotionTest {
    public static void main(String[] args) {
        boolean moves = false;
        for (int tick = 0; tick < 200; tick++) {
            for (int i = 0; i < EarthwormMotion.SEGMENTS; i++) {
                float along = (i + 0.5F) / EarthwormMotion.SEGMENTS;
                float bend = EarthwormMotion.bend(along, tick);
                float radius = EarthwormMotion.radius(along);
                check(Float.isFinite(bend) && Float.isFinite(radius) && radius > 0.0F, "valid block");
                check(Math.abs(radius / 0.005F - Math.round(radius / 0.005F)) < 0.001F,
                        "body widths stay on the pixel grid");
                if (i > 0) {
                    float previous = (i - 0.5F) / EarthwormMotion.SEGMENTS;
                    check(Math.abs(bend - EarthwormMotion.bend(previous, tick))
                                    < radius + EarthwormMotion.radius(previous),
                            "neighboring blocks stay connected throughout the bend");
                }
                check(Math.abs(bend) + radius < 0.05F, "mesh fits the tiny entity footprint");
                moves |= Math.abs(bend - EarthwormMotion.bend(along, tick + 10)) > 0.01F;
            }
        }
        check(moves, "body actually bends over time");
        int soil = 0;
        for (int i = 0; i < 1800; i++) soil = EarthwormMotion.soilTicks(soil, true);
        check(soil == 1800, "soil time advances in real ticks");
        soil = EarthwormMotion.soilTicks(soil, false);
        for (int i = 0; i < 12000; i++) soil = EarthwormMotion.soilTicks(soil, false);
        check(soil == 0, "hard blocks cannot accumulate burrowing time");
        check(EarthwormMotion.soilTicks(soil, true) == 1, "returning to soil starts a fresh wait");
        System.out.println("PASS: " + (200 * EarthwormMotion.SEGMENTS)
                + " block samples, connected pixel silhouette, animated bend, soil wait and hard-block reset");
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
