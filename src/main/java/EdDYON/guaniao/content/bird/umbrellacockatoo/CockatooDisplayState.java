package EdDYON.guaniao.content.bird.umbrellacockatoo;

/** Client animation choreography only; never delays navigation, eating, sleep or escape. */
public final class CockatooDisplayState {
    private static final double OPEN_TICKS = 10;
    private static final double BODY_TICKS = 60;
    private static final double EXIT_TICKS = 5;
    private double startedAt = Double.NEGATIVE_INFINITY;
    private double exitAt = Double.NEGATIVE_INFINITY;
    private boolean preview;

    public void request(double time) {
        if (!holdsFullDisplay(time)) {
            startedAt = time;
            exitAt = time + OPEN_TICKS + BODY_TICKS;
        }
    }

    public void update(double time, boolean allowed, boolean fullPreview) {
        if (fullPreview && !preview) {
            startedAt = time;
            exitAt = Double.POSITIVE_INFINITY;
        } else if ((!allowed || preview && !fullPreview) && time < exitAt) {
            // Keep the reference until the body/residual's five-tick exit blend is over.
            exitAt = time;
        }
        preview = fullPreview;
    }

    public boolean playsBody(double time) {
        return time >= startedAt + OPEN_TICKS && time < exitAt;
    }

    public boolean holdsFullDisplay(double time) {
        return time >= startedAt && time < exitAt + EXIT_TICKS;
    }

    public boolean needsFullReference(double time) {
        return time >= startedAt + OPEN_TICKS && holdsFullDisplay(time);
    }
}
