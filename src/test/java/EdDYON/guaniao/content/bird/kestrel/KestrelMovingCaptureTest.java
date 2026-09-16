package EdDYON.guaniao.content.bird.kestrel;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Moving prey, including the live controller's pass/miss condition. */
public final class KestrelMovingCaptureTest {
    public static void main(String[] args) {
        for (double speed : new double[]{0, 0.10, 0.20, 0.25}) {
            Vec3 prey = Vec3.ZERO, preyVelocity = new Vec3(speed, 0, 0);
            Vec3 start = new Vec3(-14, 9.5, 0), position = start, velocity = new Vec3(0.02, 0, 0);
            Vec3 committed = new Vec3(-KestrelTalons.FORWARD, 0.45 - KestrelTalons.SOLE_Y, 0);
            float yaw = -90, pitch = 0;
            Vec3 previousFoot = position.add(KestrelTalons.offset(yaw, pitch, 1));
            boolean caught = false;
            for (int tick = 1; tick <= 100; tick++) {
                prey = prey.add(preyVelocity);
                Vec3 aim = prey.add(preyVelocity.scale(Math.min(4, position.distanceTo(prey) * 0.5)))
                        .add(-KestrelTalons.FORWARD, 0.45 - KestrelTalons.SOLE_Y, 0);
                committed = KestrelFlightPaths.correctDive(committed, aim, tick, preyVelocity);
                AABB back = new AABB(prey.x - 0.225, prey.y, prey.z - 0.225, prey.x + 0.225, prey.y + 0.45, prey.z + 0.225);
                Vec3 foot = position.add(KestrelTalons.offset(yaw, pitch, 1));
                if (KestrelTalons.touching(previousFoot, foot, back, preyVelocity)) {
                    caught = true;
                    break;
                }
                Vec3 remaining = committed.subtract(position);
                if (tick > 10 && remaining.lengthSqr() < 16 && remaining.dot(velocity) < 0) break;
                Vec3 approach = KestrelFlightPaths.swoopTarget(start, committed, position);
                previousFoot = foot;
                var intent = new KestrelFlightMotor.Intent(KestrelFlightMotor.Mode.DIVE, approach,
                        KestrelFlightPaths.swoopSpeed(committed, position), -0.55, 0.12, 0,
                        KestrelFlightPaths.captureMatchingVelocity(preyVelocity, position.distanceTo(prey)), null);
                velocity = KestrelFlightMotor.advance(velocity, KestrelFlightMotor.desiredVelocity(position, velocity, intent), intent.mode(), yaw);
                if (velocity.horizontalDistance() > 0.00001) yaw = (float)Math.toDegrees(Math.atan2(velocity.z, velocity.x)) - 90;
                float targetPitch = velocity.horizontalDistance() < 0.08 ? 0
                        : Mth.clamp((float)-Math.toDegrees(Math.atan2(velocity.y, velocity.horizontalDistance())), -65, 40);
                pitch = Mth.approach(pitch, targetPitch, 4);
                position = position.add(velocity);
            }
            if (!caught) throw new AssertionError("moving capture failed, prey speed=" + speed + " bird=" + position + " prey=" + prey);
        }
        System.out.println("PASS: stationary, walking and fleeing prey reach physical capture before recovery.");
    }
}
