package EdDYON.guaniao.content.food;

import EdDYON.guaniao.registry.GuaniaoBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class BaggedFriesBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final String FRY_MASK_TAG = "FryMask";
    private static final String LAXATIVE_TAG = "Laxative";
    private static final int FULL_MASK = (1 << BaggedFriesBlock.TOTAL_FRIES) - 1;
    private static final int AMBIENT_PARTICLE_CHANCE = 8;
    private static final DustParticleOptions DIRTY_BROWN_DUST =
            new DustParticleOptions(new Vector3f(0.28F, 0.18F, 0.07F), 0.80F);
    private static final DustParticleOptions SICK_GREEN_DUST =
            new DustParticleOptions(new Vector3f(0.20F, 0.40F, 0.08F), 0.70F);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int fryMask = FULL_MASK;
    private boolean laxative;

    public BaggedFriesBlockEntity(BlockPos pos, BlockState state) {
        super(GuaniaoBlockEntityTypes.BAGGED_FRIES.get(), pos, state);
    }

    public int getFryMask() {
        return this.fryMask;
    }

    public int getRemainingFries() {
        return Integer.bitCount(this.fryMask);
    }

    public boolean hasRemainingFries() {
        return this.fryMask != 0;
    }

    public boolean isLaxative() {
        return this.laxative;
    }

    public void setLaxative(boolean laxative) {
        this.laxative = laxative;
        this.setChanged();
    }

    public boolean removeOneFry() {
        if (this.fryMask == 0) {
            return false;
        }
        this.fryMask &= (this.fryMask - 1);
        this.setChanged();
        return true;
    }

    public boolean isFryVisible(int oneBasedIndex) {
        if (oneBasedIndex < 1 || oneBasedIndex > BaggedFriesBlock.TOTAL_FRIES) {
            return false;
        }
        return (this.fryMask & (1 << (oneBasedIndex - 1))) != 0;
    }

    public int removeRandomFries(RandomSource random) {
        int remaining = this.getRemainingFries();
        if (remaining <= 0) {
            return 0;
        }

        int removeCount = Math.min(remaining, 1 + random.nextInt(3));
        for (int removed = 0; removed < removeCount; removed++) {
            int selected = random.nextInt(Integer.bitCount(this.fryMask));
            for (int index = 0; index < BaggedFriesBlock.TOTAL_FRIES; index++) {
                int bit = 1 << index;
                if ((this.fryMask & bit) == 0) {
                    continue;
                }
                if (selected-- == 0) {
                    this.fryMask &= ~bit;
                    break;
                }
            }
        }
        this.setChanged();
        return removeCount;
    }

    public void syncToClient() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide || !this.laxative || this.fryMask == 0) {
            return;
        }
        RandomSource random = level.random;
        if (random.nextInt(AMBIENT_PARTICLE_CHANCE) != 0) {
            return;
        }
        spawnAmbientParticles(level, pos, random);
    }

    public static void spawnAmbientParticles(Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.34D;
        double y = pos.getY() + 0.56D + random.nextDouble() * 0.16D;
        double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.34D;
        level.addParticle(ParticleTypes.ITEM_SLIME, x, y, z, 0.0D, 0.008D, 0.0D);
        if (random.nextFloat() < 0.45F) {
            level.addParticle(ParticleTypes.ITEM_SLIME, x, y + 0.03D, z, 0.0D, 0.010D, 0.0D);
        }
        if (random.nextFloat() < 0.6F) {
            level.addParticle(DIRTY_BROWN_DUST, x, y + 0.05D, z, 0.0D, 0.010D, 0.0D);
        }
        if (random.nextFloat() < 0.45F) {
            level.addParticle(SICK_GREEN_DUST, x, y + 0.08D, z, 0.0D, 0.012D, 0.0D);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SNEEZE, x, y + 0.12D, z, 0.0D, 0.010D, 0.0D);
        }
    }

    public static void spawnPollutionBurst(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.60D;
        double z = pos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.ITEM_SLIME, x, y, z, 16, 0.26D, 0.08D, 0.26D, 0.05D);
        level.sendParticles(DIRTY_BROWN_DUST, x, y + 0.06D, z, 10, 0.26D, 0.08D, 0.26D, 0.02D);
        level.sendParticles(SICK_GREEN_DUST, x, y + 0.10D, z, 6, 0.20D, 0.08D, 0.20D, 0.015D);
        level.sendParticles(ParticleTypes.SNEEZE, x, y + 0.16D, z, 3, 0.12D, 0.05D, 0.12D, 0.01D);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(FRY_MASK_TAG, this.fryMask);
        tag.putBoolean(LAXATIVE_TAG, this.laxative);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(FRY_MASK_TAG)) {
            this.fryMask = tag.getInt(FRY_MASK_TAG) & FULL_MASK;
        } else if (this.getBlockState().hasProperty(BaggedFriesBlock.FRIES)) {
            int count = this.getBlockState().getValue(BaggedFriesBlock.FRIES);
            this.fryMask = count <= 0 ? 0 : (1 << Math.min(count, BaggedFriesBlock.TOTAL_FRIES)) - 1;
        } else {
            this.fryMask = FULL_MASK;
        }
        this.laxative = tag.getBoolean(LAXATIVE_TAG);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            this.load(tag);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }
}
