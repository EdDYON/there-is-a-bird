package EdDYON.guaniao.content.bird.crow;

import net.minecraft.resources.ResourceLocation;

public final class CrowDefinition {
    public static final String ENTITY_ID = "crow";
    public static final String SPAWN_EGG_ID = "crow_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0x141820;
    public static final int SPAWN_EGG_SPOT_COLOR = 0x6D7582;
    public static final float WIDTH = 0.52F;
    public static final float HEIGHT = 0.72F;
    public static final double MAX_HEALTH = 8.0D;
    public static final double WALK_SPEED = 0.25D;
    public static final double FLYING_SPEED = 0.42D;
    public static final double FOLLOW_RANGE = 18.0D;
    public static final ResourceLocation MODEL = new ResourceLocation("guaniao", "geo/crow.geo.json");
    public static final ResourceLocation TEXTURE = new ResourceLocation("guaniao", "textures/entity/crow.png");
    public static final ResourceLocation ANIMATION = new ResourceLocation("guaniao", "animations/crow.animation.json");

    private CrowDefinition() {
    }
}
