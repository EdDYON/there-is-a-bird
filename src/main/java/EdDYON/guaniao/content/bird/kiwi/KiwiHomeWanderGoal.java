package EdDYON.guaniao.content.bird.kiwi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

final class KiwiHomeWanderGoal extends Goal {
    private final KiwiEntity kiwi;
    private Vec3 target;
    private int cooldown;
    private boolean returningHome;

    KiwiHomeWanderGoal(KiwiEntity kiwi) {
        this.kiwi = kiwi;
        this.cooldown = 30 + kiwi.getRandom().nextInt(70);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.kiwi.isActiveTime()
                || !this.kiwi.canStartCalmBehavior()
                || this.kiwi.isConflictActive()) {
            return false;
        }

        BlockPos home = this.kiwi.getHomeCenter();
        double homeDistance = home == null ? 0.0D : Math.sqrt(home.distSqr(this.kiwi.blockPosition()));
        if (homeDistance <= KiwiDefinition.MAX_TERRITORY_RADIUS && this.cooldown-- > 0) {
            return false;
        }

        this.returningHome = homeDistance > KiwiDefinition.NORMAL_TERRITORY_RADIUS;
        this.target = homeDistance > KiwiDefinition.MAX_TERRITORY_RADIUS
                ? KiwiHabitatUtil.findReturnTarget(this.kiwi)
                : KiwiHabitatUtil.findTerritoryWanderTarget(this.kiwi);
        if (this.target == null) {
            this.cooldown = 40 + this.kiwi.getRandom().nextInt(60);
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        if (this.returningHome) {
            this.kiwi.setBehaviorState(KiwiBehaviorState.RETURNING_HOME, 0);
        }
        this.kiwi.getNavigation().moveTo(this.target.x, this.target.y, this.target.z,
                this.returningHome ? 1.0D : KiwiDefinition.WANDER_SPEED);
    }

    @Override
    public boolean canContinueToUse() {
        return this.kiwi.isActiveTime()
                && !this.kiwi.isConflictActive()
                && this.kiwi.birdBrain().computeRiskScore() < 0.60F
                && !this.kiwi.getNavigation().isDone();
    }

    @Override
    public void stop() {
        if (this.kiwi.getBehaviorState() == KiwiBehaviorState.RETURNING_HOME) {
            this.kiwi.setBehaviorState(KiwiBehaviorState.AWAKE, 0);
        }
        this.target = null;
        this.cooldown = 45 + this.kiwi.getRandom().nextInt(100);
    }
}
