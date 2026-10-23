package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.cassowary.CassowaryEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Gives common hostile mobs a real pathfinding goal to keep away from cassowaries. */
@EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class CassowaryThreatEvents {
    private CassowaryThreatEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof PathfinderMob mob)
                || !isAffectedHostile(mob)) {
            return;
        }
        boolean alreadyInstalled = mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof CassowaryAvoidGoal);
        if (!alreadyInstalled) {
            mob.goalSelector.addGoal(1, new CassowaryAvoidGoal(mob));
        }
    }

    @SubscribeEvent
    public static void onLivingTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)
                || !isAffectedHostile(mob)
                || Math.floorMod(mob.tickCount + mob.getId(), 20) != 0) {
            return;
        }
        if (mob.getTarget() instanceof CassowaryEntity) {
            mob.setTarget(null);
        }
    }

    private static boolean isAffectedHostile(PathfinderMob mob) {
        return mob instanceof Zombie || mob instanceof AbstractSkeleton || mob instanceof Spider;
    }

    private static final class CassowaryAvoidGoal extends AvoidEntityGoal<CassowaryEntity> {
        private CassowaryAvoidGoal(PathfinderMob mob) {
            super(mob, CassowaryEntity.class, 13.0F, 1.10D, 1.35D);
        }

        @Override
        public boolean canUse() {
            boolean canUse = super.canUse();
            if (canUse && this.mob.getTarget() instanceof CassowaryEntity) {
                this.mob.setTarget(null);
            }
            return canUse;
        }
    }
}
