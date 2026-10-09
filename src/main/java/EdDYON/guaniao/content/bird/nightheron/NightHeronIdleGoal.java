package EdDYON.guaniao.content.bird.nightheron;

import java.util.EnumSet;
import EdDYON.guaniao.content.bird.nightheron.NightHeronBehaviorState;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class NightHeronIdleGoal
extends Goal {
    private final NightHeronEntity nightHeron;
    private int remainingTicks;
    private IdleAction action = IdleAction.STAND;

    public NightHeronIdleGoal(NightHeronEntity nightHeron) {
        this.nightHeron = nightHeron;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        return this.nightHeron.onGround() && !this.nightHeron.getBehaviorState().isAirborne() && !this.nightHeron.getBehaviorState().isEscape() && this.nightHeron.getTarget() == null && (this.nightHeron.hasBlockedFlightRecoveryActivity() || this.nightHeron.getRandom().nextInt(10) == 0);
    }

    public boolean canContinueToUse() {
        return this.remainingTicks > 0 && this.nightHeron.onGround() && !this.nightHeron.getBehaviorState().isAirborne() && !this.nightHeron.getBehaviorState().isEscape();
    }

    public void start() {
        this.action = this.nightHeron.consumeBlockedFlightRecoveryActivity() ? IdleAction.STROLL : this.chooseAction();
        this.remainingTicks = this.randomBetween(50, 140);
        if (this.action == IdleAction.STROLL) {
            this.remainingTicks = this.randomBetween(35, 80);
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.MICRO_STROLL);
            this.moveToShortStrollTarget();
            return;
        }
        if (this.action == IdleAction.PREEN) {
            this.nightHeron.triggerPreen();
            this.remainingTicks = 80;
            return;
        }
        if (this.action == IdleAction.NECK_STRETCH) {
            this.nightHeron.triggerNeckStretch();
            this.remainingTicks = 72;
            return;
        }
        if (this.action == IdleAction.LOOK) {
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.LOOK_AROUND);
            this.lookAtRandomPoint();
            return;
        }
        this.nightHeron.setBehaviorState(NightHeronBehaviorState.REST_STAND);
        this.nightHeron.getNavigation().stop();
    }

    public void stop() {
        this.remainingTicks = 0;
        if (!this.nightHeron.getBehaviorState().isAirborne() && !this.nightHeron.getBehaviorState().isEscape()) {
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.IDLE);
        }
    }

    public void tick() {
        --this.remainingTicks;
        if (this.action == IdleAction.STROLL) {
            if (this.nightHeron.getNavigation().isDone() && this.remainingTicks > 20 && this.nightHeron.getRandom().nextInt(18) == 0) {
                this.moveToShortStrollTarget();
            }
            return;
        }
        if (this.action == IdleAction.LOOK && this.remainingTicks % 24 == 0) {
            this.lookAtRandomPoint();
        }
        this.nightHeron.getNavigation().stop();
    }

    private IdleAction chooseAction() {
        var motivation = this.nightHeron.birdBrain().motivation();
        float hunger = motivation.hunger();
        float fear = motivation.fear();
        float fatigue = motivation.fatigue();
        float comfort = motivation.comfort();
        float alertness = motivation.alertness();
        int standWeight = NightHeronDefinition.IDLE_WEIGHT_STAND_BASE;
        int lookWeight = NightHeronDefinition.IDLE_WEIGHT_LOOK_BASE;
        int strollWeight = NightHeronDefinition.IDLE_WEIGHT_STROLL_BASE;
        int preenWeight = NightHeronDefinition.IDLE_WEIGHT_PREEN_BASE;
        int neckStretchWeight = NightHeronDefinition.IDLE_WEIGHT_NECK_STRETCH_BASE;
        if (this.nightHeron.isRoosting()) {
            standWeight += NightHeronDefinition.IDLE_ROOST_STAND_BONUS;
            lookWeight += NightHeronDefinition.IDLE_ROOST_LOOK_BONUS;
            strollWeight -= NightHeronDefinition.IDLE_ROOST_STROLL_PENALTY;
            preenWeight += NightHeronDefinition.IDLE_ROOST_PREEN_BONUS;
            neckStretchWeight += NightHeronDefinition.IDLE_ROOST_NECK_STRETCH_BONUS;
        }
        if (comfort > NightHeronDefinition.IDLE_COMFORT_HIGH) {
            standWeight += NightHeronDefinition.IDLE_COMFORT_STAND_BONUS;
            preenWeight += NightHeronDefinition.IDLE_COMFORT_PREEN_BONUS;
        }
        if (alertness > NightHeronDefinition.IDLE_ALERTNESS_HIGH) {
            lookWeight += NightHeronDefinition.IDLE_ALERT_LOOK_BONUS;
            neckStretchWeight += NightHeronDefinition.IDLE_ALERT_NECK_STRETCH_BONUS;
            preenWeight -= NightHeronDefinition.IDLE_ALERT_PREEN_PENALTY;
            strollWeight -= NightHeronDefinition.IDLE_ALERT_STROLL_PENALTY;
        }
        if (fatigue > NightHeronDefinition.IDLE_FATIGUE_HIGH) {
            standWeight += NightHeronDefinition.IDLE_FATIGUE_STAND_BONUS;
            strollWeight -= NightHeronDefinition.IDLE_FATIGUE_STROLL_PENALTY;
            preenWeight += NightHeronDefinition.IDLE_FATIGUE_PREEN_BONUS;
        }
        if (hunger > NightHeronDefinition.IDLE_HUNGER_HIGH && this.nightHeron.isNearWater(this.nightHeron.blockPosition(), 4)) {
            lookWeight += NightHeronDefinition.IDLE_HUNGER_LOOK_BONUS;
            neckStretchWeight += NightHeronDefinition.IDLE_HUNGER_NECK_STRETCH_BONUS;
        }
        if (fear > NightHeronDefinition.IDLE_FEAR_HIGH) {
            preenWeight -= NightHeronDefinition.IDLE_FEAR_PREEN_PENALTY;
            strollWeight -= NightHeronDefinition.IDLE_FEAR_STROLL_PENALTY;
            lookWeight += NightHeronDefinition.IDLE_FEAR_LOOK_BONUS;
            neckStretchWeight += NightHeronDefinition.IDLE_FEAR_NECK_STRETCH_BONUS;
        }
        return this.weightedPick(
                positive(standWeight),
                positive(lookWeight),
                positive(strollWeight),
                positive(preenWeight),
                positive(neckStretchWeight)
        );
    }

    private IdleAction weightedPick(int standWeight, int lookWeight, int strollWeight, int preenWeight, int neckStretchWeight) {
        int total = standWeight + lookWeight + strollWeight + preenWeight + neckStretchWeight;
        if (total <= 0) {
            return IdleAction.STAND;
        }
        int roll = this.nightHeron.getRandom().nextInt(total);
        if ((roll -= standWeight) < 0) {
            return IdleAction.STAND;
        }
        if ((roll -= lookWeight) < 0) {
            return IdleAction.LOOK;
        }
        if ((roll -= strollWeight) < 0) {
            return IdleAction.STROLL;
        }
        if ((roll -= preenWeight) < 0) {
            return IdleAction.PREEN;
        }
        return IdleAction.NECK_STRETCH;
    }

    private static int positive(int value) {
        return Math.max(0, value);
    }

    private void moveToShortStrollTarget() {
        Vec3 bestTarget = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int attempt = 0; attempt < 8; ++attempt) {
            Vec3 candidate = LandRandomPos.getPos((PathfinderMob)this.nightHeron, 5, 3);
            if (candidate == null) {
                continue;
            }
            float score = this.nightHeron.getWalkTargetValue(BlockPos.containing(candidate), this.nightHeron.level());
            if (score > bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }
        if (bestTarget != null) {
            this.nightHeron.getNavigation().moveTo(bestTarget.x, bestTarget.y, bestTarget.z, NightHeronDefinition.IDLE_STROLL_SPEED);
        }
    }

    private void lookAtRandomPoint() {
        float angle = this.nightHeron.getRandom().nextFloat() * ((float)Math.PI * 2);
        double distance = 3.0 + this.nightHeron.getRandom().nextDouble() * 4.0;
        this.nightHeron.getLookControl().setLookAt(this.nightHeron.getX() + (double)Mth.cos((float)angle) * distance, this.nightHeron.getEyeY(), this.nightHeron.getZ() + (double)Mth.sin((float)angle) * distance);
    }

    private int randomBetween(int min, int max) {
        return min + this.nightHeron.getRandom().nextInt(max - min + 1);
    }

    private static enum IdleAction {
        STAND,
        LOOK,
        STROLL,
        PREEN,
        NECK_STRETCH;

    }
}
