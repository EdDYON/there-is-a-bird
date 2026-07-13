package EdDYON.guaniao.config;

public class BirdGlobalConfig {
    public boolean naturalSpawning = true;
    public boolean colonialMode;
    public boolean naturalCrowNests = true;
    public boolean crowsStoreTreasures = true;
    public boolean crowsClaimPlayerNests = true;
    public double spawnMultiplier = 1.0D;
    public double crowNestGenerationMultiplier = 1.0D;
    public double droppingFrequencyMultiplier = 1.0D;
    public double soundVolumeMultiplier = 1.0D;
    public int maxBirdsNearby = 24;
    public int maxGroundDroppingsNearby = 8;
    public int crowNestSearchDistance = 96;
    public int maxCrowNestTreasures = 6;

    public BirdGlobalConfig copy() {
        BirdGlobalConfig copy = new BirdGlobalConfig();
        copy.naturalSpawning = this.naturalSpawning;
        copy.colonialMode = this.colonialMode;
        copy.naturalCrowNests = this.naturalCrowNests;
        copy.crowsStoreTreasures = this.crowsStoreTreasures;
        copy.crowsClaimPlayerNests = this.crowsClaimPlayerNests;
        copy.spawnMultiplier = this.spawnMultiplier;
        copy.crowNestGenerationMultiplier = this.crowNestGenerationMultiplier;
        copy.droppingFrequencyMultiplier = this.droppingFrequencyMultiplier;
        copy.soundVolumeMultiplier = this.soundVolumeMultiplier;
        copy.maxBirdsNearby = this.maxBirdsNearby;
        copy.maxGroundDroppingsNearby = this.maxGroundDroppingsNearby;
        copy.crowNestSearchDistance = this.crowNestSearchDistance;
        copy.maxCrowNestTreasures = this.maxCrowNestTreasures;
        return copy;
    }
}
