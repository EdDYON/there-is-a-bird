package EdDYON.guaniao.content.bird.columbid;

public final class PigeonDefinition {
    public static final String ENTITY_ID = "pigeon";
    public static final String SPAWN_EGG_ID = "pigeon_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = ColumbidVariant.GRAY_PIGEON.baseColor();
    public static final int SPAWN_EGG_SPOT_COLOR = ColumbidVariant.WHITE_PIGEON.baseColor();
    public static final float WIDTH = 0.40F;
    public static final float HEIGHT = 0.54F;
    public static final double MAX_HEALTH = 8.0D;
    public static final double WALK_SPEED = 0.22D;
    public static final double FLYING_SPEED = 0.42D;
    public static final double FOLLOW_RANGE = 18.0D;

    private PigeonDefinition() {
    }
}
