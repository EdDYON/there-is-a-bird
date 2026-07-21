package EdDYON.guaniao.command;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.event.BirdPopulationTracker;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdStressTestCommands {
    private BirdStressTestCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guaniao")
                .requires(BirdConfigCommands::canEdit)
                .then(Commands.literal("stress")
                        .then(Commands.literal("start")
                                .then(Commands.argument("birds", IntegerArgumentType.integer(1, 400))
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(10, 900))
                                                .executes(context -> start(context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "birds"),
                                                        IntegerArgumentType.getInteger(context, "seconds"), 24))
                                                .then(Commands.argument("radius", IntegerArgumentType.integer(8, 64))
                                                        .executes(context -> start(context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "birds"),
                                                                IntegerArgumentType.getInteger(context, "seconds"),
                                                                IntegerArgumentType.getInteger(context, "radius")))))))
                        .then(Commands.literal("status").executes(context -> status(context.getSource())))
                        .then(Commands.literal("stop").executes(context -> stop(context.getSource())))
                        .then(Commands.literal("cleanup").executes(context -> cleanup(context.getSource()))))
                .then(Commands.literal("perf")
                        .executes(context -> perf(context.getSource()))
                        .then(Commands.literal("reset").executes(context -> resetPerf(context.getSource())))));
    }

    private static int start(CommandSourceStack source, int birds, int seconds, int radius) {
        UUID requester = source.getEntity() instanceof ServerPlayer player ? player.getUUID() : null;
        Vec3 origin = source.getPosition();
        if (!BirdStressTestManager.start(source.getLevel(), origin, requester, birds, seconds, radius)) {
            source.sendFailure(Component.translatable("command.guaniao.stress.already_running"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.guaniao.stress.started", birds, seconds, radius), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        BirdStressTestManager.Status status = BirdStressTestManager.status();
        if (status == null) {
            source.sendFailure(Component.translatable("command.guaniao.stress.not_running"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "command.guaniao.stress.status",
                status.spawned(), status.targetBirds(), status.remainingTicks() / 20L, status.samples()
        ), false);
        return status.spawned();
    }

    private static int stop(CommandSourceStack source) {
        BirdStressTestManager.Report report = BirdStressTestManager.stop(source.getServer(), true);
        if (report == null) {
            source.sendFailure(Component.translatable("command.guaniao.stress.not_running"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.guaniao.stress.stopped", report.spawned()), true);
        return report.spawned();
    }

    private static int cleanup(CommandSourceStack source) {
        int removed = BirdStressTestManager.cleanupAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.guaniao.stress.cleaned", removed), true);
        return removed;
    }

    private static int perf(CommandSourceStack source) {
        BirdScanBudget.Stats stats = BirdScanBudget.stats(source.getLevel());
        Vec3 pos = source.getPosition();
        int nearby = BirdPopulationTracker.totalAt(source.getLevel(), pos.x, pos.z);
        source.sendSuccess(() -> Component.translatable(
                "command.guaniao.perf",
                stats.usedThisTick(), stats.granted(), stats.denied(), nearby
        ), false);
        return stats.usedThisTick();
    }

    private static int resetPerf(CommandSourceStack source) {
        BirdScanBudget.resetStats(source.getLevel());
        source.sendSuccess(() -> Component.translatable("command.guaniao.perf_reset"), false);
        return 1;
    }
}
