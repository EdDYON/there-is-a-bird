package EdDYON.guaniao.client.dropping;

import EdDYON.guaniao.content.dropping.BirdDroppingProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A single fluid surface. Neighbouring faces share their complete cross-section. */
final class BirdDroppingStreamRenderer {
    static final int SECTIONS = 24;
    static final int SIDES = 8;
    private static final float RADIUS = 0.01165F;
    private static final double MAX_LENGTH = 1.6D;
    private static final ResourceLocation TEXTURE = new ResourceLocation("guaniao", "textures/entity/bird_dropping_projectile.png");
    // The white base and dark flecks use the existing dropping palette.
    private static final float U = 7.5F / 16.0F;
    private static final float V = 10.5F / 16.0F;
    private static final float[] FLECKS = createFlecks();

    private BirdDroppingStreamRenderer() { }

    static void render(BirdDroppingProjectileEntity entity, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean invisible = entity.isInvisibleTo(minecraft.player);
        if (invisible && !minecraft.shouldEntityAppearGlowing(entity)) return;
        Mesh mesh = buildMesh(entity, partialTick);
        if (mesh == null) return;
        VertexConsumer out = buffers.getBuffer(invisible ? RenderType.outline(TEXTURE) : RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poses.last();
        for (int ring = 0; ring < SECTIONS; ring++) {
            for (int side = 0; side < SIDES; side++) {
                int next = (side + 1) % SIDES;
                vertex(out, pose, mesh, ring, side, light);
                vertex(out, pose, mesh, ring, next, light);
                vertex(out, pose, mesh, ring + 1, next, light);
                vertex(out, pose, mesh, ring + 1, side, light);
            }
        }
        cap(out, pose, mesh, 0, light);
        cap(out, pose, mesh, SECTIONS, light);
    }

    static Mesh buildMesh(BirdDroppingProjectileEntity entity, float partialTick) {
        Vec3 origin = entity.getPosition(partialTick);
        Vec3[] centers = new Vec3[SECTIONS + 1];
        double[] distances = new double[SECTIONS + 1];
        double length = 0;
        for (int ring = 0; ring <= SECTIONS; ring++) {
            centers[ring] = entity.getStripStreamPoint(ring / (float)SECTIONS, partialTick).subtract(origin);
            if (ring > 0) length += centers[ring].distanceTo(centers[ring - 1]);
            distances[ring] = length;
        }
        if (length < 0.001D) return null;
        if (length > MAX_LENGTH) {
            // Clip and resample along the curve, so a fast fall cannot stretch it into a long rope.
            Vec3[] history = centers;
            centers = new Vec3[SECTIONS + 1];
            int segment = 1;
            for (int ring = 0; ring <= SECTIONS; ring++) {
                double distance = MAX_LENGTH * ring / SECTIONS;
                while (segment < SECTIONS && distances[segment] < distance) segment++;
                double span = distances[segment] - distances[segment - 1];
                double fraction = span > 1.0E-9D ? (distance - distances[segment - 1]) / span : 0;
                centers[ring] = history[segment - 1].lerp(history[segment], Mth.clamp(fraction, 0, 1));
            }
        }

        float[] positions = new float[(SECTIONS + 1) * SIDES * 3];
        float[] normals = new float[positions.length];
        float[] shades = new float[SECTIONS + 1];
        float[] radii = new float[SECTIONS + 1];
        Vec3 tangent = new Vec3(0, 0, 1);
        Vec3 right = null;
        for (int ring = 0; ring <= SECTIONS; ring++) {
            Vec3 delta = centers[Math.min(SECTIONS, ring + 1)].subtract(centers[Math.max(0, ring - 1)]);
            // Vec3.normalize collapses vectors below 1e-4, which newborn streams can contain.
            if (delta.lengthSqr() > 1.0E-12D) tangent = delta.scale(1.0D / Math.sqrt(delta.lengthSqr()));
            // Parallel transport keeps the surface from twisting when the stream changes direction.
            if (right != null) right = right.subtract(tangent.scale(right.dot(tangent)));
            if (right == null || right.lengthSqr() < 1.0E-12D) {
                Vec3 reference = Math.abs(tangent.y) < 0.9D ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
                right = tangent.cross(reference);
            }
            right = right.normalize();
            Vec3 up = tangent.cross(right).normalize();
            float fraction = ring / (float)SECTIONS;
            float front = Mth.lerp(smooth(fraction / 0.16F), 0.35F, 1.0F);
            float source = Mth.lerp(smooth((fraction - 0.8F) / 0.2F), 1.0F, 0.75F);
            float radius = RADIUS * front * source;
            radii[ring] = radius;
            float wave = Mth.sin(fraction * Mth.PI);
            shades[ring] = 1.0F - 0.08F * wave * wave;
            for (int side = 0; side < SIDES; side++) {
                double angle = side * Math.PI * 2.0D / SIDES;
                Vec3 normal = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                Vec3 point = centers[ring].add(normal.scale(radius));
                int offset = (ring * SIDES + side) * 3;
                positions[offset] = (float)point.x;
                positions[offset + 1] = (float)point.y;
                positions[offset + 2] = (float)point.z;
                normals[offset] = (float)normal.x;
                normals[offset + 1] = (float)normal.y;
                normals[offset + 2] = (float)normal.z;
            }
        }
        return new Mesh(origin, centers, positions, normals, shades, radii);
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float[] createFlecks() {
        int[] rings = {3, 5, 8, 11, 14, 17, 19, 22};
        int[] sides = {1, 5, 7, 3, 0, 5, 2, 6};
        float[] result = new float[(SECTIONS + 1) * SIDES];
        for (int ring = 0; ring <= SECTIONS; ring++) {
            for (int side = 0; side < SIDES; side++) {
                float fleck = 0;
                for (int spot = 0; spot < rings.length; spot++) {
                    float along = (ring - rings[spot]) / 1.4F;
                    int sideDistance = Math.abs(side - sides[spot]);
                    float around = Math.min(sideDistance, SIDES - sideDistance) / 1.5F;
                    float distance = Mth.sqrt(along * along + around * around);
                    fleck = Math.max(fleck, 1.0F - smooth((distance - 0.3F) / 0.7F));
                }
                result[ring * SIDES + side] = fleck;
            }
        }
        return result;
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Mesh mesh, int ring, int side, int light) {
        int offset = (ring * SIDES + side) * 3;
        emit(out, pose, mesh.positions[offset], mesh.positions[offset + 1], mesh.positions[offset + 2],
                mesh.normals[offset], mesh.normals[offset + 1], mesh.normals[offset + 2], mesh.shades[ring],
                FLECKS[ring * SIDES + side], light);
    }

    private static void cap(VertexConsumer out, PoseStack.Pose pose, Mesh mesh, int ring, int light) {
        Vec3 center = mesh.centers[ring];
        Vec3 normal = new Vec3(0, 0, ring == 0 ? -1 : 1);
        for (int distance = 1; distance <= SECTIONS; distance++) {
            Vec3 delta = center.subtract(mesh.centers[ring == 0 ? distance : SECTIONS - distance]);
            if (delta.lengthSqr() > 1.0E-12D) {
                normal = delta.scale(1.0D / Math.sqrt(delta.lengthSqr()));
                break;
            }
        }
        for (int side = 0; side < SIDES; side++) {
            int first = ring == 0 ? (side + 1) % SIDES : side;
            int second = ring == 0 ? side : (side + 1) % SIDES;
            for (int corner = 0; corner < 4; corner++) {
                int point = corner == 1 ? first : corner == 2 ? second : -1;
                int offset = (ring * SIDES + Math.max(0, point)) * 3;
                emit(out, pose, point < 0 ? (float)center.x : mesh.positions[offset],
                        point < 0 ? (float)center.y : mesh.positions[offset + 1],
                        point < 0 ? (float)center.z : mesh.positions[offset + 2],
                        (float)normal.x, (float)normal.y, (float)normal.z, mesh.shades[ring],
                        point < 0 ? 0 : FLECKS[ring * SIDES + point], light);
            }
        }
    }

    private static void emit(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
                             float nx, float ny, float nz, float shade, float fleck, int light) {
        // Tint the sampled (237,231,233) toward the original dark-brown (47,42,39).
        // Shared vertex colours keep the flecks on one continuous surface, without rings or gaps.
        out.vertex(pose.pose(), x, y, z).color(shade * Mth.lerp(fleck, 1, 47.0F / 237),
                        shade * Mth.lerp(fleck, 1, 42.0F / 231), shade * Mth.lerp(fleck, 1, 39.0F / 233), 1.0F).uv(U, V)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();
    }

    record Mesh(Vec3 origin, Vec3[] centers, float[] positions, float[] normals, float[] shades, float[] radii) { }
}
