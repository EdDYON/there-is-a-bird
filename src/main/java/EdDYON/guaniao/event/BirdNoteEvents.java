package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.advancement.BirdAdvancements;
import EdDYON.guaniao.content.note.BirdNoteContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Rewards reading a found bird note for the first time. A written book's read happens
 * client-side, but right-clicking it fires the item-use event on the server too.
 */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdNoteEvents {
    private BirdNoteEvents() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide().isClient()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.WRITTEN_BOOK) || !stack.hasTag()) {
            return;
        }
        if (stack.getTag().getByte(BirdNoteContent.NOTE_TAG) != 1) {
            return;
        }
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            BirdAdvancements.grant(serverPlayer, BirdAdvancements.BIRD_NOTE);
            BirdAdvancements.awardDevNote(serverPlayer, stack.getTag().getString("author"));
        }
    }
}
