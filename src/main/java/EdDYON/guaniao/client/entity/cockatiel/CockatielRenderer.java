package EdDYON.guaniao.client.entity.cockatiel;

import EdDYON.guaniao.content.bird.cockatiel.CockatielEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CockatielRenderer extends GeoEntityRenderer<CockatielEntity> {
    public CockatielRenderer(EntityRendererProvider.Context context) {
        super(context, new CockatielModel());
        this.shadowRadius = 0.14F;
    }

    @Override
    public void preRender(PoseStack poseStack, CockatielEntity animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.withScale(animatable.getModelRenderScale());
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
