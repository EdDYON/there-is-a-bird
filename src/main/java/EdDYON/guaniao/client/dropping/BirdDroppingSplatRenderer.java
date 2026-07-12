package EdDYON.guaniao.client.dropping;

import EdDYON.guaniao.content.dropping.BirdDroppingSplatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BirdDroppingSplatRenderer extends GeoEntityRenderer<BirdDroppingSplatEntity> {
    private static final float SPLAT_RENDER_SCALE = 0.68F;

    public BirdDroppingSplatRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BirdDroppingSplatModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(BirdDroppingSplatEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        applySurfaceRotation(poseStack, entity.getSurfaceDirection());
        poseStack.scale(SPLAT_RENDER_SCALE, SPLAT_RENDER_SCALE, SPLAT_RENDER_SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    public void preRender(PoseStack poseStack, BirdDroppingSplatEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha * animatable.getFadeAlpha());
    }

    private static void applySurfaceRotation(PoseStack poseStack, Direction direction) {
        switch (direction) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            default -> {
            }
        }
    }
}
