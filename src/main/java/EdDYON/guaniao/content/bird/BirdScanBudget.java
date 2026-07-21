package EdDYON.guaniao.content.bird;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Per-level total budget plus refillable per-species tokens to prevent one species monopolizing scans. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdScanBudget {
    private static final Map<ServerLevel, TickUsage> USAGE = new WeakHashMap<>();

    private BirdScanBudget() {
    }

    public static boolean tryAcquire(ServerLevel level) {
        return tryAcquire(level, null, 1);
    }

    public static boolean tryAcquire(ServerLevel level, int cost) {
        return tryAcquire(level, null, cost);
    }

    public static boolean tryAcquire(ServerLevel level, Entity bird) {
        return tryAcquire(level, bird, 1);
    }

    public static boolean tryAcquire(ServerLevel level, Entity bird, int cost) {
        if (level == null) {
            return false;
        }
        long tick = level.getGameTime();
        TickUsage usage = USAGE.computeIfAbsent(level, ignored -> new TickUsage());
        int budget = BirdConfigManager.birdScanBudgetPerTick();
        usage.beginTick(tick, budget);
        int normalizedCost = Math.max(1, cost);
        BirdSpecies species = BirdSpecies.from(bird);
        if (usage.used + normalizedCost > budget || !usage.consumeSpecies(species, normalizedCost, budget)) {
            usage.denied++;
            return false;
        }
        usage.used += normalizedCost;
        usage.granted++;
        return true;
    }

    public static Stats stats(ServerLevel level) {
        TickUsage usage = USAGE.get(level);
        return usage == null ? new Stats(0, 0, 0, 0) : new Stats(usage.used, usage.granted, usage.denied, usage.tick);
    }

    public static void resetStats(ServerLevel level) {
        TickUsage usage = USAGE.get(level);
        if (usage != null) {
            usage.granted = 0L;
            usage.denied = 0L;
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            USAGE.remove(level);
        }
    }

    public record Stats(int usedThisTick, long granted, long denied, long tick) {
    }

    private static final class TickUsage {
        private final EnumMap<BirdSpecies, Integer> speciesTokens = new EnumMap<>(BirdSpecies.class);
        private int unclassifiedTokens;
        private long tick = Long.MIN_VALUE;
        private int used;
        private long granted;
        private long denied;

        private void beginTick(long currentTick, int budget) {
            if (this.tick == currentTick) {
                return;
            }
            long elapsed = this.tick == Long.MIN_VALUE ? 1L : Math.max(1L, currentTick - this.tick);
            this.tick = currentTick;
            this.used = 0;
            int refill = Math.max(1, budget / Math.max(1, BirdSpecies.values().length));
            int cap = Math.max(2, budget / 2);
            for (BirdSpecies species : BirdSpecies.values()) {
                int previous = this.speciesTokens.getOrDefault(species, cap);
                this.speciesTokens.put(species, Math.min(cap, previous + (int)Math.min(Integer.MAX_VALUE, elapsed * refill)));
            }
            this.unclassifiedTokens = Math.min(cap, this.unclassifiedTokens + (int)Math.min(Integer.MAX_VALUE, elapsed * refill));
        }

        private boolean consumeSpecies(BirdSpecies species, int cost, int budget) {
            int cap = Math.max(2, budget / 2);
            if (species == null) {
                if (this.unclassifiedTokens < cost) {
                    return false;
                }
                this.unclassifiedTokens -= cost;
                return true;
            }
            int tokens = this.speciesTokens.getOrDefault(species, cap);
            if (tokens < cost) {
                return false;
            }
            this.speciesTokens.put(species, tokens - cost);
            return true;
        }
    }
}
