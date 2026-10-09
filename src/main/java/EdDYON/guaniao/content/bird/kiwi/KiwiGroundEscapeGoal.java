package EdDYON.guaniao.content.bird.kiwi;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;

final class KiwiGroundEscapeGoal extends Goal {
    private final KiwiEntity kiwi;
    private LivingEntity threat;
    private Vec3 escapeTarget;
    private int remainingTicks;
    private int scanCooldown;

    KiwiGroundEscapeGoal(KiwiEntity kiwi) {
        this.kiwi = kiwi;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.kiwi.isConflictActive() || this.scanCooldown-- > 0) {
            return false;
        }
        this.scanCooldown = 5 + this.kiwi.getRandom().nextInt(6);
        this.threat = this.findThreat();
        if (this.threat == null) {
            return false;
        }
        this.escapeTarget = KiwiHabitatUtil.findEscapeTarget(this.kiwi, this.threat.position());
        return this.escapeTarget != null;
    }

    @Override
    public void start() {
        this.remainingTicks = 80 + this.kiwi.getRandom().nextInt(41);
        this.kiwi.getNavigation().stop();
        this.kiwi.setBehaviorState(KiwiBehaviorState.GROUND_ESCAPE, this.remainingTicks);
        this.kiwi.interruptRest(240);
        this.kiwi.birdBrain().onFrightened(0.25F);
        this.moveToEscapeTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.remainingTicks <= 0 || this.threat == null || !this.threat.isAlive()) {
            return false;
        }
        double distanceSqr = this.kiwi.distanceToSqr(this.threat);
        return distanceSqr < 18.0D * 18.0D || this.kiwi.birdBrain().computeRiskScore() > 0.48F;
    }

    @Override
    public void tick() {
        --this.remainingTicks;
        this.kiwi.setBehaviorTicks(this.remainingTicks);
        this.kiwi.getLookControl().setLookAt(
                this.escapeTarget.x,
                this.escapeTarget.y,
                this.escapeTarget.z,
                45.0F,
                this.kiwi.getMaxHeadXRot());
        if (this.kiwi.getNavigation().isDone() || this.remainingTicks % 20 == 0) {
            Vec3 refreshed = KiwiHabitatUtil.findEscapeTarget(this.kiwi, this.threat.position());
            if (refreshed != null) {
                this.escapeTarget = refreshed;
            }
            this.moveToEscapeTarget();
        }
    }

    @Override
    public void stop() {
        if (this.kiwi.getBehaviorState() == KiwiBehaviorState.GROUND_ESCAPE) {
            this.kiwi.setBehaviorState(KiwiBehaviorState.AWAKE, 0);
        }
        this.kiwi.getNavigation().stop();
        this.threat = null;
        this.escapeTarget = null;
        this.scanCooldown = 12;
    }

    private void moveToEscapeTarget() {
        this.kiwi.getNavigation().moveTo(
                this.escapeTarget.x,
                this.escapeTarget.y,
                this.escapeTarget.z,
                KiwiDefinition.PANIC_SPEED);
    }

    private LivingEntity findThreat() {
        LivingEntity attacker = this.kiwi.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && !(attacker instanceof KiwiEntity)
                && this.kiwi.distanceToSqr(attacker) < 20.0D * 20.0D) {
            return attacker;
        }

        Player player = this.kiwi.birdBrain().senses().nearestPlayer();
        float risk = this.kiwi.birdBrain().computeRiskScore();
        if (player != null && !player.isSpectator()
                && (risk >= 0.60F
                || player.isSprinting() && this.kiwi.distanceToSqr(player) < 10.0D * 10.0D)) {
            return player;
        }

        AABB area = this.kiwi.getBoundingBox().inflate(10.0D, 4.0D, 10.0D);
        return this.kiwi.level().getEntitiesOfClass(LivingEntity.class, area,
                        candidate -> candidate != this.kiwi && candidate.isAlive() && candidate instanceof Enemy)
                .stream()
                .min(Comparator.comparingDouble(this.kiwi::distanceToSqr))
                .orElse(null);
    }
}
