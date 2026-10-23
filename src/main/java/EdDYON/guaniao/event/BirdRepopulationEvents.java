package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.BirdVisibility;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Replenishes loaded habitats without sharing the cow/sheep CREATURE cap. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdRepopulationEvents {
    private static final Map<ServerLevel, AttemptState> ATTEMPTS = new WeakHashMap<>();
    private static final int SCAN_RADIUS = 16;
    private static final int SITE_ATTEMPTS = 3;
    private static final int PACK_ATTEMPTS = 12;
    private static final int MAX_PACK_SIZE = 4;
    private static final int BASE_INTERVAL_TICKS = 120;
    private static final int CROWDED_INTERVAL_TICKS = 240;
    private static final int MIN_INTERVAL_TICKS = 60;
    private static final int FAILED_ATTEMPT_RETRY_TICKS = 40;
    private static final double MIN_PLAYER_DISTANCE_SQR = 24.0D * 24.0D;
    private static final double MAX_HORIZONTAL_PLAYER_DISTANCE_SQR = 96.0D * 96.0D;

    private BirdRepopulationEvents() { }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !isEnabled(level)) {
            return;
        }
        long now = level.getGameTime();
        AttemptState state = ATTEMPTS.computeIfAbsent(level, ignored -> new AttemptState());
        if (now < state.nextAttempt) return;
        List<ServerPlayer> players = level.players().stream()
                .filter(player -> !player.isSpectator() && player.isAlive() && isSurfaceObserver(level, player))
                .toList();
        if (players.isEmpty()) {
            state.nextAttempt = now + 20L;
            return;
        }
        ServerPlayer player = players.get(Math.floorMod(state.playerCursor++, players.size()));
        int nearbyCount = BirdPopulationTracker.totalAt(level, player.getX(), player.getZ());
        int regionalCount = BirdPopulationTracker.totalInRegionAt(level, player.getX(), player.getZ());
        // Fill empty habitats promptly, then slow down as either population cap
        // approaches. Multiplayer shares one scheduler and at most doubles its rate.
        int baseInterval = isCrowded(nearbyCount, BirdConfigManager.maxBirdsNearby())
                || isCrowded(regionalCount, BirdConfigManager.maxWildBirdsPerRegion())
                ? CROWDED_INTERVAL_TICKS : BASE_INTERVAL_TICKS;
        double rate = BirdConfigManager.naturalSpawnMultiplier() * Math.min(2, players.size());
        int interval = Mth.clamp((int)Math.ceil(baseInterval / rate), MIN_INTERVAL_TICKS, 2400);
        state.nextAttempt = now + interval;
        // Preserve very small configured rates when the wake-up interval is capped.
        if (level.random.nextDouble() >= Math.min(1.0D, rate * interval / baseInterval)) return;
        if (nearbyCount >= BirdConfigManager.maxBirdsNearby()) return;

        for (int attempt = 0; attempt < SITE_ATTEMPTS; ++attempt) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = 32.0D + level.random.nextDouble() * 48.0D;
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null) continue;
            BlockPos surface = surfaceAt(chunk, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (trySpawnAt(level, surface) > 0) return;
        }
        // Avoid repeatedly colliding with AI scans on the same 20-tick phase.
        state.nextAttempt = now + Math.min(interval, FAILED_ATTEMPT_RETRY_TICKS + level.random.nextInt(20));
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) ATTEMPTS.remove(level);
    }

    private static boolean isCrowded(int count, int limit) {
        return limit > 0 && count * 4L >= limit * 3L;
    }

    static int trySpawnAt(ServerLevel level, BlockPos surface) {
        if (!isEnabled(level) || !isUsableSite(level, surface)) return 0;

        // Read the loaded biome list, rather than asking the generator to resolve
        // distant structure references. Keep datapack entries and Forge vetoes.
        var biomeSpawns = level.getBiome(surface).value().getMobSettings().getMobs(MobCategory.CREATURE);
        var configured = ForgeEventFactory.getPotentialSpawns(level, MobCategory.CREATURE, surface, biomeSpawns);
        List<MobSpawnSettings.SpawnerData> candidates = new ArrayList<>();
        for (var data : configured.unwrap()) {
            BirdSpecies species = BirdSpecies.from(data.type);
            if (species != null && BirdConfigManager.allowsNaturalSpawning(species)
                    && isActiveTime(species, level.getDayTime())
                    && BirdSpawnConfigEvents.remainingCapacity(level, surface.getX(), surface.getZ(), species) > 0) {
                // Only this replenishment draw favors less represented species.
                // Keep biome weights, habitats and the normal spawn list intact.
                int count = BirdPopulationTracker.speciesAt(level, surface.getX(), surface.getZ(), species);
                double occupancy = Math.min(1.0D, (double)count / BirdConfigManager.maxWildNearby(species));
                int weight = (int)Math.min(Integer.MAX_VALUE,
                        Math.round(data.getWeight().asInt() * (2.0D - occupancy)));
                candidates.add(new MobSpawnSettings.SpawnerData(data.type, Math.max(1, weight), data.minCount, data.maxCount));
            }
        }
        var selected = WeightedRandomList.create(candidates).getRandom(level.random);
        if (selected.isEmpty()) return 0;
        MobSpawnSettings.SpawnerData data = selected.get();
        BirdSpecies species = BirdSpecies.from(data.type);
        int maxGroup = Math.min(MAX_PACK_SIZE, data.maxCount);
        int minGroup = Math.min(maxGroup, data.minCount);
        if (maxGroup <= 0) return 0;
        int target = Mth.nextInt(level.random, minGroup, maxGroup);
        int spawned = 0;
        SpawnGroupData group = null;

        for (int attempt = 0; attempt < PACK_ATTEMPTS && spawned < target; ++attempt) {
            int x = surface.getX() + (attempt == 0 ? 0 : level.random.nextInt(9) - 4);
            int z = surface.getZ() + (attempt == 0 ? 0 : level.random.nextInt(9) - 4);
            LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null) continue;
            BlockPos pos = surfaceAt(chunk, SpawnPlacements.getHeightmapType(data.type), x, z);
            if (!isUsableSite(level, pos)
                    || BirdSpawnConfigEvents.remainingCapacity(level, x, z, species) <= 0) continue;
            // Keep the species' placement attempts separate from the unclassified
            // habitat-scan tokens; both still share the per-level total budget.
            if (!BirdScanBudget.tryAcquireForSpecies(level, species)) break;
            var atPosition = ForgeEventFactory.getPotentialSpawns(level, MobCategory.CREATURE, pos,
                    level.getBiome(pos).value().getMobSettings().getMobs(MobCategory.CREATURE));
            if (atPosition.unwrap().stream().noneMatch(entry -> entry.type == data.type)) continue;
            if (!NaturalSpawner.isSpawnPositionOk(SpawnPlacements.getPlacementType(data.type), level, pos, data.type)
                    || !SpawnPlacements.checkSpawnRules(data.type, level, MobSpawnType.NATURAL, pos, level.random)
                    || !level.noCollision(data.type.getAABB(x + 0.5D, pos.getY(), z + 0.5D))) continue;

            Entity entity = data.type.create(level);
            if (!(entity instanceof Mob mob)) continue;
            mob.moveTo(x + 0.5D, pos.getY(), z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            if (!ForgeEventFactory.checkSpawnPosition(mob, level, MobSpawnType.NATURAL)) continue;
            group = ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(pos),
                    MobSpawnType.NATURAL, group, null);
            if (level.addFreshEntity(mob)) {
                ++spawned;
                if (spawned >= ForgeEventFactory.getMaxSpawnPackSize(mob) || mob.isMaxGroupSizeReached(spawned)) break;
            }
        }
        return spawned;
    }

    private static boolean isEnabled(ServerLevel level) {
        return level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                && BirdConfigManager.naturalSpawnMultiplier() > 0.0D
                && BirdConfigManager.maxBirdsNearby() > 0;
    }

    private static boolean isUsableSite(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() + 3 >= level.getMaxBuildHeight()
                || !hasReadyNeighborhood(level, pos) || !level.isPositionEntityTicking(pos)
                || !level.getWorldBorder().isWithinBounds(pos)
                || pos.distSqr(level.getSharedSpawnPos()) <= MIN_PLAYER_DISTANCE_SQR) return false;
        boolean nearPlayer = false;
        Vec3 feet = Vec3.atBottomCenterOf(pos);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) continue;
            double distance = player.distanceToSqr(feet);
            if (distance <= MIN_PLAYER_DISTANCE_SQR) return false;
            double dx = player.getX() - feet.x;
            double dz = player.getZ() - feet.z;
            if (distance <= BirdVisibility.TRACKING_DISTANCE_SQUARED
                    && dx * dx + dz * dz <= MAX_HORIZONTAL_PLAYER_DISTANCE_SQR
                    && isSurfaceObserver(level, player)) nearPlayer = true;
        }
        return nearPlayer && isNaturalSurface(level.getBlockState(pos.below()));
    }

    static boolean hasReadyNeighborhood(ServerLevel level, BlockPos pos) {
        for (int x = (pos.getX() - SCAN_RADIUS) >> 4; x <= (pos.getX() + SCAN_RADIUS) >> 4; ++x) {
            for (int z = (pos.getZ() - SCAN_RADIUS) >> 4; z <= (pos.getZ() + SCAN_RADIUS) >> 4; ++z) {
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
            }
        }
        return true;
    }

    private static boolean isNaturalSurface(BlockState state) {
        return state.is(BlockTags.ANIMALS_SPAWNABLE_ON) || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND) || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)
                || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MUD) || state.is(Blocks.CLAY) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.FARMLAND);
    }

    private static boolean isSurfaceObserver(ServerLevel level, ServerPlayer player) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(player.blockPosition().getX() >> 4,
                player.blockPosition().getZ() >> 4);
        if (chunk == null) return false;
        int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                player.blockPosition().getX() & 15, player.blockPosition().getZ() & 15) + 1;
        // Flying above a loaded habitat must not disable its ground population.
        // Underground players still do not trigger surface replenishment.
        return player.getY() >= y - 6;
    }

    private static BlockPos surfaceAt(LevelChunk chunk, Heightmap.Types heightmap, int x, int z) {
        return new BlockPos(x, chunk.getHeight(heightmap, x & 15, z & 15) + 1, z);
    }

    private static boolean isActiveTime(BirdSpecies species, long dayTime) {
        BirdActivitySchedule schedule = switch (species) {
            case NIGHT_HERON, KIWI, WOODCOCK -> BirdActivitySchedule.NOCTURNAL_CREPUSCULAR;
            case SEAGULL -> BirdActivitySchedule.COASTAL_DIURNAL;
            default -> BirdActivitySchedule.DIURNAL;
        };
        return schedule.isActiveTime(dayTime);
    }

    private static final class AttemptState {
        private long nextAttempt;
        private int playerCursor;
    }
}
