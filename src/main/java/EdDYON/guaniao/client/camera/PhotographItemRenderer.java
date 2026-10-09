package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class PhotographItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation CARD_TEXTURE = new ResourceLocation(GuaniaoMod.MOD_ID, "textures/item/photograph_card.png");
    private static final float CARD_WIDTH = 0.82F;
    private static final float CARD_HEIGHT = 0.62F;

    public PhotographItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        try {
            applyDisplayTransform(poseStack, context);

            int light = context == ItemDisplayContext.GUI ? LightTexture.FULL_BRIGHT : packedLight;
            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer cardConsumer = bufferSource.getBuffer(RenderType.text(CARD_TEXTURE));
            renderQuad(cardConsumer, matrix, -CARD_WIDTH / 2.0F, -CARD_HEIGHT / 2.0F, CARD_WIDTH, CARD_HEIGHT, 0.0F, light);
        } finally {
            poseStack.popPose();
        }
    }

    private static void applyDisplayTransform(PoseStack poseStack, ItemDisplayContext context) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, 0.0F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-8.0F));
            poseStack.scale(0.96F, 0.96F, 0.96F);
        } else if (context == ItemDisplayContext.GROUND) {
            poseStack.translate(0.0F, 0.05F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(0.62F, 0.62F, 0.62F);
        } else if (context == ItemDisplayContext.FIXED) {
            poseStack.scale(0.78F, 0.78F, 0.78F);
        } else {
            poseStack.translate(0.0F, 0.02F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-6.0F));
            poseStack.scale(0.72F, 0.72F, 0.72F);
        }
    }

    private static void renderQuad(VertexConsumer consumer, Matrix4f matrix, float x, float y, float width, float height, float z, int packedLight) {
        consumer.vertex(matrix, x, y + height, z).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(packedLight).endVertex();
        consumer.vertex(matrix, x + width, y + height, z).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(packedLight).endVertex();
        consumer.vertex(matrix, x + width, y, z).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(packedLight).endVertex();
        consumer.vertex(matrix, x, y, z).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(packedLight).endVertex();
    }
}
