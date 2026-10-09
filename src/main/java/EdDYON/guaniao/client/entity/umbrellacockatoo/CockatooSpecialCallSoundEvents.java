package EdDYON.guaniao.client.entity.umbrellacockatoo;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.umbrellacockatoo.UmbrellaCockatooEntity;
import EdDYON.guaniao.content.bird.umbrellacockatoo.CockatooSpecialCall;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT)
public final class CockatooSpecialCallSoundEvents {
    private record Call(UmbrellaCockatooEntity bird, long startedAt, CockatooSpecialCallSound sound) { }
    private static final Map<UUID, Call> CALLS = new HashMap<>();

    private CockatooSpecialCallSoundEvents() { }

    @SubscribeEvent public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof UmbrellaCockatooEntity bird) || !bird.level().isClientSide
                || !bird.isSpecialPerformanceActive()) return;
        Call previous = CALLS.get(bird.getUUID());
        if (previous != null && previous.bird() == bird && previous.startedAt() == bird.getSpecialCallStartTick()) return;
        if (previous != null) finish(previous);
        // Tracking a bird halfway through a call must not restart its thirteen-second recording.
        if (bird.getSpecialCallElapsedTicks() > CockatooSpecialCall.START_WINDOW_TICKS) return;
        CockatooSpecialCallSound sound = new CockatooSpecialCallSound(bird);
        CALLS.put(bird.getUUID(), new Call(bird, bird.getSpecialCallStartTick(), sound));
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    @SubscribeEvent public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        var iterator = CALLS.values().iterator();
        while (iterator.hasNext()) {
            Call call = iterator.next();
            if (call.bird().level() != minecraft.level || !call.sound().canPlaySound()) {
                finish(call);
                iterator.remove();
            }
        }
    }

    @SubscribeEvent public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    @SubscribeEvent public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) clear();
    }

    private static void finish(Call call) {
        call.sound().finish();
        Minecraft.getInstance().getSoundManager().stop(call.sound());
    }

    private static void stopSounds() {
        for (Call call : CALLS.values()) finish(call);
    }

    private static void clear() {
        stopSounds();
        CALLS.clear();
    }

    @EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class EngineEvents {
        private EngineEvents() { }

        @SubscribeEvent public static void onEngineLoad(SoundEngineLoadEvent event) {
            // Keep each session remembered until it ends, so a reload cannot replay the recording.
            Minecraft.getInstance().execute(CockatooSpecialCallSoundEvents::stopSounds);
        }
    }
}
