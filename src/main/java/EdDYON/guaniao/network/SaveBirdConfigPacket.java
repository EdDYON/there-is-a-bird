package EdDYON.guaniao.network;

import EdDYON.guaniao.config.BirdConfigData;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.event.BirdDroppingEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SaveBirdConfigPacket {
    private final BirdConfigData data;

    public SaveBirdConfigPacket(BirdConfigData data) {
        this.data = data.copy();
    }

    public static void encode(SaveBirdConfigPacket packet, FriendlyByteBuf buffer) {
        BirdConfigPacketCodec.encode(buffer, packet.data);
    }

    public static SaveBirdConfigPacket decode(FriendlyByteBuf buffer) {
        return new SaveBirdConfigPacket(BirdConfigPacketCodec.decode(buffer));
    }

    public static void handle(SaveBirdConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || (!player.hasPermissions(2)
                    && (player.server == null || !player.server.isSingleplayerOwner(player.getGameProfile())))) {
                return;
            }
            BirdConfigManager.replaceAndSave(packet.data, player.server);
            BirdDroppingEvents.refreshLoadedBirdCooldowns(player.server);
            player.displayClientMessage(Component.translatable("message.guaniao.bird_config.saved"), false);
            GuaniaoNetwork.sendToPlayer(new OpenBirdConfigPacket(BirdConfigManager.snapshot()), player);
        });
        context.setPacketHandled(true);
    }
}
