package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
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
                    && UWU_FUNK_ID.equals(blockEntity.getCompound("RecordItem").getString("id"))
                    && blockEntity.getBoolean("IsPlaying")) {
                // Vanilla restores IsPlaying but does not restart the client sound.
                // Keep the disc in place; reinserting it starts a fresh playback.
                blockEntity.putBoolean("IsPlaying", false);
            }
        }
    }
}
