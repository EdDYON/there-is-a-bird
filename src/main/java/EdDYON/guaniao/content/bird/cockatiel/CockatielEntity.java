package EdDYON.guaniao.content.bird.cockatiel;

import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarBehaviorState;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;

public class CockatielEntity extends BudgerigarEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation DANCE_ANIMATION = RawAnimation.begin().thenPlay("animation.idle_diff_1").thenLoop("animation.idle");
    private static final RawAnimation PREEN_ANIMATION = RawAnimation.begin().thenPlay("animation.idle_diff_2").thenLoop("animation.idle");
    private static final RawAnimation STARTLED_NAP_ANIMATION = RawAnimation.begin().thenPlay("animation.idle_diff_3").thenLoop("animation.idle");
    private static final RawAnimation FLY_ANIMATION = RawAnimation.begin().thenLoop("animation.fly");
    private static final RawAnimation SLEEP_ANIMATION = RawAnimation.begin().thenPlay("animation.sleep").thenLoop("animation.sleep_loop");
    private static final RawAnimation EAT_ANIMATION = RawAnimation.begin().thenPlay("animation.eat").thenLoop("animation.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.walk");
    private GuidePreviewAnimation cockatielPreviewAnimation = GuidePreviewAnimation.NONE;
    private RawAnimation currentIdleAnimation = IDLE_ANIMATION;
    private long nextIdleAnimationTick;
    private long happyDanceUntilTick;
    private boolean wasEating;
    private int macawAvoidanceCooldown;

    public CockatielEntity(EntityType<? extends CockatielEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.macawAvoidanceCooldown > 0) {
            --this.macawAvoidanceCooldown;
        }
        if (!this.level().isClientSide && !this.isTame() && !this.isFlying()
                && this.macawAvoidanceCooldown <= 0 && this.tickCount % 20 == 0) {
            EdDYON.guaniao.content.bird.macaw.MacawEntity macaw = this.level().getNearestEntity(
                    this.level().getEntitiesOfClass(EdDYON.guaniao.content.bird.macaw.MacawEntity.class, this.getBoundingBox().inflate(5.0D)),
                    net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat(),
                    this, this.getX(), this.getY(), this.getZ());
            if (macaw != null) {
                Vec3 away = this.position().subtract(macaw.position()).multiply(1.0D, 0.0D, 1.0D);
                if (away.lengthSqr() < 1.0E-4D) {
                    away = new Vec3(1.0D, 0.0D, 0.0D);
                }
                away = away.normalize().scale(6.0D);
                int x = (int) Math.floor(this.getX() + away.x);
                int z = (int) Math.floor(this.getZ() + away.z);
                int y = this.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                this.startFlybyFlight(new Vec3(x + 0.5D, y, z + 0.5D));
                this.macawAvoidanceCooldown = 160;
            }
        }
    }

    public static AttributeSupplier.Builder createCockatielAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, CockatielDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, CockatielDefinition.WALK_SPEED)
                .add(Attributes.FLYING_SPEED, CockatielDefinition.FLYING_SPEED)
                .add(Attributes.FOLLOW_RANGE, CockatielDefinition.FOLLOW_RANGE);
    }

    public static boolean canCockatielSpawn(EntityType<CockatielEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean dryOpenGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(BlockTags.DIRT)
                || below.is(Blocks.SAND)
                || below.is(Blocks.RED_SAND)
                || below.is(Blocks.COARSE_DIRT)
                || below.is(Blocks.HAY_BLOCK);
        return dryOpenGround && level.getRawBrightness(pos, 0) > 8;
    }

    @Nullable
    @Override
    public CockatielEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
        CockatielEntity child = GuaniaoEntityTypes.COCKATIEL.get().create(level);
        if (child != null) {
            child.setSkinVariantForRendering(this.getRandom().nextInt(CockatielDefinition.TEXTURE_VARIANTS.length));
            float mateScale = mate instanceof BudgerigarEntity other ? other.getIndividualModelScale() : this.getIndividualModelScale();
            child.setIndividualModelScale(BirdModelScale.inheritIndividualScale(
                    child.getRandom(), this.getIndividualModelScale(), mateScale, child.modelScaleProfile()));
        }
        return child;
    }

    @Override
    public ResourceLocation getTextureResource() {
        return CockatielDefinition.textureForVariant(this.getSkinVariant());
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.COCKATIEL;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.COCKATIEL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return GuaniaoSoundEvents.COCKATIEL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuaniaoSoundEvents.COCKATIEL_DEATH.get();
    }

    @Override
    public void setGuidePreviewAnimation(BudgerigarEntity.GuidePreviewAnimation animation) {
        this.cockatielPreviewAnimation = animation == null ? GuidePreviewAnimation.NONE : switch (animation) {
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
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable) this, "movement", 4, this::movementController)});
    }

    private <T extends CockatielEntity> PlayState movementController(AnimationState<T> animationState) {
        RawAnimation preview = this.cockatielPreviewAnimation.animation;
        if (preview != null) {
            return animationState.setAndContinue(preview);
        }
        BudgerigarBehaviorState state = this.getBehaviorState();
        if (state == BudgerigarBehaviorState.EATING) {
            this.wasEating = true;
            return animationState.setAndContinue(EAT_ANIMATION);
        }
        boolean flying = this.isFlying();
        if (this.wasEating) {
            this.wasEating = false;
            if (!flying && this.onGround() && this.getRandom().nextInt(3) == 0) {
                this.happyDanceUntilTick = this.level().getGameTime() + 165L;
            }
        }
        if (flying) {
            this.happyDanceUntilTick = 0L;
            return animationState.setAndContinue(FLY_ANIMATION);
        }
        if (state == BudgerigarBehaviorState.SLEEPING || state == BudgerigarBehaviorState.ROOSTING) {
            this.happyDanceUntilTick = 0L;
            return animationState.setAndContinue(SLEEP_ANIMATION);
        }
        if (shouldWalk(state)) {
            this.happyDanceUntilTick = 0L;
            return animationState.setAndContinue(WALK_ANIMATION);
        }
        if (state == BudgerigarBehaviorState.DANCING || this.level().getGameTime() < this.happyDanceUntilTick) {
            return animationState.setAndContinue(DANCE_ANIMATION);
        }
        if (state == BudgerigarBehaviorState.PREENING) {
            return animationState.setAndContinue(PREEN_ANIMATION);
        }
        return animationState.setAndContinue(this.pickIdleAnimation());
    }

    private boolean shouldWalk(BudgerigarBehaviorState state) {
        if (!BirdGroundAnimation.canPlayWalk(this)) {
            return false;
        }
        return BirdGroundAnimation.hasWalkMotion(this)
                || state == BudgerigarBehaviorState.WALKING
                || state == BudgerigarBehaviorState.FOLLOWING
                || state == BudgerigarBehaviorState.FORAGING;
    }

    private RawAnimation pickIdleAnimation() {
        if (this.level().getGameTime() >= this.nextIdleAnimationTick) {
            int roll = this.getRandom().nextInt(10);
            if (roll < 2) {
                this.currentIdleAnimation = PREEN_ANIMATION;
                this.nextIdleAnimationTick = this.level().getGameTime() + 70L;
            } else if (roll == 2 && this.level().isDay()) {
                this.currentIdleAnimation = STARTLED_NAP_ANIMATION;
                this.nextIdleAnimationTick = this.level().getGameTime() + 170L;
            } else {
                this.currentIdleAnimation = IDLE_ANIMATION;
                this.nextIdleAnimationTick = this.level().getGameTime() + 70L + this.getRandom().nextInt(90);
            }
        }
        return this.currentIdleAnimation;
    }

    private enum GuidePreviewAnimation {
        NONE(null),
        IDLE(IDLE_ANIMATION),
        DANCE(DANCE_ANIMATION),
        PREEN(PREEN_ANIMATION),
        STARTLED_NAP(STARTLED_NAP_ANIMATION),
        FLY(FLY_ANIMATION),
        SLEEP(SLEEP_ANIMATION),
        EAT(EAT_ANIMATION),
        WALK(WALK_ANIMATION);

        private final RawAnimation animation;

        GuidePreviewAnimation(RawAnimation animation) {
            this.animation = animation;
        }
    }
}
