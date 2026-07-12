package EdDYON.guaniao.content.bird.longtailedtit;

import net.minecraft.resources.ResourceLocation;

public final class LongTailedTitDefinition {
    public static final String ENTITY_ID = "long_tailed_tit";
    public static final String SPAWN_EGG_ID = "long_tailed_tit_spawn_egg";
    public static final int SPAWN_EGG_BASE_COLOR = 0xE9E6E0;
    public static final int SPAWN_EGG_SPOT_COLOR = 0x9D6F75;
    public static final float WIDTH = 0.34F;
    public static final float HEIGHT = 0.36F;
    public static final ResourceLocation MODEL = resource("geo/long_tailed_tit.geo.json");
    public static final ResourceLocation TEXTURE = resource("textures/entity/long_tailed_tit.png");
    public static final ResourceLocation ANIMATION = resource("animations/long_tailed_tit.animation.json");

    private LongTailedTitDefinition() {
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation("guaniao", path);
    }
}
