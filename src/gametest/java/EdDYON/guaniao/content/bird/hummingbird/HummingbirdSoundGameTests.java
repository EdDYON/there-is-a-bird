package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.config.BirdConfigData;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.config.BirdSpeciesConfig;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Actual registered entities and Forge sound emission; isolated from unrelated bird suites. */
@GameTestHolder("guaniao_hummingbird_sound_qa")
@PrefixGameTestTemplate(false)
public final class HummingbirdSoundGameTests {
    private record Call(long tick, float volume, float pitch) { }

    private static final class SoundProbe {
        final HummingbirdEntity bird;
        final SoundEvent sound;
        final List<Call> calls = new ArrayList<>();
        SoundProbe(HummingbirdEntity bird, SoundEvent sound) { this.bird = bird; this.sound = sound; }
        @SubscribeEvent public void heard(PlayLevelSoundEvent event) {
            if (event.getLevel() != bird.level() || event.getSound() == null
                    || event.getSound().value() != sound) return;
            Vec3 at = event instanceof PlayLevelSoundEvent.AtEntity entity ? entity.getEntity().position()
                    : event instanceof PlayLevelSoundEvent.AtPosition position ? position.getPosition() : null;
            if (at == null || at.distanceToSqr(bird.position()) > .0001) return;
            check(event.getSource() == SoundSource.NEUTRAL, "Bird calls use the neutral creature volume channel");
            check(event.getOriginalPitch() >= .95F && event.getOriginalPitch() <= 1.05F,
                    "Call pitch cannot stretch the short clips beyond the overlap budget");
            calls.add(new Call(event.getLevel().getGameTime(), event.getOriginalVolume(), event.getOriginalPitch()));
        }
    }

    /** Observes the real PlayerList broadcast, without an audio device or a simulated call. */
    private static final class PacketProbe extends ServerGamePacketListenerImpl {
        final List<ClientboundSoundPacket> calls = new ArrayList<>();
        final List<ClientboundSoundPacket> hurts = new ArrayList<>();
        final List<ClientboundSoundPacket> deaths = new ArrayList<>();
        final List<ClientboundSoundPacket> interactions = new ArrayList<>();
        PacketProbe(ServerLevel level, ServerPlayer player) {
            super(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player);
        }
        @Override public void send(Packet<?> packet) {
            if (!(packet instanceof ClientboundSoundPacket packetSound)) return;
            SoundEvent event = packetSound.getSound().value();
            if (event == GuaniaoSoundEvents.HUMMINGBIRD_AMBIENT.get()) calls.add(packetSound);
            else if (event == GuaniaoSoundEvents.HUMMINGBIRD_HURT.get()) hurts.add(packetSound);
            else if (event == GuaniaoSoundEvents.HUMMINGBIRD_DEATH.get()) deaths.add(packetSound);
            else if (event == GuaniaoSoundEvents.HUMMINGBIRD_INTERACT.get()) interactions.add(packetSound);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final ServerLevel level;
        final Vec3 origin;
        final List<Entity> entities = new ArrayList<>();
        final Map<HummingbirdEntity, Vec3> positions = new HashMap<>();
        final List<SoundProbe> probes = new ArrayList<>();
        final Map<FakePlayer, ServerGamePacketListenerImpl> originalConnections = new HashMap<>();
        List<ServerPlayer> broadcastPlayers;
        List<ServerPlayer> originalBroadcastPlayers;
        final Field configField;
        final BirdConfigData original, config;
        final BirdSpeciesConfig soundConfig;
        boolean closed;

        Fixture(GameTestHelper helper) throws ReflectiveOperationException {
            this.helper = helper; level = helper.getLevel();
            origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 5, 4)));
            configField = BirdConfigManager.class.getDeclaredField("config");
            configField.setAccessible(true);
            original = (BirdConfigData)configField.get(null); config = original.copy();
            soundConfig = new BirdSpeciesConfig(BirdSpecies.HUMMINGBIRD);
            soundConfig.ambientSoundCooldownMultiplier = .25;
            soundConfig.soundVolumeMultiplier = 1;
            config.birds.put(BirdSpecies.HUMMINGBIRD.id(), soundConfig);
            config.global.soundVolumeMultiplier = 1;
            configField.set(null, config);
        }
        HummingbirdEntity bird(Vec3 offset) {
            HummingbirdEntity bird = GuaniaoEntityTypes.HUMMINGBIRD.get().create(level);
            check(bird != null, "Actual registered hummingbird is constructible");
            Vec3 at = origin.add(offset);
            bird.moveTo(at.x, at.y, at.z); bird.setNoAi(true); bird.setNoGravity(true); bird.noPhysics = true;
            bird.setPersistenceRequired(); bird.setSilent(false); bird.activity(HummingbirdEntity.Activity.HOVER);
            check(level.addFreshEntity(bird), "Bird is present in the actual level");
            entities.add(bird); positions.put(bird, at); return bird;
        }
        SoundProbe listen(HummingbirdEntity bird) {
            return listen(bird, GuaniaoSoundEvents.HUMMINGBIRD_AMBIENT.get());
        }
        SoundProbe listen(HummingbirdEntity bird, SoundEvent sound) {
            SoundProbe probe = new SoundProbe(bird, sound); probes.add(probe); MinecraftForge.EVENT_BUS.register(probe); return probe;
        }
        FakePlayer owner() {
            FakePlayer owner = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "HummerSoundOwner"));
            owner.getAbilities().instabuild = false;
            owner.moveTo(origin.x + 2, origin.y, origin.z); level.addNewPlayer(owner); entities.add(owner); return owner;
        }
        @SuppressWarnings("unchecked")
        PacketProbe listener(Vec3 offset) throws ReflectiveOperationException {
            if (broadcastPlayers == null) {
                Field players = PlayerList.class.getDeclaredField("players");
                players.setAccessible(true);
                broadcastPlayers = (List<ServerPlayer>)players.get(level.getServer().getPlayerList());
                originalBroadcastPlayers = List.copyOf(broadcastPlayers);
            }
            FakePlayer listener = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "HummerSoundListener"));
            Vec3 at = origin.add(offset);
            listener.moveTo(at.x, at.y, at.z);
            PacketProbe capture = new PacketProbe(level, listener);
            originalConnections.put(listener, listener.connection);
            listener.connection = capture;
            entities.add(listener);
            level.addNewPlayer(listener);
            broadcastPlayers.add(listener);
            return capture;
        }
        void run(int ticks, IntConsumer action) {
            for (int t = 1; t <= ticks; t++) {
                int tick = t;
                helper.runAtTickTime(tick, () -> {
                    if (closed) return;
                    try {
                        // This fixture isolates sound emission/state; it does not substitute for flight AI tests.
                        positions.forEach((bird, at) -> {
                            if (!bird.isRemoved() && !bird.isPassenger()) {
                                bird.setPos(at.x, at.y, at.z); bird.setDeltaMovement(Vec3.ZERO);
                            }
                        });
                        action.accept(tick); if (tick == ticks) { close(); helper.succeed(); }
                    }
                    catch (RuntimeException | Error failure) { close(); throw failure; }
                });
            }
        }
        @Override public void close() {
            if (closed) return; closed = true;
            probes.forEach(MinecraftForge.EVENT_BUS::unregister);
            if (broadcastPlayers != null) {
                broadcastPlayers.removeAll(originalConnections.keySet());
            }
            originalConnections.forEach((player, connection) -> player.connection = connection);
            entities.forEach(Entity::discard);
            try { configField.set(null, original); }
            catch (IllegalAccessException failure) { throw new AssertionError("Restore original bird sound settings", failure); }
            if (broadcastPlayers != null)
                check(broadcastPlayers.equals(originalBroadcastPlayers), "Restore the exact original server broadcast player list");
        }
    }

    @GameTest(template = "garden", batch = "hummingbird_sound_states", timeoutTicks = 50)
    public static void wingStateAndRegisteredEvents(GameTestHelper helper) throws Exception {
        Fixture f = new Fixture(helper);
        HummingbirdEntity bird = f.bird(Vec3.ZERO);
        SoundProbe probe = f.listen(bird);
        f.run(2, tick -> {
            if (tick != 1) return;
            for (String path : new String[]{"entity.hummingbird.ambient", "entity.hummingbird.wing",
                    "entity.hummingbird.hurt", "entity.hummingbird.death", "entity.hummingbird.interact"}) {
                var event = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("guaniao", path));
                check(event != null && event.getLocation().getPath().equals(path), "Hummingbird sound is actually registered: " + path);
                SoundEvent expected = switch (path.substring(path.lastIndexOf('.') + 1)) {
                    case "ambient" -> GuaniaoSoundEvents.HUMMINGBIRD_AMBIENT.get();
                    case "wing" -> GuaniaoSoundEvents.HUMMINGBIRD_WING.get();
                    case "hurt" -> GuaniaoSoundEvents.HUMMINGBIRD_HURT.get();
                    case "death" -> GuaniaoSoundEvents.HUMMINGBIRD_DEATH.get();
                    default -> GuaniaoSoundEvents.HUMMINGBIRD_INTERACT.get();
                };
                check(event == expected, "Runtime event is the registered entry");
            }
            bird.setDeltaMovement(Vec3.ZERO);
            for (var state : new HummingbirdEntity.Activity[]{HummingbirdEntity.Activity.HOVER, HummingbirdEntity.Activity.TRAVEL,
                    HummingbirdEntity.Activity.NECTAR_ENTER, HummingbirdEntity.Activity.NECTAR_LOOP, HummingbirdEntity.Activity.NECTAR_EXIT,
                    HummingbirdEntity.Activity.TAKEOFF, HummingbirdEntity.Activity.LAND, HummingbirdEntity.Activity.DROP_SEED}) {
                bird.activity(state);
                check(bird.isWingSoundActive(), "Airborne " + state + " flaps even with zero horizontal motion");
            }
            for (var state : new HummingbirdEntity.Activity[]{HummingbirdEntity.Activity.PERCH, HummingbirdEntity.Activity.SLEEP_ENTER,
                    HummingbirdEntity.Activity.SLEEP, HummingbirdEntity.Activity.WAKE, HummingbirdEntity.Activity.HEAD_PERCH}) {
                bird.activity(state); check(!bird.isWingSoundActive(), "Perching/rest state stops the wing loop: " + state);
            }
            bird.activity(HummingbirdEntity.Activity.SLEEP); bird.playAmbientSound();
            check(probe.calls.isEmpty(), "Sleeping bird sends no ambient sound packet");
            bird.activity(HummingbirdEntity.Activity.HOVER); bird.setSilent(true); bird.playAmbientSound();
            check(!bird.isWingSoundActive() && probe.calls.isEmpty(), "Silent entity emits neither call nor wing request");
            bird.setSilent(false); bird.setGuidePreviewAnimation("fly"); bird.playAmbientSound();
            check(!bird.isWingSoundActive() && probe.calls.isEmpty(), "Handbook preview never becomes an audible world bird");
            bird.setGuidePreviewAnimation(null);
            FakePlayer owner = f.owner(); bird.tame(owner);
            check(bird.startRiding(owner, true), "Head-perch scenario has a real passenger relation");
            check(!bird.isWingSoundActive(), "Actual head passenger stops wings even before its visual activity packet changes");
            bird.stopRiding(); bird.activity(HummingbirdEntity.Activity.HOVER);
            check(bird.isWingSoundActive(), "Leaving the head and resuming flight can restart wings");
            bird.discard(); check(!bird.isWingSoundActive(), "Removed entity cannot retain a wing loop");
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_sound_overlap", timeoutTicks = 340)
    public static void fastestConfiguredCallsDoNotOverlapOrShareCooldown(GameTestHelper helper) throws Exception {
        Fixture f = new Fixture(helper);
        HummingbirdEntity first = f.bird(Vec3.ZERO), second = f.bird(new Vec3(2, 0, 0));
        SoundProbe a = f.listen(first), b = f.listen(second);
        f.run(300, tick -> {
            first.playAmbientSound(); first.playAmbientSound(); second.playAmbientSound();
            if (tick == 1) check(a.calls.size() == 1 && b.calls.size() == 1, "Duplicate request is suppressed without muting another bird");
            if (tick == 300) {
                for (SoundProbe probe : new SoundProbe[]{a, b}) {
                    check(probe.calls.size() >= 4, "Shortest permitted interval continues playing rather than silencing calls forever");
                    for (int i = 1; i < probe.calls.size(); i++)
                        check(probe.calls.get(i).tick - probe.calls.get(i - 1).tick >= 60,
                                "Real emitted calls keep the full overlap guard even under repeated requests");
                }
                System.out.println("HUMMINGBIRD_SOUND_QA callsA=" + a.calls + " callsB=" + b.calls);
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_sound_natural", timeoutTicks = 650)
    public static void naturalEntityTicksEmitCallsWithoutManualRequests(GameTestHelper helper) throws Exception {
        Fixture f = new Fixture(helper);
        f.soundConfig.ambientSoundCooldownMultiplier = 1;
        HummingbirdEntity flying = f.bird(Vec3.ZERO), resting = f.bird(new Vec3(2, 0, 0));
        flying.setNoAi(false);
        flying.getRandom().setSeed(531);
        resting.getRandom().setSeed(947);
        resting.activity(HummingbirdEntity.Activity.PERCH);
        SoundProbe a = f.listen(flying), b = f.listen(resting);
        f.run(600, tick -> {
            // No playAmbientSound(), baseTick() or tick() call here: the actual level owns ticking.
            if (tick == 600) {
                for (SoundProbe probe : new SoundProbe[]{a, b}) {
                    check(probe.bird.tickCount >= 590, "Registered bird was naturally ticked by the level");
                    check(probe.calls.size() >= 2, "Natural ticking emits repeated calls without a manual request");
                    for (int i = 1; i < probe.calls.size(); i++)
                        check(probe.calls.get(i).tick - probe.calls.get(i - 1).tick >= 240,
                                "Natural calls preserve the configured ordinary ambient interval");
                }
                System.out.println("HUMMINGBIRD_NATURAL_SOUND_QA flying=" + a.calls + " resting=" + b.calls);
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_sound_natural_wake", timeoutTicks = 850)
    public static void naturalTicksResumeAfterSleepAndConfigurationMute(GameTestHelper helper) throws Exception {
        Fixture f = new Fixture(helper);
        f.soundConfig.ambientSoundCooldownMultiplier = 1;
        HummingbirdEntity bird = f.bird(Vec3.ZERO);
        bird.getRandom().setSeed(724);
        bird.activity(HummingbirdEntity.Activity.SLEEP);
        SoundProbe probe = f.listen(bird);
        int[] callsBeforeMute = {-1};
        f.run(800, tick -> {
            if (tick == 150) {
                check(probe.calls.isEmpty(), "Natural sleeping entity emits no call");
                bird.activity(HummingbirdEntity.Activity.HOVER);
            }
            if (tick == 450) {
                check(!probe.calls.isEmpty(), "Natural ticking resumes calls after waking");
                callsBeforeMute[0] = probe.calls.size();
                f.soundConfig.soundVolumeMultiplier = 0;
            }
            if (tick == 550) {
                check(probe.calls.size() == callsBeforeMute[0], "Naturally ticking bird respects the configuration mute window");
                f.soundConfig.soundVolumeMultiplier = 1;
            }
            if (tick == 800) {
                check(probe.calls.size() > callsBeforeMute[0],
                        "Natural ticking resumes after configuration is unmuted");
                System.out.println("HUMMINGBIRD_NATURAL_WAKE_SOUND_QA calls=" + probe.calls);
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_sound_network", timeoutTicks = 420)
    public static void naturalCallsReachGardenObserversThroughActualSoundPackets(GameTestHelper helper) throws Exception {
        Fixture f = new Fixture(helper);
        try {
            f.soundConfig.ambientSoundCooldownMultiplier = 1;
            HummingbirdEntity bird = f.bird(Vec3.ZERO);
            bird.getRandom().setSeed(337);
            SoundProbe emissions = f.listen(bird);
            PacketProbe garden = f.listener(new Vec3(20, 0, 0));
            PacketProbe edge = f.listener(new Vec3(31, 0, 0));
            PacketProbe outside = f.listener(new Vec3(34, 0, 0));
            f.run(360, tick -> {
                if (tick != 360) return;
                check(emissions.calls.size() >= 2, "Network check observes repeated naturally triggered calls");
                check(garden.calls.size() == emissions.calls.size() && edge.calls.size() == emissions.calls.size(),
                        "Both 20- and 31-block observers receive every actual ambient packet");
                check(outside.calls.isEmpty(), "Observer beyond the garden sound radius receives no ambient packets");
                check(GuaniaoSoundEvents.HUMMINGBIRD_AMBIENT.get().getRange(.65F) == 32,
                        "Server sound event has a fixed 32-block broadcast radius");
                for (ClientboundSoundPacket packet : garden.calls) {
                    check(packet.getSource() == SoundSource.NEUTRAL && Math.abs(packet.getVolume() - .65F) < .0001,
                            "Actual sound packets retain the chosen creature volume and channel");
                    FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
                    try {
                        packet.write(encoded);
                        ClientboundSoundPacket decoded = new ClientboundSoundPacket(encoded);
                        check(decoded.getSound().value() == GuaniaoSoundEvents.HUMMINGBIRD_AMBIENT.get(),
                                "Network registry holder round-trip retains the actual registered hummingbird sound");
                        check(decoded.getSource() == packet.getSource() && decoded.getVolume() == packet.getVolume()
                                        && decoded.getPitch() == packet.getPitch(),
                                "Network encoding preserves source, loudness and pitch");
                    } finally { encoded.release(); }
                }
                System.out.println("HUMMINGBIRD_SOUND_NETWORK_QA naturallyEmitted=" + emissions.calls.size()
                        + " at20=" + garden.calls.size() + " at31=" + edge.calls.size() + " at34=" + outside.calls.size());
            });
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            f.close();
            throw failure;
        }
    }

    @GameTest(template = "garden", batch = "hummingbird_sound_config", timeoutTicks = 110)
    public static void soundConfigurationAndSleepSuppressRealPlayback(GameTestHelper helper) throws Exception {
        Fixture f = new Fixture(helper);
        HummingbirdEntity muted = f.bird(Vec3.ZERO);
        HummingbirdEntity sleeping = f.bird(new Vec3(2, 0, 0));
        SoundProbe a = f.listen(muted), b = f.listen(sleeping);
        f.soundConfig.soundVolumeMultiplier = 0;
        f.run(85, tick -> {
            if (tick < 20) {
                muted.playAmbientSound(); sleeping.playAmbientSound();
                check(a.calls.isEmpty() && b.calls.isEmpty(), "Zero species volume sends no ambient packets");
            }
            if (tick == 20) {
                f.soundConfig.soundVolumeMultiplier = 1;
                sleeping.activity(HummingbirdEntity.Activity.SLEEP_ENTER);
                muted.playAmbientSound(); sleeping.playAmbientSound();
                check(a.calls.size() == 1 && b.calls.isEmpty(), "Unmuted bird calls while sleeping bird remains silent");
                muted.setSilent(true);
            }
            if (tick > 20 && tick < 85) {
                muted.playAmbientSound(); sleeping.playAmbientSound();
                check(a.calls.size() == 1 && b.calls.isEmpty(), "Silent/sleep guards survive past the call cooldown");
            }
            if (tick == 85) {
                f.soundConfig.soundVolumeMultiplier = .5;
                muted.setSilent(false); sleeping.activity(HummingbirdEntity.Activity.PERCH);
                muted.playAmbientSound(); sleeping.playAmbientSound();
                check(a.calls.size() == 2 && b.calls.size() == 1, "Unmuting/waking restores independent ordinary calls");
                check(Math.abs(a.calls.get(1).volume * 2 - a.calls.get(0).volume) < .0001,
                        "Species volume is applied exactly once to actual ambient playback");
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_sound_damage_feedback", timeoutTicks = 100)
    public static void realDamageAndDeathBypassAmbientLimitsAndHonorMute(GameTestHelper helper) throws Exception {
        Fixture f = new Fixture(helper);
        try {
            HummingbirdEntity bird = f.bird(Vec3.ZERO), flockMate = f.bird(new Vec3(.1, 0, 0));
            HummingbirdEntity sleeping = f.bird(new Vec3(2, 0, 0)), protectedBird = f.bird(new Vec3(4, 0, 0));
            HummingbirdEntity silent = f.bird(new Vec3(6, 0, 0)), muted = f.bird(new Vec3(8, 0, 0));
            sleeping.activity(HummingbirdEntity.Activity.SLEEP);
            protectedBird.setInvulnerable(true);
            silent.setSilent(true);
            SoundProbe hurt = f.listen(bird, GuaniaoSoundEvents.HUMMINGBIRD_HURT.get());
            SoundProbe death = f.listen(bird, GuaniaoSoundEvents.HUMMINGBIRD_DEATH.get());
            SoundProbe sleepHurt = f.listen(sleeping, GuaniaoSoundEvents.HUMMINGBIRD_HURT.get());
            SoundProbe protectedHurt = f.listen(protectedBird, GuaniaoSoundEvents.HUMMINGBIRD_HURT.get());
            SoundProbe silentHurt = f.listen(silent, GuaniaoSoundEvents.HUMMINGBIRD_HURT.get());
            SoundProbe mutedHurt = f.listen(muted, GuaniaoSoundEvents.HUMMINGBIRD_HURT.get());
            PacketProbe listener = f.listener(new Vec3(20, 0, 0));
            f.run(45, tick -> {
                if (tick == 1) {
                    // Populate both ordinary ambient guards; feedback still comes from real damage.
                    bird.playAmbientSound(); flockMate.playAmbientSound();
                    check(bird.hurt(f.level.damageSources().generic(), 1), "Actual damage is accepted");
                    check(hurt.calls.size() == 1 && listener.hurts.size() == 1,
                            "Accepted damage broadcasts one registered hurt sound despite ambient cooldown/flock pressure");
                    check(!bird.hurt(f.level.damageSources().generic(), 1) && hurt.calls.size() == 1,
                            "Invulnerability-frame rejection creates no duplicate hurt sound");
                    check(!protectedBird.hurt(f.level.damageSources().generic(), 1) && protectedHurt.calls.isEmpty(),
                            "Invulnerable entity rejects both damage and its feedback");
                    check(sleeping.hurt(f.level.damageSources().generic(), 1) && sleepHurt.calls.size() == 1,
                            "A sleeping bird can react audibly to accepted damage");
                    check(silent.hurt(f.level.damageSources().generic(), 1) && silentHurt.calls.isEmpty(),
                            "Silent entity takes real damage without a hurt sound");
                    f.soundConfig.soundVolumeMultiplier = 0;
                    check(muted.hurt(f.level.damageSources().generic(), 1) && mutedHurt.calls.isEmpty(),
                            "Zero species volume suppresses damage feedback without suppressing damage");
                    f.soundConfig.soundVolumeMultiplier = 1;
                    check(listener.hurts.size() == 2, "Twenty-block listener receives only the two permitted damage responses");
                }
                if (tick == 27) {
                    check(bird.hurt(f.level.damageSources().generic(), 10) && bird.isDeadOrDying(),
                            "Actual fatal damage invokes the vanilla death path");
                    check(death.calls.size() == 1 && hurt.calls.size() == 1 && listener.deaths.size() == 1,
                            "Fatal damage sends one distinct registered death event rather than another hurt event");
                    verifyPacket(listener.hurts.get(0), GuaniaoSoundEvents.HUMMINGBIRD_HURT.get());
                    verifyPacket(listener.deaths.get(0), GuaniaoSoundEvents.HUMMINGBIRD_DEATH.get());
                }
                if (tick == 45) System.out.println("HUMMINGBIRD_DAMAGE_SOUND_QA hurtPackets=" + listener.hurts.size()
                        + " deathPackets=" + listener.deaths.size() + " sleepingResponses=" + sleepHurt.calls.size());
            });
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            f.close(); throw failure;
        }
    }

    @GameTest(template = "garden", batch = "hummingbird_sound_interaction_feedback", timeoutTicks = 150)
    public static void actualFeedCommandsAndGardenBindingEmitOnceAndDelayAmbient(GameTestHelper helper) throws Exception {
        Fixture f = new Fixture(helper);
        try {
            f.config.global.enablePetBirdCommands = true;
            HummingbirdEntity bird = f.bird(Vec3.ZERO);
            bird.ambientSoundTime = -1000; // Keep background randomness out of response-count assertions.
            FakePlayer owner = f.owner(), other = f.owner();
            bird.tame(owner);
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SUGAR, 8));
            owner.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SUGAR, 2));
            SoundProbe feedback = f.listen(bird, GuaniaoSoundEvents.HUMMINGBIRD_INTERACT.get());
            SoundProbe ambient = f.listen(bird);
            PacketProbe listener = f.listener(new Vec3(20, 0, 0));
            f.run(110, tick -> {
                if (tick == 1) {
                    check(bird.interact(owner, InteractionHand.MAIN_HAND).consumesAction()
                                    && owner.getMainHandItem().getCount() == 7 && feedback.calls.size() == 1,
                            "Actual vanilla interaction entry consumes one valid feed and emits one response");
                    bird.interact(owner, InteractionHand.OFF_HAND);
                    check(owner.getOffhandItem().getCount() == 2 && feedback.calls.size() == 1,
                            "Duplicate hand request neither consumes the second food nor duplicates feedback");
                    other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SUGAR));
                    check(bird.interact(other, InteractionHand.MAIN_HAND) == InteractionResult.PASS
                                    && other.getMainHandItem().getCount() == 1 && feedback.calls.size() == 1,
                            "Non-owner cannot gain interaction feedback from a rejected feed");
                    bird.playAmbientSound();
                    check(ambient.calls.isEmpty(), "Feedback delays an otherwise ready ordinary ambient call");
                }
                if (tick == 13) {
                    bird.interact(owner, InteractionHand.MAIN_HAND);
                    check(owner.getMainHandItem().getCount() == 6 && feedback.calls.size() == 1,
                            "Another accepted feed inside the short response cooldown remains functional without overlapping audio");
                    bird.playAmbientSound();
                    check(ambient.calls.isEmpty(), "Ambient cannot overlap the still-playing short response");
                }
                if (tick == 26) {
                    bird.interact(owner, InteractionHand.MAIN_HAND);
                    check(feedback.calls.size() == 2, "Accepted feeding can respond again after 24 ticks");
                }
                if (tick == 55) {
                    owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    owner.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    owner.setShiftKeyDown(true);
                    BirdCommandMode before = bird.getBirdCommandMode();
                    check(bird.interact(owner, InteractionHand.MAIN_HAND).consumesAction()
                                    && bird.getBirdCommandMode() == before.next() && feedback.calls.size() == 3,
                            "Successful owner command invokes the actual state change and one response");
                    bird.interact(owner, InteractionHand.OFF_HAND);
                    check(feedback.calls.size() == 3, "Repeated hand notification does not duplicate the command response");
                    bird.playAmbientSound();
                    check(ambient.calls.isEmpty(), "A command response also delays ordinary ambient playback");
                }
                if (tick == 84) {
                    owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POPPY));
                    check(bird.interact(owner, InteractionHand.MAIN_HAND).consumesAction() && feedback.calls.size() == 4,
                            "Successful owner flower interaction emits one garden-binding response");
                    CompoundTag saved = new CompoundTag(); bird.addAdditionalSaveData(saved);
                    check(saved.contains("Garden") && BlockPos.of(saved.getLong("Garden")).equals(owner.blockPosition())
                                    && bird.getBirdCommandMode() == BirdCommandMode.FREE,
                            "Garden feedback accompanies a real persisted binding and FREE command");
                    other.setShiftKeyDown(true);
                    other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POPPY));
                    check(bird.interact(other, InteractionHand.MAIN_HAND) == InteractionResult.PASS
                                    && feedback.calls.size() == 4,
                            "Non-owner cannot bind a garden or trigger its response");
                    bird.interact(owner, InteractionHand.MAIN_HAND);
                    check(feedback.calls.size() == 4, "Repeated binding within the short cooldown cannot stack responses");
                    bird.playAmbientSound();
                    check(ambient.calls.isEmpty(), "Garden-binding response suppresses an immediate ambient start");
                }
                if (tick == 110) {
                    check(listener.interactions.size() == 4, "Twenty-block observer receives all four successful interaction packets");
                    for (int i = 1; i < feedback.calls.size(); i++)
                        check(feedback.calls.get(i).tick - feedback.calls.get(i - 1).tick >= 24,
                                "Real responses keep the full short-clip overlap guard");
                    verifyPacket(listener.interactions.get(0), GuaniaoSoundEvents.HUMMINGBIRD_INTERACT.get());
                    System.out.println("HUMMINGBIRD_INTERACTION_SOUND_QA responses=" + feedback.calls
                            + " packets=" + listener.interactions.size());
                }
            });
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            f.close(); throw failure;
        }
    }

    @GameTest(template = "garden", batch = "hummingbird_sound_rejected_interactions", timeoutTicks = 110)
    public static void rejectedSilentAndConfigurationMutedInteractionsSendNoResponses(GameTestHelper helper) throws Exception {
        Fixture f = new Fixture(helper);
        HummingbirdEntity bird = f.bird(Vec3.ZERO);
        bird.ambientSoundTime = -1000;
        FakePlayer owner = f.owner(); bird.tame(owner);
        SoundProbe response = f.listen(bird, GuaniaoSoundEvents.HUMMINGBIRD_INTERACT.get());
        f.run(85, tick -> {
            if (tick == 1) {
                owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                check(bird.interact(owner, InteractionHand.MAIN_HAND) == InteractionResult.PASS && response.calls.isEmpty(),
                        "Invalid food does not produce a successful interaction response");
                owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                owner.setShiftKeyDown(true); f.config.global.enablePetBirdCommands = false;
                check(bird.interact(owner, InteractionHand.MAIN_HAND) == InteractionResult.PASS && response.calls.isEmpty(),
                        "Disabled command produces neither an action nor a sound");
                f.config.global.enablePetBirdCommands = true; owner.setShiftKeyDown(false);
                owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SUGAR, 3));
                f.config.global.soundVolumeMultiplier = 0;
                bird.interact(owner, InteractionHand.MAIN_HAND);
                check(owner.getMainHandItem().getCount() == 2 && response.calls.isEmpty(),
                        "Global mute prevents feedback while allowing a valid feed");
                f.config.global.soundVolumeMultiplier = 1;
                bird.setSilent(true);
            }
            if (tick == 30) {
                bird.interact(owner, InteractionHand.MAIN_HAND);
                check(owner.getMainHandItem().getCount() == 1 && response.calls.isEmpty(),
                        "Silent entity can still be fed without emitting feedback");
            }
            if (tick == 60) {
                bird.setSilent(false); f.soundConfig.soundVolumeMultiplier = .5;
                bird.interact(owner, InteractionHand.MAIN_HAND);
                check(owner.getMainHandItem().isEmpty() && response.calls.size() == 1
                                && Math.abs(response.calls.get(0).volume - .325F) < .0001,
                        "Unmuting restores the response and applies species volume exactly once");
            }
        });
    }

    private static void verifyPacket(ClientboundSoundPacket packet, SoundEvent event) {
        check(packet.getSound().value() == event && packet.getSource() == SoundSource.NEUTRAL,
                "Feedback packet carries the actual registered creature sound");
        check(packet.getVolume() > 0 && packet.getPitch() >= .95F && packet.getPitch() <= 1.05F,
                "Feedback packet has positive configured volume and an ordinary short-clip pitch");
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.write(encoded);
            ClientboundSoundPacket decoded = new ClientboundSoundPacket(encoded);
            check(decoded.getSound().value() == event && decoded.getVolume() == packet.getVolume()
                            && decoded.getPitch() == packet.getPitch(),
                    "Feedback registry holder, volume and pitch survive real network encoding");
        } finally { encoded.release(); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }
}
