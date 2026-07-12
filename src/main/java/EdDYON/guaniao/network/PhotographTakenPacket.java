package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.PhotographData;
import EdDYON.guaniao.registry.GuaniaoItems;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record PhotographTakenPacket(InteractionHand hand, String photoId, int[] pixels) {
    private static final int MAX_PIXELS = PhotographData.IMAGE_SIZE * PhotographData.IMAGE_SIZE;
    private static final DateTimeFormatter PHOTO_NAME_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void encode(PhotographTakenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeUtf(packet.photoId, 96);
        buffer.writeVarInt(packet.pixels.length);
        for (int pixel : packet.pixels) {
            buffer.writeInt(pixel);
        }
    }

    public static PhotographTakenPacket decode(FriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        String photoId = buffer.readUtf(96);
        int length = buffer.readVarInt();
        int safeLength = Math.min(length, MAX_PIXELS);
        int[] pixels = new int[safeLength];
        for (int i = 0; i < length; i++) {
            int pixel = buffer.readInt();
            if (i < safeLength) {
                pixels[i] = pixel;
            }
        }
        return new PhotographTakenPacket(hand, photoId, pixels);
    }

    public static void handle(PhotographTakenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && packet.pixels.length == MAX_PIXELS) {
            ItemStack camera = player.getItemInHand(packet.hand);
            if (camera.is(GuaniaoItems.NIKON_D750.get())) {
                ItemStack film = new ItemStack(GuaniaoItems.FILM.get());
                PhotographData.write(film, packet.photoId, player.getScoreboardName(), player.getUUID(), player.level().getGameTime(), packet.pixels);
                film.setHoverName(Component.translatable("item.guaniao.film.named", captureDate(), captureLocation(player)));
                if (!player.getInventory().add(film)) {
                    player.drop(film, false);
                }
                player.getCooldowns().addCooldown(camera.getItem(), 30);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 0.8F, 1.25F);
            }
        }
        context.setPacketHandled(true);
    }

    private static String captureDate() {
        return LocalDateTime.now().format(PHOTO_NAME_DATE);
    }

    private static String captureLocation(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        String dimension = player.level().dimension().location().getPath();
        return dimension + " " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
