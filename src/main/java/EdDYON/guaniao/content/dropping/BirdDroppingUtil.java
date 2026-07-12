package EdDYON.guaniao.content.dropping;

import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.ComposterBlock;

public final class BirdDroppingUtil {
    private BirdDroppingUtil() {
    }

    public static boolean isDropping(ItemStack stack) {
        return variantOf(stack) != null;
    }

    public static BirdDroppingVariant variantOf(ItemStack stack) {
        if (stack.getItem() instanceof BirdDroppingItem droppingItem) {
            return droppingItem.variant();
        }
        if (stack.is(GuaniaoItems.BIRD_DROPPING_1.get())) {
            return BirdDroppingVariant.ONE;
        }
        if (stack.is(GuaniaoItems.BIRD_DROPPING_2.get())) {
            return BirdDroppingVariant.TWO;
        }
        if (stack.is(GuaniaoItems.BIRD_DROPPING_3.get())) {
            return BirdDroppingVariant.THREE;
        }
        if (stack.is(GuaniaoItems.BIRD_DROPPING_4.get())) {
            return BirdDroppingVariant.FOUR;
        }
        return null;
    }

    public static float compostChance(BirdDroppingVariant variant) {
        return switch (variant) {
            case ONE -> 0.60F;
            case TWO -> 0.65F;
            case THREE -> 0.70F;
            case FOUR -> 0.75F;
        };
    }

    public static float farmlandFertilizeChance(BirdDroppingVariant variant) {
        return switch (variant) {
            case ONE -> 0.20F;
            case TWO -> 0.25F;
            case THREE -> 0.30F;
            case FOUR -> 0.35F;
        };
    }

    public static ItemStack randomDroppingStack(RandomSource random) {
        return new ItemStack(BirdDroppingVariant.random(random).item());
    }

    public static void registerCompostables() {
        registerCompostable(GuaniaoItems.BIRD_DROPPING_1.get(), compostChance(BirdDroppingVariant.ONE));
        registerCompostable(GuaniaoItems.BIRD_DROPPING_2.get(), compostChance(BirdDroppingVariant.TWO));
        registerCompostable(GuaniaoItems.BIRD_DROPPING_3.get(), compostChance(BirdDroppingVariant.THREE));
        registerCompostable(GuaniaoItems.BIRD_DROPPING_4.get(), compostChance(BirdDroppingVariant.FOUR));
    }

    private static void registerCompostable(Item item, float chance) {
        ComposterBlock.COMPOSTABLES.put((ItemLike)item, chance);
    }
}
