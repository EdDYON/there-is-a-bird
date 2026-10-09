package EdDYON.guaniao.client.entity.umbrellacockatoo;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.IValue;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.Keyframe;
import software.bernie.geckolib.core.keyframe.KeyframeStack;

import java.util.List;

/** A derived animation, never a mutation of the author's idle_diff_2 or GeckoLib's shared cache. */
final class CockatooDisplayResidual {
    static final String NAME = "animation.idle_diff_2_residual";

    static Animation create(Animation source, CockatooExpressionSampler.Pose reference) {
        BoneAnimation[] result = new BoneAnimation[source.boneAnimations().length];
        for (int i = 0; i < result.length; i++) {
            BoneAnimation bone = source.boneAnimations()[i];
            var delta = reference.bone(bone.boneName());
            result[i] = new BoneAnimation(bone.boneName(),
                    subtract(bone.rotationKeyFrames(), delta.rotation(), true),
                    subtract(bone.positionKeyFrames(), delta.position(), false), bone.scaleKeyFrames());
        }
        return new Animation(NAME, source.length(), source.loopType(), result, source.keyFrames());
    }

    private static KeyframeStack<Keyframe<IValue>> subtract(KeyframeStack<Keyframe<IValue>> frames,
                                                            CockatooExpressionSampler.Vector reference,
                                                            boolean rotation) {
        return new KeyframeStack<>(subtract(frames.xKeyframes(), reference.x(), rotation, -1),
                subtract(frames.yKeyframes(), reference.y(), rotation, -1),
                subtract(frames.zKeyframes(), reference.z(), rotation, 1));
    }

    private static List<Keyframe<IValue>> subtract(List<Keyframe<IValue>> frames, double reference,
                                                  boolean rotation, int sign) {
        if (reference == 0) return frames;
        return frames.stream().map(frame -> new Keyframe<>(frame.length(),
                subtract(frame.startValue(), reference, rotation, sign),
                subtract(frame.endValue(), reference, rotation, sign), frame.easingType(), frame.easingArgs())).toList();
    }

    private static IValue subtract(IValue value, double reference, boolean rotation, int sign) {
        // 4.4.9 bakes Constant rotation values into signed radians. Molang expressions
        // remain in author degrees until AnimationController evaluates them each frame.
        if (value instanceof Constant) {
            return new Constant(value.get() - (rotation ? Math.toRadians(reference) * sign : reference));
        }
        return () -> value.get() - reference;
    }
}
