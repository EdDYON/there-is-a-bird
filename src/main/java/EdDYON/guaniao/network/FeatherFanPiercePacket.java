package EdDYON.guaniao.network;

import EdDYON.guaniao.content.fan.FeatherFanItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class FeatherFanPiercePacket {
    public static void encode(FeatherFanPiercePacket packet, FriendlyByteBuf buffer) {
    }

    public static FeatherFanPiercePacket decode(FriendlyByteBuf buffer) {
        return new FeatherFanPiercePacket();
    }

    public static void handle(FeatherFanPiercePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getUseItem().getItem() instanceof FeatherFanItem fan) {
                fan.tryLaunchPiercing(player);
            }
        });
        context.setPacketHandled(true);
    }
}
