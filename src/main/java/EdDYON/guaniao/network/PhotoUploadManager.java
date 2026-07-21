package EdDYON.guaniao.network;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.camera.PhotoImageCodec;
import EdDYON.guaniao.content.camera.PhotoIndexSavedData;
import EdDYON.guaniao.content.camera.PhotoIndexSavedData.PhotoRecord;
import EdDYON.guaniao.content.camera.PhotoIndexSavedData.PhotoStatus;
import EdDYON.guaniao.content.camera.PhotoIoService;
import EdDYON.guaniao.content.camera.PhotoRepository;
import EdDYON.guaniao.content.camera.PhotoTransferLimits;
import EdDYON.guaniao.content.camera.PhotographData;
import EdDYON.guaniao.registry.GuaniaoItems;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Main-thread session state backed by bounded asynchronous photograph I/O. */
public final class PhotoUploadManager {
    private static final DateTimeFormatter PHOTO_NAME_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<UUID, UploadSession> ACTIVE_UPLOADS = new HashMap<>();
    private static final Map<UUID, UUID> PROCESSING_UPLOADS = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_STORE_BYTES = new HashMap<>();
    private static final Map<UUID, ArrayDeque<RateEntry>> UPLOAD_RATE = new HashMap<>();
    private static final Map<UUID, RequestWindow> DOWNLOAD_RATE = new HashMap<>();
    private static final Set<UUID> PENDING_DOWNLOADS = new HashSet<>();
    private static final Map<UUID, DownloadSession> DOWNLOADS = new LinkedHashMap<>();
    private static final ArrayDeque<UUID> DOWNLOAD_ORDER = new ArrayDeque<>();

    private PhotoUploadManager() {
    }

    public static void begin(ServerPlayer player, UUID uploadId, InteractionHand hand, int totalBytes, int width, int height, String contentHash) {
        long now = gameTime(player.server);
        cleanupExpired(player.server, now);
        if (!canUpload(player)
                || ACTIVE_UPLOADS.containsKey(player.getUUID())
                || PROCESSING_UPLOADS.containsKey(player.getUUID())
                || totalBytes <= 0
                || totalBytes > PhotoTransferLimits.MAX_COMPRESSED_BYTES
                || width != PhotoTransferLimits.IMAGE_WIDTH
                || height != PhotoTransferLimits.IMAGE_HEIGHT
                || !PhotoImageCodec.isSha256(contentHash)
                || !quotaAllows(player.server, player.getUUID(), totalBytes)) {
            fail(player, uploadId);
            return;
        }

        ItemStack camera = player.getItemInHand(hand);
        if (!camera.is(GuaniaoItems.NIKON_D750.get()) || player.getCooldowns().isOnCooldown(camera.getItem())) {
            fail(player, uploadId);
            return;
        }
        if (!reserveUploadBytes(player.getUUID(), now, totalBytes)) {
            fail(player, uploadId);
            return;
        }

        player.getCooldowns().addCooldown(camera.getItem(), PhotoTransferLimits.CAPTURE_COOLDOWN_TICKS);
        ACTIVE_UPLOADS.put(player.getUUID(), new UploadSession(uploadId, totalBytes, contentHash, now));
    }

    public static void acceptChunk(ServerPlayer player, UUID uploadId, int chunkIndex, byte[] data) {
        UploadSession session = ACTIVE_UPLOADS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!session.uploadId.equals(uploadId) || !session.accept(chunkIndex, data)) {
            ACTIVE_UPLOADS.remove(player.getUUID());
            fail(player, session.uploadId);
        }
    }

    public static void finish(ServerPlayer player, UUID uploadId) {
        UUID playerId = player.getUUID();
        UploadSession session = ACTIVE_UPLOADS.remove(playerId);
        if (session == null) {
            return;
        }
        if (!session.uploadId.equals(uploadId) || !session.complete()
                || !quotaAllows(player.server, playerId, session.totalBytes)) {
            fail(player, session.uploadId);
            return;
        }

        MinecraftServer server = player.server;
        String ownerName = player.getScoreboardName();
        long gameTime = player.level().getGameTime();
        String filmName = captureDate();
        String location = captureLocation(player);
        reservePendingStore(playerId, session.totalBytes);
        PROCESSING_UPLOADS.put(playerId, uploadId);

        boolean accepted = PhotoIoService.submit(
                server,
                () -> validateAndStore(server, session),
                result -> finishStoredUpload(server, playerId, ownerName, gameTime, filmName, location, uploadId, result),
                throwable -> {
                    releasePendingStore(playerId);
                    PROCESSING_UPLOADS.remove(playerId);
                    GuaniaoMod.LOGGER.warn("Failed to validate or store photograph for {}", ownerName, throwable);
                    ServerPlayer online = server.getPlayerList().getPlayer(playerId);
                    if (online != null) {
                        fail(online, uploadId);
                    }
                }
        );
        if (!accepted) {
            releasePendingStore(playerId);
            PROCESSING_UPLOADS.remove(playerId);
            fail(player, uploadId);
        }
    }

    public static void requestDownload(ServerPlayer player, String photoId, String expectedHash) {
        UUID playerId = player.getUUID();
        if (!allowDownload(player)
                || !PhotoRepository.isValidPhotoId(photoId)
                || (!expectedHash.isEmpty() && !PhotoImageCodec.isSha256(expectedHash))
                || PENDING_DOWNLOADS.contains(playerId)
                || DOWNLOADS.containsKey(playerId)
                || PENDING_DOWNLOADS.size() + DOWNLOADS.size() >= BirdConfigManager.maxConcurrentPhotoDownloads()) {
            sendMissing(player, photoId);
            return;
        }
        PhotoRecord indexed = PhotoIndexSavedData.get(player.server).get(photoId);
        if (indexed != null && (indexed.status() != PhotoStatus.ACTIVE
                || !expectedHash.isEmpty() && !expectedHash.equals(indexed.contentHash()))) {
            sendMissing(player, photoId);
            return;
        }

        MinecraftServer server = player.server;
        PENDING_DOWNLOADS.add(playerId);
        boolean accepted = PhotoIoService.submit(
                server,
                () -> loadDownload(server, photoId, expectedHash),
                loaded -> {
                    PENDING_DOWNLOADS.remove(playerId);
                    ServerPlayer online = server.getPlayerList().getPlayer(playerId);
                    if (online == null) {
                        return;
                    }
                    PhotoIndexSavedData index = PhotoIndexSavedData.get(server);
                    PhotoRecord record = index.get(photoId);
                    long now = System.currentTimeMillis();
                    if (record == null) {
                        index.register(new PhotoRecord(
                                photoId, null, "", now, now, 0L, loaded.data.length,
                                PhotoTransferLimits.IMAGE_WIDTH, PhotoTransferLimits.IMAGE_HEIGHT,
                                loaded.contentHash, PhotoStatus.ACTIVE
                        ));
                    } else {
                        index.updateFileMetadata(
                                photoId, loaded.data.length,
                                PhotoTransferLimits.IMAGE_WIDTH, PhotoTransferLimits.IMAGE_HEIGHT,
                                loaded.contentHash, now
                        );
                    }
                    GuaniaoNetwork.sendToPlayer(new PhotoDownloadStartPacket(
                            photoId, true, loaded.data.length,
                            PhotoTransferLimits.IMAGE_WIDTH, PhotoTransferLimits.IMAGE_HEIGHT,
                            loaded.contentHash
                    ), online);
                    DOWNLOADS.put(playerId, new DownloadSession(photoId, loaded.data, loaded.contentHash, gameTime(server)));
                    DOWNLOAD_ORDER.addLast(playerId);
                },
                throwable -> {
                    PENDING_DOWNLOADS.remove(playerId);
                    ServerPlayer online = server.getPlayerList().getPlayer(playerId);
                    if (online != null) {
                        GuaniaoMod.LOGGER.debug("Unable to serve photograph {} to {}", photoId, online.getScoreboardName(), throwable);
                        sendMissing(online, photoId);
                    }
                }
        );
        if (!accepted) {
            PENDING_DOWNLOADS.remove(playerId);
            sendMissing(player, photoId);
        }
    }

    public static boolean allowDownload(ServerPlayer player) {
        long now = gameTime(player.server);
        RequestWindow window = DOWNLOAD_RATE.computeIfAbsent(player.getUUID(), ignored -> new RequestWindow(now));
        if (now - window.startedAt >= 20L) {
            window.startedAt = now;
            window.requests = 0;
        }
        return ++window.requests <= 8;
    }

    public static void tick(MinecraftServer server) {
        long now = gameTime(server);
        cleanupExpired(server, now);
        int budget = BirdConfigManager.photoDownloadBytesPerTick();
        int sessionsThisTick = DOWNLOAD_ORDER.size();
        for (int visited = 0; visited < sessionsThisTick && budget > 0; visited++) {
            UUID playerId = DOWNLOAD_ORDER.pollFirst();
            if (playerId == null) {
                break;
            }
            DownloadSession session = DOWNLOADS.get(playerId);
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (session == null || player == null || now - session.startedAt > PhotoTransferLimits.DOWNLOAD_TIMEOUT_TICKS) {
                DOWNLOADS.remove(playerId);
                continue;
            }
            int chunkBytes = session.nextChunkBytes();
            if (chunkBytes > budget) {
                DOWNLOAD_ORDER.addFirst(playerId);
                break;
            }
            GuaniaoNetwork.sendToPlayer(session.nextPacket(), player);
            budget -= chunkBytes;
            if (session.complete()) {
                DOWNLOADS.remove(playerId);
            } else {
                DOWNLOAD_ORDER.addLast(playerId);
            }
        }
    }

    public static void disconnect(UUID playerId) {
        ACTIVE_UPLOADS.remove(playerId);
        DOWNLOAD_RATE.remove(playerId);
        PENDING_DOWNLOADS.remove(playerId);
        DOWNLOADS.remove(playerId);
        DOWNLOAD_ORDER.removeIf(playerId::equals);
    }

    public static void clear() {
        ACTIVE_UPLOADS.clear();
        PROCESSING_UPLOADS.clear();
        PENDING_STORE_BYTES.clear();
        UPLOAD_RATE.clear();
        DOWNLOAD_RATE.clear();
        PENDING_DOWNLOADS.clear();
        DOWNLOADS.clear();
        DOWNLOAD_ORDER.clear();
    }

    public static int activeUploads() {
        return ACTIVE_UPLOADS.size() + PROCESSING_UPLOADS.size();
    }

    public static int activeDownloads() {
        return PENDING_DOWNLOADS.size() + DOWNLOADS.size();
    }

    private static CaptureJobResult validateAndStore(MinecraftServer server, UploadSession session) throws IOException {
        byte[] jpeg = session.assemble();
        String actualHash = PhotoImageCodec.sha256(jpeg);
        if (!session.contentHash.equals(actualHash)) {
            throw new IOException("Photograph hash mismatch");
        }
        PhotoImageCodec.Dimensions dimensions = PhotoImageCodec.validateJpeg(jpeg);
        String photoId = UUID.randomUUID().toString();
        PhotoRepository.storeValidated(server, photoId, jpeg);
        return new CaptureJobResult(photoId, jpeg.length, dimensions.width(), dimensions.height(), actualHash);
    }

    private static void finishStoredUpload(
            MinecraftServer server,
            UUID playerId,
            String ownerName,
            long gameTime,
            String filmName,
            String location,
            UUID uploadId,
            CaptureJobResult result
    ) {
        releasePendingStore(playerId);
        PROCESSING_UPLOADS.remove(playerId);
        long now = System.currentTimeMillis();
        PhotoIndexSavedData index = PhotoIndexSavedData.get(server);
        index.register(new PhotoRecord(
                result.photoId, playerId, ownerName, now, now, 0L, result.bytes,
                result.width, result.height, result.contentHash, PhotoStatus.ACTIVE
        ));

        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            trashUndelivered(server, index, result.photoId, now);
            return;
        }

        try {
            ItemStack film = new ItemStack(GuaniaoItems.FILM.get());
            PhotographData.writeReference(
                    film, result.photoId, ownerName, playerId, gameTime,
                    result.width, result.height, result.contentHash
            );
            film.setHoverName(Component.translatable("item.guaniao.film.named", filmName, location));
            if (!player.getInventory().add(film)) {
                player.drop(film, false);
            }
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 0.8F, 1.25F);
            GuaniaoNetwork.sendToPlayer(new PhotoCaptureResultPacket(uploadId, true), player);
        } catch (RuntimeException exception) {
            GuaniaoMod.LOGGER.warn("Failed to deliver stored photograph {}", result.photoId, exception);
            trashUndelivered(server, index, result.photoId, now);
            fail(player, uploadId);
        }
    }

    private static void trashUndelivered(MinecraftServer server, PhotoIndexSavedData index, String photoId, long now) {
        index.moveToTrash(photoId, now);
        PhotoIoService.submit(server, () -> {
            PhotoRepository.moveToTrash(server, photoId);
            return photoId;
        }, ignored -> { }, throwable -> GuaniaoMod.LOGGER.warn("Failed to trash an undelivered photograph {}", photoId, throwable));
    }

    private static LoadedDownload loadDownload(MinecraftServer server, String photoId, String expectedHash) throws IOException {
        byte[] data = PhotoRepository.load(server, photoId);
        String hash = PhotoImageCodec.sha256(data);
        if (!expectedHash.isEmpty() && !expectedHash.equals(hash)) {
            throw new IOException("Photograph hash mismatch");
        }
        return new LoadedDownload(data, hash);
    }

    private static boolean canUpload(ServerPlayer player) {
        return BirdConfigManager.photoUploadsEnabled()
                && (!BirdConfigManager.photoUploadsOperatorOnly() || player.hasPermissions(2))
                && (!BirdConfigManager.photoUploadsWhitelistedOnly()
                || player.hasPermissions(2)
                || player.server.getPlayerList().isWhiteListed(player.getGameProfile()));
    }

    private static boolean quotaAllows(MinecraftServer server, UUID playerId, int bytes) {
        PhotoIndexSavedData.Usage usage = PhotoIndexSavedData.get(server).usage(playerId);
        int pendingWorldCount = PENDING_STORE_BYTES.size();
        long pendingWorldBytes = PENDING_STORE_BYTES.values().stream().mapToLong(Integer::longValue).sum();
        int pendingPlayerCount = PENDING_STORE_BYTES.containsKey(playerId) ? 1 : 0;
        long pendingPlayerBytes = PENDING_STORE_BYTES.getOrDefault(playerId, 0);
        return usage.playerCount() + pendingPlayerCount + 1 <= BirdConfigManager.maxPhotosPerPlayer()
                && usage.playerBytes() + pendingPlayerBytes + bytes <= BirdConfigManager.maxPhotoBytesPerPlayer()
                && usage.worldCount() + pendingWorldCount + 1 <= BirdConfigManager.maxPhotosPerWorld()
                && usage.worldBytes() + pendingWorldBytes + bytes <= BirdConfigManager.maxPhotoBytesPerWorld();
    }

    private static void reservePendingStore(UUID playerId, int bytes) {
        PENDING_STORE_BYTES.put(playerId, bytes);
    }

    private static void releasePendingStore(UUID playerId) {
        PENDING_STORE_BYTES.remove(playerId);
    }

    private static boolean reserveUploadBytes(UUID playerId, long now, int bytes) {
        ArrayDeque<RateEntry> entries = UPLOAD_RATE.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        while (!entries.isEmpty() && now - entries.peekFirst().gameTime >= 1200L) {
            entries.removeFirst();
        }
        int used = entries.stream().mapToInt(RateEntry::bytes).sum();
        if (used + bytes > PhotoTransferLimits.MAX_UPLOAD_BYTES_PER_MINUTE) {
            return false;
        }
        entries.addLast(new RateEntry(now, bytes));
        return true;
    }

    private static void cleanupExpired(MinecraftServer server, long now) {
        ACTIVE_UPLOADS.entrySet().removeIf(entry -> {
            UploadSession session = entry.getValue();
            if (now - session.startedAt <= PhotoTransferLimits.UPLOAD_TIMEOUT_TICKS) {
                return false;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                fail(player, session.uploadId);
            }
            return true;
        });
        UPLOAD_RATE.entrySet().removeIf(entry -> {
            ArrayDeque<RateEntry> entries = entry.getValue();
            while (!entries.isEmpty() && now - entries.peekFirst().gameTime >= 1200L) {
                entries.removeFirst();
            }
            return entries.isEmpty();
        });
    }

    private static void fail(ServerPlayer player, UUID uploadId) {
        GuaniaoNetwork.sendToPlayer(new PhotoCaptureResultPacket(uploadId, false), player);
    }

    private static void sendMissing(ServerPlayer player, String photoId) {
        GuaniaoNetwork.sendToPlayer(PhotoDownloadStartPacket.missing(photoId), player);
    }

    private static long gameTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static String captureDate() {
        return LocalDateTime.now().format(PHOTO_NAME_DATE);
    }

    private static String captureLocation(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        String dimension = player.level().dimension().location().getPath();
        return dimension + " " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private record RateEntry(long gameTime, int bytes) {
    }

    private record CaptureJobResult(String photoId, int bytes, int width, int height, String contentHash) {
    }

    private record LoadedDownload(byte[] data, String contentHash) {
    }

    private static final class RequestWindow {
        private long startedAt;
        private int requests;

        private RequestWindow(long startedAt) {
            this.startedAt = startedAt;
        }
    }

    private static final class UploadSession {
        private final UUID uploadId;
        private final int totalBytes;
        private final String contentHash;
        private final long startedAt;
        private final byte[][] chunks;
        private int receivedBytes;

        private UploadSession(UUID uploadId, int totalBytes, String contentHash, long startedAt) {
            this.uploadId = uploadId;
            this.totalBytes = totalBytes;
            this.contentHash = contentHash;
            this.startedAt = startedAt;
            this.chunks = new byte[(totalBytes + PhotoTransferLimits.MAX_CHUNK_BYTES - 1) / PhotoTransferLimits.MAX_CHUNK_BYTES][];
        }

        private boolean accept(int chunkIndex, byte[] data) {
            if (chunkIndex < 0 || chunkIndex >= this.chunks.length || this.chunks[chunkIndex] != null) {
                return false;
            }
            int expected = Math.min(PhotoTransferLimits.MAX_CHUNK_BYTES,
                    this.totalBytes - chunkIndex * PhotoTransferLimits.MAX_CHUNK_BYTES);
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

    private static final class DownloadSession {
        private final String photoId;
        private final byte[] data;
        private final String contentHash;
        private final long startedAt;
        private int offset;
        private int chunkIndex;

        private DownloadSession(String photoId, byte[] data, String contentHash, long startedAt) {
            this.photoId = photoId;
            this.data = data;
            this.contentHash = contentHash;
            this.startedAt = startedAt;
        }

        private int nextChunkBytes() {
            return Math.min(PhotoTransferLimits.MAX_CHUNK_BYTES, this.data.length - this.offset);
        }

        private PhotoDownloadChunkPacket nextPacket() {
            int end = Math.min(this.offset + PhotoTransferLimits.MAX_CHUNK_BYTES, this.data.length);
            byte[] chunk = Arrays.copyOfRange(this.data, this.offset, end);
            PhotoDownloadChunkPacket packet = new PhotoDownloadChunkPacket(this.photoId, this.chunkIndex++, chunk);
            this.offset = end;
            return packet;
        }

        private boolean complete() {
            return this.offset >= this.data.length;
        }
    }
}
