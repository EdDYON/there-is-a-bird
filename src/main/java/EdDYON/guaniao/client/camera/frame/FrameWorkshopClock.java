package EdDYON.guaniao.client.camera.frame;

import java.util.ArrayDeque;

/** Interpolates confirmed work samples; it never predicts completion or awards an item. */
final class FrameWorkshopClock {
    private static final long BUFFER_NANOS=100_000_000L;
    private static final long MAX_FRAME_NANOS=100_000_000L;
    private final ArrayDeque<Sample> samples=new ArrayDeque<>();
    private long lastNanos,clockNanos;
    private float position;
    private int confirmed;
    private boolean paused;

    void reset(int elapsed,long nowNanos) {
        this.samples.clear();
        this.lastNanos=nowNanos;
        this.clockNanos=0;
        this.position=Math.max(0,Math.min(300,elapsed));
        this.confirmed=(int)this.position;
        this.paused=false;
        this.samples.addLast(new Sample(0,this.position));
    }

    void observe(int elapsed,long nowNanos,boolean paused) {
        this.advanceTime(nowNanos,paused);
        int value=Math.max(0,Math.min(300,elapsed));
        if(value<=this.confirmed)return;
        this.confirmed=value;
        if(paused)return;
        if(!this.samples.isEmpty() && this.samples.peekLast().time()==this.clockNanos) {
            this.samples.removeLast();
            if(this.samples.isEmpty())this.samples.addLast(new Sample(this.clockNanos-BUFFER_NANOS,this.position));
        }
        this.samples.addLast(new Sample(this.clockNanos,value));
        while(this.samples.size()>32)this.samples.removeFirst();
    }

    float render(long nowNanos,boolean paused) {
        this.advanceTime(nowNanos,paused);
        if(paused || this.samples.isEmpty())return this.position;
        long time=this.clockNanos-BUFFER_NANOS;
        while(this.samples.size()>1) {
            var iterator=this.samples.iterator();
            iterator.next();
            if(iterator.next().time()>time)break;
            this.samples.removeFirst();
        }
        Sample first=this.samples.peekFirst();
        float next=first.ticks();
        if(time>first.time() && this.samples.size()>1) {
            var iterator=this.samples.iterator();
            iterator.next();
            Sample second=iterator.next();
            float fraction=Math.max(0,Math.min(1,(float)(time-first.time())/(second.time()-first.time())));
            next=first.ticks()+(second.ticks()-first.ticks())*fraction;
        }
        this.position=Math.max(this.position,Math.min(this.confirmed,next));
        return this.position;
    }

    float position() {return this.position;}

    private void advanceTime(long nowNanos,boolean paused) {
        long elapsed=Math.max(0,nowNanos-this.lastNanos);
        this.lastNanos=nowNanos;
        if(!paused && !this.paused)this.clockNanos+=Math.min(MAX_FRAME_NANOS,elapsed);
        else if(!paused) {
            this.samples.clear();
            this.samples.addLast(new Sample(this.clockNanos-BUFFER_NANOS,this.position));
            if(this.confirmed>this.position)this.samples.addLast(new Sample(this.clockNanos,this.confirmed));
        }
        this.paused=paused;
    }

    private record Sample(long time,float ticks) { }
}
