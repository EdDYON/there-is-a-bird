package EdDYON.guaniao.client.entity.umbrellacockatoo;

import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.umbrellacockatoo.UmbrellaCockatooEntity;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;

/** One non-looping recording, attached to one bird and stopped only with its own performance. */
final class CockatooSpecialCallSound extends AbstractTickableSoundInstance {
    private final UmbrellaCockatooEntity bird;
    private final long startedAt;

    CockatooSpecialCallSound(UmbrellaCockatooEntity bird) {
        super(GuaniaoSoundEvents.UMBRELLA_COCKATOO_TAMED_SPECIAL.get(), bird.getSoundSource(), SoundInstance.createUnseededRandom());
        this.bird = bird;
        this.startedAt = bird.getSpecialCallStartTick();
        this.pitch = 1;
        this.looping = false;
        this.volume = BirdSoundVolume.apply(bird, bird.getSoundVolume());
        followPosition();
    }

    void finish() { stop(); }

    @Override public boolean canPlaySound() {
        return bird.isAlive() && (!bird.isSilent() || bird.isSpecialCallForced()) && bird.isSpecialPerformanceActive()
                && bird.getSpecialCallStartTick() == startedAt && bird.level().getEntity(bird.getId()) == bird;
    }

    @Override public void tick() {
        if (!canPlaySound()) { stop(); return; }
        volume = BirdSoundVolume.apply(bird, bird.getSoundVolume());
        followPosition();
    }

    private void followPosition() {
        x = bird.getX();
        y = bird.getY() + bird.getBbHeight() * .5;
        z = bird.getZ();
    }
}
