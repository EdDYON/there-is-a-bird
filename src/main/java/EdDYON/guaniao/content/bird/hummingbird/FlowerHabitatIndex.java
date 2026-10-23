package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.config.HummingbirdConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared, incremental section index. Queries request work; they never synchronously scan a volume. */
public final class FlowerHabitatIndex {
    private static final int MAX_SECTIONS = 512, SCAN_SLICE = 256, REFRESH_TICKS = 200;
    private static final int UNUSED_TICKS = 600, LEASE_TICKS = 120;
    private static final Map<ServerLevel, Index> LEVELS = new WeakHashMap<>();
    private static final Map<Block, NectarAdapter> ADAPTERS = new HashMap<>();
    private static final Set<Block> SMALL_FLOWERS = Set.of(Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID,
            Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP,
            Blocks.PINK_TULIP, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.TORCHFLOWER);
    private static final Set<Block> TALL_FLOWERS = Set.of(Blocks.SUNFLOWER, Blocks.LILAC, Blocks.ROSE_BUSH, Blocks.PEONY);

    private FlowerHabitatIndex() { }

    public enum SourceType { FLOWER, TALL_FLOWER, POTTED_FLOWER }

    public record NectarTarget(BlockPos anchor, BlockPos flowerPos, Vec3 contact, Vec3 hover,
                               SourceType type, Block flower) {
        public NectarTarget {
            anchor = anchor.immutable();
            flowerPos = flowerPos.immutable();
        }
        public NectarTarget approachedFrom(Vec3 from) {
            Vec3 offset = new Vec3(from.x - contact.x, 0, from.z - contact.z);
            if (offset.lengthSqr() < 1.0E-8) offset = new Vec3(0, 0, 1);
            Vec3 feet = contact.add(offset.normalize().scale(0.23)).add(0, -0.2703, 0);
            return new NectarTarget(anchor, flowerPos, contact, feet, type, flower);
        }
    }

    /** Explicit adapters must only read already-ready chunks via readyState(). */
    @FunctionalInterface
    public interface NectarAdapter {
        @Nullable NectarTarget resolve(ServerLevel level, BlockPos pos, BlockState state);
    }

    public static void registerAdapter(Block block, NectarAdapter adapter) {
        ADAPTERS.put(block, adapter);
        LEVELS.clear();
    }

    @Nullable
    public static BlockState readyState(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return null;
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }

    public static boolean isSupportedFlower(Block block) {
        return isVanillaFlower(block) || ADAPTERS.containsKey(block);
    }

    static boolean isVanillaFlower(Block block) {
        return SMALL_FLOWERS.contains(block) || TALL_FLOWERS.contains(block);
    }

    @Nullable
    public static NectarTarget targetAt(ServerLevel level, BlockPos pos) {
        BlockState state = readyState(level, pos);
        if (state == null) return null;
        NectarAdapter adapter = ADAPTERS.get(state.getBlock());
        if (adapter != null) return adapter.resolve(level, pos, state);
        Block flower = state.getBlock();
        SourceType type = SourceType.FLOWER;
        BlockPos anchor = pos, flowerPos = pos;
        double height = 0.60;
        if (state.getBlock() instanceof FlowerPotBlock pot && SMALL_FLOWERS.contains(pot.getPotted())) {
            flower = pot.getPotted();
            type = SourceType.POTTED_FLOWER;
            height = 0.74;
        } else if (TALL_FLOWERS.contains(flower)) {
            anchor = state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            flowerPos = anchor.above();
            BlockState lower = readyState(level, anchor), upper = readyState(level, flowerPos);
            if (lower == null || upper == null || !lower.is(flower) || !upper.is(flower)
                    || lower.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.LOWER
                    || upper.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.UPPER) return null;
            state = upper;
            type = SourceType.TALL_FLOWER;
            height = 0.68;
        } else if (!SMALL_FLOWERS.contains(flower)) return null;
        Vec3 offset = state.getOffset(level, flowerPos);
        Vec3 contact = new Vec3(flowerPos.getX() + 0.5, flowerPos.getY() + height, flowerPos.getZ() + 0.5).add(offset);
        return new NectarTarget(anchor, flowerPos, contact, contact.add(0, -0.2703, 0.23), type, flower);
    }

    public static boolean valid(ServerLevel level, NectarTarget target) {
        NectarTarget actual = targetAt(level, target.flowerPos());
        return actual != null && actual.anchor().equals(target.anchor()) && actual.flower() == target.flower()
                && actual.type() == target.type() && actual.contact().distanceToSqr(target.contact()) < 0.0001;
    }

    public static void request(ServerLevel level, BlockPos center, int radius) {
        int bounded = Math.max(0, Math.min(32, radius));
        Index index = LEVELS.computeIfAbsent(level, ignored -> new Index());
        long now = level.getGameTime();
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - bounded) >> 4;
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + bounded) >> 4;
        for (int x = (center.getX() - bounded) >> 4; x <= (center.getX() + bounded) >> 4; x++) {
            for (int z = (center.getZ() - bounded) >> 4; z <= (center.getZ() + bounded) >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) continue;
                for (int y = minY; y <= maxY; y++) {
                    SectionKey key = new SectionKey(x, y, z);
                    Entry entry = index.sections.get(key);
                    if (entry == null) {
                        if (index.sections.size() >= MAX_SECTIONS && !evictOldest(index)) continue;
                        entry = new Entry(key);
                        index.sections.put(key, entry);
                    }
                    entry.usedAt = now;
                    if (!entry.queued && now >= entry.refreshAt) {
                        entry.queued = true;
                        entry.dirtyDuringScan = false;
                        entry.cursor = 0;
                        entry.pendingFlowers.clear();
                        entry.pendingCrops.clear();
                        index.queue.addLast(entry);
                    }
                }
            }
        }
    }

    private static boolean evictOldest(Index index) {
        Iterator<Entry> iterator = index.sections.values().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (entry.queued) continue; // finish old requests before admitting more work
            iterator.remove();
            return true;
        }
        return false;
    }

    public static int countFlowers(ServerLevel level, BlockPos center, int radius) {
        return flowersNear(level, Vec3.atCenterOf(center), radius).size();
    }

    public static List<NectarTarget> flowersNear(ServerLevel level, Vec3 center, int radius) {
        request(level, BlockPos.containing(center), radius);
        Index index = LEVELS.get(level);
        Map<BlockPos, NectarTarget> found = new LinkedHashMap<>();
        double distanceSqr = (double) radius * radius;
        for (Entry entry : nearbyEntries(index, center, radius)) {
            for (NectarTarget target : entry.flowers.values()) {
                if (Vec3.atCenterOf(target.anchor()).distanceToSqr(center) <= distanceSqr && valid(level, target)) {
                    found.putIfAbsent(target.anchor(), target);
                }
            }
        }
        return List.copyOf(found.values());
    }

    public static List<BlockPos> cropsNear(ServerLevel level, Vec3 center, int radius) {
        request(level, BlockPos.containing(center), radius);
        Set<BlockPos> found = new HashSet<>();
        for (Entry entry : nearbyEntries(LEVELS.get(level), center, radius)) {
            for (BlockPos pos : entry.crops) {
                if (Vec3.atCenterOf(pos).distanceToSqr(center) <= (double) radius * radius) found.add(pos);
            }
        }
        return List.copyOf(found);
    }

    private static List<Entry> nearbyEntries(Index index, Vec3 center, int radius) {
        List<Entry> result = new ArrayList<>();
        if (index == null) return result;
        BlockPos pos = BlockPos.containing(center);
        for (int x = (pos.getX() - radius) >> 4; x <= (pos.getX() + radius) >> 4; x++)
            for (int z = (pos.getZ() - radius) >> 4; z <= (pos.getZ() + radius) >> 4; z++)
                for (int y = (pos.getY() - radius) >> 4; y <= (pos.getY() + radius) >> 4; y++) {
                    Entry entry = index.sections.get(new SectionKey(x, y, z));
                    if (entry != null) result.add(entry);
                }
        return result;
    }

    public static Optional<NectarTarget> find(ServerLevel level, Vec3 from, int radius, UUID bird) {
        return flowersNear(level, from, radius).stream().filter(target -> available(level, target, bird))
                .map(target -> target.approachedFrom(from)).filter(target -> clearHover(level, target.hover(), bird))
                .min(Comparator.comparingDouble(target -> target.hover().distanceToSqr(from)));
    }

    private static boolean clearHover(ServerLevel level, Vec3 feet, UUID bird) {
        AABB box = new AABB(feet.x - 0.13, feet.y, feet.z - 0.13, feet.x + 0.13, feet.y + 0.38, feet.z + 0.13);
        BlockPos min = BlockPos.containing(box.minX - 1, box.minY - 1, box.minZ - 1);
        BlockPos max = BlockPos.containing(box.maxX + 1, box.maxY + 1, box.maxZ + 1);
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++)
            for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        return level.noCollision(level.getEntity(bird), box);
    }

    private static boolean available(ServerLevel level, NectarTarget target, UUID bird) {
        Index index = LEVELS.computeIfAbsent(level, ignored -> new Index());
        Visit visit = index.visits.get(target.anchor());
        long now = level.getGameTime();
        return valid(level, target) && (visit == null || (now >= visit.cooldownUntil
                && (visit.owner == null || now >= visit.leaseUntil || bird.equals(visit.owner))));
    }

    public static boolean claim(ServerLevel level, NectarTarget target, UUID bird) {
        if (!available(level, target, bird)) return false;
        Index index = LEVELS.get(level);
        Visit visit = index.visits.computeIfAbsent(target.anchor(), ignored -> new Visit());
        visit.owner = bird;
        visit.leaseUntil = level.getGameTime() + Math.max(LEASE_TICKS, HummingbirdConfig.nectarTicks() + 40);
        return true;
    }

    public static void release(ServerLevel level, NectarTarget target, UUID bird) {
        Index index = LEVELS.get(level);
        if (index == null) return;
        Visit visit = index.visits.get(target.anchor());
        if (visit != null && bird.equals(visit.owner)) { visit.owner = null; visit.leaseUntil = 0; }
    }

    /** One successful completion consumes the lease; repeat calls cannot produce another reward. */
    public static boolean complete(ServerLevel level, NectarTarget target, UUID bird, RandomSource random) {
        Index index = LEVELS.get(level);
        Visit visit = index == null ? null : index.visits.get(target.anchor());
        long now = level.getGameTime();
        if (visit == null || !bird.equals(visit.owner) || now >= visit.leaseUntil || !valid(level, target)) {
            release(level, target, bird);
            return false;
        }
        int min = HummingbirdConfig.flowerCooldownMinTicks();
        int max = Math.max(min, HummingbirdConfig.flowerCooldownMaxTicks());
        visit.cooldownUntil = now + min + random.nextInt(max - min + 1);
        visit.owner = null;
        visit.leaseUntil = 0;
        return true;
    }

    public static void releaseAll(ServerLevel level, UUID bird) {
        Index index = LEVELS.get(level);
        if (index != null) for (Visit visit : index.visits.values()) if (bird.equals(visit.owner)) {
            visit.owner = null; visit.leaseUntil = 0;
        }
    }

    public static void invalidate(ServerLevel level, BlockPos pos) {
        Index index = LEVELS.get(level);
        if (index == null) return;
        for (BlockPos affected : List.of(pos, pos.above(), pos.below())) {
            Entry entry = index.sections.get(SectionKey.at(affected));
            if (entry != null) {
                entry.refreshAt = 0;
                if (entry.queued && entry.cursor > 0) entry.dirtyDuringScan = true;
            }
        }
    }

    public static void tick(ServerLevel level) {
        Index index = LEVELS.get(level);
        if (index == null || index.lastTick == level.getGameTime()) return;
        index.lastTick = level.getGameTime();
        int budget = HummingbirdConfig.flowerScanBudget();
        int initialBudget = budget;
        while (budget > 0 && !index.queue.isEmpty()) {
            Entry entry = index.queue.removeFirst();
            LevelChunk chunk = level.getChunkSource().getChunkNow(entry.key.x, entry.key.z);
            if (chunk == null || level.getGameTime() - entry.usedAt > UNUSED_TICKS) {
                index.sections.remove(entry.key);
                continue;
            }
            LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(entry.key.y << 4));
            if (entry.cursor == 0 && section.hasOnlyAir()) {
                entry.cursor = 4096;
                budget--;
            } else {
                int take = Math.min(Math.min(SCAN_SLICE, budget), 4096 - entry.cursor);
                for (int i = 0; i < take; i++, entry.cursor++) {
                    int localX = entry.cursor & 15, localZ = (entry.cursor >> 4) & 15, localY = entry.cursor >> 8;
                    BlockState state = section.getBlockState(localX, localY, localZ);
                    if (!interesting(state)) continue;
                    BlockPos pos = new BlockPos((entry.key.x << 4) + localX, (entry.key.y << 4) + localY, (entry.key.z << 4) + localZ);
                    if (HummingbirdPollinationService.isSupportedCrop(state.getBlock())) entry.pendingCrops.add(pos);
                    NectarTarget target = targetAt(level, pos);
                    if (target != null) entry.pendingFlowers.putIfAbsent(target.anchor(), target);
                }
                budget -= take;
            }
            if (entry.cursor >= 4096) {
                entry.flowers = Map.copyOf(entry.pendingFlowers);
                entry.crops = Set.copyOf(entry.pendingCrops);
                entry.pendingFlowers.clear(); entry.pendingCrops.clear();
                if (entry.dirtyDuringScan) {
                    // An edit can affect a position already passed by the incremental cursor.
                    // Finish this budgeted snapshot, then repeat immediately rather than
                    // replacing the invalidation with the usual 200-tick refresh delay.
                    entry.dirtyDuringScan = false;
                    entry.cursor = 0;
                    index.queue.addLast(entry);
                } else {
                    entry.queued = false;
                    entry.refreshAt = level.getGameTime() + REFRESH_TICKS;
                }
            } else index.queue.addLast(entry);
        }
        index.examinedThisTick = initialBudget - budget;
        if (Math.floorMod(level.getGameTime(), 100) == 0) {
            index.visits.values().removeIf(visit -> level.getGameTime() >= visit.leaseUntil
                    && level.getGameTime() >= visit.cooldownUntil);
            index.sections.values().removeIf(entry -> !entry.queued && level.getGameTime() - entry.usedAt > UNUSED_TICKS);
        }
    }

    private static boolean interesting(BlockState state) {
        return isSupportedFlower(state.getBlock()) || state.getBlock() instanceof FlowerPotBlock
                || HummingbirdPollinationService.isSupportedCrop(state.getBlock());
    }

    public static void unloadChunk(ServerLevel level, int x, int z) {
        Index index = LEVELS.get(level);
        if (index == null) return;
        index.sections.entrySet().removeIf(entry -> entry.getKey().x == x && entry.getKey().z == z);
        index.queue.removeIf(entry -> entry.key.x == x && entry.key.z == z);
        index.visits.keySet().removeIf(pos -> (pos.getX() >> 4) == x && (pos.getZ() >> 4) == z);
    }

    public static void forgetLevel(ServerLevel level) { LEVELS.remove(level); }

    public record Stats(int examinedThisTick, int queuedSections, int cachedSections) { }
    public static Stats stats(ServerLevel level) {
        Index index = LEVELS.get(level);
        return index == null ? new Stats(0, 0, 0)
                : new Stats(index.examinedThisTick, index.queue.size(), index.sections.size());
    }

    private record SectionKey(int x, int y, int z) {
        static SectionKey at(BlockPos pos) { return new SectionKey(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4); }
    }
    private static final class Visit { UUID owner; long leaseUntil, cooldownUntil; }
    private static final class Entry {
        final SectionKey key;
        final Map<BlockPos, NectarTarget> pendingFlowers = new HashMap<>();
        final Set<BlockPos> pendingCrops = new HashSet<>();
        Map<BlockPos, NectarTarget> flowers = Map.of();
        Set<BlockPos> crops = Set.of();
        int cursor;
        boolean queued, dirtyDuringScan;
        long refreshAt, usedAt;
        Entry(SectionKey key) { this.key = key; }
    }
    private static final class Index {
        final LinkedHashMap<SectionKey, Entry> sections = new LinkedHashMap<>(16, 0.75F, true);
        final ArrayDeque<Entry> queue = new ArrayDeque<>();
        final Map<BlockPos, Visit> visits = new HashMap<>();
        int examinedThisTick;
        long lastTick = Long.MIN_VALUE;
    }
}
