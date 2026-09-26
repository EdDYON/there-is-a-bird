package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.network.BirdRuntimeConfigPacket;
import EdDYON.guaniao.network.GuaniaoNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdRuntimeConfigEvents {
    private BirdRuntimeConfigEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GuaniaoNetwork.sendToPlayer(new BirdRuntimeConfigPacket(
                    BirdConfigManager.birdsPassThroughLeaves(),
                    BirdConfigManager.aprilFoolsMode(),
                    BirdConfigManager.skyBirdEcologyEnabled()), player);
        }
    }
}
