package EdDYON.guaniao.client.entity.cockatoo;

import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.content.bird.cockatoo.CockatooDefinition;
import EdDYON.guaniao.content.bird.cockatoo.CockatooEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CockatooModel extends GeoModel<CockatooEntity> {
    @Override
    public ResourceLocation getModelResource(CockatooEntity bird) {
        return CockatooDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CockatooEntity bird) {
        return BirdMutationTextureFactory.textureFor(bird.getTextureResource(), bird);
    }

    @Override
    public ResourceLocation getAnimationResource(CockatooEntity bird) {
        return CockatooDefinition.ANIMATION;
    }
}
