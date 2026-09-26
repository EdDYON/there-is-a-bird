package EdDYON.guaniao.client.entity.earthworm;

import EdDYON.guaniao.content.earthworm.EarthwormEntity;
import EdDYON.guaniao.content.earthworm.EarthwormMotion;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Flat-faced block segments retain a pixel silhouette while the body wriggles. */
public final class EarthwormRenderer extends EntityRenderer<EarthwormEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/white_concrete.png");

    public EarthwormRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(EarthwormEntity worm, float yaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light) {
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, worm.yRotO, worm.getYRot())));
        float progress = worm.burrowProgress();
        poses.translate(0.0D, -0.025D * progress, 0.0D);
        poses.scale(1.0F, 1.0F, Math.max(0.02F, 1.0F - progress));
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        float time = worm.tickCount + partialTick + worm.getId() * 7.0F;
        for (int segment = 0; segment < EarthwormMotion.SEGMENTS; segment++) {
            float along = (segment + 0.5F) / EarthwormMotion.SEGMENTS;
            float x = EarthwormMotion.bend(along, time);
            float z0 = (segment / (float) EarthwormMotion.SEGMENTS - 0.5F) * EarthwormMotion.LENGTH;
            float z1 = ((segment + 1) / (float) EarthwormMotion.SEGMENTS - 0.5F) * EarthwormMotion.LENGTH;
            float radius = EarthwormMotion.radius(along);
            float shade = segment >= 7 && segment <= 8 ? 1.12F : segment % 2 == 0 ? 1.0F : 0.86F;
            // Shared end planes keep the body joined without overlapping top faces.
            box(vertices, poses.last(), x - radius, 0.002F, z0,
                    x + radius, 0.002F + radius * 2.0F, z1, shade, light);
        }
        poses.popPose();
        super.render(worm, yaw, partialTick, poses, buffers, light);
    }

    private static void box(VertexConsumer out, PoseStack.Pose pose, float x0, float y0, float z0,
                            float x1, float y1, float z1, float shade, int light) {
        face(out, pose, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, 0,1,0, shade,light);
        face(out, pose, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, 0,-1,0, shade,light);
        face(out, pose, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1,0,0, shade,light);
        face(out, pose, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, 1,0,0, shade,light);
        face(out, pose, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, 0,0,-1, shade,light);
        face(out, pose, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, 0,0,1, shade,light);
    }

    private static void face(VertexConsumer out, PoseStack.Pose pose,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, float shade, int light) {
        vertex(out, pose, ax, ay, az, 0, 0, nx, ny, nz, shade, light);
        vertex(out, pose, bx, by, bz, 0, 1, nx, ny, nz, shade, light);
        vertex(out, pose, cx, cy, cz, 1, 1, nx, ny, nz, shade, light);
        vertex(out, pose, dx, dy, dz, 1, 0, nx, ny, nz, shade, light);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz, float shade, int light) {
        out.addVertex(pose.pose(), x, y, z).setColor(0.57F * shade, 0.29F * shade, 0.23F * shade, 1.0F)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override public ResourceLocation getTextureLocation(EarthwormEntity entity) { return TEXTURE; }
}
