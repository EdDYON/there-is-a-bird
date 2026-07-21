package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.dropping.BirdDroppingPrankHandler;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Ticks only registered birds and affected villagers instead of every LivingEntity. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdServerTickManager {
    private static final Map<ServerLevel, LinkedHashMap<UUID, Mob>> BIRDS = new WeakHashMap<>();

    private BirdServerTickManager() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity() instanceof Mob mob && BirdSpecies.from(mob) != null) {
            BIRDS.computeIfAbsent(level, ignored -> new LinkedHashMap<>()).put(mob.getUUID(), mob);
        } else if (event.getEntity() instanceof Villager villager) {
            BirdDroppingPrankHandler.trackVillager(villager);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Map<UUID, Mob> birds = BIRDS.get(level);
            if (birds != null) {
                birds.remove(event.getEntity().getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            Map<UUID, Mob> birds = BIRDS.get(level);
            if (birds == null) {
                continue;
            }
            Iterator<Mob> iterator = birds.values().iterator();
            while (iterator.hasNext()) {
                Mob bird = iterator.next();
                if (bird.isRemoved() || bird.level() != level || BirdSpecies.from(bird) == null) {
                    iterator.remove();
                    continue;
                }
                BirdDroppingEvents.tickBird(bird);
                BirdPopulationTracker.tickBird(bird);
            }
        }
        BirdDroppingPrankHandler.tickTrackedVillagers(event.getServer());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BIRDS.remove(level);
            BirdDroppingPrankHandler.forgetLevel(level);
        }
    }
}
