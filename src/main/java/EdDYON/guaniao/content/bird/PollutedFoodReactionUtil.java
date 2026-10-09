package EdDYON.guaniao.content.bird;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class PollutedFoodReactionUtil {
    private PollutedFoodReactionUtil() {
    }

    public static Player nearestWitness(Mob bird, double radius) {
        return bird.level().getNearestPlayer(bird, radius);
    }

    public static void lookAtWitness(Mob bird, Player witness) {
        if (witness != null) {
            bird.getLookControl().setLookAt(witness, 35.0F, bird.getMaxHeadXRot());
        }
    }

    public static ItemEntity spit(Mob bird, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        ItemEntity item = bird.spawnAtLocation(stack.copy(), Math.max(0.12F, bird.getBbHeight() * 0.55F));
        if (item != null) {
            Vec3 forward = bird.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
            if (forward.lengthSqr() <= 1.0E-4D) {
                forward = new Vec3(0.0D, 0.0D, 1.0D);
            } else {
                forward = forward.normalize();
            }
            item.setDeltaMovement(bird.getDeltaMovement().scale(0.12D).add(forward.scale(0.22D)).add(0.0D, 0.12D, 0.0D));
            item.setPickUpDelay(20);
        }

        if (bird.level() instanceof ServerLevel level) {
            double y = bird.getY(0.72D);
            level.sendParticles(ParticleTypes.ITEM_SLIME, bird.getX(), y, bird.getZ(), 5, 0.16D, 0.10D, 0.16D, 0.035D);
            level.sendParticles(ParticleTypes.SNEEZE, bird.getX(), y + 0.04D, bird.getZ(), 3, 0.12D, 0.08D, 0.12D, 0.018D);
            bird.playSound(SoundEvents.SLIME_BLOCK_HIT, 0.45F, 0.55F + bird.getRandom().nextFloat() * 0.15F);
        }
        return item;
    }
}
