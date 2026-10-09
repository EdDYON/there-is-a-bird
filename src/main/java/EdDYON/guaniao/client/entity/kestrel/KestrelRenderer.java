package EdDYON.guaniao.client.entity.kestrel;

import EdDYON.guaniao.content.bird.kestrel.KestrelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class KestrelRenderer extends GeoEntityRenderer<KestrelEntity> {
    public KestrelRenderer(EntityRendererProvider.Context context) {
        super(context, (GeoModel)new KestrelModel());
        this.addRenderLayer(new KestrelHeldItemLayer(this));
        this.shadowRadius = 0.18F;
    }

    @Override
    public boolean shouldShowName(KestrelEntity kestrel) {
        return kestrel.isLearningFetchItem() || super.shouldShowName(kestrel);
    }

    @Override
    public Vec3 getRenderOffset(KestrelEntity bird, float partialTick) {
        if (bird.getVehicle() instanceof AbstractClientPlayer owner && bird.isOwnedBy(owner)) {
            Vec3 ownerPosition = new Vec3(Mth.lerp(partialTick, owner.xOld, owner.getX()),
                    Mth.lerp(partialTick, owner.yOld, owner.getY()),
                    Mth.lerp(partialTick, owner.zOld, owner.getZ()));
            Vec3 birdPosition = new Vec3(Mth.lerp(partialTick, bird.xOld, bird.getX()),
                    Mth.lerp(partialTick, bird.yOld, bird.getY()),
                    Mth.lerp(partialTick, bird.zOld, bird.getZ()));
            float yaw = Mth.rotLerp(partialTick, owner.yHeadRotO, owner.yHeadRot);
            float pitch = Mth.lerp(partialTick, owner.xRotO, owner.getXRot());
            return ownerPosition.add(bird.ownerHeadOffset(yaw, pitch))
                    .subtract(birdPosition);
        }
        return super.getRenderOffset(bird, partialTick);
    }

    @Override
    protected void applyRotations(KestrelEntity bird, PoseStack pose, float age, float bodyYaw, float partialTick) {
        if (bird.getVehicle() instanceof AbstractClientPlayer owner && bird.isOwnedBy(owner)) {
            bodyYaw = Mth.rotLerp(partialTick, owner.yHeadRotO, owner.yHeadRot);
        }
        super.applyRotations(bird, pose, age, bodyYaw, partialTick);
        if (bird.getVehicle() instanceof AbstractClientPlayer owner && bird.isOwnedBy(owner)) {
            pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, owner.xRotO, owner.getXRot())));
        } else if (bird.isFlying()) {
            pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, bird.xRotO, bird.getXRot())));
            pose.mulPose(Axis.ZP.rotationDegrees(bird.flightBank(partialTick)));
        }
    }

    @Override
    public void preRender(PoseStack poseStack, KestrelEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int renderColor) {
        this.withScale(animatable.getModelRenderScale());
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, renderColor);
    }
}
