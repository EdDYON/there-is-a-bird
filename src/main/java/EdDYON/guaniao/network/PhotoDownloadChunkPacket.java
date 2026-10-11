package EdDYON.guaniao.network;

import EdDYON.guaniao.client.camera.PhotoClientRepository;
import EdDYON.guaniao.content.camera.PhotoTransferLimits;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record PhotoDownloadChunkPacket(String photoId, int chunkIndex, byte[] data) {
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

    public static void handle(PhotoDownloadChunkPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> PhotoClientRepository.acceptDownloadChunk(packet.photoId, packet.chunkIndex, packet.data)
        ));
        context.setPacketHandled(true);
    }
}
