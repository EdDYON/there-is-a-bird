package EdDYON.guaniao.content.dropping;

import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class BirdDroppingMessageUtil {
    public static final int MESSAGE_COUNT = 24;
    public static final double NEARBY_MESSAGE_RADIUS = 48.0D;
    private static final double NEARBY_MESSAGE_RADIUS_SQR = NEARBY_MESSAGE_RADIUS * NEARBY_MESSAGE_RADIUS;

    private BirdDroppingMessageUtil() {
    }

    public static String randomKey(String prefix, RandomSource random) {
        return prefix + "." + random.nextInt(MESSAGE_COUNT);
    }

    public static Component randomComponent(String prefix, RandomSource random, Object... args) {
        return Component.translatable(randomKey(prefix, random), args);
    }

    public static Component stableTooltip(String prefix, ItemStack stack) {
        int hash = stack.getHoverName().getString().hashCode();
        if (stack.hasTag()) {
            hash = 31 * hash + stack.getTag().toString().hashCode();
        }
        int index = Math.floorMod(hash, MESSAGE_COUNT);
        return Component.translatable(prefix + "." + index);
    }

    public static void sendNearby(ServerLevel level, Vec3 origin, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(origin) <= NEARBY_MESSAGE_RADIUS_SQR) {
                player.sendSystemMessage(message);
            }
        }
    }

    public static void grant(ServerPlayer player, ResourceLocation id, String criterion) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
