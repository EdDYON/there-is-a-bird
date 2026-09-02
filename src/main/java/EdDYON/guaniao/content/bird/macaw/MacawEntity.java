package EdDYON.guaniao.content.bird.macaw;

import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.BirdActivitySchedule;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarBehaviorState;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.cockatiel.CockatielEntity;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;
import java.util.List;

public class MacawEntity extends BudgerigarEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation PREEN_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_1").thenLoop("idle");
    private static final RawAnimation IDLE_TWO_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_2").thenLoop("idle");
    private static final RawAnimation DANCE_ANIMATION = RawAnimation.begin().thenLoop("idle_diff_3");
    private static final RawAnimation IDLE_FOUR_ANIMATION = RawAnimation.begin().thenPlay("idle_diff_4").thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation FLY_ANIMATION = RawAnimation.begin().thenLoop("fly_flapping_wing_loop");
    private static final RawAnimation EAT_ANIMATION = RawAnimation.begin().thenPlay("eat").thenLoop("idle");
    private static final RawAnimation SLEEP_ANIMATION = RawAnimation.begin().thenPlay("sleep").thenLoop("sleep_loop");
    private GuidePreviewAnimation macawPreviewAnimation = GuidePreviewAnimation.NONE;
    private RawAnimation currentIdleAnimation = IDLE_ANIMATION;
    private long nextIdleAnimationTick;
    private int groundPreferenceTicks;
    private int mimicCooldown;

    public MacawEntity(EntityType<? extends MacawEntity> entityType, Level level) {
        super(entityType, level);
        this.mimicCooldown = 360 + this.getRandom().nextInt(640);
    }

    @Override
    public boolean canFlockWith(net.minecraft.world.entity.Entity other) {
        return other instanceof CockatielEntity || super.canFlockWith(other);
    }

    public static AttributeSupplier.Builder createMacawAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MacawDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MacawDefinition.WALK_SPEED)
                .add(Attributes.FLYING_SPEED, MacawDefinition.FLYING_SPEED)
                .add(Attributes.FOLLOW_RANGE, MacawDefinition.FOLLOW_RANGE);
    }

    public static boolean canMacawSpawn(EntityType<MacawEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean canopyOrForestFloor = below.is(BlockTags.LEAVES)
                || below.is(BlockTags.LOGS)
                || below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(Blocks.MOSS_BLOCK);
        return canopyOrForestFloor && level.getRawBrightness(pos, 0) > 7;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            this.tickHighPerchPreference();
            this.tickMimicry();
        }
    }

    @Nullable
    @Override
    public MacawEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
        MacawEntity child = GuaniaoEntityTypes.MACAW.get().create(level);
        if (child != null) {
            child.setSkinVariantForRendering(this.getRandom().nextInt(MacawDefinition.TEXTURE_VARIANTS.length));
            float mateScale = mate instanceof BudgerigarEntity other ? other.getIndividualModelScale() : this.getIndividualModelScale();
            child.setIndividualModelScale(BirdModelScale.inheritIndividualScale(
                    child.getRandom(), this.getIndividualModelScale(), mateScale, child.modelScaleProfile()));
        }
        return child;
    }

    @Override
    public ResourceLocation getTextureResource() {
        return MacawDefinition.textureForVariant(this.getSkinVariant());
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.MACAW;
    }

    @Override
    protected TagKey<Item> foodTag() {
        return BirdTags.MACAW_FOODS;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.MACAW_AMBIENT.get();
    }

    @Override
    protected SoundEvent getInteractionSound() {
        return GuaniaoSoundEvents.MACAW_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return GuaniaoSoundEvents.MACAW_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuaniaoSoundEvents.MACAW_DEATH.get();
    }

    @Override
    public float getSoundVolume() {
        return 0.82F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return BirdFlockSoundLimiter.scaledAmbientInterval(this, 150);
    }

    @Override
    public void setGuidePreviewAnimation(BudgerigarEntity.GuidePreviewAnimation animation) {
        this.macawPreviewAnimation = animation == null ? GuidePreviewAnimation.NONE : switch (animation) {
            case NONE -> GuidePreviewAnimation.NONE;
            case IDLE, CURIOUS -> GuidePreviewAnimation.IDLE;
            case PREEN -> GuidePreviewAnimation.PREEN;
            case DANCE -> GuidePreviewAnimation.DANCE;
            case EAT -> GuidePreviewAnimation.EAT;
            case SLEEP -> GuidePreviewAnimation.SLEEP;
            case WALK -> GuidePreviewAnimation.WALK;
            case FLY -> GuidePreviewAnimation.FLY;
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable) this, "movement", 5, this::movementController)});
    }

    private <T extends MacawEntity> PlayState movementController(AnimationState<T> animationState) {
        animationState.getController().setAnimationSpeed(1.0D);
        RawAnimation preview = this.macawPreviewAnimation.animation;
        if (preview != null) {
            return animationState.setAndContinue(preview);
        }
        BudgerigarBehaviorState state = this.getBehaviorState();
        if (state == BudgerigarBehaviorState.EATING) {
            return animationState.setAndContinue(EAT_ANIMATION);
        }
        if (this.shouldPlayFlyAnimation()) {
            return animationState.setAndContinue(FLY_ANIMATION);
        }
        if (state == BudgerigarBehaviorState.SLEEPING || state == BudgerigarBehaviorState.ROOSTING) {
            return animationState.setAndContinue(SLEEP_ANIMATION);
        }
        if (this.isDancing()) {
            return animationState.setAndContinue(DANCE_ANIMATION);
        }
        if (shouldWalk(state, animationState.isMoving())) {
            animationState.getController().setAnimationSpeed(BirdGroundAnimation.walkAnimationSpeed(this));
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        if (state == BudgerigarBehaviorState.PREENING) {
            return animationState.setAndContinue(PREEN_ANIMATION);
        }
        return animationState.setAndContinue(this.pickIdleAnimation());
    }

    private boolean shouldWalk(BudgerigarBehaviorState state, boolean animationMoving) {
        if (!BirdGroundAnimation.canPlayWalk(this)) {
            return false;
        }
        return BirdGroundAnimation.hasWalkMotion(this, animationMoving)
                || state == BudgerigarBehaviorState.WALKING
                || state == BudgerigarBehaviorState.FOLLOWING
                || state == BudgerigarBehaviorState.FORAGING;
    }

    private RawAnimation pickIdleAnimation() {
        if (this.level().getGameTime() >= this.nextIdleAnimationTick) {
            int roll = this.getRandom().nextInt(12);
            if (roll < 2) {
                this.currentIdleAnimation = PREEN_ANIMATION;
                this.nextIdleAnimationTick = this.level().getGameTime() + 60L;
            } else if (roll < 4) {
                this.currentIdleAnimation = IDLE_TWO_ANIMATION;
                this.nextIdleAnimationTick = this.level().getGameTime() + 65L;
            } else if (roll == 4) {
                this.currentIdleAnimation = IDLE_FOUR_ANIMATION;
                this.nextIdleAnimationTick = this.level().getGameTime() + 45L;
            } else {
                this.currentIdleAnimation = IDLE_ANIMATION;
                this.nextIdleAnimationTick = this.level().getGameTime() + 70L + this.getRandom().nextInt(100);
            }
        }
        return this.currentIdleAnimation;
    }

    private void tickHighPerchPreference() {
        if (!this.onGround() || this.isFlying() || this.isInWaterOrBubble()
                || !BirdActivitySchedule.DIURNAL.isActiveTime(this.level().getDayTime())) {
            this.groundPreferenceTicks = 0;
            return;
        }
        if (++this.groundPreferenceTicks < 180 || !this.getNavigation().isDone()) {
            return;
        }
        this.groundPreferenceTicks = 0;
        Vec3 target = this.findCanopyTarget();
        if (target != null) {
            this.startFlybyFlight(target);
        }
    }

    @Nullable
    private Vec3 findCanopyTarget() {
        BlockPos origin = this.blockPosition();
        for (int attempt = 0; attempt < 28; ++attempt) {
            int x = origin.getX() + this.getRandom().nextInt(25) - 12;
            int z = origin.getZ() + this.getRandom().nextInt(25) - 12;
            int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos standPos = new BlockPos(x, y, z);
            BlockState below = this.level().getBlockState(standPos.below());
            if (y >= origin.getY() + 3
                    && (below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS))
                    && this.level().getBlockState(standPos).isAir()
                    && this.level().getBlockState(standPos.above()).isAir()) {
                return Vec3.atBottomCenterOf(standPos);
            }
        }
        return null;
    }

    private void tickMimicry() {
        if (--this.mimicCooldown > 0 || this.isSleepingState() || this.isFlying()) {
            return;
        }
        this.mimicCooldown = 420 + this.getRandom().nextInt(780);
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(15.0D),
                entity -> entity != this && entity.isAlive() && BirdSpecies.from(entity) != null);
        if (!nearby.isEmpty()) {
            LivingEntity source = nearby.get(this.getRandom().nextInt(nearby.size()));
            SoundEvent imitation = imitationFor(BirdSpecies.from(source));
            if (imitation != null) {
                this.playSound(imitation, 0.72F, 1.02F + this.getRandom().nextFloat() * 0.18F);
                return;
            }
        }
        Player player = this.level().getNearestPlayer(this, 12.0D);
        if (player != null) {
            this.playSound(this.getAmbientSound(), 0.68F, 0.88F + this.getRandom().nextFloat() * 0.24F);
        }
    }

    private boolean isSleepingState() {
        BudgerigarBehaviorState state = this.getBehaviorState();
        return state == BudgerigarBehaviorState.SLEEPING || state == BudgerigarBehaviorState.ROOSTING;
    }

    @Nullable
    private static SoundEvent imitationFor(BirdSpecies species) {
        if (species == null) {
            return null;
        }
        return switch (species) {
            case NIGHT_HERON -> GuaniaoSoundEvents.NIGHT_HERON_AMBIENT.get();
            case SPARROW, LONG_TAILED_TIT -> GuaniaoSoundEvents.SPARROW_AMBIENT.get();
            case BUDGERIGAR, COCKATIEL -> GuaniaoSoundEvents.BUDGERIGAR_AMBIENT.get();
            case SPOTTED_DOVE -> GuaniaoSoundEvents.SPOTTED_DOVE_AMBIENT.get();
            case PIGEON -> GuaniaoSoundEvents.PIGEON_AMBIENT.get();
            case CROW, SEAGULL, MACAW, KIWI, MYNA -> null;
        };
    }

    private enum GuidePreviewAnimation {
        NONE(null),
        IDLE(IDLE_ANIMATION),
        PREEN(PREEN_ANIMATION),
        DANCE(DANCE_ANIMATION),
        WALK(WALK_ANIMATION),
        FLY(FLY_ANIMATION),
        EAT(EAT_ANIMATION),
        SLEEP(SLEEP_ANIMATION);

        private final RawAnimation animation;

        GuidePreviewAnimation(RawAnimation animation) {
            this.animation = animation;
        }
    }
}
