package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.mutation.BirdMutation;
import EdDYON.guaniao.content.bird.mutation.BirdMutationHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Rewards for the rarest mutations. Golden birds drop gold ingots when defeated —
 * the rarer pure-gold form is worth more, and looting has a chance to add one extra.
 */
@EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdMutationEvents {
    private BirdMutationEvents() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof BirdMutationHolder holder)) {
            return;
        }
        BirdMutation mutation = holder.getBirdMutation();
        if (!mutation.isGold()) {
            return;
        }
        int count = mutation == BirdMutation.GOLDEN_PURE ? 3 : 1;
        // Same gameplay query as 1.21 loot tables; ItemStack's NeoForge hook
        // includes GetEnchantmentLevelEvent overrides, not just stored levels.
        int looting = event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity killer
                ? net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                        entity.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), killer) : 0;
        if (looting > 0 && entity.level().random.nextFloat() < 0.5F * looting) {
            count++;
        }
        // Spawned directly, not via getDrops(): the "birds never drop loot"
        // handler runs at LOWEST priority and clears the drops list, which
        // would swallow this reward if it lived in the list.
        entity.spawnAtLocation(new ItemStack(Items.GOLD_INGOT, count));
    }
}
