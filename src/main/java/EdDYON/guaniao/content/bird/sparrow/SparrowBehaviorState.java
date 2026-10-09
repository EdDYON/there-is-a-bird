package EdDYON.guaniao.content.bird.sparrow;

public enum SparrowBehaviorState {
    IDLE(false),
    LOOK_AROUND(false),
    PECKING(false),
    FORAGING(false),
    ALERT(false),
    FLEEING(false),
    SHORT_FLIGHT(true),
    PERCHING(false),
    ROOSTING(false),
    FOLLOWING_OWNER(false);

    private final boolean airborne;

    SparrowBehaviorState(boolean airborne) {
        this.airborne = airborne;
    }

    public boolean isAirborne() {
        return this.airborne;
    }

    public boolean isEscape() {
        return this == FLEEING || this == SHORT_FLIGHT;
    }
}
