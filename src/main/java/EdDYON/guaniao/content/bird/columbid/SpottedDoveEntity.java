package EdDYON.guaniao.content.bird.columbid;

import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.content.bird.scale.BirdModelScaleProfile;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ServerLevelData;

public class SpottedDoveEntity extends AbstractColumbidEntity {
    private static final EntityDataAccessor<Integer> WEATHER_SENSE_STATE = SynchedEntityData.defineId(SpottedDoveEntity.class, EntityDataSerializers.INT);
    private static final int PRE_RAIN_WINDOW_TICKS = 1200;

    private WeatherSenseState weatherSenseState = WeatherSenseState.NORMAL;
    private int weatherWarningCooldown;

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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WEATHER_SENSE_STATE, WeatherSenseState.NORMAL.ordinal());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (WEATHER_SENSE_STATE.equals(key)) {
            this.weatherSenseState = WeatherSenseState.byId(this.entityData.get(WEATHER_SENSE_STATE));
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (this.weatherWarningCooldown > 0) {
            --this.weatherWarningCooldown;
        }
        if (this.tickCount % 100 == 0) {
            this.updateWeatherSenseState();
        }
    }

    public WeatherSenseState getWeatherSenseState() {
        if (this.entityData != null) {
            return WeatherSenseState.byId(this.entityData.get(WEATHER_SENSE_STATE));
        }
        return this.weatherSenseState;
    }

    private void updateWeatherSenseState() {
        WeatherSenseState next = WeatherSenseState.NORMAL;
        if (this.level().isThundering()) {
            next = WeatherSenseState.THUNDER;
        } else if (this.level().isRaining()) {
            next = WeatherSenseState.RAIN;
        } else if (this.level() instanceof ServerLevel serverLevel
                && serverLevel.getLevelData() instanceof ServerLevelData weatherData) {
            int rainTime = weatherData.getRainTime();
            if (weatherData.getClearWeatherTime() <= 0
                    && rainTime > 0
                    && rainTime <= PRE_RAIN_WINDOW_TICKS) {
                next = WeatherSenseState.PRE_RAIN;
            }
        }
        WeatherSenseState previous = this.getWeatherSenseState();
        this.weatherSenseState = next;
        this.entityData.set(WEATHER_SENSE_STATE, next.ordinal());
        if (next == WeatherSenseState.PRE_RAIN
                && previous != WeatherSenseState.PRE_RAIN
                && this.weatherWarningCooldown <= 0) {
            this.weatherWarningCooldown = 600;
            this.playSound(GuaniaoSoundEvents.SPOTTED_DOVE_AMBIENT.get(), 0.34F, 0.78F + this.getRandom().nextFloat() * 0.08F);
        }
    }

    @Override
    public boolean sensesIncomingBadWeather() {
        return this.getWeatherSenseState() != WeatherSenseState.NORMAL;
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
    protected TagKey<Item> foodTag() {
        return BirdTags.SPOTTED_DOVE_FOODS;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.SPOTTED_DOVE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getInteractionSound() {
        return GuaniaoSoundEvents.SPOTTED_DOVE_MATE.get();
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
