package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.network.BirdRuntimeConfigPacket;
import EdDYON.guaniao.network.GuaniaoNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdRuntimeConfigEvents {
    private BirdRuntimeConfigEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GuaniaoNetwork.sendToPlayer(new BirdRuntimeConfigPacket(
                    BirdConfigManager.birdsPassThroughLeaves(),
                    BirdConfigManager.aprilFoolsMode()), player);
        }
    }
}
