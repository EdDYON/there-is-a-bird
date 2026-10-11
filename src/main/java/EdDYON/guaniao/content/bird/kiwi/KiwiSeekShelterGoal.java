package EdDYON.guaniao.content.bird.kiwi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

final class KiwiSeekShelterGoal extends Goal {
    private final KiwiEntity kiwi;
    private BlockPos target;
    private int searchCooldown;
    private int travelTicks;

    KiwiSeekShelterGoal(KiwiEntity kiwi) {
        this.kiwi = kiwi;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.kiwi.isActiveTime()
                || this.kiwi.isConflictActive()
                || this.kiwi.getRestInterruptionTicks() > 0
                || this.searchCooldown-- > 0
                || this.kiwi.getBehaviorState() != KiwiBehaviorState.AWAKE) {
            return false;
        }

        BlockPos remembered = this.kiwi.getRoostPos();
        if (KiwiHabitatUtil.isValidRoost(this.kiwi, remembered)) {
            if (this.kiwi.isAtRoost()) {
                return false;
            }
            this.target = remembered;
            return true;
        }

        this.kiwi.setRoostPos(null);
        this.target = KiwiHabitatUtil.findShelter(this.kiwi);
        if (this.target == null) {
            this.searchCooldown = 80 + this.kiwi.getRandom().nextInt(80);
            return false;
        }
        this.kiwi.setRoostPos(this.target);
        return true;
    }

    @Override
    public void start() {
        this.travelTicks = 0;
        this.kiwi.setBehaviorState(KiwiBehaviorState.SEEKING_SHELTER, 0);
        this.kiwi.getNavigation().moveTo(
                this.target.getX() + 0.5D,
                this.target.getY(),
                this.target.getZ() + 0.5D,
                0.92D);
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && !this.kiwi.isActiveTime()
                && !this.kiwi.isConflictActive()
                && this.kiwi.getRestInterruptionTicks() <= 0
                && this.travelTicks < 240
                && !this.kiwi.isAtRoost();
    }

    @Override
    public void tick() {
        ++this.travelTicks;
        this.kiwi.getLookControl().setLookAt(
                this.target.getX() + 0.5D,
                this.target.getY(),
                this.target.getZ() + 0.5D,
                30.0F,
                this.kiwi.getMaxHeadXRot());
        if (this.kiwi.getNavigation().isDone() && !this.kiwi.isAtRoost() && this.travelTicks % 30 == 0) {
            this.kiwi.getNavigation().moveTo(
                    this.target.getX() + 0.5D,
                    this.target.getY(),
                    this.target.getZ() + 0.5D,
                    0.92D);
        }
    }

    @Override
    public void stop() {
        boolean reached = this.kiwi.isAtRoost();
        if (!reached && this.travelTicks >= 200) {
            this.kiwi.setRoostPos(null);
        }
        if (this.kiwi.getBehaviorState() == KiwiBehaviorState.SEEKING_SHELTER) {
            this.kiwi.setBehaviorState(KiwiBehaviorState.AWAKE, 0);
        }
        this.target = null;
        this.searchCooldown = reached ? 20 : 60 + this.kiwi.getRandom().nextInt(80);
    }
}
