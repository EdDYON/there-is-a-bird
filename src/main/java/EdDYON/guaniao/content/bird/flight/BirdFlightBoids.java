package EdDYON.guaniao.content.bird.flight;

import EdDYON.guaniao.content.bird.flock.BirdFlockManager;
import java.util.List;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public final class BirdFlightBoids {
    private BirdFlightBoids() {
    }

    public static Vec3 sameTypeHeading(PathfinderMob bird, double radius, double separationRadius, double cohesionWeight, double alignmentWeight, double separationWeight, double randomnessWeight) {
        List<PathfinderMob> nearby = BirdFlockManager.nearby(bird, PathfinderMob.class, radius).stream()
                .filter(other -> other != bird
                        && other instanceof BirdFlightAware aware
                        && aware.isBirdFlightActive())
                .toList();
        return headingFrom(bird, nearby, separationRadius, cohesionWeight, alignmentWeight, separationWeight, randomnessWeight);
    }

    public static Vec3 headingFrom(PathfinderMob bird, List<? extends PathfinderMob> nearby, double separationRadius, double cohesionWeight, double alignmentWeight, double separationWeight, double randomnessWeight) {
        if (nearby.isEmpty()) {
            return randomHeading(bird, randomnessWeight);
        }
        Vec3 separation = Vec3.ZERO;
        Vec3 alignment = Vec3.ZERO;
        Vec3 center = Vec3.ZERO;
        int alignmentCount = 0;
        int centerCount = 0;
        double separationSqr = separationRadius * separationRadius;
        for (PathfinderMob other : nearby) {
            Vec3 offset = bird.position().subtract(other.position());
            double distanceSqr = offset.lengthSqr();
            if (distanceSqr > 1.0E-4D && distanceSqr < separationSqr) {
                double distance = Math.sqrt(distanceSqr);
                separation = separation.add(offset.normalize().scale((separationRadius - distance) / separationRadius));
            }
            Vec3 otherMovement = other.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
            if (otherMovement.lengthSqr() > 1.0E-4D) {
                alignment = alignment.add(otherMovement.normalize());
                ++alignmentCount;
            }
            center = center.add(other.position());
            ++centerCount;
        }
        Vec3 heading = Vec3.ZERO;
        if (separation.lengthSqr() > 1.0E-4D) {
            heading = heading.add(separation.normalize().scale(separationWeight));
        }
        if (alignmentCount > 0 && alignment.lengthSqr() > 1.0E-4D) {
            heading = heading.add(alignment.normalize().scale(alignmentWeight));
        }
        if (centerCount > 0) {
            Vec3 cohesion = center.scale(1.0D / (double)centerCount).subtract(bird.position()).multiply(1.0D, 0.0D, 1.0D);
            if (cohesion.lengthSqr() > 1.0E-4D) {
                heading = heading.add(cohesion.normalize().scale(cohesionWeight));
            }
        }
        return heading.add(randomHeading(bird, randomnessWeight));
    }

    private static Vec3 randomHeading(PathfinderMob bird, double randomnessWeight) {
        if (randomnessWeight <= 0.0D) {
            return Vec3.ZERO;
        }
        return BirdFlightTargeting.randomHorizontalDirection(bird.getRandom()).scale(randomnessWeight);
    }
}
