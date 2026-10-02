package EdDYON.guaniao.content.bird.visibility;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.BirdVisibility;
import EdDYON.guaniao.event.BirdPopulationTracker;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("guaniao_visibility_qa")
@PrefixGameTestTemplate(false)
public final class BirdVisibilityGameTests {
    private static final String LAST_NEARBY = "GuaniaoLastNearbyPlayerTime";
    private static final String FLYBY_SPAWN = "GuaniaoTransientFlybySpawnTime";
    private static final Set<BirdSpecies> VANILLA_DESPAWN = Set.of(
            BirdSpecies.NIGHT_HERON, BirdSpecies.KIWI, BirdSpecies.CASSOWARY, BirdSpecies.HUMMINGBIRD);

    private BirdVisibilityGameTests() { }

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    /** Synchronous fixtures avoid navigation, world ticking, and another test's players affecting distance checks. */
    private static final class Fixture implements AutoCloseable {
        final ServerLevel level;
        final Vec3 origin;
        final FakePlayer observer;
        final List<Mob> birds = new ArrayList<>();

        Fixture(GameTestHelper helper) {
            level = helper.getLevel();
            BlockPos anchor = helper.absolutePos(BlockPos.ZERO);
            // Above the generated test structures: long horizontal sight lines do not cross other fixtures.
            origin = new Vec3(anchor.getX() + 0.5, level.getMaxBuildHeight() - 20, anchor.getZ() + 0.5);
            observer = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "VisibilityQA"));
            observer.moveTo(origin.x, origin.y, origin.z, 0, 0);
            level.addNewPlayer(observer);
        }

        Mob bird(BirdSpecies species) {
            Entity entity = species.entityType().create(level);
            check(entity instanceof Mob, species.id() + " creates a real bird Mob");
            Mob mob = (Mob) entity;
            mob.moveTo(origin.x, origin.y, origin.z, 0, 0);
            birds.add(mob);
            return mob;
        }

        Mob trackedBird(BirdSpecies species) {
            Mob bird = bird(species);
            check(level.addFreshEntity(bird), species.id() + " joins the test world");
            return bird;
        }

        void observerAt(double dx, double dy, double dz) {
            observer.moveTo(origin.x + dx, origin.y + dy, origin.z + dz, 0, 0);
        }

        @Override public void close() {
            for (Mob bird : birds) if (!bird.isRemoved()) bird.discard();
            level.removePlayerImmediately(observer, Entity.RemovalReason.DISCARDED);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void allSpeciesRenderDistanceAndHeight(GameTestHelper helper) {
        check(BirdSpecies.values().length == 17, "visibility suite covers all 17 registered species");
        check(BirdVisibility.BASE_RENDER_DISTANCE == 192, "normal entity distance is 192 blocks");
        double previousScale = Entity.getViewScale();
        try (Fixture fixture = new Fixture(helper)) {
            for (BirdSpecies species : BirdSpecies.values()) {
                Mob bird = fixture.bird(species);
                check(!bird.noCulling, species.id() + " keeps native frustum culling enabled");
                for (double scale : new double[] {0.5, 1, 2}) {
                    Entity.setViewScale(scale);
                    double radius = 192 * scale;
                    for (double distance : new double[] {0, 32, radius - 0.25, radius, radius + 0.25}) {
                        boolean expected = distance < radius;
                        String label = species.id() + " scale=" + scale + " distance=" + distance;
                        check(BirdVisibility.shouldRender(distance * distance, scale) == expected,
                                "shared policy: " + label);
                        check(bird.shouldRenderAtSqrDistance(distance * distance) == expected,
                                "registered entity dispatch: " + label);
                        check(bird.shouldRender(bird.getX() + distance, bird.getY(), bird.getZ()) == expected,
                                "horizontal distance: " + label);
                        check(bird.shouldRender(bird.getX(), bird.getY() + distance, bird.getZ()) == expected,
                                "vertical flight distance: " + label);
                    }
                    // 3:4:5 offsets ensure altitude participates in the same spherical limit.
                    for (double distance : new double[] {radius - 1, radius + 1}) {
                        check(bird.shouldRender(bird.getX() + distance * 0.6,
                                        bird.getY() + distance * 0.8, bird.getZ()) == (distance < radius),
                                species.id() + " combined horizontal/vertical distance at scale " + scale);
                    }
                }
                System.out.println("VISIBILITY_QA PASS render/height/scales: " + species.id());
            }
        } finally {
            Entity.setViewScale(previousScale);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void registrationAndNonBirdScope(GameTestHelper helper) {
        check(BirdVisibility.TRACKING_RANGE_CHUNKS == 16 && BirdVisibility.TRACKING_DISTANCE == 256,
                "network tracking budget is 16 chunks / 256 blocks");
        for (BirdSpecies species : BirdSpecies.values()) {
            check(species.entityType().clientTrackingRange() == 16, species.id() + " tracks to 16 chunks");
        }
        check(GuaniaoEntityTypes.EARTHWORM.get().clientTrackingRange() == 6, "earthworm remains 6 chunks");
        check(GuaniaoEntityTypes.PHOTOGRAPH.get().clientTrackingRange() == 10, "photograph remains 10 chunks");
        check(GuaniaoEntityTypes.BIRD_DROPPING_PROJECTILE.get().clientTrackingRange() == 4,
                "dropping projectile remains 4 chunks");
        check(GuaniaoEntityTypes.BIRD_DROPPING_SPLAT.get().clientTrackingRange() == 8,
                "dropping splat remains 8 chunks");
        check(GuaniaoEntityTypes.FEATHER_FAN_PROJECTILE.get().clientTrackingRange() == 8,
                "fan projectile remains 8 chunks");
        System.out.println("VISIBILITY_QA PASS tracking: all 17 birds; all 5 non-bird ranges unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void realVanillaDespawnBoundary(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            for (BirdSpecies species : BirdSpecies.values()) {
                Mob bird = fixture.bird(species);
                check(!bird.removeWhenFarAway(256 * 256), species.id() + " survives inside tracking distance");
                check(bird.removeWhenFarAway(257 * 257) == VANILLA_DESPAWN.contains(species),
                        species.id() + " keeps its original wild despawn rule outside tracking distance");
            }
            for (BirdSpecies species : VANILLA_DESPAWN) {
                for (double distance : new double[] {100, 129, 192, 255.75, 256, 256.25}) {
                    Mob bird = fixture.bird(species);
                    fixture.observerAt(distance, 0, 0);
                    check(fixture.level.getNearestPlayer(bird, -1) == fixture.observer,
                            "fixture has only the intended nearest observer");
                    // Exercise both the immediate 128-block path and the idle random despawn path.
                    bird.setNoActionTime(601);
                    bird.getRandom().setSeed(idleDespawnSeed());
                    bird.checkDespawn();
                    check(bird.isRemoved() == (distance > 256),
                            species.id() + " actual checkDespawn at horizontal distance " + distance);
                }
                for (double distance : new double[] {192, 256, 257}) {
                    Mob bird = fixture.bird(species);
                    fixture.observerAt(0, -distance, 0);
                    bird.checkDespawn();
                    check(bird.isRemoved() == (distance > 256),
                            species.id() + " actual checkDespawn at vertical distance " + distance);
                }
                System.out.println("VISIBILITY_QA PASS real despawn boundaries: " + species.id());
            }
            fixture.observerAt(257, 0, 0);
            Mob persistent = fixture.bird(BirdSpecies.KIWI);
            persistent.setPersistenceRequired();
            persistent.checkDespawn();
            check(!persistent.isRemoved(), "explicit persistence survives outside the expanded range");
            TamableAnimal pet = (TamableAnimal) fixture.bird(BirdSpecies.NIGHT_HERON);
            pet.tame(fixture.observer);
            pet.checkDespawn();
            check(!pet.isRemoved() && !pet.removeWhenFarAway(1024 * 1024),
                    "tamed night heron remains protected even outside the expanded range");
        }
        helper.succeed();
    }

    private static long idleDespawnSeed() {
        for (long seed = 0; seed < 100000; seed++) {
            if (RandomSource.create(seed).nextInt(800) == 0) return seed;
        }
        throw new GameTestAssertException("cannot construct an idle despawn roll");
    }

    /** Tests the public lifecycle entry point with expired saved timestamps, without waiting 20 minutes. */
    private static boolean expiredCull(Mob bird, boolean flyby) {
        long now = bird.level().getGameTime();
        bird.getPersistentData().putLong(LAST_NEARBY, now - BirdConfigManager.wildBirdDespawnTicks());
        if (flyby) {
            BirdPopulationTracker.markTransientFlyby(bird);
            bird.getPersistentData().putLong(FLYBY_SPAWN, now - BirdConfigManager.flybyBirdLifetimeTicks());
            // Isolate the short-lived flyby's expiry from the ordinary 20-minute expiry.
            bird.getPersistentData().putLong(LAST_NEARBY, now);
        }
        bird.tickCount = Math.floorMod(-bird.getId(), 100);
        return BirdPopulationTracker.tickBird(bird);
    }

    private static boolean clearRay(Fixture fixture, Mob bird) {
        return fixture.level.clip(new ClipContext(fixture.observer.getEyePosition(), bird.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, fixture.observer)).getType() == HitResult.Type.MISS;
    }

    private static Set<Long> sightChunkPositions(Fixture fixture, Mob bird) {
        Vec3 a = fixture.observer.getEyePosition(), b = bird.getEyePosition();
        BlockPos min = BlockPos.containing(Math.min(a.x, b.x) - 1, Math.min(a.y, b.y), Math.min(a.z, b.z) - 1);
        BlockPos max = BlockPos.containing(Math.max(a.x, b.x) + 1, Math.max(a.y, b.y), Math.max(a.z, b.z) + 1);
        Set<Long> positions = new HashSet<>();
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) {
            for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) {
                positions.add(ChunkPos.asLong(x, z));
            }
        }
        return positions;
    }

    private static Set<Long> loadedSightChunks(Fixture fixture, Mob bird) {
        Set<Long> loaded = new HashSet<>();
        for (long position : sightChunkPositions(fixture, bird)) {
            if (fixture.level.getChunkSource().getChunkNow(ChunkPos.getX(position), ChunkPos.getZ(position)) != null) {
                loaded.add(position);
            }
        }
        return loaded;
    }

    private static boolean sightChunksLoaded(Fixture fixture, Mob bird) {
        return loadedSightChunks(fixture, bird).equals(sightChunkPositions(fixture, bird));
    }

    /** Only fixture preparation requests chunks; the production cleanup check must not do so. */
    private static void loadSightChunks(Fixture fixture, Mob bird) {
        for (long position : sightChunkPositions(fixture, bird)) {
            fixture.level.getChunk(ChunkPos.getX(position), ChunkPos.getZ(position));
        }
        check(sightChunksLoaded(fixture, bird), "fixture has every sight chunk ready including one-block padding");
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void expiredBirdsRespectExtendedSightAndProtection(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.observerAt(191, 0, 0);
            for (BirdSpecies species : BirdSpecies.values()) {
                Mob bird = fixture.trackedBird(species);
                loadSightChunks(fixture, bird);
                check(clearRay(fixture, bird), species.id() + " unobstructed 191-block fixture");
                check(!expiredCull(bird, false), species.id() + " remains visible after ordinary expiry");
                bird.discard();
            }
            for (boolean flyby : new boolean[] {false, true}) {
                Mob bird = fixture.trackedBird(BirdSpecies.SPARROW);
                for (double distance : new double[] {64, 127, 129, 191, 256}) {
                    fixture.observerAt(distance, 0, 0);
                    loadSightChunks(fixture, bird);
                    check(clearRay(fixture, bird), "clear sight line at " + distance);
                    check(!expiredCull(bird, flyby), "expired flyby=" + flyby + " visible at " + distance);
                }
                fixture.observerAt(257, 0, 0);
                check(expiredCull(bird, flyby), "expired flyby=" + flyby + " can be cleaned beyond 256");

                fixture.observerAt(191, 0, 0);
                loadSightChunks(fixture, bird);
                BlockPos wall = BlockPos.containing(fixture.observer.getEyePosition().lerp(bird.getEyePosition(), 0.5));
                BlockState previous = fixture.level.getBlockState(wall);
                try {
                    fixture.level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
                    check(!clearRay(fixture, bird), "solid block actually occludes the sight line");
                    check(sightChunksLoaded(fixture, bird), "occlusion fixture has the complete sight corridor loaded");
                    check(expiredCull(bird, flyby), "occluded expired flyby=" + flyby + " still permits ecological cleanup");
                } finally {
                    fixture.level.setBlockAndUpdate(wall, previous);
                }
                bird.discard();
            }

            Mob elevated = fixture.trackedBird(BirdSpecies.SPARROW);
            fixture.observerAt(120, -160, 0);
            loadSightChunks(fixture, elevated);
            check(clearRay(fixture, elevated), "200-block diagonal high-flight sight line is unobstructed");
            check(!expiredCull(elevated, false), "height and horizontal separation preserve a visible high-flying bird");
            elevated.discard();

            fixture.observerAt(257, 0, 0);
            Mob named = fixture.trackedBird(BirdSpecies.SPARROW);
            named.setCustomName(Component.literal("Protected visibility test"));
            check(!expiredCull(named, true), "named birds retain lifecycle protection");
            Mob persistent = fixture.trackedBird(BirdSpecies.KIWI);
            persistent.setPersistenceRequired();
            check(!expiredCull(persistent, true), "persistent birds retain lifecycle protection");
            Mob noAi = fixture.trackedBird(BirdSpecies.SPARROW);
            noAi.setNoAi(true);
            check(!expiredCull(noAi, true), "NoAI birds retain custom cleanup protection");
            TamableAnimal pet = (TamableAnimal) fixture.trackedBird(BirdSpecies.NIGHT_HERON);
            pet.tame(fixture.observer);
            check(!expiredCull(pet, true), "tamed birds retain lifecycle protection");
        }
        System.out.println("VISIBILITY_QA PASS expired lifecycle: sight, occlusion, flyby, name, persistence, pet");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void missingSightChunksPreserveBirdWithoutLoading(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            Mob bird = fixture.bird(BirdSpecies.SPARROW);
            boolean foundUnloaded = false;
            for (int offset = 1024; offset <= 8192; offset += 1024) {
                bird.moveTo(fixture.origin.x + offset, fixture.origin.y, fixture.origin.z, 0, 0);
                fixture.observerAt(offset + 191, 0, 0);
                if (!sightChunksLoaded(fixture, bird)) {
                    foundUnloaded = true;
                    break;
                }
            }
            check(foundUnloaded, "fixture locates an unloaded 191-block sight corridor");
            // Deliberately load only one end, so the assertion covers partial loading as well as an empty corridor.
            fixture.level.getChunk(bird.blockPosition().getX() >> 4, bird.blockPosition().getZ() >> 4);
            check(fixture.level.addFreshEntity(bird), "bird joins without generating its full sight corridor");
            check(!sightChunksLoaded(fixture, bird), "sight corridor is still incomplete after entity registration");
            Set<Long> loadedBefore = loadedSightChunks(fixture, bird);
            check(!loadedBefore.isEmpty(), "missing-chunk fixture has a partially loaded corridor");
            check(!expiredCull(bird, false), "missing chunks conservatively preserve an expired ordinary bird");
            check(loadedBefore.equals(loadedSightChunks(fixture, bird)),
                    "ordinary cleanup does not load even a subset of missing sight chunks");
            check(!expiredCull(bird, true), "missing chunks conservatively preserve an expired flyby bird");
            check(loadedBefore.equals(loadedSightChunks(fixture, bird)),
                    "flyby cleanup does not load even a subset of missing sight chunks");
        }
        System.out.println("VISIBILITY_QA PASS missing sight chunks: retained without chunk generation");
        helper.succeed();
    }
}
