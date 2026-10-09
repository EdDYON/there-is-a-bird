package EdDYON.guaniao.content.bird.flight;

public interface BirdFlightAware {
    BirdFlightProfile birdFlightProfile();

    boolean isBirdFlightActive();

    /** Allows route planning through leaves before a special-purpose flight has fully started. */
    default boolean shouldPassThroughLeaves() {
        return false;
    }

    default boolean isBirdLanding() {
        return false;
    }

    default boolean isBirdEscaping() {
        return false;
    }
}
