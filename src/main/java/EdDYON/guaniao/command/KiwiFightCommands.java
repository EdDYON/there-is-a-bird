package EdDYON.guaniao.command;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.kiwi.KiwiEntity;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class KiwiFightCommands {
    private KiwiFightCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guaniao")
                .requires(BirdConfigCommands::canEdit)
                .then(Commands.literal("kiwiFight")
                        .executes(context -> spawnKiwiFight(context.getSource()))));
    }

    private static int spawnKiwiFight(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();

        Vec3 forward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() < 1.0E-4D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forward = forward.normalize();
        }
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 center = player.position().add(forward.scale(4.0D));
        Vec3 desiredA = center.add(right.scale(1.5D));
        Vec3 desiredB = center.subtract(right.scale(1.5D));
        BlockPos standA = findStandPosition(level, desiredA);
        BlockPos standB = findStandPosition(level, desiredB);
        if (standA == null || standB == null || standA.equals(standB)) {
            return 0;
        }

        KiwiEntity first = createKiwi(level, desiredA, standA, player.getYRot());
        KiwiEntity second = createKiwi(level, desiredB, standB, player.getYRot() + 180.0F);
        if (first == null || second == null) {
            return 0;
        }

        if (!level.addFreshEntity(first)) {
            return 0;
        }
        if (!level.addFreshEntity(second)) {
            first.discard();
            return 0;
        }

        BlockPos homeA = BlockPos.containing(center.add(right.scale(8.0D)));
        BlockPos homeB = BlockPos.containing(center.subtract(right.scale(8.0D)));
        if (!first.startCommandConflict(second, homeA, homeB)) {
            first.discard();
            second.discard();
            return 0;
        }

        return 2;
    }

    private static KiwiEntity createKiwi(ServerLevel level, Vec3 desired, BlockPos stand, float yaw) {
        KiwiEntity kiwi = GuaniaoEntityTypes.KIWI.get().create(level);
        if (kiwi == null) {
            return null;
        }
        kiwi.moveTo(desired.x, stand.getY(), desired.z, yaw, 0.0F);
        kiwi.finalizeSpawn(level, level.getCurrentDifficultyAt(stand), MobSpawnType.COMMAND, null, null);
        kiwi.setPersistenceRequired();
        return kiwi;
    }

    private static BlockPos findStandPosition(ServerLevel level, Vec3 desired) {
        BlockPos origin = BlockPos.containing(desired);
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4, -4};
        for (int offset : offsets) {
            BlockPos feet = origin.offset(0, offset, 0);
            if (isOpen(level, feet)
                    && isOpen(level, feet.above())
                    && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) {
                return feet;
            }
        }
        return null;
    }

    private static boolean isOpen(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }
}
