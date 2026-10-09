package EdDYON.guaniao.content.bird.columbid;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;

public final class ColumbidDefinition {
    public static final ResourceLocation MODEL = resource("geo/columbid.geo.json");
    public static final ResourceLocation ANIMATION = resource("animations/columbid.animation.json");

    private ColumbidDefinition() {
    }

    static ResourceLocation resource(String path) {
        return new ResourceLocation(GuaniaoMod.MOD_ID, path);
    }
}
