package EdDYON.guaniao.content.camera;

import EdDYON.guaniao.client.camera.PhotographItemRenderer;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

public class PhotographItem extends Item {
    public PhotographItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        LegacyPhotoMigration.queue(level, stack);
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
                    throw new IllegalStateException("Failed to open photograph screen", exception);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        Direction face = context.getClickedFace();
        ItemStack stack = context.getItemInHand();

        if (player == null || !PhotographData.hasImage(stack) || !face.getAxis().isHorizontal()) {
            return InteractionResult.PASS;
        }

        BlockPos placePos = context.getClickedPos().relative(face);
        if (!level.isClientSide) {
            LegacyPhotoMigration.migrateNow(level, stack);
            ItemStack photo = stack.copy();
            photo.setCount(1);
            LegacyPhotoMigration.queue(level, photo);
            PhotographEntity entity = new PhotographEntity(level, placePos, face, photo);
            if (!entity.survives()) {
                return InteractionResult.FAIL;
            }
            level.addFreshEntity(entity);
            level.playSound(null, placePos, SoundEvents.ITEM_FRAME_PLACE, SoundSource.BLOCKS, 0.8F, 1.1F);
            stack.shrink(1);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!PhotographData.hasImage(stack)) {
            tooltip.add(Component.translatable("item.guaniao.photograph.tooltip.empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        String photographer = PhotographData.photographer(stack);
        if (!photographer.isEmpty()) {
            tooltip.add(Component.translatable("item.guaniao.photograph.tooltip.photographer", photographer).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.guaniao.photograph.tooltip.actions").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private PhotographItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new PhotographItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
