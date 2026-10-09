package EdDYON.guaniao.content.bird.command;

import EdDYON.guaniao.content.bird.flight.BirdFlightAware;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class BirdStayGoal<T extends TamableAnimal & CommandableBird> extends Goal {
    private final T bird;

    public BirdStayGoal(T bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.bird.isTame()
                && this.bird.isBirdCommandMode(BirdCommandMode.STAY)
                && !this.bird.isBirdEmergencyOverrideActive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.bird.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.bird.getNavigation().stop();
        // Let the flight controller reach a safe landing surface before holding
        // position. Cancelling its horizontal approach can strand STAY over water.
        if (this.bird instanceof BirdFlightAware flying && flying.isBirdLanding()) {
            return;
        }
        Vec3 velocity = this.bird.getDeltaMovement();
        this.bird.setDeltaMovement(0.0D, velocity.y, 0.0D);
    }
}
