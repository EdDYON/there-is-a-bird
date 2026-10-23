package EdDYON.guaniao.content.bird.cockatoo;

import net.minecraft.resources.ResourceLocation;

public final class CockatooDefinition {
    public static final String ENTITY_ID = "cockatoo";
    public static final String SPAWN_EGG_ID = "cockatoo_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0xEEEDE4;
    public static final int SPAWN_EGG_SPOT_COLOR = 0xE8CF59;
    public static final float WIDTH = 0.44F;
    public static final float HEIGHT = 0.704F;
    public static final double MAX_HEALTH = 14.0D;
    public static final double WALK_SPEED = 0.23D;
    public static final double FLYING_SPEED = 0.37D;
    public static final double FOLLOW_RANGE = 28.0D;
    // Authored toes sit slightly below the origin after the leg/cube rotations.
    public static final float FOOT_OFFSET_PIXELS = 0.751F;
    public static final ResourceLocation MODEL = resource("geo/cockatoo.geo.json");
    public static final ResourceLocation ANIMATION = resource("animations/cockatoo.animation.json");
    public static final ResourceLocation TEXTURE = resource("textures/entity/cockatoo.png");

    private CockatooDefinition() { }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation("guaniao", path);
    }
}
