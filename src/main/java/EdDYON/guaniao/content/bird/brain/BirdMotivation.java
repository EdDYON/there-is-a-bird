package EdDYON.guaniao.content.bird.brain;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public class BirdMotivation {
    private float hunger;
    private float fear;
    private float fatigue;
    private float comfort = 0.5F;
    private float alertness;
    private float socialStress;
    private float roostNeed;

    public void tick(BirdBrain brain) {
        BirdSenses senses = brain.senses();
        BirdSpeciesProfile profile = brain.profile();

        this.hunger = this.clamp(this.hunger + profile.hungerGainPerTick(senses));
        if (senses.isAirborne()) {
            this.fatigue = this.clamp(this.fatigue + profile.flightFatigueGainPerTick());
        } else if (senses.isOnGround() || senses.roostTime() || senses.nearRoost()) {
            this.fatigue = this.clamp(this.fatigue - profile.restFatigueRecoveryPerTick(senses));
        }

        float risk = brain.computeRiskScore();
        this.fear = this.approach(this.fear, risk, risk > this.fear ? profile.fearRiseRate() : profile.fearFallRate());
        this.comfort = this.approach(this.comfort, profile.computeComfort(senses), 0.015F);
        this.alertness = this.clamp(this.fear * 0.72F + (senses.hasNearbyThreat() ? 0.28F : 0.0F));
        this.roostNeed = this.clamp(this.roostNeed + (senses.roostTime() ? 0.0018F : -0.0012F));
        this.socialStress = this.clamp(this.socialStress * 0.985F);
    }

    public void onEat(float amount) {
        this.hunger = this.clamp(this.hunger - amount);
        this.comfort = this.clamp(this.comfort + amount * 0.35F);
    }

    public void onFrightened(float amount) {
        this.fear = this.clamp(this.fear + amount);
        this.alertness = this.clamp(this.alertness + amount * 0.8F);
    }

    public void onRest(float amount) {
        this.fatigue = this.clamp(this.fatigue - amount);
        this.comfort = this.clamp(this.comfort + amount * 0.25F);
    }

    public void save(CompoundTag tag) {
        tag.putFloat("Hunger", this.hunger);
        tag.putFloat("Fear", this.fear);
        tag.putFloat("Fatigue", this.fatigue);
        tag.putFloat("Comfort", this.comfort);
        tag.putFloat("Alertness", this.alertness);
        tag.putFloat("SocialStress", this.socialStress);
        tag.putFloat("RoostNeed", this.roostNeed);
    }

    public void load(CompoundTag tag) {
        this.hunger = this.readFloat(tag, "Hunger", this.hunger);
        this.fear = this.readFloat(tag, "Fear", this.fear);
        this.fatigue = this.readFloat(tag, "Fatigue", this.fatigue);
        this.comfort = this.readFloat(tag, "Comfort", this.comfort);
        this.alertness = this.readFloat(tag, "Alertness", this.alertness);
        this.socialStress = this.readFloat(tag, "SocialStress", this.socialStress);
        this.roostNeed = this.readFloat(tag, "RoostNeed", this.roostNeed);
    }

    public float hunger() {
        return this.hunger;
    }

    public float fear() {
        return this.fear;
    }

    public float fatigue() {
        return this.fatigue;
    }

    public float comfort() {
        return this.comfort;
    }

    public float alertness() {
        return this.alertness;
    }

    public float socialStress() {
        return this.socialStress;
    }

    public float roostNeed() {
        return this.roostNeed;
    }

    private float readFloat(CompoundTag tag, String key, float fallback) {
        return tag.contains(key, 99) ? this.clamp(tag.getFloat(key)) : fallback;
    }

    private float approach(float value, float target, float step) {
        return this.clamp(Mth.approach(value, this.clamp(target), Math.max(0.0F, step)));
    }

    private float clamp(float value) {
        return Mth.clamp(value, 0.0F, 1.0F);
    }
}
