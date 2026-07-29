package EdDYON.guaniao.content.bird.sparrow;

import EdDYON.guaniao.GuaniaoMod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Persistent per-chunk breadcrumb index. Sparrow queries scale with the number of
 * real breadcrumb piles instead of the volume of the surrounding block region.
 */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BreadcrumbSearchCache {
    private static final String DATA_NAME = "guaniao_breadcrumb_index";
    private static final String TAG_POSITIONS = "Positions";
    private static final long CLAIM_TTL_TICKS = 100L;
    private static final long CLAIM_CLEANUP_INTERVAL_TICKS = 20L;
    private static final Map<ServerLevel, TargetClaims> TARGET_USERS = new WeakHashMap<>();

    private BreadcrumbSearchCache() {
    }

    public static List<BlockPos> nearby(SparrowEntity sparrow, int horizontalRadius, int verticalRadius) {
        if (!(sparrow.level() instanceof ServerLevel level)) {
            return List.of();
        }
        BlockPos origin = sparrow.blockPosition();
        int minChunkX = Math.floorDiv(origin.getX() - horizontalRadius, 16);
        int maxChunkX = Math.floorDiv(origin.getX() + horizontalRadius, 16);
        int minChunkZ = Math.floorDiv(origin.getZ() - horizontalRadius, 16);
        int maxChunkZ = Math.floorDiv(origin.getZ() + horizontalRadius, 16);
        BreadcrumbIndexData index = BreadcrumbIndexData.get(level);
        List<BlockPos> positions = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                for (long packedPos : index.positionsInChunk(ChunkPos.asLong(chunkX, chunkZ))) {
                    BlockPos pos = BlockPos.of(packedPos);
                    if (Math.abs(pos.getX() - origin.getX()) <= horizontalRadius
                            && Math.abs(pos.getY() - origin.getY()) <= verticalRadius
                            && Math.abs(pos.getZ() - origin.getZ()) <= horizontalRadius) {
                        positions.add(pos);
                    }
                }
            }
        }
        return positions;
    }

    public static void register(ServerLevel level, BlockPos pos) {
        BreadcrumbIndexData.get(level).add(pos);
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        BreadcrumbIndexData.get(level).remove(pos);
        TargetClaims claims = TARGET_USERS.get(level);
        if (claims != null) {
            claims.removePosition(pos);
            if (claims.isEmpty()) {
                TARGET_USERS.remove(level);
            }
        }
    }

    public static void claim(ServerLevel level, BlockPos pos, UUID sparrowId) {
        TARGET_USERS.computeIfAbsent(level, ignored -> new TargetClaims())
                .claim(pos, sparrowId, level.getGameTime());
    }

    public static void release(ServerLevel level, BlockPos pos, UUID sparrowId) {
        TargetClaims claims = TARGET_USERS.get(level);
        if (claims == null) {
            return;
        }
        claims.release(pos, sparrowId);
        if (claims.isEmpty()) {
            TARGET_USERS.remove(level);
        }
    }

    public static int userCount(ServerLevel level, BlockPos pos, UUID excludedSparrow) {
        TargetClaims claims = TARGET_USERS.get(level);
        if (claims == null) {
            return 0;
        }
        int count = claims.userCount(pos, excludedSparrow, level.getGameTime());
        if (claims.isEmpty()) {
            TARGET_USERS.remove(level);
        }
        return count;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TARGET_USERS.remove(level);
        }
    }

    private static final class TargetClaims {
        private final Map<Long, Map<UUID, Long>> usersByPosition = new HashMap<>();
        private long nextCleanupTick;

        private void claim(BlockPos pos, UUID sparrowId, long now) {
            this.cleanupExpired(now);
            this.usersByPosition.computeIfAbsent(pos.asLong(), ignored -> new HashMap<>())
                    .put(sparrowId, now + CLAIM_TTL_TICKS);
        }

        private void release(BlockPos pos, UUID sparrowId) {
            Map<UUID, Long> users = this.usersByPosition.get(pos.asLong());
            if (users == null) {
                return;
            }
            users.remove(sparrowId);
            if (users.isEmpty()) {
                this.usersByPosition.remove(pos.asLong());
            }
        }

        private void removePosition(BlockPos pos) {
            this.usersByPosition.remove(pos.asLong());
        }

        private int userCount(BlockPos pos, UUID excludedSparrow, long now) {
            this.cleanupExpired(now);
            Map<UUID, Long> users = this.usersByPosition.get(pos.asLong());
            if (users == null) {
                return 0;
            }
            int count = users.size();
            if (excludedSparrow != null && users.containsKey(excludedSparrow)) {
                count--;
            }
            return Math.max(0, count);
        }

        private void cleanupExpired(long now) {
            if (now < this.nextCleanupTick) {
                return;
            }
            this.nextCleanupTick = now + CLAIM_CLEANUP_INTERVAL_TICKS;
            this.usersByPosition.entrySet().removeIf(entry -> {
                entry.getValue().entrySet().removeIf(user -> user.getValue() < now);
                return entry.getValue().isEmpty();
            });
        }

        private boolean isEmpty() {
            return this.usersByPosition.isEmpty();
        }
    }

    private static final class BreadcrumbIndexData extends SavedData {
        private final Map<Long, Set<Long>> positionsByChunk = new HashMap<>();

        private static BreadcrumbIndexData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                    BreadcrumbIndexData::load,
                    BreadcrumbIndexData::new,
                    DATA_NAME
            );
        }

        private static BreadcrumbIndexData load(CompoundTag tag) {
            BreadcrumbIndexData data = new BreadcrumbIndexData();
            for (long packedPos : tag.getLongArray(TAG_POSITIONS)) {
                data.addPacked(packedPos, false);
            }
            return data;
        }

        private Set<Long> positionsInChunk(long chunkKey) {
            Set<Long> positions = this.positionsByChunk.get(chunkKey);
            return positions == null ? Set.of() : positions;
        }

        private void add(BlockPos pos) {
            this.addPacked(pos.asLong(), true);
        }

        private void addPacked(long packedPos, boolean markDirty) {
            BlockPos pos = BlockPos.of(packedPos);
            long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            if (this.positionsByChunk.computeIfAbsent(chunkKey, ignored -> new HashSet<>()).add(packedPos)
                    && markDirty) {
                this.setDirty();
            }
        }

        private void remove(BlockPos pos) {
            long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            Set<Long> positions = this.positionsByChunk.get(chunkKey);
            if (positions == null || !positions.remove(pos.asLong())) {
                return;
            }
            if (positions.isEmpty()) {
                this.positionsByChunk.remove(chunkKey);
            }
            this.setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            int size = this.positionsByChunk.values().stream().mapToInt(Set::size).sum();
            long[] positions = new long[size];
            int index = 0;
            for (Set<Long> chunkPositions : this.positionsByChunk.values()) {
                for (long packedPos : chunkPositions) {
                    positions[index++] = packedPos;
                }
            }
            tag.putLongArray(TAG_POSITIONS, positions);
            return tag;
        }
    }
}
