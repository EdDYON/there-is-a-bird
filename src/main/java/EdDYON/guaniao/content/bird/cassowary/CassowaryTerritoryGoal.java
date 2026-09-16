package EdDYON.guaniao.content.bird.cassowary;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Owns the complete territorial encounter so its phases cannot fight other goals. */
final class CassowaryTerritoryGoal extends Goal {
    private static final int MAX_CHARGE_TICKS = 55;
    private static final int TRACKING_CHARGE_TICKS = 18;
    private static final int COMMITTED_CHARGE_TICKS = 12;
    private static final int PASS_THROUGH_TICKS = 6;
    private static final double COMMIT_DISTANCE = 5.0D;

    private final CassowaryEntity cassowary;
    @Nullable private LivingEntity target;
    @Nullable private Vec3 withdrawTarget;
    @Nullable private Vec3 committedDirection;
    @Nullable private Vec3 originalChargePoint;
    private int repathTicks;
    private int chargeTicks;
    private int committedChargeTicks;
    private int passThroughTicks;
    private boolean attackApplied;

    CassowaryTerritoryGoal(CassowaryEntity cassowary) {
        this.cassowary = cassowary;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity candidate = this.cassowary.acquireTerritoryTarget();
        if (candidate == null) {
            return false;
        }
        this.cassowary.rememberThreat(candidate);
        if (this.cassowary.isRestOrSleepState()) {
            this.cassowary.beginExitForThreat();
            return false;
        }
        this.target = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.cassowary.isTerritoryState()) {
            return false;
        }
        LivingEntity resolved = this.cassowary.resolveThreatTarget();
        if (resolved != null) {
            this.target = resolved;
        }
        return this.target != null && this.target.isAlive()
                && (!(this.target instanceof Player player)
                || (!player.isCreative() && !this.cassowary.isTrusted(player)))
                && (this.cassowary.distanceToSqr(this.target) <= 32.0D * 32.0D
                || this.cassowary.getLostSightTicks() < 70);
    }

    @Override
    public void start() {
        if (!this.cassowary.isTerritoryState()) {
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.WATCHING);
        }
        this.repathTicks = 0;
        this.chargeTicks = 0;
        this.committedChargeTicks = 0;
        this.passThroughTicks = -1;
        this.committedDirection = null;
        this.originalChargePoint = null;
        this.attackApplied = false;
        this.withdrawTarget = null;
    }

    @Override
    public void tick() {
        LivingEntity resolved = this.cassowary.resolveThreatTarget();
        if (resolved != null) {
            this.target = resolved;
        }
        if (this.target == null || !this.target.isAlive()) {
            return;
        }

        boolean visible = this.cassowary.getSensing().hasLineOfSight(this.target);
        if (visible) {
            this.cassowary.recordVisibleThreat(this.target);
        } else {
            this.cassowary.incrementLostSightTicks();
        }
        double distance = this.cassowary.distanceTo(this.target);
        this.cassowary.updateThreatLevel(this.target, distance, visible);

        CassowaryBehaviorState state = this.cassowary.getCassowaryBehaviorState();
        if (state != CassowaryBehaviorState.CHARGING
                && state != CassowaryBehaviorState.ATTACKING
                && state != CassowaryBehaviorState.WITHDRAWING
                && this.cassowary.isOutsideTerritoryLimit()) {
            this.cassowary.getNavigation().stop();
            this.cassowary.clearThreatTarget();
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.CALM);
            return;
        }

        switch (state) {
            case WATCHING -> this.tickWatching(distance, visible);
            case ALERT -> this.tickAlert(distance, visible);
            case WARNING -> this.tickWarning(distance);
            case CHARGING -> this.tickCharging(distance);
            case ATTACKING -> this.tickAttacking(distance);
            case WITHDRAWING -> this.tickWithdrawing(distance);
            default -> {
            }
        }
    }

    private void tickWatching(double distance, boolean visible) {
        if (this.cassowary.getBehaviorTicks() < 12) {
            Vec3 motion = this.cassowary.getDeltaMovement();
            this.cassowary.setDeltaMovement(motion.x * 0.88D, motion.y, motion.z * 0.88D);
        } else {
            this.cassowary.getNavigation().stop();
        }
        if (visible) {
            this.cassowary.turnBodyToward(this.target.position(), 0.7F, 66.0F);
        }
        if (visible && (distance < 9.0D || this.cassowary.getThreatLevel() >= 0.22F)) {
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.ALERT);
        } else if ((distance > 18.0D || !visible && this.cassowary.getLostSightTicks() >= 70)
                && this.cassowary.getThreatLevel() <= 0.03F
                && this.cassowary.getBehaviorTicks() >= 40) {
            this.cassowary.clearThreatTarget();
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.CALM);
        }
    }

    private void tickAlert(double distance, boolean visible) {
        this.cassowary.getNavigation().stop();
        if (visible) {
            this.cassowary.turnBodyToward(this.target.position(), 1.35F, 38.0F);
        }
        if (this.cassowary.getWarningCooldown() <= 0
                && visible
                && (distance < 5.5D || this.cassowary.getThreatLevel() >= 0.56F)) {
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.WARNING);
        } else if ((distance > 11.5D || !visible && this.cassowary.getLostSightTicks() > 35)
                && this.cassowary.getThreatLevel() < 0.24F
                && this.cassowary.getBehaviorTicks() >= 55) {
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.WATCHING);
        }
    }

    private void tickWarning(double distance) {
        this.cassowary.getNavigation().stop();
        this.cassowary.turnBodyToward(this.target.position(), 1.7F, 24.0F);
        int ticks = this.cassowary.getBehaviorTicks();
        if (this.cassowary.wasRecentlyProvoked()) {
            this.beginCharge();
        } else if (distance > 7.5D && !this.cassowary.isClosingThreat(this.target)) {
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.ALERT);
        } else if (ticks >= CassowaryEntity.WARNING_TICKS
                || ticks >= 45 && (distance <= 3.5D || this.cassowary.getThreatLevel() >= 0.90F)) {
            this.beginCharge();
        }
    }

    private void beginCharge() {
        this.repathTicks = 0;
        this.chargeTicks = 0;
        this.committedChargeTicks = 0;
        this.passThroughTicks = -1;
        this.committedDirection = null;
        this.originalChargePoint = null;
        this.attackApplied = false;
        this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.CHARGING);
    }

    private void tickCharging(double distance) {
        ++this.chargeTicks;
        if (this.committedDirection == null) {
            if (distance <= COMMIT_DISTANCE || this.chargeTicks >= TRACKING_CHARGE_TICKS) {
                this.beginCommittedCharge();
                this.tickCommittedCharge();
            } else {
                if (--this.repathTicks <= 0) {
                    this.cassowary.getNavigation().moveTo(this.target, 1.65D);
                    this.repathTicks = 4 + this.cassowary.getRandom().nextInt(3);
                }
                this.cassowary.turnBodyToward(this.target.position(), 4.0F, 0.0F);
            }
        } else {
            this.tickCommittedCharge();
        }
        this.cassowary.trySwoopAction();

        double contact = this.cassowary.getBbWidth() * 0.5D
                + this.target.getBbWidth() * 0.5D + 0.28D;
        if (this.cassowary.getAttackCooldown() <= 0
                && distance <= contact
                && Math.abs(this.target.getY() - this.cassowary.getY()) < 2.1D) {
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.ATTACKING);
            this.attackApplied = false;
            return;
        }

        boolean committedChargeFinished = this.committedDirection != null
                && (this.committedChargeTicks <= 0 || this.passThroughTicks == 0
                || this.cassowary.horizontalCollision);
        if (this.chargeTicks >= MAX_CHARGE_TICKS
                || committedChargeFinished
                || (this.committedDirection == null
                && (distance > 34.0D || this.cassowary.getLostSightTicks() >= 70))) {
            this.beginWithdrawal();
        }
    }

    private void beginCommittedCharge() {
        Vec3 direction = this.target.position().subtract(this.cassowary.position())
                .multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() <= 1.0E-5D) {
            direction = Vec3.directionFromRotation(0.0F, this.cassowary.getYRot());
        } else {
            direction = direction.normalize();
        }
        this.committedDirection = direction;
        this.originalChargePoint = this.target.position();
        this.committedChargeTicks = COMMITTED_CHARGE_TICKS;
        this.passThroughTicks = -1;
        this.cassowary.getNavigation().stop();
    }

    private void tickCommittedCharge() {
        this.cassowary.getNavigation().stop();
        double currentSpeed = Math.sqrt(this.cassowary.getDeltaMovement().horizontalDistanceSqr());
        double chargeSpeed = Math.min(0.62D, Math.max(0.50D, currentSpeed));
        Vec3 movement = this.committedDirection.scale(chargeSpeed);
        this.cassowary.setDeltaMovement(
                movement.x, this.cassowary.getDeltaMovement().y, movement.z);
        this.cassowary.turnBodyToward(
                this.cassowary.position().add(this.committedDirection.scale(4.0D)), 1.25F, 0.0F);

        if (this.hasPassedOriginalChargePoint()) {
            if (this.passThroughTicks < 0) {
                this.passThroughTicks = PASS_THROUGH_TICKS;
            } else if (this.passThroughTicks > 0) {
                --this.passThroughTicks;
            }
        } else {
            --this.committedChargeTicks;
        }
    }

    private boolean hasPassedOriginalChargePoint() {
        if (this.originalChargePoint == null || this.committedDirection == null) {
            return false;
        }
        Vec3 beyond = this.cassowary.position().subtract(this.originalChargePoint)
                .multiply(1.0D, 0.0D, 1.0D);
        return beyond.dot(this.committedDirection) > 0.0D;
    }

    private void tickAttacking(double distance) {
        Vec3 motion = this.cassowary.getDeltaMovement();
        double speed = Math.max(0.30D, Math.sqrt(motion.horizontalDistanceSqr()));
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.cassowary.getYRot()).scale(speed);
        this.cassowary.setDeltaMovement(forward.x, motion.y, forward.z);
        int ticks = this.cassowary.getBehaviorTicks();
        if (!this.attackApplied && ticks >= 4) {
            this.attackApplied = true;
            double reach = this.cassowary.getBbWidth() * 0.5D
                    + this.target.getBbWidth() * 0.5D + 0.85D;
            if (distance <= reach && Math.abs(this.target.getY() - this.cassowary.getY()) < 2.2D) {
                this.cassowary.performTerritorialHit(this.target);
            }
        }
        if (ticks >= 8) {
            this.beginWithdrawal();
        }
    }

    private void beginWithdrawal() {
        this.withdrawTarget = this.cassowary.findWithdrawPosition(this.target);
        if (this.withdrawTarget != null) {
            this.cassowary.getNavigation().moveTo(
                    this.withdrawTarget.x, this.withdrawTarget.y, this.withdrawTarget.z, 1.35D);
        }
        this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.WITHDRAWING);
    }

    private void tickWithdrawing(double distance) {
        if (this.withdrawTarget == null) {
            this.withdrawTarget = this.cassowary.findWithdrawPosition(this.target);
        }
        if (this.withdrawTarget != null
                && (this.cassowary.getNavigation().isDone() || this.cassowary.getBehaviorTicks() % 10 == 0)) {
            this.cassowary.getNavigation().moveTo(
                    this.withdrawTarget.x, this.withdrawTarget.y, this.withdrawTarget.z,
                    distance < 6.5D ? 1.35D : 1.08D);
        }
        if (this.cassowary.getBehaviorTicks() >= 52
                || this.cassowary.getBehaviorTicks() >= 18 && distance >= 8.0D) {
            this.cassowary.getNavigation().stop();
            this.cassowary.setWarningCooldown(60);
            this.cassowary.setThreatLevel(Math.min(this.cassowary.getThreatLevel(), 0.46F));
            if (!this.cassowary.isOutsideTerritoryLimit()
                    && this.target.isAlive() && distance < 16.0D) {
                this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.ALERT);
            } else {
                this.cassowary.clearThreatTarget();
                this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.CALM);
            }
        }
    }

    @Override
    public void stop() {
        this.cassowary.getNavigation().stop();
        this.target = null;
        this.withdrawTarget = null;
        this.committedDirection = null;
        this.originalChargePoint = null;
        this.repathTicks = 0;
        this.chargeTicks = 0;
        this.committedChargeTicks = 0;
        this.passThroughTicks = -1;
        this.attackApplied = false;
        if (this.cassowary.isTerritoryState()) {
            this.cassowary.clearThreatTarget();
            this.cassowary.setCassowaryBehaviorState(CassowaryBehaviorState.CALM);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
