package EdDYON.guaniao.content.bird.flight;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;

/** Observes vanilla navigation flight without changing its steering or gravity. */
public final class BirdNavigationMoveControl extends FlyingMoveControl {
    private final BirdNavigationFlightState navigationFlight = new BirdNavigationFlightState();

    public BirdNavigationMoveControl(Mob mob, int maxTurn, boolean hoversInPlace) {
        super(mob, maxTurn, hoversInPlace);
    }

    @Override
    public void tick() {
        if (this.operation == MoveControl.Operation.MOVE_TO
                && this.mob.position().distanceToSqr(this.wantedX, this.wantedY, this.wantedZ)
                    >= 2.5000003E-7D) {
            this.navigationFlight.navigationMovedAt(this.mob.tickCount);
        }
        super.tick();
    }

    /** Called on the server after super.aiStep(), once travel has established whether we left the ground. */
    public static boolean flightActiveAfterTravel(Mob bird) {
        if (!(bird.getMoveControl() instanceof BirdNavigationMoveControl control)) {
            return false;
        }
        return control.navigationFlight.afterTravel(bird.tickCount, bird.onGround(), bird.isPassenger(),
                bird.isInWaterOrBubble(), bird.isNoAi(), bird.isAlive());
    }
}
