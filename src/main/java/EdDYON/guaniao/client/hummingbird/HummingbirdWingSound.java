package EdDYON.guaniao.client.hummingbird;

import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.Mth;

/** A single quiet, positional wing loop; hovering does not require horizontal movement. */
final class HummingbirdWingSound extends AbstractTickableSoundInstance {
    static final float BASE_VOLUME = .64F;
    private static final int MIN_BURST_TICKS = 80;
    private static final int BURST_TICK_RANGE = 21;
    private static final int PAUSE_TICKS = 10;
    private static final int FADE_TICKS = 3;
    private final HummingbirdEntity bird;
    private float flockGain = 1;
    private float currentGain;
    private int burstTicks;
    private int cycleTick;

    HummingbirdWingSound(HummingbirdEntity bird) {
        super(GuaniaoSoundEvents.HUMMINGBIRD_WING.get(), bird.getSoundSource(), SoundInstance.createUnseededRandom());
        this.bird = bird;
        this.looping = true;
        this.delay = 0;
        this.volume = 0;
        this.burstTicks = MIN_BURST_TICKS + random.nextInt(BURST_TICK_RANGE);
        followPosition();
    }

    HummingbirdEntity bird() { return bird; }
    void flockGain(float gain) { flockGain = gain; }
    void finish() { stop(); }
    boolean isBurstActive() { return cycleTick > 0 && cycleTick <= burstTicks; }

    @Override public boolean canStartSilent() { return true; }

    @Override public boolean canPlaySound() {
        return bird.isWingSoundActive() && bird.level().getEntity(bird.getId()) == bird
                && BirdSoundVolume.apply(bird, BASE_VOLUME) > 0;
    }

    @Override public void tick() {
        if (!canPlaySound()) { stop(); return; }
        followPosition();
        currentGain = Mth.lerp(.25F, currentGain, BirdSoundVolume.apply(bird, BASE_VOLUME) * flockGain);
        cycleTick++;
        if (cycleTick <= burstTicks) {
            // Fade the burst edges without changing the recording's original pitch.
            float envelope = Mth.clamp(Math.min(cycleTick / (float) FADE_TICKS,
                    (burstTicks - cycleTick + 1) / (float) FADE_TICKS), 0, 1);
            volume = currentGain * envelope;
        } else {
            // Keep this channel alive during the brief pause instead of restarting the loop.
            volume = 0;
        }
        if (cycleTick >= burstTicks + PAUSE_TICKS) {
            cycleTick = 0;
            burstTicks = MIN_BURST_TICKS + random.nextInt(BURST_TICK_RANGE);
        }
    }

    private void followPosition() {
        x = bird.getX();
        y = bird.getY() + bird.getBbHeight() * .5;
        z = bird.getZ();
    }
}
