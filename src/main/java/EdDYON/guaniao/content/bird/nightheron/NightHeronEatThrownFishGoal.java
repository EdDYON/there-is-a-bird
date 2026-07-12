package EdDYON.guaniao.content.bird.nightheron;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

public class NightHeronEatThrownFishGoal extends Goal {
    private final NightHeronEntity nightHeron;
    private ItemEntity targetFish;
    private int eatDelayTicks;

    public NightHeronEatThrownFishGoal(NightHeronEntity nightHeron) {
        this.nightHeron = nightHeron;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.nightHeron.canEatThrownFish()) {
            return false;
        }
        this.targetFish = this.findNearestFish();
        return this.targetFish != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.nightHeron.isEatingFish()) {
            return true;
        }
        return this.targetFish != null
                && this.targetFish.isAlive()
                && NightHeronEntity.isDroppedFishCandidate(this.targetFish.getItem())
                && this.nightHeron.canEatThrownFish()
                && this.nightHeron.distanceToSqr((Entity)this.targetFish) < 324.0;
    }

    @Override
    public void start() {
        this.eatDelayTicks = 0;
        if (!this.nightHeron.getBehaviorState().isEscape()) {
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.FORAGING);
        }
    }

    @Override
    public void tick() {
        if (this.nightHeron.isEatingFish()) {
            this.nightHeron.getNavigation().stop();
            return;
        }
        if (this.targetFish == null || !this.targetFish.isAlive() || !NightHeronEntity.isDroppedFishCandidate(this.targetFish.getItem())) {
            this.targetFish = this.findNearestFish();
            this.eatDelayTicks = 0;
            if (this.targetFish == null) {
                return;
            }
        }
        this.nightHeron.getLookControl().setLookAt((Entity)this.targetFish, 30.0f, 30.0f);
        double distanceSqr = this.nightHeron.distanceToSqr((Entity)this.targetFish);
        if (distanceSqr > 2.56) {
            this.eatDelayTicks = 0;
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.FORAGING);
            this.nightHeron.getNavigation().moveTo((Entity)this.targetFish, 1.0);
            return;
        }
        this.nightHeron.getNavigation().stop();
        if (++this.eatDelayTicks >= 6) {
            this.nightHeron.eatThrownFish(this.targetFish);
            this.targetFish = null;
        }
    }

    @Override
    public void stop() {
        this.targetFish = null;
        this.eatDelayTicks = 0;
        if (!this.nightHeron.isEatingFish() && this.nightHeron.getBehaviorState() == NightHeronBehaviorState.FORAGING) {
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.IDLE);
        }
    }

    private ItemEntity findNearestFish() {
        return this.nightHeron.level()
                .getEntitiesOfClass(
                        ItemEntity.class,
                        this.nightHeron.getBoundingBox().inflate(12.0, 3.0, 12.0),
                        itemEntity -> itemEntity.isAlive() && NightHeronEntity.isDroppedFishCandidate(itemEntity.getItem()))
                .stream()
                .min(Comparator.comparingDouble(itemEntity -> this.nightHeron.distanceToSqr((Entity)itemEntity)))
                .orElse(null);
    }
}
