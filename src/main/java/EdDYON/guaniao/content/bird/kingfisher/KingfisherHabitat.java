package EdDYON.guaniao.content.bird.kingfisher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Bounded, loaded-only shoreline checks; water geometry stands in for food habitat. */
public final class KingfisherHabitat {
    private static final int MAX_WATER_DISTANCE_SQR = 100;
    private static final List<BlockPos> WATER_COLUMNS = waterColumns();

    private KingfisherHabitat() { }

    public static boolean canSpawn(ServerLevelAccessor level, MobSpawnType reason, BlockPos pos) {
        if (reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND) return true;
        if (!loaded(level, pos) || !loaded(level, pos.below()) || !loaded(level, pos.above())) return false;
        if (level.getRawBrightness(pos, 0) < 8 || !clearAir(level, pos) || !clearAir(level, pos.above())) return false;
        BlockState support = level.getBlockState(pos.below());
        if (!safeShoreSupport(level, pos.below(), support)) return false;
        // Natural banks and perches beside connected open water provide the habitat
        // for the surface-skimming display; no living fish entity is required.
        BlockPos water = findWater(level, pos, Vec3.atBottomCenterOf(pos).add(0, 0.25D, 0));
        return water != null;
    }

    @Nullable public static BlockPos findWater(Level level, BlockPos center) {
        return findWater(level, center, null);
    }

    @Nullable private static BlockPos findWater(LevelReader level, BlockPos center, @Nullable Vec3 view) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // 97 columns at most, eight heights each. Close water is sampled on every
        // block so a narrow river is not lost to the parity of a two-block grid.
        for (BlockPos offset : WATER_COLUMNS) {
            for (int dy = 1; dy >= -6; dy--) {
                cursor.set(center.getX() + offset.getX(), center.getY() + dy, center.getZ() + offset.getZ());
                if (!isOpenWater(level, cursor)) continue;
                BlockPos water = cursor.immutable();
                if (view == null || hasWaterView(level, view, water)) return water;
            }
        }
        return null;
    }

    /** Five connected source-water cells with room to enter and leave the surface. */
    public static boolean isOpenWater(LevelReader level, BlockPos surface) {
        if (!openSurface(level, surface)) return false;
        boolean[] visited = new boolean[9];
        int[] queue = new int[9];
        visited[4] = true;
        queue[0] = 4;
        int read = 0, size = 1;
        while (read < size) {
            int index = queue[read++];
            int x = index % 3, z = index / 3;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int nx = x + direction.getStepX(), nz = z + direction.getStepZ();
                if (nx < 0 || nx > 2 || nz < 0 || nz > 2) continue;
                int next = nx + nz * 3;
                if (visited[next]) continue;
                visited[next] = true;
                if (!openSurface(level, surface.offset(nx - 1, 0, nz - 1))) continue;
                queue[size++] = next;
                if (size >= 5) return true;
            }
        }
        return false;
    }

    public static boolean hasWaterView(Level level, Vec3 perchEye, BlockPos water) {
        return hasWaterView((LevelReader) level, perchEye, water);
    }

    private static boolean hasWaterView(LevelReader level, Vec3 perchEye, BlockPos water) {
        double dx = perchEye.x - water.getX() - 0.5D;
        double dz = perchEye.z - water.getZ() - 0.5D;
        double height = perchEye.y - water.getY() - 1.0D;
        if (dx * dx + dz * dz > MAX_WATER_DISTANCE_SQR || height < 0 || height > 5.0D
                || !openSurface(level, water)) return false;
        Vec3 surface = Vec3.atCenterOf(water).add(0, 0.55D, 0);
        if (!loadedLine(level, perchEye, surface)) return false;
        return level.clip(new ClipContext(perchEye, surface, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, null)).getType() == HitResult.Type.MISS;
    }

    private static boolean openSurface(LevelReader level, BlockPos pos) {
        if (!loaded(level, pos) || !loaded(level, pos.above(2))) return false;
        BlockState state = level.getBlockState(pos);
        // Pure source water only: waterlogged fences/slabs and waterfall runoff
        // cannot masquerade as a pond just because their fluid tag contains water.
        return state.is(Blocks.WATER) && state.getFluidState().is(FluidTags.WATER)
                && state.getFluidState().isSource() && clearAir(level, pos.above())
                && clearAir(level, pos.above(2));
    }

    private static boolean clearAir(LevelReader level, BlockPos pos) {
        if (!loaded(level, pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && !state.is(Blocks.FIRE) && !state.is(Blocks.SOUL_FIRE)
                && !state.is(Blocks.POWDER_SNOW) && !state.is(Blocks.SWEET_BERRY_BUSH)
                && !state.is(Blocks.WITHER_ROSE) && state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean safeShoreSupport(LevelReader level, BlockPos pos, BlockState state) {
        if (!state.getFluidState().isEmpty()) return false;
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.FENCES)
                || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO)) return true;
        boolean naturalBank = state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY) || state.is(Blocks.GRAVEL) || state.is(Blocks.MOSS_BLOCK);
        return naturalBank && state.isFaceSturdy(level, pos, Direction.UP);
    }

    private static boolean loaded(LevelReader level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return false;
        int x = pos.getX() >> 4, z = pos.getZ() >> 4;
        if (level instanceof ServerLevel server) return server.getChunkSource().getChunkNow(x, z) != null;
        if (level instanceof WorldGenRegion region) {
            // hasChunk() may include neighbouring generation dependencies which
            // cannot satisfy a block read at this stage. Stay in the centre chunk.
            return x == region.getCenter().x && z == region.getCenter().z && region.hasChunk(x, z);
        }
        return level.hasChunk(x, z);
    }

    private static boolean loadedLine(LevelReader level, Vec3 start, Vec3 end) {
        if (!Double.isFinite(start.x) || !Double.isFinite(start.y) || !Double.isFinite(start.z)) return false;
        BlockPos a = BlockPos.containing(start), b = BlockPos.containing(end);
        if (!loaded(level, a) || !loaded(level, b)) return false;
        for (int x = Math.min(a.getX(), b.getX()) >> 4; x <= (Math.max(a.getX(), b.getX()) >> 4); x++)
            for (int z = Math.min(a.getZ(), b.getZ()) >> 4; z <= (Math.max(a.getZ(), b.getZ()) >> 4); z++)
                if (!loaded(level, new BlockPos(x << 4, b.getY(), z << 4))) return false;
        return true;
    }

    private static List<BlockPos> waterColumns() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
            int radius = Math.max(Math.abs(x), Math.abs(z));
            boolean outerProbe = (radius == 6 || radius == 8 || radius == 10)
                    && (x == 0 || z == 0 || Math.abs(x) == Math.abs(z));
            if (x * x + z * z <= MAX_WATER_DISTANCE_SQR && (radius <= 4 || outerProbe))
                offsets.add(new BlockPos(x, 0, z));
        }
        offsets.sort(Comparator.comparingInt(pos -> pos.getX() * pos.getX() + pos.getZ() * pos.getZ()));
        return List.copyOf(offsets);
    }
}
