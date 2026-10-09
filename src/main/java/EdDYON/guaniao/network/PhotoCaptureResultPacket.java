package EdDYON.guaniao.network;

import EdDYON.guaniao.client.camera.PhotoClientRepository;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import EdDYON.guaniao.util.ClientActions;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhotoCaptureResultPacket(UUID uploadId, boolean success) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PhotoCaptureResultPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "photo_capture_result"));

    @Override
    public CustomPacketPayload.Type<PhotoCaptureResultPacket> type() { return TYPE; }

    public static void encode(PhotoCaptureResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.uploadId);
        buffer.writeBoolean(packet.success);
    }

    public static PhotoCaptureResultPacket decode(FriendlyByteBuf buffer) {
        return new PhotoCaptureResultPacket(buffer.readUUID(), buffer.readBoolean());
    }

    public static void handle(PhotoCaptureResultPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientActions.run(() -> () -> PhotoClientRepository.captureResult(packet.uploadId, packet.success)
        ));
    }
}
