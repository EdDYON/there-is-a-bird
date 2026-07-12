package EdDYON.guaniao.content.bird.brain;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.nightheron.NightHeronDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;

public class BirdBrain {
    private final PathfinderMob bird;
    private final BirdSpeciesProfile profile;
    private final BirdSenses senses = new BirdSenses();
    private final BirdMotivation motivation = new BirdMotivation();
    private BirdPersonality personality;
    private int senseCooldown;
    private float cachedRiskScore;
    private BirdIntent currentIntent = BirdIntent.IDLE;

    public BirdBrain(PathfinderMob bird, BirdSpeciesProfile profile) {
        this.bird = bird;
        this.profile = profile;
        this.personality = BirdPersonality.create(bird.getRandom(), profile);
    }

    public void tick() {
        if (this.bird.level().isClientSide) {
            return;
        }

        if (this.senseCooldown-- <= 0) {
            this.senses.tick(this);
            this.senseCooldown = 5 + this.bird.getRandom().nextInt(6);
        }

        this.cachedRiskScore = this.profile.computeRisk(this);
        this.motivation.tick(this);
        this.cachedRiskScore = this.profile.computeRisk(this);
        this.currentIntent = this.chooseIntent();
        if (NightHeronDefinition.DEBUG_BRAIN && this.bird.tickCount % NightHeronDefinition.DEBUG_BRAIN_LOG_INTERVAL_TICKS == 0) {
            this.logDebugState();
        }
    }

    public float computeRiskScore() {
        this.cachedRiskScore = this.profile.computeRisk(this);
        return this.cachedRiskScore;
    }

    public boolean wantsForage() {
        return this.profile.wantsForage(this);
    }

    public boolean wantsRoost() {
        return this.profile.wantsRoost(this);
    }

    public boolean wantsAlert() {
        return this.cachedRiskScore >= 0.45F || this.motivation.alertness() > 0.45F;
    }

    public boolean wantsShortEscape() {
        return this.profile.wantsShortEscape(this);
    }

    public boolean wantsLongEscape() {
        return this.profile.wantsLongEscape(this);
    }

    public void onEat(float amount) {
        this.motivation.onEat(amount);
    }

    public void onFrightened(float amount) {
        this.motivation.onFrightened(amount);
    }

    public void onRest(float amount) {
        this.motivation.onRest(amount);
    }

    public void save(CompoundTag tag) {
        CompoundTag brainTag = new CompoundTag();
        CompoundTag motivationTag = new CompoundTag();
        CompoundTag personalityTag = new CompoundTag();
        this.motivation.save(motivationTag);
        this.personality.save(personalityTag);
        brainTag.put("Motivation", motivationTag);
        brainTag.put("Personality", personalityTag);
        tag.put("BirdBrain", brainTag);
    }

    public void load(CompoundTag tag) {
        if (!tag.contains("BirdBrain", 10)) {
            this.personality = BirdPersonality.create(this.bird.getRandom(), this.profile);
            return;
        }
        CompoundTag brainTag = tag.getCompound("BirdBrain");
        if (brainTag.contains("Motivation", 10)) {
            this.motivation.load(brainTag.getCompound("Motivation"));
        }
        this.personality = BirdPersonality.load(brainTag.getCompound("Personality"), this.bird.getRandom(), this.profile);
    }

    public PathfinderMob bird() {
        return this.bird;
    }

    public BirdSpeciesProfile profile() {
        return this.profile;
    }

    public BirdSenses senses() {
        return this.senses;
    }

    public BirdMotivation motivation() {
        return this.motivation;
    }

    public BirdPersonality personality() {
        return this.personality;
    }

    public BirdIntent currentIntent() {
        return this.currentIntent;
    }

    private BirdIntent chooseIntent() {
        if (this.wantsLongEscape()) {
            return BirdIntent.LONG_FLIGHT;
        }
        if (this.wantsShortEscape()) {
            return BirdIntent.SHORT_FLIGHT;
        }
        if (this.cachedRiskScore >= 0.45F) {
            return BirdIntent.ALERT;
        }
        if (this.wantsRoost()) {
            return BirdIntent.ROOST;
        }
        if (this.wantsForage()) {
            return BirdIntent.FORAGE;
        }
        if (this.motivation.alertness() > 0.45F) {
            return BirdIntent.WATCH;
        }
        return BirdIntent.IDLE;
    }

    private void logDebugState() {
        double playerDistance = this.senses.nearestPlayer() == null ? -1.0D : this.senses.nearestPlayerDistance();
        GuaniaoMod.LOGGER.info(
                "BirdBrain id={} type={} intent={} risk={} hunger={} fear={} fatigue={} comfort={} alert={} roost={} day={} active={} roostTime={} water={} edge={} cover={} roostNear={} playerDist={}",
                this.bird.getId(),
                BuiltInRegistries.ENTITY_TYPE.getKey(this.bird.getType()),
                this.currentIntent,
                this.cachedRiskScore,
                this.motivation.hunger(),
                this.motivation.fear(),
                this.motivation.fatigue(),
                this.motivation.comfort(),
                this.motivation.alertness(),
                this.motivation.roostNeed(),
                this.senses.dayTime(),
                this.senses.activeTime(),
                this.senses.roostTime(),
                this.senses.nearWater(),
                this.senses.waterEdge(),
                this.senses.nearCover(),
                this.senses.nearRoost(),
                playerDistance
        );
    }
}
