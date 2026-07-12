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
        buffer.writeDouble(global.spawnMultiplier);
        buffer.writeDouble(global.droppingFrequencyMultiplier);
        buffer.writeDouble(global.soundVolumeMultiplier);
        buffer.writeVarInt(global.maxBirdsNearby);
        buffer.writeVarInt(global.maxGroundDroppingsNearby);
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
        }
        buffer.writeEnum(BirdConfigScope.sanitize(data.storageScope));
        buffer.writeBoolean(data.worldScopeAllowed);
    }

    static BirdConfigData decode(FriendlyByteBuf buffer) {
        BirdConfigData data = new BirdConfigData();
        data.global.naturalSpawning = buffer.readBoolean();
        data.global.spawnMultiplier = buffer.readDouble();
        data.global.droppingFrequencyMultiplier = buffer.readDouble();
        data.global.soundVolumeMultiplier = buffer.readDouble();
        data.global.maxBirdsNearby = buffer.readVarInt();
        data.global.maxGroundDroppingsNearby = buffer.readVarInt();
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
            data.birds.put(id, bird);
        }
        data.storageScope = BirdConfigScope.sanitize(buffer.readEnum(BirdConfigScope.class));
        data.worldScopeAllowed = buffer.readBoolean();
        return data;
    }
}
