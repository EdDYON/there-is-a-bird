package EdDYON.guaniao.client.particle;

import EdDYON.guaniao.content.bath.BirdBathVariant;
import EdDYON.guaniao.content.bath.BirdBathBlock;
import EdDYON.guaniao.content.cage.BirdCageBlock;
import EdDYON.guaniao.content.cage.BirdCageVariant;
import EdDYON.guaniao.content.feed.BreadcrumbPileBlock;
import EdDYON.guaniao.content.food.BaggedFriesBlock;
import EdDYON.guaniao.registry.GuaniaoBlocks;
import EdDYON.guaniao.registry.GuaniaoParticleTypes;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;

public final class PlaceableBlockBreakEffects {
    private static final Color[] FRIES = colors(0.98F, 0.69F, 0.18F, 1.00F, 0.84F, 0.31F, 0.82F, 0.46F, 0.10F);
    private static final Color[] RED_PAPER = colors(0.72F, 0.12F, 0.08F, 0.92F, 0.25F, 0.12F, 0.96F, 0.72F, 0.46F);
    private static final Color[] WOOD = colors(0.40F, 0.22F, 0.09F, 0.64F, 0.39F, 0.17F, 0.78F, 0.58F, 0.28F);
    private static final Color[] STONE = colors(0.43F, 0.47F, 0.49F, 0.62F, 0.65F, 0.66F, 0.76F, 0.77F, 0.75F);
    private static final Color[] METAL = colors(0.34F, 0.39F, 0.41F, 0.61F, 0.68F, 0.70F, 0.82F, 0.86F, 0.85F);
    private static final Color[] STRAW = colors(0.45F, 0.29F, 0.10F, 0.70F, 0.51F, 0.20F, 0.86F, 0.69F, 0.32F);
    private static final Color[] CRUMBS = colors(0.59F, 0.39F, 0.13F, 0.82F, 0.61F, 0.24F, 0.95F, 0.79F, 0.43F);
    private static final Color[] LIGHT_STAIN = colors(0.82F, 0.82F, 0.67F, 0.63F, 0.66F, 0.50F, 0.91F, 0.89F, 0.75F);
    private static final Color[] DARK_STAIN = colors(0.29F, 0.25F, 0.14F, 0.43F, 0.38F, 0.21F, 0.56F, 0.51F, 0.30F);

    private PlaceableBlockBreakEffects() {
    }

    public static IClientBlockExtensions baggedFries() {
        return extension(Kind.BAGGED_FRIES);
    }

    public static IClientBlockExtensions birdBath() {
        return extension(Kind.BIRD_BATH);
    }

    public static IClientBlockExtensions birdCage() {
        return extension(Kind.BIRD_CAGE);
    }

    public static IClientBlockExtensions crowNest() {
        return extension(Kind.CROW_NEST);
    }

    public static IClientBlockExtensions breadcrumbs() {
        return extension(Kind.BREADCRUMBS);
    }

    public static IClientBlockExtensions droppingStain() {
        return extension(Kind.DROPPING_STAIN);
    }

    private static IClientBlockExtensions extension(Kind kind) {
        return new IClientBlockExtensions() {
            @Override
            public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine particles) {
                spawnHitEffect(kind, state, level, target.getLocation(), particles);
                return true;
            }

            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine particles) {
                spawnDestroyEffect(kind, state, level, pos, particles);
                return true;
            }
        };
    }

    private static void spawnHitEffect(Kind kind, BlockState state,
                                       Level level, Vec3 hit, ParticleEngine particles) {
        Color[] palette = palette(kind, state);
        RandomSource random = level.getRandom();
        Color color = palette[random.nextInt(palette.length)];
        spawnFleck(particles, color,
                hit.x + gaussian(random, 0.018D),
                hit.y + gaussian(random, 0.018D),
                hit.z + gaussian(random, 0.018D),
                gaussian(random, 0.018D), 0.025D + random.nextDouble() * 0.025D,
                gaussian(random, 0.018D), 0.72F);
    }

    private static void spawnDestroyEffect(Kind kind, BlockState state, Level level, BlockPos pos,
                                           ParticleEngine particles) {
        switch (kind) {
            case BAGGED_FRIES -> spawnBaggedFries(state, level, pos, particles);
            case BIRD_BATH -> spawnBirdBath(bathVariant(state), level, pos, particles);
            case BIRD_CAGE -> spawnFleckCloud(level, pos, particles, WOOD,
                    switch (cageVariant(state)) {
                        case SMALL -> 13;
                        case MEDIUM -> 19;
                        case LARGE -> 25;
                    }, switch (cageVariant(state)) {
                        case SMALL -> 1.0D;
                        case MEDIUM -> 2.0D;
                        case LARGE -> 3.0D;
                    }, 0.92F);
            case CROW_NEST -> {
                spawnFleckCloud(level, pos, particles, STRAW, 15, 0.42D, 0.80F);
                spawnVanillaCloud(level, pos, particles, ParticleTypes.WHITE_ASH, 3, 0.38D);
            }
            case BREADCRUMBS -> {
                int bites = state.hasProperty(BreadcrumbPileBlock.BITES) ? state.getValue(BreadcrumbPileBlock.BITES) : 7;
                spawnFleckCloud(level, pos, particles, CRUMBS, 4 + bites, 0.18D, 0.56F);
            }
            case DROPPING_STAIN -> {
                spawnFleckCloud(level, pos, particles, palette(kind, state), 7, 0.12D, 0.50F);
                spawnVanillaCloud(level, pos, particles, ParticleTypes.SNEEZE, 2, 0.16D);
            }
        }
    }

    private static void spawnBaggedFries(BlockState state, Level level, BlockPos pos, ParticleEngine particles) {
        int remaining = state.hasProperty(BaggedFriesBlock.FRIES) ? state.getValue(BaggedFriesBlock.FRIES) : BaggedFriesBlock.TOTAL_FRIES;
        spawnFleckCloud(level, pos, particles, RED_PAPER, 5, 0.72D, 0.75F);
        if (remaining > 0) {
            spawnFleckCloud(level, pos, particles, FRIES, Math.min(11, 3 + remaining / 2), 0.82D, 0.68F);
        }
    }

    private static void spawnBirdBath(BirdBathVariant variant, Level level, BlockPos pos, ParticleEngine particles) {
        Color[] material = switch (variant) {
            case WOODEN_BIRD_BATH, WOODEN_BIRD_BATH_2 -> WOOD;
            case STONE_BIRD_BATH, STONE_BIRD_BATH_2 -> STONE;
            case BIRD_BATH, BIRD_BATH_2 -> METAL;
        };
        spawnFleckCloud(level, pos, particles, material, 20, 1.55D, 1.0F);
    }

    private static Color[] palette(Kind kind, BlockState state) {
        return switch (kind) {
            case BAGGED_FRIES -> state.hasProperty(BaggedFriesBlock.FRIES) && state.getValue(BaggedFriesBlock.FRIES) == 0
                    ? RED_PAPER : FRIES;
            case BIRD_BATH -> switch (bathVariant(state)) {
                case WOODEN_BIRD_BATH, WOODEN_BIRD_BATH_2 -> WOOD;
                case STONE_BIRD_BATH, STONE_BIRD_BATH_2 -> STONE;
                case BIRD_BATH, BIRD_BATH_2 -> METAL;
            };
            case BIRD_CAGE -> WOOD;
            case CROW_NEST -> STRAW;
            case BREADCRUMBS -> CRUMBS;
            case DROPPING_STAIN -> state.is(GuaniaoBlocks.BIRD_DROPPING_STAIN_DARK.get()) ? DARK_STAIN : LIGHT_STAIN;
        };
    }

    private static BirdBathVariant bathVariant(BlockState state) {
        if (state.getBlock() instanceof BirdBathBlock birdBath) {
            BirdBathVariant variant = birdBath.variant();
            if (variant != null) {
                return variant;
            }
        }
        return BirdBathVariant.STONE_BIRD_BATH;
    }

    private static BirdCageVariant cageVariant(BlockState state) {
        if (state.getBlock() instanceof BirdCageBlock birdCage) {
            BirdCageVariant variant = birdCage.variant();
            if (variant != null) {
                return variant;
            }
        }
        return BirdCageVariant.SMALL;
    }

    private static void spawnFleckCloud(Level level, BlockPos pos, ParticleEngine particles, Color[] palette,
                                        int count, double height, float scale) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.12D + random.nextDouble() * 0.76D;
            double y = pos.getY() + 0.05D + random.nextDouble() * height;
            double z = pos.getZ() + 0.12D + random.nextDouble() * 0.76D;
            double xSpeed = (x - (pos.getX() + 0.5D)) * 0.13D + gaussian(random, 0.025D);
            double ySpeed = 0.045D + random.nextDouble() * 0.10D;
            double zSpeed = (z - (pos.getZ() + 0.5D)) * 0.13D + gaussian(random, 0.025D);
            Color color = palette[random.nextInt(palette.length)];
            spawnFleck(particles, color, x, y, z, xSpeed, ySpeed, zSpeed,
                    scale * (0.78F + random.nextFloat() * 0.44F));
        }
    }

    private static void spawnFleck(ParticleEngine particles, Color color,
                                   double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed, float scale) {
        Particle particle = particles.createParticle(GuaniaoParticleTypes.PLACEABLE_FLECK.get(),
                x, y, z, xSpeed, ySpeed, zSpeed);
        if (particle != null) {
            particle.setColor(color.red, color.green, color.blue);
            particle.scale(scale);
        }
    }

    private static void spawnVanillaCloud(Level level, BlockPos pos, ParticleEngine particles,
                                          ParticleOptions type, int count, double spread) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.5D + gaussian(random, spread * 0.34D);
            double y = pos.getY() + 0.12D + random.nextDouble() * 0.42D;
            double z = pos.getZ() + 0.5D + gaussian(random, spread * 0.34D);
            particles.createParticle(type, x, y, z,
                    gaussian(random, 0.025D), 0.025D + random.nextDouble() * 0.055D, gaussian(random, 0.025D));
        }
    }

    private static double gaussian(RandomSource random, double deviation) {
        return random.nextGaussian() * deviation;
    }

    private static Color[] colors(float... channels) {
        Color[] result = new Color[channels.length / 3];
        for (int index = 0; index < result.length; index++) {
            result[index] = new Color(channels[index * 3], channels[index * 3 + 1], channels[index * 3 + 2]);
        }
        return result;
    }

    private enum Kind {
        BAGGED_FRIES,
        BIRD_BATH,
        BIRD_CAGE,
        CROW_NEST,
        BREADCRUMBS,
        DROPPING_STAIN
    }

    private record Color(float red, float green, float blue) {
    }
}
