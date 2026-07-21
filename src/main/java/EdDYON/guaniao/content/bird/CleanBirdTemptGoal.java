package EdDYON.guaniao.content.bird;

import EdDYON.guaniao.config.BirdConfigManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class CleanBirdTemptGoal extends Goal {
    private static final TargetingConditions TEMPT_TARGETING = TargetingConditions.forNonCombat().range(10.0D).ignoreLineOfSight();

    private final PathfinderMob mob;
    private final double speedModifier;
    private final Ingredient items;
    private final boolean canScare;
    private final TargetingConditions targetingConditions;
    @Nullable
    private Player player;
    private int calmDown;
    private double playerX;
    private double playerY;
    private double playerZ;
    private double playerRotX;
    private double playerRotY;
    private boolean running;

    public CleanBirdTemptGoal(PathfinderMob mob, double speedModifier, Ingredient items, boolean canScare) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.items = items;
        this.canScare = canScare;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.targetingConditions = TEMPT_TARGETING.copy().selector(this::shouldFollow);
    }

    @Override
    public boolean canUse() {
        if (this.calmDown > 0) {
            --this.calmDown;
            return false;
        }
        this.player = this.mob.level().getNearestPlayer(this.targetingConditions, this.mob);
        return this.player != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.player == null) {
            return false;
        }
        if (this.canScare && !BirdConfigManager.aprilFoolsMode() && this.mob.distanceToSqr(this.player) < 36.0D) {
            if (this.player.distanceToSqr(this.playerX, this.playerY, this.playerZ) > 0.01D) {
                return false;
            }
            if (Math.abs(this.player.getXRot() - this.playerRotX) > 5.0D || Math.abs(this.player.getYRot() - this.playerRotY) > 5.0D) {
                return false;
            }
        } else {
            this.rememberPlayerPose();
        }
        return this.canUse();
    }

    @Override
    public void start() {
        if (this.player != null) {
            this.rememberPlayerPose();
        }
        this.running = true;
    }

    @Override
    public void stop() {
        this.player = null;
        this.mob.getNavigation().stop();
        this.calmDown = reducedTickDelay(100);
        this.running = false;
    }

    @Override
    public void tick() {
        if (this.player == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(this.player, this.mob.getMaxHeadYRot() + 20.0F, this.mob.getMaxHeadXRot());
        if (this.mob.distanceToSqr(this.player) < 6.25D) {
            this.mob.getNavigation().stop();
        } else {
            this.mob.getNavigation().moveTo(this.player, this.speedModifier);
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    private boolean shouldFollow(LivingEntity entity) {
        return BirdFoodSafety.matchesClean(this.items, entity.getMainHandItem())
                || BirdFoodSafety.matchesClean(this.items, entity.getOffhandItem());
    }

    private void rememberPlayerPose() {
        if (this.player == null) {
            return;
        }
        this.playerX = this.player.getX();
        this.playerY = this.player.getY();
        this.playerZ = this.player.getZ();
        this.playerRotX = this.player.getXRot();
        this.playerRotY = this.player.getYRot();
    }
}
