package EdDYON.guaniao.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server behavior settings, separate from the existing species population presets. */
public final class HummingbirdConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue NECTAR, FEEDS, PERIOD, GRACE, COOLDOWN_MIN, COOLDOWN_MAX,
            SCAN, CROPS, SEED_MIN, SEED_MAX, TERRITORY_COOLDOWN;
    private static final ModConfigSpec.DoubleValue RADIUS, SEED_CHANCE, MALE_CHANCE, FEMALE_CHANCE, BONUS;
    private static final ModConfigSpec.BooleanValue POLLINATION;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hummingbird");
        NECTAR = b.defineInRange("nectarTicks", 60, 20, 200);
        FEEDS = b.defineInRange("tamingFeeds", 5, 1, 20);
        POLLINATION = b.define("pollinationEnabled", true);
        PERIOD = b.defineInRange("pollinationPeriodTicks", 400, 100, 2400);
        RADIUS = b.defineInRange("pollinationRadius", 8.0, 1.0, 16.0);
        GRACE = b.defineInRange("pollinationGraceTicks", 120, 0, 400);
        COOLDOWN_MIN = b.defineInRange("flowerCooldownMinTicks", 1200, 200, 12000);
        COOLDOWN_MAX = b.defineInRange("flowerCooldownMaxTicks", 2400, 200, 12000);
        SCAN = b.defineInRange("flowerScanBudget", 4096, 256, 16384);
        CROPS = b.defineInRange("cropBudget", 512, 16, 4096);
        SEED_MIN = b.defineInRange("seedMinTicks", 12000, 1200, 72000);
        SEED_MAX = b.defineInRange("seedMaxTicks", 18000, 1200, 72000);
        SEED_CHANCE = b.defineInRange("seedChance", 0.35, 0.0, 1.0);
        MALE_CHANCE = b.defineInRange("territoryMaleChance", 0.20, 0.0, 1.0);
        FEMALE_CHANCE = b.defineInRange("territoryFemaleChance", 0.02, 0.0, 1.0);
        TERRITORY_COOLDOWN = b.defineInRange("territoryCooldownTicks", 2400, 200, 12000);
        BONUS = b.defineInRange("gardenBonus", 0.15, 0.0, 0.15);
        b.pop();
        SPEC = b.build();
    }
    private HummingbirdConfig() { }
    public static int nectarTicks() { return NECTAR.get(); }
    public static int tamingFeeds() { return FEEDS.get(); }
    public static boolean pollinationEnabled() { return POLLINATION.get(); }
    public static int pollinationPeriodTicks() { return PERIOD.get(); }
    public static double pollinationRadius() { return RADIUS.get(); }
    public static int pollinationGraceTicks() { return GRACE.get(); }
    public static int flowerCooldownMinTicks() { return COOLDOWN_MIN.get(); }
    public static int flowerCooldownMaxTicks() { return Math.max(COOLDOWN_MIN.get(), COOLDOWN_MAX.get()); }
    public static int flowerScanBudget() { return SCAN.get(); }
    public static int cropBudget() { return CROPS.get(); }
    public static int seedMinTicks() { return SEED_MIN.get(); }
    public static int seedMaxTicks() { return Math.max(SEED_MIN.get(), SEED_MAX.get()); }
    public static double seedChance() { return SEED_CHANCE.get(); }
    public static double territoryMaleChance() { return MALE_CHANCE.get(); }
    public static double territoryFemaleChance() { return FEMALE_CHANCE.get(); }
    public static int territoryCooldownTicks() { return TERRITORY_COOLDOWN.get(); }
    public static double gardenBonus() { return BONUS.get(); }
    public static int gardenRadius() { return 16; }
}
