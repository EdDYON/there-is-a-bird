package EdDYON.guaniao.content.bird.nightheron;

import java.util.EnumSet;
import EdDYON.guaniao.content.bird.nightheron.NightHeronBehaviorState;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronFlightController;
import EdDYON.guaniao.content.bird.nightheron.NightHeronLandingSelector;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class NightHeronHighTransitGoal
extends Goal {
    private final NightHeronEntity nightHeron;
    private BlockPos landingTarget;
    private Vec3 flightDirection = Vec3.ZERO;
    private int remainingTicks;

    public NightHeronHighTransitGoal(NightHeronEntity nightHeron) {
        this.nightHeron = nightHeron;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        int chance = this.nightHeron.level().isRaining() ? 650 : 900;
        return this.nightHeron.isActiveTime() && this.nightHeron.onGround() && this.nightHeron.getTarget() == null && !this.nightHeron.hasExternalFright() && this.nightHeron.getRandom().nextInt(chance) == 0;
    }

    public boolean canContinueToUse() {
        return this.nightHeron.isControlledFlightActive() && (this.remainingTicks > 0 || !this.nightHeron.onGround()) && !this.nightHeron.hasExternalFright();
    }

    public void start() {
        this.remainingTicks = 120 + this.nightHeron.getRandom().nextInt(101);
        this.landingTarget = NightHeronLandingSelector.findTransitLanding(this.nightHeron, 28, 72);
        this.flightDirection = this.landingTarget != null ? NightHeronLandingSelector.directionTo(this.landingTarget, this.nightHeron) : this.randomDirection();
        NightHeronFlightController.takeOff(this.nightHeron, this.flightDirection, 0.36, 0.72);
    }

    public void stop() {
        this.remainingTicks = 0;
        this.landingTarget = null;
        this.flightDirection = Vec3.ZERO;
        this.nightHeron.getNavigation().stop();
        this.nightHeron.settleInterruptedFlight(NightHeronBehaviorState.IDLE);
    }

    public void tick() {
        --this.remainingTicks;
        this.nightHeron.getNavigation().stop();
        if (this.remainingTicks <= 0 && this.landingTarget == null) {
            this.landingTarget = NightHeronLandingSelector.findTransitLanding(this.nightHeron, 10, 36);
            if (this.landingTarget == null) {
                NightHeronFlightController.tickOpenLanding(this.nightHeron, this.flightDirection);
                return;
            }
        }
        if (this.landingTarget != null && NightHeronFlightController.shouldBeginLandingApproach(this.nightHeron, this.landingTarget, this.remainingTicks, 26.0)) {
            if (NightHeronFlightController.tickLandingApproach(this.nightHeron, this.landingTarget)) {
                this.remainingTicks = 0;
            }
            return;
        }
        if (this.landingTarget != null) {
            this.flightDirection = NightHeronLandingSelector.directionTo(this.landingTarget, this.nightHeron);
        } else if (this.flightDirection.lengthSqr() <= 1.0E-4 || this.nightHeron.horizontalCollision || this.remainingTicks % 40 == 0) {
            this.flightDirection = this.randomDirection();
        }
        NightHeronFlightController.tickHighTransitFlight(this.nightHeron, this.flightDirection);
    }

    private Vec3 randomDirection() {
        float angle = this.nightHeron.getRandom().nextFloat() * ((float)Math.PI * 2);
        return new Vec3((double)Mth.cos((float)angle), 0.0, (double)Mth.sin((float)angle));
    }
}

