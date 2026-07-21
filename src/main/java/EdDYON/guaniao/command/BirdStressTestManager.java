package EdDYON.guaniao.command;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdSpecies;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Operator-controlled bird load test with automatic reporting and cleanup. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdStressTestManager {
    private static final String STRESS_TAG = "GuaniaoStressTest";
    private static final int SPAWNS_PER_TICK = 8;
    private static StressSession active;
    private static long tickStartedNanos;

    private BirdStressTestManager() {
    }

    public static boolean start(ServerLevel level, Vec3 origin, UUID requester, int targetBirds, int seconds, int radius) {
        if (active != null) {
            return false;
        }
        long now = level.getServer().overworld().getGameTime();
        active = new StressSession(
                level, origin, requester, targetBirds, radius,
                now, now + seconds * 20L
        );
        return true;
    }

    public static boolean isActive() {
        return active != null;
    }

    public static Status status() {
        if (active == null) {
            return null;
        }
        long now = active.level.getServer().overworld().getGameTime();
        return new Status(active.targetBirds, active.spawned.size(), Math.max(0L, active.endsAt - now), active.samples.size());
    }

    public static Report stop(MinecraftServer server, boolean cleanup) {
        if (active == null) {
            return null;
        }
        StressSession session = active;
        active = null;
        Report report = report(session);
        if (cleanup) {
            cleanupSession(session);
        }
        announce(server, session.requester, report);
        return report;
    }

    public static int cleanupAll(MinecraftServer server) {
        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> matches = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity.getPersistentData().getBoolean(STRESS_TAG)) {
                    matches.add(entity);
                }
            }
            for (Entity entity : matches) {
                entity.discard();
                removed++;
            }
        }
        return removed;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            tickStartedNanos = System.nanoTime();
            return;
        }
        if (active == null) {
            return;
        }
        double elapsedMillis = (System.nanoTime() - tickStartedNanos) / 1_000_000.0D;
        active.samples.add(elapsedMillis);
        spawnBatch(active);
        long now = event.getServer().overworld().getGameTime();
        if (now >= active.endsAt) {
            stop(event.getServer(), true);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (active != null) {
            cleanupSession(active);
            active = null;
        }
    }

    private static void spawnBatch(StressSession session) {
        int remaining = session.targetBirds - session.spawned.size();
        for (int index = 0; index < Math.min(SPAWNS_PER_TICK, remaining); index++) {
            if (!spawnOne(session)) {
                break;
            }
        }
    }

    private static boolean spawnOne(StressSession session) {
        BirdSpecies[] species = BirdSpecies.values();
        for (int attempt = 0; attempt < 12; attempt++) {
            BirdSpecies selected = species[Math.floorMod(session.spawnCursor++, species.length)];
            EntityType<?> type = selected.entityType();
            if (type == null || !(type.create(session.level) instanceof Mob mob)) {
                continue;
            }
            double angle = session.level.random.nextDouble() * Math.PI * 2.0D;
            double distance = 4.0D + session.level.random.nextDouble() * Math.max(1.0D, session.radius - 4.0D);
            int x = Mth.floor(session.origin.x + Math.cos(angle) * distance);
            int z = Mth.floor(session.origin.z + Math.sin(angle) * distance);
            if (!session.level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int surfaceY = session.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, surfaceY + 2 + session.level.random.nextInt(5), z);
            mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                    session.level.random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(session.level, session.level.getCurrentDifficultyAt(pos), MobSpawnType.COMMAND, null, null);
            mob.getPersistentData().putBoolean(STRESS_TAG, true);
            mob.setPersistenceRequired();
            if (!session.level.noCollision(mob, mob.getBoundingBox()) || !session.level.addFreshEntity(mob)) {
                mob.discard();
                continue;
            }
            session.spawned.add(mob.getUUID());
            return true;
        }
        return false;
    }

    private static Report report(StressSession session) {
        List<Double> sorted = session.samples.stream().sorted(Comparator.naturalOrder()).toList();
        double average = session.samples.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
        double maximum = sorted.isEmpty() ? 0.0D : sorted.get(sorted.size() - 1);
        int p95Index = sorted.isEmpty() ? 0 : Math.min(sorted.size() - 1, (int)Math.ceil(sorted.size() * 0.95D) - 1);
        double p95 = sorted.isEmpty() ? 0.0D : sorted.get(p95Index);
        double estimatedTps = average <= 0.0D ? 20.0D : Math.min(20.0D, 1000.0D / average);
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return new Report(session.spawned.size(), session.samples.size(), average, p95, maximum, estimatedTps, usedMemory);
    }

    private static void cleanupSession(StressSession session) {
        for (UUID uuid : session.spawned) {
            Entity entity = session.level.getEntity(uuid);
            if (entity != null && entity.getPersistentData().getBoolean(STRESS_TAG)) {
                entity.discard();
            }
        }
    }

    private static void announce(MinecraftServer server, UUID requester, Report report) {
        Component message = Component.translatable(
                "command.guaniao.stress.report",
                report.spawned(), report.samples(), format(report.averageMs()), format(report.p95Ms()),
                format(report.maxMs()), format(report.estimatedTps()), formatMiB(report.usedMemory())
        );
        ServerPlayer player = requester == null ? null : server.getPlayerList().getPlayer(requester);
        if (player != null) {
            player.sendSystemMessage(message);
        }
        GuaniaoMod.LOGGER.info(message.getString());
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String formatMiB(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.1f MiB", bytes / (1024.0D * 1024.0D));
    }

    private static final class StressSession {
        private final ServerLevel level;
        private final Vec3 origin;
        private final UUID requester;
        private final int targetBirds;
        private final int radius;
        private final long startedAt;
        private final long endsAt;
        private final List<UUID> spawned = new ArrayList<>();
        private final List<Double> samples = new ArrayList<>();
        private int spawnCursor;

        private StressSession(ServerLevel level, Vec3 origin, UUID requester, int targetBirds, int radius, long startedAt, long endsAt) {
            this.level = level;
            this.origin = origin;
            this.requester = requester;
            this.targetBirds = targetBirds;
            this.radius = radius;
            this.startedAt = startedAt;
            this.endsAt = endsAt;
        }
    }

    public record Status(int targetBirds, int spawned, long remainingTicks, int samples) {
    }

    public record Report(int spawned, int samples, double averageMs, double p95Ms, double maxMs, double estimatedTps, long usedMemory) {
    }
}
