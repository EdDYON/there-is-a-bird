package EdDYON.guaniao.client.nest;

import EdDYON.guaniao.content.nest.CrowNestItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrowNestItemModel extends GeoModel<CrowNestItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("guaniao", "geo/crow_nest.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("guaniao", "textures/block/crow_nest.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("guaniao", "animations/crow_nest.animation.json");

    @Override
    public ResourceLocation getModelResource(CrowNestItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CrowNestItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CrowNestItem animatable) {
        return ANIMATION;
    }
}
