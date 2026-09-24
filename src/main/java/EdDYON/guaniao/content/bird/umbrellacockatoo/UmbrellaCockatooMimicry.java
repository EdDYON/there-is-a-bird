package EdDYON.guaniao.content.bird.umbrellacockatoo;

import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nullable;

/**
 * Which registered bird voice the cockatoo copies when it decides to chatter.
 *
 * <p>Every entry here is an existing {@code guaniao} sound event belonging to the
 * species being imitated, which is the point: the bird is repeating a neighbour,
 * not producing a voice of its own. The cockatoo has no species audio asset yet,
 * so it returns {@code null} for itself and for species with no usable event
 * rather than substituting someone else's recording as its own call.</p>
 */
public final class UmbrellaCockatooMimicry {
    private UmbrellaCockatooMimicry() {
    }

    @Nullable
    public static SoundEvent imitationFor(@Nullable BirdSpecies species) {
        if (species == null) {
            return null;
        }
        return switch (species) {
            case NIGHT_HERON -> GuaniaoSoundEvents.NIGHT_HERON_AMBIENT.get();
            case SPARROW, LONG_TAILED_TIT -> GuaniaoSoundEvents.SPARROW_AMBIENT.get();
            case BUDGERIGAR, COCKATIEL -> GuaniaoSoundEvents.BUDGERIGAR_AMBIENT.get();
            case SPOTTED_DOVE -> GuaniaoSoundEvents.SPOTTED_DOVE_AMBIENT.get();
            case PIGEON -> GuaniaoSoundEvents.PIGEON_AMBIENT.get();
            case WOODCOCK -> GuaniaoSoundEvents.WOODCOCK_AMBIENT.get();
            case SEAGULL -> GuaniaoSoundEvents.SEAGULL_AMBIENT.get();
            case KIWI -> GuaniaoSoundEvents.KIWI_AMBIENT.get();
            case CROW -> GuaniaoSoundEvents.CROW_AMBIENT.get();
            case KESTREL -> GuaniaoSoundEvents.KESTREL_AMBIENT.get();
            case CASSOWARY -> GuaniaoSoundEvents.CASSOWARY_AMBIENT.get();
            case MYNA -> GuaniaoSoundEvents.MYNA_CALL_03.get();
            // The macaw's own voice is not registered as a dedicated imitation
            // source in this project, and the cockatoo has no voice of its own yet.
            case MACAW, UMBRELLA_COCKATOO -> null;
        };
    }
}
