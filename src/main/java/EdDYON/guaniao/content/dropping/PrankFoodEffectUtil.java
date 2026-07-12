package EdDYON.guaniao.content.dropping;

import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class PrankFoodEffectUtil {
    private static final double MESSAGE_RADIUS_SQR = 48.0D * 48.0D;

    private PrankFoodEffectUtil() {
    }

    public static void apply(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 160, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));

        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        RandomSource random = entity.getRandom();
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SLIME_BLOCK_HIT, SoundSource.PLAYERS, 0.75F, 0.7F + random.nextFloat() * 0.25F);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.6F, 0.75F + random.nextFloat() * 0.35F);
        level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY(0.65D), entity.getZ(), 8, 0.35D, 0.25D, 0.35D, 0.02D);
        level.sendParticles(ParticleTypes.SPLASH, entity.getX(), entity.getY(0.6D), entity.getZ(), 5, 0.2D, 0.12D, 0.2D, 0.01D);

        playLaughingBirds(level, entity, random);
        if (entity instanceof Player player) {
            broadcastNearby(level, player);
            player.displayClientMessage(BirdDroppingMessageUtil.randomComponent("message.guaniao.prank_food_review", random), true);
        }
    }

    private static void playLaughingBirds(ServerLevel level, LivingEntity entity, RandomSource random) {
        SoundEvent[] sounds = new SoundEvent[] {
                GuaniaoSoundEvents.SPARROW_AMBIENT.get(),
                GuaniaoSoundEvents.BUDGERIGAR_AMBIENT.get(),
                GuaniaoSoundEvents.PIGEON_AMBIENT.get()
        };
        int count = 2 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            SoundEvent sound = sounds[random.nextInt(sounds.length)];
            double x = entity.getX() + (random.nextDouble() - 0.5D) * 4.0D;
            double y = entity.getY(0.6D) + random.nextDouble() * 1.4D;
            double z = entity.getZ() + (random.nextDouble() - 0.5D) * 4.0D;
            level.playSound(null, x, y, z, sound, SoundSource.AMBIENT, 0.65F, 0.85F + random.nextFloat() * 0.45F);
        }
    }

    private static void broadcastNearby(ServerLevel level, Player eater) {
        Component message = Component.translatable("message.guaniao.prank_food_eaten", eater.getDisplayName());
        for (ServerPlayer nearby : level.players()) {
            if (nearby.distanceToSqr(eater) <= MESSAGE_RADIUS_SQR) {
                nearby.sendSystemMessage(message);
            }
        }
    }
}
