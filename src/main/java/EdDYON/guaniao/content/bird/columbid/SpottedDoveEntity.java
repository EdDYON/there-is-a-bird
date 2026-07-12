package EdDYON.guaniao.content.bird.columbid;

import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class SpottedDoveEntity extends AbstractColumbidEntity {
    public SpottedDoveEntity(EntityType<? extends SpottedDoveEntity> entityType, Level level) {
        super(entityType, level, SpottedDoveProfile.INSTANCE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createColumbidAttributes(
                SpottedDoveDefinition.MAX_HEALTH,
                SpottedDoveDefinition.WALK_SPEED,
                SpottedDoveDefinition.FLYING_SPEED,
                SpottedDoveDefinition.FOLLOW_RANGE);
    }

    public static boolean canSpawn(EntityType<SpottedDoveEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return canColumbidSpawn(level, pos, random, false);
    }

    @Override
    public ColumbidVariant getColumbidVariant() {
        return ColumbidVariant.SPOTTED_DOVE;
    }

    @Override
    public BirdModelScaleProfile modelScaleProfile() {
        return BirdModelScaleProfile.SPOTTED_DOVE;
    }

    @Override
    protected boolean usesWeatherSense() {
        return true;
    }

    @Override
    protected boolean supportsPairBond() {
        return true;
    }

    @Override
    protected boolean supportsChasing() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.SPOTTED_DOVE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return GuaniaoSoundEvents.SPOTTED_DOVE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuaniaoSoundEvents.SPOTTED_DOVE_DEATH.get();
    }

    @Override
    protected AbstractColumbidEntity createChildEntity(ServerLevel level) {
        return GuaniaoEntityTypes.SPOTTED_DOVE.get().create(level);
    }
}
