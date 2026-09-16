package EdDYON.guaniao.content.bird.kestrel;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** One server-side writer for airborne velocity and facing. Behaviors only submit intent. */
public final class KestrelFlightMotor {
    public enum Mode {
        CRUISE(0.035, 4), LAUNCH(0.040, 7), HOVER(0.055, 6), DIVE(0.065, 4),
        RECOVER(0.045, 6), LAND(0.050, 5), FLEE(0.060, 10), FOLLOW(0.045, 7), FETCH(0.045, 6);
        public final double acceleration;
        public final double turnDegrees;
        Mode(double acceleration, double turnDegrees) {
            this.acceleration = acceleration;
            this.turnDegrees = turnDegrees;
        }
    }

    public record Intent(Mode mode, Vec3 target, double speed, double minY, double maxY,
                         double arrivalRadius, Vec3 matchingVelocity, @Nullable Vec3 lookTarget) { }
    @Nullable private Intent intent;

    public void beginTick() { this.intent = null; }
    public void request(Intent intent) { this.intent = intent; }

    public void tick(KestrelEntity bird) {
        Intent request = this.intent;
        if (request == null || bird.isPassenger() || !bird.isAlive()) return;
        bird.getNavigation().stop();
        bird.setNoGravity(true);
        Vec3 current = bird.getDeltaMovement();
        Vec3 desired = desiredVelocity(bird.position(), current, request);
        desired = avoidObstacles(bird, desired);
        Vec3 next = advance(current, desired, request.mode(), bird.getYRot());
        // Contact safety wins over inertia if there is no space even for one tick.
        if (!clear(bird, next, 1.0D)) next = Vec3.ZERO;
        bird.setDeltaMovement(next);
        bird.hasImpulse = true;
        double horizontal = next.horizontalDistance();
        float yaw = bird.getYRot();
        if (request.lookTarget() != null && horizontal < 0.08D) {
            Vec3 look = request.lookTarget().subtract(bird.position());
            if (look.horizontalDistanceSqr() > 1.0E-6D) {
                float targetYaw = (float)Math.toDegrees(Math.atan2(look.z, look.x)) - 90.0F;
                yaw += Mth.clamp(Mth.wrapDegrees(targetYaw - yaw), -3.0F, 3.0F);
            }
        } else if (horizontal > 1.0E-5D) {
            yaw = (float)Math.toDegrees(Math.atan2(next.z, next.x)) - 90.0F;
        }
        bird.setYRot(yaw);
        bird.yBodyRot = yaw;
        bird.setYHeadRot(yaw);
        float pitch = horizontal < 0.08D ? 0.0F
                : Mth.clamp((float)-Math.toDegrees(Math.atan2(next.y, horizontal)), -65.0F, 40.0F);
        bird.setXRot(Mth.approach(bird.getXRot(), pitch, 4.0F));
    }

    public static Vec3 desiredVelocity(Vec3 position, Vec3 current, Intent request) {
        Vec3 delta = request.target().subtract(position);
        double distance = delta.length();
        if (request.mode() == Mode.DIVE) {
            // Preserve the committed three-dimensional line even on a steep dive.
            // Ease near impact so the recovery can flatten before ground contact.
            Vec3 dive = delta.scale(Math.min(0.22D, request.speed() / Math.max(distance, 1.0E-5D)));
            if (dive.y < request.minY()) dive = dive.scale(request.minY() / dive.y);
            if (dive.y > request.maxY()) dive = dive.scale(request.maxY() / dive.y);
            return dive.add(request.matchingVelocity());
        }
        double horizontal = delta.horizontalDistance();
        double speed = request.speed();
        if (request.arrivalRadius() > 0) speed *= Math.min(1.0D, distance / request.arrivalRadius());
        // Brake before a reversal instead of tracing a tight circle around an arrival point.
        if (request.arrivalRadius() > 0 && current.horizontalDistance() > 0.05D && horizontal > 1.0E-5D) {
            double alignment = (delta.x * current.x + delta.z * current.z) / (horizontal * current.horizontalDistance());
            speed *= 0.15D + 0.85D * Math.max(0.0D, alignment);
        }
        Vec3 horizontalVelocity = horizontal < 1.0E-5D ? Vec3.ZERO
                : new Vec3(delta.x, 0, delta.z).scale(speed / horizontal);
        return horizontalVelocity.add(0, Mth.clamp(delta.y * 0.16D, request.minY(), request.maxY()), 0)
                .add(request.matchingVelocity());
    }

    /** Bounded vector acceleration plus bounded heading; also used by numerical regressions. */
    public static Vec3 advance(Vec3 current, Vec3 desired, Mode mode, float fallbackYaw) {
        double oldSpeed = current.horizontalDistance();
        double desiredSpeed = desired.horizontalDistance();
        // Tiny hover drift is not a flight heading: launch along the body's aim.
        double heading = oldSpeed > 0.05D ? Math.atan2(current.z, current.x)
                : Math.toRadians(fallbackYaw + 90.0D);
        double desiredHeading = desiredSpeed > 1.0E-5D ? Math.atan2(desired.z, desired.x) : heading;
        double turn = Math.atan2(Math.sin(desiredHeading - heading), Math.cos(desiredHeading - heading));
        double maxTurn = Math.toRadians(mode.turnDegrees / Math.max(1.0D, oldSpeed / 0.65D));
        heading += Mth.clamp(turn, -maxTurn, maxTurn);
        double speed = oldSpeed + Mth.clamp(desiredSpeed - oldSpeed, -mode.acceleration, mode.acceleration);
        double y = current.y + Mth.clamp(desired.y - current.y, -mode.acceleration, mode.acceleration);
        Vec3 candidate = new Vec3(Math.cos(heading) * speed, y, Math.sin(heading) * speed);
        Vec3 acceleration = candidate.subtract(current);
        if (acceleration.lengthSqr() > mode.acceleration * mode.acceleration) {
            candidate = current.add(acceleration.normalize().scale(mode.acceleration));
        }
        return candidate;
    }

    private static boolean clear(KestrelEntity bird, Vec3 movement, double ticks) {
        return bird.level().noCollision(bird, bird.getBoundingBox().deflate(0.015D)
                .expandTowards(movement.scale(ticks)));
    }

    private Vec3 avoidObstacles(KestrelEntity bird, Vec3 desired) {
        Vec3 probe = bird.getDeltaMovement().scale(0.65D).add(desired.scale(0.35D));
        if (clear(bird, probe, 4.0D) && clear(bird, desired, 4.0D)) return desired;
        Vec3 best = Vec3.ZERO;
        double bestScore = -Double.MAX_VALUE;
        for (int turn : new int[]{0, -25, 25, -45, 45}) {
            for (double climb : new double[]{0.0D, 0.22D}) {
                Vec3 candidate = desired.yRot((float)Math.toRadians(turn));
                candidate = new Vec3(candidate.x, Math.max(candidate.y, climb == 0 ? -1 : climb), candidate.z);
                if (!clear(bird, candidate, 4.0D)) continue;
                double score = -candidate.distanceToSqr(desired) - Math.abs(turn) * 0.001D;
                if (score > bestScore) { bestScore = score; best = candidate; }
            }
        }
        return best;
    }
}
