package EdDYON.guaniao.client.hummingbird;

import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns nearby loops independently of render passes, with one instance per live bird. */
public final class HummingbirdSoundManager {
    private static final int MAX_LOOPS = 4;
    private static final int MIN_SELECTION_TICKS = 100;
    private static final double RANGE = 16;
    public static final HummingbirdSoundManager INSTANCE = new HummingbirdSoundManager(new Playback() {
        @Override public void play(HummingbirdWingSound sound) { Minecraft.getInstance().getSoundManager().play(sound); }
        @Override public void stop(HummingbirdWingSound sound) { Minecraft.getInstance().getSoundManager().stop(sound); }
        @Override public boolean isActive(HummingbirdWingSound sound) { return Minecraft.getInstance().getSoundManager().isActive(sound); }
    });

    interface Playback {
        void play(HummingbirdWingSound sound);
        void stop(HummingbirdWingSound sound);
        boolean isActive(HummingbirdWingSound sound);
    }

    private record Loop(HummingbirdWingSound sound, long startedAt) { }
    private final Playback playback;
    private final Map<UUID, Loop> loops = new HashMap<>();
    @Nullable private Level level;
    private long clock;
    private int scanDelay;

    HummingbirdSoundManager(Playback playback) { this.playback = playback; }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (level != minecraft.level) clear();
        if (minecraft.isPaused()) return;
        Entity listener = minecraft.getCameraEntity();
        boolean enabled = minecraft.options.getSoundSourceVolume(SoundSource.MASTER) > 0
                && minecraft.options.getSoundSourceVolume(SoundSource.NEUTRAL) > 0;
        update(minecraft.level, listener == null ? null : listener.position(), enabled);
    }

    void update(@Nullable Level world, @Nullable Vec3 listener, boolean enabled) {
        if (level != world) { clear(); level = world; }
        if (world == null || listener == null || !enabled) { clear(); return; }
        clock++;
        var iterator = loops.entrySet().iterator();
        while (iterator.hasNext()) {
            Loop loop = iterator.next().getValue();
            HummingbirdWingSound sound = loop.sound();
            if (sound.isStopped() || !sound.canPlaySound() || sound.bird().level() != world
                    || sound.bird().distanceToSqr(listener) > RANGE * RANGE
                    || clock - loop.startedAt() >= 20 && !playback.isActive(sound)) {
                stop(sound); iterator.remove(); scanDelay = 0;
            }
        }
        if (scanDelay-- > 0) return;
        scanDelay = 4;
        var candidates = world.getEntitiesOfClass(HummingbirdEntity.class, new AABB(listener, listener).inflate(RANGE),
                bird -> bird.isWingSoundActive() && world.getEntity(bird.getId()) == bird
                        && bird.distanceToSqr(listener) <= RANGE * RANGE
                        && BirdSoundVolume.apply(bird, HummingbirdWingSound.BASE_VOLUME) > 0);
        // Keep startup stable, then rebalance only during each loop's natural quiet pause.
        // This also protects later four-to-five-second bursts from being cut short by a distance crossing.
        // Landing, sleeping, muting, removal and leaving earshot still stop it immediately above.
        candidates.sort(Comparator.comparingInt((HummingbirdEntity bird) -> {
                    Loop existing = loops.get(bird.getUUID());
                    return existing != null && (clock - existing.startedAt() < MIN_SELECTION_TICKS
                            || existing.sound().isBurstActive()) ? 0 : 1;
                })
                .thenComparingDouble(bird -> bird.distanceToSqr(listener))
                .thenComparing(HummingbirdEntity::getUUID));
        Set<UUID> selected = new HashSet<>();
        int count = Math.min(MAX_LOOPS, candidates.size());
        for (int i = 0; i < count; i++) selected.add(candidates.get(i).getUUID());
        iterator = loops.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!selected.contains(entry.getKey())) { stop(entry.getValue().sound()); iterator.remove(); }
        }
        float gain = count == 0 ? 1 : (float)(1 / Math.sqrt(count));
        for (int i = 0; i < count; i++) {
            HummingbirdEntity bird = candidates.get(i);
            Loop existing = loops.get(bird.getUUID());
            if (existing != null && existing.sound().bird() != bird) {
                stop(existing.sound()); loops.remove(bird.getUUID()); existing = null;
            }
            if (existing == null) {
                HummingbirdWingSound sound = new HummingbirdWingSound(bird);
                sound.flockGain(gain);
                loops.put(bird.getUUID(), new Loop(sound, clock));
                playback.play(sound);
            } else existing.sound().flockGain(gain);
        }
    }

    public void clear() {
        for (Loop loop : loops.values()) stop(loop.sound());
        loops.clear(); level = null; scanDelay = 0;
    }

    private void stop(HummingbirdWingSound sound) {
        sound.finish();
        playback.stop(sound);
    }
}
