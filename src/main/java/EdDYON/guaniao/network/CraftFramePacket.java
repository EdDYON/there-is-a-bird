package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.frame.FrameDesign;
import EdDYON.guaniao.content.camera.frame.FrameWorkbenchMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

public record CraftFramePacket(int containerId, int revision, @Nullable FrameDesign design) {
    public static void encode(CraftFramePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.revision);
        if (packet.design == null) throw new IllegalArgumentException("A frame design is required");
        packet.design.writeTo(buffer);
    }

    public static CraftFramePacket decode(FriendlyByteBuf buffer) {
        return new CraftFramePacket(buffer.readVarInt(), buffer.readVarInt(), FrameDesign.readFrom(buffer));
    }

    public static void handle(CraftFramePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && packet.design != null && player.containerMenu instanceof FrameWorkbenchMenu menu
                    && menu.containerId == packet.containerId) {
                menu.craft(player, packet.revision, packet.design);
            }
        });
        context.setPacketHandled(true);
    }
}
