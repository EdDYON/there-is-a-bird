package EdDYON.guaniao.content.bird.nightheron;

import java.util.EnumSet;
import EdDYON.guaniao.content.bird.nightheron.NightHeronBehaviorState;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronFlightController;
import EdDYON.guaniao.content.bird.nightheron.NightHeronLandingSelector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class NightHeronRoostFlightGoal
extends Goal {
    private final NightHeronEntity nightHeron;
    private BlockPos roostTarget;
    private Vec3 flightDirection = Vec3.ZERO;
    private int remainingTicks;

    public NightHeronRoostFlightGoal(NightHeronEntity nightHeron) {
        this.nightHeron = nightHeron;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        if (!this.nightHeron.shouldRoost() || !this.nightHeron.onGround() || this.nightHeron.getTarget() != null || this.nightHeron.hasExternalFright() || this.nightHeron.getRandom().nextInt(this.roostFlightChance()) != 0) {
            return false;
        }
        this.roostTarget = NightHeronLandingSelector.findRoostLanding(this.nightHeron, 8, 44);
        return this.roostTarget != null && NightHeronLandingSelector.isRoostingSpot(this.nightHeron.level(), this.roostTarget) && this.roostTarget.getY() >= this.nightHeron.blockPosition().getY() + 1;
    }

    public boolean canContinueToUse() {
        return this.nightHeron.isControlledFlightActive() && (this.remainingTicks > 0 || !this.nightHeron.onGround()) && this.roostTarget != null && !this.nightHeron.hasExternalFright();
    }

    public void start() {
        this.remainingTicks = 100 + this.nightHeron.getRandom().nextInt(130);
        this.flightDirection = NightHeronLandingSelector.directionTo(this.roostTarget, this.nightHeron);
        NightHeronFlightController.takeOff(this.nightHeron, this.flightDirection, 0.46, 0.82);
    }

    public void stop() {
        this.remainingTicks = 0;
        this.roostTarget = null;
        this.flightDirection = Vec3.ZERO;
        this.nightHeron.getNavigation().stop();
        this.nightHeron.settleInterruptedFlight(NightHeronBehaviorState.ROOSTING);
    }

    public void tick() {
        --this.remainingTicks;
        this.nightHeron.getNavigation().stop();
        if (NightHeronFlightController.shouldBeginLandingApproach(this.nightHeron, this.roostTarget, this.remainingTicks, 14.0)) {
            if (NightHeronFlightController.tickLandingApproach(this.nightHeron, this.roostTarget)) {
                this.remainingTicks = 0;
                this.nightHeron.setBehaviorState(NightHeronBehaviorState.ROOSTING);
            }
            return;
        }
        this.flightDirection = NightHeronLandingSelector.directionTo(this.roostTarget, this.nightHeron);
        NightHeronFlightController.tickLocalFlight(this.nightHeron, this.flightDirection);
    }

    private int roostFlightChance() {
        return NightHeronLandingSelector.hasRoostCoverNear(this.nightHeron.level(), this.nightHeron.blockPosition(), 5) ? 320 : 70;
    }
}
