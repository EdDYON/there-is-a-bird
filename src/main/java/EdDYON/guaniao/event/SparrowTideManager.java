package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.content.bird.sparrow.SparrowTideRules;
import EdDYON.guaniao.content.bird.sparrow.SparrowTideRules.Phase;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** One controller per level; bounded, loaded-chunk-only waves rather than larger spawn groups. */
@EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class SparrowTideManager {
    private static final TagKey<Biome> HABITAT = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, "sparrow_tide_habitat"));
    private static final int MAX_EVENTS_PER_LEVEL = 128;
    private static final int MAX_ACTIVE_EVENTS = 2;
    private static final Map<ServerLevel, LevelTides> LEVELS = new WeakHashMap<>();

    private SparrowTideManager() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.dimension() != Level.OVERWORLD || level.getGameTime() % 5L != 0L) {
                continue;
            }
            LevelTides tides = LEVELS.get(level);
            if (tides == null && !BirdConfigManager.sparrowTideMode()) {
                continue;
            }
            if (tides == null) {
                tides = new LevelTides();
                LEVELS.put(level, tides);
            }
            tickLevel(level, tides);
        }
    }

    private static boolean enabled(ServerLevel level) {
        return BirdConfigManager.sparrowTideMode()
                && BirdConfigManager.allowsNaturalSpawning(BirdSpecies.SPARROW)
                && BirdConfigManager.maxBirdsNearby() > 0
                && BirdConfigManager.maxWildNearby(BirdSpecies.SPARROW) > 0
                && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
    }

    private static void tickLevel(ServerLevel level, LevelTides tides) {
        long now = level.getGameTime();
        boolean enabled = enabled(level);
        double activity = SparrowTideRules.activityMultiplier(level.getDayTime(), level.isRaining(), level.isThundering());
        Iterator<TideEvent> iterator = tides.events.values().iterator();
        int activeEvents = 0;
        while (iterator.hasNext()) {
            TideEvent tide = iterator.next();
            if (tide.timeline.phase() == Phase.COOLDOWN) {
                if (now >= tide.cooldownUntil) {
                    iterator.remove();
                }
                continue;
            }
            boolean observed = hasObserver(level, tide.origin);
            Phase previous = tide.timeline.phase();
            tide.timeline.advance(now, enabled && observed, activity > 0.0D,
                    Math.max(tide.localSparrows, countSparrows(level, tide.origin)) >= tide.target);
            Phase phase = tide.timeline.phase();
            if (phase == Phase.ACTIVE && previous == Phase.GATHERING) {
                tide.nextMove = now + Mth.nextInt(level.random, 40 * 20, 120 * 20);
            }
            if (phase == Phase.DISPERSING || phase == Phase.COOLDOWN) {
                if (previous != Phase.DISPERSING && previous != Phase.COOLDOWN) {
                    releaseMembers(level, tide);
                }
                if (phase == Phase.COOLDOWN) {
                    tide.cooldownUntil = now + (tide.spawned > 0
                            ? Mth.nextInt(level.random, SparrowTideRules.COOLDOWN_MIN, SparrowTideRules.COOLDOWN_MAX)
                            : 30 * 20);
                }
                continue;
            }
            activeEvents++;
            if (now >= tide.nextMemberRefresh && BirdScanBudget.tryAcquire(level, 1)) {
                refreshMembers(level, tide, now);
                tide.nextMemberRefresh = now + 100L;
            }
            if (phase == Phase.GATHERING) {
                tickWave(level, tide, now);
            } else if (now >= tide.nextMove && activity > 0.0D && !level.isRaining()) {
                tide.nextMove = now + Mth.nextInt(level.random, 40 * 20, 120 * 20);
                if (level.random.nextFloat() < 0.30F && BirdScanBudget.tryAcquire(level, 1)) {
                    double angle = level.random.nextDouble() * Math.PI * 2.0D;
                    double distance = Mth.nextDouble(level.random, 20.0D, 40.0D);
                    BlockPos next = surfaceAt(level, tide.center.getX() + Mth.floor(Math.cos(angle) * distance),
                            tide.center.getZ() + Mth.floor(Math.sin(angle) * distance));
                    if (next != null && horizontalDistanceSqr(next, tide.origin) <= 48.0D * 48.0D
                            && suitableHabitat(level, next)) {
                        tide.center = next;
                        tide.nextMemberRefresh = now;
                    }
                }
            }
        }
        if (now % 200L == 0L) {
            tides.playerAttempts.keySet().removeIf(id -> level.players().stream().noneMatch(p -> p.getUUID().equals(id)));
        }
        if (!enabled || activity <= 0.0D || activeEvents >= MAX_ACTIVE_EVENTS || tides.events.size() >= MAX_EVENTS_PER_LEVEL) {
            return;
        }
        // At most one player's habitat attempt per level and controller tick.
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || now < tides.playerAttempts.getOrDefault(player.getUUID(), 0L)) {
                continue;
            }
            tides.playerAttempts.put(player.getUUID(), now + Mth.nextInt(level.random, 100, 200));
            if (level.random.nextDouble() < 0.08D * activity) {
                tryStart(level, tides, player, now);
            }
            break;
        }
    }

    private static void tryStart(ServerLevel level, LevelTides tides, ServerPlayer player, long now) {
        if (!level.canSeeSky(player.blockPosition()) || !BirdScanBudget.tryAcquire(level, 1)) {
            return;
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double distance = Mth.nextDouble(level.random, 16.0D, 48.0D);
        BlockPos center = surfaceAt(level, player.getBlockX() + Mth.floor(Math.cos(angle) * distance),
                player.getBlockZ() + Mth.floor(Math.sin(angle) * distance));
        if (center == null || Math.abs(center.getY() - player.getY()) > 12.0D || !suitableHabitat(level, center)) {
            return;
        }
        int cellX = Math.floorDiv(center.getX(), SparrowTideRules.COOLDOWN_CELL_SIZE);
        int cellZ = Math.floorDiv(center.getZ(), SparrowTideRules.COOLDOWN_CELL_SIZE);
        for (TideEvent existing : tides.events.values()) {
            if (SparrowTideRules.eventsOverlap(center.getX(), center.getZ(), existing.origin.getX(), existing.origin.getZ())) {
                return;
            }
        }
        int score = BirdColonySpawnRules.sparrowTideHabitatScore(level, center);
        int minimum = SparrowTideRules.minTarget(score);
        if (minimum == 0) {
            return;
        }
        int target = Mth.nextInt(level.random, minimum, SparrowTideRules.maxTarget(score));
        if (remainingCapacity(level, center, target, countSparrows(level, center)) <= 0) {
            return;
        }
        TideEvent tide = new TideEvent(center, target, now, level.random);
        tides.events.put(cellKey(cellX, cellZ), tide);
    }

    private static void tickWave(ServerLevel level, TideEvent tide, long now) {
        if (tide.localSparrows < 0) {
            return;
        }
        if (tide.waveRemaining <= 0) {
            if (now < tide.nextWave) {
                return;
            }
            tide.waveRemaining = Mth.nextInt(level.random, SparrowTideRules.WAVE_MIN, SparrowTideRules.WAVE_MAX);
            tide.waveAttempts = tide.waveRemaining * 4;
            tide.waveAngle += 2.399963229728653D;
            tide.nextWave = now + Mth.nextInt(level.random, SparrowTideRules.WAVE_DELAY_MIN, SparrowTideRules.WAVE_DELAY_MAX);
        }
        if (!BirdScanBudget.tryAcquire(level, 1)) {
            return;
        }
        int current = Math.max(tide.localSparrows, countSparrows(level, tide.origin));
        if (remainingCapacity(level, tide.origin, tide.target, current) <= 0) {
            tide.waveRemaining = 0;
            return;
        }
        // A wave is spread over several ticks; blocked spawn points have a finite attempt budget.
        if (--tide.waveAttempts < 0) {
            tide.waveRemaining = 0;
            return;
        }
        double angle = tide.waveAngle + Mth.nextDouble(level.random, -0.65D, 0.65D);
        double distance = Mth.nextDouble(level.random, 20.0D, 45.0D);
        BlockPos ground = surfaceAt(level, tide.center.getX() + Mth.floor(Math.cos(angle) * distance),
                tide.center.getZ() + Mth.floor(Math.sin(angle) * distance));
        BlockPos landing = findGatheringPoint(level, tide.center, level.random);
        if (ground == null || landing == null || Math.abs(ground.getY() - tide.center.getY()) > 10
                || horizontalDistanceSqr(ground, tide.origin) > SparrowTideRules.EVENT_RADIUS * SparrowTideRules.EVENT_RADIUS
                || !suitableHabitat(level, ground)
                || remainingCapacity(level, ground, tide.target, current) <= 0) {
            return;
        }
        boolean flying = level.random.nextFloat() < 0.70F;
        Vec3 start = Vec3.atBottomCenterOf(ground).add(0.0D, flying ? Mth.nextDouble(level.random, 2.0D, 4.0D) : 0.05D, 0.0D);
        if (!BirdFlybySpawnEvents.isHiddenFromNearbyPlayers(level, start)) {
            return;
        }
        SparrowEntity bird = GuaniaoEntityTypes.SPARROW.get().create(level);
        if (bird == null) {
            return;
        }
        Vec3 target = Vec3.atBottomCenterOf(landing).add(0.0D, 0.05D, 0.0D);
        Vec3 direction = target.subtract(start);
        float yaw = (float)Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
        bird.moveTo(start.x, start.y, start.z, yaw, 0.0F);
        bird.setYHeadRot(yaw);
        bird.yBodyRot = yaw;
        bird.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), MobSpawnType.NATURAL, null);
        if (!level.noCollision(bird, bird.getBoundingBox()) || (flying && !bird.startFlybyFlight(target))) {
            return;
        }
        bird.joinSparrowTide(tide.center, now + 200L, current + 1);
        if (level.addFreshEntity(bird)) {
            tide.waveRemaining--;
            tide.spawned++;
            tide.localSparrows++;
            tide.memberIds.add(bird.getId());
        }
    }

    private static int remainingCapacity(ServerLevel level, BlockPos pos, int target, int local) {
        return SparrowTideRules.remainingCapacity(BirdConfigManager.maxBirdsNearby(),
                BirdConfigManager.maxWildNearby(BirdSpecies.SPARROW), BirdConfigManager.maxWildBirdsPerRegion(),
                BirdPopulationTracker.totalAt(level, pos.getX(), pos.getZ()), countSparrows(level, pos),
                BirdPopulationTracker.totalInRegionAt(level, pos.getX(), pos.getZ()), local, target);
    }

    private static int countSparrows(ServerLevel level, BlockPos pos) {
        return BirdPopulationTracker.speciesAt(level, pos.getX(), pos.getZ(), BirdSpecies.SPARROW);
    }

    private static void refreshMembers(ServerLevel level, TideEvent tide, long now) {
        List<SparrowEntity> nearby = level.getEntitiesOfClass(SparrowEntity.class,
                new AABB(tide.origin).inflate(SparrowTideRules.EVENT_RADIUS, 32.0D, SparrowTideRules.EVENT_RADIUS),
                bird -> bird.isAlive() && BirdSpecies.from(bird) == BirdSpecies.SPARROW
                        && horizontalDistanceSqr(bird.blockPosition(), tide.origin)
                        <= SparrowTideRules.EVENT_RADIUS * SparrowTideRules.EVENT_RADIUS);
        tide.localSparrows = nearby.size();
        tide.memberIds.clear();
        for (SparrowEntity bird : nearby) {
            if (tide.memberIds.size() >= SparrowTideRules.HARD_CAP) {
                break;
            }
            if (!bird.isTame() && !bird.isBaby() && !bird.isNoAi() && !bird.hasCustomName()
                    && !bird.isLeashed() && !bird.isPassenger()) {
                bird.joinSparrowTide(tide.center, now + 200L, tide.localSparrows);
                tide.memberIds.add(bird.getId());
            }
        }
    }

    private static void releaseMembers(ServerLevel level, TideEvent tide) {
        for (int id : tide.memberIds) {
            Entity member = level.getEntity(id);
            if (member instanceof SparrowEntity bird) {
                bird.leaveSparrowTide();
            }
        }
        tide.memberIds.clear();
    }

    public static BlockPos findGatheringPoint(ServerLevel level, BlockPos center, RandomSource random) {
        for (int attempt = 0; attempt < 4; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = Mth.nextDouble(random, 3.0D, 14.0D);
            BlockPos pos = surfaceAt(level, center.getX() + Mth.floor(Math.cos(angle) * distance),
                    center.getZ() + Mth.floor(Math.sin(angle) * distance));
            if (pos != null && Math.abs(pos.getY() - center.getY()) <= 6) {
                return pos;
            }
        }
        return null;
    }

    private static BlockPos surfaceAt(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
        }
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int y = height + 1; y >= height - 2; y--) {
            if (y <= level.getMinBuildHeight() || y + 1 >= level.getMaxBuildHeight()) {
                continue;
            }
            BlockPos pos = new BlockPos(x, y, z);
            BlockState below = level.getBlockState(pos.below());
            boolean foodGround = below.is(BlockTags.DIRT) || below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                    || below.is(Blocks.FARMLAND) || below.is(Blocks.HAY_BLOCK) || below.is(Blocks.COMPOSTER);
            if (foodGround && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.below()).isEmpty()
                    && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    private static boolean suitableHabitat(ServerLevel level, BlockPos pos) {
        return level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && level.getBiome(pos).is(HABITAT)
                && level.canSeeSky(pos) && !level.getBlockState(pos.below()).is(Blocks.SNOW_BLOCK);
    }

    private static boolean hasObserver(ServerLevel level, BlockPos center) {
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator()
                    && player.distanceToSqr(Vec3.atCenterOf(center)) <= 112.0D * 112.0D) {
                return true;
            }
        }
        return false;
    }

    private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private static long cellKey(int x, int z) {
        return (x & 0xffffffffL) | ((z & 0xffffffffL) << 32);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LEVELS.remove(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LEVELS.clear();
    }

    private static final class LevelTides {
        private final Map<Long, TideEvent> events = new LinkedHashMap<>();
        private final Map<UUID, Long> playerAttempts = new HashMap<>();
    }

    private static final class TideEvent {
        private final BlockPos origin;
        private BlockPos center;
        private final int target;
        private final SparrowTideRules.Timeline timeline;
        private final List<Integer> memberIds = new ArrayList<>();
        private int localSparrows = -1;
        private int spawned;
        private int waveRemaining;
        private int waveAttempts;
        private double waveAngle;
        private long nextWave;
        private long nextMove;
        private long nextMemberRefresh;
        private long cooldownUntil;

        private TideEvent(BlockPos center, int target, long now, RandomSource random) {
            this.origin = center;
            this.center = center;
            this.target = target;
            this.timeline = new SparrowTideRules.Timeline(now,
                    Mth.nextInt(random, SparrowTideRules.ACTIVE_MIN, SparrowTideRules.ACTIVE_MAX));
            this.nextWave = now + Mth.nextInt(random, SparrowTideRules.WAVE_DELAY_MIN, SparrowTideRules.WAVE_DELAY_MAX);
            this.nextMove = now + SparrowTideRules.GATHERING_TICKS + Mth.nextInt(random, 40 * 20, 120 * 20);
            this.waveAngle = random.nextDouble() * Math.PI * 2.0D;
        }
    }
}
