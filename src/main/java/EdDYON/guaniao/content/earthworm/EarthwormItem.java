package EdDYON.guaniao.content.earthworm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public final class EarthwormItem extends Item {
    public EarthwormItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        Vec3 position = context.getClickLocation().add(0.0D, 0.01D, 0.0D);
        BlockPos stand = BlockPos.containing(position);
        if (!context.getLevel().mayInteract(player, context.getClickedPos())
                || !player.mayUseItemAt(stand, Direction.UP, context.getItemInHand())) return InteractionResult.FAIL;
        if (context.getLevel() instanceof ServerLevel level) {
            EarthwormEntity worm = EarthwormEntity.spawn(level, position, false);
            if (worm == null) return InteractionResult.FAIL;
            if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
