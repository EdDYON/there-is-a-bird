package EdDYON.guaniao.command;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.woodcock.WoodcockEntity;
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
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Operator-only visual test commands for the woodcock's authored locomotion. */
@EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class WoodcockTestCommands {
    private WoodcockTestCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guaniao")
                .requires(BirdConfigCommands::canEdit)
                .then(Commands.literal("woodcockRockWalk")
                        .executes(context -> spawnRockWalkingWoodcock(context.getSource(), false))
                        .then(Commands.literal("loop")
                                .executes(context -> spawnRockWalkingWoodcock(context.getSource(), true)))));
    }

    private static int spawnRockWalkingWoodcock(CommandSourceStack source, boolean loop) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        Vec3 forward = horizontalForward(player);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        BlockPos spawnPos = findStandPosition(level, player.position().add(forward.scale(4.0D)));
        if (spawnPos == null) {
            return 0;
        }
        BlockPos targetPos = findStandPosition(level, Vec3.atBottomCenterOf(spawnPos).add(right.scale(5.0D)));
        if (targetPos == null || targetPos.equals(spawnPos)) {
            return 0;
        }

        WoodcockEntity woodcock = GuaniaoEntityTypes.WOODCOCK.get().create(level);
        if (woodcock == null) {
            return 0;
        }
        Vec3 spawn = Vec3.atBottomCenterOf(spawnPos);
        Vec3 target = Vec3.atBottomCenterOf(targetPos);
        Vec3 travel = target.subtract(spawn);
        float yaw = (float)(Math.atan2(travel.z, travel.x) * 180.0D / Math.PI) - 90.0F;
        woodcock.moveTo(spawn.x, spawn.y, spawn.z, yaw, 0.0F);
        woodcock.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                MobSpawnType.COMMAND, null);
        woodcock.setPersistenceRequired();
        if (!level.addFreshEntity(woodcock)) {
            return 0;
        }
        woodcock.startRockWalkTest(target, loop);
        return 1;
    }

    private static Vec3 horizontalForward(ServerPlayer player) {
        Vec3 forward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        return forward.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : forward.normalize();
    }

    private static BlockPos findStandPosition(ServerLevel level, Vec3 desired) {
        BlockPos origin = BlockPos.containing(desired);
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4, -4};
        for (int offset : offsets) {
            BlockPos feet = origin.offset(0, offset, 0);
            if (isOpen(level, feet) && isOpen(level, feet.above())
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
