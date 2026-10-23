package EdDYON.guaniao.network;

import EdDYON.guaniao.client.camera.PhotoClientRepository;
import EdDYON.guaniao.content.camera.PhotoTransferLimits;
import net.minecraft.network.FriendlyByteBuf;
import EdDYON.guaniao.util.ClientActions;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhotoDownloadStartPacket(
        String photoId,
        boolean found,
        int totalBytes,
        int width,
        int height,
        String contentHash
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PhotoDownloadStartPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "photo_download_start"));

    @Override
    public CustomPacketPayload.Type<PhotoDownloadStartPacket> type() { return TYPE; }

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

    public static void handle(PhotoDownloadStartPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientActions.run(() -> () -> PhotoClientRepository.beginDownload(
                        packet.photoId,
                        packet.found,
                        packet.totalBytes,
                        packet.width,
                        packet.height,
                        packet.contentHash
                )
        ));
    }
}
