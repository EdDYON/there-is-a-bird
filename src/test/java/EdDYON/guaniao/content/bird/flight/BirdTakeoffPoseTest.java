package EdDYON.guaniao.content.bird.flight;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.loading.object.BakedAnimations;

/** Checks the first visible takeoff pose, not just the eventual animation name. */
public final class BirdTakeoffPoseTest {
    private static final List<Species> SPECIES = List.of(
            new Species("night_heron", "idle", "fly_flapping_wing", 4),
            new Species("sparrow", "animation.idle", "animation.fly", 4),
            new Species("long_tailed_tit", "idle", "fly_loop", 4),
            new Species("cockatiel", "animation.idle", "animation.fly", 4),
            new Species("macaw", "idle", "fly_flapping_wing_loop", 5),
            new Species("budgerigar", "idle", "fly_flapping_wing_loop", 4),
            new Species("columbid", "idle", "fly_flapping_wing_loop", 4),
            new Species("crow", "animation.idle", "animation.fly", 0),
            new Species("seagull", "idle", "fly_flapping_wing", 4),
            new Species("myna", "animation.idle", "animation.fly", 4),
            new Species("woodcock", "animation.idle", "animation.fly", 1),
            new Species("kestrel", "idle", "fly_flapping_wing_loop", 3),
            new Species("umbrella_cockatoo", "animation.idle", "animation.fly", 5));

    public static void main(String[] args) throws Exception {
        for (Species species : SPECIES) {
            Fixture actual = new Fixture(species);
            Fixture reference = new Fixture(species);
            Bird bird = new Bird(species, true);
            Bird immediate = new Bird(species, false);
            for (int frame = 0; frame <= 80; frame++) {
                actual.frame(bird, frame / 4d);
                reference.frame(immediate, frame / 4d);
            }
            bird.flying = immediate.flying = true;
            float[] firstWingPose = null;
            boolean wingsMoved = false;
            for (int frame = 84; frame <= 100; frame++) {
                double time = frame / 4d;
                actual.frame(bird, time);
                reference.frame(immediate, time);
                assertSamePose(actual, reference, species.name + " takeoff tick " + time);
                float[] wingPose = actual.wingPose();
                if (firstWingPose == null) firstWingPose = wingPose;
                else if (!java.util.Arrays.equals(firstWingPose, wingPose)) wingsMoved = true;
            }
            require(wingsMoved, species.name + " must advance its wing cycle immediately after launch");
            bird.flying = immediate.flying = false;
            for (int frame = 104; frame <= 144; frame++) {
                actual.frame(bird, frame / 4d);
                reference.frame(immediate, frame / 4d);
                assertSamePose(actual, reference, species.name + " landing tick " + frame / 4d);
            }
            System.out.println("PASS: first-frame takeoff, wing motion and landing: " + species.name);
        }
    }

    private static void assertSamePose(Fixture actual, Fixture expected, String context) {
        for (var entry : actual.bones.entrySet()) {
            GeoBone a = entry.getValue(), b = expected.bones.get(entry.getKey());
            float[] av = pose(a), bv = pose(b);
            for (int axis = 0; axis < av.length; axis++)
                require(Math.abs(av[axis] - bv[axis]) < 0.0001F,
                        context + ": delayed bone " + entry.getKey() + " channel " + axis + " (" + av[axis] + " != " + bv[axis] + ")");
        }
    }

    private static float[] pose(GeoBone bone) {
        return new float[]{bone.getRotX(), bone.getRotY(), bone.getRotZ(), bone.getPosX(), bone.getPosY(), bone.getPosZ(),
                bone.getScaleX(), bone.getScaleY(), bone.getScaleZ()};
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private record Species(String name, String idle, String fly, int transition) {}

    private static final class Bird implements GeoAnimatable {
        final Species species;
        final boolean useFlightHelper;
        final AnimatableManager<Bird> manager;
        boolean flying;
        Bird(Species species, boolean useFlightHelper) {
            this.species = species;
            this.useFlightHelper = useFlightHelper;
            this.manager = new AnimatableManager<>(this);
        }
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            RawAnimation idle = RawAnimation.begin().thenLoop(species.idle);
            RawAnimation fly = RawAnimation.begin().thenLoop(species.fly);
            controllers.add(new AnimationController<>(this, "movement", species.transition, state -> {
                state.getController().transitionLength(species.transition);
                if (!flying) return state.setAndContinue(idle);
                if (useFlightHelper) return BirdFlightAnimation.play(state, fly);
                state.getController().transitionLength(0);
                return state.setAndContinue(fly);
            }));
        }
        public AnimatableInstanceCache getAnimatableInstanceCache() { return null; }
        public double getTick(Object object) { return 0; }
    }

    private static final class Fixture extends GeoModel<Bird> {
        final AnimationProcessor<Bird> processor = new AnimationProcessor<>(this);
        final Map<String, GeoBone> bones = new HashMap<>();
        final BakedAnimations animations;
        Fixture(Species species) throws Exception {
            Path assets = Path.of("src/main/resources/assets/guaniao");
            JsonObject raw = JsonParser.parseString(Files.readString(assets.resolve("animations/" + species.name + ".animation.json")))
                    .getAsJsonObject().getAsJsonObject("animations");
            animations = software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter.GEO_GSON.fromJson(raw, BakedAnimations.class);
            JsonObject geometry = JsonParser.parseString(Files.readString(assets.resolve("geo/" + species.name + ".geo.json")))
                    .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            for (var element : geometry.getAsJsonArray("bones")) {
                JsonObject json = element.getAsJsonObject();
                String name = json.get("name").getAsString();
                GeoBone bone = new GeoBone(null, name, false, 0d, false, false);
                if (json.has("rotation")) {
                    var r = json.getAsJsonArray("rotation");
                    bone.setRotX((float)-Math.toRadians(r.get(0).getAsDouble()));
                    bone.setRotY((float)-Math.toRadians(r.get(1).getAsDouble()));
                    bone.setRotZ((float)Math.toRadians(r.get(2).getAsDouble()));
                }
                bone.saveInitialSnapshot();
                bone.resetStateChanges();
                bones.put(name, bone);
                processor.registerGeoBone(bone);
            }
        }
        float[] wingPose() {
            var values = new java.util.ArrayList<Float>();
            bones.entrySet().stream().filter(e -> e.getKey().toLowerCase(java.util.Locale.ROOT).contains("wing")
                    || e.getKey().toLowerCase(java.util.Locale.ROOT).contains("fly"))
                    .sorted(Map.Entry.comparingByKey()).forEach(e -> {
                        for (float value : pose(e.getValue())) values.add(value);
                    });
            float[] result = new float[values.size()];
            for (int i = 0; i < result.length; i++) result[i] = values.get(i);
            return result;
        }
        void frame(Bird bird, double time) {
            var state = new AnimationState<>(bird, 0, 0, 0, false);
            state.animationTick = time;
            processor.tickAnimation(bird, this, bird.manager, time, state, true);
        }
        public ResourceLocation getModelResource(Bird bird) { return ResourceLocation.fromNamespaceAndPath("guaniao", "test"); }
        public ResourceLocation getTextureResource(Bird bird) { return ResourceLocation.fromNamespaceAndPath("guaniao", "test"); }
        public ResourceLocation getAnimationResource(Bird bird) { return ResourceLocation.fromNamespaceAndPath("guaniao", "test"); }
        public AnimationProcessor<Bird> getAnimationProcessor() { return processor; }
        public void handleAnimations(Bird bird, long id, AnimationState<Bird> state) {}
        public Animation getAnimation(Bird bird, String name) { return animations.getAnimation(name); }
    }
}
