package EdDYON.guaniao.content.bird.brain;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.content.bird.command.CommandableBird;
import EdDYON.guaniao.content.bird.flight.BirdFlightController;
import EdDYON.guaniao.content.bird.flight.BirdFlightTargeting;
import java.util.EnumSet;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Drives long-distance migration flight when the bird's brain selects MIGRATE intent.
 *
 * Picks a target living inside a compatible <species>_habitat biome at migrationRadius and
 * holds the real {@link #finalTarget} and steers through {@link #steeringTarget} across many ticks, steering toward it each tick
 * (with stall recovery) until arrival, the schedule deadline, or an escape/high-risk interrupt.
 *
 * The brain is supplied lazily: this goal is registered from the entity's {@code registerGoals()},
 * which the Mob constructor calls before the entity's own brain field is initialized, so capturing
 * the brain eagerly would freeze it at null.
 */
public class BirdMigrationGoal extends Goal {
    private static final double FLIGHT_SPEED = 0.30D;
    private static final double MIN_VERTICAL = -0.05D;
    private static final double MAX_VERTICAL = 0.35D;
    private static final int ARRIVAL_DISTANCE_SQUARED = 36;
    private static final long TIMEOUT_TICKS = 12000L;

    private final PathfinderMob bird;
    private final Supplier<BirdBrain> brain;
    @Nullable
    private Vec3 finalTarget;
    @Nullable
    private Vec3 steeringTarget;
    private long migrationDeadlineTick;

    public BirdMigrationGoal(PathfinderMob bird, Supplier<BirdBrain> brain) {
        this.bird = bird;
        this.brain = brain;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!BirdConfigManager.migrationEnabled() || !(this.bird.level() instanceof ServerLevel) || this.isControlled()) {
            return false;
        }
        BirdBrain brain = this.brain.get();
        return brain != null && brain.currentIntent() == BirdIntent.MIGRATE;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.finalTarget == null || this.isControlled()) {
            return false;
        }
        if (!(this.bird.level() instanceof ServerLevel level)) {
            return false;
        }
        if (level.getGameTime() >= this.migrationDeadlineTick) {
            return false;
        }
        BirdBrain brain = this.brain.get();
        if (brain == null) {
            return false;
        }
        BirdIntent intent = brain.currentIntent();
        if (intent == BirdIntent.LONG_FLIGHT || intent == BirdIntent.SHORT_FLIGHT) {
            return false;
        }
        return brain.senses().roostTime() && brain.computeRiskScore() < 0.8F;
    }

    @Override
    public void start() {
        if (!(this.bird.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        Vec3 habitat = this.pickMigrationTarget(level);
        if (habitat == null) {
            // Stay put: no random fallback, so an already-comfortable habitat or an
            // unreachable target will not launch the bird on a pointless flight.
            return;
        }
        habitat = this.clampToWorldHeight(level, habitat);
        this.finalTarget = habitat;

        Vec3 steer = habitat;
        if (!BirdFlightTargeting.hasClearFlightPath(this.bird, habitat)) {
            Vec3 recovery = BirdFlightTargeting.findRecoveryTarget(this.bird, habitat.subtract(this.bird.position()), 10, 14);
            if (recovery == null) {
                this.finalTarget = null;
                return;
            }
            steer = recovery;
        }
        this.steeringTarget = steer;
        this.migrationDeadlineTick = now + TIMEOUT_TICKS;
        this.bird.getNavigation().stop();
        this.bird.setNoGravity(true);
        this.bird.setOnGround(false);
        this.bird.hasImpulse = true;
        this.bird.fallDistance = 0.0F;
    }

    @Override
    public void tick() {
        Vec3 finalTarget = this.finalTarget;
        Vec3 steeringTarget = this.steeringTarget;
        if (finalTarget == null || steeringTarget == null) {
            this.stop();
            return;
        }
        if (!finalTarget.equals(steeringTarget) && this.bird.position().distanceToSqr(steeringTarget) < ARRIVAL_DISTANCE_SQUARED) {
            this.steeringTarget = finalTarget;
            steeringTarget = finalTarget;
        }
        Vec3 toTarget = steeringTarget.subtract(this.bird.position());
        if (steeringTarget.equals(finalTarget) && toTarget.lengthSqr() < ARRIVAL_DISTANCE_SQUARED) {
            this.stop();
            return;
        }
        if (BirdFlightController.isFlightProgressStalled(this.bird, steeringTarget, 20, 20)) {
            Vec3 recovery = BirdFlightTargeting.findRecoveryTarget(this.bird, toTarget, 10, 14);
            if (recovery == null) {
                this.stop();
                return;
            }
            this.steeringTarget = recovery;
            steeringTarget = recovery;
            toTarget = steeringTarget.subtract(this.bird.position());
        }
        Vec3 desired = BirdFlightController.steerToward(this.bird, steeringTarget, FLIGHT_SPEED, MIN_VERTICAL, MAX_VERTICAL);
        Vec3 movement = BirdFlightController.blendMovement(this.bird.getDeltaMovement(), desired, 0.68D);
        this.bird.setDeltaMovement(movement);
        this.bird.setNoGravity(true);
        this.bird.setOnGround(false);
        this.bird.hasImpulse = true;
        this.bird.fallDistance = 0.0F;
        BirdFlightController.faceMovement(this.bird, movement, 30.0F);
    }

    @Override
    public void stop() {
        this.finalTarget = null;
        this.steeringTarget = null;
        this.migrationDeadlineTick = 0L;
        BirdFlightController.clearFlightProgress(this.bird);
        this.bird.setNoGravity(false);
        this.bird.getNavigation().stop();
        Vec3 movement = this.bird.getDeltaMovement();
        this.bird.setDeltaMovement(movement.x * 0.35D, Math.min(movement.y, -0.04D), movement.z * 0.35D);
    }

    private boolean isControlled() {
        if (this.bird instanceof TamableAnimal tamable && tamable.isTame()) {
            return true;
        }
        return this.bird instanceof CommandableBird commandable && commandable.getBirdCommandMode() != BirdCommandMode.FREE;
    }

    /**
     * Returns the exact target coordinates (chunk center) inside a compatible <species>_habitat biome
     * within the migration search radius, or null when none is found so the bird stays put.
     */
    @Nullable
    private Vec3 pickMigrationTarget(ServerLevel level) {
        BirdSpecies species = BirdSpecies.from(this.bird);
        if (species == null) {
            return null;
        }
        TagKey<Biome> habitatTag = TagKey.create(
                Registries.BIOME,
                new ResourceLocation("guaniao", species.id() + "_habitat")
        );
        int regionSize = Math.max(32, BirdConfigManager.migrationRadius());
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = this.bird.getRandom().nextDouble() * 2.0D * Math.PI;
            int distance = regionSize + this.bird.getRandom().nextInt(Math.max(1, regionSize / 2));
            int x = this.bird.getBlockX() + (int)(Math.cos(angle) * distance);
            int z = this.bird.getBlockZ() + (int)(Math.sin(angle) * distance);
            BlockPos probe = new BlockPos(x, this.bird.getBlockY(), z);
            if (level.hasChunk(x >> 4, z >> 4) && level.getBiome(probe).is(habitatTag)) {
                return new Vec3(x + 0.5D, this.bird.getY(), z + 0.5D);
            }
        }
        // No farther habitat found, or the bird is already in suitable habitat: return null so
        // start() stays put instead of flinging the bird in a random direction (#10).
        return null;
    }

    private Vec3 clampToWorldHeight(ServerLevel level, Vec3 migrationTarget) {
        int x = (int)Math.floor(migrationTarget.x);
        int z = (int)Math.floor(migrationTarget.z);
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return migrationTarget;
        }
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new Vec3(migrationTarget.x, surfaceY + 8, migrationTarget.z);
    }
}
