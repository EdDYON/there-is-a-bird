package EdDYON.guaniao.content.bird.kestrel;

import EdDYON.guaniao.content.bird.BirdHeadPerch;
import net.minecraft.world.phys.Vec3;

/** Contact point on the crown, transformed with the player's actual head pose. */
public final class KestrelHeadPerch {
    private KestrelHeadPerch() { }

    public static Vec3 offset(float yawDegrees, float pitchDegrees, double soleOffset, double soleForward) {
        return BirdHeadPerch.offset(yawDegrees, pitchDegrees, soleOffset, soleForward);
    }

    public static Vec3 offset(float yawDegrees, float pitchDegrees, double soleOffset,
                              double soleForward, boolean crouching) {
        return BirdHeadPerch.offset(yawDegrees, pitchDegrees, soleOffset, soleForward, crouching);
    }
}
