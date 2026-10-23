package EdDYON.guaniao.content.bird.sparrow;

/** Pure balance rules, shared by the event controller and its standalone checks. */
public final class SparrowTideRules {
    public static final int HARD_CAP = 48;
    public static final int REGION_CAP = 96;
    public static final int EVENT_RADIUS = 80;
    public static final int COOLDOWN_CELL_SIZE = 128;
    public static final int WAVE_MIN = 4;
    public static final int WAVE_MAX = 8;
    public static final int WAVE_DELAY_MIN = 7 * 20;
    public static final int WAVE_DELAY_MAX = 18 * 20;
    public static final int GATHERING_TICKS = 70 * 20;
    public static final int ACTIVE_MIN = 3 * 60 * 20;
    public static final int ACTIVE_MAX = 8 * 60 * 20;
    public static final int COOLDOWN_MIN = 8 * 60 * 20;
    public static final int COOLDOWN_MAX = 15 * 60 * 20;

    private SparrowTideRules() {
    }

    public static double activityMultiplier(long dayTime, boolean raining, boolean thundering) {
        long time = Math.floorMod(dayTime, 24000L);
        if (thundering || time >= 13000L) {
            return 0.0D;
        }
        double daily = time < 3500L ? 1.25D : time >= 11000L ? 1.35D : 1.0D;
        return daily * (raining ? 0.5D : 1.0D);
    }

    public static int minTarget(int score) {
        return score >= 30 ? 24 : score >= 18 ? 18 : score >= 10 ? 12 : 0;
    }

    public static int maxTarget(int score) {
        return score >= 30 ? 36 : score >= 18 ? 26 : score >= 10 ? 18 : 0;
    }

    /** Only tide waves call this. Normal spawns keep the user's original caps. */
    public static int remainingCapacity(int configuredNearby, int configuredSparrows, int configuredRegional,
            int nearbyTotal, int nearbySparrows, int regionalTotal, int localSparrows, int target) {
        if (configuredNearby <= 0 || configuredSparrows <= 0) {
            return 0;
        }
        int sparrowCap = Math.min(HARD_CAP, Math.max(40, configuredSparrows));
        int regionalCap = Math.max(REGION_CAP, configuredRegional);
        return Math.max(0, Math.min(Math.min(HARD_CAP - nearbyTotal, sparrowCap - nearbySparrows),
                Math.min(regionalCap - regionalTotal, Math.min(HARD_CAP, target) - localSparrows)));
    }

    public static double ambientChance(int flockSize) {
        return flockSize >= 25 ? 0.20D : flockSize >= 13 ? 0.35D : flockSize >= 6 ? 0.65D : 1.0D;
    }

    public static boolean nearbyCells(int firstX, int firstZ, int secondX, int secondZ) {
        return Math.abs((long)firstX - secondX) <= 1L && Math.abs((long)firstZ - secondZ) <= 1L;
    }

    public static boolean eventsOverlap(int firstX, int firstZ, int secondX, int secondZ) {
        long dx = (long)firstX - secondX;
        long dz = (long)firstZ - secondZ;
        return dx * dx + dz * dz <= (long)EVENT_RADIUS * EVENT_RADIUS * 4
                || nearbyCells(Math.floorDiv(firstX, COOLDOWN_CELL_SIZE), Math.floorDiv(firstZ, COOLDOWN_CELL_SIZE),
                        Math.floorDiv(secondX, COOLDOWN_CELL_SIZE), Math.floorDiv(secondZ, COOLDOWN_CELL_SIZE));
    }

    public enum Phase { GATHERING, ACTIVE, DISPERSING, COOLDOWN }

    public static final class Timeline {
        private Phase phase = Phase.GATHERING;
        private final long gatherUntil;
        private long activeUntil;
        private long disperseUntil;
        private final int activeTicks;

        public Timeline(long now, int activeTicks) {
            this.gatherUntil = now + GATHERING_TICKS;
            this.activeTicks = activeTicks;
            this.activeUntil = gatherUntil + activeTicks;
        }

        public void advance(long now, boolean enabled, boolean canGather, boolean targetReached) {
            if (!enabled && (phase == Phase.GATHERING || phase == Phase.ACTIVE)) {
                phase = Phase.DISPERSING;
                disperseUntil = now + 20 * 20;
            } else if (phase == Phase.GATHERING && (!canGather || targetReached || now >= gatherUntil)) {
                phase = Phase.ACTIVE;
                activeUntil = now + activeTicks;
            } else if (phase == Phase.ACTIVE && now >= activeUntil) {
                phase = Phase.DISPERSING;
                disperseUntil = now + 20 * 20;
            }
            if (phase == Phase.DISPERSING && now >= disperseUntil) {
                phase = Phase.COOLDOWN;
            }
        }

        public Phase phase() { return phase; }
        public long activeUntil() { return activeUntil; }
    }
}
