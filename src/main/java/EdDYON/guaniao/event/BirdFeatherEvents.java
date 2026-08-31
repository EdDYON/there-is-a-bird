package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.BirdAmbientDropControl;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.mutation.BirdMutation;
import EdDYON.guaniao.content.bird.mutation.BirdMutationHolder;
import EdDYON.guaniao.registry.GuaniaoItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Shared feather selection plus the no-loot rule for dead birds.
 */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdFeatherEvents {
    private static final int DEATH_MESSAGE_COUNT = 72;

    private BirdFeatherEvents() {
    }

    /**
     * The feather a bird would drop: a random color from its species pool for normal birds,
     * the rainbow feather for rainbow mutation birds, or null for other mutations.
     */
    public static Item randomFeatherFor(LivingEntity bird) {
        BirdSpecies species = BirdSpecies.from(bird);
        if (species == null) {
            return null;
        }
        BirdMutation mutation = bird instanceof BirdMutationHolder holder ? holder.getBirdMutation() : BirdMutation.NONE;
        if (mutation == BirdMutation.RAINBOW) {
            return GuaniaoItems.FEATHER_RAINBOW.get();
        }
        if (mutation != BirdMutation.NONE) {
            return null;
        }
        List<Item> feathers = GuaniaoItems.feathersFor(species);
        if (feathers.isEmpty()) {
            return null;
        }
        return feathers.get(bird.level().random.nextInt(feathers.size()));
    }

    /** Shearing accepts every mutation, mapping its appearance to the closest feather color. */
    public static Item randomShearFeatherFor(LivingEntity bird) {
        BirdMutation mutation = bird instanceof BirdMutationHolder holder
                ? holder.getBirdMutation() : BirdMutation.NONE;
        return switch (mutation) {
            case LEUCISTIC -> GuaniaoItems.FEATHER_WHITE.get();
            case MELANISTIC -> GuaniaoItems.FEATHER_BLACK.get();
            case GOLDEN, GOLDEN_PURE -> GuaniaoItems.FEATHER_YELLOW.get();
            case RAINBOW -> GuaniaoItems.FEATHER_RAINBOW.get();
            case NONE -> randomFeatherFor(bird);
        };
    }

    /** Drops a feather on the ground where the bird is standing. */
    public static void spawnFeather(LivingEntity bird, Item feather, int count) {
        if (bird.level().isClientSide()) {
            return;
        }
        ItemEntity item = new ItemEntity(bird.level(), bird.getX(),
                bird.getY() + Math.max(0.1D, bird.getBbHeight() * 0.5D), bird.getZ(),
                new ItemStack(feather, count));
        item.setDeltaMovement(bird.level().random.nextDouble() * 0.08D - 0.04D, 0.1D,
                bird.level().random.nextDouble() * 0.08D - 0.04D);
        item.setPickUpDelay(20);
        BirdAmbientDropControl.applyRandomLifetime(item);
        bird.level().addFreshEntity(item);
    }

    public static boolean canSpawnNaturalFeather(ServerLevel level, LivingEntity bird) {
        if (!BirdAmbientDropControl.hasNearbyPlayer(level, bird)) {
            return false;
        }
        return level.getEntitiesOfClass(
                ItemEntity.class,
                bird.getBoundingBox().inflate(BirdAmbientDropControl.LOCAL_CAP_RADIUS),
                item -> item.isAlive() && item.getItem().is(BirdTags.FEATHERS)
        ).size() < BirdAmbientDropControl.MAX_NATURAL_FEATHERS_NEARBY;
    }

    /** Birds never drop items on death; a player killer instead receives a random warning. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof LivingEntity bird)
                || BirdSpecies.from(bird) == null) {
            return;
        }

        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            int message = player.getRandom().nextInt(DEATH_MESSAGE_COUNT);
            player.sendSystemMessage(Component.translatable("message.guaniao.bird_death." + message)
                    .withStyle(ChatFormatting.GOLD));
        }

        // LOWEST runs after this mod's mutation rewards and normal loot generation.
        event.getDrops().clear();
        event.setCanceled(true);
    }
}
