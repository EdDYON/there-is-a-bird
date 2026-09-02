package EdDYON.guaniao.client.entity.macaw;

import EdDYON.guaniao.content.bird.macaw.MacawEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MacawRenderer extends GeoEntityRenderer<MacawEntity> {
    public MacawRenderer(EntityRendererProvider.Context context) {
        super(context, new MacawModel());
        this.shadowRadius = 0.24F;
    }

    @Override
    public void preRender(PoseStack poseStack, MacawEntity animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.withScale(animatable.getModelRenderScale());
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
