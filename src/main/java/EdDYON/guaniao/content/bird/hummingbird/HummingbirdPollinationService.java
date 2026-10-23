package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.config.HummingbirdConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;

/** One crop scheduler per level: UUID sources are merged before any crop gets a single round roll. */
public final class HummingbirdPollinationService {
    private static final Map<ServerLevel, State> LEVELS = new WeakHashMap<>();
    private static final Set<Block> CROPS = Set.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS);
    private static final Map<Block, CropAdapter> ADAPTERS = new HashMap<>();
    private static final int MAX_SOURCES_PER_CROP = 4;
    private static final ResourceLocation HUMMINGBIRD = ResourceLocation.fromNamespaceAndPath("guaniao", "hummingbird");

    private HummingbirdPollinationService() { }

    /** A protection adapter receives the genuine source entity and owner UUID, never a fabricated player. */
    public record Source(UUID id, @Nullable UUID owner, Mob entity, Vec3 position) { }

    public interface CropAdapter {
        boolean canGrow(ServerLevel level, BlockPos pos, BlockState state);
        void grow(ServerLevel level, BlockPos pos, BlockState state);
    }

    public static void registerCropAdapter(Block block, CropAdapter adapter) {
        ADAPTERS.put(block, adapter);
    }

    public static boolean isSupportedCrop(Block block) { return CROPS.contains(block) || ADAPTERS.containsKey(block); }

    public static boolean isHummingbird(Mob bird) {
        return HUMMINGBIRD.equals(BuiltInRegistries.ENTITY_TYPE.getKey(bird.getType()));
    }

    /** Call only while a server-side nectar transaction is valid (or after a sugar-water serving was consumed). */
    public static void activate(Mob bird) {
        if (!(bird.level() instanceof ServerLevel level) || !bird.isAlive() || !isHummingbird(bird)) return;
        State state = LEVELS.computeIfAbsent(level, ignored -> new State());
        Active previous = state.sources.put(bird.getUUID(), new Active(bird, bird.position(),
                level.getGameTime() + HummingbirdConfig.pollinationGraceTicks()));
        // Nectar loops refresh eligibility every tick; the shared scheduler already
        // refreshes the index every 20 ticks for a bird feeding in the same block.
        if (previous == null || !BlockPos.containing(previous.lastNectarPosition).equals(bird.blockPosition())) {
            FlowerHabitatIndex.request(level, bird.blockPosition(), (int) Math.ceil(HummingbirdConfig.pollinationRadius()));
        }
    }

    /** Sleep, panic, territory, a stay command, removal and unload must revoke immediately. */
    public static void remove(Mob bird) {
        if (bird.level() instanceof ServerLevel level) remove(level, bird.getUUID());
    }

    public static void remove(ServerLevel level, UUID bird) {
        State state = LEVELS.get(level);
        if (state != null) state.sources.remove(bird);
    }

    private static boolean valid(ServerLevel level, Active active) {
        return active.bird.isAlive() && !active.bird.isRemoved() && active.bird.level() == level
                && level.getGameTime() <= active.expiresAt
                && active.bird.position().distanceToSqr(active.lastNectarPosition) <= 64
                && level.getChunkSource().getChunkNow(active.bird.blockPosition().getX() >> 4,
                        active.bird.blockPosition().getZ() >> 4) != null;
    }

    public static int activeSourcesNear(ServerLevel level, Vec3 center, double radius) {
        State state = LEVELS.get(level);
        if (state == null) return 0;
        int count = 0;
        for (Active active : state.sources.values()) {
            if (valid(level, active) && active.bird.position().distanceToSqr(center) <= radius * radius) count++;
        }
        return count;
    }

    public static void tick(ServerLevel level) {
        State state = LEVELS.get(level);
        if (state == null || state.lastGameTick == level.getGameTime()) return;
        state.lastGameTick = level.getGameTime();
        state.clock++; // activity ticks only; no catch-up for world time jumps or server downtime
        state.sources.values().removeIf(active -> !valid(level, active));
        if (!HummingbirdConfig.pollinationEnabled()) {
            state.chunks.clear(); state.chunkOrder.clear(); state.discovery.clear(); state.queued.clear();
            state.nextRoundAt = state.clock + HummingbirdConfig.pollinationPeriodTicks();
            return;
        }
        state.sourceChunks.clear();
        for (Active active : state.sources.values()) {
            state.sourceChunks.computeIfAbsent(ChunkPos.asLong(active.bird.blockPosition()), ignored -> new ArrayList<>()).add(active);
        }
        if (state.clock % 20 == 0) for (Active active : state.sources.values()) {
            FlowerHabitatIndex.request(level, active.bird.blockPosition(), (int) Math.ceil(HummingbirdConfig.pollinationRadius()));
        }
        if (state.nextRoundAt == 0) state.nextRoundAt = state.clock + HummingbirdConfig.pollinationPeriodTicks();
        if (state.clock >= state.nextRoundAt && state.discovery.isEmpty() && state.chunks.isEmpty()) {
            startRound(state);
            state.nextRoundAt = state.clock + HummingbirdConfig.pollinationPeriodTicks();
        }

        // Only a few sources contribute cached candidate positions per frame, independent of flock size.
        for (int i = 0; i < 4 && !state.discovery.isEmpty(); i++) {
            UUID id = state.discovery.removeFirst();
            Active active = state.sources.get(id);
            if (active == null || !valid(level, active)) continue;
            for (BlockPos pos : FlowerHabitatIndex.cropsNear(level, active.bird.position(),
                    (int) Math.ceil(HummingbirdConfig.pollinationRadius()))) {
                if (state.queued.add(pos)) {
                    long chunk = ChunkPos.asLong(pos);
                    ArrayDeque<BlockPos> crops = state.chunks.get(chunk);
                    if (crops == null) {
                        crops = new ArrayDeque<>(); state.chunks.put(chunk, crops); state.chunkOrder.addLast(chunk);
                    }
                    crops.addLast(pos);
                }
            }
        }
        int budget = Math.max(1, HummingbirdConfig.cropBudget());
        while (budget-- > 0 && !state.chunkOrder.isEmpty()) {
            long key = state.chunkOrder.removeFirst();
            ArrayDeque<BlockPos> crops = state.chunks.get(key);
            if (crops == null || crops.isEmpty()) { state.chunks.remove(key); continue; }
            BlockPos pos = crops.removeFirst();
            if (crops.isEmpty()) state.chunks.remove(key); else state.chunkOrder.addLast(key);
            pollinate(level, state, pos);
        }
        // A delayed round gets no stacked make-up rounds on the following tick.
        if (state.discovery.isEmpty() && state.chunks.isEmpty() && state.clock >= state.nextRoundAt) {
            state.nextRoundAt = state.clock + HummingbirdConfig.pollinationPeriodTicks();
        }
    }

    private static void startRound(State state) {
        state.round++;
        state.queued.clear();
        List<UUID> ids = new ArrayList<>(state.sources.keySet());
        ids.sort(Comparator.naturalOrder());
        if (!ids.isEmpty()) {
            int start = (int) Math.floorMod(state.round, ids.size());
            for (int i = 0; i < ids.size(); i++) state.discovery.addLast(ids.get((start + i) % ids.size()));
        }
    }

    private static void pollinate(ServerLevel level, State state, BlockPos pos) {
        BlockState before = FlowerHabitatIndex.readyState(level, pos);
        if (before == null || !isSupportedCrop(before.getBlock())
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        CropAdapter adapter = ADAPTERS.get(before.getBlock());
        CropBlock crop = before.getBlock() instanceof CropBlock c ? c : null;
        if (adapter != null ? !adapter.canGrow(level, pos, before)
                : crop == null || !crop.isValidBonemealTarget(level, pos, before)) return;
        double radius = HummingbirdConfig.pollinationRadius();
        List<Active> candidates = new ArrayList<>();
        int range = (int) Math.ceil(radius);
        for (int x = (pos.getX() - range) >> 4; x <= (pos.getX() + range) >> 4; x++)
            for (int z = (pos.getZ() - range) >> 4; z <= (pos.getZ() + range) >> 4; z++) {
                List<Active> nearby = state.sourceChunks.get(ChunkPos.asLong(x, z));
                if (nearby != null) candidates.addAll(nearby);
            }
        candidates.sort(Comparator.comparing(active -> active.bird.getUUID()));
        List<Source> sources = new ArrayList<>(MAX_SOURCES_PER_CROP);
        Vec3 center = Vec3.atCenterOf(pos);
        for (Active active : candidates) {
            if (valid(level, active) && active.bird.position().distanceToSqr(center) <= radius * radius
                    && EventHooks.canEntityGrief(level, active.bird)) {
                UUID owner = active.bird instanceof TamableAnimal tame ? tame.getOwnerUUID() : null;
                sources.add(new Source(active.bird.getUUID(), owner, active.bird, active.bird.position()));
                if (sources.size() == MAX_SOURCES_PER_CROP) break;
            }
        }
        if (sources.isEmpty() || level.random.nextFloat() >= chanceFor(sources.size())) return;
        if (NeoForge.EVENT_BUS.post(new HummingbirdPollinationEvent(level, pos, before, state.round, sources)).isCanceled()) return;
        // Cancellation listeners can change the world or revoke a source: re-check before committing.
        for (Source source : sources) {
            if (!EventHooks.canEntityGrief(level, source.entity())) return;
        }
        if (!HummingbirdConfig.pollinationEnabled() || !before.equals(FlowerHabitatIndex.readyState(level, pos))
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        for (Source source : sources) {
            Active active = state.sources.get(source.id());
            UUID currentOwner = active != null && active.bird instanceof TamableAnimal tame ? tame.getOwnerUUID() : null;
            if (active == null || !valid(level, active) || active.bird.position().distanceToSqr(center) > radius * radius
                    || !java.util.Objects.equals(source.owner(), currentOwner)) return;
        }
        // This is automated bone-meal behavior, not a natural random tick or a fabricated player action.
        // Permissions are EntityMobGriefingEvent plus the cancellable, owner-aware event above.
        if (adapter != null) adapter.grow(level, pos, before);
        else if (crop.isBonemealSuccess(level, level.random, pos, before)) crop.performBonemeal(level, level.random, pos, before);
        BlockState after = FlowerHabitatIndex.readyState(level, pos);
        if (after != null && !before.equals(after)) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6,
                    pos.getZ() + 0.5, 2, 0.2, 0.15, 0.2, 0);
        }
    }

    public static double chanceFor(int distinctSources) { return 0.25 * Math.min(4, Math.max(0, distinctSources)); }
    public static void forgetLevel(ServerLevel level) { LEVELS.remove(level); }

    private record Active(Mob bird, Vec3 lastNectarPosition, long expiresAt) { }
    private static final class State {
        final Map<UUID, Active> sources = new LinkedHashMap<>();
        final Map<Long, List<Active>> sourceChunks = new HashMap<>();
        final ArrayDeque<UUID> discovery = new ArrayDeque<>();
        final Map<Long, ArrayDeque<BlockPos>> chunks = new LinkedHashMap<>();
        final ArrayDeque<Long> chunkOrder = new ArrayDeque<>();
        final Set<BlockPos> queued = new java.util.HashSet<>();
        long clock, nextRoundAt, round, lastGameTick = Long.MIN_VALUE;
    }
}
