package EdDYON.guaniao.content.bird.scale;

public final class BirdModelScaleProfile {
    private static final float MIN_INDIVIDUAL_SCALE = 0.75F;
    private static final float MAX_INDIVIDUAL_SCALE = 1.25F;
    private static final float SMALL_PARROT_MIN_INDIVIDUAL_SCALE = MIN_INDIVIDUAL_SCALE + 0.20F;
    private static final float SMALL_PARROT_MAX_INDIVIDUAL_SCALE = MAX_INDIVIDUAL_SCALE - 0.10F;
    private static final float MEDIUM_BIRD_MIN_INDIVIDUAL_SCALE = MIN_INDIVIDUAL_SCALE + 0.10F;
    private static final float MEDIUM_BIRD_MAX_INDIVIDUAL_SCALE = MAX_INDIVIDUAL_SCALE - 0.13F;
    private static final float PIGEON_MAX_INDIVIDUAL_SCALE = 1.10F;
    private static final float SEAGULL_MAX_INDIVIDUAL_SCALE = MAX_INDIVIDUAL_SCALE - 0.25F;
    private static final float MYNA_MAX_INDIVIDUAL_SCALE = MAX_INDIVIDUAL_SCALE - 0.17F;
    private static final float CROW_MAX_INDIVIDUAL_SCALE = MAX_INDIVIDUAL_SCALE - 0.18F;

    // Linear body size is derived from the cube root of representative adult
    // body mass, with a 28.5 g house sparrow as 1.0. This avoids treating long
    // tails as body bulk. Model median extents come from the fully transformed
    // geo AABB; using the median axis also avoids open wings dominating scale.
    private static final float SPARROW_MASS_GRAMS = 28.5F;
    private static final float SPARROW_MODEL_MEDIAN_EXTENT = 6.120F;

    public static final BirdModelScaleProfile NIGHT_HERON = realBodyMassProfile(870.5F, 20.619F);
    public static final BirdModelScaleProfile SPARROW = realBodyMassProfile(SPARROW_MASS_GRAMS, SPARROW_MODEL_MEDIAN_EXTENT);
    // Long-tailed tits are much lighter than sparrows, but their adult linear
    // body length is similar. Match the visible model length instead of using
    // mass-derived bulk so the two species read as roughly the same size.
    public static final BirdModelScaleProfile LONG_TAILED_TIT = relativeLinearSizeProfile(0.98F, 7.661F);
    public static final BirdModelScaleProfile BUDGERIGAR = relativeLinearSizeProfile(
            0.90F, 13.572F, SMALL_PARROT_MIN_INDIVIDUAL_SCALE, SMALL_PARROT_MAX_INDIVIDUAL_SCALE);
    public static final BirdModelScaleProfile COCKATIEL = relativeLinearSizeProfile(
            1.25F, 16.058F, SMALL_PARROT_MIN_INDIVIDUAL_SCALE, SMALL_PARROT_MAX_INDIVIDUAL_SCALE);
    // Keep the shared visual hierarchy compact: dove < pigeon < macaw < myna < crow.
    public static final BirdModelScaleProfile SPOTTED_DOVE = relativeLinearSizeProfile(
            1.75F, 9.897F, MEDIUM_BIRD_MIN_INDIVIDUAL_SCALE, MEDIUM_BIRD_MAX_INDIVIDUAL_SCALE);
    public static final BirdModelScaleProfile COLUMBID = relativeLinearSizeProfile(
            1.88F, 9.897F, MEDIUM_BIRD_MIN_INDIVIDUAL_SCALE, PIGEON_MAX_INDIVIDUAL_SCALE);
    public static final BirdModelScaleProfile MACAW = relativeLinearSizeProfile(
            2.02F, 20.709F, MEDIUM_BIRD_MIN_INDIVIDUAL_SCALE, MEDIUM_BIRD_MAX_INDIVIDUAL_SCALE);
    public static final BirdModelScaleProfile CROW = realBodyMassProfile(
            468.0F, 16.265F, MIN_INDIVIDUAL_SCALE, CROW_MAX_INDIVIDUAL_SCALE);
    public static final BirdModelScaleProfile SEAGULL = realBodyMassProfile(
            1025.0F, 19.586F, MIN_INDIVIDUAL_SCALE, SEAGULL_MAX_INDIVIDUAL_SCALE);
    // The supplied Kiwi model's median raw extent is about 17 px. A 1.75x
    // sparrow linear-size target keeps the rendered bird near 0.69 blocks tall.
    public static final BirdModelScaleProfile KIWI = relativeLinearSizeProfile(1.75F, 17.034F);
    // Slightly smaller than the crow while remaining visibly larger than the macaw.
    public static final BirdModelScaleProfile MYNA = relativeLinearSizeProfile(
            2.35F, 13.045F, MEDIUM_BIRD_MIN_INDIVIDUAL_SCALE, MYNA_MAX_INDIVIDUAL_SCALE);

    private final float baseRenderScale;
    private final float minIndividualScale;
    private final float maxIndividualScale;

    private BirdModelScaleProfile(float baseRenderScale, float minIndividualScale, float maxIndividualScale) {
        this.baseRenderScale = baseRenderScale;
        this.minIndividualScale = minIndividualScale;
        this.maxIndividualScale = maxIndividualScale;
    }

    private static BirdModelScaleProfile realBodyMassProfile(float adultMassGrams, float sourceModelMedianExtent) {
        return realBodyMassProfile(adultMassGrams, sourceModelMedianExtent, MIN_INDIVIDUAL_SCALE, MAX_INDIVIDUAL_SCALE);
    }

    private static BirdModelScaleProfile realBodyMassProfile(float adultMassGrams, float sourceModelMedianExtent,
                                                              float minIndividualScale, float maxIndividualScale) {
        float realWorldLinearRatio = (float) Math.cbrt(adultMassGrams / SPARROW_MASS_GRAMS);
        float sourceModelCorrection = SPARROW_MODEL_MEDIAN_EXTENT / sourceModelMedianExtent;
        return new BirdModelScaleProfile(
                realWorldLinearRatio * sourceModelCorrection,
                minIndividualScale,
                maxIndividualScale
        );
    }

    private static BirdModelScaleProfile relativeLinearSizeProfile(float sparrowLinearRatio, float sourceModelMedianExtent) {
        return relativeLinearSizeProfile(
                sparrowLinearRatio, sourceModelMedianExtent, MIN_INDIVIDUAL_SCALE, MAX_INDIVIDUAL_SCALE);
    }

    private static BirdModelScaleProfile relativeLinearSizeProfile(float sparrowLinearRatio,
                                                                    float sourceModelMedianExtent,
                                                                    float minIndividualScale,
                                                                    float maxIndividualScale) {
        float sourceModelCorrection = SPARROW_MODEL_MEDIAN_EXTENT / sourceModelMedianExtent;
        return new BirdModelScaleProfile(
                sparrowLinearRatio * sourceModelCorrection,
                minIndividualScale,
                maxIndividualScale
        );
    }

    public float baseRenderScale() {
        return this.baseRenderScale;
    }

    public float minIndividualScale() {
        return this.minIndividualScale;
    }

    public float maxIndividualScale() {
        return this.maxIndividualScale;
    }
}
