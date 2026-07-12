package EdDYON.guaniao.network;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GuaniaoNetwork {
    private static final String PROTOCOL = "3";
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GuaniaoMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private GuaniaoNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(PhotographTakenPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PhotographTakenPacket::encode)
                .decoder(PhotographTakenPacket::decode)
                .consumerMainThread(PhotographTakenPacket::handle)
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
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
