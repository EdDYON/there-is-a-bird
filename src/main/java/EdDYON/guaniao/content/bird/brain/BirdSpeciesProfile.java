package EdDYON.guaniao.content.bird.brain;

import EdDYON.guaniao.config.BirdConfigManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;

public abstract class BirdSpeciesProfile {
    public double playerSenseRadius() {
        return 18.0D;
    }

    public float baseBoldness() {
        return 0.35F;
    }

    public float baseWariness() {
        return 0.65F;
    }

    public float baseActivity() {
        return 0.5F;
    }

    public float baseSociability() {
        return 0.45F;
    }

    public float baseFlightiness() {
        return 0.55F;
    }

    public float hungerGainPerTick(BirdSenses senses) {
        return senses.activeTime() ? 0.00018F : 0.00008F;
    }

    public float flightFatigueGainPerTick() {
        return 0.0015F;
    }

    public float restFatigueRecoveryPerTick(BirdSenses senses) {
        return senses.nearRoost() || senses.roostTime() ? 0.0022F : 0.0011F;
    }

    public float fearRiseRate() {
        return 0.08F;
    }

    public float fearFallRate() {
        return 0.018F;
    }

    public float computeComfort(BirdSenses senses) {
        float comfort = 0.35F;
        if (senses.nearWater()) {
            comfort += 0.18F;
        }
        if (senses.nearCover()) {
            comfort += 0.18F;
        }
        if (senses.nearRoost()) {
            comfort += 0.2F;
        }
        if (senses.hasNearbyThreat()) {
            comfort -= 0.22F;
        }
        return this.clamp(comfort);
    }

    public float computeRisk(BirdBrain brain) {
        BirdSenses senses = brain.senses();
        BirdMotivation motivation = brain.motivation();
        BirdPersonality personality = brain.personality();
        float risk = 0.0F;

        if (!BirdConfigManager.aprilFoolsMode() && senses.nearestPlayer() != null) {
            double radius = Math.max(1.0D, this.playerSenseRadius());
            float closeness = (float)(1.0D - Mth.clamp(senses.nearestPlayerDistance() / radius, 0.0D, 1.0D));
            risk += closeness * 0.62F;
            if (senses.nearestPlayerSprinting()) {
                risk += 0.18F;
            }
            if (senses.temptingPlayerNearby()) {
                risk -= 0.16F;
            }
        }

        if (senses.nearCover()) {
            risk -= 0.08F;
        }

        risk += personality.wariness() * 0.22F;
        risk += personality.flightiness() * 0.18F;
        risk -= personality.boldness() * 0.22F;
        risk -= motivation.hunger() * 0.08F;
        risk += motivation.fear() * 0.24F;
        return this.clamp(risk);
    }

    public boolean wantsForage(BirdBrain brain) {
        return brain.senses().activeTime()
                && brain.motivation().hunger() > 0.45F
                && brain.computeRiskScore() < 0.55F;
    }

    public boolean wantsRoost(BirdBrain brain) {
        return brain.senses().roostTime()
                && (brain.motivation().roostNeed() > 0.35F || brain.motivation().fatigue() > 0.55F)
                && brain.computeRiskScore() < 0.7F;
    }

    public boolean wantsShortEscape(BirdBrain brain) {
        float risk = brain.computeRiskScore();
        return risk >= 0.58F && risk < 0.78F;
    }

    public boolean wantsLongEscape(BirdBrain brain) {
        return brain.computeRiskScore() >= 0.78F;
    }

    public abstract boolean isActiveTime(BirdSenses senses);

    public abstract boolean isRoostTime(BirdSenses senses);

    public abstract boolean isPreferredPrey(LivingEntity entity);

    public boolean isTemptingPlayer(Player player) {
        return false;
    }

    public boolean isTemptingPlayer(PathfinderMob bird, Player player) {
        return this.isTemptingPlayer(player);
    }

    public LivingEntity findNearestPrey(PathfinderMob bird) {
        return null;
    }

    public boolean isNearWater(PathfinderMob bird) {
        return false;
    }

    public boolean isWaterEdge(PathfinderMob bird) {
        return false;
    }

    public boolean isNearCover(PathfinderMob bird) {
        return false;
    }

    public boolean isNearRoost(PathfinderMob bird) {
        return false;
    }

    public int habitatScanCost() {
        return 0;
    }

    public BirdHabitatSnapshot scanHabitat(PathfinderMob bird) {
        return new BirdHabitatSnapshot(
                this.isNearWater(bird),
                this.isWaterEdge(bird),
                this.isNearCover(bird),
                this.isNearRoost(bird));
    }

    protected float clamp(float value) {
        return Mth.clamp(value, 0.0F, 1.0F);
    }
}
