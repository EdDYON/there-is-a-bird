package EdDYON.guaniao.content.bird.seagull;

import net.minecraft.resources.ResourceLocation;

public final class SeagullDefinition {
    public static final String ENTITY_ID = "seagull";
    public static final String SPAWN_EGG_ID = "seagull_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0xF2F0E8;
    public static final int SPAWN_EGG_SPOT_COLOR = 0xD7A13A;
    public static final float WIDTH = 0.56F;
    public static final float HEIGHT = 0.72F;
    public static final double MAX_HEALTH = 8.0D;
    public static final double WALK_SPEED = 0.27D;
    public static final double FLYING_SPEED = 0.62D;
    public static final double FOLLOW_RANGE = 22.0D;
    public static final ResourceLocation MODEL = SeagullDefinition.resource("geo/seagull.geo.json");
    public static final ResourceLocation TEXTURE = SeagullDefinition.resource("textures/entity/seagull.png");
    public static final ResourceLocation ANIMATION = SeagullDefinition.resource("animations/seagull.animation.json");

    private SeagullDefinition() {
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation("guaniao", path);
    }
}
