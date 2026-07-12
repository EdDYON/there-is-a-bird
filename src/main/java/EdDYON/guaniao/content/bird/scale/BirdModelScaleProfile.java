package EdDYON.guaniao.content.bird.scale;

public final class BirdModelScaleProfile {
    public static final BirdModelScaleProfile NIGHT_HERON = new BirdModelScaleProfile(1.0F, 0.90F, 1.10F);
    public static final BirdModelScaleProfile SPARROW = new BirdModelScaleProfile(1.0F, 0.86F, 1.14F);
    public static final BirdModelScaleProfile BUDGERIGAR = new BirdModelScaleProfile(0.6F, 0.84F, 1.16F);
    public static final BirdModelScaleProfile COCKATIEL = new BirdModelScaleProfile(0.72F, 0.88F, 1.12F);
    public static final BirdModelScaleProfile COLUMBID = new BirdModelScaleProfile(1.0F, 0.90F, 1.10F);
    public static final BirdModelScaleProfile SPOTTED_DOVE = new BirdModelScaleProfile(0.92F, 0.90F, 1.10F);
    public static final BirdModelScaleProfile CROW = new BirdModelScaleProfile(1.0F, 0.88F, 1.12F);
    public static final BirdModelScaleProfile SEAGULL = new BirdModelScaleProfile(1.0F, 0.88F, 1.12F);

    private final float baseRenderScale;
    private final float minIndividualScale;
    private final float maxIndividualScale;

    private BirdModelScaleProfile(float baseRenderScale, float minIndividualScale, float maxIndividualScale) {
        this.baseRenderScale = baseRenderScale;
        this.minIndividualScale = minIndividualScale;
        this.maxIndividualScale = maxIndividualScale;
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
