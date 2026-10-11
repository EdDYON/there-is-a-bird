package EdDYON.guaniao.client.entity.kestrel;

import EdDYON.guaniao.content.bird.kestrel.KestrelEntity;
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

public final class KestrelHeldItemLayer extends GeoRenderLayer<KestrelEntity> {
    private static final String TALON_ANCHOR_BONE = "root";

    public KestrelHeldItemLayer(GeoRenderer<KestrelEntity> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, KestrelEntity kestrel, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        ItemStack stack = kestrel.getCarriedItem();
        if (!TALON_ANCHOR_BONE.equals(bone.getName()) || stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        try {
            // The talons meet just below and slightly ahead of the model root.
            // Keeping this on the root prevents head-look animation from moving
            // an item that is supposed to be held by the feet.
            poseStack.translate(0.0D, 0.018D, -0.095D);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F));
            poseStack.scale(0.46F, 0.46F, 0.46F);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                    kestrel, stack, ItemDisplayContext.GROUND, false, poseStack, bufferSource, packedLight);
        } finally {
            poseStack.popPose();
            bufferSource.getBuffer(renderType);
        }
    }
}
