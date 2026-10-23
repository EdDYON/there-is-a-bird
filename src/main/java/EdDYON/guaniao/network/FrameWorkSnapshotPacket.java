package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.frame.FrameWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import EdDYON.guaniao.util.ClientActions;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** An owner-only display snapshot; the preview is never an inventory slot. */
public record FrameWorkSnapshotPacket(int containerId, BlockPos pos, ItemStack preview,
                                      long seed, int remaining, int revision) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FrameWorkSnapshotPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "frame_work_snapshot"));

    @Override
    public CustomPacketPayload.Type<FrameWorkSnapshotPacket> type() { return TYPE; }
    public static void encode(FrameWorkSnapshotPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeBlockPos(packet.pos);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.preview);
        buffer.writeLong(packet.seed);
        buffer.writeVarInt(packet.remaining);
        buffer.writeVarInt(packet.revision);
    }

    public static FrameWorkSnapshotPacket decode(RegistryFriendlyByteBuf buffer) {
        return new FrameWorkSnapshotPacket(buffer.readVarInt(), buffer.readBlockPos(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                buffer.readLong(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(FrameWorkSnapshotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientActions.run( () -> () -> Client.receive(packet)));
    }

    private static final class Client {
        static void receive(FrameWorkSnapshotPacket packet) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof FrameWorkbenchMenu menu
                    && menu.containerId == packet.containerId) {
                menu.acceptWorkSnapshot(packet.pos, packet.preview, packet.seed, packet.remaining, packet.revision);
            }
        }
    }
}
