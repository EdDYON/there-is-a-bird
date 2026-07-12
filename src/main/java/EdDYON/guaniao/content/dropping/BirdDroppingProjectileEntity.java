package EdDYON.guaniao.content.dropping;

import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class BirdDroppingProjectileEntity extends ThrowableItemProjectile implements GeoEntity {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(BirdDroppingProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_NATURAL_DROPPING = SynchedEntityData.defineId(BirdDroppingProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private int lifeTicks;
    private UUID sourceBirdUuid;

    public BirdDroppingProjectileEntity(EntityType<? extends BirdDroppingProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public BirdDroppingProjectileEntity(Level level, LivingEntity owner, BirdDroppingVariant variant) {
        super(GuaniaoEntityTypes.BIRD_DROPPING_PROJECTILE.get(), owner, level);
        this.setVariant(variant);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, BirdDroppingVariant.ONE.id());
        this.entityData.define(DATA_NATURAL_DROPPING, false);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return this.getVariant().item();
    }

    @Override
    protected float getGravity() {
        return 0.06F;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && ++this.lifeTicks > 200) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }

        Entity hit = result.getEntity();
        if (BirdDroppingInteractionHandler.handleEntityHit(this, hit)) {
            this.splat();
            this.discard();
            return;
        }

        if (hit instanceof Player player) {
            if (this.isNaturalDropping()) {
                BirdDroppingPrankHandler.handleNaturalHitPlayer(this, player);
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.guaniao.bird_dropping_blessing"), true);
            }
        } else if (hit instanceof Villager villager) {
            BirdDroppingPrankHandler.handleVillagerHit(this, villager);
        } else if (hit instanceof IronGolem golem) {
            BirdDroppingPrankHandler.handleIronGolemHit(this, golem);
        } else if (hit instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
        }

        this.splat();
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide) {
            return;
        }

        if (BirdDroppingPrankHandler.handleCakeHit(this, result.getBlockPos())) {
            this.discard();
            return;
        }

        if (BirdDroppingInteractionHandler.handleNearbyFoodHit(this, result.getLocation())) {
            this.splat();
            this.discard();
            return;
        }

        this.spawnBlockSplat(result);
        this.splat();
        this.discard();
    }

    private void spawnBlockSplat(BlockHitResult result) {
        Level level = this.level();
        if (level.getFluidState(result.getBlockPos().relative(result.getDirection())).isSource()) {
            return;
        }
        if (!BirdDroppingSplatEntity.canAddSplatAt(level, result.getLocation())) {
            return;
        }
        BirdDroppingSplatEntity splat = BirdDroppingSplatEntity.onBlock(level, result.getLocation(), result.getDirection(), result.getBlockPos());
        if (level.addFreshEntity(splat)) {
            BlockState state = level.getBlockState(result.getBlockPos());
            if (state.getBlock() instanceof BasePressurePlateBlock) {
                state.entityInside(level, result.getBlockPos(), splat);
            }
        }
    }

    private void splat() {
        Level level = this.level();
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_BLOCK_HIT, SoundSource.NEUTRAL, 0.65F, 0.85F + level.random.nextFloat() * 0.25F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), 4, 0.08D, 0.04D, 0.08D, 0.004D);
        }
    }

    public BirdDroppingVariant getVariant() {
        return BirdDroppingVariant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(BirdDroppingVariant variant) {
        if (variant == null) {
            variant = BirdDroppingVariant.ONE;
        }
        this.entityData.set(DATA_VARIANT, variant.id());
    }

    public boolean isNaturalDropping() {
        return this.entityData.get(DATA_NATURAL_DROPPING);
    }

    public void markNaturalDropping(UUID birdUuid) {
        this.entityData.set(DATA_NATURAL_DROPPING, true);
        this.sourceBirdUuid = birdUuid;
    }

    public UUID getSourceBirdUuid() {
        return this.sourceBirdUuid;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant().id());
        tag.putBoolean("NaturalDropping", this.isNaturalDropping());
        if (this.sourceBirdUuid != null) {
            tag.putUUID("SourceBirdUuid", this.sourceBirdUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(BirdDroppingVariant.byId(tag.getInt("Variant")));
        this.entityData.set(DATA_NATURAL_DROPPING, tag.getBoolean("NaturalDropping"));
        if (tag.hasUUID("SourceBirdUuid")) {
            this.sourceBirdUuid = tag.getUUID("SourceBirdUuid");
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }
}
