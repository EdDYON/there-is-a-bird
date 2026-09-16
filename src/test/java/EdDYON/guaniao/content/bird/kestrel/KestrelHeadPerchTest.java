package EdDYON.guaniao.content.bird.kestrel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Check actual talon contact against the rotated crown, not the head's enclosing box. */
public final class KestrelHeadPerchTest {
    public static void main(String[] args) {
        for (int yaw = -180; yaw <= 180; yaw += 30) {
            for (int pitch = -90; pitch <= 90; pitch += 10) {
                ModelPart head = new ModelPart(List.of(), Map.of());
                head.xRot = (float)Math.toRadians(pitch);
                head.yRot = (float)Math.toRadians(yaw);
                PoseStack pose = new PoseStack();
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.scale(-0.9375F, -0.9375F, 0.9375F);
                pose.translate(0, -1.501, 0);
                head.translateAndRotate(pose);
                Vector3f crown = pose.last().pose().transformPosition(new Vector3f(0, -8.5F / 16, 0));
                Vector3f normal = pose.last().pose().transformDirection(new Vector3f(0, -1, 0)).normalize();
                Vec3 expected = new Vec3(crown.x + normal.x * 0.001, crown.y + normal.y * 0.001, crown.z + normal.z * 0.001);
                for (double scale : new double[]{0.5, 1, 2}) {
                    double sole = KestrelTalons.SOLE_Y * scale, forward = KestrelTalons.FORWARD * scale;
                    Vec3 anchor = KestrelHeadPerch.offset(yaw, pitch, sole, forward);
                    PoseStack birdPose = new PoseStack();
                    birdPose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
                    birdPose.mulPose(Axis.XP.rotationDegrees(-pitch));
                    Vector3f renderedFoot = birdPose.last().pose().transformPosition(new Vector3f(0, (float)sole, (float)-forward));
                    Vec3 foot = anchor.add(renderedFoot.x, renderedFoot.y, renderedFoot.z);
                    check(foot.distanceTo(expected) < 1.0E-6, "rendered feet touch rotated crown");
                    check(anchor.add(KestrelTalons.offset(yaw, pitch, scale)).distanceTo(foot) < 1.0E-6,
                            "server and renderer use the same talon position");
                }
            }
        }
        System.out.println("PASS: rendered talon/crown contact at 247 head poses and three bird scales; matching server anchor.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
