package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.dropping.BirdDroppingMessageUtil;
import EdDYON.guaniao.content.dropping.PrankFoodEffectUtil;
import EdDYON.guaniao.content.dropping.PrankFoodUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

public final class PrankFoodEvents {
    private PrankFoodEvents() {
    }

    @Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
    public static final class Common {
        private Common() {
        }

        @SubscribeEvent
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            ItemStack stack = event.getItemStack();
            Player player = event.getEntity();
            if (!PrankFoodUtil.isPrankFood(stack) || !stack.isEdible() || player.isUsingItem()) {
                return;
            }

            player.startUsingItem(event.getHand());
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onFoodFinished(LivingEntityUseItemEvent.Finish event) {
            ItemStack stack = event.getItem();
            if (!PrankFoodUtil.isPrankFood(stack)) {
                return;
            }

            PrankFoodEffectUtil.apply(event.getEntity());
        }
    }

    @Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT)
    public static final class Client {
        private Client() {
        }

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (!PrankFoodUtil.isPrankFood(stack)) {
                return;
            }

            List<Component> tooltip = event.getToolTip();
            if (!tooltip.isEmpty()) {
                tooltip.set(0, PrankFoodUtil.storedPrankDisplayName(stack, tooltip.get(0)));
            }
            if (Screen.hasShiftDown()) {
                tooltip.add(BirdDroppingMessageUtil.stableTooltip("tooltip.guaniao.prank_food_suspicious", stack).copy().withStyle(ChatFormatting.GRAY));
            }
        }

        @SubscribeEvent
        public static void onDroppingTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (!PrankFoodUtil.isDropping(stack)) {
                return;
            }

            event.getToolTip().add(Component.translatable("tooltip.guaniao.bird_dropping_fertilizer").withStyle(ChatFormatting.GRAY));
            if (Screen.hasShiftDown()) {
                event.getToolTip().add(BirdDroppingMessageUtil.stableTooltip("tooltip.guaniao.dropping_evidence", stack).copy().withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
