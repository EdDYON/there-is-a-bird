package EdDYON.guaniao.content.bird.myna;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;

public final class MynaDefinition {
    public static final String ENTITY_ID = "myna";
    public static final String SPAWN_EGG_ID = "myna_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0x211E1A;
    public static final int SPAWN_EGG_SPOT_COLOR = 0xE5A62A;

    public static final float WIDTH = 0.42F;
    public static final float HEIGHT = 0.62F;
    public static final double MAX_HEALTH = 8.0D;
    public static final double WALK_SPEED = 0.24D;
    public static final double FLYING_SPEED = 0.31D;
    public static final double FOLLOW_RANGE = 22.0D;
    public static final double SOCIAL_RADIUS = 10.0D;

    public static final ResourceLocation MODEL = resource("geo/myna.geo.json");
    public static final ResourceLocation TEXTURE = resource("textures/entity/myna.png");
    public static final ResourceLocation ANIMATION = resource("animations/myna.animation.json");

    private MynaDefinition() {
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(GuaniaoMod.MOD_ID, path);
    }
}
