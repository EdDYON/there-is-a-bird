package EdDYON.guaniao.network;

import EdDYON.guaniao.client.config.BirdConfigClient;
import EdDYON.guaniao.config.BirdConfigData;
import net.minecraft.network.FriendlyByteBuf;
import EdDYON.guaniao.util.ClientActions;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;


public final class OpenBirdConfigPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenBirdConfigPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "open_bird_config"));

    @Override
    public CustomPacketPayload.Type<OpenBirdConfigPacket> type() { return TYPE; }

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

    public static void handle(OpenBirdConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientActions.run(() -> () -> BirdConfigClient.open(packet.data)
        ));
    }
}
