package EdDYON.guaniao.content.bird.budgerigar;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BudgerigarRoostGoal extends Goal {
    private final BudgerigarEntity budgerigar;
    private BlockPos roostPos;

    public BudgerigarRoostGoal(BudgerigarEntity budgerigar) {
        this.budgerigar = budgerigar;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.budgerigar.isRoostTime()
                || this.budgerigar.isRestInterrupted()
                || this.budgerigar.isEating()
                || this.budgerigar.isDancing()
                || this.budgerigar.getBehaviorState().isEscape()) {
            return false;
        }
        this.roostPos = this.findRoost();
        return this.roostPos != null || this.budgerigar.getNavigation().isDone();
    }

    @Override
    public boolean canContinueToUse() {
        return this.budgerigar.isRoostTime()
                && !this.budgerigar.isRestInterrupted()
                && !this.budgerigar.isEating()
                && !this.budgerigar.isDancing()
                && !this.budgerigar.getBehaviorState().isEscape();
    }

    @Override
    public void start() {
        this.budgerigar.setBehaviorState(BudgerigarBehaviorState.ROOSTING);
        if (this.roostPos != null) {
            this.moveToRoost();
        }
    }

    @Override
    public void tick() {
        if (this.roostPos == null) {
            this.budgerigar.getNavigation().stop();
            this.settleToSleep(60);
            this.budgerigar.birdBrain().onRest(0.02F);
            return;
        }
        double distanceSqr = this.budgerigar.distanceToSqr(this.roostPos.getX() + 0.5D, this.roostPos.getY(), this.roostPos.getZ() + 0.5D);
        if (distanceSqr > 2.25D) {
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.ROOSTING);
            if (distanceSqr > 49.0D && this.budgerigar.onGround()) {
                this.budgerigar.startShortFlight(Vec3.atCenterOf(this.roostPos), false);
            } else {
                this.moveToRoost();
            }
            return;
        }
        this.budgerigar.getNavigation().stop();
        this.settleToSleep(80);
        if (this.budgerigar.tickCount % 40 == 0) {
            this.budgerigar.birdBrain().onRest(0.03F);
        }
    }

    @Override
    public void stop() {
        this.roostPos = null;
        if (!this.budgerigar.isRoostTime() && this.budgerigar.isSleepingOrRoosting()) {
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.IDLE);
        }
    }

    private void moveToRoost() {
        this.budgerigar.getNavigation().moveTo(this.roostPos.getX() + 0.5D, this.roostPos.getY(), this.roostPos.getZ() + 0.5D, 0.88D);
    }

    private void settleToSleep(int introTicks) {
        if (this.budgerigar.getBehaviorState() != BudgerigarBehaviorState.SLEEPING) {
            this.budgerigar.setBehaviorStateFor(BudgerigarBehaviorState.SLEEPING, introTicks);
        } else {
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.SLEEPING);
        }
    }

    private BlockPos findRoost() {
        BlockPos origin = this.budgerigar.blockPosition();
        for (int attempts = 0; attempts < 36; ++attempts) {
            int x = origin.getX() + this.budgerigar.getRandom().nextInt(15) - 7;
            int y = origin.getY() + this.budgerigar.getRandom().nextInt(7) - 2;
            int z = origin.getZ() + this.budgerigar.getRandom().nextInt(15) - 7;
            BlockPos blockPos = new BlockPos(x, y, z);
            BlockState state = this.budgerigar.level().getBlockState(blockPos);
            BlockPos standPos = blockPos.above();
            if (this.isRoostBlock(state)
                    && this.budgerigar.level().getBlockState(standPos).isAir()
                    && this.budgerigar.level().getBlockState(standPos.above()).isAir()) {
                return standPos;
            }
        }
        return null;
    }

    private boolean isRoostBlock(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(Blocks.HAY_BLOCK)
                || state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof FenceGateBlock;
    }
}
