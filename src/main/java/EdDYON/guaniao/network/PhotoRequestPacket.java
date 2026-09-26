package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.PhotoTransferLimits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhotoRequestPacket(String photoId, String expectedHash) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PhotoRequestPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "photo_request"));

    @Override
    public CustomPacketPayload.Type<PhotoRequestPacket> type() { return TYPE; }

    public static void encode(PhotoRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.photoId, PhotoTransferLimits.MAX_PHOTO_ID_LENGTH);
        buffer.writeUtf(packet.expectedHash, PhotoTransferLimits.SHA256_HEX_LENGTH);
    }

    public static PhotoRequestPacket decode(FriendlyByteBuf buffer) {
        return new PhotoRequestPacket(
                buffer.readUtf(PhotoTransferLimits.MAX_PHOTO_ID_LENGTH),
                buffer.readUtf(PhotoTransferLimits.SHA256_HEX_LENGTH)
        );
    }

    public static void handle(PhotoRequestPacket packet, IPayloadContext context) {
        ServerPlayer player = (context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null);
        if (player != null) {
            PhotoUploadManager.requestDownload(player, packet.photoId, packet.expectedHash);
        }
    }
}
