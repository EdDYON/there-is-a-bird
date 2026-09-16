package EdDYON.guaniao.content.bird.kestrel;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class KestrelHoverLockTest {
    public static void main(String[] args) {
        Vec3 anchor = new Vec3(-14, 9.5, 0), position = anchor.add(2, 0, 0), velocity = new Vec3(-0.6, 0, 0);
        float yaw = 90;
        int readyTicks = 0;
        for (int tick = 0; tick < 220; tick++) {
            double phase = tick * 0.035;
            Vec3 drifting = anchor.add(Math.sin(phase) * 0.18, Math.sin(phase * 0.63) * 0.10, Math.cos(phase) * 0.18);
            var intent = new KestrelFlightMotor.Intent(KestrelFlightMotor.Mode.HOVER, drifting, 0.48,
                    -0.20, 0.24, 5, Vec3.ZERO, Vec3.ZERO);
            velocity = KestrelFlightMotor.advance(velocity, KestrelFlightMotor.desiredVelocity(position, velocity, intent), intent.mode(), yaw);
            float preyYaw = (float)Math.toDegrees(Math.atan2(-position.z, -position.x)) - 90;
            if (velocity.horizontalDistance() < 0.08) yaw += Mth.clamp(Mth.wrapDegrees(preyYaw - yaw), -3, 3);
            else yaw = (float)Math.toDegrees(Math.atan2(velocity.z, velocity.x)) - 90;
            position = position.add(velocity);
            if (position.distanceToSqr(anchor) < 1 && velocity.lengthSqr() < 0.015
                    && Math.abs(Mth.wrapDegrees(preyYaw - yaw)) < 15) ++readyTicks;
            else readyTicks = 0;
            if (readyTicks >= 20) {
                System.out.println("PASS: hover approach settles and faces prey for target lock before timeout.");
                return;
            }
        }
        throw new AssertionError("hover could not settle and aim before timeout, position=" + position + " yaw=" + yaw);
    }
}
