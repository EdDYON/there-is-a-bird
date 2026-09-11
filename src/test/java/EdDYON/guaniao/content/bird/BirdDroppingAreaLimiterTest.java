package EdDYON.guaniao.content.bird;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Standalone assertions; runs without launching Minecraft or adding a test dependency. */
public final class BirdDroppingAreaLimiterTest {
    public static void main(String[] args) {
        boundariesAndExpiry();
        multipleDropsInOneCell();
        cleanupAndMemoryBound();
        compareWithDistanceOracle();
        System.out.println("PASS: area boundaries, expiry, world isolation, cache bound, 10000 spatial queries");
    }

    private static void boundariesAndExpiry() {
        BirdDroppingAreaLimiter.AreaWindows windows = new BirdDroppingAreaLimiter.AreaWindows();
        check(windows.canDrop(0, 15, -1, 16), "empty area");
        check(windows.canDrop(0, 15, -1, 16), "checking without a successful drop must not reserve the area");
        windows.recordDrop(0, 15, -1, 400);
        check(!windows.canDrop(399, 16, 0, 16), "cross both positive and negative cell boundaries");
        check(!windows.canDrop(399, -1, -1, 16), "exactly 16 blocks is restricted");
        check(windows.canDrop(399, -2, -1, 16), "17 blocks remains independent");
        check(windows.canDrop(399, 31, 15, 16), "nearby cell outside actual radius remains independent");
        check(windows.canDrop(400, 15, -1, 16), "exact expiry allows a drop");
        windows.recordDrop(400, -1, -1, 800);
        check(!windows.canDrop(1199, 0, 0, 16), "negative coordinates remain restricted");
        check(windows.canDrop(1200, 0, 0, 16), "40-second expiry");
        BirdDroppingAreaLimiter.AreaWindows otherWorld = new BirdDroppingAreaLimiter.AreaWindows();
        check(otherWorld.canDrop(500, -1, -1, 16), "world caches are independent");
    }

    private static void multipleDropsInOneCell() {
        BirdDroppingAreaLimiter.AreaWindows windows = new BirdDroppingAreaLimiter.AreaWindows();
        windows.recordDrop(0, 0, 0, 400);
        check(windows.canDrop(0, 15, 15, 4), "small configurable radius");
        windows.recordDrop(0, 15, 15, 800);
        check(!windows.canDrop(1, 0, 0, 4), "second drop in cell must not overwrite first");
        check(windows.canDrop(400, 0, 0, 4), "first drop expires independently");
        check(!windows.canDrop(400, 15, 15, 4), "second drop retains its own expiry");
        check(!windows.canDrop(400, 40, 15, 32), "larger radius scans beyond adjacent cells");
    }

    private static void cleanupAndMemoryBound() {
        BirdDroppingAreaLimiter.AreaWindows windows = new BirdDroppingAreaLimiter.AreaWindows();
        for (int i = 0; i < 2048; i++) {
            int x = i * 64;
            check(windows.canDrop(0, x, 0, 16), "independent area before capacity");
            windows.recordDrop(0, x, 0, 400);
        }
        check(!windows.canDrop(399, -64, 0, 16), "bound must not evict still-active protection");
        check(windows.canDrop(400, -64, 0, 16), "expired entries reclaimed even before periodic cleanup");
        check(windows.size() == 0, "expired cells released");
        windows.recordDrop(400, 0, 0, 800);
        windows.cleanIfDue(1200);
        check(windows.size() == 0, "periodic cleanup without any new drops");
    }

    private static void compareWithDistanceOracle() {
        BirdDroppingAreaLimiter.AreaWindows windows = new BirdDroppingAreaLimiter.AreaWindows();
        List<long[]> history = new ArrayList<>();
        Random random = new Random(20260909L);
        long now = 0;
        for (int i = 0; i < 10000; i++) {
            now += random.nextInt(8);
            int x = random.nextInt(401) - 200;
            int z = random.nextInt(401) - 200;
            int radius = 4 + random.nextInt(29);
            boolean expected = true;
            for (long[] drop : history) {
                long dx = x - drop[0];
                long dz = z - drop[1];
                if (drop[2] > now && dx * dx + dz * dz <= (long)radius * radius) {
                    expected = false;
                    break;
                }
            }
            check(windows.canDrop(now, x, z, radius) == expected, "spatial oracle query " + i);
            if (expected && random.nextBoolean()) {
                int cooldown = 400 + random.nextInt(401);
                windows.recordDrop(now, x, z, cooldown);
                history.add(new long[]{x, z, now + cooldown});
            }
        }
        windows.cleanIfDue(now + 1200);
        check(windows.size() == 0, "all explored cells eventually expire");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
