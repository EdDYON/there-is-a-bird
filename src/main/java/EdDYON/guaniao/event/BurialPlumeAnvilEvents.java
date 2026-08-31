package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.enchantment.GuaniaoEnchantments;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BurialPlumeAnvilEvents {
    private static final int ANVIL_LEVEL_COST = 8;

    private BurialPlumeAnvilEvents() {
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack fan = event.getLeft();
        ItemStack book = event.getRight();
        if (!fan.is(GuaniaoItems.WIND_FEATHER_FAN.get())) {
            return;
        }

        boolean burialBook = book.is(GuaniaoItems.BURIAL_PLUME_BOOK.get());
        boolean rivenBook = book.is(GuaniaoItems.RIVEN_PLUME_BOOK.get());
        boolean huntingBook = book.is(GuaniaoItems.HUNTING_RETURN_BOOK.get());
        if (!burialBook && !rivenBook && !huntingBook) {
            return;
        }

        boolean hasBurial = EnchantmentHelper.getItemEnchantmentLevel(
                GuaniaoEnchantments.BURIAL_PLUME.get(), fan) > 0;
        boolean hasRiven = EnchantmentHelper.getItemEnchantmentLevel(
                GuaniaoEnchantments.RIVEN_PLUME.get(), fan) > 0;
        boolean hasHunting = EnchantmentHelper.getItemEnchantmentLevel(
                GuaniaoEnchantments.HUNTING_RETURN.get(), fan) > 0;
        if (hasBurial || hasRiven || hasHunting) {
            return;
        }

        ItemStack output = fan.copy();
        if (burialBook) {
            output.enchant(GuaniaoEnchantments.BURIAL_PLUME.get(), 1);
        } else if (rivenBook) {
            output.enchant(GuaniaoEnchantments.RIVEN_PLUME.get(), 1);
        } else {
            output.enchant(GuaniaoEnchantments.HUNTING_RETURN.get(), 1);
        }
        event.setOutput(output);
        event.setCost(ANVIL_LEVEL_COST);
        event.setMaterialCost(1);
    }
}
