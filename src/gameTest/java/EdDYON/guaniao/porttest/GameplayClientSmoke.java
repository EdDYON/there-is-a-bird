package EdDYON.guaniao.porttest;

import EdDYON.guaniao.client.skybird.*;
import EdDYON.guaniao.content.dropping.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in client scenarios in a disposable world; screenshots need visual review. */
@EventBusSubscriber(modid = "guaniao_port_tests", value = Dist.CLIENT)
public final class GameplayClientSmoke {
    private static int ticks, waiting;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("guaniao.gameplayClientSmoke")) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (++waiting > 2400) throw new IllegalStateException("Gameplay smoke did not enter its world");
            return;
        }
        ticks++;
        if (ticks == 60 && Boolean.getBoolean("guaniao.skipSkySmoke")) ticks = 1519;
        if (ticks == 10) {
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            mc.options.bobView().set(false);
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            mc.options.renderDistance().set(12);
            mc.getSingleplayerServer().execute(() -> {
                var server = mc.getSingleplayerServer();
                var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                var level = player.serverLevel();
                level.setDayTime(6000);
                level.setWeatherParameters(60000, 0, false, false);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                player.setGameMode(GameType.CREATIVE);
                player.teleportTo(0.5, 160, 0.5);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            });
        }
        int elapsed = ticks - 60;
        if (elapsed >= 0 && elapsed < 28 * 50) {
            int scenario = elapsed / 50;
            int phase = elapsed % 50;
            var species = SkyBirdSpecies.values()[scenario / 4];
            if (phase == 0) {
                mc.player.setYRot(scenario % 4 * 90.0F);
                mc.player.setXRot(-35.0F);
                mc.player.yRotO = mc.player.getYRot();
                mc.player.xRotO = mc.player.getXRot();
                SkyBirdManager.INSTANCE.clearDebugFlocks();
            }
            if (phase == 3) {
                var manager = SkyBirdManager.INSTANCE;
                int spawned = switch (species) {
                    case GOOSE -> manager.spawnDebugFlock(7);
                    case EAGLE -> manager.spawnDebugEagle();
                    case SEAGULL -> manager.spawnDebugSeagulls(7);
                    case SWALLOW -> manager.spawnDebugSwallows(7);
                    case CRANE -> manager.spawnDebugCranes(7);
                    case VULTURE -> manager.spawnDebugVultures(3);
                    case STARLING -> manager.spawnDebugStarlings(18);
                };
                if (spawned < 1) throw new IllegalStateException("Sky texture/spawn failed: " + species);
            }
            if (phase == 35) {
                capture(mc, "sky-" + species.name().toLowerCase() + "-" + scenario % 4 + ".png");
                System.out.println("GAMEPLAY SKY: " + species + " yaw=" + mc.player.getYRot()
                        + " submitted=" + SkyBirdManager.INSTANCE.lastRenderedBirdCount() + "; screenshot requires pixel review");
            }
        }
        if (ticks == 1465) {
            SkyBirdManager.INSTANCE.clearDebugFlocks();
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.FANCY);
            mc.player.setYRot(90);
            mc.player.setXRot(-35);
            SkyBirdManager.INSTANCE.spawnDebugFlock(7);
        }
        if (ticks == 1500) capture(mc, "sky-with-clouds.png");
        if (ticks == 1520) {
            SkyBirdManager.INSTANCE.clearDebugFlocks();
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                var level = player.serverLevel();
                for (int x = -5; x <= 5; x++) for (int z = -5; z <= 8; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 150, z), Blocks.SMOOTH_STONE.defaultBlockState());
                }
                player.teleportTo(0.5, 153, -1.5);
                player.setYRot(0);
                player.setXRot(40);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BirdDroppingVariant.ONE.item(), 16));
                player.containerMenu.broadcastChanges();
            });
        }
        if (ticks == 1540) {
            mc.player.setYRot(0);
            mc.player.setXRot(40);
            mc.options.keyUse.setDown(true);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
        if (ticks == 1560) {
            mc.options.keyUse.setDown(false);
            mc.gameMode.releaseUsingItem(mc.player);
        }
        if (ticks == 1562) capture(mc, "dropping-in-flight.png");
        if (ticks == 1585) {
            long splats = java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                    .filter(e -> e instanceof BirdDroppingSplatEntity).count();
            if (splats < 1) throw new IllegalStateException("Thrown dropping did not sync an impact splat to client");
            capture(mc, "dropping-impact.png");
            System.out.println("GAMEPLAY DROPPING: client use/release packets and synchronized impact splat passed");
        }
        if (ticks == 1620) {
            System.out.println("GAMEPLAY CLIENT PASS: flight and splat screenshots saved; sky scenarios "
                    + (Boolean.getBoolean("guaniao.skipSkySmoke") ? "skipped" : "28 views plus clouds captured"));
            mc.stop();
        }
    }

    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                message -> System.out.println("GAMEPLAY CAPTURE: " + message.getString()));
    }
}
