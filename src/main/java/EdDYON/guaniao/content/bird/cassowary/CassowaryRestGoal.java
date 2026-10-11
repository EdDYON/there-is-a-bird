package EdDYON.guaniao.content.bird.cassowary;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class CassowaryRestGoal extends Goal {
    private static final double REST_POSITION_REACHED_SQR = 1.5D * 1.5D;
    private static final int MAX_TRAVEL_TICKS = 180;

    private final CassowaryEntity cassowary;
    @Nullable private Vec3 restPosition;
    private int cooldown;
    private int restTicks;
    private int travelTicks;
    private int repathTicks;
    private boolean traveling;

    CassowaryRestGoal(CassowaryEntity cassowary) {
        this.cassowary = cassowary;
        this.cooldown = 300 + cassowary.getRandom().nextInt(500);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cassowary.isRestState()) {
            return true;
        }
        if (--this.cooldown > 0
                || !this.cassowary.isHotMidday()
                || this.cassowary.getCassowaryBehaviorState() != CassowaryBehaviorState.CALM
                || this.cassowary.isOutsideTerritoryLimit()
                || !this.cassowary.getNavigation().isDone()
                || this.cassowary.hasImmediateThreat()) {
            return false;
        }
        if (this.cassowary.getRandom().nextInt(120) != 0) {
            return false;
        }
        this.restTicks = this.cassowary.randomBetween(200, 800);
        this.restPosition = this.cassowary.findRestPosition();
        if (this.restPosition == null) {
            this.cooldown = 80 + this.cassowary.getRandom().nextInt(100);
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.cassowary.isRestState()) {
            return true;
        }
        return this.traveling
                && this.restPosition != null
                && this.travelTicks < MAX_TRAVEL_TICKS
                && this.cassowary.isHotMidday()
                && this.cassowary.getCassowaryBehaviorState() == CassowaryBehaviorState.CALM
                && !this.cassowary.hasImmediateThreat();
    }

    @Override
    public void start() {
        if (this.cassowary.isRestState() || this.restPosition == null) {
            return;
        }
        this.travelTicks = 0;
        this.repathTicks = 0;
        if (this.cassowary.distanceToSqr(this.restPosition) <= REST_POSITION_REACHED_SQR) {
            this.beginResting();
        } else {
            this.traveling = true;
            this.moveToRestPosition();
        }
    }

    @Override
    public void tick() {
        if (this.traveling) {
            ++this.travelTicks;
            if (this.restPosition == null
                    || !this.cassowary.isHotMidday()
                    || this.cassowary.hasImmediateThreat()
                    || this.travelTicks >= MAX_TRAVEL_TICKS) {
                this.cancelTravel();
                return;
            }
            if (this.cassowary.distanceToSqr(this.restPosition) <= REST_POSITION_REACHED_SQR) {
                this.beginResting();
                return;
            }
            if (--this.repathTicks <= 0 || this.cassowary.getNavigation().isDone()) {
                this.moveToRestPosition();
            }
            return;
        }

        this.cassowary.getNavigation().stop();
        switch (this.cassowary.getCassowaryBehaviorState()) {
            case REST_ENTER -> {
                if (this.cassowary.hasThreatWithin(8.0D) || !this.cassowary.isDayActivityTime()) {
                    this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.REST_EXIT);
                } else if (this.cassowary.getBehaviorTicks() >= 34) {
                    this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.RESTING);
                }
            }
            case RESTING -> {
                if (this.cassowary.hasThreatWithin(8.0D)
                        || !this.cassowary.isDayActivityTime()
                        || this.cassowary.getBehaviorTicks() >= this.restTicks) {
                    this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.REST_EXIT);
                }
            }
            case REST_EXIT -> {
                if (this.cassowary.getBehaviorTicks() >= 27) {
                    this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.CALM);
                }
            }
            default -> {
            }
        }
    }

    private void moveToRestPosition() {
        if (this.restPosition == null) {
            return;
        }
        this.cassowary.getNavigation().moveTo(
                this.restPosition.x, this.restPosition.y, this.restPosition.z, 0.64D);
        this.repathTicks = 18;
    }

    private void beginResting() {
        this.traveling = false;
        this.cassowary.getNavigation().stop();
        this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.REST_ENTER);
    }

    private void cancelTravel() {
        this.traveling = false;
        this.restPosition = null;
        this.cooldown = 120 + this.cassowary.getRandom().nextInt(120);
        this.cassowary.getNavigation().stop();
    }

    @Override
    public void stop() {
        if (this.traveling) {
            this.cassowary.getNavigation().stop();
        }
        this.traveling = false;
        this.restPosition = null;
        this.travelTicks = 0;
        this.repathTicks = 0;
        this.cooldown = 500 + this.cassowary.getRandom().nextInt(900);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
