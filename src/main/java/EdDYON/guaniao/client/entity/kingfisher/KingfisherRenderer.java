package EdDYON.guaniao.client.entity.kingfisher;

import EdDYON.guaniao.content.bird.kingfisher.KingfisherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class KingfisherRenderer extends GeoEntityRenderer<KingfisherEntity> {
    // Rest-pose soles after the exported leg, foot and cube rotations.
    private static final float FOOT_OFFSET_PIXELS = -0.006486F;

    public KingfisherRenderer(EntityRendererProvider.Context context) {
        super(context, new KingfisherModel());
        shadowRadius = 0.10F;
        addRenderLayer(new KingfisherHeldFishLayer(this));
    }

    @Override
    public void preRender(PoseStack pose, KingfisherEntity bird, BakedGeoModel model,
                          MultiBufferSource buffers, VertexConsumer buffer, boolean reRender,
                          float partialTick, int light, int overlay, int renderColor) {
        float scale = bird.getModelRenderScale();
        withScale(scale);
        if (!reRender) pose.translate(0, FOOT_OFFSET_PIXELS * scale / 16.0F, 0);
        super.preRender(pose, bird, model, buffers, buffer, reRender, partialTick,
                light, overlay, renderColor);
    }

    @Override
    protected void applyRotations(KingfisherEntity bird, PoseStack pose, float age, float bodyYaw, float partialTick, float nativeScale) {
        super.applyRotations(bird, pose, age, bodyYaw, partialTick, nativeScale);
        if (bird.deathTime > 0 || bird.isSleeping()) return;
        // The flight clips already extend the body. Tilt the complete rig only once,
        // including its bill and carried fish, around the author's body pivot.
        float pitch = bird.getFlightPitch(partialTick);
        if (pitch != 0) {
            pose.translate(0, 3.75D / 16.0D, 0.3D / 16.0D);
            pose.mulPose(Axis.XP.rotationDegrees(-pitch));
            pose.translate(0, -3.75D / 16.0D, -0.3D / 16.0D);
        }
    }

    @Override
    public void renderRecursively(PoseStack pose, KingfisherEntity bird, GeoBone bone,
                                  RenderType renderType, MultiBufferSource buffers, VertexConsumer buffer,
                                  boolean reRender, float partialTick, int light, int overlay,
                                  int renderColor) {
        boolean body = "body".equals(bone.getName());
        boolean track = body && !reRender && bird.getActivity() == KingfisherEntity.Activity.DEFECATE
                && bird.level().getEntity(bird.getId()) == bird;
        if (body) bone.setTrackingMatrices(track);
        super.renderRecursively(pose, bird, bone, renderType, buffers, buffer, reRender,
                partialTick, light, overlay, renderColor);
        if (!track) return;

        Matrix4f local = new Matrix4f(bone.getLocalSpaceMatrix());
        // GeckoLib 4.4.9 adds an identity matrix while adding the render offset.
        // Remove that extra identity before transforming a non-zero pivot offset.
        if (Math.abs(local.m33() - 2.0F) < 0.001F) local.sub(new Matrix4f());
        Vector4f anchor = local.transform(new Vector4f(-bone.getPivotX() / 16.0F,
                (2.65F - bone.getPivotY()) / 16.0F, (2.70F - bone.getPivotZ()) / 16.0F, 1));
        Vec3 position = new Vec3(Mth.lerp(partialTick, bird.xOld, bird.getX()),
                Mth.lerp(partialTick, bird.yOld, bird.getY()), Mth.lerp(partialTick, bird.zOld, bird.getZ()));
        // Add the double-precision world position after transforming local floats.
        bird.setDroppingRenderOrigin(position.add(anchor.x(),
                anchor.y() + FOOT_OFFSET_PIXELS * bird.getModelRenderScale() / 16.0F, anchor.z()), partialTick);
    }
}
