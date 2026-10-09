package EdDYON.guaniao.content.cage;

import EdDYON.guaniao.client.particle.PlaceableBlockBreakEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

import java.util.function.Consumer;

public class BirdCageBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<BirdCageBlock> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
                    com.mojang.serialization.Codec.STRING.xmap(BirdCageVariant::valueOf, BirdCageVariant::name)
                            .fieldOf("variant").forGetter(block -> block.variant),
                    propertiesCodec()).apply(instance, BirdCageBlock::new));

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }


    private final BirdCageVariant variant;

    public BirdCageBlock(BirdCageVariant variant, BlockBehaviour.Properties properties) {
        super(properties);
        this.variant = variant;
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(PlaceableBlockBreakEffects.birdCage());
    }

    public BirdCageVariant variant() {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BirdCageBlockEntity(pos, state);
    }
}
