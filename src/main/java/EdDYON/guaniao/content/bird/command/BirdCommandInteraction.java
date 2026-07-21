package EdDYON.guaniao.content.bird.command;

import EdDYON.guaniao.config.BirdConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

public final class BirdCommandInteraction {
    private BirdCommandInteraction() {
    }

    public static InteractionResult tryHandle(TamableAnimal bird, CommandableBird commandable, Player player,
                                              InteractionHand hand) {
        if (!BirdConfigManager.petBirdCommandsEnabled()
                || !bird.isTame() || !bird.isOwnedBy(player) || !player.isShiftKeyDown()
                || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!bird.level().isClientSide) {
            BirdCommandMode next = commandable.getBirdCommandMode().next();
            commandable.setBirdCommandMode(next);
            bird.getNavigation().stop();
            player.displayClientMessage(Component.translatable(next.translationKey(), bird.getDisplayName()), true);
        }
        return InteractionResult.sidedSuccess(bird.level().isClientSide);
    }
}
