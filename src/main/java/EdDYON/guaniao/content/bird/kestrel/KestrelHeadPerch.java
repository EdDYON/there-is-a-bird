package EdDYON.guaniao.content.bird.kestrel;

import net.minecraft.world.phys.Vec3;

/** Contact point on the crown, transformed with the player's actual head pose. */
public final class KestrelHeadPerch {
    private static final double PIXEL = 0.9375D / 16.0D;

    private KestrelHeadPerch() { }

    public static Vec3 offset(float yawDegrees, float pitchDegrees, double soleOffset, double soleForward) {
        double pitch = Math.toRadians(Math.max(-90.0F, Math.min(90.0F, pitchDegrees)));
        double yaw = Math.toRadians(yawDegrees);
        double crown = 8.5D * PIXEL + 0.001D - soleOffset;
        double forward = crown * Math.sin(pitch) - soleForward * Math.cos(pitch);
        double height = 1.501D * 0.9375D + crown * Math.cos(pitch) + soleForward * Math.sin(pitch);
        return new Vec3(-Math.sin(yaw) * forward, height, Math.cos(yaw) * forward);
    }
}
