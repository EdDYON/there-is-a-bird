package EdDYON.guaniao.client.entity.hummingbird;

import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
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

/** Draw the server-synchronised stack only; no reward or inventory changes during rendering. */
public final class HummingbirdHeldItemLayer extends GeoRenderLayer<HummingbirdEntity> {
    public HummingbirdHeldItemLayer(GeoRenderer<HummingbirdEntity> renderer) { super(renderer); }

    @Override
    public void renderForBone(PoseStack pose, HummingbirdEntity bird, GeoBone bone, RenderType renderType,
                              MultiBufferSource buffers, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        if (!"head".equals(bone.getName())) return;
        ItemStack held = bird.getHeldGardenItem();
        if (held.isEmpty()) return;
        pose.pushPose();
        try {
            // renderForBone has applied the bone transform and moved back from its pivot.
            // The author's exported rig has no added locator bones. This head-local
            // anchor keeps the seed at the same bill tip without changing the rig.
            pose.translate(0, 4.25 / 16.0, -3.45 / 16.0);
            pose.mulPose(Axis.ZP.rotationDegrees(-20));
            pose.scale(0.12F, 0.12F, 0.12F);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                    bird, held, ItemDisplayContext.NONE, false, pose, buffers, packedLight);
        } finally {
            pose.popPose();
            buffers.getBuffer(renderType);
        }
    }
}
