package EdDYON.guaniao.client.dropping;

import EdDYON.guaniao.content.dropping.BirdDroppingSplatEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BirdDroppingSplatModel extends GeoModel<BirdDroppingSplatEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation("guaniao", "geo/bird_dropping_splat.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation("guaniao", "textures/entity/bird_dropping_splat.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation("guaniao", "animations/bird_dropping.animation.json");

    @Override
    public ResourceLocation getModelResource(BirdDroppingSplatEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BirdDroppingSplatEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BirdDroppingSplatEntity animatable) {
        return ANIMATION;
    }
}
