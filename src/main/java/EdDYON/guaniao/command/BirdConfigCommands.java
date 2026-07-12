package EdDYON.guaniao.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.network.GuaniaoNetwork;
import EdDYON.guaniao.network.OpenBirdConfigPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdConfigCommands {
    private BirdConfigCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guaniaoconfig")
                .requires(BirdConfigCommands::canEdit)
                .executes(context -> open(context.getSource())));
        event.getDispatcher().register(Commands.literal("birdconfig")
                .requires(BirdConfigCommands::canEdit)
                .executes(context -> open(context.getSource())));
    }

    private static int open(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BirdConfigManager.loadForServer(source.getServer());
        GuaniaoNetwork.sendToPlayer(new OpenBirdConfigPacket(BirdConfigManager.snapshot()), player);
        return 1;
    }

    public static boolean canEdit(CommandSourceStack source) {
        if (source.hasPermission(2)) {
            return true;
        }
        return source.getEntity() instanceof ServerPlayer player
                && source.getServer().isSingleplayerOwner(player.getGameProfile());
    }
}
