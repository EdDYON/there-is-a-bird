package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.PhotoTransferLimits;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BeginPhotoUploadPacket(
        UUID uploadId,
        InteractionHand hand,
        int totalBytes,
        int width,
        int height,
        String contentHash
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BeginPhotoUploadPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "begin_photo_upload"));

    @Override
    public CustomPacketPayload.Type<BeginPhotoUploadPacket> type() { return TYPE; }

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

    public static void handle(BeginPhotoUploadPacket packet, IPayloadContext context) {
        ServerPlayer player = (context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null);
        if (player != null) {
            PhotoUploadManager.begin(player, packet.uploadId, packet.hand, packet.totalBytes, packet.width, packet.height, packet.contentHash);
        }
    }
}
