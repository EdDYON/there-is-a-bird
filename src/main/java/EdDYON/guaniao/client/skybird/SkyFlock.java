package EdDYON.guaniao.client.skybird;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SkyFlock {
    private static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D);

    private final SkyBirdSpecies species;
    private final long seed;
    private final List<SkyBird> birds;
    private final int lifetime;
    private final double cruiseAltitude;
    private final double speed;
    private final double baseHeading;
    private final double curvePhase;
    private final MotionMode motionMode;
    private final Vec3 orbitCenter;
    private final double orbitRadius;
    private final double orbitAngularSpeed;

    private Vec3 previousCenter;
    private Vec3 center;
    private Vec3 previousVelocity;
    private Vec3 velocity;
    private double heading;
    private double targetAltitude;
    private double orbitAngle;
    private float previousRoll;
    private float roll;
    private int age;
    private int retirementStartAge = -1;
    private int retirementEndAge = Integer.MAX_VALUE;

    public SkyFlock(SkyBirdSpecies species, long seed, Vec3 center, Vec3 direction,
                    int birdCount, double speed, int lifetime, double cruiseAltitude) {
        this(species, seed, center, direction, birdCount, speed, lifetime, cruiseAltitude,
                MotionMode.CROSSING, Vec3.ZERO, 0.0D, 0.0D, 0.0D);
    }

    private SkyFlock(SkyBirdSpecies species, long seed, Vec3 center, Vec3 direction,
                     int birdCount, double speed, int lifetime, double cruiseAltitude,
                     MotionMode motionMode, Vec3 orbitCenter, double orbitRadius,
                     double orbitAngularSpeed, double orbitAngle) {
        this.species = species;
        this.seed = seed;
        this.center = center;
        this.previousCenter = center;
        this.speed = speed;
        this.lifetime = lifetime;
        this.cruiseAltitude = cruiseAltitude;
        this.targetAltitude = cruiseAltitude;
        this.motionMode = motionMode;
        this.orbitCenter = orbitCenter;
        this.orbitRadius = orbitRadius;
        this.orbitAngularSpeed = orbitAngularSpeed;
        this.orbitAngle = orbitAngle;

        Vec3 horizontalDirection = new Vec3(direction.x, 0.0D, direction.z).normalize();
        if (horizontalDirection.lengthSqr() < 1.0E-6D) {
            horizontalDirection = new Vec3(1.0D, 0.0D, 0.0D);
        }
        this.baseHeading = Math.atan2(horizontalDirection.z, horizontalDirection.x);
        this.heading = this.baseHeading;
        this.velocity = horizontalDirection.scale(speed);
        this.previousVelocity = this.velocity;

        RandomSource random = RandomSource.create(seed);
        this.curvePhase = random.nextDouble() * Mth.TWO_PI;
        this.birds = Collections.unmodifiableList(createFormation(species, birdCount, random));
    }

    public static SkyFlock orbiting(SkyBirdSpecies species, long seed, Vec3 orbitCenter,
                                    double radius, double startAngle, double speed,
                                    boolean clockwise, int lifetime) {
        double directionSign = clockwise ? -1.0D : 1.0D;
        Vec3 start = orbitCenter.add(
                Math.cos(startAngle) * radius,
                0.0D,
                Math.sin(startAngle) * radius
        );
        Vec3 tangent = new Vec3(
                -Math.sin(startAngle) * directionSign,
                0.0D,
                Math.cos(startAngle) * directionSign
        );
        double angularSpeed = directionSign * speed / radius;
        return new SkyFlock(
                species,
                seed,
                start,
                tangent,
                species.randomFlockSize(RandomSource.create(seed ^ 0x5EEDL)),
                speed,
                lifetime,
                orbitCenter.y,
                MotionMode.ORBIT,
                orbitCenter,
                radius,
                angularSpeed,
                startAngle
        );
    }

    public static SkyFlock coastal(SkyBirdSpecies species, long seed, Vec3 center, Vec3 direction,
                                   int birdCount, double speed, int lifetime, double cruiseAltitude) {
        return new SkyFlock(
                species,
                seed,
                center,
                direction,
                birdCount,
                speed,
                lifetime,
                cruiseAltitude,
                MotionMode.COASTAL_GLIDE,
                Vec3.ZERO,
                0.0D,
                0.0D,
                0.0D
        );
    }

    public static SkyFlock aerialDart(SkyBirdSpecies species, long seed, Vec3 center, Vec3 direction,
                                      int birdCount, double speed, int lifetime, double cruiseAltitude) {
        return new SkyFlock(
                species,
                seed,
                center,
                direction,
                birdCount,
                speed,
                lifetime,
                cruiseAltitude,
                MotionMode.AERIAL_DART,
                Vec3.ZERO,
                0.0D,
                0.0D,
                0.0D
        );
    }

    public static SkyFlock gracefulMigration(SkyBirdSpecies species, long seed, Vec3 center, Vec3 direction,
                                             int birdCount, double speed, int lifetime,
                                             double cruiseAltitude) {
        return new SkyFlock(
                species,
                seed,
                center,
                direction,
                birdCount,
                speed,
                lifetime,
                cruiseAltitude,
                MotionMode.GRACEFUL_MIGRATION,
                Vec3.ZERO,
                0.0D,
                0.0D,
                0.0D
        );
    }

    public static SkyFlock thermalSoaring(SkyBirdSpecies species, long seed, Vec3 orbitCenter,
                                          double radius, double startAngle, double speed,
                                          boolean clockwise, int birdCount, int lifetime) {
        double directionSign = clockwise ? -1.0D : 1.0D;
        Vec3 start = orbitCenter.add(
                Math.cos(startAngle) * radius,
                0.0D,
                Math.sin(startAngle) * radius
        );
        Vec3 tangent = new Vec3(
                -Math.sin(startAngle) * directionSign,
                0.0D,
                Math.cos(startAngle) * directionSign
        );
        return new SkyFlock(
                species,
                seed,
                start,
                tangent,
                birdCount,
                speed,
                lifetime,
                orbitCenter.y,
                MotionMode.THERMAL_SOAR,
                orbitCenter,
                radius,
                directionSign * speed / radius,
                startAngle
        );
    }

    public static SkyFlock murmuration(SkyBirdSpecies species, long seed, Vec3 center, Vec3 direction,
                                       int birdCount, double speed, int lifetime,
                                       double cruiseAltitude) {
        return new SkyFlock(
                species,
                seed,
                center,
                direction,
                birdCount,
                speed,
                lifetime,
                cruiseAltitude,
                MotionMode.MURMURATION,
                Vec3.ZERO,
                0.0D,
                0.0D,
                0.0D
        );
    }

    public void tick(ClientLevel level) {
        this.previousCenter = this.center;
        this.previousVelocity = this.velocity;
        this.previousRoll = this.roll;
        this.age++;

        if (this.motionMode == MotionMode.ORBIT) {
            tickOrbit();
            return;
        }
        if (this.motionMode == MotionMode.COASTAL_GLIDE) {
            tickCoastalGlide(level);
            return;
        }
        if (this.motionMode == MotionMode.AERIAL_DART) {
            tickAerialDart(level);
            return;
        }
        if (this.motionMode == MotionMode.GRACEFUL_MIGRATION) {
            tickGracefulMigration(level);
            return;
        }
        if (this.motionMode == MotionMode.THERMAL_SOAR) {
            tickThermalSoar();
            return;
        }
        if (this.motionMode == MotionMode.MURMURATION) {
            tickMurmuration(level);
            return;
        }

        tickCrossing(level);
    }

    private void tickCrossing(ClientLevel level) {

        double desiredHeading = this.baseHeading
                + Math.sin(this.curvePhase + this.age * 0.008D) * 0.09D;
        double headingDelta = wrapRadians(desiredHeading - this.heading);
        this.heading += Mth.clamp(headingDelta, -0.0014D, 0.0014D);

        Vec3 horizontalDirection = new Vec3(Math.cos(this.heading), 0.0D, Math.sin(this.heading));
        if ((this.age + (int)(this.seed & 15L)) % 20 == 0) {
            updateTerrainClearance(level, horizontalDirection);
        }

        double desiredVerticalSpeed = Mth.clamp(
                (this.targetAltitude - this.center.y) * 0.003D,
                -0.018D,
                0.026D
        );
        double verticalSpeed = Mth.lerp(0.06D, this.velocity.y, desiredVerticalSpeed);
        this.velocity = horizontalDirection.scale(this.speed).add(0.0D, verticalSpeed, 0.0D);
        this.center = this.center.add(this.velocity);

        Vec3 oldHorizontal = horizontal(this.previousVelocity);
        Vec3 newHorizontal = horizontal(this.velocity);
        double cross = oldHorizontal.x * newHorizontal.z - oldHorizontal.z * newHorizontal.x;
        double dot = oldHorizontal.x * newHorizontal.x + oldHorizontal.z * newHorizontal.z;
        float targetRoll = Mth.clamp((float)Math.toDegrees(Math.atan2(cross, dot)) * 115.0F,
                -18.0F, 18.0F);
        this.roll = Mth.lerp(0.10F, this.roll, targetRoll);
    }

    private void tickCoastalGlide(ClientLevel level) {
        double desiredHeading = this.baseHeading
                + Math.sin(this.curvePhase + this.age * 0.0065D) * 0.30D;
        double headingDelta = wrapRadians(desiredHeading - this.heading);
        this.heading += Mth.clamp(headingDelta, -0.0024D, 0.0024D);

        Vec3 horizontalDirection = new Vec3(Math.cos(this.heading), 0.0D, Math.sin(this.heading));
        if ((this.age + (int)(this.seed & 15L)) % 20 == 0) {
            updateTerrainClearance(level, horizontalDirection, 28.0D);
        }

        double altitudeWave = Math.sin(this.curvePhase * 1.7D + this.age * 0.016D) * 1.8D;
        double desiredVerticalSpeed = Mth.clamp(
                (this.targetAltitude + altitudeWave - this.center.y) * 0.004D,
                -0.024D,
                0.030D
        );
        double verticalSpeed = Mth.lerp(0.08D, this.velocity.y, desiredVerticalSpeed);
        this.velocity = horizontalDirection.scale(this.speed).add(0.0D, verticalSpeed, 0.0D);
        this.center = this.center.add(this.velocity);

        updateTurnRoll(95.0F, 20.0F, 0.12F);
    }

    private void tickAerialDart(ClientLevel level) {
        double desiredHeading = this.baseHeading
                + Math.sin(this.curvePhase + this.age * 0.014D) * 0.46D
                + Math.sin(this.curvePhase * 0.63D + this.age * 0.041D) * 0.12D;
        double headingDelta = wrapRadians(desiredHeading - this.heading);
        this.heading += Mth.clamp(headingDelta, -0.009D, 0.009D);

        Vec3 horizontalDirection = new Vec3(Math.cos(this.heading), 0.0D, Math.sin(this.heading));
        if ((this.age + (int)(this.seed & 15L)) % 16 == 0) {
            updateTerrainClearance(level, horizontalDirection, 24.0D);
        }

        double altitudeWave = Math.sin(this.curvePhase * 1.3D + this.age * 0.027D) * 2.5D
                + Math.sin(this.curvePhase * 0.5D + this.age * 0.061D) * 0.7D;
        double desiredVerticalSpeed = Mth.clamp(
                (this.targetAltitude + altitudeWave - this.center.y) * 0.007D,
                -0.050D,
                0.058D
        );
        double verticalSpeed = Mth.lerp(0.14D, this.velocity.y, desiredVerticalSpeed);
        this.velocity = horizontalDirection.scale(this.speed).add(0.0D, verticalSpeed, 0.0D);
        this.center = this.center.add(this.velocity);

        updateTurnRoll(82.0F, 30.0F, 0.18F);
    }

    private void tickGracefulMigration(ClientLevel level) {
        double desiredHeading = this.baseHeading
                + Math.sin(this.curvePhase + this.age * 0.0045D) * 0.12D;
        double headingDelta = wrapRadians(desiredHeading - this.heading);
        this.heading += Mth.clamp(headingDelta, -0.0011D, 0.0011D);

        Vec3 horizontalDirection = new Vec3(Math.cos(this.heading), 0.0D, Math.sin(this.heading));
        if ((this.age + (int)(this.seed & 15L)) % 24 == 0) {
            updateTerrainClearance(level, horizontalDirection, 38.0D);
        }

        double altitudeWave = Math.sin(this.curvePhase * 0.8D + this.age * 0.008D) * 1.4D;
        double desiredVerticalSpeed = Mth.clamp(
                (this.targetAltitude + altitudeWave - this.center.y) * 0.0035D,
                -0.020D,
                0.026D
        );
        double verticalSpeed = Mth.lerp(0.07D, this.velocity.y, desiredVerticalSpeed);
        this.velocity = horizontalDirection.scale(this.speed).add(0.0D, verticalSpeed, 0.0D);
        this.center = this.center.add(this.velocity);

        updateTurnRoll(78.0F, 12.0F, 0.08F);
    }

    private void tickThermalSoar() {
        this.orbitAngle += this.orbitAngularSpeed;
        double radiusDrift = (Math.sin(this.curvePhase + this.age * 0.004D)
                - Math.sin(this.curvePhase)) * 2.6D;
        double currentRadius = this.orbitRadius + radiusDrift;
        double windDistance = this.age * 0.006D;
        double windAngle = this.curvePhase * 0.73D;
        double verticalDrift = (Math.sin(this.curvePhase * 0.55D + this.age * 0.0035D)
                - Math.sin(this.curvePhase * 0.55D)) * 4.5D;
        Vec3 nextCenter = new Vec3(
                this.orbitCenter.x + Math.cos(windAngle) * windDistance
                        + Math.cos(this.orbitAngle) * currentRadius,
                this.orbitCenter.y + verticalDrift,
                this.orbitCenter.z + Math.sin(windAngle) * windDistance
                        + Math.sin(this.orbitAngle) * currentRadius
        );
        this.velocity = nextCenter.subtract(this.center);
        this.center = nextCenter;

        float targetRoll = this.orbitAngularSpeed >= 0.0D ? 11.0F : -11.0F;
        this.roll = Mth.lerp(0.055F, this.roll, targetRoll);
    }

    private void tickMurmuration(ClientLevel level) {
        double desiredHeading = this.baseHeading
                + Math.sin(this.curvePhase + this.age * 0.009D) * 0.31D
                + Math.sin(this.curvePhase * 0.52D + this.age * 0.026D) * 0.075D;
        double headingDelta = wrapRadians(desiredHeading - this.heading);
        this.heading += Mth.clamp(headingDelta, -0.006D, 0.006D);

        Vec3 horizontalDirection = new Vec3(Math.cos(this.heading), 0.0D, Math.sin(this.heading));
        if ((this.age + (int)(this.seed & 15L)) % 18 == 0) {
            updateTerrainClearance(level, horizontalDirection, 25.0D);
        }

        double altitudeWave = Math.sin(this.curvePhase * 1.15D + this.age * 0.019D) * 2.2D;
        double desiredVerticalSpeed = Mth.clamp(
                (this.targetAltitude + altitudeWave - this.center.y) * 0.006D,
                -0.042D,
                0.048D
        );
        double verticalSpeed = Mth.lerp(0.12D, this.velocity.y, desiredVerticalSpeed);
        this.velocity = horizontalDirection.scale(this.speed).add(0.0D, verticalSpeed, 0.0D);
        this.center = this.center.add(this.velocity);

        updateTurnRoll(88.0F, 26.0F, 0.16F);
    }

    private void tickOrbit() {
        this.orbitAngle += this.orbitAngularSpeed;
        double radiusDrift = (Math.sin(this.curvePhase + this.age * 0.005D)
                - Math.sin(this.curvePhase)) * 0.9D;
        double currentRadius = this.orbitRadius + radiusDrift;
        double windDistance = this.age * 0.0025D;
        double windAngle = this.curvePhase * 0.91D;
        double verticalDrift = (Math.sin(this.curvePhase + this.age * 0.009D)
                - Math.sin(this.curvePhase)) * 2.1D
                + (Math.sin(this.curvePhase * 0.57D + this.age * 0.0032D)
                - Math.sin(this.curvePhase * 0.57D)) * 0.9D;
        Vec3 nextCenter = new Vec3(
                this.orbitCenter.x + Math.cos(windAngle) * windDistance
                        + Math.cos(this.orbitAngle) * currentRadius,
                this.orbitCenter.y + verticalDrift,
                this.orbitCenter.z + Math.sin(windAngle) * windDistance
                        + Math.sin(this.orbitAngle) * currentRadius
        );
        this.velocity = nextCenter.subtract(this.center);
        this.center = nextCenter;

        float bank = 14.0F + (float)Math.sin(this.curvePhase + this.age * 0.006D) * 2.5F;
        float targetRoll = this.orbitAngularSpeed >= 0.0D ? bank : -bank;
        this.roll = Mth.lerp(0.08F, this.roll, targetRoll);
    }

    private void updateTerrainClearance(ClientLevel level, Vec3 direction) {
        updateTerrainClearance(level, direction, 46.0D);
    }

    private void updateTerrainClearance(ClientLevel level, Vec3 direction, double clearance) {
        int sampleX = Mth.floor(this.center.x + direction.x * 48.0D);
        int sampleZ = Mth.floor(this.center.z + direction.z * 48.0D);
        if (!level.hasChunk(sampleX >> 4, sampleZ >> 4)) {
            return;
        }

        // Use the heightmap synchronized to clients, including the canopy.
        int terrainY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, sampleX, sampleZ);
        this.targetAltitude = Math.max(this.cruiseAltitude, terrainY + clearance);
    }

    private static List<SkyBird> createFormation(SkyBirdSpecies species, int count, RandomSource random) {
        if (species == SkyBirdSpecies.STARLING) {
            return createMurmurationFlock(count, random);
        }
        if (species == SkyBirdSpecies.VULTURE) {
            return createVultureKettle(count, random);
        }
        if (species == SkyBirdSpecies.CRANE) {
            return createCraneFormation(count, random);
        }
        if (species == SkyBirdSpecies.SWALLOW) {
            return createStreamFlock(count, random);
        }
        if (species == SkyBirdSpecies.SEAGULL) {
            return createLooseFlock(count, random);
        }
        if (species != SkyBirdSpecies.GOOSE) {
            return createSolitaryFormation(count, random);
        }

        List<SkyBird> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            Vec3 offset;
            if (index == 0) {
                offset = Vec3.ZERO;
            } else {
                int row = (index + 1) / 2;
                boolean left = (index & 1) == 1;
                double side = (left ? -1.0D : 1.0D) * row * 2.3D;
                double behind = -row * 2.0D;
                offset = new Vec3(
                        side + randomBetween(random, -0.22D, 0.22D),
                        randomBetween(random, -0.42D, 0.42D),
                        behind + randomBetween(random, -0.18D, 0.18D)
                );
            }

            result.add(new SkyBird(
                    index,
                    offset,
                    random.nextFloat(),
                    0.92F + random.nextFloat() * 0.16F,
                    (random.nextFloat() - 0.5F) * 3.0F
            ));
        }
        return result;
    }

    private static List<SkyBird> createMurmurationFlock(int count, RandomSource random) {
        List<SkyBird> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(new SkyBird(
                    index,
                    Vec3.ZERO,
                    random.nextFloat(),
                    0.80F + random.nextFloat() * 0.28F,
                    (random.nextFloat() - 0.5F) * 10.0F
            ));
        }
        return result;
    }

    private static List<SkyBird> createVultureKettle(int count, RandomSource random) {
        List<SkyBird> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            Vec3 offset;
            if (index == 0) {
                offset = Vec3.ZERO;
            } else {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double distance = randomBetween(random, 2.6D, 5.5D);
                offset = new Vec3(
                        Math.cos(angle) * distance,
                        randomBetween(random, -2.4D, 2.4D),
                        Math.sin(angle) * distance
                );
            }
            result.add(new SkyBird(
                    index,
                    offset,
                    random.nextFloat(),
                    0.90F + random.nextFloat() * 0.18F,
                    (random.nextFloat() - 0.5F) * 4.0F
            ));
        }
        return result;
    }

    private static List<SkyBird> createCraneFormation(int count, RandomSource random) {
        List<SkyBird> result = new ArrayList<>(count);
        boolean shallowV = random.nextBoolean();
        double echelonDirection = random.nextBoolean() ? -1.0D : 1.0D;
        for (int index = 0; index < count; index++) {
            Vec3 offset;
            if (index == 0) {
                offset = Vec3.ZERO;
            } else if (shallowV) {
                int row = (index + 1) / 2;
                boolean left = (index & 1) == 1;
                offset = new Vec3(
                        (left ? -1.0D : 1.0D) * row * 3.15D,
                        randomBetween(random, -0.32D, 0.32D),
                        -row * 1.45D + randomBetween(random, -0.18D, 0.18D)
                );
            } else {
                offset = new Vec3(
                        echelonDirection * index * 2.65D,
                        randomBetween(random, -0.30D, 0.30D),
                        -index * 1.75D + randomBetween(random, -0.16D, 0.16D)
                );
            }
            result.add(new SkyBird(
                    index,
                    offset,
                    random.nextFloat(),
                    0.92F + random.nextFloat() * 0.16F,
                    (random.nextFloat() - 0.5F) * 4.0F
            ));
        }
        return result;
    }

    private static List<SkyBird> createStreamFlock(int count, RandomSource random) {
        List<SkyBird> result = new ArrayList<>(count);
        List<Vec3> occupied = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            Vec3 offset = Vec3.ZERO;
            if (index > 0) {
                for (int attempt = 0; attempt < 16; attempt++) {
                    Vec3 candidate = new Vec3(
                            randomBetween(random, -6.5D, 6.5D),
                            randomBetween(random, -1.4D, 1.4D),
                            randomBetween(random, -7.5D, 3.0D)
                    );
                    boolean separated = true;
                    for (Vec3 other : occupied) {
                        double dx = candidate.x - other.x;
                        double dz = candidate.z - other.z;
                        if (dx * dx + dz * dz < 1.8D) {
                            separated = false;
                            break;
                        }
                    }
                    if (separated || attempt == 15) {
                        offset = candidate;
                        break;
                    }
                }
            }
            occupied.add(offset);
            result.add(new SkyBird(
                    index,
                    offset,
                    random.nextFloat(),
                    0.82F + random.nextFloat() * 0.28F,
                    (random.nextFloat() - 0.5F) * 12.0F
            ));
        }
        return result;
    }

    private static List<SkyBird> createLooseFlock(int count, RandomSource random) {
        List<SkyBird> result = new ArrayList<>(count);
        List<Vec3> occupied = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            Vec3 offset = Vec3.ZERO;
            if (index > 0) {
                for (int attempt = 0; attempt < 16; attempt++) {
                    Vec3 candidate = new Vec3(
                            randomBetween(random, -5.2D, 5.2D),
                            randomBetween(random, -0.9D, 0.9D),
                            randomBetween(random, -4.2D, 4.2D)
                    );
                    boolean separated = true;
                    for (Vec3 other : occupied) {
                        double dx = candidate.x - other.x;
                        double dz = candidate.z - other.z;
                        if (dx * dx + dz * dz < 2.6D) {
                            separated = false;
                            break;
                        }
                    }
                    if (separated || attempt == 15) {
                        offset = candidate;
                        break;
                    }
                }
            }
            occupied.add(offset);
            result.add(new SkyBird(
                    index,
                    offset,
                    random.nextFloat(),
                    0.88F + random.nextFloat() * 0.22F,
                    (random.nextFloat() - 0.5F) * 7.0F
            ));
        }
        return result;
    }

    private static List<SkyBird> createSolitaryFormation(int count, RandomSource random) {
        List<SkyBird> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(new SkyBird(
                    index,
                    Vec3.ZERO,
                    random.nextFloat(),
                    0.94F + random.nextFloat() * 0.12F,
                    (random.nextFloat() - 0.5F) * 2.0F
            ));
        }
        return result;
    }

    public Vec3 renderCenter(float partialTick) {
        return this.previousCenter.lerp(this.center, partialTick);
    }

    public Vec3 renderVelocity(float partialTick) {
        Vec3 interpolated = this.previousVelocity.lerp(this.velocity, partialTick);
        return interpolated.lengthSqr() < 1.0E-6D ? this.velocity : interpolated;
    }

    public Vec3 birdPosition(SkyBird bird, float partialTick) {
        Vec3 forward = horizontal(this.renderVelocity(partialTick));
        if (forward.lengthSqr() < 1.0E-6D) {
            forward = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            forward = forward.normalize();
        }
        Vec3 right = WORLD_UP.cross(forward).normalize();
        Vec3 local = bird.formationOffset();
        if (this.species == SkyBirdSpecies.STARLING) {
            local = starlingLocalOffset(bird, this.age + partialTick);
        } else if (this.species == SkyBirdSpecies.GOOSE) {
            double phase = bird.wingPhase() * Mth.TWO_PI;
            double animationAge = this.age + partialTick;
            local = local.add(
                    Math.sin(phase + animationAge * 0.006D) * 0.10D,
                    Math.cos(phase + animationAge * 0.008D) * 0.12D,
                    Math.sin(phase * 0.7D + animationAge * 0.005D) * 0.08D
            );
        } else if (this.species == SkyBirdSpecies.SEAGULL) {
            double phase = bird.wingPhase() * Mth.TWO_PI;
            double animationAge = this.age + partialTick;
            local = local.add(
                    Math.sin(phase + animationAge * 0.013D) * 0.16D,
                    Math.cos(phase + animationAge * 0.018D) * 0.24D,
                    0.0D
            );
        } else if (this.species == SkyBirdSpecies.SWALLOW) {
            double phase = bird.wingPhase() * Mth.TWO_PI;
            double animationAge = this.age + partialTick;
            local = local.add(
                    Math.sin(phase + animationAge * 0.031D) * 0.42D,
                    Math.cos(phase * 1.3D + animationAge * 0.044D) * 0.34D,
                    Math.sin(phase * 0.7D + animationAge * 0.023D) * 0.28D
            );
        } else if (this.species == SkyBirdSpecies.CRANE) {
            double phase = bird.wingPhase() * Mth.TWO_PI;
            double animationAge = this.age + partialTick;
            local = local.add(
                    0.0D,
                    Math.sin(phase * 0.35D + animationAge * 0.009D) * 0.12D,
                    0.0D
            );
        } else if (this.species == SkyBirdSpecies.VULTURE) {
            local = vultureLocalOffset(bird, this.age + partialTick);
        }
        return this.renderCenter(partialTick)
                .add(right.scale(local.x))
                .add(forward.scale(local.z))
                .add(0.0D, local.y, 0.0D);
    }

    public Vec3 birdRenderVelocity(SkyBird bird, float partialTick) {
        Vec3 flockVelocity = this.renderVelocity(partialTick);
        Vec3 horizontalForward = horizontal(flockVelocity);
        if (horizontalForward.lengthSqr() < 1.0E-6D) {
            return flockVelocity;
        }
        horizontalForward = horizontalForward.normalize();
        Vec3 right = WORLD_UP.cross(horizontalForward).normalize();
        double animationAge = this.age + partialTick;
        double phase = bird.wingPhase() * Mth.TWO_PI;

        if (this.species == SkyBirdSpecies.SWALLOW) {
            return flockVelocity
                    .add(right.scale(Math.cos(phase + animationAge * 0.031D) * 0.052D))
                    .add(0.0D, -Math.sin(phase * 1.3D + animationAge * 0.044D) * 0.015D, 0.0D);
        }
        if (this.species == SkyBirdSpecies.STARLING) {
            Vec3 currentLocal = starlingLocalOffset(bird, animationAge);
            Vec3 nextLocal = starlingLocalOffset(bird, animationAge + 1.0D);
            Vec3 localVelocity = nextLocal.subtract(currentLocal);
            return flockVelocity
                    .add(right.scale(localVelocity.x))
                    .add(horizontalForward.scale(localVelocity.z))
                    .add(0.0D, localVelocity.y, 0.0D);
        }
        if (this.species == SkyBirdSpecies.SEAGULL) {
            return flockVelocity.add(right.scale(
                    Math.cos(phase + animationAge * 0.013D) * 0.010D
            ));
        }
        if (this.species == SkyBirdSpecies.CRANE) {
            return flockVelocity.add(right.scale(
                    Math.cos(phase * 0.35D + animationAge * 0.009D) * 0.004D
            ));
        }
        if (this.species == SkyBirdSpecies.VULTURE) {
            Vec3 currentLocal = vultureLocalOffset(bird, animationAge);
            Vec3 nextLocal = vultureLocalOffset(bird, animationAge + 1.0D);
            Vec3 localVelocity = nextLocal.subtract(currentLocal);
            return flockVelocity
                    .add(right.scale(localVelocity.x))
                    .add(horizontalForward.scale(localVelocity.z))
                    .add(0.0D, localVelocity.y, 0.0D);
        }
        return flockVelocity;
    }

    private Vec3 vultureLocalOffset(SkyBird bird, double animationAge) {
        double phase = bird.wingPhase() * Mth.TWO_PI;
        double radius = 1.2D + bird.wingPhase() * 1.0D;
        double angularSpeed = 0.0025D + bird.wingPhase() * 0.0012D;
        double angle = phase + animationAge * angularSpeed;
        return bird.formationOffset().add(
                Math.cos(angle) * radius,
                Math.sin(angle * 0.7D + phase * 0.4D) * 0.50D,
                Math.sin(angle) * radius
        );
    }

    public float renderRoll(SkyBird bird, float partialTick) {
        return Mth.lerp(partialTick, this.previousRoll, this.roll) + bird.rollOffset();
    }

    public void retire(int fadeTicks) {
        if (this.retirementStartAge >= 0) {
            return;
        }
        this.retirementStartAge = this.age;
        this.retirementEndAge = Math.min(this.lifetime, this.age + Math.max(1, fadeTicks));
    }

    public boolean isRetiring() {
        return this.retirementStartAge >= 0;
    }

    public float lifecycleAlpha(float partialTick) {
        float renderAge = this.age + partialTick;
        float fadeIn = Mth.clamp(renderAge / 12.0F, 0.0F, 1.0F);
        int endAge = effectiveEndAge();
        float fadeOut;
        if (this.retirementStartAge >= 0) {
            int duration = Math.max(1, this.retirementEndAge - this.retirementStartAge);
            fadeOut = Mth.clamp((endAge - renderAge) / duration, 0.0F, 1.0F);
        } else {
            fadeOut = Mth.clamp((endAge - renderAge) / 30.0F, 0.0F, 1.0F);
        }
        return Math.min(fadeIn, fadeOut);
    }

    public boolean isExpired() {
        return this.age >= effectiveEndAge();
    }

    private int effectiveEndAge() {
        return Math.min(this.lifetime, this.retirementEndAge);
    }

    public SkyBirdSpecies species() {
        return this.species;
    }

    public long seed() {
        return this.seed;
    }

    public List<SkyBird> birds() {
        return this.birds;
    }

    public int age() {
        return this.age;
    }

    private static Vec3 horizontal(Vec3 vector) {
        return new Vec3(vector.x, 0.0D, vector.z);
    }

    private static double wrapRadians(double value) {
        return Math.atan2(Math.sin(value), Math.cos(value));
    }

    private static double randomBetween(RandomSource random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private Vec3 starlingLocalOffset(SkyBird bird, double animationAge) {
        int count = Math.max(1, this.birds.size());
        double normalized = (bird.index() + 0.5D) / count;
        double phase = bird.index() * 2.399963229728653D + bird.wingPhase() * 0.72D;

        double cloudSide = Math.cos(phase + animationAge * 0.018D) * 4.4D
                + Math.cos(phase * 2.3D - animationAge * 0.011D) * 1.0D;
        double cloudHeight = Math.sin(phase * 1.7D + animationAge * 0.021D) * 2.25D;
        double cloudForward = Math.sin(phase + animationAge * 0.018D) * 3.3D;

        double ribbonSide = (normalized - 0.5D) * 14.0D;
        double ribbonHeight = Math.sin(normalized * Math.PI * 4.0D
                + animationAge * 0.025D) * 1.35D;
        double ribbonForward = Math.cos(normalized * Math.PI * 3.0D
                + animationAge * 0.017D) * 1.85D;

        double morph = 0.5D + 0.5D * Math.sin(this.curvePhase * 0.9D + animationAge * 0.006D);
        morph = morph * morph * (3.0D - 2.0D * morph);
        double side = cloudSide + (ribbonSide - cloudSide) * morph;
        double height = cloudHeight + (ribbonHeight - cloudHeight) * morph;
        double forward = cloudForward + (ribbonForward - cloudForward) * morph;

        double splitSignal = Math.sin(this.curvePhase * 0.41D + animationAge * 0.0048D);
        double split = Mth.clamp((splitSignal - 0.62D) / 0.38D, 0.0D, 1.0D);
        split = split * split * (3.0D - 2.0D * split);
        double groupSide = bird.index() < (count + 1) / 2 ? -1.0D : 1.0D;
        side += groupSide * split * 4.0D;
        forward += groupSide * split
                * Math.sin(phase * 0.55D + animationAge * 0.012D) * 0.85D;

        side += Math.sin(phase * 0.7D + animationAge * 0.031D) * 0.45D;
        height += Math.cos(phase * 1.2D + animationAge * 0.027D) * 0.28D;
        return new Vec3(side, height, forward);
    }

    private void updateTurnRoll(float multiplier, float maxRoll, float blend) {
        Vec3 oldHorizontal = horizontal(this.previousVelocity);
        Vec3 newHorizontal = horizontal(this.velocity);
        double cross = oldHorizontal.x * newHorizontal.z - oldHorizontal.z * newHorizontal.x;
        double dot = oldHorizontal.x * newHorizontal.x + oldHorizontal.z * newHorizontal.z;
        float targetRoll = Mth.clamp((float)Math.toDegrees(Math.atan2(cross, dot)) * multiplier,
                -maxRoll, maxRoll);
        this.roll = Mth.lerp(blend, this.roll, targetRoll);
    }

    private enum MotionMode {
        CROSSING,
        ORBIT,
        COASTAL_GLIDE,
        AERIAL_DART,
        GRACEFUL_MIGRATION,
        THERMAL_SOAR,
        MURMURATION
    }
}
