package EdDYON.guaniao.content.bird;

import net.minecraft.util.Mth;

/** Isolated heading regressions; actual entity/client integration is checked in the game harness. */
public final class BirdBodyRotationControlTest {
    public static void main(String[] args) {
        correctBodyDespiteIndependentYaw();
        recordedCrowTakeoffNoLongerFliesSideways();
        stableCurvedTravelDoesNotAccumulateLag();
        reversalDoesNotRestartFromStaleEntityYaw();
        crossing180UsesShortArc();
        exclusionsKeepVanillaFacing();
        System.out.println("PASS: measured direction, stale yaw, sustained turns, reversal, angle wrap, "
                + "idle/jitter/vertical/teleport/invalid-motion and disabled-correction exclusions");
    }

    private static void correctBodyDespiteIndependentYaw() {
        close(-90, resolve(0, 0, .2, 0, 0, true), "east travel must not keep facing south");
        close(90, resolve(0, -70, -.2, 0, 0, true), "west travel independent of network yaw");
        close(0, resolve(-70, 85, 0, 0, .2, true), "south travel must not inherit next-turn yaw");
        close(45, toward(0, -130, 45), "diagonal travel faces its actual direction");
    }

    private static void recordedCrowTakeoffNoLongerFliesSideways() {
        // Real Forge 1.20.1 client samples, crow entity ticks 86-88. The renderer
        // also captured this sequence: its largest measured sideways angle was
        // 57.35 degrees. Columns: vanilla body yaw, actual delta X/Y/Z.
        double[][] samples = {
                {-60.46875, .005699763999018614, .03999999999999204, .07979669598645878},
                {-60.46875, .008353984916993795, .07545247395833599, .15341412217118044},
                {-42.1875, .009068672236367092, .10189670138888118, .20855863353078696}
        };
        float body = 25.3125F;
        int originalSidewaysTicks = 0;
        for (double[] sample : samples) {
            float measuredYaw = (float)Math.toDegrees(Math.atan2(sample[3], sample[1])) - 90;
            if (Math.abs(Mth.wrapDegrees(measuredYaw - (float)sample[0])) > 30) {
                originalSidewaysTicks++;
            }
            body = resolve(body, (float)sample[0], sample[1], sample[2], sample[3], true);
            closeAngle(measuredYaw, body, "recorded crow path must retain matching body direction");
        }
        if (originalSidewaysTicks != 3) throw new AssertionError("baseline must reproduce sustained sideways flight");
    }

    private static void stableCurvedTravelDoesNotAccumulateLag() {
        float body = 0;
        for (int tick = 1; tick <= 180; tick++) {
            float target = tick * 6;
            body = toward(body, target + 45, target);
            closeAngle(target, body, "ordinary curve must reach measured direction in one tick");
        }
    }

    private static void reversalDoesNotRestartFromStaleEntityYaw() {
        float body = toward(0, 0, 180);
        close(90, Math.abs(body), "first reversal tick remains bounded");
        body = toward(body, 0, 180);
        closeAngle(180, body, "second reversal tick converges despite stale entity yaw");
        closeAngle(180, toward(body, 0, 180), "stable heading has no further easing lag");
    }

    private static void crossing180UsesShortArc() {
        float body = toward(179, 0, -179);
        close(181, body, "crossing +180 should move +2 degrees");
        body = toward(-179, 0, 179);
        close(-181, body, "crossing -180 should move -2 degrees");
    }

    private static void exclusionsKeepVanillaFacing() {
        close(43, resolve(0, 43, 0, 0, 0, true), "idle must retain head-led body rotation");
        close(-45, resolve(0, 43, .001, 0, .001, true), "slow walking must also follow actual travel");
        close(43, resolve(0, 43, .00001, 0, .00001, true), "tiny corrections should not turn the bird");
        close(43, resolve(0, 43, 0, .4, 0, true), "pure vertical flight has no horizontal heading");
        close(43, resolve(0, 43, 3, 0, 0, true), "teleports must not set movement facing");
        close(43, resolve(0, 43, .2, 3, 0, true), "large vertical teleport must also be excluded");
        close(43, resolve(0, 43, Double.NaN, 0, .1, true), "NaN should retain valid vanilla rotation");
        close(43, resolve(0, 43, Double.POSITIVE_INFINITY, 0, .1, true), "infinite motion excluded");
        close(43, resolve(0, 43, .2, 0, 0, false), "server/passenger/NoAI/hurt/intentional strafe gate");
    }

    private static float toward(float previous, float vanillaYaw, float heading) {
        double radians = Math.toRadians(heading);
        return resolve(previous, vanillaYaw, -Math.sin(radians) * .2, 0, Math.cos(radians) * .2, true);
    }

    private static float resolve(float previous, float vanillaYaw,
                                 double x, double y, double z, boolean eligible) {
        return BirdBodyRotationControl.bodyYawForMotion(previous, vanillaYaw, x, y, z, eligible);
    }

    private static void closeAngle(float expected, float actual, String message) {
        close(0, Mth.wrapDegrees(expected - actual), message);
    }

    private static void close(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > .001F) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
