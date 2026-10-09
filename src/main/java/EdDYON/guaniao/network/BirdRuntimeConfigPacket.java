package EdDYON.guaniao.network;

import EdDYON.guaniao.config.BirdConfigManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BirdRuntimeConfigPacket(boolean birdsPassThroughLeaves, boolean aprilFoolsMode,
                                      boolean skyBirdEcologyEnabled) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BirdRuntimeConfigPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "bird_runtime_config"));

    @Override
    public CustomPacketPayload.Type<BirdRuntimeConfigPacket> type() { return TYPE; }

    public static void encode(BirdRuntimeConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.birdsPassThroughLeaves);
        buffer.writeBoolean(packet.aprilFoolsMode);
        buffer.writeBoolean(packet.skyBirdEcologyEnabled);
    }

    public static BirdRuntimeConfigPacket decode(FriendlyByteBuf buffer) {
        return new BirdRuntimeConfigPacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(BirdRuntimeConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> BirdConfigManager.applyRemoteRuntime(
                packet.birdsPassThroughLeaves,
                packet.aprilFoolsMode,
                packet.skyBirdEcologyEnabled));
    }
}
