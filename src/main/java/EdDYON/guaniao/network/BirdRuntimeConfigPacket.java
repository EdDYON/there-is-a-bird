package EdDYON.guaniao.network;

import EdDYON.guaniao.config.BirdConfigManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record BirdRuntimeConfigPacket(boolean birdsPassThroughLeaves, boolean aprilFoolsMode) {
    public static void encode(BirdRuntimeConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.birdsPassThroughLeaves);
        buffer.writeBoolean(packet.aprilFoolsMode);
    }

    public static BirdRuntimeConfigPacket decode(FriendlyByteBuf buffer) {
        return new BirdRuntimeConfigPacket(buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(BirdRuntimeConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> BirdConfigManager.applyRemoteRuntime(
                packet.birdsPassThroughLeaves,
                packet.aprilFoolsMode));
        context.setPacketHandled(true);
    }
}
