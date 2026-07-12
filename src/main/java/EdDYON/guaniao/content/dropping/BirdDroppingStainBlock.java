package EdDYON.guaniao.content.dropping;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BirdDroppingStainBlock extends Block implements LiquidBlockContainer {
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);

    public BirdDroppingStainBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (isTouchingWater(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.BRUSH)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            cleanWithBrush(serverLevel, pos, player, hand, stack);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isRainingAt(pos.above()) && random.nextFloat() < 0.25F) {
            washAway(level, pos, ParticleTypes.SPLASH, SoundEvents.SLIME_BLOCK_BREAK);
        }
    }

    @Override
    public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return fluid == Fluids.WATER;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (fluidState.getType() != Fluids.WATER) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel) {
            spawnParticles(serverLevel, pos, ParticleTypes.SPLASH, 5);
            serverLevel.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, 0.45F, 1.2F);
        }
        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
        return true;
    }

    private static void cleanWithBrush(ServerLevel level, BlockPos pos, Player player, InteractionHand hand, ItemStack brush) {
        Block.popResource(level, pos, BirdDroppingUtil.randomDroppingStack(level.random));
        level.removeBlock(pos, false);
        spawnParticles(level, pos, ParticleTypes.POOF, 8);
        level.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, 0.65F, 0.85F + level.random.nextFloat() * 0.25F);
        if (!player.getAbilities().instabuild) {
            brush.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
        }
    }

    private static void washAway(ServerLevel level, BlockPos pos, ParticleOptions particles, net.minecraft.sounds.SoundEvent sound) {
        level.removeBlock(pos, false);
        spawnParticles(level, pos, particles, 5);
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.35F, 1.1F + level.random.nextFloat() * 0.25F);
    }

    private static void spawnParticles(ServerLevel level, BlockPos pos, ParticleOptions particles, int count) {
        level.sendParticles(particles, pos.getX() + 0.5D, pos.getY() + 0.08D, pos.getZ() + 0.5D, count, 0.25D, 0.03D, 0.25D, 0.01D);
    }

    private static boolean isTouchingWater(LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getFluidState(pos.relative(direction)).is(Fluids.WATER)) {
                return true;
            }
        }
        return false;
    }
}
