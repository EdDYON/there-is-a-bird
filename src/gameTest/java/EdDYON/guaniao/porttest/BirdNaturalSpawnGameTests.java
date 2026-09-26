package EdDYON.guaniao.porttest;

import EdDYON.guaniao.config.BirdConfigData;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.event.BirdNaturalSpawnEvents;
import EdDYON.guaniao.event.BirdPopulationTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;

@GameTestHolder("guaniao_port_tests")
@PrefixGameTestTemplate(false)
public final class BirdNaturalSpawnGameTests {
    @GameTest(template = "empty", batch = "habitat_spawning", timeoutTicks = 200)
    public static void allSixteenSpeciesUseTheirHabitatAndRespectCaps(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        var configField = BirdConfigManager.class.getDeclaredField("config");
        configField.setAccessible(true);
        BirdConfigData original = BirdConfigManager.snapshot();
        var spawn = BirdNaturalSpawnEvents.class.getDeclaredMethod("trySpawnGroup", ServerLevel.class, BlockPos.class, RandomSource.class);
        spawn.setAccessible(true);
        long oldTime = level.getDayTime();
        boolean oldRule = level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
        // Center the fixture inside a chunk, since some habitat predicates avoid crossing chunk edges.
        BlockPos origin = h.absolutePos(new BlockPos(136, 24, 8));
        BlockPos center = new BlockPos((origin.getX() >> 4) * 16 + 8, origin.getY(), (origin.getZ() >> 4) * 16 + 8);
        for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) {
            level.setBlockAndUpdate(center.offset(x, -1, z), x == -6 ? Blocks.WATER.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
            for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
            if (x == 6) level.setBlockAndUpdate(center.offset(x, 0, z), Blocks.OAK_LOG.defaultBlockState());
        }
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true, level.getServer());
        var livestock = new ArrayList<Mob>();
        for (int i = 0; i < 32; i++) {
            var cow = net.minecraft.world.entity.EntityType.COW.create(level);
            cow.setPos(center.getX() + 25 + i % 8, center.getY(), center.getZ() + i / 8);
            level.addFreshEntity(cow);
            livestock.add(cow);
        }
        try {
            h.assertTrue(BirdPopulationTracker.totalAt(level, center.getX(), center.getZ()) == 0,
                    "Vanilla livestock must not consume bird capacity");
            var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
            for (BirdSpecies species : BirdSpecies.values()) {
                BirdConfigData settings = BirdConfigManager.defaultConfig();
                settings.birds.values().forEach(bird -> bird.naturalSpawning = false);
                var birdSettings = settings.birds.get(species.id());
                birdSettings.naturalSpawning = true;
                birdSettings.minGroup = 2;
                birdSettings.maxGroup = 2;
                birdSettings.maxWildNearby = 2;
                configField.set(null, settings);
                var habitat = registry.holders().filter(biome -> biome.value().getMobSettings().getMobs(MobCategory.CREATURE)
                        .unwrap().stream().anyMatch(entry -> entry.type == species.entityType())).findFirst().orElseThrow();
                setBiome(level, center, habitat);
                var random = RandomSource.create(7823 + species.ordinal());
                int added = 0;
                for (int attempt = 0; attempt < 128 && added < 2; attempt++) added += (int)spawn.invoke(null, level, center, random);
                if (added != 2) {
                    var pos = level.getHeightmapPos(net.minecraft.world.entity.SpawnPlacements.getHeightmapType(species.entityType()), center);
                    System.out.println("SPAWN FIXTURE: " + species + " at=" + pos + " ground=" + level.getBlockState(pos.below())
                            + " light=" + level.getRawBrightness(pos, 0) + " sky=" + level.canSeeSky(pos)
                            + " biome=" + level.getBiome(pos).unwrapKey() + " count=" + BirdPopulationTracker.totalAt(level, pos.getX(), pos.getZ())
                            + " placement=" + net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(species.entityType(), level, net.minecraft.world.entity.MobSpawnType.NATURAL, pos, random));
                    var bird = (Mob)species.entityType().create(level);bird.setPos(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5);
                    System.out.println("SPAWN FIXTURE MOB: rules=" + bird.checkSpawnRules(level, net.minecraft.world.entity.MobSpawnType.NATURAL) + " obstruction=" + bird.checkSpawnObstruction(level) + " collision=" + level.noCollision(bird));
                }
                h.assertTrue(added == 2, "Habitat replenishment failed for " + species + ": " + added);
                for (int attempt = 0; attempt < 32; attempt++) {
                    h.assertTrue((int)spawn.invoke(null, level, center, random) == 0, "Species cap exceeded: " + species);
                }
                System.out.println("NATURAL SPAWN PASS: " + species.id() + " habitat=" + habitat.key().location() + " count=2 cap=2 livestock=32");
                var birds = new ArrayList<Mob>();
                for (var entity : level.getAllEntities()) if (entity instanceof Mob mob && BirdSpecies.from(mob) == species) birds.add(mob);
                birds.forEach(Mob::discard);
                BirdPopulationTracker.rebuild(level.getServer());

                settings.global.naturalSpawning = false;
                h.assertTrue((int)spawn.invoke(null, level, center, random) == 0, "Global spawning switch ignored");
                settings.global.naturalSpawning = true;
                birdSettings.naturalSpawning = false;
                h.assertTrue((int)spawn.invoke(null, level, center, random) == 0, "Species spawning switch ignored");
                birdSettings.naturalSpawning = true;
                settings.global.maxBirdsNearby = 0;
                h.assertTrue((int)spawn.invoke(null, level, center, random) == 0, "Zero global cap ignored");
                settings.global.maxBirdsNearby = original.global.maxBirdsNearby;
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
                h.assertTrue((int)spawn.invoke(null, level, center, random) == 0, "doMobSpawning=false ignored");
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true, level.getServer());
                setBiome(level, center, registry.getHolderOrThrow(Biomes.NETHER_WASTES));
                h.assertTrue((int)spawn.invoke(null, level, center, random) == 0, "Bird spawned outside its biome table");
            }
        } finally {
            livestock.forEach(Mob::discard);
            configField.set(null, original);
            level.setDayTime(oldTime);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(oldRule, level.getServer());
        }
        h.succeed();
    }

    private static void setBiome(ServerLevel level, BlockPos center, Holder<Biome> biome) {
        int cx = center.getX() >> 4, cz = center.getZ() >> 4;
        for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) {
            level.getChunk(x, z).fillBiomesFromNoise((qx, qy, qz, sampler) -> biome, level.getChunkSource().randomState().sampler());
        }
    }
}
