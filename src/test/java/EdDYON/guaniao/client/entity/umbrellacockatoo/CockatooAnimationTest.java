package EdDYON.guaniao.client.entity.umbrellacockatoo;

import EdDYON.guaniao.content.bird.umbrellacockatoo.CockatooDisplayState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.CoreBakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.util.JsonUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Standalone, real GeckoLib 4.4.9 regressions; no Minecraft client or OpenGL required. */
public final class CockatooAnimationTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/guaniao");
    private static CockatooExpressionSampler curves;
    private static BakedAnimations animations;
    private static JsonObject geometry;
    private static Animation residual;
    private static int checks;

    public static void main(String[] args) throws Exception {
        JsonObject raw = JsonParser.parseString(Files.readString(ASSETS.resolve("animations/umbrella_cockatoo.animation.json")))
                .getAsJsonObject().getAsJsonObject("animations");
        geometry = JsonParser.parseString(Files.readString(ASSETS.resolve("geo/umbrella_cockatoo.geo.json")))
                .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        String original = raw.toString();
        curves = new CockatooExpressionSampler(raw);
        animations = JsonUtil.GEO_GSON.fromJson(raw, BakedAnimations.class);
        residual = CockatooDisplayResidual.create(animations.getAnimation("animation.idle_diff_2"), curves.hold(2));
        check(original.equals(raw.toString()), "source JSON is not changed by sampling or residual generation");
        curvesAndTransitions();
        scopesAndIsolation();
        displayChoreography();
        walkAndEat();
        reconstructDisplay();
        displayInterruptions();
        System.out.println("PASS: cockatoo authored curves, transitions, real GeckoLib walk/eat/display, scopes and isolation ("
                + checks + " checks)");
    }

    private static void curvesAndTransitions() {
        near(curves.open(1, 0).bone("crest").rotation().x(), 0, "partial starts closed");
        near(curves.open(1, 1.25).bone("crest").rotation().x(), 14.7265625, "Catmull-Rom quarter segment, not linear");
        near(curves.open(1, 5).bone("crest").rotation().x(), 72.5, "authored partial key");
        near(curves.open(1, 7.5).bone("crest").rotation().x(), 77.03125, "preserve authored curve overshoot");
        near(curves.open(1, 100000).bone("crest").rotation().x(), 72.5, "zero-length hold remains finite");
        near(curves.open(2, 3.334).bone("crest").rotation().x(), 83, "full intermediate rotation key");
        near(curves.open(2, 1.666).bone("crest").position().z(), 0, "position starts later than rotation");
        near(curves.open(2, 6.666).bone("crest").position().z(), -1.405, "full position key");
        for (String name : CockatooExpressionSampler.BONES) {
            check(geometry.getAsJsonArray("bones").asList().stream()
                    .anyMatch(b -> b.getAsJsonObject().get("name").getAsString().equals(name)), "mask bone exists: " + name);
            if (!name.equals("crest")) equal(curves.hold(1).bone(name), CockatooExpressionSampler.Delta.ZERO, "partial missing track is zero");
            equal(curves.open(2, 10).bone(name), curves.hold(2).bone(name), "opening ends at full hold");
        }

        CockatooVisualState bird = new CockatooVisualState();
        bird.sample(curves, 1, 0);
        var interrupted = bird.sample(curves, 1, 2);
        samePose(interrupted, bird.sample(curves, 2, 2), "partial opening -> full is continuous");
        samePose(bird.sample(curves, 2, 5), bird.sample(curves, 2, 5), "multiple renders do not advance time");
        samePose(curves.hold(2), bird.sample(curves, 2, 8), "full target reached");
        samePose(curves.hold(2), bird.sample(curves, 1, 8), "full -> partial is continuous");
        samePose(curves.hold(1), bird.sample(curves, 1, 14), "feathers and tail retire on downgrade");
        samePose(curves.hold(1), bird.sample(curves, 1, 18), "same target renewal does not restart opening");
        bird.sample(curves, 0, 18);
        var closing = bird.sample(curves, 0, 21);
        samePose(closing, bird.sample(curves, 2, 21), "interrupted retraction starts from current pose");
        bird.sample(curves, 0, 28);
        samePose(CockatooExpressionSampler.Pose.ZERO, bird.sample(curves, 0, 38), "sleep/relax retracts to zero");
        samePose(CockatooExpressionSampler.Pose.ZERO, bird.sample(curves, 0, 0), "clock reset drops old pose");
        samePose(curves.hold(2), new CockatooVisualState(2).sample(curves, 2, 200),
                "reload/first visible draw of residual restores full reference immediately");
    }

    private static void scopesAndIsolation() {
        CockatooPoseComposer composer = new CockatooPoseComposer();
        GeoBone bone = new GeoBone(null, "crest", false, 0d, false, false);
        bone.setRotX(-1.5f);
        bone.saveInitialSnapshot();
        bone.setRotX(-1.7f); bone.setRotY(0.4f); bone.setRotZ(-0.2f);
        bone.setPosX(3); bone.setPosY(4); bone.setPosZ(5);
        bone.setScaleX(0.7f); bone.setScaleY(0); bone.setScaleZ(1.2f);
        float[] before = values(bone);
        for (int flags = 0; flags < 8; flags++) {
            bone.resetStateChanges();
            if ((flags & 1) != 0) bone.markRotationAsChanged();
            if ((flags & 2) != 0) bone.markPositionAsChanged();
            if ((flags & 4) != 0) bone.markScaleAsChanged();
            for (int pass = 0; pass < 2; pass++) {
                try (var scope = composer.apply(bone, curves.hold(2).bone("crest"))) {
                    near(bone.getRotX(), before[0] - Math.toRadians(100), "add once, retain bind/base pose");
                    near(bone.getPosZ(), before[5] - 1.405, "model units, not divided by 16");
                    near(bone.getScaleX(), before[6], "keep base scale");
                    try (var reentry = composer.apply(bone, curves.hold(2).bone("crest"))) {
                        check(reentry == null, "active recursion is not added twice");
                        near(bone.getRotX(), before[0] - Math.toRadians(100), "reentry keeps current pose");
                    }
                    throw new DrawFailure();
                } catch (DrawFailure expected) {
                    // The try-with-resources must restore even when the draw fails.
                }
                sameValues(values(bone), before, "restore base pose after draw");
                check(bone.hasRotationChanged() == ((flags & 1) != 0), "restore rotation dirty flag");
                check(bone.hasPositionChanged() == ((flags & 2) != 0), "restore position dirty flag");
                check(bone.hasScaleChanged() == ((flags & 4) != 0), "restore scale dirty flag");
                near(bone.getInitialSnapshot().getRotX(), -1.5, "initial snapshot is immutable");
            }
        }
        CockatooVisualState first = new CockatooVisualState(), second = new CockatooVisualState();
        first.sample(curves, 2, 0);
        second.sample(curves, 1, 8);
        samePose(curves.hold(2), first.sample(curves, 2, 20), "first bird retains full display");
        samePose(curves.open(1, 2), second.sample(curves, 1, 10), "second bird/preview owns its own clock");
        var roll = new CockatooPoseComposer.RenderPose(curves.hold(2), 20);
        near(roll.bone("head").rotation().z(), 20, "head roll retained");
        near(roll.bone("neck").rotation().z(), 7, "neck roll follows 0.35");
    }

    private static void displayChoreography() {
        CockatooDisplayState display = new CockatooDisplayState();
        check(!display.holdsFullDisplay(0), "no unsolicited initial display");
        display.request(20);
        check(display.holdsFullDisplay(20) && !display.playsBody(29.99), "unfold before residual");
        check(display.playsBody(30) && display.playsBody(89.99), "bounded body display");
        check(!display.playsBody(90) && display.holdsFullDisplay(94.99), "hold reference across body exit");
        check(!display.holdsFullDisplay(95), "return control to emotional target");
        display.request(100);
        display.update(125, false, false);
        check(!display.playsBody(125) && display.holdsFullDisplay(129.99), "interrupt body immediately, reference fades last");
        check(!display.holdsFullDisplay(130), "interrupt hold ends");
        display.update(150, true, true);
        check(!display.playsBody(155) && display.playsBody(160), "guide uses same unfolding");
        display.update(1000, true, true);
        check(display.playsBody(1000), "same preview does not restart on every render");
        display.update(1001, false, false);
        check(!display.playsBody(1001) && !display.holdsFullDisplay(1006), "guide exits residual and hold");
    }

    private static void displayInterruptions() {
        for (boolean sleep : new boolean[]{false, true}) {
            Fixture fixture = new Fixture("animation.idle");
            fixture.manager.getAnimationControllers().get("movement").transitionLength(5);
            CockatooDisplayState director = new CockatooDisplayState();
            CockatooVisualState visual = new CockatooVisualState();
            CockatooPoseComposer composer = new CockatooPoseComposer();
            for (int frame = 0; frame <= 480; frame++) {
                double time = frame / 4d;
                if (time == 20) director.request(time);
                director.update(time, !sleep || time < 50, false);
                String animation = director.playsBody(time) ? CockatooDisplayResidual.NAME
                        : sleep && time >= 50 ? "animation.sleep_loop" : "animation.idle";
                fixture.bird.animation = sleep && time >= 50
                        ? RawAnimation.begin().thenPlay("animation.sleep").thenLoop("animation.sleep_loop")
                        : RawAnimation.begin().thenLoop(animation);
                int target = director.holdsFullDisplay(time) ? 2 : sleep ? 0 : 1;
                var pose = visual.sample(curves, target, time);
                fixture.frame(time);
                GeoBone crest = fixture.bones.get("crest");
                float base = crest.getRotX();
                try (var scope = composer.apply(crest, pose.bone("crest"))) {
                    double local = -Math.toDegrees(crest.getRotX() - crest.getInitialSnapshot().getRotX());
                    check(local < 115, "display entry/exit never doubles the 100-degree reference");
                }
                near(crest.getRotX(), base, "display interruption leaves processor base untouched");
                if (time == 52 && sleep) samePose(curves.hold(2), pose, "sleep interruption holds reference through residual exit");
                if (time == 120) samePose(curves.hold(sleep ? 0 : 1), pose, "body exit returns to sleep/partial target");
            }
        }
    }

    private static void walkAndEat() {
        Fixture walk = new Fixture("animation.walk");
        CockatooPoseComposer composer = new CockatooPoseComposer();
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (int frame = 0; frame <= 160; frame++) {
            walk.frame(frame / 4d);
            GeoBone crest = walk.bones.get("crest");
            float base = crest.getRotX();
            try (var scope = composer.apply(crest, curves.hold(1).bone("crest"))) {
                double local = -Math.toDegrees(crest.getRotX() - crest.getInitialSnapshot().getRotX());
                if (frame > 4) {
                    min = Math.min(min, local); max = Math.max(max, local);
                    check(local >= 79.999 && local <= 90.001, "walk + partial keeps 80-90 degree oscillation");
                }
                near(crest.getRotX() - base, -Math.toRadians(72.5), "walk base preserved in same channel");
            }
            near(crest.getRotX(), base, "walk snapshot remains base-only");
        }
        check(min < 80.05 && max > 89.95, "walk spans the expected range");
        Fixture eat = new Fixture("animation.eat");
        for (int frame = 0; frame <= 115; frame++) {
            eat.frame(frame);
            for (String name : CockatooExpressionSampler.BONES) {
                GeoBone bone = eat.bones.get(name);
                float[] base = values(bone);
                var delta = curves.hold(2).bone(name);
                try (var scope = composer.apply(bone, delta)) {
                    near(bone.getRotX(), base[0] - Math.toRadians(delta.rotation().x()), "eat + full x: " + name);
                    near(bone.getRotY(), base[1] - Math.toRadians(delta.rotation().y()), "eat + full y: " + name);
                    near(bone.getRotZ(), base[2] + Math.toRadians(delta.rotation().z()), "eat + full z: " + name);
                    near(bone.getPosZ(), base[5] + delta.position().z(), "eat + full position: " + name);
                }
                sameValues(values(bone), base, "eat base restored");
            }
        }
    }

    private static void reconstructDisplay() {
        Fixture authored = new Fixture("animation.idle_diff_2"), derived = new Fixture(CockatooDisplayResidual.NAME);
        CockatooPoseComposer composer = new CockatooPoseComposer();
        for (int frame = 0; frame < 200; frame++) {
            authored.frame(frame / 4d);
            derived.frame(frame / 4d);
            // Zero-duration first-controller setup precedes actual animation evaluation.
            if (frame < 2) continue;
            for (String name : authored.bones.keySet()) {
                GeoBone originalBone = authored.bones.get(name), residualBone = derived.bones.get(name);
                try (var scope = composer.apply(residualBone, curves.hold(2).bone(name))) {
                    sameValues(values(residualBone), values(originalBone), "residual + full reconstructs original: " + name);
                }
            }
        }
        check(residual != animations.getAnimation("animation.idle_diff_2"), "derived animation has its own identity");
    }

    private static float[] values(GeoBone bone) {
        return new float[]{bone.getRotX(), bone.getRotY(), bone.getRotZ(), bone.getPosX(), bone.getPosY(), bone.getPosZ(),
                bone.getScaleX(), bone.getScaleY(), bone.getScaleZ()};
    }

    private static void sameValues(float[] a, float[] b, String message) {
        for (int i = 0; i < a.length; i++) near(a[i], b[i], message + " channel " + i);
    }

    private static void samePose(CockatooExpressionSampler.Pose a, CockatooExpressionSampler.Pose b, String message) {
        for (String name : CockatooExpressionSampler.BONES) equal(a.bone(name), b.bone(name), message + ": " + name);
    }

    private static void equal(CockatooExpressionSampler.Delta a, CockatooExpressionSampler.Delta b, String message) {
        near(a.rotation().x(), b.rotation().x(), message); near(a.rotation().y(), b.rotation().y(), message);
        near(a.rotation().z(), b.rotation().z(), message); near(a.position().x(), b.position().x(), message);
        near(a.position().y(), b.position().y(), message); near(a.position().z(), b.position().z(), message);
    }

    private static void near(double actual, double expected, String message) {
        check(Double.isFinite(actual) && Math.abs(actual - expected) < 0.0001, message + ": " + actual + " != " + expected);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class DrawFailure extends RuntimeException {}

    private static final class Bird implements GeoAnimatable {
        RawAnimation animation;
        Bird(String name) { animation = RawAnimation.begin().thenLoop(name); }
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "movement", 0, state -> state.setAndContinue(animation)));
        }
        public AnimatableInstanceCache getAnimatableInstanceCache() { return null; }
        public double getTick(Object object) { return 0; }
        public double getBoneResetTime() { return 5; }
    }

    private static final class Fixture implements CoreGeoModel<Bird> {
        final Bird bird;
        final AnimatableManager<Bird> manager;
        final AnimationProcessor<Bird> processor = new AnimationProcessor<>(this);
        final Map<String, GeoBone> bones = new HashMap<>();

        Fixture(String animation) {
            bird = new Bird(animation);
            manager = new AnimatableManager<>(bird);
            for (var element : geometry.getAsJsonArray("bones")) {
                JsonObject json = element.getAsJsonObject();
                String name = json.get("name").getAsString();
                GeoBone bone = new GeoBone(null, name, false, 0d, false, false);
                if (json.has("rotation")) {
                    var rotation = json.getAsJsonArray("rotation");
                    bone.setRotX((float) -Math.toRadians(rotation.get(0).getAsDouble()));
                    bone.setRotY((float) -Math.toRadians(rotation.get(1).getAsDouble()));
                    bone.setRotZ((float) Math.toRadians(rotation.get(2).getAsDouble()));
                }
                bone.saveInitialSnapshot();
                bone.resetStateChanges();
                bones.put(name, bone);
                processor.registerGeoBone(bone);
            }
        }

        void frame(double time) {
            var state = new AnimationState<>(bird, 0, 0, 0, false);
            state.animationTick = time;
            processor.tickAnimation(bird, this, manager, time, state, true);
        }

        public CoreBakedGeoModel getBakedGeoModel(String location) { return null; }
        public AnimationProcessor<Bird> getAnimationProcessor() { return processor; }
        public void handleAnimations(Bird bird, long id, AnimationState<Bird> state) {}
        public Animation getAnimation(Bird bird, String name) {
            return name.equals(CockatooDisplayResidual.NAME) ? residual : animations.getAnimation(name);
        }
    }
}
