package EdDYON.guaniao.client.entity.umbrellacockatoo;

import EdDYON.guaniao.content.bird.umbrellacockatoo.UmbrellaCockatooEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class UmbrellaCockatooRenderer extends GeoEntityRenderer<UmbrellaCockatooEntity> {
    private final CockatooPoseComposer composer = new CockatooPoseComposer();
    private CockatooPoseComposer.RenderPose framePose;

    public UmbrellaCockatooRenderer(EntityRendererProvider.Context context) {
        super(context, new UmbrellaCockatooModel());
        this.shadowRadius = 0.20F;
    }

    @Override
    public Vec3 getRenderOffset(UmbrellaCockatooEntity bird, float partialTick) {
        if (bird.getVehicle() instanceof AbstractClientPlayer owner && bird.isOwnedBy(owner)) {
            Vec3 ownerPosition = new Vec3(Mth.lerp(partialTick, owner.xOld, owner.getX()),
                    Mth.lerp(partialTick, owner.yOld, owner.getY()),
                    Mth.lerp(partialTick, owner.zOld, owner.getZ()));
            Vec3 birdPosition = new Vec3(Mth.lerp(partialTick, bird.xOld, bird.getX()),
                    Mth.lerp(partialTick, bird.yOld, bird.getY()),
                    Mth.lerp(partialTick, bird.zOld, bird.getZ()));
            float headYaw = Mth.rotLerp(partialTick, owner.yHeadRotO, owner.yHeadRot);
            float pitch = Mth.lerp(partialTick, owner.xRotO, owner.getXRot());
            float bodyYaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot);
            return ownerPosition.add(bird.ownerPerchOffset(headYaw, pitch, bodyYaw)).subtract(birdPosition);
        }
        return super.getRenderOffset(bird, partialTick);
    }

    @Override
    protected void applyRotations(UmbrellaCockatooEntity bird, PoseStack pose, float age,
                                  float bodyYaw, float partialTick) {
        if (bird.getVehicle() instanceof AbstractClientPlayer owner && bird.isOwnedBy(owner)) {
            bodyYaw = Mth.rotLerp(partialTick, owner.yHeadRotO, owner.yHeadRot);
        }
        super.applyRotations(bird, pose, age, bodyYaw, partialTick);
        if (bird.getVehicle() instanceof AbstractClientPlayer owner && bird.isOwnedBy(owner)) {
            float pitch = Mth.lerp(partialTick, owner.xRotO, owner.getXRot());
            pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        }
    }

    @Override
    public void preRender(PoseStack poseStack, UmbrellaCockatooEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        this.withScale(animatable.getModelRenderScale());
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, UmbrellaCockatooEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        var previousPose = framePose;
        framePose = null;
        try {
            // super evaluates the base controller before calling renderRecursively.
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        } finally {
            framePose = previousPose;
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, UmbrellaCockatooEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        if (framePose == null) framePose = ((UmbrellaCockatooModel) getGeoModel()).samplePose(animatable);
        try (var scope = composer.apply(bone, framePose.bone(bone.getName()))) {
            // Re-render passes also need the delta; only active recursion is suppressed.
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }
}
