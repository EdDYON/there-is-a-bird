package EdDYON.guaniao.network;

import EdDYON.guaniao.config.BirdConfigData;
import EdDYON.guaniao.config.BirdConfigScope;
import EdDYON.guaniao.config.BirdGlobalConfig;
import EdDYON.guaniao.config.BirdSpeciesConfig;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;

final class BirdConfigPacketCodec {
    private static final int MAX_BIRD_ENTRIES = 32;

    private BirdConfigPacketCodec() {
    }

    static void encode(FriendlyByteBuf buffer, BirdConfigData data) {
        BirdGlobalConfig global = data.global;
        buffer.writeBoolean(global.naturalSpawning);
        buffer.writeBoolean(global.colonialMode);
        buffer.writeBoolean(global.naturalCrowNests);
        buffer.writeBoolean(global.crowsStoreTreasures);
        buffer.writeBoolean(global.crowsClaimPlayerNests);
        buffer.writeBoolean(global.enablePetBirdCommands);
        buffer.writeBoolean(global.enableSeagullStealing);
        buffer.writeBoolean(global.crowItemSafety);
        buffer.writeBoolean(global.birdsPassThroughLeaves);
        buffer.writeBoolean(global.aprilFoolsMode);
        buffer.writeBoolean(global.droppingPressurePlatePulseEnabled);
        buffer.writeBoolean(global.photoUploadsEnabled);
        buffer.writeBoolean(global.photoUploadsOperatorOnly);
        buffer.writeBoolean(global.photoUploadsWhitelistedOnly);
        buffer.writeDouble(global.spawnMultiplier);
        buffer.writeDouble(global.crowNestGenerationMultiplier);
        buffer.writeDouble(global.droppingFrequencyMultiplier);
        buffer.writeDouble(global.soundVolumeMultiplier);
        buffer.writeVarInt(global.maxBirdsNearby);
        buffer.writeVarInt(global.maxGroundDroppingsNearby);
        buffer.writeVarInt(global.crowNestSearchDistance);
        buffer.writeVarInt(global.maxCrowNestTreasures);
        buffer.writeVarInt(global.maxWildBirdsPerRegion);
        buffer.writeVarInt(global.populationRegionChunks);
        buffer.writeVarInt(global.flockRefreshTicks);
        buffer.writeVarInt(global.habitatCacheTicks);
        buffer.writeVarInt(global.seagullPlayerCooldownTicks);
        buffer.writeVarInt(global.maxConcurrentSeagullTargetsPerPlayer);
        buffer.writeVarInt(global.birdScanBudgetPerTick);
        buffer.writeVarInt(global.droppingPressurePlatePulseTicks);
        buffer.writeVarInt(global.maxPhotosPerPlayer);
        buffer.writeVarInt(global.maxPhotoStorageMiBPerPlayer);
        buffer.writeVarInt(global.maxPhotosPerWorld);
        buffer.writeVarInt(global.maxPhotoStorageMiBPerWorld);
        buffer.writeVarInt(global.photoTrashRetentionDays);
        buffer.writeVarInt(global.maxConcurrentPhotoDownloads);
        buffer.writeVarInt(global.photoDownloadKiBPerTick);
        buffer.writeVarInt(data.birds.size());
        for (Map.Entry<String, BirdSpeciesConfig> entry : data.birds.entrySet()) {
            BirdSpeciesConfig bird = entry.getValue();
            buffer.writeUtf(entry.getKey(), 64);
            buffer.writeBoolean(bird.enabled);
            buffer.writeBoolean(bird.naturalSpawning);
            buffer.writeDouble(bird.spawnMultiplier);
            buffer.writeVarInt(bird.minGroup);
            buffer.writeVarInt(bird.maxGroup);
            buffer.writeDouble(bird.droppingFrequencyMultiplier);
            buffer.writeDouble(bird.soundVolumeMultiplier);
            buffer.writeVarInt(bird.maxWildNearby);
            buffer.writeDouble(bird.flockRadius);
            buffer.writeVarInt(bird.flockMaxMembers);
            buffer.writeVarInt(bird.foodScanInterval);
            buffer.writeVarInt(bird.threatScanInterval);
            buffer.writeDouble(bird.ownerTeleportDistance);
            buffer.writeDouble(bird.ambientSoundCooldownMultiplier);
        }
        buffer.writeEnum(BirdConfigScope.sanitize(data.storageScope));
        buffer.writeBoolean(data.worldScopeAllowed);
    }

    static BirdConfigData decode(FriendlyByteBuf buffer) {
        BirdConfigData data = new BirdConfigData();
        data.global.naturalSpawning = buffer.readBoolean();
        data.global.colonialMode = buffer.readBoolean();
        data.global.naturalCrowNests = buffer.readBoolean();
        data.global.crowsStoreTreasures = buffer.readBoolean();
        data.global.crowsClaimPlayerNests = buffer.readBoolean();
        data.global.enablePetBirdCommands = buffer.readBoolean();
        data.global.enableSeagullStealing = buffer.readBoolean();
        data.global.crowItemSafety = buffer.readBoolean();
        data.global.birdsPassThroughLeaves = buffer.readBoolean();
        data.global.aprilFoolsMode = buffer.readBoolean();
        data.global.droppingPressurePlatePulseEnabled = buffer.readBoolean();
        data.global.photoUploadsEnabled = buffer.readBoolean();
        data.global.photoUploadsOperatorOnly = buffer.readBoolean();
        data.global.photoUploadsWhitelistedOnly = buffer.readBoolean();
        data.global.spawnMultiplier = buffer.readDouble();
        data.global.crowNestGenerationMultiplier = buffer.readDouble();
        data.global.droppingFrequencyMultiplier = buffer.readDouble();
        data.global.soundVolumeMultiplier = buffer.readDouble();
        data.global.maxBirdsNearby = buffer.readVarInt();
        data.global.maxGroundDroppingsNearby = buffer.readVarInt();
        data.global.crowNestSearchDistance = buffer.readVarInt();
        data.global.maxCrowNestTreasures = buffer.readVarInt();
        data.global.maxWildBirdsPerRegion = buffer.readVarInt();
        data.global.populationRegionChunks = buffer.readVarInt();
        data.global.flockRefreshTicks = buffer.readVarInt();
        data.global.habitatCacheTicks = buffer.readVarInt();
        data.global.seagullPlayerCooldownTicks = buffer.readVarInt();
        data.global.maxConcurrentSeagullTargetsPerPlayer = buffer.readVarInt();
        data.global.birdScanBudgetPerTick = buffer.readVarInt();
        data.global.droppingPressurePlatePulseTicks = buffer.readVarInt();
        data.global.maxPhotosPerPlayer = buffer.readVarInt();
        data.global.maxPhotoStorageMiBPerPlayer = buffer.readVarInt();
        data.global.maxPhotosPerWorld = buffer.readVarInt();
        data.global.maxPhotoStorageMiBPerWorld = buffer.readVarInt();
        data.global.photoTrashRetentionDays = buffer.readVarInt();
        data.global.maxConcurrentPhotoDownloads = buffer.readVarInt();
        data.global.photoDownloadKiBPerTick = buffer.readVarInt();
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_BIRD_ENTRIES) {
            throw new IllegalArgumentException("Invalid bird config entry count: " + size);
        }
        data.birds.clear();
        for (int i = 0; i < size; i++) {
            String id = buffer.readUtf(64);
            BirdSpeciesConfig bird = new BirdSpeciesConfig();
            bird.enabled = buffer.readBoolean();
            bird.naturalSpawning = buffer.readBoolean();
            bird.spawnMultiplier = buffer.readDouble();
            bird.minGroup = buffer.readVarInt();
            bird.maxGroup = buffer.readVarInt();
            bird.droppingFrequencyMultiplier = buffer.readDouble();
            bird.soundVolumeMultiplier = buffer.readDouble();
            bird.maxWildNearby = buffer.readVarInt();
            bird.flockRadius = buffer.readDouble();
            bird.flockMaxMembers = buffer.readVarInt();
            bird.foodScanInterval = buffer.readVarInt();
            bird.threatScanInterval = buffer.readVarInt();
            bird.ownerTeleportDistance = buffer.readDouble();
            bird.ambientSoundCooldownMultiplier = buffer.readDouble();
            data.birds.put(id, bird);
        }
        data.storageScope = BirdConfigScope.sanitize(buffer.readEnum(BirdConfigScope.class));
        data.worldScopeAllowed = buffer.readBoolean();
        return data;
    }
}
