package EdDYON.guaniao.content.bird.cassowary;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

final class CassowaryIdleGoal extends Goal {
    private static final int LOOK_TICKS = 350;
    private static final int PECK_TICKS = 25;

    private final CassowaryEntity cassowary;
    private int cooldown;
    private CassowaryBehaviorState action;

    CassowaryIdleGoal(CassowaryEntity cassowary) {
        this.cassowary = cassowary;
        this.cooldown = 400 + cassowary.getRandom().nextInt(601);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--this.cooldown > 0
                || !this.cassowary.isDayActivityTime()
                || this.cassowary.getCassowaryBehaviorState() != CassowaryBehaviorState.CALM
                || !this.cassowary.getNavigation().isDone()
                || this.cassowary.hasImmediateThreat()) {
            return false;
        }
        this.action = this.cassowary.getRandom().nextFloat() < 0.32F
                ? CassowaryBehaviorState.PECKING : CassowaryBehaviorState.LOOKING;
        return true;
    }

    @Override
    public void start() {
        this.cassowary.getNavigation().stop();
        this.cassowary.setCassowaryBehaviorState(this.action);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.cassowary.getCassowaryBehaviorState() != this.action
                || this.cassowary.hasImmediateThreat()) {
            return false;
        }
        return this.cassowary.getBehaviorTicks()
                < (this.action == CassowaryBehaviorState.LOOKING ? LOOK_TICKS : PECK_TICKS);
    }

    @Override
    public void stop() {
        if (this.cassowary.getCassowaryBehaviorState() == this.action) {
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.CALM);
        }
        this.cooldown = 400 + this.cassowary.getRandom().nextInt(601);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
