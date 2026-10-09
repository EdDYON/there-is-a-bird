package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.CameraFilter;
import EdDYON.guaniao.content.camera.CameraSettingsData;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetCameraFilterPacket(InteractionHand hand, CameraFilter filter) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetCameraFilterPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "set_camera_filter"));

    @Override
    public CustomPacketPayload.Type<SetCameraFilterPacket> type() { return TYPE; }

    public static void encode(SetCameraFilterPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeVarInt(packet.filter.id());
    }

    public static SetCameraFilterPacket decode(FriendlyByteBuf buffer) {
        return new SetCameraFilterPacket(buffer.readEnum(InteractionHand.class), CameraFilter.byId(buffer.readVarInt()));
    }

    public static void handle(SetCameraFilterPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null);
            if (player == null) {
                return;
            }
            ItemStack camera = player.getItemInHand(packet.hand);
            if (camera.is(GuaniaoItems.NIKON_D750.get())) {
                CameraSettingsData.setFilter(camera, packet.filter);
            }
        });
    }
}
