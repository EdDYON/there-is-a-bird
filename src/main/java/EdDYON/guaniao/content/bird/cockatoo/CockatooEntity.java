package EdDYON.guaniao.content.bird.cockatoo;

import EdDYON.guaniao.content.bird.BirdFlockSoundLimiter;
import EdDYON.guaniao.content.bird.BirdGroundAnimation;
import EdDYON.guaniao.content.bird.BirdMovementAnimationController;
import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarBehaviorState;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.flight.BirdFlightAnimation;
import EdDYON.guaniao.content.bird.flight.BirdFlightProfile;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;

public class CockatooEntity extends BudgerigarEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.fly");
    private static final RawAnimation EAT = RawAnimation.begin().thenPlay("animation.eat").thenLoop("animation.idle");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenPlay("animation.sleep").thenLoop("animation.sleep_loop");
    private static final RawAnimation LOOK_AROUND = RawAnimation.begin().thenLoop("animation.idle_diff_1");
    private static final RawAnimation DISPLAY = RawAnimation.begin().thenLoop("animation.idle_diff_2");
    private GuidePreviewAnimation preview = GuidePreviewAnimation.NONE;
    private RawAnimation idle = IDLE;
    private long nextIdleTick;

    public CockatooEntity(EntityType<? extends CockatooEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createCockatooAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, CockatooDefinition.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, CockatooDefinition.WALK_SPEED)
                .add(Attributes.FLYING_SPEED, CockatooDefinition.FLYING_SPEED)
                .add(Attributes.FOLLOW_RANGE, CockatooDefinition.FOLLOW_RANGE);
    }

    public static boolean canCockatooSpawn(EntityType<CockatooEntity> type, ServerLevelAccessor level,
                                           MobSpawnType reason, BlockPos pos, RandomSource random) {
        // The placement predicate can also run at a world-generation region edge.
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return false;
        BlockState below = level.getBlockState(pos.below());
        return (below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(BlockTags.DIRT)
                || below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS))
                && level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    protected TagKey<Item> foodTag() {
        return BirdTags.COCKATOO_FOODS;
    }

    @Override
    protected int minimumEatingAnimationTicks() {
        // The supplied clip is 115 ticks long, plus the four-tick transition.
        return 120;
    }

    @Override
    public ResourceLocation getTextureResource() {
        return CockatooDefinition.TEXTURE;
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.COCKATOO;
    }

    @Override
    public BirdFlightProfile birdFlightProfile() {
        return BirdFlightProfile.UMBRELLA_COCKATOO;
    }

    @Override
    protected double flightAnimationSpeed() {
        return Mth.clamp(getAttributeValue(Attributes.FLYING_SPEED) / CockatooDefinition.FLYING_SPEED, 0.80D, 1.35D);
    }

    @Nullable
    @Override
    public CockatooEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
        CockatooEntity child = GuaniaoEntityTypes.COCKATOO.get().create(level);
        if (child != null) {
            float otherScale = mate instanceof CockatooEntity other ? other.getIndividualModelScale() : getIndividualModelScale();
            child.setIndividualModelScale(BirdModelScale.inheritIndividualScale(
                    child.getRandom(), getIndividualModelScale(), otherScale, child.modelScaleProfile()));
        }
        return child;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.COCKATOO_AMBIENT.get();
    }

    @Override
    protected SoundEvent getInteractionSound() {
        return GuaniaoSoundEvents.COCKATOO_INTERACT.get();
    }

    @Override
    public float getSoundVolume() {
        // The source clips are quieter, so this stays slightly below the umbrella cockatoo.
        return 1.0F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return BirdFlockSoundLimiter.scaledAmbientInterval(this, 180);
    }

    @Override
    protected void playInteractionSound() {
        this.playSound(this.getInteractionSound(), 0.78F, 0.95F + this.getRandom().nextFloat() * 0.18F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return GuaniaoSoundEvents.COCKATOO_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuaniaoSoundEvents.COCKATOO_DEATH.get();
    }

    @Override
    public void setGuidePreviewAnimation(GuidePreviewAnimation animation) {
        preview = animation == null ? GuidePreviewAnimation.NONE : animation;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new BirdMovementAnimationController<>(this, "movement", 4, this::movementController));
    }

    private PlayState movementController(AnimationState<CockatooEntity> state) {
        state.getController().setAnimationSpeed(1.0D);
        state.getController().transitionLength(4);
        if (preview != GuidePreviewAnimation.NONE) {
            return state.setAndContinue(switch (preview) {
                case WALK -> WALK;
                case FLY -> FLY;
                case EAT -> EAT;
                case SLEEP -> SLEEP;
                case CURIOUS, PREEN -> LOOK_AROUND;
                case DANCE -> DISPLAY;
                default -> IDLE;
            });
        }
        if (shouldPlayFlyAnimation()) {
            state.getController().transitionLength(0);
            state.getController().setAnimationSpeed(flightAnimationSpeed());
            return BirdFlightAnimation.play(state, FLY);
        }
        BudgerigarBehaviorState behavior = getBehaviorState();
        // ROOSTING is also used while travelling to a perch. Travel must win
        // over resting/eating state so the bird does not sleep-walk or slide.
        if (BirdGroundAnimation.hasWalkMotion(this, state.isMoving())) {
            return BirdGroundAnimation.play(state, this, WALK);
        }
        if (behavior == BudgerigarBehaviorState.SLEEPING || behavior == BudgerigarBehaviorState.ROOSTING) {
            return state.setAndContinue(SLEEP);
        }
        if (behavior == BudgerigarBehaviorState.EATING) return state.setAndContinue(EAT);
        if (isDancing()) return state.setAndContinue(DISPLAY);
        if (behavior == BudgerigarBehaviorState.PREENING) return state.setAndContinue(LOOK_AROUND);
        return state.setAndContinue(pickIdle());
    }

    private RawAnimation pickIdle() {
        long now = level().getGameTime();
        if (now >= nextIdleTick) {
            // Both authored variations are endless Molang loops. Time their exit
            // explicitly rather than queuing an idle they would never reach.
            if (idle != IDLE) {
                idle = IDLE;
                nextIdleTick = now + 100 + getRandom().nextInt(100);
            } else {
                idle = getRandom().nextBoolean() ? LOOK_AROUND : DISPLAY;
                nextIdleTick = now + 40 + getRandom().nextInt(41);
            }
        }
        return idle;
    }
}
