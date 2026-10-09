package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Replenishes habitats under bird caps even when vanilla livestock fills CREATURE's cap. */
@EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdNaturalSpawnEvents {
    private static final int INTERVAL_TICKS = 60;
    private static final int COLUMNS_PER_ATTEMPT = 4;
    private static final int MAX_GROUP_ATTEMPTS = 16;

    private BirdNaturalSpawnEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD
                || level.getGameTime() % INTERVAL_TICKS != 0 || !canSpawn(level)) {
            return;
        }
        var players = level.players().stream().filter(player -> player.isAlive() && !player.isSpectator()).toList();
        if (players.isEmpty()) return;
        // One player per level per interval keeps the work bounded on multiplayer servers.
        ServerPlayer player = players.get(level.random.nextInt(players.size()));
        if (BirdPopulationTracker.totalAt(level, player.getX(), player.getZ()) >= BirdConfigManager.maxBirdsNearby()) return;
        int playerSurface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, player.getBlockX(), player.getBlockZ());
        if (player.getY() < playerSurface - 12 || player.getY() > playerSurface + 48) return;

        for (int attempt = 0; attempt < COLUMNS_PER_ATTEMPT; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 32 + level.random.nextDouble() * 48;
            int x = Mth.floor(player.getX() + Math.cos(angle) * radius);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * radius);
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            if (trySpawnGroup(level, new BlockPos(x, 0, z), level.random) > 0) return;
        }
    }

    private static boolean canSpawn(ServerLevel level) {
        return level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                && level.getServer().isSpawningAnimals() && BirdConfigManager.maxBirdsNearby() > 0;
    }

    static int trySpawnGroup(ServerLevel level, BlockPos column, RandomSource random) {
        if (!canSpawn(level) || !level.hasChunk(column.getX() >> 4, column.getZ() >> 4)) return 0;
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
        var spawns = EventHooks.getPotentialSpawns(level, MobCategory.CREATURE, surface,
                level.getBiome(surface).value().getMobSettings().getMobs(MobCategory.CREATURE));
        // Reuse the habitat/config hooks, but draw only birds. Livestock must
        // affect neither this pool's weights nor its independent population cap.
        var birdSpawns = WeightedRandomList.create(spawns.unwrap().stream()
                .filter(entry -> BirdSpecies.from(entry.type) != null).toList());
        var selected = birdSpawns.getRandom(random).orElse(null);
        if (selected == null) return 0;

        int desired = Mth.nextInt(random, selected.minCount, selected.maxCount);
        int spawned = 0;
        SpawnGroupData group = null;
        for (int attempt = 0; attempt < MAX_GROUP_ATTEMPTS && spawned < desired; attempt++) {
            int x = column.getX() + random.nextInt(9) - 4;
            int z = column.getZ() + random.nextInt(9) - 4;
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            BlockPos pos = level.getHeightmapPos(SpawnPlacements.getHeightmapType(selected.type), new BlockPos(x, 0, z));
            // Do not carry a species across a biome boundary or ignore another mod's veto.
            var candidates = EventHooks.getPotentialSpawns(level, MobCategory.CREATURE, pos,
                    level.getBiome(pos).value().getMobSettings().getMobs(MobCategory.CREATURE));
            if (candidates.unwrap().stream().noneMatch(entry -> entry.type == selected.type)
                    || level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 24)
                    || !BirdFlybySpawnEvents.isHiddenFromNearbyPlayers(level, Vec3.atBottomCenterOf(pos))
                    || !SpawnPlacements.isSpawnPositionOk(selected.type, level, pos)
                    || !SpawnPlacements.checkSpawnRules(selected.type, level, MobSpawnType.NATURAL, pos, random)) continue;

            if (!(selected.type.create(level) instanceof Mob bird)) continue;
            bird.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
            if (!level.noCollision(bird) || !EventHooks.checkSpawnPosition(bird, level, MobSpawnType.NATURAL)) {
                bird.discard();
                continue;
            }
            group = EventHooks.finalizeMobSpawn(bird, level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, group);
            if (level.addFreshEntity(bird)) spawned++;
        }
        return spawned;
    }
}
