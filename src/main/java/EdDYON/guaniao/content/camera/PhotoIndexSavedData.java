package EdDYON.guaniao.content.camera;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent metadata for server-side photographs. JPEG bytes remain outside NBT. */
public final class PhotoIndexSavedData extends SavedData {
    private static final String DATA_NAME = "guaniao_photo_index";
    private final Map<String, PhotoRecord> records = new HashMap<>();

    public static PhotoIndexSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                PhotoIndexSavedData::load,
                PhotoIndexSavedData::new,
                DATA_NAME
        );
    }

    public static PhotoIndexSavedData load(CompoundTag tag) {
        PhotoIndexSavedData data = new PhotoIndexSavedData();
        ListTag list = tag.getList("Photos", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            String id = entry.getString("Id");
            if (!PhotoRepository.isValidPhotoId(id)) {
                continue;
            }
            UUID owner = entry.hasUUID("Owner") ? entry.getUUID("Owner") : null;
            PhotoStatus status = PhotoStatus.byName(entry.getString("Status"));
            PhotoRecord record = new PhotoRecord(
                    id,
                    owner,
                    entry.getString("OwnerName"),
                    Math.max(0L, entry.getLong("CreatedAt")),
                    Math.max(0L, entry.getLong("LastAccessAt")),
                    Math.max(0L, entry.getLong("DeletedAt")),
                    Math.max(0, entry.getInt("Bytes")),
                    Math.max(0, entry.getInt("Width")),
                    Math.max(0, entry.getInt("Height")),
                    entry.getString("Hash"),
                    status
            );
            data.records.put(id, record);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (PhotoRecord record : this.records.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", record.id());
            if (record.owner() != null) {
                entry.putUUID("Owner", record.owner());
            }
            entry.putString("OwnerName", record.ownerName());
            entry.putLong("CreatedAt", record.createdAt());
            entry.putLong("LastAccessAt", record.lastAccessAt());
            entry.putLong("DeletedAt", record.deletedAt());
            entry.putInt("Bytes", record.bytes());
            entry.putInt("Width", record.width());
            entry.putInt("Height", record.height());
            entry.putString("Hash", record.contentHash());
            entry.putString("Status", record.status().serializedName());
            list.add(entry);
        }
        tag.put("Photos", list);
        return tag;
    }

    public PhotoRecord get(String photoId) {
        return this.records.get(photoId);
    }

    public Collection<PhotoRecord> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(this.records.values()));
    }

    public void register(PhotoRecord record) {
        this.records.put(record.id(), record);
        this.setDirty();
    }

    public void touch(String photoId, long now) {
        PhotoRecord record = this.records.get(photoId);
        if (record == null || now <= record.lastAccessAt()) {
            return;
        }
        this.records.put(photoId, record.withLastAccess(now));
        this.setDirty();
    }

    public void updateFileMetadata(String photoId, int bytes, int width, int height, String contentHash, long now) {
        PhotoRecord record = this.records.get(photoId);
        if (record == null) {
            return;
        }
        this.records.put(photoId, new PhotoRecord(
                record.id(), record.owner(), record.ownerName(), record.createdAt(), Math.max(record.lastAccessAt(), now),
                record.deletedAt(), Math.max(0, bytes), Math.max(0, width), Math.max(0, height),
                contentHash == null ? record.contentHash() : contentHash, record.status()
        ));
        this.setDirty();
    }

    public boolean moveToTrash(String photoId, long now) {
        PhotoRecord record = this.records.get(photoId);
        if (record == null || record.status() == PhotoStatus.TRASH) {
            return false;
        }
        this.records.put(photoId, record.withStatus(PhotoStatus.TRASH, now));
        this.setDirty();
        return true;
    }

    public boolean restore(String photoId) {
        PhotoRecord record = this.records.get(photoId);
        if (record == null || record.status() != PhotoStatus.TRASH) {
            return false;
        }
        this.records.put(photoId, record.withStatus(PhotoStatus.ACTIVE, 0L));
        this.setDirty();
        return true;
    }

    public void markMissing(String photoId) {
        PhotoRecord record = this.records.get(photoId);
        if (record != null && record.status() != PhotoStatus.MISSING) {
            this.records.put(photoId, record.withStatus(PhotoStatus.MISSING, 0L));
            this.setDirty();
        }
    }

    public void remove(String photoId) {
        if (this.records.remove(photoId) != null) {
            this.setDirty();
        }
    }

    public Usage usage(UUID owner) {
        int playerCount = 0;
        long playerBytes = 0L;
        int worldCount = 0;
        long worldBytes = 0L;
        int activeCount = 0;
        int trashCount = 0;
        int missingCount = 0;
        for (PhotoRecord record : this.records.values()) {
            worldCount++;
            worldBytes += record.bytes();
            if (owner != null && owner.equals(record.owner())) {
                playerCount++;
                playerBytes += record.bytes();
            }
            switch (record.status()) {
                case ACTIVE -> activeCount++;
                case TRASH -> trashCount++;
                case MISSING -> missingCount++;
            }
        }
        return new Usage(playerCount, playerBytes, worldCount, worldBytes, activeCount, trashCount, missingCount);
    }

    public List<PhotoRecord> ownedBy(UUID owner) {
        if (owner == null) {
            return List.of();
        }
        return this.records.values().stream().filter(record -> owner.equals(record.owner())).toList();
    }

    public enum PhotoStatus {
        ACTIVE,
        TRASH,
        MISSING;

        private String serializedName() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }

        private static PhotoStatus byName(String name) {
            for (PhotoStatus status : values()) {
                if (status.serializedName().equalsIgnoreCase(name)) {
                    return status;
                }
            }
            return ACTIVE;
        }
    }

    public record Usage(
            int playerCount,
            long playerBytes,
            int worldCount,
            long worldBytes,
            int activeCount,
            int trashCount,
            int missingCount
    ) {
    }

    public record PhotoRecord(
            String id,
            UUID owner,
            String ownerName,
            long createdAt,
            long lastAccessAt,
            long deletedAt,
            int bytes,
            int width,
            int height,
            String contentHash,
            PhotoStatus status
    ) {
        public PhotoRecord {
            ownerName = ownerName == null ? "" : ownerName;
            contentHash = contentHash == null ? "" : contentHash;
            status = status == null ? PhotoStatus.ACTIVE : status;
        }

        private PhotoRecord withLastAccess(long value) {
            return new PhotoRecord(this.id, this.owner, this.ownerName, this.createdAt, value, this.deletedAt,
                    this.bytes, this.width, this.height, this.contentHash, this.status);
        }

        private PhotoRecord withStatus(PhotoStatus value, long deleted) {
            return new PhotoRecord(this.id, this.owner, this.ownerName, this.createdAt, this.lastAccessAt, deleted,
                    this.bytes, this.width, this.height, this.contentHash, value);
        }
    }
}
