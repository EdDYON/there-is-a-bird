package EdDYON.guaniao.content.bird.nightheron;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class NightHeronSeekWaterGoal extends Goal {
    private final NightHeronEntity nightHeron;
    private BlockPos shorelineTarget;
    private int remainingTicks;
    private int repathTicks;
    private int searchCooldown;

    public NightHeronSeekWaterGoal(NightHeronEntity nightHeron) {
        this.nightHeron = nightHeron;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.searchCooldown > 0) {
            --this.searchCooldown;
            return false;
        }
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
        this.searchCooldown = 60 + this.nightHeron.getRandom().nextInt(41);
        this.shorelineTarget = NightHeronShorelineCache.find(this.nightHeron);
        if (this.shorelineTarget == null) {
            this.searchCooldown = 100 + this.nightHeron.getRandom().nextInt(81);
        }
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

}
