package EdDYON.guaniao.content.cage;

import EdDYON.guaniao.registry.GuaniaoBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BirdCageBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public BirdCageBlockEntity(BlockPos pos, BlockState state) {
        super(GuaniaoBlockEntityTypes.BIRD_CAGE.get(), pos, state);
    }

    public BirdCageVariant variant() {
        if (this.getBlockState().getBlock() instanceof BirdCageBlock birdCageBlock) {
            return birdCageBlock.variant();
        }
        return BirdCageVariant.SMALL;
    }

    public AABB getRenderBoundingBox() {
        BlockPos pos = this.getBlockPos();
        return switch (this.variant()) {
            case SMALL -> new AABB(pos).inflate(0.75D, 0.25D, 0.75D).expandTowards(0.0D, 1.0D, 0.0D);
            case MEDIUM -> new AABB(pos).inflate(1.25D, 0.25D, 1.25D).expandTowards(0.0D, 2.5D, 0.0D);
            case LARGE -> new AABB(pos).inflate(1.75D, 0.25D, 1.25D).expandTowards(0.0D, 3.5D, 0.0D);
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }
}
