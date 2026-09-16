package EdDYON.guaniao.content.bird.cassowary;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

final class CassowaryForageGoal extends Goal {
    private final CassowaryEntity cassowary;
    private int scanCooldown;
    private int travelTicks;
    private int repathTicks;
    private int consumeTick;

    CassowaryForageGoal(CassowaryEntity cassowary) {
        this.cassowary = cassowary;
        this.scanCooldown = cassowary.getRandom().nextInt(20);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cassowary.getCassowaryBehaviorState() == CassowaryBehaviorState.EATING) {
            return true;
        }
        if (--this.scanCooldown > 0
                || !this.cassowary.isDayActivityTime()
                || this.cassowary.getCassowaryBehaviorState() != CassowaryBehaviorState.CALM
                || this.cassowary.isOutsideTerritoryLimit()
                || this.cassowary.hasImmediateThreat()) {
            return false;
        }
        this.scanCooldown = this.cassowary.foodScanInterval();
        return this.cassowary.findFoodTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        CassowaryBehaviorState state = this.cassowary.getCassowaryBehaviorState();
        if (state == CassowaryBehaviorState.EATING) {
            return this.cassowary.getBehaviorTicks() <= CassowaryEntity.EATING_TICKS;
        }
        ItemEntity target = this.cassowary.getFoodTarget();
        return state == CassowaryBehaviorState.FORAGING
                && target != null && target.isAlive()
                && this.travelTicks < 240
                && !this.cassowary.isOutsideTerritoryLimit()
                && !this.cassowary.hasImmediateThreat();
    }

    @Override
    public void start() {
        this.travelTicks = 0;
        this.repathTicks = 0;
        this.consumeTick = this.cassowary.randomBetween(25, 40);
        if (this.cassowary.getCassowaryBehaviorState() != CassowaryBehaviorState.EATING) {
            this.cassowary.setFoodConsumed(false);
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.FORAGING);
        }
    }

    @Override
    public void tick() {
        if (this.cassowary.getCassowaryBehaviorState() == CassowaryBehaviorState.EATING) {
            this.cassowary.getNavigation().stop();
            if (!this.cassowary.isFoodConsumed() && this.cassowary.getBehaviorTicks() >= this.consumeTick) {
                this.cassowary.consumeFoodTarget();
            }
            return;
        }

        ItemEntity target = this.cassowary.getFoodTarget();
        if (target == null) {
            return;
        }
        ++this.travelTicks;
        this.cassowary.getLookControl().setLookAt(target, 18.0F, this.cassowary.getMaxHeadXRot());
        if (this.cassowary.distanceToSqr(target) <= 1.3D * 1.3D) {
            this.cassowary.getNavigation().stop();
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.EATING);
            return;
        }
        if (--this.repathTicks <= 0 || this.cassowary.getNavigation().isDone()) {
            this.cassowary.getNavigation().moveTo(target, 0.72D);
            this.repathTicks = 18 + this.cassowary.getRandom().nextInt(7);
        }
    }

    @Override
    public void stop() {
        this.cassowary.getNavigation().stop();
        if (this.cassowary.getCassowaryBehaviorState() == CassowaryBehaviorState.FORAGING
                || this.cassowary.getCassowaryBehaviorState() == CassowaryBehaviorState.EATING) {
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.CALM);
        }
        this.cassowary.clearFoodTarget();
        this.scanCooldown = this.cassowary.foodScanInterval();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
