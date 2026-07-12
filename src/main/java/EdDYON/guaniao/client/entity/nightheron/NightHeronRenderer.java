package EdDYON.guaniao.client.entity.nightheron;

import EdDYON.guaniao.client.entity.nightheron.NightHeronModel;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NightHeronRenderer
extends GeoEntityRenderer<NightHeronEntity> {
    public NightHeronRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, (GeoModel)new NightHeronModel());
        this.addRenderLayer(new NightHeronHeldFishLayer(this));
        this.shadowRadius = 0.45f;
    }

    @Override
    public void preRender(PoseStack poseStack, NightHeronEntity animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        float scale = animatable.getModelRenderScale();
        this.withScale(scale);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
