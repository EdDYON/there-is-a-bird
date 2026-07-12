package EdDYON.guaniao.content.bird.columbid;

import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class PigeonEntity extends AbstractColumbidEntity {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(PigeonEntity.class, EntityDataSerializers.INT);

    public PigeonEntity(EntityType<? extends PigeonEntity> entityType, Level level) {
        super(entityType, level, PigeonProfile.INSTANCE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createColumbidAttributes(
                PigeonDefinition.MAX_HEALTH,
                PigeonDefinition.WALK_SPEED,
                PigeonDefinition.FLYING_SPEED,
                PigeonDefinition.FOLLOW_RANGE);
    }

    public static boolean canSpawn(EntityType<PigeonEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return canColumbidSpawn(level, pos, random, true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, ColumbidVariant.GRAY_PIGEON.ordinal());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag compoundTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, compoundTag);
        if (compoundTag == null || !compoundTag.contains("PigeonVariant", 3)) {
            this.setPigeonVariant(this.getRandom().nextBoolean() ? ColumbidVariant.GRAY_PIGEON : ColumbidVariant.WHITE_PIGEON);
        }
        return data;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putInt("PigeonVariant", this.getColumbidVariant().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.setPigeonVariant(ColumbidVariant.pigeonByOrdinal(compoundTag.getInt("PigeonVariant")));
    }

    @Override
    public ColumbidVariant getColumbidVariant() {
        if (this.entityData == null) {
            return ColumbidVariant.GRAY_PIGEON;
        }
        return ColumbidVariant.pigeonByOrdinal(this.entityData.get(VARIANT));
    }

    public void setPigeonVariant(ColumbidVariant variant) {
        ColumbidVariant pigeonVariant = variant == ColumbidVariant.WHITE_PIGEON ? ColumbidVariant.WHITE_PIGEON : ColumbidVariant.GRAY_PIGEON;
        if (this.entityData != null) {
            this.entityData.set(VARIANT, pigeonVariant.ordinal());
        }
    }

    @Override
    protected boolean prefersHumanSettlements() {
        return true;
    }

    @Override
    protected boolean supportsPairBond() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuaniaoSoundEvents.PIGEON_AMBIENT.get();
    }

    @Override
    protected AbstractColumbidEntity createChildEntity(ServerLevel level) {
        PigeonEntity child = GuaniaoEntityTypes.PIGEON.get().create(level);
        if (child != null) {
            child.setPigeonVariant(this.getColumbidVariant());
        }
        return child;
    }
}
