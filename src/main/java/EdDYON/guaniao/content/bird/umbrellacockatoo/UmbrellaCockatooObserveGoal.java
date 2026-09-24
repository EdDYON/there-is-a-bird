package EdDYON.guaniao.content.bird.umbrellacockatoo;

import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarBehaviorState;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarControl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * The signature behaviour: stopping to study a player for a while.
 *
 * <p>Vanilla look goals re-pick a target constantly, which reads as a twitchy bird.
 * This one holds a chosen player for at least {@value #MIN_STICKY_TICKS} ticks, keeps
 * watching the last known position after losing sight, and only expresses interest
 * through the head and neck — the body stays put.</p>
 */
public class UmbrellaCockatooObserveGoal extends Goal {
    private static final double NOTICE_RADIUS = 13.0D;
    private static final double LOSE_RADIUS = 20.0D;
    private static final int MIN_STICKY_TICKS = 40;
    private static final int MAX_STICKY_TICKS = 100;
    private static final int MIN_HOLD_TICKS = 40;
    private static final int MAX_HOLD_TICKS = 120;
    private static final int LAST_SEEN_TICKS = 30;

    private final UmbrellaCockatooEntity bird;
    @Nullable
    private Player target;
    private int holdTicks;
    private int stickyTicks;
    private int lastSeenTicks;
    @Nullable
    private Vec3 lastSeen;

    public UmbrellaCockatooObserveGoal(UmbrellaCockatooEntity bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.bird.isCalmAndAwake() || !this.bird.isDayActive()) {
            return false;
        }
        if (!this.bird.isObserveCooldownReady()) {
            return false;
        }
        // Stickiness first: a second player standing slightly closer does not steal
        // the bird's attention.
        if (this.target != null && this.isWorthKeeping(this.target)) {
            return true;
        }
        Player found = this.findPlayer();
        if (found == null) {
            this.bird.markObserveScan();
            return false;
        }
        this.target = found;
        this.stickyTicks = MIN_STICKY_TICKS
                + this.bird.getRandom().nextInt(MAX_STICKY_TICKS - MIN_STICKY_TICKS + 1);
        this.holdTicks = MIN_HOLD_TICKS
                + this.bird.getRandom().nextInt(MAX_HOLD_TICKS - MIN_HOLD_TICKS + 1);
        this.lastSeen = found.position().add(0.0D, found.getEyeHeight(), 0.0D);
        this.lastSeenTicks = LAST_SEEN_TICKS;
        this.bird.setObservedPlayer(found.getUUID());
        this.bird.feel(UmbrellaCockatooEmotion.CURIOUS, this.holdTicks);
        BudgerigarControl.setBehaviorStateFor(this.bird, BudgerigarBehaviorState.CURIOUS, this.holdTicks);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.holdTicks > 0
                && this.bird.isCalmAndAwake()
                && this.target != null
                && this.target.isAlive()
                && !this.target.isSpectator()
                && this.bird.distanceToSqr(this.target) < LOSE_RADIUS * LOSE_RADIUS;
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        --this.holdTicks;
        if (this.stickyTicks > 0) {
            --this.stickyTicks;
        }
        if (this.bird.hasLineOfSight(this.target)) {
            this.lastSeen = this.target.position().add(0.0D, this.target.getEyeHeight(), 0.0D);
            this.lastSeenTicks = LAST_SEEN_TICKS;
            // Only the head and neck track the player; the body is free to stay put.
            this.bird.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        } else if (this.lastSeenTicks > 0) {
            --this.lastSeenTicks;
            if (this.lastSeen != null) {
                this.bird.getLookControl().setLookAt(
                        this.lastSeen.x, this.lastSeen.y, this.lastSeen.z, 20.0F, 20.0F);
            }
        }
        // Slow, deliberate head cocks — roughly one every few seconds, never a twitch.
        if (this.bird.getRandom().nextInt(45) == 0) {
            this.bird.beginHeadTilt();
        }
        if (this.bird.getRandom().nextInt(120) == 0) {
            this.bird.feel(UmbrellaCockatooEmotion.CURIOUS, 80);
        }
        // A tame bird may hop onto the person it has been studying.
        if (this.bird.getRandom().nextInt(90) == 0 && this.bird.tryPerchOnOwner(this.target)) {
            this.holdTicks = 0;
        }
    }

    @Override
    public void stop() {
        if (this.target != null && this.bird.getObservedPlayerId() != null
                && this.bird.getObservedPlayerId().equals(this.target.getUUID())) {
            this.bird.setObservedPlayer(null);
        }
        this.target = null;
        this.lastSeen = null;
        this.lastSeenTicks = 0;
        this.bird.markObserveScan();
    }

    private boolean isWorthKeeping(Player player) {
        return player.isAlive()
                && !player.isSpectator()
                && this.bird.distanceToSqr(player) < LOSE_RADIUS * LOSE_RADIUS
                && this.stickyTicks > 0;
    }

    @Nullable
    private Player findPlayer() {
        if (!(this.bird.level() instanceof ServerLevel serverLevel)
                || !BirdScanBudget.tryAcquire(serverLevel, this.bird)) {
            return null;
        }
        Player nearest = this.bird.level().getNearestPlayer(this.bird, NOTICE_RADIUS);
        if (nearest == null || nearest.isSpectator() || !nearest.isAlive()) {
            return null;
        }
        return nearest;
    }
}
