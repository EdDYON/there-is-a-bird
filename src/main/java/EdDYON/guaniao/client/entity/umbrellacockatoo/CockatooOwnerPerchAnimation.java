package EdDYON.guaniao.client.entity.umbrellacockatoo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.util.JsonUtil;

/** An idle derivative that immediately closes the previous flight's untracked channels. */
final class CockatooOwnerPerchAnimation {
    static final String NAME = "animation.owner_perch";

    private CockatooOwnerPerchAnimation() { }

    static Animation create(JsonObject idle, JsonArray geometryBones) {
        JsonObject pose = idle.deepCopy();
        JsonObject tracks = pose.getAsJsonObject("bones");
        for (var element : geometryBones) {
            String name = element.getAsJsonObject().get("name").getAsString();
            JsonObject bone = tracks.has(name) ? tracks.getAsJsonObject(name) : new JsonObject();
            tracks.add(name, bone);
            if (!bone.has("rotation")) bone.add("rotation", vector(0));
            if (!bone.has("position")) bone.add("position", vector(0));
            if (!bone.has("scale")) bone.add("scale", vector(1));
        }
        JsonObject animations = new JsonObject();
        animations.add(NAME, pose);
        return JsonUtil.GEO_GSON.fromJson(animations, BakedAnimations.class).getAnimation(NAME);
    }

    private static JsonObject vector(int value) {
        JsonArray values = new JsonArray();
        for (int axis = 0; axis < 3; axis++) values.add(value);
        JsonObject vector = new JsonObject();
        vector.add("vector", values);
        return vector;
    }
}
