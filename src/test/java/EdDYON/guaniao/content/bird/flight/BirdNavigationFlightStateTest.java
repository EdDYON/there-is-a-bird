package EdDYON.guaniao.content.bird.flight;

import net.minecraft.world.phys.Vec3;

/** Replays the slow navigation takeoff captured in the Forge 1.20.1 client. */
public final class BirdNavigationFlightStateTest {
    public static void main(String[] args) {
        replayRecordedTakeoff();
        flatGroundDoesNotStartFlight();
        landingAndSuppressedStatesClearFlight();
        unrelatedAirborneMotionDoesNotStartNavigationFlight();
        System.out.println("PASS: recorded slow navigation takeoff, ground walking, coast, landing, water, passenger, NoAI, death and fresh-instance reset");
    }

    private static void replayRecordedTakeoff() {
        // Entity ticks 61..73, flat surface Y=97. These samples all had
        // flightTicks=0, noGravity=false, flightActive=false on the server.
        double[][] samples = {
                {97.00839999954998, 0.007491119933897847, 0.008231999719202516},
                {97.02503199881917, 0.014308039270207325, 0.016299359601033913},
                {97.04973135797019, 0.020511436045029776, 0.024205372439101362},
                {97.08233672995928, 0.02615652727280876, 0.03195326517120269},
                {97.12268999468047, 0.031293560438136046, 0.03954620019644132},
                {97.17063619442689, 0.03596826075330793, 0.04698727666599892},
                {97.22602347064287, 0.040222238162713246, 0.054279531748092635},
                {97.28870300194095, 0.044093357716837075, 0.061425941867633206},
                {97.35852894335856, 0.04761607661261391, 0.06842942392108992},
                {97.43535836682963, 0.050821750900157815, 0.07529283646705831},
                {97.51905120284667, 0.05373891458589492, 0.08201898089301654},
                {97.60947018328967, 0.05639353361642135, 0.08861060255874664},
                {97.7064807853984, 0.058809237003820555, 0.09507039191688735}
        };
        BirdNavigationFlightState flight = new BirdNavigationFlightState();
        int nearGroundFrames = 0;
        for (int i = 0; i < samples.length; i++) {
            int tick = 61 + i;
            double[] sample = samples[i];
            check(!BirdFlightController.isTakeoffMotion(new Vec3(sample[1], sample[2], 0)),
                    "recorded slow lift must exercise the velocity fallback's blind spot");
            if (sample[0] - 97.0 < 0.7) nearGroundFrames++;
            flight.navigationMovedAt(tick);
            check(flight.afterTravel(tick, false, false, false, false, true),
                    "navigation flight must be signalled from the very first lift, tick " + tick);
        }
        check(nearGroundFrames == 12, "recording includes twelve near-ground frames with no velocity fallback");
        check(flight.afterTravel(74, false, false, false, false, true),
                "stopping navigation in the air must not fold the wings during the coast");
    }

    private static void flatGroundDoesNotStartFlight() {
        BirdNavigationFlightState flight = new BirdNavigationFlightState();
        for (int tick = 1; tick <= 40; tick++) {
            flight.navigationMovedAt(tick);
            check(!flight.afterTravel(tick, true, false, false, false, true),
                    "navigation along the ground must keep walking");
        }
        check(!flight.afterTravel(41, false, false, false, false, true),
                "an old ground navigation request must not classify a later hop as flight");
    }

    private static void landingAndSuppressedStatesClearFlight() {
        boolean[][] endings = {
                {true, false, false, false, true},
                {false, true, false, false, true},
                {false, false, true, false, true},
                {false, false, false, true, true},
                {false, false, false, false, false}
        };
        for (boolean[] ending : endings) {
            BirdNavigationFlightState flight = new BirdNavigationFlightState();
            flight.navigationMovedAt(1);
            check(flight.afterTravel(1, false, false, false, false, true), "start navigation flight");
            flight.navigationMovedAt(2);
            check(!flight.afterTravel(2, ending[0], ending[1], ending[2], ending[3], ending[4]),
                    "landing and suppressed states must override even a fresh navigation request");
            check(!flight.afterTravel(3, false, false, false, false, true),
                    "cleared navigation flight must not reappear from an old request");
            flight.navigationMovedAt(4);
            check(flight.afterTravel(4, false, false, false, false, true), "a later genuine flight can restart");
        }
    }

    private static void unrelatedAirborneMotionDoesNotStartNavigationFlight() {
        BirdNavigationFlightState flight = new BirdNavigationFlightState();
        for (int tick = 0; tick < 20; tick++) {
            check(!flight.afterTravel(tick, false, false, false, false, true),
                    "a fresh entity, fall or manual hop is not navigation flight");
        }
        check(!BirdFlightController.isTakeoffMotion(new Vec3(0.13, 0.23, 0)),
                "the deliberate sparrow ground hop must retain its existing classification");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
