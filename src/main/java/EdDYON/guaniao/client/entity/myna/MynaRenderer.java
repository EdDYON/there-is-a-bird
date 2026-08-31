package EdDYON.guaniao.client.entity.myna;

import EdDYON.guaniao.content.bird.myna.MynaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MynaRenderer extends GeoEntityRenderer<MynaEntity> {
    public MynaRenderer(EntityRendererProvider.Context context) {
        super(context, new MynaModel());
        this.shadowRadius = 0.22F;
    }

    @Override
    public void preRender(PoseStack poseStack, MynaEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        this.withScale(animatable.getModelRenderScale());
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
