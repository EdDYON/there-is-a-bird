package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.content.dropping.PrankFoodUtil;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("guaniao_hummingbird_qa")
@PrefixGameTestTemplate(false)
public final class HummingbirdEntityGameTests {
    private HummingbirdEntityGameTests() { }
    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    private static String nectarTrace(HummingbirdEntity bird) {
        StringBuilder trace = new StringBuilder(" yaw=" + bird.getYRot() + " head=" + bird.yHeadRot
                + " velocity=" + bird.getDeltaMovement());
        for (String name : new String[] {"flower", "destination", "aim", "ticks", "nectarAge"}) {
            try {
                var field = HummingbirdGardenBehavior.class.getDeclaredField(name);
                field.setAccessible(true);
                trace.append(' ').append(name).append('=').append(field.get(bird.behavior));
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError("nectar QA diagnostic field changed: " + name, failure);
            }
        }
        return trace.toString();
    }

    @GameTest(template = "garden", batch = "hummingbird_real_nectar", timeoutTicks = 900)
    public static void realAiTakesOffHoversAndCompletesNectar(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(1000);
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++)
            f.set(f.origin.offset(x, -1, z), Blocks.DIRT.defaultBlockState());
        for (BlockPos pos : new BlockPos[] {f.origin, f.origin.east(4), f.origin.west(4), f.origin.north(4), f.origin.south(4)})
            f.flower(pos, Blocks.POPPY);
        HummingbirdEntity bird = f.actualBird(Vec3.atBottomCenterOf(f.origin.offset(2, 0, 2)));
        bird.setOnGround(true);
        Vec3 start = bird.position();
        Set<HummingbirdEntity.Activity> seen = new HashSet<>();
        boolean[] published = {false};
        int[] airTicks = {0}, stableNectar = {0};
        f.run(820, tick -> {
            check(bird.isAlive() && !bird.isNoAi() && !bird.noPhysics, "real bird keeps normal AI and collision");
            seen.add(bird.activity());
            if (bird.isAirborne()) airTicks[0]++;
            if (bird.activity() == HummingbirdEntity.Activity.NECTAR_LOOP && bird.getDeltaMovement().lengthSqr() < .0016)
                stableNectar[0]++;
            published[0] |= HummingbirdPollinationService.activeSourcesNear(f.level, bird.position(), 1) > 0;
            if (tick % 100 == 0) {
                CompoundTag saved = new CompoundTag(); bird.addAdditionalSaveData(saved);
                System.out.println("HUMMINGBIRD_QA nectar tick=" + tick + " activity=" + bird.activity()
                        + " pos=" + bird.position().subtract(start) + " visits=" + saved.getInt("FlowerVisits")
                        + nectarTrace(bird));
            }
            if (tick == 820) {
                CompoundTag saved = new CompoundTag(); bird.addAdditionalSaveData(saved);
                check(airTicks[0] > 80 && bird.position().distanceToSqr(start) > .01, "bird leaves its initial standing position by real flight");
                check(seen.contains(HummingbirdEntity.Activity.NECTAR_ENTER)
                        && seen.contains(HummingbirdEntity.Activity.NECTAR_LOOP)
                        && seen.contains(HummingbirdEntity.Activity.NECTAR_EXIT), "real AI enters, maintains and leaves nectar feeding");
                check(stableNectar[0] >= 100 && saved.getInt("FlowerVisits") >= 2,
                        "stable hovering completes at least two real flower visits, including acquiring and facing the next flower"
                                + nectarTrace(bird));
                check(published[0], "actual validated flower feeding activates the pollination service");
            }
        });
    }

    private static final class TameHook {
        HummingbirdEntity target;
        boolean cancel = true;
        int attempts;
        @SubscribeEvent public void tame(AnimalTameEvent event) {
            if (event.getAnimal() == target) { attempts++; if (cancel) event.setCanceled(true); }
        }
    }

    private static void feed(HummingbirdEntity bird, FakePlayer player) {
        bird.mobInteract(player, InteractionHand.MAIN_HAND);
    }

    @GameTest(template = "garden", batch = "hummingbird_real_taming", timeoutTicks = 170)
    public static void fiveFeedsOwnershipAndVanillaFlowerSave(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(1000);
        HummingbirdEntity bird = f.actualBird(Vec3.atCenterOf(f.origin).add(0, 2, 0));
        FakePlayer alice = f.player("HummerOwner", Vec3.atCenterOf(f.origin).add(2, 0, 0));
        FakePlayer bob = f.player("HummerOther", Vec3.atCenterOf(f.origin).add(-2, 0, 0));
        alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SUGAR, 6));
        alice.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.POPPY));
        bob.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POPPY));
        TameHook hook = new TameHook(); hook.target = bird;
        MinecraftForge.EVENT_BUS.register(hook);
        f.cleanups.add(() -> MinecraftForge.EVENT_BUS.unregister(hook));
        f.run(140, tick -> {
            if (tick == 10) {
                feed(bird, alice);
                bird.mobInteract(alice, InteractionHand.OFF_HAND);
                check(alice.getMainHandItem().getCount() == 5 && alice.getOffhandItem().getCount() == 1,
                        "main/offhand duplicate consumes one valid feed only");
            }
            if (tick == 22) feed(bird, bob); // resets the candidate instead of inheriting Alice's progress
            if (tick == 34 || tick == 46 || tick == 58 || tick == 70 || tick == 82) feed(bird, alice);
            if (tick == 70) check(!bird.isTame() && hook.attempts == 0, "four consecutive feeds after switching owner do not tame");
            if (tick == 82) {
                check(!bird.isTame() && hook.attempts == 1, "canceled fifth feed does not grant ownership");
                check(alice.getMainHandItem().isEmpty() && bob.getMainHandItem().isEmpty()
                        && alice.getInventory().countItem(Items.GLASS_BOTTLE) == 0
                        && bob.getInventory().countItem(Items.GLASS_BOTTLE) == 0,
                        "valid vanilla sugar and flower feeds consume exactly one item without bottle remainders");
            }
            if (tick == 94) {
                hook.cancel = false;
                alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SUGAR));
                feed(bird, alice);
                check(bird.isOwnedBy(alice) && !bird.isOwnedBy(bob) && bird.getBirdCommandMode() == BirdCommandMode.FOLLOW,
                        "successful retry belongs to the actual feeder and selects FOLLOW");
                bob.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SUGAR));
                check(bird.mobInteract(bob, InteractionHand.MAIN_HAND) == InteractionResult.PASS
                        && bob.getMainHandItem().getCount() == 1, "another player cannot feed to steal a tamed bird");
                bird.setHeldGardenItem(new ItemStack(Items.POPPY));
                CompoundTag saved = new CompoundTag(); bird.addAdditionalSaveData(saved);
                HummingbirdEntity restored = (HummingbirdEntity) f.bird(Vec3.atCenterOf(f.origin).add(0, 3, 4));
                restored.readAdditionalSaveData(saved); restored.setNoAi(true);
                check(restored.isOwnedBy(alice) && restored.isMale() == bird.isMale()
                        && restored.getBirdCommandMode() == BirdCommandMode.FOLLOW
                        && restored.getHeldGardenItem().is(Items.POPPY)
                        && restored.getHeldGardenItem().getCount() == 1, "NBT preserves owner, command, sex and one actual carried item");
                f.active.remove(restored); HummingbirdPollinationService.remove(restored);
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_interaction_boundaries", timeoutTicks = 100)
    public static void vanillaInteractionPermissionsAndPartialTamingReload(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(1000);
        HummingbirdEntity original = (HummingbirdEntity) f.bird(Vec3.atCenterOf(f.origin));
        f.active.remove(original);
        HummingbirdPollinationService.remove(original);
        FakePlayer alice = f.player("HummerReload", Vec3.atCenterOf(f.origin).add(1, 0, 0));
        FakePlayer other = f.player("HummerVisitor", Vec3.atCenterOf(f.origin).add(-1, 0, 0));
        HummingbirdEntity[] restored = {null};
        f.run(75, tick -> {
            if (tick == 1) {
                other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SUGAR, 2));
                other.setGameMode(GameType.SPECTATOR);
                check(other.interactOn(original, InteractionHand.MAIN_HAND) == InteractionResult.PASS
                                && other.getMainHandItem().getCount() == 2,
                        "real Player.interactOn rejects spectator feeding before the bird's handler");
                other.setGameMode(GameType.SURVIVAL);
                ItemStack dirty = new ItemStack(Items.SUGAR, 2);
                dirty.getOrCreateTag().putBoolean(PrankFoodUtil.TAG_PRANK_FOOD, true);
                alice.setItemInHand(InteractionHand.MAIN_HAND, dirty);
                check(alice.interactOn(original, InteractionHand.MAIN_HAND) == InteractionResult.PASS
                                && dirty.getCount() == 2,
                        "polluted sugar cannot grant a feed or consume an item");
                alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WITHER_ROSE));
                check(alice.interactOn(original, InteractionHand.MAIN_HAND) == InteractionResult.PASS
                                && alice.getMainHandItem().getCount() == 1,
                        "a toxic flower is not accepted as a taming food");
                CompoundTag untouched = new CompoundTag(); original.addAdditionalSaveData(untouched);
                check(untouched.getInt("TamingProgress") == 0 && !untouched.hasUUID("TamingCandidate"),
                        "rejected interactions leave the partial-taming record untouched");
                alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SUGAR, 6));
                check(alice.interactOn(original, InteractionHand.MAIN_HAND).consumesAction(),
                        "survival owner candidate can feed through the real vanilla interaction entry");
            }
            if (tick == 13) alice.interactOn(original, InteractionHand.MAIN_HAND);
            if (tick == 15) {
                CompoundTag saved = new CompoundTag(); original.addAdditionalSaveData(saved);
                check(saved.getInt("TamingProgress") == 2 && saved.getUUID("TamingCandidate").equals(alice.getUUID())
                                && !original.isTame(), "two valid feeds are saved for their actual candidate");
                original.discard();
                restored[0] = (HummingbirdEntity) f.bird(Vec3.atCenterOf(f.origin));
                restored[0].readAdditionalSaveData(saved);
                f.active.remove(restored[0]); HummingbirdPollinationService.remove(restored[0]);
                check(!restored[0].isTame(), "reloading partial progress does not prematurely grant ownership");
            }
            if (tick == 25 || tick == 37 || tick == 49)
                alice.interactOn(restored[0], InteractionHand.MAIN_HAND);
            if (tick == 37) check(!restored[0].isTame(), "four total feeds across reload remain untamed");
            if (tick == 49) {
                check(restored[0].isOwnedBy(alice) && restored[0].getBirdCommandMode() == BirdCommandMode.FOLLOW
                                && alice.getMainHandItem().getCount() == 1,
                        "the fifth total feed after reload preserves ownership and exact item consumption");
                other.setShiftKeyDown(true);
                other.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                check(other.interactOn(restored[0], InteractionHand.MAIN_HAND) == InteractionResult.PASS
                                && restored[0].getBirdCommandMode() == BirdCommandMode.FOLLOW,
                        "another player cannot issue an empty-hand command");
                other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POPPY));
                check(other.interactOn(restored[0], InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                        "another player cannot bind an owner's garden with a flower");
                CompoundTag saved = new CompoundTag(); restored[0].addAdditionalSaveData(saved);
                check(!saved.contains("Garden"), "unauthorized garden binding has no saved side effect");
            }
            if (tick == 60) {
                HummingbirdEntity bounds = (HummingbirdEntity) f.bird(Vec3.atCenterOf(f.origin).add(3, 0, 0));
                f.active.remove(bounds); HummingbirdPollinationService.remove(bounds);
                CompoundTag malformed = new CompoundTag();
                malformed.putInt("HummingbirdCommand", Integer.MAX_VALUE);
                malformed.putInt("TamingProgress", Integer.MAX_VALUE);
                malformed.putUUID("TamingCandidate", alice.getUUID());
                malformed.put("GardenItem", new ItemStack(Items.POPPY, 64).save(new CompoundTag()));
                bounds.readAdditionalSaveData(malformed);
                CompoundTag normalized = new CompoundTag(); bounds.addAdditionalSaveData(normalized);
                check(bounds.getBirdCommandMode() == BirdCommandMode.FREE && !bounds.isTame()
                                && bounds.getHeldGardenItem().is(Items.POPPY) && bounds.getHeldGardenItem().getCount() == 1
                                && normalized.getInt("TamingProgress") == EdDYON.guaniao.config.HummingbirdConfig.tamingFeeds() - 1,
                        "out-of-range legacy NBT cannot grant ownership, select an invalid mode or duplicate a carried stack");
                malformed.putInt("TamingProgress", -1);
                bounds.readAdditionalSaveData(malformed);
                normalized = new CompoundTag(); bounds.addAdditionalSaveData(normalized);
                check(normalized.getInt("TamingProgress") == 0, "negative legacy taming progress is clamped");
                bounds.setHealth(0);
                alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SUGAR, 2));
                check(alice.interactOn(bounds, InteractionHand.MAIN_HAND) == InteractionResult.PASS
                                && alice.getMainHandItem().getCount() == 2,
                        "vanilla Mob.interact rejects a dead bird before feeding can heal or tame it");
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_real_sleep", timeoutTicks = 480)
    public static void stayLandsNightSleepsAndDayWakes(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(6000);
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++)
            f.set(f.origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        f.set(f.origin.below(), Blocks.OAK_LOG.defaultBlockState());
        f.set(f.origin, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        HummingbirdEntity bird = f.actualBird(Vec3.atCenterOf(f.origin).add(0, 4, 0));
        FakePlayer owner = f.player("HummerSleeper", Vec3.atCenterOf(f.origin).add(3, 0, 0));
        bird.tame(owner);
        bird.setBirdCommandMode(BirdCommandMode.STAY);
        HummingbirdPollinationService.activate(bird);
        Vec3[] restingAt = {null};
        boolean[] releasedTakeoff = {false}, releasedFlight = {false};
        double[] releasedMaxDistanceSqr = {0};
        f.run(420, tick -> {
            if (tick == 10) check(HummingbirdPollinationService.activeSourcesNear(f.level, bird.position(), 8) == 0,
                    "STAY promptly revokes agricultural activity");
            if (tick == 220) {
                check(bird.activity() == HummingbirdEntity.Activity.PERCH && bird.behavior.hasAnchor() && !bird.isAirborne(),
                        "STAY flies to a valid support and actually perches");
                restingAt[0] = bird.position(); f.day(18000);
            }
            if (tick == 280) check(bird.isSleeping() && bird.behavior.hasAnchor()
                    && bird.position().distanceToSqr(restingAt[0]) < .01, "night sleep keeps the real supported anchor");
            if (tick == 300) f.day(1000);
            if (tick == 340) {
                check(!bird.isSleeping() && bird.behavior.hasAnchor() && !bird.isAirborne(),
                        "daylight wakes a STAY bird without releasing its commanded perch");
                bird.setBirdCommandMode(BirdCommandMode.FREE);
            }
            if (tick > 340) {
                releasedTakeoff[0] |= bird.activity() == HummingbirdEntity.Activity.TAKEOFF;
                releasedMaxDistanceSqr[0] = Math.max(releasedMaxDistanceSqr[0], bird.position().distanceToSqr(restingAt[0]));
                releasedFlight[0] |= bird.isAirborne() && !bird.behavior.hasAnchor()
                        && bird.position().distanceToSqr(restingAt[0]) > .09;
            }
            if (tick == 370 || tick == 420) System.out.println("HUMMINGBIRD_QA released STAY tick=" + tick
                    + " activity=" + bird.activity() + " delta=" + bird.position().subtract(restingAt[0])
                    + " maxDistance=" + Math.sqrt(releasedMaxDistanceSqr[0])
                    + " observedTakeoff=" + releasedTakeoff[0] + " observedFlight=" + releasedFlight[0]);
            // FREE may choose another short perch after taking off; verify the real transition over
            // the whole observation window instead of requiring one arbitrary final frame to be airborne.
            if (tick == 420) check(releasedTakeoff[0] && releasedFlight[0],
                    "releasing STAY resumes real takeoff and leaves the old support by flight; observed takeoff="
                            + releasedTakeoff[0] + ", flight=" + releasedFlight[0] + ", maxDistance="
                            + Math.sqrt(releasedMaxDistanceSqr[0]) + ", finalActivity=" + bird.activity());
        });
    }
}
