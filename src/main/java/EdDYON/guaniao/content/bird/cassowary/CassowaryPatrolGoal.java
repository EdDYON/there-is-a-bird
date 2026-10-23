package EdDYON.guaniao.content.bird.cassowary;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class CassowaryPatrolGoal extends Goal {
    private final CassowaryEntity cassowary;
    @Nullable private Vec3 target;
    private int cooldown;
    private int failedPathCooldown;

    CassowaryPatrolGoal(CassowaryEntity cassowary) {
        this.cassowary = cassowary;
        this.cooldown = 60 + cassowary.getRandom().nextInt(140);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.failedPathCooldown-- > 0) return false;
        boolean returningHome = this.cassowary.isOutsideTerritoryLimit();
        if (!this.cassowary.isDayActivityTime()
                || this.cassowary.getCassowaryBehaviorState() != CassowaryBehaviorState.CALM
                || this.cassowary.hasImmediateThreat()
                || (!returningHome && --this.cooldown > 0)) {
            return false;
        }
        this.target = this.cassowary.findPatrolPosition();
        if (this.target == null) {
            this.failedPathCooldown = 20;
            this.cooldown = 60 + this.cassowary.getRandom().nextInt(100);
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        if (this.target != null) {
            if (!this.cassowary.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, 0.58D)) {
                this.failedPathCooldown = 20;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.cassowary.isDayActivityTime()
                && this.cassowary.getCassowaryBehaviorState() == CassowaryBehaviorState.CALM
                && !this.cassowary.hasImmediateThreat()
                && !this.cassowary.getNavigation().isDone();
    }

    @Override
    public void stop() {
        this.target = null;
        this.cooldown = 100 + this.cassowary.getRandom().nextInt(260);
    }
}
