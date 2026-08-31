package EdDYON.guaniao.content.camera;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class PhotographData {
    public static final int IMAGE_SIZE = PhotoTransferLimits.IMAGE_WIDTH;
    public static final int LEGACY_IMAGE_SIZE = PhotoTransferLimits.LEGACY_IMAGE_WIDTH;
    private static final int MIN_IMAGE_SIZE = 16;
    private static final int MAX_IMAGE_SIZE = PhotoTransferLimits.IMAGE_WIDTH;
    public static final String TAG_PHOTO_ID = "PhotoId";
    public static final String TAG_PHOTOGRAPHER = "Photographer";
    public static final String TAG_PHOTOGRAPHER_ID = "PhotographerId";
    public static final String TAG_GAME_TIME = "GameTime";
    public static final String TAG_WIDTH = "Width";
    public static final String TAG_HEIGHT = "Height";
    public static final String TAG_CONTENT_HASH = "ContentHash";
    public static final String TAG_PIXELS = "Pixels";

    private PhotographData() {
    }

    public static boolean hasImage(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null
                && tag.contains(TAG_PHOTO_ID)
                && PhotoTransferLimits.isValidPhotoId(tag.getString(TAG_PHOTO_ID))
                && imageWidth(tag) > 0
                && imageHeight(tag) > 0;
    }

    public static String id(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(TAG_PHOTO_ID);
    }

    public static String photographer(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(TAG_PHOTOGRAPHER);
    }

    public static UUID photographerId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_PHOTOGRAPHER_ID) ? tag.getUUID(TAG_PHOTOGRAPHER_ID) : null;
    }

    public static long gameTime(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(TAG_GAME_TIME);
    }

    public static int[] pixels(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new int[0] : tag.getIntArray(TAG_PIXELS);
    }

    public static boolean hasLegacyPixels(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getIntArray(TAG_PIXELS).length == LEGACY_IMAGE_SIZE * LEGACY_IMAGE_SIZE;
    }

    public static String contentHash(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(TAG_CONTENT_HASH);
    }

    public static int width(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : imageWidth(tag);
    }

    public static int height(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : imageHeight(tag);
    }

    public static void writeReference(
            ItemStack stack,
            String id,
            String photographer,
            UUID photographerId,
            long gameTime,
            int width,
            int height,
            String contentHash
    ) {
        if (!PhotoTransferLimits.isValidPhotoId(id)
                || width < MIN_IMAGE_SIZE
                || height < MIN_IMAGE_SIZE
                || width > MAX_IMAGE_SIZE
                || height > MAX_IMAGE_SIZE
                || !PhotoImageCodec.isSha256(contentHash)) {
            throw new IllegalArgumentException("Invalid photograph reference");
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_PHOTO_ID, id);
        tag.putString(TAG_PHOTOGRAPHER, photographer);
        tag.putUUID(TAG_PHOTOGRAPHER_ID, photographerId);
        tag.putLong(TAG_GAME_TIME, gameTime);
        tag.putInt(TAG_WIDTH, width);
        tag.putInt(TAG_HEIGHT, height);
        tag.putString(TAG_CONTENT_HASH, contentHash);
        tag.remove(TAG_PIXELS);
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
        if (source.contains(TAG_CONTENT_HASH)) {
            target.putString(TAG_CONTENT_HASH, source.getString(TAG_CONTENT_HASH));
        }
        int width = imageWidth(source);
        int height = imageHeight(source);
        target.putInt(TAG_WIDTH, width > 0 ? width : LEGACY_IMAGE_SIZE);
        target.putInt(TAG_HEIGHT, height > 0 ? height : LEGACY_IMAGE_SIZE);
        // Legacy pixels are copied only so an old film crafted before migration can
        // still be imported by the resulting photograph item on its next server tick.
        if (source.getIntArray(TAG_PIXELS).length == LEGACY_IMAGE_SIZE * LEGACY_IMAGE_SIZE) {
            target.putIntArray(TAG_PIXELS, source.getIntArray(TAG_PIXELS));
        }
    }

    public static void finishLegacyMigration(ItemStack stack, String contentHash) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !PhotoImageCodec.isSha256(contentHash)) {
            return;
        }
        tag.putInt(TAG_WIDTH, LEGACY_IMAGE_SIZE);
        tag.putInt(TAG_HEIGHT, LEGACY_IMAGE_SIZE);
        tag.putString(TAG_CONTENT_HASH, contentHash);
        tag.remove(TAG_PIXELS);
    }

    private static int imageWidth(CompoundTag tag) {
        int width = tag.getInt(TAG_WIDTH);
        int height = tag.getInt(TAG_HEIGHT);
        if (validDimensions(width, height)) {
            return width;
        }

        return tag.getIntArray(TAG_PIXELS).length == LEGACY_IMAGE_SIZE * LEGACY_IMAGE_SIZE ? LEGACY_IMAGE_SIZE : 0;
    }

    private static int imageHeight(CompoundTag tag) {
        int width = tag.getInt(TAG_WIDTH);
        int height = tag.getInt(TAG_HEIGHT);
        if (validDimensions(width, height)) {
            return height;
        }

        return tag.getIntArray(TAG_PIXELS).length == LEGACY_IMAGE_SIZE * LEGACY_IMAGE_SIZE ? LEGACY_IMAGE_SIZE : 0;
    }

    private static boolean validDimensions(int width, int height) {
        return width >= MIN_IMAGE_SIZE
                && height >= MIN_IMAGE_SIZE
                && width <= MAX_IMAGE_SIZE
                && height <= MAX_IMAGE_SIZE;
    }
}
