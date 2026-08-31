package EdDYON.guaniao.config;

public class BirdSpeciesConfig {
    public boolean enabled = true;
    public boolean naturalSpawning = true;
    public double spawnMultiplier = 1.0D;
    public int minGroup = 1;
    public int maxGroup = 1;
    public double droppingFrequencyMultiplier = 1.0D;
    public double soundVolumeMultiplier = 1.0D;
    public int maxWildNearby = 12;
    public double flockRadius = 12.0D;
    public int flockMaxMembers = 12;
    public int foodScanInterval = 20;
    public int threatScanInterval = 20;
    public double ownerTeleportDistance = 24.0D;
    public double ambientSoundCooldownMultiplier = 1.0D;

    public BirdSpeciesConfig() {
    }

    public BirdSpeciesConfig(BirdSpecies species) {
        this.minGroup = species.defaultMinGroup();
        this.maxGroup = species.defaultMaxGroup();
        this.maxWildNearby = switch (species) {
            case SPARROW, LONG_TAILED_TIT, PIGEON -> 8;
            case BUDGERIGAR, SPOTTED_DOVE, SEAGULL, MYNA -> 6;
            case COCKATIEL, CROW, KIWI -> 4;
            case MACAW, NIGHT_HERON -> 3;
        };
        this.flockMaxMembers = switch (species) {
            case SPARROW -> 12;
            case LONG_TAILED_TIT, BUDGERIGAR, PIGEON -> 10;
            case SEAGULL, MYNA -> 8;
            case COCKATIEL, SPOTTED_DOVE, CROW -> 6;
            case NIGHT_HERON, KIWI -> 4;
            case MACAW -> 3;
        };
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
        copy.maxWildNearby = this.maxWildNearby;
        copy.flockRadius = this.flockRadius;
        copy.flockMaxMembers = this.flockMaxMembers;
        copy.foodScanInterval = this.foodScanInterval;
        copy.threatScanInterval = this.threatScanInterval;
        copy.ownerTeleportDistance = this.ownerTeleportDistance;
        copy.ambientSoundCooldownMultiplier = this.ambientSoundCooldownMultiplier;
        return copy;
    }
}
