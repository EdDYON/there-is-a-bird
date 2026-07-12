package EdDYON.guaniao.client.nest;

import EdDYON.guaniao.content.nest.CrowNestBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrowNestModel extends GeoModel<CrowNestBlockEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation("guaniao", "geo/crow_nest.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation("guaniao", "textures/block/crow_nest.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation("guaniao", "animations/crow_nest.animation.json");

    @Override
    public ResourceLocation getModelResource(CrowNestBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CrowNestBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CrowNestBlockEntity animatable) {
        return ANIMATION;
    }
}
