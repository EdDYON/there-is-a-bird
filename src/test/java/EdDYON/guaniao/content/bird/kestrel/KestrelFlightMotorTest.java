package EdDYON.guaniao.content.bird.kestrel;

import java.util.Random;
import net.minecraft.world.phys.Vec3;
import EdDYON.guaniao.content.bird.kestrel.KestrelFlightMotor.Intent;
import EdDYON.guaniao.content.bird.kestrel.KestrelFlightMotor.Mode;

/** Standalone numerical trajectories through the production motor, with no game world. */
public final class KestrelFlightMotorTest {
    public static void main(String[] args) {
        Random random = new Random(812L);
        for (Mode mode : Mode.values()) {
            for (int i = 0; i < 2000; i++) {
                Vec3 current = new Vec3(random.nextDouble() * 2 - 1, random.nextDouble() - 0.5, random.nextDouble() * 2 - 1);
                Vec3 desired = new Vec3(random.nextDouble() * 2 - 1, random.nextDouble() - 0.5, random.nextDouble() * 2 - 1);
                Vec3 next = KestrelFlightMotor.advance(current, desired, mode, 0);
                check(next.subtract(current).length() <= mode.acceleration + 1.0E-8, "acceleration bound " + mode);
                if (current.horizontalDistance() > 0.05 && next.horizontalDistance() > 0.01) {
                    double angle = Math.atan2(current.x * next.z - current.z * next.x, current.x * next.x + current.z * next.z);
                    double cap = Math.toRadians(mode.turnDegrees / Math.max(1, current.horizontalDistance() / 0.65));
                    check(Math.abs(angle) <= cap + 1.0E-7, "turn bound " + mode);
                }
            }
        }
        for (Mode mode : new Mode[]{Mode.HOVER, Mode.LAND, Mode.FETCH}) {
            for (int angle = 0; angle < 360; angle += 30) {
                Vec3 velocity = new Vec3(Math.cos(Math.toRadians(angle)) * 0.62, -0.15, Math.sin(Math.toRadians(angle)) * 0.62);
                Vec3 position = new Vec3(0.2, 1, 0.1);
                boolean settled = false;
                for (int tick = 0; tick < 220; tick++) {
                    Intent request = new Intent(mode, Vec3.ZERO, 0.48, -0.20, 0.24, 5, Vec3.ZERO, null);
                    velocity = step(position, velocity, request);
                    position = position.add(velocity);
                    if (position.length() < 0.28 && velocity.length() < 0.1) { settled = true; break; }
                }
                check(settled, "arrival convergence " + mode + " heading=" + angle);
            }
        }
        for (int angle = 0; angle < 360; angle += 45) {
            Vec3 landingPosition = new Vec3(30, 18, 0);
            Vec3 landingVelocity = new Vec3(Math.cos(Math.toRadians(angle)) * 0.62, 0, Math.sin(Math.toRadians(angle)) * 0.62);
            Vec3 approach = new Vec3(-3, 2.5D, 0);
            boolean finalApproach = false, landed = false;
            for (int tick = 0; tick < 220; tick++) {
                if (landingPosition.distanceToSqr(approach) < 1 && landingVelocity.lengthSqr() < 0.09) finalApproach = true;
                Vec3 target = finalApproach ? Vec3.ZERO : approach;
                Intent intent = new Intent(Mode.LAND, target, finalApproach ? 0.25 : 0.46,
                        -0.18, 0.16, finalApproach ? 2.5 : 5, Vec3.ZERO, null);
                landingVelocity = step(landingPosition, landingVelocity, intent);
                landingPosition = landingPosition.add(landingVelocity);
                if (finalApproach && landingPosition.length() < 0.28 && landingVelocity.lengthSqr() < 0.012) {
                    landed = true;
                    break;
                }
            }
            check(landed, "two-stage landing within timeout, heading=" + angle);
        }
        // A moving owner should be matched, not approached with a full-speed overshoot.
        Vec3 position = Vec3.ZERO, velocity = Vec3.ZERO;
        for (double ownerSpeed : new double[]{0.0D, 0.22D, 0.28D, 0.40D}) {
            Vec3 owner = Vec3.ZERO, playerVelocity = new Vec3(ownerSpeed, 0, 0);
            position = new Vec3(-10, 3, 4);
            velocity = Vec3.ZERO;
            for (int tick = 0; tick < 350; tick++) {
                owner = owner.add(playerVelocity);
                Vec3 approach = KestrelFlightPaths.ownerApproachTarget(owner, playerVelocity, position.distanceTo(owner));
                Intent request = new Intent(Mode.FOLLOW, approach, 0.64, -0.22, 0.28, 6, playerVelocity, null);
                velocity = step(position, velocity, request);
                position = position.add(velocity);
            }
            check(position.distanceTo(owner) < 0.8 && velocity.subtract(playerVelocity).length() < 0.1,
                    "owner velocity matching, speed=" + ownerSpeed);
        }
        for (int direction : new int[]{-1, 1}) {
            position = new Vec3(24, 18, 0);
            velocity = new Vec3(0, 0, direction * 0.62);
            for (int tick = 0; tick < 1000; tick++) {
                Vec3 target = KestrelFlightPaths.orbitTarget(Vec3.ZERO, position, 24, direction, 18);
                velocity = step(position, velocity, new Intent(Mode.CRUISE, target, 0.62, -0.16, 0.24, 0, Vec3.ZERO, null));
                position = position.add(velocity);
                check(position.horizontalDistance() > 15 && position.horizontalDistance() < 32, "bounded cruise orbit");
                check((position.x * velocity.z - position.z * velocity.x) * direction > 0, "orbit must not reverse");
            }
        }
        // A steep attack must reach the prey before it passes horizontally overhead.
        for (double offset : new double[]{3.5D, 8.0D, 14.0D}) {
            position = new Vec3(-offset, 9.5D, 0);
            velocity = new Vec3(0.02D, 0, 0);
            boolean reached = false;
            for (int tick = 0; tick < 65; tick++) {
                velocity = step(position, velocity, new Intent(Mode.DIVE, Vec3.ZERO, 1.05D, -0.72D, 0.12D, 0, Vec3.ZERO, null));
                position = position.add(velocity);
                if (position.lengthSqr() <= 2.1D) { reached = true; break; }
            }
            check(reached, "dive reaches prey at horizontal offset " + offset);
        }
        Vec3 drift = new Vec3(-0.02D, 0, 0);
        Vec3 launch = KestrelFlightMotor.advance(drift, new Vec3(0.3D, -0.72D, 0), Mode.DIVE, -90);
        check(launch.x > drift.x && Math.abs(launch.z) < 1.0E-8,
                "hover drift must not cause a sideways turn on dive launch");
        Vec3 committed = Vec3.ZERO;
        for (int tick = 1; tick <= 30; tick++) {
            Vec3 updated = KestrelFlightPaths.correctDive(committed, new Vec3(20, 0, tick % 2 == 0 ? 20 : -20), tick);
            check(updated.distanceTo(committed) <= (tick <= 10 ? 0 : 0.12) + 1.0E-8, "committed dive line");
            committed = updated;
        }
        velocity = new Vec3(0.95, -0.60, 0);
        for (int tick = 1; tick <= 38; tick++) {
            double lift = KestrelFlightPaths.recoveryLift(tick);
            Vec3 previous = velocity;
            velocity = KestrelFlightMotor.advance(velocity, new Vec3(tick <= 7 ? 0.9 : 0.68, lift, 0), Mode.RECOVER, -90);
            check(velocity.x > 0.60, "pass-through retains forward momentum");
            check(velocity.subtract(previous).length() <= 0.045 + 1.0E-8, "smooth pull-up");
            if (tick <= 7) check(lift == 0, "pass before pull-up");
        }
        check(velocity.y > 0.25, "recovery reaches climb");
        System.out.println("PASS: 18,000 acceleration/turn cases; 36 arrival trajectories; 8 staged landings; moving-owner match; 2,000 orbit ticks; 3 dive approaches; hover departure, dive commitment and progressive recovery.");
    }

    private static Vec3 step(Vec3 position, Vec3 velocity, Intent request) {
        float yaw = (float)Math.toDegrees(Math.atan2(velocity.z, velocity.x)) - 90;
        return KestrelFlightMotor.advance(velocity, KestrelFlightMotor.desiredVelocity(position, velocity, request), request.mode(), yaw);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
