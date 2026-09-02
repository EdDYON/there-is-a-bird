package EdDYON.guaniao.client.entity.columbid;

import EdDYON.guaniao.content.bird.columbid.PigeonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PigeonRenderer extends GeoEntityRenderer<PigeonEntity> {
    public PigeonRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ColumbidModel<>());
        this.shadowRadius = 0.23F;
    }

    @Override
    public void preRender(PoseStack poseStack, PigeonEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.withScale(animatable.getModelRenderScale());
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
