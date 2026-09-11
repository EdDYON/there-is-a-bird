package EdDYON.guaniao.content.bird.sparrow;

import java.util.Random;

/** Standalone balance and lifecycle assertions without a Minecraft launch. */
public final class SparrowTideRulesTest {
    public static void main(String[] args) {
        weatherAndTime();
        populationAndWaves();
        lifecycle();
        for (int coordinate = -260; coordinate <= 260; coordinate++) {
            int first = Math.floorDiv(coordinate, SparrowTideRules.COOLDOWN_CELL_SIZE);
            int adjacent = Math.floorDiv(coordinate + 1, SparrowTideRules.COOLDOWN_CELL_SIZE);
            check(SparrowTideRules.nearbyCells(first, first, adjacent, adjacent), "adjacent blocks across cell edges");
        }
        check(!SparrowTideRules.nearbyCells(0, 0, 3, 0), "distant regions remain independent");
        check(SparrowTideRules.eventsOverlap(127, 0, 256, 0), "overlapping event radii across nonadjacent cells");
        check(SparrowTideRules.eventsOverlap(-129, 0, 0, 0), "negative boundary cannot duplicate a gathering");
        check(!SparrowTideRules.eventsOverlap(0, 0, 384, 0), "separated gatherings allowed");
        check(SparrowTideRules.ambientChance(30) == 0.20D, "large gathering sound reduction");
        System.out.println("PASS: time/weather, habitat tiers, capacity, 1000 wave simulations, lifecycle and cell edges");
    }

    private static void weatherAndTime() {
        check(SparrowTideRules.activityMultiplier(0, false, false) == 1.25D, "dawn boost");
        check(SparrowTideRules.activityMultiplier(3500, false, false) == 1.0D, "daytime");
        check(SparrowTideRules.activityMultiplier(11000, false, false) == 1.35D, "dusk boost");
        check(SparrowTideRules.activityMultiplier(12999, false, false) == 1.35D, "last dusk tick");
        check(SparrowTideRules.activityMultiplier(13000, false, false) == 0.0D, "night stops new waves");
        check(SparrowTideRules.activityMultiplier(23999, false, false) == 0.0D, "night until next dawn");
        check(SparrowTideRules.activityMultiplier(24000, false, false) == 1.25D, "next day wraps");
        check(SparrowTideRules.activityMultiplier(-1, false, false) == 0.0D, "negative time wraps");
        check(SparrowTideRules.activityMultiplier(6000, true, false) == 0.5D, "rain halves triggers");
        check(SparrowTideRules.activityMultiplier(1000, false, true) == 0.0D, "thunder overrides dawn");
        check(SparrowTideRules.maxTarget(9) == 0, "ordinary habitat has no event");
        check(SparrowTideRules.minTarget(10) == 12 && SparrowTideRules.maxTarget(17) == 18, "low tier");
        check(SparrowTideRules.minTarget(18) == 18 && SparrowTideRules.maxTarget(29) == 26, "middle tier");
        check(SparrowTideRules.minTarget(30) == 24 && SparrowTideRules.maxTarget(42) == 36, "high tier");
    }

    private static void populationAndWaves() {
        check(room(0, 8, 0, 0, 0, 0, 28) == 0, "global zero disables waves");
        check(room(24, 0, 0, 0, 0, 0, 28) == 0, "species zero disables waves");
        check(room(24, 8, 11, 11, 11, 11, 28) == 17, "existing birds reduce the target");
        check(room(24, 8, 47, 20, 47, 20, 36) == 1, "other species consume global slots");
        check(room(24, 8, 48, 20, 48, 20, 36) == 0, "global hard cap");
        check(room(24, 8, 20, 20, 96, 20, 36) == 0, "regional cap");
        check(room(24, 8, 12, 12, 12, 48, 36) == 0, "local existing tame or named birds also count");
        check(room(200, 200, 48, 48, 48, 48, 200) == 0, "configured high values cannot bypass hard cap");
        Random random = new Random(42);
        for (int simulation = 0; simulation < 1000; simulation++) {
            int target = 24 + random.nextInt(13);
            int sparrows = random.nextInt(12);
            int others = random.nextInt(25);
            int initial = sparrows;
            for (int wave = 0; wave < 8; wave++) {
                int planned = 4 + random.nextInt(5);
                int generated = Math.min(planned, room(24, 8, sparrows + others, sparrows,
                        sparrows + others, sparrows, target));
                sparrows += generated;
                check(generated <= 8 && sparrows <= target, "wave and target bounds");
                check(sparrows + others <= 48, "mixed population bound");
            }
            check(sparrows == Math.max(initial, Math.min(target, 48 - others)), "waves fill only available slots");
        }
    }

    private static int room(int configuredNearby, int configuredSparrows, int total, int sparrows,
            int regional, int local, int target) {
        return SparrowTideRules.remainingCapacity(configuredNearby, configuredSparrows, 48,
                total, sparrows, regional, local, target);
    }

    private static void lifecycle() {
        SparrowTideRules.Timeline timeline = new SparrowTideRules.Timeline(0, 3600);
        timeline.advance(1399, true, true, false);
        check(timeline.phase() == SparrowTideRules.Phase.GATHERING, "gathering before timeout");
        timeline.advance(1400, true, true, false);
        check(timeline.phase() == SparrowTideRules.Phase.ACTIVE, "failed spawns cannot extend gathering forever");
        timeline.advance(5000, true, true, false);
        check(timeline.phase() == SparrowTideRules.Phase.DISPERSING, "active phase ends");
        timeline.advance(5400, true, true, false);
        check(timeline.phase() == SparrowTideRules.Phase.COOLDOWN, "dispersal completes");
        timeline = new SparrowTideRules.Timeline(0, 3600);
        timeline.advance(100, true, false, false);
        check(timeline.phase() == SparrowTideRules.Phase.ACTIVE, "night or thunder cancels further gathering");
        timeline.advance(105, false, true, false);
        check(timeline.phase() == SparrowTideRules.Phase.DISPERSING, "switching mode off releases the event");
        timeline = new SparrowTideRules.Timeline(0, 3600);
        timeline.advance(100, true, true, true);
        check(timeline.phase() == SparrowTideRules.Phase.ACTIVE, "stop replenishing after target reached");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
