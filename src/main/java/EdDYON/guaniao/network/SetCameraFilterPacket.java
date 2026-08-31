package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.CameraFilter;
import EdDYON.guaniao.content.camera.CameraSettingsData;
import EdDYON.guaniao.registry.GuaniaoItems;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record SetCameraFilterPacket(InteractionHand hand, CameraFilter filter) {
    public static void encode(SetCameraFilterPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeVarInt(packet.filter.ordinal());
    }

    public static SetCameraFilterPacket decode(FriendlyByteBuf buffer) {
        return new SetCameraFilterPacket(buffer.readEnum(InteractionHand.class), CameraFilter.byId(buffer.readVarInt()));
    }

    public static void handle(SetCameraFilterPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ItemStack camera = player.getItemInHand(packet.hand);
            if (camera.is(GuaniaoItems.NIKON_D750.get())) {
                CameraSettingsData.setFilter(camera, packet.filter);
            }
        });
        context.setPacketHandled(true);
    }
}
