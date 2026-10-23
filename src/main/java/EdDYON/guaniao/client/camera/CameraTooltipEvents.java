package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT)
public final class CameraTooltipEvents {
    private CameraTooltipEvents() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(GuaniaoItems.NIKON_D750.get())) return;
        if (Screen.hasShiftDown()) {
            event.getToolTip().add(Component.translatable("tooltip.guaniao.camera.open").withStyle(ChatFormatting.GRAY));
            for (Component line : CameraHelpScreen.controlLines()) {
                event.getToolTip().add(line.copy().withStyle(ChatFormatting.GRAY));
            }
        } else {
            event.getToolTip().add(Component.translatable("tooltip.guaniao.camera.help",
                    Component.translatable("key.keyboard.left.shift")).withStyle(ChatFormatting.GRAY));
        }
    }
}
