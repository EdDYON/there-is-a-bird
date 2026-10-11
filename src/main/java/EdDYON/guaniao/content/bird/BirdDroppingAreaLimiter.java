package EdDYON.guaniao.content.bird;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-thread-only rate limit shared by all naturally defecating bird species. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdDroppingAreaLimiter {
    private static final int CELL_SIZE = 16;
    private static final int CLEANUP_INTERVAL_TICKS = 600;
    private static final int MAX_TRACKED_DROPS = 2048;
    private static final Map<ServerLevel, AreaWindows> WINDOWS = new WeakHashMap<>();

    private BirdDroppingAreaLimiter() {
    }

    public static boolean canDrop(ServerLevel level, BlockPos pos) {
        AreaWindows windows = WINDOWS.get(level);
        return windows == null || windows.canDrop(level.getGameTime(), pos.getX(), pos.getZ(),
                BirdConfigManager.droppingNearbyRadius());
    }

    /** Call only after addFreshEntity succeeds; failed attempts must not reserve an area. */
    public static void recordDrop(ServerLevel level, BlockPos pos, RandomSource random) {
        int cooldown = Mth.nextInt(random, BirdConfigManager.droppingAreaCooldownMinTicks(),
                BirdConfigManager.droppingAreaCooldownMaxTicks());
        WINDOWS.computeIfAbsent(level, ignored -> new AreaWindows())
                .recordDrop(level.getGameTime(), pos.getX(), pos.getZ(), cooldown);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Iterator<Map.Entry<ServerLevel, AreaWindows>> iterator = WINDOWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ServerLevel, AreaWindows> entry = iterator.next();
            entry.getValue().cleanIfDue(entry.getKey().getGameTime());
            if (entry.getValue().size == 0) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WINDOWS.remove(level);
        }
    }

    // Kept independent of world objects so boundary, expiry and cache limits can be tested.
    static final class AreaWindows {
        private final Map<Long, List<DropWindow>> cells = new HashMap<>();
        private long nextCleanup;
        private int size;

        boolean canDrop(long now, int x, int z, int radius) {
            cleanIfDue(now);
            if (size >= MAX_TRACKED_DROPS) {
                // Reclaim expired entries before applying a conservative memory bound.
                prune(now);
                if (size >= MAX_TRACKED_DROPS) {
                    return false;
                }
            }
            long radiusSquared = (long)radius * radius;
            for (int cellX = Math.floorDiv(x - radius, CELL_SIZE);
                    cellX <= Math.floorDiv(x + radius, CELL_SIZE); cellX++) {
                for (int cellZ = Math.floorDiv(z - radius, CELL_SIZE);
                        cellZ <= Math.floorDiv(z + radius, CELL_SIZE); cellZ++) {
                    List<DropWindow> drops = cells.get(cellKey(cellX, cellZ));
                    if (drops == null) {
                        continue;
                    }
                    for (DropWindow drop : drops) {
                        long dx = (long)x - drop.x;
                        long dz = (long)z - drop.z;
                        if (drop.expiresAt > now && dx * dx + dz * dz <= radiusSquared) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        void recordDrop(long now, int x, int z, int cooldown) {
            cleanIfDue(now);
            long key = cellKey(Math.floorDiv(x, CELL_SIZE), Math.floorDiv(z, CELL_SIZE));
            cells.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(new DropWindow(x, z, now + cooldown));
            size++;
        }

        void cleanIfDue(long now) {
            if (now >= nextCleanup) {
                prune(now);
                nextCleanup = now + CLEANUP_INTERVAL_TICKS;
            }
        }

        private void prune(long now) {
            Iterator<List<DropWindow>> iterator = cells.values().iterator();
            while (iterator.hasNext()) {
                List<DropWindow> drops = iterator.next();
                int before = drops.size();
                drops.removeIf(drop -> drop.expiresAt <= now);
                size -= before - drops.size();
                if (drops.isEmpty()) {
                    iterator.remove();
                }
            }
        }

        int size() {
            return size;
        }

        private static long cellKey(int x, int z) {
            return (x & 0xffffffffL) | ((z & 0xffffffffL) << 32);
        }
    }

    private record DropWindow(int x, int z, long expiresAt) {
    }
}
