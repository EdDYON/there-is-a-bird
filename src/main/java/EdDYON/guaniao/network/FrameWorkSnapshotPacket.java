package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.frame.FrameWorkbenchMenu;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** An owner-only display snapshot; the preview is never an inventory slot. */
public record FrameWorkSnapshotPacket(int containerId, BlockPos pos, ItemStack preview,
                                      long seed, int remaining, int revision) {
    public static void encode(FrameWorkSnapshotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeBlockPos(packet.pos);
        buffer.writeItem(packet.preview);
        buffer.writeLong(packet.seed);
        buffer.writeVarInt(packet.remaining);
        buffer.writeVarInt(packet.revision);
    }

    public static FrameWorkSnapshotPacket decode(FriendlyByteBuf buffer) {
        return new FrameWorkSnapshotPacket(buffer.readVarInt(), buffer.readBlockPos(), buffer.readItem(),
                buffer.readLong(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(FrameWorkSnapshotPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.receive(packet)));
        context.setPacketHandled(true);
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
