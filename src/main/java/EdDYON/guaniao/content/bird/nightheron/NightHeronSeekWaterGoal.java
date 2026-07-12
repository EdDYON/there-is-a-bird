package EdDYON.guaniao.content.bird.nightheron;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NightHeronSeekWaterGoal extends Goal {
    private final NightHeronEntity nightHeron;
    private BlockPos shorelineTarget;
    private int remainingTicks;
    private int repathTicks;

    public NightHeronSeekWaterGoal(NightHeronEntity nightHeron) {
        this.nightHeron = nightHeron;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        NightHeronBehaviorState state = this.nightHeron.getBehaviorState();
        if (!this.nightHeron.isActiveTime()
                || !this.nightHeron.onGround()
                || this.nightHeron.isEatingFish()
                || state.isAirborne()
                || state.isEscape()
                || state == NightHeronBehaviorState.ROOSTING
                || this.nightHeron.getTarget() != null
                || this.nightHeron.hasExternalFright()
                || this.isAtShoreline()
                || this.nightHeron.getRandom().nextInt(NightHeronDefinition.WATER_SEEK_ACTIVE_CHANCE) != 0) {
            return false;
        }
        this.shorelineTarget = this.findShorelineTarget();
        return this.shorelineTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        NightHeronBehaviorState state = this.nightHeron.getBehaviorState();
        return this.remainingTicks > 0
                && this.shorelineTarget != null
                && this.nightHeron.onGround()
                && !this.nightHeron.isEatingFish()
                && !state.isAirborne()
                && !state.isEscape()
                && !this.nightHeron.hasExternalFright()
                && this.nightHeron.position().distanceToSqr(Vec3.atBottomCenterOf(this.shorelineTarget)) > 1.8D;
    }

    @Override
    public void start() {
        this.remainingTicks = 180;
        this.repathTicks = 0;
        this.nightHeron.setBehaviorState(NightHeronBehaviorState.MICRO_STROLL);
        this.moveToTarget();
    }

    @Override
    public void tick() {
        --this.remainingTicks;
        --this.repathTicks;
        if (this.shorelineTarget == null) {
            return;
        }
        Vec3 target = Vec3.atBottomCenterOf(this.shorelineTarget);
        this.nightHeron.getLookControl().setLookAt(target.x, target.y, target.z, 25.0F, 20.0F);
        if (this.repathTicks <= 0 || this.nightHeron.getNavigation().isDone()) {
            this.moveToTarget();
        }
    }

    @Override
    public void stop() {
        this.remainingTicks = 0;
        this.shorelineTarget = null;
        this.nightHeron.getNavigation().stop();
        if (!this.nightHeron.isEatingFish() && !this.nightHeron.getBehaviorState().isEscape()) {
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.IDLE);
        }
    }

    private void moveToTarget() {
        if (this.shorelineTarget == null) {
            return;
        }
        this.repathTicks = 25;
        this.nightHeron.getNavigation().moveTo(
                this.shorelineTarget.getX() + 0.5D,
                this.shorelineTarget.getY(),
                this.shorelineTarget.getZ() + 0.5D,
                NightHeronDefinition.WATER_SEEK_SPEED);
    }

    private boolean isAtShoreline() {
        BlockPos pos = this.nightHeron.blockPosition();
        return NightHeronEntity.isWaterEdge(this.nightHeron.level(), pos)
                || this.nightHeron.isNearWater(pos, 2);
    }

    private BlockPos findShorelineTarget() {
        Level level = this.nightHeron.level();
        BlockPos origin = this.nightHeron.blockPosition();
        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int radius = NightHeronDefinition.WATER_SEEK_SCAN_RADIUS;
        for (int xOffset = -radius; xOffset <= radius; ++xOffset) {
            for (int zOffset = -radius; zOffset <= radius; ++zOffset) {
                int horizontalDistanceSqr = xOffset * xOffset + zOffset * zOffset;
                if (horizontalDistanceSqr > radius * radius) {
                    continue;
                }
                for (int yOffset = -4; yOffset <= 4; ++yOffset) {
                    mutable.set(origin.getX() + xOffset, origin.getY() + yOffset, origin.getZ() + zOffset);
                    if (!this.isSafeShorelinePosition(level, mutable)) {
                        continue;
                    }
                    double score = -horizontalDistanceSqr - Math.abs(yOffset) * 3.0D;
                    BlockState below = level.getBlockState(mutable.below());
                    if (below.is(Blocks.MUD) || below.is(Blocks.CLAY) || below.is(Blocks.SAND) || below.is(Blocks.RED_SAND)) {
                        score += 10.0D;
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        best = mutable.immutable();
                    }
                }
            }
        }
        return best;
    }

    private boolean isSafeShorelinePosition(Level level, BlockPos pos) {
        if (!NightHeronEntity.canReadChunk(level, pos)
                || !NightHeronEntity.isWaterEdge(level, pos)
                || !level.getFluidState(pos).isEmpty()
                || !level.getFluidState(pos.above()).isEmpty()) {
            return false;
        }
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        if (!feet.getCollisionShape((BlockGetter)level, pos).isEmpty()
                || !head.getCollisionShape((BlockGetter)level, pos.above()).isEmpty()) {
            return false;
        }
        return below.isFaceSturdy((BlockGetter)level, pos.below(), Direction.UP)
                || below.is(Blocks.MUD)
                || below.is(Blocks.CLAY)
                || below.is(Blocks.SAND)
                || below.is(Blocks.RED_SAND);
    }
}
