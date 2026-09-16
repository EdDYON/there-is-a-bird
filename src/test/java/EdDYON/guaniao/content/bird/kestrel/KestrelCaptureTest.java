package EdDYON.guaniao.content.bird.kestrel;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class KestrelCaptureTest {
    public static void main(String[] args) {
        AABB prey = new AABB(-0.2, 0, -0.2, 0.2, 0.5, 0.2);
        check(!KestrelTalons.touching(new Vec3(1, 0.5, 0), prey), "no one-block suction");
        check(!KestrelTalons.touching(new Vec3(0, 0.9, 0), prey), "no grab above the back");
        check(!KestrelTalons.touching(new Vec3(0, 0.1, 0), prey), "no grab below the back");
        check(KestrelTalons.touching(new Vec3(0.05, 0.52, 0.02), prey), "actual foot contact accepted");
        check(KestrelTalons.touching(new Vec3(0.21, 0.50, 0), prey), "contact at the back edge accepted");
        check(KestrelTalons.touching(new Vec3(0, 0.65, 0), new Vec3(0, 0.35, 0), prey, Vec3.ZERO),
                "contact crossed between ticks is not missed");
        check(!KestrelTalons.touching(new Vec3(-1, 0.5, 0), new Vec3(1, 0.5, 0), prey, Vec3.ZERO),
                "long sweep cannot attach prey a block away");
        for (double length : new double[]{12, 14, 18}) {
            Vec3 start = new Vec3(-length, 9.5, 0), position = start, velocity = new Vec3(0.02, 0, 0);
            Vec3 contact = new Vec3(-KestrelTalons.FORWARD, 0.5 - KestrelTalons.SOLE_Y, 0);
            float pitch = 0;
            boolean caught = false;
            for (int tick = 0; tick < 100; tick++) {
                Vec3 talon = position.add(KestrelTalons.offset(-90, pitch, 1));
                if (KestrelTalons.touching(talon, prey)) {
                    Vec3 preyPosition = new Vec3(0, 0, 0);
                    Vec3 offset = preyPosition.subtract(talon.add(0, -0.5, 0));
                    check(talon.add(offset).add(0, -0.5, 0).distanceTo(preyPosition) < 1.0E-8,
                            "mount retains prey position instead of snapping");
                    check(Math.abs(velocity.y) < 0.16, "swoop flattens before capture");
                    caught = true;
                    break;
                }
                Vec3 aim = KestrelFlightPaths.swoopTarget(start, contact, position);
                var intent = new KestrelFlightMotor.Intent(KestrelFlightMotor.Mode.DIVE, aim,
                        KestrelFlightPaths.swoopSpeed(contact, position), -0.55, 0.12, 0, Vec3.ZERO, null);
                velocity = KestrelFlightMotor.advance(velocity,
                        KestrelFlightMotor.desiredVelocity(position, velocity, intent), intent.mode(), -90);
                float targetPitch = velocity.horizontalDistance() < 0.08 ? 0
                        : Mth.clamp((float)-Math.toDegrees(Math.atan2(velocity.y, velocity.horizontalDistance())), -65, 40);
                pitch = Mth.approach(pitch, targetPitch, 4);
                position = position.add(velocity);
                check(position.y > 0, "swoop must not cross ground");
            }
            check(caught, "swoop reaches physical capture, run-in=" + length + " final=" + position + " pitch=" + pitch);
        }
        Vec3 returning = new Vec3(100, 15, 0), flightVelocity = Vec3.ZERO;
        float heading = -90;
        for (int tick = 0; tick < 400; tick++) {
            var intent = new KestrelFlightMotor.Intent(KestrelFlightMotor.Mode.FOLLOW, Vec3.ZERO,
                    0.64, -0.22, 0.28, 6, Vec3.ZERO, null);
            flightVelocity = KestrelFlightMotor.advance(flightVelocity,
                    KestrelFlightMotor.desiredVelocity(returning, flightVelocity, intent), intent.mode(), heading);
            if (flightVelocity.horizontalDistance() > 0.00001) {
                heading = (float)Math.toDegrees(Math.atan2(flightVelocity.z, flightVelocity.x)) - 90;
            }
            check(flightVelocity.length() < 1, "distant return must use continuous movement");
            returning = returning.add(flightVelocity);
        }
        check(returning.length() < 0.8, "return from 100 blocks reaches owner by flight");
        System.out.println("PASS: rejects distant grabs; 3 contact swoops with flat finishes; zero mount snap; continuous 100-block return.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
