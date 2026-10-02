package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.content.bath.BirdBathBlockEntity;
import EdDYON.guaniao.content.bath.BirdBathContentType;
import EdDYON.guaniao.registry.GuaniaoBlocks;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("guaniao_hummingbird_qa")
@PrefixGameTestTemplate(false)
public final class HummingbirdSiteAndBathGameTests {
    private HummingbirdSiteAndBathGameTests() { }

    private static void check(boolean value, String message) {
        if (!value) throw new GameTestAssertException(message);
    }

    @GameTest(template = "garden", batch = "hummingbird_real_perch_geometry", timeoutTicks = 70)
    public static void vineFacesAndFlowerCrownsAreRealSupportedPerches(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(1000);
        HummingbirdEntity bird = (HummingbirdEntity) f.bird(Vec3.atCenterOf(f.origin).add(0, 5, 0));
        Map<BlockPos, Direction> vines = new LinkedHashMap<>();
        int index = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos pos = f.origin.offset((index % 2) * 5, 1, (index / 2) * 5);
            index++;
            f.set(pos.relative(side), Blocks.STONE.defaultBlockState());
            f.set(pos, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(side), true));
            vines.put(pos, side);
        }
        BlockPos ceiling = f.origin.offset(10, 1, 0), tall = f.origin.offset(10, 0, 5), small = f.origin.offset(10, 0, 8);
        f.set(ceiling.above(), Blocks.STONE.defaultBlockState());
        f.set(ceiling, Blocks.VINE.defaultBlockState().setValue(VineBlock.UP, true));
        f.set(tall.below(), Blocks.DIRT.defaultBlockState());
        f.previousBlocks.putIfAbsent(tall, f.level.getBlockState(tall));
        f.previousBlocks.putIfAbsent(tall.above(), f.level.getBlockState(tall.above()));
        DoublePlantBlock.placeAt(f.level, Blocks.ROSE_BUSH.defaultBlockState(), tall, 3);
        f.flower(small, Blocks.POPPY);

        f.run(40, tick -> {
            if (tick == 10) {
                for (var entry : vines.entrySet()) {
                    BlockPos pos = entry.getKey(); Direction side = entry.getValue();
                    check(VineBlock.isAcceptableNeighbour(f.level, pos.relative(side), side), "vine has its real neighboring support");
                    HummingbirdSites.Perch perch = HummingbirdSites.at(f.level, bird, pos, false);
                    check(perch != null && perch.delicate() && perch.block().equals(pos), "attached vine can support an explicit daytime perch");
                    check(HummingbirdSites.at(f.level, bird, pos, true) == null,
                            "a stone-wall vine is physically supported but is not a tree sleeping anchor");
                    Vec3 displacement = perch.feet().subtract(Vec3.atCenterOf(pos));
                    check(displacement.x * side.getStepX() + displacement.z * side.getStepZ() > .25,
                            "vine perch is next to its attached face, not floating at the block center");
                    Vec3 look = perch.facing().subtract(perch.feet());
                    check(look.x * side.getStepX() + look.z * side.getStepZ() > .9,
                            "vine perch faces its physical support");
                    check(f.level.noCollision(bird, bird.getBoundingBox().move(perch.feet().subtract(bird.position()))),
                            "supported vine position does not put the bird through the wall");
                }
                check(HummingbirdSites.at(f.level, bird, ceiling, false) == null
                                && HummingbirdSites.at(f.level, bird, ceiling, true) == null,
                        "ceiling-only vines cannot use an upright wall-perch pose");
                HummingbirdSites.Perch lower = HummingbirdSites.at(f.level, bird, tall, false);
                HummingbirdSites.Perch upper = HummingbirdSites.at(f.level, bird, tall.above(), false);
                check(lower != null && upper != null && lower.block().equals(tall.above())
                                && lower.feet().distanceToSqr(upper.feet()) < .0001 && lower.feet().y > tall.getY() + 1.4,
                        "both halves of a tall flower resolve to the upper flower crown");
                check(f.level.noCollision(bird, bird.getBoundingBox().move(lower.feet().subtract(bird.position()))),
                        "flower crown leaves actual room for the perched bird");
                check(HummingbirdSites.at(f.level, bird, tall, true) == null
                                && HummingbirdSites.at(f.level, bird, small, true) == null,
                        "night sleep does not use either tall or small flowers");
                var first = vines.entrySet().iterator().next();
                BlockPos pos = first.getKey(); Direction side = first.getValue();
                var oldFace = f.level.getBlockState(pos);
                f.set(pos.relative(side), Blocks.AIR.defaultBlockState());
                // Reproduce a stale face before the neighbor update has repaired the vine state.
                f.level.setBlock(pos, oldFace, 18);
                check(HummingbirdSites.at(f.level, bird, pos, true) == null,
                        "a stale face bit cannot make a detached, unsupported vine into an anchor");
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_real_sugar_transaction", timeoutTicks = 1100)
    public static void realSugarVisitPublishesOnlyAfterConsumptionAndCancelsCleanly(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(1000);
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++)
            f.set(f.origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        f.set(f.origin, GuaniaoBlocks.BIRD_BATH.get().defaultBlockState());
        check(f.level.getBlockEntity(f.origin) instanceof BirdBathBlockEntity, "placed bath has its real server block entity");
        BirdBathBlockEntity bath = (BirdBathBlockEntity) f.level.getBlockEntity(f.origin);
        bath.setContent(BirdBathContentType.WATER, 3);
        var gardener = f.player("HummerBathGardener", Vec3.atCenterOf(f.origin).add(2, 0, 0));
        gardener.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SUGAR, 2));
        var hit = new BlockHitResult(Vec3.atCenterOf(f.origin).add(0, .5, 0), Direction.UP, f.origin, false);
        var bathState = f.level.getBlockState(f.origin);
        check(bathState.getBlock().use(bathState, f.level, f.origin, gardener, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "using vanilla sugar invokes the actual bird-bath interaction");
        check(bath.hasUsableSugarWater() && bath.getContentLevel() == 3 && gardener.getMainHandItem().getCount() == 1,
                "one vanilla sugar sweetens the existing three water portions");
        bathState.getBlock().use(bathState, f.level, f.origin, gardener, InteractionHand.MAIN_HAND, hit);
        check(gardener.getMainHandItem().getCount() == 1 && bath.getContentLevel() == 3,
                "already sweetened water does not consume another sugar");
        gardener.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        HummingbirdEntity[] visitor = {f.actualBird(Vec3.atCenterOf(f.origin).add(3, 2, 0))};
        int[] phase = {0}, loopTicks = {0}, phaseAt = {0};
        boolean[] consumed = {false}, destroyed = {false};
        f.run(1000, tick -> {
            HummingbirdEntity bird = visitor[0];
            int sources = HummingbirdPollinationService.activeSourcesNear(f.level, bird.position(), 1);
            CompoundTag saved = new CompoundTag(); bird.addAdditionalSaveData(saved);
            check(saved.getInt("FlowerVisits") == 0 && bird.getHeldGardenItem().isEmpty(),
                    "sugar water never fabricates completed flower visits or carried flower rewards");
            if (phase[0] == 0) {
                check(bath.getContentLevel() == 3 && sources == 0, "approach and unfinished sugar sipping grant no free activity");
                if (bird.activity() == HummingbirdEntity.Activity.NECTAR_LOOP && ++loopTicks[0] == 15) {
                    bath.clearContent(); phase[0] = 1; phaseAt[0] = tick;
                }
            } else if (phase[0] == 1) {
                check(bath.isEmpty() && sources == 0, "withdrawing sugar mid-visit cancels activity without a reward");
                if (tick - phaseAt[0] >= 30) {
                    bath.setContent(BirdBathContentType.SUGAR_WATER, 3); phase[0] = 2;
                }
            } else if (phase[0] == 2) {
                if (bath.getContentLevel() == 3) check(sources == 0, "a retry still waits for a completed consumption");
                else {
                    check(bath.getContentLevel() == 2 && sources == 1,
                            "one real completed visit consumes exactly one serving before publishing one source");
                    consumed[0] = true;
                    bird.discard();
                    check(HummingbirdPollinationService.activeSourcesNear(f.level, bird.position(), 8) == 0,
                            "removing the completed visitor revokes its grace period immediately");
                    bath.setContent(BirdBathContentType.SUGAR_WATER, 3);
                    visitor[0] = f.actualBird(Vec3.atCenterOf(f.origin).add(-3, 2, 0));
                    loopTicks[0] = 0; phase[0] = 3;
                }
            } else if (phase[0] == 3) {
                check(bath.getContentLevel() == 3 && sources == 0, "second visitor has not earned activity while drinking");
                if (bird.activity() == HummingbirdEntity.Activity.NECTAR_LOOP && ++loopTicks[0] == 15) {
                    f.set(f.origin, Blocks.AIR.defaultBlockState());
                    destroyed[0] = true; phase[0] = 4; phaseAt[0] = tick;
                }
            } else {
                check(HummingbirdSites.bathAt(f.level, f.origin) == null && sources == 0,
                        "destroyed bath cannot finish a pending visit or leave agricultural activity");
            }
            if (tick == 1000) check(consumed[0] && destroyed[0] && phase[0] == 4 && tick - phaseAt[0] >= 30,
                    "real AI completes the withdrawal, successful retry and destroyed-bath scenarios within the test; phase="
                            + phase[0] + ", activity=" + bird.activity() + ", position=" + bird.position()
                            + ", bath=" + bath.getContentLevel() + ", sources=" + sources);
        });
    }
}
