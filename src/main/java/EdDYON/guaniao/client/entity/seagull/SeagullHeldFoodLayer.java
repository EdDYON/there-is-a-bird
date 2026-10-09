package EdDYON.guaniao.client.entity.seagull;

import EdDYON.guaniao.content.bird.seagull.SeagullEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class SeagullHeldFoodLayer extends GeoRenderLayer<SeagullEntity> {
    private static final String FOOT_BONE = "foot_right";

    public SeagullHeldFoodLayer(GeoRenderer<SeagullEntity> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, SeagullEntity seagull, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        ItemStack stack = seagull.getHeldFoodForRendering();
        if (!FOOT_BONE.equals(bone.getName()) || stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        try {
            // Anchor to the animated right foot instead of the beak.
            poseStack.translate(0.0D, 0.015D, -0.018D);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(145.0F));
            poseStack.scale(0.46F, 0.46F, 0.46F);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                    seagull, stack, ItemDisplayContext.GROUND, false, poseStack, bufferSource, packedLight);
        } finally {
            poseStack.popPose();
            bufferSource.getBuffer(renderType);
        }
    }
}
