package EdDYON.guaniao.content.bird.nightheron;

import java.util.EnumSet;
import EdDYON.guaniao.content.bird.nightheron.NightHeronBehaviorState;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronLandingSelector;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class NightHeronRoostGoal
extends Goal {
    private final NightHeronEntity nightHeron;

    public NightHeronRoostGoal(NightHeronEntity nightHeron) {
        this.nightHeron = nightHeron;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        return this.nightHeron.shouldRoost()
                && this.nightHeron.onGround()
                && this.nightHeron.getTarget() == null
                && !this.nightHeron.hasExternalFright()
                && !this.nightHeron.isRestInterrupted()
                && this.isNearRoostCover();
    }

    public boolean canContinueToUse() {
        return this.nightHeron.shouldRoost()
                && this.nightHeron.onGround()
                && !this.nightHeron.hasExternalFright()
                && !this.nightHeron.isRestInterrupted()
                && !this.nightHeron.getBehaviorState().isEscape();
    }

    public void start() {
        this.nightHeron.setBehaviorState(NightHeronBehaviorState.ROOSTING);
        this.nightHeron.getNavigation().stop();
    }

    public void stop() {
        if (!this.nightHeron.getBehaviorState().isEscape()) {
            this.nightHeron.setBehaviorState(this.nightHeron.isRestInterrupted()
                    ? NightHeronBehaviorState.ALERT_FREEZE
                    : NightHeronBehaviorState.IDLE);
        }
    }

    public void tick() {
        this.nightHeron.getNavigation().stop();
        Vec3 movement = this.nightHeron.getDeltaMovement();
        this.nightHeron.setDeltaMovement(0.0D, movement.y, 0.0D);
        this.nightHeron.setBehaviorState(NightHeronBehaviorState.ROOSTING);
    }

    private boolean isNearRoostCover() {
        return NightHeronLandingSelector.isRoostingSpot(this.nightHeron.level(), this.nightHeron.blockPosition()) || NightHeronLandingSelector.hasRoostCoverNear(this.nightHeron.level(), this.nightHeron.blockPosition(), 5);
    }
}
