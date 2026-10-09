package EdDYON.guaniao.content.bird.flight;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Maintains temporary, persistent flight loss without freezing ground movement. */
public final class BirdFlightLock {
    private static final String TAG_DISABLED_UNTIL = "GuaniaoFlightDisabledUntil";

    private BirdFlightLock() {
    }

    public static void disableFlight(LivingEntity bird, int durationTicks) {
        long now = bird.level().getGameTime();
        long disabledUntil = now + Math.max(0, durationTicks);
        CompoundTag data = bird.getPersistentData();
        data.putLong(TAG_DISABLED_UNTIL, Math.max(data.getLong(TAG_DISABLED_UNTIL), disabledUntil));
        keepGrounded(bird);
    }

    public static void tickBird(LivingEntity bird) {
        CompoundTag data = bird.getPersistentData();
        long disabledUntil = data.getLong(TAG_DISABLED_UNTIL);
        if (disabledUntil <= 0L) {
            return;
        }
        if (bird.level().getGameTime() >= disabledUntil) {
            data.remove(TAG_DISABLED_UNTIL);
            return;
        }
        keepGrounded(bird);
    }

    private static void keepGrounded(LivingEntity bird) {
        bird.setNoGravity(false);
        if (bird.isInWaterOrBubble()) {
            return;
        }
        Vec3 movement = bird.getDeltaMovement();
        double vertical = bird.onGround() ? Math.min(movement.y, 0.0D) : Math.min(movement.y, -0.08D);
        if (vertical != movement.y) {
            bird.setDeltaMovement(movement.x, vertical, movement.z);
        }
    }
}
