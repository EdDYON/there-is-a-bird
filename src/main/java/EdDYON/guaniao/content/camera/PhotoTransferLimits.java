package EdDYON.guaniao.content.camera;

public final class PhotoTransferLimits {
    public static final int IMAGE_WIDTH = 256;
    public static final int IMAGE_HEIGHT = 256;
    public static final int MAX_COMPRESSED_BYTES = 96 * 1024;
    public static final int MAX_CHUNK_BYTES = 24 * 1024;
    public static final int MAX_CHUNKS = MAX_COMPRESSED_BYTES / MAX_CHUNK_BYTES;
    public static final int UPLOAD_TIMEOUT_TICKS = 10 * 20;
    public static final int DOWNLOAD_TIMEOUT_TICKS = 10 * 20;
    public static final int CAPTURE_COOLDOWN_TICKS = 30;
    public static final int MAX_UPLOAD_BYTES_PER_MINUTE = MAX_COMPRESSED_BYTES * 12;
    public static final int MAX_PHOTO_ID_LENGTH = 80;
    public static final int SHA256_HEX_LENGTH = 64;

    private PhotoTransferLimits() {
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
