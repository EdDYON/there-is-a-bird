package EdDYON.guaniao.network;

import EdDYON.guaniao.client.camera.PhotoClientRepository;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record PhotoCaptureResultPacket(UUID uploadId, boolean success) {
    public static void encode(PhotoCaptureResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.uploadId);
        buffer.writeBoolean(packet.success);
    }

    public static PhotoCaptureResultPacket decode(FriendlyByteBuf buffer) {
        return new PhotoCaptureResultPacket(buffer.readUUID(), buffer.readBoolean());
    }

    public static void handle(PhotoCaptureResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> PhotoClientRepository.captureResult(packet.uploadId, packet.success)
        ));
        context.setPacketHandled(true);
    }
}
