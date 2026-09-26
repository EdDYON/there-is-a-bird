package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.enchantment.GuaniaoEnchantments;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
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

        boolean hasBurial = GuaniaoEnchantments.level(fan, GuaniaoEnchantments.BURIAL_PLUME) > 0;
        boolean hasRiven = GuaniaoEnchantments.level(fan, GuaniaoEnchantments.RIVEN_PLUME) > 0;
        boolean hasHunting = GuaniaoEnchantments.level(fan, GuaniaoEnchantments.HUNTING_RETURN) > 0;
        if (hasBurial || hasRiven || hasHunting) {
            return;
        }

        ItemStack output = fan.copy();
        if (burialBook) {
            output.enchant(GuaniaoEnchantments.holder(event.getPlayer().registryAccess(), GuaniaoEnchantments.BURIAL_PLUME), 1);
        } else if (rivenBook) {
            output.enchant(GuaniaoEnchantments.holder(event.getPlayer().registryAccess(), GuaniaoEnchantments.RIVEN_PLUME), 1);
        } else {
            output.enchant(GuaniaoEnchantments.holder(event.getPlayer().registryAccess(), GuaniaoEnchantments.HUNTING_RETURN), 1);
        }
        event.setOutput(output);
        event.setCost(ANVIL_LEVEL_COST);
        event.setMaterialCost(1);
    }
}
