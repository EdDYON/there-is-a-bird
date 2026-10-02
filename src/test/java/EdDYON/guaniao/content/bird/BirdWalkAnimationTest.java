package EdDYON.guaniao.content.bird;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.CoreBakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.util.JsonUtil;

/** Replays baked walking poses under variable travel, stops, loop wraps and animation handoffs. */
public final class BirdWalkAnimationTest {
    public static void main(String[] args) throws Exception {
        String[] speciesNames = {"night_heron", "sparrow", "long_tailed_tit", "cockatiel", "macaw", "budgerigar", "columbid", "crow", "seagull", "myna", "woodcock", "kestrel", "umbrella_cockatoo", "kiwi", "cassowary"};
        for (String name : speciesNames) {
            JsonObject raw = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/guaniao/animations/" + name + ".animation.json"))).getAsJsonObject().getAsJsonObject("animations");
            for (var entry : raw.entrySet()) {
                if (!List.of("walk", "animation.walk", "run", "running", "trotting", "sprinting").contains(entry.getKey())) continue;
                Species species = new Species(name, entry.getKey());
                double length = entry.getValue().getAsJsonObject().get("animation_length").getAsDouble()*20;
                for (int fps : new int[]{15, 30, 60, 144}) {
                    Fixture actual = new Fixture(species);
                    Bird bird = new Bird(species, true);
                    actual.frame(bird, 0); actual.frame(bird, 0.00001);
                    double distance = 0;
                    for (int frame = 1; frame <= fps*6; frame++) {
                        double time = frame*20d/fps;
                        // A decelerating then accelerating route, with a full second stopped.
                        double previous = (frame-1)*20d/fps;
                        double increment = travel(time) - travel(previous);
                        distance += increment;
                        bird.x = distance / 16;
                        actual.frame(bird, time);
                        double expectedPhase = (distance/0.5) % length;
                        require(Math.abs(bird.controller.phase - expectedPhase) < 0.0001,
                                name + " " + entry.getKey() + " at " + fps + " FPS: phase " + bird.controller.phase + " != " + expectedPhase);
                        if (frame % fps == 0) {
                            Fixture reference = new Fixture(species);
                            Bird steady = new Bird(species, false);
                            reference.frame(steady, 0); reference.frame(steady, 0.00001);
                            reference.frame(steady, expectedPhase + .00001);
                            assertSamePose(actual, reference, name + " variable-speed pose at " + time);
                        }
                    }
                }
                String other = raw.has("animation.fly") ? "animation.fly" : raw.has("fly_flapping_wing_loop") ? "fly_flapping_wing_loop" : raw.has("idle") ? "idle" : "animation.idle";
                Fixture actual = new Fixture(species), reference = new Fixture(species);
                Bird moving = new Bird(species,true), normal = new Bird(species,false);
                for (int frame=0;frame<80;frame++) {
                    moving.x=frame*.05;
                    actual.frame(moving,frame*.25); reference.frame(normal,frame*.25);
                }
                moving.nextClip=normal.nextClip=other;
                for (int frame=80;frame<120;frame++) {
                    actual.frame(moving,frame*.25); reference.frame(normal,frame*.25);
                    // Unkeyed channels intentionally retain GeckoLib's normal reset blend.
                    // Both fixtures have different preceding walk poses, so compare those
                    // channels after the blend has finished; keyed channels must agree immediately.
                    if (frame >= 108) assertSamePose(actual,reference,name + " walk to " + other);
                    else assertKeyedPose(actual,reference,other,name);
                }
                System.out.println("PASS distance/phase/real bones at 15/30/60/144 FPS: " + name + "/" + entry.getKey());
            }
        }
        distanceGuards();
    }

    private static double travel(double tick) {
        double[] rates = {0.125, 0.5, 0, 1.5, 0.25, 0.75};
        double result = 0;
        for (int i=0;i<rates.length;i++) result += Math.min(20, Math.max(0,tick-i*20))*rates[i];
        return result;
    }

    private static void distanceGuards() {
        BirdWalkDistance distance = new BirdWalkDistance();
        distance.sample(0,0,0,true,1); distance.sample(1,.1,0,true,1);
        require(Math.abs(distance.pixels()-1.6)<1e-9,"real travel");
        distance.sample(1,.1,0,true,1); distance.sample(2,.1,0,true,1);
        require(Math.abs(distance.pixels()-1.6)<1e-9,"duplicate render / stopped");
        distance.sample(3,50,0,true,1); distance.sample(4,51,0,false,1);
        distance.sample(5,52,0,true,1); distance.sample(20,53,0,true,1);
        require(Math.abs(distance.pixels()-1.6)<1e-9,"teleport / air / offscreen");
        distance.sample(21,53.1,0,true,2);
        require(Math.abs(distance.pixels()-2.4)<1e-9,"larger bird takes longer strides");
    }

    private static void assertKeyedPose(Fixture actual, Fixture expected, String clip, String context) {
        for (var track : actual.animations.getAnimation(clip).boneAnimations()) {
            float[] a=pose(actual.bones.get(track.boneName())), b=pose(expected.bones.get(track.boneName()));
            boolean[] keyed={!track.rotationKeyFrames().xKeyframes().isEmpty(), !track.positionKeyFrames().xKeyframes().isEmpty(), !track.scaleKeyFrames().xKeyframes().isEmpty()};
            for (int channel=0;channel<9;channel++) if(keyed[channel/3])
                require(Math.abs(a[channel]-b[channel])<.0001,context+" keyed handoff: "+track.boneName()+" / "+channel);
        }
    }

    private static void assertSamePose(Fixture actual, Fixture expected, String context) {
        for (var entry : actual.bones.entrySet()) {
            GeoBone a = entry.getValue(), b = expected.bones.get(entry.getKey());
            float[] av = pose(a), bv = pose(b);
            for (int axis = 0; axis < av.length; axis++)
                require(Math.abs(av[axis] - bv[axis]) < 0.0001F,
                        context + ": unexpected bone " + entry.getKey() + " channel " + axis + " (" + av[axis] + " != " + bv[axis] + ")");
        }
    }

    private static float[] pose(GeoBone bone) {
        return new float[]{bone.getRotX(), bone.getRotY(), bone.getRotZ(), bone.getPosX(), bone.getPosY(), bone.getPosZ(),
                bone.getScaleX(), bone.getScaleY(), bone.getScaleZ()};
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private record Species(String name, String idle) {}

    private static final class Bird implements GeoAnimatable {
        final Species species;
        final boolean driven;
        final AnimatableManager<Bird> manager;
        Clock controller;
        double x;
        String nextClip;
        Bird(Species species, boolean driven) {
            this.species=species; this.driven=driven; this.manager=new AnimatableManager<>(this);
        }
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            RawAnimation clip=RawAnimation.begin().thenLoop(species.idle);
            if (driven) {
                controller=new Clock(this, state -> {
                    if (nextClip != null) return state.setAndContinue(RawAnimation.begin().thenLoop(nextClip));
                    ((Clock)state.getController()).useGroundMovement(.5);
                    return state.setAndContinue(clip);
                });
                controllers.add(controller);
            } else controllers.add(new AnimationController<>(this,"movement",0,state -> state.setAndContinue(nextClip == null ? clip : RawAnimation.begin().thenLoop(nextClip))));
        }
        public AnimatableInstanceCache getAnimatableInstanceCache() { return null; }
        public double getTick(Object object) { return 0; }
    }

    private static final class Clock extends BirdMovementAnimationController<Bird> {
        double phase;
        Clock(Bird bird, AnimationStateHandler<Bird> handler) { super(bird,"movement",0,handler); }
        protected double adjustTick(double time) { return phase=super.adjustTick(time); }
    }

    private static final class Fixture implements CoreGeoModel<Bird> {
        final AnimationProcessor<Bird> processor = new AnimationProcessor<>(this);
        final Map<String, GeoBone> bones = new HashMap<>();
        final BakedAnimations animations;
        Fixture(Species species) throws Exception {
            Path assets = Path.of("src/main/resources/assets/guaniao");
            JsonObject raw = JsonParser.parseString(Files.readString(assets.resolve("animations/" + species.name + ".animation.json")))
                    .getAsJsonObject().getAsJsonObject("animations");
            animations = JsonUtil.GEO_GSON.fromJson(raw, BakedAnimations.class);
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
            if(bird.driven) bird.controller.sampleTravel(time,bird.x,0,true,1);
            var state = new AnimationState<>(bird, 0, 0, 0, false);
            state.animationTick = time;
            processor.tickAnimation(bird, this, bird.manager, time, state, true);
        }
        public CoreBakedGeoModel getBakedGeoModel(String location) { return null; }
        public AnimationProcessor<Bird> getAnimationProcessor() { return processor; }
        public void handleAnimations(Bird bird, long id, AnimationState<Bird> state) {}
        public Animation getAnimation(Bird bird, String name) { return animations.getAnimation(name); }
    }
}
