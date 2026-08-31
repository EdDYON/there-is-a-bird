package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.content.camera.PhotoImageCodec;
import EdDYON.guaniao.content.camera.PhotoTransferLimits;
import EdDYON.guaniao.network.BeginPhotoUploadPacket;
import EdDYON.guaniao.network.FinishPhotoUploadPacket;
import EdDYON.guaniao.network.GuaniaoNetwork;
import EdDYON.guaniao.network.PhotoRequestPacket;
import EdDYON.guaniao.network.PhotoUploadChunkPacket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

public final class PhotoClientRepository {
    private static final int MAX_CACHED_IMAGES = 192;
    private static final long REQUEST_RETRY_MILLIS = 5_000L;
    private static final Map<String, CachedImage> IMAGES = new LinkedHashMap<>(MAX_CACHED_IMAGES + 1, 0.75F, true);
    private static final Map<String, DownloadSession> DOWNLOADS = new HashMap<>();
    private static final Map<String, String> EXPECTED_HASHES = new HashMap<>();
    private static final Map<String, Long> RETRY_AFTER = new HashMap<>();
    private static final Map<UUID, Long> PENDING_UPLOADS = new HashMap<>();
    private static final Map<String, String> REQUEST_QUEUE = new LinkedHashMap<>();
    private static final Map<String, Integer> FAILURE_COUNTS = new HashMap<>();
    private static final Set<String> VALIDATING = new HashSet<>();
    private static final ExecutorService VALIDATION_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Guaniao-Photo-Decode");
        thread.setDaemon(true);
        return thread;
    });
    private static int generation;
    private static String activeRequest;
    private static long activeRequestStarted;

    private PhotoClientRepository() {
    }

    public static void upload(InteractionHand hand, byte[] jpeg) throws IOException {
        PhotoImageCodec.Dimensions dimensions = PhotoImageCodec.validateJpeg(jpeg);
        long now = System.currentTimeMillis();
        if (activeRequest != null && now - activeRequestStarted > 10_000L) {
            reject(activeRequest);
        }
        PENDING_UPLOADS.entrySet().removeIf(entry -> now - entry.getValue() > 60_000L);
        UUID uploadId = UUID.randomUUID();
        String hash = PhotoImageCodec.sha256(jpeg);
        PENDING_UPLOADS.put(uploadId, now);
        GuaniaoNetwork.sendToServer(new BeginPhotoUploadPacket(
                uploadId,
                hand,
                jpeg.length,
                dimensions.width(),
                dimensions.height(),
                hash
        ));
        for (int offset = 0, index = 0; offset < jpeg.length; offset += PhotoTransferLimits.MAX_CHUNK_BYTES, index++) {
            int end = Math.min(offset + PhotoTransferLimits.MAX_CHUNK_BYTES, jpeg.length);
            GuaniaoNetwork.sendToServer(new PhotoUploadChunkPacket(uploadId, index, Arrays.copyOfRange(jpeg, offset, end)));
        }
        GuaniaoNetwork.sendToServer(new FinishPhotoUploadPacket(uploadId));
    }

    public static void captureResult(UUID uploadId, boolean success) {
        if (PENDING_UPLOADS.remove(uploadId) == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(
                    success ? "item.guaniao.nikon_d750.captured" : "item.guaniao.nikon_d750.capture_failed"
            ), true);
        }
    }

    public static byte[] getOrRequest(String photoId, String expectedHash) {
        CachedImage cached = IMAGES.get(photoId);
        if (cached != null && (expectedHash == null || expectedHash.isEmpty() || expectedHash.equals(cached.contentHash()))) {
            return cached.data();
        }
        if (cached != null) {
            IMAGES.remove(photoId);
        }
        long now = System.currentTimeMillis();
        if (!DOWNLOADS.containsKey(photoId) && !VALIDATING.contains(photoId)
                && !photoId.equals(activeRequest) && now >= RETRY_AFTER.getOrDefault(photoId, 0L)) {
            EXPECTED_HASHES.put(photoId, expectedHash == null ? "" : expectedHash);
            RETRY_AFTER.put(photoId, now + REQUEST_RETRY_MILLIS);
            if (REQUEST_QUEUE.size() < MAX_CACHED_IMAGES) {
                REQUEST_QUEUE.putIfAbsent(photoId, expectedHash == null ? "" : expectedHash);
                dispatchNextRequest();
            }
        }
        return null;
    }

    public static byte[] cached(String photoId) {
        CachedImage cached = IMAGES.get(photoId);
        return cached == null ? null : cached.data();
    }

    public static void beginDownload(String photoId, boolean found, int totalBytes, int width, int height, String contentHash) {
        if (!found) {
            DOWNLOADS.remove(photoId);
            RETRY_AFTER.put(photoId, System.currentTimeMillis() + REQUEST_RETRY_MILLIS);
            finishActiveRequest(photoId);
            return;
        }
        String expectedHash = EXPECTED_HASHES.getOrDefault(photoId, "");
        if (totalBytes <= 0
                || totalBytes > PhotoTransferLimits.MAX_COMPRESSED_BYTES
                || !PhotoTransferLimits.isSupportedDimensions(width, height)
                || !PhotoImageCodec.isSha256(contentHash)
                || (!expectedHash.isEmpty() && !expectedHash.equals(contentHash))) {
            reject(photoId);
            return;
        }
        DOWNLOADS.put(photoId, new DownloadSession(totalBytes, width, height, contentHash));
    }

    public static void acceptDownloadChunk(String photoId, int chunkIndex, byte[] data) {
        DownloadSession session = DOWNLOADS.get(photoId);
        if (session == null || !session.accept(chunkIndex, data)) {
            reject(photoId);
            return;
        }
        if (!session.complete()) {
            return;
        }
        DOWNLOADS.remove(photoId);
        finishActiveRequest(photoId);
        final byte[] jpeg;
        try {
            jpeg = session.assemble();
        } catch (IOException exception) {
            reject(photoId);
            return;
        }
        int validationGeneration = generation;
        VALIDATING.add(photoId);
        VALIDATION_EXECUTOR.execute(() -> {
            boolean valid;
            try {
                valid = session.contentHash.equals(PhotoImageCodec.sha256(jpeg));
                if (valid) {
                    PhotoImageCodec.Dimensions dimensions = PhotoImageCodec.validateJpeg(jpeg);
                    valid = dimensions.width() == session.width && dimensions.height() == session.height;
                }
            } catch (IOException | RuntimeException exception) {
                valid = false;
            }
            boolean accepted = valid;
            Minecraft.getInstance().execute(() -> finishValidation(
                    photoId, session.contentHash, jpeg, validationGeneration, accepted));
        });
    }

    public static void clear() {
        IMAGES.clear();
        DOWNLOADS.clear();
        EXPECTED_HASHES.clear();
        RETRY_AFTER.clear();
        PENDING_UPLOADS.clear();
        REQUEST_QUEUE.clear();
        FAILURE_COUNTS.clear();
        VALIDATING.clear();
        generation++;
        activeRequest = null;
        activeRequestStarted = 0L;
    }

    private static void reject(String photoId) {
        DOWNLOADS.remove(photoId);
        VALIDATING.remove(photoId);
        EXPECTED_HASHES.remove(photoId);
        int failures = Math.min(5, FAILURE_COUNTS.getOrDefault(photoId, 0) + 1);
        FAILURE_COUNTS.put(photoId, failures);
        long delay = Math.min(60_000L, REQUEST_RETRY_MILLIS << (failures - 1));
        RETRY_AFTER.put(photoId, System.currentTimeMillis() + delay);
        REQUEST_QUEUE.remove(photoId);
        finishActiveRequest(photoId);
    }

    private static void dispatchNextRequest() {
        if (activeRequest != null || REQUEST_QUEUE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, String>> iterator = REQUEST_QUEUE.entrySet().iterator();
        Map.Entry<String, String> next = iterator.next();
        activeRequest = next.getKey();
        activeRequestStarted = System.currentTimeMillis();
        String expectedHash = next.getValue();
        iterator.remove();
        GuaniaoNetwork.sendToServer(new PhotoRequestPacket(activeRequest, expectedHash));
    }

    private static void finishActiveRequest(String photoId) {
        if (photoId.equals(activeRequest)) {
            activeRequest = null;
            activeRequestStarted = 0L;
            dispatchNextRequest();
        }
    }

    private static void finishValidation(String photoId, String contentHash, byte[] jpeg, int validationGeneration, boolean valid) {
        VALIDATING.remove(photoId);
        if (validationGeneration != generation) {
            return;
        }
        if (!valid) {
            reject(photoId);
            return;
        }
        IMAGES.put(photoId, new CachedImage(jpeg, contentHash));
        EXPECTED_HASHES.remove(photoId);
        RETRY_AFTER.remove(photoId);
        FAILURE_COUNTS.remove(photoId);
        evictOldestImages();
    }

    private static void evictOldestImages() {
        Iterator<String> iterator = IMAGES.keySet().iterator();
        while (IMAGES.size() > MAX_CACHED_IMAGES && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private record CachedImage(byte[] data, String contentHash) {
    }

    private static final class DownloadSession {
        private final int totalBytes;
        private final int width;
        private final int height;
        private final String contentHash;
        private final byte[][] chunks;
        private int receivedBytes;

        private DownloadSession(int totalBytes, int width, int height, String contentHash) {
            this.totalBytes = totalBytes;
            this.width = width;
            this.height = height;
            this.contentHash = contentHash;
            this.chunks = new byte[(totalBytes + PhotoTransferLimits.MAX_CHUNK_BYTES - 1) / PhotoTransferLimits.MAX_CHUNK_BYTES][];
        }

        private boolean accept(int chunkIndex, byte[] data) {
            if (chunkIndex < 0 || chunkIndex >= this.chunks.length || this.chunks[chunkIndex] != null) {
                return false;
            }
            int expected = Math.min(
                    PhotoTransferLimits.MAX_CHUNK_BYTES,
                    this.totalBytes - chunkIndex * PhotoTransferLimits.MAX_CHUNK_BYTES
            );
            if (data.length != expected || this.receivedBytes + data.length > this.totalBytes) {
                return false;
            }
            this.chunks[chunkIndex] = Arrays.copyOf(data, data.length);
            this.receivedBytes += data.length;
            return true;
        }

        private boolean complete() {
            return this.receivedBytes == this.totalBytes && Arrays.stream(this.chunks).allMatch(chunk -> chunk != null);
        }

        private byte[] assemble() throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream(this.totalBytes);
            for (byte[] chunk : this.chunks) {
                output.write(chunk);
            }
            return output.toByteArray();
        }
    }
}
