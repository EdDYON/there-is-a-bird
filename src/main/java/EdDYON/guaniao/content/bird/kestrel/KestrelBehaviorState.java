package EdDYON.guaniao.content.bird.kestrel;

public enum KestrelBehaviorState {
    PERCHED(false),
    TAKEOFF(true),
    PATROL(true),
    HOVER_SEARCH(true),
    TARGET_LOCKED(true),
    DIVE_ATTACK(true),
    RECOVER(true),
    LANDING(true),
    SLEEP(false),
    FLEE(true),
    FOLLOW_OWNER(true),
    STAY(false),
    HOME(true),
    SCOUT(true),
    ASSIST_ATTACK(true),
    FETCH(true),
    CARRY_PREY(true),
    OWNER_PERCH(false);

    private static final KestrelBehaviorState[] VALUES = values();
    private final boolean airborne;

    KestrelBehaviorState(boolean airborne) {
        this.airborne = airborne;
    }

    public boolean isAirborne() {
        return this.airborne;
    }

    public static KestrelBehaviorState byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : PERCHED;
    }
}
