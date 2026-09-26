package EdDYON.guaniao.porttest;

import EdDYON.guaniao.client.skybird.SkyBirdManager;
import EdDYON.guaniao.client.skybird.SkyFlock;
import EdDYON.guaniao.config.BirdSpecies;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/** Observes natural spawning in a COPY of a normal Overworld save; never creates birds or flocks. */
@EventBusSubscriber(modid = "guaniao_port_tests", value = Dist.CLIENT)
public final class NaturalSpawningClientSmoke {
    private static final boolean ENABLED = Boolean.getBoolean("guaniao.naturalSpawningClientSmoke");
    private static final Map<String, Integer> PLACEMENTS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> FINALIZED = new ConcurrentHashMap<>();
    private static int ticks;
    private static SkyFlock watching;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!ENABLED || BirdSpecies.from(event.getEntityType()) == null) return;
        PLACEMENTS.merge(event.getSpawnType() + "/" + BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntityType())
                + "/" + event.getResult(), 1, Integer::sum);
    }

    @SubscribeEvent
    public static void spawned(FinalizeSpawnEvent event) {
        if (!ENABLED || BirdSpecies.from(event.getEntity()) == null) return;
        FINALIZED.merge(event.getSpawnType() + "/" + BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()),
                1, Integer::sum);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        ticks++;
        if (ticks == 10) {
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            System.out.println("NATURAL OPTIONS: graphics=" + mc.options.graphicsMode().get()
                    + " clouds=" + mc.options.cloudStatus().get() + " pos=" + mc.player.position()
                    + " biome=" + mc.level.getBiome(mc.player.blockPosition()).unwrapKey());
        }
        var manager = SkyBirdManager.INSTANCE;
        if (watching == null || watching.isExpired() || !manager.flocks().contains(watching)) {
            watching = manager.flocks().stream()
                    .filter(flock -> flock.age() > 40 && flock.renderCenter(1).y > mc.player.getEyeY() + 10)
                    .findFirst().orElse(null);
        }
        if (watching != null && ticks > 100) {
            Vec3 direction = watching.renderCenter(1).subtract(mc.player.getEyePosition());
            mc.player.setYRot((float)Math.toDegrees(Math.atan2(-direction.x, direction.z)));
            mc.player.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,
                    Math.sqrt(direction.x * direction.x + direction.z * direction.z))));
            mc.player.yRotO = mc.player.getYRot();
            mc.player.xRotO = mc.player.getXRot();
        }
        if (ticks == 240) mc.options.cloudStatus().set(CloudStatus.OFF);
        if (ticks == 300) {
            mc.options.graphicsMode().set(GraphicsStatus.FABULOUS);
            mc.levelRenderer.allChanged();
        }
        if (ticks == 350) mc.options.cloudStatus().set(CloudStatus.FANCY);
        if (ticks == 180 || ticks == 220 || ticks == 280 || ticks == 340 || ticks == 390 || ticks == 600 || ticks == 1000) {
            int sampleTick = ticks;
            System.out.println("NATURAL SKY t=" + sampleTick + " " + manager.runtimeStatus()
                    + " ecology=" + manager.ecologyStatus() + " render=" + manager.renderDiagnosticsStatus()
                    + " watching=" + (watching == null ? "none" : watching.species() + " at " + watching.renderCenter(1)));
            Screenshot.grab(mc.gameDirectory, "natural-" + sampleTick + ".png", mc.getMainRenderTarget(), message -> {});
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                var level = server.overworld();
                var counts = new TreeMap<String, Integer>();
                for (var entity : level.getAllEntities()) {
                    if (entity.getType().getCategory() == MobCategory.CREATURE) {
                        counts.merge(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), 1, Integer::sum);
                    }
                }
                var state = level.getChunkSource().getLastSpawnState();
                System.out.println("NATURAL SERVER t=" + sampleTick + " creatures=" + counts
                        + " categoryCounts=" + (state == null ? "none" : state.getMobCategoryCounts())
                        + " finalized=" + FINALIZED + " placements=" + PLACEMENTS);
            });
        }
        if (ticks == 1100) {
            System.out.println("NATURAL OBSERVATION COMPLETE (screenshots require visual review)");
            mc.stop();
        }
    }
}
