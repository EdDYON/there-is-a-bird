package EdDYON.guaniao.client.skybird;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Exercises the same empty-pose event used by NeoForge 1.21's AFTER_SKY stage. */
public final class SkyBirdCameraTransformTest {
    public static void main(String[] args) throws Exception {
        var renderBird = SkyBirdRenderer.class.getDeclaredMethod("renderBird", PoseStack.class,
                VertexConsumer.class, Vec3.class, Vec3.class, Vec3.class, Vec3.class, Vec3.class,
                int.class, float.class, float.class, float.class, SkyBirdSpecies.class);
        renderBird.setAccessible(true);
        Vec3[] directions = {new Vec3(0, 1, -2), new Vec3(2, 1, 0),
                new Vec3(0, 1, 2), new Vec3(-2, 1, 0)};
        int checked = 0;
        for (SkyBirdSpecies species : SkyBirdSpecies.values()) {
            for (Vec3 direction : directions) {
                Vec3 relative = direction.normalize().scale(100);
                Vec3 camera = new Vec3(124, 80, -456);
                Matrix4f view = new Matrix4f().lookAt(new Vector3f(),
                        new Vector3f((float)relative.x, (float)relative.y, (float)relative.z),
                        new Vector3f(0, 1, 0));
                Matrix4f originalView = new Matrix4f(view);
                var event = new RenderLevelStageEvent(null, null, null, view,
                        new Matrix4f(), 0, null, null, null);
                for (int frame = 0; frame < species.frameCount(); frame++) {
                    Capture vertices = new Capture();
                    renderBird.invoke(null, SkyBirdRenderer.cameraPose(event), vertices,
                            camera, camera.add(relative), new Vec3(1, 0, 0), new Vec3(0, 0, -1),
                            new Vec3(0, 1, 0), frame, 0.0F, 1.0F, 1.0F, species);
                    // A bird placed directly along the look direction must be centered
                    // in view space, regardless of compass direction or camera pitch.
                    require(vertices.positions.size() == 8, "Both faces must be emitted");
                    require(vertices.center().distance(new Vector3f(0, 0, -100)) < 0.001F,
                            species + " did not follow camera rotation: " + vertices.center());
                    for (float u : vertices.us) {
                        require(u > frame / 4.0F && u < (frame + 1) / 4.0F,
                                "Animation UV sampled outside its frame");
                    }
                    checked++;
                }
                require(view.equals(originalView), "Renderer mutated the event's view matrix");
                require(event.getPoseStack().last().pose().equals(new Matrix4f()),
                        "Renderer mutated the event's empty pose stack");
            }
        }
        System.out.println("Sky bird camera and frame checks passed: " + checked);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Capture implements VertexConsumer {
        final List<Vector3f> positions = new ArrayList<>();
        final List<Float> us = new ArrayList<>();
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            positions.add(new Vector3f(x, y, z));
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { us.add(u); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
        Vector3f center() {
            Vector3f center = new Vector3f();
            for (Vector3f position : positions) center.add(position);
            return center.div(positions.size());
        }
    }
}
