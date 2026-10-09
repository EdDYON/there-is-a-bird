package EdDYON.guaniao.content.bird.umbrellacockatoo;

import net.minecraft.util.RandomSource;

/** Timing shared by the server performance and its positional client sound. */
public final class CockatooSpecialCall {
    public static final int DURATION_TICKS = 260;
    public static final int START_WINDOW_TICKS = 10;
    public static final double JOG_RADIUS = 2.0D;

    private CockatooSpecialCall() { }

    public static boolean shouldTrigger(boolean tamed, RandomSource random) {
        return tamed && random.nextInt(50) == 0;
    }

    public static int elapsedTicks(long startedAt, long now) {
        if (startedAt < 0) return -1;
        return (int)Math.min(Integer.MAX_VALUE, Math.max(0L, now - startedAt));
    }

    public static boolean active(long startedAt, long now) {
        int elapsed = elapsedTicks(startedAt, now);
        return elapsed >= 0 && elapsed < DURATION_TICKS;
    }

    public static double jogSpeed(int elapsed) {
        return elapsed >= 0 && elapsed < DURATION_TICKS && elapsed % 32 < 20 ? .12D : 0D;
    }
}
