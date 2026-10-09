package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.PhotoTransferLimits;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record PhotoUploadChunkPacket(UUID uploadId, int chunkIndex, byte[] data) {
    public static void encode(PhotoUploadChunkPacket packet, FriendlyByteBuf buffer) {
        if (packet.data.length <= 0 || packet.data.length > PhotoTransferLimits.MAX_CHUNK_BYTES) {
            throw new IllegalArgumentException("Invalid photograph chunk size");
        }
        buffer.writeUUID(packet.uploadId);
        buffer.writeVarInt(packet.chunkIndex);
        buffer.writeByteArray(packet.data);
    }

    public static PhotoUploadChunkPacket decode(FriendlyByteBuf buffer) {
        return new PhotoUploadChunkPacket(
                buffer.readUUID(),
                buffer.readVarInt(),
                buffer.readByteArray(PhotoTransferLimits.MAX_CHUNK_BYTES)
        );
    }

    public static void handle(PhotoUploadChunkPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            PhotoUploadManager.acceptChunk(player, packet.uploadId, packet.chunkIndex, packet.data);
        }
        context.setPacketHandled(true);
    }
}
