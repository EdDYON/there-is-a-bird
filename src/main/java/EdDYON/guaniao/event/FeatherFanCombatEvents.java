package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.fan.FeatherFanProjectileEntity;
import EdDYON.guaniao.registry.GuaniaoItems;
import EdDYON.guaniao.registry.GuaniaoParticleTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class FeatherFanCombatEvents {
    private FeatherFanCombatEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (!victim.getType().is(BirdTags.BIRDS)) {
            return;
        }

        DamageSource source = event.getSource();
        Entity directAttacker = source.getDirectEntity();
        boolean projectileAttack = directAttacker instanceof FeatherFanProjectileEntity;
        boolean meleeAttack = source.getEntity() instanceof Player player
                && directAttacker == player
                && player.getMainHandItem().is(GuaniaoItems.WIND_FEATHER_FAN.get());
        if (projectileAttack || meleeAttack) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || victim instanceof Player) {
            return;
        }

        DamageSource source = event.getSource();
        Entity directAttacker = source.getDirectEntity();
        boolean projectileKill = directAttacker instanceof FeatherFanProjectileEntity;
        boolean meleeKill = source.getEntity() instanceof Player player
                && directAttacker == player
                && player.getMainHandItem().is(GuaniaoItems.WIND_FEATHER_FAN.get());
        if (!projectileKill && !meleeKill) {
            return;
        }

        double x = victim.getX();
        double y = victim.getY() + victim.getBbHeight() * 0.72D;
        double z = victim.getZ();
        level.sendParticles(ParticleTypes.POOF, x, y, z, 3, 0.16D, 0.18D, 0.16D, 0.018D);
        int featherCount = 3 + level.random.nextInt(5);
        level.sendParticles(GuaniaoParticleTypes.KILL_FEATHER.get(), x, y + 0.10D, z,
                featherCount, 0.22D, 0.10D, 0.22D, 0.0D);
    }
}
