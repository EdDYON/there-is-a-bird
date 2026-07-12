package EdDYON.guaniao.content.bird.columbid;

public final class SpottedDoveDefinition {
    public static final String ENTITY_ID = "spotted_dove";
    public static final String SPAWN_EGG_ID = "spotted_dove_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = ColumbidVariant.SPOTTED_DOVE.baseColor();
    public static final int SPAWN_EGG_SPOT_COLOR = ColumbidVariant.SPOTTED_DOVE.spotColor();
    public static final float WIDTH = 0.37F;
    public static final float HEIGHT = 0.50F;
    public static final double MAX_HEALTH = 8.0D;
    public static final double WALK_SPEED = 0.22D;
    public static final double FLYING_SPEED = 0.43D;
    public static final double FOLLOW_RANGE = 20.0D;

    private SpottedDoveDefinition() {
    }
}
