package EdDYON.guaniao.content.bird.kiwi;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

final class KiwiSpacingGoal extends Goal {
    private final KiwiEntity kiwi;
    private Vec3 target;
    private int cooldown;

    KiwiSpacingGoal(KiwiEntity kiwi) {
        this.kiwi = kiwi;
        this.cooldown = 40 + kiwi.getRandom().nextInt(80);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown-- > 0
                || !this.kiwi.isActiveTime()
                || this.kiwi.isConflictActive()
                || !this.kiwi.canStartCalmBehavior()
                || this.kiwi.birdBrain().computeRiskScore() >= 0.45F) {
            return false;
        }

        List<KiwiEntity> neighbors = this.kiwi.level().getEntitiesOfClass(
                KiwiEntity.class,
                this.kiwi.getBoundingBox().inflate(6.0D, 2.5D, 6.0D),
                candidate -> candidate != this.kiwi && candidate.isAlive());
        if (neighbors.size() < 2) {
            this.cooldown = 80 + this.kiwi.getRandom().nextInt(100);
            return false;
        }

        Vec3 center = this.kiwi.position();
        for (KiwiEntity neighbor : neighbors) {
            center = center.add(neighbor.position());
        }
        center = center.scale(1.0D / (neighbors.size() + 1.0D));
        this.target = KiwiHabitatUtil.findSpacingTarget(this.kiwi, center);
        return this.target != null;
    }

    @Override
    public void start() {
        this.kiwi.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, 0.78D);
    }

    @Override
    public boolean canContinueToUse() {
        return !this.kiwi.isConflictActive() && !this.kiwi.getNavigation().isDone();
    }

    @Override
    public void stop() {
        this.target = null;
        this.cooldown = 100 + this.kiwi.getRandom().nextInt(140);
    }
}
