package EdDYON.guaniao.content.food;

import EdDYON.guaniao.registry.GuaniaoBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class BaggedFriesBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final String FRY_MASK_TAG = "FryMask";
    private static final int FULL_MASK = (1 << BaggedFriesBlock.TOTAL_FRIES) - 1;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int fryMask = FULL_MASK;

    public BaggedFriesBlockEntity(BlockPos pos, BlockState state) {
        super(GuaniaoBlockEntityTypes.BAGGED_FRIES.get(), pos, state);
    }

    public int getFryMask() {
        return this.fryMask;
    }

    public int getRemainingFries() {
        return Integer.bitCount(this.fryMask);
    }

    public boolean isFryVisible(int oneBasedIndex) {
        if (oneBasedIndex < 1 || oneBasedIndex > BaggedFriesBlock.TOTAL_FRIES) {
            return false;
        }
        return (this.fryMask & (1 << (oneBasedIndex - 1))) != 0;
    }

    public int removeRandomFries(RandomSource random) {
        int remaining = this.getRemainingFries();
        if (remaining <= 0) {
            return 0;
        }

        int removeCount = Math.min(remaining, 1 + random.nextInt(3));
        for (int removed = 0; removed < removeCount; removed++) {
            int selected = random.nextInt(Integer.bitCount(this.fryMask));
            for (int index = 0; index < BaggedFriesBlock.TOTAL_FRIES; index++) {
                int bit = 1 << index;
                if ((this.fryMask & bit) == 0) {
                    continue;
                }
                if (selected-- == 0) {
                    this.fryMask &= ~bit;
                    break;
                }
            }
        }
        this.setChanged();
        return removeCount;
    }

    public void syncToClient() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(FRY_MASK_TAG, this.fryMask);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(FRY_MASK_TAG)) {
            this.fryMask = tag.getInt(FRY_MASK_TAG) & FULL_MASK;
        } else if (this.getBlockState().hasProperty(BaggedFriesBlock.FRIES)) {
            int count = this.getBlockState().getValue(BaggedFriesBlock.FRIES);
            this.fryMask = count <= 0 ? 0 : (1 << Math.min(count, BaggedFriesBlock.TOTAL_FRIES)) - 1;
        } else {
            this.fryMask = FULL_MASK;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            this.load(tag);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }
}
