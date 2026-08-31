package EdDYON.guaniao.content.bird.myna;

import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Builds recognisable imitations entirely from crested-myna vocal syllables.
 * Source sounds provide only identity, pitch and timing; their audio is never
 * replayed by the bird.
 */
public final class MynaMimicVoice {
    private static final int DEFAULT_TEMPO_TICKS = 6;

    private MynaMimicVoice() {
    }

    public static Phrase nativeCall(RandomSource random) {
        float variation = 0.96F + random.nextFloat() * 0.08F;
        Builder phrase = new Builder();
        if (random.nextBoolean()) {
            phrase.add(0, GuaniaoSoundEvents.MYNA_MIMIC_WHISTLE_SHORT.get(), 0.66F, 1.04F * variation)
                    .add(5, GuaniaoSoundEvents.MYNA_MIMIC_TRILL.get(), 0.61F, 0.98F * variation);
        } else {
            phrase.add(0, GuaniaoSoundEvents.MYNA_MIMIC_WHISTLE_RISE.get(), 0.64F, variation)
                    .add(8, GuaniaoSoundEvents.MYNA_MIMIC_WHISTLE_FALL.get(), 0.62F, 0.94F * variation);
        }
        return phrase.build();
    }

    public static Phrase imitation(MynaMimicCue cue, float rememberedPitch,
                                   int rememberedTempo, RandomSource random) {
        float pitch = voicePitch(rememberedPitch) * (0.97F + random.nextFloat() * 0.06F);
        int beat = Mth.clamp(rememberedTempo <= 0 ? DEFAULT_TEMPO_TICKS : rememberedTempo, 3, 12);
        Builder phrase = new Builder();

        switch (cue) {
            case BELL -> phrase
                    .add(0, rise(), 0.70F, pitch * 1.02F)
                    .add(beat + 2, fall(), 0.68F, pitch * 0.90F);
            case NOTE_BLOCK -> phrase
                    .add(0, shortWhistle(), 0.67F, pitch)
                    .add(beat, shortWhistle(), 0.61F, pitch * 1.05F);
            case DOOR -> phrase
                    .add(0, rasp(), 0.56F, pitch * 0.78F)
                    .add(beat, click(), 0.50F, pitch * 0.92F);
            case CAT -> phrase
                    .add(0, rise(), 0.65F, pitch * 1.03F)
                    .add(beat + 1, shortWhistle(), 0.62F, pitch * 1.12F)
                    .add(beat * 2, fall(), 0.58F, pitch * 0.97F);
            case WOLF -> phrase
                    .add(0, fall(), 0.68F, pitch * 0.78F)
                    .add(beat + 2, trill(), 0.58F, pitch * 0.84F);
            case NIGHT_HERON -> phrase
                    .add(0, rasp(), 0.64F, pitch * 0.72F)
                    .add(beat + 3, fall(), 0.58F, pitch * 0.79F);
            case SPARROW -> phrase
                    .add(0, shortWhistle(), 0.56F, pitch * 1.16F)
                    .add(3, shortWhistle(), 0.54F, pitch * 1.22F)
                    .add(7, shortWhistle(), 0.52F, pitch * 1.12F);
            case LONG_TAILED_TIT -> phrase
                    .add(0, trill(), 0.57F, pitch * 1.16F)
                    .add(beat + 3, shortWhistle(), 0.52F, pitch * 1.25F);
            case BUDGERIGAR -> phrase
                    .add(0, trill(), 0.62F, pitch * 1.08F)
                    .add(beat, click(), 0.43F, pitch * 1.18F)
                    .add(beat + 3, shortWhistle(), 0.55F, pitch * 1.16F);
            case COCKATIEL -> phrase
                    .add(0, rise(), 0.65F, pitch * 1.15F)
                    .add(beat + 2, fall(), 0.60F, pitch * 1.08F);
            case MACAW -> phrase
                    .add(0, rasp(), 0.70F, pitch * 0.76F)
                    .add(beat + 2, burst(), 0.62F, pitch * 0.82F);
            case SPOTTED_DOVE -> phrase
                    .add(0, rasp(), 0.55F, pitch * 0.68F)
                    .add(beat, fall(), 0.57F, pitch * 0.72F)
                    .add(beat * 2, rasp(), 0.51F, pitch * 0.65F);
            case PIGEON -> phrase
                    .add(0, rasp(), 0.55F, pitch * 0.69F)
                    .add(beat + 1, trill(), 0.48F, pitch * 0.72F);
            case CROW -> phrase
                    .add(0, rasp(), 0.70F, pitch * 0.65F)
                    .add(beat + 1, rasp(), 0.64F, pitch * 0.60F);
            case SEAGULL -> phrase
                    .add(0, rise(), 0.66F, pitch * 0.92F)
                    .add(beat + 2, rasp(), 0.59F, pitch * 0.82F)
                    .add(beat * 2 + 1, fall(), 0.55F, pitch * 0.88F);
            case CHEST -> phrase
                    .add(0, click(), 0.52F, pitch * 0.88F)
                    .add(beat, rasp(), 0.50F, pitch * 0.80F);
            case MECHANISM -> phrase
                    .add(0, click(), 0.54F, pitch * 0.93F)
                    .add(Math.max(2, beat / 2), click(), 0.50F, pitch * 1.06F)
                    .add(beat, click(), 0.47F, pitch * 0.86F);
            case PLAYER -> phrase
                    .add(0, rise(), 0.61F, pitch * 0.92F)
                    .add(beat + 1, rasp(), 0.54F, pitch * 0.84F);
            case CREEPER_HISS -> phrase
                    .add(0, hiss(), 0.62F, pitch * 0.93F)
                    .add(beat + 7, shortWhistle(), 0.34F, pitch * 1.12F);
            case EXPLOSION -> phrase
                    .add(0, rasp(), 0.54F, pitch * 0.72F)
                    .add(Math.max(3, beat), burst(), 0.76F, pitch * 0.82F);
            case ZOMBIE -> phrase
                    .add(0, rasp(), 0.66F, pitch * 0.67F)
                    .add(beat + 2, rasp(), 0.61F, pitch * 0.61F);
            case SKELETON -> phrase
                    .add(0, click(), 0.58F, pitch * 0.86F)
                    .add(3, click(), 0.54F, pitch * 1.03F)
                    .add(6, click(), 0.50F, pitch * 0.91F)
                    .add(9, click(), 0.46F, pitch * 1.09F);
            case SPIDER -> phrase
                    .add(0, click(), 0.49F, pitch * 1.10F)
                    .add(2, click(), 0.46F, pitch * 1.19F)
                    .add(5, trill(), 0.54F, pitch * 1.07F);
            case ENDERMAN -> phrase
                    .add(0, rise(), 0.64F, pitch * 0.79F)
                    .add(beat + 2, hiss(), 0.52F, pitch * 1.14F)
                    .add(beat * 2 + 5, burst(), 0.58F, pitch * 1.20F);
            case WITCH -> phrase
                    .add(0, trill(), 0.60F, pitch * 0.94F)
                    .add(beat + 2, rasp(), 0.56F, pitch * 0.78F)
                    .add(beat * 2 + 1, shortWhistle(), 0.49F, pitch * 1.10F);
            case PILLAGER -> phrase
                    .add(0, rasp(), 0.64F, pitch * 0.73F)
                    .add(beat, click(), 0.48F, pitch * 0.88F)
                    .add(beat + 3, rasp(), 0.57F, pitch * 0.69F);
        }
        return phrase.build();
    }

    private static float voicePitch(float sourcePitch) {
        float normalized = (Mth.clamp(sourcePitch, 0.5F, 2.0F) - 0.5F) / 1.5F;
        return Mth.lerp(normalized, 0.82F, 1.27F);
    }

    private static SoundEvent shortWhistle() {
        return GuaniaoSoundEvents.MYNA_MIMIC_WHISTLE_SHORT.get();
    }

    private static SoundEvent rise() {
        return GuaniaoSoundEvents.MYNA_MIMIC_WHISTLE_RISE.get();
    }

    private static SoundEvent fall() {
        return GuaniaoSoundEvents.MYNA_MIMIC_WHISTLE_FALL.get();
    }

    private static SoundEvent rasp() {
        return GuaniaoSoundEvents.MYNA_MIMIC_RASP.get();
    }

    private static SoundEvent click() {
        return GuaniaoSoundEvents.MYNA_MIMIC_CLICK.get();
    }

    private static SoundEvent trill() {
        return GuaniaoSoundEvents.MYNA_MIMIC_TRILL.get();
    }

    private static SoundEvent hiss() {
        return GuaniaoSoundEvents.MYNA_MIMIC_HISS.get();
    }

    private static SoundEvent burst() {
        return GuaniaoSoundEvents.MYNA_MIMIC_BURST.get();
    }

    public record Syllable(int tick, SoundEvent sound, float volume, float pitch) {
        public void play(MynaEntity myna) {
            myna.level().playSound(null, myna, this.sound, SoundSource.NEUTRAL,
                    this.volume, Mth.clamp(this.pitch, 0.55F, 1.65F));
        }
    }

    public record Phrase(List<Syllable> syllables, int minimumDurationTicks) {
    }

    private static final class Builder {
        private final List<Syllable> syllables = new ArrayList<>();

        private Builder add(int tick, SoundEvent sound, float volume, float pitch) {
            this.syllables.add(new Syllable(Math.max(0, tick), sound, volume, pitch));
            return this;
        }

        private Phrase build() {
            this.syllables.sort((first, second) -> Integer.compare(first.tick(), second.tick()));
            int lastTick = this.syllables.isEmpty()
                    ? 0 : this.syllables.get(this.syllables.size() - 1).tick();
            return new Phrase(List.copyOf(this.syllables), lastTick + 20);
        }
    }
}
