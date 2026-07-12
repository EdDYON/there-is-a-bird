package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.PhotographEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class PhotographEntityRenderer extends EntityRenderer<PhotographEntity> {
    private static final ResourceLocation FRAME_TEXTURE = new ResourceLocation(GuaniaoMod.MOD_ID, "textures/entity/photograph_frame.png");

    public PhotographEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull PhotographEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(@NotNull PhotographEntity entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        float frameSize = PhotographEntity.FRAME_SIZE_PIXELS / 16.0F;
        float photoSize = PhotographEntity.PHOTO_SIZE_PIXELS / 16.0F;
        float margin = (frameSize - photoSize) / 2.0F;
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getRotation() * 90.0F + 180.0F));
        poseStack.translate(-frameSize / 2.0F, -frameSize / 2.0F, 0.026F);

        VertexConsumer photoConsumer = bufferSource.getBuffer(RenderType.text(PhotographTextureCache.textureFor(entity.getItem())));
        Matrix4f matrix = poseStack.last().pose();
        renderQuad(photoConsumer, matrix, margin, margin, photoSize, 0.0F, packedLight);

        VertexConsumer frameConsumer = bufferSource.getBuffer(RenderType.text(FRAME_TEXTURE));
        renderQuad(frameConsumer, matrix, 0.0F, 0.0F, frameSize, 0.002F, packedLight);

        poseStack.popPose();
    }

    private static void renderQuad(VertexConsumer consumer, Matrix4f matrix, float x, float y, float size, float z, int packedLight) {
        consumer.vertex(matrix, x, y + size, z).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(packedLight).endVertex();
        consumer.vertex(matrix, x + size, y + size, z).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(packedLight).endVertex();
        consumer.vertex(matrix, x + size, y, z).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(packedLight).endVertex();
        consumer.vertex(matrix, x, y, z).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(packedLight).endVertex();
    }
}
