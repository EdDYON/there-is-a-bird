package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.content.bath.BirdBathBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

/** Bounded searches of ready chunks only. An anchor is a real block and never an airborne sleep flag. */
final class HummingbirdSites {
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> PERCHES =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    new net.minecraft.resources.ResourceLocation("guaniao", "hummingbird_perches"));
    record Perch(BlockPos block, Vec3 feet, Vec3 facing, boolean delicate) { }
    private HummingbirdSites() { }
    @Nullable static BirdBathBlockEntity bath(ServerLevel level, Vec3 center, boolean sugarOnly) {
        BirdBathBlockEntity found = null;
        double closest = 16 * 16;
        for (int x = (BlockPos.containing(center).getX() - 16) >> 4; x <= (BlockPos.containing(center).getX() + 16) >> 4; x++)
            for (int z = (BlockPos.containing(center).getZ() - 16) >> 4; z <= (BlockPos.containing(center).getZ() + 16) >> 4; z++) {
                var chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (var blockEntity : chunk.getBlockEntities().values()) if (blockEntity instanceof BirdBathBlockEntity candidate
                        && (!sugarOnly || candidate.hasUsableSugarWater())) {
                    double distance = Vec3.atCenterOf(candidate.getBlockPos()).distanceToSqr(center);
                    if (distance < closest) { closest = distance; found = candidate; }
                }
            }
        return found;
    }
    @Nullable static BirdBathBlockEntity bathAt(ServerLevel level, @Nullable BlockPos pos) {
        if (pos == null) return null;
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockEntity(pos) instanceof BirdBathBlockEntity bath ? bath : null;
    }
    @Nullable static Perch perch(ServerLevel level, HummingbirdEntity bird, Vec3 center, boolean night) {
        if (night) return treePerch(level, bird, center);
        Perch best = null;
        double bestScore = Double.MAX_VALUE;
        // Repeated bounded samples find tree branches without a per-bird cubic block scan.
        for (int i = 0; i < 64; i++) {
            BlockPos at = BlockPos.containing(center).offset(bird.getRandom().nextInt(17) - 8,
                    bird.getRandom().nextInt(9) - 4, bird.getRandom().nextInt(17) - 8);
            Perch candidate = at(level, bird, at, night);
            if (candidate == null) continue;
            var state = FlowerHabitatIndex.readyState(level, at);
            boolean preferred = state.is(PERCHES) || state.is(BlockTags.FENCES) || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
                    || state.getBlock() instanceof LanternBlock
                    || net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("bird_stand");
            double score = bird.position().distanceToSqr(candidate.feet()) + (preferred ? 0 : 40);
            if (score < bestScore) { bestScore = score; best = candidate; }
        }
        // Include the support directly below: commands should not require a random lucky sample.
        BlockPos base = bird.blockPosition();
        for (int i = 0; i < 10; i++) {
            Perch candidate = at(level, bird, base.below(i), night);
            if (candidate != null) {
                var support = FlowerHabitatIndex.readyState(level, candidate.block());
                boolean preferred = support.is(PERCHES) || support.is(BlockTags.FENCES) || support.is(BlockTags.LOGS)
                        || support.is(BlockTags.LEAVES) || support.getBlock() instanceof LanternBlock
                        || net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(support.getBlock()).getPath().contains("bird_stand");
                double score = bird.position().distanceToSqr(candidate.feet()) + (preferred ? 0 : 40);
                if (score < bestScore) { best = candidate; bestScore = score; }
            }
        }
        return best;
    }
    @Nullable static Perch treePerch(ServerLevel level, HummingbirdEntity bird, Vec3 center) {
        return treePerch(level, bird, center, null);
    }
    @Nullable static Perch treePerch(ServerLevel level, HummingbirdEntity bird, Vec3 center, @Nullable BlockPos avoid) {
        Perch best = null;
        double bestScore = Double.MAX_VALUE;
        BlockPos origin = BlockPos.containing(center);
        // Deterministic columns cover crowns well above the old random +/-4 vertical range.
        // Rotate both parities on the negative-cache period so even a one-block crown cannot
        // be missed forever by a stationary bird. Each pass uses at most 81 * 21 probes.
        int phase = (int)(level.getGameTime() / 60) & 3;
        for (int x = -8 + (phase & 1); x <= 8; x += 2)
            for (int z = -8 + ((phase >> 1) & 1); z <= 8; z += 2) {
            for (int y = 12; y >= -8; y--) {
                BlockPos pos = origin.offset(x, y, z);
                if (pos.equals(avoid)) continue;
                var support = FlowerHabitatIndex.readyState(level, pos);
                if (support == null || !(support.is(BlockTags.LEAVES) || support.is(BlockTags.LOGS))) continue;
                Perch candidate = at(level, bird, pos, true);
                if (candidate == null) continue;
                double score = bird.position().distanceToSqr(candidate.feet());
                if (score < bestScore) { best = candidate; bestScore = score; }
                break;
            }
        }
        return best;
    }
    static boolean isTreeSupport(ServerLevel level, BlockPos pos) {
        var state = FlowerHabitatIndex.readyState(level, pos);
        if (state == null) return false;
        if (state.is(BlockTags.LEAVES)) {
            // Vanilla maintains this distance along connected leaves to a tagged trunk.
            if (state.hasProperty(LeavesBlock.DISTANCE) && state.getValue(LeavesBlock.DISTANCE) < 7) return true;
            for (Direction side : Direction.values()) {
                var neighbor = FlowerHabitatIndex.readyState(level, pos.relative(side));
                if (neighbor != null && neighbor.is(BlockTags.LOGS)) return true;
            }
            return false;
        }
        if (state.is(BlockTags.LOGS)) {
            // A lone decorative log is not a tree. Branches need a nearby leaf crown.
            for (BlockPos probe : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
                var nearby = FlowerHabitatIndex.readyState(level, probe);
                if (nearby != null && nearby.is(BlockTags.LEAVES)) return true;
            }
            return false;
        }
        if (state.getBlock() instanceof VineBlock) {
            for (Direction side : Direction.Plane.HORIZONTAL)
                if (state.getValue(VineBlock.getPropertyForFace(side)) && supportedVine(level, pos, side, true)) return true;
        }
        return false;
    }
    @Nullable static Perch at(ServerLevel level, HummingbirdEntity bird, BlockPos pos, boolean night) {
        for (int x = (pos.getX() - 1) >> 4; x <= (pos.getX() + 1) >> 4; x++)
            for (int z = (pos.getZ() - 1) >> 4; z <= (pos.getZ() + 1) >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return null;
        var state = FlowerHabitatIndex.readyState(level, pos);
        if (state == null || !state.getFluidState().isEmpty()) return null;
        if (FlowerHabitatIndex.isSupportedFlower(state.getBlock()) && state.getBlock() instanceof DoublePlantBlock
                && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER) {
            pos = pos.above(); state = FlowerHabitatIndex.readyState(level, pos);
            if (state == null || !(state.getBlock() instanceof DoublePlantBlock)
                    || state.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.UPPER) return null;
        }
        var shape = state.getCollisionShape(level, pos);
        if ((FlowerHabitatIndex.isSupportedFlower(state.getBlock()) || state.getBlock() instanceof LanternBlock)
                && !state.canSurvive(level, pos)) return null;
        boolean delicate = shape.isEmpty();
        if (delicate && !(state.getBlock() instanceof VineBlock || (!night && FlowerHabitatIndex.isSupportedFlower(state.getBlock())))) return null;
        if (delicate && night && !(state.getBlock() instanceof VineBlock)) return null;
        double top = delicate ? (state.getBlock() instanceof VineBlock ? 0.65 : 0.52)
                : shape.max(net.minecraft.core.Direction.Axis.Y);
        Vec3 feet = new Vec3(pos.getX() + 0.5, pos.getY() + top + 0.015, pos.getZ() + 0.5);
        if (FlowerHabitatIndex.isSupportedFlower(state.getBlock())) feet = feet.add(state.getOffset(level, pos));
        Vec3 facing = feet.add(0, 0, 1);
        if (state.getBlock() instanceof VineBlock) {
            net.minecraft.core.Direction attached = null;
            for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                if (state.getValue(VineBlock.getPropertyForFace(side)) && supportedVine(level, pos, side, night)) { attached = side; break; }
            }
            if (attached == null) return null; // ceiling-only vines do not support this upright perch pose
            feet = new Vec3(pos.getX() + .5 + attached.getStepX() * .33, pos.getY() + .42,
                    pos.getZ() + .5 + attached.getStepZ() * .33);
            facing = feet.add(attached.getStepX(), 0, attached.getStepZ());
        }
        var box = bird.getBoundingBox().move(feet.subtract(bird.position()));
        BlockPos upper = BlockPos.containing(feet.add(0, bird.getBbHeight(), 0));
        for (int x = (pos.getX() - 1) >> 4; x <= (pos.getX() + 1) >> 4; x++)
            for (int z = (pos.getZ() - 1) >> 4; z <= (pos.getZ() + 1) >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return null;
        if (FlowerHabitatIndex.readyState(level, upper) == null || !level.noCollision(bird, box)) return null;
        // Reject obstructed interior logs before the bounded crown-association check.
        if (night && !isTreeSupport(level, pos)) return null;
        return new Perch(pos.immutable(), feet, facing, delicate);
    }
    static boolean valid(ServerLevel level, HummingbirdEntity bird, Perch perch, boolean night) {
        Perch actual = at(level, bird, perch.block(), night);
        return actual != null && actual.feet().distanceToSqr(perch.feet()) < 0.0001;
    }
    private static boolean supportedVine(ServerLevel level, BlockPos pos, net.minecraft.core.Direction side, boolean treeOnly) {
        for (int height = 0; height < 8; height++) {
            BlockPos probe = pos.above(height);
            var vine = FlowerHabitatIndex.readyState(level, probe);
            if (vine == null || !(vine.getBlock() instanceof VineBlock) || !vine.getValue(VineBlock.getPropertyForFace(side))) return false;
            if (VineBlock.isAcceptableNeighbour(level, probe.relative(side), side))
                return !treeOnly || isTreeSupport(level, probe.relative(side));
        }
        return false;
    }
    static Vec3 bathHover(BirdBathBlockEntity bath, Vec3 from) {
        Vec3 center = Vec3.atCenterOf(bath.getBlockPos()).add(0, 1.02, 0);
        Vec3 away = new Vec3(from.x - center.x, 0, from.z - center.z);
        if (away.lengthSqr() < 0.01) away = new Vec3(0, 0, 1);
        // Outside the basin collision footprint; the beak points inward towards the rim.
        return center.add(away.normalize().scale(0.65));
    }
}
