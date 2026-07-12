package EdDYON.guaniao.content.dropping;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class PollutedCakeData extends SavedData {
    private static final String DATA_NAME = "guaniao_polluted_cakes";
    private static final String TAG_POSITIONS = "Positions";
    private static final int PARTICLE_INTERVAL_TICKS = 10;
    private static final DustParticleOptions DIRTY_BROWN_DUST = new DustParticleOptions(new Vector3f(0.28F, 0.18F, 0.07F), 0.80F);
    private static final DustParticleOptions SICK_GREEN_DUST = new DustParticleOptions(new Vector3f(0.20F, 0.40F, 0.08F), 0.70F);

    private final Set<Long> positions = new HashSet<>();

    public static PollutedCakeData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PollutedCakeData::load, PollutedCakeData::new, DATA_NAME);
    }

    public static PollutedCakeData load(CompoundTag tag) {
        PollutedCakeData data = new PollutedCakeData();
        for (long packedPos : tag.getLongArray(TAG_POSITIONS)) {
            data.positions.add(packedPos);
        }
        return data;
    }

    public void pollute(ServerLevel level, BlockPos pos) {
        if (this.positions.add(pos.asLong())) {
            this.setDirty();
        }
        spawnBurst(level, cakeSurface(level.getBlockState(pos), pos));
    }

    public boolean isPolluted(BlockPos pos) {
        return this.positions.contains(pos.asLong());
    }

    public void tick(ServerLevel level) {
        boolean changed = false;
        Iterator<Long> iterator = this.positions.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = BlockPos.of(iterator.next());
            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof CakeBlock)) {
                iterator.remove();
                changed = true;
                continue;
            }

            if (Math.floorMod(level.getGameTime() + pos.asLong(), PARTICLE_INTERVAL_TICKS) == 0) {
                spawnAmbient(level, cakeSurface(state, pos));
            }
        }
        if (changed) {
            this.setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        long[] packedPositions = new long[this.positions.size()];
        int index = 0;
        for (long packedPos : this.positions) {
            packedPositions[index++] = packedPos;
        }
        tag.putLongArray(TAG_POSITIONS, packedPositions);
        return tag;
    }

    private static Vec3 cakeSurface(BlockState state, BlockPos pos) {
        int bites = state.hasProperty(CakeBlock.BITES) ? state.getValue(CakeBlock.BITES) : 0;
        return new Vec3(pos.getX() + 0.5D + bites * 0.0625D, pos.getY() + 0.58D, pos.getZ() + 0.5D);
    }

    private static void spawnAmbient(ServerLevel level, Vec3 position) {
        level.sendParticles(ParticleTypes.ITEM_SLIME, position.x, position.y, position.z, 1, 0.18D, 0.035D, 0.18D, 0.012D);
        level.sendParticles(ParticleTypes.COMPOSTER, position.x, position.y + 0.03D, position.z, 1, 0.17D, 0.04D, 0.17D, 0.015D);
        level.sendParticles(DIRTY_BROWN_DUST, position.x, position.y + 0.07D, position.z, 2, 0.18D, 0.05D, 0.18D, 0.008D);
        if (level.random.nextBoolean()) {
            level.sendParticles(SICK_GREEN_DUST, position.x, position.y + 0.10D, position.z, 1, 0.14D, 0.05D, 0.14D, 0.006D);
        }
        if (level.random.nextInt(8) == 0) {
            level.sendParticles(ParticleTypes.SNEEZE, position.x, position.y + 0.15D, position.z, 1, 0.08D, 0.03D, 0.08D, 0.005D);
        }
    }

    private static void spawnBurst(ServerLevel level, Vec3 position) {
        level.sendParticles(ParticleTypes.ITEM_SLIME, position.x, position.y, position.z, 10, 0.26D, 0.06D, 0.26D, 0.055D);
        level.sendParticles(ParticleTypes.COMPOSTER, position.x, position.y + 0.02D, position.z, 9, 0.24D, 0.08D, 0.24D, 0.04D);
        level.sendParticles(DIRTY_BROWN_DUST, position.x, position.y + 0.05D, position.z, 12, 0.28D, 0.08D, 0.28D, 0.025D);
        level.sendParticles(SICK_GREEN_DUST, position.x, position.y + 0.10D, position.z, 6, 0.20D, 0.08D, 0.20D, 0.015D);
        level.sendParticles(ParticleTypes.POOF, position.x, position.y + 0.14D, position.z, 5, 0.18D, 0.08D, 0.18D, 0.006D);
        level.sendParticles(ParticleTypes.SNEEZE, position.x, position.y + 0.18D, position.z, 3, 0.12D, 0.05D, 0.12D, 0.01D);
    }
}
