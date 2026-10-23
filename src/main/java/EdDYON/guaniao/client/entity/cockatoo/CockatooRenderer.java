package EdDYON.guaniao.client.entity.cockatoo;

import EdDYON.guaniao.content.bird.cockatoo.CockatooDefinition;
import EdDYON.guaniao.content.bird.cockatoo.CockatooEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CockatooRenderer extends GeoEntityRenderer<CockatooEntity> {
    public CockatooRenderer(EntityRendererProvider.Context context) {
        super(context, new CockatooModel());
        shadowRadius = 0.22F;
    }

    @Override
    public void preRender(PoseStack pose, CockatooEntity bird, BakedGeoModel model,
                          MultiBufferSource buffers, VertexConsumer buffer, boolean reRender,
                          float partialTick, int light, int overlay, float red, float green, float blue, float alpha) {
        float scale = bird.getModelRenderScale();
        withScale(scale);
        pose.translate(0, CockatooDefinition.FOOT_OFFSET_PIXELS * scale / 16.0F, 0);
        super.preRender(pose, bird, model, buffers, buffer, reRender, partialTick, light, overlay, red, green, blue, alpha);
    }
}
