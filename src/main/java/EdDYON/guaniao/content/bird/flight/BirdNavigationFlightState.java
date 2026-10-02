package EdDYON.guaniao.content.bird.flight;

/** Tracks a navigation-created flight until the bird lands, independently of its AI state. */
final class BirdNavigationFlightState {
    private int lastNavigationTick = Integer.MIN_VALUE;
    private boolean active;

    void navigationMovedAt(int tick) {
        this.lastNavigationTick = tick;
    }

    boolean afterTravel(int tick, boolean onGround, boolean passenger, boolean inWater,
                        boolean noAi, boolean alive) {
        if (onGround || passenger || inWater || noAi || !alive) {
            this.active = false;
        } else if (this.lastNavigationTick == tick) {
            this.active = true;
        }
        return this.active;
    }
}
