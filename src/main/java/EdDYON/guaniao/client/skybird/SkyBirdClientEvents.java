package EdDYON.guaniao.client.skybird;

import EdDYON.guaniao.GuaniaoMod;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT)
public final class SkyBirdClientEvents {
    private SkyBirdClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        SkyBirdManager.INSTANCE.tick();
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("skybird")
                .then(Commands.literal("spawn")
                        .executes(context -> spawnDebugFlock(context.getSource(), 12))
                        .then(Commands.literal("goose")
                                .executes(context -> spawnDebugFlock(context.getSource(), 12))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 32))
                                        .executes(context -> spawnDebugFlock(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "count")
                                        ))))
                        .then(Commands.literal("eagle")
                                .executes(context -> spawnDebugEagle(context.getSource())))
                        .then(Commands.literal("seagull")
                                .executes(context -> spawnDebugSeagulls(context.getSource(), 5))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 16))
                                        .executes(context -> spawnDebugSeagulls(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "count")
                                        ))))
                        .then(Commands.literal("swallow")
                                .executes(context -> spawnDebugSwallows(context.getSource(), 8))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 20))
                                        .executes(context -> spawnDebugSwallows(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "count")
                                        ))))
                        .then(Commands.literal("crane")
                                .executes(context -> spawnDebugCranes(context.getSource(), 5))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 12))
                                        .executes(context -> spawnDebugCranes(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "count")
                                        ))))
                        .then(Commands.literal("vulture")
                                .executes(context -> spawnDebugVultures(context.getSource(), 2))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 6))
                                        .executes(context -> spawnDebugVultures(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "count")
                                        ))))
                        .then(Commands.literal("starling")
                                .executes(context -> spawnDebugStarlings(context.getSource(), 18))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 32))
                                        .executes(context -> spawnDebugStarlings(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "count")
                                        ))))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 32))
                                .executes(context -> spawnDebugFlock(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "count")
                                ))))
                .then(Commands.literal("status")
                        .executes(context -> showStatus(context.getSource())))
                .then(Commands.literal("refresh")
                        .executes(context -> refreshEcology(context.getSource())))
                .then(Commands.literal("clear")
                        .executes(context -> clearFlocks(context.getSource()))));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            SkyBirdRenderer.render(event, SkyBirdManager.INSTANCE);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SkyBirdManager.INSTANCE.clear();
    }

    private static int spawnDebugFlock(net.minecraft.commands.CommandSourceStack source, int count) {
        int spawned = SkyBirdManager.INSTANCE.spawnDebugFlock(count);
        if (spawned <= 0) {
            source.sendFailure(Component.literal("天空鸟生成失败：客户端世界或大雁贴图尚未就绪。"));
            return 0;
        }
        source.sendSuccess(
                () -> Component.literal("已在准星前上方生成 " + spawned
                        + " 只大雁。抬头观察；一秒后可执行 /skybird status。"),
                false
        );
        return spawned;
    }

    private static int spawnDebugStarlings(net.minecraft.commands.CommandSourceStack source, int count) {
        int spawned = SkyBirdManager.INSTANCE.spawnDebugStarlings(count);
        if (spawned <= 0) {
            source.sendFailure(Component.literal("天空椋鸟群生成失败：客户端世界或椋鸟贴图尚未就绪。"));
            return 0;
        }
        source.sendSuccess(
                () -> Component.literal("已在准星前上方生成 " + spawned
                        + " 只动态变阵的椋鸟。"),
                false
        );
        return spawned;
    }

    private static int spawnDebugVultures(net.minecraft.commands.CommandSourceStack source, int count) {
        int spawned = SkyBirdManager.INSTANCE.spawnDebugVultures(count);
        if (spawned <= 0) {
            source.sendFailure(Component.literal("天空秃鹫生成失败：客户端世界或秃鹫贴图尚未就绪。"));
            return 0;
        }
        source.sendSuccess(
                () -> Component.literal("已在准星前上方生成 " + spawned
                        + " 只利用热气流盘升的秃鹫。"),
                false
        );
        return spawned;
    }

    private static int spawnDebugCranes(net.minecraft.commands.CommandSourceStack source, int count) {
        int spawned = SkyBirdManager.INSTANCE.spawnDebugCranes(count);
        if (spawned <= 0) {
            source.sendFailure(Component.literal("天空鹤群生成失败：客户端世界或鹤类贴图尚未就绪。"));
            return 0;
        }
        source.sendSuccess(
                () -> Component.literal("已在准星前上方生成 " + spawned
                        + " 只迁飞鹤。它们会使用浅 V 队或斜列队缓慢巡航。"),
                false
        );
        return spawned;
    }

    private static int spawnDebugSwallows(net.minecraft.commands.CommandSourceStack source, int count) {
        int spawned = SkyBirdManager.INSTANCE.spawnDebugSwallows(count);
        if (spawned <= 0) {
            source.sendFailure(Component.literal("天空燕子生成失败：客户端世界或燕子贴图尚未就绪。"));
            return 0;
        }
        source.sendSuccess(
                () -> Component.literal("已在准星前上方生成 " + spawned
                        + " 只燕子。它们会快速拍翼并沿连续弧线掠过。"),
                false
        );
        return spawned;
    }

    private static int spawnDebugSeagulls(net.minecraft.commands.CommandSourceStack source, int count) {
        int spawned = SkyBirdManager.INSTANCE.spawnDebugSeagulls(count);
        if (spawned <= 0) {
            source.sendFailure(Component.literal("天空海鸥生成失败：客户端世界或海鸥贴图尚未就绪。"));
            return 0;
        }
        source.sendSuccess(
                () -> Component.literal("已在准星前上方生成 " + spawned
                        + " 只松散飞行的海鸥。它们会沿宽缓曲线滑翔。"),
                false
        );
        return spawned;
    }

    private static int spawnDebugEagle(net.minecraft.commands.CommandSourceStack source) {
        int spawned = SkyBirdManager.INSTANCE.spawnDebugEagle();
        if (spawned <= 0) {
            source.sendFailure(Component.literal("天空鹰生成失败：客户端世界或鹰贴图尚未就绪。"));
            return 0;
        }
        source.sendSuccess(
                () -> Component.literal("已在准星前上方生成一只盘旋的鹰。它会沿固定世界轨迹飞行。"),
                false
        );
        return spawned;
    }

    private static int showStatus(net.minecraft.commands.CommandSourceStack source) {
        SkyBirdManager manager = SkyBirdManager.INSTANCE;
        source.sendSuccess(
                () -> Component.literal("天空鸟状态：textures={" + manager.textureStatus() + "}"
                        + ", ecology={" + manager.ecologyStatus() + "}"
                        + ", runtime={" + manager.runtimeStatus() + "}"
                        + ", flocks=" + manager.flocks().size()
                        + ", birds=" + manager.totalBirdCount()
                        + ", renderedLastFrame=" + manager.lastRenderedBirdCount()
                        + ", render={" + manager.renderDiagnosticsStatus() + "}"),
                false
        );
        return 1;
    }

    private static int refreshEcology(net.minecraft.commands.CommandSourceStack source) {
        SkyBirdManager manager = SkyBirdManager.INSTANCE;
        if (!manager.refreshEcologyNow()) {
            source.sendFailure(Component.literal(
                    "天空生态刷新失败：" + manager.ecologyRefreshBlockReason()
            ));
            return 0;
        }
        source.sendSuccess(
                () -> Component.literal("已按当前位置、群系、时间和天气刷新天空生态："
                        + manager.ecologyStatus()),
                false
        );
        return 1;
    }

    private static int clearFlocks(net.minecraft.commands.CommandSourceStack source) {
        SkyBirdManager.INSTANCE.clearDebugFlocks();
        source.sendSuccess(() -> Component.literal("已清除客户端天空鸟群。"), false);
        return 1;
    }
}
