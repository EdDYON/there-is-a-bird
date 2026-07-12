package EdDYON.guaniao.content.bird.budgerigar;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BudgerigarSentinelGoal extends Goal {
    private final BudgerigarEntity budgerigar;
    private BlockPos perchPos;
    private int sentinelTicks;

    public BudgerigarSentinelGoal(BudgerigarEntity budgerigar) {
        this.budgerigar = budgerigar;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.budgerigar.canStartSocialGoal() || this.budgerigar.getRandom().nextInt(160) != 0) {
            return false;
        }
        List<BudgerigarEntity> flock = this.nearbyFlock();
        if (flock.size() < BudgerigarDefinition.MIN_FLOCK_SIZE) {
            return false;
        }
        for (BudgerigarEntity other : flock) {
            if (other != this.budgerigar && other.getBehaviorState() == BudgerigarBehaviorState.SENTINEL) {
                return false;
            }
        }
        this.perchPos = this.findPerch();
        return this.perchPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.sentinelTicks > 0
                && this.budgerigar.canStartSocialGoal()
                && this.budgerigar.getBehaviorState() == BudgerigarBehaviorState.SENTINEL;
    }

    @Override
    public void start() {
        this.sentinelTicks = 140 + this.budgerigar.getRandom().nextInt(120);
        this.budgerigar.setBehaviorState(BudgerigarBehaviorState.SENTINEL);
        if (this.perchPos != null) {
            this.budgerigar.getNavigation().moveTo(this.perchPos.getX() + 0.5D, this.perchPos.getY(), this.perchPos.getZ() + 0.5D, 0.9D);
        }
    }

    @Override
    public void tick() {
        --this.sentinelTicks;
        if (this.perchPos != null && this.budgerigar.distanceToSqr(this.perchPos.getX() + 0.5D, this.perchPos.getY(), this.perchPos.getZ() + 0.5D) > 2.0D) {
            this.budgerigar.getNavigation().moveTo(this.perchPos.getX() + 0.5D, this.perchPos.getY(), this.perchPos.getZ() + 0.5D, 0.9D);
            return;
        }
        this.budgerigar.getNavigation().stop();
        this.budgerigar.setBehaviorState(BudgerigarBehaviorState.SENTINEL);
        if (this.budgerigar.tickCount % 20 == 0) {
            this.checkThreats();
        }
    }

    @Override
    public void stop() {
        this.perchPos = null;
        if (this.budgerigar.getBehaviorState() == BudgerigarBehaviorState.SENTINEL) {
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.IDLE);
        }
    }

    private List<BudgerigarEntity> nearbyFlock() {
        return this.budgerigar.level().getEntitiesOfClass(BudgerigarEntity.class, this.budgerigar.getBoundingBox().inflate(BudgerigarDefinition.SOCIAL_RADIUS));
    }

    private void checkThreats() {
        List<Monster> monsters = this.budgerigar.level().getEntitiesOfClass(Monster.class, this.budgerigar.getBoundingBox().inflate(8.0D), Monster::isAlive);
        if (!monsters.isEmpty()) {
            this.budgerigar.alertNearbyBudgerigars(monsters.get(0).position(), 80);
        }
    }

    private BlockPos findPerch() {
        BlockPos origin = this.budgerigar.blockPosition();
        for (int attempts = 0; attempts < 32; ++attempts) {
            int x = origin.getX() + this.budgerigar.getRandom().nextInt(11) - 5;
            int y = origin.getY() + this.budgerigar.getRandom().nextInt(5) - 1;
            int z = origin.getZ() + this.budgerigar.getRandom().nextInt(11) - 5;
            BlockPos blockPos = new BlockPos(x, y, z);
            BlockState state = this.budgerigar.level().getBlockState(blockPos);
            BlockPos standPos = blockPos.above();
            if (this.isPerchBlock(state)
                    && this.budgerigar.level().getBlockState(standPos).isAir()
                    && this.budgerigar.level().getBlockState(standPos.above()).isAir()) {
                return standPos;
            }
        }
        return null;
    }

    private boolean isPerchBlock(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(Blocks.HAY_BLOCK)
                || state.getBlock() instanceof FenceBlock;
    }
}
