package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.flight.BirdFlightLock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Shearing: right-click a bird with shears to pluck one of its species' color feathers.
 * The feather needs time to regrow before the bird can be sheared again; mutations yield
 * the closest matching special color.
 */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdShearEvents {
    private static final String TAG_LAST_SHEAR = "GuaniaoLastShearTime";
    private static final int REGROW_TICKS = 6000;

    private BirdShearEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Entity target = event.getTarget();
        if (target.level().isClientSide() || !(target instanceof LivingEntity bird)) {
            return;
        }
        if (BirdSpecies.from(bird) == null) {
            return;
        }
        Player player = event.getEntity();
        if (player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!(stack.getItem() instanceof ShearsItem)) {
            return;
        }

        Item feather = BirdFeatherEvents.randomShearFeatherFor(bird);
        if (feather == null) {
            player.displayClientMessage(Component.translatable("message.guaniao.bird_shear.no_feather"), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        CompoundTag data = bird.getPersistentData();
        long last = data.getLong(TAG_LAST_SHEAR);
        long now = bird.level().getGameTime();
        long elapsed = now - last;
        if (data.contains(TAG_LAST_SHEAR) && elapsed >= 0L && elapsed < REGROW_TICKS) {
            long remainingSeconds = Math.max(1L, (REGROW_TICKS - elapsed + 19L) / 20L);
            player.displayClientMessage(Component.translatable(
                    "message.guaniao.bird_shear.cooldown", remainingSeconds), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        data.putLong(TAG_LAST_SHEAR, now);
        BirdFeatherEvents.spawnFeather(bird, feather, 1);
        BirdFlightLock.disableFlight(bird, REGROW_TICKS);
        stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(event.getHand()));
        player.displayClientMessage(Component.translatable("message.guaniao.bird_shear.success"), true);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
