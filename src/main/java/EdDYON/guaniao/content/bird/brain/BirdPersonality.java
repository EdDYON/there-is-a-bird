package EdDYON.guaniao.content.bird.brain;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class BirdPersonality {
    private final float boldness;
    private final float wariness;
    private final float activity;
    private final float sociability;
    private final float flightiness;

    private BirdPersonality(float boldness, float wariness, float activity, float sociability, float flightiness) {
        this.boldness = clamp(boldness);
        this.wariness = clamp(wariness);
        this.activity = clamp(activity);
        this.sociability = clamp(sociability);
        this.flightiness = clamp(flightiness);
    }

    public static BirdPersonality create(RandomSource random, BirdSpeciesProfile profile) {
        return new BirdPersonality(
                vary(random, profile.baseBoldness()),
                vary(random, profile.baseWariness()),
                vary(random, profile.baseActivity()),
                vary(random, profile.baseSociability()),
                vary(random, profile.baseFlightiness()));
    }

    public static BirdPersonality load(CompoundTag tag, RandomSource random, BirdSpeciesProfile profile) {
        if (!tag.contains("Boldness", 99)
                || !tag.contains("Wariness", 99)
                || !tag.contains("Activity", 99)
                || !tag.contains("Sociability", 99)
                || !tag.contains("Flightiness", 99)) {
            return BirdPersonality.create(random, profile);
        }
        return new BirdPersonality(
                tag.getFloat("Boldness"),
                tag.getFloat("Wariness"),
                tag.getFloat("Activity"),
                tag.getFloat("Sociability"),
                tag.getFloat("Flightiness"));
    }

    public void save(CompoundTag tag) {
        tag.putFloat("Boldness", this.boldness);
        tag.putFloat("Wariness", this.wariness);
        tag.putFloat("Activity", this.activity);
        tag.putFloat("Sociability", this.sociability);
        tag.putFloat("Flightiness", this.flightiness);
    }

    public float boldness() {
        return this.boldness;
    }

    public float wariness() {
        return this.wariness;
    }

    public float activity() {
        return this.activity;
    }

    public float sociability() {
        return this.sociability;
    }

    public float flightiness() {
        return this.flightiness;
    }

    private static float vary(RandomSource random, float base) {
        return clamp(base + (random.nextFloat() - 0.5F) * 0.24F);
    }

    private static float clamp(float value) {
        return Mth.clamp(value, 0.0F, 1.0F);
    }
}
