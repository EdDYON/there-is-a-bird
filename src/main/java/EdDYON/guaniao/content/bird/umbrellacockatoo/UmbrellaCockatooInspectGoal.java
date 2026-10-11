package EdDYON.guaniao.content.bird.umbrellacockatoo;

import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarBehaviorState;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * "What is that thing?" — the cockatoo walks over to furniture and studies it.
 *
 * <p>It never touches what it finds. There is no call to any block use, no
 * inventory access and no redstone interaction anywhere in this goal: the bird
 * navigates to the block, looks at it, cocks its head, and leaves. That restraint
 * is the feature, not an omission.</p>
 */
public class UmbrellaCockatooInspectGoal extends Goal {
    private static final int SCAN_RADIUS = 8;
    private static final double STAND_DISTANCE = 1.6D;
    private static final int MIN_STAY_TICKS = 40;
    private static final int MAX_STAY_TICKS = 120;
    private static final int MAX_APPROACH_TICKS = 260;

    private final UmbrellaCockatooEntity bird;
    @Nullable
    private BlockPos targetBlock;
    private int stayTicks;
    private int approachTicks;
    private int shiftTicks;

    public UmbrellaCockatooInspectGoal(UmbrellaCockatooEntity bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.bird.isCalmAndAwake() || !this.bird.isDayActive()) {
            return false;
        }
        if (!this.bird.isInspectCooldownReady()) {
            return false;
        }
        BlockPos found = this.findInterestingBlock();
        if (found == null) {
            // Nothing worth a look around here: back off for a while.
            this.bird.markInspect(120 + this.bird.getRandom().nextInt(180));
            return false;
        }
        this.targetBlock = found;
        this.approachTicks = MAX_APPROACH_TICKS;
        this.stayTicks = 0;
        this.shiftTicks = 0;
        this.bird.feel(UmbrellaCockatooEmotion.CURIOUS, 140);
        BudgerigarControl.setBehaviorStateFor(this.bird, BudgerigarBehaviorState.CURIOUS, 140);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetBlock == null || !this.bird.isCalmAndAwake()) {
            return false;
        }
        if (this.approachTicks <= 0) {
            return false;
        }
        return this.isStillInteresting(this.targetBlock);
    }

    @Override
    public void start() {
        this.moveTowardTarget();
    }

    @Override
    public void tick() {
        if (this.targetBlock == null) {
            return;
        }
        --this.approachTicks;
        Vec3 centre = Vec3.atCenterOf(this.targetBlock);
        this.bird.getLookControl().setLookAt(centre.x, centre.y, centre.z, 35.0F, 35.0F);
        boolean arrived = this.bird.distanceToSqr(centre) <= (STAND_DISTANCE + 0.8D) * (STAND_DISTANCE + 0.8D);
        if (!arrived) {
            if (this.bird.getNavigation().isDone()) {
                this.moveTowardTarget();
            }
            return;
        }
        this.bird.getNavigation().stop();
        if (this.stayTicks == 0) {
            this.stayTicks = MIN_STAY_TICKS
                    + this.bird.getRandom().nextInt(MAX_STAY_TICKS - MIN_STAY_TICKS + 1);
            this.bird.beginHeadTilt();
        }
        if (--this.stayTicks <= 0) {
            this.approachTicks = 0;
            return;
        }
        // Studying involves the occasional shuffle around the object, never a use of it.
        if (this.shiftTicks <= 0 && this.bird.getRandom().nextInt(70) == 0) {
            this.shiftTicks = 30;
            this.moveTowardTarget();
        }
        if (this.shiftTicks > 0) {
            --this.shiftTicks;
        }
        if (this.bird.getRandom().nextInt(80) == 0) {
            this.bird.beginHeadTilt();
        }
    }

    @Override
    public void stop() {
        this.bird.getNavigation().stop();
        this.targetBlock = null;
        // Long cooldown afterwards so the bird does not chain-inspect every barrel in a base.
        this.bird.markInspect(240 + this.bird.getRandom().nextInt(360));
    }

    private void moveTowardTarget() {
        if (this.targetBlock == null) {
            return;
        }
        Vec3 stand = this.findStandPosition(this.targetBlock);
        if (stand == null) {
            this.approachTicks = 0;
            return;
        }
        // A short hop across a gap, a walk if it is close: both are already supported.
        if (this.bird.distanceToSqr(stand) > 36.0D && !this.bird.isFlying()) {
            this.bird.startFlybyFlight(stand);
            return;
        }
        this.bird.getNavigation().moveTo(stand.x, stand.y, stand.z, 0.62D);
    }

    @Nullable
    private Vec3 findStandPosition(BlockPos block) {
        Vec3 birdPos = this.bird.position();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = block.relative(side);
            BlockState floor = this.bird.level().getBlockState(candidate.below());
            if (!floor.isSolid()) {
                continue;
            }
            if (!this.bird.level().getBlockState(candidate).getCollisionShape(this.bird.level(), candidate).isEmpty()) {
                continue;
            }
            if (!this.bird.level().getBlockState(candidate.above()).getCollisionShape(this.bird.level(), candidate.above()).isEmpty()) {
                continue;
            }
            Vec3 stand = Vec3.atBottomCenterOf(candidate);
            if (stand.distanceToSqr(birdPos) < 6.0D) {
                continue;
            }
            return stand;
        }
        return null;
    }

    @Nullable
    private BlockPos findInterestingBlock() {
        if (!(this.bird.level() instanceof ServerLevel serverLevel)
                || !BirdScanBudget.tryAcquire(serverLevel, this.bird, 3)) {
            return null;
        }
        BlockPos origin = this.bird.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-SCAN_RADIUS, -2, -SCAN_RADIUS),
                origin.offset(SCAN_RADIUS, 2, SCAN_RADIUS))) {
            if (!this.isStillInteresting(pos)) {
                continue;
            }
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    private boolean isStillInteresting(BlockPos pos) {
        // Reachability is handled by the approach step: if the bird cannot get
        // there the goal simply times out. Only the data-driven tag decides what
        // counts as interesting, so datapacks can extend the list.
        return this.bird.level().getBlockState(pos).is(BirdTags.UMBRELLA_COCKATOO_INTERESTING_BLOCKS);
    }
}
