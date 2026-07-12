package EdDYON.guaniao.content.bath;

public enum BirdBathCleanliness {
    CLEAN,
    USED,
    DIRTY,
    FILTHY;

    public static BirdBathCleanliness fromOrdinal(int ordinal) {
        BirdBathCleanliness[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return CLEAN;
        }
        return values[ordinal];
    }

    public boolean isDirty() {
        return this != CLEAN;
    }

    public BirdBathCleanliness nextDirtier() {
        return switch (this) {
            case CLEAN -> USED;
            case USED -> DIRTY;
            case DIRTY, FILTHY -> FILTHY;
        };
    }

    public BirdBathCleanliness cleanOneStep() {
        return switch (this) {
            case FILTHY -> DIRTY;
            case DIRTY -> USED;
            case USED -> CLEAN;
            case CLEAN -> CLEAN;
        };
    }

    public int particleIntensity() {
        return switch (this) {
            case CLEAN -> 0;
            case USED -> 2;
            case DIRTY -> 5;
            case FILTHY -> 8;
        };
    }
}
