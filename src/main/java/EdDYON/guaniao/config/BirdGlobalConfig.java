package EdDYON.guaniao.config;

public class BirdGlobalConfig {
    public boolean naturalSpawning = true;
    public double spawnMultiplier = 1.0D;
    public double droppingFrequencyMultiplier = 1.0D;
    public double soundVolumeMultiplier = 1.0D;
    public int maxBirdsNearby = 48;
    public int maxGroundDroppingsNearby = 8;

    public BirdGlobalConfig copy() {
        BirdGlobalConfig copy = new BirdGlobalConfig();
        copy.naturalSpawning = this.naturalSpawning;
        copy.spawnMultiplier = this.spawnMultiplier;
        copy.droppingFrequencyMultiplier = this.droppingFrequencyMultiplier;
        copy.soundVolumeMultiplier = this.soundVolumeMultiplier;
        copy.maxBirdsNearby = this.maxBirdsNearby;
        copy.maxGroundDroppingsNearby = this.maxGroundDroppingsNearby;
        return copy;
    }
}
