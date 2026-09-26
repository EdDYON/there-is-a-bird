package EdDYON.guaniao.network;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class GuaniaoNetwork {
    private static final String PROTOCOL = "18";

    private GuaniaoNetwork() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GuaniaoNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL);
        registrar.playToServer(BeginPhotoUploadPacket.TYPE, StreamCodec.of((buffer, packet) -> BeginPhotoUploadPacket.encode(packet, buffer), BeginPhotoUploadPacket::decode), BeginPhotoUploadPacket::handle);
        registrar.playToServer(PhotoUploadChunkPacket.TYPE, StreamCodec.of((buffer, packet) -> PhotoUploadChunkPacket.encode(packet, buffer), PhotoUploadChunkPacket::decode), PhotoUploadChunkPacket::handle);
        registrar.playToServer(FinishPhotoUploadPacket.TYPE, StreamCodec.of((buffer, packet) -> FinishPhotoUploadPacket.encode(packet, buffer), FinishPhotoUploadPacket::decode), FinishPhotoUploadPacket::handle);
        registrar.playToClient(PhotoCaptureResultPacket.TYPE, StreamCodec.of((buffer, packet) -> PhotoCaptureResultPacket.encode(packet, buffer), PhotoCaptureResultPacket::decode), PhotoCaptureResultPacket::handle);
        registrar.playToServer(PhotoRequestPacket.TYPE, StreamCodec.of((buffer, packet) -> PhotoRequestPacket.encode(packet, buffer), PhotoRequestPacket::decode), PhotoRequestPacket::handle);
        registrar.playToClient(PhotoDownloadStartPacket.TYPE, StreamCodec.of((buffer, packet) -> PhotoDownloadStartPacket.encode(packet, buffer), PhotoDownloadStartPacket::decode), PhotoDownloadStartPacket::handle);
        registrar.playToClient(PhotoDownloadChunkPacket.TYPE, StreamCodec.of((buffer, packet) -> PhotoDownloadChunkPacket.encode(packet, buffer), PhotoDownloadChunkPacket::decode), PhotoDownloadChunkPacket::handle);
        registrar.playToClient(OpenBirdConfigPacket.TYPE, StreamCodec.of((buffer, packet) -> OpenBirdConfigPacket.encode(packet, buffer), OpenBirdConfigPacket::decode), OpenBirdConfigPacket::handle);
        registrar.playToServer(SaveBirdConfigPacket.TYPE, StreamCodec.of((buffer, packet) -> SaveBirdConfigPacket.encode(packet, buffer), SaveBirdConfigPacket::decode), SaveBirdConfigPacket::handle);
        registrar.playToClient(BirdRuntimeConfigPacket.TYPE, StreamCodec.of((buffer, packet) -> BirdRuntimeConfigPacket.encode(packet, buffer), BirdRuntimeConfigPacket::decode), BirdRuntimeConfigPacket::handle);
        registrar.playToServer(FeatherFanPiercePacket.TYPE, StreamCodec.of((buffer, packet) -> FeatherFanPiercePacket.encode(packet, buffer), FeatherFanPiercePacket::decode), FeatherFanPiercePacket::handle);
        registrar.playToServer(SetCameraFilterPacket.TYPE, StreamCodec.of((buffer, packet) -> SetCameraFilterPacket.encode(packet, buffer), SetCameraFilterPacket::decode), SetCameraFilterPacket::handle);
        registrar.playToServer(SetCameraSettingsPacket.TYPE, StreamCodec.of((buffer, packet) -> SetCameraSettingsPacket.encode(packet, buffer), SetCameraSettingsPacket::decode), SetCameraSettingsPacket::handle);
    }

    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
