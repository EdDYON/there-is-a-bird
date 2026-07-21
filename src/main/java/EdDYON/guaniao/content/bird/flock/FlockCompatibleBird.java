package EdDYON.guaniao.content.bird.flock;

import net.minecraft.world.entity.Entity;

/** Explicit flock compatibility; subclasses no longer inherit their parent's flock. */
public interface FlockCompatibleBird {
    default boolean canFlockWith(Entity other) {
        return other != null && other.getType() == ((Entity)this).getType();
    }
}
