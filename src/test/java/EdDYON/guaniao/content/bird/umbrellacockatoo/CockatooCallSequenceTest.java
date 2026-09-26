package EdDYON.guaniao.content.bird.umbrellacockatoo;

import java.util.EnumSet;
import net.minecraft.util.RandomSource;

public final class CockatooCallSequenceTest {
    public static void main(String[] args) {
        RandomSource random = RandomSource.create(20260925L);
        CockatooCallSequence sequence = new CockatooCallSequence();
        EnumSet<CockatooCallSequence.Clip> heard = EnumSet.noneOf(CockatooCallSequence.Clip.class);
        CockatooCallSequence.Clip previous = null;
        int tick = 0;
        int ambientBouts = 5000;
        int continuousAmbient = 0;
        int totalClips = 0;
        boolean heardSlowLongPhrase = false;
        for (int bout = 0; bout < ambientBouts + 1000; bout++) {
            boolean interaction = bout >= ambientBouts;
            require(sequence.start(tick, interaction, random), "Ready bird must be able to start");
            int startTick = tick;
            int lastEnd = -1;
            int clips = 0;
            boolean shortCall = false;
            boolean phrase = false;
            while (sequence.isBusy(tick)) {
                require(tick - startTick < 600, "A bout must end, even when spammed with requests");
                require(!sequence.start(tick, !interaction, random), "Do not restart or extend a busy bout");
                CockatooCallSequence.Playback playback = sequence.poll(tick, random);
                if (playback != null) {
                    if (lastEnd >= 0) {
                        require(tick - lastEnd >= 1 && tick - lastEnd <= 3,
                                "Clips must finish, then take only a brief breath");
                    } else {
                        require(tick == startTick, "First call should play immediately");
                    }
                    require(playback.clip() != previous, "Do not repeat neighbouring clips");
                    double seconds = playback.clip().seconds / playback.pitch();
                    double scheduledSeconds = playback.durationTicks() / 20.0D;
                    require(scheduledSeconds >= seconds && scheduledSeconds < seconds + 0.05D,
                            "Pitch-adjusted clip duration must be rounded up to the next tick");
                    require(playback.pitch() >= 0.94F && playback.pitch() < 1.10F, "Natural pitch range");
                    require(sequence.poll(tick, random) == null, "Never send a clip twice in the same tick");
                    heardSlowLongPhrase |= playback.clip() == CockatooCallSequence.Clip.PHRASE_04
                            && playback.pitch() < 1.0F && playback.durationTicks() > 66;
                    lastEnd = tick + playback.durationTicks();
                    previous = playback.clip();
                    shortCall |= !playback.clip().isPhrase();
                    phrase |= playback.clip().isPhrase();
                    heard.add(playback.clip());
                    clips++;
                }
                tick++;
            }
            require(tick - lastEnd >= 41 && tick - lastEnd <= 83, "Leave a rest after each bout");
            require(clips == 1 || (clips >= 3 && clips <= 6), "Bounded bout length");
            if (clips > 1) {
                require(shortCall && phrase, "Every continuous bout must mix both types of recording");
                if (!interaction) continuousAmbient++;
            }
            if (interaction) require(clips >= 3, "Interactions should use continuous calls");
            totalClips += clips;
        }
        double continuousRate = continuousAmbient / (double)ambientBouts;
        require(continuousRate > 0.82D && continuousRate < 0.88D, "Most ambient calls should be continuous");
        require(heard.size() == 11, "All seven short calls and four phrases must be reachable");
        require(heardSlowLongPhrase, "Exercise the long recording at a slower playback pitch");

        CockatooCallSequence firstBird = new CockatooCallSequence();
        CockatooCallSequence secondBird = new CockatooCallSequence();
        require(firstBird.start(0, true, random) && secondBird.start(0, true, random), "Independent birds");
        CockatooCallSequence.Playback first = firstBird.poll(0, random);
        firstBird.cancel();
        require(!firstBird.start(1, true, random), "A cancelled bout must not overlap its sent clip");
        for (int t = 1; t < 100; t++) {
            require(firstBird.poll(t, random) == null, "Sleep/silence cancels every queued clip");
        }
        require(secondBird.poll(0, random) != null, "Cancelling one bird must not affect another");
        require(firstBird.start(first.durationTicks() + 4, true, random), "Bird can call again after waking");
        require(firstBird.poll(first.durationTicks() + 4, random) != null, "New bout after cancellation");

        System.out.println("CockatooCallSequenceTest passed: 6000 bouts, " + totalClips
                + " clips; continuous ambient rate " + continuousRate
                + "; all 11 recordings, timing, spam, cancellation and isolation checked");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
