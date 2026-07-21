package EdDYON.guaniao.content.bird.nightheron;

import java.util.EnumSet;
import java.util.Optional;
import EdDYON.guaniao.content.bird.nightheron.NightHeronBehaviorState;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.BirdTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NightHeronForagingGoal
extends Goal {
    private final NightHeronEntity nightHeron;
    private int remainingTicks;
    private int repositionCooldown;

    public NightHeronForagingGoal(NightHeronEntity nightHeron) {
        this.nightHeron = nightHeron;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        if (this.nightHeron.isEatingFish() || !this.nightHeron.birdBrain().wantsForage()) {
            return false;
        }
        int chance = this.nightHeron.level().isRaining() ? 7 : 12;
        return this.nightHeron.isActiveTime() && this.nightHeron.onGround() && this.nightHeron.isNearWater(this.nightHeron.blockPosition(), 4) && this.nightHeron.getTarget() == null && this.nightHeron.getRandom().nextInt(chance) == 0;
    }

    public boolean canContinueToUse() {
        if (this.nightHeron.birdBrain().motivation().fear() > NightHeronDefinition.FORAGING_STOP_FEAR_THRESHOLD) {
            return false;
        }
        if (this.nightHeron.birdBrain().computeRiskScore() > NightHeronDefinition.FORAGING_STOP_RISK_THRESHOLD) {
            return false;
        }
        return this.remainingTicks > 0 && !this.nightHeron.isEatingFish() && this.nightHeron.isActiveTime() && this.nightHeron.onGround() && !this.nightHeron.getBehaviorState().isEscape();
    }

    public void start() {
        this.remainingTicks = 120 + this.nightHeron.getRandom().nextInt(141);
        this.repositionCooldown = 0;
        this.nightHeron.setBehaviorState(NightHeronBehaviorState.FORAGING);
    }

    public void stop() {
        this.remainingTicks = 0;
        this.nightHeron.getNavigation().stop();
        if (!this.nightHeron.isEatingFish() && !this.nightHeron.getBehaviorState().isEscape()) {
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.IDLE);
        }
    }

    public void tick() {
        Optional<LivingEntity> prey;
        --this.remainingTicks;
        if (this.repositionCooldown > 0) {
            --this.repositionCooldown;
        }
        if ((prey = this.findPrey()).isPresent()) {
            this.stalkPrey(prey.get());
            return;
        }
        this.nightHeron.setBehaviorState(NightHeronBehaviorState.WATER_EDGE_WAIT);
        if (this.repositionCooldown <= 0 && this.nightHeron.getRandom().nextInt(26) == 0) {
            this.repositionNearWater();
            this.repositionCooldown = 35 + this.nightHeron.getRandom().nextInt(35);
        } else {
            this.nightHeron.getNavigation().stop();
        }
        if (this.nightHeron.getRandom().nextInt(90) == 0) {
            this.nightHeron.triggerNeckStretch();
        }
    }

    private Optional<LivingEntity> findPrey() {
        LivingEntity prey = this.nightHeron.birdBrain().senses().nearestPrey();
        if (prey == null || !prey.isAlive() || !this.isPrey(prey)
                || this.nightHeron.distanceToSqr(prey) > 49.0D) {
            return Optional.empty();
        }
        return Optional.of(prey);
    }

    private boolean isPrey(LivingEntity entity) {
        return entity.getType().is(BirdTags.NIGHT_HERON_PREY);
    }

    private void stalkPrey(LivingEntity prey) {
        this.nightHeron.setBehaviorState(NightHeronBehaviorState.FORAGING);
        this.nightHeron.getLookControl().setLookAt((Entity)prey, 30.0f, 30.0f);
        double distanceSqr = this.nightHeron.distanceToSqr((Entity)prey);
        if (distanceSqr > 4.41) {
            Vec3 stalkingPosition = this.findStalkingPosition(prey);
            if (stalkingPosition != null) {
                this.nightHeron.getNavigation().moveTo(stalkingPosition.x, stalkingPosition.y, stalkingPosition.z, 0.14);
            } else {
                this.nightHeron.getNavigation().stop();
            }
            return;
        }
        this.nightHeron.getNavigation().stop();
        if (this.nightHeron.canStrikePrey()) {
            this.nightHeron.triggerNeckStretch();
            if (prey instanceof AbstractFish fish && this.nightHeron.catchFish(fish)) {
                return;
            }
            boolean damaged = this.nightHeron.doHurtTarget((Entity)prey);
            if (damaged && !prey.isAlive()) {
                this.nightHeron.birdBrain().onEat(NightHeronDefinition.BRAIN_EAT_HUNGER_REDUCTION);
            }
            this.nightHeron.afterPreyStrike();
        }
    }

    private void repositionNearWater() {
        Vec3 best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int attempt = 0; attempt < 10; ++attempt) {
            float score;
            Vec3 candidate = LandRandomPos.getPos((PathfinderMob)this.nightHeron, (int)6, (int)3);
            if (candidate == null || !((score = this.nightHeron.getWalkTargetValue(BlockPos.containing((Position)candidate), (LevelReader)this.nightHeron.level())) > bestScore)) continue;
            best = candidate;
            bestScore = score;
        }
        if (best != null) {
            this.nightHeron.getNavigation().moveTo(best.x, best.y, best.z, 0.14);
        }
    }

    private Vec3 findStalkingPosition(LivingEntity prey) {
        Level level = this.nightHeron.level();
        BlockPos preyPos = prey.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        BlockPos bestPos = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int xOffset = -4; xOffset <= 4; ++xOffset) {
            for (int zOffset = -4; zOffset <= 4; ++zOffset) {
                if (xOffset * xOffset + zOffset * zOffset > 16) continue;
                for (int yOffset = -2; yOffset <= 2; ++yOffset) {
                    mutablePos.set(preyPos.getX() + xOffset, preyPos.getY() + yOffset, preyPos.getZ() + zOffset);
                    if (!this.isSafeStalkingPosition(level, (BlockPos)mutablePos)) continue;
                    double distanceToPrey = Vec3.atCenterOf((Vec3i)mutablePos).distanceTo(prey.position());
                    double distanceToHeron = Vec3.atCenterOf((Vec3i)mutablePos).distanceTo(this.nightHeron.position());
                    double score = -distanceToPrey * 1.35 - distanceToHeron * 0.18;
                    if (NightHeronEntity.isWaterEdge((LevelReader)level, (BlockPos)mutablePos)) {
                        score += 10.0;
                    }
                    if (this.nightHeron.isNearWater((BlockPos)mutablePos, 3)) {
                        score += 4.0;
                    }
                    if (!(score > bestScore)) continue;
                    bestScore = score;
                    bestPos = mutablePos.immutable();
                }
            }
        }
        return bestPos == null ? null : Vec3.atBottomCenterOf(bestPos);
    }

    private boolean isSafeStalkingPosition(Level level, BlockPos pos) {
        if (!NightHeronEntity.canReadChunk((LevelReader)level, pos)) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        if (!feet.getCollisionShape((BlockGetter)level, pos).isEmpty() || !head.getCollisionShape((BlockGetter)level, pos.above()).isEmpty()) {
            return false;
        }
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }
        return below.isFaceSturdy((BlockGetter)level, pos.below(), Direction.UP) || below.is(Blocks.MUD) || below.is(Blocks.CLAY) || below.is(Blocks.SAND) || below.is(Blocks.RED_SAND);
    }
}
