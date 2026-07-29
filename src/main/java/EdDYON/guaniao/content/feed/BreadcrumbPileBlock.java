package EdDYON.guaniao.content.feed;

import EdDYON.guaniao.content.bird.sparrow.BreadcrumbSearchCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BreadcrumbPileBlock extends Block {
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 4);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 5);
    public static final IntegerProperty BITES = IntegerProperty.create("bites", 1, 7);
    private static final int TICK_INTERVAL = 600;
    private static final int MAX_AGE = 5;
    private static final VoxelShape[] SHAPES = new VoxelShape[]{
            Shapes.empty(),
            Block.box(1.0, 0.0, 1.0, 15.0, 1.0, 15.0),
            Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0),
            Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
            Block.box(1.0, 0.0, 1.0, 15.0, 4.0, 15.0)
    };

    public BreadcrumbPileBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LAYERS, 4)
                .setValue(AGE, 0)
                .setValue(BITES, 7));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return context.getItemInHand().is(GuaniaoItems.BREADCRUMBS.get()) && state.getValue(BITES) < 7
                || super.canBeReplaced(state, context);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.is(this)) {
            return this.stateForBites(state, Math.min(7, state.getValue(BITES) + 7), 0);
        }
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        if (!fluidState.isEmpty()) {
            return null;
        }
        return this.defaultBlockState().setValue(AGE, 0).setValue(BITES, 7).setValue(LAYERS, 4);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            BreadcrumbSearchCache.register(serverLevel, pos);
            level.scheduleTick(pos, this, TICK_INTERVAL);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel && !newState.is(this)) {
            BreadcrumbSearchCache.remove(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        return below.isFaceSturdy(level, belowPos, Direction.UP) || below.is(net.minecraft.world.level.block.Blocks.FARMLAND);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BreadcrumbSearchCache.register(level, pos);
        if (!state.canSurvive(level, pos) || !level.getFluidState(pos).isEmpty()) {
            level.removeBlock(pos, false);
            return;
        }

        int ageIncrease = level.isRainingAt(pos.above()) ? 2 : 1;
        int newAge = state.getValue(AGE) + ageIncrease;
        if (newAge > MAX_AGE) {
            level.removeBlock(pos, false);
            return;
        }

        level.setBlock(pos, state.setValue(AGE, newAge), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, this, TICK_INTERVAL);
    }

    public boolean consumeOneServing(Level level, BlockPos pos, BlockState state) {
        BlockState currentState = level.getBlockState(pos);
        if (!currentState.is(this)) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel) {
            BreadcrumbSearchCache.register(serverLevel, pos);
        }
        int bites = currentState.getValue(BITES);
        if (bites > 1) {
            level.setBlock(pos, this.stateForBites(currentState, bites - 1, 0), Block.UPDATE_CLIENTS);
            if (!level.isClientSide) {
                level.scheduleTick(pos, this, TICK_INTERVAL);
            }
        } else {
            level.removeBlock(pos, false);
        }
        return true;
    }

    private BlockState stateForBites(BlockState state, int bites, int age) {
        return state.setValue(BITES, bites).setValue(LAYERS, this.layersForBites(bites)).setValue(AGE, age);
    }

    private int layersForBites(int bites) {
        if (bites >= 6) {
            return 4;
        }
        if (bites >= 4) {
            return 3;
        }
        if (bites >= 2) {
            return 2;
        }
        return 1;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS, AGE, BITES);
    }
}
