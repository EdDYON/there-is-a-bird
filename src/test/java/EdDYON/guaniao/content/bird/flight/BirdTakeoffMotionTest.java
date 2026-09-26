package EdDYON.guaniao.content.bird.flight;

import net.minecraft.world.phys.Vec3;

/** Motion can arrive before the behavior/on-ground metadata on the client. */
public final class BirdTakeoffMotionTest {
    public static void main(String[] args) {
        for (double lift : new double[]{0.28, 0.32, 0.48, 0.52, 0.56, 0.64, 0.68, 0.72, 0.82}) {
            require(BirdFlightController.isTakeoffMotion(new Vec3(0, lift, 0)), "Vertical launch missed: " + lift);
            require(BirdFlightController.isTakeoffMotion(new Vec3(0.01, lift, -0.02)), "Near-vertical launch missed: " + lift);
        }
        require(BirdFlightController.isTakeoffMotion(new Vec3(0.3, 0.06, 0)), "Shallow forward launch missed");
        require(!BirdFlightController.isTakeoffMotion(Vec3.ZERO), "Standing must not flap");
        require(!BirdFlightController.isTakeoffMotion(new Vec3(0.3, 0, 0)), "Walking must not flap");
        require(!BirdFlightController.isTakeoffMotion(new Vec3(0, -0.3, 0)), "Falling is not an upward launch");
        require(!BirdFlightController.isTakeoffMotion(new Vec3(0, 0.05, 0)), "Small vertical correction must not flap");
        require(!BirdFlightController.isTakeoffMotion(new Vec3(0, 0.23, 0)), "Vertical ground hop must remain a hop");
        require(!BirdFlightController.isTakeoffMotion(new Vec3(0.13, 0.23, 0)), "Normal sparrow hop must remain a hop");
        System.out.println("PASS: vertical, near-vertical and shallow takeoff; standing, walking, falling and short-hop exclusions");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
