package EdDYON.guaniao.network;

import EdDYON.guaniao.client.camera.PhotoClientRepository;
import EdDYON.guaniao.content.camera.PhotoTransferLimits;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record PhotoDownloadStartPacket(
        String photoId,
        boolean found,
        int totalBytes,
        int width,
        int height,
        String contentHash
) {
    public static PhotoDownloadStartPacket missing(String photoId) {
        return new PhotoDownloadStartPacket(photoId, false, 0, 0, 0, "");
    }

    public static void encode(PhotoDownloadStartPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.photoId, PhotoTransferLimits.MAX_PHOTO_ID_LENGTH);
        buffer.writeBoolean(packet.found);
        buffer.writeVarInt(packet.totalBytes);
        buffer.writeVarInt(packet.width);
        buffer.writeVarInt(packet.height);
        buffer.writeUtf(packet.contentHash, PhotoTransferLimits.SHA256_HEX_LENGTH);
    }

    public static PhotoDownloadStartPacket decode(FriendlyByteBuf buffer) {
        return new PhotoDownloadStartPacket(
                buffer.readUtf(PhotoTransferLimits.MAX_PHOTO_ID_LENGTH),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(PhotoTransferLimits.SHA256_HEX_LENGTH)
        );
    }

    public static void handle(PhotoDownloadStartPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> PhotoClientRepository.beginDownload(
                        packet.photoId,
                        packet.found,
                        packet.totalBytes,
                        packet.width,
                        packet.height,
                        packet.contentHash
                )
        ));
        context.setPacketHandled(true);
    }
}
