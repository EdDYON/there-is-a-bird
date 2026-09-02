package EdDYON.guaniao.content.bird.myna;

import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.BirdSleepWakeable;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.brain.BirdIntent;
import EdDYON.guaniao.content.bird.flight.BirdFlightController;
import EdDYON.guaniao.content.bird.flight.BirdFlightProfile;
import EdDYON.guaniao.content.bird.flock.BirdFlockManager;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.content.bird.sparrow.SparrowBehaviorState;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.content.bird.species.MynaProfile;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Crested myna using the shared small-bird flight foundation with its own
 * server-authoritative decorative, vocal and sleep state machines.
 */
public class MynaEntity extends SparrowEntity implements FlyingAnimal, BirdSleepWakeable {
    private static final EntityDataAccessor<Integer> ACTION_STATE =
            SynchedEntityData.defineId(MynaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VOCAL_STATE =
            SynchedEntityData.defineId(MynaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CURRENT_MIMIC_CUE =
            SynchedEntityData.defineId(MynaEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation FLY_ANIMATION = RawAnimation.begin().thenLoop("animation.fly");
    private static final RawAnimation IDLE_1_ANIMATION = RawAnimation.begin().thenPlay("animation.idle_diff_1");
    private static final RawAnimation IDLE_2_ANIMATION = RawAnimation.begin().thenPlay("animation.idle_diff_2");
    private static final RawAnimation SPEAK_START_ANIMATION = RawAnimation.begin().thenPlay("animation.speak");
    // These source animations use Molang-driven poses with no numeric length. They must remain explicit loops.
    private static final RawAnimation SPEAK_LOOP_ANIMATION = RawAnimation.begin().thenLoop("animation.speak_loop");
    private static final RawAnimation SPEAK_END_ANIMATION = RawAnimation.begin().thenPlay("animation.speak2");
    // The bbmodel marks sleep as looped, but its semantic role is a one-shot entrance.
    private static final RawAnimation SLEEP_ENTER_ANIMATION = RawAnimation.begin().thenPlay("animation.sleep");
    private static final RawAnimation SLEEP_LOOP_ANIMATION = RawAnimation.begin().thenLoop("animation.sleep_loop");

    private static final int IDLE_1_TICKS = 60;
    private static final int IDLE_2_TICKS = 55;
    private static final int SPEAK_START_TICKS = 7;
    private static final int SPEAK_END_TICKS = 7;
    private static final int MIN_SPEAK_COOLDOWN = 520;
    private static final int MAX_SPEAK_COOLDOWN = 1500;
    private static final int MIN_PEAK_SPEAK_COOLDOWN = 240;
    private static final int MAX_PEAK_SPEAK_COOLDOWN = 620;
    private static final float MIMIC_RECALL_CHANCE = 0.42F;
    private static final int SLEEP_ENTER_TICKS = 15;
    private static final int MIMIC_SHOWCASE_GAP_TICKS = 12;
    public static final double MIMIC_LEARNING_RANGE = 16.0D;

    private static final NativeCall[] NATIVE_CALLS = {
            new NativeCall(GuaniaoSoundEvents.MYNA_CALL_03, 19),
            new NativeCall(GuaniaoSoundEvents.MYNA_CALL_04, 44),
            new NativeCall(GuaniaoSoundEvents.MYNA_CALL_05, 31),
            new NativeCall(GuaniaoSoundEvents.MYNA_CALL_07, 33),
            new NativeCall(GuaniaoSoundEvents.MYNA_CALL_08, 24),
            new NativeCall(GuaniaoSoundEvents.MYNA_CALL_10, 83),
            new NativeCall(GuaniaoSoundEvents.MYNA_CALL_11, 48),
            new NativeCall(GuaniaoSoundEvents.MYNA_CALL_12, 19),
            new NativeCall(GuaniaoSoundEvents.MYNA_CALL_13, 44)
    };

    private final MynaMimicMemory mimicMemory = new MynaMimicMemory();
    private int actionTicks;
    private int vocalTicks;
    private int idleCooldown;
    private int vocalCooldown;
    private int restInterruptionTicks;
    private boolean mimicShowcase;
    private int mimicShowcaseCueIndex;
    private int mimicShowcaseGapTicks;
    @Nullable
    private MynaMimicVoice.Phrase activeMimicPhrase;
    private int mimicPhraseTick;
    private int mimicPhraseStepIndex;
    private GuidePreviewAnimation guidePreviewAnimation = GuidePreviewAnimation.NONE;

    public MynaEntity(EntityType<? extends MynaEntity> entityType, Level level) {
        super(entityType, level, MynaProfile.INSTANCE);
        this.idleCooldown = this.randomBetween(220, 620);
        this.vocalCooldown = this.nextVocalCooldown();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MynaDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MynaDefinition.WALK_SPEED)
                .add(Attributes.FLYING_SPEED, MynaDefinition.FLYING_SPEED)
                .add(Attributes.FOLLOW_RANGE, MynaDefinition.FOLLOW_RANGE);
    }

    public static boolean canMynaSpawn(EntityType<MynaEntity> entityType, ServerLevelAccessor level,
                                       MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean validGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(Blocks.FARMLAND)
                || below.is(Blocks.HAY_BLOCK)
                || below.getBlock() instanceof FenceBlock
                || below.getBlock() instanceof FenceGateBlock;
        if (!validGround || !level.getFluidState(pos).isEmpty() || level.getRawBrightness(pos, 0) <= 7) {
            return false;
        }
        boolean openSky = level.canSeeSky(pos);
        int treeScore = 0;
        int foodScore = 0;
        int settlementScore = 0;
        int perchScore = 0;
        int originChunkX = pos.getX() >> 4;
        int originChunkZ = pos.getZ() >> 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -6; x <= 6; ++x) {
            for (int z = -6; z <= 6; ++z) {
                if (x * x + z * z > 36) {
                    continue;
                }
                for (int y = -2; y <= 3; ++y) {
                    cursor.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if ((cursor.getX() >> 4) != originChunkX || (cursor.getZ() >> 4) != originChunkZ) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                        treeScore += 2;
                        ++perchScore;
                    }
                    if (state.is(Blocks.FARMLAND) || state.is(Blocks.HAY_BLOCK)
                            || state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)
                            || state.is(Blocks.SWEET_BERRY_BUSH)
                            || state.getBlock() instanceof CropBlock
                            || state.getBlock() instanceof ComposterBlock) {
                        foodScore += 2;
                    }
                    if (state.getBlock() instanceof FenceBlock
                            || state.getBlock() instanceof FenceGateBlock
                            || state.getBlock() instanceof WallBlock
                            || state.getBlock() instanceof SignBlock) {
                        perchScore += 2;
                        ++settlementScore;
                    }
                    if (state.getBlock() instanceof SignBlock
                            || state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                            || state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
                        settlementScore += 3;
                    }
                    if (state.is(Blocks.FARMLAND) || state.is(Blocks.HAY_BLOCK)
                            || state.getBlock() instanceof CropBlock
                            || state.getBlock() instanceof ComposterBlock) {
                        settlementScore += 2;
                    }
                }
            }
        }
        boolean hasFood = foodScore >= 4 || treeScore >= 12;
        boolean hasHighPerch = perchScore >= 3;
        boolean settlementOrFarmEdge = settlementScore >= 7;
        boolean openWoodlandEdge = openSky && treeScore >= 8 && treeScore <= 120;
        if (!hasFood || !hasHighPerch || (!settlementOrFarmEdge && !openWoodlandEdge)) {
            return false;
        }
        float chance = settlementOrFarmEdge ? 0.82F : 0.56F;
        return random.nextFloat() < chance;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new MynaSleepGoal(this));
        this.goalSelector.addGoal(2, new MynaActionLockGoal(this));
        this.goalSelector.addGoal(8, new MynaFlockGoal(this));
        this.goalSelector.addGoal(9, new MynaObservePlayerGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACTION_STATE, MynaActionState.NONE.ordinal());
        this.entityData.define(VOCAL_STATE, MynaVocalState.NONE.ordinal());
        this.entityData.define(CURRENT_MIMIC_CUE, -1);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }

        if (this.idleCooldown > 0) {
            --this.idleCooldown;
        }
        if (this.vocalCooldown > 0) {
            --this.vocalCooldown;
        }
        if (this.restInterruptionTicks > 0) {
            --this.restInterruptionTicks;
        }

        if (this.mimicShowcase) {
            this.tickMimicShowcase();
            return;
        }
        if (this.mustInterruptDecorativeState()) {
            this.interruptDecorativeState();
            return;
        }
        if (this.isBirdSleeping()) {
            this.tickSleepState();
            return;
        }
        if (this.getVocalState() != MynaVocalState.NONE) {
            this.tickSpeaking();
            return;
        }
        if (this.getActionState() == MynaActionState.IDLE_1
                || this.getActionState() == MynaActionState.IDLE_2) {
            if (--this.actionTicks <= 0) {
                this.setActionState(MynaActionState.NONE, 0);
                this.idleCooldown = this.randomBetween(280, 760);
            }
            return;
        }

        if (this.vocalCooldown <= 0) {
            if (this.canStartCalmAction()) {
                this.startSpeaking();
                return;
            }
            this.vocalCooldown = this.randomBetween(50, 110);
        }
        if (this.idleCooldown <= 0) {
            if (this.canStartCalmAction()) {
                this.startIdleVariation();
                return;
            }
            this.idleCooldown = this.randomBetween(35, 90);
        }
    }

    private boolean mustInterruptDecorativeState() {
        return this.hurtTime > 0
                || this.isInWaterOrBubble()
                || this.getTarget() != null
                || this.isBirdFlightActive()
                || (!this.onGround() && Math.abs(this.getDeltaMovement().y) > 0.04D);
    }

    private void interruptDecorativeState() {
        if (this.isBirdSleeping()) {
            this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 240);
        }
        this.setActionState(MynaActionState.NONE, 0);
        this.setVocalState(MynaVocalState.NONE, 0);
        this.idleCooldown = Math.max(this.idleCooldown, 120);
        this.vocalCooldown = Math.max(this.vocalCooldown, 240);
    }

    private boolean canStartCalmAction() {
        SparrowBehaviorState movementState = this.getBehaviorState();
        BirdIntent intent = this.birdBrain().currentIntent();
        return this.onGround()
                && this.getNavigation().isDone()
                && !this.isInWaterOrBubble()
                && this.hurtTime <= 0
                && this.getTarget() == null
                && this.getActionState() == MynaActionState.NONE
                && this.getVocalState() == MynaVocalState.NONE
                && (movementState == SparrowBehaviorState.IDLE || movementState == SparrowBehaviorState.LOOK_AROUND)
                && (intent == BirdIntent.IDLE || intent == BirdIntent.WATCH)
                && BirdActivitySchedule.DIURNAL.isActiveTime(this.level().getDayTime())
                && this.birdBrain().computeRiskScore() < 0.53F;
    }

    private void startIdleVariation() {
        this.getNavigation().stop();
        if (this.getRandom().nextBoolean()) {
            this.setActionState(MynaActionState.IDLE_1, IDLE_1_TICKS);
        } else {
            this.setActionState(MynaActionState.IDLE_2, IDLE_2_TICKS);
        }
    }

    private void startSpeaking() {
        MynaMimicCue cue = this.getRandom().nextFloat() < MIMIC_RECALL_CHANCE
                ? this.mimicMemory.selectForRecall(this.getRandom(), this.level().getGameTime())
                : null;
        this.startSpeaking(cue);
    }

    private void startSpeaking(@Nullable MynaMimicCue cue) {
        this.getNavigation().stop();
        this.setActionState(MynaActionState.NONE, 0);
        this.setCurrentMimicCue(cue);
        this.setVocalState(MynaVocalState.STARTING, SPEAK_START_TICKS);
    }

    private void tickMimicShowcase() {
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.getVocalState() != MynaVocalState.NONE) {
            this.tickSpeaking();
            return;
        }
        if (this.mimicShowcaseGapTicks > 0) {
            --this.mimicShowcaseGapTicks;
            return;
        }

        MynaMimicCue[] cues = MynaMimicCue.values();
        if (this.mimicShowcaseCueIndex >= cues.length) {
            this.mimicShowcase = false;
            this.vocalCooldown = this.nextVocalCooldown();
            return;
        }
        this.startSpeaking(cues[this.mimicShowcaseCueIndex++]);
    }

    private void tickSpeaking() {
        this.getNavigation().stop();
        MynaVocalState state = this.getVocalState();
        if (state == MynaVocalState.SPEAKING) {
            this.tickMimicPhrase();
        }
        if (--this.vocalTicks > 0) {
            return;
        }
        switch (state) {
            case STARTING -> {
                MynaMimicCue cue = this.getCurrentMimicCue();
                NativeCall nativeCall = null;
                MynaMimicVoice.Phrase phrase = null;
                int baseDuration;
                if (cue == null) {
                    nativeCall = NATIVE_CALLS[this.getRandom().nextInt(NATIVE_CALLS.length)];
                    baseDuration = nativeCall.durationTicks();
                } else {
                    phrase = MynaMimicVoice.imitation(
                            cue,
                            this.mimicMemory.rememberedPitch(cue),
                            this.mimicMemory.rememberedTempo(cue),
                            this.getRandom());
                    baseDuration = cue.randomVocalTicks(this.getRandom());
                }
                this.setVocalState(
                        MynaVocalState.SPEAKING,
                        phrase == null ? baseDuration : Math.max(baseDuration, phrase.minimumDurationTicks()));
                this.activeMimicPhrase = phrase;
                this.mimicPhraseTick = 0;
                this.mimicPhraseStepIndex = 0;
                if (nativeCall != null) {
                    nativeCall.play(this);
                }
            }
            case SPEAKING -> {
                MynaMimicCue cue = this.getCurrentMimicCue();
                if (cue != null) {
                    this.mimicMemory.markRecalled(cue, this.level().getGameTime());
                }
                this.setVocalState(MynaVocalState.ENDING, SPEAK_END_TICKS);
            }
            case ENDING -> {
                this.setVocalState(MynaVocalState.NONE, 0);
                if (this.mimicShowcase) {
                    this.mimicShowcaseGapTicks = MIMIC_SHOWCASE_GAP_TICKS;
                    this.vocalCooldown = 0;
                } else {
                    this.vocalCooldown = this.nextVocalCooldown();
                }
                this.idleCooldown = Math.max(this.idleCooldown, 160);
            }
            default -> this.setVocalState(MynaVocalState.NONE, 0);
        }
    }

    private void tickMimicPhrase() {
        if (this.activeMimicPhrase == null) {
            return;
        }
        List<MynaMimicVoice.Syllable> syllables = this.activeMimicPhrase.syllables();
        while (this.mimicPhraseStepIndex < syllables.size()) {
            MynaMimicVoice.Syllable syllable = syllables.get(this.mimicPhraseStepIndex);
            if (syllable.tick() > this.mimicPhraseTick) {
                break;
            }
            syllable.play(this);
            ++this.mimicPhraseStepIndex;
        }
        ++this.mimicPhraseTick;
    }

    private void tickSleepState() {
        this.getNavigation().stop();
        this.setVocalState(MynaVocalState.NONE, 0);
        if (this.getActionState() == MynaActionState.ENTERING_SLEEP && --this.actionTicks <= 0) {
            this.setActionState(MynaActionState.SLEEPING, 0);
            return;
        }
        if (this.getActionState() == MynaActionState.SLEEPING && this.tickCount % 80 == 0) {
            this.birdBrain().onRest(0.03F);
        }
    }

    private void beginSleep() {
        this.getNavigation().stop();
        this.setVocalState(MynaVocalState.NONE, 0);
        this.setActionState(MynaActionState.ENTERING_SLEEP, SLEEP_ENTER_TICKS);
    }

    private void setActionState(MynaActionState state, int ticks) {
        this.entityData.set(ACTION_STATE, state.ordinal());
        this.actionTicks = Math.max(0, ticks);
    }

    private void setVocalState(MynaVocalState state, int ticks) {
        this.entityData.set(VOCAL_STATE, state.ordinal());
        this.vocalTicks = Math.max(0, ticks);
        if (state != MynaVocalState.SPEAKING) {
            this.activeMimicPhrase = null;
            this.mimicPhraseTick = 0;
            this.mimicPhraseStepIndex = 0;
        }
        if (state == MynaVocalState.NONE) {
            this.setCurrentMimicCue(null);
        }
    }

    public MynaActionState getActionState() {
        return MynaActionState.byId(this.entityData.get(ACTION_STATE));
    }

    public MynaVocalState getVocalState() {
        return MynaVocalState.byId(this.entityData.get(VOCAL_STATE));
    }

    @Nullable
    public MynaMimicCue getCurrentMimicCue() {
        return MynaMimicCue.byNetworkId(this.entityData.get(CURRENT_MIMIC_CUE));
    }

    public int getLearnedMimicCount() {
        return this.mimicMemory.learnedCount();
    }

    public boolean knowsMimicCue(MynaMimicCue cue) {
        return this.mimicMemory.knows(cue);
    }

    public int getMimicLearningProgress(MynaMimicCue cue) {
        return this.mimicMemory.learningProgress(cue);
    }

    /**
     * Records a learnable sound if this bird is awake, calm, in range and not
     * separated from the source by a solid wall.
     */
    public MynaMimicMemory.LearningResult hearMimicCue(MynaMimicCue cue, Vec3 soundPosition) {
        return this.hearMimicCue(cue, soundPosition, 1.0F);
    }

    public MynaMimicMemory.LearningResult hearMimicCue(
            MynaMimicCue cue, Vec3 soundPosition, float sourcePitch) {
        if (this.level().isClientSide || cue == null || soundPosition == null
                || !this.canLearnMimicCue(soundPosition)) {
            return MynaMimicMemory.LearningResult.IGNORED;
        }
        return this.mimicMemory.observe(cue, this.level().getGameTime(), sourcePitch);
    }

    private boolean canLearnMimicCue(Vec3 soundPosition) {
        boolean trustedCompanion = this.isTame();
        if (!this.isAlive()
                || this.isBirdSleeping()
                || this.isInWaterOrBubble()
                || this.hurtTime > 0
                || this.getTarget() != null
                || this.getActionState() != MynaActionState.NONE
                || this.getVocalState() != MynaVocalState.NONE
                || (!trustedCompanion && (this.isBirdFlightActive() || !this.onGround()))
                || this.birdBrain().computeRiskScore() >= (trustedCompanion ? 0.90F : 0.62F)
                || this.distanceToSqr(soundPosition) > MIMIC_LEARNING_RANGE * MIMIC_LEARNING_RANGE) {
            return false;
        }

        Vec3 eyePosition = this.getEyePosition();
        HitResult obstruction = this.level().clip(
                new ClipContext(
                        eyePosition,
                        soundPosition,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        this));
        return obstruction.getType() == HitResult.Type.MISS
                || obstruction.getLocation().distanceToSqr(soundPosition) <= 1.0D;
    }

    private void setCurrentMimicCue(@Nullable MynaMimicCue cue) {
        this.entityData.set(CURRENT_MIMIC_CUE, cue == null ? -1 : cue.networkId());
    }

    @Override
    protected boolean blocksSparrowDecorativeBehavior() {
        return this.getActionState() != MynaActionState.NONE || this.getVocalState() != MynaVocalState.NONE;
    }

    @Override
    protected boolean usesSparrowFlockGoal() {
        return false;
    }

    @Override
    public boolean canFlockWith(Entity other) {
        return other != null
                && (other.getType() == GuaniaoEntityTypes.MYNA.get()
                || other.getType() == GuaniaoEntityTypes.SPARROW.get());
    }

    @Override
    protected boolean isPreferredPerchBase(BlockState state) {
        return super.isPreferredPerchBase(state)
                || state.getBlock() instanceof SignBlock
                || state.getBlock() instanceof WallBlock
                || state.getBlock() instanceof StairBlock
                || state.getBlock() instanceof SlabBlock;
    }

    @Override
    protected double speciesPerchPreferenceScore(BlockPos pos, BlockState below, boolean roosting) {
        if (below.getBlock() instanceof SignBlock) {
            return roosting ? 30.0D : 34.0D;
        }
        if (below.getBlock() instanceof FenceBlock || below.getBlock() instanceof FenceGateBlock) {
            return 13.0D;
        }
        if (below.getBlock() instanceof WallBlock) {
            return roosting ? 23.0D : 26.0D;
        }
        if (below.getBlock() instanceof StairBlock || below.getBlock() instanceof SlabBlock) {
            return roosting ? 18.0D : 21.0D;
        }
        return 0.0D;
    }

    @Override
    protected TagKey<Item> foodTag() {
        return BirdTags.MYNA_FOODS;
    }

    @Override
    public MynaEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
        MynaEntity child = GuaniaoEntityTypes.MYNA.get().create(level);
        if (child != null) {
            float mateScale = mate instanceof MynaEntity other
                    ? other.getIndividualModelScale()
                    : this.getIndividualModelScale();
            child.setIndividualModelScale(BirdModelScale.inheritIndividualScale(
                    child.getRandom(), this.getIndividualModelScale(), mateScale, child.modelScaleProfile()));
        }
        return child;
    }

    @Override
    public BirdFlightProfile birdFlightProfile() {
        return BirdFlightProfile.MYNA;
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.MYNA;
    }

    @Override
    public boolean isFlying() {
        return this.isBirdFlightActive() || (!this.onGround() && !this.isInWaterOrBubble());
    }

    @Override
    public boolean isBirdSleeping() {
        MynaActionState state = this.getActionState();
        return state == MynaActionState.ENTERING_SLEEP || state == MynaActionState.SLEEPING;
    }

    @Override
    public void wakeFromLoudSound(Vec3 soundPosition) {
        if (this.level().isClientSide) {
            return;
        }
        if (this.isBirdSleeping() && soundPosition != null) {
            this.getLookControl().setLookAt(soundPosition.x, soundPosition.y, soundPosition.z,
                    35.0F, this.getMaxHeadXRot());
        }
        this.restInterruptionTicks = Math.max(this.restInterruptionTicks, 240);
        this.setActionState(MynaActionState.NONE, 0);
        this.setVocalState(MynaVocalState.NONE, 0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            Vec3 sourcePosition = source.getEntity() == null ? this.position() : source.getEntity().position();
            this.wakeFromLoudSound(sourcePosition);
        }
        return hurt;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.mimicMemory.save(tag);
        tag.putBoolean("MynaMimicShowcase", this.mimicShowcase);
        tag.putInt("MynaMimicShowcaseIndex", this.mimicShowcaseCueIndex);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.mimicMemory.load(tag);
        this.mimicShowcase = tag.getBoolean("MynaMimicShowcase");
        this.mimicShowcaseCueIndex = Mth.clamp(
                tag.getInt("MynaMimicShowcaseIndex"), 0, MynaMimicCue.values().length);
        this.mimicShowcaseGapTicks = 0;
        this.setActionState(MynaActionState.NONE, 0);
        this.setVocalState(MynaVocalState.NONE, 0);
        this.idleCooldown = this.randomBetween(120, 520);
        this.vocalCooldown = this.mimicShowcase
                ? 0 : this.nextVocalCooldown();
    }

    public void setGuidePreviewAnimation(GuidePreviewAnimation animation) {
        this.guidePreviewAnimation = animation == null ? GuidePreviewAnimation.NONE : animation;
    }

    private boolean shouldPlayFlyAnimation() {
        return BirdFlightController.shouldPlayFlyAnimation(
                this, this.isBirdFlightActive(), this.onGround(), this.isNoGravity(),
                this.getDeltaMovement(), 6);
    }

    private <T extends MynaEntity> PlayState movementController(AnimationState<T> animationState) {
        animationState.getController().setAnimationSpeed(1.0D);
        RawAnimation preview = this.guidePreviewAnimation.animation();
        if (preview != null) {
            return animationState.setAndContinue(preview);
        }
        MynaActionState action = this.getActionState();
        if (action == MynaActionState.ENTERING_SLEEP) {
            return animationState.setAndContinue(SLEEP_ENTER_ANIMATION);
        }
        if (action == MynaActionState.SLEEPING) {
            return animationState.setAndContinue(SLEEP_LOOP_ANIMATION);
        }
        MynaVocalState vocal = this.getVocalState();
        if (vocal == MynaVocalState.STARTING) {
            return animationState.setAndContinue(SPEAK_START_ANIMATION);
        }
        if (vocal == MynaVocalState.SPEAKING) {
            return animationState.setAndContinue(SPEAK_LOOP_ANIMATION);
        }
        if (vocal == MynaVocalState.ENDING) {
            return animationState.setAndContinue(SPEAK_END_ANIMATION);
        }
        if (this.shouldPlayFlyAnimation()) {
            return animationState.setAndContinue(FLY_ANIMATION);
        }
        if (action == MynaActionState.IDLE_1) {
            return animationState.setAndContinue(IDLE_1_ANIMATION);
        }
        if (action == MynaActionState.IDLE_2) {
            return animationState.setAndContinue(IDLE_2_ANIMATION);
        }
        if (BirdGroundAnimation.hasWalkMotion(this, animationState.isMoving())) {
            animationState.getController().setAnimationSpeed(BirdGroundAnimation.walkAnimationSpeed(this));
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{
                new AnimationController((GeoAnimatable)this, "movement", 4, this::movementController)
        });
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void playAmbientSound() {
        // Natural calls are driven by the synced vocal state so sound and beak animation stay aligned.
    }

    public void startAudienceCheerCall() {
        if (!this.level().isClientSide
                && !this.isBirdSleeping()
                && this.getVocalState() == MynaVocalState.NONE) {
            this.startSpeaking(null);
        }
    }

    private int randomBetween(int min, int max) {
        return min + this.getRandom().nextInt(Math.max(1, max - min + 1));
    }

    private int nextVocalCooldown() {
        long dayTime = Math.floorMod(this.level().getDayTime(), 24000L);
        boolean dawnOrDusk = dayTime < 1800L || dayTime >= 10800L && dayTime <= 13200L;
        return dawnOrDusk
                ? this.randomBetween(MIN_PEAK_SPEAK_COOLDOWN, MAX_PEAK_SPEAK_COOLDOWN)
                : this.randomBetween(MIN_SPEAK_COOLDOWN, MAX_SPEAK_COOLDOWN);
    }

    private record NativeCall(Supplier<SoundEvent> sound, int durationTicks) {
        private void play(MynaEntity myna) {
            myna.level().playSound(
                    null,
                    myna,
                    this.sound.get(),
                    SoundSource.NEUTRAL,
                    0.82F,
                    0.97F + myna.getRandom().nextFloat() * 0.06F);
        }
    }

    public enum GuidePreviewAnimation {
        NONE(null),
        IDLE(IDLE_ANIMATION),
        WALK(WALK_ANIMATION),
        FLY(FLY_ANIMATION),
        IDLE_1(IDLE_1_ANIMATION),
        IDLE_2(IDLE_2_ANIMATION),
        SPEAK(RawAnimation.begin().thenPlay("animation.speak").thenLoop("animation.speak_loop")),
        SLEEP(RawAnimation.begin().thenPlay("animation.sleep").thenLoop("animation.sleep_loop"));

        @Nullable
        private final RawAnimation animation;

        GuidePreviewAnimation(@Nullable RawAnimation animation) {
            this.animation = animation;
        }

        @Nullable
        private RawAnimation animation() {
            return this.animation;
        }
    }

    private static final class MynaActionLockGoal extends Goal {
        private final MynaEntity myna;

        private MynaActionLockGoal(MynaEntity myna) {
            this.myna = myna;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.myna.blocksSparrowDecorativeBehavior() && !this.myna.isBirdSleeping();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.myna.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.myna.getNavigation().stop();
        }
    }

    private static final class MynaSleepGoal extends Goal {
        private final MynaEntity myna;

        private MynaSleepGoal(MynaEntity myna) {
            this.myna = myna;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.myna.restInterruptionTicks > 0
                    || this.myna.isBirdSleeping()
                    || this.myna.isBirdFlightActive()
                    || !this.myna.onGround()
                    || !this.myna.getNavigation().isDone()
                    || this.myna.getTarget() != null
                    || this.myna.hurtTime > 0
                    || this.myna.isInWaterOrBubble()
                    || !BirdActivitySchedule.DIURNAL.isRestTime(this.myna.level().getDayTime())) {
                return false;
            }
            return this.myna.birdBrain().computeRiskScore() < 0.56F
                    && this.myna.getRandom().nextInt(60) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.myna.isBirdSleeping()
                    && this.myna.restInterruptionTicks <= 0
                    && this.myna.onGround()
                    && this.myna.getTarget() == null
                    && this.myna.hurtTime <= 0
                    && !this.myna.isInWaterOrBubble()
                    && BirdActivitySchedule.DIURNAL.isRestTime(this.myna.level().getDayTime());
        }

        @Override
        public void start() {
            this.myna.beginSleep();
        }

        @Override
        public void tick() {
            this.myna.getNavigation().stop();
        }

        @Override
        public void stop() {
            if (this.myna.isBirdSleeping()) {
                this.myna.wakeFromLoudSound(this.myna.position());
            }
        }
    }

    /** Makes a calm myna pause and visibly inspect an approaching player. */
    private static final class MynaObservePlayerGoal extends Goal {
        private final MynaEntity myna;
        @Nullable
        private Player player;
        private int observeTicks;

        private MynaObservePlayerGoal(MynaEntity myna) {
            this.myna = myna;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.myna.canStartCalmAction() || this.myna.getRandom().nextInt(90) != 0) {
                return false;
            }
            Player nearest = this.myna.level().getNearestPlayer(this.myna, 8.0D);
            if (nearest == null || !nearest.isAlive() || nearest.isSpectator()
                    || nearest.isSprinting() || !this.myna.hasLineOfSight(nearest)) {
                return false;
            }
            this.player = nearest;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.player != null
                    && this.player.isAlive()
                    && !this.player.isSpectator()
                    && this.observeTicks > 0
                    && this.myna.distanceToSqr(this.player) <= 64.0D
                    && !this.myna.mustInterruptDecorativeState()
                    && (this.myna.getActionState() == MynaActionState.IDLE_1
                    || this.myna.getActionState() == MynaActionState.IDLE_2);
        }

        @Override
        public void start() {
            this.observeTicks = 42 + this.myna.getRandom().nextInt(19);
            this.myna.getNavigation().stop();
            this.myna.setActionState(
                    this.myna.getRandom().nextBoolean() ? MynaActionState.IDLE_1 : MynaActionState.IDLE_2,
                    this.observeTicks);
        }

        @Override
        public void tick() {
            --this.observeTicks;
            if (this.player != null) {
                this.myna.getLookControl().setLookAt(
                        this.player, 28.0F, this.myna.getMaxHeadXRot());
            }
        }

        @Override
        public void stop() {
            this.player = null;
            this.observeTicks = 0;
        }
    }

    private static final class MynaFlockGoal extends Goal {
        private final MynaEntity myna;
        @Nullable
        private Vec3 target;

        private MynaFlockGoal(MynaEntity myna) {
            this.myna = myna;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.myna.isTame() || this.myna.blocksSparrowDecorativeBehavior()
                    || this.myna.isBirdFlightActive() || !this.myna.onGround()) {
                return false;
            }
            float sociability = this.myna.birdBrain().personality().sociability();
            int chance = Mth.clamp((int)(62.0F - sociability * 48.0F), 10, 62);
            if (this.myna.getRandom().nextInt(chance) != 0) {
                return false;
            }
            List<Entity> flock = BirdFlockManager.nearby(
                    this.myna, Entity.class, MynaDefinition.SOCIAL_RADIUS)
                    .stream()
                    .filter(other -> other != this.myna)
                    .filter(other -> other instanceof SparrowEntity bird
                            && !bird.isTame() && bird.onGround())
                    .toList();
            if (flock.isEmpty()) {
                return false;
            }
            Vec3 center = Vec3.ZERO;
            for (Entity other : flock) {
                center = center.add(other.position());
            }
            center = center.scale(1.0D / flock.size());
            if (this.myna.position().distanceToSqr(center) < 6.25D) {
                return false;
            }
            this.target = center;
            return true;
        }

        @Override
        public void start() {
            if (this.target != null) {
                this.myna.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, 0.86D);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && !this.myna.getNavigation().isDone()
                    && !this.myna.blocksSparrowDecorativeBehavior();
        }

        @Override
        public void stop() {
            this.target = null;
        }
    }
}
