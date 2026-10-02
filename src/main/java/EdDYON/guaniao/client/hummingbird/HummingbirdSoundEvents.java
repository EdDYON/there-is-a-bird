package EdDYON.guaniao.client.hummingbird;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT)
public final class HummingbirdSoundEvents {
    private HummingbirdSoundEvents() { }

    @SubscribeEvent public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) HummingbirdSoundManager.INSTANCE.tick();
    }

    @SubscribeEvent public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HummingbirdSoundManager.INSTANCE.clear();
    }

    @SubscribeEvent public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) HummingbirdSoundManager.INSTANCE.clear();
    }

    // Forge fires sound-engine reloads on the mod bus, including output-device changes.
    @Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class EngineEvents {
        private EngineEvents() { }

        @SubscribeEvent public static void onEngineLoad(SoundEngineLoadEvent event) {
            Minecraft.getInstance().execute(HummingbirdSoundManager.INSTANCE::clear);
        }
    }
}
