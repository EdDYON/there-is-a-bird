package EdDYON.guaniao.content.bird.command;

import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
import EdDYON.guaniao.content.bird.kestrel.KestrelEntity;
import EdDYON.guaniao.content.bird.umbrellacockatoo.UmbrellaCockatooEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Owner-only placement through vanilla block use; neither the client nor the item chooses bird state. */
public final class BirdHeadPlacement {
    private BirdHeadPlacement() { }

    public static boolean hasHeadBird(Player owner) {
        return headBird(owner) != null;
    }

    private static TamableAnimal headBird(Player owner) {
        for (var passenger : owner.getPassengers()) {
            if ((passenger instanceof HummingbirdEntity || passenger instanceof KestrelEntity
                    || passenger instanceof UmbrellaCockatooEntity)
                    && passenger instanceof TamableAnimal bird && bird.isOwnedBy(owner)
                    && bird.isAlive() && bird.getVehicle() == owner) return bird;
        }
        return null;
    }

    public static boolean isPlacementGesture(Player owner, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND && owner.isAlive() && !owner.isSpectator()
                && owner.isShiftKeyDown() && owner.getMainHandItem().isEmpty()
                && owner.getOffhandItem().isEmpty() && hasHeadBird(owner);
    }

    public static boolean tryPlace(Player owner, InteractionHand hand, BlockHitResult hit) {
        if (!(owner.level() instanceof ServerLevel level) || !isPlacementGesture(owner, hand)
                || hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) return false;
        TamableAnimal bird = headBird(owner);
        BlockPos support = hit.getBlockPos();
        Vec3 point = hit.getLocation();
        if (!Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z)
                || level.isOutsideBuildHeight(support) || !level.hasChunkAt(support)
                || !level.getWorldBorder().isWithinBounds(support)
                || !level.mayInteract(owner, support) || !level.mayInteract(owner, support.above())) return false;
        double reach = owner.getBlockReach();
        if (reach <= 0 || owner.getEyePosition().distanceToSqr(point) > (reach + .5) * (reach + .5)
                || point.x < support.getX() || point.x > support.getX() + 1
                || point.z < support.getZ() || point.z > support.getZ() + 1
                || point.y < support.getY() || point.y > support.getY() + 1.5) return false;

        // Check the actual surface and line of sight rather than trusting the packet's hit coordinates.
        Vec3 eye = owner.getEyePosition();
        if (!level.hasChunksAt(BlockPos.containing(eye), support)) return false;
        Vec3 end = point.add(point.subtract(eye).normalize().scale(.02));
        BlockHitResult visible = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        if (visible.getType() != HitResult.Type.BLOCK || !visible.getBlockPos().equals(support)
                || visible.getDirection() != Direction.UP || visible.getLocation().distanceToSqr(point) > .01) return false;
        BlockState state = level.getBlockState(support);
        if (!state.getFluidState().isEmpty() || unsafe(state)) return false;
        var shape = state.getCollisionShape(level, support);
        if (shape.isEmpty()) return false;
        double top = shape.max(Direction.Axis.Y);
        if (Math.abs(point.y - (support.getY() + top)) > .1
                || shape.toAabbs().stream().noneMatch(box -> box.minX <= .5 && box.maxX >= .5
                && box.minZ <= .5 && box.maxZ >= .5 && Math.abs(box.maxY - top) < .001)) return false;
        Vec3 feet = new Vec3(support.getX() + .5, support.getY() + top + .015, support.getZ() + .5);
        AABB box = bird.getBoundingBox().move(feet.subtract(bird.position()));
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        if (level.isOutsideBuildHeight(max) || !level.hasChunksAt(min, max)
                || !level.getWorldBorder().isWithinBounds(box) || !level.noCollision(bird, box)) return false;
        for (BlockPos occupied : BlockPos.betweenClosed(min, max)) {
            BlockState at = level.getBlockState(occupied);
            if (!at.getFluidState().isEmpty() || unsafe(at)) return false;
        }
        if (bird instanceof HummingbirdEntity hummingbird) return hummingbird.placeFromOwnerHead(owner, support, feet);
        if (bird instanceof UmbrellaCockatooEntity cockatoo) return cockatoo.placeFromOwnerHead(owner, support, feet);
        return bird instanceof KestrelEntity kestrel && kestrel.placeFromOwnerHead(owner, support, feet);
    }

    private static boolean unsafe(BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
                || (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT));
    }
}
