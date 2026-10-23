package EdDYON.guaniao.content.bird.kiwi;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

final class KiwiForagePatchGoal extends Goal {
    private static final int MAX_TRAVEL_TICKS = 180;
    private final KiwiEntity kiwi;
    private BlockPos foragePos;
    private int cooldown;
    private int travelTicks;
    private boolean foodApplied;

    KiwiForagePatchGoal(KiwiEntity kiwi) {
        this.kiwi = kiwi;
        this.cooldown = 120 + kiwi.getRandom().nextInt(220);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown-- > 0
                || !this.kiwi.isActiveTime()
                || !this.kiwi.canStartCalmBehavior()
                || this.kiwi.isConflictActive()
                || !this.kiwi.birdBrain().wantsForage()) {
            return false;
        }

        this.foragePos = KiwiHabitatUtil.findForagePatch(this.kiwi);
        if (this.foragePos == null) {
            this.cooldown = 80 + this.kiwi.getRandom().nextInt(100);
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        KiwiBehaviorState state = this.kiwi.getBehaviorState();
        return this.foragePos != null
                && this.kiwi.isActiveTime()
                && !this.kiwi.isConflictActive()
                && this.kiwi.hurtTime <= 0
                && this.kiwi.birdBrain().computeRiskScore() < 0.62F
                && ((state == KiwiBehaviorState.FORAGING && this.travelTicks < MAX_TRAVEL_TICKS)
                || (state == KiwiBehaviorState.PECKING && this.kiwi.getBehaviorTicks() > 0));
    }

    @Override
    public void start() {
        this.travelTicks = 0;
        this.foodApplied = false;
        this.kiwi.setBehaviorState(KiwiBehaviorState.FORAGING, 0);
        this.kiwi.getNavigation().moveTo(
                this.foragePos.getX() + 0.5D,
                this.foragePos.getY(),
                this.foragePos.getZ() + 0.5D,
                KiwiDefinition.WANDER_SPEED);
    }

    @Override
    public void tick() {
        if (this.foragePos == null) {
            return;
        }
        this.kiwi.getLookControl().setLookAt(
                this.foragePos.getX() + 0.5D,
                this.foragePos.getY() - 0.15D,
                this.foragePos.getZ() + 0.5D,
                30.0F,
                this.kiwi.getMaxHeadXRot());

        if (this.kiwi.getBehaviorState() == KiwiBehaviorState.FORAGING) {
            ++this.travelTicks;
            double distanceSqr = this.kiwi.distanceToSqr(Vec3.atBottomCenterOf(this.foragePos));
            if (distanceSqr <= 1.8D) {
                this.kiwi.getNavigation().stop();
                this.kiwi.setBehaviorState(KiwiBehaviorState.PECKING, KiwiDefinition.PECK_DURATION_TICKS);
            } else if (this.kiwi.getNavigation().isDone() && this.travelTicks % 20 == 0) {
                this.kiwi.getNavigation().moveTo(
                        this.foragePos.getX() + 0.5D,
                        this.foragePos.getY(),
                        this.foragePos.getZ() + 0.5D,
                        KiwiDefinition.WANDER_SPEED);
            }
            return;
        }

        this.kiwi.getNavigation().stop();
        int remaining = this.kiwi.decrementBehaviorTicks();
        int elapsed = KiwiDefinition.PECK_DURATION_TICKS - remaining;
        if (!this.foodApplied && elapsed >= KiwiDefinition.PECK_FOOD_TICK) {
            this.foodApplied = true;
            this.kiwi.birdBrain().onEat(0.14F);
            this.spawnGroundParticles();
        }
    }

    @Override
    public void stop() {
        if (this.kiwi.getBehaviorState() == KiwiBehaviorState.FORAGING
                || this.kiwi.getBehaviorState() == KiwiBehaviorState.PECKING) {
            this.kiwi.setBehaviorState(KiwiBehaviorState.AWAKE, 0);
        }
        this.cooldown = this.kiwi.level().isRainingAt(this.kiwi.blockPosition())
                ? 140 + this.kiwi.getRandom().nextInt(261)
                : 220 + this.kiwi.getRandom().nextInt(381);
        this.foragePos = null;
    }

    private void spawnGroundParticles() {
        if (!(this.kiwi.level() instanceof ServerLevel serverLevel) || this.foragePos == null) {
            return;
        }
        BlockState ground = serverLevel.getBlockState(this.foragePos.below());
        serverLevel.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, ground),
                this.foragePos.getX() + 0.5D,
                this.foragePos.getY() + 0.05D,
                this.foragePos.getZ() + 0.5D,
                3,
                0.14D,
                0.03D,
                0.14D,
                0.03D);
    }
}
