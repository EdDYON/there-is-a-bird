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
    private int emptySkyMisses;
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
            return;
        }
        if (level.dimension() != Level.OVERWORLD) {
            clearFlocks();
            this.ecologyRefreshCooldown = 0;
            return;
        }

        // ClientLevel.hasChunk always returns true, even before chunk packets
        // arrive. EmptyChunk heightmaps report the world bottom (-64).
        BlockPos regionCenter = ecologyRegionCenter(level, player);
        if (!level.getChunkSource().hasChunk(regionCenter.getX() >> 4, regionCenter.getZ() >> 4)) {
            return;
        }

        updateTextureAvailability(minecraft);
        if (this.readySpecies.isEmpty()) {
            clearFlocks();
            this.activeEcology.clear();
            return;
        }

        refreshEcology(level, player);

        for (SkyFlock flock : this.flocks) {
            flock.tick(level);
        }
        double removalDistance = farFadeEnd(minecraft) + 72.0D;
        this.flocks.removeIf(flock -> !textureReady(flock.species())
                || flock.isExpired()
                || horizontalDistanceToSqr(flock.renderCenter(1.0F), player.position())
                > removalDistance * removalDistance);

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
        spawnNaturalBird(level, player, minecraft, emptySky);
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

    private void refreshEcology(ClientLevel level, LocalPlayer player) {
        if (this.ecologyRefreshCooldown > 0) {
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
        if (refreshed.equals(this.activeEcology)) {
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
        if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
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
            if (level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
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

    public void clear() {
        clearFlocks();
        this.spawnCooldown = 5;
        this.starlingSpawnCooldown = 20 * 3;
        this.resourceCheckCooldown = 0;
        this.ecologyRefreshCooldown = 0;
        this.sceneRefreshCooldown = SCENE_REFRESH_MIN_TICKS;
        this.readySpecies.clear();
        this.warnedMissingTextures.clear();
        this.loggedFirstSpawns.clear();
        this.loggedFirstRenders.clear();
        this.activeEcology.clear();
        this.lastNaturalSpecies = null;
        this.emptySkyMisses = 0;
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
