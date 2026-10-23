package EdDYON.guaniao.content.camera;

import EdDYON.guaniao.content.camera.frame.FrameDesign;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import EdDYON.guaniao.util.ClientActions;

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
            ClientActions.run(() -> () -> {
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

        if (player == null || (!PhotographData.hasImage(stack) && FrameDesign.read(stack) == null) || !face.getAxis().isHorizontal()) {
            return InteractionResult.PASS;
        }

        FrameDesign placementDesign = FrameDesign.read(stack);
        if (placementDesign != null && !placementDesign.hasArea()) return InteractionResult.FAIL;
        BlockPos placePos = context.getClickedPos().relative(face);
        if (!player.mayBuild() || !level.mayInteract(player, placePos) || !player.mayUseItemAt(placePos, face, stack)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            LegacyPhotoMigration.migrateNow(level, stack);
            ItemStack photo = stack.copy();
            photo.setCount(1);
            LegacyPhotoMigration.queue(level, photo);
            PhotographEntity entity = new PhotographEntity(level, placePos, face, photo);
            if (!entity.survives()) {
                return InteractionResult.FAIL;
            }
            if (!level.addFreshEntity(entity)) {
                return InteractionResult.FAIL;
            }
            level.playSound(null, placePos, SoundEvents.ITEM_FRAME_PLACE, SoundSource.BLOCKS, 0.8F, 1.1F);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FrameDesign design = FrameDesign.read(stack);
        if (design == null) {
            PhotographFrameStyle style = PhotographFrameStyle.read(stack);
            tooltip.add(Component.translatable("item.guaniao.photograph.tooltip.style",
                    Component.translatable("frame.guaniao.shape." + style.shape().id()),
                    Component.translatable("frame.guaniao.material." + style.material().id()),
                    Component.translatable("frame.guaniao.ornament." + style.ornament().id())).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.guaniao.photograph.tooltip.design", blockSize(design.widthPixels()), blockSize(design.heightPixels()),
                    BuiltInRegistries.ITEM.containsKey(design.frameMaterial())
                            ? BuiltInRegistries.ITEM.get(design.frameMaterial()).getDescription()
                            : Component.literal(design.frameMaterial().toString())).withStyle(ChatFormatting.GRAY));
        }
        if (!PhotographData.hasImage(stack)) {
            tooltip.add(Component.translatable("item.guaniao.photograph.tooltip.empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        String photographer = PhotographData.photographer(stack);
        if (!photographer.isEmpty()) {
            tooltip.add(Component.translatable("item.guaniao.photograph.tooltip.photographer",
                    PhotoAuthorText.component(photographer)).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.guaniao.photograph.tooltip.actions").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.guaniao.photograph.tooltip.wall").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String blockSize(int pixels) {
        return BigDecimal.valueOf(pixels).divide(BigDecimal.valueOf(16)).stripTrailingZeros().toPlainString();
    }

}
