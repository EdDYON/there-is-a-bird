package EdDYON.guaniao.content.camera;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Appearance belongs to this frame item, independently of the referenced photograph. */
public record PhotographFrameStyle(Shape shape, Material material, Ornament ornament) {
    public static final String TAG = "PhotoFrame";
    public static final PhotographFrameStyle DEFAULT = new PhotographFrameStyle(Shape.LEGACY, Material.OAK, Ornament.NONE);

    public PhotographFrameStyle {
        shape = shape == null ? Shape.LEGACY : shape;
        material = material == null ? Material.OAK : material;
        ornament = ornament == null ? Ornament.NONE : ornament;
    }

    public static PhotographFrameStyle read(ItemStack stack) {
        CompoundTag data = EdDYON.guaniao.util.ItemData.read(stack);
        CompoundTag tag = data != null && data.contains(TAG, net.minecraft.nbt.Tag.TAG_COMPOUND) ? data.getCompound(TAG) : null;
        if (tag == null) {
            return DEFAULT;
        }
        return new PhotographFrameStyle(Shape.fromId(tag.getString("Shape")),
                Material.fromId(tag.getString("Material")), Ornament.fromId(tag.getString("Ornament")));
    }

    public static void write(ItemStack stack, PhotographFrameStyle style) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("Shape", style.shape.id());
        tag.putString("Material", style.material.id());
        tag.putString("Ornament", style.ornament.id());
        CompoundTag data = EdDYON.guaniao.util.ItemData.copy(stack);
        data.put(TAG, tag);
        EdDYON.guaniao.util.ItemData.write(stack, data);
    }

    public PhotographFrameStyle withShape(Shape shape) {
        return new PhotographFrameStyle(shape, this.material, this.ornament);
    }

    public PhotographFrameStyle withMaterial(Material material) {
        return new PhotographFrameStyle(this.shape, material, this.ornament);
    }

    public PhotographFrameStyle withOrnament(Ornament ornament) {
        return new PhotographFrameStyle(this.shape, this.material, ornament);
    }

    public enum Shape {
        LEGACY("legacy", 12, 12),
        SQUARE("square", 16, 16),
        LARGE("large", 32, 32),
        LANDSCAPE("landscape", 32, 16),
        PORTRAIT("portrait", 16, 32),
        ROUND("round", 32, 32),
        ARCH("arch", 16, 32);

        private final String id;
        private final int widthPixels;
        private final int heightPixels;

        Shape(String id, int widthPixels, int heightPixels) {
            this.id = id;
            this.widthPixels = widthPixels;
            this.heightPixels = heightPixels;
        }

        public String id() { return this.id; }
        public int widthPixels() { return this.widthPixels; }
        public int heightPixels() { return this.heightPixels; }

        public static Shape fromId(String id) {
            for (Shape value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return LEGACY;
        }
    }

    public enum Material {
        OAK("oak"), SPRUCE("spruce"), BIRCH("birch"), COPPER("copper");

        private final String id;

        Material(String id) { this.id = id; }
        public String id() { return this.id; }

        public static Material fromId(String id) {
            for (Material value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return OAK;
        }
    }

    public enum Ornament {
        NONE("none"), LEAVES("leaves"), FEATHER("feather");

        private final String id;

        Ornament(String id) { this.id = id; }
        public String id() { return this.id; }

        public static Ornament fromId(String id) {
            for (Ornament value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return NONE;
        }
    }
}
