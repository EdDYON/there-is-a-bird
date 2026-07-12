package EdDYON.guaniao.content.bath;

import EdDYON.guaniao.registry.GuaniaoBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class BirdBathBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final String CONTENT_TYPE_TAG = "ContentType";
    private static final String CONTENT_LEVEL_TAG = "ContentLevel";
    private static final String CLEANLINESS_TAG = "Cleanliness";
    private static final String SPOILED_CONTENT_TYPE_TAG = "SpoiledContentType";
    private static final String SPOIL_TICKS_TAG = "SpoilTicks";
    private static final String ENVIRONMENTAL_TICK_OFFSET_TAG = "EnvironmentalTickOffset";
    private static final String CURRENT_USER_TAG = "CurrentUser";
    private static final String OCCUPIED_TICKS_TAG = "OccupiedTicks";
    private static final String RECENT_BIRD_USE_TICKS_TAG = "RecentBirdUseTicks";
    private static final int MAX_CONTENT_LEVEL = 3;
    private static final int RAIN_REFILL_AMOUNT = 3;
    private static final int ENVIRONMENT_TICK_INTERVAL = 200;
    private static final int EVAPORATION_CHANCE = 72;
    private static final int WATER_DIRT_CHANCE = 36;
    private static final int FISH_MEAT_SPOIL_TICKS = 72000;
    private static final int BREAD_SPOIL_TICKS = 144000;
    private static final int SUNLIGHT_SPOIL_BONUS_DIVISOR = 8;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private BirdBathContentType contentType = BirdBathContentType.EMPTY;
    private BirdBathContentType spoiledContentType = BirdBathContentType.EMPTY;
    private int contentLevel;
    private BirdBathCleanliness cleanliness = BirdBathCleanliness.CLEAN;
    private int spoilTicks;
    private int environmentalTickOffset;
    private UUID currentUser;
    private int occupiedTicks;
    private int recentBirdUseTicks;

    public BirdBathBlockEntity(BlockPos pos, BlockState state) {
        super(GuaniaoBlockEntityTypes.BIRD_BATH.get(), pos, state);
        this.environmentalTickOffset = Math.floorMod((int)(pos.asLong() ^ (pos.asLong() >>> 32)), 20000);
    }

    public BirdBathVariant variant() {
        if (this.getBlockState().getBlock() instanceof BirdBathBlock birdBathBlock) {
            return birdBathBlock.variant();
        }
        return BirdBathVariant.BIRD_BATH;
    }

    public BirdBathContentType getContentType() {
        return this.contentType;
    }

    public BirdBathContentType getRenderContentType() {
        if (this.contentType == BirdBathContentType.SPOILED) {
            return this.spoiledContentType.isEmpty() ? BirdBathContentType.FISH : this.spoiledContentType;
        }
        return this.contentType;
    }

    public int getContentLevel() {
        return this.contentLevel;
    }

    public BirdBathCleanliness getCleanliness() {
        return this.cleanliness;
    }

    public boolean isEmpty() {
        return this.contentType.isEmpty() || this.contentLevel <= 0;
    }

    public boolean hasUsableWater() {
        return this.contentType == BirdBathContentType.WATER && this.contentLevel > 0 && this.cleanliness != BirdBathCleanliness.FILTHY;
    }

    public boolean hasUsableFood() {
        return this.contentType.isFood() && this.contentLevel > 0 && this.cleanliness != BirdBathCleanliness.FILTHY;
    }

    public boolean hasFoodForBird(BirdBathFoodPreference preference) {
        return preference != null && this.contentLevel > 0 && !this.isSpoiled()
                && this.cleanliness != BirdBathCleanliness.FILTHY
                && preference.matches(this.contentType);
    }

    public boolean isFrozen() {
        return this.contentType == BirdBathContentType.FROZEN_WATER;
    }

    public boolean isSpoiled() {
        return this.contentType == BirdBathContentType.SPOILED;
    }

    public boolean isDirty() {
        return this.cleanliness.isDirty();
    }

    public boolean canAccept(BirdBathContentType type) {
        if (type == null || type.isEmpty() || this.isSpoiled()) {
            return false;
        }
        if (type == BirdBathContentType.WATER) {
            return this.isEmpty() || this.contentType == BirdBathContentType.WATER || this.contentType == BirdBathContentType.FROZEN_WATER;
        }
        if (type.isFood()) {
            return this.isEmpty() || this.contentType == type;
        }
        return false;
    }

    public boolean setContent(BirdBathContentType type, int level) {
        BirdBathContentType normalizedType = type == null ? BirdBathContentType.EMPTY : type;
        int normalizedLevel = Mth.clamp(level, 0, MAX_CONTENT_LEVEL);
        if (normalizedType.isEmpty() || normalizedLevel <= 0) {
            normalizedType = BirdBathContentType.EMPTY;
            normalizedLevel = 0;
        }
        boolean changed = this.contentType != normalizedType || this.contentLevel != normalizedLevel;
        if (!changed) {
            if (normalizedType.isFood() && this.spoilTicks != 0) {
                this.spoilTicks = 0;
                this.sync();
                return true;
            }
            return false;
        }
        this.contentType = normalizedType;
        this.contentLevel = normalizedLevel;
        if (normalizedType != BirdBathContentType.SPOILED) {
            this.spoiledContentType = BirdBathContentType.EMPTY;
        }
        this.spoilTicks = normalizedType.isFood() ? 0 : this.spoilTicks;
        if (!normalizedType.isFood() && normalizedType != BirdBathContentType.SPOILED) {
            this.spoilTicks = 0;
        }
        this.sync();
        return true;
    }

    public boolean clearContent() {
        return this.setContent(BirdBathContentType.EMPTY, 0);
    }

    public boolean addContent(BirdBathContentType type, int amount) {
        if (type == null || type.isEmpty() || amount <= 0) {
            return false;
        }
        if (!this.canAccept(type)) {
            return false;
        }
        if (this.contentLevel >= MAX_CONTENT_LEVEL) {
            return false;
        }
        int baseLevel = this.isEmpty() ? 0 : this.contentLevel;
        return this.setContent(type, Math.min(MAX_CONTENT_LEVEL, baseLevel + amount));
    }

    public boolean cleanByHand() {
        if (this.isSpoiled()) {
            this.contentType = BirdBathContentType.EMPTY;
            this.spoiledContentType = BirdBathContentType.EMPTY;
            this.contentLevel = 0;
            this.spoilTicks = 0;
            this.cleanliness = dirtierOf(this.cleanliness, BirdBathCleanliness.DIRTY);
            this.sync();
            return true;
        }
        if (!this.cleanliness.isDirty()) {
            return false;
        }
        this.cleanliness = this.cleanliness.cleanOneStep();
        this.sync();
        return true;
    }

    public boolean consumeOneServing() {
        if (this.contentLevel <= 0 || this.isSpoiled() || this.isFrozen()) {
            return false;
        }
        boolean water = this.contentType == BirdBathContentType.WATER;
        if (!water && !this.contentType.isFood()) {
            return false;
        }
        this.contentLevel--;
        if (this.contentLevel <= 0) {
            this.contentType = BirdBathContentType.EMPTY;
            this.spoiledContentType = BirdBathContentType.EMPTY;
            this.contentLevel = 0;
            this.spoilTicks = 0;
        }
        this.markUsedByBird();
        if (this.level != null) {
            BirdBathEffects.birdUsed(this.level, this.worldPosition, water);
        }
        this.sync();
        return true;
    }

    public void markUsedByBird() {
        this.recentBirdUseTicks = 600;
        this.cleanliness = this.cleanliness.nextDirtier();
        this.sync();
    }

    public boolean tryClaimUse(UUID birdUuid, int ticks) {
        if (birdUuid == null || ticks <= 0) {
            return false;
        }
        if (this.currentUser != null && this.occupiedTicks > 0 && !this.currentUser.equals(birdUuid)) {
            return false;
        }
        this.currentUser = birdUuid;
        this.occupiedTicks = Math.max(this.occupiedTicks, ticks);
        this.sync();
        return true;
    }

    public void releaseUse(UUID birdUuid) {
        if (birdUuid != null && birdUuid.equals(this.currentUser)) {
            this.currentUser = null;
            this.occupiedTicks = 0;
            this.sync();
        }
    }

    public boolean isOccupied() {
        return this.currentUser != null && this.occupiedTicks > 0;
    }

    public boolean isOccupiedBy(UUID birdUuid) {
        return birdUuid != null && birdUuid.equals(this.currentUser) && this.occupiedTicks > 0;
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, BirdBathBlockEntity birdBath) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        birdBath.serverTick(serverLevel, pos, state);
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean changed = this.tickOccupation();
        long gameTime = level.getGameTime();
        if (Math.floorMod((int)(gameTime + this.environmentalTickOffset), ENVIRONMENT_TICK_INTERVAL) == 0) {
            changed |= this.runEnvironmentTick(level, pos, state, level.random, ENVIRONMENT_TICK_INTERVAL);
        }
        if (changed) {
            this.sync();
        }
    }

    public void environmentTick(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (this.runEnvironmentTick(level, pos, state, random, ENVIRONMENT_TICK_INTERVAL)) {
            this.sync();
        }
    }

    private boolean runEnvironmentTick(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, int elapsedTicks) {
        boolean changed = false;
        boolean openToSky = level.canSeeSky(pos.above());
        boolean rainingHere = openToSky && level.isRainingAt(pos.above());
        if (rainingHere) {
            changed |= this.tickRainRefill(level, pos, random);
        } else if (this.contentType == BirdBathContentType.WATER && openToSky && level.isDay() && random.nextInt(EVAPORATION_CHANCE) == 0) {
            changed |= this.reduceWaterByEvaporation(level, pos);
        }
        if (openToSky) {
            changed |= this.tickFreezeAndMelt(level, pos, random);
        }
        changed |= this.tickSpoilage(level, pos, elapsedTicks);
        changed |= this.tickLongTermDirt(random);
        if (this.isDirty() || this.isSpoiled()) {
            BirdBathEffects.idleDirty(level, pos, this.cleanliness, this.isSpoiled());
        }
        return changed;
    }

    public AABB getRenderBoundingBox() {
        BlockPos pos = this.getBlockPos();
        return new AABB(pos).inflate(1.25D, 0.25D, 1.25D).expandTowards(0.0D, 2.5D, 0.0D);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(CONTENT_TYPE_TAG, this.contentType.ordinal());
        tag.putInt(CONTENT_LEVEL_TAG, this.contentLevel);
        tag.putInt(CLEANLINESS_TAG, this.cleanliness.ordinal());
        tag.putInt(SPOILED_CONTENT_TYPE_TAG, this.spoiledContentType.ordinal());
        tag.putInt(SPOIL_TICKS_TAG, this.spoilTicks);
        tag.putInt(ENVIRONMENTAL_TICK_OFFSET_TAG, this.environmentalTickOffset);
        tag.putInt(OCCUPIED_TICKS_TAG, this.occupiedTicks);
        tag.putInt(RECENT_BIRD_USE_TICKS_TAG, this.recentBirdUseTicks);
        if (this.currentUser != null) {
            tag.putUUID(CURRENT_USER_TAG, this.currentUser);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        BirdBathContentType loadedType = tag.contains(CONTENT_TYPE_TAG) ? BirdBathContentType.fromOrdinal(tag.getInt(CONTENT_TYPE_TAG)) : BirdBathContentType.EMPTY;
        int loadedLevel = tag.contains(CONTENT_LEVEL_TAG) ? Mth.clamp(tag.getInt(CONTENT_LEVEL_TAG), 0, 3) : 0;
        if (loadedType.isEmpty() || loadedLevel <= 0) {
            this.contentType = BirdBathContentType.EMPTY;
            this.contentLevel = 0;
        } else {
            this.contentType = loadedType;
            this.contentLevel = loadedLevel;
        }
        this.cleanliness = tag.contains(CLEANLINESS_TAG) ? BirdBathCleanliness.fromOrdinal(tag.getInt(CLEANLINESS_TAG)) : BirdBathCleanliness.CLEAN;
        this.spoiledContentType = tag.contains(SPOILED_CONTENT_TYPE_TAG) ? BirdBathContentType.fromOrdinal(tag.getInt(SPOILED_CONTENT_TYPE_TAG)) : BirdBathContentType.EMPTY;
        if (this.contentType != BirdBathContentType.SPOILED || !this.spoiledContentType.isFood()) {
            this.spoiledContentType = BirdBathContentType.EMPTY;
        }
        this.spoilTicks = tag.contains(SPOIL_TICKS_TAG) ? Math.max(0, tag.getInt(SPOIL_TICKS_TAG)) : 0;
        this.environmentalTickOffset = tag.contains(ENVIRONMENTAL_TICK_OFFSET_TAG) ? tag.getInt(ENVIRONMENTAL_TICK_OFFSET_TAG) : this.environmentalTickOffset;
        this.occupiedTicks = tag.contains(OCCUPIED_TICKS_TAG) ? Math.max(0, tag.getInt(OCCUPIED_TICKS_TAG)) : 0;
        this.recentBirdUseTicks = tag.contains(RECENT_BIRD_USE_TICKS_TAG) ? Math.max(0, tag.getInt(RECENT_BIRD_USE_TICKS_TAG)) : 0;
        this.currentUser = tag.hasUUID(CURRENT_USER_TAG) ? tag.getUUID(CURRENT_USER_TAG) : null;
        if (this.occupiedTicks <= 0) {
            this.currentUser = null;
        }
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
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
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

    private void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    private boolean tickOccupation() {
        boolean changed = false;
        if (this.occupiedTicks > 0) {
            this.occupiedTicks = Math.max(0, this.occupiedTicks - 1);
            this.setChanged();
            if (this.occupiedTicks == 0) {
                this.currentUser = null;
                changed = true;
            }
        }
        if (this.recentBirdUseTicks > 0) {
            this.recentBirdUseTicks = Math.max(0, this.recentBirdUseTicks - 1);
            this.setChanged();
            if (this.recentBirdUseTicks == 0) {
                changed = true;
            }
        }
        return changed;
    }

    private boolean tickRainRefill(ServerLevel level, BlockPos pos, RandomSource random) {
        boolean changed = false;
        if (this.isEmpty() || this.contentType == BirdBathContentType.WATER) {
            int oldLevel = this.contentLevel;
            BirdBathContentType oldType = this.contentType;
            int newLevel = Math.min(MAX_CONTENT_LEVEL, (this.isEmpty() ? 0 : this.contentLevel) + RAIN_REFILL_AMOUNT);
            this.contentType = BirdBathContentType.WATER;
            this.contentLevel = newLevel;
            this.spoilTicks = 0;
            changed = oldType != this.contentType || oldLevel != newLevel;
            if (changed) {
                BirdBathEffects.waterAdded(level, pos, SoundEvents.WEATHER_RAIN);
            }
        }
        if (!this.isSpoiled() && this.cleanliness.isDirty() && random.nextInt(3) == 0) {
            this.cleanliness = this.cleanliness.cleanOneStep();
            changed = true;
        }
        return changed;
    }

    private boolean reduceWaterByEvaporation(ServerLevel level, BlockPos pos) {
        if (this.contentType != BirdBathContentType.WATER || this.contentLevel <= 0) {
            return false;
        }
        this.contentLevel--;
        if (this.contentLevel <= 0) {
            this.contentType = BirdBathContentType.EMPTY;
            this.contentLevel = 0;
        }
        BirdBathEffects.evaporated(level, pos);
        return true;
    }

    private boolean tickFreezeAndMelt(ServerLevel level, BlockPos pos, RandomSource random) {
        boolean coldEnough = level.getBiome(pos).value().coldEnoughToSnow(pos);
        if (this.contentType == BirdBathContentType.WATER && coldEnough && random.nextInt(3) == 0) {
            this.contentType = BirdBathContentType.FROZEN_WATER;
            this.spoilTicks = 0;
            BirdBathEffects.froze(level, pos);
            return true;
        }
        if (this.contentType == BirdBathContentType.FROZEN_WATER && !coldEnough && level.isDay() && random.nextInt(4) == 0) {
            this.contentType = BirdBathContentType.WATER;
            BirdBathEffects.melted(level, pos);
            return true;
        }
        return false;
    }

    private boolean tickSpoilage(ServerLevel level, BlockPos pos, int elapsedTicks) {
        if (!this.contentType.isFood()) {
            return false;
        }
        int increment = Math.max(1, elapsedTicks);
        if (level.isDay() && level.canSeeSky(pos.above()) && !level.isRaining()) {
            increment += Math.max(1, elapsedTicks / SUNLIGHT_SPOIL_BONUS_DIVISOR);
        }
        this.spoilTicks += increment;
        int threshold = this.contentType == BirdBathContentType.BREAD ? BREAD_SPOIL_TICKS : FISH_MEAT_SPOIL_TICKS;
        if (this.spoilTicks >= threshold) {
            this.spoiledContentType = this.contentType;
            this.contentType = BirdBathContentType.SPOILED;
            this.cleanliness = dirtierOf(this.cleanliness, BirdBathCleanliness.DIRTY);
            this.spoilTicks = 0;
            BirdBathEffects.spoiled(level, pos);
        }
        return true;
    }

    private boolean tickLongTermDirt(RandomSource random) {
        if (this.contentType != BirdBathContentType.WATER || this.cleanliness == BirdBathCleanliness.FILTHY || random.nextInt(WATER_DIRT_CHANCE) != 0) {
            return false;
        }
        this.cleanliness = this.cleanliness.nextDirtier();
        return true;
    }

    private static BirdBathCleanliness dirtierOf(BirdBathCleanliness first, BirdBathCleanliness second) {
        return first.ordinal() >= second.ordinal() ? first : second;
    }
}
