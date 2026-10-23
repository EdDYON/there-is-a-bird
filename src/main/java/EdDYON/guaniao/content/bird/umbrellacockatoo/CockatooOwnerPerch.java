package EdDYON.guaniao.content.bird.umbrellacockatoo;

import EdDYON.guaniao.content.bird.BirdHeadPerch;
import net.minecraft.world.phys.Vec3;

/** Contact points for the authored standing rig, including GeckoLib's .01 model translation. */
public final class CockatooOwnerPerch {
    // Lowest matching left/right toe vertices in umbrella_cockatoo.geo.json.
    public static final double SOLE_Y = 0.10032576924544323D / 16.0D;
    public static final double SOLE_FORWARD = -2.27559868189111D / 16.0D;

    private CockatooOwnerPerch() { }

    public static Vec3 head(float yaw, float pitch, double scale, boolean crouching) {
        return BirdHeadPerch.offset(yaw, pitch, SOLE_Y * scale, SOLE_FORWARD * scale, crouching);
    }

}
