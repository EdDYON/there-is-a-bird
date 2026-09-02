package EdDYON.guaniao.content.bird;

import EdDYON.guaniao.config.BirdSpecies;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Gives every ground bird a stable individual walking speed and a slowly
 * changing pace. Goal speed modifiers remain the behavior layer, so forage,
 * follow, chase and escape logic keep their existing tuning.
 */
public final class BirdGroundLocomotion {
    private static final String TAG_INDIVIDUAL_FACTOR = "GuaniaoIndividualWalkFactor";
    private static final UUID GROUND_SPEED_MODIFIER_ID =
            UUID.fromString("5d7e8914-9a44-4af0-b6fc-1de640e178a1");
    private static final String GROUND_SPEED_MODIFIER_NAME = "Guaniao ground locomotion";
    private static final float PACE_LERP = 0.08F;
    private static final Map<Mob, State> STATES = new WeakHashMap<>();

    private BirdGroundLocomotion() {
    }

    public static void tickBird(Mob bird, BirdSpecies species) {
        AttributeInstance movementSpeed = bird.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }
        if (!bird.isAlive() || bird.isRemoved()) {
            removeModifier(movementSpeed);
            STATES.remove(bird);
            return;
        }
        if (!bird.onGround() || bird.isPassenger() || bird.isInWaterOrBubble()) {
            removeModifier(movementSpeed);
            State state = STATES.get(bird);
            if (state != null) {
                state.appliedMultiplier = Double.NaN;
            }
            return;
        }

        LocomotionProfile profile = profile(species);
        State state = STATES.computeIfAbsent(bird, ignored -> State.create(bird, profile));
        if (--state.paceTicks <= 0) {
            GroundPace nextPace = choosePace(bird.getRandom(), profile);
            state.targetPace = nextPace.multiplier;
            state.paceTicks = randomBetween(
                    bird.getRandom(), profile.minimumPaceTicks, profile.maximumPaceTicks);
        }
        state.currentPace = Mth.lerp(PACE_LERP, state.currentPace, state.targetPace);

        // The navigation speed chosen by the active Goal is the behavior layer.
        // Do not let a leisurely pace undermine an urgent chase or escape.
        float effectivePace = state.currentPace;
        double behaviorSpeed = bird.getNavigation().isDone()
                ? 0.0D : bird.getMoveControl().getSpeedModifier();
        if (bird.hurtTime > 0 || behaviorSpeed >= 1.35D) {
            effectivePace = Math.max(effectivePace, 1.18F);
        } else if (behaviorSpeed >= 1.12D) {
            effectivePace = Math.max(effectivePace, 1.05F);
        }

        double multiplier = profile.speciesFactor * state.individualFactor * effectivePace;
        applyModifier(movementSpeed, multiplier, state);
    }

    private static void applyModifier(AttributeInstance attribute, double multiplier, State state) {
        if (Math.abs(multiplier - state.appliedMultiplier) < 0.0025D) {
            return;
        }
        removeModifier(attribute);
        attribute.addTransientModifier(new AttributeModifier(
                GROUND_SPEED_MODIFIER_ID,
                GROUND_SPEED_MODIFIER_NAME,
                multiplier - 1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        state.appliedMultiplier = multiplier;
    }

    private static void removeModifier(AttributeInstance attribute) {
        attribute.removeModifier(GROUND_SPEED_MODIFIER_ID);
    }

    private static GroundPace choosePace(RandomSource random, LocomotionProfile profile) {
        int roll = random.nextInt(profile.totalWeight());
        if (roll < profile.slowWeight) {
            return GroundPace.SLOW;
        }
        if (roll < profile.slowWeight + profile.normalWeight) {
            return GroundPace.NORMAL;
        }
        return GroundPace.BRISK;
    }

    private static int randomBetween(RandomSource random, int minimum, int maximum) {
        return minimum + random.nextInt(Math.max(1, maximum - minimum + 1));
    }

    private static LocomotionProfile profile(BirdSpecies species) {
        return switch (species) {
            case NIGHT_HERON -> new LocomotionProfile(0.72F, 0.08F, 55, 40, 5, 80, 220);
            case SPARROW -> new LocomotionProfile(1.10F, 0.10F, 25, 55, 20, 30, 100);
            case LONG_TAILED_TIT -> new LocomotionProfile(1.05F, 0.10F, 25, 55, 20, 30, 90);
            case COCKATIEL -> new LocomotionProfile(0.85F, 0.10F, 30, 55, 15, 50, 140);
            case MACAW -> new LocomotionProfile(0.68F, 0.08F, 45, 50, 5, 80, 180);
            case BUDGERIGAR -> new LocomotionProfile(0.95F, 0.10F, 25, 55, 20, 35, 100);
            case SPOTTED_DOVE -> new LocomotionProfile(0.88F, 0.08F, 35, 55, 10, 60, 160);
            case PIGEON -> new LocomotionProfile(1.00F, 0.10F, 15, 50, 35, 35, 110);
            case CROW -> new LocomotionProfile(0.95F, 0.10F, 22, 53, 25, 25, 90);
            case SEAGULL -> new LocomotionProfile(0.82F, 0.08F, 40, 50, 10, 60, 160);
            case KIWI -> new LocomotionProfile(0.90F, 0.10F, 35, 55, 10, 50, 150);
            case MYNA -> new LocomotionProfile(1.05F, 0.10F, 20, 45, 35, 20, 70);
        };
    }

    private enum GroundPace {
        SLOW(0.65F),
        NORMAL(1.00F),
        BRISK(1.28F);

        private final float multiplier;

        GroundPace(float multiplier) {
            this.multiplier = multiplier;
        }
    }

    private record LocomotionProfile(
            float speciesFactor,
            float individualVariance,
            int slowWeight,
            int normalWeight,
            int briskWeight,
            int minimumPaceTicks,
            int maximumPaceTicks) {

        private int totalWeight() {
            return this.slowWeight + this.normalWeight + this.briskWeight;
        }
    }

    private static final class State {
        private final float individualFactor;
        private float currentPace;
        private float targetPace;
        private int paceTicks;
        private double appliedMultiplier = Double.NaN;

        private State(float individualFactor, float currentPace, int paceTicks) {
            this.individualFactor = individualFactor;
            this.currentPace = currentPace;
            this.targetPace = currentPace;
            this.paceTicks = paceTicks;
        }

        private static State create(Mob bird, LocomotionProfile profile) {
            CompoundTag data = bird.getPersistentData();
            float individualFactor;
            if (data.contains(TAG_INDIVIDUAL_FACTOR, 99)) {
                individualFactor = Mth.clamp(
                        data.getFloat(TAG_INDIVIDUAL_FACTOR),
                        1.0F - profile.individualVariance,
                        1.0F + profile.individualVariance);
            } else {
                individualFactor = 1.0F
                        + (bird.getRandom().nextFloat() * 2.0F - 1.0F) * profile.individualVariance;
                data.putFloat(TAG_INDIVIDUAL_FACTOR, individualFactor);
            }
            GroundPace initialPace = choosePace(bird.getRandom(), profile);
            return new State(
                    individualFactor,
                    initialPace.multiplier,
                    randomBetween(bird.getRandom(), profile.minimumPaceTicks, profile.maximumPaceTicks));
        }
    }
}
