package EdDYON.guaniao.content.camera.frame;

import EdDYON.guaniao.content.camera.PhotographFrameStyle;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/** A bounded, immutable frame design. Photo identity and ownership stay on the enclosing item. */
public record FrameDesign(int widthPixels, int heightPixels, Template template, int[] mask,
                          Wood wood, Trim trim, Mat mat, PhotographFrameStyle.Ornament ornament,
                          int corners, int borderCells, int depthPixels, PhotoFit fit,
                          int zoomPercent, int offsetX, int offsetY, int rotation, List<Decoration> decorations,
                          ResourceLocation frameMaterial, ResourceLocation trimMaterial, ResourceLocation liningMaterial) {
    public static final String TAG = "FrameDesign";
    public static final int GRID = 32;
    public static final int MIN_SIZE = 0;
    public static final int MAX_SIZE = 512;
    public static final int MAX_RESOURCE_ID_LENGTH = 256;
    public static final int MIN_BORDER = 1;
    public static final int MAX_BORDER = 4;
    public static final int MIN_DEPTH = 1;
    public static final int MAX_DEPTH = 3;
    public static final int MIN_ZOOM = 100;
    public static final int MAX_ZOOM = 300;
    public static final int MAX_DECORATIONS = 16;
    public static final int MIN_DECORATION_SIZE = 8;
    public static final int MAX_DECORATION_SIZE = 40;
    public static final FrameDesign DEFAULT = new FrameDesign(16, 16, Template.RECTANGLE,
            templateMask(Template.RECTANGLE), Wood.OAK, Trim.NONE, Mat.PAPER,
            PhotographFrameStyle.Ornament.NONE, 15, 2, 1, PhotoFit.CONTAIN, 100, 0, 0, 0);

    /** Source-compatible constructor for pre-point-decoration designs. */
    public FrameDesign(int widthPixels, int heightPixels, Template template, int[] mask,
                       Wood wood, Trim trim, Mat mat, PhotographFrameStyle.Ornament ornament,
                       int corners, int borderCells, int depthPixels, PhotoFit fit,
                       int zoomPercent, int offsetX, int offsetY, int rotation) {
        this(widthPixels, heightPixels, template, mask, wood, trim, mat, ornament, corners,
                borderCells, depthPixels, fit, zoomPercent, offsetX, offsetY, rotation,
                legacyDecorations(ornament, corners));
    }

    /** Source-compatible constructor for version 2 designs. */
    public FrameDesign(int widthPixels, int heightPixels, Template template, int[] mask,
                       Wood wood, Trim trim, Mat mat, PhotographFrameStyle.Ornament ornament,
                       int corners, int borderCells, int depthPixels, PhotoFit fit,
                       int zoomPercent, int offsetX, int offsetY, int rotation, List<Decoration> decorations) {
        this(widthPixels, heightPixels, template, mask, wood, trim, mat, ornament, corners,
                borderCells, depthPixels, fit, zoomPercent, offsetX, offsetY, rotation, decorations,
                materialId(wood), materialId(trim), materialId(mat));
    }

    public FrameDesign {
        Objects.requireNonNull(template);
        Objects.requireNonNull(wood);
        Objects.requireNonNull(trim);
        Objects.requireNonNull(mat);
        Objects.requireNonNull(ornament);
        Objects.requireNonNull(fit);
        validateResourceId(frameMaterial);
        validateResourceId(trimMaterial);
        validateResourceId(liningMaterial);
        if (!validSize(widthPixels) || !validSize(heightPixels)
                || mask == null || mask.length != GRID
                || corners < 0 || corners > 15 || borderCells < 1 || borderCells > 4
                || depthPixels < 1 || depthPixels > 3 || zoomPercent < 100 || zoomPercent > 300
                || offsetX < -100 || offsetX > 100 || offsetY < -100 || offsetY > 100
                || rotation < 0 || rotation > 3 || decorations == null || decorations.size() > MAX_DECORATIONS) {
            throw new IllegalArgumentException("Frame design outside supported limits");
        }
        mask = mask.clone();
        decorations = List.copyOf(decorations);
        // A template is descriptive; the bounded bitmap remains the exact saved silhouette.
        if (Arrays.stream(mask).allMatch(row -> row == 0)) {
            throw new IllegalArgumentException("Frame silhouette cannot be empty");
        }
    }

    public static boolean validSize(int pixels) {
        return pixels >= MIN_SIZE && pixels <= MAX_SIZE;
    }

    @Override
    public int[] mask() { return this.mask.clone(); }

    public boolean occupied(int x, int y) {
        return x >= 0 && x < GRID && y >= 0 && y < GRID && (this.mask[y] & (1 << x)) != 0;
    }

    public int[] windowMask() { return erode(this.mask, this.borderCells); }

    public static int[] erode(int[] input, int cells) {
        if (input == null || input.length != GRID || cells < 0 || cells > 4) {
            throw new IllegalArgumentException("Invalid erosion input");
        }
        int[] current = input.clone();
        for (int step = 0; step < cells; step++) {
            int[] next = new int[GRID];
            for (int y = 1; y < GRID - 1; y++) {
                int row = current[y - 1] & current[y] & current[y + 1];
                next[y] = row & (row << 1) & (row >>> 1);
            }
            current = next;
        }
        return current;
    }

    public static int[] templateMask(Template template) {
        Objects.requireNonNull(template);
        int[] rows = new int[GRID];
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                double nx = (x + 0.5 - 16.0) / 16.0;
                double ny = (y + 0.5 - 16.0) / 16.0;
                boolean inside = switch (template) {
                    case RECTANGLE, CUSTOM -> true;
                    case CIRCLE, ELLIPSE -> nx * nx + ny * ny <= 1.0;
                    case ARCH -> ny >= 0.0 || nx * nx + ny * ny <= 1.0;
                    case DIAMOND -> Math.abs(nx) + Math.abs(ny) <= 1.0;
                    case HEART -> {
                        double hx = nx * 1.25, hy = -ny * 1.25;
                        double a = hx * hx + hy * hy - 1.0;
                        yield a * a * a - hx * hx * hy * hy * hy <= 0.0;
                    }
                };
                if (inside) rows[y] |= 1 << x;
            }
        }
        return rows;
    }

    /** Rectangles are disjoint, in bitmap coordinates; row zero is the top edge. */
    public static List<CellRect> rectangles(int[] rows) {
        if (rows == null || rows.length != GRID) throw new IllegalArgumentException("Invalid mask");
        List<CellRect> result = new ArrayList<>();
        int[] remaining = rows.clone();
        for (int y = 0; y < GRID; y++) {
            while (remaining[y] != 0) {
                int left = Integer.numberOfTrailingZeros(remaining[y]), right = left + 1;
                while (right < GRID && (remaining[y] & (1 << right)) != 0) right++;
                int bits = (int)(((1L << (right - left)) - 1L) << left);
                int bottom = y + 1;
                while (bottom < GRID && (remaining[bottom] & bits) == bits) bottom++;
                for (int row = y; row < bottom; row++) remaining[row] &= ~bits;
                result.add(new CellRect(left, y, right, bottom));
            }
        }
        return List.copyOf(result);
    }

    public record CellRect(int left, int top, int right, int bottom) { }

    @Nullable
    public static FrameDesign read(ItemStack stack) {
        CompoundTag data = EdDYON.guaniao.util.ItemData.read(stack);
        CompoundTag tag = data != null && data.contains(TAG, net.minecraft.nbt.Tag.TAG_COMPOUND) ? data.getCompound(TAG) : null;
        return tag == null ? null : fromTag(tag);
    }

    public static void write(ItemStack stack, FrameDesign design) {
        Objects.requireNonNull(design);
        if (!stack.isEmpty()) {
            CompoundTag data = EdDYON.guaniao.util.ItemData.copy(stack);
            data.put(TAG, design.toTag());
            EdDYON.guaniao.util.ItemData.write(stack, data);
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 3);
        tag.putInt("Width", this.widthPixels);
        tag.putInt("Height", this.heightPixels);
        tag.putString("Template", this.template.id());
        tag.putIntArray("Mask", this.mask.clone());
        tag.putString("Wood", this.wood.id());
        tag.putString("Trim", this.trim.id());
        tag.putString("Mat", this.mat.id());
        tag.putString("FrameMaterial", this.frameMaterial.toString());
        tag.putString("TrimMaterial", this.trimMaterial.toString());
        tag.putString("LiningMaterial", this.liningMaterial.toString());
        tag.putString("Ornament", this.ornament.id());
        tag.putInt("Corners", this.corners);
        tag.putInt("Border", this.borderCells);
        tag.putInt("Depth", this.depthPixels);
        tag.putString("Fit", this.fit.id());
        tag.putInt("Zoom", this.zoomPercent);
        tag.putInt("OffsetX", this.offsetX);
        tag.putInt("OffsetY", this.offsetY);
        tag.putInt("Rotation", this.rotation);
        ListTag points = new ListTag();
        for (Decoration decoration : this.decorations) points.add(decoration.toTag());
        tag.put("Decorations", points);
        return tag;
    }

    @Nullable
    public static FrameDesign fromTag(CompoundTag tag) {
        int version = tag.getInt("Version");
        if ((version < 1 || version > 3) || !tag.contains("Mask", Tag.TAG_INT_ARRAY)) return null;
        try {
            PhotographFrameStyle.Ornament ornament = required(ornamentFromId(tag.getString("Ornament")));
            int corners = tag.getInt("Corners");
            List<Decoration> decorations = version == 1 ? legacyDecorations(ornament, corners) : readDecorations(tag);
            FrameDesign design = new FrameDesign(tag.getInt("Width"), tag.getInt("Height"), required(Template.fromId(tag.getString("Template"))),
                    tag.getIntArray("Mask"), required(Wood.fromId(tag.getString("Wood"))), required(Trim.fromId(tag.getString("Trim"))),
                    required(Mat.fromId(tag.getString("Mat"))), ornament,
                    corners, tag.getInt("Border"), tag.getInt("Depth"), required(PhotoFit.fromId(tag.getString("Fit"))),
                    tag.getInt("Zoom"), tag.getInt("OffsetX"), tag.getInt("OffsetY"), tag.getInt("Rotation"), decorations);
            return version < 3 ? design : design.withMaterials(resourceId(tag.getString("FrameMaterial")),
                    resourceId(tag.getString("TrimMaterial")), resourceId(tag.getString("LiningMaterial")));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static <T> T required(@Nullable T value) {
        if (value == null) throw new IllegalArgumentException("Unknown frame design value");
        return value;
    }

    private static List<Decoration> readDecorations(CompoundTag tag) {
        if (!(tag.get("Decorations") instanceof ListTag points)
                || points.size() > MAX_DECORATIONS
                || (!points.isEmpty() && points.getElementType() != Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Invalid frame decorations");
        }
        List<Decoration> result = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) result.add(Decoration.fromTag(points.getCompound(i)));
        return List.copyOf(result);
    }

    private static List<Decoration> legacyDecorations(PhotographFrameStyle.Ornament ornament, int corners) {
        if (ornament == PhotographFrameStyle.Ornament.NONE) return List.of();
        DecorationType type = ornament == PhotographFrameStyle.Ornament.LEAVES ? DecorationType.LEAF : DecorationType.FEATHER;
        List<Decoration> result = new ArrayList<>(4);
        for (int corner = 0; corner < 4; corner++) {
            if ((corners & (1 << corner)) != 0) result.add(new Decoration(type,
                    corner == 1 || corner == 2 ? 940 : 60, corner >= 2 ? 940 : 60, 20, corner, false));
        }
        return List.copyOf(result);
    }

    /** Coordinates use the full frame bounds, with the origin at its top left. */
    public record Decoration(DecorationType type, int x, int y, int size, int rotation, boolean locked, ResourceLocation itemId) {
        public Decoration(DecorationType type, int x, int y, int size, int rotation, boolean locked) {
            this(type, x, y, size, rotation, locked, legacyDecorationItem(type));
        }

        public Decoration(ResourceLocation itemId, int x, int y, int size, int rotation, boolean locked) {
            this(DecorationType.LEAF, x, y, size, rotation, locked, itemId);
        }
        public Decoration {
            Objects.requireNonNull(type);
            validateResourceId(itemId);
            if (x < 0 || x > 1000 || y < 0 || y > 1000 || size < MIN_DECORATION_SIZE
                    || size > MAX_DECORATION_SIZE || rotation < 0 || rotation > 3) {
                throw new IllegalArgumentException("Decoration outside supported limits");
            }
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Type", this.type.id()); tag.putString("Item", this.itemId.toString());
            tag.putInt("X", this.x); tag.putInt("Y", this.y);
            tag.putInt("Size", this.size); tag.putInt("Rotation", this.rotation); tag.putBoolean("Locked", this.locked);
            return tag;
        }

        public static Decoration fromTag(CompoundTag tag) {
            if (!tag.contains("Type", Tag.TAG_STRING) || !tag.contains("X", Tag.TAG_INT)
                    || !tag.contains("Y", Tag.TAG_INT) || !tag.contains("Size", Tag.TAG_INT)
                    || !tag.contains("Rotation", Tag.TAG_INT) || !tag.contains("Locked", Tag.TAG_BYTE)
                    || tag.getByte("Locked") < 0 || tag.getByte("Locked") > 1) {
                throw new IllegalArgumentException("Invalid decoration fields");
            }
            DecorationType type = required(DecorationType.fromId(tag.getString("Type")));
            return new Decoration(type, tag.getInt("X"), tag.getInt("Y"), tag.getInt("Size"),
                    tag.getInt("Rotation"), tag.getBoolean("Locked"), tag.contains("Item", Tag.TAG_STRING)
                    ? resourceId(tag.getString("Item")) : legacyDecorationItem(type));
        }
    }

    @Nullable
    private static PhotographFrameStyle.Ornament ornamentFromId(String id) {
        for (PhotographFrameStyle.Ornament value : PhotographFrameStyle.Ornament.values()) {
            if (value.id().equals(id)) return value;
        }
        return null;
    }

    /** Bounded IDs and at most sixteen decorations keep the request below 5.2 KiB. */
    public void writeTo(FriendlyByteBuf buffer) {
        buffer.writeByte(3);
        buffer.writeShort(this.widthPixels);
        buffer.writeShort(this.heightPixels);
        buffer.writeByte(this.template.ordinal());
        for (int row : this.mask) buffer.writeInt(row);
        buffer.writeByte(this.wood.ordinal());
        buffer.writeByte(this.trim.ordinal());
        buffer.writeByte(this.mat.ordinal());
        buffer.writeByte(this.ornament.ordinal());
        buffer.writeByte(this.corners);
        buffer.writeByte(this.borderCells);
        buffer.writeByte(this.depthPixels);
        buffer.writeByte(this.fit.ordinal());
        buffer.writeShort(this.zoomPercent);
        buffer.writeByte(this.offsetX);
        buffer.writeByte(this.offsetY);
        buffer.writeByte(this.rotation);
        buffer.writeUtf(this.frameMaterial.toString(), MAX_RESOURCE_ID_LENGTH);
        buffer.writeUtf(this.trimMaterial.toString(), MAX_RESOURCE_ID_LENGTH);
        buffer.writeUtf(this.liningMaterial.toString(), MAX_RESOURCE_ID_LENGTH);
        buffer.writeByte(this.decorations.size());
        for (Decoration decoration : this.decorations) {
            buffer.writeByte(decoration.type.ordinal());
            buffer.writeShort(decoration.x);
            buffer.writeShort(decoration.y);
            buffer.writeByte(decoration.size);
            buffer.writeByte(decoration.rotation);
            buffer.writeBoolean(decoration.locked);
            buffer.writeUtf(decoration.itemId.toString(), MAX_RESOURCE_ID_LENGTH);
        }
    }

    @Nullable
    public static FrameDesign readFrom(FriendlyByteBuf buffer) {
        try {
            int version = buffer.readUnsignedByte();
            if (version < 1 || version > 3) return null;
            int width = version >= 3 ? buffer.readUnsignedShort() : buffer.readUnsignedByte();
            int height = version >= 3 ? buffer.readUnsignedShort() : buffer.readUnsignedByte();
            int shape = buffer.readUnsignedByte();
            int[] rows = new int[GRID];
            for (int i = 0; i < GRID; i++) rows[i] = buffer.readInt();
            int wood = buffer.readUnsignedByte(), trim = buffer.readUnsignedByte(), mat = buffer.readUnsignedByte();
            int ornament = buffer.readUnsignedByte(), corners = buffer.readUnsignedByte(), border = buffer.readUnsignedByte();
            int depth = buffer.readUnsignedByte(), fit = buffer.readUnsignedByte(), zoom = buffer.readUnsignedShort();
            int offsetX = buffer.readByte(), offsetY = buffer.readByte(), rotation = buffer.readUnsignedByte();
            ResourceLocation frameMaterial = version >= 3 ? resourceId(buffer.readUtf(MAX_RESOURCE_ID_LENGTH)) : materialId(Wood.values()[wood]);
            ResourceLocation trimMaterial = version >= 3 ? resourceId(buffer.readUtf(MAX_RESOURCE_ID_LENGTH)) : materialId(Trim.values()[trim]);
            ResourceLocation liningMaterial = version >= 3 ? resourceId(buffer.readUtf(MAX_RESOURCE_ID_LENGTH)) : materialId(Mat.values()[mat]);
            List<Decoration> decorations;
            if (version == 1) {
                decorations = legacyDecorations(PhotographFrameStyle.Ornament.values()[ornament], corners);
            } else {
                int count = buffer.readUnsignedByte();
                if (count > MAX_DECORATIONS) return null;
                List<Decoration> points = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    int type = buffer.readUnsignedByte(), x = buffer.readUnsignedShort(), y = buffer.readUnsignedShort();
                    int size = buffer.readUnsignedByte(), turn = buffer.readUnsignedByte(), locked = buffer.readUnsignedByte();
                    if (locked > 1) return null;
                    ResourceLocation item = version >= 3 ? resourceId(buffer.readUtf(MAX_RESOURCE_ID_LENGTH)) : legacyDecorationItem(DecorationType.values()[type]);
                    points.add(new Decoration(DecorationType.values()[type], x, y, size, turn, locked == 1, item));
                }
                decorations = points;
            }
            return new FrameDesign(width, height, Template.values()[shape], rows, Wood.values()[wood], Trim.values()[trim],
                    Mat.values()[mat], PhotographFrameStyle.Ornament.values()[ornament], corners, border, depth,
                    PhotoFit.values()[fit], zoom, offsetX, offsetY, rotation, decorations, frameMaterial, trimMaterial, liningMaterial);
        } catch (IllegalArgumentException | IndexOutOfBoundsException | DecoderException exception) {
            return null;
        }
    }

    public static FrameDesign fromStyle(PhotographFrameStyle style) {
        Template template = switch (style.shape()) {
            case ROUND -> Template.CIRCLE;
            case ARCH -> Template.ARCH;
            default -> Template.RECTANGLE;
        };
        Wood wood = switch (style.material()) {
            case SPRUCE -> Wood.SPRUCE;
            case BIRCH -> Wood.BIRCH;
            default -> Wood.OAK;
        };
        return new FrameDesign(style.shape().widthPixels(), style.shape().heightPixels(), template, templateMask(template),
                wood, style.material() == PhotographFrameStyle.Material.COPPER ? Trim.COPPER : Trim.NONE, Mat.NONE,
                style.ornament(), 15, 2, 1, PhotoFit.CONTAIN, 100, 0, 0, 0);
    }

    public FrameDesign withWood(Wood value) { return copy(this.widthPixels, this.heightPixels, this.template, this.mask, value, this.trim, this.mat, this.ornament, this.corners, this.borderCells, this.depthPixels, this.fit, this.zoomPercent, this.offsetX, this.offsetY, this.rotation).withFrameMaterial(materialId(value)); }
    public FrameDesign withTrim(Trim value) { return copy(this.widthPixels, this.heightPixels, this.template, this.mask, this.wood, value, this.mat, this.ornament, this.corners, this.borderCells, this.depthPixels, this.fit, this.zoomPercent, this.offsetX, this.offsetY, this.rotation).withTrimMaterial(materialId(value)); }
    public FrameDesign withMat(Mat value) { return copy(this.widthPixels, this.heightPixels, this.template, this.mask, this.wood, this.trim, value, this.ornament, this.corners, this.borderCells, this.depthPixels, this.fit, this.zoomPercent, this.offsetX, this.offsetY, this.rotation).withLiningMaterial(materialId(value)); }
    public FrameDesign withOrnament(PhotographFrameStyle.Ornament value) {
        if (value == PhotographFrameStyle.Ornament.NONE) return this.withDecorations(List.of());
        Decoration decoration = new Decoration(value == PhotographFrameStyle.Ornament.LEAVES ? DecorationType.LEAF : DecorationType.FEATHER,
                500, 150, 20, 0, false);
        if (this.decorations.contains(decoration) || this.decorations.size() >= MAX_DECORATIONS) return this;
        List<Decoration> points = new ArrayList<>(this.decorations);
        points.add(decoration);
        return this.withDecorations(points);
    }
    public FrameDesign withSize(int width, int height) { return copy(width, height, this.template, this.mask, this.wood, this.trim, this.mat, this.ornament, this.corners, this.borderCells, this.depthPixels, this.fit, this.zoomPercent, this.offsetX, this.offsetY, this.rotation); }
    public FrameDesign withTemplate(Template value) { return copy(this.widthPixels, this.heightPixels, value, templateMask(value), this.wood, this.trim, this.mat, this.ornament, this.corners, this.borderCells, this.depthPixels, this.fit, this.zoomPercent, this.offsetX, this.offsetY, this.rotation); }

    public FrameDesign withDecorations(List<Decoration> points) {
        return new FrameDesign(this.widthPixels, this.heightPixels, this.template, this.mask, this.wood, this.trim, this.mat,
                PhotographFrameStyle.Ornament.NONE, this.corners, this.borderCells, this.depthPixels, this.fit,
                this.zoomPercent, this.offsetX, this.offsetY, this.rotation, points, this.frameMaterial, this.trimMaterial, this.liningMaterial);
    }

    private FrameDesign copy(int width, int height, Template template, int[] mask, Wood wood, Trim trim, Mat mat,
                                    PhotographFrameStyle.Ornament ornament, int corners, int border, int depth, PhotoFit fit,
                                    int zoom, int offsetX, int offsetY, int rotation) {
        return new FrameDesign(width, height, template, mask, wood, trim, mat, ornament, corners, border, depth, fit, zoom, offsetX, offsetY, rotation, this.decorations, this.frameMaterial, this.trimMaterial, this.liningMaterial);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FrameDesign value)) return false;
        return this.widthPixels == value.widthPixels && this.heightPixels == value.heightPixels && this.template == value.template
                && Arrays.equals(this.mask, value.mask) && this.wood == value.wood && this.trim == value.trim && this.mat == value.mat
                && this.ornament == value.ornament && this.corners == value.corners && this.borderCells == value.borderCells
                && this.depthPixels == value.depthPixels && this.fit == value.fit && this.zoomPercent == value.zoomPercent
                && this.offsetX == value.offsetX && this.offsetY == value.offsetY && this.rotation == value.rotation
                && this.decorations.equals(value.decorations) && this.frameMaterial.equals(value.frameMaterial)
                && this.trimMaterial.equals(value.trimMaterial) && this.liningMaterial.equals(value.liningMaterial);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(this.widthPixels, this.heightPixels, this.template, this.wood, this.trim, this.mat, this.ornament,
                this.corners, this.borderCells, this.depthPixels, this.fit, this.zoomPercent, this.offsetX, this.offsetY, this.rotation, this.decorations, this.frameMaterial, this.trimMaterial, this.liningMaterial)
                + Arrays.hashCode(this.mask);
    }

    public FrameDesign withFrameMaterial(ResourceLocation id) { return withMaterials(id, this.trimMaterial, this.liningMaterial); }
    public FrameDesign withTrimMaterial(ResourceLocation id) { return withMaterials(this.frameMaterial, id, this.liningMaterial); }
    public FrameDesign withLiningMaterial(ResourceLocation id) { return withMaterials(this.frameMaterial, this.trimMaterial, id); }

    private FrameDesign withMaterials(ResourceLocation frame, ResourceLocation trim, ResourceLocation lining) {
        return new FrameDesign(this.widthPixels, this.heightPixels, this.template, this.mask, this.wood, this.trim, this.mat,
                this.ornament, this.corners, this.borderCells, this.depthPixels, this.fit, this.zoomPercent,
                this.offsetX, this.offsetY, this.rotation, this.decorations, frame, trim, lining);
    }

    public boolean hasArea() { return this.widthPixels > 0 && this.heightPixels > 0; }

    /** Registry checks apply when crafting, not while loading old saves with absent optional mods. */
    public boolean hasAvailableMaterials() {
        if (!isBlockMaterial(this.frameMaterial)
                || (!this.trimMaterial.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "air")) && !isBlockMaterial(this.trimMaterial))
                || (!this.liningMaterial.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "paper")) && !isBlockMaterial(this.liningMaterial))) return false;
        for (Decoration point : this.decorations) {
            if (!BuiltInRegistries.ITEM.containsKey(point.itemId) || BuiltInRegistries.ITEM.get(point.itemId) == Items.AIR) return false;
        }
        return true;
    }

    public static boolean isBlockMaterial(ResourceLocation id) {
        return BuiltInRegistries.ITEM.containsKey(id)
                && BuiltInRegistries.ITEM.get(id) instanceof BlockItem;
    }

    private static ResourceLocation materialId(Wood wood) {
        return BuiltInRegistries.ITEM.getKey(wood.materialItem());
    }

    private static ResourceLocation materialId(Trim trim) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", switch (trim) {
            case NONE -> "air";
            case COPPER -> "copper_block";
            case IRON -> "iron_block";
            case GOLD -> "gold_block";
            case QUARTZ -> "quartz_block";
            case AMETHYST -> "amethyst_block";
        });
    }

    private static ResourceLocation materialId(Mat mat) {
        return mat == Mat.NONE ? ResourceLocation.fromNamespaceAndPath("minecraft", "paper") : BuiltInRegistries.ITEM.getKey(mat.materialItem());
    }

    public static ResourceLocation legacyDecorationItem(DecorationType type) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", switch (type) {
            case LEAF -> "oak_leaves";
            case FEATHER -> "feather";
            case FLOWER -> "poppy";
            case STAR -> "nether_star";
            case HEART -> "heart_of_the_sea";
            case SHELL -> "nautilus_shell";
            case BUTTERFLY -> "elytra";
            case MUSHROOM -> "red_mushroom";
            case ACORN -> "oak_sapling";
            case BERRY -> "sweet_berries";
            case MOON -> "clock";
            case BIRD -> "egg";
        });
    }

    private static ResourceLocation resourceId(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_RESOURCE_ID_LENGTH) {
            throw new IllegalArgumentException("Invalid frame resource ID length");
        }
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) throw new IllegalArgumentException("Invalid frame resource ID");
        return id;
    }

    private static void validateResourceId(ResourceLocation id) {
        if (id == null || id.toString().length() > MAX_RESOURCE_ID_LENGTH) {
            throw new IllegalArgumentException("Invalid frame resource ID");
        }
    }

    public enum Template {
        RECTANGLE, CIRCLE, ELLIPSE, ARCH, DIAMOND, HEART, CUSTOM;
        public String id() { return name().toLowerCase(Locale.ROOT); }
        @Nullable public static Template fromId(String id) { for (Template value : values()) if (value.id().equals(id)) return value; return null; }
    }

    /** Small canonical pixel sprites shared by the editor and world renderer. */
    public enum DecorationType {
        LEAF(0x5D973E), FEATHER(0xD6DFE7), FLOWER(0xEB91B4), STAR(0xF4CB55), HEART(0xD95569),
        SHELL(0xE8C5A0), BUTTERFLY(0x89A8E8), MUSHROOM(0xC75A43), ACORN(0x9B703F),
        BERRY(0xA4415D), MOON(0xE7DAA0), BIRD(0x5F9CB5);

        public static final int SPRITE_SIZE = 16;
        private final int color;
        DecorationType(int color) { this.color = color; }
        public String id() { return name().toLowerCase(Locale.ROOT); }
        @Nullable public static DecorationType fromId(String id) { for (DecorationType value : values()) if (value.id().equals(id)) return value; return null; }

        /** Sixteen rows with bit zero on the left. Returned masks are defensive copies. */
        public int[] mask() {
            int[] rows = new int[SPRITE_SIZE];
            for (int y = 0; y < SPRITE_SIZE; y++) for (int x = 0; x < SPRITE_SIZE; x++) if (inside(x, y)) rows[y] |= 1 << x;
            return rows;
        }

        public int colorAt(int x, int y) {
            if (!inside(x, y)) return 0;
            int rgb = this.color;
            if (this == FLOWER && circle(x, y, 7.5, 7.5, 2.0)) rgb = 0xE6BD4B;
            if (this == MUSHROOM && y >= 10) rgb = 0xE7D9AE;
            if (this == MUSHROOM && y < 9 && ((x + y * 3) % 7 < 2)) rgb = 0xEFE5CA;
            if (this == ACORN && y < 6) rgb = 0x5B432B;
            if (this == BERRY && y < 5) rgb = 0x5D973E;
            if (this == BUTTERFLY && x >= 7 && x <= 8) rgb = 0x55402E;
            if (this == BIRD && x >= 10 && y <= 6 && x <= 11) rgb = 0x24362C;
            boolean edge = !inside(x-1, y) || !inside(x+1, y) || !inside(x, y-1) || !inside(x, y+1);
            float shade = edge ? 0.65F : x + y < 15 ? 1.14F : 1.0F;
            int red = Math.min(255, (int)(((rgb >> 16) & 255) * shade));
            int green = Math.min(255, (int)(((rgb >> 8) & 255) * shade));
            int blue = Math.min(255, (int)((rgb & 255) * shade));
            return 0xFF000000 | red << 16 | green << 8 | blue;
        }

        private boolean inside(int x, int y) {
            if (x < 0 || x >= SPRITE_SIZE || y < 0 || y >= SPRITE_SIZE) return false;
            double nx=(x-7.5)/7.0, ny=(y-7.5)/7.0;
            return switch (this) {
                case LEAF -> square((nx+ny)/1.6)+square((nx-ny)/0.58)<=1 || x==y && x>=8;
                case FEATHER -> square((nx+ny)/1.55)+square((nx-ny)/0.48)<=1 || x==y && x>=7;
                case FLOWER -> circle(x,y,7.5,7.5,2.2) || circle(x,y,7.5,3.5,3) || circle(x,y,11.3,6.5,3)
                        || circle(x,y,10,11,3) || circle(x,y,5,11,3) || circle(x,y,3.7,6.5,3);
                case STAR -> Math.hypot(nx,ny) <= 0.60 + 0.30*Math.cos(5*(Math.atan2(ny,nx)+Math.PI/2));
                case HEART -> { double hx=nx*1.25,hy=-ny*1.25,a=hx*hx+hy*hy-1; yield a*a*a-hx*hx*hy*hy*hy<=0; }
                case SHELL -> circle(x,y,7.5,8,6.5) && y<=12 && Math.abs(x-7.5)<=Math.max(2,14-y);
                case BUTTERFLY -> ellipse(x,y,4.5,5,3.5,4.0) || ellipse(x,y,10.5,5,3.5,4.0)
                        || ellipse(x,y,5,11,3,3.0) || ellipse(x,y,10,11,3,3.0) || x>=7&&x<=8&&y>=3&&y<=13;
                case MUSHROOM -> ellipse(x,y,7.5,8,6.5,6.0)&&y<=9 || x>=6&&x<=9&&y>=8&&y<=14;
                case ACORN -> ellipse(x,y,7.5,8,4.5,6.0) || y>=4&&y<=6&&x>=2&&x<=13 || x==8&&y>=0&&y<=3;
                case BERRY -> circle(x,y,5,7,3.5)||circle(x,y,10,7,3.5)||circle(x,y,7.5,11,3.5)
                        || y>=2&&y<=4&&x>=5&&x<=10;
                case MOON -> circle(x,y,7.5,7.5,6.5)&&!circle(x,y,10.5,5,5.8);
                case BIRD -> ellipse(x,y,6.5,9,5.0,3.5)||circle(x,y,10.5,5.5,3)||x>=12&&y>=5&&y<=6
                        || x<=3&&y>=8&&y<=11 || x==6&&y>=11&&y<=14 || x==9&&y>=11&&y<=14;
            };
        }
        private static double square(double x) { return x*x; }
        private static boolean circle(double x,double y,double cx,double cy,double r) { return square(x-cx)+square(y-cy)<=r*r; }
        private static boolean ellipse(double x,double y,double cx,double cy,double rx,double ry) { return square((x-cx)/rx)+square((y-cy)/ry)<=1; }
    }

    public enum Wood {
        OAK(Items.OAK_PLANKS, "oak_planks"), SPRUCE(Items.SPRUCE_PLANKS, "spruce_planks"), BIRCH(Items.BIRCH_PLANKS, "birch_planks"), JUNGLE(Items.JUNGLE_PLANKS, "jungle_planks"),
        ACACIA(Items.ACACIA_PLANKS, "acacia_planks"), DARK_OAK(Items.DARK_OAK_PLANKS, "dark_oak_planks"), MANGROVE(Items.MANGROVE_PLANKS, "mangrove_planks"), CHERRY(Items.CHERRY_PLANKS, "cherry_planks"),
        BAMBOO(Items.BAMBOO_PLANKS, "bamboo_planks"), CRIMSON(Items.CRIMSON_PLANKS, "crimson_planks"), WARPED(Items.WARPED_PLANKS, "warped_planks"),
        STONE(Items.STONE, "stone"), ANDESITE(Items.ANDESITE, "andesite"), DIORITE(Items.DIORITE, "diorite"), GRANITE(Items.GRANITE, "granite"),
        DEEPSLATE(Items.DEEPSLATE, "deepslate"), BLACKSTONE(Items.BLACKSTONE, "blackstone"), BRICKS(Items.BRICKS, "bricks"), STONE_BRICKS(Items.STONE_BRICKS, "stone_bricks"),
        QUARTZ(Items.QUARTZ_BLOCK, "quartz_block_side"), PRISMARINE(Items.PRISMARINE, "prismarine"), END_STONE(Items.END_STONE, "end_stone"), SANDSTONE(Items.SANDSTONE, "sandstone"),
        IRON(Items.IRON_BLOCK, "iron_block"), GOLD(Items.GOLD_BLOCK, "gold_block"), COPPER(Items.COPPER_BLOCK, "copper_block"), AMETHYST(Items.AMETHYST_BLOCK, "amethyst_block"),
        WHITE_CONCRETE(Items.WHITE_CONCRETE, "white_concrete"), ORANGE_CONCRETE(Items.ORANGE_CONCRETE, "orange_concrete"), MAGENTA_CONCRETE(Items.MAGENTA_CONCRETE, "magenta_concrete"),
        LIGHT_BLUE_CONCRETE(Items.LIGHT_BLUE_CONCRETE, "light_blue_concrete"), YELLOW_CONCRETE(Items.YELLOW_CONCRETE, "yellow_concrete"), LIME_CONCRETE(Items.LIME_CONCRETE, "lime_concrete"),
        PINK_CONCRETE(Items.PINK_CONCRETE, "pink_concrete"), GRAY_CONCRETE(Items.GRAY_CONCRETE, "gray_concrete"), LIGHT_GRAY_CONCRETE(Items.LIGHT_GRAY_CONCRETE, "light_gray_concrete"),
        CYAN_CONCRETE(Items.CYAN_CONCRETE, "cyan_concrete"), PURPLE_CONCRETE(Items.PURPLE_CONCRETE, "purple_concrete"), BLUE_CONCRETE(Items.BLUE_CONCRETE, "blue_concrete"),
        BROWN_CONCRETE(Items.BROWN_CONCRETE, "brown_concrete"), GREEN_CONCRETE(Items.GREEN_CONCRETE, "green_concrete"), RED_CONCRETE(Items.RED_CONCRETE, "red_concrete"), BLACK_CONCRETE(Items.BLACK_CONCRETE, "black_concrete");
        private final Item item;
        private final String texture;
        Wood(Item item, String texture) { this.item = item; this.texture = "minecraft:textures/block/" + texture + ".png"; }
        public String id() { return name().toLowerCase(Locale.ROOT); }
        public String texturePath() { return this.texture; }
        public Item materialItem() { return this.item; }
        @Nullable public static Wood fromId(String id) { for (Wood value : values()) if (value.id().equals(id)) return value; return null; }
        @Nullable public static Wood fromItem(ItemStack stack) { for (Wood value : values()) if (stack.is(value.item)) return value; return null; }
    }

    public enum Trim {
        NONE(null), COPPER(Items.COPPER_INGOT), IRON(Items.IRON_INGOT), GOLD(Items.GOLD_INGOT), QUARTZ(Items.QUARTZ), AMETHYST(Items.AMETHYST_SHARD);
        private final Item item;
        Trim(@Nullable Item item) { this.item = item; }
        public String id() { return name().toLowerCase(Locale.ROOT); }
        @Nullable public Item materialItem() { return this.item; }
        @Nullable public String texturePath() {
            return switch (this) {
                case NONE -> null;
                case COPPER -> "minecraft:textures/block/copper_block.png";
                case IRON -> "minecraft:textures/block/iron_block.png";
                case GOLD -> "minecraft:textures/block/gold_block.png";
                case QUARTZ -> "minecraft:textures/block/quartz_block_side.png";
                case AMETHYST -> "minecraft:textures/block/amethyst_block.png";
            };
        }
        @Nullable public static Trim fromId(String id) { for (Trim value : values()) if (value.id().equals(id)) return value; return null; }
        @Nullable public static Trim fromItem(ItemStack stack) { for (Trim value : values()) if (value.item != null && stack.is(value.item)) return value; return null; }
    }

    public enum Mat {
        NONE(null), PAPER(Items.PAPER), WHITE_WOOL(Items.WHITE_WOOL), ORANGE_WOOL(Items.ORANGE_WOOL), MAGENTA_WOOL(Items.MAGENTA_WOOL),
        LIGHT_BLUE_WOOL(Items.LIGHT_BLUE_WOOL), YELLOW_WOOL(Items.YELLOW_WOOL), LIME_WOOL(Items.LIME_WOOL), PINK_WOOL(Items.PINK_WOOL),
        GRAY_WOOL(Items.GRAY_WOOL), LIGHT_GRAY_WOOL(Items.LIGHT_GRAY_WOOL), CYAN_WOOL(Items.CYAN_WOOL), PURPLE_WOOL(Items.PURPLE_WOOL),
        BLUE_WOOL(Items.BLUE_WOOL), BROWN_WOOL(Items.BROWN_WOOL), GREEN_WOOL(Items.GREEN_WOOL), RED_WOOL(Items.RED_WOOL), BLACK_WOOL(Items.BLACK_WOOL);
        private final Item item;
        Mat(@Nullable Item item) { this.item = item; }
        public String id() { return name().toLowerCase(Locale.ROOT); }
        @Nullable public Item materialItem() { return this.item; }
        @Nullable public static Mat fromId(String id) { for (Mat value : values()) if (value.id().equals(id)) return value; return null; }
    }

    public enum PhotoFit {
        CONTAIN, COVER;
        public String id() { return name().toLowerCase(Locale.ROOT); }
        @Nullable public static PhotoFit fromId(String id) { for (PhotoFit value : values()) if (value.id().equals(id)) return value; return null; }
    }
}
