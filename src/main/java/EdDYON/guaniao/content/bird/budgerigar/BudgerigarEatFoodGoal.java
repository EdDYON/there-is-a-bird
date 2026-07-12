package EdDYON.guaniao.content.bird.budgerigar;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

public class BudgerigarEatFoodGoal extends Goal {
    private final BudgerigarEntity budgerigar;
    private ItemEntity targetFood;
    private int eatDelayTicks;
    private int scanCooldown;

    public BudgerigarEatFoodGoal(BudgerigarEntity budgerigar) {
        this.budgerigar = budgerigar;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.budgerigar.canStartFoodGoal()) {
            return false;
        }
        if (this.scanCooldown-- > 0) {
            return false;
        }
        this.scanCooldown = 10 + this.budgerigar.getRandom().nextInt(20);
        this.targetFood = this.findNearestFood();
        return this.targetFood != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetFood != null
                && this.targetFood.isAlive()
                && BudgerigarEntity.isEdibleFood(this.targetFood.getItem())
                && this.budgerigar.canStartFoodGoal()
                && this.budgerigar.distanceToSqr((Entity)this.targetFood) < 196.0D;
    }

    @Override
    public void start() {
        this.eatDelayTicks = 0;
        this.budgerigar.setBehaviorState(BudgerigarBehaviorState.FORAGING);
        if (this.targetFood != null) {
            this.budgerigar.getNavigation().moveTo((Entity)this.targetFood, 0.92D);
        }
    }

    @Override
    public void tick() {
        if (this.targetFood == null || !this.targetFood.isAlive() || !BudgerigarEntity.isEdibleFood(this.targetFood.getItem())) {
            this.targetFood = this.findNearestFood();
            this.eatDelayTicks = 0;
            if (this.targetFood == null) {
                return;
            }
        }
        this.budgerigar.getLookControl().setLookAt((Entity)this.targetFood, 30.0F, 30.0F);
        double distanceSqr = this.budgerigar.distanceToSqr((Entity)this.targetFood);
        if (distanceSqr > 2.25D) {
            this.eatDelayTicks = 0;
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.FORAGING);
            this.budgerigar.getNavigation().moveTo((Entity)this.targetFood, 0.92D);
            return;
        }
        this.budgerigar.getNavigation().stop();
        this.budgerigar.setBehaviorState(BudgerigarBehaviorState.FORAGING);
        if (++this.eatDelayTicks >= 5) {
            this.budgerigar.consumeItemEntity(this.targetFood);
            this.targetFood = null;
        }
    }

    @Override
    public void stop() {
        this.targetFood = null;
        this.eatDelayTicks = 0;
        if (!this.budgerigar.isEating() && this.budgerigar.getBehaviorState() == BudgerigarBehaviorState.FORAGING) {
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.IDLE);
        }
    }

    private ItemEntity findNearestFood() {
        return this.budgerigar.level()
                .getEntitiesOfClass(
                        ItemEntity.class,
                        this.budgerigar.getBoundingBox().inflate(10.0D, 3.0D, 10.0D),
                        itemEntity -> itemEntity.isAlive() && BudgerigarEntity.isEdibleFood(itemEntity.getItem()))
                .stream()
                .min(Comparator.comparingDouble(itemEntity -> this.budgerigar.distanceToSqr((Entity)itemEntity)))
                .orElse(null);
    }
}
