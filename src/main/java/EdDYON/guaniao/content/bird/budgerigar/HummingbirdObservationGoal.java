package EdDYON.guaniao.content.bird.budgerigar;

import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.cockatiel.CockatielEntity;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

/** A brief, low-priority curiosity response; never changes a pet's command. */
public final class HummingbirdObservationGoal extends Goal {
    private final BudgerigarEntity bird;
    private final boolean watchOnly;
    private HummingbirdEntity target;
    private long nextScan;
    private int remainingTicks;
    private int repathTicks;

    public HummingbirdObservationGoal(BudgerigarEntity bird) {
        this.bird = bird;
        this.watchOnly = bird instanceof CockatielEntity;
        this.setFlags(this.watchOnly ? EnumSet.of(Flag.LOOK) : EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean available() {
        return (this.bird.getClass() == BudgerigarEntity.class || this.watchOnly)
                && this.bird.isAlive() && !this.bird.isNoAi()
                && this.bird.getBirdCommandMode() == BirdCommandMode.FREE
                && this.bird.canStartSocialGoal()
                && !this.bird.isFlightInProgress() && this.bird.onGround();
    }

    @Override
    public boolean canUse() {
        if (!this.available() || !this.bird.getNavigation().isDone()
                || !(this.bird.level() instanceof ServerLevel level)) return false;
        long now = level.getGameTime();
        if (now < this.nextScan) return false;
        this.nextScan = now + 200 + this.bird.getRandom().nextInt(40);
        if (this.bird.getRandom().nextInt(5) != 0 || !BirdScanBudget.tryAcquire(level, this.bird)) return false;
        this.target = level.getEntitiesOfClass(HummingbirdEntity.class, this.bird.getBoundingBox().inflate(7),
                        hummingbird -> hummingbird.isAlive() && !hummingbird.isSleeping()
                                && this.bird.distanceToSqr(hummingbird) <= 49
                                && this.bird.hasLineOfSight(hummingbird))
                .stream().min(Comparator.comparingDouble(this.bird::distanceToSqr)).orElse(null);
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.remainingTicks > 0 && this.available() && this.target != null
                && this.target.isAlive() && !this.target.isSleeping()
                && this.bird.distanceToSqr(this.target) <= 64
                && (!this.watchOnly || this.bird.getNavigation().isDone());
    }

    @Override
    public void start() {
        this.remainingTicks = 80;
        this.repathTicks = 0;
        if (!this.watchOnly) this.bird.setBehaviorStateFor(BudgerigarBehaviorState.CURIOUS, 80);
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        --this.remainingTicks;
        this.bird.getLookControl().setLookAt(this.target, 25, 25);
        if (this.watchOnly) return;
        double distance = this.bird.distanceToSqr(this.target);
        if (distance <= 6.25 || Math.abs(this.target.getY() - this.bird.getY()) > 1.5) {
            this.bird.getNavigation().stop();
            this.bird.setBehaviorStateFor(BudgerigarBehaviorState.CURIOUS, 20);
        } else if (--this.repathTicks <= 0) {
            this.repathTicks = 20;
            this.bird.setBehaviorState(BudgerigarBehaviorState.FOLLOWING);
            // Walk a few steps toward the flower visitor, without invoking the flight controller.
            this.bird.getNavigation().moveTo(this.target.getX(), this.bird.getY(), this.target.getZ(), .6);
        }
    }

    @Override
    public void stop() {
        this.target = null;
        if (!this.watchOnly) {
            this.bird.getNavigation().stop();
            if (this.bird.getBehaviorState() == BudgerigarBehaviorState.CURIOUS
                    || this.bird.getBehaviorState() == BudgerigarBehaviorState.FOLLOWING) {
                this.bird.setBehaviorState(BudgerigarBehaviorState.IDLE);
            }
        }
    }
}
