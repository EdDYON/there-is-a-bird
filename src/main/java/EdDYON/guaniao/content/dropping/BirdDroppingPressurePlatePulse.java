package EdDYON.guaniao.content.dropping;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Event-driven, one-shot pressure plate pulses caused by an impacting bird dropping. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdDroppingPressurePlatePulse {
    private static final Map<ServerLevel, Map<Long, Long>> ACTIVE = new WeakHashMap<>();

    private BirdDroppingPressurePlatePulse() {
    }

    public static boolean tryTrigger(ServerLevel level, BlockPos pos, Entity triggeringEntity) {
        if (!BirdConfigManager.droppingPressurePlatePulseEnabled()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BasePressurePlateBlock)) {
            return false;
        }

        long now = level.getGameTime();
        Map<Long, Long> pulses = ACTIVE.computeIfAbsent(level, ignored -> new HashMap<>());
        long key = pos.asLong();
        long currentExpiry = pulses.getOrDefault(key, Long.MIN_VALUE);
        if (currentExpiry > now) {
            return false;
        }

        int duration = BirdConfigManager.droppingPressurePlatePulseTicks();
        pulses.put(key, now + duration);
        state.entityInside(level, pos, triggeringEntity);
        level.scheduleTick(pos, state.getBlock(), duration);
        return true;
    }

    public static boolean isActive(ServerLevel level, BlockPos pos) {
        Map<Long, Long> pulses = ACTIVE.get(level);
        if (pulses == null) {
            return false;
        }
        long key = pos.asLong();
        Long expiry = pulses.get(key);
        if (expiry == null) {
            return false;
        }
        if (level.getGameTime() < expiry) {
            return true;
        }
        pulses.remove(key);
        if (pulses.isEmpty()) {
            ACTIVE.remove(level);
        }
        return false;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ACTIVE.remove(level);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || level.getGameTime() % 200L != 0L) {
            return;
        }
        Map<Long, Long> pulses = ACTIVE.get(level);
        if (pulses != null) {
            long now = level.getGameTime();
            pulses.values().removeIf(expiry -> expiry <= now);
            if (pulses.isEmpty()) {
                ACTIVE.remove(level);
            }
        }
    }
}
