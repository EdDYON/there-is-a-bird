package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.network.PhotoUploadManager;
import EdDYON.guaniao.content.camera.LegacyPhotoMigration;
import EdDYON.guaniao.content.camera.PhotoIndexSavedData;
import EdDYON.guaniao.content.camera.PhotoIoService;
import EdDYON.guaniao.content.camera.PhotoMaintenance;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class PhotoTransferEvents {
    private static long nextMaintenanceTick;
    private PhotoTransferEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PhotoUploadManager.tick(event.getServer());
            LegacyPhotoMigration.tick(event.getServer());
            long now = event.getServer().overworld().getGameTime();
            if (now >= nextMaintenanceTick && !PhotoMaintenance.isRunning()) {
                nextMaintenanceTick = now + 36_000L;
                PhotoMaintenance.schedule(event.getServer(), true, result -> { });
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        PhotoIndexSavedData.get(event.getServer());
        nextMaintenanceTick = event.getServer().overworld().getGameTime() + 200L;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PhotoUploadManager.disconnect(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PhotoUploadManager.clear();
        LegacyPhotoMigration.clear();
        PhotoMaintenance.reset();
        PhotoIoService.shutdown();
        nextMaintenanceTick = 0L;
    }
}
