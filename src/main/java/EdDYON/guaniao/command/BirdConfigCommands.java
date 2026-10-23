package EdDYON.guaniao.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigData;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.note.BirdNoteContent;
import EdDYON.guaniao.network.GuaniaoNetwork;
import EdDYON.guaniao.network.OpenBirdConfigPacket;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
        event.getDispatcher().register(Commands.literal("guaniao")
                .requires(BirdConfigCommands::canEdit)
                .then(Commands.literal("seagullRaid")
                        .executes(context -> seagullRaid(context.getSource())))
                .then(Commands.literal("birdNotes")
                        .executes(context -> birdNotes(context.getSource()))));
    }

    private static int open(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BirdConfigManager.loadForServer(source.getServer());
        GuaniaoNetwork.sendToPlayer(new OpenBirdConfigPacket(BirdConfigManager.snapshot()), player);
        return 1;
    }

    private static int seagullRaid(CommandSourceStack source) {
        BirdConfigData config = BirdConfigManager.snapshot();
        config.global.seagullPlayerCooldownTicks = 0;
        config.global.maxConcurrentSeagullTargetsPerPlayer = 8;
        config.global.aprilFoolsMode = true;
        if (BirdConfigManager.replaceAndSave(config, source.getServer())) {
            return 1;
        }
        return 0;
    }

    private static int birdNotes(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        List<ItemStack> notes = BirdNoteContent.allNotes();
        // A single chest holds 27 slots, so the full collection needs a row of chests.
        int chestCount = (notes.size() + 26) / 27;
        BlockPos base = findChestSpot(level, player);
        if (base == null) {
            return 0;
        }
        Direction facing = player.getDirection().getOpposite();
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0.0, look.x);
        right = right.lengthSqr() < 1.0E-8 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        int stepX = (int) Math.round(right.x);
        int stepZ = (int) Math.round(right.z);
        for (int c = 0; c < chestCount; c++) {
            BlockPos pos = base.offset(stepX * c, 0, stepZ * c);
            if (!canPlaceChest(level, pos)) {
                return 0;
            }
            level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 3);
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                int from = c * 27;
                int to = Math.min(from + 27, notes.size());
                for (int i = from; i < to; i++) {
                    chest.setItem(i - from, notes.get(i));
                }
                chest.setChanged();
            }
        }
        return 1;
    }

    private static boolean canPlaceChest(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()) {
            return false;
        }
        BlockState above = level.getBlockState(pos.above());
        return above.isAir() || above.canBeReplaced();
    }

    /** Finds a reachable spot in front of the player where a chest can be placed. */
    private static BlockPos findChestSpot(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        for (int dist = 2; dist <= 6; dist++) {
            BlockPos pos = BlockPos.containing(eye.x + look.x * dist, eye.y + look.y * dist, eye.z + look.z * dist);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.canBeReplaced()) {
                continue;
            }
            BlockState above = level.getBlockState(pos.above());
            if (above.isAir() || above.canBeReplaced()) {
                return pos;
            }
        }
        return null;
    }

    public static boolean canEdit(CommandSourceStack source) {
        if (source.hasPermission(2)) {
            return true;
        }
        return source.getEntity() instanceof ServerPlayer player
                && source.getServer().isSingleplayerOwner(player.getGameProfile());
    }
}
