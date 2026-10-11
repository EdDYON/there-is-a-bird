package EdDYON.guaniao.client.dropping;

import EdDYON.guaniao.content.dropping.BirdDroppingProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BirdDroppingProjectileRenderer extends GeoEntityRenderer<BirdDroppingProjectileEntity> {
    private static final float PROJECTILE_RENDER_SCALE = 0.72F;

    public BirdDroppingProjectileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BirdDroppingProjectileModel());
        this.shadowRadius = 0.03F;
    }

    @Override
    public void render(BirdDroppingProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(PROJECTILE_RENDER_SCALE, PROJECTILE_RENDER_SCALE, PROJECTILE_RENDER_SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
