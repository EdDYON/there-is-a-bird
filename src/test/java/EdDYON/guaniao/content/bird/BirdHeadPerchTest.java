package EdDYON.guaniao.content.bird;

import EdDYON.guaniao.content.bird.kestrel.KestrelHeadPerch;
import EdDYON.guaniao.content.bird.kestrel.KestrelTalons;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Validate rendered contact against vanilla's transformed crown for both bird rigs. */
public final class BirdHeadPerchTest {
    public static void main(String[] args) {
        int contacts = 0;
        // These are actual bound-pose sole coordinates of the user-supplied
        // hummingbird mesh, not its entity hitbox or its bounding-box centre.
        double[][] soles = {{KestrelTalons.SOLE_Y, KestrelTalons.FORWARD},
                {0.0021585249 / 16, 0.582899738 / 16}};
        for (int yaw = -180; yaw <= 180; yaw += 30) {
            for (int pitch = -90; pitch <= 90; pitch += 10) {
                for (boolean crouching : new boolean[]{false, true}) {
                    ModelPart head = new ModelPart(List.of(), Map.of());
                    head.xRot = (float)Math.toRadians(pitch);
                    head.yRot = (float)Math.toRadians(yaw);
                    head.y = crouching ? 4.2F : 0;
                    PoseStack playerPose = new PoseStack();
                    if (crouching) playerPose.translate(0, -.125, 0);
                    playerPose.mulPose(Axis.YP.rotationDegrees(180));
                    playerPose.scale(-.9375F, -.9375F, .9375F);
                    playerPose.translate(0, -1.501, 0);
                    head.translateAndRotate(playerPose);
                    Vector3f crown = playerPose.last().pose().transformPosition(new Vector3f(0, -8.5F / 16, 0));
                    Vector3f normal = playerPose.last().pose().transformDirection(new Vector3f(0, -1, 0)).normalize();
                    Vec3 expected = new Vec3(crown.x + normal.x * .001, crown.y + normal.y * .001, crown.z + normal.z * .001);
                    for (double[] sole : soles) for (double scale : new double[]{.5, 1, 2}) {
                        double soleY = sole[0] * scale, soleForward = sole[1] * scale;
                        Vec3 anchor = BirdHeadPerch.offset(yaw, pitch, soleY, soleForward, crouching);
                        PoseStack birdPose = new PoseStack();
                        birdPose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
                        birdPose.mulPose(Axis.XP.rotationDegrees(-pitch));
                        Vector3f foot = birdPose.last().pose().transformPosition(new Vector3f(0, (float)soleY, (float)-soleForward));
                        check(anchor.add(foot.x, foot.y, foot.z).distanceTo(expected) < 1.0E-6,
                                "bird sole touches the rendered crown: yaw=" + yaw + ", pitch=" + pitch + ", crouching=" + crouching);
                        if (!crouching) check(BirdHeadPerch.offset(yaw, pitch, soleY, soleForward)
                                .distanceTo(anchor) < 1.0E-12, "four-argument compatibility uses the standing pose");
                        contacts++;
                    }
                }
                check(KestrelHeadPerch.offset(yaw, pitch, KestrelTalons.SOLE_Y, KestrelTalons.FORWARD)
                        .distanceTo(BirdHeadPerch.offset(yaw, pitch, KestrelTalons.SOLE_Y, KestrelTalons.FORWARD)) < 1.0E-12,
                        "existing kestrel callers use the shared head contact");
            }
        }
        check(BirdHeadPerch.offset(27, 135, 0, 0).distanceTo(BirdHeadPerch.offset(27, 90, 0, 0)) < 1.0E-12,
                "invalid pitch above the vanilla head limit clamps safely");
        check(BirdHeadPerch.offset(27, -135, 0, 0).distanceTo(BirdHeadPerch.offset(27, -90, 0, 0)) < 1.0E-12,
                "invalid pitch below the vanilla head limit clamps safely");
        System.out.println("PASS: " + contacts + " rendered crown/sole contacts across standing, crouching, pitch, yaw and both bird rigs.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
