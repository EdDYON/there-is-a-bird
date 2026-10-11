package EdDYON.guaniao.content.bird.cassowary;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class CassowarySleepGoal extends Goal {
    private static final double SLEEP_POSITION_REACHED_SQR = 1.6D * 1.6D;
    private static final int MAX_TRAVEL_TICKS = 360;

    private final CassowaryEntity cassowary;
    @Nullable private Vec3 sleepPosition;
    private int retryCooldown;
    private int travelTicks;
    private int repathTicks;
    private boolean traveling;

    CassowarySleepGoal(CassowaryEntity cassowary) {
        this.cassowary = cassowary;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cassowary.isSleepState()) {
            return true;
        }
        if (this.retryCooldown > 0) {
            --this.retryCooldown;
            return false;
        }
        if (this.cassowary.isDayActivityTime()
                || this.cassowary.getCassowaryBehaviorState() != CassowaryBehaviorState.CALM
                || this.cassowary.hasImmediateThreat()) {
            return false;
        }
        this.sleepPosition = this.cassowary.findSleepPosition();
        if (this.sleepPosition == null) {
            this.retryCooldown = 80 + this.cassowary.getRandom().nextInt(80);
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.cassowary.isSleepState()) {
            return true;
        }
        return this.traveling
                && this.sleepPosition != null
                && this.travelTicks < MAX_TRAVEL_TICKS
                && !this.cassowary.isDayActivityTime()
                && this.cassowary.getCassowaryBehaviorState() == CassowaryBehaviorState.CALM
                && !this.cassowary.hasImmediateThreat();
    }

    @Override
    public void start() {
        if (this.cassowary.isSleepState() || this.sleepPosition == null) {
            return;
        }
        this.travelTicks = 0;
        this.repathTicks = 0;
        if (this.cassowary.distanceToSqr(this.sleepPosition) <= SLEEP_POSITION_REACHED_SQR) {
            this.beginSleeping();
        } else {
            this.traveling = true;
            this.moveToSleepPosition();
        }
    }

    @Override
    public void tick() {
        if (this.traveling) {
            ++this.travelTicks;
            if (this.sleepPosition == null
                    || this.cassowary.isDayActivityTime()
                    || this.cassowary.hasImmediateThreat()
                    || this.travelTicks >= MAX_TRAVEL_TICKS) {
                this.cancelTravel();
                return;
            }
            if (this.cassowary.distanceToSqr(this.sleepPosition) <= SLEEP_POSITION_REACHED_SQR) {
                this.beginSleeping();
                return;
            }
            if (--this.repathTicks <= 0 || this.cassowary.getNavigation().isDone()) {
                this.moveToSleepPosition();
            }
            return;
        }

        this.cassowary.getNavigation().stop();
        switch (this.cassowary.getCassowaryBehaviorState()) {
            case SLEEP_ENTER -> {
                if (this.cassowary.hasImmediateThreat()) {
                    this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.SLEEP_EXIT);
                } else if (this.cassowary.getBehaviorTicks() >= 29) {
                    this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.SLEEPING);
                }
            }
            case SLEEPING -> {
                if (this.cassowary.isDayActivityTime() || this.cassowary.hasImmediateThreat()) {
                    this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.SLEEP_EXIT);
                }
            }
            case SLEEP_EXIT -> {
                if (this.cassowary.getBehaviorTicks() >= 32) {
                    this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.CALM);
                }
            }
            default -> {
            }
        }
    }

    private void moveToSleepPosition() {
        if (this.sleepPosition == null) {
            return;
        }
        this.cassowary.getNavigation().moveTo(
                this.sleepPosition.x, this.sleepPosition.y, this.sleepPosition.z, 0.72D);
        this.repathTicks = 20;
    }

    private void beginSleeping() {
        this.traveling = false;
        this.cassowary.getNavigation().stop();
        this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.SLEEP_ENTER);
    }

    private void cancelTravel() {
        this.traveling = false;
        this.sleepPosition = null;
        this.retryCooldown = 100 + this.cassowary.getRandom().nextInt(100);
        this.cassowary.getNavigation().stop();
    }

    @Override
    public void stop() {
        if (this.traveling) {
            this.cassowary.getNavigation().stop();
        }
        this.traveling = false;
        this.sleepPosition = null;
        this.travelTicks = 0;
        this.repathTicks = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
