package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.PhotoTransferLimits;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record PhotoRequestPacket(String photoId, String expectedHash) {
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

    public static void handle(PhotoRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            PhotoUploadManager.requestDownload(player, packet.photoId, packet.expectedHash);
        }
        context.setPacketHandled(true);
    }
}
