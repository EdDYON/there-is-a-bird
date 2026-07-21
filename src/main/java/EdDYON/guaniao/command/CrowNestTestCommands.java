package EdDYON.guaniao.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.nest.CrowNestBlockEntity;
import EdDYON.guaniao.content.nest.CrowNestTestLootPool;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Operator-only command for quickly exercising every bird-nest rummage profile. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class CrowNestTestCommands {
    private CrowNestTestCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (FMLEnvironment.production) {
            return;
        }
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

        List<ItemStack> rewards = CrowNestTestLootPool.roll(player.getRandom());
        for (int slot = 0; slot < rewards.size(); slot++) {
            put(nest, slot, rewards.get(slot));
        }
        BlockPos pos = blockHit.getBlockPos();
        source.sendSuccess(() -> Component.translatable("message.guaniao.crow_nest.test_filled", pos.getX(), pos.getY(), pos.getZ()), false);
        return 1;
    }

    private static void put(CrowNestBlockEntity nest, int slot, ItemStack stack) {
        nest.setItem(slot, stack);
    }
}
