package EdDYON.guaniao.client.guide;

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
import software.bernie.geckolib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.util.JsonUtil;

/** Reproduces the frozen thumbnail with the actual assets and GeckoLib animation processor. */
public final class BirdGuideStandingPoseTest {
    public static void main(String[] args) throws Exception {
        for (String name : List.of("night_heron", "seagull", "myna", "umbrella_cockatoo")) {
            Fixture model = new Fixture(name);
            String idle = name.equals("night_heron") || name.equals("seagull") ? "idle" : "animation.idle";
            String fly = idle.equals("idle") ? "fly_loop" : "animation.fly";
            List<String> wings = idle.equals("idle") ? List.of("wing_fly_left", "wing_fly_right") : List.of("fly");
            Bird thumbnail = new Bird(idle, name.equals("umbrella_cockatoo") ? 5 : 4);
            for (int frame = 0; frame < 120; frame++) model.frame(thumbnail, frame % 10 / 10d);
            require(model.bones.get(wings.get(0)).getScaleX() > 0.5F, "Old frozen clock reproduces spread wings: " + name);
            Bird flyingPreview = new Bird(fly, 4);
            for (int tick = 1; tick <= 180; tick++) {
                model.frame(flyingPreview, tick);
                model.frame(thumbnail, tick);
                if (tick > 10) for (String wing : wings) {
                    GeoBone bone = model.bones.get(wing);
                    require(Math.abs(bone.getScaleX()) < 0.0001F && Math.abs(bone.getScaleY()) < 0.0001F
                            && Math.abs(bone.getScaleZ()) < 0.0001F, "Thumbnail folds wings even beside flying preview: " + name);
                }
            }
        }
        System.out.println("PASS: actual idle/fly animations of night heron, seagull, myna and umbrella cockatoo; frozen-clock reproduction and 180-tick shared-model isolation");
    }

    private static final class Bird implements GeoAnimatable {
        final RawAnimation animation;
        final int transition;
        final AnimatableManager<Bird> manager;
        Bird(String animation, int transition) {
            this.animation = RawAnimation.begin().thenLoop(animation);
            this.transition = transition;
            this.manager = new AnimatableManager<>(this);
        }
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "movement", this.transition, state -> state.setAndContinue(this.animation)));
        }
        public AnimatableInstanceCache getAnimatableInstanceCache() { return null; }
        public double getTick(Object object) { return 0; }
    }

    private static final class Fixture extends GeoModel<Bird> {
        final AnimationProcessor<Bird> processor = new AnimationProcessor<>(this);
        final Map<String, GeoBone> bones = new HashMap<>();
        final BakedAnimations animations;

        Fixture(String name) throws Exception {
            Path assets = Path.of("src/main/resources/assets/guaniao");
            JsonObject raw = JsonParser.parseString(Files.readString(assets.resolve("animations/" + name + ".animation.json")))
                    .getAsJsonObject().getAsJsonObject("animations");
            this.animations = software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter.GEO_GSON.fromJson(raw, BakedAnimations.class);
            JsonObject geometry = JsonParser.parseString(Files.readString(assets.resolve("geo/" + name + ".geo.json")))
                    .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            for (var element : geometry.getAsJsonArray("bones")) {
                JsonObject json = element.getAsJsonObject();
                String boneName = json.get("name").getAsString();
                GeoBone bone = new GeoBone(null, boneName, false, 0d, false, false);
                if (json.has("rotation")) {
                    var rotation = json.getAsJsonArray("rotation");
                    bone.setRotX((float)-Math.toRadians(rotation.get(0).getAsDouble()));
                    bone.setRotY((float)-Math.toRadians(rotation.get(1).getAsDouble()));
                    bone.setRotZ((float)Math.toRadians(rotation.get(2).getAsDouble()));
                }
                bone.saveInitialSnapshot();
                bone.resetStateChanges();
                this.bones.put(boneName, bone);
                this.processor.registerGeoBone(bone);
            }
        }
        void frame(Bird bird, double time) {
            var state = new AnimationState<>(bird, 0, 0, 0, false);
            state.animationTick = time;
            this.processor.tickAnimation(bird, this, bird.manager, time, state, true);
        }
        public ResourceLocation getModelResource(Bird bird) { return ResourceLocation.fromNamespaceAndPath("guaniao", "test_model"); }
        public ResourceLocation getTextureResource(Bird bird) { return ResourceLocation.fromNamespaceAndPath("guaniao", "test_texture"); }
        public ResourceLocation getAnimationResource(Bird bird) { return ResourceLocation.fromNamespaceAndPath("guaniao", "test_animation"); }
        public AnimationProcessor<Bird> getAnimationProcessor() { return this.processor; }
        public void handleAnimations(Bird bird, long id, AnimationState<Bird> state) {}
        public Animation getAnimation(Bird bird, String name) { return this.animations.getAnimation(name); }
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
