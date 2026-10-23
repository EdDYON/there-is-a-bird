package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.frame.FrameDesign;
import EdDYON.guaniao.content.camera.frame.FrameWorkbenchMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record CraftFramePacket(int containerId, int revision, @Nullable FrameDesign design) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CraftFramePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "craft_frame"));

    @Override
    public CustomPacketPayload.Type<CraftFramePacket> type() { return TYPE; }
    public static void encode(CraftFramePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.revision);
        if (packet.design == null) throw new IllegalArgumentException("A frame design is required");
        packet.design.writeTo(buffer);
    }

    public static CraftFramePacket decode(FriendlyByteBuf buffer) {
        return new CraftFramePacket(buffer.readVarInt(), buffer.readVarInt(), FrameDesign.readFrom(buffer));
    }

    public static void handle(CraftFramePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null);
            if (player != null && packet.design != null && player.containerMenu instanceof FrameWorkbenchMenu menu
                    && menu.containerId == packet.containerId) {
                menu.craft(player, packet.revision, packet.design);
            }
        });
    }
}
