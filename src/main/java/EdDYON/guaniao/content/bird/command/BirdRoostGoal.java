package EdDYON.guaniao.content.bird.command;

import EdDYON.guaniao.content.bird.BirdTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class BirdRoostGoal<T extends TamableAnimal & CommandableBird> extends Goal {
    private final T bird;
    private BlockPos roostPos;
    private int rescanTicks;

    public BirdRoostGoal(T bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.bird.isTame()
                && this.bird.isBirdCommandMode(BirdCommandMode.ROOST)
                && !this.bird.isBirdEmergencyOverrideActive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.rescanTicks = 0;
        this.findRoost();
    }

    @Override
    public void tick() {
        if (this.roostPos == null || --this.rescanTicks <= 0 || !this.isValidRoost(this.roostPos)) {
            this.findRoost();
        }
        if (this.roostPos != null) {
            BlockPos destination = this.roostPos.above();
            if (this.bird.distanceToSqr(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D) > 1.0D) {
                this.bird.getNavigation().moveTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D, 1.0D);
            } else {
                this.bird.getNavigation().stop();
            }
        } else {
            this.bird.getNavigation().stop();
        }
    }

    private void findRoost() {
        this.rescanTicks = 80;
        BlockPos origin = this.bird.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-8, -4, -8), origin.offset(8, 6, 8))) {
            if (!this.isValidRoost(candidate)) {
                continue;
            }
            double distance = candidate.distSqr(origin);
            if (distance < bestDistance) {
                best = candidate.immutable();
                bestDistance = distance;
            }
        }
        this.roostPos = best;
    }

    private boolean isValidRoost(BlockPos pos) {
        return this.bird.level().getBlockState(pos).is(BirdTags.BIRD_PERCHES)
                && this.bird.level().getBlockState(pos.above()).getCollisionShape(this.bird.level(), pos.above()).isEmpty();
    }
}
