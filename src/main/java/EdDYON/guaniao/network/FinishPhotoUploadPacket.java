package EdDYON.guaniao.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FinishPhotoUploadPacket(UUID uploadId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FinishPhotoUploadPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "finish_photo_upload"));

    @Override
    public CustomPacketPayload.Type<FinishPhotoUploadPacket> type() { return TYPE; }

    public static void encode(FinishPhotoUploadPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.uploadId);
    }

    public static FinishPhotoUploadPacket decode(FriendlyByteBuf buffer) {
        return new FinishPhotoUploadPacket(buffer.readUUID());
    }

    public static void handle(FinishPhotoUploadPacket packet, IPayloadContext context) {
        ServerPlayer player = (context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null);
        if (player != null) {
            PhotoUploadManager.finish(player, packet.uploadId);
        }
    }
}
