package EdDYON.guaniao.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

public final class MusicDiscEventsTest {
    public static void main(String[] args) throws Exception {
        CompoundTag playing = jukebox("guaniao:music_disc_uwu_funk", true);
        CompoundTag stopped = jukebox("guaniao:music_disc_uwu_funk", false);
        CompoundTag vanilla = jukebox("minecraft:music_disc_cat", true);
        CompoundTag otherMod = jukebox("other:music_disc", true);
        CompoundTag empty = jukebox("minecraft:air", false);
        empty.remove("RecordItem");
        CompoundTag unrelated = playing.copy();
        unrelated.putString("id", "other:music_player");

        ListTag blockEntities = new ListTag();
        blockEntities.add(playing);
        blockEntities.add(stopped);
        blockEntities.add(vanilla);
        blockEntities.add(otherMod);
        blockEntities.add(empty);
        blockEntities.add(unrelated);
        CompoundTag savedChunk = new CompoundTag();
        savedChunk.put("block_entities", blockEntities);
        savedChunk.putLong("InhabitedTime", 987654L);

        // Reproduce loading the on-disk data while the song was still playing.
        CompoundTag loadedChunk = roundTrip(savedChunk);
        require(loadedChunk.equals(savedChunk), "NBT round trip changed the fixture");
        CompoundTag expected = savedChunk.copy();
        expected.getList("block_entities", 10).getCompound(0).putBoolean("IsPlaying", false);
        MusicDiscEvents.clearSavedPlayback(loadedChunk);
        require(loadedChunk.equals(expected),
                "Only the active UwU Funk playback flag may change; retain the item, tags, timers and other discs");
        require(playing.getBoolean("IsPlaying"), "Loading must not modify a live session's separate data");

        MusicDiscEvents.clearSavedPlayback(loadedChunk);
        require(loadedChunk.equals(expected), "Repeated loading must be idempotent");
        require(roundTrip(loadedChunk).equals(expected), "The stopped state must survive another save/load");

        CompoundTag missingEntities = new CompoundTag();
        MusicDiscEvents.clearSavedPlayback(missingEntities);
        require(missingEntities.isEmpty(), "Chunks without block entities must stay unchanged");
        CompoundTag malformedEntities = new CompoundTag();
        malformedEntities.putString("block_entities", "not a list");
        CompoundTag originalMalformed = malformedEntities.copy();
        MusicDiscEvents.clearSavedPlayback(malformedEntities);
        require(malformedEntities.equals(originalMalformed), "Unrelated malformed data must not be rewritten");
        System.out.println("MusicDiscEventsTest: all 7 checks passed");
    }

    private static CompoundTag jukebox(String discId, boolean playing) {
        CompoundTag disc = new CompoundTag();
        disc.putString("id", discId);
        disc.putByte("Count", (byte) 1);
        CompoundTag customData = new CompoundTag();
        customData.putString("test_note", "keep custom disc data");
        disc.put("tag", customData);
        CompoundTag jukebox = new CompoundTag();
        jukebox.putString("id", "minecraft:jukebox");
        jukebox.putInt("x", -16);
        jukebox.putInt("y", 64);
        jukebox.putInt("z", 32);
        jukebox.put("RecordItem", disc);
        jukebox.putBoolean("IsPlaying", playing);
        jukebox.putLong("RecordStartTick", 100L);
        jukebox.putLong("TickCount", 700L);
        return jukebox;
    }

    private static CompoundTag roundTrip(CompoundTag tag) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.write(tag, new DataOutputStream(bytes));
        return NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
