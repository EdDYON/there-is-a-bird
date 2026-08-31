package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.myna.MynaEntity;
import EdDYON.guaniao.content.bird.myna.MynaMimicCue;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Routes audible, learnable sound events into nearby mynas' private memories. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class MynaMimicLearningEvents {
    private MynaMimicLearningEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        Entity source = event.getEntity();
        if (source instanceof MynaEntity) {
            // Prevent a future learned playback from teaching itself or nearby mynas in a feedback loop.
            return;
        }
        processSound(event, source.position());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        processSound(event, event.getPosition());
    }

    private static void processSound(PlayLevelSoundEvent event, Vec3 soundPosition) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.isCanceled()
                || !Float.isFinite(event.getOriginalVolume())
                || event.getOriginalVolume() <= 0.05F) {
            return;
        }
        MynaMimicCue cue = cueFrom(event.getSound());
        if (cue == null) {
            return;
        }

        double radius = MynaEntity.MIMIC_LEARNING_RANGE;
        double radiusSqr = radius * radius;
        AABB listeningArea = new AABB(soundPosition, soundPosition).inflate(radius);
        for (MynaEntity myna : level.getEntitiesOfClass(MynaEntity.class, listeningArea, Entity::isAlive)) {
            if (myna.distanceToSqr(soundPosition) <= radiusSqr) {
                myna.hearMimicCue(cue, soundPosition, event.getOriginalPitch());
            }
        }
    }

    @Nullable
    private static MynaMimicCue cueFrom(@Nullable Holder<SoundEvent> sound) {
        if (sound == null) {
            return null;
        }
        ResourceLocation soundId = ForgeRegistries.SOUND_EVENTS.getKey(sound.value());
        return MynaMimicCue.fromSoundId(soundId);
    }
}
