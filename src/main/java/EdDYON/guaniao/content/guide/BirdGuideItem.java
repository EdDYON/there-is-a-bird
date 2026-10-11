package EdDYON.guaniao.content.guide;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class BirdGuideItem
extends Item {
    public BirdGuideItem(Item.Properties properties) {
        super(properties);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> {
                try {
                    Class.forName("EdDYON.guaniao.client.guide.BirdGuideClient").getMethod("open", new Class[0]).invoke(null, new Object[0]);
                }
                catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Failed to open bird guide screen", exception);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
