package EdDYON.guaniao.content.bird.umbrellacockatoo;

import net.minecraft.util.RandomSource;

/** Server-tick scheduling only; each bird owns a sequence and no queued audio is saved. */
final class CockatooCallSequence {
    enum Clip {
        // Durations measured from the mono Ogg files, before pitch adjustment.
        CALL_01(0.55), CALL_02(0.58), CALL_03(0.70), CALL_04(0.73),
        CALL_05(0.54), CALL_07(0.66), CALL_08(0.70),
        PHRASE_01(1.21), PHRASE_02(1.02), PHRASE_03(1.38), PHRASE_04(3.30);

        final double seconds;

        Clip(double seconds) {
            this.seconds = seconds;
        }

        boolean isPhrase() {
            return this.ordinal() >= SHORT_CLIPS;
        }
    }

    record Playback(Clip clip, float pitch, int durationTicks) {}

    private static final Clip[] CLIPS = Clip.values();
    private static final int SHORT_CLIPS = 7;
    private int remaining;
    private int nextClipTick;
    private int readyTick;
    private Clip previous;
    private boolean hasShort;
    private boolean hasPhrase;

    boolean start(int tick, boolean interaction, RandomSource random) {
        if (isBusy(tick)) {
            return false;
        }
        // Most ambient bouts, and every interaction bout, combine several clips.
        this.remaining = interaction || random.nextInt(100) < 85 ? 3 + random.nextInt(4) : 1;
        this.nextClipTick = tick;
        this.hasShort = false;
        this.hasPhrase = false;
        return true;
    }

    Playback poll(int tick, RandomSource random) {
        if (this.remaining == 0 || tick < this.nextClipTick) {
            return null;
        }
        int first = 0;
        int count = CLIPS.length;
        // Every multi-clip bout includes both a short call and a longer phrase.
        if (this.remaining == 1 && this.hasShort && !this.hasPhrase) {
            first = SHORT_CLIPS;
            count = CLIPS.length - SHORT_CLIPS;
        } else if (this.remaining == 1 && this.hasPhrase && !this.hasShort) {
            count = SHORT_CLIPS;
        }
        int previousIndex = this.previous == null ? -1 : this.previous.ordinal() - first;
        boolean excludePrevious = previousIndex >= 0 && previousIndex < count;
        int index = random.nextInt(count - (excludePrevious ? 1 : 0));
        if (excludePrevious && index >= previousIndex) {
            index++;
        }
        Clip clip = CLIPS[first + index];
        float pitch = 0.94F + random.nextFloat() * 0.16F;
        int durationTicks = (int)Math.ceil(clip.seconds * 20.0D / pitch);
        this.previous = clip;
        this.hasShort |= !clip.isPhrase();
        this.hasPhrase |= clip.isPhrase();
        this.remaining--;
        // A 1–3 tick breath between clips; pitch changes the playback duration.
        this.nextClipTick = tick + durationTicks + 1 + random.nextInt(3);
        this.readyTick = this.nextClipTick;
        if (this.remaining == 0) {
            this.readyTick += 40 + random.nextInt(41);
        }
        return new Playback(clip, pitch, durationTicks);
    }

    boolean isBusy(int tick) {
        return this.remaining > 0 || tick < this.readyTick;
    }

    void cancel() {
        // Drop future clips, but let an already-sent clip finish before restarting.
        this.remaining = 0;
    }
}
