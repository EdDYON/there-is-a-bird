package EdDYON.guaniao.content.bird.sparrow;

import net.minecraft.resources.ResourceLocation;

public final class SparrowDefinition {
    public static final String ENTITY_ID = "sparrow";
    public static final String SPAWN_EGG_ID = "sparrow_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0x8A6E4D;
    public static final int SPAWN_EGG_SPOT_COLOR = 0xD7C7A2;
    public static final float WIDTH = 0.32f;
    public static final float HEIGHT = 0.38f;
    public static final double MAX_HEALTH = 6.0;
    public static final double WALK_SPEED = 0.25;
    public static final double FOLLOW_RANGE = 18.0;
    public static final double SOCIAL_RADIUS = 9.0;
    public static final ResourceLocation MODEL = SparrowDefinition.resource("geo/sparrow.geo.json");
    public static final ResourceLocation TEXTURE = SparrowDefinition.resource("textures/entity/sparrow.png");
    public static final ResourceLocation ANIMATION = SparrowDefinition.resource("animations/sparrow.animation.json");

    private SparrowDefinition() {
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation("guaniao", path);
    }
}
