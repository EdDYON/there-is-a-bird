package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.command.BirdHeadPlacement;
import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
import EdDYON.guaniao.content.bird.kestrel.KestrelEntity;
import EdDYON.guaniao.content.bird.umbrellacockatoo.UmbrellaCockatooEntity;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdHeadPerchEvents {
    private BirdHeadPerchEvents() { }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onServerStopping(ServerStoppingEvent event) {
        // Passenger birds are skipped by world saves; detach before players are saved.
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            releaseHeadBirds(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            releaseHeadBirds(player);
        }
    }

    private static void releaseHeadBirds(ServerPlayer player) {
        for (Entity passenger : java.util.List.copyOf(player.getPassengers())) {
            if (!(passenger instanceof HummingbirdEntity || passenger instanceof KestrelEntity
                    || passenger instanceof UmbrellaCockatooEntity)
                    || !(passenger instanceof TamableAnimal bird) || !bird.isTame() || !bird.isOwnedBy(player)) continue;
            bird.stopRiding();
            if (bird.isPassenger()) continue;
            bird.setDeltaMovement(Vec3.ZERO);
            bird.setNoGravity(false);
            bird.resetFallDistance();
        }
    }

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
