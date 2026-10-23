package EdDYON.guaniao.content.bird.kingfisher;

import net.minecraft.resources.ResourceLocation;

public final class KingfisherDefinition {
    public static final String ENTITY_ID = "kingfisher";
    public static final String SPAWN_EGG_ID = "kingfisher_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0x117F9E;
    public static final int SPAWN_EGG_SPOT_COLOR = 0xD28235;
    public static final float WIDTH = 0.35F, HEIGHT = 0.45F;
    public static final double MAX_HEALTH = 6.0D;
    public static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("guaniao", "geo/kingfisher.geo.json");
    public static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("guaniao", "animations/kingfisher.animation.json");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("guaniao", "textures/entity/kingfisher.png");

    private KingfisherDefinition() { }
}
