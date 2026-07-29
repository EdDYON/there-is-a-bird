package EdDYON.guaniao.content.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.camera.PhotoIndexSavedData.PhotoRecord;
import EdDYON.guaniao.content.camera.PhotoIndexSavedData.PhotoStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

/** Bounded, asynchronous orphan/missing/trash maintenance for the photograph store. */
public final class PhotoMaintenance {
    private static final int MAX_FILE_ACTIONS = 128;
    private static final int AUTOMATIC_SHARDS_PER_PASS = 16;
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static int nextAutomaticShard;

    private PhotoMaintenance() {
    }

    public static boolean schedule(MinecraftServer server, boolean dryRun, Consumer<Result> callback) {
        return schedule(server, dryRun, callback, null);
    }

    public static boolean scheduleAutomatic(MinecraftServer server, Consumer<Result> callback) {
        int firstShard = nextAutomaticShard;
        boolean accepted = schedule(
                server,
                true,
                callback,
                new ShardWindow(firstShard, AUTOMATIC_SHARDS_PER_PASS)
        );
        if (accepted) {
            nextAutomaticShard = Math.floorMod(firstShard + AUTOMATIC_SHARDS_PER_PASS, 256);
        }
        return accepted;
    }

    private static boolean schedule(
            MinecraftServer server,
            boolean dryRun,
            Consumer<Result> callback,
            ShardWindow shardWindow
    ) {
        if (!RUNNING.compareAndSet(false, true)) {
            return false;
        }
        PhotoIndexSavedData index = PhotoIndexSavedData.get(server);
        Collection<PhotoRecord> records = shardWindow == null
                ? index.snapshot()
                : index.snapshotInShards(shardWindow.firstShard, shardWindow.shardCount);
        int retentionDays = BirdConfigManager.photoTrashRetentionDays();
        long now = System.currentTimeMillis();
        boolean accepted = PhotoIoService.submit(
                server,
                () -> inspect(server, records, retentionDays, now, dryRun, shardWindow),
                result -> {
                    applyIndexChanges(server, result, now, dryRun);
                    RUNNING.set(false);
                    callback.accept(result);
                },
                throwable -> {
                    RUNNING.set(false);
                    GuaniaoMod.LOGGER.warn("Photograph maintenance failed", throwable);
                    callback.accept(Result.failed(throwable.getMessage()));
                }
        );
        if (!accepted) {
            RUNNING.set(false);
        }
        return accepted;
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static void reset() {
        RUNNING.set(false);
        nextAutomaticShard = 0;
    }

    private static Result inspect(
            MinecraftServer server,
            Collection<PhotoRecord> records,
            int retentionDays,
            long now,
            boolean dryRun,
            ShardWindow shardWindow
    ) throws IOException {
        Map<String, PhotoRecord> indexed = new HashMap<>();
        for (PhotoRecord record : records) {
            indexed.put(record.id(), record);
        }
        Set<String> stored = new HashSet<>(shardWindow == null
                ? PhotoRepository.listStoredPhotoIds(server, Integer.MAX_VALUE)
                : PhotoRepository.listStoredPhotoIdsInShards(
                        server, shardWindow.firstShard, shardWindow.shardCount, Integer.MAX_VALUE));
        List<String> missing = new ArrayList<>();
        List<OrphanFile> orphans = new ArrayList<>();
        List<String> deletedTrash = new ArrayList<>();
        int actions = 0;

        for (PhotoRecord record : records) {
            if (shardWindow != null && !shardWindow.includes(record.id())) {
                continue;
            }
            if (record.status() == PhotoStatus.TRASH) {
                long retentionMillis = retentionDays * 86_400_000L;
                if (record.deletedAt() > 0L && now - record.deletedAt() >= retentionMillis) {
                    if (dryRun) {
                        deletedTrash.add(record.id());
                    } else if (actions < MAX_FILE_ACTIONS) {
                        PhotoRepository.deleteTrashPermanently(server, record.id());
                        deletedTrash.add(record.id());
                        actions++;
                    }
                }
            } else if (!stored.contains(record.id())) {
                missing.add(record.id());
            }
        }

        for (String id : stored) {
            if (indexed.containsKey(id)) {
                continue;
            }
            int bytes = (int)Math.min(Integer.MAX_VALUE, Files.size(PhotoRepository.photoPath(server, id)));
            if (dryRun) {
                orphans.add(new OrphanFile(id, bytes));
            } else if (actions < MAX_FILE_ACTIONS) {
                PhotoRepository.moveToTrash(server, id);
                orphans.add(new OrphanFile(id, bytes));
                actions++;
            }
        }
        return new Result(true, dryRun, stored.size(), missing, orphans, deletedTrash, "");
    }

    private static void applyIndexChanges(MinecraftServer server, Result result, long now, boolean dryRun) {
        if (!result.success() || dryRun) {
            return;
        }
        PhotoIndexSavedData index = PhotoIndexSavedData.get(server);
        for (String id : result.missing()) {
            index.markMissing(id);
        }
        for (OrphanFile orphan : result.orphans()) {
            if (index.get(orphan.id()) == null) {
                index.register(new PhotoRecord(
                        orphan.id(), null, "", now, now, now, orphan.bytes(),
                        PhotoTransferLimits.IMAGE_WIDTH, PhotoTransferLimits.IMAGE_HEIGHT, "", PhotoStatus.TRASH
                ));
            }
        }
        for (String id : result.deletedTrash()) {
            index.remove(id);
        }
    }

    public record OrphanFile(String id, int bytes) {
    }

    private record ShardWindow(int firstShard, int shardCount) {
        private boolean includes(String photoId) {
            if (photoId == null || photoId.length() < 2) {
                return false;
            }
            try {
                int shard = Integer.parseInt(photoId.substring(0, 2), 16);
                for (int offset = 0; offset < this.shardCount; offset++) {
                    if (shard == Math.floorMod(this.firstShard + offset, 256)) {
                        return true;
                    }
                }
            } catch (NumberFormatException ignored) {
                return false;
            }
            return false;
        }
    }

    public record Result(
            boolean success,
            boolean dryRun,
            int storedFiles,
            List<String> missing,
            List<OrphanFile> orphans,
            List<String> deletedTrash,
            String error
    ) {
        private static Result failed(String error) {
            return new Result(false, false, 0, List.of(), List.of(), List.of(), error == null ? "unknown" : error);
        }
    }
}
