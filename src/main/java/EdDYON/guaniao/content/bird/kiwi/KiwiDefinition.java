package EdDYON.guaniao.content.bird.kiwi;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;

public final class KiwiDefinition {
    public static final String ENTITY_ID = "kiwi";
    public static final String SPAWN_EGG_ID = "kiwi_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0x4B3528;
    public static final int SPAWN_EGG_SPOT_COLOR = 0xA88B65;

    public static final float WIDTH = 0.58F;
    public static final float HEIGHT = 0.70F;
    public static final double MAX_HEALTH = 10.0D;
    public static final double WALK_SPEED = 0.18D;
    public static final double FOLLOW_RANGE = 18.0D;
    public static final double WANDER_SPEED = 0.85D;
    public static final double PANIC_SPEED = 1.45D;

    public static final int CORE_TERRITORY_RADIUS = 11;
    public static final int NORMAL_TERRITORY_RADIUS = 24;
    public static final int MAX_TERRITORY_RADIUS = 34;
    public static final int FORAGE_SEARCH_RADIUS = 9;
    public static final int SHELTER_SEARCH_RADIUS = 12;
    public static final int PECK_DURATION_TICKS = 60;
    public static final int PECK_FOOD_TICK = 36;
    public static final int MAX_FIGHT_TICKS = 240;

    public static final ResourceLocation MODEL = resource("geo/kiwi.geo.json");
    public static final ResourceLocation TEXTURE = resource("textures/entity/kiwi.png");
    public static final ResourceLocation ANIMATION = resource("animations/kiwi.animation.json");

    private KiwiDefinition() {
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(GuaniaoMod.MOD_ID, path);
    }
}
