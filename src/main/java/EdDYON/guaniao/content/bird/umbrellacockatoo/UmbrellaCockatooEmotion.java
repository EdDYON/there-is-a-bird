package EdDYON.guaniao.content.bird.umbrellacockatoo;

/**
 * The cockatoo's crest mood, kept separate from what the bird is physically doing.
 *
 * <p>Locomotion uses one GeckoLib controller; the client adds this emotion's
 * sampled local pose to the resulting base pose during rendering.</p>
 */
public enum UmbrellaCockatooEmotion {
    /** No overlay. The crest keeps whatever the movement animation does with it. */
    RELAXED(0),
    /** Noticed something worth studying: crest raises, the bird leans in and tilts its head. */
    CURIOUS(1),
    /** Something loud or sudden: crest up, body tense, ready to move. */
    ALERT(1),
    /** Delighted: owner arriving, being fed, flock display. */
    EXCITED(2),
    /** Right after a shock: full crest and feather display. */
    STARTLED(2);

    private final int overlayLevel;

    UmbrellaCockatooEmotion(int overlayLevel) {
        this.overlayLevel = overlayLevel;
    }

    /**
     * 0 = no overlay, 1 = emotion1 pair (crest only), 2 = emotion2 pair (crest,
     * crown feathers and tail accents). These are alternative targets for one layer.
     */
    public int overlayLevel() {
        return this.overlayLevel;
    }

    public boolean isOverlayActive() {
        return this.overlayLevel > 0;
    }

    public static UmbrellaCockatooEmotion byId(int id) {
        UmbrellaCockatooEmotion[] values = values();
        return id >= 0 && id < values.length ? values[id] : RELAXED;
    }
}
