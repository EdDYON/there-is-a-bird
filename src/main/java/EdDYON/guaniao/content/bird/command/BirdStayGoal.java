package EdDYON.guaniao.content.bird.command;

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
        Vec3 velocity = this.bird.getDeltaMovement();
        this.bird.setDeltaMovement(0.0D, velocity.y, 0.0D);
    }
}
