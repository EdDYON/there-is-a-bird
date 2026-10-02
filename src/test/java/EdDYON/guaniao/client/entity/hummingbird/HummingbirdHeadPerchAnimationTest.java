package EdDYON.guaniao.client.entity.hummingbird;

import EdDYON.guaniao.content.bird.BirdHeadPerch;
import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
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
import software.bernie.geckolib.core.molang.MolangParser;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.JsonUtil;
import software.bernie.geckolib.util.RenderUtils;

/** Real GeckoLib geometry, controller reset/blend, and the actual production support constraint. */
public final class HummingbirdHeadPerchAnimationTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/guaniao");
    private static int checks;

    public static void main(String[] args) throws Exception {
        // Test the current zero-transition controllers/default bone-reset period,
        // plus GeckoLib's five-tick blend to ensure contact throughout a transition.
        for (int transitionTicks : new int[]{0, 5}) {
            for (double forward : new double[]{0, 1}) verifyFlightToHeadContact(transitionTicks, forward);
        }
        System.out.println("PASS: actual GeckoLib baked foot geometry, flight-to-idle contact during reset/blend, and preserved head/eye/wing animation ("
                + checks + " checks).");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void near(double actual, double expected, String message) {
        check(Double.isFinite(actual) && Math.abs(actual - expected) < 1.0E-6,
                message + ": " + actual + " != " + expected);
    }

    private static void verifyFlightToHeadContact(int transitionTicks, double forward) throws Exception {
        Fixture f = new Fixture();
        Bird bird = new Bird(transitionTicks, forward);
        Vec3 bindSole = f.sole();
        near(bindSole.y, HummingbirdEntity.HEAD_PERCH_SOLE_Y, "shared sole Y comes from the actual supplied geometry");
        near(-bindSole.z, HummingbirdEntity.HEAD_PERCH_SOLE_FORWARD, "shared forward offset comes from the actual supplied geometry");
        near(bindSole.x, 0, "the paired foot contact is centred across the actual mesh");
        boolean sawFlightDisplacement = false;
        for (int quarter = 0; quarter <= 120; quarter++) {
            double tick = quarter / 4.0;
            f.frame(bird, tick);
            sawFlightDisplacement |= f.sole().distanceTo(bindSole) > .02;
        }
        check(sawFlightDisplacement, "normal hovering/forward flight keeps the authored moving legs before head contact");
        bird.onHead = true;
        double minHeadYaw = Double.POSITIVE_INFINITY, maxHeadYaw = Double.NEGATIVE_INFINITY;
        boolean sawResetDisplacement = false;
        for (int quarter = 121; quarter <= 160; quarter++) {
            double tick = quarter / 4.0;
            f.frame(bird, tick);
            Vec3 beforeContact = f.sole();
            sawResetDisplacement |= beforeContact.distanceTo(bindSole) > .002;
            float[] head = f.pose("head"), eye = f.pose("eye"), wing = f.pose("wing_left");
            // This is the actual production method invoked after the owned-player
            // passenger guard, not a copied bone reset implemented in the test.
            f.production.restoreHeadPerchSupportPose();
            Vec3 sole = f.sole();
            check(sole.distanceTo(bindSole) < 1.0E-6,
                    "the real post-animation constraint fixes foot contact throughout transition at tick=" + tick);
            check(Arrays.equals(head, f.pose("head")) && Arrays.equals(eye, f.pose("eye")) && Arrays.equals(wing, f.pose("wing_left")),
                    "the support constraint preserves head, eye and wing animation at tick=" + tick);
            minHeadYaw = Math.min(minHeadYaw, head[1]); maxHeadYaw = Math.max(maxHeadYaw, head[1]);
            for (float yaw : new float[]{-135, 0, 135}) for (float pitch : new float[]{-90, -45, 0, 45, 90}) {
                Vec3 anchor = BirdHeadPerch.offset(yaw, pitch, HummingbirdEntity.HEAD_PERCH_SOLE_Y,
                        HummingbirdEntity.HEAD_PERCH_SOLE_FORWARD);
                PoseStack renderedBird = new PoseStack();
                renderedBird.mulPose(Axis.YP.rotationDegrees(180 - yaw));
                renderedBird.mulPose(Axis.XP.rotationDegrees(-pitch));
                Vector3f renderedFoot = renderedBird.last().pose().transformPosition(new Vector3f((float)sole.x, (float)sole.y, (float)sole.z));
                Vec3 actual = anchor.add(renderedFoot.x, renderedFoot.y, renderedFoot.z);
                // An independent vanilla crown normal/contact calculation.
                double p = Math.toRadians(pitch), y = Math.toRadians(yaw);
                double crown = 8.5 * .9375 / 16 + .001;
                Vec3 expected = new Vec3(-Math.sin(y) * crown * Math.sin(p),
                        1.501 * .9375 + crown * Math.cos(p), Math.cos(y) * crown * Math.sin(p));
                check(actual.distanceTo(expected) < 1.0E-6,
                        "actual baked feet remain on the rotated crown during controller blending");
            }
        }
        if (transitionTicks == 5) check(sawResetDisplacement, "test actually exercises the mixed flight leg pose before the constraint");
        check(maxHeadYaw - minHeadYaw > .01, "head motion keeps playing while only the support chain is fixed");
        // The same model can render another bird immediately afterwards. Sampling
        // ordinary flight must restore its own moving pose, without perch residue.
        bird.onHead = false;
        boolean resumedFlight = false;
        for (int quarter = 161; quarter <= 200; quarter++) {
            f.frame(bird, quarter / 4.0);
            resumedFlight |= f.sole().distanceTo(bindSole) > .02;
        }
        check(resumedFlight, "ordinary flight after head perching retains the author's moving feet");
    }

    private static final class Bird implements GeoAnimatable {
        boolean onHead;
        double age;
        final double forward;
        final int transitionTicks;
        final AnimatableManager<Bird> manager;

        Bird(int transitionTicks, double forward) {
            this.transitionTicks = transitionTicks; this.forward = forward;
            manager = new AnimatableManager<>(this);
        }

        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            for (String layer : List.of("body", "wings", "eyes")) controllers.add(new AnimationController<>(this, layer, transitionTicks, state -> {
                String clip = layer.equals("body") ? onHead ? "idle" : "flight"
                        : layer.equals("wings") ? onHead ? "idle" : "fly" : "idle";
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation." + layer + "." + clip));
            }));
        }
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return null; }
        @Override public double getTick(Object object) { return age; }
    }

    private static final class Fixture implements CoreGeoModel<Bird> {
        final BakedAnimations animations;
        final BakedGeoModel baked;
        final HummingbirdModel production = new HummingbirdModel();
        final AnimationProcessor<Bird> processor = new AnimationProcessor<>(this);

        Fixture() throws Exception {
            Model rawModel = JsonUtil.GEO_GSON.fromJson(Files.readString(ASSETS.resolve("geo/hummingbird.geo.json")), Model.class);
            baked = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(rawModel));
            // Register the same actual bones in both processors before sampling,
            // so the production model's snapshot restore acts on the tested pose.
            processor.setActiveModel(baked);
            production.getAnimationProcessor().setActiveModel(baked);
            String raw = Files.readString(ASSETS.resolve("animations/hummingbird_runtime.animation.json"));
            animations = JsonUtil.GEO_GSON.fromJson(com.google.gson.JsonParser.parseString(raw).getAsJsonObject().get("animations"), BakedAnimations.class);
        }

        GeoBone bone(String name) { return baked.getBone(name).orElseThrow(); }

        void frame(Bird bird, double tick) {
            bird.age = tick / 20;
            AnimationState<Bird> state = new AnimationState<>(bird, 0, 0, 0, false);
            state.animationTick = tick;
            processor.preAnimationSetup(bird, tick);
            processor.tickAnimation(bird, this, bird.manager, tick, state, true);
        }

        float[] pose(String name) {
            GeoBone b = bone(name);
            return new float[]{b.getRotX(), b.getRotY(), b.getRotZ(), b.getPosX(), b.getPosY(), b.getPosZ(),
                    b.getScaleX(), b.getScaleY(), b.getScaleZ()};
        }

        void transform(PoseStack pose, GeoBone bone) {
            if (bone.getParent() != null) transform(pose, bone.getParent());
            RenderUtils.prepMatrixForBone(pose, bone);
        }

        Vec3 sole() {
            List<Vector3f> vertices = new ArrayList<>();
            for (String name : List.of("foot_left", "foot_right")) {
                GeoBone bone = bone(name);
                for (GeoCube cube : bone.getCubes()) {
                    PoseStack pose = new PoseStack(); transform(pose, bone);
                    RenderUtils.translateToPivotPoint(pose, cube);
                    RenderUtils.rotateMatrixAroundCube(pose, cube);
                    RenderUtils.translateAwayFromPivotPoint(pose, cube);
                    for (var quad : cube.quads()) if (quad != null) for (var vertex : quad.vertices())
                        vertices.add(pose.last().pose().transformPosition(new Vector3f(vertex.position())));
                }
            }
            double minimum = vertices.stream().mapToDouble(vertex -> vertex.y).min().orElseThrow();
            double x = 0, y = 0, z = 0; int count = 0;
            for (Vector3f vertex : vertices) if (vertex.y < minimum + 1.0E-7) {
                x += vertex.x; y += vertex.y; z += vertex.z; count++;
            }
            return new Vec3(x / count, y / count, z / count);
        }

        @Override public void applyMolangQueries(Bird bird, double tick) {
            MolangParser p = MolangParser.INSTANCE;
            p.setValue("query.anim_time", () -> bird.age);
            p.setValue("variable.flight_time", () -> bird.age);
            p.setValue("variable.wing_stroke_time", () -> bird.age);
            p.setValue("variable.forward_blend", () -> bird.forward);
            p.setValue("variable.backward_blend", () -> 0);
            p.setValue("variable.lateral_blend", () -> 0);
            p.setValue("variable.wing_pitch", () -> 40 - 7.5 * bird.forward);
            p.setValue("variable.wing_yaw_bias", () -> 0);
            p.setValue("variable.bank_angle", () -> bird.onHead ? 0 : 7);
        }
        @Override public CoreBakedGeoModel getBakedGeoModel(String location) { return baked; }
        @Override public AnimationProcessor<Bird> getAnimationProcessor() { return processor; }
        @Override public void handleAnimations(Bird bird, long id, AnimationState<Bird> state) { }
        @Override public Animation getAnimation(Bird bird, String name) { return animations.getAnimation(name); }
    }
}
