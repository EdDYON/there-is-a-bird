package EdDYON.guaniao.client.skybird;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public enum SkyBirdSpecies {
    GOOSE(
            new ResourceLocation(GuaniaoMod.MOD_ID, "textures/sky_birds/goose_flying.png"),
            FlightStyle.CROSSING,
            AnimationStyle.CONTINUOUS_FLAP,
            4,
            3,
            1.45F,
            5,
            12,
            0.28D,
            0.38D,
            0,
            0
    ),
    EAGLE(
            new ResourceLocation(GuaniaoMod.MOD_ID, "textures/sky_birds/eagle_flying.png"),
            FlightStyle.ORBIT,
            AnimationStyle.GLIDE_WITH_OCCASIONAL_FLAP,
            4,
            4,
            1.90F,
            1,
            1,
            0.18D,
            0.24D,
            90,
            160
    ),
    SEAGULL(
            new ResourceLocation(GuaniaoMod.MOD_ID, "textures/sky_birds/seagull_flying.png"),
            FlightStyle.COASTAL_GLIDE,
            AnimationStyle.GLIDE_WITH_OCCASIONAL_FLAP,
            4,
            3,
            1.60F,
            3,
            6,
            0.24D,
            0.32D,
            50,
            100
    ),
    SWALLOW(
            new ResourceLocation(GuaniaoMod.MOD_ID, "textures/sky_birds/swallow_flying.png"),
            FlightStyle.AERIAL_DART,
            AnimationStyle.BURST_FLAP_WITH_SHORT_GLIDE,
            4,
            2,
            0.86F,
            4,
            9,
            0.38D,
            0.52D,
            34,
            54
    ),
    CRANE(
            new ResourceLocation(GuaniaoMod.MOD_ID, "textures/sky_birds/crane_flying.png"),
            FlightStyle.GRACEFUL_MIGRATION,
            AnimationStyle.COORDINATED_SLOW_FLAP,
            4,
            5,
            2.15F,
            3,
            6,
            0.24D,
            0.31D,
            0,
            0
    ),
    VULTURE(
            new ResourceLocation(GuaniaoMod.MOD_ID, "textures/sky_birds/vulture_flying.png"),
            FlightStyle.THERMAL_SOAR,
            AnimationStyle.GLIDE_WITH_OCCASIONAL_FLAP,
            4,
            5,
            2.20F,
            1,
            2,
            0.13D,
            0.18D,
            240,
            460
    ),
    STARLING(
            new ResourceLocation(GuaniaoMod.MOD_ID, "textures/sky_birds/starling_flying.png"),
            FlightStyle.MURMURATION,
            AnimationStyle.BURST_FLAP_WITH_SHORT_GLIDE,
            4,
            2,
            0.72F,
            10,
            18,
            0.33D,
            0.44D,
            32,
            48
    );

    /** Every sky-bird texture is a horizontal strip of square 32 px frames. */
    public static final int FRAME_PIXELS = 32;

    private final ResourceLocation texture;
    private final FlightStyle flightStyle;
    private final AnimationStyle animationStyle;
    private final int frameCount;
    private final int frameInterval;
    private final float renderSize;
    private final int minFlockSize;
    private final int maxFlockSize;
    private final double minSpeed;
    private final double maxSpeed;
    private final int minFlapCycle;
    private final int maxFlapCycle;

    SkyBirdSpecies(ResourceLocation texture, FlightStyle flightStyle, AnimationStyle animationStyle,
                   int frameCount, int frameInterval,
                   float renderSize, int minFlockSize, int maxFlockSize,
                   double minSpeed, double maxSpeed, int minFlapCycle, int maxFlapCycle) {
        this.texture = texture;
        this.flightStyle = flightStyle;
        this.animationStyle = animationStyle;
        this.frameCount = frameCount;
        this.frameInterval = frameInterval;
        this.renderSize = renderSize;
        this.minFlockSize = minFlockSize;
        this.maxFlockSize = maxFlockSize;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.minFlapCycle = minFlapCycle;
        this.maxFlapCycle = maxFlapCycle;
    }

    public ResourceLocation texture() {
        return this.texture;
    }

    public FlightStyle flightStyle() {
        return this.flightStyle;
    }

    public AnimationStyle animationStyle() {
        return this.animationStyle;
    }

    public int frameCount() {
        return this.frameCount;
    }

    public int frameInterval() {
        return this.frameInterval;
    }

    public int textureWidthPixels() {
        return this.frameCount * FRAME_PIXELS;
    }

    public float renderSize() {
        return this.renderSize;
    }

    public int randomFlockSize(RandomSource random) {
        return this.minFlockSize + random.nextInt(this.maxFlockSize - this.minFlockSize + 1);
    }

    public double randomSpeed(RandomSource random) {
        return this.minSpeed + random.nextDouble() * (this.maxSpeed - this.minSpeed);
    }

    public int flapCycle(long seed, int birdIndex) {
        if (this.maxFlapCycle <= this.minFlapCycle) {
            return this.minFlapCycle;
        }
        long mixed = seed ^ ((long)birdIndex * 0x9E3779B97F4A7C15L);
        int offset = Math.floorMod((int)(mixed ^ (mixed >>> 32)),
                this.maxFlapCycle - this.minFlapCycle + 1);
        return this.minFlapCycle + offset;
    }

    public enum FlightStyle {
        CROSSING,
        ORBIT,
        COASTAL_GLIDE,
        AERIAL_DART,
        GRACEFUL_MIGRATION,
        THERMAL_SOAR,
        MURMURATION
    }

    public enum AnimationStyle {
        CONTINUOUS_FLAP,
        GLIDE_WITH_OCCASIONAL_FLAP,
        BURST_FLAP_WITH_SHORT_GLIDE,
        COORDINATED_SLOW_FLAP
    }
}
