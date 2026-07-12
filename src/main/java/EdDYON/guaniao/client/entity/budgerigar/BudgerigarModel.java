package EdDYON.guaniao.client.entity.budgerigar;

import EdDYON.guaniao.content.bird.budgerigar.BudgerigarDefinition;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BudgerigarModel extends GeoModel<BudgerigarEntity> {
    @Override
    public ResourceLocation getModelResource(BudgerigarEntity animatable) {
        return BudgerigarDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BudgerigarEntity animatable) {
        return animatable.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(BudgerigarEntity animatable) {
        return BudgerigarDefinition.ANIMATION;
    }
}
