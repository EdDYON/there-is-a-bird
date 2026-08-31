package EdDYON.guaniao.content.advancement;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdSpecies;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

public final class BirdAdvancements {
    public static final ResourceLocation SEAGULL_STOLE_FOOD = id("seagull_stole_food");
    public static final ResourceLocation HIGH_EYE = id("high_eye");
    public static final ResourceLocation FLUFFY_LINE = id("fluffy_line");
    public static final ResourceLocation BIRD_CONFERENCE = id("bird_conference");
    public static final ResourceLocation ACCEPTED_BY_BIRD = id("accepted_by_bird");
    public static final ResourceLocation FOREST_FRIEND = id("forest_friend");
    public static final ResourceLocation SEASIDE_MENACE = id("seaside_menace");
    public static final ResourceLocation RED_LOUDMOUTH = id("red_loudmouth");
    public static final ResourceLocation LITTLE_SUN = id("little_sun");
    public static final ResourceLocation AVIARY_KEEPER = id("aviary_keeper");
    public static final ResourceLocation BIRD_NOTE = id("bird_note");
    public static final ResourceLocation DEV_NOTE_PROGRAMMER = id("dev_note_programmer");
    public static final ResourceLocation DEV_NOTE_KEEPER = id("dev_note_keeper");
    public static final ResourceLocation DEV_NOTE_ANIMATOR = id("dev_note_animator");
    public static final ResourceLocation DEV_NOTE_SOUND = id("dev_note_sound");
    public static final ResourceLocation DEV_NOTE_MODELER = id("dev_note_modeler");
    public static final ResourceLocation DEV_NOTES_ALL = id("dev_notes_all");
    private static final String CRITERION = "triggered";

    /** Maps each dev easter-egg note's author to the hidden "note found" advancement. */
    private static final Map<String, ResourceLocation> DEV_NOTE_BY_AUTHOR = Map.of(
            "蛋炒饭", DEV_NOTE_PROGRAMMER,
            "伊洛哥斯拉", DEV_NOTE_KEEPER,
            "多雨", DEV_NOTE_ANIMATOR,
            "老三", DEV_NOTE_SOUND,
            "千年村庄", DEV_NOTE_MODELER);
    private static final List<ResourceLocation> DEV_NOTE_ADVANCEMENTS =
            List.copyOf(DEV_NOTE_BY_AUTHOR.values());

    private BirdAdvancements() {
    }

    public static void awardTamedBird(Player player, Entity bird) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        grant(serverPlayer, ACCEPTED_BY_BIRD);
        BirdSpecies species = BirdSpecies.from(bird);
        if (species == BirdSpecies.LONG_TAILED_TIT) {
            grant(serverPlayer, FOREST_FRIEND);
        } else if (species == BirdSpecies.SEAGULL) {
            grant(serverPlayer, SEASIDE_MENACE);
        } else if (species == BirdSpecies.MACAW) {
            grant(serverPlayer, RED_LOUDMOUTH);
        } else if (species == BirdSpecies.COCKATIEL) {
            grant(serverPlayer, LITTLE_SUN);
        }
        if (countOwnedLoadedBirds(serverPlayer) >= 5) {
            grant(serverPlayer, AVIARY_KEEPER);
        }
    }

    public static void awardSeagullStoleFood(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            grant(serverPlayer, SEAGULL_STOLE_FOOD);
        }
    }

    /**
     * Records that the player read a dev easter-egg note (matched by author). Each
     * note's hidden advancement latches permanently; once all five are read, the
     * "collect the devs' notes" advancement unlocks.
     */
    public static void awardDevNote(ServerPlayer player, String author) {
        ResourceLocation perNote = DEV_NOTE_BY_AUTHOR.get(author);
        if (perNote == null) {
            return;
        }
        grant(player, perNote);
        for (ResourceLocation noteId : DEV_NOTE_ADVANCEMENTS) {
            if (!isDone(player, noteId)) {
                return;
            }
        }
        grant(player, DEV_NOTES_ALL);
    }

    public static boolean grant(ServerPlayer player, ResourceLocation advancementId) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        return advancement != null && player.getAdvancements().award(advancement, CRITERION);
    }

    public static boolean isDone(ServerPlayer player, ResourceLocation advancementId) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static int countOwnedLoadedBirds(ServerPlayer player) {
        int count = 0;
        for (ServerLevel level : player.server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof TamableAnimal bird
                        && bird.isAlive()
                        && bird.isTame()
                        && player.getUUID().equals(bird.getOwnerUUID())
                        && BirdSpecies.from(entity) != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(GuaniaoMod.MOD_ID, "husbandry/" + path);
    }
}
