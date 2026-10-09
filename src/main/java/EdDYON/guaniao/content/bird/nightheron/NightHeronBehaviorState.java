package EdDYON.guaniao.content.bird.nightheron;

public enum NightHeronBehaviorState {
    IDLE(false),
    REST_STAND(false),
    LOOK_AROUND(false),
    MICRO_STROLL(false),
    PREEN(false),
    NECK_STRETCH(false),
    WATER_EDGE_WAIT(false),
    ALERT_FREEZE(false),
    WALK_ESCAPE(false),
    RUN_ESCAPE(false),
    FORAGING(false),
    EATING(false),
    ROOSTING(false),
    SOCIAL_SPACING(false),
    TAKEOFF(true),
    LOCAL_FLIGHT(true),
    LOW_FLAP_ESCAPE(true),
    LONG_FLIGHT_ESCAPE(true),
    CLIMB(true),
    HIGH_TRANSIT(true),
    SOARING(true),
    GLIDE(true),
    LANDING(true);

    private final boolean airborne;

    private NightHeronBehaviorState(boolean airborne) {
        this.airborne = airborne;
    }

    public boolean isAirborne() {
        return this.airborne;
    }

    public boolean isEscape() {
        return this == WALK_ESCAPE || this == RUN_ESCAPE || this == LOW_FLAP_ESCAPE || this == LONG_FLIGHT_ESCAPE;
    }
}
