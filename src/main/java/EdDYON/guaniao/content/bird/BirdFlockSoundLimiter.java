package EdDYON.guaniao.content.bird;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Prevents a dense same-species flock from starting many ambient calls together. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdFlockSoundLimiter {
    private static final int MAX_KEYS_PER_LEVEL = 2048;
    private static final int MAX_AMBIENT_CALLS_PER_WINDOW = 2;
    private static final Map<ServerLevel, LevelWindows> WINDOWS = new WeakHashMap<>();

    private BirdFlockSoundLimiter() {
    }

    public static boolean allowAmbient(Entity bird) {
        BirdSpecies species = BirdSpecies.from(bird);
        if (species == null || !(bird.level() instanceof ServerLevel serverLevel)) {
            return true;
        }
        long section = SectionPos.asLong(
                SectionPos.blockToSectionCoord(bird.getBlockX()),
                SectionPos.blockToSectionCoord(bird.getBlockY()),
                SectionPos.blockToSectionCoord(bird.getBlockZ()));
        int windowTicks = Math.max(20, (int)Math.round(80.0D * BirdConfigManager.ambientSoundCooldownMultiplier(species)));
        long window = serverLevel.getGameTime() / windowTicks;
        LevelWindows levelWindows = WINDOWS.computeIfAbsent(serverLevel, ignored -> new LevelWindows());
        SoundKey key = new SoundKey(section, species, window);
        int count = levelWindows.counts.getOrDefault(key, 0);
        if (count >= MAX_AMBIENT_CALLS_PER_WINDOW) {
            return false;
        }
        levelWindows.counts.put(key, count + 1);
        levelWindows.trim(window);
        return true;
    }

    public static int scaledAmbientInterval(Entity bird, int baseTicks) {
        BirdSpecies species = BirdSpecies.from(bird);
        if (species == null) {
            return baseTicks;
        }
        return Math.max(20, (int)Math.round(baseTicks * BirdConfigManager.ambientSoundCooldownMultiplier(species)));
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            WINDOWS.remove(serverLevel);
        }
    }

    private record SoundKey(long section, BirdSpecies species, long window) {
    }

    private static final class LevelWindows {
        private final LinkedHashMap<SoundKey, Integer> counts = new LinkedHashMap<>(64, 0.75F, true);

        private void trim(long currentWindow) {
            this.counts.entrySet().removeIf(entry -> entry.getKey().window < currentWindow - 2L);
            while (this.counts.size() > MAX_KEYS_PER_LEVEL) {
                this.counts.remove(this.counts.keySet().iterator().next());
            }
        }
    }
}
