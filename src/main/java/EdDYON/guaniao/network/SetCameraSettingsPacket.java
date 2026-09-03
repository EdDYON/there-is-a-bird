package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.CameraAperture;
import EdDYON.guaniao.content.camera.CameraFilter;
import EdDYON.guaniao.content.camera.CameraFocusMode;
import EdDYON.guaniao.content.camera.CameraLens;
import EdDYON.guaniao.content.camera.CameraSettingsData;
import EdDYON.guaniao.content.camera.CameraShootingMode;
import EdDYON.guaniao.content.camera.CameraState;
import EdDYON.guaniao.registry.GuaniaoItems;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record SetCameraSettingsPacket(InteractionHand hand, CameraState state) {
    public static void encode(SetCameraSettingsPacket packet, FriendlyByteBuf buffer) {
        CameraState state = packet.state;
        buffer.writeEnum(packet.hand);
        buffer.writeVarInt(state.filter().id());
        buffer.writeVarInt(state.lens().id());
        buffer.writeVarInt(state.shootingMode().id());
        buffer.writeDouble(state.focalLength());
        buffer.writeVarInt(state.aperture().id());
        buffer.writeVarInt(state.focusMode().id());
        buffer.writeDouble(state.focusDistance());
    }

    public static SetCameraSettingsPacket decode(FriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        CameraState state = new CameraState(
                CameraFilter.byId(buffer.readVarInt()),
                CameraLens.byId(buffer.readVarInt()),
                CameraShootingMode.byId(buffer.readVarInt()),
                buffer.readDouble(),
                CameraAperture.byId(buffer.readVarInt()),
                CameraFocusMode.byId(buffer.readVarInt()),
                buffer.readDouble()
        );
        return new SetCameraSettingsPacket(hand, state);
    }

    public static void handle(SetCameraSettingsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ItemStack camera = player.getItemInHand(packet.hand);
            if (camera.is(GuaniaoItems.NIKON_D750.get())) {
                CameraSettingsData.setState(camera, packet.state);
            }
        });
        context.setPacketHandled(true);
    }
}
