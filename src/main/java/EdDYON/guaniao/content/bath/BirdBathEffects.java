package EdDYON.guaniao.content.bath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class BirdBathEffects {
    private BirdBathEffects() {
    }

    public static void waterAdded(Level level, BlockPos pos, SoundEvent sound) {
        play(level, pos, sound, 0.8F, 1.0F);
        splash(level, pos, 10);
    }

    public static void foodAdded(Level level, BlockPos pos, BirdBathContentType type) {
        SoundEvent sound = switch (type) {
            case FISH, MEAT -> SoundEvents.SLIME_BLOCK_PLACE;
            case BREAD -> SoundEvents.SAND_PLACE;
            default -> SoundEvents.ITEM_FRAME_ADD_ITEM;
        };
        play(level, pos, sound, 0.7F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(type == BirdBathContentType.FISH ? ParticleTypes.SPLASH : ParticleTypes.POOF,
                    pos.getX() + 0.5D, pos.getY() + 0.95D, pos.getZ() + 0.5D,
                    5, 0.18D, 0.08D, 0.18D, 0.02D);
        }
    }

    public static void cleaned(Level level, BlockPos pos, BirdBathCleanliness previous) {
        play(level, pos, SoundEvents.SAND_BREAK, 0.65F, 1.15F);
        if (level instanceof ServerLevel serverLevel) {
            int count = Math.max(3, previous.particleIntensity());
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                    count, 0.2D, 0.08D, 0.2D, 0.015D);
        }
    }

    public static void spoiledCleared(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.SLIME_BLOCK_BREAK, 0.75F, 0.9F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                    8, 0.18D, 0.08D, 0.18D, 0.01D);
        }
    }

    public static void contentCleared(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, 0.7F, 1.1F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF,
                    pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                    4, 0.14D, 0.08D, 0.14D, 0.01D);
        }
    }

    public static void evaporated(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.FIRE_EXTINGUISH, 0.25F, 1.7F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5D, pos.getY() + 0.95D, pos.getZ() + 0.5D,
                    3, 0.14D, 0.03D, 0.14D, 0.004D);
        }
    }

    public static void froze(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.GLASS_PLACE, 0.55F, 1.35F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    pos.getX() + 0.5D, pos.getY() + 0.95D, pos.getZ() + 0.5D,
                    6, 0.18D, 0.08D, 0.18D, 0.01D);
        }
    }

    public static void melted(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.GLASS_BREAK, 0.45F, 1.4F);
        splash(level, pos, 5);
    }

    public static void spoiled(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.SLIME_BLOCK_BREAK, 0.45F, 0.8F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                    6, 0.18D, 0.08D, 0.18D, 0.01D);
        }
    }

    public static void birdUsed(Level level, BlockPos pos, boolean water) {
        if (water) {
            splash(level, pos, 4);
        } else if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF,
                    pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                    3, 0.12D, 0.05D, 0.12D, 0.01D);
        }
    }

    public static void idleDirty(Level level, BlockPos pos, BirdBathCleanliness cleanliness, boolean spoiled) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        RandomSource random = serverLevel.random;
        if (spoiled && random.nextInt(8) == 0) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                    1, 0.12D, 0.04D, 0.12D, 0.002D);
        } else if (cleanliness == BirdBathCleanliness.FILTHY && random.nextInt(4) == 0) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D,
                    1, 0.12D, 0.04D, 0.12D, 0.002D);
        }
    }

    private static void splash(Level level, BlockPos pos, int count) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    pos.getX() + 0.5D, pos.getY() + 0.95D, pos.getZ() + 0.5D,
                    count, 0.2D, 0.08D, 0.2D, 0.04D);
        }
    }

    private static void play(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }
}
