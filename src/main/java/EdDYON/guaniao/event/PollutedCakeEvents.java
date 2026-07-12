package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.dropping.PollutedCakeData;
import EdDYON.guaniao.content.dropping.PrankFoodEffectUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class PollutedCakeEvents {
    private PollutedCakeEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) {
            PollutedCakeData.get(level).tick(level);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickCake(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)
                || event.isCanceled()
                || !(level.getBlockState(event.getPos()).getBlock() instanceof CakeBlock)
                || !PollutedCakeData.get(level).isPolluted(event.getPos())
                || !player.canEat(false)) {
            return;
        }

        PrankFoodEffectUtil.apply(player);
    }
}
