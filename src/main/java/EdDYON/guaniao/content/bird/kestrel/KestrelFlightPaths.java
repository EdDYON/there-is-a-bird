package EdDYON.guaniao.content.bird.kestrel;

import net.minecraft.world.phys.Vec3;

/** Geometry shared by the behavior phases and their trajectory regressions. */
public final class KestrelFlightPaths {
    private KestrelFlightPaths() { }

    public static Vec3 orbitTarget(Vec3 center, Vec3 position, double radius, int direction, double altitude) {
        double angle = Math.atan2(position.z - center.z, position.x - center.x)
                + direction * Math.toRadians(32);
        return new Vec3(center.x + Math.cos(angle) * radius, altitude, center.z + Math.sin(angle) * radius);
    }

    public static Vec3 correctDive(Vec3 committed, Vec3 prediction, int diveTicks) {
        return correctDive(committed, prediction, diveTicks, Vec3.ZERO);
    }

    public static Vec3 correctDive(Vec3 committed, Vec3 prediction, int diveTicks, Vec3 preyMovement) {
        if (diveTicks <= 10) return committed;
        committed = committed.add(preyMovement);
        Vec3 correction = prediction.subtract(committed);
        return committed.add(correction.length() > 0.12D ? correction.normalize().scale(0.12D) : correction);
    }

    public static Vec3 swoopTarget(Vec3 start, Vec3 contact, Vec3 position) {
        Vec3 horizontal = contact.subtract(start).multiply(1, 0, 1);
        double length = horizontal.length();
        if (length < 1.0D) return contact;
        Vec3 heading = horizontal.scale(1.0D / length);
        double progress = Math.max(0, Math.min(1,
                (position.subtract(start).dot(heading) + 3.0D) / length));
        // A descending parabola becomes horizontal at the contact point.
        double height = contact.y + Math.max(0, start.y - contact.y) * Math.pow(1 - progress, 2);
        return new Vec3(start.x + horizontal.x * progress, height, start.z + horizontal.z * progress);
    }

    public static double swoopSpeed(Vec3 contact, Vec3 position) {
        return Math.max(0.08D, Math.min(0.85D, contact.subtract(position).horizontalDistance() * 0.10D));
    }

    public static Vec3 captureMatchingVelocity(Vec3 preyMovement, double distance) {
        return preyMovement.scale(Math.max(0, Math.min(1, 1 - distance / 10.0D)));
    }

    public static double recoveryLift(int tick) {
        return tick <= 7 ? 0.0D : Math.min(0.30D, (tick - 7) * 0.018D);
    }

    public static Vec3 ownerApproachTarget(Vec3 anchor, Vec3 velocity, double distance) {
        // Fade prediction at contact; a fixed lead can keep the bird ahead of a runner forever.
        double lead = Math.min(8.0D, distance * 0.5D);
        return anchor.add(velocity.scale(lead));
    }
}
