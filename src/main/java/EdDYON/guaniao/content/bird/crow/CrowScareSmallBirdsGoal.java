package EdDYON.guaniao.content.bird.crow;

import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Crow approaches and scares small birds (sparrows, long-tailed tits, budgerigars)
 * within a short radius, triggering their escape flight.
 */
public class CrowScareSmallBirdsGoal extends Goal {
    private static final double SCARE_RADIUS = 8.0D;
    private static final double APPROACH_RADIUS = 2.5D;
    private static final int SCARE_COOLDOWN = 100;

    private final CrowEntity crow;
    @Nullable
    private PathfinderMob target;
    private int cooldown;
    private int recalcTicks;

    public CrowScareSmallBirdsGoal(CrowEntity crow) {
        this.crow = crow;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            --this.cooldown;
            return false;
        }
        if (this.crow.isFlying() || this.crow.getRandom().nextInt(60) != 0) {
            return false;
        }
        this.target = this.findNearestSmallBird();
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null || !this.target.isAlive()) {
            return false;
        }
        double distSqr = this.crow.distanceToSqr(this.target);
        return distSqr <= SCARE_RADIUS * SCARE_RADIUS + 4.0D;
    }

    @Override
    public void start() {
        this.recalcTicks = 0;
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        this.crow.getLookControl().setLookAt(this.target, 12.0F, 12.0F);
        double distSqr = this.crow.distanceToSqr(this.target);
        if (distSqr <= APPROACH_RADIUS * APPROACH_RADIUS) {
            this.scareTarget();
            this.target = null;
            return;
        }
        if (--this.recalcTicks <= 0) {
            this.recalcTicks = 15;
            this.crow.getNavigation().moveTo(this.target, 1.0D);
        }
    }

    @Override
    public void stop() {
        this.target = null;
        this.cooldown = SCARE_COOLDOWN + this.crow.getRandom().nextInt(SCARE_COOLDOWN);
        this.crow.getNavigation().stop();
    }

    @Nullable
    private PathfinderMob findNearestSmallBird() {
        List<PathfinderMob> candidates = this.crow.level().getEntitiesOfClass(PathfinderMob.class,
                this.crow.getBoundingBox().inflate(SCARE_RADIUS),
                e -> e != this.crow && e.isAlive()
                        && (e instanceof SparrowEntity || e instanceof BudgerigarEntity));
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(java.util.Comparator.comparingDouble(this.crow::distanceToSqr));
        return candidates.get(0);
    }

    private void scareTarget() {
        if (this.target instanceof SparrowEntity sparrow) {
            sparrow.birdBrain().onFrightened(0.5F);
        } else if (this.target instanceof BudgerigarEntity budgerigar) {
            budgerigar.birdBrain().onFrightened(0.5F);
        }
    }
}