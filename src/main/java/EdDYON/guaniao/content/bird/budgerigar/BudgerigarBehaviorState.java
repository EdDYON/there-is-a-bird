package EdDYON.guaniao.content.bird.budgerigar;

public enum BudgerigarBehaviorState {
    IDLE(false),
    PREENING(false),
    CURIOUS(false),
    DANCING(false),
    FORAGING(false),
    EATING(false),
    SLEEPING(false),
    WALKING(false),
    FLYING(true),
    ALERT(false),
    FLEEING(true),
    FOLLOWING(false),
    PERCHING(false),
    ROOSTING(false),
    SENTINEL(false);

    private final boolean airborne;

    BudgerigarBehaviorState(boolean airborne) {
        this.airborne = airborne;
    }

    public boolean isAirborne() {
        return this.airborne;
    }

    public boolean isEscape() {
        return this == FLEEING || this == FLYING;
    }
}
