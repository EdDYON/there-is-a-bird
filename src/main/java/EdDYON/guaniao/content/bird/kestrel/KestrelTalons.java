package EdDYON.guaniao.content.bird.kestrel;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Extended-foot geometry shared by rendering, capture and passenger placement. */
public final class KestrelTalons {
    public static final double SOLE_Y = 0.229901D / 16.0D;
    public static final double FORWARD = 1.0D / 16.0D;

    private KestrelTalons() { }

    public static Vec3 offset(float yaw, float pitch, double scale) {
        double p = Math.toRadians(pitch), y = Math.toRadians(yaw);
        double up = (SOLE_Y * Math.cos(p) - FORWARD * Math.sin(p)) * scale;
        double forward = (SOLE_Y * Math.sin(p) + FORWARD * Math.cos(p)) * scale;
        return new Vec3(-Math.sin(y) * forward, up, Math.cos(y) * forward);
    }

    public static boolean touching(Vec3 talons, AABB prey) {
        return touching(talons, talons, prey, Vec3.ZERO);
    }

    public static boolean touching(Vec3 previous, Vec3 current, AABB prey, Vec3 preyMovement) {
        // Use the back's surface plus the small physical claw footprint, not a
        // point at the exact centre. Sweep in the prey's frame to avoid tick gaps.
        AABB contact = new AABB(prey.minX - 0.06D, prey.maxY - 0.08D, prey.minZ - 0.06D,
                prey.maxX + 0.06D, prey.maxY + 0.08D, prey.maxZ + 0.06D);
        if (contact.contains(current)) return true;
        if (!contact.inflate(0.18D).contains(current)) return false;
        return contact.clip(previous.add(preyMovement), current).isPresent();
    }
}
