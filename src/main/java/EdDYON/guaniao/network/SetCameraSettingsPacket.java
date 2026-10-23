package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.CameraAperture;
import EdDYON.guaniao.content.camera.CameraFilter;
import EdDYON.guaniao.content.camera.CameraFocusMode;
import EdDYON.guaniao.content.camera.CameraLens;
import EdDYON.guaniao.content.camera.CameraSettingsData;
import EdDYON.guaniao.content.camera.CameraShootingMode;
import EdDYON.guaniao.content.camera.CameraState;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetCameraSettingsPacket(InteractionHand hand, CameraState state) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetCameraSettingsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "set_camera_settings"));

    @Override
    public CustomPacketPayload.Type<SetCameraSettingsPacket> type() { return TYPE; }

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

    public static void handle(SetCameraSettingsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null);
            if (player == null) {
                return;
            }
            if (!Double.isFinite(packet.state.focalLength()) || !Double.isFinite(packet.state.focusDistance())) {
                return;
            }
            ItemStack camera = player.getItemInHand(packet.hand);
            if (camera.is(GuaniaoItems.NIKON_D750.get())) {
                CameraSettingsData.setState(camera, packet.state);
            }
        });
    }
}
