package EdDYON.guaniao.client.entity.cassowary;

import EdDYON.guaniao.content.bird.cassowary.CassowaryEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CassowaryRenderer extends GeoEntityRenderer<CassowaryEntity> {
    public CassowaryRenderer(EntityRendererProvider.Context context) {
        super(context, new CassowaryModel());
        this.shadowRadius = 0.493F;
    }

    @Override
    public void preRender(PoseStack poseStack, CassowaryEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          int renderColor) {
        this.withScale(animatable.getModelRenderScale());
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, renderColor);
    }
}
