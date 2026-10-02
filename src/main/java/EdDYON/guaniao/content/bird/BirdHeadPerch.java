package EdDYON.guaniao.content.bird;

import net.minecraft.world.phys.Vec3;

/** Player-model crown contact shared by species, including the real standing/crouching head pivot. */
public final class BirdHeadPerch {
    private static final double PLAYER_SCALE = 0.9375D;
    private static final double PIXEL = PLAYER_SCALE / 16.0D;

    private BirdHeadPerch() { }

    public static Vec3 offset(float yawDegrees, float pitchDegrees, double soleOffset, double soleForward) {
        return offset(yawDegrees, pitchDegrees, soleOffset, soleForward, false);
    }

    public static Vec3 offset(float yawDegrees, float pitchDegrees, double soleOffset,
                              double soleForward, boolean crouching) {
        double pitch = Math.toRadians(Math.max(-90.0F, Math.min(90.0F, pitchDegrees)));
        double yaw = Math.toRadians(yawDegrees);
        double crown = 8.5D * PIXEL + 0.001D - soleOffset;
        double forward = crown * Math.sin(pitch) - soleForward * Math.cos(pitch);
        // HumanoidModel lowers the head by 4.2 pixels; PlayerRenderer also shifts a crouching player by -0.125.
        double pivotHeight = 1.501D * PLAYER_SCALE - (crouching ? 4.2D * PIXEL + 0.125D : 0);
        double height = pivotHeight + crown * Math.cos(pitch) + soleForward * Math.sin(pitch);
        return new Vec3(-Math.sin(yaw) * forward, height, Math.cos(yaw) * forward);
    }
}
