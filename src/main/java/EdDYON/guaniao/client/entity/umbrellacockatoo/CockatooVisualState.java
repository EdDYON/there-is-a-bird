package EdDYON.guaniao.client.entity.umbrellacockatoo;

import EdDYON.guaniao.client.entity.umbrellacockatoo.CockatooExpressionSampler.Pose;

/** Per-entity transition history. Time is GeckoLib's paused-aware animation clock, in ticks. */
final class CockatooVisualState {
    private int target;
    private double startedAt;
    private double duration;
    private double lastTime = Double.NEGATIVE_INFINITY;
    private Pose from = Pose.ZERO;
    private boolean authoredOpening;

    CockatooVisualState() {}

    CockatooVisualState(int initialLevel) {
        this.target = initialLevel;
    }

    Pose sample(CockatooExpressionSampler curves, int nextTarget, double time) {
        if (time < lastTime) {
            target = 0;
            from = Pose.ZERO;
            duration = 0;
            authoredOpening = false;
        }
        if (nextTarget != target) {
            Pose current = current(curves, time);
            boolean wasClosed = target == 0 && time - startedAt >= duration;
            from = current;
            target = nextTarget;
            startedAt = time;
            authoredOpening = wasClosed && target != 0;
            duration = authoredOpening ? curves.openingTicks(target) : target == 0 ? 10 : 6;
        }
        lastTime = time;
        return current(curves, time);
    }

    private Pose current(CockatooExpressionSampler curves, double time) {
        double elapsed = Math.max(0, time - startedAt);
        if (authoredOpening) return curves.open(target, elapsed);
        if (duration == 0 || elapsed >= duration) return curves.hold(target);
        double u = elapsed / duration;
        return from.lerp(curves.hold(target), u * u * (3 - 2 * u));
    }
}
