package EdDYON.guaniao.client.entity.longtailedtit;

import EdDYON.guaniao.content.bird.longtailedtit.LongTailedTitEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LongTailedTitRenderer extends GeoEntityRenderer<LongTailedTitEntity> {
    public LongTailedTitRenderer(EntityRendererProvider.Context context) {
        super(context, new LongTailedTitModel());
        this.shadowRadius = 0.15F;
    }

    @Override
    public void preRender(PoseStack poseStack, LongTailedTitEntity animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.withScale(animatable.getModelRenderScale());
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
