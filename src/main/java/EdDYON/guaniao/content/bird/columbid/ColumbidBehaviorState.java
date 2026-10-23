package EdDYON.guaniao.content.bird.columbid;

public enum ColumbidBehaviorState {
    IDLE(false),
    PREENING(false),
    CURIOUS(false),
    FORAGING(false),
    EATING(false),
    WALKING(false),
    ALERT(false),
    FLEEING(true),
    FLYING(true),
    GLIDING(true),
    FLAP_FLYING(true),
    ROOSTING(false),
    SLEEPING(false),
    FOLLOWING_OWNER(false),
    PAIR_FOLLOWING(false),
    COURTING(false),
    CHASING(false);

    private final boolean airborne;

    ColumbidBehaviorState(boolean airborne) {
        this.airborne = airborne;
    }

    public boolean isAirborne() {
        return this.airborne;
    }

    public boolean isEscape() {
        return this == FLEEING || this == FLYING || this == FLAP_FLYING;
    }
}
