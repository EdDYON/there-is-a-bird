package EdDYON.guaniao.config;

import EdDYON.guaniao.content.camera.frame.FrameDesign;
import net.neoforged.neoforge.common.ModConfigSpec;

/** World-specific crafting limits. Existing frames keep their saved dimensions. */
public final class FrameWorkbenchConfig {
    public static final int DEFAULT_MIN_PIXELS = 0;
    public static final int DEFAULT_MAX_PIXELS = 160;
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.DoubleValue MIN_SIZE_BLOCKS;
    private static final ModConfigSpec.DoubleValue MAX_SIZE_BLOCKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("frameWorkbench");
        MIN_SIZE_BLOCKS = builder.comment("Minimum width and height shown in the editor, in blocks. Zero is an editable draft only.")
                .defineInRange("minSizeBlocks", 0.0D, 0.0D, 32.0D);
        MAX_SIZE_BLOCKS = builder.comment("Maximum crafted frame width and height, in blocks. Does not resize existing frames.",
                        "Values are rounded inward to a 1/16-block grid. Reversed endpoints are automatically sorted.")
                .defineInRange("maxSizeBlocks", 10.0D, 0.0D, 32.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private FrameWorkbenchConfig() { }

    public static int minSizePixels() {
        if (!SPEC.isLoaded()) return DEFAULT_MIN_PIXELS;
        double lower = Math.min(minBlocks(), maxBlocks());
        return Math.min(FrameDesign.MAX_SIZE, (int)Math.ceil(lower * 16.0D));
    }

    public static int maxSizePixels() {
        if (!SPEC.isLoaded()) return DEFAULT_MAX_PIXELS;
        double upper = Math.max(minBlocks(), maxBlocks());
        // A sub-pixel interval has no exact grid point. Use its nearest upper grid
        // point consistently rather than exposing an inverted range to sliders.
        return Math.max(minSizePixels(), Math.min(FrameDesign.MAX_SIZE, (int)Math.floor(upper * 16.0D)));
    }

    private static double minBlocks() {
        double value = MIN_SIZE_BLOCKS.get();
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(32.0D, value)) : DEFAULT_MIN_PIXELS / 16.0D;
    }

    private static double maxBlocks() {
        double value = MAX_SIZE_BLOCKS.get();
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(32.0D, value)) : DEFAULT_MAX_PIXELS / 16.0D;
    }

    public static boolean allows(FrameDesign design) {
        return design.hasArea() && design.widthPixels() >= minSizePixels() && design.heightPixels() >= minSizePixels()
                && design.widthPixels() <= maxSizePixels() && design.heightPixels() <= maxSizePixels();
    }
}
