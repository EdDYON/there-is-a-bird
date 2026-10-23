package EdDYON.guaniao.content.dropping;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class BirdDroppingMessageUtil {
    public static final int MESSAGE_COUNT = 24;

    private BirdDroppingMessageUtil() {
    }

    public static Component stableTooltip(String prefix, ItemStack stack) {
        int hash = stack.getHoverName().getString().hashCode();
        if (EdDYON.guaniao.util.ItemData.hasMetadata(stack)) {
            hash = 31 * hash + stack.getComponents().hashCode();
        }
        int index = Math.floorMod(hash, MESSAGE_COUNT);
        return Component.translatable(prefix + "." + index);
    }

    public static void grant(ServerPlayer player, ResourceLocation id, String criterion) {
        AdvancementHolder advancement = player.server.getAdvancements().get(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
