package EdDYON.guaniao.content.bird.umbrellacockatoo;

import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarBehaviorState;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarControl;
import EdDYON.guaniao.content.bird.cockatiel.CockatielEntity;
import EdDYON.guaniao.content.bird.macaw.MacawEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * The flock half of this bird's personality.
 *
 * <p>Cockatoos hang around other cockatoos, and will happily sidle up to a cockatiel
 * or a macaw. They do not chase, hunt or harass small parrots — a budgerigar is
 * never a target here.</p>
 */
public class UmbrellaCockatooSocialGoal extends Goal {
    private static final double LOOKOUT_RADIUS = 12.0D;
    private static final double APPROACH_DISTANCE = 3.0D;
    private static final int MAX_APPROACH_TICKS = 200;

    private final UmbrellaCockatooEntity bird;
    @Nullable
    private LivingEntity partner;
    private int approachTicks;
    private int displayCooldown;

    public UmbrellaCockatooSocialGoal(UmbrellaCockatooEntity bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.bird.isCalmAndAwake() || !this.bird.isDayActive()
                || !this.bird.isSocialCooldownReady()) {
            return false;
        }
        // A bird already being studied by a player should not wander off to a friend.
        if (this.bird.getObservedPlayerId() != null) {
            return false;
        }
        LivingEntity found = this.findPartner();
        if (found == null) {
            this.bird.markSocial(60 + this.bird.getRandom().nextInt(120));
            return false;
        }
        this.partner = found;
        this.approachTicks = MAX_APPROACH_TICKS;
        this.displayCooldown = 60 + this.bird.getRandom().nextInt(160);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.partner != null
                && this.partner.isAlive()
                && this.approachTicks > 0
                && this.bird.isCalmAndAwake()
                && this.bird.distanceToSqr(this.partner) < LOOKOUT_RADIUS * LOOKOUT_RADIUS * 2.0D;
    }

    @Override
    public void start() {
        this.bird.feel(UmbrellaCockatooEmotion.CURIOUS, 120);
        BudgerigarControl.setBehaviorStateFor(this.bird, BudgerigarBehaviorState.CURIOUS, 120);
        this.moveCloser();
    }

    @Override
    public void tick() {
        if (this.partner == null) {
            return;
        }
        --this.approachTicks;
        this.bird.getLookControl().setLookAt(this.partner, 30.0F, 30.0F);
        if (this.bird.distanceToSqr(this.partner) > APPROACH_DISTANCE * APPROACH_DISTANCE) {
            if (this.bird.getNavigation().isDone()) {
                this.moveCloser();
            }
            return;
        }
        this.bird.getNavigation().stop();
        // Side by side: check each other out, and every so often throw a full display.
        if (this.bird.getRandom().nextInt(70) == 0) {
            this.bird.beginHeadTilt();
        }
        if (this.displayCooldown > 0) {
            --this.displayCooldown;
            return;
        }
        this.displayCooldown = 200 + this.bird.getRandom().nextInt(400);
        if (this.bird.getRandom().nextFloat() < 0.5F) {
            // Showing off is contagious only for the cockatoo's own kind.
            this.bird.feel(UmbrellaCockatooEmotion.EXCITED, 100 + this.bird.getRandom().nextInt(80));
            BudgerigarControl.setBehaviorStateFor(this.bird, BudgerigarBehaviorState.DANCING, 60);
        } else {
            this.bird.feel(UmbrellaCockatooEmotion.CURIOUS, 100);
        }
    }

    @Override
    public void stop() {
        this.bird.getNavigation().stop();
        this.partner = null;
        this.bird.markSocial(120 + this.bird.getRandom().nextInt(240));
    }

    private void moveCloser() {
        if (this.partner == null) {
            return;
        }
        this.bird.getNavigation().moveTo(this.partner, 0.68D);
    }

    @Nullable
    private LivingEntity findPartner() {
        if (!(this.bird.level() instanceof ServerLevel serverLevel)
                || !BirdScanBudget.tryAcquire(serverLevel, this.bird)) {
            return null;
        }
        List<LivingEntity> candidates = this.bird.level().getEntitiesOfClass(LivingEntity.class,
                this.bird.getBoundingBox().inflate(LOOKOUT_RADIUS),
                this::isCompatible);
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(this.bird.getRandom().nextInt(candidates.size()));
    }

    private boolean isCompatible(Entity entity) {
        if (entity == this.bird || !entity.isAlive()) {
            return false;
        }
        // Large parrots only. Small parrots are neighbours, not flockmates.
        return entity instanceof UmbrellaCockatooEntity
                || entity instanceof CockatielEntity
                || entity instanceof MacawEntity;
    }
}
