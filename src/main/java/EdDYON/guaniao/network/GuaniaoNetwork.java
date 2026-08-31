package EdDYON.guaniao.network;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GuaniaoNetwork {
    private static final String PROTOCOL = "14";
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GuaniaoMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private GuaniaoNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(BeginPhotoUploadPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BeginPhotoUploadPacket::encode)
                .decoder(BeginPhotoUploadPacket::decode)
                .consumerMainThread(BeginPhotoUploadPacket::handle)
                .add();
        CHANNEL.messageBuilder(PhotoUploadChunkPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PhotoUploadChunkPacket::encode)
                .decoder(PhotoUploadChunkPacket::decode)
                .consumerMainThread(PhotoUploadChunkPacket::handle)
                .add();
        CHANNEL.messageBuilder(FinishPhotoUploadPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FinishPhotoUploadPacket::encode)
                .decoder(FinishPhotoUploadPacket::decode)
                .consumerMainThread(FinishPhotoUploadPacket::handle)
                .add();
        CHANNEL.messageBuilder(PhotoCaptureResultPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PhotoCaptureResultPacket::encode)
                .decoder(PhotoCaptureResultPacket::decode)
                .consumerMainThread(PhotoCaptureResultPacket::handle)
                .add();
        CHANNEL.messageBuilder(PhotoRequestPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PhotoRequestPacket::encode)
                .decoder(PhotoRequestPacket::decode)
                .consumerMainThread(PhotoRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(PhotoDownloadStartPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PhotoDownloadStartPacket::encode)
                .decoder(PhotoDownloadStartPacket::decode)
                .consumerMainThread(PhotoDownloadStartPacket::handle)
                .add();
        CHANNEL.messageBuilder(PhotoDownloadChunkPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PhotoDownloadChunkPacket::encode)
                .decoder(PhotoDownloadChunkPacket::decode)
                .consumerMainThread(PhotoDownloadChunkPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenBirdConfigPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenBirdConfigPacket::encode)
                .decoder(OpenBirdConfigPacket::decode)
                .consumerMainThread(OpenBirdConfigPacket::handle)
                .add();
        CHANNEL.messageBuilder(SaveBirdConfigPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveBirdConfigPacket::encode)
                .decoder(SaveBirdConfigPacket::decode)
                .consumerMainThread(SaveBirdConfigPacket::handle)
                .add();
        CHANNEL.messageBuilder(BirdRuntimeConfigPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BirdRuntimeConfigPacket::encode)
                .decoder(BirdRuntimeConfigPacket::decode)
                .consumerMainThread(BirdRuntimeConfigPacket::handle)
                .add();
        CHANNEL.messageBuilder(FeatherFanPiercePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FeatherFanPiercePacket::encode)
                .decoder(FeatherFanPiercePacket::decode)
                .consumerMainThread(FeatherFanPiercePacket::handle)
                .add();
        CHANNEL.messageBuilder(SetCameraFilterPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetCameraFilterPacket::encode)
                .decoder(SetCameraFilterPacket::decode)
                .consumerMainThread(SetCameraFilterPacket::handle)
                .add();
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
