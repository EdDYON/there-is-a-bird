package EdDYON.guaniao.client.entity.columbid;

import EdDYON.guaniao.content.bird.columbid.AbstractColumbidEntity;
import EdDYON.guaniao.content.bird.columbid.ColumbidDefinition;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ColumbidModel<T extends AbstractColumbidEntity> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T animatable) {
        return ColumbidDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return animatable.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ColumbidDefinition.ANIMATION;
    }
}
