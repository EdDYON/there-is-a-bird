package EdDYON.guaniao.client.camera;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

public final class NikonCameraArmPose {
    private static final HumanoidModel.ArmPose CAMERA_HOLD = HumanoidModel.ArmPose.create("guaniao_camera_hold", true, NikonCameraArmPose::applyCameraHold);

    private NikonCameraArmPose() {
    }

    public static HumanoidModel.ArmPose cameraHold() {
        return CAMERA_HOLD;
    }

    private static void applyCameraHold(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float headYaw = model.head.yRot * 0.35F;
        float idle = Mth.sin(entity.tickCount * 0.067F) * 0.012F;

        model.rightArm.xRot = -1.32F + model.head.xRot * 0.18F + idle;
        model.rightArm.yRot = -0.24F + headYaw;
        model.rightArm.zRot = 0.04F;

        model.leftArm.xRot = -1.32F + model.head.xRot * 0.18F + idle;
        model.leftArm.yRot = 0.24F + headYaw;
        model.leftArm.zRot = -0.04F;
    }
}
