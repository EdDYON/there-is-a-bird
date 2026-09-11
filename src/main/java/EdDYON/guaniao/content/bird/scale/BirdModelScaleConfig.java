package EdDYON.guaniao.content.bird.scale;

public final class BirdModelScaleConfig {
    public static final Scale NIGHT_HERON = new Scale(0.82F, 0.94F, 1.06F);
    public static final Scale SPARROW = new Scale(0.36F, 0.94F, 1.06F);
    public static final Scale LONG_TAILED_TIT = new Scale(0.37F, 0.94F, 1.06F);
    public static final Scale BUDGERIGAR = new Scale(0.38F, 0.94F, 1.06F);
    public static final Scale COCKATIEL = new Scale(0.612F, 0.94F, 1.06F);
    public static final Scale MACAW = new Scale(1.14F, 0.94F, 1.06F);
    public static final Scale SPOTTED_DOVE = new Scale(0.40F, 0.94F, 1.06F);
    public static final Scale COLUMBID = new Scale(0.43F, 0.94F, 1.06F);
    public static final Scale CROW = new Scale(0.58F, 0.94F, 1.06F);
    public static final Scale SEAGULL = new Scale(0.68F, 0.94F, 1.06F);
    public static final Scale KIWI = new Scale(0.65F, 0.94F, 1.06F);
    public static final Scale MYNA = new Scale(0.55F, 0.94F, 1.06F);
    public static final Scale WOODCOCK = new Scale(0.46F, 0.94F, 1.06F);

    private BirdModelScaleConfig() {
    }

    public record Scale(float targetHeightBlocks,
                        float minIndividualScale,
                        float maxIndividualScale) {
        public Scale {
            if (!Float.isFinite(targetHeightBlocks) || targetHeightBlocks <= 0.0F) {
                throw new IllegalArgumentException("Target bird height must be positive and finite");
            }
            if (!Float.isFinite(minIndividualScale) || minIndividualScale <= 0.0F) {
                throw new IllegalArgumentException("Minimum individual scale must be positive and finite");
            }
            if (!Float.isFinite(maxIndividualScale) || maxIndividualScale < minIndividualScale) {
                throw new IllegalArgumentException("Maximum individual scale must be finite and at least the minimum");
            }
        }
    }
}
