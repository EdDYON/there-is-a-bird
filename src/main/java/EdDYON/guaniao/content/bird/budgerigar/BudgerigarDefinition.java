package EdDYON.guaniao.content.bird.budgerigar;

import net.minecraft.resources.ResourceLocation;

public final class BudgerigarDefinition {
    public static final String ENTITY_ID = "budgerigar";
    public static final String SPAWN_EGG_ID = "budgerigar_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0x6FBF45;
    public static final int SPAWN_EGG_SPOT_COLOR = 0xF5D84A;
    public static final float WIDTH = 0.19F;
    public static final float HEIGHT = 0.24F;
    public static final double MAX_HEALTH = 6.0D;
    public static final double WALK_SPEED = 0.24D;
    public static final double FLYING_SPEED = 0.32D;
    public static final double FOLLOW_RANGE = 18.0D;
    public static final int SOCIAL_RADIUS = 14;
    public static final int MIN_FLOCK_SIZE = 3;
    public static final int MAX_FLOCK_SIZE = 10;
    public static final ResourceLocation MODEL = BudgerigarDefinition.resource("geo/budgerigar.geo.json");
    public static final ResourceLocation TEXTURE = BudgerigarDefinition.resource("textures/entity/budgerigar.png");
    public static final ResourceLocation ANIMATION = BudgerigarDefinition.resource("animations/budgerigar.animation.json");
    public static final ResourceLocation[] TEXTURE_VARIANTS = new ResourceLocation[] {
            TEXTURE,
            BudgerigarDefinition.resource("textures/entity/budgerigar/white_lark.png"),
            BudgerigarDefinition.resource("textures/entity/budgerigar/mystery_green.png"),
            BudgerigarDefinition.resource("textures/entity/budgerigar/blue_lark.png"),
            BudgerigarDefinition.resource("textures/entity/budgerigar/blue_porcelain.png"),
            BudgerigarDefinition.resource("textures/entity/budgerigar/yellow_lark.png"),
            BudgerigarDefinition.resource("textures/entity/budgerigar/yellow.png"),
            BudgerigarDefinition.resource("textures/entity/budgerigar/yellow_2.png"),
            BudgerigarDefinition.resource("textures/entity/budgerigar/yellow_black.png"),
            BudgerigarDefinition.resource("textures/entity/budgerigar/black_white.png")
    };

    private BudgerigarDefinition() {
    }

    public static ResourceLocation textureForVariant(int variant) {
        if (variant < 0 || variant >= TEXTURE_VARIANTS.length) {
            return TEXTURE;
        }
        return TEXTURE_VARIANTS[variant];
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation("guaniao", path);
    }
}
