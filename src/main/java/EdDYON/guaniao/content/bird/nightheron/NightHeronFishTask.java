package EdDYON.guaniao.content.bird.nightheron;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** An occasional gift in the existing mouth slot; no fishing trip or world scan. */
public final class NightHeronFishTask {
    public static final String DELIVERED_MARKER = "guaniao:night_heron_delivery";
    static final float CATCH_CHANCE = 0.5F;
    private final NightHeronEntity bird;
    private int giftRemaining = -1;
    private boolean delivering;

    NightHeronFishTask(NightHeronEntity bird) { this.bird = bird; }

    boolean permitsTravel() {
        BirdCommandMode mode = this.bird.getBirdCommandMode();
        return mode == BirdCommandMode.FREE || mode == BirdCommandMode.FOLLOW;
    }

    Player owner() {
        if (!(this.bird.level() instanceof ServerLevel level) || !this.bird.isTame() || this.bird.getOwnerUUID() == null) return null;
        Player player = level.getPlayerByUUID(this.bird.getOwnerUUID());
        return player != null && player.isAlive() && !player.isSpectator() && player.level() == level ? player : null;
    }

    Player nearbyOwner() {
        Player player = this.owner();
        double range = BirdConfigManager.nightHeronOwnerRange();
        return player != null && this.bird.distanceToSqr(player) <= range * range ? player : null;
    }

    boolean safe() {
        return this.bird.isAlive() && !this.bird.isNoAi() && !this.bird.isPassenger()
                && !this.bird.isBirdEmergencyOverrideActive() && this.bird.birdBrain().motivation().fear() < 0.55F;
    }

    void tick() {
        if (this.bird.level().isClientSide || !this.bird.isTame() || !BirdConfigManager.nightHeronGiftsEnabled()
                || this.bird.hasHeldFishForRendering() || !this.permitsTravel() || this.nearbyOwner() == null
                || !this.safe() || !this.bird.isActiveTime()) return;
        if (this.giftRemaining < 0) this.resetGiftTimer();
        if (this.giftRemaining > 0) --this.giftRemaining;
        if (this.giftRemaining == 0) {
            // Consume the attempt even on failure. Never accumulate offline gifts.
            this.resetGiftTimer();
            if (this.bird.getRandom().nextFloat() < CATCH_CHANCE)
                this.bird.setHeldFish(new ItemStack(this.bird.getRandom().nextBoolean() ? Items.COD : Items.SALMON), HeldFishPurpose.OWNER_DELIVERY);
        }
    }

    private int betweenSeconds(int min, int max) { return 20 * (min + this.bird.getRandom().nextInt(max - min + 1)); }
    private void resetGiftTimer() { this.giftRemaining = betweenSeconds(BirdConfigManager.nightHeronGiftMinSeconds(), BirdConfigManager.nightHeronGiftMaxSeconds()); }

    private static boolean canAccept(Player player, ItemStack fish) {
        if (player.getInventory().getFreeSlot() >= 0) return true;
        for (ItemStack stack : player.getInventory().items)
            if (ItemStack.isSameItemSameComponents(stack, fish) && stack.getCount() < Math.min(stack.getMaxStackSize(), player.getInventory().getMaxStackSize())) return true;
        ItemStack offhand = player.getOffhandItem();
        return ItemStack.isSameItemSameComponents(offhand, fish) && offhand.getCount() < offhand.getMaxStackSize();
    }

    boolean deliver(Player owner, boolean intoInventory) {
        if (this.delivering || this.bird.level().isClientSide || !this.bird.isAlive() || !this.bird.hasDeliveryFish()
                || !this.bird.isOwnedBy(owner) || !owner.isAlive() || owner.level() != this.bird.level()
                || this.bird.distanceToSqr(owner) > 9 || !this.bird.hasLineOfSight(owner)
                || !intoInventory && (!this.permitsTravel() || !this.safe() || this.nearbyOwner() != owner)) return false;
        ItemStack fish = this.bird.getHeldFishForRendering();
        if (!canAccept(owner, fish)) return false;
        this.delivering = true;
        try {
            boolean transferred;
            if (intoInventory) {
                ItemStack remaining = fish.copy();
                owner.getInventory().add(remaining);
                transferred = remaining.isEmpty();
            } else {
                Vec3 direction = owner.getLookAngle().multiply(1, 0, 1).normalize();
                BlockPos dropPos = NightHeronFishing.standNear(this.bird, owner.position().add(direction.scale(0.8)), 1);
                if (dropPos == null || Vec3.atBottomCenterOf(dropPos).distanceToSqr(owner.position()) > 4) return false;
                Vec3 position = Vec3.atBottomCenterOf(dropPos).add(0, 0.1, 0);
                if (this.bird.level().clip(new net.minecraft.world.level.ClipContext(owner.getEyePosition(), position,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE,
                        owner)).getType() != net.minecraft.world.phys.HitResult.Type.MISS) return false;
                ItemEntity drop = new ItemEntity(this.bird.level(), position.x, position.y, position.z, fish.copy());
                drop.setDeltaMovement(Vec3.ZERO);
                drop.setTarget(owner.getUUID());
                drop.setPickUpDelay(10);
                drop.getPersistentData().putBoolean(DELIVERED_MARKER, true);
                transferred = this.bird.level().addFreshEntity(drop);
            }
            if (!transferred) return false;
            this.bird.setHeldFish(ItemStack.EMPTY, HeldFishPurpose.NONE);
            this.bird.setOfferTicks(0);
            this.bird.playSound(EdDYON.guaniao.registry.GuaniaoSoundEvents.NIGHT_HERON_AMBIENT.get(), 0.45F, 1.0F);
            this.resetGiftTimer();
            return true;
        } finally { this.delivering = false; }
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("GiftRemaining", this.giftRemaining);
        return tag;
    }

    void load(CompoundTag tag) {
        this.giftRemaining = -1;
        if (!this.bird.isTame() || tag.getInt("Version") != 1) return;
        this.giftRemaining = tag.contains("GiftRemaining", 3) ? Mth.clamp(tag.getInt("GiftRemaining"), -1, 72000) : -1;
        // Old shoreline jobs are ignored; the existing owner and mouth slot are loaded by the entity.
    }
}
