package EdDYON.guaniao.client.entity.hummingbird;

import com.eliotlash.mclib.math.IValue;
import com.google.gson.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.CoreBakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.keyframe.*;
import software.bernie.geckolib.core.molang.MolangParser;
import software.bernie.geckolib.core.object.Axis;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.util.JsonUtil;

/** Supplied Bedrock-export expectations, consumed by the real GeckoLib loader/controller/easing pipeline. */
public final class HummingbirdAnimationTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/guaniao");
    private static final Path REFERENCES = Path.of("src/test/resources/guaniao");
    private static final String[] AXES = {"x", "y", "z"};
    private static final Set<String> WINGS = Set.of("wing_left", "wing_right");
    private static final Pattern EXPONENT_LITERAL = Pattern.compile("(?<![A-Za-z_])(?:\\d+(?:\\.\\d*)?|\\.\\d+)[eE][+-]?\\d+");
    private static JsonObject raw, geometry, author;
    private static BakedAnimations animations;
    private static Method pointAtTick;
    private static AnimationController<Bird> sampler;
    private static int checks;

    public static void main(String[] args) throws Exception {
        raw = JsonParser.parseString(Files.readString(ASSETS.resolve("animations/hummingbird_runtime.animation.json")))
                .getAsJsonObject().getAsJsonObject("animations");
        geometry = JsonParser.parseString(Files.readString(ASSETS.resolve("geo/hummingbird.geo.json")))
                .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        author = JsonParser.parseString(Files.readString(REFERENCES.resolve("hummingbird-author-reference.json"))).getAsJsonObject();
        suppliedAssetsRemainByteExact();
        // This invokes BakedAnimationsAdapter, including its real expression baking and unit conversion.
        animations = JsonUtil.GEO_GSON.fromJson(raw, BakedAnimations.class);
        for (String name : raw.keySet()) check(animations.getAnimation(name) != null, "GeckoLib loaded clip " + name);
        pointAtTick = AnimationController.class.getDeclaredMethod("getAnimationPointAtTick", List.class, double.class, boolean.class, Axis.class);
        pointAtTick.setAccessible(true);
        sampler = new AnimationController<>(new Bird(), "sample", 0, state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.idle")));
        molangNumericLiteralsAndHeadCurveRegression();
        authoredKeysAndLinearSegments();
        authoredCatmullSamples();
        layerScopesAndFlightEndpoints();
        directionalFlightSamples();
        realControllerClocksAndIsolation();
        realDirectionalFlightContinuity();
        exportPoses();
        System.out.println("PASS: hummingbird real GeckoLib loader, all six authored clips, Catmull controls/interiors, layer masks, flight endpoints and clocks ("
                + checks + " checks)");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void near(double actual, double expected, String message) {
        check(Double.isFinite(actual) && Math.abs(actual - expected) < 0.00015,
                message + ": " + actual + " != " + expected);
    }
    private static void suppliedAssetsRemainByteExact() throws Exception {
        Map<String, String> paths = Map.of("animation", "animations/hummingbird.animation.json",
                "geometry", "geo/hummingbird.geo.json", "texture", "textures/entity/hummingbird.png");
        for (var asset : paths.entrySet()) {
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(ASSETS.resolve(asset.getValue()))));
            JsonObject source = author.getAsJsonObject("source_assets").getAsJsonObject(asset.getKey());
            check(actual.equals(source.get("sha256").getAsString()),
                    "supplied " + source.get("filename").getAsString() + " stays byte-for-byte unchanged at " + asset.getValue());
        }
        JsonObject supplied = JsonParser.parseString(Files.readString(ASSETS.resolve("animations/hummingbird.animation.json")))
                .getAsJsonObject().getAsJsonObject("animations");
        check(supplied.equals(author.getAsJsonObject("animations")), "the independent test fixture contains the supplied Bedrock clips exactly");
    }
    private static void variables(double time, double blend) {
        variables(time, blend, 0, 0, 0, 0, time);
    }
    private static void variables(double time, double forward, double backward, double lateral,
                                  double bank, double wingYawBias, double strokeTime) {
        MolangParser p = MolangParser.INSTANCE;
        p.setValue("query.anim_time", () -> time);
        p.setValue("variable.flight_time", () -> time);
        p.setValue("variable.wing_stroke_time", () -> strokeTime);
        p.setValue("variable.forward_blend", () -> forward);
        p.setValue("variable.backward_blend", () -> backward);
        p.setValue("variable.lateral_blend", () -> lateral);
        p.setValue("variable.wing_pitch", () -> 40 - 7.5 * forward + 7.5 * backward);
        p.setValue("variable.bank_angle", () -> bank);
        p.setValue("variable.wing_yaw_bias", () -> wingYawBias);
    }
    private static double authorValue(SourceFrame frame, int axis, double time) throws Exception {
        MolangParser.INSTANCE.setValue("query.anim_time", () -> time);
        return MolangParser.parseJson(frame.vector().get(axis)).get();
    }
    // These inputs are already Bedrock-export values. GeckoLib negates rotation X/Y once;
    // positional values are consumed directly, without another Blockbench export transform.
    private static double sourceToRuntime(String channel, int axis, double value) {
        return channel.equals("rotation") ? Math.toRadians(value) * (axis == 2 ? 1 : -1) : value;
    }
    private static void molangNumericLiteralsAndHeadCurveRegression() throws Exception {
        assertNoExponentExpressions(raw, "animations");
        variables(2, 0);
        for (String clip : List.of("animation.idle_diff_2", "animation.body.idle_diff_2"))
            near(sample(clip, "head", "position", 1, 2), 0,
                    "head returns to its authored Y origin at 2 seconds, without an exponent becoming a 17-unit offset");
    }
    private static void assertNoExponentExpressions(JsonElement value, String path) {
        if (value.isJsonObject()) {
            for (var entry : value.getAsJsonObject().entrySet()) assertNoExponentExpressions(entry.getValue(), path + "/" + entry.getKey());
        } else if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            for (int index = 0; index < array.size(); index++) assertNoExponentExpressions(array.get(index), path + "[" + index + "]");
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String expression = value.getAsString();
            // GeckoLib parseJson accepts a complete numeric string through Double.parseDouble.
            // Inside a compound expression mclib's decimal-token grammar excludes exponent syntax.
            try { Double.parseDouble(expression); }
            catch (NumberFormatException notANumericLiteral) {
                check(!EXPONENT_LITERAL.matcher(expression).find(), "compound Molang uses parser-safe decimal literals at " + path);
            }
        }
    }
    private static BoneAnimation track(String clip, String bone) {
        Animation animation = animations.getAnimation(clip);
        check(animation != null, "animation exists " + clip);
        return Arrays.stream(animation.boneAnimations()).filter(b -> b.boneName().equals(bone)).findFirst().orElse(null);
    }
    private static KeyframeStack<Keyframe<IValue>> channel(BoneAnimation bone, String channel) {
        return switch (channel) {
            case "rotation" -> bone.rotationKeyFrames();
            case "position" -> bone.positionKeyFrames();
            case "scale" -> bone.scaleKeyFrames();
            default -> throw new AssertionError("Unsupported authored channel " + channel);
        };
    }
    private static List<Keyframe<IValue>> axis(KeyframeStack<Keyframe<IValue>> stack, int axis) {
        return switch (axis) { case 0 -> stack.xKeyframes(); case 1 -> stack.yKeyframes(); default -> stack.zKeyframes(); };
    }
    private static double sample(String clip, String bone, String channel, int axis, double time) throws Exception {
        BoneAnimation track = track(clip, bone);
        check(track != null, "bone track exists " + clip + "/" + bone);
        List<Keyframe<IValue>> frames = axis(channel(track, channel), axis);
        check(!frames.isEmpty(), "channel exists " + clip + "/" + bone + "/" + channel);
        // Calling the library's own keyframe selector avoids implementing a second sampler in this test.
        AnimationPoint point = (AnimationPoint)pointAtTick.invoke(sampler, frames, time * 20, channel.equals("rotation"), Axis.values()[axis]);
        return EasingType.lerpWithOverride(point, null);
    }
    private record SourceFrame(double time, JsonArray vector, String interpolation) { }
    private static JsonArray sourceVector(JsonElement value) {
        if (value.isJsonArray()) return value.getAsJsonArray();
        JsonObject object = value.getAsJsonObject();
        if (object.has("vector")) return sourceVector(object.get("vector"));
        if (object.has("post")) return sourceVector(object.get("post"));
        throw new AssertionError("Unsupported supplied vector payload " + value);
    }
    private static SourceFrame sourceFrame(double time, JsonElement value) {
        String interpolation = value.isJsonObject() && value.getAsJsonObject().has("lerp_mode")
                ? value.getAsJsonObject().get("lerp_mode").getAsString() : "linear";
        return new SourceFrame(time, sourceVector(value), interpolation);
    }
    private static List<SourceFrame> sourceFrames(JsonElement track) {
        if (track.isJsonArray() || track.getAsJsonObject().has("vector") || track.getAsJsonObject().has("post"))
            return List.of(sourceFrame(0, track));
        List<SourceFrame> frames = new ArrayList<>();
        for (var entry : track.getAsJsonObject().entrySet()) frames.add(sourceFrame(Double.parseDouble(entry.getKey()), entry.getValue()));
        frames.sort(Comparator.comparingDouble(SourceFrame::time));
        return frames;
    }
    private static void authoredSample(String clip, String bone, String channel, int axis, double time, double expected, String description) throws Exception {
        near(sample(clip, bone, channel, axis, time), expected, description);
        if (clip.equals("animation.fly") || clip.equals("animation.fly_idle")) return; // Flight endpoints are checked with their own blend values.
        String layer = WINGS.contains(bone) ? "wings" : bone.equals("eye") ? "eyes" : "body";
        String layered = "animation." + layer + "." + clip.substring("animation.".length());
        near(sample(layered, bone, channel, axis, time), expected, "controller layer retains " + description);
    }
    private static void authoredKeysAndLinearSegments() throws Exception {
        check(author.getAsJsonObject("animations").size() == 6, "the source fixture contains all six supplied clips");
        int frameCount = 0, interiorCount = 0;
        for (var clipEntry : author.getAsJsonObject("animations").entrySet()) {
            JsonObject clip = clipEntry.getValue().getAsJsonObject();
            String name = clipEntry.getKey();
            Animation baked = animations.getAnimation(name);
            double length = clip.has("animation_length") ? clip.get("animation_length").getAsDouble() : 0;
            if (length > 0) near(baked.length(), length * 20, "authored duration " + name);
            else check(baked.length() == Double.MAX_VALUE, "unbounded authored clock stays continuous " + name);
            check(baked.loopType() == (clip.has("loop") && clip.get("loop").getAsBoolean() ? Animation.LoopType.LOOP : Animation.LoopType.PLAY_ONCE),
                    "authored loop flag " + name);
            for (var boneEntry : clip.getAsJsonObject("bones").entrySet()) {
                String bone = boneEntry.getKey();
                for (var authored : boneEntry.getValue().getAsJsonObject().entrySet()) {
                    String channel = authored.getKey();
                    List<SourceFrame> frames = sourceFrames(authored.getValue());
                    for (SourceFrame frame : frames) {
                        frameCount++;
                        double time = frame.time(); variables(time, 0);
                        for (int axis = 0; axis < 3; axis++) authoredSample(name, bone, channel, axis, time,
                                sourceToRuntime(channel, axis, authorValue(frame, axis, time)),
                                "authored key " + name + "/" + bone + "/" + channel + "/" + AXES[axis] + "@" + time);
                    }
                    for (int i = 1; i < frames.size(); i++) {
                        SourceFrame a = frames.get(i - 1), b = frames.get(i);
                        if (!a.interpolation().equals("linear") || !b.interpolation().equals("linear")) continue;
                        double start = a.time(), end = b.time();
                        if (end <= start) continue;
                        for (double fraction : new double[] {.25, .5, .75}) {
                            interiorCount++;
                            double time = start + (end - start) * fraction; variables(time, 0);
                            for (int axis = 0; axis < 3; axis++) {
                                double av = authorValue(a, axis, time), bv = authorValue(b, axis, time);
                                authoredSample(name, bone, channel, axis, time, sourceToRuntime(channel, axis, av + (bv - av) * fraction),
                                        "authored linear interior " + name + "/" + bone + "/" + channel + "@" + time);
                            }
                        }
                    }
                }
            }
        }
        check(frameCount > 150 && interiorCount > 100, "nontrivial full source coverage: keys=" + frameCount + ", intervals=" + interiorCount);
    }

    private static void authoredCatmullSamples() throws Exception {
        JsonObject reference = JsonParser.parseString(Files.readString(REFERENCES.resolve("hummingbird-curve-reference.json"))).getAsJsonObject();
        check(reference.get("source_sha256").getAsString().equals(author.getAsJsonObject("source_assets").getAsJsonObject("animation").get("sha256").getAsString()),
                "curve oracle and source fixture refer to the same supplied Bedrock animation");
        int count = 0;
        for (JsonElement entry : reference.getAsJsonArray("curves")) {
            JsonObject curve = entry.getAsJsonObject();
            String clip = curve.get("clip").getAsString(), bone = curve.get("bone").getAsString(), channel = curve.get("channel").getAsString();
            for (JsonElement value : curve.getAsJsonArray("samples")) {
                JsonObject point = value.getAsJsonObject();
                double time = point.get("time").getAsDouble(); variables(time, 0); count++;
                for (int axis = 0; axis < 3; axis++) {
                    double expected = sourceToRuntime(channel, axis, point.getAsJsonArray("bedrock_vector").get(axis).getAsDouble());
                    near(sample(clip, bone, channel, axis, time), expected, "author Catmull interior " + channel + "@" + time);
                    near(sample("animation.body." + clip.substring("animation.".length()), bone, channel, axis, time), expected,
                            "layered Catmull interior " + channel + "@" + time);
                }
            }
        }
        check(count > 100, "dense independent author-curve sampling includes intermediate times: " + count);
    }

    private static void layerScopesAndFlightEndpoints() throws Exception {
        Set<String> names = new HashSet<>();
        for (JsonElement bone : geometry.getAsJsonArray("bones")) names.add(bone.getAsJsonObject().get("name").getAsString());
        Map<String, Set<String>> scopes = new HashMap<>();
        for (String layer : List.of("body", "wings", "eyes")) {
            Set<String> scope = new HashSet<>(); scopes.put(layer, scope);
            for (String clip : raw.keySet()) if (clip.startsWith("animation." + layer + ".")) {
                for (BoneAnimation bone : animations.getAnimation(clip).boneAnimations()) {
                    check(names.contains(bone.boneName()), "layer only targets existing bones " + clip + "/" + bone.boneName());
                    scope.add(bone.boneName());
                }
            }
        }
        for (String a : scopes.keySet()) for (String b : scopes.keySet()) if (!a.equals(b))
            check(Collections.disjoint(scopes.get(a), scopes.get(b)), "controller bone masks do not overwrite one another " + a + "/" + b);
        for (String name : List.of("idle", "flight", "sleep", "idle_diff_1", "idle_diff_2", "nectar_enter", "nectar_loop", "nectar_exit",
                "takeoff", "land", "sleep_enter", "wake", "drop_seed")) check(animations.getAnimation("animation.body." + name) != null, "body controller name exists " + name);
        for (String layer : List.of("wings", "eyes")) for (String name : List.of("idle", "idle_diff_1", "idle_diff_2"))
            check(animations.getAnimation("animation." + layer + "." + name) != null, "detail controller name exists " + layer + "/" + name);
        for (double blend : new double[] {0, 1}) for (double time : new double[] {0, .013, .03125, .0625, .11, .24, .7, 1.2, 2.65, 3.1}) {
            variables(time, blend);
            String source = blend == 0 ? "animation.fly_idle" : "animation.fly";
            for (BoneAnimation bone : animations.getAnimation(source).boneAnimations()) {
                String runtime = WINGS.contains(bone.boneName()) ? "animation.wings.fly"
                        : bone.boneName().equals("eye") ? "animation.eyes.idle" : "animation.body.flight";
                for (String channel : List.of("rotation", "position", "scale")) if (!channel(bone, channel).xKeyframes().isEmpty())
                    for (int axis = 0; axis < 3; axis++) {
                        double expected = sample(source, bone.boneName(), channel, axis, time);
                        near(sample(runtime, bone.boneName(), channel, axis, time), expected,
                                "flight endpoint " + blend + " " + bone.boneName() + "/" + channel + "@" + time);
                    }
            }
            check(MolangParser.INSTANCE.getVariable("variable.wing_pitch").get() > 0,
                    "the runtime exposes the positive wing-pitch control expected by the supplied Bedrock export");
            near(sample("animation.wings.fly", "wing_left", "rotation", 0, time), -Math.toRadians(40 - 7.5 * blend),
                    "GeckoLib applies the supplied positive wing pitch's X conversion exactly once");
        }
        for (double blend : new double[] {.25, .5, .75}) {
            variables(.413, blend);
            double hoverPitch = sample("animation.fly_idle", "all", "rotation", 0, .413);
            double forwardPitch = sample("animation.fly", "all", "rotation", 0, .413);
            near(sample("animation.body.flight", "all", "rotation", 0, .413), hoverPitch + (forwardPitch - hoverPitch) * blend,
                    "flight blend interpolates the newly supplied whole-body pitches without switching clips");
        }
        assertForwardFlightLeansTowardBeak();
    }

    private static void assertForwardFlightLeansTowardBeak() throws Exception {
        JsonObject head = null;
        for (JsonElement entry : geometry.getAsJsonArray("bones")) {
            JsonObject bone = entry.getAsJsonObject();
            if (bone.get("name").getAsString().equals("head")) head = bone;
        }
        check(head != null && head.has("cubes"), "the supplied geometry contains the author's head and beak cubes");
        JsonObject beak = null;
        for (JsonElement entry : head.getAsJsonArray("cubes")) {
            JsonObject cube = entry.getAsJsonObject();
            if (beak == null || cube.getAsJsonArray("origin").get(2).getAsDouble() < beak.getAsJsonArray("origin").get(2).getAsDouble()) beak = cube;
        }
        check(beak != null && beak.getAsJsonArray("size").get(2).getAsDouble() > 3 * beak.getAsJsonArray("size").get(0).getAsDouble(),
                "the foremost head cube is the narrow, extended beak, without requiring an added mouth-tip bone");
        double beakDirectionZ = beak.getAsJsonArray("origin").get(2).getAsDouble()
                - head.getAsJsonArray("pivot").get(2).getAsDouble();
        check(beakDirectionZ < 0, "the model's beak points along local negative Z");
        variables(.413, 1);
        double bodyPitch = sample("animation.body.flight", "all", "rotation", 0, .413);
        double headPitch = sample("animation.body.flight", "head", "rotation", 0, .413);
        // A local upright body axis (0, 1, 0), rotated about X, has forward component sin(pitch).
        // This checks anatomical direction from geometry, not an equality to the exported curve.
        check(Math.sin(bodyPitch) * beakDirectionZ > 0, "forward flight leans toward the beak rather than backwards");
        near(bodyPitch, sample("animation.fly", "all", "rotation", 0, .413), "natural forward lean retains the supplied pitch without an extra sign correction");
        near(bodyPitch + headPitch, 0, "the head compensates the forward lean so the beak remains level");
    }

    private static void directionalFlightSamples() throws Exception {
        for (double time : new double[] {.013, .0625, .24, .7, 1.2, 2.65}) {
            variables(time, 0);
            double hoverBody = sample("animation.body.flight", "all", "rotation", 0, time);
            double hoverHead = sample("animation.body.flight", "head", "rotation", 0, time);
            double hoverRoll = sample("animation.body.flight", "all", "rotation", 2, time);
            double leftYaw = sample("animation.wings.fly", "wing_left", "rotation", 1, time);
            double rightYaw = sample("animation.wings.fly", "wing_right", "rotation", 1, time);
            double previousPitch = hoverBody;
            for (int step = 1; step <= 20; step++) {
                variables(time, 0, step / 20.0, 0, 0, 0, time);
                double body = sample("animation.body.flight", "all", "rotation", 0, time);
                double head = sample("animation.body.flight", "head", "rotation", 0, time);
                check(body > previousPitch && body - previousPitch < Math.toRadians(1),
                        "backwards blending progressively inclines the upright body without a posture jump");
                near(body + head, hoverBody + hoverHead,
                        "the backwards head adjustment compensates body pitch while retaining the authored hover motion");
                previousPitch = body;
            }
            check(previousPitch > 0 && previousPitch < Math.toRadians(20),
                    "backwards flight has a modest opposite lean and stays more upright than 47.5-degree forward flight");
            for (double side : new double[] {-1, 1}) {
                variables(time, 0, 0, side, side * 8, side * 4, time);
                near(sample("animation.body.flight", "all", "rotation", 0, time), hoverBody,
                        "sideways flight does not inherit the full forward body lean");
                near(sample("animation.body.flight", "all", "rotation", 2, time) - hoverRoll, Math.toRadians(side * 8),
                        "lateral bank is applied once, rather than accumulating on another roll");
                double leftOffset = sample("animation.wings.fly", "wing_left", "rotation", 1, time) - leftYaw;
                double rightOffset = sample("animation.wings.fly", "wing_right", "rotation", 1, time) - rightYaw;
                near(leftOffset, rightOffset, "lateral strokes shift both local yaw tracks in the same direction");
                check(Math.abs(leftOffset) > Math.toRadians(2) && Math.abs(leftOffset) < Math.toRadians(4.01),
                        "lateral wing asymmetry is visible but bounded");
                check(leftOffset * side < 0, "left and right side flights reverse their wing-yaw adjustment");
            }
        }
    }

    private static void realControllerClocksAndIsolation() throws Exception {
        Fixture model = new Fixture(); Bird bird = new Bird(), other = new Bird();
        bird.body = "idle"; bird.wings = "idle";
        for (int i = 0; i < 8; i++) model.frame(bird, i);
        bird.body = "flight"; bird.wings = "fly"; bird.blend = 1; bird.flightTime = .413;
        for (int i = 8; i <= 14; i++) {
            model.frame(bird, i);
            variables(.413, 1);
            near(model.rotationDelta("wing_left", 1), sample("animation.fly", "wing_left", "rotation", 1, .413),
                    "real controller transition may reset query.anim_time but never overwrites the independent wing phase");
        }
        bird.body = "idle_diff_2"; bird.wings = "idle_diff_2"; bird.eyes = "idle_diff_2";
        for (int i = 15; i <= 75; i++) {
            model.frame(bird, i);
            double phase = bird.clocks.get("body").phase / 20;
            near(bird.clocks.get("wings").phase, phase * 20, "body and wing detail clocks stay synchronized");
            near(bird.clocks.get("eyes").phase, phase * 20, "body and eye detail clocks stay synchronized");
            variables(phase, 0);
            for (int axis = 0; axis < 3; axis++) near(model.rotationDelta("head", axis),
                    sample("animation.idle_diff_2", "head", "rotation", axis, phase), "real processor evaluates Catmull on its actual detail clock");
            float[] before = model.pose("head");
            other.body = "flight"; other.wings = "fly"; other.flightTime = i * .047; other.blend = (i % 4) / 3.0;
            model.frame(other, 100 + i);
            model.frame(bird, i);
            float[] after = model.pose("head");
            for (int axis = 0; axis < before.length; axis++) near(after[axis], before[axis], "other bird rendering cannot retain detail pose/query values");
        }
    }

    private static void realDirectionalFlightContinuity() throws Exception {
        Fixture model = new Fixture(); Bird bird = new Bird();
        for (int tick = 0; tick < 8; tick++) model.frame(bird, tick);
        double continuousStroke = 13.123;
        for (int tick = 0; tick <= 200; tick++) {
            int stage = tick / 40;
            double amount = (tick % 40) / 39.0;
            bird.blend = stage == 0 ? 1 - amount : stage == 4 ? amount : stage == 5 ? 1 : 0;
            bird.backward = stage == 1 ? amount : stage == 2 ? 1 - amount : 0;
            bird.lateral = stage == 0 || stage == 1 ? Math.sin(amount * Math.PI)
                    : stage == 2 || stage == 3 ? -Math.sin(amount * Math.PI) : 0;
            bird.bank = bird.lateral * 8; bird.wingYawBias = bird.lateral * 4;
            bird.flightTime = tick / 20.0;
            // Deliberately vary phase speed independently of the body clock without resetting it.
            continuousStroke += .05 + .01 * bird.backward;
            bird.strokeTime = continuousStroke;
            // Use the unchanged author's flap as the oracle, with an intentionally distinct
            // continuous clock. Taking the two wings' difference cancels the yaw-bias overlay.
            variables(bird.strokeTime, 0);
            double expectedStroke = (sample("animation.fly", "wing_left", "rotation", 1, bird.strokeTime)
                    - sample("animation.fly", "wing_right", "rotation", 1, bird.strokeTime)) / 2;
            double expectedPosition = sample("animation.fly", "wing_left", "position", 2, bird.strokeTime);
            double expectedBodyRoll = sample("animation.fly_idle", "all", "rotation", 2, bird.strokeTime);
            double expectedBodyPosition = sample("animation.fly_idle", "all", "position", 2, bird.strokeTime);
            model.frame(bird, tick + 8);
            near((model.rotationDelta("wing_left", 1) - model.rotationDelta("wing_right", 1)) / 2,
                    expectedStroke, "changing forward, side and backward poses retains the independent authored wing phase");
            near(model.bones.get("wing_left").getPosZ(), expectedPosition,
                    "wing stroke position also keeps its phase through directional changes and clip loop boundaries");
            near(model.rotationDelta("all", 2), expectedBodyRoll + Math.toRadians(bird.bank),
                    "the body's authored flap response shares the wing clock while bank remains an independent overlay");
            near(model.bones.get("all").getPosZ(), expectedBodyPosition,
                    "high-frequency body displacement stays synchronized with the continuous wing phase");
        }
    }

    private static void exportPoses() throws Exception {
        JsonObject result = new JsonObject(); result.addProperty("units", "GeckoLib local rotations in radians; offsets in model units; scales are multipliers");
        for (String kind : List.of("hover", "fly", "backward", "side_left", "side_right", "sleep", "idle", "idle_diff_2")) {
            double time = kind.equals("idle_diff_2") ? .3333333333333333 : .413;
            double blend = kind.equals("fly") ? 1 : 0;
            double side = kind.equals("side_left") ? -1 : kind.equals("side_right") ? 1 : 0;
            variables(time, blend, kind.equals("backward") ? 1 : 0, side, side * 8, side * 4, time);
            boolean flying = Set.of("hover", "fly", "backward", "side_left", "side_right").contains(kind);
            String body = flying ? "flight" : kind;
            String wing = kind.equals("idle_diff_2") ? kind : flying ? "fly" : "idle";
            String eye = kind.equals("idle_diff_2") ? kind : kind.equals("sleep") ? "sleep" : "idle";
            JsonObject pose = new JsonObject(); pose.addProperty("sample_seconds", time);
            JsonObject bones = new JsonObject();
            for (JsonElement value : geometry.getAsJsonArray("bones")) {
                JsonObject bone = value.getAsJsonObject(); String name = bone.get("name").getAsString();
                String clip = "animation." + (WINGS.contains(name) ? "wings." + wing : name.equals("eye") ? "eyes." + eye : "body." + body);
                BoneAnimation track = track(clip, name);
                JsonObject transforms = new JsonObject();
                for (String channel : List.of("rotation", "position", "scale")) {
                    JsonArray vector = new JsonArray();
                    for (int axis = 0; axis < 3; axis++) {
                        double v = channel.equals("scale") ? 1 : 0;
                        if (track != null && !axis(channel(track, channel), axis).isEmpty()) v = sample(clip, name, channel, axis, time);
                        if (channel.equals("rotation") && bone.has("rotation")) v += Math.toRadians(bone.getAsJsonArray("rotation").get(axis).getAsDouble()) * (axis == 2 ? 1 : -1);
                        vector.add(v);
                    }
                    transforms.add(channel, vector);
                }
                bones.add(name, transforms);
            }
            pose.add("bones", bones); result.add(kind, pose);
        }
        Path out = Path.of("build/verification/hummingbird-animation-review/geckolib-sampled-poses.json");
        Files.createDirectories(out.getParent()); Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }

    private static final class Bird implements GeoAnimatable {
        String body = "flight", wings = "fly", eyes = "idle";
        double flightTime, blend, backward, lateral, bank, wingYawBias;
        double strokeTime = Double.NaN;
        final Map<String, Clock> clocks = new HashMap<>();
        final AnimatableManager<Bird> manager = new AnimatableManager<>(this);
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            for (String layer : List.of("body", "wings", "eyes")) {
                Clock controller = new Clock(this, layer, state -> {
                    String clip = switch (layer) { case "body" -> body; case "wings" -> wings; default -> eyes; };
                    RawAnimation animation = RawAnimation.begin();
                    return state.setAndContinue(clip.startsWith("idle_diff_") ? animation.thenPlayAndHold("animation." + layer + "." + clip)
                            : animation.thenLoop("animation." + layer + "." + clip));
                });
                clocks.put(layer, controller); controllers.add(controller);
            }
        }
        public AnimatableInstanceCache getAnimatableInstanceCache() { return null; }
        public double getTick(Object unused) { return 0; }
    }
    private static final class Clock extends AnimationController<Bird> {
        double phase;
        Clock(Bird bird, String name, AnimationStateHandler<Bird> handler) { super(bird, name, 0, handler); }
        @Override protected double adjustTick(double tick) { return phase = super.adjustTick(tick); }
    }
    private static final class Fixture implements CoreGeoModel<Bird> {
        final AnimationProcessor<Bird> processor = new AnimationProcessor<>(this);
        final Map<String, GeoBone> bones = new HashMap<>();
        Fixture() {
            for (JsonElement value : geometry.getAsJsonArray("bones")) {
                JsonObject json = value.getAsJsonObject(); String name = json.get("name").getAsString();
                GeoBone bone = new GeoBone(null, name, false, 0d, false, false);
                if (json.has("rotation")) {
                    var rotation = json.getAsJsonArray("rotation");
                    bone.setRotX((float)-Math.toRadians(rotation.get(0).getAsDouble()));
                    bone.setRotY((float)-Math.toRadians(rotation.get(1).getAsDouble()));
                    bone.setRotZ((float)Math.toRadians(rotation.get(2).getAsDouble()));
                }
                bone.saveInitialSnapshot(); bone.resetStateChanges(); bones.put(name, bone); processor.registerGeoBone(bone);
            }
        }
        void frame(Bird bird, double tick) {
            AnimationState<Bird> state = new AnimationState<>(bird, 0, 0, 0, false); state.animationTick = tick;
            processor.preAnimationSetup(bird, tick);
            processor.tickAnimation(bird, this, bird.manager, tick, state, true);
        }
        double rotationDelta(String name, int axis) {
            GeoBone bone = bones.get(name); var initial = bone.getInitialSnapshot();
            return switch (axis) { case 0 -> bone.getRotX() - initial.getRotX(); case 1 -> bone.getRotY() - initial.getRotY(); default -> bone.getRotZ() - initial.getRotZ(); };
        }
        float[] pose(String name) {
            GeoBone b = bones.get(name);
            return new float[] {b.getRotX(), b.getRotY(), b.getRotZ(), b.getPosX(), b.getPosY(), b.getPosZ(), b.getScaleX(), b.getScaleY(), b.getScaleZ()};
        }
        public void applyMolangQueries(Bird bird, double tick) {
            variables(bird.flightTime, bird.blend, bird.backward, bird.lateral, bird.bank, bird.wingYawBias,
                    Double.isNaN(bird.strokeTime) ? bird.flightTime : bird.strokeTime);
            // Intentionally wrong: real controllers must replace query.anim_time with their own clock.
            MolangParser.INSTANCE.setValue("query.anim_time", () -> 987.654);
        }
        public CoreBakedGeoModel getBakedGeoModel(String location) { return null; }
        public AnimationProcessor<Bird> getAnimationProcessor() { return processor; }
        public void handleAnimations(Bird bird, long id, AnimationState<Bird> state) { }
        public Animation getAnimation(Bird bird, String name) { return animations.getAnimation(name); }
    }
}
