package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class MusicDiscEvents {
    private static final String UWU_FUNK_ID = GuaniaoMod.MOD_ID + ":music_disc_uwu_funk";

    private MusicDiscEvents() {
    }

    @SubscribeEvent
    public static void onChunkDataLoad(ChunkDataEvent.Load event) {
        clearSavedPlayback(event.getData());
    }

    static void clearSavedPlayback(CompoundTag chunkData) {
        // This event runs on the chunk I/O thread, before the jukeboxes are loaded.
        // Only edit their saved data; do not query the world or item registries here.
        ListTag blockEntities = chunkData.getList("block_entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockEntities.size(); i++) {
            CompoundTag blockEntity = blockEntities.getCompound(i);
            if ("minecraft:jukebox".equals(blockEntity.getString("id"))
                    && UWU_FUNK_ID.equals(blockEntity.getCompound("RecordItem").getString("id"))) {
                // NeoForge 1.21 restores the song timer without restarting the client sound.
                // Keep the disc in place; reinserting it starts a fresh playback.
                blockEntity.remove("ticks_since_song_started");
                if (blockEntity.contains("IsPlaying")) blockEntity.putBoolean("IsPlaying", false);
            }
        }
    }
}
