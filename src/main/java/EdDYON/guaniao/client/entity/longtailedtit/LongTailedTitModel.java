package EdDYON.guaniao.client.entity.longtailedtit;

import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.content.bird.longtailedtit.LongTailedTitDefinition;
import EdDYON.guaniao.content.bird.longtailedtit.LongTailedTitEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class LongTailedTitModel extends GeoModel<LongTailedTitEntity> {
    @Override
    public ResourceLocation getModelResource(LongTailedTitEntity animatable) {
        return LongTailedTitDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(LongTailedTitEntity animatable) {
        return BirdMutationTextureFactory.textureFor(LongTailedTitDefinition.TEXTURE, animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(LongTailedTitEntity animatable) {
        return LongTailedTitDefinition.ANIMATION;
    }
}
