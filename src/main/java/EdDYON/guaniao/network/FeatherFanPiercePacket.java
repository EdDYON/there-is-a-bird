package EdDYON.guaniao.network;

import EdDYON.guaniao.content.fan.FeatherFanItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;


public final class FeatherFanPiercePacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FeatherFanPiercePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "feather_fan_pierce"));

    @Override
    public CustomPacketPayload.Type<FeatherFanPiercePacket> type() { return TYPE; }

    public static void encode(FeatherFanPiercePacket packet, FriendlyByteBuf buffer) {
    }

    public static FeatherFanPiercePacket decode(FriendlyByteBuf buffer) {
        return new FeatherFanPiercePacket();
    }

    public static void handle(FeatherFanPiercePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null);
            if (player != null && player.getUseItem().getItem() instanceof FeatherFanItem fan) {
                fan.tryLaunchPiercing(player);
            }
        });
    }
}
