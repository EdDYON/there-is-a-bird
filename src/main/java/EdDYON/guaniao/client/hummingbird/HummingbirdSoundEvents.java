package EdDYON.guaniao.client.hummingbird;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT)
public final class HummingbirdSoundEvents {
    private HummingbirdSoundEvents() { }

    @SubscribeEvent public static void onClientTick(ClientTickEvent.Post event) {
        HummingbirdSoundManager.INSTANCE.tick();
    }

    @SubscribeEvent public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HummingbirdSoundManager.INSTANCE.clear();
    }

    @SubscribeEvent public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) HummingbirdSoundManager.INSTANCE.clear();
    }

    // NeoForge fires sound-engine reloads on the mod bus, including output-device changes.
    @EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class EngineEvents {
        private EngineEvents() { }

        @SubscribeEvent public static void onEngineLoad(SoundEngineLoadEvent event) {
            Minecraft.getInstance().execute(HummingbirdSoundManager.INSTANCE::clear);
        }
    }
}
