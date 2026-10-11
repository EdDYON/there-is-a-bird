package EdDYON.guaniao.content.bird.kiwi;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

final class KiwiListenGoal extends Goal {
    private final KiwiEntity kiwi;
    private Vec3 sourcePosition;
    private int remainingTicks;
    private int elapsedTicks;
    private int cooldown;

    KiwiListenGoal(KiwiEntity kiwi) {
        this.kiwi = kiwi;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown-- > 0
                || this.kiwi.isConflictActive()
                || this.kiwi.getBehaviorState() != KiwiBehaviorState.AWAKE
                || !this.kiwi.onGround()) {
            return false;
        }

        float risk = this.kiwi.birdBrain().computeRiskScore();
        boolean heardSound = this.kiwi.hasRecentLoudSound();
        if (!heardSound && (risk < 0.35F || risk >= 0.60F)) {
            return false;
        }

        this.sourcePosition = heardSound ? this.kiwi.getLastHeardSound() : null;
        Player player = this.kiwi.birdBrain().senses().nearestPlayer();
        if (this.sourcePosition == null && player != null) {
            this.sourcePosition = player.getEyePosition();
        }
        return this.sourcePosition != null;
    }

    @Override
    public void start() {
        this.elapsedTicks = 0;
        this.remainingTicks = 15 + this.kiwi.getRandom().nextInt(21);
        this.kiwi.getNavigation().stop();
        this.kiwi.setBehaviorState(KiwiBehaviorState.LISTENING, this.remainingTicks);
    }

    @Override
    public boolean canContinueToUse() {
        return this.remainingTicks > 0
                && !this.kiwi.isConflictActive()
                && this.kiwi.hurtTime <= 0
                && this.kiwi.birdBrain().computeRiskScore() < 0.62F;
    }

    @Override
    public void tick() {
        this.kiwi.getNavigation().stop();
        ++this.elapsedTicks;
        --this.remainingTicks;
        this.kiwi.setBehaviorTicks(this.remainingTicks);
        this.kiwi.getLookControl().setLookAt(
                this.sourcePosition.x,
                this.sourcePosition.y,
                this.sourcePosition.z,
                35.0F,
                this.kiwi.getMaxHeadXRot());
        if (this.elapsedTicks >= 15
                && !this.kiwi.hasRecentLoudSound()
                && this.kiwi.birdBrain().computeRiskScore() < 0.28F) {
            this.remainingTicks = 0;
        }
    }

    @Override
    public void stop() {
        if (this.kiwi.getBehaviorState() == KiwiBehaviorState.LISTENING) {
            this.kiwi.setBehaviorState(KiwiBehaviorState.AWAKE, 0);
        }
        this.kiwi.consumeLoudSound();
        this.cooldown = 45 + this.kiwi.getRandom().nextInt(55);
        this.sourcePosition = null;
    }
}
