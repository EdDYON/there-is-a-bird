package EdDYON.guaniao.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record FinishPhotoUploadPacket(UUID uploadId) {
    public static void encode(FinishPhotoUploadPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.uploadId);
    }

    public static FinishPhotoUploadPacket decode(FriendlyByteBuf buffer) {
        return new FinishPhotoUploadPacket(buffer.readUUID());
    }

    public static void handle(FinishPhotoUploadPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            PhotoUploadManager.finish(player, packet.uploadId);
        }
        context.setPacketHandled(true);
    }
}
