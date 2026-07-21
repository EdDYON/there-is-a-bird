package EdDYON.guaniao.content.bird;

import net.minecraft.world.phys.Vec3;

/** A bird whose server-side sleep state can be interrupted by nearby loud sounds. */
public interface BirdSleepWakeable {
    boolean isBirdSleeping();

    void wakeFromLoudSound(Vec3 soundPosition);
}
