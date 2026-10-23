package EdDYON.guaniao.client.entity.kingfisher;

import EdDYON.guaniao.content.bird.kingfisher.KingfisherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class KingfisherHeldFishLayer extends GeoRenderLayer<KingfisherEntity> {
    public KingfisherHeldFishLayer(GeoRenderer<KingfisherEntity> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack pose, KingfisherEntity bird, GeoBone bone, RenderType renderType,
                              MultiBufferSource buffers, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        if (!"head".equals(bone.getName())) return;
        ItemStack held = bird.getHeldFish();
        if (held.isEmpty()) return;
        float swallowing = bird.isEating() ? Mth.clamp((bird.getEatingTicks(partialTick) - 32) / 15, 0, 1) : 0;
        swallowing = swallowing * swallowing * (3 - 2 * swallowing);
        if (swallowing >= 0.999F) return;
        pose.pushPose();
        try {
            // The head transform is active here; coordinates stay in the exported
            // model space so the catch follows its bill in flight and while eating.
            pose.translate(0, (6.35D + 0.25D * swallowing) / 16.0D, (-6.05D + 1.65D * swallowing) / 16.0D);
            pose.mulPose(Axis.ZP.rotationDegrees(-20 - 45 * swallowing));
            float size = 0.17F * (1 - swallowing);
            pose.scale(size, size, size);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                    bird, held, ItemDisplayContext.NONE, false, pose, buffers, packedLight);
        } finally {
            pose.popPose();
            buffers.getBuffer(renderType);
        }
    }
}
