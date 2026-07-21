package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.LegacyPhotoMigration;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class PhotoLegacyMigrationEvents {
    private PhotoLegacyMigrationEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        for (int slot = 0; slot < event.getEntity().getInventory().getContainerSize(); slot++) {
            LegacyPhotoMigration.queue(event.getEntity().level(), event.getEntity().getInventory().getItem(slot));
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        for (Slot slot : event.getContainer().slots) {
            LegacyPhotoMigration.queue(event.getEntity().level(), slot.getItem());
        }
    }
}
