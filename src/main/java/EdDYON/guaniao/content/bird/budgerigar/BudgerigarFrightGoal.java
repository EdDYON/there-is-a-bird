package EdDYON.guaniao.content.bird.budgerigar;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class BudgerigarFrightGoal extends Goal {
    private final BudgerigarEntity budgerigar;
    private int moveCooldown;

    public BudgerigarFrightGoal(BudgerigarEntity budgerigar) {
        this.budgerigar = budgerigar;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.budgerigar.shouldFlee();
    }

    @Override
    public boolean canContinueToUse() {
        return this.budgerigar.shouldFlee() && this.budgerigar.getBehaviorState().isEscape();
    }

    @Override
    public void start() {
        this.moveCooldown = 0;
        if (this.budgerigar.frightSource() == null && this.budgerigar.birdBrain().senses().nearestPlayer() != null) {
            this.budgerigar.frightenFrom(this.budgerigar.birdBrain().senses().nearestPlayer().position(), 80);
        }
        this.budgerigar.setBehaviorStateFor(BudgerigarBehaviorState.FLEEING, 70);
    }

    @Override
    public void tick() {
        Vec3 source = this.budgerigar.frightSource();
        if (source == null) {
            return;
        }
        this.budgerigar.getLookControl().setLookAt(source.x, source.y + 0.8D, source.z, 30.0F, 30.0F);
        if (this.budgerigar.isFlightInProgress()) {
            return;
        }
        if (this.moveCooldown-- > 0) {
            return;
        }
        this.moveCooldown = 16 + this.budgerigar.getRandom().nextInt(16);
        Vec3 away = this.budgerigar.position().subtract(source);
        if (away.horizontalDistanceSqr() < 0.01D) {
            away = new Vec3(this.budgerigar.getRandom().nextDouble() - 0.5D, 0.0D, this.budgerigar.getRandom().nextDouble() - 0.5D);
        }
        away = new Vec3(away.x, 0.0D, away.z).normalize();
        Vec3 target = this.budgerigar.position().add(away.scale(5.0D + this.budgerigar.getRandom().nextDouble() * 3.0D));
        this.budgerigar.getNavigation().moveTo(target.x, target.y, target.z, 1.15D);
    }

    @Override
    public void stop() {
        if (this.budgerigar.getBehaviorState() == BudgerigarBehaviorState.FLEEING && !this.budgerigar.isFlying()) {
            this.budgerigar.setBehaviorStateFor(BudgerigarBehaviorState.ALERT, 35);
        }
    }
}
