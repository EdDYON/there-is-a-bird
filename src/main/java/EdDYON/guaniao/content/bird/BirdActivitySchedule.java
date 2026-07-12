package EdDYON.guaniao.content.bird;

public enum BirdActivitySchedule {
    DIURNAL(23000L, 12500L),
    COASTAL_DIURNAL(22000L, 13500L),
    NOCTURNAL_CREPUSCULAR(11000L, 3000L);

    private static final long DAY_LENGTH = 24000L;
    private final long activeStart;
    private final long activeEnd;

    BirdActivitySchedule(long activeStart, long activeEnd) {
        this.activeStart = activeStart;
        this.activeEnd = activeEnd;
    }

    public boolean isActiveTime(long dayTime) {
        long time = Math.floorMod(dayTime, DAY_LENGTH);
        if (this.activeStart <= this.activeEnd) {
            return time >= this.activeStart && time < this.activeEnd;
        }
        return time >= this.activeStart || time < this.activeEnd;
    }

    public boolean isRestTime(long dayTime) {
        return !this.isActiveTime(dayTime);
    }
}
