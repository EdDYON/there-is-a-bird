package EdDYON.guaniao.network;

import EdDYON.guaniao.client.camera.PhotoClientRepository;
import EdDYON.guaniao.content.camera.PhotoTransferLimits;
import net.minecraft.network.FriendlyByteBuf;
import EdDYON.guaniao.util.ClientActions;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhotoDownloadChunkPacket(String photoId, int chunkIndex, byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PhotoDownloadChunkPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "photo_download_chunk"));

    @Override
    public CustomPacketPayload.Type<PhotoDownloadChunkPacket> type() { return TYPE; }

    public static void encode(PhotoDownloadChunkPacket packet, FriendlyByteBuf buffer) {
        if (packet.data.length <= 0 || packet.data.length > PhotoTransferLimits.MAX_CHUNK_BYTES) {
            throw new IllegalArgumentException("Invalid photograph chunk size");
        }
        buffer.writeUtf(packet.photoId, PhotoTransferLimits.MAX_PHOTO_ID_LENGTH);
        buffer.writeVarInt(packet.chunkIndex);
        buffer.writeByteArray(packet.data);
    }

    public static PhotoDownloadChunkPacket decode(FriendlyByteBuf buffer) {
        return new PhotoDownloadChunkPacket(
                buffer.readUtf(PhotoTransferLimits.MAX_PHOTO_ID_LENGTH),
                buffer.readVarInt(),
                buffer.readByteArray(PhotoTransferLimits.MAX_CHUNK_BYTES)
        );
    }

    public static void handle(PhotoDownloadChunkPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientActions.run(() -> () -> PhotoClientRepository.acceptDownloadChunk(packet.photoId, packet.chunkIndex, packet.data)
        ));
    }
}
