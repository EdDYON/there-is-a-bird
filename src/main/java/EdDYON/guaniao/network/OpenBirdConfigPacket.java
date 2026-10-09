package EdDYON.guaniao.network;

import EdDYON.guaniao.client.config.BirdConfigClient;
import EdDYON.guaniao.config.BirdConfigData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class OpenBirdConfigPacket {
    private final BirdConfigData data;

    public OpenBirdConfigPacket(BirdConfigData data) {
        this.data = data.copy();
    }

    public static void encode(OpenBirdConfigPacket packet, FriendlyByteBuf buffer) {
        BirdConfigPacketCodec.encode(buffer, packet.data);
    }

    public static OpenBirdConfigPacket decode(FriendlyByteBuf buffer) {
        return new OpenBirdConfigPacket(BirdConfigPacketCodec.decode(buffer));
    }

    public static void handle(OpenBirdConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> BirdConfigClient.open(packet.data)
        ));
        context.setPacketHandled(true);
    }
}
