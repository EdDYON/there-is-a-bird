package EdDYON.guaniao.client.hummingbird;

import EdDYON.guaniao.content.bird.BirdSoundVolume;
import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Listener;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;

/** Isolated runClient-only QA. No saves are opened and this source set is never packaged. */
@Mod.EventBusSubscriber(modid = "guaniao", value = Dist.CLIENT)
public final class HummingbirdWingClientQa {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("guaniao.hummingbirdClientQa");
    private static final long WAIT_STARTED = System.nanoTime();
    private static final List<String> PASSED = new ArrayList<>();
    private static volatile Fixture fixture;
    private static boolean finished;

    private HummingbirdWingClientQa() { }

    @SubscribeEvent public static void onTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || finished) return;
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (fixture == null) {
                if (event.phase != TickEvent.Phase.END) return;
                SoundManager sounds = minecraft.getSoundManager();
                boolean ready = minecraft.getOverlay() == null && minecraft.screen != null
                        && GuaniaoEntityTypes.HUMMINGBIRD.isPresent()
                        && sounds.getSoundEvent(GuaniaoSoundEvents.HUMMINGBIRD_WING.getId()) != null
                        && (boolean)field(field(sounds, "soundEngine"), "loaded");
                if (!ready) {
                    check(System.nanoTime() - WAIT_STARTED < 120_000_000_000L, "client resources/OpenAL initialize within 120 seconds");
                    return;
                }
                check(minecraft.level == null && minecraft.player == null, "QA remains at the menu without opening a user world");
                fixture = new Fixture(minecraft);
                LOGGER.info("HUMMINGBIRD_CLIENT_QA START actual Forge ClientLevel, SoundManager and OpenAL");
                return;
            }
            if (event.phase == TickEvent.Phase.START) fixture.advance();
            else fixture.verify();
        } catch (Throwable failure) {
            finish(minecraft, failure);
        }
    }

    @SubscribeEvent public static void onSource(PlaySoundSourceEvent event) {
        Fixture current = fixture;
        if (ENABLED && current != null && event.getSound() instanceof HummingbirdWingSound wing
                && current.played.contains(wing)) {
            try {
                // Forge dispatches this event inside ChannelHandle.execute on the
                // SoundEngine executor, where the real OpenAL context is current.
                Listener listener = (Listener)field(current.engine, "listener");
                listener.setListenerPosition(current.listener);
                current.started.put(wing, sample(wing, event.getChannel(), current.tick));
            } catch (Throwable failure) {
                current.asyncFailure.compareAndSet(null, failure);
            }
        }
    }

    private static void pass(String message) {
        PASSED.add(message);
        LOGGER.info("HUMMINGBIRD_CLIENT_QA PASS {}", message);
    }

    private static void finish(Minecraft minecraft, Throwable failure) {
        if (finished) return;
        finished = true;
        Fixture current = fixture;
        try {
            if (current != null) current.cleanup();
            Map<String, Object> result = new HashMap<>();
            result.put("passed", failure == null);
            result.put("checks", PASSED);
            result.put("testTicks", current == null ? 0 : current.tick);
            result.put("realSourceStarts", current == null ? 0 : current.started.size());
            result.put("playRequests", current == null ? 0 : current.played.size());
            result.put("sourceReadbacks", current == null ? List.of() : current.reportSources());
            result.put("burstMeasurements", current == null ? Map.of() : current.burstReport());
            result.put("selectionStops", current == null ? List.of() : List.copyOf(current.selectionStops));
            result.put("failure", failure == null ? null : failure.toString());
            Path path = Path.of(System.getProperty("guaniao.hummingbirdClientQa.result", "hummingbird-client-qa.json"));
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(result), StandardCharsets.UTF_8);
        } catch (Throwable cleanupFailure) {
            LOGGER.error("HUMMINGBIRD_CLIENT_QA result/cleanup failure", cleanupFailure);
        }
        if (failure == null) LOGGER.info("HUMMINGBIRD_CLIENT_QA ALL PASS (real OpenAL; no human listening claim)");
        else LOGGER.error("HUMMINGBIRD_CLIENT_QA FAIL", failure);
        minecraft.stop();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Object field(Object instance, String name) {
        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("QA field " + name, failure);
        }
    }

    private static SourceSample sample(HummingbirdWingSound sound, Channel channel, int tick) {
        return sample(sound, channel, tick, sound.getVolume());
    }

    private static SourceSample sample(HummingbirdWingSound sound, Channel channel, int tick, float requestedVolume) {
        int source = (int)field(channel, "source");
        float[] position = new float[3];
        float[] listener = new float[3];
        AL10.alGetSourcefv(source, AL10.AL_POSITION, position);
        AL10.alGetListenerfv(AL10.AL_POSITION, listener);
        return new SourceSample(tick, source, channel.playing(), AL10.alGetSourcef(source, AL10.AL_GAIN),
                AL10.alGetSourcef(source, AL10.AL_PITCH), AL10.alGetSourcei(source, AL10.AL_LOOPING),
                position[0], position[1], position[2], AL10.alGetSourcef(source, AL10.AL_MAX_DISTANCE),
                AL10.alGetSourcei(source, AL10.AL_DISTANCE_MODEL), listener[0], listener[1], listener[2], requestedVolume, AL10.alGetError());
    }

    private record SourceSample(int tick, int sourceId, boolean playing, float gain, float pitch, int looping,
                                float x, float y, float z, float maxDistance, int distanceModel,
                                float listenerX, float listenerY, float listenerZ, float requestedVolume, int alError) { }
    private record GainFrame(int tick, int sourceId, int cycleTick, int burstTicks, float requestedVolume,
                             float gain, boolean playing, int alError) { }
    private record SelectionStop(int tick, int entityId, int cycleTick, int burstTicks, float gain) { }

    private static final class Fixture {
        private final Minecraft minecraft;
        private final SoundManager sounds;
        private final SoundEngine engine;
        private final ClientPacketListener connection;
        private final ClientLevel world;
        private ClientLevel secondWorld;
        private final RegistryAccess registries;
        private final HummingbirdSoundManager manager;
        private final List<HummingbirdEntity> birds = new ArrayList<>();
        private final CopyOnWriteArrayList<HummingbirdWingSound> played = new CopyOnWriteArrayList<>();
        private final Map<HummingbirdWingSound, SourceSample> started = new ConcurrentHashMap<>();
        private final Map<HummingbirdWingSound, SourceSample> readbacks = new ConcurrentHashMap<>();
        private final Map<Integer, GainFrame> gainFrames = new ConcurrentSkipListMap<>();
        private final List<SelectionStop> selectionStops = new ArrayList<>();
        private List<Integer> measuredBursts = List.of(), measuredPauses = List.of();
        private int recoveries;
        private final AtomicReference<Throwable> asyncFailure = new AtomicReference<>();
        private final Vec3 listener = new Vec3(0, 64, 0);
        private final EntityDataAccessor<Integer> activity;
        private HummingbirdWingSound first;
        private HummingbirdWingSound selectionIncumbent;
        private HummingbirdEntity selectionOutsider;
        private List<HummingbirdWingSound> stopping = List.of();
        private int stablePlayCount;
        private int tick;

        @SuppressWarnings("unchecked") Fixture(Minecraft minecraft) throws ReflectiveOperationException {
            this.minecraft = minecraft;
            this.sounds = minecraft.getSoundManager();
            this.engine = (SoundEngine)field(sounds, "soundEngine");
            minecraft.options.pauseOnLostFocus = false;
            minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1D);
            minecraft.options.getSoundSourceOptionInstance(SoundSource.NEUTRAL).set(1D);
            minecraft.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0D);
            minecraft.options.getSoundSourceOptionInstance(SoundSource.AMBIENT).set(0D);
            HolderLookup.Provider vanilla = VanillaRegistries.createLookup();
            List<Registry<?>> entries = new ArrayList<>();
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registries().forEach(entry -> entries.add(entry.value()));
            entries.add(copy(vanilla, Registries.DIMENSION_TYPE));
            entries.add(copy(vanilla, Registries.BIOME));
            entries.add(copy(vanilla, Registries.DAMAGE_TYPE));
            this.registries = new RegistryAccess.ImmutableRegistryAccess(entries).freeze();
            this.connection = new ClientPacketListener(minecraft, minecraft.screen, new Connection(PacketFlow.CLIENTBOUND),
                    null, minecraft.getUser().getGameProfile(),
                    new WorldSessionTelemetryManager(TelemetryEventSender.DISABLED, false, null, null)) {
                @Override public RegistryAccess registryAccess() { return registries; }
            };
            this.world = world();
            Field field = HummingbirdEntity.class.getDeclaredField("ACTIVITY");
            field.setAccessible(true);
            this.activity = (EntityDataAccessor<Integer>)field.get(null);
            this.manager = new HummingbirdSoundManager(new HummingbirdSoundManager.Playback() {
                @Override public void play(HummingbirdWingSound sound) { played.add(sound); sounds.play(sound); }
                @Override public void stop(HummingbirdWingSound sound) {
                    if (selectionTick() >= 65 && sound.canPlaySound() && sound.bird().distanceToSqr(listener) <= 16 * 16) {
                        selectionStops.add(new SelectionStop(tick, sound.bird().getId(), (int)field(sound, "cycleTick"),
                                (int)field(sound, "burstTicks"), sound.getVolume()));
                        check(sound.getVolume() == 0 && !sound.isBurstActive(),
                                "eligible source rebalancing occurs only in its natural quiet pause, never mid-burst");
                    }
                    sounds.stop(sound);
                }
                @Override public boolean isActive(HummingbirdWingSound sound) { return sounds.isActive(sound); }
            });
            birds.add(bird(world, 1001, 0));
        }

        private static <T> Registry<T> copy(HolderLookup.Provider provider, ResourceKey<Registry<T>> key) {
            MappedRegistry<T> result = new MappedRegistry<>(key, Lifecycle.stable());
            provider.lookupOrThrow(key).listElements().forEach(value -> result.register(value.key(), value.value(), Lifecycle.stable()));
            return result.freeze();
        }

        private ClientLevel world() {
            return new ClientLevel(connection, new ClientLevel.ClientLevelData(Difficulty.NORMAL, false, false),
                    Level.OVERWORLD, registries.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
                    2, 2, () -> InactiveProfiler.INSTANCE, minecraft.levelRenderer, false, 0L);
        }

        private HummingbirdEntity bird(ClientLevel world, int id, double x) {
            HummingbirdEntity bird = new HummingbirdEntity(GuaniaoEntityTypes.HUMMINGBIRD.get(), world);
            bird.setId(id);
            bird.setUUID(new java.util.UUID(0, id));
            bird.setPos(x, 64, 0);
            bird.getEntityData().set(activity, HummingbirdEntity.Activity.HOVER.ordinal());
            world.putNonPlayerEntity(id, bird);
            check(world.getEntity(id) == bird && bird.isWingSoundActive(), "real registered hummingbird is tracked in the independent ClientLevel");
            return bird;
        }

        private void state(HummingbirdEntity bird, HummingbirdEntity.Activity state) {
            bird.getEntityData().set(activity, state.ordinal());
        }

        private void advance() {
            tick++;
            int stage = lifecycleTick();
            if (tick >= 21 && tick <= 450) birds.get(0).setPos((tick - 20) * (1.6 / 430), 64 + Math.sin(tick * .04) * .1, .3);
            if (stage == 101) { first = played.get(0); state(birds.get(0), HummingbirdEntity.Activity.SLEEP); }
            if (stage == 131) state(birds.get(0), HummingbirdEntity.Activity.HOVER);
            if (stage == 161) state(birds.get(0), HummingbirdEntity.Activity.PERCH);
            if (stage == 191) state(birds.get(0), HummingbirdEntity.Activity.HOVER);
            if (stage == 211) for (int i = 1; i < 6; i++) birds.add(bird(world, 1001 + i, i + 1));
            if (stage >= 231 && stage <= 270) {
                birds.get(3).setPos(stage % 2 == 0 ? 4.02 : 3.98, 64, 0);
                birds.get(4).setPos(stage % 2 == 0 ? 3.98 : 4.02, 64, 0);
            }
            if (selectionTick() == 1) {
                selectionIncumbent = active().stream().filter(sound -> sound.bird() == birds.get(3) || sound.bird() == birds.get(4))
                        .findFirst().orElseThrow(() -> new AssertionError("the fourth actual source exists before extended selection QA"));
                selectionOutsider = selectionIncumbent.bird() == birds.get(3) ? birds.get(4) : birds.get(3);
                selectionIncumbent.bird().setPos(3.98, 64, 0);
                selectionOutsider.setPos(4.02, 64, 0);
            }
            if (selectionTick() == 65) {
                // The existing channel is now over 120 ticks old and audibly in its
                // next burst. Make a different bird nearer without changing either activity.
                check(selectionIncumbent.getVolume() > 0 && selectionIncumbent.isBurstActive(),
                        "a real post-startup burst is active before the deliberate distance reorder");
                selectionIncumbent.bird().setPos(4.2, 64, 0);
                selectionOutsider.setPos(3.8, 64, 0);
            }
            if (stage == 271) birds.get(0).setPos(25, 64, 0);
            if (stage == 291) {
                stopping = active();
                birds.forEach(bird -> bird.setSilent(true));
            }
            if (stage == 321) birds.forEach(bird -> bird.setSilent(false));
            if (stage == 351) stopping = active();
            if (stage == 395) { stopping = active(); secondWorld = world(); }
            if (stage == 411) birds.add(bird(secondWorld, 2001, 1));
            if (stage >= 351 && stage <= 360) manager.update(null, listener, true);
            else if (stage >= 361 && stage <= 380) manager.update(world, listener, false);
            else manager.update(stage >= 395 ? secondWorld : world, listener, true);
        }

        private int lifecycleTick() {
            if (tick <= 450) return -1;
            int stage = tick - 350;
            // Add 230 real ticks beyond the old five-second protection test, then
            // resume the same landing/muting/unload/world-replacement scenarios.
            return stage >= 271 && stage <= 500 ? -2 : stage > 500 ? stage - 230 : stage;
        }

        private int selectionTick() {
            int stage = tick - 350;
            return tick > 450 && stage >= 271 && stage <= 500 ? stage - 270 : -1;
        }

        @SuppressWarnings("unchecked") private List<HummingbirdWingSound> active() {
            Map<SoundInstance, ChannelAccess.ChannelHandle> channels = (Map<SoundInstance, ChannelAccess.ChannelHandle>)field(engine, "instanceToChannel");
            return played.stream().filter(sound -> !sound.isStopped() && channels.containsKey(sound) && sounds.isActive(sound)).toList();
        }

        @SuppressWarnings("unchecked") private void verify() {
            int stage = lifecycleTick();
            Throwable failure = asyncFailure.get();
            if (failure != null) throw new AssertionError("OpenAL readback", failure);
            Map<SoundInstance, ChannelAccess.ChannelHandle> channels = (Map<SoundInstance, ChannelAccess.ChannelHandle>)field(engine, "instanceToChannel");
            for (HummingbirdWingSound sound : active()) {
                int frameTick = tick;
                float requestedVolume = sound.getVolume();
                boolean initialBird = tick <= 450 && sound == played.get(0);
                int cycle = initialBird ? (int)field(sound, "cycleTick") : -1;
                int burst = initialBird ? (int)field(sound, "burstTicks") : -1;
                channels.get(sound).execute(channel -> {
                    try {
                        SourceSample sample = sample(sound, channel, frameTick, requestedVolume);
                        readbacks.put(sound, sample);
                        if (initialBird) gainFrames.put(frameTick, new GainFrame(frameTick, sample.sourceId, cycle, burst,
                                requestedVolume, sample.gain, sample.playing, sample.alError));
                    }
                    catch (Throwable exception) { asyncFailure.compareAndSet(null, exception); }
                });
            }
            check(active().size() <= 4, "no more than four actual hummingbird OpenAL sources are live");
            if (selectionTick() == 230) {
                check(selectionIncumbent.isStopped() && !sounds.isActive(selectionIncumbent)
                                && active().size() == 4 && active().stream().anyMatch(sound -> sound.bird() == selectionOutsider)
                                && selectionStops.stream().anyMatch(stop -> stop.entityId == selectionIncumbent.bird().getId()),
                        "a nearer bird eventually gets a source during the incumbent's real quiet pause, without monopolizing the budget");
                pass("after the startup protection expires, distance reordering preserves the current full burst and releases its source in the next quiet pause");
            }
            if (tick == 20) {
                check(played.size() == 1 && active().size() == 1, "one hovering bird creates one real active source");
                source(active().get(0), 1F, true);
                pass("real static wing OGG plays near the actual listener with original pitch, positive gain and 16-block linear attenuation");
            }
            if (tick == 450) {
                check(played.size() == 1 && active().size() == 1 && started.size() == 1, "repeated wing bursts keep the same real source without new play requests");
                HummingbirdWingSound sound = active().get(0);
                source(sound, 1F, false);
                verifyBurstRuns(sound);
                SourceSample read = readbacks.get(sound);
                HummingbirdEntity bird = birds.get(0);
                check(Math.abs(read.x - bird.getX()) < .1 && Math.abs(read.y - bird.getY() - bird.getBbHeight() * .5) < .1
                        && Math.abs(read.z - bird.getZ()) < .1, "real channel position follows the moving hummingbird");
                pass("same OpenAL source plays 80--100 tick bursts, pauses for exactly 10 zero-gain ticks and recovers at least three times");
                pass("loop follows entity position across burst/pause cycles without restarting or changing pitch");
            }
            if (stage == 125) {
                check(first.isStopped() && !sounds.isActive(first) && active().isEmpty() && played.size() == 1,
                        "sleep stops and releases the actual source without restarting it");
                pass("sleep shuts down the loop");
            }
            if (stage == 155) {
                check(played.size() == 2 && active().size() == 1 && active().get(0) != first, "waking/hovering creates a new live loop");
                source(active().get(0), 1F, true);
                pass("hovering after sleep restarts one real source");
            }
            if (stage == 185) {
                check(active().isEmpty() && played.get(1).isStopped() && !sounds.isActive(played.get(1)), "ground perch shuts down the restarted loop");
                pass("landing/perching shuts down the loop");
            }
            if (stage == 230) {
                List<HummingbirdEntity> nearest = birds.stream().sorted(Comparator.comparingDouble(bird -> bird.distanceToSqr(listener))).limit(4).toList();
                check(active().size() == 4 && active().stream().allMatch(sound -> nearest.contains(sound.bird())), "six birds select the nearest four actual sources");
                active().forEach(sound -> source(sound, .5F, true));
                stablePlayCount = played.size();
                pass("nearest-four source budget and flock gain are applied to real OpenAL channels");
            }
            if (stage == 270) {
                check(played.size() == stablePlayCount && active().size() == 4,
                        "slight fourth/fifth bird distance crossings retain the existing sources instead of short restarts");
                pass("nearby birds trading places do not interrupt continuous wing sources");
            }
            if (stage == 285) {
                check(active().size() == 4 && active().stream().noneMatch(sound -> sound.bird() == birds.get(0)), "out-of-range source stops and the next nearest replaces it");
                pass("range change replaces the source without exceeding four");
            }
            if (stage == 315) {
                check(active().isEmpty() && stopping.stream().allMatch(sound -> sound.isStopped() && !sounds.isActive(sound)), "silent birds release all actual sources");
                pass("silent entity state shuts down all loops");
            }
            if (stage == 345) check(active().size() == 4, "enabling the birds again restores four loops");
            if (stage == 375) {
                check(active().isEmpty() && stopping.stream().allMatch(sound -> sound.isStopped() && !sounds.isActive(sound)), "null world and disabled sound clear all sources");
                pass("world unload and disabled sound leave no active loop");
            }
            if (stage == 390) check(active().size() == 4, "re-enabling starts the current world's nearest sources");
            if (stage == 407) {
                check(active().isEmpty() && stopping.stream().allMatch(sound -> sound.isStopped() && !sounds.isActive(sound)), "world identity change stops the previous world's loops");
                pass("world replacement cleans up prior sources");
            }
            if (stage == 435) {
                check(active().size() == 1 && active().get(0).bird().level() == secondWorld, "new world's entity starts one independently owned source");
                source(active().get(0), 1F, true);
                pass("a second ClientLevel starts its own real source");
            }
            if (stage >= 441) {
                check(started.size() == played.size(), "every QA play request reached the real source-created event");
                finish(minecraft, null);
            }
        }

        private void source(HummingbirdWingSound sound, float gain, boolean requirePlateau) {
            SourceSample sample = readbacks.get(sound);
            check(sample != null && tick - sample.tick <= 5 && sample.playing && sample.alError == AL10.AL_NO_ERROR,
                    "recent real OpenAL readback is playing and error-free");
            check(started.containsKey(sound) && started.get(sound).playing, "Forge source event followed buffer attachment and actual play");
            float expected = BirdSoundVolume.apply(sound.bird(), HummingbirdWingSound.BASE_VOLUME) * gain;
            check(sample.gain >= 0 && sample.gain <= expected + .015F
                            && Math.abs(sample.gain - sample.requestedVolume) < .001F
                            && Math.abs(sample.pitch - 1) < .001F && sample.looping == AL10.AL_TRUE,
                    "real channel follows the frame's gain envelope while retaining original pitch and looping");
            if (requirePlateau) check(sample.gain > 0 && Math.abs(sample.gain - expected) < .015F,
                    "the deliberately selected non-pause platform reaches the configured peak gain");
            check(Math.abs(sample.maxDistance - 16) < .001F && sample.distanceModel == AL11.AL_LINEAR_DISTANCE,
                    "real channel uses the sounds.json 16-block linear attenuation distance");
            check(Math.abs(sample.listenerX - listener.x) < .001 && Math.abs(sample.listenerY - listener.y) < .001
                            && Math.abs(sample.listenerZ - listener.z) < .001,
                    "actual OpenAL listener is at the fixture listener rather than the menu origin");
            check(sound.getVolume() >= 0 && sound.getPitch() == 1 && sound.isLooping(), "tickable sound retains its gain envelope/pitch/looping");
        }

        private void verifyBurstRuns(HummingbirdWingSound sound) {
            List<Integer> bursts = new ArrayList<>(), pauses = new ArrayList<>();
            int sourceId = started.get(sound).sourceId;
            int positiveRun = 0, quietRun = 0, lastTick = 0;
            boolean completePositiveRun = false;
            for (GainFrame frame : List.copyOf(gainFrames.values())) {
                if (frame.tick < 20) continue; // The first cold decode may make the initial audible run partial.
                check(lastTick == 0 || frame.tick == lastTick + 1, "real gain readbacks cover consecutive client ticks");
                lastTick = frame.tick;
                check(frame.sourceId == sourceId && frame.playing && frame.alError == AL10.AL_NO_ERROR,
                        "every burst and pause keeps the same live, error-free OpenAL source");
                check(Math.abs(frame.gain - frame.requestedVolume) < .001F,
                        "the actual channel gain receives each envelope frame including precise zero pauses");
                check(frame.burstTicks >= 80 && frame.burstTicks <= 100, "each independently randomized burst has a four-to-five-second target");
                if (frame.gain > 0) {
                    if (quietRun > 0) {
                        check(quietRun == 10, "the observed exact-zero pause lasts ten ticks, got " + quietRun);
                        pauses.add(quietRun); quietRun = 0; completePositiveRun = true; recoveries++;
                    }
                    positiveRun++;
                } else {
                    if (positiveRun > 0) {
                        if (completePositiveRun) {
                            check(positiveRun >= 80 && positiveRun <= 100,
                                    "the observed complete positive-gain burst lasts 80--100 ticks, got " + positiveRun);
                            bursts.add(positiveRun);
                        }
                        positiveRun = 0;
                    }
                    quietRun++;
                }
            }
            check(lastTick >= 440 && bursts.size() >= 3 && pauses.size() >= 3 && recoveries >= 3,
                    "at least three complete real bursts/pause/recovery cycles were observed; bursts=" + bursts + ", pauses=" + pauses);
            float peak = (float)gainFrames.values().stream().mapToDouble(GainFrame::gain).max().orElse(0);
            check(Math.abs(peak - BirdSoundVolume.apply(sound.bird(), HummingbirdWingSound.BASE_VOLUME)) < .001F,
                    "the burst platform reaches the full configured peak rather than staying permanently quiet");
            measuredBursts = List.copyOf(bursts); measuredPauses = List.copyOf(pauses);
        }

        private Map<String, Object> burstReport() {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("completeBurstTicks", measuredBursts);
            result.put("pauseTicks", measuredPauses);
            result.put("recoveries", recoveries);
            result.put("peakGain", gainFrames.values().stream().mapToDouble(GainFrame::gain).max().orElse(0));
            result.put("sourceIds", gainFrames.values().stream().map(GainFrame::sourceId).distinct().toList());
            result.put("gainFrames", List.copyOf(gainFrames.values()));
            return result;
        }

        private List<Map<String, Object>> reportSources() {
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < played.size(); i++) {
                HummingbirdWingSound sound = played.get(i);
                Map<String, Object> entry = new java.util.LinkedHashMap<>();
                entry.put("request", i + 1);
                entry.put("entityId", sound.bird().getId());
                entry.put("sound", sound.getLocation().toString());
                entry.put("started", started.get(sound));
                entry.put("latest", readbacks.get(sound));
                entry.put("stoppedAtCleanup", sound.isStopped());
                result.add(entry);
            }
            return result;
        }

        private void cleanup() {
            manager.clear();
            birds.forEach(HummingbirdEntity::discard);
            MinecraftForge.EVENT_BUS.post(new LevelEvent.Unload(world));
            if (secondWorld != null) MinecraftForge.EVENT_BUS.post(new LevelEvent.Unload(secondWorld));
            connection.close();
        }
    }
}
