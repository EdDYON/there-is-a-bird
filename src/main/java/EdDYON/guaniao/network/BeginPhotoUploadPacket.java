package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.PhotoTransferLimits;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

public record BeginPhotoUploadPacket(
        UUID uploadId,
        InteractionHand hand,
        int totalBytes,
        int width,
        int height,
        String contentHash
) {
    public static void encode(BeginPhotoUploadPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.uploadId);
        buffer.writeEnum(packet.hand);
        buffer.writeVarInt(packet.totalBytes);
        buffer.writeVarInt(packet.width);
        buffer.writeVarInt(packet.height);
        buffer.writeUtf(packet.contentHash, PhotoTransferLimits.SHA256_HEX_LENGTH);
    }

    public static BeginPhotoUploadPacket decode(FriendlyByteBuf buffer) {
        return new BeginPhotoUploadPacket(
                buffer.readUUID(),
                buffer.readEnum(InteractionHand.class),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(PhotoTransferLimits.SHA256_HEX_LENGTH)
        );
    }

    public static void handle(BeginPhotoUploadPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            PhotoUploadManager.begin(player, packet.uploadId, packet.hand, packet.totalBytes, packet.width, packet.height, packet.contentHash);
        }
        context.setPacketHandled(true);
    }
}
