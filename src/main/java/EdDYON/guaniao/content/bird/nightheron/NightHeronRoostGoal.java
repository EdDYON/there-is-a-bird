package EdDYON.guaniao.content.bird.nightheron;

import java.util.EnumSet;
import EdDYON.guaniao.content.bird.nightheron.NightHeronBehaviorState;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronLandingSelector;
import net.minecraft.world.entity.ai.goal.Goal;

public class NightHeronRoostGoal
extends Goal {
    private final NightHeronEntity nightHeron;
    private int remainingTicks;

    public NightHeronRoostGoal(NightHeronEntity nightHeron) {
        this.nightHeron = nightHeron;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        return this.nightHeron.shouldRoost()
                && this.nightHeron.onGround()
                && this.nightHeron.getTarget() == null
                && !this.nightHeron.hasExternalFright()
                && this.isNearRoostCover()
                && this.nightHeron.getRandom().nextInt(24) == 0;
    }

    public boolean canContinueToUse() {
        return this.remainingTicks > 0
                && this.nightHeron.shouldRoost()
                && this.nightHeron.onGround()
                && !this.nightHeron.hasExternalFright()
                && !this.nightHeron.getBehaviorState().isEscape();
    }

    public void start() {
        this.remainingTicks = 420 + this.nightHeron.getRandom().nextInt(700);
        this.nightHeron.setBehaviorState(NightHeronBehaviorState.ROOSTING);
        this.nightHeron.getNavigation().stop();
    }

    public void stop() {
        this.remainingTicks = 0;
        if (!this.nightHeron.getBehaviorState().isEscape()) {
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.IDLE);
        }
    }

    public void tick() {
        --this.remainingTicks;
        this.nightHeron.getNavigation().stop();
        this.nightHeron.setBehaviorState(NightHeronBehaviorState.ROOSTING);
    }

    private boolean isNearRoostCover() {
        return NightHeronLandingSelector.isRoostingSpot(this.nightHeron.level(), this.nightHeron.blockPosition()) || NightHeronLandingSelector.hasRoostCoverNear(this.nightHeron.level(), this.nightHeron.blockPosition(), 5);
    }
}
