package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.registry.GuaniaoBlocks;
import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Release regressions for unreachable targets, with actual AI, collision and item spawning. */
@GameTestHolder("guaniao_hummingbird_qa")
@PrefixGameTestTemplate(false)
public final class HummingbirdReleaseGameTests {
    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    @GameTest(template = "garden", batch = "hummingbird_release_blocked_browse", timeoutTicks = 350)
    public static void blockedBrowsingTargetIsAbandonedWithoutDisablingCollision(GameTestHelper helper) throws Exception {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(6000);
        BlockPos enclosed = f.origin.offset(5, 5, 0);
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++)
            f.set(enclosed.offset(x, y, z), Blocks.STONE.defaultBlockState());
        Vec3 unreachable = Vec3.atCenterOf(enclosed);
        HummingbirdEntity bird = f.actualBird(unreachable.add(-5, 0, 0));
        Field destination = HummingbirdGardenBehavior.class.getDeclaredField("destination");
        destination.setAccessible(true);
        destination.set(bird.behavior, unreachable);
        boolean[] abandoned = {false};
        Vec3[] previous = {bird.position()};
        f.run(300, tick -> {
            check(bird.isAlive() && !bird.isNoAi() && !bird.noPhysics, "blocked browsing keeps real autonomous AI");
            check(f.level.noCollision(bird, bird.getBoundingBox()), "browsing never escapes an obstacle by clipping");
            check(bird.position().distanceTo(previous[0]) < .65, "reselection never teleports the bird");
            previous[0] = bird.position();
            try { abandoned[0] |= !unreachable.equals(destination.get(bird.behavior)); }
            catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
            if (tick == 300) check(abandoned[0], "a solid-block browse target is not retained forever");
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_release_blocked_delivery", timeoutTicks = 620)
    public static void enclosedBathKeepsFlowerAndDeliveryResumesAfterOpening(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(6000);
        BlockPos bath = f.origin.offset(5, 4, 0);
        f.set(bath.below(), Blocks.STONE.defaultBlockState());
        f.set(bath, GuaniaoBlocks.BIRD_BATH.get().defaultBlockState());
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) for (int z = -1; z <= 1; z++) {
            if (x != 0 || z != 0 || y == 2) f.set(bath.offset(x, y, z), Blocks.STONE.defaultBlockState());
        }
        HummingbirdEntity bird = f.actualBird(Vec3.atCenterOf(f.origin).add(0, 5, 0));
        bird.behavior.bindGarden(f.level, bath);
        bird.setHeldGardenItem(new ItemStack(Items.POPPY));
        AABB drops = new AABB(bath).inflate(16);
        boolean[] delivered = {false};
        Vec3[] previous = {bird.position()};
        f.run(570, tick -> {
            check(f.level.noCollision(bird, bird.getBoundingBox()), "delivery respects the enclosed bath and world collision");
            check(bird.position().distanceTo(previous[0]) < .65, "delivery never teleports through the enclosure");
            previous[0] = bird.position();
            var flowers = f.level.getEntitiesOfClass(ItemEntity.class, drops, i -> i.getItem().is(Items.POPPY));
            if (tick < 180) {
                check(bird.getHeldGardenItem().is(Items.POPPY) && flowers.isEmpty(), "an inaccessible bath cannot discard or duplicate the carried flower");
                check(bird.activity() != HummingbirdEntity.Activity.DROP_SEED, "blocked delivery has no remote item drop");
            }
            if (tick == 180) {
                for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) for (int z = -1; z <= 1; z++)
                    if (x != 0 || z != 0 || y == 2) f.set(bath.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
            if (bird.getHeldGardenItem().isEmpty()) {
                check(flowers.size() == 1 && flowers.get(0).getItem().getCount() == 1, "opening the bath permits exactly one real flower delivery");
                if (!delivered[0]) check(flowers.get(0).position().distanceToSqr(Vec3.atCenterOf(bath)) < 9, "delivery appears at the bath");
                delivered[0] = true;
            }
            if (tick == 570) check(delivered[0], "delivery retries after the obstacle is removed");
        });
    }
}
