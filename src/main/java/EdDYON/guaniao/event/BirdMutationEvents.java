package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.mutation.BirdMutation;
import EdDYON.guaniao.content.bird.mutation.BirdMutationHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Rewards for the rarest mutations. Golden birds drop gold ingots when defeated —
 * the rarer pure-gold form is worth more, and looting has a chance to add one extra.
 */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
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
        if (event.getLootingLevel() > 0 && entity.level().random.nextFloat() < 0.5F * event.getLootingLevel()) {
            count++;
        }
        event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                new ItemStack(Items.GOLD_INGOT, count)));
    }
}
