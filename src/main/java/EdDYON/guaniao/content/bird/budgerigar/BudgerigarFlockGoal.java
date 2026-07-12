package EdDYON.guaniao.content.bird.budgerigar;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class BudgerigarFlockGoal extends Goal {
    private final BudgerigarEntity budgerigar;
    private Vec3 target;
    private int moveTicks;

    public BudgerigarFlockGoal(BudgerigarEntity budgerigar) {
        this.budgerigar = budgerigar;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.budgerigar.canStartSocialGoal() || this.budgerigar.getRandom().nextInt(28) != 0) {
            return false;
        }
        List<BudgerigarEntity> flock = this.nearbyFlock();
        if (flock.size() <= 1) {
            return false;
        }
        Vec3 center = Vec3.ZERO;
        BudgerigarEntity tooClose = null;
        int count = 0;
        for (BudgerigarEntity other : flock) {
            if (other == this.budgerigar) {
                continue;
            }
            center = center.add(other.position());
            ++count;
            if (this.budgerigar.distanceToSqr(other) < 1.8D) {
                tooClose = other;
            }
        }
        if (count == 0) {
            return false;
        }
        center = center.scale(1.0D / count);
        if (tooClose != null) {
            Vec3 away = this.budgerigar.position().subtract(tooClose.position());
            if (away.horizontalDistanceSqr() < 0.01D) {
                away = new Vec3(this.budgerigar.getRandom().nextDouble() - 0.5D, 0.0D, this.budgerigar.getRandom().nextDouble() - 0.5D);
            }
            away = new Vec3(away.x, 0.0D, away.z).normalize();
            this.target = this.budgerigar.position().add(away.scale(2.2D));
            return true;
        }
        if (this.budgerigar.position().distanceToSqr(center) > 48.0D) {
            this.target = center;
            return true;
        }
        if (this.budgerigar.birdBrain().wantsForage()) {
            this.target = this.budgerigar.position().add(this.budgerigar.getRandom().nextDouble() * 4.0D - 2.0D, 0.0D, this.budgerigar.getRandom().nextDouble() * 4.0D - 2.0D);
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.moveTicks > 0 && this.target != null && this.budgerigar.canStartSocialGoal() && !this.budgerigar.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.moveTicks = 40 + this.budgerigar.getRandom().nextInt(35);
        this.budgerigar.setBehaviorState(this.budgerigar.birdBrain().wantsForage() ? BudgerigarBehaviorState.FORAGING : BudgerigarBehaviorState.WALKING);
        this.budgerigar.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, 0.82D);
    }

    @Override
    public void tick() {
        --this.moveTicks;
        if (this.target != null && this.budgerigar.tickCount % 16 == 0) {
            this.budgerigar.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, 0.82D);
        }
    }

    @Override
    public void stop() {
        this.target = null;
        if (this.budgerigar.getBehaviorState() == BudgerigarBehaviorState.WALKING || this.budgerigar.getBehaviorState() == BudgerigarBehaviorState.FORAGING) {
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.IDLE);
        }
    }

    private List<BudgerigarEntity> nearbyFlock() {
        return this.budgerigar.level().getEntitiesOfClass(BudgerigarEntity.class, this.budgerigar.getBoundingBox().inflate(BudgerigarDefinition.SOCIAL_RADIUS));
    }
}
