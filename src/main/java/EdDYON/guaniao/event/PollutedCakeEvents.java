package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.dropping.PollutedCakeData;
import EdDYON.guaniao.content.dropping.PrankFoodEffectUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CakeBlock;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class PollutedCakeEvents {
    private PollutedCakeEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PollutedCakeData.get(level).tick(level);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PollutedCakeData.get(level).validateChunk(level, event.getChunk().getPos());
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
