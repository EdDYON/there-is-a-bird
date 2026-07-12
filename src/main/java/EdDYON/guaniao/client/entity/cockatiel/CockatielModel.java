package EdDYON.guaniao.client.entity.cockatiel;

import EdDYON.guaniao.content.bird.cockatiel.CockatielDefinition;
import EdDYON.guaniao.content.bird.cockatiel.CockatielEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CockatielModel extends GeoModel<CockatielEntity> {
    @Override
    public ResourceLocation getModelResource(CockatielEntity animatable) {
        return CockatielDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CockatielEntity animatable) {
        return animatable.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(CockatielEntity animatable) {
        return CockatielDefinition.ANIMATION;
    }
}
