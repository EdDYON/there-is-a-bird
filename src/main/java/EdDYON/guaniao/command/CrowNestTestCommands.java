package EdDYON.guaniao.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.nest.CrowNestBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Operator-only command for quickly exercising every bird-nest rummage profile. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class CrowNestTestCommands {
    private CrowNestTestCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guaniao_nest_test")
                .requires(BirdConfigCommands::canEdit)
                .executes(context -> fillTargetedNest(context.getSource())));
    }

    private static int fillTargetedNest(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(8.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)
                || !(player.level().getBlockEntity(blockHit.getBlockPos()) instanceof CrowNestBlockEntity nest)) {
            source.sendFailure(Component.translatable("message.guaniao.crow_nest.test_target"));
            return 0;
        }
        if (nest.hasTreasure()) {
            source.sendFailure(Component.translatable("message.guaniao.crow_nest.test_not_empty"));
            return 0;
        }

        put(nest, 0, new ItemStack(Items.IRON_NUGGET, 12));
        put(nest, 1, new ItemStack(Items.IRON_INGOT, 4));
        put(nest, 2, new ItemStack(Items.AMETHYST_SHARD, 3));
        put(nest, 3, new ItemStack(Items.GOLD_INGOT, 2));
        put(nest, 4, new ItemStack(Items.EMERALD));
        put(nest, 5, new ItemStack(Items.DIAMOND));
        BlockPos pos = blockHit.getBlockPos();
        source.sendSuccess(() -> Component.translatable("message.guaniao.crow_nest.test_filled", pos.getX(), pos.getY(), pos.getZ()), false);
        return 1;
    }

    private static void put(CrowNestBlockEntity nest, int slot, ItemStack stack) {
        nest.setItem(slot, stack);
    }
}
