package EdDYON.guaniao.client.entity.macaw;

import EdDYON.guaniao.content.bird.macaw.MacawDefinition;
import EdDYON.guaniao.content.bird.macaw.MacawEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MacawModel extends GeoModel<MacawEntity> {
    @Override
    public ResourceLocation getModelResource(MacawEntity animatable) {
        return MacawDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MacawEntity animatable) {
        return animatable.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(MacawEntity animatable) {
        return MacawDefinition.ANIMATION;
    }
}
