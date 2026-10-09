package EdDYON.guaniao.content.bird.nightheron;

import EdDYON.guaniao.content.bird.BirdTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Loaded-world checks for safe delivery, navigation and natural foraging. */
final class NightHeronFishing {
    private NightHeronFishing() { }

    static boolean isPetPrey(AbstractFish fish) {
        return fish.isAlive() && !fish.isRemoved() && !fish.hasCustomName() && !fish.fromBucket()
                && !fish.isPassenger() && fish.getType().is(BirdTags.PET_NIGHT_HERON_PREY)
                && (fish.getType() == EntityType.COD || fish.getType() == EntityType.SALMON);
    }

    static boolean safeStand(NightHeronEntity bird, BlockPos pos) {
        if (!NightHeronLandingSelector.isSafeLanding(bird.level(), pos)) return false;
        for (BlockPos at : new BlockPos[]{pos.below(), pos, pos.above()}) {
            var state = bird.level().getBlockState(at);
            if (!bird.level().getFluidState(at).isEmpty() || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                    || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.POWDER_SNOW)
                    || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)) return false;
        }
        Vec3 point = Vec3.atBottomCenterOf(pos);
        return bird.level().noCollision(bird, bird.getBoundingBox().move(point.subtract(bird.position())));
    }

    static boolean nearSurface(AbstractFish fish) {
        return fish.isInWater() && NightHeronEntity.canReadChunk(fish.level(), fish.blockPosition())
                && !fish.level().getFluidState(fish.blockPosition().above()).is(FluidTags.WATER);
    }

    static boolean canReachFish(NightHeronEntity bird, AbstractFish fish) {
        return fish != null && bird.onGround() && nearSurface(fish)
                && bird.distanceToSqr(fish) <= 4.41D && fish.getY() >= bird.getY() - 1.5D
                && fish.getY() <= bird.getY() + 0.25D
                && safeStand(bird, bird.blockPosition())
                && NightHeronShorelineCache.isSafeShorelinePosition(bird.level(), bird.blockPosition())
                && bird.hasLineOfSight(fish);
    }

    static boolean loadedPathRegion(NightHeronEntity bird) {
        int radius = (int)(bird.getAttributeValue(Attributes.FOLLOW_RANGE) + 8);
        if (radius < 0 || radius > 128) return false;
        BlockPos p = bird.blockPosition();
        for (int x = SectionPos.blockToSectionCoord(p.getX() - radius); x <= SectionPos.blockToSectionCoord(p.getX() + radius); x++)
            for (int z = SectionPos.blockToSectionCoord(p.getZ() - radius); z <= SectionPos.blockToSectionCoord(p.getZ() + radius); z++)
                if (!bird.level().hasChunk(x, z)) return false;
        return true;
    }

    static Path groundPath(NightHeronEntity bird, BlockPos target) {
        if (!safeStand(bird, target) || !loadedPathRegion(bird)) return null;
        Path path = bird.getNavigation().createPath(target, 0);
        return path != null && path.canReach() ? path : null;
    }

    static BlockPos standNear(NightHeronEntity bird, Vec3 position, int radius) {
        return standNear(bird, position, radius, 2);
    }

    static BlockPos landingBelow(NightHeronEntity bird, Vec3 position, int radius) {
        return standNear(bird, position, radius, 12);
    }

    private static BlockPos standNear(NightHeronEntity bird, Vec3 position, int radius, int below) {
        BlockPos center = BlockPos.containing(position);
        BlockPos best = null;
        double score = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -below, -radius), center.offset(radius, 2, radius))) {
            double distance = Vec3.atBottomCenterOf(pos).distanceToSqr(position);
            if (distance < score && safeStand(bird, pos)) { best = pos.immutable(); score = distance; }
        }
        return best;
    }
}
