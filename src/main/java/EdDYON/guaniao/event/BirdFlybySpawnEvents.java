package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.columbid.AbstractColumbidEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.seagull.SeagullEntity;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdFlybySpawnEvents {
    private static final TagKey<Biome> NIGHT_HERON_HABITAT = biomeTag("night_heron_habitat");
    private static final TagKey<Biome> SPARROW_HABITAT = biomeTag("sparrow_habitat");
    private static final TagKey<Biome> BUDGERIGAR_HABITAT = biomeTag("budgerigar_habitat");
    private static final TagKey<Biome> SPOTTED_DOVE_HABITAT = biomeTag("spotted_dove_habitat");
    private static final TagKey<Biome> PIGEON_HABITAT = biomeTag("pigeon_habitat");
    private static final Map<UUID, Integer> PLAYER_COOLDOWNS = new HashMap<>();
    private static final Map<ServerLevel, Long> DIMENSION_ATTEMPTS = new WeakHashMap<>();
    private static final double VIEW_CHECK_RADIUS_SQR = 96.0D * 96.0D;
    private static final double MIN_HIDDEN_SPAWN_DISTANCE_SQR = 18.0D * 18.0D;
    private static final double VISIBLE_VIEW_DOT = 0.38D;

    private BirdFlybySpawnEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || BirdConfigManager.maxBirdsNearby() <= 0) {
            return;
        }
        if (!isPlayerNearSurface(level, player)) {
            return;
        }
        UUID playerId = player.getUUID();
        int cooldown = PLAYER_COOLDOWNS.getOrDefault(playerId, 0);
        if (cooldown > 0) {
            PLAYER_COOLDOWNS.put(playerId, cooldown - 1);
            return;
        }
        RandomSource random = level.getRandom();
        if (random.nextInt(120) != 0) {
            return;
        }
        long now = level.getGameTime();
        Long lastAttempt = DIMENSION_ATTEMPTS.get(level);
        if (lastAttempt != null && now - lastAttempt < 20L) {
            return;
        }
        DIMENSION_ATTEMPTS.put(level, now);
        if (trySpawnFlyby(level, player, random)) {
            PLAYER_COOLDOWNS.put(playerId, 450 + random.nextInt(451));
        } else {
            PLAYER_COOLDOWNS.put(playerId, 80 + random.nextInt(121));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        PLAYER_COOLDOWNS.remove(playerId);
        SeagullEntity.clearTheftTargetsFor(playerId);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            DIMENSION_ATTEMPTS.remove(level);
        }
    }

    private static boolean trySpawnFlyby(ServerLevel level, ServerPlayer player, RandomSource random) {
        Vec3 forward = horizontalForward(player);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double ahead = 30.0D + random.nextDouble() * 28.0D;
        Vec3 center = player.position().add(forward.scale(ahead));
        BlockPos centerPos = BlockPos.containing(center);
        if (!level.hasChunk(centerPos.getX() >> 4, centerPos.getZ() >> 4)) {
            return false;
        }
        BirdKind kind = chooseKind(level, centerPos, random);
        if (kind == null) {
            return false;
        }
        int speciesLimit = Math.min(kind.maxNearby, BirdConfigManager.maxWildNearby(kind.species()));
        int currentSpeciesCount = nearbyCount(level, player, kind);
        if (speciesLimit <= 0 || currentSpeciesCount >= speciesLimit) {
            return false;
        }
        int globalAllowed = BirdConfigManager.maxBirdsNearby()
                - BirdPopulationTracker.totalAt(level, player.getX(), player.getZ());
        if (globalAllowed <= 0) {
            return false;
        }
        int regionalLimit = BirdConfigManager.maxWildBirdsPerRegion();
        int regionalAllowed = regionalLimit <= 0
                ? Integer.MAX_VALUE
                : regionalLimit - BirdPopulationTracker.totalInRegionAt(level, center.x, center.z);
        if (regionalAllowed <= 0) {
            return false;
        }
        int sideSign = random.nextBoolean() ? 1 : -1;
        double sideDistance = kind.sideDistance + 18.0D + random.nextDouble() * kind.sideVariance;
        double behind = 18.0D + random.nextDouble() * 18.0D;
        Vec3 startBase = player.position()
                .add(forward.scale(-behind))
                .add(right.scale(sideSign * sideDistance));
        Vec3 targetBase = center
                .add(forward.scale(8.0D + random.nextDouble() * 14.0D))
                .add(right.scale(-sideSign * (sideDistance + 8.0D + random.nextDouble() * 10.0D)));
        FlybyPath path = new FlybyPath(forward, right, startBase, targetBase);
        int speciesAllowed = Math.max(0, speciesLimit - currentSpeciesCount);
        int count = Math.min(Math.min(Math.min(kind.groupCount(random), speciesAllowed), globalAllowed), regionalAllowed);
        return switch (kind) {
            case NIGHT_HERON -> spawnNightHeronFlyby(level, path, count, random);
            case SPARROW -> spawnSparrowFlyby(level, path, count, random);
            case BUDGERIGAR -> spawnBudgerigarFlyby(level, path, count, random);
            case SPOTTED_DOVE, PIGEON -> spawnColumbidFlyby(level, path, count, kind, random);
        };
    }

    private static BirdKind chooseKind(ServerLevel level, BlockPos pos, RandomSource random) {
        List<BirdKind> candidates = new ArrayList<>();
        int totalWeight = 0;
        BlockPos surfacePos = new BlockPos(pos.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()), pos.getZ());
        for (BirdKind kind : BirdKind.values()) {
            if (!BirdConfigManager.allowsNaturalSpawning(kind.species())
                    || !kind.isActive(level) || !level.getBiome(pos).is(kind.habitatTag)) {
                continue;
            }
            if (kind != BirdKind.NIGHT_HERON && level.getRawBrightness(surfacePos, 0) <= 8) {
                continue;
            }
            candidates.add(kind);
            totalWeight += kind.effectiveWeight();
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int roll = random.nextInt(totalWeight);
        for (BirdKind kind : candidates) {
            roll -= kind.effectiveWeight();
            if (roll < 0) {
                return kind;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private static boolean spawnSparrowFlyby(ServerLevel level, FlybyPath path, int count, RandomSource random) {
        boolean spawned = false;
        for (int index = 0; index < count; ++index) {
            Vec3 spawnPos = path.offsetStart(index, count, random, 1.0D);
            Vec3 airPos = findAirPoint(level, spawnPos.x, spawnPos.z, BirdKind.SPARROW, random);
            BlockPos landing = findDryLandingSurface(level, BlockPos.containing(path.offsetTarget(index, count, random, 1.4D)), 12);
            if (airPos == null || landing == null) {
                continue;
            }
            if (!isHiddenFromNearbyPlayers(level, airPos)) {
                continue;
            }
            SparrowEntity sparrow = GuaniaoEntityTypes.SPARROW.get().create(level);
            if (sparrow == null) {
                continue;
            }
            Vec3 target = Vec3.atBottomCenterOf(landing).add(0.0D, 0.08D, 0.0D);
            Vec3 direction = target.subtract(airPos).multiply(1.0D, 0.0D, 1.0D).normalize();
            placeMobForFlyby(sparrow, airPos, direction);
            sparrow.finalizeSpawn(level, level.getCurrentDifficultyAt(sparrow.blockPosition()), MobSpawnType.NATURAL, null, null);
            BirdPopulationTracker.markTransientFlyby(sparrow);
            if (!level.noCollision((Entity)sparrow, sparrow.getBoundingBox()) || !sparrow.startFlybyFlight(target)) {
                continue;
            }
            level.addFreshEntity(sparrow);
            spawned = true;
        }
        return spawned;
    }

    private static boolean spawnBudgerigarFlyby(ServerLevel level, FlybyPath path, int count, RandomSource random) {
        boolean spawned = false;
        for (int index = 0; index < count; ++index) {
            Vec3 spawnPos = path.offsetStart(index, count, random, 1.2D);
            Vec3 targetPos = path.offsetTarget(index, count, random, 1.8D);
            Vec3 airPos = findAirPoint(level, spawnPos.x, spawnPos.z, BirdKind.BUDGERIGAR, random);
            Vec3 airTarget = findAirPoint(level, targetPos.x, targetPos.z, BirdKind.BUDGERIGAR, random);
            if (airPos == null || airTarget == null) {
                continue;
            }
            if (!isHiddenFromNearbyPlayers(level, airPos)) {
                continue;
            }
            BudgerigarEntity budgerigar = GuaniaoEntityTypes.BUDGERIGAR.get().create(level);
            if (budgerigar == null) {
                continue;
            }
            Vec3 direction = airTarget.subtract(airPos).multiply(1.0D, 0.0D, 1.0D).normalize();
            placeMobForFlyby(budgerigar, airPos, direction);
            budgerigar.finalizeSpawn(level, level.getCurrentDifficultyAt(budgerigar.blockPosition()), MobSpawnType.NATURAL, null, null);
            BirdPopulationTracker.markTransientFlyby(budgerigar);
            if (!level.noCollision((Entity)budgerigar, budgerigar.getBoundingBox())) {
                continue;
            }
            budgerigar.startFlybyFlight(airTarget);
            level.addFreshEntity(budgerigar);
            spawned = true;
        }
        return spawned;
    }

    private static boolean spawnNightHeronFlyby(ServerLevel level, FlybyPath path, int count, RandomSource random) {
        boolean spawned = false;
        for (int index = 0; index < count; ++index) {
            Vec3 spawnPos = path.offsetStart(index, count, random, 1.8D);
            Vec3 targetPos = path.offsetTarget(index, count, random, 2.2D);
            Vec3 airPos = findAirPoint(level, spawnPos.x, spawnPos.z, BirdKind.NIGHT_HERON, random);
            BlockPos landing = findDryLandingSurface(level, BlockPos.containing(targetPos), 18);
            if (airPos == null || landing == null) {
                continue;
            }
            if (!isHiddenFromNearbyPlayers(level, airPos)) {
                continue;
            }
            NightHeronEntity nightHeron = GuaniaoEntityTypes.NIGHT_HERON.get().create(level);
            if (nightHeron == null) {
                continue;
            }
            Vec3 direction = Vec3.atBottomCenterOf(landing).subtract(airPos).multiply(1.0D, 0.0D, 1.0D).normalize();
            placeMobForFlyby(nightHeron, airPos, direction);
            nightHeron.finalizeSpawn(level, level.getCurrentDifficultyAt(nightHeron.blockPosition()), MobSpawnType.NATURAL, null, null);
            BirdPopulationTracker.markTransientFlyby(nightHeron);
            if (!level.noCollision((Entity)nightHeron, nightHeron.getBoundingBox())) {
                continue;
            }
            nightHeron.startFlybyFlight(direction, landing, 145 + random.nextInt(96));
            level.addFreshEntity(nightHeron);
            spawned = true;
        }
        return spawned;
    }

    private static boolean spawnColumbidFlyby(ServerLevel level, FlybyPath path, int count, BirdKind kind, RandomSource random) {
        boolean spawned = false;
        for (int index = 0; index < count; ++index) {
            Vec3 spawnPos = path.offsetStart(index, count, random, 1.65D);
            Vec3 targetPos = path.offsetTarget(index, count, random, 2.0D);
            Vec3 airPos = findAirPoint(level, spawnPos.x, spawnPos.z, kind, random);
            BlockPos landing = findDryLandingSurface(level, BlockPos.containing(targetPos), 16);
            if (airPos == null || landing == null) {
                continue;
            }
            if (!isHiddenFromNearbyPlayers(level, airPos)) {
                continue;
            }
            AbstractColumbidEntity columbid = switch (kind) {
                case SPOTTED_DOVE -> GuaniaoEntityTypes.SPOTTED_DOVE.get().create(level);
                case PIGEON -> GuaniaoEntityTypes.PIGEON.get().create(level);
                default -> null;
            };
            if (columbid == null) {
                continue;
            }
            Vec3 target = Vec3.atBottomCenterOf(landing).add(0.0D, 0.08D, 0.0D);
            Vec3 direction = target.subtract(airPos).multiply(1.0D, 0.0D, 1.0D);
            if (direction.lengthSqr() <= 1.0E-4D) {
                direction = path.forward;
            }
            direction = direction.normalize();
            placeMobForFlyby(columbid, airPos, direction);
            columbid.finalizeSpawn(level, level.getCurrentDifficultyAt(columbid.blockPosition()), MobSpawnType.NATURAL, null, null);
            BirdPopulationTracker.markTransientFlyby(columbid);
            if (!level.noCollision((Entity)columbid, columbid.getBoundingBox())) {
                continue;
            }
            if (!columbid.startFlybyFlight(target, 150 + random.nextInt(91))) {
                continue;
            }
            level.addFreshEntity(columbid);
            spawned = true;
        }
        return spawned;
    }

    private static Vec3 findAirPoint(ServerLevel level, double x, double z, BirdKind kind, RandomSource random) {
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        if (!level.hasChunk(blockX >> 4, blockZ >> 4)) {
            return null;
        }
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
        BlockPos surface = new BlockPos(blockX, surfaceY, blockZ);
        if (surfaceY <= level.getMinBuildHeight() || surfaceY >= level.getMaxBuildHeight() - kind.maxAltitude - 2 || isWetSurface(level, surface)) {
            return null;
        }
        int altitude = kind.minAltitude + random.nextInt(kind.maxAltitude - kind.minAltitude + 1);
        for (int yOffset = 0; yOffset <= 4; ++yOffset) {
            BlockPos airPos = new BlockPos(blockX, surfaceY + altitude + yOffset, blockZ);
            if (isOpenAir(level, airPos)) {
                return new Vec3(blockX + 0.5D, airPos.getY() + random.nextDouble() * 0.35D, blockZ + 0.5D);
            }
        }
        return null;
    }

    private static BlockPos findDryLandingSurface(ServerLevel level, BlockPos center, int verticalRange) {
        if (!level.hasChunk(center.getX() >> 4, center.getZ() >> 4)) {
            return null;
        }
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
        BlockPos first = new BlockPos(center.getX(), surfaceY, center.getZ());
        if (isSafeLanding(level, first)) {
            return first;
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int radius = 1; radius <= 7; ++radius) {
            for (int xOffset = -radius; xOffset <= radius; ++xOffset) {
                for (int zOffset = -radius; zOffset <= radius; ++zOffset) {
                    if (Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                        continue;
                    }
                    int x = center.getX() + xOffset;
                    int z = center.getZ() + zOffset;
                    if (!level.hasChunk(x >> 4, z >> 4)) {
                        continue;
                    }
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    for (int yOffset = 0; yOffset <= verticalRange; ++yOffset) {
                        mutable.set(x, y - yOffset, z);
                        if (isSafeLanding(level, mutable)) {
                            return mutable.immutable();
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeLanding(ServerLevel level, BlockPos pos) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        if (!feet.getCollisionShape((BlockGetter)level, pos).isEmpty() || !head.getCollisionShape((BlockGetter)level, pos.above()).isEmpty()) {
            return false;
        }
        if (isBlockedFluid(level, pos) || isBlockedFluid(level, pos.below())) {
            return false;
        }
        if (below.isAir() || below.is(Blocks.CACTUS) || below.is(Blocks.MAGMA_BLOCK)) {
            return false;
        }
        return below.isFaceSturdy((BlockGetter)level, pos.below(), Direction.UP)
                || below.is(Blocks.FARMLAND)
                || below.is(Blocks.HAY_BLOCK)
                || below.is(Blocks.COMPOSTER)
                || below.is(Blocks.MUD)
                || below.is(Blocks.CLAY)
                || below.is(Blocks.SAND)
                || below.is(Blocks.RED_SAND)
                || below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(BlockTags.LEAVES)
                || below.is(BlockTags.LOGS)
                || below.getBlock() instanceof FenceBlock
                || below.getBlock() instanceof FenceGateBlock;
    }

    private static boolean isOpenAir(ServerLevel level, BlockPos pos) {
        return level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                && level.getBlockState(pos).getCollisionShape((BlockGetter)level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape((BlockGetter)level, pos.above()).isEmpty()
                && !isBlockedFluid(level, pos)
                && !isBlockedFluid(level, pos.above());
    }

    private static boolean isWetSurface(ServerLevel level, BlockPos pos) {
        return isBlockedFluid(level, pos) || isBlockedFluid(level, pos.below());
    }

    private static boolean isBlockedFluid(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER) || level.getFluidState(pos).is(FluidTags.LAVA);
    }

    private static void placeMobForFlyby(Mob mob, Vec3 pos, Vec3 direction) {
        float yaw = (float)(Mth.atan2(direction.z, direction.x) * 57.29577951308232D) - 90.0F;
        mob.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
        mob.fallDistance = 0.0F;
    }

    private static int nearbyCount(ServerLevel level, ServerPlayer player, BirdKind kind) {
        return BirdPopulationTracker.speciesAt(level, player.getX(), player.getZ(), kind.species());
    }

    private static boolean isHiddenFromNearbyPlayers(ServerLevel level, Vec3 spawnPos) {
        for (ServerPlayer observer : level.players()) {
            if (observer.isSpectator()) {
                continue;
            }
            double distanceSqr = observer.distanceToSqr(spawnPos);
            if (distanceSqr < MIN_HIDDEN_SPAWN_DISTANCE_SQR) {
                return false;
            }
            if (distanceSqr > VIEW_CHECK_RADIUS_SQR) {
                continue;
            }
            if (canPlayerSeeSpawnPoint(level, observer, spawnPos)) {
                return false;
            }
        }
        return true;
    }

    private static boolean canPlayerSeeSpawnPoint(ServerLevel level, ServerPlayer player, Vec3 spawnPos) {
        Vec3 eye = player.getEyePosition();
        Vec3 toSpawn = spawnPos.subtract(eye);
        if (toSpawn.lengthSqr() <= 1.0E-4D) {
            return true;
        }
        double lookDot = player.getLookAngle().normalize().dot(toSpawn.normalize());
        if (lookDot < VISIBLE_VIEW_DOT) {
            return false;
        }
        return hasClearLineOfSight(level, player, eye, spawnPos);
    }

    private static boolean hasClearLineOfSight(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to) {
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        return level.clip(context).getType() == HitResult.Type.MISS;
    }

    private static boolean isPlayerNearSurface(ServerLevel level, ServerPlayer player) {
        int x = Mth.floor(player.getX());
        int z = Mth.floor(player.getZ());
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return player.getY() >= surfaceY - 6.0D && player.getY() <= surfaceY + 24.0D;
    }

    private static Vec3 horizontalForward(ServerPlayer player) {
        Vec3 forward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() <= 1.0E-4D) {
            float yaw = player.getYRot() * ((float)Math.PI / 180.0F);
            forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        }
        return forward.normalize();
    }

    private static TagKey<Biome> biomeTag(String id) {
        return TagKey.create(Registries.BIOME, new ResourceLocation(GuaniaoMod.MOD_ID, id));
    }

    private enum BirdKind {
        NIGHT_HERON(NIGHT_HERON_HABITAT, 3, 8, 17, 20.0D, 8.0D, 4),
        SPARROW(SPARROW_HABITAT, 9, 4, 9, 15.0D, 8.0D, 24),
        BUDGERIGAR(BUDGERIGAR_HABITAT, 8, 6, 13, 17.0D, 8.0D, 22),
        SPOTTED_DOVE(SPOTTED_DOVE_HABITAT, 6, 10, 21, 21.0D, 10.0D, 14),
        PIGEON(PIGEON_HABITAT, 7, 11, 23, 22.0D, 11.0D, 16);

        private final TagKey<Biome> habitatTag;
        private final int weight;
        private final int minAltitude;
        private final int maxAltitude;
        private final double sideDistance;
        private final double sideVariance;
        private final int maxNearby;

        BirdKind(TagKey<Biome> habitatTag, int weight, int minAltitude, int maxAltitude, double sideDistance, double sideVariance, int maxNearby) {
            this.habitatTag = habitatTag;
            this.weight = weight;
            this.minAltitude = minAltitude;
            this.maxAltitude = maxAltitude;
            this.sideDistance = sideDistance;
            this.sideVariance = sideVariance;
            this.maxNearby = maxNearby;
        }

        private int groupCount(RandomSource random) {
            int configuredMin = BirdConfigManager.minGroup(this.species());
            int configuredMax = BirdConfigManager.maxGroup(this.species());
            return configuredMin + random.nextInt(configuredMax - configuredMin + 1);
        }

        private BirdSpecies species() {
            return BirdSpecies.valueOf(this.name());
        }

        private int effectiveWeight() {
            return Math.max(1, (int)Math.round(this.weight * BirdConfigManager.spawnMultiplier(this.species())));
        }

        private boolean isActive(ServerLevel level) {
            return switch (this) {
                case NIGHT_HERON -> BirdActivitySchedule.NOCTURNAL_CREPUSCULAR.isActiveTime(level.getDayTime());
                case SPARROW, BUDGERIGAR, SPOTTED_DOVE, PIGEON -> !level.isThundering()
                        && BirdActivitySchedule.DIURNAL.isActiveTime(level.getDayTime());
            };
        }
    }

    private static final class FlybyPath {
        private final Vec3 forward;
        private final Vec3 right;
        private final Vec3 startBase;
        private final Vec3 targetBase;

        private FlybyPath(Vec3 forward, Vec3 right, Vec3 startBase, Vec3 targetBase) {
            this.forward = forward;
            this.right = right;
            this.startBase = startBase;
            this.targetBase = targetBase;
        }

        private Vec3 offsetStart(int index, int count, RandomSource random, double spacing) {
            double lane = ((double)index - ((double)count - 1.0D) * 0.5D) * spacing;
            return this.startBase
                    .add(this.right.scale(lane + randomSigned(random, 1.1D)))
                    .add(this.forward.scale(randomSigned(random, 1.8D)));
        }

        private Vec3 offsetTarget(int index, int count, RandomSource random, double spacing) {
            double lane = ((double)index - ((double)count - 1.0D) * 0.5D) * spacing;
            return this.targetBase
                    .add(this.right.scale(-lane * 0.45D + randomSigned(random, 1.4D)))
                    .add(this.forward.scale(randomSigned(random, 2.0D)));
        }
    }

    private static double randomSigned(RandomSource random, double amount) {
        return (random.nextDouble() * 2.0D - 1.0D) * amount;
    }
}
