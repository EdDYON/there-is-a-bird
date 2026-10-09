package EdDYON.guaniao.client.fan;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.fan.FeatherFanItem;
import EdDYON.guaniao.network.FeatherFanPiercePacket;
import EdDYON.guaniao.network.GuaniaoNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT)
public final class FeatherFanClientEvents {
    private FeatherFanClientEvents() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                || event.getAction() != GLFW.GLFW_PRESS
                || minecraft.screen != null) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null || !FeatherFanItem.isFullyCharged(player)) {
            return;
        }

        event.setCanceled(true);
        GuaniaoNetwork.sendToServer(new FeatherFanPiercePacket());
    }
}
