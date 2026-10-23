package EdDYON.guaniao.content.bird.woodcock;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;

public final class WoodcockDefinition {
    public static final String ENTITY_ID = "woodcock";
    public static final String SPAWN_EGG_ID = "woodcock_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0x6B4935;
    public static final int SPAWN_EGG_SPOT_COLOR = 0xC49A6C;

    public static final float WIDTH = 0.32F;
    public static final float HEIGHT = 0.36F;
    public static final float MODEL_ROOT_SCALE = 0.45F;
    public static final double MAX_HEALTH = 8.0D;
    public static final double WALK_SPEED = 0.22D;
    public static final double FLYING_SPEED = 0.38D;
    public static final double FOLLOW_RANGE = 18.0D;

    public static final int CORE_TERRITORY_RADIUS = 12;
    public static final int NORMAL_TERRITORY_RADIUS = 24;

    public static final ResourceLocation MODEL = resource("geo/woodcock.geo.json");
    public static final ResourceLocation TEXTURE = resource("textures/entity/woodcock.png");
    public static final ResourceLocation ANIMATION = resource("animations/woodcock.animation.json");

    private WoodcockDefinition() {
    }

    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, path);
    }
}
