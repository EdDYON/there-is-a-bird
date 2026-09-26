package EdDYON.guaniao.content.bird.kestrel;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;

public final class KestrelDefinition {
    public static final String ENTITY_ID = "kestrel";
    public static final String SPAWN_EGG_ID = "kestrel_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0xA65F32;
    public static final int SPAWN_EGG_SPOT_COLOR = 0x2B2422;

    public static final float WIDTH = 0.42F;
    public static final float HEIGHT = 0.54F;
    public static final double MAX_HEALTH = 14.0D;
    public static final double ATTACK_DAMAGE = 4.0D;
    public static final double WALK_SPEED = 0.22D;
    public static final double FLYING_SPEED = 0.76D;
    public static final double FOLLOW_RANGE = 32.0D;
    public static final float MAX_LIFT_WIDTH = 0.80F;
    public static final float MAX_LIFT_HEIGHT = 1.25F;
    public static final double MAX_LIFT_VOLUME = 0.58D;
    public static final double MIN_PREY_DROP_HEIGHT = 9.0D;
    public static final double MAX_PREY_DROP_HEIGHT = 13.0D;

    public static final ResourceLocation MODEL = resource("geo/kestrel.geo.json");
    public static final ResourceLocation TEXTURE = resource("textures/entity/kestrel.png");
    public static final ResourceLocation ANIMATION = resource("animations/kestrel.animation.json");

    private KestrelDefinition() {
    }

    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, path);
    }
}
