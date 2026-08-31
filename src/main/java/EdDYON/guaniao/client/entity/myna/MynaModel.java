package EdDYON.guaniao.client.entity.myna;

import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.content.bird.myna.MynaDefinition;
import EdDYON.guaniao.content.bird.myna.MynaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MynaModel extends GeoModel<MynaEntity> {
    @Override
    public ResourceLocation getModelResource(MynaEntity animatable) {
        return MynaDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MynaEntity animatable) {
        return BirdMutationTextureFactory.textureFor(MynaDefinition.TEXTURE, animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(MynaEntity animatable) {
        return MynaDefinition.ANIMATION;
    }
}
