package EdDYON.guaniao.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.BirdAmbientDropControl;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BirdConfigManager {
    public static final double BIRD_CAP_HORIZONTAL_RADIUS = 96.0D;
    public static final double BIRD_CAP_VERTICAL_RADIUS = 48.0D;
    public static final double DROPPING_CAP_RADIUS = 16.0D;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve(GuaniaoMod.MOD_ID);
    private static final Path GLOBAL_CONFIG_FILE = CONFIG_DIR.resolve("bird_settings.json");
    private static final DateTimeFormatter BROKEN_FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static volatile BirdConfigData config = defaultConfig();
    private static volatile BirdConfigScope activeScope = BirdConfigScope.GLOBAL;
    private static volatile boolean worldScopeAllowed;
    private static volatile Path activeWorldConfigFile;

    private BirdConfigManager() {
    }

    public static synchronized void loadOrCreateDefaults() {
        try {
            setActive(readOrCreate(GLOBAL_CONFIG_FILE, defaultConfig()), BirdConfigScope.GLOBAL, false, null);
        } catch (Exception exception) {
            GuaniaoMod.LOGGER.warn("Failed to load global bird settings, using defaults", exception);
            setActive(defaultConfig(), BirdConfigScope.GLOBAL, false, null);
        }
    }

    public static synchronized void loadForServer(MinecraftServer server) {
        try {
            BirdConfigData global = readOrCreate(GLOBAL_CONFIG_FILE, defaultConfig());
            boolean allowWorldScope = allowsWorldScope(server);
            Path worldFile = allowWorldScope ? worldConfigFile(server) : null;
            if (worldFile != null && Files.exists(worldFile)) {
                setActive(readOrCreate(worldFile, global), BirdConfigScope.WORLD, true, worldFile);
                return;
            }
            setActive(global, BirdConfigScope.GLOBAL, allowWorldScope, worldFile);
        } catch (Exception exception) {
            GuaniaoMod.LOGGER.warn("Failed to load bird settings for server, using defaults", exception);
            setActive(defaultConfig(), BirdConfigScope.GLOBAL, false, null);
        }
    }

    public static synchronized BirdConfigData snapshot() {
        BirdConfigData copy = config.copy();
        applyStorageMetadata(copy);
        return copy;
    }

    public static synchronized boolean replaceAndSave(BirdConfigData replacement) {
        return replaceAndSave(replacement, null);
    }

    public static synchronized boolean replaceAndSave(BirdConfigData replacement, MinecraftServer server) {
        boolean allowWorldScope = allowsWorldScope(server);
        Path worldFile = allowWorldScope ? worldConfigFile(server) : null;
        BirdConfigScope requestedScope = allowWorldScope ? BirdConfigScope.sanitize(replacement.storageScope) : BirdConfigScope.GLOBAL;
        BirdConfigData normalized = normalize(replacement);
        Path disabledWorldFile = null;
        try {
            if (requestedScope == BirdConfigScope.WORLD && worldFile != null) {
                write(worldFile, normalized);
                setActive(normalized, BirdConfigScope.WORLD, true, worldFile);
            } else {
                if (worldFile != null && Files.exists(worldFile)) {
                    disabledWorldFile = worldFile.resolveSibling(worldFile.getFileName() + ".switching");
                    moveAtomically(worldFile, disabledWorldFile);
                }
                write(GLOBAL_CONFIG_FILE, normalized);
                if (disabledWorldFile != null) {
                    Files.deleteIfExists(disabledWorldFile);
                }
                setActive(normalized, BirdConfigScope.GLOBAL, allowWorldScope, worldFile);
            }
            return true;
        } catch (Exception exception) {
            GuaniaoMod.LOGGER.warn("Failed to save bird settings", exception);
            if (disabledWorldFile != null && worldFile != null && Files.exists(disabledWorldFile)) {
                try {
                    moveAtomically(disabledWorldFile, worldFile);
                } catch (Exception restoreException) {
                    GuaniaoMod.LOGGER.error("Failed to restore world bird settings after save failure", restoreException);
                }
            }
            return false;
        }
    }

    public static BirdConfigData defaultConfig() {
        BirdConfigData defaults = new BirdConfigData();
        defaults.birds.clear();
        for (BirdSpecies species : BirdSpecies.values()) {
            defaults.birds.put(species.id(), new BirdSpeciesConfig(species));
        }
        return defaults;
    }

    public static boolean isEnabled(BirdSpecies species) {
        if (species == null) {
            return false;
        }
        return speciesConfig(species).enabled;
    }

    public static boolean allowsNaturalSpawning(BirdSpecies species) {
        BirdConfigData current = config;
        BirdSpeciesConfig bird = speciesConfig(current, species);
        return species != null && current.global.naturalSpawning && bird.enabled && bird.naturalSpawning
                && current.global.spawnMultiplier > 0.0D && bird.spawnMultiplier > 0.0D;
    }

    public static double spawnMultiplier(BirdSpecies species) {
        if (!allowsNaturalSpawning(species)) {
            return 0.0D;
        }
        BirdConfigData current = config;
        return current.global.spawnMultiplier * speciesConfig(current, species).spawnMultiplier;
    }

    public static boolean colonialMode() {
        return config.global.colonialMode;
    }

    public static boolean naturalCrowNests() {
        return config.global.naturalCrowNests;
    }

    public static double crowNestGenerationMultiplier() {
        return config.global.crowNestGenerationMultiplier;
    }

    public static boolean crowsStoreTreasures() {
        return config.global.crowsStoreTreasures;
    }

    public static boolean crowsClaimPlayerNests() {
        return config.global.crowsClaimPlayerNests;
    }

    public static int crowNestSearchDistance() {
        return config.global.crowNestSearchDistance;
    }

    public static int maxCrowNestTreasures() {
        return config.global.maxCrowNestTreasures;
    }

    public static int minGroup(BirdSpecies species) {
        return speciesConfig(species).minGroup;
    }

    public static int maxGroup(BirdSpecies species) {
        return speciesConfig(species).maxGroup;
    }

    public static double droppingMultiplier(BirdSpecies species) {
        BirdConfigData current = config;
        BirdSpeciesConfig bird = speciesConfig(current, species);
        if (species == null || !bird.enabled) {
            return 0.0D;
        }
        return current.global.droppingFrequencyMultiplier * bird.droppingFrequencyMultiplier;
    }

    public static double soundMultiplier(BirdSpecies species) {
        BirdConfigData current = config;
        BirdSpeciesConfig bird = speciesConfig(current, species);
        if (species == null || !bird.enabled) {
            return 0.0D;
        }
        return current.global.soundVolumeMultiplier * bird.soundVolumeMultiplier;
    }

    public static int maxBirdsNearby() {
        return config.global.maxBirdsNearby;
    }

    public static int maxGroundDroppingsNearby() {
        return config.global.maxGroundDroppingsNearby;
    }

    public static int maxWildBirdsPerRegion() { return config.global.maxWildBirdsPerRegion; }
    public static int populationRegionChunks() { return config.global.populationRegionChunks; }
    public static int wildBirdDespawnTicks() { return config.global.wildBirdDespawnTicks; }
    public static int flybyBirdLifetimeTicks() { return config.global.flybyBirdLifetimeTicks; }
    public static int flockRefreshTicks() { return config.global.flockRefreshTicks; }
    public static int habitatCacheTicks() { return config.global.habitatCacheTicks; }
    public static boolean petBirdCommandsEnabled() { return config.global.enablePetBirdCommands; }
    public static boolean seagullStealingEnabled() { return config.global.enableSeagullStealing; }
    public static boolean crowItemSafetyEnabled() { return config.global.crowItemSafety; }
    public static int seagullPlayerCooldownTicks() { return config.global.seagullPlayerCooldownTicks; }
    public static int maxConcurrentSeagullTargetsPerPlayer() { return config.global.maxConcurrentSeagullTargetsPerPlayer; }
    public static int birdScanBudgetPerTick() { return config.global.birdScanBudgetPerTick; }
    public static boolean migrationEnabled() { return config.global.enableMigration; }
    public static int migrationIntervalTicks() { return config.global.migrationIntervalTicks; }
    public static int migrationRadius() { return config.global.migrationRadius; }
    public static boolean birdsPassThroughLeaves() { return config.global.birdsPassThroughLeaves; }
    public static boolean aprilFoolsMode() { return config.global.aprilFoolsMode; }
    public static boolean droppingPressurePlatePulseEnabled() { return config.global.droppingPressurePlatePulseEnabled; }
    public static int droppingPressurePlatePulseTicks() { return config.global.droppingPressurePlatePulseTicks; }
    public static boolean photoUploadsEnabled() { return config.global.photoUploadsEnabled; }
    public static boolean photoUploadsOperatorOnly() { return config.global.photoUploadsOperatorOnly; }
    public static boolean photoUploadsWhitelistedOnly() { return config.global.photoUploadsWhitelistedOnly; }
    public static int maxPhotosPerPlayer() { return config.global.maxPhotosPerPlayer; }
    public static long maxPhotoBytesPerPlayer() { return (long)config.global.maxPhotoStorageMiBPerPlayer * 1024L * 1024L; }
    public static int maxPhotosPerWorld() { return config.global.maxPhotosPerWorld; }
    public static long maxPhotoBytesPerWorld() { return (long)config.global.maxPhotoStorageMiBPerWorld * 1024L * 1024L; }
    public static int photoTrashRetentionDays() { return config.global.photoTrashRetentionDays; }
    public static int maxConcurrentPhotoDownloads() { return config.global.maxConcurrentPhotoDownloads; }
    public static int photoDownloadBytesPerTick() { return config.global.photoDownloadKiBPerTick * 1024; }

    public static synchronized void applyRemoteRuntime(boolean birdsPassThroughLeaves, boolean aprilFoolsMode) {
        BirdConfigData updated = config.copy();
        updated.global.birdsPassThroughLeaves = birdsPassThroughLeaves;
        updated.global.aprilFoolsMode = aprilFoolsMode;
        config = updated;
    }
    public static int maxWildNearby(BirdSpecies species) { return speciesConfig(species).maxWildNearby; }
    public static double flockRadius(BirdSpecies species) { return speciesConfig(species).flockRadius; }
    public static int flockMaxMembers(BirdSpecies species) { return speciesConfig(species).flockMaxMembers; }
    public static int foodScanInterval(BirdSpecies species) { return speciesConfig(species).foodScanInterval; }
    public static int threatScanInterval(BirdSpecies species) { return speciesConfig(species).threatScanInterval; }
    public static double ownerTeleportDistance(BirdSpecies species) { return speciesConfig(species).ownerTeleportDistance; }
    public static double ambientSoundCooldownMultiplier(BirdSpecies species) { return speciesConfig(species).ambientSoundCooldownMultiplier; }

    private static boolean allowsWorldScope(MinecraftServer server) {
        return server != null && server.isSingleplayer();
    }

    private static Path worldConfigFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(GuaniaoMod.MOD_ID).resolve("bird_settings.json");
    }

    private static BirdConfigData readOrCreate(Path file, BirdConfigData fallback) throws Exception {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            BirdConfigData initialized = normalize(fallback);
            write(file, initialized);
            return initialized;
        }
        BirdConfigData loaded;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            BirdConfigData parsed = GSON.fromJson(reader, BirdConfigData.class);
            if (parsed == null) {
                throw new com.google.gson.JsonSyntaxException("Bird settings file is empty");
            }
            loaded = normalize(parsed);
        } catch (com.google.gson.JsonParseException exception) {
            Path broken = brokenConfigPath(file);
            GuaniaoMod.LOGGER.warn("Broken bird settings moved to {}", broken, exception);
            moveAtomically(file, broken);
            BirdConfigData recovered = normalize(fallback);
            write(file, recovered);
            return recovered;
        }
        write(file, loaded);
        return loaded;
    }

    private static void setActive(BirdConfigData loaded, BirdConfigScope scope, boolean allowWorldScope, Path worldFile) {
        config = normalize(loaded);
        activeScope = BirdConfigScope.sanitize(scope);
        worldScopeAllowed = allowWorldScope;
        activeWorldConfigFile = worldFile;
    }

    private static void applyStorageMetadata(BirdConfigData data) {
        data.storageScope = activeScope;
        data.worldScopeAllowed = worldScopeAllowed && activeWorldConfigFile != null;
    }

    private static BirdSpeciesConfig speciesConfig(BirdSpecies species) {
        return speciesConfig(config, species);
    }

    private static BirdSpeciesConfig speciesConfig(BirdConfigData source, BirdSpecies species) {
        if (species == null || source.birds == null) {
            return new BirdSpeciesConfig(species == null ? BirdSpecies.SPARROW : species);
        }
        BirdSpeciesConfig bird = source.birds.get(species.id());
        return bird == null ? new BirdSpeciesConfig(species) : bird;
    }

    private static BirdConfigData normalize(BirdConfigData input) {
        BirdConfigData defaults = defaultConfig();
        BirdConfigData normalized = new BirdConfigData();
        BirdGlobalConfig sourceGlobal = input == null || input.global == null ? defaults.global : input.global;
        normalized.global.naturalSpawning = sourceGlobal.naturalSpawning;
        normalized.global.colonialMode = sourceGlobal.colonialMode;
        normalized.global.naturalCrowNests = sourceGlobal.naturalCrowNests;
        normalized.global.crowsStoreTreasures = sourceGlobal.crowsStoreTreasures;
        normalized.global.crowsClaimPlayerNests = sourceGlobal.crowsClaimPlayerNests;
        normalized.global.enablePetBirdCommands = sourceGlobal.enablePetBirdCommands;
        normalized.global.enableSeagullStealing = sourceGlobal.enableSeagullStealing;
        normalized.global.crowItemSafety = sourceGlobal.crowItemSafety;
        normalized.global.birdsPassThroughLeaves = sourceGlobal.birdsPassThroughLeaves;
        normalized.global.aprilFoolsMode = sourceGlobal.aprilFoolsMode;
        normalized.global.droppingPressurePlatePulseEnabled = sourceGlobal.droppingPressurePlatePulseEnabled;
        normalized.global.photoUploadsEnabled = sourceGlobal.photoUploadsEnabled;
        normalized.global.photoUploadsOperatorOnly = sourceGlobal.photoUploadsOperatorOnly;
        normalized.global.photoUploadsWhitelistedOnly = sourceGlobal.photoUploadsWhitelistedOnly
                && !sourceGlobal.photoUploadsOperatorOnly;
        normalized.global.spawnMultiplier = finiteClamp(sourceGlobal.spawnMultiplier, 0.0D, 10.0D, 1.0D);
        normalized.global.crowNestGenerationMultiplier = finiteClamp(sourceGlobal.crowNestGenerationMultiplier, 0.0D, 5.0D, 1.0D);
        normalized.global.droppingFrequencyMultiplier = finiteClamp(sourceGlobal.droppingFrequencyMultiplier, 0.0D, 10.0D, 1.0D);
        normalized.global.soundVolumeMultiplier = finiteClamp(sourceGlobal.soundVolumeMultiplier, 0.0D, 4.0D, 1.0D);
        normalized.global.maxBirdsNearby = clamp(sourceGlobal.maxBirdsNearby, 0, 256);
        normalized.global.maxGroundDroppingsNearby = clamp(
                sourceGlobal.maxGroundDroppingsNearby,
                0,
                BirdAmbientDropControl.HARD_MAX_DROPPINGS_NEARBY
        );
        normalized.global.crowNestSearchDistance = clamp(sourceGlobal.crowNestSearchDistance, 16, 128);
        normalized.global.maxCrowNestTreasures = clamp(sourceGlobal.maxCrowNestTreasures, 1, 6);
        normalized.global.wildBirdDespawnTicks = clamp(sourceGlobal.wildBirdDespawnTicks, 200, 1728000);
        normalized.global.flybyBirdLifetimeTicks = clamp(sourceGlobal.flybyBirdLifetimeTicks, 200, 72000);
        normalized.global.maxWildBirdsPerRegion = clamp(sourceGlobal.maxWildBirdsPerRegion, 0, 1024);
        normalized.global.populationRegionChunks = clamp(sourceGlobal.populationRegionChunks, 1, 16);
        normalized.global.flockRefreshTicks = clamp(sourceGlobal.flockRefreshTicks, 5, 200);
        normalized.global.habitatCacheTicks = clamp(sourceGlobal.habitatCacheTicks, 20, 2400);
        normalized.global.seagullPlayerCooldownTicks = clamp(sourceGlobal.seagullPlayerCooldownTicks, 0, 72000);
        normalized.global.maxConcurrentSeagullTargetsPerPlayer = clamp(sourceGlobal.maxConcurrentSeagullTargetsPerPlayer, 0, 8);
        normalized.global.birdScanBudgetPerTick = clamp(sourceGlobal.birdScanBudgetPerTick, 1, 128);
        normalized.global.enableMigration = sourceGlobal.enableMigration;
        normalized.global.migrationIntervalTicks = clamp(sourceGlobal.migrationIntervalTicks, 200, 72000);
        normalized.global.migrationRadius = clamp(sourceGlobal.migrationRadius, 32, 512);
        normalized.global.droppingPressurePlatePulseTicks = clamp(sourceGlobal.droppingPressurePlatePulseTicks, 5, 100);
        normalized.global.maxPhotosPerPlayer = clamp(sourceGlobal.maxPhotosPerPlayer, 1, 10000);
        normalized.global.maxPhotoStorageMiBPerPlayer = clamp(sourceGlobal.maxPhotoStorageMiBPerPlayer, 1, 4096);
        normalized.global.maxPhotosPerWorld = clamp(sourceGlobal.maxPhotosPerWorld, 1, 100000);
        normalized.global.maxPhotoStorageMiBPerWorld = clamp(sourceGlobal.maxPhotoStorageMiBPerWorld, 1, 65536);
        normalized.global.photoTrashRetentionDays = clamp(sourceGlobal.photoTrashRetentionDays, 1, 90);
        normalized.global.maxConcurrentPhotoDownloads = clamp(sourceGlobal.maxConcurrentPhotoDownloads, 1, 128);
        normalized.global.photoDownloadKiBPerTick = clamp(sourceGlobal.photoDownloadKiBPerTick, 24, 2048);
        normalized.birds.clear();

        for (BirdSpecies species : BirdSpecies.values()) {
            BirdSpeciesConfig fallback = defaults.birds.get(species.id());
            BirdSpeciesConfig source = input == null || input.birds == null ? null : input.birds.get(species.id());
            if (source == null) {
                normalized.birds.put(species.id(), fallback.copy());
                continue;
            }
            BirdSpeciesConfig bird = new BirdSpeciesConfig(species);
            bird.enabled = source.enabled;
            bird.naturalSpawning = source.naturalSpawning;
            bird.spawnMultiplier = finiteClamp(source.spawnMultiplier, 0.0D, 10.0D, 1.0D);
            bird.minGroup = clamp(source.minGroup, 1, 32);
            bird.maxGroup = clamp(source.maxGroup, bird.minGroup, 32);
            bird.droppingFrequencyMultiplier = finiteClamp(source.droppingFrequencyMultiplier, 0.0D, 10.0D, 1.0D);
            bird.soundVolumeMultiplier = finiteClamp(source.soundVolumeMultiplier, 0.0D, 4.0D, 1.0D);
            bird.maxWildNearby = clamp(source.maxWildNearby, 0, 256);
            bird.flockRadius = finiteClamp(source.flockRadius, 2.0D, 48.0D, fallback.flockRadius);
            bird.flockMaxMembers = clamp(source.flockMaxMembers, 2, 64);
            bird.foodScanInterval = clamp(source.foodScanInterval, 5, 1200);
            bird.threatScanInterval = clamp(source.threatScanInterval, 5, 1200);
            bird.ownerTeleportDistance = finiteClamp(source.ownerTeleportDistance, 8.0D, 128.0D, fallback.ownerTeleportDistance);
            bird.ambientSoundCooldownMultiplier = finiteClamp(source.ambientSoundCooldownMultiplier, 0.25D, 8.0D, 1.0D);
            normalized.birds.put(species.id(), bird);
        }
        return normalized;
    }

    private static double finiteClamp(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void write(Path file, BirdConfigData values) throws Exception {
        Files.createDirectories(file.getParent());
        byte[] json = GSON.toJson(values, BirdConfigData.class).getBytes(StandardCharsets.UTF_8);
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (FileChannel channel = FileChannel.open(
                temporary,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(json);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
        moveAtomically(temporary, file);
    }

    private static Path brokenConfigPath(Path file) {
        String fileName = file.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        String base = extension < 0 ? fileName : fileName.substring(0, extension);
        String suffix = extension < 0 ? "" : fileName.substring(extension);
        return file.resolveSibling(base + ".broken-" + LocalDateTime.now().format(BROKEN_FILE_TIME) + suffix);
    }

    private static void moveAtomically(Path source, Path target) throws Exception {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
