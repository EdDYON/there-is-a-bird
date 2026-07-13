package EdDYON.guaniao.network;

import EdDYON.guaniao.content.camera.PhotographData;
import EdDYON.guaniao.registry.GuaniaoItems;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
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

public record PhotographTakenPacket(InteractionHand hand, int[] pixels) {
    private static final int MAX_PIXELS = PhotographData.IMAGE_SIZE * PhotographData.IMAGE_SIZE;
    private static final int CAPTURE_COOLDOWN_TICKS = 30;
    private static final DateTimeFormatter PHOTO_NAME_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void encode(PhotographTakenPacket packet, FriendlyByteBuf buffer) {
        if (packet.pixels.length != MAX_PIXELS) {
            throw new IllegalArgumentException("Photograph packet must contain exactly " + MAX_PIXELS + " pixels");
        }
        buffer.writeEnum(packet.hand);
        buffer.writeVarInt(packet.pixels.length);
        for (int pixel : packet.pixels) {
            buffer.writeInt(pixel);
        }
    }

    public static PhotographTakenPacket decode(FriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        int length = buffer.readVarInt();
        if (length != MAX_PIXELS) {
            throw new IllegalArgumentException("Invalid photograph pixel count: " + length);
        }
        int[] pixels = new int[MAX_PIXELS];
        for (int i = 0; i < MAX_PIXELS; i++) {
            pixels[i] = buffer.readInt();
        }
        return new PhotographTakenPacket(hand, pixels);
    }

    public static void handle(PhotographTakenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null || packet.pixels.length != MAX_PIXELS) {
            context.setPacketHandled(true);
            return;
        }

        ItemStack camera = player.getItemInHand(packet.hand);
        if (!camera.is(GuaniaoItems.NIKON_D750.get()) || player.getCooldowns().isOnCooldown(camera.getItem())) {
            context.setPacketHandled(true);
            return;
        }

        player.getCooldowns().addCooldown(camera.getItem(), CAPTURE_COOLDOWN_TICKS);
        ItemStack film = new ItemStack(GuaniaoItems.FILM.get());
        PhotographData.write(film, createPhotoId(player), player.getScoreboardName(), player.getUUID(), player.level().getGameTime(), packet.pixels);
        film.setHoverName(Component.translatable("item.guaniao.film.named", captureDate(), captureLocation(player)));
        if (!player.getInventory().add(film)) {
            player.drop(film, false);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 0.8F, 1.25F);
        context.setPacketHandled(true);
    }

    private static String createPhotoId(ServerPlayer player) {
        return player.getUUID() + "_" + UUID.randomUUID();
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
