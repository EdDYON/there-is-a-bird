package EdDYON.guaniao.content.camera;

public final class PhotoTransferLimits {
    /** Resolution used by newly captured photographs. */
    public static final int IMAGE_WIDTH = 1024;
    public static final int IMAGE_HEIGHT = 1024;
    /** Resolution used by photographs created by the first high-resolution capture update. */
    public static final int PREVIOUS_IMAGE_WIDTH = 512;
    public static final int PREVIOUS_IMAGE_HEIGHT = 512;
    /** Resolution used by photographs created before the high-resolution capture update. */
    public static final int LEGACY_IMAGE_WIDTH = 256;
    public static final int LEGACY_IMAGE_HEIGHT = 256;
    public static final int MAX_COMPRESSED_BYTES = 2 * 1024 * 1024;
    public static final int MAX_CHUNK_BYTES = 24 * 1024;
    public static final int MAX_CHUNKS = (MAX_COMPRESSED_BYTES + MAX_CHUNK_BYTES - 1) / MAX_CHUNK_BYTES;
    public static final int UPLOAD_TIMEOUT_TICKS = 10 * 20;
    public static final int DOWNLOAD_TIMEOUT_TICKS = 10 * 20;
    public static final int CAPTURE_COOLDOWN_TICKS = 30;
    public static final int MAX_UPLOAD_BYTES_PER_MINUTE = MAX_COMPRESSED_BYTES * 12;
    public static final int MAX_PHOTO_ID_LENGTH = 80;
    public static final int SHA256_HEX_LENGTH = 64;

    private PhotoTransferLimits() {
    }

    public static boolean isCaptureDimensions(int width, int height) {
        return width == IMAGE_WIDTH && height == IMAGE_HEIGHT;
    }

    public static boolean isSupportedDimensions(int width, int height) {
        return isCaptureDimensions(width, height)
                || width == PREVIOUS_IMAGE_WIDTH && height == PREVIOUS_IMAGE_HEIGHT
                || width == LEGACY_IMAGE_WIDTH && height == LEGACY_IMAGE_HEIGHT;
    }

    public static boolean isValidPhotoId(String photoId) {
        if (photoId == null || photoId.length() < 2 || photoId.length() > MAX_PHOTO_ID_LENGTH) {
            return false;
        }
        for (int index = 0; index < photoId.length(); index++) {
            char character = photoId.charAt(index);
            boolean asciiLetterOrDigit = character >= '0' && character <= '9'
                    || character >= 'A' && character <= 'Z'
                    || character >= 'a' && character <= 'z';
            if (!asciiLetterOrDigit && character != '_' && character != '-') {
                return false;
            }
        }
        return true;
    }
}
