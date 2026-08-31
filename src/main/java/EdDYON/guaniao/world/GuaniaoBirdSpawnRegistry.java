package EdDYON.guaniao.world;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

import java.util.List;
import java.util.function.Supplier;

public final class GuaniaoBirdSpawnRegistry {
    private static final List<SpawnRule> SPAWN_RULES = List.of(
            new SpawnRule(BirdSpecies.NIGHT_HERON, GuaniaoEntityTypes.NIGHT_HERON, 3),
            new SpawnRule(BirdSpecies.SPARROW, GuaniaoEntityTypes.SPARROW, 6),
            new SpawnRule(BirdSpecies.LONG_TAILED_TIT, GuaniaoEntityTypes.LONG_TAILED_TIT, 5),
            new SpawnRule(BirdSpecies.COCKATIEL, GuaniaoEntityTypes.COCKATIEL, 3),
            new SpawnRule(BirdSpecies.MACAW, GuaniaoEntityTypes.MACAW, 2),
            new SpawnRule(BirdSpecies.BUDGERIGAR, GuaniaoEntityTypes.BUDGERIGAR, 4),
            new SpawnRule(BirdSpecies.SPOTTED_DOVE, GuaniaoEntityTypes.SPOTTED_DOVE, 3),
            new SpawnRule(BirdSpecies.PIGEON, GuaniaoEntityTypes.PIGEON, 5),
            new SpawnRule(BirdSpecies.CROW, GuaniaoEntityTypes.CROW, 3),
            new SpawnRule(BirdSpecies.SEAGULL, GuaniaoEntityTypes.SEAGULL, 4),
            new SpawnRule(BirdSpecies.KIWI, GuaniaoEntityTypes.KIWI, 2),
            new SpawnRule(BirdSpecies.MYNA, GuaniaoEntityTypes.MYNA, 4)
    );

    private GuaniaoBirdSpawnRegistry() {
    }

    public static void addBiomeSpawns(Holder<Biome> biome, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        for (SpawnRule rule : SPAWN_RULES) {
            if (!biome.is(rule.habitatTag)) {
                continue;
            }
            builder.getMobSpawnSettings().getSpawner(MobCategory.CREATURE).add(new MobSpawnSettings.SpawnerData(
                    rule.entityType(),
                    rule.baseWeight,
                    rule.species.defaultMinGroup(),
                    rule.species.defaultMaxGroup()
            ));
        }
    }

    private static TagKey<Biome> habitatTag(BirdSpecies species) {
        return TagKey.create(Registries.BIOME, new ResourceLocation(GuaniaoMod.MOD_ID, species.id() + "_habitat"));
    }

    private static final class SpawnRule {
        private final BirdSpecies species;
        private final Supplier<? extends EntityType<?>> entityType;
        private final TagKey<Biome> habitatTag;
        private final int baseWeight;

        private SpawnRule(BirdSpecies species, Supplier<? extends EntityType<?>> entityType, int baseWeight) {
            this.species = species;
            this.entityType = entityType;
            this.habitatTag = habitatTag(species);
            this.baseWeight = baseWeight;
        }

        @SuppressWarnings("unchecked")
        private EntityType<? extends Mob> entityType() {
            return (EntityType<? extends Mob>) this.entityType.get();
        }
    }
}
