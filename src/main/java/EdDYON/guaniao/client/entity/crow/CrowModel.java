package EdDYON.guaniao.client.entity.crow;

import EdDYON.guaniao.content.bird.crow.CrowDefinition;
import EdDYON.guaniao.content.bird.crow.CrowEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrowModel extends GeoModel<CrowEntity> {
    @Override
    public ResourceLocation getModelResource(CrowEntity animatable) {
        return CrowDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CrowEntity animatable) {
        return animatable.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(CrowEntity animatable) {
        return CrowDefinition.ANIMATION;
    }
}
