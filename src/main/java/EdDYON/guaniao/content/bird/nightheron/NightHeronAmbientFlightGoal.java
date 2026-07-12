package EdDYON.guaniao.content.bird.nightheron;

import java.util.EnumSet;
import EdDYON.guaniao.content.bird.nightheron.NightHeronBehaviorState;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronFlightController;
import EdDYON.guaniao.content.bird.nightheron.NightHeronLandingSelector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class NightHeronAmbientFlightGoal
extends Goal {
    private final NightHeronEntity nightHeron;
    private BlockPos landingTarget;
    private Vec3 flightDirection = Vec3.ZERO;
    private FlightKind flightKind = FlightKind.LOCAL;
    private int remainingTicks;

    public NightHeronAmbientFlightGoal(NightHeronEntity nightHeron) {
        this.nightHeron = nightHeron;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        int chance;
        if (!this.nightHeron.onGround() || this.nightHeron.getTarget() != null || this.nightHeron.hasExternalFright()) {
            return false;
        }
        int n = chance = this.nightHeron.isActiveTime() ? 120 : 900;
        if (this.nightHeron.getRandom().nextInt(chance) != 0) {
            return false;
        }
        this.flightKind = this.chooseFlightKind();
        int minRadius = this.flightKind == FlightKind.SOARING ? 30 : 10;
        int maxRadius = this.flightKind == FlightKind.SOARING ? 82 : 44;
        this.landingTarget = NightHeronLandingSelector.findTransitLanding(this.nightHeron, minRadius, maxRadius);
        return this.landingTarget != null;
    }

    public boolean canContinueToUse() {
        return this.nightHeron.isControlledFlightActive() && (this.remainingTicks > 0 || !this.nightHeron.onGround()) && this.landingTarget != null && !this.nightHeron.hasExternalFright();
    }

    public void start() {
        this.remainingTicks = this.flightKind == FlightKind.SOARING ? this.randomBetween(190, 360) : this.randomBetween(100, 220);
        this.flightDirection = NightHeronLandingSelector.directionTo(this.landingTarget, this.nightHeron);
        NightHeronFlightController.takeOff(this.nightHeron, this.flightDirection, 0.56, 0.82);
    }

    public void stop() {
        this.remainingTicks = 0;
        this.landingTarget = null;
        this.flightDirection = Vec3.ZERO;
        this.nightHeron.getNavigation().stop();
        this.nightHeron.settleInterruptedFlight(NightHeronBehaviorState.IDLE);
    }

    public void tick() {
        double approachDistance;
        --this.remainingTicks;
        this.nightHeron.getNavigation().stop();
        double d = approachDistance = this.flightKind == FlightKind.SOARING ? 26.0 : 14.0;
        if (NightHeronFlightController.shouldBeginLandingApproach(this.nightHeron, this.landingTarget, this.remainingTicks, approachDistance)) {
            if (NightHeronFlightController.tickLandingApproach(this.nightHeron, this.landingTarget)) {
                this.remainingTicks = 0;
            }
            return;
        }
        this.flightDirection = NightHeronLandingSelector.directionTo(this.landingTarget, this.nightHeron);
        if (this.flightKind == FlightKind.SOARING) {
            NightHeronFlightController.tickSoaringFlight(this.nightHeron, this.flightDirection);
        } else {
            NightHeronFlightController.tickLocalFlight(this.nightHeron, this.flightDirection);
        }
    }

    private FlightKind chooseFlightKind() {
        if (this.nightHeron.isActiveTime() && this.nightHeron.getRandom().nextInt(4) == 0) {
            return FlightKind.SOARING;
        }
        return FlightKind.LOCAL;
    }

    private int randomBetween(int min, int max) {
        return min + this.nightHeron.getRandom().nextInt(max - min + 1);
    }

    private static enum FlightKind {
        LOCAL,
        SOARING;

    }
}
