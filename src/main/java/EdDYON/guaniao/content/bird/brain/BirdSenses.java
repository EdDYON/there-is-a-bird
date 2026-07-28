package EdDYON.guaniao.content.bird.brain;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class BirdSenses {
    private Player nearestPlayer;
    private double nearestPlayerDistance = Double.MAX_VALUE;
    private boolean nearestPlayerSprinting;
    private boolean temptingPlayerNearby;
    private LivingEntity nearestPrey;
    private double nearestPreyDistance = Double.MAX_VALUE;
    private boolean nearWater;
    private boolean waterEdge;
    private boolean nearCover;
    private boolean nearRoost;
    private boolean airborne;
    private boolean onGround;
    private boolean activeTime;
    private boolean roostTime;
    private long dayTime;
    private BlockPos lastKnownWaterEdge;
    private BlockPos lastKnownRoost;
    private long nextPlayerScanTick;
    private long nextPreyScanTick;

    public void tick(BirdBrain brain) {
        PathfinderMob bird = brain.bird();
        BirdSpeciesProfile profile = brain.profile();

        this.dayTime = bird.level().getDayTime() % 24000L;
        this.onGround = bird.onGround();
        this.airborne = !this.onGround;

        BirdSpecies species = BirdSpecies.from(bird);
        long now = bird.level().getGameTime();
        int threatInterval = species == null ? 20 : BirdConfigManager.threatScanInterval(species);
        int foodInterval = species == null ? 20 : BirdConfigManager.foodScanInterval(species);

        if (now >= this.nextPlayerScanTick) {
            this.nextPlayerScanTick = now + threatInterval;
            this.nearestPlayer = bird.level().getNearestPlayer(bird, profile.playerSenseRadius());
            if (this.nearestPlayer != null && !this.nearestPlayer.isSpectator()) {
                this.nearestPlayerDistance = Math.sqrt(bird.distanceToSqr(this.nearestPlayer));
                this.nearestPlayerSprinting = this.nearestPlayer.isSprinting();
                this.temptingPlayerNearby = profile.isTemptingPlayer(bird, this.nearestPlayer);
            } else {
                this.nearestPlayer = null;
                this.nearestPlayerDistance = Double.MAX_VALUE;
                this.nearestPlayerSprinting = false;
                this.temptingPlayerNearby = false;
            }
        }

        if (now >= this.nextPreyScanTick) {
            this.nextPreyScanTick = now + foodInterval;
            this.nearestPrey = profile.findNearestPrey(bird);
            this.nearestPreyDistance = this.nearestPrey == null ? Double.MAX_VALUE : Math.sqrt(bird.distanceToSqr(this.nearestPrey));
        }

        BirdHabitatSnapshot habitat = BirdHabitatCache.sample(bird, profile);
        if (habitat != null) {
            this.nearWater = habitat.nearWater();
            this.waterEdge = habitat.waterEdge();
            this.nearCover = habitat.nearCover();
            this.nearRoost = habitat.nearRoost();
        }
        this.activeTime = profile.isActiveTime(this);
        this.roostTime = profile.isRoostTime(this);

        if (this.waterEdge) {
            this.lastKnownWaterEdge = bird.blockPosition();
        }
        if (this.nearRoost) {
            this.lastKnownRoost = bird.blockPosition();
        }
    }

    public Player nearestPlayer() {
        return this.nearestPlayer;
    }

    public double nearestPlayerDistance() {
        return this.nearestPlayerDistance;
    }

    public boolean nearestPlayerSprinting() {
        return this.nearestPlayerSprinting;
    }

    public boolean temptingPlayerNearby() {
        return this.temptingPlayerNearby;
    }

    public LivingEntity nearestPrey() {
        return this.nearestPrey;
    }

    public double nearestPreyDistance() {
        return this.nearestPreyDistance;
    }

    public boolean nearWater() {
        return this.nearWater;
    }

    public boolean waterEdge() {
        return this.waterEdge;
    }

    public boolean nearCover() {
        return this.nearCover;
    }

    public boolean nearRoost() {
        return this.nearRoost;
    }

    public boolean isAirborne() {
        return this.airborne;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean activeTime() {
        return this.activeTime;
    }

    public boolean roostTime() {
        return this.roostTime;
    }

    public long dayTime() {
        return this.dayTime;
    }

    public BlockPos lastKnownWaterEdge() {
        return this.lastKnownWaterEdge;
    }

    public BlockPos lastKnownRoost() {
        return this.lastKnownRoost;
    }

    public boolean hasNearbyThreat() {
        return !BirdConfigManager.aprilFoolsMode()
                && this.nearestPlayer != null
                && this.nearestPlayerDistance < 16.0D;
    }
}
