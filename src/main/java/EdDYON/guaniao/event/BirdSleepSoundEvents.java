package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.BirdLoudSoundListener;
import EdDYON.guaniao.content.bird.BirdSleepWakeable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdSleepSoundEvents {
    private static final float LOUD_SOUND_VOLUME = 1.5F;
    private static final double MIN_WAKE_RADIUS = 12.0D;
    private static final double MAX_WAKE_RADIUS = 48.0D;

    private BirdSleepSoundEvents() {
    }

    @SubscribeEvent
    public static void onSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        wakeNearbySleepingBirds(event, event.getEntity().position());
    }

    @SubscribeEvent
    public static void onSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        wakeNearbySleepingBirds(event, event.getPosition());
    }

    private static void wakeNearbySleepingBirds(PlayLevelSoundEvent event, Vec3 soundPosition) {
        Level level = event.getLevel();
        float volume = event.getOriginalVolume();
        if (level.isClientSide || event.isCanceled() || !Float.isFinite(volume) || volume < LOUD_SOUND_VOLUME) {
            return;
        }

        double radius = Math.min(MAX_WAKE_RADIUS, Math.max(MIN_WAKE_RADIUS, 8.0D + volume * 8.0D));
        double radiusSqr = radius * radius;
        AABB wakeArea = new AABB(soundPosition, soundPosition).inflate(radius);
        for (Entity entity : level.getEntities((Entity) null, wakeArea, candidate ->
                candidate instanceof BirdLoudSoundListener
                        || candidate instanceof BirdSleepWakeable bird && bird.isBirdSleeping())) {
            if (entity.distanceToSqr(soundPosition) <= radiusSqr) {
                if (entity instanceof BirdLoudSoundListener listener) {
                    listener.onLoudSound(soundPosition, volume);
                } else if (entity instanceof BirdSleepWakeable wakeable) {
                    wakeable.wakeFromLoudSound(soundPosition);
                }
            }
        }
    }
}
