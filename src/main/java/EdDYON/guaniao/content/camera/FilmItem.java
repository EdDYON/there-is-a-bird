package EdDYON.guaniao.content.camera;

import EdDYON.guaniao.client.camera.FilmItemRenderer;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

public class FilmItem extends Item {
    public FilmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && PhotographData.hasImage(stack)) {
            ItemStack copy = stack.copy();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    Class.forName("EdDYON.guaniao.client.camera.PhotographClientActions")
                            .getMethod("openScreen", ItemStack.class)
                            .invoke(null, copy);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Failed to open film preview", exception);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!PhotographData.hasImage(stack)) {
            tooltip.add(Component.translatable("item.guaniao.film.tooltip.empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        String photographer = PhotographData.photographer(stack);
        if (!photographer.isEmpty()) {
            tooltip.add(Component.translatable("item.guaniao.photograph.tooltip.photographer", photographer).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.guaniao.film.tooltip.frame").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private FilmItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new FilmItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
