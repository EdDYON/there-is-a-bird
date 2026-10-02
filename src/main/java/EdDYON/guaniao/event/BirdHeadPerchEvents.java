package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.command.BirdHeadPlacement;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdHeadPerchEvents {
    private BirdHeadPerchEvents() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var owner = event.getEntity();
        if (event.getUseBlock() == Event.Result.DENY || event.getUseItem() == Event.Result.DENY
                || event.getFace() != Direction.UP || !BirdHeadPlacement.isPlacementGesture(owner, event.getHand())) return;
        boolean placed = event.getSide().isClient()
                || BirdHeadPlacement.tryPlace(owner, event.getHand(), event.getHitVec());
        // Vanilla still sends its UseItemOn packet after the client prediction returns.
        event.setCanceled(true);
        event.setCancellationResult(placed ? InteractionResult.sidedSuccess(owner.level().isClientSide) : InteractionResult.FAIL);
    }
}
