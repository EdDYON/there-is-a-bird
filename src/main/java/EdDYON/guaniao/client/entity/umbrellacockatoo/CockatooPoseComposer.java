package EdDYON.guaniao.client.entity.umbrellacockatoo;

import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Adds to local channels before their matrix is built, then restores values AND dirty flags. */
final class CockatooPoseComposer {
    private final Set<GeoBone> active = Collections.newSetFromMap(new IdentityHashMap<>());

    record RenderPose(CockatooExpressionSampler.Pose emotion, float headRoll) {
        CockatooExpressionSampler.Delta bone(String name) {
            var delta = emotion.bone(name);
            double roll = name.equals("head") ? headRoll : name.equals("neck") ? headRoll * 0.35 : 0;
            if (roll == 0) return delta;
            return new CockatooExpressionSampler.Delta(
                    delta.rotation().add(new CockatooExpressionSampler.Vector(0, 0, roll)), delta.position());
        }
    }

    Scope apply(GeoBone bone, CockatooExpressionSampler.Delta delta) {
        if (delta.equals(CockatooExpressionSampler.Delta.ZERO) || !active.add(bone)) return null;
        Scope scope = new Scope(bone);
        var rotation = delta.rotation();
        var position = delta.position();
        bone.setRotX(scope.rx - (float) Math.toRadians(rotation.x()));
        bone.setRotY(scope.ry - (float) Math.toRadians(rotation.y()));
        bone.setRotZ(scope.rz + (float) Math.toRadians(rotation.z()));
        bone.setPosX(scope.px + (float) position.x());
        bone.setPosY(scope.py + (float) position.y());
        bone.setPosZ(scope.pz + (float) position.z());
        return scope;
    }

    final class Scope implements AutoCloseable {
        private final GeoBone bone;
        private final float rx, ry, rz, px, py, pz;
        private final boolean rotationChanged, positionChanged, scaleChanged;

        private Scope(GeoBone bone) {
            this.bone = bone;
            rx = bone.getRotX(); ry = bone.getRotY(); rz = bone.getRotZ();
            px = bone.getPosX(); py = bone.getPosY(); pz = bone.getPosZ();
            rotationChanged = bone.hasRotationChanged();
            positionChanged = bone.hasPositionChanged();
            scaleChanged = bone.hasScaleChanged();
        }

        @Override
        public void close() {
            bone.setRotX(rx); bone.setRotY(ry); bone.setRotZ(rz);
            bone.setPosX(px); bone.setPosY(py); bone.setPosZ(pz);
            bone.resetStateChanges();
            if (rotationChanged) bone.markRotationAsChanged();
            if (positionChanged) bone.markPositionAsChanged();
            if (scaleChanged) bone.markScaleAsChanged();
            active.remove(bone);
        }
    }
}
