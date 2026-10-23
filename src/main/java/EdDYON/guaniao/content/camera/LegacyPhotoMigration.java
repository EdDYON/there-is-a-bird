package EdDYON.guaniao.content.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.PhotoIndexSavedData.PhotoRecord;
import EdDYON.guaniao.content.camera.PhotoIndexSavedData.PhotoStatus;
import net.minecraft.nbt.CompoundTag;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Migrates legacy pixel NBT through the same bounded photograph I/O executor. */
public final class LegacyPhotoMigration {
    private static final Queue<MigrationTask> PENDING = new ArrayDeque<>();
    private static final Set<ItemStack> QUEUED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean processing;

    private LegacyPhotoMigration() {
    }

    public static void queue(Level level, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!PhotoMaintenance.isRunning() && !PhotographData.hasLegacyPixels(stack)) {
            observeReference(serverLevel, stack);
        }
        if (!PhotographData.hasLegacyPixels(stack)) {
            return;
        }
        if (QUEUED.add(stack)) {
            PENDING.add(new MigrationTask(serverLevel, stack));
        }
    }

    public static void migrateNow(Level level, ItemStack stack) {
        queue(level, stack);
    }

    public static void tick(MinecraftServer server) {
        if (processing || PhotoMaintenance.isRunning()) {
            return;
        }
        MigrationTask task = PENDING.poll();
        if (task == null) {
            return;
        }
        QUEUED.remove(task.stack());
        if (task.level().getServer() != server || !PhotographData.hasLegacyPixels(task.stack())) {
            return;
        }

        String photoId = PhotographData.id(task.stack());
        if (!PhotoRepository.isValidPhotoId(photoId)) {
            return;
        }
        PhotoRecord existing = PhotoIndexSavedData.get(server).get(photoId);
        if (existing != null && existing.status() != PhotoStatus.ACTIVE) {
            return;
        }
        int[] pixels = PhotographData.pixels(task.stack());
        if (pixels.length != PhotographData.LEGACY_IMAGE_SIZE * PhotographData.LEGACY_IMAGE_SIZE) {
            return;
        }
        int[] snapshot = Arrays.copyOf(pixels, pixels.length);
        processing = true;
        boolean accepted = PhotoIoService.submitMutation(
                server, photoId,
                () -> migrateFiles(server, photoId, snapshot, existing != null),
                result -> {
                    processing = false;
                    if (PhotographData.hasLegacyPixels(task.stack()) && photoId.equals(PhotographData.id(task.stack()))) {
                        CompoundTag migrated = EdDYON.guaniao.util.ItemData.copy(task.stack());
                        migrated.putString(PhotographData.TAG_PHOTO_ID, result.photoId());
                        EdDYON.guaniao.util.ItemData.write(task.stack(), migrated);
                        PhotographData.finishLegacyMigration(task.stack(), result.contentHash());
                    }
                    registerIndex(server, task.stack(), result.photoId(), result);
                },
                throwable -> {
                    processing = false;
                    GuaniaoMod.LOGGER.warn("Failed to migrate legacy photograph {}", photoId, throwable);
                }
        );
        if (!accepted) {
            processing = false;
            if (QUEUED.add(task.stack())) {
                PENDING.add(task);
            }
        }
    }

    public static void clear() {
        PENDING.clear();
        QUEUED.clear();
        processing = false;
    }

    private static MigrationResult migrateFiles(MinecraftServer server, String photoId, int[] pixels,
                                                boolean indexed) throws IOException {
        byte[] jpeg;
        // A legacy payload proves only its own pixels, not ownership of an existing disk file.
        // Keep an orphan intact and import those pixels under a fresh id instead.
        if (!indexed && PhotoRepository.exists(server, photoId)) {
            photoId = UUID.randomUUID().toString();
        }
        if (!PhotoRepository.exists(server, photoId)) {
            PhotoRepository.backupLegacy(server, photoId, pixels);
            jpeg = PhotoImageCodec.encodeJpeg(pixels, PhotographData.LEGACY_IMAGE_SIZE, PhotographData.LEGACY_IMAGE_SIZE);
            PhotoRepository.storeValidated(server, photoId, jpeg);
        } else {
            jpeg = PhotoRepository.load(server, photoId);
            PhotoImageCodec.validateJpeg(jpeg);
        }
        return new MigrationResult(photoId, jpeg.length, PhotoImageCodec.sha256(jpeg));
    }

    private static void registerIndex(MinecraftServer server, ItemStack stack, String photoId, MigrationResult result) {
        PhotoIndexSavedData index = PhotoIndexSavedData.get(server);
        if (index.get(photoId) != null || PhotoIoService.isMutationPending(photoId)) {
            return;
        }
        long now = System.currentTimeMillis();
        UUID owner = PhotographData.photographerId(stack);
        index.register(new PhotoRecord(
                photoId, owner, PhotographData.photographer(stack), now, now, 0L,
                result.bytes(), PhotographData.LEGACY_IMAGE_SIZE, PhotographData.LEGACY_IMAGE_SIZE,
                result.contentHash(), PhotoStatus.ACTIVE
        ));
    }

    private static void observeReference(ServerLevel level, ItemStack stack) {
        if (!PhotographData.hasImage(stack)) {
            return;
        }
        String photoId = PhotographData.id(stack);
        PhotoIndexSavedData index = PhotoIndexSavedData.get(level.getServer());
        if (index.get(photoId) != null || PhotoIoService.isMutationPending(photoId)) {
            return;
        }
        String contentHash = PhotographData.contentHash(stack);
        if (!PhotoImageCodec.isSha256(contentHash)) {
            return;
        }
        long now = System.currentTimeMillis();
        index.register(new PhotoRecord(
                // Item NBT is not proof of ownership. Preserve the file for an administrator
                // to recover its trusted index, but do not grant access to an orphaned photo.
                photoId, null, "", now, now, 0L,
                0, 0, 0, "", PhotoStatus.MISSING
        ));
    }

    private record MigrationTask(ServerLevel level, ItemStack stack) {
    }

    private record MigrationResult(String photoId, int bytes, String contentHash) {
    }
}
