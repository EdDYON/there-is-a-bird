package EdDYON.guaniao.content.camera;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class PhotographData {
    public static final int IMAGE_SIZE = 256;
    private static final int MIN_IMAGE_SIZE = 16;
    private static final int MAX_IMAGE_SIZE = 512;
    public static final String TAG_PHOTO_ID = "PhotoId";
    public static final String TAG_PHOTOGRAPHER = "Photographer";
    public static final String TAG_PHOTOGRAPHER_ID = "PhotographerId";
    public static final String TAG_GAME_TIME = "GameTime";
    public static final String TAG_WIDTH = "Width";
    public static final String TAG_HEIGHT = "Height";
    public static final String TAG_PIXELS = "Pixels";

    private PhotographData() {
    }

    public static boolean hasImage(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null
                && tag.contains(TAG_PHOTO_ID)
                && tag.contains(TAG_PIXELS)
                && imagePixelCount(tag) > 0;
    }

    public static String id(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(TAG_PHOTO_ID);
    }

    public static String photographer(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(TAG_PHOTOGRAPHER);
    }

    public static int[] pixels(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new int[0] : tag.getIntArray(TAG_PIXELS);
    }

    public static int width(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : imageWidth(tag);
    }

    public static int height(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : imageHeight(tag);
    }

    public static void write(ItemStack stack, String id, String photographer, UUID photographerId, long gameTime, int[] pixels) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_PHOTO_ID, id);
        tag.putString(TAG_PHOTOGRAPHER, photographer);
        tag.putUUID(TAG_PHOTOGRAPHER_ID, photographerId);
        tag.putLong(TAG_GAME_TIME, gameTime);
        tag.putInt(TAG_WIDTH, IMAGE_SIZE);
        tag.putInt(TAG_HEIGHT, IMAGE_SIZE);
        tag.putIntArray(TAG_PIXELS, pixels);
    }

    public static void copyImage(ItemStack from, ItemStack to) {
        CompoundTag source = from.getTag();
        if (source == null) {
            return;
        }

        CompoundTag target = to.getOrCreateTag();
        if (source.contains(TAG_PHOTO_ID)) {
            target.putString(TAG_PHOTO_ID, source.getString(TAG_PHOTO_ID));
        }
        if (source.contains(TAG_PHOTOGRAPHER)) {
            target.putString(TAG_PHOTOGRAPHER, source.getString(TAG_PHOTOGRAPHER));
        }
        if (source.hasUUID(TAG_PHOTOGRAPHER_ID)) {
            target.putUUID(TAG_PHOTOGRAPHER_ID, source.getUUID(TAG_PHOTOGRAPHER_ID));
        }
        if (source.contains(TAG_GAME_TIME)) {
            target.putLong(TAG_GAME_TIME, source.getLong(TAG_GAME_TIME));
        }
        int width = imageWidth(source);
        int height = imageHeight(source);
        target.putInt(TAG_WIDTH, width > 0 ? width : IMAGE_SIZE);
        target.putInt(TAG_HEIGHT, height > 0 ? height : IMAGE_SIZE);
        if (source.contains(TAG_PIXELS)) {
            target.putIntArray(TAG_PIXELS, source.getIntArray(TAG_PIXELS));
        }
    }

    private static int imageWidth(CompoundTag tag) {
        int width = tag.getInt(TAG_WIDTH);
        int height = tag.getInt(TAG_HEIGHT);
        int pixels = tag.getIntArray(TAG_PIXELS).length;
        if (validDimensions(width, height, pixels)) {
            return width;
        }

        int square = squareDimension(pixels);
        return square > 0 ? square : 0;
    }

    private static int imageHeight(CompoundTag tag) {
        int width = tag.getInt(TAG_WIDTH);
        int height = tag.getInt(TAG_HEIGHT);
        int pixels = tag.getIntArray(TAG_PIXELS).length;
        if (validDimensions(width, height, pixels)) {
            return height;
        }

        int square = squareDimension(pixels);
        return square > 0 ? square : 0;
    }

    private static int imagePixelCount(CompoundTag tag) {
        int width = imageWidth(tag);
        int height = imageHeight(tag);
        return width > 0 && height > 0 ? width * height : 0;
    }

    private static boolean validDimensions(int width, int height, int pixels) {
        return width >= MIN_IMAGE_SIZE
                && height >= MIN_IMAGE_SIZE
                && width <= MAX_IMAGE_SIZE
                && height <= MAX_IMAGE_SIZE
                && pixels == width * height;
    }

    private static int squareDimension(int pixels) {
        if (pixels <= 0) {
            return 0;
        }
        int side = (int)Math.sqrt(pixels);
        return side * side == pixels && side >= MIN_IMAGE_SIZE && side <= MAX_IMAGE_SIZE ? side : 0;
    }
}
