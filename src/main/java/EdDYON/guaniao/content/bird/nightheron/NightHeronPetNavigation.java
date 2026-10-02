package EdDYON.guaniao.content.bird.nightheron;

import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.kestrel.KestrelFlightPaths;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Ground companionship alternates with short flights; a stopped owner can interact with the pet. */
final class NightHeronPetNavigation {
    private static final int OWNER_STOP_TICKS = 20;
    private static final int GROUND_FOLLOW_TICKS = 200;
    private static final int AIR_FOLLOW_TICKS = 120;
    private final NightHeronEntity bird;
    private int repath;
    private boolean following;
    private Vec3 lastOwnerPosition;
    private final int orbitDirection;
    private boolean flying;
    private int flightTicks;
    private BlockPos landing;
    private boolean approaching;
    private int ownerStillTicks;
    private int groundFollowTicks;
    private int airFollowTicks;

    NightHeronPetNavigation(NightHeronEntity bird) {
        this.bird = bird;
        this.orbitDirection = bird.getRandom().nextBoolean() ? 1 : -1;
    }

    boolean isFlying() { return this.flying || this.following; }

    void tick() {
        if (this.repath > 0) --this.repath;
        if (!this.flying) return;
        ++this.flightTicks;
        if (this.bird.onGround() && this.flightTicks > 2) {
            this.bird.finishFlight(NightHeronBehaviorState.IDLE);
            this.finishLanding();
        } else if (this.flightTicks > 320 || this.landing == null || !NightHeronFishing.safeStand(this.bird, this.landing)) {
            this.stop();
            this.repath = 80;
        } else if (this.approaching && this.bird.getY() >= this.landing.getY() - 0.1 || this.bird.getY() >= this.landing.getY() + 0.6
                && (this.flightTicks > 100 || this.bird.distanceToSqr(Vec3.atBottomCenterOf(this.landing)) < 64)) {
            this.approaching = true;
            if (NightHeronFlightController.tickPetLanding(this.bird, this.landing)) this.finishLanding();
        } else {
            this.approaching = false;
            NightHeronFlightController.tickLocalFlight(this.bird, NightHeronLandingSelector.directionTo(this.landing, this.bird));
        }
    }

    void follow(Player owner) {
        Vec3 ownerVelocity = this.lastOwnerPosition == null ? Vec3.ZERO : owner.position().subtract(this.lastOwnerPosition);
        this.lastOwnerPosition = owner.position();
        this.ownerStillTicks = ownerVelocity.horizontalDistanceSqr() <= 0.0004 && Math.abs(ownerVelocity.y) <= 0.08
                ? Math.min(OWNER_STOP_TICKS, this.ownerStillTicks + 1) : 0;
        boolean stopped = this.ownerStillTicks >= OWNER_STOP_TICKS;
        if (ownerVelocity.lengthSqr() > 2.25) ownerVelocity = Vec3.ZERO;
        // Continue an existing descent. Resetting it here each tick used to keep FOLLOW airborne forever.
        if (this.flying) {
            if (this.landing == null || owner.distanceToSqr(Vec3.atBottomCenterOf(this.landing)) <= 144) return;
            this.startFollowingFlight();
        }
        if (!this.following && !this.bird.onGround() && this.bird.getBehaviorState().isAirborne())
            this.startFollowingFlight();
        if (!this.following) {
            if (stopped) this.groundFollowTicks = 0;
            else ++this.groundFollowTicks;
            double distance = this.bird.distanceToSqr(owner);
            boolean needsFlight = distance > 144 || Math.abs(this.bird.getY() - owner.getY()) > 2
                    || !stopped && this.groundFollowTicks >= GROUND_FOLLOW_TICKS;
            if (needsFlight && NightHeronFlightController.canLaunchPet(this.bird)) {
                this.startFollowingFlight();
            } else {
                if (distance <= 6.25 && Math.abs(this.bird.getY() - owner.getY()) <= 1.25
                        && this.bird.hasLineOfSight(owner) && NightHeronFishing.safeStand(this.bird, this.bird.blockPosition())) {
                    this.bird.getNavigation().stop();
                    this.bird.setBehaviorState(NightHeronBehaviorState.IDLE);
                    return;
                }
                if (this.repath > 0 || !(this.bird.level() instanceof ServerLevel level)
                        || !BirdScanBudget.tryAcquire(level, this.bird, 2)) return;
                this.repath = 20 + Math.floorMod(this.bird.getId(), 7);
                BlockPos stand = this.ownerStand(owner);
                Path path = stand == null ? null : NightHeronFishing.groundPath(this.bird, stand);
                if (path != null) {
                    this.bird.setBehaviorState(NightHeronBehaviorState.MICRO_STROLL);
                    this.bird.getNavigation().moveTo(path, 1.2);
                    return;
                }
                if (!NightHeronFlightController.canLaunchPet(this.bird)) return;
                this.startFollowingFlight();
            }
        }
        ++this.airFollowTicks;
        if (!this.bird.onGround() && this.airFollowTicks > 10
                && (stopped || this.airFollowTicks >= AIR_FOLLOW_TICKS) && this.bird.distanceToSqr(owner) <= 144
                && this.repath == 0 && this.bird.level() instanceof ServerLevel level
                && BirdScanBudget.tryAcquire(level, this.bird, 2)) {
            this.repath = 20;
            BlockPos stand = this.ownerStand(owner);
            if (stand != null) {
                this.bird.getNavigation().stop();
                this.beginLanding(stand);
                return;
            }
        }
        double altitude = Mth.clamp(owner.getY() + 4, this.bird.level().getMinBuildHeight() + 2,
                this.bird.level().getMaxBuildHeight() - 3);
        Vec3 anchor = new Vec3(owner.getX(), altitude, owner.getZ());
        double distance = this.bird.position().distanceTo(anchor);
        boolean catchingUp = distance > 10;
        Vec3 target = catchingUp
                ? KestrelFlightPaths.ownerApproachTarget(anchor, ownerVelocity, distance)
                : KestrelFlightPaths.orbitTarget(anchor, this.bird.position(), 5, this.orbitDirection, altitude);
        this.bird.getNavigation().stop();
        if (this.bird.onGround()) {
            NightHeronFlightController.takeOff(this.bird, target.subtract(this.bird.position()), 0.25, 0.32);
        } else {
            NightHeronFlightController.tickPetFollow(this.bird, target, catchingUp ? 0.62 : 0.30);
        }
    }

    private void startFollowingFlight() {
        this.following = true;
        this.flying = false;
        this.landing = null;
        this.airFollowTicks = 0;
        this.bird.getNavigation().stop();
    }

    private void finishLanding() {
        this.flying = false;
        this.landing = null;
        this.approaching = false;
        this.groundFollowTicks = 0;
        this.airFollowTicks = 0;
    }

    /** The landing must be both safe and reachable by an ordinary short-range right click. */
    private BlockPos ownerStand(Player owner) {
        Vec3 preferred = owner.position().add(owner.getLookAngle().multiply(1, 0, 1).normalize().scale(1.7));
        BlockPos center = owner.blockPosition();
        BlockPos best = null;
        double nearest = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 1, 3))) {
            Vec3 feet = Vec3.atBottomCenterOf(pos);
            double distance = feet.distanceToSqr(owner.position());
            if (distance < 1 || distance > 6.25 || !NightHeronFishing.safeStand(this.bird, pos)) continue;
            double score = feet.distanceToSqr(preferred);
            if (score >= nearest || this.bird.level().clip(new ClipContext(owner.getEyePosition(),
                    feet.add(0, this.bird.getEyeHeight(), 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.bird))
                    .getType() != HitResult.Type.MISS) continue;
            best = pos.immutable();
            nearest = score;
        }
        return best;
    }

    void landWithoutOwner() {
        if (!this.following) return;
        if (this.repath > 0 || !(this.bird.level() instanceof ServerLevel level)
                || !BirdScanBudget.tryAcquire(level, this.bird, 2)) {
            NightHeronFlightController.tickOpenLanding(this.bird, this.bird.getDeltaMovement());
            return;
        }
        this.repath = 20;
        BlockPos stand = NightHeronFishing.landingBelow(this.bird, this.bird.position(), 4);
        if (stand == null && this.lastOwnerPosition != null)
            stand = NightHeronFishing.standNear(this.bird, this.lastOwnerPosition, 4);
        if (stand == null) stand = NightHeronLandingSelector.findEscapeLanding(this.bird, null, 0, 16);
        if (stand != null) {
            this.beginLanding(stand);
        } else {
            NightHeronFlightController.tickOpenLanding(this.bird, this.bird.getDeltaMovement());
            if (this.bird.onGround()) this.stop();
        }
    }

    private void beginLanding(BlockPos stand) {
        this.bird.beginPetLanding();
        this.following = false;
        this.flying = true;
        this.flightTicks = 0;
        this.landing = stand;
        this.approaching = this.bird.getY() >= stand.getY() + 0.6;
    }

    void moveTo(Vec3 destination) {
        this.moveTo(destination, true);
    }

    private void moveTo(Vec3 destination, boolean allowFlight) {
        if (this.following) {
            BlockPos stand = NightHeronFishing.standNear(this.bird, destination, 1);
            if (stand != null) this.beginLanding(stand);
            else this.landWithoutOwner();
            return;
        }
        if (this.flying || this.repath > 0) return;
        this.repath = 20 + Math.floorMod(this.bird.getId(), 7);
        if (!(this.bird.level() instanceof ServerLevel level) || !BirdScanBudget.tryAcquire(level, this.bird)) return;
        BlockPos stand = NightHeronFishing.standNear(this.bird, destination, 1);
        if (stand == null) return;
        if (this.bird.distanceToSqr(Vec3.atBottomCenterOf(stand)) < 0.64) { this.bird.getNavigation().stop(); return; }
        Path path = NightHeronFishing.groundPath(this.bird, stand);
        if (path != null) {
            this.bird.setBehaviorState(NightHeronBehaviorState.MICRO_STROLL);
            // Use a purposeful pet pace; 1.0 permits the shared idle/foraging slowdowns.
            this.bird.getNavigation().moveTo(path, 1.2);
            return;
        }
        // Never invoke natural flyby (which owns its own long transit timer/target).
        if (!allowFlight || !NightHeronEntity.canReadChunk(level, stand)
                || this.bird.distanceToSqr(Vec3.atBottomCenterOf(stand)) > 4096) return;
        this.bird.getNavigation().stop();
        this.beginLanding(stand);
        NightHeronFlightController.takeOff(this.bird, NightHeronLandingSelector.directionTo(stand, this.bird), 0.30, 0.42);
    }

    void stop() {
        this.bird.getNavigation().stop();
        // Also settle a completed emergency hop when pet waiting takes control again.
        if (this.isFlying() || !this.bird.isBirdEmergencyOverrideActive()) this.bird.settleInterruptedFlight(NightHeronBehaviorState.IDLE);
        this.following = false;
        this.flying = false;
        this.landing = null;
        this.approaching = false;
        this.repath = 0;
        this.ownerStillTicks = 0;
        this.groundFollowTicks = 0;
        this.airFollowTicks = 0;
        this.lastOwnerPosition = null;
    }
}
