package EdDYON.guaniao.client.fan;

import EdDYON.guaniao.content.fan.FeatherFanProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

public class FeatherFanProjectileRenderer extends EntityRenderer<FeatherFanProjectileEntity> {
    private static final float RENDER_SCALE = 1.35F;
    private final ItemRenderer itemRenderer;

    public FeatherFanProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(FeatherFanProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.tickCount < 2) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        boolean renderMainFan = !entity.isRivenActive()
                || entity.getRivenTicks() < FeatherFanProjectileEntity.RIVEN_PREPARE_END
                || entity.getRivenTicks() >= FeatherFanProjectileEntity.RIVEN_CONVERGE_END;
        if (renderMainFan) {
            this.renderMainFan(entity, partialTick, poseStack, bufferSource, packedLight);
        }
        if (entity.isRivenActive()) {
            this.renderRivenArray(entity, partialTick, poseStack, bufferSource);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderMainFan(FeatherFanProjectileEntity entity, float partialTick,
                               PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(RENDER_SCALE, RENDER_SCALE, RENDER_SCALE);
        if (entity.isPiercing() || entity.isStuck()) {
            float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
            // Point the fan along the flight vector without distorting the original item proportions.
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(43.0F));
            poseStack.scale(0.90F, 0.90F, 0.90F);
            if (entity.isRivenActive()
                    && entity.getRivenTicks() >= FeatherFanProjectileEntity.RIVEN_CONVERGE_END) {
                float reformProgress = Mth.clamp(
                        (entity.getRivenTicks() + partialTick - FeatherFanProjectileEntity.RIVEN_CONVERGE_END)
                                / (FeatherFanProjectileEntity.RIVEN_END
                                - FeatherFanProjectileEntity.RIVEN_CONVERGE_END),
                        0.0F,
                        1.0F
                );
                float eased = reformProgress * reformProgress * (3.0F - 2.0F * reformProgress);
                float reformScale = Mth.lerp(eased, 0.34F, 1.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees((1.0F - eased) * 240.0F));
                poseStack.scale(reformScale, reformScale, reformScale);
            }
        } else {
            float spinSpeed = entity.isHunting()
                    ? 72.0F
                    : Mth.lerp(entity.getCharge(), 30.0F, 55.0F);
            float spinDirection = entity.isReturning() ? -1.0F : 1.0F;
            // Lay the generated item model flat, then spin it around the world's vertical axis.
            poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * spinSpeed * spinDirection));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            if (entity.isHunting()) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(
                        Mth.sin((entity.tickCount + partialTick) * 0.72F) * 7.5F));
                poseStack.scale(1.08F, 1.08F, 1.08F);
            }
        }
        this.renderFanCopy(entity, poseStack, bufferSource,
                entity.isHunting() ? LightTexture.FULL_BRIGHT : packedLight,
                entity.getId());
        poseStack.popPose();
    }

    private void renderRivenArray(FeatherFanProjectileEntity entity, float partialTick,
                                  PoseStack poseStack, MultiBufferSource bufferSource) {
        float age = entity.getRivenTicks() + partialTick;
        float radius = FeatherFanProjectileEntity.getRivenArrayRadius(age);
        if (radius <= 0.0F) {
            return;
        }

        double ringRotation = FeatherFanProjectileEntity.getRivenRingRotation(age);
        double heightScale = radius / 3.8D;
        float pulse = 0.82F + Mth.sin(age * 0.82F) * 0.035F;
        for (int i = 0; i < 8; i++) {
            double baseAngle = Mth.TWO_PI * i / 8.0D;
            double angle = baseAngle + ringRotation;
            double x = Math.cos(angle) * radius;
            double y = Math.sin(baseAngle * 2.0D) * 0.85D * heightScale;
            double z = Math.sin(angle) * radius;
            double directionLength = Math.sqrt(x * x + y * y + z * z);
            if (directionLength < 1.0E-5D) {
                continue;
            }

            double directionX = -x / directionLength;
            double directionY = -y / directionLength;
            double directionZ = -z / directionLength;
            double horizontal = Math.sqrt(directionX * directionX + directionZ * directionZ);
            float yaw = (float)Math.toDegrees(Mth.atan2(directionX, directionZ));
            float pitch = (float)Math.toDegrees(Mth.atan2(directionY, horizontal));

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.scale(RENDER_SCALE * pulse, RENDER_SCALE * pulse, RENDER_SCALE * pulse);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(43.0F));
            poseStack.scale(0.82F, 0.82F, 0.82F);
            this.renderFanCopy(entity, poseStack, bufferSource,
                    LightTexture.FULL_BRIGHT, entity.getId() * 31 + i);
            poseStack.popPose();
        }
    }

    private void renderFanCopy(FeatherFanProjectileEntity entity, PoseStack poseStack,
                               MultiBufferSource bufferSource, int packedLight, int seed) {
        this.itemRenderer.renderStatic(
                entity.getItem(),
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                entity.level(),
                seed
        );
    }

    @Override
    public ResourceLocation getTextureLocation(FeatherFanProjectileEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
