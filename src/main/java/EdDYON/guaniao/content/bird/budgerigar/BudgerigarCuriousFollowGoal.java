package EdDYON.guaniao.content.bird.budgerigar;

import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class BudgerigarCuriousFollowGoal extends Goal {
    private final BudgerigarEntity budgerigar;
    private Player targetPlayer;
    private int repathTicks;

    public BudgerigarCuriousFollowGoal(BudgerigarEntity budgerigar) {
        this.budgerigar = budgerigar;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.budgerigar.canStartSocialGoal()) {
            return false;
        }
        Player player = this.budgerigar.level().getNearestPlayer(this.budgerigar, 12.0D);
        if (player == null || player.isSpectator()) {
            return false;
        }
        boolean holdingFood = this.budgerigar.isEdibleFoodForThisBird(player.getMainHandItem())
                || this.budgerigar.isEdibleFoodForThisBird(player.getOffhandItem());
        if (!holdingFood && this.budgerigar.trustTicks() < 650) {
            return false;
        }
        this.targetPlayer = player;
        return this.budgerigar.distanceToSqr((Entity)player) > 4.0D || this.budgerigar.trustTicks() > 1000;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetPlayer != null
                && this.targetPlayer.isAlive()
                && !this.targetPlayer.isSpectator()
                && this.budgerigar.canStartSocialGoal()
                && this.budgerigar.distanceToSqr((Entity)this.targetPlayer) < 196.0D;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
        this.budgerigar.setBehaviorState(BudgerigarBehaviorState.CURIOUS);
    }

    @Override
    public void tick() {
        this.budgerigar.getLookControl().setLookAt((Entity)this.targetPlayer, 30.0F, 30.0F);
        double distanceSqr = this.budgerigar.distanceToSqr((Entity)this.targetPlayer);
        if (distanceSqr < 5.0D) {
            this.budgerigar.getNavigation().stop();
            this.budgerigar.setBehaviorStateFor(BudgerigarBehaviorState.CURIOUS, 30);
            return;
        }
        if (this.repathTicks-- <= 0) {
            this.repathTicks = 12 + this.budgerigar.getRandom().nextInt(12);
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.FOLLOWING);
            this.budgerigar.getNavigation().moveTo((Entity)this.targetPlayer, 0.78D);
        }
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        if (this.budgerigar.getBehaviorState() == BudgerigarBehaviorState.FOLLOWING || this.budgerigar.getBehaviorState() == BudgerigarBehaviorState.CURIOUS) {
            this.budgerigar.setBehaviorState(BudgerigarBehaviorState.IDLE);
        }
    }
}
