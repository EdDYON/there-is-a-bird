package EdDYON.guaniao.content.bird.cassowary;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Lets calm cassowaries drift apart without treating one another as threats. */
final class CassowaryPersonalSpaceGoal extends Goal {
    private static final double START_DISTANCE_SQR = 6.0D * 6.0D;
    private static final double COMFORT_DISTANCE_SQR = 7.5D * 7.5D;

    private final CassowaryEntity cassowary;
    @Nullable private CassowaryEntity neighbor;
    @Nullable private Vec3 moveTarget;
    private int cooldown;
    private int moveTicks;
    private int repathTicks;

    CassowaryPersonalSpaceGoal(CassowaryEntity cassowary) {
        this.cassowary = cassowary;
        this.cooldown = 30 + cassowary.getRandom().nextInt(70);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            --this.cooldown;
            return false;
        }
        this.cooldown = 30 + this.cassowary.getRandom().nextInt(50);
        if (!this.cassowary.isDayActivityTime()
                || this.cassowary.getCassowaryBehaviorState() != CassowaryBehaviorState.CALM
                || this.cassowary.isOutsideTerritoryLimit()
                || this.cassowary.hasImmediateThreat()) {
            return false;
        }

        this.neighbor = this.cassowary.level().getEntitiesOfClass(
                        CassowaryEntity.class,
                        this.cassowary.getBoundingBox().inflate(6.0D, 3.0D, 6.0D),
                        other -> other != this.cassowary && other.isAlive())
                .stream()
                .filter(other -> this.cassowary.distanceToSqr(other) < START_DISTANCE_SQR)
                .min(Comparator.comparingDouble(this.cassowary::distanceToSqr))
                .orElse(null);
        if (this.neighbor == null) {
            return false;
        }

        this.moveTarget = this.cassowary.findPersonalSpacePosition(this.neighbor.position());
        return this.moveTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.neighbor != null
                && this.neighbor.isAlive()
                && this.moveTarget != null
                && this.moveTicks < 80
                && this.cassowary.getCassowaryBehaviorState() == CassowaryBehaviorState.CALM
                && this.cassowary.distanceToSqr(this.neighbor) < COMFORT_DISTANCE_SQR;
    }

    @Override
    public void start() {
        this.moveTicks = 0;
        this.repathTicks = 0;
        this.moveAway();
    }

    @Override
    public void tick() {
        ++this.moveTicks;
        if (this.neighbor == null) {
            return;
        }
        if (--this.repathTicks <= 0 || this.cassowary.getNavigation().isDone()) {
            Vec3 updatedTarget = this.cassowary.findPersonalSpacePosition(this.neighbor.position());
            if (updatedTarget != null) {
                this.moveTarget = updatedTarget;
            }
            this.moveAway();
        }
    }

    private void moveAway() {
        if (this.moveTarget == null) {
            return;
        }
        this.cassowary.getNavigation().moveTo(
                this.moveTarget.x, this.moveTarget.y, this.moveTarget.z, 0.62D);
        this.repathTicks = 18;
    }

    @Override
    public void stop() {
        this.cassowary.getNavigation().stop();
        this.neighbor = null;
        this.moveTarget = null;
        this.moveTicks = 0;
        this.repathTicks = 0;
        this.cooldown = 80 + this.cassowary.getRandom().nextInt(100);
    }
}
