package EdDYON.guaniao.content.bath;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class BirdBathUseGoal extends Goal {
    private static final int CLAIM_TICKS = 100;
    private static final int CONSUME_WARMUP_TICKS = 46;
    private static final double TOP_HORIZONTAL_REACH_SQR = 0.85D * 0.85D;
    private static final double TOP_MIN_FEET_Y_OFFSET = 1.18D;
    private static final double TOP_MAX_FEET_Y_OFFSET = 1.95D;
    private static final double TOP_MOUNT_RANGE_SQR = 2.15D * 2.15D;
    private static final double TOP_SETTLE_HORIZONTAL_SQR = 0.95D * 0.95D;
    private static final double TOP_SETTLE_MIN_FEET_Y_OFFSET = 0.92D;
    private final PathfinderMob bird;
    private final double speedModifier;
    private final double searchRadius;
    private final int scanChance;
    private final Predicate<BirdBathBlockEntity> bathPredicate;
    private final BooleanSupplier canStart;
    private final Consumer<BirdBathBlockEntity> onApproach;
    private final BiConsumer<BirdBathBlockEntity, BirdBathContentType> onConsume;
    private final BiConsumer<BirdBathBlockEntity, Boolean> onStop;
    private BirdBathBlockEntity targetBath;
    private int repathTicks;
    private int topUseTicks;
    private int mountHopCooldown;
    private int totalTicks;
    private boolean consumed;
    private boolean feedingAnimationStarted;

    public BirdBathUseGoal(PathfinderMob bird, double speedModifier, double searchRadius, int scanChance,
                           Predicate<BirdBathBlockEntity> bathPredicate, BooleanSupplier canStart,
                           Consumer<BirdBathBlockEntity> onApproach,
                           BiConsumer<BirdBathBlockEntity, BirdBathContentType> onConsume,
                           BiConsumer<BirdBathBlockEntity, Boolean> onStop) {
        this.bird = bird;
        this.speedModifier = speedModifier;
        this.searchRadius = searchRadius;
        this.scanChance = Math.max(1, scanChance);
        this.bathPredicate = bathPredicate;
        this.canStart = canStart;
        this.onApproach = onApproach;
        this.onConsume = onConsume;
        this.onStop = onStop;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.bird.level().isClientSide || !this.canStart.getAsBoolean()) {
            return false;
        }
        if (this.bird.getRandom().nextInt(this.scanChance) != 0) {
            return false;
        }
        Optional<BirdBathBlockEntity> found = BirdBathAttraction.findNearbyUsableBath(
                this.bird.level(),
                this.bird.blockPosition(),
                this.searchRadius,
                this::canUseBath
        );
        if (found.isEmpty()) {
            return false;
        }
        BirdBathBlockEntity bath = found.get();
        if (!BirdBathAttraction.tryClaimUse(bath, this.bird, CLAIM_TICKS)) {
            return false;
        }
        this.targetBath = bath;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.consumed
                && this.totalTicks < 220
                && this.targetBath != null
                && this.canUseBath(this.targetBath)
                && (this.targetBath.isOccupiedBy(this.bird.getUUID()) || BirdBathAttraction.tryClaimUse(this.targetBath, this.bird, CLAIM_TICKS));
    }

    @Override
    public void start() {
        this.consumed = false;
        this.repathTicks = 0;
        this.topUseTicks = 0;
        this.mountHopCooldown = 0;
        this.totalTicks = 0;
        this.feedingAnimationStarted = false;
        if (this.targetBath != null) {
            this.onApproach.accept(this.targetBath);
            this.moveToBathTop();
        }
    }

    @Override
    public void tick() {
        if (this.targetBath == null) {
            return;
        }
        ++this.totalTicks;
        if (this.mountHopCooldown > 0) {
            --this.mountHopCooldown;
        }
        Vec3 usePosition = BirdBathAttraction.topUsePosition(this.targetBath);
        this.bird.getLookControl().setLookAt(usePosition.x, usePosition.y, usePosition.z, 30.0F, 30.0F);
        if (this.isAtTopUsePosition(usePosition)) {
            this.bird.getNavigation().stop();
            ++this.topUseTicks;
            this.startFeedingAnimationIfNeeded();
            if (this.topUseTicks >= CONSUME_WARMUP_TICKS) {
                this.consumeFromBath();
            }
            return;
        }
        this.topUseTicks = 0;
        this.feedingAnimationStarted = false;
        Vec3 standPosition = BirdBathAttraction.topStandPosition(this.targetBath);
        if (this.trySettleOntoTop(standPosition)) {
            return;
        }
        if (this.tryHopOntoTop(standPosition)) {
            return;
        }
        if (--this.repathTicks <= 0 || this.bird.getNavigation().isDone()) {
            this.moveToBathTop();
        }
        if (this.totalTicks % 35 == 0) {
            BirdBathAttraction.tryClaimUse(this.targetBath, this.bird, CLAIM_TICKS);
        }
    }

    @Override
    public void stop() {
        BirdBathBlockEntity bath = this.targetBath;
        if (bath != null) {
            UUID uuid = this.bird.getUUID();
            bath.releaseUse(uuid);
            this.onStop.accept(bath, this.consumed);
        }
        this.targetBath = null;
        this.topUseTicks = 0;
        this.mountHopCooldown = 0;
        this.totalTicks = 0;
        this.feedingAnimationStarted = false;
        this.consumed = false;
    }

    private boolean canUseBath(BirdBathBlockEntity bath) {
        if (bath == null || bath.isRemoved() || !this.bird.isAlive() || bath.getLevel() != this.bird.level()) {
            return false;
        }
        UUID uuid = this.bird.getUUID();
        return this.bathPredicate.test(bath) && (!bath.isOccupied() || bath.isOccupiedBy(uuid));
    }

    private void moveToBathTop() {
        if (this.targetBath == null) {
            return;
        }
        Vec3 top = BirdBathAttraction.topStandPosition(this.targetBath);
        this.bird.getNavigation().moveTo(top.x, top.y, top.z, this.speedModifier);
        this.repathTicks = 18 + this.bird.getRandom().nextInt(16);
    }

    private boolean tryHopOntoTop(Vec3 standPosition) {
        if (this.targetBath == null || this.mountHopCooldown > 0) {
            return false;
        }
        Vec3 feet = this.bird.position();
        double horizontalDistanceSqr = feet.subtract(standPosition).multiply(1.0D, 0.0D, 1.0D).lengthSqr();
        double feetOffset = feet.y - this.targetBath.getBlockPos().getY();
        if (feetOffset >= TOP_MIN_FEET_Y_OFFSET || horizontalDistanceSqr > TOP_MOUNT_RANGE_SQR) {
            return false;
        }
        if (this.bird instanceof BirdBathMountable mountable && mountable.startBirdBathMountFlight(standPosition)) {
            this.mountHopCooldown = 22;
            return true;
        }
        Vec3 horizontal = standPosition.subtract(feet).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() <= 1.0E-4D) {
            horizontal = Vec3.ZERO;
        } else {
            horizontal = horizontal.normalize().scale(0.27D);
        }
        this.bird.getNavigation().stop();
        this.bird.setDeltaMovement(horizontal.x, 0.66D, horizontal.z);
        this.bird.fallDistance = 0.0F;
        this.bird.hasImpulse = true;
        this.mountHopCooldown = 20;
        return true;
    }

    private boolean trySettleOntoTop(Vec3 standPosition) {
        if (this.targetBath == null) {
            return false;
        }
        Vec3 feet = this.bird.position();
        double horizontalDistanceSqr = feet.subtract(standPosition).multiply(1.0D, 0.0D, 1.0D).lengthSqr();
        double feetOffset = feet.y - this.targetBath.getBlockPos().getY();
        if (horizontalDistanceSqr > TOP_SETTLE_HORIZONTAL_SQR
                || feetOffset < TOP_SETTLE_MIN_FEET_Y_OFFSET
                || feetOffset > TOP_MAX_FEET_Y_OFFSET) {
            return false;
        }
        this.bird.getNavigation().stop();
        this.bird.setPos(standPosition.x, standPosition.y, standPosition.z);
        this.bird.setDeltaMovement(Vec3.ZERO);
        this.bird.fallDistance = 0.0F;
        this.bird.hasImpulse = true;
        this.mountHopCooldown = 8;
        return true;
    }

    private boolean isAtTopUsePosition(Vec3 usePosition) {
        Vec3 feet = this.bird.position();
        double horizontalDistanceSqr = feet.subtract(usePosition).multiply(1.0D, 0.0D, 1.0D).lengthSqr();
        double feetOffset = feet.y - this.targetBath.getBlockPos().getY();
        return horizontalDistanceSqr <= TOP_HORIZONTAL_REACH_SQR
                && feetOffset >= TOP_MIN_FEET_Y_OFFSET
                && feetOffset <= TOP_MAX_FEET_Y_OFFSET;
    }

    private void startFeedingAnimationIfNeeded() {
        if (this.feedingAnimationStarted || this.targetBath == null) {
            return;
        }
        this.feedingAnimationStarted = true;
        if (this.bird instanceof BirdBathFeedingAnimatable animatable) {
            animatable.startBirdBathFeedingAnimation(this.targetBath.getContentType(), CONSUME_WARMUP_TICKS);
        }
    }

    private void consumeFromBath() {
        if (this.targetBath == null || this.consumed) {
            return;
        }
        BirdBathContentType consumedType = this.targetBath.getContentType();
        if (BirdBathAttraction.consumeServingForBird(this.targetBath)) {
            this.consumed = true;
            this.onConsume.accept(this.targetBath, consumedType);
        }
    }
}
