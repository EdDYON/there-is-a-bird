package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.advancement.BirdAdvancements;
import EdDYON.guaniao.content.note.BirdNoteContent;
import EdDYON.guaniao.util.ItemData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Rewards reading a found bird note for the first time. A written book's read happens
 * client-side, but right-clicking it fires the item-use event on the server too.
 */
@EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdNoteEvents {
    private BirdNoteEvents() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide().isClient()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.WRITTEN_BOOK)) {
            return;
        }
        CompoundTag marker = ItemData.read(stack);
        if (marker == null || marker.getByte(BirdNoteContent.NOTE_TAG) != 1) {
            return;
        }
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            BirdAdvancements.grant(serverPlayer, BirdAdvancements.BIRD_NOTE);
            // Vanilla moves the author out of legacy item NBT during world upgrades.
            var content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            String author = content != null ? content.author() : marker.getString("author");
            BirdAdvancements.awardDevNote(serverPlayer, author);
        }
    }
}
