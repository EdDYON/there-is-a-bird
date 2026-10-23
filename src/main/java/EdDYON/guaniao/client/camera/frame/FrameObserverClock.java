package EdDYON.guaniao.client.camera.frame;

/** A screen-local departure starts only after an observed job receives its completion. */
final class FrameObserverClock {
    static final float EXIT_TICKS=24;
    private static final long MAX_FRAME_NANOS=100_000_000L;
    private long seed,lastNanos;
    private float exitTicks;
    private boolean observed,departing,paused;

    void observe(long seed) {
        if(this.observed && this.seed==seed)return;
        this.clear();
        this.seed=seed;
        this.observed=true;
    }

    void complete(long seed,long nowNanos,boolean paused) {
        if(!this.observed || this.seed!=seed || this.departing)return;
        this.departing=true;
        this.lastNanos=nowNanos;
        this.paused=paused;
    }

    float renderExit(long nowNanos,boolean paused) {
        if(!this.departing)return 0;
        long elapsed=Math.max(0,nowNanos-this.lastNanos);
        this.lastNanos=nowNanos;
        if(!paused && !this.paused)this.exitTicks=Math.min(EXIT_TICKS,this.exitTicks+Math.min(MAX_FRAME_NANOS,elapsed)/50_000_000F);
        this.paused=paused;
        return this.exitTicks;
    }

    boolean isDeparting() {return this.departing;}
    long seed() {return this.seed;}

    void clear() {
        this.observed=false;
        this.departing=false;
        this.exitTicks=0;
        this.lastNanos=0;
        this.paused=false;
    }
}
