package EdDYON.guaniao.content.bird.flight;

public final class BirdFlightProfile {
    public static final BirdFlightProfile SPARROW = new BirdFlightProfile(2.0D, 6.0D, 0.24D, 0.42D, 0.18D, 22, 122, 4.0D, 18.0D, 3.2D, 42.0F);
    public static final BirdFlightProfile BUDGERIGAR = new BirdFlightProfile(3.0D, 9.0D, 0.26D, 0.34D, 0.20D, 90, 260, 4.0D, 12.0D, 3.8D, 42.0F);
    public static final BirdFlightProfile COLUMBID = new BirdFlightProfile(12.0D, 24.0D, 0.38D, 0.44D, 0.24D, 520, 820, 24.0D, 68.0D, 9.5D, 40.0F);
    public static final BirdFlightProfile NIGHT_HERON = new BirdFlightProfile(7.0D, 36.0D, 0.40D, 0.55D, 0.24D, 80, 320, 18.0D, 64.0D, 9.0D, 36.0F);
    public static final BirdFlightProfile CROW = new BirdFlightProfile(7.0D, 18.0D, 0.34D, 0.46D, 0.22D, 150, 340, 10.0D, 34.0D, 7.0D, 38.0F);
    public static final BirdFlightProfile SEAGULL = new BirdFlightProfile(9.0D, 24.0D, 0.62D, 0.76D, 0.30D, 110, 220, 18.0D, 54.0D, 12.0D, 34.0F);

    private final double minCruiseHeight;
    private final double maxCruiseHeight;
    private final double cruiseSpeed;
    private final double escapeSpeed;
    private final double landingSpeed;
    private final int minFlightTicks;
    private final int maxFlightTicks;
    private final double minAirTargetDistance;
    private final double maxAirTargetDistance;
    private final double maxVerticalStep;
    private final float maxPitchDegrees;

    private BirdFlightProfile(double minCruiseHeight, double maxCruiseHeight, double cruiseSpeed, double escapeSpeed, double landingSpeed, int minFlightTicks, int maxFlightTicks, double minAirTargetDistance, double maxAirTargetDistance, double maxVerticalStep, float maxPitchDegrees) {
        this.minCruiseHeight = minCruiseHeight;
        this.maxCruiseHeight = maxCruiseHeight;
        this.cruiseSpeed = cruiseSpeed;
        this.escapeSpeed = escapeSpeed;
        this.landingSpeed = landingSpeed;
        this.minFlightTicks = minFlightTicks;
        this.maxFlightTicks = Math.max(minFlightTicks, maxFlightTicks);
        this.minAirTargetDistance = minAirTargetDistance;
        this.maxAirTargetDistance = Math.max(minAirTargetDistance, maxAirTargetDistance);
        this.maxVerticalStep = maxVerticalStep;
        this.maxPitchDegrees = maxPitchDegrees;
    }

    public double minCruiseHeight() {
        return this.minCruiseHeight;
    }

    public double maxCruiseHeight() {
        return this.maxCruiseHeight;
    }

    public double cruiseSpeed() {
        return this.cruiseSpeed;
    }

    public double escapeSpeed() {
        return this.escapeSpeed;
    }

    public double landingSpeed() {
        return this.landingSpeed;
    }

    public int minFlightTicks() {
        return this.minFlightTicks;
    }

    public int maxFlightTicks() {
        return this.maxFlightTicks;
    }

    public double minAirTargetDistance() {
        return this.minAirTargetDistance;
    }

    public double maxAirTargetDistance() {
        return this.maxAirTargetDistance;
    }

    public double maxVerticalStep() {
        return this.maxVerticalStep;
    }

    public float maxPitchDegrees() {
        return this.maxPitchDegrees;
    }
}
