package EdDYON.guaniao.content.camera.frame;

import EdDYON.guaniao.registry.GuaniaoBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

/** The workshop retains one private work area per visiting player. */
public final class FrameWorkbenchBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Table, recessed work surface, cabinet and legs; texture seam offsets do not
    // need sub-pixel collision boxes. These shared shapes are built only once.
    private static final VoxelShape BODY = Shapes.or(
            Block.box(0, 12, 0, 16, 15, 16),
            Block.box(0, 15, 0, 3, 16, 16), Block.box(13, 15, 0, 16, 16, 16),
            Block.box(3, 15, 0, 13, 16, 3), Block.box(3, 15, 13, 13, 16, 16),
            Block.box(1, 8, 1, 15, 12, 15), Block.box(2, 2, 2, 14, 8, 14),
            Block.box(1, 0, 1, 15, 2, 15),
            Block.box(0, 0, 0, 3, 12, 3), Block.box(0, 0, 13, 3, 12, 16),
            Block.box(13, 0, 0, 16, 12, 3), Block.box(13, 0, 13, 16, 12, 16));
    private static final VoxelShape NORTH_SOUTH_SHAPE = Shapes.or(BODY,
            Block.box(1, 2, 1, 2, 8, 15), Block.box(14, 2, 1, 15, 8, 15));
    private static final VoxelShape EAST_WEST_SHAPE = Shapes.or(BODY,
            Block.box(1, 2, 1, 15, 8, 2), Block.box(1, 2, 14, 15, 8, 15));

    public FrameWorkbenchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? EAST_WEST_SHAPE : NORTH_SOUTH_SHAPE;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof FrameWorkbenchBlockEntity bench) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, ignored) -> new FrameWorkbenchMenu(id, inventory, bench),
                    Component.translatable("gui.guaniao.frame_workbench.title")), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FrameWorkbenchBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,
                GuaniaoBlockEntityTypes.FRAME_WORKBENCH.get(), FrameWorkbenchBlockEntity::serverTick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof FrameWorkbenchBlockEntity bench) {
            bench.dismantle();
        }
        super.onRemove(state, level, pos, replacement, movedByPiston);
    }
}
