package EdDYON.guaniao.client.entity.budgerigar;

import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BudgerigarRenderer extends GeoEntityRenderer<BudgerigarEntity> {
    public BudgerigarRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, (GeoModel)new BudgerigarModel());
        this.shadowRadius = 0.12F;
    }

    @Override
    public void preRender(PoseStack poseStack, BudgerigarEntity animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        float scale = animatable.getModelRenderScale();
        this.withScale(scale);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
