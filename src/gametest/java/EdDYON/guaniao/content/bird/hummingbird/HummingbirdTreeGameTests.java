package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.config.BirdConfigData;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real tree geometry and actual flight/anchor transitions; no fabricated movement. */
@GameTestHolder("guaniao_hummingbird_tree_qa")
@PrefixGameTestTemplate(false)
public final class HummingbirdTreeGameTests {
    private HummingbirdTreeGameTests() { }

    private static void check(boolean value, String message) {
        if (!value) throw new GameTestAssertException(message);
    }

    private static BlockPos tree(HummingbirdEcologyGameTests.Fixture f, BlockPos base, int height, int crownRadius) {
        f.set(base.below(), Blocks.DIRT.defaultBlockState());
        for (int y = 0; y < height; y++) f.set(base.above(y), Blocks.OAK_LOG.defaultBlockState());
        BlockPos crown = base.above(height);
        for (int x = -crownRadius; x <= crownRadius; x++) for (int z = -crownRadius; z <= crownRadius; z++)
            f.set(crown.offset(x, 0, z), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        return crown;
    }

    private static CompoundTag savedAt(HummingbirdEntity bird, HummingbirdSites.Perch perch, boolean sleeping) {
        CompoundTag tag = new CompoundTag();
        bird.addAdditionalSaveData(tag);
        tag.putLong("Perch", perch.block().asLong());
        tag.putBoolean("HummingbirdSleeping", sleeping);
        tag.putInt("HummingbirdCommand", BirdCommandMode.STAY.ordinal());
        return tag;
    }

    private static String flightSnapshot(HummingbirdEntity bird) {
        CompoundTag tag = new CompoundTag(); bird.addAdditionalSaveData(tag);
        try {
            var perchTicks = HummingbirdGardenBehavior.class.getDeclaredField("perchTicks");
            var freeFlightTicks = HummingbirdGardenBehavior.class.getDeclaredField("freeFlightTicks");
            perchTicks.setAccessible(true); freeFlightTicks.setAccessible(true);
            return "activity=" + bird.activity() + ", position=" + bird.position()
                    + ", anchor=" + bird.behavior.hasAnchor() + ", savedPerch="
                    + (tag.contains("Perch") ? BlockPos.of(tag.getLong("Perch")) : "none")
                    + ", perchTicks=" + perchTicks.getInt(bird.behavior)
                    + ", freeFlightTicks=" + freeFlightTicks.getInt(bird.behavior);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot inspect the actual priority-test flight state", failure);
        }
    }

    private static String supportSnapshot(HummingbirdEntity bird, HummingbirdSites.Perch support) {
        var level = (net.minecraft.server.level.ServerLevel) bird.level();
        return flightSnapshot(bird) + ", support=" + support.block()
                + ", supportState=" + FlowerHabitatIndex.readyState(level, support.block())
                + ", treeSupport=" + HummingbirdSites.isTreeSupport(level, support.block())
                + ", validDay=" + HummingbirdSites.valid(level, bird, support, false)
                + ", validNight=" + HummingbirdSites.valid(level, bird, support, true)
                + ", entityTicks=" + bird.tickCount + ", noAi=" + bird.isNoAi()
                + ", gameTime=" + level.getGameTime() + ", dayTime=" + level.getDayTime()
                + ", isNight=" + level.isNight();
    }

    @GameTest(template = "garden", batch = "hummingbird_tree_sites", timeoutTicks = 70)
    public static void highOffGridCrownsAndStrictNightSupports(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(1000);
        BlockPos crown = tree(f, f.origin.offset(2, 0, 2), 8, 1);
        HummingbirdEntity bird = (HummingbirdEntity)f.bird(Vec3.atCenterOf(f.origin).add(0, 1, 0));
        BlockPos stone = f.origin.offset(-6, 0, -6), fence = stone.offset(3, 0, 0), log = stone.offset(6, 0, 0);
        BlockPos leaf = stone.offset(9, 0, 0), vine = stone.offset(0, 2, 0);
        f.set(stone, Blocks.STONE.defaultBlockState());
        f.set(fence, Blocks.OAK_FENCE.defaultBlockState());
        f.set(log, Blocks.OAK_LOG.defaultBlockState());
        f.set(leaf, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        f.set(vine.relative(Direction.WEST), Blocks.STONE.defaultBlockState());
        f.set(vine, Blocks.VINE.defaultBlockState().setValue(VineBlock.WEST, true));
        f.run(40, tick -> {
            if (tick != 10) return;
            HummingbirdSites.Perch chosen = HummingbirdSites.treePerch(f.level, bird, bird.position());
            check(chosen != null && chosen.block().getY() >= crown.getY()
                    && HummingbirdSites.isTreeSupport(f.level, chosen.block()),
                    "bounded tree search finds an eight-block-high 3x3 crown between the old coarse grid columns");
            check(f.level.noCollision(bird, bird.getBoundingBox().move(chosen.feet().subtract(bird.position()))),
                    "the crown landing has room above the leaves");
            for (BlockPos support : new BlockPos[]{stone, fence, log, leaf, vine})
                check(HummingbirdSites.at(f.level, bird, support, true) == null,
                        "non-tree support cannot be a sleeping anchor: " + support);
            check(HummingbirdSites.at(f.level, bird, stone, false) != null
                    && HummingbirdSites.at(f.level, bird, vine, false) != null,
                    "explicit daytime placement still accepts safe ground and physically attached vines");
            BlockPos distant = f.origin.offset(200000, 0, 200000);
            check(f.level.getChunkSource().getChunkNow(distant.getX() >> 4, distant.getZ() >> 4) == null,
                    "far test chunk starts unloaded");
            check(HummingbirdSites.treePerch(f.level, bird, Vec3.atCenterOf(distant)) == null
                    && f.level.getChunkSource().getChunkNow(distant.getX() >> 4, distant.getZ() >> 4) == null,
                    "tree search does not load a far chunk");
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_tree_free", timeoutTicks = 460)
    public static void freeBirdActuallyFliesToHighCrownAtMinimumScanBudget(GameTestHelper helper) throws Exception {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(6000);
        var configField = BirdConfigManager.class.getDeclaredField("config");
        configField.setAccessible(true);
        BirdConfigData original = (BirdConfigData)configField.get(null), lowBudget = original.copy();
        lowBudget.global.birdScanBudgetPerTick = 1;
        configField.set(null, lowBudget);
        f.cleanups.add(() -> {
            try { configField.set(null, original); }
            catch (IllegalAccessException failure) { throw new AssertionError(failure); }
        });
        BlockPos crown = tree(f, f.origin.offset(2, 0, 2), 8, 1);
        HummingbirdEntity bird = f.actualBird(Vec3.atCenterOf(f.origin).add(-4, 1, 0));
        bird.setBirdCommandMode(BirdCommandMode.FREE);
        Vec3[] previous = {bird.position()};
        int[] treeTicks = {0};
        double[] highest = {bird.getY()};
        f.run(420, tick -> {
            check(bird.position().distanceTo(previous[0]) < .65, "the bird flies continuously instead of teleporting to a crown");
            previous[0] = bird.position();
            highest[0] = Math.max(highest[0], bird.getY());
            check(f.level.noCollision(bird, bird.getBoundingBox()), "flight and landing do not pass through leaves or the trunk");
            if (bird.activity() == HummingbirdEntity.Activity.PERCH) {
                CompoundTag tag = new CompoundTag(); bird.addAdditionalSaveData(tag);
                check(tag.contains("Perch") && HummingbirdSites.isTreeSupport(f.level, BlockPos.of(tag.getLong("Perch"))),
                        "FREE resting is anchored to a real tree, never the closer ground");
                treeTicks[0]++;
            }
            if (tick == 420) check(treeTicks[0] >= 100 && highest[0] > crown.getY() + .8,
                    "at the minimum scan budget actual AI reaches the high crown and spends substantial time perched; ticks="
                            + treeTicks[0] + ", activity=" + bird.activity() + ", position=" + bird.position());
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_tree_night", timeoutTicks = 540)
    public static void nightRejectsLegacyGroundAndFindsThinTreeWhileTreeSavesRestore(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(18000);
        BlockPos support = f.origin;
        f.set(support, Blocks.STONE.defaultBlockState());
        Vec3 feet = Vec3.atBottomCenterOf(support.above()).add(0, .015, 0);
        HummingbirdEntity legacy = f.actualBird(feet);
        HummingbirdSites.Perch ground = HummingbirdSites.at(f.level, legacy, support, false);
        check(ground != null, "the old daytime ground anchor is physically valid");
        legacy.readAdditionalSaveData(savedAt(legacy, ground, true));
        HummingbirdEntity[] restoredTree = {null};
        Vec3[] treeFeet = {null};
        BlockPos[] crown = {null};
        Vec3[] previous = {legacy.position()};
        f.run(500, tick -> {
            check(legacy.position().distanceTo(previous[0]) < .65, "legacy perch migration must use real flight");
            previous[0] = legacy.position();
            if (tick <= 80) check(!legacy.isSleeping() && !legacy.behavior.hasAnchor() && legacy.isAirborne(),
                    "a loaded old stone sleeping anchor is rejected; a treeless bird hovers rather than sleeping on the ground");
            if (tick == 100) {
                // The one-block crown is on an odd/odd column, so fixed even samples would never find it.
                crown[0] = tree(f, f.origin.offset(1, 0, 1), 6, 0);
                HummingbirdSites.Perch perch = HummingbirdSites.at(f.level, legacy, crown[0], true);
                check(perch != null, "a real trunk-linked thin crown is a safe sleeping support");
                treeFeet[0] = perch.feet();
                restoredTree[0] = f.actualBird(perch.feet());
                restoredTree[0].readAdditionalSaveData(savedAt(restoredTree[0], perch, true));
            }
            if (tick == 110) {
                check(restoredTree[0].isSleeping() && restoredTree[0].behavior.hasAnchor()
                                && restoredTree[0].position().distanceToSqr(treeFeet[0]) < .0001,
                        "a valid old tree sleeping anchor restores without displacement");
                restoredTree[0].discard();
            }
            if (legacy.isSleeping()) {
                CompoundTag tag = new CompoundTag(); legacy.addAdditionalSaveData(tag);
                check(HummingbirdSites.isTreeSupport(f.level, BlockPos.of(tag.getLong("Perch"))),
                        "sleeping after migration always has a trunk-linked tree anchor");
            }
            if (tick == 500) check(legacy.isSleeping() && legacy.behavior.hasAnchor()
                            && legacy.getY() > crown[0].getY() + .9,
                    "rotating bounded search and real flight eventually reach the odd/odd thin tree, then sleep; activity="
                            + legacy.activity() + ", position=" + legacy.position());
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_tree_placement", timeoutTicks = 450)
    public static void daytimeHeadPlacementKeepsStayButGroundCannotSleepAtNight(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(6000);
        BlockPos support = f.origin;
        for (int x = -9; x <= 9; x++) for (int z = -9; z <= 9; z++)
            f.set(support.offset(x, 0, z), Blocks.STONE.defaultBlockState());
        Vec3 feet = Vec3.atBottomCenterOf(support.above()).add(0, .015, 0);
        var owner = f.player("HummerTreePlacement", feet.add(3, 0, 0));
        HummingbirdEntity bird = f.actualBird(owner.position().add(0, 2, 0));
        bird.tame(owner); bird.setBirdCommandMode(BirdCommandMode.FOLLOW);
        check(bird.startRiding(owner, true), "the owned bird can be mounted for actual head placement");
        bird.activity(HummingbirdEntity.Activity.HEAD_PERCH);
        check(bird.placeFromOwnerHead(owner, support, feet), "valid daytime ground placement succeeds");
        boolean[] waiting = {false};
        Vec3[] daytimeFeet = {null};
        f.run(420, tick -> {
            check(f.level.noCollision(bird, bird.getBoundingBox()), "STAY day/night transitions remain on collision-safe supports");
            if (tick < 60) check(bird.getBirdCommandMode() == BirdCommandMode.STAY && !bird.isPassenger()
                            && bird.behavior.hasAnchor() && !bird.isAirborne() && bird.position().distanceToSqr(feet) < .0001,
                    "manual ground placement preserves the daytime STAY command and support");
            if (tick == 60) f.day(18000);
            if (tick == 120) check(bird.getBirdCommandMode() == BirdCommandMode.STAY && !bird.isSleeping()
                            && !bird.behavior.hasAnchor() && bird.isAirborne(),
                    "STAY is retained at night, but a ground support is not turned into a sleeping tree");
            if (tick > 240 && tick < 360 && bird.behavior.hasAnchor()) {
                check(bird.getBirdCommandMode() == BirdCommandMode.STAY && !bird.isSleeping()
                                && bird.activity() == HummingbirdEntity.Activity.PERCH && !bird.isWingSoundActive(),
                        "a treeless STAY bird eventually waits awake with folded wings on a real support");
                waiting[0] = true;
            }
            if (tick == 360) {
                CompoundTag tag = new CompoundTag(); bird.addAdditionalSaveData(tag);
                check(waiting[0] && bird.behavior.hasAnchor(),
                        "nighttime STAY finds a safe awake waiting support; waited=" + waiting[0]
                                + ", activity=" + bird.activity() + ", position=" + bird.position()
                                + ", hasAnchor=" + bird.behavior.hasAnchor() + ", nbt=" + tag
                                + ", owner=" + owner.position() + ", ownerBox=" + owner.getBoundingBox()
                                + ", noCollision=" + f.level.noCollision(bird, bird.getBoundingBox()));
                daytimeFeet[0] = bird.position(); f.day(6000);
            }
            if (tick > 370) check(bird.getBirdCommandMode() == BirdCommandMode.STAY
                            && bird.behavior.hasAnchor() && !bird.isSleeping() && !bird.isAirborne()
                            && bird.position().distanceToSqr(daytimeFeet[0]) < .0001,
                    "daytime preserves STAY on its current valid waiting support without inventing a return point");
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_tree_night_search", timeoutTicks = 660)
    public static void treelessNightSearchMovesOffGroundWaitsAwakeAndFindsNewTree(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(18000);
        for (int x = -9; x <= 9; x++) for (int z = -9; z <= 9; z++)
            f.set(f.origin.offset(x, 0, z), Blocks.STONE.defaultBlockState());
        Vec3 start = Vec3.atBottomCenterOf(f.origin.above()).add(0, .015, 0);
        HummingbirdEntity bird = f.actualBird(start);
        bird.setBirdCommandMode(BirdCommandMode.FREE);
        Vec3[] previous = {start};
        double[] path = {0}, height = {start.y}, displacement = {0};
        boolean[] waitedAwake = {false};
        BlockPos[] crown = {null};
        f.run(620, tick -> {
            double step = bird.position().distanceTo(previous[0]);
            check(step < .65, "night searching and landing use actual continuous flight rather than teleporting");
            previous[0] = bird.position();
            check(f.level.noCollision(bird, bird.getBoundingBox()), "the treeless night search never enters the floor or the new tree");
            if (tick <= 160) {
                path[0] += step; height[0] = Math.max(height[0], bird.getY());
                displacement[0] = Math.max(displacement[0], bird.position().distanceToSqr(start));
                check(!bird.isSleeping() && !bird.behavior.hasAnchor(), "the first treeless search is awake actual flight");
            }
            if (tick == 160) check(height[0] > start.y + .65 && displacement[0] > 1 && path[0] > 2,
                    "a bird starting just above the floor climbs and searches instead of flapping in one place; height="
                            + (height[0] - start.y) + ", displacement=" + displacement[0] + ", path=" + path[0]);
            if (tick > 180 && tick < 225 && bird.behavior.hasAnchor()) {
                CompoundTag tag = new CompoundTag(); bird.addAdditionalSaveData(tag);
                BlockPos support = bird.blockPosition().below();
                check(!tag.contains("Perch") && !bird.isSleeping() && bird.activity() == HummingbirdEntity.Activity.PERCH
                                && !bird.isAirborne() && !bird.isWingSoundActive()
                                && HummingbirdSites.at(f.level, bird, support, false) != null
                                && !HummingbirdSites.isTreeSupport(f.level, support),
                        "no tree means safe awake folded-wing waiting, never ground sleep or a fake anchor");
                waitedAwake[0] = true;
            }
            if (tick == 225) {
                check(waitedAwake[0], "an unsuccessful night search takes a safe awake rest before trying again");
                // Make a real tree near the actual waiting bird. Rotating search must discover this new support.
                BlockPos base = BlockPos.containing(bird.position()).offset(2, -1, 1);
                crown[0] = tree(f, base, 5, 0);
            }
            if (bird.isSleeping()) {
                CompoundTag tag = new CompoundTag(); bird.addAdditionalSaveData(tag);
                check(crown[0] != null && bird.behavior.hasAnchor()
                                && HummingbirdSites.isTreeSupport(f.level, BlockPos.of(tag.getLong("Perch"))),
                        "only finding a real tree permits the sleeping state");
            }
            if (tick == 620) check(waitedAwake[0] && bird.isSleeping() && bird.behavior.hasAnchor()
                            && bird.getY() > crown[0].getY() + .9 && !bird.isWingSoundActive(),
                    "bounded retries discover a newly built tree and end in true quiet tree sleep; activity="
                            + bird.activity() + ", position=" + bird.position());
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_tree_night_blocked", timeoutTicks = 690)
    public static void lowCeilingNightWaitIsCollisionSafeAndDaytimeFreeFlightResumes(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(18000);
        for (int x = -9; x <= 9; x++) for (int z = -9; z <= 9; z++)
            f.set(f.origin.offset(x, 0, z), Blocks.STONE.defaultBlockState());
        // A one-block high enclosed air pocket leaves room for this actual .38-high bird,
        // but blocks the normal climb and every outside night-search waypoint.
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            f.set(f.origin.offset(x, 2, z), Blocks.STONE.defaultBlockState());
            if (Math.abs(x) == 2 || Math.abs(z) == 2) f.set(f.origin.offset(x, 1, z), Blocks.STONE.defaultBlockState());
        }
        Vec3 start = Vec3.atBottomCenterOf(f.origin.above()).add(0, .015, 0);
        HummingbirdEntity bird = f.actualBird(start);
        bird.setBirdCommandMode(BirdCommandMode.FREE);
        Vec3[] previous = {start};
        boolean[] waitedAwake = {false}, resumedNight = {false};
        double[] dayPath = {0};
        f.run(650, tick -> {
            double step = bird.position().distanceTo(previous[0]); previous[0] = bird.position();
            check(step < .65 && f.level.noCollision(bird, bird.getBoundingBox()),
                    "a blocked night search stays collision-safe and never bypasses the ceiling by teleporting");
            check(!bird.isSleeping(), "an enclosed stone room never becomes a sleeping tree");
            if (tick < 230) check(bird.getBoundingBox().minX >= f.origin.getX() - 1
                            && bird.getBoundingBox().maxX <= f.origin.getX() + 2
                            && bird.getBoundingBox().minZ >= f.origin.getZ() - 1
                            && bird.getBoundingBox().maxZ <= f.origin.getZ() + 2
                            && bird.getBoundingBox().maxY <= f.origin.getY() + 2.0001,
                    "the bird stays inside the real one-block air pocket while it is enclosed");
            if (tick > 180 && tick < 230 && bird.behavior.hasAnchor()) {
                check(bird.activity() == HummingbirdEntity.Activity.PERCH && !bird.isWingSoundActive(),
                        "a blocked search safely folds the wings and waits awake");
                waitedAwake[0] = true;
            }
            if (tick == 230) {
                check(waitedAwake[0], "a blocked bird finds its collision-safe floor rest instead of flapping indefinitely");
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                    f.set(f.origin.offset(x, 2, z), Blocks.AIR.defaultBlockState());
            }
            if (tick > 400 && tick < 560 && bird.isAirborne() && bird.getY() > start.y + .65 && step > .002)
                resumedNight[0] = true;
            if (tick == 560) {
                check(resumedNight[0], "after opening the ceiling a later real search climbs and moves again");
                f.day(6000);
            }
            if (tick > 560) dayPath[0] += step;
            if (tick == 650) check(bird.getBirdCommandMode() == BirdCommandMode.FREE
                            && bird.isAirborne() && !bird.behavior.hasAnchor() && dayPath[0] > .5,
                    "daytime FREE clears nighttime waiting and resumes the ordinary actual flight behavior");
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_tree_night_high", timeoutTicks = 410)
    public static void highTreelessNightSearchDescendsAndWaitsOnRealSupport(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(18000);
        for (int x = -9; x <= 9; x++) for (int z = -9; z <= 9; z++)
            f.set(f.origin.offset(x, 0, z), Blocks.STONE.defaultBlockState());
        Vec3 floorFeet = Vec3.atBottomCenterOf(f.origin.above()).add(0, .015, 0);
        Vec3 start = floorFeet.add(0, 12.4, 0);
        HummingbirdEntity bird = f.actualBird(start);
        bird.setBirdCommandMode(BirdCommandMode.FREE);
        Vec3[] previous = {start};
        double[] descended = {0};
        int[] awakeRestTicks = {0};
        f.run(380, tick -> {
            double step = bird.position().distanceTo(previous[0]); previous[0] = bird.position();
            check(step < .65 && f.level.noCollision(bird, bird.getBoundingBox()),
                    "high-altitude night recovery uses continuous real movement without entering the floor");
            check(!bird.isSleeping(), "descending onto a plain floor never permits tree sleep");
            check(bird.getY() >= floorFeet.y - .0001, "the treeless high bird never moves under its real stone floor");
            descended[0] = Math.max(descended[0], start.y - bird.getY());
            if (bird.behavior.hasAnchor()) {
                CompoundTag tag = new CompoundTag(); bird.addAdditionalSaveData(tag);
                HummingbirdSites.Perch support = HummingbirdSites.at(f.level, bird, bird.blockPosition().below(), false);
                check(!tag.contains("Perch") && bird.activity() == HummingbirdEntity.Activity.PERCH
                                && !bird.isAirborne() && !bird.isWingSoundActive() && support != null
                                && bird.position().distanceToSqr(support.feet()) < .0025,
                        "the high bird eventually stands awake with folded wings on actual ground, with no persisted sleeping anchor");
                awakeRestTicks[0]++;
            }
            if (tick == 380) check(descended[0] > 11 && awakeRestTicks[0] >= 20
                            && bird.behavior.hasAnchor() && bird.position().y < floorFeet.y + .05,
                    "a search starting above the ten-block support scan descends and finishes safely waiting on the floor; descent="
                            + descended[0] + ", restTicks=" + awakeRestTicks[0] + ", activity=" + bird.activity());
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_tree_nectar", timeoutTicks = 1040)
    public static void nectarVisitLeavesTreesAndReturnsToTreeRest(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(6000);
        BlockPos flower = f.origin;
        f.flower(flower, Blocks.POPPY);
        tree(f, f.origin.offset(6, 0, 0), 5, 1);
        HummingbirdEntity bird = f.actualBird(Vec3.atCenterOf(flower).add(0, .5, .72));
        boolean[] fed = {false};
        int[] treeRest = {0};
        // Includes the full 12--24 second first tree rest, real flight both ways,
        // the unchanged nectar work interval, and at least 30 ticks resting again.
        f.run(1000, tick -> {
            fed[0] |= bird.activity() == HummingbirdEntity.Activity.NECTAR_LOOP;
            if (fed[0] && bird.activity() == HummingbirdEntity.Activity.PERCH) {
                CompoundTag tag = new CompoundTag(); bird.addAdditionalSaveData(tag);
                check(HummingbirdSites.isTreeSupport(f.level, BlockPos.of(tag.getLong("Perch"))),
                        "the post-nectar resting anchor is on the tree");
                treeRest[0]++;
            }
            if (tick == 1000) {
                CompoundTag tag = new CompoundTag(); bird.addAdditionalSaveData(tag);
                check(fed[0] && tag.getInt("FlowerVisits") >= 1 && treeRest[0] >= 30,
                        "actual AI still completes nectar feeding, then returns to a tree; visits="
                                + tag.getInt("FlowerVisits") + ", treeTicks=" + treeRest[0] + ", activity=" + bird.activity());
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_tree_flight_window", timeoutTicks = 1040)
    public static void naturalFreeTreeDepartureKeepsRealWingFlightBeforeReturning(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(6000);
        BlockPos crown = tree(f, f.origin.offset(2, 0, 2), 5, 1);
        HummingbirdEntity bird = f.actualBird(Vec3.atCenterOf(f.origin).add(-4, 1, 0));
        Vec3[] previous = {bird.position()};
        Vec3[] departedFeet = {null};
        boolean[] perched = {false}, departed = {false}, returned = {false};
        int[] airTicks = {0}, treeTicks = {0};
        f.run(1000, tick -> {
            check(bird.position().distanceTo(previous[0]) < .65, "the full tree flight cycle uses real continuous movement");
            previous[0] = bird.position();
            check(f.level.noCollision(bird, bird.getBoundingBox()), "the tree flight window never moves through leaves or the trunk");
            if (bird.behavior.hasAnchor() && bird.activity() == HummingbirdEntity.Activity.PERCH) {
                CompoundTag tag = new CompoundTag(); bird.addAdditionalSaveData(tag);
                check(HummingbirdSites.isTreeSupport(f.level, BlockPos.of(tag.getLong("Perch"))), "both rests use an actual tree");
                treeTicks[0]++;
                if (!perched[0]) {
                    perched[0] = true; departedFeet[0] = bird.position();
                } else if (departed[0] && !returned[0]) {
                    check(airTicks[0] >= 80, "natural FREE departure stays genuinely airborne at least four seconds; ticks=" + airTicks[0]);
                    returned[0] = true;
                }
            } else if (perched[0] && !returned[0]) {
                departed[0] = true;
                check(bird.isAirborne() && bird.isWingSoundActive(), "the flight window has real flying/hovering activity, not a perched sound override");
                airTicks[0]++;
                if (airTicks[0] <= 80) {
                    check(bird.getY() >= departedFeet[0].y - .06
                                    && bird.position().distanceToSqr(departedFeet[0]) < 25,
                            "the short flight stays around its tree crown rather than browsing down on the ground");
                }
            }
            if (tick == 1000) check(perched[0] && departed[0] && returned[0] && airTicks[0] >= 80 && treeTicks[0] >= 240
                            && bird.getY() > crown.getY(),
                    "real AI rests mostly on trees, flies for four-to-five seconds, and returns safely; airTicks="
                            + airTicks[0] + ", treeTicks=" + treeTicks[0] + ", activity=" + bird.activity());
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_tree_flight_priority", timeoutTicks = 430)
    public static void stayAndNightInterruptTheFreeFlightWindowImmediately(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(6000);
        HummingbirdEntity[] birds = new HummingbirdEntity[2];
        HummingbirdSites.Perch[] supports = new HummingbirdSites.Perch[2];
        for (int i = 0; i < birds.length; i++) {
            // The 1--2.2 block crown browse remains over this support. Daytime
            // STAY can therefore use its real directly-below fallback immediately,
            // without relying on a lucky sample among its 64 random candidates.
            BlockPos crown = tree(f, f.origin.offset(i * 16, 0, 0), 5, 3);
            birds[i] = f.actualBird(Vec3.atBottomCenterOf(crown.above()).add(0, .015, 0));
            HummingbirdSites.Perch perch = HummingbirdSites.at(f.level, birds[i], crown, true);
            check(perch != null, "the real saved tree support has room and a connected trunk");
            supports[i] = perch;
            CompoundTag tag = savedAt(birds[i], perch, false);
            tag.putInt("HummingbirdCommand", BirdCommandMode.FREE.ordinal());
            birds[i].readAdditionalSaveData(tag);
        }
        int[] departure = {-1, -1};
        BlockPos[] staySupport = {null};
        net.minecraft.world.level.block.state.BlockState[] priorSupport = {null};
        Vec3[] previous = {birds[0].position(), birds[1].position()};
        f.run(390, tick -> {
            if (tick == 1 || tick == 10 || tick == 50) {
                System.out.println("HUMMINGBIRD_PRIORITY_DIAGNOSTIC frame=" + tick
                        + ", bird0={" + supportSnapshot(birds[0], supports[0])
                        + "}, bird1={" + supportSnapshot(birds[1], supports[1]) + "}");
            }
            for (int i = 0; i < birds.length; i++) {
                check(birds[i].position().distanceTo(previous[i]) < .65, "priority transitions continue flying rather than teleporting");
                previous[i] = birds[i].position();
                check(f.level.noCollision(birds[i], birds[i].getBoundingBox()), "priority landing keeps the actual bird outside the tree");
                if (departure[i] < 0 && tick > 50 && !birds[i].behavior.hasAnchor() && birds[i].isAirborne()) departure[i] = tick;
            }
            if (tick == 50) check(birds[0].behavior.hasAnchor() && birds[1].behavior.hasAnchor(),
                    "both saved tree anchors restored as real rests; frame=" + tick
                            + ", bird0={" + supportSnapshot(birds[0], supports[0])
                            + "}, bird1={" + supportSnapshot(birds[1], supports[1]) + "}");
            if (departure[0] >= 0) {
                int flight = tick - departure[0];
                if (flight == 20) {
                    check(birds[0].isAirborne() && birds[1].isAirborne(),
                            "both birds are genuinely flying before the interrupt; frame=" + tick
                                    + ", departures=" + java.util.Arrays.toString(departure)
                                    + ", bird0={" + flightSnapshot(birds[0]) + "}, bird1={" + flightSnapshot(birds[1]) + "}");
                    // Provide a definite safe daytime support: bounded random searches may otherwise miss a crown.
                    staySupport[0] = BlockPos.containing(birds[0].position()).below();
                    priorSupport[0] = f.level.getBlockState(staySupport[0]);
                    f.set(staySupport[0], Blocks.STONE.defaultBlockState());
                    birds[0].setBirdCommandMode(BirdCommandMode.STAY);
                }
                if (flight == 23) check(birds[0].activity() == HummingbirdEntity.Activity.LAND || birds[0].behavior.hasAnchor(),
                        "STAY starts finding/landing on support immediately instead of waiting out the four-second FREE window");
                if (flight == 30) {
                    f.set(staySupport[0], priorSupport[0]);
                    f.day(18000);
                }
                if (flight == 33) check(birds[1].activity() == HummingbirdEntity.Activity.LAND || birds[1].isSleeping(),
                        "night immediately cancels ordinary crown browsing and starts returning to the tree");
            }
            if (tick == 390) {
                check(departure[0] >= 0 && birds[0].getBirdCommandMode() == BirdCommandMode.STAY,
                        "the interrupt acts on a real natural departure and preserves the STAY command");
                for (HummingbirdEntity bird : birds) {
                    CompoundTag tag = new CompoundTag(); bird.addAdditionalSaveData(tag);
                    check(bird.isSleeping() && bird.behavior.hasAnchor()
                                    && HummingbirdSites.isTreeSupport(f.level, BlockPos.of(tag.getLong("Perch")))
                                    && !bird.isWingSoundActive(),
                            "interrupted flight finishes with true tree sleep and no artificial wing sound");
                }
            }
        });
    }
}
