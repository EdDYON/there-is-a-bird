package EdDYON.guaniao.content.bird.woodcock;

public enum WoodcockBehaviorState {
    IDLE,
    SPECIAL_IDLE,
    /** Kept for network ordinal compatibility; natural stillness is now an overlay flag. */
    @Deprecated
    STILL,
    STEP_1,
    ROCK_1,
    STEP_2,
    ROCK_2,
    NORMAL_WALK,
    FORAGE_APPROACH,
    FORAGING,
    EATING,
    ALERT,
    FREEZE,
    TAKEOFF,
    FLYING,
    LANDING,
    SEEK_COVER,
    SLEEP_PREP,
    SLEEPING;

    public boolean isRockWalk() {
        return this == STEP_1 || this == ROCK_1 || this == STEP_2 || this == ROCK_2;
    }

    public boolean isForagingSequence() {
        return this == FORAGE_APPROACH || this == FORAGING || this == EATING;
    }

    public boolean isSleeping() {
        return this == SLEEP_PREP || this == SLEEPING;
    }

    public boolean isAirborne() {
        return this == TAKEOFF || this == FLYING || this == LANDING;
    }

    public static WoodcockBehaviorState byId(int id) {
        WoodcockBehaviorState[] values = values();
        return id < 0 || id >= values.length ? IDLE : values[id];
    }
}
