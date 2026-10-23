package EdDYON.guaniao.content.bird.nightheron;

import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Pet movement owns MOVE/LOOK together; emergency goals and STAY retain higher precedence. */
final class NightHeronPetGoal extends Goal {
    private final NightHeronEntity bird;
    private final NightHeronPetNavigation navigation;
    private BlockPos roost;
    private int scanTicks;
    private int offerTicks;
    private int retryTicks;
    private int travelTicks;

    NightHeronPetGoal(NightHeronEntity bird) {
        this.bird = bird;
        this.navigation = new NightHeronPetNavigation(bird);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        return this.bird.isTame() && !this.bird.isLeashed() && !this.bird.isPassenger()
                && !this.bird.isBirdEmergencyOverrideActive() && !this.bird.isEatingFish()
                && (this.bird.getBirdCommandMode() != BirdCommandMode.FREE
                    || this.bird.hasDeliveryFish() || !this.bird.isActiveTime());
    }
    @Override public boolean canContinueToUse() { return this.canUse(); }
    @Override public void start() { this.scanTicks = Math.floorMod(this.bird.getId(), 20); }
    @Override public void stop() {
        this.navigation.stop();
        this.roost = null;
        this.offerTicks = 0;
        this.travelTicks = 0;
        this.bird.setOfferTicks(0);
        // Losing arbitration does not consume the mouth slot or restart a gift timer.
    }

    @Override public void tick() {
        this.navigation.tick();
        if (this.scanTicks > 0) --this.scanTicks;
        if (this.bird.getBirdCommandMode() == BirdCommandMode.STAY) {
            this.navigation.landWithoutOwner();
            if (!this.navigation.isFlying()) this.waitHere();
            return;
        }
        if (this.retryTicks > 0) { --this.retryTicks; return; }
        if (this.bird.getBirdCommandMode() == BirdCommandMode.ROOST) { this.roost(); return; }
        boolean following = this.bird.getBirdCommandMode() == BirdCommandMode.FOLLOW;
        Player owner = following ? this.bird.fishTask().owner() : this.bird.fishTask().nearbyOwner();
        if (owner == null) {
            this.navigation.landWithoutOwner();
            // An owner disconnect must not discard a safe landing already in progress.
            if (!this.navigation.isFlying()) {
                if (this.bird.isActiveTime()) this.waitHere(); else this.rest();
            }
            return;
        }
        if (this.bird.hasDeliveryFish() && (!following || this.bird.distanceToSqr(owner) <= 144)) {
            this.deliver(owner); return;
        }
        this.offerTicks = 0;
        this.bird.setOfferTicks(0);
        if (following) {
            this.travelTicks = 0;
            this.navigation.follow(owner);
            if (this.bird.onGround() && this.bird.getNavigation().isDone() && this.bird.distanceToSqr(owner) <= 6.25)
                this.bird.getLookControl().setLookAt(owner, 30, 30);
        } else if (this.bird.isActiveTime()) this.waitHere();
        else this.rest();
    }

    private void deliver(Player owner) {
        double distance = this.bird.distanceToSqr(owner);
        if (!this.bird.fishTask().permitsTravel()) { this.waitHere(); return; }
        this.bird.getLookControl().setLookAt(owner, 30, 30);
        if (distance <= 5.0 && this.bird.onGround() && this.bird.hasLineOfSight(owner)) {
            this.navigation.stop();
            this.travelTicks = 0;
            this.bird.setBehaviorState(NightHeronBehaviorState.IDLE);
            this.bird.setOfferTicks(++this.offerTicks);
            if (this.offerTicks >= 24) {
                if (!this.bird.fishTask().deliver(owner, false)) this.retryTicks = 100;
                this.offerTicks = 0;
                this.bird.setOfferTicks(0);
            }
        } else {
            this.offerTicks = 0;
            this.bird.setOfferTicks(0);
            Vec3 forward = owner.getLookAngle().multiply(1, 0, 1).normalize();
            this.travel(owner.position().add(forward.scale(1.7)));
        }
    }

    private void travel(Vec3 target) {
        if (++this.travelTicks > 400) {
            this.navigation.stop();
            this.travelTicks = 0;
            this.retryTicks = 100;
            return;
        }
        this.navigation.moveTo(target);
    }

    private void waitHere() {
        this.navigation.stop();
        this.travelTicks = 0;
        this.bird.setOfferTicks(0);
        this.offerTicks = 0;
        if (this.bird.onGround()) this.bird.setBehaviorState(NightHeronBehaviorState.IDLE);
    }

    private void rest() {
        this.waitHere();
        if (this.bird.onGround()) this.bird.setBehaviorState(NightHeronBehaviorState.ROOSTING);
    }

    private void roost() {
        if (this.roost == null && this.scanTicks == 0 && this.bird.level() instanceof ServerLevel level
                && BirdScanBudget.tryAcquire(level, this.bird, 2)) {
            this.scanTicks = 100;
            double nearest = Double.MAX_VALUE;
            BlockPos origin = this.bird.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -3, -8), origin.offset(8, 5, 8))) {
                if (!NightHeronEntity.canReadChunk(level, pos) || !level.getBlockState(pos.below()).is(BirdTags.BIRD_PERCHES)
                        || !NightHeronFishing.safeStand(this.bird, pos)) continue;
                double distance = pos.distSqr(origin);
                if (distance < nearest) { nearest = distance; this.roost = pos.immutable(); }
            }
        }
        if (this.roost != null && !NightHeronFishing.safeStand(this.bird, this.roost)) this.roost = null;
        if (this.roost != null && (!this.bird.onGround() || this.bird.distanceToSqr(Vec3.atBottomCenterOf(this.roost)) > 1)) {
            this.travel(Vec3.atBottomCenterOf(this.roost));
        } else {
            this.navigation.landWithoutOwner();
            if (!this.navigation.isFlying()) this.rest();
        }
    }
}
