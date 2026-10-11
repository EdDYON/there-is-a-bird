package EdDYON.guaniao.content.bird.budgerigar;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class BudgerigarIdleGoal extends Goal {
    private final BudgerigarEntity budgerigar;
    private int idleTicks;
    private Vec3 strollTarget;

    public BudgerigarIdleGoal(BudgerigarEntity budgerigar) {
        this.budgerigar = budgerigar;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.budgerigar.canStartSocialGoal() && this.budgerigar.getRandom().nextInt(24) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.idleTicks > 0
                && this.budgerigar.canStartSocialGoal()
                && (this.strollTarget == null || !this.budgerigar.getNavigation().isDone());
    }

    @Override
    public void start() {
        this.idleTicks = 45 + this.budgerigar.getRandom().nextInt(85);
        this.strollTarget = null;
        int roll = this.budgerigar.getRandom().nextInt(100);
        if (roll < 20) {
            this.budgerigar.getNavigation().stop();
            this.budgerigar.setBehaviorStateFor(BudgerigarBehaviorState.PREENING, this.idleTicks);
        } else if (roll < (this.budgerigar.trustTicks() > 700 ? 48 : 38)) {
            this.budgerigar.getNavigation().stop();
            this.budgerigar.setBehaviorStateFor(BudgerigarBehaviorState.CURIOUS, Math.min(this.idleTicks, 70));
        } else {
            this.strollTarget = DefaultRandomPos.getPos(this.budgerigar, 5, 2);
            if (this.strollTarget != null) {
                this.budgerigar.setBehaviorState(this.budgerigar.birdBrain().wantsForage() ? BudgerigarBehaviorState.FORAGING : BudgerigarBehaviorState.WALKING);
                this.budgerigar.getNavigation().moveTo(this.strollTarget.x, this.strollTarget.y, this.strollTarget.z, 0.72D);
            } else {
                this.budgerigar.setBehaviorState(BudgerigarBehaviorState.IDLE);
            }
        }
    }

    @Override
    public void tick() {
        --this.idleTicks;
        if (this.strollTarget == null && this.budgerigar.getRandom().nextInt(25) == 0) {
            this.budgerigar.getLookControl().setLookAt(
                    this.budgerigar.getX() + this.budgerigar.getRandom().nextDouble() * 4.0D - 2.0D,
                    this.budgerigar.getEyeY(),
                    this.budgerigar.getZ() + this.budgerigar.getRandom().nextDouble() * 4.0D - 2.0D,
                    25.0F,
                    25.0F);
        }
    }

    @Override
    public void stop() {
        this.strollTarget = null;
        if (!this.budgerigar.isEating() && !this.budgerigar.isDancing() && !this.budgerigar.isSleepingOrRoosting()) {
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.IDLE);
        }
    }
}
