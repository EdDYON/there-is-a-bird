package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.config.HummingbirdConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("guaniao_hummingbird_qa")
@PrefixGameTestTemplate(false)
public final class HummingbirdEcologyGameTests {
    private HummingbirdEcologyGameTests() { }
    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    private static final class Hooks {
        final Set<UUID> denied = new HashSet<>();
        final Set<BlockPos> canceled = new HashSet<>();
        final Set<BlockPos> revokeDuringPollination = new HashSet<>();
        final Map<BlockPos, List<HummingbirdPollinationEvent>> events = new HashMap<>();
        @SubscribeEvent public void permission(EntityMobGriefingEvent event) {
            if (denied.contains(event.getEntity().getUUID())) event.setResult(Event.Result.DENY);
        }
        @SubscribeEvent public void pollination(HummingbirdPollinationEvent event) {
            events.computeIfAbsent(event.getTarget(), ignored -> new ArrayList<>()).add(event);
            if (canceled.contains(event.getTarget())) event.setCanceled(true);
            if (revokeDuringPollination.contains(event.getTarget())) {
                event.getSources().forEach(source -> denied.add(source.id()));
            }
        }
        int count(BlockPos pos) { return events.getOrDefault(pos, List.of()).size(); }
    }

    /** Every test has its own batch so temporary gamerules and service reset cannot interfere. */
    static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final ServerLevel level;
        final BlockPos origin;
        final Hooks hooks = new Hooks();
        final Map<Mob, Vec3> birds = new HashMap<>();
        final Set<Mob> active = new HashSet<>();
        final Set<Mob> unfrozen = new HashSet<>();
        final List<FakePlayer> players = new ArrayList<>();
        final List<Runnable> cleanups = new ArrayList<>();
        final Set<Long> tickets = new HashSet<>();
        final Map<BlockPos, BlockState> previousBlocks = new HashMap<>();
        final int oldRandomTicks;
        final boolean oldGriefing;
        final long oldDayTime;
        final boolean oldDaylight;
        boolean closed;

        Fixture(GameTestHelper helper) {
            this.helper = helper; level = helper.getLevel();
            origin = helper.absolutePos(new BlockPos(8, 3, 8));
            oldRandomTicks = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            oldGriefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            oldDayTime = level.getDayTime();
            oldDaylight = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, level.getServer());
            FlowerHabitatIndex.forgetLevel(level);
            HummingbirdPollinationService.forgetLevel(level);
            MinecraftForge.EVENT_BUS.register(hooks);
            loadAround(origin);
        }

        void loadAround(BlockPos pos) {
            for (int x = (pos.getX() - 17) >> 4; x <= (pos.getX() + 17) >> 4; x++)
                for (int z = (pos.getZ() - 17) >> 4; z <= (pos.getZ() + 17) >> 4; z++) {
                    long key = ChunkPos.asLong(x, z);
                    if (!level.getForcedChunks().contains(key)) {
                        level.setChunkForced(x, z, true); tickets.add(key);
                    }
                    level.getChunk(x, z);
                }
        }

        BlockPos crop(int x, int z, Block block) {
            BlockPos pos = origin.offset(x, 0, z);
            loadAround(pos);
            set(pos.below(), Blocks.FARMLAND.defaultBlockState());
            set(pos, block.defaultBlockState());
            FlowerHabitatIndex.invalidate(level, pos);
            return pos;
        }

        void flower(BlockPos pos, Block block) {
            set(pos.below(), Blocks.DIRT.defaultBlockState());
            set(pos, block.defaultBlockState());
            FlowerHabitatIndex.invalidate(level, pos);
        }

        void set(BlockPos pos, BlockState state) {
            previousBlocks.putIfAbsent(pos.immutable(), level.getBlockState(pos));
            level.setBlockAndUpdate(pos, state);
        }

        Mob bird(Vec3 pos) {
            return bird(pos, UUID.randomUUID());
        }

        Mob bird(Vec3 pos, UUID id) {
            ResourceLocation typeId = new ResourceLocation("guaniao", "hummingbird");
            check(BuiltInRegistries.ENTITY_TYPE.containsKey(typeId), "hummingbird type is registered (not a default registry fallback)");
            Entity entity = BuiltInRegistries.ENTITY_TYPE.get(typeId).create(level);
            check(entity instanceof Mob, "registered hummingbird creates an actual Mob");
            Mob bird = (Mob) entity;
            bird.setUUID(id);
            bird.setNoAi(true); bird.setNoGravity(true); bird.noPhysics = true; bird.setPersistenceRequired();
            bird.moveTo(pos.x, pos.y, pos.z, 0, 0);
            check(level.addFreshEntity(bird), "hummingbird joins the test level");
            birds.put(bird, pos); active.add(bird);
            HummingbirdPollinationService.activate(bird);
            return bird;
        }

        List<Mob> group(BlockPos crop, int count) {
            List<Mob> result = new ArrayList<>();
            for (int i = 0; i < count; i++) result.add(bird(Vec3.atCenterOf(crop).add(0, 1, 0)));
            return result;
        }

        HummingbirdEntity actualBird(Vec3 pos) {
            HummingbirdEntity bird = (HummingbirdEntity) bird(pos);
            bird.setNoAi(false); bird.noPhysics = false;
            active.remove(bird); unfrozen.add(bird);
            HummingbirdPollinationService.remove(bird);
            return bird;
        }

        FakePlayer player(String name, Vec3 pos) {
            FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), name));
            player.moveTo(pos.x, pos.y, pos.z, 0, 0);
            player.getAbilities().instabuild = false;
            level.addNewPlayer(player); players.add(player);
            return player;
        }

        void day(long time) {
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setDayTime(time);
        }

        int age(BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            check(state.getBlock() instanceof CropBlock, "expected surviving crop at " + pos + ", found " + state
                    + ", support=" + level.getBlockState(pos.below()) + ", brightness=" + level.getRawBrightness(pos, 0));
            return ((CropBlock) state.getBlock()).getAge(state);
        }

        void run(int ticks, IntConsumer assertions) {
            for (int tick = 1; tick <= ticks; tick++) {
                int current = tick;
                helper.runAtTickTime(tick, () -> {
                    if (closed) return;
                    try {
                        for (Map.Entry<Mob, Vec3> entry : birds.entrySet()) if (!entry.getKey().isRemoved() && !unfrozen.contains(entry.getKey())) {
                            Mob bird = entry.getKey(); Vec3 pos = entry.getValue();
                            bird.setPos(pos.x, pos.y, pos.z); bird.setDeltaMovement(Vec3.ZERO);
                            if (active.contains(bird)) HummingbirdPollinationService.activate(bird);
                        }
                        check(FlowerHabitatIndex.stats(level).examinedThisTick() <= HummingbirdConfig.flowerScanBudget(),
                                "section index stays within the per-level block budget");
                        assertions.accept(current);
                        if (current == ticks) { close(); helper.succeed(); }
                    } catch (RuntimeException | Error failure) {
                        close(); throw failure;
                    }
                });
            }
        }

        @Override public void close() {
            if (closed) return; closed = true;
            MinecraftForge.EVENT_BUS.unregister(hooks);
            cleanups.forEach(Runnable::run);
            for (Mob bird : birds.keySet()) {
                HummingbirdPollinationService.remove(bird);
                if (!bird.isRemoved()) bird.discard();
            }
            for (FakePlayer player : players) level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            for (Map.Entry<BlockPos, BlockState> entry : previousBlocks.entrySet()) {
                level.setBlock(entry.getKey(), entry.getValue(), 18);
            }
            for (long key : tickets) level.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(oldRandomTicks, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(oldGriefing, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(oldDaylight, level.getServer());
            level.setDayTime(oldDayTime);
            FlowerHabitatIndex.forgetLevel(level);
            HummingbirdPollinationService.forgetLevel(level);
        }
    }

    @GameTest(template = "garden", batch = "hummingbird_flowers", timeoutTicks = 180)
    public static void flowerTargetsCountsLeasesAndInvalidation(GameTestHelper helper) {
        Fixture f = new Fixture(helper);
        BlockPos ordinary = f.origin, tall = f.origin.east(3), pot = f.origin.east(6);
        f.flower(ordinary, Blocks.POPPY);
        f.set(tall.below(), Blocks.DIRT.defaultBlockState());
        f.previousBlocks.putIfAbsent(tall, f.level.getBlockState(tall));
        f.previousBlocks.putIfAbsent(tall.above(), f.level.getBlockState(tall.above()));
        DoublePlantBlock.placeAt(f.level, Blocks.ROSE_BUSH.defaultBlockState(), tall, 3);
        f.set(pot.below(), Blocks.STONE.defaultBlockState());
        f.set(pot, Blocks.POTTED_DANDELION.defaultBlockState());
        f.flower(f.origin.south(3), Blocks.WITHER_ROSE);
        FlowerHabitatIndex.request(f.level, f.origin, 16);
        check(FlowerHabitatIndex.countFlowers(f.level, f.origin, 16) == 0, "query queues asynchronous indexing");
        f.run(140, tick -> {
            if (tick == 80) {
                check(FlowerHabitatIndex.countFlowers(f.level, f.origin, 16) == 3,
                        "ordinary flower + tall plant + potted flower count as 3; dangerous flower excluded");
                FlowerHabitatIndex.NectarTarget lower = FlowerHabitatIndex.targetAt(f.level, tall);
                FlowerHabitatIndex.NectarTarget upper = FlowerHabitatIndex.targetAt(f.level, tall.above());
                check(lower != null && upper != null && lower.anchor().equals(upper.anchor())
                        && lower.flowerPos().equals(tall.above()) && lower.contact().y > tall.getY() + 1,
                        "tall plant uses its upper flower but has one shared anchor");
                FlowerHabitatIndex.NectarTarget potted = FlowerHabitatIndex.targetAt(f.level, pot);
                check(potted != null && potted.flower() == Blocks.DANDELION
                        && potted.type() == FlowerHabitatIndex.SourceType.POTTED_FLOWER, "pot resolves the real flower");
                UUID a = UUID.randomUUID(), b = UUID.randomUUID();
                check(FlowerHabitatIndex.claim(f.level, lower, a), "first bird claims flower");
                check(!FlowerHabitatIndex.claim(f.level, upper, b), "upper half cannot bypass another bird's lease");
                check(FlowerHabitatIndex.complete(f.level, lower, a, f.level.random), "owner completes once");
                check(!FlowerHabitatIndex.complete(f.level, lower, a, f.level.random)
                        && !FlowerHabitatIndex.claim(f.level, upper, b), "completion and 60–120 second cooldown cannot be duplicated");
                FlowerHabitatIndex.NectarTarget target = FlowerHabitatIndex.targetAt(f.level, ordinary);
                check(FlowerHabitatIndex.claim(f.level, target, a), "second flower can be leased");
                f.set(ordinary, Blocks.AIR.defaultBlockState());
                check(!FlowerHabitatIndex.valid(f.level, target)
                        && !FlowerHabitatIndex.complete(f.level, target, a, f.level.random), "removed flower cannot settle a reward");
                check(FlowerHabitatIndex.countFlowers(f.level, f.origin, 16) == 2,
                        "count revalidates removed flowers before cache refresh");
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_inflight_index_edit", timeoutTicks = 90)
    public static void editsBehindScanCursorRefreshWithoutWaitingTwoHundredTicks(GameTestHelper helper) throws ReflectiveOperationException {
        Fixture f = new Fixture(helper);
        var scanField = HummingbirdConfig.class.getDeclaredField("SCAN");
        scanField.setAccessible(true);
        ForgeConfigSpec.IntValue scan = (ForgeConfigSpec.IntValue) scanField.get(null);
        int previousBudget = scan.get();
        scan.set(256);
        f.cleanups.add(() -> scan.set(previousBudget));
        BlockPos section = new BlockPos((f.origin.getX() >> 4) << 4,
                (f.origin.getY() >> 4) << 4, (f.origin.getZ() >> 4) << 4);
        BlockPos flower = section.offset(3, 2, 3), crop = section.offset(5, 2, 3);
        f.set(flower.below(), Blocks.DIRT.defaultBlockState());
        f.set(flower, Blocks.AIR.defaultBlockState());
        f.set(crop.below(), Blocks.FARMLAND.defaultBlockState());
        f.set(crop, Blocks.AIR.defaultBlockState());
        f.set(crop.above(), Blocks.AIR.defaultBlockState());
        f.set(crop.above(2), Blocks.AIR.defaultBlockState());
        f.set(crop.above(3), Blocks.GLOWSTONE.defaultBlockState());
        FlowerHabitatIndex.request(f.level, flower, 0);
        f.run(55, tick -> {
            if (tick == 6) {
                check(FlowerHabitatIndex.stats(f.level).queuedSections() == 1,
                        "single 4096-block section is still in its 256-block-per-tick scan");
                f.flower(flower, Blocks.POPPY);
                f.set(crop, Blocks.WHEAT.defaultBlockState());
                FlowerHabitatIndex.invalidate(f.level, crop);
            }
            if (tick == 42) {
                check(FlowerHabitatIndex.flowersNear(f.level, Vec3.atCenterOf(flower), 4).stream()
                                .anyMatch(target -> target.anchor().equals(flower)),
                        "new flower behind the original cursor is found by an immediate second scan");
                check(FlowerHabitatIndex.cropsNear(f.level, Vec3.atCenterOf(crop), 4).contains(crop),
                        "new crop behind the original cursor is found without a 200-tick refresh delay");
                check(f.age(crop) == 0, "indexing itself does not grow the newly planted crop");
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_commit_permissions", timeoutTicks = 520)
    public static void permissionsRevokedDuringPollinationPreventTheCommit(GameTestHelper helper) {
        Fixture f = new Fixture(helper);
        BlockPos revoked = f.crop(0, 0, Blocks.WHEAT), allowed = f.crop(32, 0, Blocks.CARROTS);
        f.group(revoked, 4); f.group(allowed, 4);
        f.hooks.revokeDuringPollination.add(revoked);
        f.run(470, tick -> {
            if (tick == 470) {
                check(f.hooks.count(revoked) == 1 && f.age(revoked) == 0,
                        "permission was initially allowed, then revoked by the transaction listener before growth");
                check(f.hooks.count(allowed) == 1 && f.age(allowed) > 0,
                        "an unchanged, authorized garden still commits in the same round");
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_stacking", timeoutTicks = 900)
    public static void sourceCapRoundDeduplicationAndOwners(GameTestHelper helper) {
        Fixture f = new Fixture(helper);
        BlockPos crop = f.crop(0, 0, Blocks.WHEAT);
        List<Mob> group = f.group(crop, 6);
        UUID owner = UUID.randomUUID();
        for (Mob bird : group) { ((TamableAnimal) bird).setOwnerUUID(owner); ((TamableAnimal) bird).setTame(true); }
        check(HummingbirdPollinationService.chanceFor(1) == 0.25
                && HummingbirdPollinationService.chanceFor(2) == 0.5
                && HummingbirdPollinationService.chanceFor(3) == 0.75
                && HummingbirdPollinationService.chanceFor(4) == 1
                && HummingbirdPollinationService.chanceFor(6) == 1, "25/50/75/100 percent with a four-source cap");
        f.run(850, tick -> {
            for (Mob bird : group) HummingbirdPollinationService.activate(bird); // duplicates must not create extra sources
            if (tick == 450) {
                check(f.age(crop) > 0 && f.hooks.count(crop) == 1, "six birds give one bone-meal action in round one");
                HummingbirdPollinationEvent event = f.hooks.events.get(crop).get(0);
                check(event.getSources().size() == 4 && event.getOwners().equals(Set.of(owner)),
                        "event carries four distinct sources and their actual owner");
                check(event.getSources().stream().map(HummingbirdPollinationService.Source::id).distinct().count() == 4,
                        "refreshing a UUID does not inflate the crop source count");
            }
            if (tick == 850) {
                check(f.hooks.count(crop) == 2, "same crop is evaluated at most once in each of two rounds");
                check(f.hooks.events.get(crop).stream().map(HummingbirdPollinationEvent::getRound).distinct().count() == 2,
                        "second growth belongs to a genuinely new round");
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_permissions", timeoutTicks = 900)
    public static void gameruleForgePermissionAndCancellation(GameTestHelper helper) {
        Fixture f = new Fixture(helper);
        BlockPos denied = f.crop(0, 0, Blocks.WHEAT), canceled = f.crop(32, 0, Blocks.CARROTS), allowed = f.crop(64, 0, Blocks.POTATOES);
        for (Mob bird : f.group(denied, 4)) f.hooks.denied.add(bird.getUUID());
        f.group(canceled, 4); f.group(allowed, 4); f.hooks.canceled.add(canceled);
        f.level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, f.level.getServer());
        f.run(850, tick -> {
            if (tick == 450) {
                check(f.age(denied) == 0 && f.age(canceled) == 0 && f.age(allowed) == 0
                        && f.hooks.events.isEmpty(), "mobGriefing=false stops the entire automated growth transaction");
                f.level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, f.level.getServer());
            }
            if (tick == 850) {
                check(f.age(denied) == 0 && f.hooks.count(denied) == 0, "Forge denial removes all eligible sources");
                check(f.age(canceled) == 0 && f.hooks.count(canceled) == 1, "cancelable owner-aware event prevents crop changes");
                check(f.age(allowed) > 0 && f.hooks.count(allowed) == 1, "unblocked control still grows in the same round");
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_range", timeoutTicks = 520)
    public static void threeDimensionalRangeAndCropWhitelist(GameTestHelper helper) {
        Fixture f = new Fixture(helper);
        BlockPos inside = f.crop(0, 0, Blocks.BEETROOTS), above = f.crop(32, 0, Blocks.WHEAT);
        for (int i = 0; i < 4; i++) {
            f.bird(Vec3.atCenterOf(inside).add(8, 0, 0));
            f.bird(Vec3.atCenterOf(above).add(0, 8.25, 0));
        }
        BlockPos sapling = inside.south(3);
        f.set(sapling.below(), Blocks.DIRT.defaultBlockState());
        f.set(sapling, Blocks.OAK_SAPLING.defaultBlockState());
        f.run(470, tick -> {
            if (tick == 470) {
                // Vanilla beetroot can legitimately gain zero age from one bone-meal use.
                check(f.hooks.count(inside) == 1, "beetroot exactly eight blocks from four sources receives one legal bone-meal transaction");
                check(f.age(above) == 0 && f.hooks.count(above) == 0, "8.25-block vertical separation is outside the sphere");
                check(f.level.getBlockState(sapling).is(Blocks.OAK_SAPLING) && f.hooks.count(sapling) == 0,
                        "non-whitelisted bone-meal targets are never processed");
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_lifecycle", timeoutTicks = 520)
    public static void expiryRemovalAndReloadRevokeSources(GameTestHelper helper) {
        Fixture f = new Fixture(helper);
        BlockPos crop = f.crop(0, 0, Blocks.WHEAT);
        List<Mob> sources = f.group(crop, 4);
        f.active.clear();
        f.run(470, tick -> {
            if (tick == 20) check(HummingbirdPollinationService.activeSourcesNear(f.level, Vec3.atCenterOf(crop), 8) == 4,
                    "short flower-to-flower grace preserves effective sources");
            if (tick == 150) check(HummingbirdPollinationService.activeSourcesNear(f.level, Vec3.atCenterOf(crop), 8) == 0,
                    "unrefreshed activity expires after six seconds");
            if (tick == 170) {
                sources.forEach(HummingbirdPollinationService::activate);
                sources.forEach(HummingbirdPollinationService::remove);
                check(HummingbirdPollinationService.activeSourcesNear(f.level, Vec3.atCenterOf(crop), 8) == 0,
                        "explicit sleep/stay/panic removal is immediate");
                sources.forEach(HummingbirdPollinationService::activate);
                sources.forEach(Entity::discard);
                check(HummingbirdPollinationService.activeSourcesNear(f.level, Vec3.atCenterOf(crop), 8) == 0,
                        "entity removal revokes activity and its leases");
                f.group(crop, 4); f.active.clear();
                HummingbirdPollinationService.forgetLevel(f.level);
                check(HummingbirdPollinationService.activeSourcesNear(f.level, Vec3.atCenterOf(crop), 8) == 0,
                        "world reload cannot retain activity or accumulate offline rounds");
            }
            if (tick == 470) check(f.age(crop) == 0 && f.hooks.count(crop) == 0,
                    "expired, removed and unloaded sources produce no delayed crop rewards");
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_garden", timeoutTicks = 240)
    public static void gardenWeightsAndLoadedOnlyIndex(GameTestHelper helper) {
        Fixture f = new Fixture(helper);
        for (int i = 0; i < 60; i++) f.flower(f.origin.offset(i % 12 - 6, 0, i / 12 - 2), Blocks.POPPY);
        f.group(f.origin, 4);
        FlowerHabitatIndex.request(f.level, f.origin, 16);
        check(GardenAttraction.flowerMultiplier(4) == 0 && GardenAttraction.flowerMultiplier(5) == 1
                && GardenAttraction.flowerMultiplier(14) == 1 && GardenAttraction.flowerMultiplier(15) == 1.5
                && GardenAttraction.flowerMultiplier(29) == 1.5 && GardenAttraction.flowerMultiplier(30) == 2
                && GardenAttraction.flowerMultiplier(59) == 2 && GardenAttraction.flowerMultiplier(60) == 3,
                "flower thresholds are 5/15/30/60 plants");
        f.run(160, tick -> {
            if (tick == 120) {
                check(FlowerHabitatIndex.countFlowers(f.level, f.origin, 16) == 60
                        && GardenAttraction.hummingbirdSpawnMultiplier(f.level, f.origin) == 3, "real 60-flower garden gives local multiplier three");
                check(Math.abs(GardenAttraction.songbirdMultiplier(f.level, f.origin, BirdSpecies.SPARROW) - 1.15) < 0.0001,
                        "flowers plus four active birds cap the songbird bonus at 15 percent");
                check(GardenAttraction.songbirdMultiplier(f.level, f.origin, BirdSpecies.KESTREL) == 1,
                        "garden does not boost birds of prey");
                BlockPos far = f.origin.offset(4096, 0, 4096);
                Set<Long> before = readyChunks(f.level, far);
                check(before.size() < 16, "missing-chunk fixture is outside loaded test regions");
                FlowerHabitatIndex.request(f.level, far, 16);
                check(FlowerHabitatIndex.countFlowers(f.level, far, 16) == 0, "unloaded flowers are not fabricated");
                check(before.equals(readyChunks(f.level, far)), "index requests and queries never load missing chunks");
            }
        });
    }

    private static Set<Long> readyChunks(ServerLevel level, BlockPos center) {
        Set<Long> result = new HashSet<>();
        for (int x = (center.getX() - 17) >> 4; x <= (center.getX() + 17) >> 4; x++)
            for (int z = (center.getZ() - 17) >> 4; z <= (center.getZ() + 17) >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) != null) result.add(ChunkPos.asLong(x, z));
        return result;
    }

    @GameTest(template = "garden", batch = "hummingbird_budget", timeoutTicks = 520)
    public static void cropBudgetDefersFairlyAcrossGardens(GameTestHelper helper) {
        Fixture f = new Fixture(helper);
        f.day(6000);
        List<List<BlockPos>> gardens = new ArrayList<>();
        for (int garden = 0; garden < 4; garden++) {
            f.loadAround(f.origin.east(garden * 32));
            // The three outer gardens lie outside GameTest's cleared volume. Seal
            // their native fluid boundary so a generated lava pool cannot replace
            // the crops while we measure the scheduler, then restore it on cleanup.
            for (int x = -10; x <= 10; x++) for (int y = -2; y <= 5; y++) for (int z = -10; z <= 10; z++) {
                if (Math.abs(x) == 10 || Math.abs(z) == 10 || y == -2 || y == 5)
                    f.set(f.origin.offset(garden * 32 + x, y, z), Blocks.STONE.defaultBlockState());
            }
            // The four fields extend beyond the template's cleared skylight shaft. Build each
            // field's own real, unobstructed light space instead of assuming that noon means light.
            for (int x = -9; x <= 9; x++) for (int z = -9; z <= 9; z++) {
                for (int y = 0; y <= 3; y++)
                    f.set(f.origin.offset(garden * 32 + x, y, z), Blocks.AIR.defaultBlockState());
            }
            for (int x = -9; x <= 9; x += 3) for (int z = -9; z <= 9; z += 3)
                f.set(f.origin.offset(garden * 32 + x, 3, z), Blocks.GLOWSTONE.defaultBlockState());
            List<BlockPos> crops = new ArrayList<>();
            for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) {
                if (x * x + z * z > 49) continue;
                BlockPos pos = f.origin.offset(garden * 32 + x, 0, z);
                f.set(pos, Blocks.AIR.defaultBlockState());
                f.set(pos.below(), Blocks.FARMLAND.defaultBlockState());
                crops.add(pos);
            }
            gardens.add(crops);
            for (int source = 0; source < 4; source++) {
                // Sorted source order alternates gardens, producing more than 512 unique crops in one discovery frame.
                f.bird(Vec3.atCenterOf(f.origin.offset(garden * 32, 0, 0)).add(0, 1, 0),
                        new UUID(0, 1L + garden + source * 4));
            }
        }
        check(gardens.stream().mapToInt(List::size).sum() > HummingbirdConfig.cropBudget(),
                "fixture actually exceeds one crop-processing frame's budget");
        f.run(480, tick -> {
            if (tick == 30) {
                // Let native block lighting propagate through all four fields before planting.
                for (List<BlockPos> garden : gardens) for (BlockPos crop : garden) {
                    check(f.level.getRawBrightness(crop, 0) >= 8
                                    && Blocks.WHEAT.defaultBlockState().canSurvive(f.level, crop),
                            "prepared field supports wheat at " + crop + ", support=" + f.level.getBlockState(crop.below())
                                    + ", brightness=" + f.level.getRawBrightness(crop, 0));
                    f.set(crop, Blocks.WHEAT.defaultBlockState());
                    FlowerHabitatIndex.invalidate(f.level, crop);
                }
            }
            if (tick == 40) for (List<BlockPos> garden : gardens) for (BlockPos crop : garden)
                check(f.age(crop) == 0, "all 596 fixture crops survive native updates before the first pollination round");
            if (tick == 480) {
                for (int garden = 0; garden < gardens.size(); garden++) for (BlockPos crop : gardens.get(garden)) {
                    check(f.age(crop) > 0 && f.hooks.count(crop) == 1,
                            "garden " + garden + " crop " + crop + " receives one deferred action without starvation or catch-up duplicates");
                }
            }
        });
    }
}
