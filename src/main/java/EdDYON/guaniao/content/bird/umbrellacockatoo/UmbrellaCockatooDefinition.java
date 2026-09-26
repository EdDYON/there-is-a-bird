package EdDYON.guaniao.content.bird.umbrellacockatoo;

import net.minecraft.resources.ResourceLocation;

/** Authoring constants for the umbrella cockatoo, including its own art paths. */
public final class UmbrellaCockatooDefinition {
    public static final String ENTITY_ID = "umbrella_cockatoo";
    public static final String SPAWN_EGG_ID = "umbrella_cockatoo_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0xF7F6F1;
    public static final int SPAWN_EGG_SPOT_COLOR = 0xF7CD48;
    public static final float WIDTH = 0.40F;
    public static final float HEIGHT = 0.64F;
    public static final double MAX_HEALTH = 14.0D;
    public static final double WALK_SPEED = 0.23D;
    public static final double FLYING_SPEED = 0.37D;
    public static final double FOLLOW_RANGE = 28.0D;

    public static final ResourceLocation MODEL = resource("geo/umbrella_cockatoo.geo.json");
    public static final ResourceLocation ANIMATION = resource("animations/umbrella_cockatoo.animation.json");
    public static final ResourceLocation TEXTURE = resource("textures/entity/umbrella_cockatoo.png");

    /** Progressive taming: total trust needed, and the range granted by one accepted feed. */
    public static final int TAMING_TRUST_MAX = 100;
    public static final int TRUST_PER_FEED_MIN = 15;
    public static final int TRUST_PER_FEED_MAX = 25;
    /** Trust slowly decays while the bird is still wild, but never fast enough to erase a session's progress. */
    public static final int WILD_TRUST_DECAY_INTERVAL = 2400;
    /** An attack from a player it was beginning to trust costs this much. */
    public static final int TRUST_PENALTY_ON_ATTACK = 60;
    /** Losing the whole trust bar to a killing blow is intentional: betrayal should sting. */
    public static final int TRUST_LOST_ON_FATAL_ATTACK = 100;

    private UmbrellaCockatooDefinition() {
    }

    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath("guaniao", path);
    }
}
