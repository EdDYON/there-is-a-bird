package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.PhotoTransferLimits;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhotoUploadChunkPacket(UUID uploadId, int chunkIndex, byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PhotoUploadChunkPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "photo_upload_chunk"));

    @Override
    public CustomPacketPayload.Type<PhotoUploadChunkPacket> type() { return TYPE; }

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

    public static void handle(PhotoUploadChunkPacket packet, IPayloadContext context) {
        ServerPlayer player = (context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null);
        if (player != null) {
            PhotoUploadManager.acceptChunk(player, packet.uploadId, packet.chunkIndex, packet.data);
        }
    }
}
