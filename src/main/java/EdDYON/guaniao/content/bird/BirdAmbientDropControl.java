package EdDYON.guaniao.content.bird;

import EdDYON.guaniao.config.BirdConfigManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/** Shared lifecycle rules for decorative droppings and naturally shed feathers. */
public final class BirdAmbientDropControl {
    public static final double PLAYER_ACTIVITY_RADIUS = 64.0D;
    public static final double LOCAL_CAP_RADIUS = 16.0D;
    public static final int MIN_LIFETIME_TICKS = 20 * 60 * 5;
    public static final int MAX_LIFETIME_TICKS = 20 * 60 * 8;
    private static final int MIN_FEATHER_LIFETIME_TICKS = 20 * 60 * 10;
    private static final int MAX_FEATHER_LIFETIME_TICKS = 20 * 60 * 15;
    public static final int HARD_MAX_DROPPINGS_NEARBY = 16;
    public static final int MAX_NATURAL_FEATHERS_NEARBY = 6;

    private BirdAmbientDropControl() {
    }

    public static boolean hasNearbyPlayer(ServerLevel level, Entity source) {
        return level.hasNearbyAlivePlayer(
                source.getX(), source.getY(), source.getZ(), PLAYER_ACTIVITY_RADIUS
        );
    }

    public static int randomLifetime(RandomSource random) {
        int min = BirdConfigManager.droppingLifetimeMinTicks();
        return min + random.nextInt(BirdConfigManager.droppingLifetimeMaxTicks() - min + 1);
    }

    /** Natural feathers keep their existing lifetime independently of dropping settings. */
    public static void applyRandomLifetime(ItemEntity itemEntity) {
        itemEntity.lifespan = MIN_FEATHER_LIFETIME_TICKS
                + itemEntity.level().random.nextInt(MAX_FEATHER_LIFETIME_TICKS - MIN_FEATHER_LIFETIME_TICKS + 1);
    }

    public static void applyDroppingLifetime(ItemEntity itemEntity) {
        itemEntity.lifespan = randomLifetime(itemEntity.level().random);
    }
}
