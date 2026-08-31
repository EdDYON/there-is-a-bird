package EdDYON.guaniao.content.bird.scale;

public final class BirdModelScaleProfile {
    private static final float MIN_INDIVIDUAL_SCALE = 0.75F;
    private static final float MAX_INDIVIDUAL_SCALE = 1.25F;
    private static final float SMALL_PARROT_MIN_INDIVIDUAL_SCALE = MIN_INDIVIDUAL_SCALE + 0.20F;
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
    public static final BirdModelScaleProfile BUDGERIGAR = realBodyMassProfile(
            30.0F, 13.572F, SMALL_PARROT_MIN_INDIVIDUAL_SCALE, MAX_INDIVIDUAL_SCALE);
    public static final BirdModelScaleProfile COCKATIEL = realBodyMassProfile(
            85.0F, 16.058F, SMALL_PARROT_MIN_INDIVIDUAL_SCALE, MAX_INDIVIDUAL_SCALE);
    public static final BirdModelScaleProfile MACAW = realBodyMassProfile(1200.0F, 20.709F);
    public static final BirdModelScaleProfile COLUMBID = realBodyMassProfile(322.5F, 9.897F);
    public static final BirdModelScaleProfile SPOTTED_DOVE = realBodyMassProfile(157.5F, 9.897F);
    public static final BirdModelScaleProfile CROW = realBodyMassProfile(
            468.0F, 16.265F, MIN_INDIVIDUAL_SCALE, CROW_MAX_INDIVIDUAL_SCALE);
    public static final BirdModelScaleProfile SEAGULL = realBodyMassProfile(1025.0F, 19.586F);
    // The supplied Kiwi model's median raw extent is about 17 px. A 1.65x
    // sparrow linear-size target keeps the rendered bird near 0.65 blocks tall.
    public static final BirdModelScaleProfile KIWI = relativeLinearSizeProfile(1.65F, 17.034F);
    // Supplied model median extent is 13.045 px; 1.35x sparrow linear size yields a roughly 0.52-block adult.
    public static final BirdModelScaleProfile MYNA = relativeLinearSizeProfile(1.35F, 13.045F);

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
        float sourceModelCorrection = SPARROW_MODEL_MEDIAN_EXTENT / sourceModelMedianExtent;
        return new BirdModelScaleProfile(
                sparrowLinearRatio * sourceModelCorrection,
                MIN_INDIVIDUAL_SCALE,
                MAX_INDIVIDUAL_SCALE
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
