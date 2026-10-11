package EdDYON.guaniao.content.bird.nightheron;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import EdDYON.guaniao.content.bird.nightheron.NightHeronBehaviorState;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import EdDYON.guaniao.content.bird.flock.BirdFlockManager;

public class NightHeronFlockGoal
extends Goal {
    private final NightHeronEntity nightHeron;
    private NightHeronEntity neighbor;
    private int remainingTicks;

    public NightHeronFlockGoal(NightHeronEntity nightHeron) {
        this.nightHeron = nightHeron;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        if (!this.nightHeron.onGround() || this.nightHeron.getBehaviorState().isEscape() || this.nightHeron.getBehaviorState().isAirborne() || this.nightHeron.getRandom().nextInt(this.nightHeron.shouldRoost() ? 12 : 30) != 0) {
            return false;
        }
        Optional<NightHeronEntity> nearest = this.nearbyNightHerons().stream().min(Comparator.comparingDouble(arg_0 -> ((NightHeronEntity)this.nightHeron).distanceToSqr(arg_0)));
        if (nearest.isEmpty()) {
            return false;
        }
        this.neighbor = nearest.get();
        double distance = Math.sqrt(this.nightHeron.distanceToSqr((Entity)this.neighbor));
        return distance < 2.25 || this.nightHeron.shouldRoost() && distance > 5.0;
    }

    public boolean canContinueToUse() {
        return this.remainingTicks > 0 && this.neighbor != null && this.neighbor.isAlive() && this.nightHeron.onGround() && !this.nightHeron.getBehaviorState().isEscape();
    }

    public void start() {
        this.remainingTicks = 30 + this.nightHeron.getRandom().nextInt(45);
        this.nightHeron.setBehaviorState(NightHeronBehaviorState.SOCIAL_SPACING);
        this.moveForSpacing();
    }

    public void stop() {
        this.remainingTicks = 0;
        this.neighbor = null;
        if (!this.nightHeron.getBehaviorState().isEscape()) {
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.IDLE);
        }
    }

    public void tick() {
        --this.remainingTicks;
        if (this.remainingTicks % 16 == 0 || this.nightHeron.getNavigation().isDone()) {
            this.moveForSpacing();
        }
    }

    private void moveForSpacing() {
        Vec3 target;
        if (this.neighbor == null) {
            return;
        }
        double distance = Math.sqrt(this.nightHeron.distanceToSqr((Entity)this.neighbor));
        if (distance < 2.25) {
            target = LandRandomPos.getPosAway((PathfinderMob)this.nightHeron, (int)6, (int)3, (Vec3)this.neighbor.position());
        } else {
            Vec3 toward = this.neighbor.position().subtract(this.nightHeron.position());
            target = this.nightHeron.position().add(toward.normalize().scale(Math.min(4.0, distance - 3.5)));
        }
        if (target != null) {
            this.nightHeron.getNavigation().moveTo(target.x, target.y, target.z, 0.17);
        }
    }

    private List<NightHeronEntity> nearbyNightHerons() {
        return BirdFlockManager.nearby(this.nightHeron, NightHeronEntity.class, 8.0).stream()
                .filter(other -> other != this.nightHeron).toList();
    }
}
