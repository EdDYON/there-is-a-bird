package EdDYON.guaniao.content.bath;

import EdDYON.guaniao.registry.GuaniaoItems;
import EdDYON.guaniao.registry.GuaniaoBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BirdBathBlock extends BaseEntityBlock {
    private final BirdBathVariant variant;

    public BirdBathBlock(BirdBathVariant variant, BlockBehaviour.Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public BirdBathVariant variant() {
        return this.variant;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.variant.shape();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BirdBathBlockEntity birdBath)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return this.handleEmptyHand(level, pos, player, birdBath);
        }
        if (stack.is(Items.WATER_BUCKET)) {
            return this.fillWithWaterBucket(level, pos, player, hand, stack, birdBath);
        }
        if (isWaterBottle(stack)) {
            return this.addStackContent(level, pos, player, hand, stack, birdBath, BirdBathContentType.WATER, new ItemStack(Items.GLASS_BOTTLE));
        }
        BirdBathContentType contentType = contentTypeForStack(stack);
        if (!contentType.isEmpty()) {
            return this.addStackContent(level, pos, player, hand, stack, birdBath, contentType, ItemStack.EMPTY);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BirdBathBlockEntity birdBath) {
            birdBath.environmentTick(level, pos, state, random);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BirdBathBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, GuaniaoBlockEntityTypes.BIRD_BATH.get(), BirdBathBlockEntity::serverTick);
    }

    private InteractionResult handleEmptyHand(Level level, BlockPos pos, Player player, BirdBathBlockEntity birdBath) {
        if (birdBath.isSpoiled()) {
            if (!level.isClientSide && birdBath.cleanByHand()) {
                BirdBathEffects.spoiledCleared(level, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown()) {
            if (birdBath.isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide && birdBath.clearContent()) {
                BirdBathEffects.contentCleared(level, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (birdBath.isDirty()) {
            BirdBathCleanliness previous = birdBath.getCleanliness();
            if (!level.isClientSide && birdBath.cleanByHand()) {
                BirdBathEffects.cleaned(level, pos, previous);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult fillWithWaterBucket(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BirdBathBlockEntity birdBath) {
        if (!birdBath.canAccept(BirdBathContentType.WATER)) {
            return InteractionResult.PASS;
        }
        if (birdBath.getContentType() == BirdBathContentType.WATER && birdBath.getContentLevel() >= 3) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            if (birdBath.getCleanliness() == BirdBathCleanliness.FILTHY) {
                birdBath.cleanByHand();
            }
            birdBath.setContent(BirdBathContentType.WATER, 3);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                giveOrReplaceHeldItem(player, hand, new ItemStack(Items.BUCKET));
            }
            BirdBathEffects.waterAdded(level, pos, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult addStackContent(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BirdBathBlockEntity birdBath, BirdBathContentType contentType, ItemStack remainder) {
        if (!birdBath.canAccept(contentType)) {
            if (canReplaceContent(birdBath, contentType)) {
                return this.replaceStackContent(level, pos, player, hand, stack, birdBath, contentType, remainder);
            }
            return InteractionResult.PASS;
        }
        if (birdBath.getContentLevel() >= 3) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            if (birdBath.addContent(contentType, 1)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    if (!remainder.isEmpty()) {
                        giveOrReplaceHeldItem(player, hand, remainder.copy());
                    }
                }
                if (contentType == BirdBathContentType.WATER) {
                    BirdBathEffects.waterAdded(level, pos, net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY);
                } else {
                    BirdBathEffects.foodAdded(level, pos, contentType);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult replaceStackContent(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BirdBathBlockEntity birdBath, BirdBathContentType contentType, ItemStack remainder) {
        if (!level.isClientSide) {
            if (birdBath.setContent(contentType, 1)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    if (!remainder.isEmpty()) {
                        giveOrReplaceHeldItem(player, hand, remainder.copy());
                    }
                }
                BirdBathEffects.foodAdded(level, pos, contentType);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean canReplaceContent(BirdBathBlockEntity birdBath, BirdBathContentType contentType) {
        if (birdBath == null || contentType == null || !contentType.isFood() || birdBath.isSpoiled() || birdBath.isEmpty()) {
            return false;
        }
        BirdBathContentType currentType = birdBath.getContentType();
        return currentType != contentType && (currentType.isWaterLike() || currentType.isFood());
    }

    private static BirdBathContentType contentTypeForStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return BirdBathContentType.EMPTY;
        }
        if (isFish(stack)) {
            return BirdBathContentType.FISH;
        }
        if (isMeat(stack)) {
            return BirdBathContentType.MEAT;
        }
        if (stack.is(Items.BREAD) || stack.is(GuaniaoItems.BREADCRUMBS.get())) {
            return BirdBathContentType.BREAD;
        }
        return BirdBathContentType.EMPTY;
    }

    private static boolean isFish(ItemStack stack) {
        return stack.is(Items.COD)
                || stack.is(Items.COOKED_COD)
                || stack.is(Items.SALMON)
                || stack.is(Items.COOKED_SALMON)
                || stack.is(Items.TROPICAL_FISH)
                || stack.is(Items.PUFFERFISH);
    }

    private static boolean isMeat(ItemStack stack) {
        return stack.is(Items.BEEF)
                || stack.is(Items.COOKED_BEEF)
                || stack.is(Items.PORKCHOP)
                || stack.is(Items.COOKED_PORKCHOP)
                || stack.is(Items.MUTTON)
                || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.CHICKEN)
                || stack.is(Items.COOKED_CHICKEN)
                || stack.is(Items.RABBIT)
                || stack.is(Items.COOKED_RABBIT);
    }

    private static boolean isWaterBottle(ItemStack stack) {
        return stack.is(Items.POTION) && PotionUtils.getPotion(stack) == Potions.WATER;
    }

    private static void giveOrReplaceHeldItem(Player player, InteractionHand hand, ItemStack replacement) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            player.setItemInHand(hand, replacement);
        } else if (!player.addItem(replacement)) {
            player.drop(replacement, false);
        }
    }
}
