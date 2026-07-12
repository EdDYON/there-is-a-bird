package EdDYON.guaniao.config;

public class BirdSpeciesConfig {
    public boolean enabled = true;
    public boolean naturalSpawning = true;
    public double spawnMultiplier = 1.0D;
    public int minGroup = 1;
    public int maxGroup = 1;
    public double droppingFrequencyMultiplier = 1.0D;
    public double soundVolumeMultiplier = 1.0D;

    public BirdSpeciesConfig() {
    }

    public BirdSpeciesConfig(BirdSpecies species) {
        this.minGroup = species.defaultMinGroup();
        this.maxGroup = species.defaultMaxGroup();
    }

    public BirdSpeciesConfig copy() {
        BirdSpeciesConfig copy = new BirdSpeciesConfig();
        copy.enabled = this.enabled;
        copy.naturalSpawning = this.naturalSpawning;
        copy.spawnMultiplier = this.spawnMultiplier;
        copy.minGroup = this.minGroup;
        copy.maxGroup = this.maxGroup;
        copy.droppingFrequencyMultiplier = this.droppingFrequencyMultiplier;
        copy.soundVolumeMultiplier = this.soundVolumeMultiplier;
        return copy;
    }
}
