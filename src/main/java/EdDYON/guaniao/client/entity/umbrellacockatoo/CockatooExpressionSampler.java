package EdDYON.guaniao.client.entity.umbrellacockatoo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads the authored numeric emotion tracks; body Molang stays in GeckoLib. */
final class CockatooExpressionSampler {
    static final List<String> BONES = List.of("crest", "left_1", "right_1", "left_2", "right_2",
            "left_3", "right_3", "left_1B", "right_1B", "left_2B", "right_2B", "left_3B",
            "right_3B", "bone7", "left_4", "right_4", "left_tail_2", "right_tail_2");

    private final Clip[] opening = new Clip[3];
    private final Pose[] holding = {Pose.ZERO, null, null};

    CockatooExpressionSampler(JsonObject animations) {
        for (int level = 1; level <= 2; level++) {
            opening[level] = new Clip(animations.getAsJsonObject("animation.emotion" + level));
            holding[level] = new Clip(animations.getAsJsonObject("animation.emotion" + level + "_loop")).sample(0);
        }
    }

    Pose hold(int level) {
        return holding[level];
    }

    double openingTicks(int level) {
        return opening[level].length * 20;
    }

    Pose open(int level, double ticks) {
        return ticks >= openingTicks(level) ? hold(level) : opening[level].sample(Math.max(0, ticks) / 20);
    }

    /** Author units: degrees and model units. Conversion happens only at the bone write. */
    record Vector(double x, double y, double z) {
        static final Vector ZERO = new Vector(0, 0, 0);

        Vector add(Vector other) {
            return new Vector(x + other.x, y + other.y, z + other.z);
        }

        Vector scale(double weight) {
            return new Vector(x * weight, y * weight, z * weight);
        }

        Vector lerp(Vector other, double weight) {
            return scale(1 - weight).add(other.scale(weight));
        }
    }

    record Delta(Vector rotation, Vector position) {
        static final Delta ZERO = new Delta(Vector.ZERO, Vector.ZERO);

        Delta lerp(Delta other, double weight) {
            return new Delta(rotation.lerp(other.rotation, weight), position.lerp(other.position, weight));
        }
    }

    static final class Pose {
        static final Pose ZERO = new Pose(Map.of());
        private final Map<String, Delta> bones;

        Pose(Map<String, Delta> bones) {
            this.bones = Map.copyOf(bones);
        }

        Delta bone(String name) {
            return bones.getOrDefault(name, Delta.ZERO);
        }

        Pose lerp(Pose target, double weight) {
            Map<String, Delta> result = new HashMap<>();
            for (String name : BONES) {
                result.put(name, bone(name).lerp(target.bone(name), weight));
            }
            return new Pose(result);
        }
    }

    private static final class Clip {
        final double length;
        final Map<String, Track> rotations = new HashMap<>();
        final Map<String, Track> positions = new HashMap<>();

        Clip(JsonObject json) {
            if (json == null) throw new JsonParseException("Missing cockatoo emotion animation");
            length = json.has("animation_length") ? json.get("animation_length").getAsDouble() : 0;
            for (var entry : json.getAsJsonObject("bones").entrySet()) {
                String name = entry.getKey();
                if (!BONES.contains(name)) throw new JsonParseException("Unexpected emotion bone: " + name);
                JsonObject channels = entry.getValue().getAsJsonObject();
                for (String channel : channels.keySet()) {
                    if (!channel.equals("rotation") && !(channel.equals("position") && name.equals("crest"))) {
                        throw new JsonParseException("Unexpected emotion channel: " + name + "." + channel);
                    }
                }
                if (channels.has("rotation")) rotations.put(name, Track.read(channels.get("rotation")));
                if (channels.has("position")) positions.put(name, Track.read(channels.get("position")));
            }
        }

        Pose sample(double seconds) {
            Map<String, Delta> result = new HashMap<>();
            for (String name : BONES) {
                result.put(name, new Delta(sample(rotations.get(name), seconds), sample(positions.get(name), seconds)));
            }
            return new Pose(result);
        }

        private static Vector sample(Track track, double seconds) {
            return track == null ? Vector.ZERO : track.sample(seconds);
        }
    }

    private record Key(double time, Vector pre, Vector post, boolean catmullRom) {}

    private record Track(List<Key> keys) {
        static Track read(JsonElement json) {
            List<Key> keys = new ArrayList<>();
            if (!json.isJsonObject() || json.getAsJsonObject().has("vector")) {
                Vector value = vector(json);
                return new Track(List.of(new Key(0, value, value, false)));
            }
            for (var entry : json.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                JsonObject obj = value.isJsonObject() ? value.getAsJsonObject() : null;
                JsonElement post = obj != null && obj.has("post") ? obj.get("post")
                        : obj != null && obj.has("pre") ? obj.get("pre") : value;
                Vector after = vector(post);
                Vector before = obj != null && obj.has("pre") ? vector(obj.get("pre")) : after;
                String mode = obj != null && obj.has("lerp_mode") ? obj.get("lerp_mode").getAsString() : "linear";
                if (!mode.equals("linear") && !mode.equals("catmullrom")) {
                    throw new JsonParseException("Unsupported emotion interpolation: " + mode);
                }
                keys.add(new Key(Double.parseDouble(entry.getKey()), before, after, mode.equals("catmullrom")));
            }
            keys.sort(Comparator.comparingDouble(Key::time));
            if (keys.isEmpty()) throw new JsonParseException("Empty emotion track");
            return new Track(List.copyOf(keys));
        }

        Vector sample(double time) {
            Key first = keys.get(0);
            if (time < first.time) return first.pre;
            for (int i = 0; i < keys.size() - 1; i++) {
                Key a = keys.get(i), b = keys.get(i + 1);
                if (time == a.time) return a.post;
                if (time >= b.time) continue;
                double u = (time - a.time) / (b.time - a.time);
                if (!a.catmullRom && !b.catmullRom) return a.post.lerp(b.pre, u);
                // Blockbench's uniform Catmull-Rom: duplicate endpoints, use both adjacent keys.
                // A discontinuity must not borrow a tangent across its pre/post jump.
                Vector p0 = i > 0 && a.pre.equals(a.post) ? keys.get(i - 1).post : a.post;
                Vector p3 = i + 2 < keys.size() && b.pre.equals(b.post) ? keys.get(i + 2).pre : b.pre;
                double u2 = u * u, u3 = u2 * u;
                return p0.scale(-0.5 * u3 + u2 - 0.5 * u)
                        .add(a.post.scale(1.5 * u3 - 2.5 * u2 + 1))
                        .add(b.pre.scale(-1.5 * u3 + 2 * u2 + 0.5 * u))
                        .add(p3.scale(0.5 * u3 - 0.5 * u2));
            }
            return keys.get(keys.size() - 1).post;
        }

        private static Vector vector(JsonElement json) {
            if (json.isJsonObject()) return vector(json.getAsJsonObject().get("vector"));
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                double value = number(json);
                return new Vector(value, value, value);
            }
            if (!json.isJsonArray() || json.getAsJsonArray().size() != 3) {
                throw new JsonParseException("Expected a numeric emotion vector: " + json);
            }
            var values = json.getAsJsonArray();
            return new Vector(number(values.get(0)), number(values.get(1)), number(values.get(2)));
        }

        private static double number(JsonElement value) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                    || !Double.isFinite(value.getAsDouble())) {
                throw new JsonParseException("Emotion tracks must contain finite numbers: " + value);
            }
            return value.getAsDouble();
        }
    }
}
