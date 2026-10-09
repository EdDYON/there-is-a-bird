package EdDYON.guaniao.content.bird.kiwi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

final class KiwiReturnHomeAfterFightGoal extends Goal {
    private final KiwiEntity kiwi;
    private Vec3 target;

    KiwiReturnHomeAfterFightGoal(KiwiEntity kiwi) {
        this.kiwi = kiwi;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.kiwi.shouldReturnHomeAfterConflict() || this.kiwi.isConflictActive()) {
            return false;
        }
        BlockPos home = this.kiwi.getHomeCenter();
        if (home == null || this.isInsideCoreTerritory(home)) {
            this.kiwi.finishReturnHomeAfterConflict();
            return false;
        }
        this.target = KiwiHabitatUtil.findReturnTarget(this.kiwi);
        if (this.target == null) {
            this.target = Vec3.atBottomCenterOf(home);
        }
        return true;
    }

    @Override
    public void start() {
        this.kiwi.setBehaviorState(KiwiBehaviorState.RETURNING_HOME, 0);
        this.moveToTarget();
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos home = this.kiwi.getHomeCenter();
        return this.kiwi.shouldReturnHomeAfterConflict()
                && !this.kiwi.isConflictActive()
                && home != null
                && !this.isInsideCoreTerritory(home);
    }

    @Override
    public void tick() {
        BlockPos home = this.kiwi.getHomeCenter();
        if (home == null || this.isInsideCoreTerritory(home)) {
            this.kiwi.getNavigation().stop();
            this.kiwi.finishReturnHomeAfterConflict();
            return;
        }
        if (this.kiwi.getNavigation().isDone() || this.kiwi.tickCount % 20 == 0) {
            this.target = KiwiHabitatUtil.findReturnTarget(this.kiwi);
            if (this.target == null) {
                this.target = Vec3.atBottomCenterOf(home);
            }
            this.moveToTarget();
        }
    }

    @Override
    public void stop() {
        BlockPos home = this.kiwi.getHomeCenter();
        if (home == null || this.isInsideCoreTerritory(home)) {
            this.kiwi.finishReturnHomeAfterConflict();
        }
        this.target = null;
    }

    private boolean isInsideCoreTerritory(BlockPos home) {
        return home.distSqr(this.kiwi.blockPosition())
                <= (double) KiwiDefinition.CORE_TERRITORY_RADIUS * KiwiDefinition.CORE_TERRITORY_RADIUS;
    }

    private void moveToTarget() {
        if (this.target != null) {
            this.kiwi.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, 1.05D);
        }
    }
}
