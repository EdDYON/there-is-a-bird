package EdDYON.guaniao.content.bird;

import net.minecraft.world.entity.PathfinderMob;

public final class BirdGroundAnimation {
    private static final double WALK_MOTION_THRESHOLD_SQR = 1.0E-5D;

    private BirdGroundAnimation() {
    }

    public static boolean canPlayWalk(PathfinderMob bird) {
        return bird.onGround()
                && !bird.isPassenger()
                && !bird.isInWaterOrBubble();
    }

    public static boolean hasWalkMotion(PathfinderMob bird) {
        return canPlayWalk(bird)
                && (bird.getDeltaMovement().horizontalDistanceSqr() > WALK_MOTION_THRESHOLD_SQR
                || !bird.getNavigation().isDone());
    }
}
