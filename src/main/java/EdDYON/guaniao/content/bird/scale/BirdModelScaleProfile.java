package EdDYON.guaniao.content.bird.scale;

import EdDYON.guaniao.content.bird.woodcock.WoodcockDefinition;

public final class BirdModelScaleProfile {
    public static final float PLAYER_HEIGHT_BLOCKS = 1.80F;

    // Scale every species from its authored standing height. Sixteen model pixels
    // equal one Minecraft block, so the requested in-world height is explicit and
    // is no longer inferred indirectly from body mass or sparrow-relative ratios.
    public static final BirdModelScaleProfile NIGHT_HERON = visualHeightProfile(
            BirdModelScaleConfig.NIGHT_HERON, 17.251F);
    public static final BirdModelScaleProfile SPARROW = visualHeightProfile(
            BirdModelScaleConfig.SPARROW, 6.120F);
    public static final BirdModelScaleProfile LONG_TAILED_TIT = visualHeightProfile(
            BirdModelScaleConfig.LONG_TAILED_TIT, 7.663F);
    public static final BirdModelScaleProfile BUDGERIGAR = visualHeightProfile(
            BirdModelScaleConfig.BUDGERIGAR, 13.572F);
    public static final BirdModelScaleProfile COCKATIEL = visualHeightProfile(
            BirdModelScaleConfig.COCKATIEL, 17.628F);
    public static final BirdModelScaleProfile MACAW = visualHeightProfile(
            BirdModelScaleConfig.MACAW, 20.796F);
    public static final BirdModelScaleProfile SPOTTED_DOVE = visualHeightProfile(
            BirdModelScaleConfig.SPOTTED_DOVE, 9.897F);
    public static final BirdModelScaleProfile COLUMBID = visualHeightProfile(
            BirdModelScaleConfig.COLUMBID, 9.897F);
    public static final BirdModelScaleProfile CROW = visualHeightProfile(
            BirdModelScaleConfig.CROW, 16.267F);
    public static final BirdModelScaleProfile SEAGULL = visualHeightProfile(
            BirdModelScaleConfig.SEAGULL, 16.935F);
    public static final BirdModelScaleProfile KIWI = visualHeightProfile(
            BirdModelScaleConfig.KIWI, 17.400F);
    public static final BirdModelScaleProfile MYNA = visualHeightProfile(
            BirdModelScaleConfig.MYNA, 11.969F);
    // Woodcock animations permanently scale root_shape to 0.45. Compensate here
    // so its final visible height is still 0.36 blocks without restoring the old
    // transition-time inflation bug.
    public static final BirdModelScaleProfile WOODCOCK = visualHeightProfile(
            BirdModelScaleConfig.WOODCOCK, 19.950F, WoodcockDefinition.MODEL_ROOT_SCALE);
    public static final BirdModelScaleProfile KESTREL = visualHeightProfile(
            BirdModelScaleConfig.KESTREL, 9.980F);
    public static final BirdModelScaleProfile CASSOWARY = visualHeightProfile(
            BirdModelScaleConfig.CASSOWARY, 29.424F);
    // Measured with _measure_model_height.py (rest-pose Y span, crest raised).
    public static final BirdModelScaleProfile UMBRELLA_COCKATOO = visualHeightProfile(
            BirdModelScaleConfig.UMBRELLA_COCKATOO, 18.307F);

    private final float baseRenderScale;
    private final float targetHeightBlocks;
    private final float minIndividualScale;
    private final float maxIndividualScale;

    private BirdModelScaleProfile(float baseRenderScale, float targetHeightBlocks,
                                  float minIndividualScale, float maxIndividualScale) {
        this.baseRenderScale = baseRenderScale;
        this.targetHeightBlocks = targetHeightBlocks;
        this.minIndividualScale = minIndividualScale;
        this.maxIndividualScale = maxIndividualScale;
    }

    private static BirdModelScaleProfile visualHeightProfile(BirdModelScaleConfig.Scale config,
                                                              float sourceStandingHeightPixels) {
        return visualHeightProfile(config, sourceStandingHeightPixels, 1.0F);
    }

    private static BirdModelScaleProfile visualHeightProfile(BirdModelScaleConfig.Scale config,
                                                              float sourceStandingHeightPixels,
                                                              float authoredRootScale) {
        float baseRenderScale = config.targetHeightBlocks() * 16.0F
                / (sourceStandingHeightPixels * authoredRootScale);
        return new BirdModelScaleProfile(
                baseRenderScale,
                config.targetHeightBlocks(),
                config.minIndividualScale(),
                config.maxIndividualScale()
        );
    }

    public float baseRenderScale() {
        return this.baseRenderScale;
    }

    public float targetHeightBlocks() {
        return this.targetHeightBlocks;
    }

    public float minIndividualScale() {
        return this.minIndividualScale;
    }

    public float maxIndividualScale() {
        return this.maxIndividualScale;
    }
}
