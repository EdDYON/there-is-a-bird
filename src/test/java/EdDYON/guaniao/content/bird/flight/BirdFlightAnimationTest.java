package EdDYON.guaniao.content.bird.flight;

import java.util.Map;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.keyframe.BoneAnimation;
import software.bernie.geckolib.animation.keyframe.event.data.CustomInstructionKeyframeData;
import software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.animation.keyframe.event.data.SoundKeyframeData;

/** Standalone regression using the real GeckoLib controller, without a Minecraft client. */
public final class BirdFlightAnimationTest {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");

    public static void main(String[] args) {
        for (int transition : new int[]{0, 4, 5}) {
            Fixture original = new Fixture(false, transition);
            original.frame(0);
            original.bird.flying = true;
            original.frame(0);
            original.frame(20);
            check(original.stage().equals("idle"), "reproduce missed flight handoff");

            Fixture fixed = new Fixture(true, transition);
            fixed.frame(0);
            fixed.bird.flying = true;
            fixed.frame(0);
            fixed.frame(20);
            check(fixed.stage().equals("fly"), "recover missed flight handoff");
            int resets = fixed.controller.resets;
            for (int frame = 81; frame < 400; frame++) fixed.frame(frame / 4.0);
            check(fixed.stage().equals("fly"), "flight continues over many loops");
            check(resets == 1 && fixed.controller.resets == resets, "do not restart healthy flight every frame");
            fixed.bird.flying = false;
            fixed.frame(101);
            fixed.frame(110);
            check(fixed.stage().equals("idle"), "landing still returns to idle");

            Fixture normal = new Fixture(true, transition);
            normal.frame(0);
            normal.frame(10);
            normal.bird.flying = true;
            normal.frame(11);
            normal.frame(20);
            check(normal.stage().equals("fly") && normal.controller.resets == 0, "normal takeoff needs no recovery");
            checkSequence(transition, false);
            checkSequence(transition, true);
        }
        System.out.println("PASS: real GeckoLib handoff recovery, uninterrupted loops, landing, normal takeoff, multi-stage takeoff/glide and glide-to-flap (0/4/5 tick transitions)");
    }

    private static void checkSequence(int transition, boolean missedHandoff) {
        Fixture fixture = new Fixture(true, transition);
        fixture.bird.flightAnimation = RawAnimation.begin().thenPlay("takeoff").thenLoop("glide");
        fixture.frame(0);
        if (!missedHandoff) fixture.frame(10);
        fixture.bird.flying = true;
        fixture.frame(missedHandoff ? 0 : 11);
        fixture.frame(20);
        check(fixture.stage().equals("takeoff"), "sequence starts with takeoff");
        boolean reachedGlide = false;
        for (int frame = 81; frame < 400; frame++) {
            fixture.frame(frame / 4.0);
            if (fixture.stage().equals("glide")) reachedGlide = true;
            check(!reachedGlide || fixture.stage().equals("glide"), "cruise must not restart takeoff");
        }
        check(reachedGlide, "takeoff advances to glide");
        check(fixture.controller.resets == (missedHandoff ? 1 : 0), "valid second stage needs no recovery");
        fixture.bird.flightAnimation = FLY;
        fixture.frame(101);
        fixture.frame(110);
        check(fixture.stage().equals("fly"), "gliding can resume flapping");
        fixture.bird.flying = false;
        fixture.frame(111);
        fixture.frame(120);
        check(fixture.stage().equals("idle"), "multi-stage flight can land");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture {
        final Bird bird = new Bird();
        final Model model = new Model();
        final CountingController controller;

        Fixture(boolean recover, int transition) {
            controller = new CountingController(bird, state -> {
                state.getController().transitionLength(bird.flying ? transition : 5);
                return bird.flying && recover
                        ? BirdFlightAnimation.play(state, bird.flightAnimation)
                        : state.setAndContinue(bird.flying ? bird.flightAnimation : IDLE);
            });
        }

        void frame(double time) {
            controller.process(model, new AnimationState<>(bird, 0, 0, 0, false).withController(controller),
                    Map.of(), Map.of(), time, false);
        }

        String stage() {
            return controller.getCurrentAnimation().animation().name();
        }
    }

    private static final class CountingController extends AnimationController<Bird> {
        int resets;

        CountingController(Bird bird, AnimationStateHandler<Bird> handler) {
            super(bird, "movement", 5, handler);
        }

        @Override
        public void forceAnimationReset() {
            resets++;
            super.forceAnimationReset();
        }
    }

    private static final class Bird implements GeoAnimatable {
        boolean flying;
        RawAnimation flightAnimation = FLY;
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
        public AnimatableInstanceCache getAnimatableInstanceCache() { return null; }
        public double getTick(Object object) { return 0; }
    }

    private static final class Model extends GeoModel<Bird> {
        private final AnimationProcessor<Bird> processor = new AnimationProcessor<>(this);
        public ResourceLocation getModelResource(Bird bird) { return ResourceLocation.fromNamespaceAndPath("guaniao", "test_model"); }
        public ResourceLocation getTextureResource(Bird bird) { return ResourceLocation.fromNamespaceAndPath("guaniao", "test_texture"); }
        public ResourceLocation getAnimationResource(Bird bird) { return ResourceLocation.fromNamespaceAndPath("guaniao", "test_animation"); }
        public AnimationProcessor<Bird> getAnimationProcessor() { return processor; }
        public void handleAnimations(Bird bird, long id, AnimationState<Bird> state) {}

        public Animation getAnimation(Bird bird, String name) {
            return new Animation(name, 6, name.equals("takeoff") ? Animation.LoopType.PLAY_ONCE : Animation.LoopType.LOOP,
                    new BoneAnimation[0],
                    new Animation.Keyframes(new SoundKeyframeData[0], new ParticleKeyframeData[0],
                            new CustomInstructionKeyframeData[0]));
        }
    }
}
