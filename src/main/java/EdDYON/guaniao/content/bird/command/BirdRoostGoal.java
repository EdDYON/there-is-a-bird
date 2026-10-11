package EdDYON.guaniao.content.bird.command;

import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.BirdTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
        if (this.roostPos != null && !this.isValidRoost(this.roostPos)) {
            this.roostPos = null;
        }
        // An empty search still waits before scanning again.
        if (--this.rescanTicks <= 0) {
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
        if (!(this.bird.level() instanceof ServerLevel level)) {
            return;
        }
        if (!BirdScanBudget.tryAcquire(level, this.bird)) {
            // Spread denied searches across ticks instead of retrying every tick
            // or making all birds contend again on the same 80-tick phase.
            this.rescanTicks = 20 + Math.floorMod(this.bird.getId() + this.bird.tickCount, 20);
            return;
        }
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
        if (!(this.bird.level() instanceof ServerLevel level) || level.isOutsideBuildHeight(pos)
                || level.isOutsideBuildHeight(pos.above())) {
            return false;
        }
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockState(pos).is(BirdTags.BIRD_PERCHES)
                && chunk.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }
}
