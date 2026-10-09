package EdDYON.guaniao.content.bird.cassowary;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;

public final class CassowaryDefinition {
    public static final String ENTITY_ID = "cassowary";
    public static final String SPAWN_EGG_ID = "cassowary_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0x16242C;
    public static final int SPAWN_EGG_SPOT_COLOR = 0x28768E;

    /** Approved 0.85x size applies to the entity collision box as well as rendering. */
    public static final float SIZE_MULTIPLIER = 0.85F;
    public static final float WIDTH = 1.15F * SIZE_MULTIPLIER;
    public static final float HEIGHT = 2.62F * SIZE_MULTIPLIER;
    public static final double MAX_HEALTH = 30.0D;
    public static final double ATTACK_DAMAGE = 7.0D;
    public static final double ATTACK_KNOCKBACK = 1.8D;
    public static final double WALK_SPEED = 0.26D;
    public static final double FOLLOW_RANGE = 32.0D;

    public static final ResourceLocation MODEL = resource("geo/cassowary.geo.json");
    public static final ResourceLocation TEXTURE = resource("textures/entity/cassowary.png");
    public static final ResourceLocation ANIMATION = resource("animations/cassowary.animation.json");

    private CassowaryDefinition() {
    }

    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, path);
    }
}
