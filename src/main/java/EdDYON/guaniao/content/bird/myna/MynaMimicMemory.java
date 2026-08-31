package EdDYON.guaniao.content.bird.myna;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Bounded, server-authoritative long-term mimic memory for a single myna.
 */
public final class MynaMimicMemory {
    public static final int MAX_LEARNED_CUES = 8;
    private static final int MAX_FAMILIARITY = 12;
    private static final String MEMORY_TAG = "MynaMimicMemory";
    private static final String ENTRIES_TAG = "Entries";

    private final EnumMap<MynaMimicCue, MemoryEntry> entries = new EnumMap<>(MynaMimicCue.class);
    @Nullable
    private MynaMimicCue lastSelectedCue;

    public LearningResult observe(MynaMimicCue cue, long gameTime) {
        return this.observe(cue, gameTime, 1.0F);
    }

    public LearningResult observe(MynaMimicCue cue, long gameTime, float sourcePitch) {
        MemoryEntry entry = this.entries.computeIfAbsent(cue, ignored -> new MemoryEntry(cue));
        long rawInterval = entry.lastRawHeard == Long.MIN_VALUE
                ? -1L : Math.max(0L, gameTime - entry.lastRawHeard);
        entry.lastRawHeard = gameTime;
        entry.observeVoiceSignature(sourcePitch, rawInterval);
        if (entry.lastHeard != Long.MIN_VALUE
                && gameTime - entry.lastHeard < cue.minimumObservationGap()) {
            return LearningResult.IGNORED;
        }

        entry.lastHeard = gameTime;
        if (entry.learned) {
            entry.familiarity = Math.min(MAX_FAMILIARITY, entry.familiarity + 1);
            return LearningResult.REINFORCED;
        }

        entry.progress = Math.min(cue.learningThreshold(), entry.progress + 1);
        if (entry.progress < cue.learningThreshold()) {
            return LearningResult.PROGRESSED;
        }

        this.makeRoomForLearnedCue(cue);
        entry = this.entries.computeIfAbsent(cue, ignored -> new MemoryEntry(cue));
        entry.progress = cue.learningThreshold();
        entry.learned = true;
        entry.familiarity = Math.max(1, entry.familiarity);
        entry.lastHeard = gameTime;
        return LearningResult.LEARNED;
    }

    @Nullable
    public MynaMimicCue selectForRecall(RandomSource random, long gameTime) {
        List<MemoryEntry> candidates = this.entries.values().stream()
                .filter(entry -> entry.learned)
                .filter(entry -> this.lastSelectedCue == null
                        || this.learnedCount() == 1
                        || entry.cue != this.lastSelectedCue)
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (MemoryEntry entry : candidates) {
            totalWeight += recallWeight(entry, gameTime);
        }
        int roll = random.nextInt(Math.max(1, totalWeight));
        for (MemoryEntry entry : candidates) {
            roll -= recallWeight(entry, gameTime);
            if (roll < 0) {
                this.lastSelectedCue = entry.cue;
                return entry.cue;
            }
        }
        this.lastSelectedCue = candidates.get(candidates.size() - 1).cue;
        return this.lastSelectedCue;
    }

    public void markRecalled(MynaMimicCue cue, long gameTime) {
        MemoryEntry entry = this.entries.get(cue);
        if (entry == null || !entry.learned) {
            return;
        }
        entry.lastRecalled = gameTime;
        if (entry.recallCount < Integer.MAX_VALUE) {
            ++entry.recallCount;
        }
    }

    public boolean knows(MynaMimicCue cue) {
        MemoryEntry entry = this.entries.get(cue);
        return entry != null && entry.learned;
    }

    public int learningProgress(MynaMimicCue cue) {
        MemoryEntry entry = this.entries.get(cue);
        return entry == null ? 0 : entry.progress;
    }

    public int learnedCount() {
        int count = 0;
        for (MemoryEntry entry : this.entries.values()) {
            if (entry.learned) {
                ++count;
            }
        }
        return count;
    }

    public float rememberedPitch(MynaMimicCue cue) {
        MemoryEntry entry = this.entries.get(cue);
        return entry == null || entry.pitchSamples <= 0 ? 1.0F : entry.rememberedPitch;
    }

    public int rememberedTempo(MynaMimicCue cue) {
        MemoryEntry entry = this.entries.get(cue);
        return entry == null || entry.tempoSamples <= 0
                ? 6 : Mth.clamp(Math.round(entry.rememberedTempo), 2, 20);
    }

    public void save(CompoundTag ownerTag) {
        CompoundTag memoryTag = new CompoundTag();
        ListTag storedEntries = new ListTag();
        for (MemoryEntry entry : this.entries.values()) {
            if (entry.progress <= 0 && !entry.learned) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Cue", entry.cue.id());
            entryTag.putInt("Progress", entry.progress);
            entryTag.putBoolean("Learned", entry.learned);
            entryTag.putInt("Familiarity", entry.familiarity);
            entryTag.putLong("LastHeard", entry.lastHeard);
            entryTag.putLong("LastRecalled", entry.lastRecalled);
            entryTag.putInt("RecallCount", entry.recallCount);
            entryTag.putFloat("Pitch", entry.rememberedPitch);
            entryTag.putInt("PitchSamples", entry.pitchSamples);
            entryTag.putFloat("Tempo", entry.rememberedTempo);
            entryTag.putInt("TempoSamples", entry.tempoSamples);
            storedEntries.add(entryTag);
        }
        memoryTag.putInt("Version", 2);
        memoryTag.put(ENTRIES_TAG, storedEntries);
        ownerTag.put(MEMORY_TAG, memoryTag);
    }

    public void load(CompoundTag ownerTag) {
        this.entries.clear();
        this.lastSelectedCue = null;
        if (!ownerTag.contains(MEMORY_TAG, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag memoryTag = ownerTag.getCompound(MEMORY_TAG);
        ListTag storedEntries = memoryTag.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < storedEntries.size(); ++index) {
            CompoundTag entryTag = storedEntries.getCompound(index);
            MynaMimicCue cue = MynaMimicCue.byId(entryTag.getString("Cue"));
            if (cue == null || this.entries.containsKey(cue)) {
                continue;
            }
            MemoryEntry entry = new MemoryEntry(cue);
            entry.progress = Mth.clamp(entryTag.getInt("Progress"), 0, cue.learningThreshold());
            entry.learned = entryTag.getBoolean("Learned") || entry.progress >= cue.learningThreshold();
            if (entry.learned) {
                entry.progress = cue.learningThreshold();
            }
            entry.familiarity = entry.learned
                    ? Mth.clamp(entryTag.getInt("Familiarity"), 1, MAX_FAMILIARITY)
                    : 0;
            entry.lastHeard = entryTag.contains("LastHeard", Tag.TAG_LONG)
                    ? entryTag.getLong("LastHeard") : Long.MIN_VALUE;
            entry.lastRecalled = entryTag.contains("LastRecalled", Tag.TAG_LONG)
                    ? entryTag.getLong("LastRecalled") : Long.MIN_VALUE;
            entry.recallCount = Math.max(0, entryTag.getInt("RecallCount"));
            entry.rememberedPitch = entryTag.contains("Pitch", Tag.TAG_FLOAT)
                    ? Mth.clamp(entryTag.getFloat("Pitch"), 0.5F, 2.0F) : 1.0F;
            entry.pitchSamples = Mth.clamp(entryTag.getInt("PitchSamples"), 0, 16);
            entry.rememberedTempo = entryTag.contains("Tempo", Tag.TAG_FLOAT)
                    ? Mth.clamp(entryTag.getFloat("Tempo"), 2.0F, 40.0F) : 6.0F;
            entry.tempoSamples = Mth.clamp(entryTag.getInt("TempoSamples"), 0, 16);
            this.entries.put(cue, entry);
        }
        this.trimExcessLearnedCues();
    }

    private void makeRoomForLearnedCue(MynaMimicCue incomingCue) {
        if (this.learnedCount() < MAX_LEARNED_CUES) {
            return;
        }
        MemoryEntry weakest = this.entries.values().stream()
                .filter(entry -> entry.learned && entry.cue != incomingCue)
                .min(Comparator.comparingInt((MemoryEntry entry) -> entry.familiarity)
                        .thenComparingLong(entry -> entry.lastHeard)
                        .thenComparingInt(entry -> entry.recallCount))
                .orElse(null);
        if (weakest != null) {
            this.entries.remove(weakest.cue);
        }
    }

    private void trimExcessLearnedCues() {
        while (this.learnedCount() > MAX_LEARNED_CUES) {
            MemoryEntry weakest = this.entries.values().stream()
                    .filter(entry -> entry.learned)
                    .min(Comparator.comparingInt((MemoryEntry entry) -> entry.familiarity)
                            .thenComparingLong(entry -> entry.lastHeard)
                            .thenComparingInt(entry -> entry.recallCount))
                    .orElse(null);
            if (weakest == null) {
                return;
            }
            this.entries.remove(weakest.cue);
        }
    }

    private static int recallWeight(MemoryEntry entry, long gameTime) {
        long timeSinceRecall = entry.lastRecalled == Long.MIN_VALUE
                ? Long.MAX_VALUE : Math.max(0L, gameTime - entry.lastRecalled);
        int freshnessBonus = timeSinceRecall == Long.MAX_VALUE
                ? 8 : (int)Math.min(8L, timeSinceRecall / 600L);
        return Math.max(1, entry.familiarity * 3 + freshnessBonus);
    }

    public enum LearningResult {
        IGNORED,
        PROGRESSED,
        LEARNED,
        REINFORCED
    }

    private static final class MemoryEntry {
        private final MynaMimicCue cue;
        private int progress;
        private boolean learned;
        private int familiarity;
        private long lastHeard = Long.MIN_VALUE;
        private long lastRecalled = Long.MIN_VALUE;
        private long lastRawHeard = Long.MIN_VALUE;
        private int recallCount;
        private float rememberedPitch = 1.0F;
        private int pitchSamples;
        private float rememberedTempo = 6.0F;
        private int tempoSamples;

        private MemoryEntry(MynaMimicCue cue) {
            this.cue = cue;
        }

        private void observeVoiceSignature(float sourcePitch, long rawInterval) {
            float safePitch = Float.isFinite(sourcePitch)
                    ? Mth.clamp(sourcePitch, 0.5F, 2.0F) : 1.0F;
            int nextPitchSamples = Math.min(16, this.pitchSamples + 1);
            float pitchWeight = 1.0F / nextPitchSamples;
            this.rememberedPitch += (safePitch - this.rememberedPitch) * pitchWeight;
            this.pitchSamples = nextPitchSamples;

            if (rawInterval < 2L || rawInterval > 40L) {
                return;
            }
            int nextTempoSamples = Math.min(16, this.tempoSamples + 1);
            float tempoWeight = 1.0F / nextTempoSamples;
            this.rememberedTempo += ((float)rawInterval - this.rememberedTempo) * tempoWeight;
            this.tempoSamples = nextTempoSamples;
        }
    }
}
