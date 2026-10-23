package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class HummingbirdEcologyEvents {
    private HummingbirdEcologyEvents() { }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            FlowerHabitatIndex.tick(level);
            HummingbirdPollinationService.tick(level);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) FlowerHabitatIndex.invalidate(level, event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void broken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) FlowerHabitatIndex.invalidate(level, event.getPos());
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Mob mob
                && HummingbirdPollinationService.isHummingbird(mob)) {
            HummingbirdPollinationService.remove(mob);
            FlowerHabitatIndex.releaseAll(level, mob.getUUID());
        }
    }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            FlowerHabitatIndex.unloadChunk(level, event.getChunk().getPos().x, event.getChunk().getPos().z);
        }
    }

    @SubscribeEvent
    public static void levelUnloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            FlowerHabitatIndex.forgetLevel(level);
            HummingbirdPollinationService.forgetLevel(level);
        }
    }
}
