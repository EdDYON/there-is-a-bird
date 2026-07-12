package EdDYON.guaniao.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BirdConfigManager {
    public static final double BIRD_CAP_HORIZONTAL_RADIUS = 96.0D;
    public static final double BIRD_CAP_VERTICAL_RADIUS = 48.0D;
    public static final double DROPPING_CAP_RADIUS = 8.0D;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve(GuaniaoMod.MOD_ID);
    private static final Path GLOBAL_CONFIG_FILE = CONFIG_DIR.resolve("bird_settings.json");
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

    public static synchronized void replaceAndSave(BirdConfigData replacement) {
        replaceAndSave(replacement, null);
    }

    public static synchronized void replaceAndSave(BirdConfigData replacement, MinecraftServer server) {
        boolean allowWorldScope = allowsWorldScope(server);
        Path worldFile = allowWorldScope ? worldConfigFile(server) : null;
        BirdConfigScope requestedScope = allowWorldScope ? BirdConfigScope.sanitize(replacement.storageScope) : BirdConfigScope.GLOBAL;
        BirdConfigData normalized = normalize(replacement);
        try {
            if (requestedScope == BirdConfigScope.WORLD && worldFile != null) {
                write(worldFile, normalized);
                setActive(normalized, BirdConfigScope.WORLD, true, worldFile);
            } else {
                write(GLOBAL_CONFIG_FILE, normalized);
                if (worldFile != null) {
                    Files.deleteIfExists(worldFile);
                }
                setActive(normalized, BirdConfigScope.GLOBAL, allowWorldScope, worldFile);
            }
        } catch (Exception exception) {
            GuaniaoMod.LOGGER.warn("Failed to save bird settings", exception);
            setActive(normalized, requestedScope == BirdConfigScope.WORLD && worldFile != null ? BirdConfigScope.WORLD : BirdConfigScope.GLOBAL, allowWorldScope, worldFile);
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
        BirdSpeciesConfig bird = speciesConfig(species);
        return species != null && bird.enabled;
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

    private static boolean allowsWorldScope(MinecraftServer server) {
        return server != null && server.isSingleplayer();
    }

    private static Path worldConfigFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(GuaniaoMod.MOD_ID).resolve("bird_settings.json");
    }

    private static BirdConfigData readOrCreate(Path file, BirdConfigData fallback) throws Exception {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            write(file, fallback);
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            BirdConfigData loaded = normalize(GSON.fromJson(reader, BirdConfigData.class));
            write(file, loaded);
            return loaded;
        }
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
        normalized.global.spawnMultiplier = finiteClamp(sourceGlobal.spawnMultiplier, 0.0D, 10.0D, 1.0D);
        normalized.global.droppingFrequencyMultiplier = finiteClamp(sourceGlobal.droppingFrequencyMultiplier, 0.0D, 10.0D, 1.0D);
        normalized.global.soundVolumeMultiplier = finiteClamp(sourceGlobal.soundVolumeMultiplier, 0.0D, 4.0D, 1.0D);
        normalized.global.maxBirdsNearby = clamp(sourceGlobal.maxBirdsNearby, 0, 256);
        normalized.global.maxGroundDroppingsNearby = clamp(sourceGlobal.maxGroundDroppingsNearby, 0, 128);
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
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(values, BirdConfigData.class, writer);
        }
    }
}
