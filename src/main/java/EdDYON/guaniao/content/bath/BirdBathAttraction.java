package EdDYON.guaniao.content.bath;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class BirdBathAttraction {
    private static final double TOP_USE_Y_OFFSET = 1.42D;
    private static final double TOP_STAND_Y_OFFSET = 1.52D;
    private static final double EDGE_APPROACH_DISTANCE = 1.35D;

    private BirdBathAttraction() {
    }

    public static Optional<BirdBathBlockEntity> findNearbyUsableBath(Level level, BlockPos origin, double radius, Predicate<BirdBathBlockEntity> predicate) {
        int range = (int)Math.ceil(radius);
        double bestDistance = radius * radius;
        BirdBathBlockEntity best = null;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = -range; x <= range; x++) {
            for (int y = -Math.min(4, range); y <= Math.min(4, range); y++) {
                for (int z = -range; z <= range; z++) {
                    mutable.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    double distance = mutable.distSqr(origin);
                    if (distance > bestDistance) {
                        continue;
                    }
                    BlockEntity blockEntity = level.getBlockEntity(mutable);
                    if (blockEntity instanceof BirdBathBlockEntity birdBath && predicate.test(birdBath)) {
                        best = birdBath;
                        bestDistance = distance;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    public static boolean isAttractiveToNightHeron(BirdBathBlockEntity bath) {
        return bath != null && (bath.hasFoodForBird(BirdBathFoodPreference.FISH) || bath.hasUsableWater());
    }

    public static boolean isAttractiveToSmallSeedBird(BirdBathBlockEntity bath) {
        return bath != null && (bath.hasFoodForBird(BirdBathFoodPreference.BREAD) || bath.hasUsableWater());
    }

    public static boolean isAttractiveToColumbid(BirdBathBlockEntity bath) {
        return bath != null && (bath.hasFoodForBird(BirdBathFoodPreference.BREAD) || bath.hasUsableWater());
    }

    public static boolean isAttractiveToBudgerigar(BirdBathBlockEntity bath) {
        return bath != null && (bath.hasFoodForBird(BirdBathFoodPreference.BREAD) || bath.hasUsableWater());
    }

    public static boolean isAttractiveToCrow(BirdBathBlockEntity bath) {
        return bath != null
                && (bath.hasFoodForBird(BirdBathFoodPreference.FISH)
                || bath.hasFoodForBird(BirdBathFoodPreference.MEAT)
                || bath.hasFoodForBird(BirdBathFoodPreference.BREAD)
                || bath.hasUsableWater());
    }

    public static boolean isAttractiveToWaterBird(BirdBathBlockEntity bath) {
        return bath != null && bath.hasUsableWater();
    }

    public static Vec3 topUsePosition(BirdBathBlockEntity bath) {
        BlockPos pos = bath.getBlockPos();
        return new Vec3(pos.getX() + 0.5D, pos.getY() + TOP_USE_Y_OFFSET, pos.getZ() + 0.5D);
    }

    public static Vec3 topStandPosition(BirdBathBlockEntity bath) {
        BlockPos pos = bath.getBlockPos();
        return new Vec3(pos.getX() + 0.5D, pos.getY() + TOP_STAND_Y_OFFSET, pos.getZ() + 0.5D);
    }

    public static Vec3 edgeApproachPosition(BirdBathBlockEntity bath, Vec3 birdPosition) {
        Vec3 center = topUsePosition(bath);
        Vec3 horizontal = new Vec3(birdPosition.x - center.x, 0.0D, birdPosition.z - center.z);
        if (horizontal.lengthSqr() <= 1.0E-4D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        BlockPos pos = bath.getBlockPos();
        return new Vec3(center.x + horizontal.x * EDGE_APPROACH_DISTANCE, pos.getY() + 0.05D, center.z + horizontal.z * EDGE_APPROACH_DISTANCE);
    }

    public static boolean consumeServingForBird(BirdBathBlockEntity bath) {
        return bath != null && bath.consumeOneServing();
    }

    public static boolean tryClaimUse(BirdBathBlockEntity bath, Entity bird, int ticks) {
        if (bath == null || bird == null) {
            return false;
        }
        UUID uuid = bird.getUUID();
        return bath.tryClaimUse(uuid, ticks);
    }

    public static int getCompetitionPriority(Entity bird) {
        if (bird == null) {
            return 0;
        }
        String key = bird.getType().getDescriptionId();
        if (key.contains("night_heron")) {
            return 4;
        }
        if (key.contains("pigeon") || key.contains("dove") || key.contains("collared")) {
            return 3;
        }
        if (key.contains("crow")) {
            return 3;
        }
        if (key.contains("macaw")) {
            return 3;
        }
        if (key.contains("budgerigar") || key.contains("parakeet") || key.contains("cockatiel")) {
            return 2;
        }
        if (key.contains("sparrow")) {
            return 1;
        }
        return 1;
    }
}
