package EdDYON.guaniao.config;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

public enum BirdSpecies {
    NIGHT_HERON("night_heron", 1, 2, 3600, 6000),
    SPARROW("sparrow", 5, 10, 3600, 6000),
    LONG_TAILED_TIT("long_tailed_tit", 4, 10, 3600, 6000),
    COCKATIEL("cockatiel", 2, 5, 3600, 6000),
    MACAW("macaw", 2, 4, 3600, 6000),
    BUDGERIGAR("budgerigar", 3, 8, 3600, 6000),
    SPOTTED_DOVE("spotted_dove", 1, 2, 3000, 5400),
    PIGEON("pigeon", 3, 7, 2400, 4800),
    CROW("crow", 1, 3, 3600, 6000),
    SEAGULL("seagull", 2, 5, 3000, 5400),
    KIWI("kiwi", 1, 2, 3600, 6000, false),
    MYNA("myna", 2, 4, 3000, 5400),
    CASSOWARY("cassowary", 1, 1, 3600, 6000, false),
    KESTREL("kestrel", 1, 1, 3600, 6000),
    WOODCOCK("woodcock", 1, 2, 3600, 6000, false);

    private final String id;
    private final int defaultMinGroup;
    private final int defaultMaxGroup;
    private final int defaultDroppingMinTicks;
    private final int defaultDroppingMaxTicks;
    private final boolean requiresOpenSkyForNaturalSpawn;

    BirdSpecies(String id, int defaultMinGroup, int defaultMaxGroup, int defaultDroppingMinTicks, int defaultDroppingMaxTicks) {
        this(id, defaultMinGroup, defaultMaxGroup, defaultDroppingMinTicks, defaultDroppingMaxTicks, true);
    }

    BirdSpecies(String id, int defaultMinGroup, int defaultMaxGroup, int defaultDroppingMinTicks,
                int defaultDroppingMaxTicks, boolean requiresOpenSkyForNaturalSpawn) {
        this.id = id;
        this.defaultMinGroup = defaultMinGroup;
        this.defaultMaxGroup = defaultMaxGroup;
        this.defaultDroppingMinTicks = defaultDroppingMinTicks;
        this.defaultDroppingMaxTicks = defaultDroppingMaxTicks;
        this.requiresOpenSkyForNaturalSpawn = requiresOpenSkyForNaturalSpawn;
    }

    public String id() {
        return this.id;
    }

    public String translationKey() {
        return "entity." + GuaniaoMod.MOD_ID + "." + this.id;
    }

    public int defaultMinGroup() {
        return this.defaultMinGroup;
    }

    public int defaultMaxGroup() {
        return this.defaultMaxGroup;
    }

    public int defaultDroppingMinTicks() {
        return this.defaultDroppingMinTicks;
    }

    public int defaultDroppingMaxTicks() {
        return this.defaultDroppingMaxTicks;
    }

    public boolean requiresOpenSkyForNaturalSpawn() {
        return this.requiresOpenSkyForNaturalSpawn;
    }

    public EntityType<?> entityType() {
        return ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(GuaniaoMod.MOD_ID, this.id));
    }

    public static BirdSpecies from(Entity entity) {
        return entity == null ? null : from(entity.getType());
    }

    public static BirdSpecies from(EntityType<?> entityType) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        if (key == null || !GuaniaoMod.MOD_ID.equals(key.getNamespace())) {
            return null;
        }
        return fromId(key.getPath());
    }

    public static BirdSpecies fromId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (BirdSpecies species : values()) {
            if (species.id.equals(normalized)) {
                return species;
            }
        }
        return null;
    }
}
