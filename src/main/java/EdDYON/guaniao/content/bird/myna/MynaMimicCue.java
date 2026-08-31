package EdDYON.guaniao.content.bird.myna;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * A stable, data-sized description of a sound pattern a myna can remember.
 *
 * <p>No playback sound is assigned yet. The cue is deliberately separate from
 * {@code SoundEvent}, so saved memories remain valid while real myna recordings
 * are still being prepared.</p>
 */
public enum MynaMimicCue {
    BELL("bell", 3, 28, 42, 30),
    NOTE_BLOCK("note_block", 4, 16, 28, 12),
    DOOR("door", 4, 12, 20, 20),
    CAT("cat", 3, 28, 44, 35),
    WOLF("wolf", 3, 28, 44, 35),
    NIGHT_HERON("night_heron", 3, 30, 50, 40),
    SPARROW("sparrow", 3, 22, 38, 35),
    LONG_TAILED_TIT("long_tailed_tit", 3, 22, 38, 35),
    BUDGERIGAR("budgerigar", 3, 24, 42, 35),
    COCKATIEL("cockatiel", 3, 24, 42, 35),
    MACAW("macaw", 3, 30, 52, 40),
    SPOTTED_DOVE("spotted_dove", 3, 32, 54, 45),
    PIGEON("pigeon", 3, 30, 50, 45),
    CROW("crow", 3, 28, 48, 40),
    SEAGULL("seagull", 3, 28, 48, 40),
    CHEST("chest", 4, 12, 20, 20),
    MECHANISM("mechanism", 4, 12, 22, 16),
    PLAYER("player", 3, 20, 34, 28),
    CREEPER_HISS("creeper_hiss", 2, 30, 48, 34),
    EXPLOSION("explosion", 2, 16, 28, 45),
    ZOMBIE("zombie", 3, 26, 46, 34),
    SKELETON("skeleton", 3, 24, 42, 34),
    SPIDER("spider", 3, 22, 38, 30),
    ENDERMAN("enderman", 3, 28, 48, 38),
    WITCH("witch", 3, 26, 46, 34),
    PILLAGER("pillager", 3, 26, 46, 34);

    private final String id;
    private final int learningThreshold;
    private final int minimumVocalTicks;
    private final int maximumVocalTicks;
    private final int minimumObservationGap;

    MynaMimicCue(String id, int learningThreshold, int minimumVocalTicks,
                 int maximumVocalTicks, int minimumObservationGap) {
        this.id = id;
        this.learningThreshold = learningThreshold;
        this.minimumVocalTicks = minimumVocalTicks;
        this.maximumVocalTicks = maximumVocalTicks;
        this.minimumObservationGap = minimumObservationGap;
    }

    public String id() {
        return this.id;
    }

    public int learningThreshold() {
        return this.learningThreshold;
    }

    public int minimumObservationGap() {
        return this.minimumObservationGap;
    }

    public int randomVocalTicks(RandomSource random) {
        return this.minimumVocalTicks
                + random.nextInt(Math.max(1, this.maximumVocalTicks - this.minimumVocalTicks + 1));
    }

    public int networkId() {
        return this.ordinal();
    }

    @Nullable
    public static MynaMimicCue byId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (MynaMimicCue cue : values()) {
            if (cue.id.equals(normalized)) {
                return cue;
            }
        }
        return null;
    }

    @Nullable
    public static MynaMimicCue byNetworkId(int id) {
        MynaMimicCue[] values = values();
        return id >= 0 && id < values.length ? values[id] : null;
    }

    @Nullable
    public static MynaMimicCue fromSoundId(ResourceLocation soundId) {
        if (soundId == null) {
            return null;
        }
        String namespace = soundId.getNamespace();
        String path = soundId.getPath();
        if ("minecraft".equals(namespace)) {
            if ("block.bell.use".equals(path) || "block.bell.resonate".equals(path)) {
                return BELL;
            }
            if (path.startsWith("block.note_block.")) {
                return NOTE_BLOCK;
            }
            if (isDoorSound(path)) {
                return DOOR;
            }
            if (isChestSound(path)) {
                return CHEST;
            }
            if (isMechanismSound(path)) {
                return MECHANISM;
            }
            if (isCatSound(path)) {
                return CAT;
            }
            if (isWolfSound(path)) {
                return WOLF;
            }
            if (isPlayerSound(path)) {
                return PLAYER;
            }
            if ("entity.creeper.primed".equals(path)) {
                return CREEPER_HISS;
            }
            if ("entity.generic.explode".equals(path)) {
                return EXPLOSION;
            }
            if (isZombieSound(path)) {
                return ZOMBIE;
            }
            if (isSkeletonSound(path)) {
                return SKELETON;
            }
            if (isSpiderSound(path)) {
                return SPIDER;
            }
            if (isEndermanSound(path)) {
                return ENDERMAN;
            }
            if (path.startsWith("entity.witch.")) {
                return WITCH;
            }
            if (path.startsWith("entity.pillager.")) {
                return PILLAGER;
            }
            return null;
        }
        if (!"guaniao".equals(namespace)) {
            return null;
        }
        return switch (path) {
            case "entity.night_heron.ambient" -> NIGHT_HERON;
            case "entity.sparrow.ambient" -> SPARROW;
            case "entity.long_tailed_tit.ambient" -> LONG_TAILED_TIT;
            case "entity.budgerigar.ambient" -> BUDGERIGAR;
            case "entity.cockatiel.ambient" -> COCKATIEL;
            case "entity.macaw.ambient" -> MACAW;
            case "entity.spotted_dove.ambient", "entity.spotted_dove.mate" -> SPOTTED_DOVE;
            case "entity.pigeon.ambient" -> PIGEON;
            case "entity.crow.ambient" -> CROW;
            case "entity.seagull.ambient" -> SEAGULL;
            default -> null;
        };
    }

    private static boolean isDoorSound(String path) {
        if (!path.startsWith("block.") || !(path.endsWith(".open") || path.endsWith(".close"))) {
            return false;
        }
        return path.contains("door.") || path.contains("trapdoor.") || path.contains("fence_gate.");
    }

    private static boolean isChestSound(String path) {
        return path.startsWith("block.chest.")
                || path.startsWith("block.ender_chest.")
                || path.startsWith("block.barrel.");
    }

    private static boolean isMechanismSound(String path) {
        return path.startsWith("block.piston.")
                || path.startsWith("block.lever.")
                || path.startsWith("block.stone_button.")
                || path.startsWith("block.wooden_button.")
                || path.startsWith("block.dispenser.")
                || path.startsWith("block.tripwire.")
                || path.startsWith("block.comparator.");
    }

    private static boolean isPlayerSound(String path) {
        return "entity.player.hurt".equals(path)
                || "entity.player.hurt_drown".equals(path)
                || "entity.player.hurt_freeze".equals(path)
                || "entity.player.hurt_on_fire".equals(path)
                || "entity.player.burp".equals(path);
    }

    private static boolean isZombieSound(String path) {
        return path.startsWith("entity.zombie.")
                || path.startsWith("entity.husk.")
                || path.startsWith("entity.drowned.ambient");
    }

    private static boolean isSkeletonSound(String path) {
        return path.startsWith("entity.skeleton.")
                || path.startsWith("entity.stray.")
                || path.startsWith("entity.wither_skeleton.");
    }

    private static boolean isSpiderSound(String path) {
        return path.startsWith("entity.spider.") || path.startsWith("entity.cave_spider.");
    }

    private static boolean isEndermanSound(String path) {
        return path.startsWith("entity.enderman.");
    }

    private static boolean isCatSound(String path) {
        return "entity.cat.ambient".equals(path)
                || "entity.cat.stray_ambient".equals(path)
                || "entity.cat.purr".equals(path)
                || "entity.cat.purreow".equals(path);
    }

    private static boolean isWolfSound(String path) {
        return "entity.wolf.ambient".equals(path)
                || "entity.wolf.growl".equals(path)
                || "entity.wolf.howl".equals(path)
                || "entity.wolf.pant".equals(path)
                || "entity.wolf.whine".equals(path);
    }
}
