package EdDYON.guaniao.client.skybird;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public final class SkyBirdManager {
    public static final SkyBirdManager INSTANCE = new SkyBirdManager();

    public static final double NEAR_FADE_START = 45.0D;
    public static final double NEAR_FADE_END = 55.0D;

    private static final int MAX_POOLED_FLOCKS = 2;
    private static final int STARLING_RESPAWN_MIN_TICKS = 20 * 6;
    private static final int STARLING_RESPAWN_VARIANCE_TICKS = 20 * 6;
    private static final int SPAWN_MIN_TICKS = 20 * 8;
    private static final int SPAWN_VARIANCE_TICKS = 20 * 6;
    private static final int EMPTY_SKY_RETRY_MIN_TICKS = 20 * 3;
    private static final int EMPTY_SKY_RETRY_VARIANCE_TICKS = 20 * 4;
    private static final int RESOURCE_RECHECK_TICKS = 100;
    private static final int ECOLOGY_REFRESH_TICKS = 100;
    private static final int ECOLOGY_REGION_SIZE = 64;
    private static final int SCENE_REFRESH_MIN_TICKS = 20 * 45;
    private static final int SCENE_REFRESH_VARIANCE_TICKS = 20 * 30;
    // Keep command-spawned flocks isolated briefly for visual inspection, then
    // let the normal ecology resume. A multi-minute override made a successful
    // debug command look as if it had permanently disabled natural spawning.
    private static final int DEBUG_OVERRIDE_TICKS = 20 * 12;

    private final List<SkyFlock> flocks = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private final EnumSet<SkyBirdSpecies> readySpecies = EnumSet.noneOf(SkyBirdSpecies.class);
    private final EnumSet<SkyBirdSpecies> warnedMissingTextures = EnumSet.noneOf(SkyBirdSpecies.class);
    private final EnumSet<SkyBirdSpecies> loggedFirstSpawns = EnumSet.noneOf(SkyBirdSpecies.class);
    private final EnumSet<SkyBirdSpecies> loggedFirstRenders = EnumSet.noneOf(SkyBirdSpecies.class);
    private final EnumSet<SkyBirdSpecies> activeEcology = EnumSet.noneOf(SkyBirdSpecies.class);

    private int spawnCooldown = 5;
    private int starlingSpawnCooldown = 20 * 3;
    private int resourceCheckCooldown;
    private int ecologyRefreshCooldown;
    private int sceneRefreshCooldown = SCENE_REFRESH_MIN_TICKS;
    private int debugOverrideTicks;
    private int lastRenderedBirdCount;
    private int lastRenderCandidateCount;
    private int lastInvalidMotionCount;
    private int lastDistanceRejectedCount;
    private int lastViewRejectedCount;
    private int lastLifecycleRejectedCount;
    private int naturalSpawnAttempts;
    private int naturalSpawnSuccesses;
    private int naturalStarlingSpawns;
    private int emptySkyMisses;
    private boolean renderStageSeen;
    private String lastNaturalResult = "none";
    private SkyBirdSpecies lastNaturalSpecies;

    private SkyBirdManager() {
    }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        if (!BirdConfigManager.skyBirdEcologyEnabled()) {
            clearFlocks();
            this.activeEcology.clear();
            this.ecologyRefreshCooldown = 0;
            this.debugOverrideTicks = 0;
            this.lastRenderedBirdCount = 0;
            return;
        }
        if (level.dimension() != Level.OVERWORLD) {
            clearFlocks();
            this.ecologyRefreshCooldown = 0;
            return;
        }

        updateTextureAvailability(minecraft);
        if (this.readySpecies.isEmpty()) {
            clearFlocks();
            this.activeEcology.clear();
            return;
        }

        boolean debugOverride = this.debugOverrideTicks > 0;
        if (debugOverride) {
            this.debugOverrideTicks--;
        } else {
            refreshEcology(level, player, false);
        }

        for (SkyFlock flock : this.flocks) {
            flock.tick(level);
        }
        double removalDistance = farFadeEnd(minecraft) + 72.0D;
        this.flocks.removeIf(flock -> !textureReady(flock.species())
                || flock.isExpired()
                || horizontalDistanceToSqr(flock.renderCenter(1.0F), player.position())
                > removalDistance * removalDistance);

        if (debugOverride && this.flocks.isEmpty()) {
            this.debugOverrideTicks = 0;
            this.ecologyRefreshCooldown = 0;
            debugOverride = false;
        }
        if (debugOverride) {
            return;
        }
        tickSceneRefresh();
        tickAmbientStarlings(level, player, minecraft);
        if (!hasPooledEcology()) {
            this.spawnCooldown = Math.max(this.spawnCooldown, 40);
            return;
        }
        if (this.spawnCooldown-- > 0) {
            return;
        }

        boolean emptySky = pooledFlockCount() == 0;
        this.spawnCooldown = emptySky
                ? EMPTY_SKY_RETRY_MIN_TICKS
                    + this.random.nextInt(EMPTY_SKY_RETRY_VARIANCE_TICKS + 1)
                : SPAWN_MIN_TICKS
                    + this.random.nextInt(SPAWN_VARIANCE_TICKS + 1);
        if (pooledFlockCount() >= MAX_POOLED_FLOCKS) {
            return;
        }
        this.naturalSpawnAttempts++;
        if (spawnNaturalBird(level, player, minecraft, emptySky)) {
            this.naturalSpawnSuccesses++;
        }
    }

    private boolean spawnNaturalBird(ClientLevel level, LocalPlayer player, Minecraft minecraft,
                                     boolean emptySky) {
        int[] weights = new int[SkyBirdSpecies.values().length];
        int totalWeight = 0;
        for (SkyBirdSpecies species : this.activeEcology) {
            // Murmurations are a separate ambient ecology layer. They have
            // their own slot and cooldown and never compete in this pool.
            if (species == SkyBirdSpecies.STARLING) {
                continue;
            }
            long currentFlocks = this.flocks.stream()
                    .filter(flock -> flock.species() == species && !flock.isRetiring())
                    .count();
            if (currentFlocks >= SkyBirdEcology.maxFlocks(species)) {
                continue;
            }
            int weight = SkyBirdEcology.spawnWeight(species, level);
            if (species == this.lastNaturalSpecies) {
                weight = Math.max(1, weight / 3);
            }
            weights[species.ordinal()] = weight;
            totalWeight += weight;
        }
        if (totalWeight <= 0) {
            this.lastNaturalResult = "no_eligible_weight";
            return false;
        }

        int spawnChancePercent = 15 + totalWeight / 2;
        if (emptySky) {
            spawnChancePercent += 30;
        }
        spawnChancePercent = emptySky
                ? Mth.clamp(spawnChancePercent, 55, 80)
                : Mth.clamp(spawnChancePercent, 25, 70);
        if (emptySky && this.emptySkyMisses >= 2) {
            // An eligible daytime habitat must not remain visually empty just
            // because several independent random rolls happened to miss.
            spawnChancePercent = 100;
        }
        if (this.random.nextInt(100) >= spawnChancePercent) {
            if (emptySky) {
                this.emptySkyMisses++;
            }
            this.lastNaturalResult = "chance_miss_" + spawnChancePercent;
            return false;
        }

        int roll = this.random.nextInt(totalWeight);
        SkyBirdSpecies selected = null;
        for (SkyBirdSpecies species : SkyBirdSpecies.values()) {
            roll -= weights[species.ordinal()];
            if (roll < 0) {
                selected = species;
                break;
            }
        }
        if (selected == null) {
            this.lastNaturalResult = "selection_failed";
            return false;
        }

        Vec3 ecologyCenter = Vec3.atCenterOf(ecologyRegionCenter(level, player));
        switch (selected) {
            case GOOSE -> spawnGooseMigration(level, player, minecraft, ecologyCenter);
            case EAGLE -> spawnEagleOrbit(level, player, ecologyCenter);
            case SEAGULL -> spawnSeagullGlide(level, player, minecraft, ecologyCenter);
            case SWALLOW -> spawnSwallowFlight(level, player, minecraft, ecologyCenter);
            case CRANE -> spawnCraneMigration(level, player, minecraft, ecologyCenter);
            case VULTURE -> spawnVultureThermal(level, player, ecologyCenter);
            // Unreachable from this pool: starlings carry zero natural weight and
            // are spawned by tickAmbientStarlings instead. Kept so the exhaustive
            // switch covers the enum if weights ever change.
            case STARLING -> spawnStarlingMurmuration(level, player, minecraft, ecologyCenter);
        }
        this.emptySkyMisses = 0;
        this.lastNaturalSpecies = selected;
        this.lastNaturalResult = "spawned_" + selected.name().toLowerCase();
        return true;
    }

    private void tickAmbientStarlings(ClientLevel level, LocalPlayer player, Minecraft minecraft) {
        if (!this.activeEcology.contains(SkyBirdSpecies.STARLING)) {
            this.starlingSpawnCooldown = Math.max(this.starlingSpawnCooldown, 40);
            return;
        }
        boolean starlingPresent = this.flocks.stream()
                .anyMatch(flock -> flock.species() == SkyBirdSpecies.STARLING);
        if (starlingPresent || this.starlingSpawnCooldown-- > 0) {
            return;
        }

        this.starlingSpawnCooldown = STARLING_RESPAWN_MIN_TICKS
                + this.random.nextInt(STARLING_RESPAWN_VARIANCE_TICKS + 1);
        Vec3 ecologyCenter = Vec3.atCenterOf(ecologyRegionCenter(level, player));
        spawnStarlingMurmuration(level, player, minecraft, ecologyCenter);
        this.naturalStarlingSpawns++;
    }

    private long pooledFlockCount() {
        return this.flocks.stream()
                .filter(flock -> flock.species() != SkyBirdSpecies.STARLING)
                .count();
    }

    private boolean hasPooledEcology() {
        return this.activeEcology.stream()
                .anyMatch(species -> species != SkyBirdSpecies.STARLING);
    }

    private void spawnStarlingMurmuration(ClientLevel level, LocalPlayer player, Minecraft minecraft,
                                          Vec3 ecologyCenter) {
        SkyBirdSpecies species = SkyBirdSpecies.STARLING;
        CrossingRoute route = crossingRoute(ecologyCenter, 44.0D,
                Math.min(70.0D, farFadeStart(minecraft) - 4.0D));
        Vec3 direction = route.direction();
        Vec3 horizontalStart = route.start();
        int surfaceY = Math.max(
                ecologyFlightSurface(level, ecologyCenter),
                surfaceHeightAt(level, horizontalStart)
        );
        double altitude = surfaceY + randomBetween(28.0D, 44.0D);
        Vec3 start = new Vec3(horizontalStart.x, altitude, horizontalStart.z);

        int birdCount = species.randomFlockSize(this.random);
        this.flocks.add(SkyFlock.murmuration(
                species,
                this.random.nextLong(),
                start,
                direction,
                birdCount,
                species.randomSpeed(this.random),
                20 * (36 + this.random.nextInt(30)),
                altitude
        ));
        if (this.loggedFirstSpawns.add(species)) {
            GuaniaoMod.LOGGER.info(
                    "Spawned first client sky starling murmuration: {} birds, distance {}, altitude {}",
                    birdCount,
                    Mth.floor(start.distanceTo(player.position())),
                    Mth.floor(altitude - surfaceY)
            );
        }
    }

    private void spawnVultureThermal(ClientLevel level, LocalPlayer player, Vec3 ecologyCenter) {
        SkyBirdSpecies species = SkyBirdSpecies.VULTURE;
        Vec3 radialDirection = naturalEventDirection();
        double centerDistance = randomBetween(12.0D, 36.0D);
        double radius = randomBetween(22.0D, 32.0D);
        Vec3 horizontalCenter = ecologyCenter.add(radialDirection.scale(centerDistance));
        int surfaceY = Math.max(
                ecologyFlightSurface(level, ecologyCenter),
                maxSurfaceHeightAround(level, horizontalCenter, radius + 18.0D)
        );
        double altitude = surfaceY + randomBetween(56.0D, 74.0D);
        Vec3 orbitCenter = new Vec3(horizontalCenter.x, altitude, horizontalCenter.z);

        double startAngle = this.random.nextDouble() * Mth.TWO_PI;
        int birdCount = species.randomFlockSize(this.random);
        this.flocks.add(SkyFlock.thermalSoaring(
                species,
                this.random.nextLong(),
                orbitCenter,
                radius,
                startAngle,
                species.randomSpeed(this.random),
                this.random.nextBoolean(),
                birdCount,
                20 * (70 + this.random.nextInt(51))
        ));
        if (this.loggedFirstSpawns.add(species)) {
            GuaniaoMod.LOGGER.info(
                    "Spawned first client sky vulture kettle: {} birds, radius {}, altitude {}",
                    birdCount,
                    Mth.floor(radius),
                    Mth.floor(altitude - surfaceY)
            );
        }
    }

    private void spawnCraneMigration(ClientLevel level, LocalPlayer player, Minecraft minecraft,
                                     Vec3 ecologyCenter) {
        SkyBirdSpecies species = SkyBirdSpecies.CRANE;
        CrossingRoute route = crossingRoute(ecologyCenter, 48.0D,
                Math.min(78.0D, farFadeStart(minecraft) - 4.0D));
        Vec3 direction = route.direction();
        Vec3 horizontalStart = route.start();
        int surfaceY = Math.max(
                ecologyFlightSurface(level, ecologyCenter),
                surfaceHeightAt(level, horizontalStart)
        );
        double altitude = surfaceY + randomBetween(40.0D, 60.0D);
        Vec3 start = new Vec3(horizontalStart.x, altitude, horizontalStart.z);

        int birdCount = species.randomFlockSize(this.random);
        this.flocks.add(SkyFlock.gracefulMigration(
                species,
                this.random.nextLong(),
                start,
                direction,
                birdCount,
                species.randomSpeed(this.random),
                20 * (50 + this.random.nextInt(31)),
                altitude
        ));
        if (this.loggedFirstSpawns.add(species)) {
            GuaniaoMod.LOGGER.info(
                    "Spawned first client sky crane flock: {} birds, distance {}, altitude {}",
                    birdCount,
                    Mth.floor(start.distanceTo(player.position())),
                    Mth.floor(altitude - surfaceY)
            );
        }
    }

    private void spawnSwallowFlight(ClientLevel level, LocalPlayer player, Minecraft minecraft,
                                    Vec3 ecologyCenter) {
        SkyBirdSpecies species = SkyBirdSpecies.SWALLOW;
        CrossingRoute route = crossingRoute(ecologyCenter, 44.0D,
                Math.min(70.0D, farFadeStart(minecraft) - 4.0D));
        Vec3 direction = route.direction();
        Vec3 horizontalStart = route.start();
        int surfaceY = Math.max(
                ecologyFlightSurface(level, ecologyCenter),
                surfaceHeightAt(level, horizontalStart)
        );
        double altitude = surfaceY + randomBetween(23.0D, 37.0D);
        Vec3 start = new Vec3(horizontalStart.x, altitude, horizontalStart.z);

        int birdCount = species.randomFlockSize(this.random);
        this.flocks.add(SkyFlock.aerialDart(
                species,
                this.random.nextLong(),
                start,
                direction,
                birdCount,
                species.randomSpeed(this.random),
                20 * (28 + this.random.nextInt(20)),
                altitude
        ));
        if (this.loggedFirstSpawns.add(species)) {
            GuaniaoMod.LOGGER.info(
                    "Spawned first client sky swallow flock: {} birds, distance {}, altitude {}",
                    birdCount,
                    Mth.floor(start.distanceTo(player.position())),
                    Mth.floor(altitude - surfaceY)
            );
        }
    }

    private void spawnSeagullGlide(ClientLevel level, LocalPlayer player, Minecraft minecraft,
                                   Vec3 ecologyCenter) {
        SkyBirdSpecies species = SkyBirdSpecies.SEAGULL;
        CrossingRoute route = crossingRoute(ecologyCenter, 46.0D,
                Math.min(76.0D, farFadeStart(minecraft) - 4.0D));
        Vec3 direction = route.direction();
        Vec3 horizontalStart = route.start();
        int surfaceY = Math.max(
                ecologyFlightSurface(level, ecologyCenter),
                surfaceHeightAt(level, horizontalStart)
        );
        double altitude = surfaceY + randomBetween(28.0D, 44.0D);
        Vec3 start = new Vec3(horizontalStart.x, altitude, horizontalStart.z);

        int birdCount = species.randomFlockSize(this.random);
        this.flocks.add(SkyFlock.coastal(
                species,
                this.random.nextLong(),
                start,
                direction,
                birdCount,
                species.randomSpeed(this.random),
                20 * (40 + this.random.nextInt(31)),
                altitude
        ));
        if (this.loggedFirstSpawns.add(species)) {
            GuaniaoMod.LOGGER.info(
                    "Spawned first client sky seagull flock: {} birds, distance {}, altitude {}",
                    birdCount,
                    Mth.floor(start.distanceTo(player.position())),
                    Mth.floor(altitude - surfaceY)
            );
        }
    }

    private void spawnGooseMigration(ClientLevel level, LocalPlayer player, Minecraft minecraft,
                                     Vec3 ecologyCenter) {
        SkyBirdSpecies species = SkyBirdSpecies.GOOSE;
        CrossingRoute route = crossingRoute(ecologyCenter, 46.0D,
                Math.min(84.0D, farFadeStart(minecraft) - 4.0D));
        Vec3 direction = route.direction();
        Vec3 horizontalStart = route.start();
        int surfaceY = Math.max(
                ecologyFlightSurface(level, ecologyCenter),
                surfaceHeightAt(level, horizontalStart)
        );
        double altitude = surfaceY + 45.0D + this.random.nextDouble() * 20.0D;
        Vec3 start = new Vec3(horizontalStart.x, altitude, horizontalStart.z);

        double speed = species.randomSpeed(this.random);
        int lifetime = 20 * (35 + this.random.nextInt(26));
        int birdCount = species.randomFlockSize(this.random);
        this.flocks.add(new SkyFlock(
                species,
                this.random.nextLong(),
                start,
                direction,
                birdCount,
                speed,
                lifetime,
                altitude
        ));
        if (this.loggedFirstSpawns.add(species)) {
            GuaniaoMod.LOGGER.info(
                    "Spawned first client sky goose flock: {} birds, distance {}, altitude {}",
                    birdCount,
                    Mth.floor(start.distanceTo(player.position())),
                    Mth.floor(altitude - surfaceY)
            );
        }
    }

    private void spawnEagleOrbit(ClientLevel level, LocalPlayer player, Vec3 ecologyCenter) {
        SkyBirdSpecies species = SkyBirdSpecies.EAGLE;
        Vec3 radialDirection = naturalEventDirection();
        double centerDistance = randomBetween(12.0D, 36.0D);
        double radius = randomBetween(15.0D, 23.0D);
        Vec3 horizontalCenter = ecologyCenter.add(radialDirection.scale(centerDistance));
        int surfaceY = Math.max(
                ecologyFlightSurface(level, ecologyCenter),
                maxSurfaceHeightAround(level, horizontalCenter, radius + 6.0D)
        );
        double altitude = surfaceY + randomBetween(54.0D, 74.0D);
        Vec3 orbitCenter = new Vec3(horizontalCenter.x, altitude, horizontalCenter.z);

        double startAngle = this.random.nextDouble() * Mth.TWO_PI;
        double speed = species.randomSpeed(this.random);
        int lifetime = 20 * (55 + this.random.nextInt(46));
        this.flocks.add(SkyFlock.orbiting(
                species,
                this.random.nextLong(),
                orbitCenter,
                radius,
                startAngle,
                speed,
                this.random.nextBoolean(),
                lifetime
        ));
        if (this.loggedFirstSpawns.add(species)) {
            GuaniaoMod.LOGGER.info(
                    "Spawned first client sky eagle: orbit radius {}, distance {}, altitude {}",
                    Mth.floor(radius),
                    Mth.floor(orbitCenter.distanceTo(player.position())),
                    Mth.floor(altitude - surfaceY)
            );
        }
    }

    private void refreshEcology(ClientLevel level, LocalPlayer player, boolean force) {
        if (!force && this.ecologyRefreshCooldown > 0) {
            this.ecologyRefreshCooldown--;
            return;
        }
        this.ecologyRefreshCooldown = ECOLOGY_REFRESH_TICKS;
        BlockPos ecologyCenter = ecologyRegionCenter(level, player);
        EnumSet<SkyBirdSpecies> refreshed = SkyBirdEcology.availableSpecies(
                level,
                ecologyCenter,
                this.readySpecies
        );
        if (!force && refreshed.equals(this.activeEcology)) {
            return;
        }

        this.activeEcology.clear();
        this.activeEcology.addAll(refreshed);
        for (SkyFlock flock : this.flocks) {
            if (!this.activeEcology.contains(flock.species())) {
                flock.retire(40 + this.random.nextInt(41));
            }
        }
        this.spawnCooldown = Math.min(this.spawnCooldown, 20 + this.random.nextInt(41));
        this.starlingSpawnCooldown = Math.min(
                this.starlingSpawnCooldown,
                20 + this.random.nextInt(41)
        );
    }

    private void tickSceneRefresh() {
        if (this.sceneRefreshCooldown-- > 0) {
            return;
        }
        this.sceneRefreshCooldown = SCENE_REFRESH_MIN_TICKS
                + this.random.nextInt(SCENE_REFRESH_VARIANCE_TICKS + 1);
        this.spawnCooldown = Math.min(this.spawnCooldown, 20 + this.random.nextInt(41));
        if (pooledFlockCount() < MAX_POOLED_FLOCKS) {
            return;
        }

        SkyFlock oldest = null;
        for (SkyFlock flock : this.flocks) {
            if (flock.species() != SkyBirdSpecies.STARLING
                    && !flock.isRetiring()
                    && this.activeEcology.contains(flock.species())
                    && flock.age() >= 20 * 20
                    && (oldest == null || flock.age() > oldest.age())) {
                oldest = flock;
            }
        }
        if (oldest != null) {
            oldest.retire(50 + this.random.nextInt(31));
        }
    }

    public boolean refreshEcologyNow() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (!BirdConfigManager.skyBirdEcologyEnabled()
                || level == null || player == null
                || level.dimension() != Level.OVERWORLD) {
            return false;
        }
        this.resourceCheckCooldown = 0;
        updateTextureAvailability(minecraft);
        this.debugOverrideTicks = 0;
        refreshEcology(level, player, true);
        this.sceneRefreshCooldown = SCENE_REFRESH_MIN_TICKS
                + this.random.nextInt(SCENE_REFRESH_VARIANCE_TICKS + 1);
        return true;
    }

    public String ecologyRefreshBlockReason() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (!BirdConfigManager.skyBirdEcologyEnabled()) {
            return "请先在观鸟配置中开启天空鸟群生态。";
        }
        if (level == null || player == null) {
            return "尚未进入客户端世界。";
        }
        if (level.dimension() != Level.OVERWORLD) {
            return "天空鸟群生态只在主世界刷新。";
        }
        return "当前环境暂时无法刷新。";
    }

    public String ecologyStatus() {
        if (this.activeEcology.isEmpty()) {
            return "quiet";
        }
        StringBuilder result = new StringBuilder();
        for (SkyBirdSpecies species : this.activeEcology) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(species.name().toLowerCase());
        }
        return result.toString();
    }

    public String runtimeStatus() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        boolean overworld = level != null && level.dimension() == Level.OVERWORLD;
        BlockPos ecologyCenter = level != null && player != null
                ? ecologyRegionCenter(level, player)
                : null;
        return "enabled=" + BirdConfigManager.skyBirdEcologyEnabled()
                + ",overworld=" + overworld
                + ",worldAnchored=true"
                + ",ecologyCenter=" + (ecologyCenter == null
                    ? "none"
                    : ecologyCenter.getX() + "/" + ecologyCenter.getZ())
                + ",surfaceY=" + (ecologyCenter == null ? "none" : ecologyCenter.getY())
                + ",playerY=" + (player == null ? "none" : Mth.floor(player.getY()))
                + ",flockY=" + this.flocks.stream()
                    .map(flock -> flock.species().name().toLowerCase() + ":"
                            + Mth.floor(flock.renderCenter(1.0F).y))
                    .collect(java.util.stream.Collectors.joining("/", "[", "]"))
                + ",spawnCooldown=" + Math.max(0, this.spawnCooldown)
                + ",starlingCooldown=" + Math.max(0, this.starlingSpawnCooldown)
                + ",pooledFlocks=" + pooledFlockCount()
                + ",naturalAttempts=" + this.naturalSpawnAttempts
                + ",naturalSpawns=" + this.naturalSpawnSuccesses
                + ",naturalStarlingSpawns=" + this.naturalStarlingSpawns
                + ",lastNatural=" + this.lastNaturalResult
                + ",fadeRange=" + Mth.floor(farFadeStart(minecraft))
                + "-" + Mth.floor(farFadeEnd(minecraft))
                + ",shaderTransparency=" + Minecraft.useShaderTransparency()
                + ",renderStageSeen=" + this.renderStageSeen;
    }

    private void updateTextureAvailability(Minecraft minecraft) {
        if (this.resourceCheckCooldown-- > 0) {
            return;
        }
        this.resourceCheckCooldown = RESOURCE_RECHECK_TICKS;
        for (SkyBirdSpecies species : SkyBirdSpecies.values()) {
            boolean present = minecraft.getResourceManager().getResource(species.texture()).isPresent();
            if (present) {
                // The particles-target RenderType applies nearest sampling with
                // mipmaps when drawing these pixel-art strips. Do not fight that
                // state here by changing the texture filter every resource check.
                this.readySpecies.add(species);
                this.warnedMissingTextures.remove(species);
            } else {
                this.readySpecies.remove(species);
                if (this.warnedMissingTextures.add(species)) {
                    GuaniaoMod.LOGGER.warn(
                            "Sky bird rendering for {} is disabled because {} is missing",
                            species,
                            species.texture()
                    );
                }
            }
        }
    }

    private static int surfaceHeightAt(ClientLevel level, Vec3 position) {
        int x = Mth.floor(position.x);
        int z = Mth.floor(position.z);
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return level.getSeaLevel();
        }
        // NO_LEAVES is not sent in chunk packets: its client heightmap can
        // remain empty and return the world bottom, spawning birds underground.
        return level.getHeight(
                Heightmap.Types.MOTION_BLOCKING,
                x,
                z
        );
    }

    private static int ecologyFlightSurface(ClientLevel level, Vec3 ecologyCenter) {
        return Math.max(
                Mth.floor(ecologyCenter.y),
                maxSurfaceHeightAround(level, ecologyCenter, ECOLOGY_REGION_SIZE * 0.75D)
        );
    }

    /** Samples the whole orbit footprint so soaring birds do not start inside a distant ridge. */
    private static int maxSurfaceHeightAround(ClientLevel level, Vec3 center, double radius) {
        double diagonal = radius * 0.70710678118D;
        double[][] offsets = {
                {0.0D, 0.0D},
                {radius, 0.0D}, {-radius, 0.0D},
                {0.0D, radius}, {0.0D, -radius},
                {diagonal, diagonal}, {-diagonal, diagonal},
                {diagonal, -diagonal}, {-diagonal, -diagonal}
        };
        int highest = Integer.MIN_VALUE;
        for (double[] offset : offsets) {
            int x = Mth.floor(center.x + offset[0]);
            int z = Mth.floor(center.z + offset[1]);
            if (level.hasChunk(x >> 4, z >> 4)) {
                highest = Math.max(highest,
                        level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z));
            }
        }
        return highest == Integer.MIN_VALUE ? surfaceHeightAt(level, center) : highest;
    }

    /**
     * Selects the fixed world ecology region currently loaded around this
     * client. Player movement only changes which region is observed; natural
     * routes and altitude are derived from this snapped world position.
     */
    private static BlockPos ecologyRegionCenter(ClientLevel level, LocalPlayer player) {
        int regionX = Math.floorDiv(player.getBlockX(), ECOLOGY_REGION_SIZE);
        int regionZ = Math.floorDiv(player.getBlockZ(), ECOLOGY_REGION_SIZE);
        int centerX = regionX * ECOLOGY_REGION_SIZE + ECOLOGY_REGION_SIZE / 2;
        int centerZ = regionZ * ECOLOGY_REGION_SIZE + ECOLOGY_REGION_SIZE / 2;
        int surfaceY = surfaceHeightAt(level, new Vec3(centerX, 0.0D, centerZ));
        return new BlockPos(centerX, surfaceY, centerZ);
    }

    public double farFadeStart(Minecraft minecraft) {
        return Math.max(72.0D, farFadeEnd(minecraft) - 36.0D);
    }

    public double farFadeEnd(Minecraft minecraft) {
        double viewBlocks = minecraft.options.getEffectiveRenderDistance() * 16.0D;
        // Sky flocks are client-only atmosphere objects and do not require the
        // terrain beneath them to be rendered. Keep a useful sky range even at
        // low chunk render distances so naturally spawned flocks stay visible.
        return Mth.clamp(viewBlocks * 0.90D, 160.0D, 220.0D);
    }

    public List<SkyFlock> flocks() {
        return Collections.unmodifiableList(this.flocks);
    }

    public int spawnDebugFlock(int requestedCount) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return 0;
        }

        this.resourceCheckCooldown = 0;
        updateTextureAvailability(minecraft);
        if (!textureReady(SkyBirdSpecies.GOOSE)) {
            return 0;
        }

        Vec3 sight = player.getViewVector(1.0F).normalize();
        if (sight.y < 0.24D) {
            sight = new Vec3(sight.x, 0.60D, sight.z).normalize();
        }
        Vec3 direction = horizontalDirection(sight);
        Vec3 center = player.getEyePosition().add(sight.scale(62.0D));
        int birdCount = Mth.clamp(requestedCount, 1, 32);

        beginDebugOverride();
        this.flocks.add(new SkyFlock(
                SkyBirdSpecies.GOOSE,
                this.random.nextLong(),
                center,
                direction,
                birdCount,
                0.32D,
                20 * 120,
                center.y
        ));
        return birdCount;
    }

    public int spawnDebugEagle() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return 0;
        }

        this.resourceCheckCooldown = 0;
        updateTextureAvailability(minecraft);
        if (!textureReady(SkyBirdSpecies.EAGLE)) {
            return 0;
        }

        Vec3 sight = player.getViewVector(1.0F).normalize();
        if (sight.y < 0.24D) {
            sight = new Vec3(sight.x, 0.60D, sight.z).normalize();
        }
        Vec3 visibleStart = player.getEyePosition().add(sight.scale(62.0D));
        double radius = 12.0D;
        Vec3 orbitCenter = visibleStart.subtract(radius, 0.0D, 0.0D);

        beginDebugOverride();
        this.flocks.add(SkyFlock.orbiting(
                SkyBirdSpecies.EAGLE,
                this.random.nextLong(),
                orbitCenter,
                radius,
                0.0D,
                0.22D,
                false,
                20 * 120
        ));
        return 1;
    }

    public int spawnDebugSeagulls(int requestedCount) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return 0;
        }

        this.resourceCheckCooldown = 0;
        updateTextureAvailability(minecraft);
        if (!textureReady(SkyBirdSpecies.SEAGULL)) {
            return 0;
        }

        Vec3 sight = player.getViewVector(1.0F).normalize();
        if (sight.y < 0.24D) {
            sight = new Vec3(sight.x, 0.60D, sight.z).normalize();
        }
        Vec3 direction = horizontalDirection(sight);
        Vec3 center = player.getEyePosition().add(sight.scale(62.0D));
        int birdCount = Mth.clamp(requestedCount, 1, 16);

        beginDebugOverride();
        this.flocks.add(SkyFlock.coastal(
                SkyBirdSpecies.SEAGULL,
                this.random.nextLong(),
                center,
                direction,
                birdCount,
                0.28D,
                20 * 120,
                center.y
        ));
        return birdCount;
    }

    public int spawnDebugSwallows(int requestedCount) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return 0;
        }

        this.resourceCheckCooldown = 0;
        updateTextureAvailability(minecraft);
        if (!textureReady(SkyBirdSpecies.SWALLOW)) {
            return 0;
        }

        Vec3 sight = player.getViewVector(1.0F).normalize();
        if (sight.y < 0.20D) {
            sight = new Vec3(sight.x, 0.48D, sight.z).normalize();
        }
        Vec3 direction = horizontalDirection(sight);
        Vec3 center = player.getEyePosition().add(sight.scale(58.0D));
        int birdCount = Mth.clamp(requestedCount, 1, 20);

        beginDebugOverride();
        this.flocks.add(SkyFlock.aerialDart(
                SkyBirdSpecies.SWALLOW,
                this.random.nextLong(),
                center,
                direction,
                birdCount,
                0.46D,
                20 * 120,
                center.y
        ));
        return birdCount;
    }

    public int spawnDebugCranes(int requestedCount) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return 0;
        }

        this.resourceCheckCooldown = 0;
        updateTextureAvailability(minecraft);
        if (!textureReady(SkyBirdSpecies.CRANE)) {
            return 0;
        }

        Vec3 sight = player.getViewVector(1.0F).normalize();
        if (sight.y < 0.24D) {
            sight = new Vec3(sight.x, 0.58D, sight.z).normalize();
        }
        Vec3 direction = horizontalDirection(sight);
        Vec3 center = player.getEyePosition().add(sight.scale(64.0D));
        int birdCount = Mth.clamp(requestedCount, 1, 12);

        beginDebugOverride();
        this.flocks.add(SkyFlock.gracefulMigration(
                SkyBirdSpecies.CRANE,
                this.random.nextLong(),
                center,
                direction,
                birdCount,
                0.28D,
                20 * 120,
                center.y
        ));
        return birdCount;
    }

    public int spawnDebugVultures(int requestedCount) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return 0;
        }

        this.resourceCheckCooldown = 0;
        updateTextureAvailability(minecraft);
        if (!textureReady(SkyBirdSpecies.VULTURE)) {
            return 0;
        }

        Vec3 sight = player.getViewVector(1.0F).normalize();
        if (sight.y < 0.28D) {
            sight = new Vec3(sight.x, 0.68D, sight.z).normalize();
        }
        Vec3 visibleStart = player.getEyePosition().add(sight.scale(66.0D));
        double radius = 15.0D;
        Vec3 orbitCenter = visibleStart.subtract(radius, 0.0D, 0.0D);
        int birdCount = Mth.clamp(requestedCount, 1, 6);

        beginDebugOverride();
        this.flocks.add(SkyFlock.thermalSoaring(
                SkyBirdSpecies.VULTURE,
                this.random.nextLong(),
                orbitCenter,
                radius,
                0.0D,
                0.16D,
                false,
                birdCount,
                20 * 150
        ));
        return birdCount;
    }

    public int spawnDebugStarlings(int requestedCount) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return 0;
        }

        this.resourceCheckCooldown = 0;
        updateTextureAvailability(minecraft);
        if (!textureReady(SkyBirdSpecies.STARLING)) {
            return 0;
        }

        Vec3 sight = player.getViewVector(1.0F).normalize();
        if (sight.y < 0.22D) {
            sight = new Vec3(sight.x, 0.52D, sight.z).normalize();
        }
        Vec3 direction = horizontalDirection(sight);
        Vec3 center = player.getEyePosition().add(sight.scale(58.0D));
        int birdCount = Mth.clamp(requestedCount, 1, 32);

        beginDebugOverride();
        this.flocks.add(SkyFlock.murmuration(
                SkyBirdSpecies.STARLING,
                this.random.nextLong(),
                center,
                direction,
                birdCount,
                0.39D,
                20 * 150,
                center.y
        ));
        return birdCount;
    }

    public void clearDebugFlocks() {
        clearFlocks();
        this.debugOverrideTicks = 0;
        this.ecologyRefreshCooldown = 0;
        this.sceneRefreshCooldown = SCENE_REFRESH_MIN_TICKS;
        this.spawnCooldown = 5;
        this.starlingSpawnCooldown = 20 * 3;
        this.emptySkyMisses = 0;
        this.lastRenderedBirdCount = 0;
    }

    public int totalBirdCount() {
        int total = 0;
        for (SkyFlock flock : this.flocks) {
            total += flock.birds().size();
        }
        return total;
    }

    public int lastRenderedBirdCount() {
        return this.lastRenderedBirdCount;
    }

    void setLastRenderedBirdCount(int count) {
        this.lastRenderedBirdCount = count;
    }

    void markRenderStageSeen() {
        this.renderStageSeen = true;
    }

    void setRenderDiagnostics(int candidates, int invalidMotion, int distanceRejected,
                              int viewRejected, int lifecycleRejected) {
        this.lastRenderCandidateCount = candidates;
        this.lastInvalidMotionCount = invalidMotion;
        this.lastDistanceRejectedCount = distanceRejected;
        this.lastViewRejectedCount = viewRejected;
        this.lastLifecycleRejectedCount = lifecycleRejected;
    }

    public String renderDiagnosticsStatus() {
        return "candidates=" + this.lastRenderCandidateCount
                + ",invalidMotion=" + this.lastInvalidMotionCount
                + ",distanceRejected=" + this.lastDistanceRejectedCount
                + ",viewRejected=" + this.lastViewRejectedCount
                + ",lifecycleRejected=" + this.lastLifecycleRejectedCount;
    }

    void recordRenderedSpecies(SkyBirdSpecies species, int renderedCount) {
        if (this.loggedFirstRenders.add(species)) {
            GuaniaoMod.LOGGER.info(
                    "Submitted first client sky {} flock: {} sprites (screen visibility not measured)",
                    species.name().toLowerCase(),
                    renderedCount
            );
        }
    }

    public boolean textureReady() {
        return !this.readySpecies.isEmpty();
    }

    public boolean textureReady(SkyBirdSpecies species) {
        return this.readySpecies.contains(species);
    }

    public String textureStatus() {
        StringBuilder result = new StringBuilder();
        for (SkyBirdSpecies species : SkyBirdSpecies.values()) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(species.name().toLowerCase())
                    .append('=')
                    .append(textureReady(species));
        }
        return result.toString();
    }

    public void clear() {
        clearFlocks();
        this.spawnCooldown = 5;
        this.starlingSpawnCooldown = 20 * 3;
        this.resourceCheckCooldown = 0;
        this.ecologyRefreshCooldown = 0;
        this.sceneRefreshCooldown = SCENE_REFRESH_MIN_TICKS;
        this.debugOverrideTicks = 0;
        this.readySpecies.clear();
        this.warnedMissingTextures.clear();
        this.loggedFirstSpawns.clear();
        this.loggedFirstRenders.clear();
        this.activeEcology.clear();
        this.lastNaturalSpecies = null;
        this.naturalSpawnAttempts = 0;
        this.naturalSpawnSuccesses = 0;
        this.naturalStarlingSpawns = 0;
        this.emptySkyMisses = 0;
        this.lastNaturalResult = "none";
        this.lastRenderedBirdCount = 0;
        this.lastRenderCandidateCount = 0;
        this.lastInvalidMotionCount = 0;
        this.lastDistanceRejectedCount = 0;
        this.lastViewRejectedCount = 0;
        this.lastLifecycleRejectedCount = 0;
        this.renderStageSeen = false;
    }

    private void beginDebugOverride() {
        clearFlocks();
        this.debugOverrideTicks = DEBUG_OVERRIDE_TICKS;
        this.lastNaturalSpecies = null;
    }

    private void clearFlocks() {
        this.flocks.clear();
    }

    private double randomBetween(double min, double max) {
        return min + this.random.nextDouble() * (max - min);
    }

    private static double horizontalDistanceToSqr(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    /**
     * Builds a world-space crossing through a fixed ecology region. Neither
     * the player's exact position nor camera heading influences the route.
     */
    private CrossingRoute crossingRoute(Vec3 ecologyCenter, double minStartDistance,
                                         double maxStartDistance) {
        double angle = this.random.nextDouble() * Mth.TWO_PI;
        Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        Vec3 right = new Vec3(-direction.z, 0.0D, direction.x);
        double approachDistance = randomBetween(minStartDistance, maxStartDistance);
        double lateralOffset = randomBetween(-24.0D, 24.0D);
        Vec3 start = ecologyCenter
                .subtract(direction.scale(approachDistance))
                .add(right.scale(lateralOffset));
        double headingVariation = randomBetween(-0.08D, 0.08D);
        return new CrossingRoute(
                start,
                direction.add(right.scale(headingVariation)).normalize()
        );
    }

    private Vec3 naturalEventDirection() {
        double angle = this.random.nextDouble() * Mth.TWO_PI;
        return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
    }

    private static Vec3 horizontalDirection(Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        return horizontal.normalize();
    }

    private record CrossingRoute(Vec3 start, Vec3 direction) {
    }

}
