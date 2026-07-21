package EdDYON.guaniao.content.bird.budgerigar;

import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

public class BudgerigarMusicDanceGoal extends Goal {
    private final BudgerigarEntity budgerigar;

    public BudgerigarMusicDanceGoal(BudgerigarEntity budgerigar) {
        this.budgerigar = budgerigar;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.budgerigar.nearbyMusicTicks() > 0
                && (!this.budgerigar.isTame()
                || this.budgerigar.isBirdCommandMode(BirdCommandMode.FREE))
                && !this.budgerigar.isEating()
                && !this.budgerigar.getBehaviorState().isEscape();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.budgerigar.getNavigation().stop();
        this.budgerigar.setBehaviorStateFor(BudgerigarBehaviorState.DANCING, 60);
    }

    @Override
    public void tick() {
        this.budgerigar.getNavigation().stop();
        this.budgerigar.setBehaviorState(BudgerigarBehaviorState.DANCING);
        BlockPos source = this.budgerigar.musicSourcePos();
        if (source != null) {
            this.budgerigar.getLookControl().setLookAt(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        if (this.budgerigar.getBehaviorState() == BudgerigarBehaviorState.DANCING) {
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.IDLE);
        }
    }
}
