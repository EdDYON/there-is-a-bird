package EdDYON.guaniao.content.bird;

import net.minecraft.world.phys.Vec3;

/** A server-side bird that can react to loud environmental sounds while awake or asleep. */
public interface BirdLoudSoundListener {
    void onLoudSound(Vec3 soundPosition, float volume);
}
