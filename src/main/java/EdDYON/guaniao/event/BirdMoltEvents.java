package EdDYON.guaniao.event;

import EdDYON.guaniao.content.bird.BirdAmbientDropControl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/**
 * Molting: live birds occasionally shed a feather of their species' color where they
 * stand. Special birds never molt — the rainbow mutation sheds nothing naturally.
 */
public final class BirdMoltEvents {
    private static final String TAG_COOLDOWN = "GuaniaoMoltCooldown";
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int MOLT_MIN_TICKS = 12000;
    private static final int MOLT_MAX_TICKS = 24000;
    private static final int RETRY_MIN_TICKS = 2400;
    private static final int RETRY_MAX_TICKS = 4800;
    private static final int MIN_EXISTING_AGE_TICKS = 200;

    private BirdMoltEvents() {
    }

    public static void tickBird(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || entity.tickCount < MIN_EXISTING_AGE_TICKS) {
            return;
        }
        if ((entity.tickCount + entity.getId()) % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        if (!BirdAmbientDropControl.hasNearbyPlayer(level, entity)) {
            return;
        }

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG_COOLDOWN)) {
            data.putInt(TAG_COOLDOWN, randomBetween(entity.level().random, MOLT_MIN_TICKS, MOLT_MAX_TICKS));
            return;
        }

        int cooldown = Math.max(0, data.getInt(TAG_COOLDOWN) - CHECK_INTERVAL_TICKS);
        if (cooldown > 0) {
            data.putInt(TAG_COOLDOWN, cooldown);
            return;
        }

        if (tryMolt(entity)) {
            data.putInt(TAG_COOLDOWN, randomBetween(entity.level().random, MOLT_MIN_TICKS, MOLT_MAX_TICKS));
        } else {
            data.putInt(TAG_COOLDOWN, randomBetween(entity.level().random, RETRY_MIN_TICKS, RETRY_MAX_TICKS));
        }
    }

    private static boolean tryMolt(LivingEntity bird) {
        if (bird.isDeadOrDying() || bird.isRemoved() || !bird.isAlive() || bird.isInWaterOrBubble()) {
            return false;
        }
        if (!(bird.level() instanceof ServerLevel level)
                || !BirdFeatherEvents.canSpawnNaturalFeather(level, bird)) {
            return false;
        }
        Item feather = BirdFeatherEvents.randomFeatherFor(bird);
        if (feather == null) {
            return false;
        }
        BirdFeatherEvents.spawnFeather(bird, feather, 1);
        return true;
    }

    private static int randomBetween(RandomSource random, int minInclusive, int maxInclusive) {
        return minInclusive + random.nextInt(Math.max(1, maxInclusive - minInclusive + 1));
    }
}
