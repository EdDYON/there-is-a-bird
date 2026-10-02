package EdDYON.guaniao.content.bird.headperch;

import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.content.bird.command.BirdHeadPlacement;
import EdDYON.guaniao.content.bird.command.CommandableBird;
import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
import EdDYON.guaniao.content.bird.kestrel.KestrelEntity;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Test the registered Forge block-use path, then observe real server AI after placement. */
@GameTestHolder("guaniao_head_perch_qa")
@PrefixGameTestTemplate(false)
public final class BirdHeadPlacementGameTests {
    private BirdHeadPlacementGameTests() { }

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final ServerLevel level;
        final BlockPos origin;
        final FakePlayer owner, stranger;
        final TamableAnimal bird;
        final Map<BlockPos, BlockState> previous = new HashMap<>();
        final Set<Long> tickets = new HashSet<>();
        final List<Entity> extra = new ArrayList<>();
        boolean closed;

        Fixture(GameTestHelper helper, boolean hummingbird) {
            this.helper = helper;
            level = helper.getLevel();
            // A fresh dry platform isolates the suite from stale test-world water.
            origin = helper.absolutePos(new BlockPos(8, 130, 8));
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setDayTime(6000);
            for (int x = (origin.getX() - 10) >> 4; x <= (origin.getX() + 10) >> 4; x++)
                for (int z = (origin.getZ() - 10) >> 4; z <= (origin.getZ() + 10) >> 4; z++) {
                    long key = ChunkPos.asLong(x, z);
                    if (!level.getForcedChunks().contains(key)) { level.setChunkForced(x, z, true); tickets.add(key); }
                    level.getChunk(x, z);
                }
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5, -1, -5), origin.offset(9, 6, 9)))
                set(pos, pos.getY() == origin.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            owner = player("HeadOwner", Vec3.atBottomCenterOf(origin));
            stranger = player("HeadOther", Vec3.atBottomCenterOf(origin.offset(-3, 0, 3)));
            bird = hummingbird ? GuaniaoEntityTypes.HUMMINGBIRD.get().create(level) : GuaniaoEntityTypes.KESTREL.get().create(level);
            check(bird != null, "registered species creates a real bird");
            bird.setPersistenceRequired();
            bird.tame(owner);
            ((CommandableBird)bird).setBirdCommandMode(BirdCommandMode.FOLLOW);
            bird.moveTo(owner.getX(), owner.getY() + 2, owner.getZ(), 0, 0);
            check(level.addFreshEntity(bird) && bird.startRiding(owner, true), "owned bird starts on its actual owner's head");
            check(!bird.isNoAi() && !bird.noPhysics, "head-perch fixture runs real AI and collision");
        }

        FakePlayer player(String name, Vec3 pos) {
            FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), name));
            player.moveTo(pos.x, pos.y, pos.z, 0, 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            level.addNewPlayer(player);
            return player;
        }

        void set(BlockPos pos, BlockState state) {
            previous.putIfAbsent(pos.immutable(), level.getBlockState(pos));
            level.setBlockAndUpdate(pos, state);
        }

        BlockPos support() { return origin.offset(2, -1, 0); }

        BlockHitResult hit(BlockPos support) {
            double top = level.getBlockState(support).getCollisionShape(level, support).max(Direction.Axis.Y);
            return new BlockHitResult(new Vec3(support.getX() + .5, support.getY() + top, support.getZ() + .5),
                    Direction.UP, support, false);
        }

        void mounted(String label) {
            check(bird.getVehicle() == owner && ((CommandableBird)bird).getBirdCommandMode() == BirdCommandMode.FOLLOW,
                    label + " preserves the existing head passenger and command");
        }

        void run(int ticks, IntConsumer assertion) {
            for (int tick = 1; tick <= ticks; tick++) {
                int current = tick;
                helper.runAtTickTime(current, () -> {
                    if (closed) return;
                    try {
                        assertion.accept(current);
                        if (current == ticks) { close(); helper.succeed(); }
                    } catch (RuntimeException | Error failure) { close(); throw failure; }
                });
            }
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            bird.discard();
            extra.forEach(Entity::discard);
            level.removePlayerImmediately(owner, Entity.RemovalReason.DISCARDED);
            level.removePlayerImmediately(stranger, Entity.RemovalReason.DISCARDED);
            previous.forEach((pos, state) -> level.setBlock(pos, state, 18));
            for (long key : tickets) level.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
        }
    }

    private static void shiftAndMove(GameTestHelper helper, boolean hummingbird) {
        Fixture f = new Fixture(helper, hummingbird);
        f.run(70, tick -> {
            if (tick == 5) f.owner.setShiftKeyDown(true);
            if (tick >= 5) f.mounted("Shift alone");
            if (tick % 10 == 0) {
                f.owner.setXRot((tick % 30 - 10) * 4);
                f.owner.setYHeadRot(tick * 3);
                f.owner.moveTo(f.origin.getX() + .5 + (tick % 20 == 0 ? .4 : 0), f.origin.getY(), f.origin.getZ() + .5);
            } else if (tick > 10) {
                Vec3 offset = f.bird instanceof HummingbirdEntity bird
                        ? bird.ownerHeadOffset(f.owner.getYHeadRot(), f.owner.getXRot())
                        : ((KestrelEntity)f.bird).ownerHeadOffset(f.owner.getYHeadRot(), f.owner.getXRot());
                check(f.bird.position().distanceTo(f.owner.position().add(offset)) < .001,
                        "real passenger follows the owner's current head position, pitch and crouching pose");
            }
        });
    }

    private static void placeAndStay(GameTestHelper helper, boolean hummingbird, boolean slab) {
        Fixture f = new Fixture(helper, hummingbird);
        if (slab) {
            f.set(f.origin.offset(1, -1, 0), Blocks.STONE_SLAB.defaultBlockState());
            f.set(f.support(), Blocks.STONE_SLAB.defaultBlockState());
        }
        Vec3 feet = f.hit(f.support()).getLocation().add(0, .015, 0);
        f.run(250, tick -> {
            if (tick == 5) {
                f.owner.setShiftKeyDown(true);
                check(BirdHeadPlacement.hasHeadBird(f.owner), "registered interaction sees the existing head bird");
                var result = f.owner.gameMode.useItemOn(f.owner, f.level, ItemStack.EMPTY,
                        InteractionHand.MAIN_HAND, f.hit(f.support()));
                check(result.consumesAction(), "vanilla UseItemOn triggers the registered Forge placement event");
                check(!f.bird.isPassenger() && !BirdHeadPlacement.hasHeadBird(f.owner), "successful ground click removes the head passenger");
                check(f.bird.position().distanceTo(feet) < .0001, "feet use the actual support surface plus .015");
                check(f.bird.getDeltaMovement().lengthSqr() < 1.0E-10, "placement clears old flight velocity");
                f.owner.setShiftKeyDown(false);
            }
            if (tick >= 6) {
                check(!f.bird.isPassenger() && ((CommandableBird)f.bird).getBirdCommandMode() == BirdCommandMode.STAY,
                        "placed bird remains STAY even after the owner stops crouching and the old head timer expires");
                check(f.bird.position().subtract(feet).horizontalDistanceSqr() < .001,
                        "real STAY does not walk or fly away from the selected support");
                check(Math.abs(f.bird.getY() - feet.y) < .025, "placed bird remains supported instead of falling or floating");
            }
        });
    }

    private static void rejectInvalid(GameTestHelper helper, boolean hummingbird) {
        Fixture f = new Fixture(helper, hummingbird);
        f.run(12, tick -> {
            if (tick != 5) return;
            f.owner.setShiftKeyDown(true);
            BlockHitResult hit = f.hit(f.support());
            check(!BirdHeadPlacement.tryPlace(f.owner, InteractionHand.OFF_HAND, hit), "offhand duplicate cannot place");
            f.mounted("offhand");
            f.owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            check(!BirdHeadPlacement.tryPlace(f.owner, InteractionHand.MAIN_HAND, hit), "held main-hand item prevents placement");
            f.mounted("main-hand item");
            f.owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            f.owner.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
            check(!BirdHeadPlacement.tryPlace(f.owner, InteractionHand.MAIN_HAND, hit), "held offhand item prevents placement");
            f.mounted("offhand item");
            f.owner.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            f.owner.setShiftKeyDown(false);
            check(!BirdHeadPlacement.tryPlace(f.owner, InteractionHand.MAIN_HAND, hit), "ordinary right click keeps the head perch");
            f.mounted("ordinary click");
            f.owner.setShiftKeyDown(true);
            f.stranger.setShiftKeyDown(true);
            check(!BirdHeadPlacement.tryPlace(f.stranger, InteractionHand.MAIN_HAND, hit), "another player cannot place the owner's bird");
            f.mounted("other player");
            BlockPos far = f.origin.offset(9, -1, 0);
            check(!BirdHeadPlacement.tryPlace(f.owner, InteractionHand.MAIN_HAND, f.hit(far)), "fabricated hit beyond interaction reach cannot place");
            f.mounted("out-of-range hit");
            check(!BirdHeadPlacement.tryPlace(f.owner, InteractionHand.MAIN_HAND,
                    new BlockHitResult(hit.getLocation().add(3, 0, 0), Direction.UP, f.support(), false)),
                    "coordinates outside their claimed support are rejected");
            f.mounted("invalid support coordinates");
            check(!BirdHeadPlacement.tryPlace(f.owner, InteractionHand.MAIN_HAND,
                    new BlockHitResult(hit.getLocation(), Direction.EAST, f.support(), false)), "side faces do not place birds");
            f.mounted("side face");
            check(!BirdHeadPlacement.tryPlace(f.owner, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(Double.NaN, hit.getLocation().y, hit.getLocation().z), Direction.UP, f.support(), false)),
                    "non-finite client coordinates are rejected");
            f.mounted("non-finite hit");
            Entity boat = EntityType.BOAT.create(f.level);
            check(boat != null, "real boat supplies an entity collision fixture");
            Vec3 feet = hit.getLocation().add(0, .015, 0);
            boat.moveTo(feet.x, feet.y, feet.z, 0, 0); boat.setNoGravity(true);
            check(f.level.addFreshEntity(boat), "blocking boat enters the level"); f.extra.add(boat);
            check(!BirdHeadPlacement.tryPlace(f.owner, InteractionHand.MAIN_HAND, hit),
                    "visible support with a colliding entity cannot place the bird inside it");
            f.mounted("entity collision");
            boat.discard();
            f.set(f.support().above(), Blocks.STONE.defaultBlockState());
            check(!BirdHeadPlacement.tryPlace(f.owner, InteractionHand.MAIN_HAND, hit), "obstructed or occupied support cannot place");
            f.mounted("block collision");
            f.set(f.support().above(), Blocks.AIR.defaultBlockState());
            f.set(f.support(), Blocks.MAGMA_BLOCK.defaultBlockState());
            check(!BirdHeadPlacement.tryPlace(f.owner, InteractionHand.MAIN_HAND, f.hit(f.support())), "dangerous ground is rejected");
            f.mounted("dangerous ground");
        });
    }

    private static final class CancellationHook {
        final Fixture fixture;
        int deny, blockAttempts, mountAttempts;
        boolean cancelDismount;
        CancellationHook(Fixture fixture) { this.fixture = fixture; }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void block(PlayerInteractEvent.RightClickBlock event) {
            if (event.getEntity() != fixture.owner || !event.getPos().equals(fixture.support())) return;
            blockAttempts++;
            if (deny == 1) event.setUseBlock(Event.Result.DENY);
            if (deny == 2) event.setUseItem(Event.Result.DENY);
        }

        @SubscribeEvent
        public void mount(EntityMountEvent event) {
            if (cancelDismount && event.isDismounting() && event.getEntityMounting() == fixture.bird
                    && event.getEntityBeingMounted() == fixture.owner) {
                mountAttempts++;
                event.setCanceled(true);
            }
        }
    }

    private static void honorForgeCancellation(GameTestHelper helper, boolean hummingbird) {
        Fixture f = new Fixture(helper, hummingbird);
        f.run(20, tick -> {
            if (tick != 5) return;
            f.owner.setShiftKeyDown(true);
            CancellationHook hook = new CancellationHook(f);
            MinecraftForge.EVENT_BUS.register(hook);
            try {
                Vec3 before = f.bird.position();
                for (int deny : new int[]{1, 2}) {
                    hook.deny = deny;
                    f.owner.gameMode.useItemOn(f.owner, f.level, ItemStack.EMPTY,
                            InteractionHand.MAIN_HAND, f.hit(f.support()));
                    f.mounted("Forge use" + (deny == 1 ? "Block" : "Item") + " DENY");
                    check(f.bird.position().distanceToSqr(before) < 1.0E-12, "interaction denial leaves the passenger position intact");
                }
                check(hook.blockAttempts == 2 && hook.mountAttempts == 0,
                        "both high-priority permission denials reached the real event and prevented dismount attempts");
                hook.deny = 0;
                hook.cancelDismount = true;
                var canceled = f.owner.gameMode.useItemOn(f.owner, f.level, ItemStack.EMPTY,
                        InteractionHand.MAIN_HAND, f.hit(f.support()));
                check(hook.mountAttempts == 1 && !canceled.consumesAction(),
                        "canceled EntityMountEvent prevents successful placement");
                f.mounted("canceled dismount");
                check(f.bird.position().distanceToSqr(before) < 1.0E-12
                        && f.bird.getDeltaMovement().lengthSqr() < 1.0E-10,
                        "canceled dismount cannot move the bird to the ground or apply its STAY physics");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(hook);
            }
            check(f.owner.gameMode.useItemOn(f.owner, f.level, ItemStack.EMPTY,
                    InteractionHand.MAIN_HAND, f.hit(f.support())).consumesAction(),
                    "removing the temporary permission hook allows the same valid click");
            check(!f.bird.isPassenger() && ((CommandableBird)f.bird).getBirdCommandMode() == BirdCommandMode.STAY,
                    "allowed retry actually places the bird after cancellation");
        });
    }

    @GameTest(template="platform", batch="head_perch", timeoutTicks=90)
    public static void hummingbirdShiftAndMovementKeepHeadPerch(GameTestHelper helper) { shiftAndMove(helper, true); }
    @GameTest(template="platform", batch="head_perch", timeoutTicks=90)
    public static void kestrelShiftAndMovementKeepHeadPerch(GameTestHelper helper) { shiftAndMove(helper, false); }
    @GameTest(template="platform", batch="head_perch", timeoutTicks=280)
    public static void hummingbirdVanillaGroundClickStays(GameTestHelper helper) { placeAndStay(helper, true, false); }
    @GameTest(template="platform", batch="head_perch", timeoutTicks=280)
    public static void kestrelVanillaGroundClickStays(GameTestHelper helper) { placeAndStay(helper, false, false); }
    @GameTest(template="platform", batch="head_perch", timeoutTicks=280)
    public static void hummingbirdSlabPlacementUsesSurface(GameTestHelper helper) { placeAndStay(helper, true, true); }
    @GameTest(template="platform", batch="head_perch", timeoutTicks=280)
    public static void kestrelSlabPlacementUsesSurface(GameTestHelper helper) { placeAndStay(helper, false, true); }
    @GameTest(template="platform", batch="head_perch", timeoutTicks=40)
    public static void hummingbirdInvalidClicksDoNotDrop(GameTestHelper helper) { rejectInvalid(helper, true); }
    @GameTest(template="platform", batch="head_perch", timeoutTicks=40)
    public static void kestrelInvalidClicksDoNotDrop(GameTestHelper helper) { rejectInvalid(helper, false); }
    @GameTest(template="platform", batch="head_perch", timeoutTicks=40)
    public static void hummingbirdHonorsForgeDenialAndDismountCancellation(GameTestHelper helper) { honorForgeCancellation(helper, true); }
    @GameTest(template="platform", batch="head_perch", timeoutTicks=40)
    public static void kestrelHonorsForgeDenialAndDismountCancellation(GameTestHelper helper) { honorForgeCancellation(helper, false); }
}
