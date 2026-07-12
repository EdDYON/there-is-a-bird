package EdDYON.guaniao.content.bird.nightheron;

import EdDYON.guaniao.content.bird.flight.BirdFlightBoids;
import EdDYON.guaniao.content.bird.flight.BirdFlightController;
import EdDYON.guaniao.content.bird.nightheron.NightHeronBehaviorState;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class NightHeronFlightController {
    private NightHeronFlightController() {
    }

    public static void takeOff(NightHeronEntity nightHeron, Vec3 direction, double horizontalSpeed, double verticalBoost) {
        double safeVerticalBoost;
        Vec3 safeDirection = NightHeronFlightController.chooseOpenDirection(nightHeron, NightHeronFlightController.normalizeHorizontal(direction, nightHeron.getLookAngle()), 4.5);
        nightHeron.markTakeoffFlapping();
        nightHeron.setBehaviorState(NightHeronBehaviorState.TAKEOFF);
        double d = safeVerticalBoost = NightHeronFlightController.hasVerticalClearance(nightHeron, 2.8) ? verticalBoost : Math.min(verticalBoost, 0.32);
        if (!NightHeronFlightController.isAirPathClear(nightHeron, safeDirection, 2.2, 0.25) && NightHeronFlightController.hasVerticalClearance(nightHeron, 4.0)) {
            safeVerticalBoost += 0.12;
        }
        Vec3 movement = nightHeron.getDeltaMovement().scale(0.35).add(safeDirection.scale(horizontalSpeed)).add(0.0, safeVerticalBoost, 0.0);
        NightHeronFlightController.applyMovement(nightHeron, movement);
    }

    public static void tickLowEscapeFlight(NightHeronEntity nightHeron, Vec3 direction, double speed, double targetHeight, double maxHeight) {
        NightHeronFlightController.tickDirectedFlight(nightHeron, direction, Math.max(speed, 0.42), targetHeight, maxHeight, -0.045, false, NightHeronBehaviorState.LOW_FLAP_ESCAPE);
    }

    public static void tickLocalFlight(NightHeronEntity nightHeron, Vec3 direction) {
        NightHeronFlightController.tickDirectedFlight(nightHeron, direction, 0.36, 7.0, 13.0, -0.045, false, NightHeronBehaviorState.LOCAL_FLIGHT);
    }

    public static void tickLongEscapeFlight(NightHeronEntity nightHeron, Vec3 direction, double speed, double targetHeight, double maxHeight) {
        NightHeronFlightController.tickDirectedFlight(nightHeron, direction, speed, targetHeight, maxHeight, -0.025, true, NightHeronBehaviorState.LONG_FLIGHT_ESCAPE);
    }

    public static void tickHighTransitFlight(NightHeronEntity nightHeron, Vec3 direction) {
        NightHeronFlightController.tickDirectedFlight(nightHeron, direction, 0.42, 18.0, 28.0, -0.025, true, NightHeronBehaviorState.HIGH_TRANSIT);
    }

    public static void tickSoaringFlight(NightHeronEntity nightHeron, Vec3 direction) {
        NightHeronFlightController.tickDirectedFlight(nightHeron, direction, 0.40, 23.0, 36.0, -0.025, true, NightHeronBehaviorState.SOARING);
    }

    public static boolean tickLanding(NightHeronEntity nightHeron, BlockPos landingTarget) {
        return NightHeronFlightController.tickLandingApproach(nightHeron, landingTarget);
    }

    public static boolean shouldBeginLandingApproach(NightHeronEntity nightHeron, BlockPos landingTarget, int remainingTicks, double approachDistance) {
        if (landingTarget == null) {
            return false;
        }
        return remainingTicks <= 60 || nightHeron.distanceToSqr(Vec3.atCenterOf((Vec3i)landingTarget)) <= approachDistance * approachDistance;
    }

    public static boolean tickLandingApproach(NightHeronEntity nightHeron, BlockPos landingTarget) {
        double glidePathHeight;
        Vec3 toTarget = Vec3.atCenterOf((Vec3i)landingTarget).subtract(nightHeron.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
        double horizontalDistance = horizontal.length();
        if (nightHeron.onGround()) {
            nightHeron.finishFlight(NightHeronBehaviorState.IDLE);
            return true;
        }
        nightHeron.setBehaviorState(NightHeronBehaviorState.LANDING);
        Vec3 targetDirection = horizontal.lengthSqr() <= 1.0E-4 ? nightHeron.getLookAngle() : horizontal.normalize();
        double height = nightHeron.heightAboveSurface();
        if (horizontalDistance <= 1.15) {
            double flareSpeed;
            Vec3 flareDirection = nightHeron.updateLandingApproachDirection(landingTarget, nightHeron.getDeltaMovement(), 0.0);
            double d = flareSpeed = height > 0.55 ? 0.22 : 0.099;
            double verticalSpeed = height > 1.65 ? -0.052 : (height > 0.65 ? -0.034 : -0.018);
            Vec3 desired = flareDirection.scale(flareSpeed).add(0.0, verticalSpeed, 0.0);
            Vec3 movement = nightHeron.getDeltaMovement().scale(0.35).add(desired.scale(0.65));
            NightHeronFlightController.applyMovement(nightHeron, movement);
            return false;
        }
        Vec3 approachDirection = nightHeron.updateLandingApproachDirection(landingTarget, targetDirection, horizontalDistance > 8.0 ? 0.18 : 0.08);
        boolean blockedApproach = !NightHeronFlightController.isAirPathClear(nightHeron, approachDirection, 3.0, 0.1);
        boolean stuckInPlace = nightHeron.tickFlightObstructionProbe(blockedApproach);
        if (blockedApproach || stuckInPlace) {
            Vec3 openDirection = NightHeronFlightController.chooseOpenDirection(nightHeron, approachDirection, 3.0);
            if (NightHeronFlightController.airPathScore(nightHeron, openDirection, 3.0, 0.1) < 1.25 || stuckInPlace) {
                NightHeronFlightController.tickBlockedRecovery(nightHeron, approachDirection);
                return false;
            }
            approachDirection = nightHeron.updateLandingApproachDirection(landingTarget, openDirection, 0.22);
        }
        double verticalSpeed = height > (glidePathHeight = Mth.clamp((double)(horizontalDistance * 0.18 + 0.55), (double)0.85, (double)7.0)) ? -Mth.clamp((double)((height - glidePathHeight) * 0.038), (double)0.025, (double)0.11) : (height < glidePathHeight - 1.4 && horizontalDistance > 7.0 ? 0.028 : (height > 1.3 ? -0.026 : -0.012));
        double speed = horizontalDistance > 7.0 ? 0.28 : 0.24;
        speed = BirdFlightController.decelerateNearLanding(speed, horizontalDistance, 5.0D, 0.46D);
        Vec3 desired = approachDirection.scale(speed).add(0.0, verticalSpeed, 0.0);
        Vec3 movement = nightHeron.getDeltaMovement().scale(0.58).add(desired.scale(0.42));
        NightHeronFlightController.applyMovement(nightHeron, movement);
        return false;
    }

    public static void tickOpenLanding(NightHeronEntity nightHeron, Vec3 direction) {
        if (nightHeron.onGround()) {
            nightHeron.finishFlight(NightHeronBehaviorState.IDLE);
            return;
        }
        nightHeron.setBehaviorState(NightHeronBehaviorState.LANDING);
        Vec3 safeDirection = NightHeronFlightController.chooseOpenDirection(nightHeron, NightHeronFlightController.normalizeHorizontal(direction, nightHeron.getLookAngle()), 3.5);
        double descent = nightHeron.heightAboveSurface() > 1.5 ? -0.12 : -0.04;
        Vec3 desired = safeDirection.scale(0.24).add(0.0, descent, 0.0);
        Vec3 movement = nightHeron.getDeltaMovement().scale(0.62).add(desired.scale(0.38));
        NightHeronFlightController.applyMovement(nightHeron, movement);
    }

    public static void tickBlockedRecovery(NightHeronEntity nightHeron, Vec3 preferredDirection) {
        double recoveryScore;
        if (nightHeron.onGround()) {
            nightHeron.finishFlight(NightHeronBehaviorState.IDLE);
            return;
        }
        nightHeron.clearLandingApproach();
        nightHeron.markBlockedFlightRecovery();
        nightHeron.setBehaviorState(NightHeronBehaviorState.LANDING);
        Vec3 recoveryDirection = nightHeron.getBlockedFlightRecoveryDirection();
        if (recoveryDirection.lengthSqr() <= 1.0E-4) {
            recoveryDirection = NightHeronFlightController.chooseRecoveryDirection(nightHeron, preferredDirection, 3.4);
            nightHeron.lockBlockedFlightRecoveryDirection(recoveryDirection, 28);
        }
        double horizontalSpeed = (recoveryScore = NightHeronFlightController.airPathScore(nightHeron, recoveryDirection, 3.2, 0.1)) >= 0.55 ? 0.18 : 0.08;
        double height = nightHeron.heightAboveSurface();
        double descent = height > 3.0 ? -0.105 : (height > 1.1 ? -0.075 : -0.035);
        Vec3 desired = recoveryDirection.scale(horizontalSpeed).add(0.0, descent, 0.0);
        Vec3 movement = nightHeron.getDeltaMovement().scale(0.25).add(desired.scale(0.75));
        NightHeronFlightController.applyMovement(nightHeron, movement);
    }

    public static boolean shouldGlide(NightHeronEntity nightHeron) {
        Vec3 movement = nightHeron.getDeltaMovement();
        return nightHeron.heightAboveSurface() >= 8.5 && movement.horizontalDistance() >= 0.15 && movement.y <= 0.09;
    }

    private static void tickDirectedFlight(NightHeronEntity nightHeron, Vec3 direction, double speed, double targetHeight, double maxHeight, double cruiseDescent, boolean allowGlide, NightHeronBehaviorState defaultFlightState) {
        double lift;
        Vec3 requestedDirection = NightHeronFlightController.normalizeHorizontal(direction, nightHeron.getLookAngle());
        if (defaultFlightState != NightHeronBehaviorState.LOCAL_FLIGHT) {
            Vec3 flockHeading = BirdFlightBoids.sameTypeHeading(nightHeron, 26.0D, 5.0D, 0.018D, 0.34D, 0.06D, defaultFlightState.isEscape() ? 0.14D : 0.06D);
            if (flockHeading.lengthSqr() > 1.0E-4) {
                requestedDirection = NightHeronFlightController.normalizeHorizontal(requestedDirection.add(flockHeading), requestedDirection);
            }
        }
        Vec3 safeDirection = NightHeronFlightController.chooseOpenDirection(nightHeron, requestedDirection, defaultFlightState == NightHeronBehaviorState.LOCAL_FLIGHT ? 4.0 : 6.0);
        double height = nightHeron.heightAboveSurface();
        Vec3 currentMovement = nightHeron.getDeltaMovement();
        double clearScore = NightHeronFlightController.airPathScore(nightHeron, safeDirection, 3.2, 0.25);
        boolean blockedAhead = clearScore < 1.25;
        boolean hasClimbRoom = NightHeronFlightController.hasVerticalClearance(nightHeron, 3.6) && height < maxHeight - 1.0;
        boolean stuckInPlace = nightHeron.tickFlightObstructionProbe(blockedAhead);
        boolean cruiseBand = height >= targetHeight - 0.8 && height <= maxHeight - 0.75;
        boolean glideWindow = allowGlide && cruiseBand && clearScore >= 2.2 && !blockedAhead;
        if (nightHeron.horizontalCollision || blockedAhead || stuckInPlace) {
            if (stuckInPlace || !hasClimbRoom || clearScore < 0.75) {
                NightHeronFlightController.tickBlockedRecovery(nightHeron, safeDirection);
                return;
            }
            lift = 0.19;
            nightHeron.setBehaviorState(NightHeronBehaviorState.CLIMB);
        } else if (height < targetHeight) {
            lift = height < targetHeight - 5.0 ? 0.24 : 0.15;
            nightHeron.setBehaviorState(NightHeronBehaviorState.CLIMB);
        } else if (height > maxHeight) {
            lift = cruiseDescent;
            nightHeron.setBehaviorState(allowGlide ? NightHeronBehaviorState.GLIDE : defaultFlightState);
        } else if (glideWindow) {
            lift = Math.max(cruiseDescent - 0.008, -0.055);
            nightHeron.setBehaviorState(NightHeronBehaviorState.GLIDE);
        } else {
            lift = allowGlide ? Math.max(cruiseDescent, -0.035) : 0.11;
            nightHeron.setBehaviorState(allowGlide && NightHeronFlightController.shouldGlide(nightHeron) ? NightHeronBehaviorState.GLIDE : defaultFlightState);
        }
        boolean fastFlap = NightHeronFlightController.shouldFastFlap(nightHeron, height, targetHeight, maxHeight, allowGlide);
        double flightSpeed = speed;
        if (fastFlap && height < maxHeight - 0.45) {
            flightSpeed *= 1.22;
            lift = Math.max(lift, 0.08) + 0.09;
            if (allowGlide || height < targetHeight) {
                nightHeron.setBehaviorState(NightHeronBehaviorState.CLIMB);
            }
        } else if (glideWindow) {
            flightSpeed *= 1.05;
            lift = Math.min(lift, -0.012);
        }
        Vec3 desired = safeDirection.scale(flightSpeed).add(0.0, lift, 0.0);
        Vec3 movement = glideWindow && !fastFlap ? currentMovement.scale(0.78).add(desired.scale(0.22)) : currentMovement.scale(0.62).add(desired.scale(0.38));
        NightHeronFlightController.applyMovement(nightHeron, movement);
    }

    private static boolean shouldFastFlap(NightHeronEntity nightHeron, double height, double targetHeight, double maxHeight, boolean allowGlide) {
        if (nightHeron.isTakeoffFlapping()) {
            return true;
        }
        if (height < targetHeight - 1.2) {
            return true;
        }
        if (height >= maxHeight - 0.75) {
            return false;
        }
        if (allowGlide && nightHeron.getDeltaMovement().horizontalDistance() < 0.19) {
            return true;
        }
        int ticks = Math.max(0, nightHeron.getControlledFlightTicks());
        int phase = Math.floorMod(ticks + nightHeron.getId() * 7, allowGlide ? 56 : 42);
        if (!allowGlide) {
            return phase < 18;
        }
        if (height < targetHeight + 1.5) {
            return phase < 18;
        }
        return phase < 12;
    }

    private static Vec3 normalizeHorizontal(Vec3 direction, Vec3 fallback) {
        Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
        if (horizontal.lengthSqr() <= 1.0E-4) {
            horizontal = new Vec3(fallback.x, 0.0, fallback.z);
        }
        if (horizontal.lengthSqr() <= 1.0E-4) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        return horizontal.normalize();
    }

    private static Vec3 chooseOpenDirection(NightHeronEntity nightHeron, Vec3 preferred, double lookAhead) {
        double[] angles;
        Vec3 baseDirection;
        Vec3 bestDirection = baseDirection = NightHeronFlightController.normalizeHorizontal(preferred, nightHeron.getLookAngle());
        double bestScore = NightHeronFlightController.airPathScore(nightHeron, baseDirection, lookAhead, 0.25);
        for (double angle : angles = new double[]{0.32, -0.32, 0.68, -0.68, 1.05, -1.05, 1.55, -1.55}) {
            Vec3 candidate = NightHeronFlightController.rotateHorizontal(baseDirection, angle);
            double score = NightHeronFlightController.airPathScore(nightHeron, candidate, lookAhead, 0.25);
            if (!(score > bestScore)) continue;
            bestScore = score;
            bestDirection = candidate;
        }
        return bestDirection;
    }

    private static Vec3 chooseRecoveryDirection(NightHeronEntity nightHeron, Vec3 preferred, double lookAhead) {
        double[] angles;
        Vec3 baseDirection;
        Vec3 bestDirection = baseDirection = NightHeronFlightController.normalizeHorizontal(preferred.scale(-1.0), nightHeron.getDeltaMovement().scale(-1.0));
        double bestScore = NightHeronFlightController.airPathScore(nightHeron, baseDirection, lookAhead, 0.1);
        for (double angle : angles = new double[]{0.45, -0.45, 0.95, -0.95, 1.45, -1.45, 2.15, -2.15}) {
            Vec3 candidate = NightHeronFlightController.rotateHorizontal(baseDirection, angle);
            double score = NightHeronFlightController.airPathScore(nightHeron, candidate, lookAhead, 0.1);
            if (!(score > bestScore)) continue;
            bestScore = score;
            bestDirection = candidate;
        }
        return bestDirection;
    }

    private static boolean isAirPathClear(NightHeronEntity nightHeron, Vec3 direction, double distance, double verticalOffset) {
        return NightHeronFlightController.airPathScore(nightHeron, direction, distance, verticalOffset) >= distance;
    }

    private static double airPathScore(NightHeronEntity nightHeron, Vec3 direction, double distance, double verticalOffset) {
        double sampleDistance;
        Vec3 offset;
        Vec3 safeDirection = NightHeronFlightController.normalizeHorizontal(direction, nightHeron.getLookAngle());
        double score = 0.0;
        int samples = Math.max(2, Mth.ceil((double)distance));
        for (int step = 1; step <= samples && NightHeronFlightController.isAirSpaceClear(nightHeron, offset = safeDirection.scale(sampleDistance = distance * (double)step / (double)samples).add(0.0, verticalOffset, 0.0)); ++step) {
            score += distance / (double)samples;
        }
        return score;
    }

    private static boolean hasVerticalClearance(NightHeronEntity nightHeron, double height) {
        for (double yOffset = 0.45; yOffset <= height; yOffset += 0.75) {
            if (NightHeronFlightController.isAirSpaceClear(nightHeron, new Vec3(0.0, yOffset, 0.0))) continue;
            return false;
        }
        return true;
    }

    private static boolean isAirSpaceClear(NightHeronEntity nightHeron, Vec3 offset) {
        AABB box;
        Level level = nightHeron.level();
        if (!NightHeronFlightController.canReadBox(level, box = nightHeron.getBoundingBox().move(offset).inflate(-0.06, -0.02, -0.06))) {
            return false;
        }
        return level.noCollision((Entity)nightHeron, box) && !NightHeronFlightController.containsBlockedFluid(level, box);
    }

    private static boolean canReadBox(Level level, AABB box) {
        int minX = Mth.floor((double)box.minX);
        int maxX = Mth.floor((double)box.maxX);
        int minZ = Mth.floor((double)box.minZ);
        int maxZ = Mth.floor((double)box.maxZ);
        return NightHeronEntity.canReadChunk((LevelReader)level, new BlockPos(minX, Mth.floor((double)box.minY), minZ)) && NightHeronEntity.canReadChunk((LevelReader)level, new BlockPos(minX, Mth.floor((double)box.minY), maxZ)) && NightHeronEntity.canReadChunk((LevelReader)level, new BlockPos(maxX, Mth.floor((double)box.minY), minZ)) && NightHeronEntity.canReadChunk((LevelReader)level, new BlockPos(maxX, Mth.floor((double)box.minY), maxZ));
    }

    private static boolean containsBlockedFluid(Level level, AABB box) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int minX = Mth.floor((double)box.minX);
        int maxX = Mth.floor((double)box.maxX);
        int minY = Mth.floor((double)box.minY);
        int maxY = Mth.floor((double)box.maxY);
        int minZ = Mth.floor((double)box.minZ);
        int maxZ = Mth.floor((double)box.maxZ);
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    mutablePos.set(x, y, z);
                    if (!level.getFluidState((BlockPos)mutablePos).is(FluidTags.WATER) && !level.getFluidState((BlockPos)mutablePos).is(FluidTags.LAVA)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private static Vec3 rotateHorizontal(Vec3 direction, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(direction.x * cos - direction.z * sin, 0.0, direction.x * sin + direction.z * cos).normalize();
    }

    private static void applyMovement(NightHeronEntity nightHeron, Vec3 movement) {
        nightHeron.setDeltaMovement(movement);
        nightHeron.hasImpulse = true;
        nightHeron.fallDistance = 0.0f;
        nightHeron.faceMovementDirection(movement);
    }
}
