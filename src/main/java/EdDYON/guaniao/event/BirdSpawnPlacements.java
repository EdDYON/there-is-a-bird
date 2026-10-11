package EdDYON.guaniao.event;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;

final class BirdSpawnPlacements {
    static final SpawnPlacements.Type ON_GROUND_OR_LEAVES = SpawnPlacements.Type.create(
            "GUANIAO_ON_GROUND_OR_LEAVES", BirdSpawnPlacements::canSpawnOnGroundOrLeaves);

    private BirdSpawnPlacements() {
    }

    private static boolean canSpawnOnGroundOrLeaves(LevelReader level, BlockPos pos, EntityType<?> type) {
        if (type == null || !level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)) {
            return false;
        }
        if (NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, level, pos, type)) {
            return true;
        }

        BlockPos below = pos.below();
        BlockPos above = pos.above();
        if (level.isOutsideBuildHeight(below) || level.isOutsideBuildHeight(above)
                || !level.getBlockState(below).is(BlockTags.LEAVES)) {
            return false;
        }
        // Vanilla leaves only permit ocelots and parrots. Canopy birds retain the
        // same empty-body and head-space checks while accepting their intended footing.
        return NaturalSpawner.isValidEmptySpawnBlock(level, pos, level.getBlockState(pos),
                level.getFluidState(pos), type)
                && NaturalSpawner.isValidEmptySpawnBlock(level, above, level.getBlockState(above),
                level.getFluidState(above), type);
    }
}
