package EdDYON.guaniao.content.bird.kiwi;

import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

final class KiwiTerritorialFightGoal extends Goal {
    private static final double HOME_SEPARATION_SQR = 14.0D * 14.0D;
    private static final double BRAWL_START_DISTANCE = 1.30D;
    private static final double BRAWL_ATTACK_DISTANCE = 0.72D;
    private static final double BRAWL_OVERLAP_DISTANCE = 0.40D;
    private static final double APPROACH_STALL_DISTANCE = 1.50D;
    private static final int APPROACH_STALL_TICKS = 5;
    private static final int MIN_BRAWL_TICKS = 60;
    private final KiwiEntity kiwi;
    private KiwiEntity pendingRival;
    private Vec3 fleeTarget;
    private int territoryCheckCooldown;
    private int fightCallCooldown;
    private double previousApproachDistanceSqr = Double.MAX_VALUE;
    private int approachStallTicks;

    KiwiTerritorialFightGoal(KiwiEntity kiwi) {
        this.kiwi = kiwi;
        this.territoryCheckCooldown = 80 + kiwi.getRandom().nextInt(120);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.kiwi.isConflictActive()) {
            return this.kiwi.getConflictRival() != null;
        }
        if (this.territoryCheckCooldown-- > 0 || !this.canStartConflict(this.kiwi)) {
            return false;
        }
        this.territoryCheckCooldown = 100 + this.kiwi.getRandom().nextInt(180);
        this.pendingRival = this.findIntruder();
        return this.pendingRival != null;
    }

    @Override
    public void start() {
        if (!this.kiwi.isConflictActive() && this.pendingRival != null) {
            int warningTicks = 20 + this.kiwi.getRandom().nextInt(31);
            this.kiwi.startConflictWith(this.pendingRival, warningTicks);
        }
        this.pendingRival = null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.kiwi.isConflictActive() && this.kiwi.getConflictRival() != null;
    }

    @Override
    public void tick() {
        KiwiEntity rival = this.kiwi.getConflictRival();
        if (rival == null || !rival.isAlive()) {
            this.kiwi.finishConflict();
            return;
        }
        if (!this.kiwi.canContinueTerritorialConflict()) {
            this.kiwi.endConflictPair(rival);
            return;
        }

        switch (this.kiwi.getConflictState()) {
            case WARNING -> this.tickWarning(rival);
            case APPROACH -> this.tickApproach(rival);
            case FIGHTING -> this.tickFight(rival);
            case CHASING -> this.tickChase(rival);
            case FLEEING -> this.tickFlee(rival);
            case NONE -> {
            }
        }
    }

    @Override
    public void stop() {
        this.fleeTarget = null;
        this.pendingRival = null;
        this.fightCallCooldown = 0;
        this.previousApproachDistanceSqr = Double.MAX_VALUE;
        this.approachStallTicks = 0;
        this.territoryCheckCooldown = Math.max(this.territoryCheckCooldown, 80);
    }

    private void tickWarning(KiwiEntity rival) {
        this.kiwi.getNavigation().stop();
        this.lookAt(rival);
        int remaining = this.kiwi.decrementConflictTicks();
        if (remaining > 0 || this.kiwi.getId() > rival.getId()
                || rival.getConflictState() != KiwiConflictState.WARNING) {
            return;
        }

        float ownWillingness = this.kiwi.territorialWillingness(rival);
        float rivalWillingness = rival.territorialWillingness(this.kiwi);
        if (Math.min(ownWillingness, rivalWillingness) < 0.12F) {
            this.resolveWinner(ownWillingness >= rivalWillingness ? this.kiwi : rival,
                    ownWillingness >= rivalWillingness ? rival : this.kiwi);
            return;
        }
        this.previousApproachDistanceSqr = Double.MAX_VALUE;
        this.approachStallTicks = 0;
        this.kiwi.setConflictState(KiwiConflictState.APPROACH, 100);
        rival.setConflictState(KiwiConflictState.APPROACH, 100);
    }

    private void tickApproach(KiwiEntity rival) {
        this.lookAt(rival);
        this.kiwi.decrementConflictTicks();
        this.kiwi.getNavigation().moveTo(rival, 1.15D);
        double distanceSqr = this.kiwi.distanceToSqr(rival);
        if (distanceSqr <= APPROACH_STALL_DISTANCE * APPROACH_STALL_DISTANCE
                && distanceSqr >= this.previousApproachDistanceSqr - 0.0025D) {
            ++this.approachStallTicks;
        } else {
            this.approachStallTicks = 0;
        }
        this.previousApproachDistanceSqr = distanceSqr;
        if ((distanceSqr <= BRAWL_START_DISTANCE * BRAWL_START_DISTANCE
                || this.approachStallTicks >= APPROACH_STALL_TICKS
                || this.kiwi.getConflictTicks() <= 0)
                && this.kiwi.getId() < rival.getId()) {
            this.kiwi.setConflictState(KiwiConflictState.FIGHTING, KiwiDefinition.MAX_FIGHT_TICKS);
            rival.setConflictState(KiwiConflictState.FIGHTING, KiwiDefinition.MAX_FIGHT_TICKS);
            this.kiwi.setFightAttackCooldown(4);
            rival.setFightAttackCooldown(10);
        }
    }

    private void tickFight(KiwiEntity rival) {
        this.lookAt(rival);
        this.kiwi.decrementConflictTicks();
        this.kiwi.tickFightAttackCooldown();

        if (this.kiwi.getId() < rival.getId()) {
            if (this.fightCallCooldown-- <= 0) {
                this.playFightCall(rival);
                this.fightCallCooldown = 32 + this.kiwi.getRandom().nextInt(35);
            }
            if (this.kiwi.level().getGameTime() % 3L == 0L) {
                this.spawnBrawlSmoke(rival, false);
            }
        }

        if (this.kiwi.getId() < rival.getId()) {
            KiwiEntity loser = this.chooseLoserIfResolved(rival);
            if (loser != null) {
                this.resolveWinner(loser == this.kiwi ? rival : this.kiwi, loser);
                return;
            }
        }

        double distanceSqr = this.kiwi.distanceToSqr(rival);
        this.maintainBrawlOverlap(rival);
        if (distanceSqr > BRAWL_ATTACK_DISTANCE * BRAWL_ATTACK_DISTANCE) {
            return;
        }

        if (this.kiwi.getFightAttackCooldown() > 0) {
            return;
        }
        this.kiwi.setFightAttackCooldown(10 + this.kiwi.getRandom().nextInt(5));
        if (rival.getHealth() <= rival.getMaxHealth() * 0.42F) {
            if (this.hasCompletedMinimumBrawl()) {
                this.resolveWinner(this.kiwi, rival);
            }
            return;
        }

        if (rival.hurt(this.kiwi.damageSources().mobAttack(this.kiwi), 1.0F)) {
            this.playFightPainCall(rival);
            this.spawnBrawlSmoke(rival, true);
        }
    }

    private void maintainBrawlOverlap(KiwiEntity rival) {
        this.kiwi.getNavigation().stop();
        Vec3 towardRival = rival.position().subtract(this.kiwi.position()).multiply(1.0D, 0.0D, 1.0D);
        double distance = towardRival.length();
        if (distance < 1.0E-4D) {
            towardRival = this.kiwi.getId() < rival.getId()
                    ? new Vec3(1.0D, 0.0D, 0.0D)
                    : new Vec3(-1.0D, 0.0D, 0.0D);
            distance = 0.0D;
        } else {
            towardRival = towardRival.scale(1.0D / distance);
        }

        long sharedSeed = Math.min(this.kiwi.getId(), rival.getId()) * 13L;
        float brawlPhase = (float) ((this.kiwi.level().getGameTime() + sharedSeed) * 0.58D);
        double targetDistance = BRAWL_OVERLAP_DISTANCE + Mth.sin(brawlPhase) * 0.11D;
        double inwardSpeed = Mth.clamp(
                (distance - targetDistance) * 0.34D,
                -0.055D,
                0.055D);
        double shuffle = Mth.sin(brawlPhase * 0.73F) * 0.032D;
        Vec3 sideways = new Vec3(-towardRival.z, 0.0D, towardRival.x).scale(shuffle);
        Vec3 brawlMovement = towardRival.scale(inwardSpeed).add(sideways);
        this.kiwi.setDeltaMovement(
                brawlMovement.x,
                this.kiwi.getDeltaMovement().y,
                brawlMovement.z);
    }

    private void playFightCall(KiwiEntity rival) {
        KiwiEntity caller = this.kiwi.getRandom().nextBoolean() ? this.kiwi : rival;
        caller.playSound(
                GuaniaoSoundEvents.KIWI_AMBIENT.get(),
                0.68F,
                0.98F + caller.getRandom().nextFloat() * 0.20F);
    }

    private void playFightPainCall(KiwiEntity victim) {
        victim.playSound(
                GuaniaoSoundEvents.KIWI_AMBIENT.get(),
                0.82F,
                1.16F + victim.getRandom().nextFloat() * 0.22F);
    }

    private void spawnBrawlSmoke(KiwiEntity rival, boolean impact) {
        if (!(this.kiwi.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 center = this.kiwi.position().add(rival.position()).scale(0.5D);
        double y = Math.min(this.kiwi.getY(), rival.getY()) + 0.045D;
        serverLevel.sendParticles(
                ParticleTypes.POOF,
                center.x,
                y,
                center.z,
                impact ? 2 : 1,
                impact ? 0.13D : 0.07D,
                impact ? 0.018D : 0.008D,
                impact ? 0.13D : 0.07D,
                impact ? 0.026D : 0.012D);
    }

    private void tickChase(KiwiEntity rival) {
        this.lookAt(rival);
        int remaining = this.kiwi.decrementConflictTicks();
        BlockPos home = this.kiwi.getHomeCenter();
        boolean drivenFromTerritory = home != null
                && home.distSqr(rival.blockPosition())
                > (double) KiwiDefinition.NORMAL_TERRITORY_RADIUS * KiwiDefinition.NORMAL_TERRITORY_RADIUS;
        if (remaining <= 0 || drivenFromTerritory) {
            this.kiwi.finishConflict();
            this.kiwi.requestReturnHomeAfterConflict();
            return;
        }
        this.kiwi.getNavigation().moveTo(rival, 1.25D);
    }

    private void tickFlee(KiwiEntity rival) {
        this.lookAwayFrom(rival);
        int remaining = this.kiwi.decrementConflictTicks();
        BlockPos rivalHome = rival.getHomeCenter();
        boolean reachedSafety = rivalHome != null
                && rivalHome.distSqr(this.kiwi.blockPosition())
                > (double) KiwiDefinition.MAX_TERRITORY_RADIUS * KiwiDefinition.MAX_TERRITORY_RADIUS;
        if (remaining <= 0 || reachedSafety) {
            this.kiwi.endConflictPair(rival);
            return;
        }
        if (this.fleeTarget == null || this.kiwi.getNavigation().isDone() || remaining % 20 == 0) {
            this.fleeTarget = KiwiHabitatUtil.findEscapeTarget(this.kiwi, rival.position());
            if (this.fleeTarget != null) {
                this.kiwi.getNavigation().moveTo(
                        this.fleeTarget.x,
                        this.fleeTarget.y,
                        this.fleeTarget.z,
                        1.38D);
            }
        }
    }

    private KiwiEntity chooseLoserIfResolved(KiwiEntity rival) {
        boolean ownYield = this.kiwi.shouldYieldFight();
        boolean rivalYield = rival.shouldYieldFight();
        boolean timedOut = this.kiwi.level().getGameTime() - this.kiwi.getConflictStartTime()
                >= KiwiDefinition.MAX_FIGHT_TICKS;
        if (!this.hasCompletedMinimumBrawl() && !timedOut) {
            return null;
        }
        if (!ownYield && !rivalYield && !timedOut) {
            return null;
        }
        return this.kiwi.fightResolutionScore() <= rival.fightResolutionScore() ? this.kiwi : rival;
    }

    private boolean hasCompletedMinimumBrawl() {
        return this.kiwi.getConflictTicks() <= KiwiDefinition.MAX_FIGHT_TICKS - MIN_BRAWL_TICKS;
    }

    private void resolveWinner(KiwiEntity winner, KiwiEntity loser) {
        winner.setConflictState(KiwiConflictState.CHASING, 160 + winner.getRandom().nextInt(61));
        loser.setConflictState(KiwiConflictState.FLEEING, 320 + loser.getRandom().nextInt(121));
        loser.rememberDefeatBy(winner);
        winner.getNavigation().stop();
        loser.getNavigation().stop();
    }

    private KiwiEntity findIntruder() {
        BlockPos home = this.kiwi.getHomeCenter();
        if (home == null) {
            return null;
        }
        double radius = KiwiDefinition.CORE_TERRITORY_RADIUS;
        AABB area = new AABB(home).inflate(radius, 4.0D, radius);
        List<KiwiEntity> candidates = this.kiwi.level().getEntitiesOfClass(
                KiwiEntity.class,
                area,
                candidate -> candidate != this.kiwi
                        && this.canStartConflict(candidate)
                        && !this.kiwi.isAvoiding(candidate)
                        && !candidate.isAvoiding(this.kiwi)
                        && candidate.getHomeCenter() != null
                        && candidate.getHomeCenter().distSqr(home) >= HOME_SEPARATION_SQR);
        if (candidates.isEmpty()) {
            return null;
        }

        long crowding = candidates.stream()
                .filter(candidate -> home.distSqr(candidate.blockPosition()) <= radius * radius)
                .count();
        return candidates.stream()
                .filter(candidate -> this.conflictScore(candidate, crowding) >= 0.60F)
                .min(Comparator.comparingDouble(this.kiwi::distanceToSqr))
                .orElse(null);
    }

    private float conflictScore(KiwiEntity rival, long crowding) {
        BlockPos home = this.kiwi.getHomeCenter();
        double distance = Math.sqrt(home.distSqr(rival.blockPosition()));
        float intrusion = (float) Math.max(0.0D,
                1.0D - distance / KiwiDefinition.CORE_TERRITORY_RADIUS);
        float aggression = this.kiwi.territorialWillingness(rival);
        float crowdingScore = Math.min(1.0F, crowding / 3.0F);
        float score = intrusion * 0.55F + aggression * 0.30F + crowdingScore * 0.15F;
        score += (this.kiwi.getRandom().nextFloat() - 0.5F) * 0.12F;
        return score;
    }

    private boolean canStartConflict(KiwiEntity bird) {
        return bird.isAlive()
                && bird.isActiveTime()
                && !bird.isConflictActive()
                && !bird.isBirdSleeping()
                && bird.getBehaviorState() != KiwiBehaviorState.GROUND_ESCAPE
                && bird.hurtTime <= 0
                && bird.birdBrain().motivation().hunger() < 0.92F;
    }

    private void lookAt(KiwiEntity rival) {
        this.kiwi.getLookControl().setLookAt(rival, 45.0F, this.kiwi.getMaxHeadXRot());
        double offsetX = rival.getX() - this.kiwi.getX();
        double offsetZ = rival.getZ() - this.kiwi.getZ();
        if (offsetX * offsetX + offsetZ * offsetZ < 1.0E-6D) {
            return;
        }
        float targetYaw = (float) (Mth.atan2(offsetZ, offsetX) * Mth.RAD_TO_DEG) - 90.0F;
        float facingYaw = Mth.approachDegrees(this.kiwi.getYRot(), targetYaw, 35.0F);
        this.kiwi.setYRot(facingYaw);
        this.kiwi.setYBodyRot(facingYaw);
        this.kiwi.setYHeadRot(facingYaw);
    }

    private void lookAwayFrom(KiwiEntity rival) {
        Vec3 away = this.kiwi.position().subtract(rival.position());
        this.kiwi.getLookControl().setLookAt(
                this.kiwi.getX() + away.x,
                this.kiwi.getEyeY(),
                this.kiwi.getZ() + away.z,
                45.0F,
                this.kiwi.getMaxHeadXRot());
    }
}
