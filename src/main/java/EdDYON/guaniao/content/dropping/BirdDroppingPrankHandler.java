package EdDYON.guaniao.content.dropping;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BirdDroppingPrankHandler {
    private static final ResourceLocation SKY_DELIVERY_ADVANCEMENT = new ResourceLocation(GuaniaoMod.MOD_ID, "husbandry/sky_delivery");
    private static final ResourceLocation LUCKY_ADVANCEMENT = new ResourceLocation(GuaniaoMod.MOD_ID, "husbandry/good_luck_i_guess");
    private static final ResourceLocation HELMET_ADVANCEMENT = new ResourceLocation(GuaniaoMod.MOD_ID, "husbandry/helmet_saved_the_day");
    private static final ResourceLocation VILLAGER_ADVANCEMENT = new ResourceLocation(GuaniaoMod.MOD_ID, "husbandry/not_very_civilized");
    private static final ResourceLocation CAKE_ADVANCEMENT = new ResourceLocation(GuaniaoMod.MOD_ID, "husbandry/cake_no_longer_cake");

    private static final String VILLAGER_HIT_PREFIX = "GuaniaoDroppingHitBy_";
    private static final String VILLAGER_TRADE_PENALTY = "GuaniaoDroppingTradePenalty";
    private static final String VILLAGER_TRADE_PENALTY_UNTIL = "GuaniaoDroppingTradePenaltyUntil";
    private static final String GOLEM_HIT_PREFIX = "GuaniaoDroppingGolemHitBy_";
    private static final String GOLEM_HIT_TIME_PREFIX = "GuaniaoDroppingGolemHitTimeBy_";
    private static final int VILLAGER_TRADE_PENALTY_TICKS = 20 * 60 * 3;
    private static final int VILLAGER_TRADE_PENALTY_BASE = 2;
    private static final int VILLAGER_TRADE_PENALTY_MAX = 12;
    private static final int GOLEM_MEMORY_TICKS = 20 * 60;
    private static final float LUCKY_HIT_CHANCE = 0.05F;
    private static final Map<ServerLevel, Map<UUID, Long>> TRACKED_VILLAGERS = new WeakHashMap<>();
    private static final Map<ServerLevel, Long> NEXT_VILLAGER_EXPIRY = new WeakHashMap<>();

    private BirdDroppingPrankHandler() {
    }

    public static void handleNaturalHitPlayer(BirdDroppingProjectileEntity projectile, Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        RandomSource random = level.random;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        spawnPoof(level, player.position().add(0.0D, player.getBbHeight() * 0.72D, 0.0D), 8);

        if (!helmet.isEmpty()) {
            if (!player.getAbilities().instabuild && helmet.isDamageableItem()) {
                helmet.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(EquipmentSlot.HEAD));
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0));
            playWetHit(level, player.position(), 0.55F);
            BirdDroppingMessageUtil.sendNearby(level, player.position(), BirdDroppingMessageUtil.randomComponent("message.guaniao.helmet_blocked_dropping", random, player.getDisplayName()));
            if (player instanceof ServerPlayer serverPlayer) {
                BirdDroppingMessageUtil.grant(serverPlayer, HELMET_ADVANCEMENT, "blocked_with_helmet");
            }
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100 + random.nextInt(41), 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + random.nextInt(21), 0));
        playWetHit(level, player.position(), 0.75F);
        playLaughingBirds(level, player, random);
        BirdDroppingMessageUtil.sendNearby(level, player.position(), BirdDroppingMessageUtil.randomComponent("message.guaniao.sky_delivery", random, player.getDisplayName()));
        player.displayClientMessage(BirdDroppingMessageUtil.randomComponent("message.guaniao.no_helmet_delivery", random), true);

        if (player instanceof ServerPlayer serverPlayer) {
            BirdDroppingMessageUtil.grant(serverPlayer, SKY_DELIVERY_ADVANCEMENT, "natural_hit");
            if (random.nextFloat() < LUCKY_HIT_CHANCE) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 200 + random.nextInt(201), 0));
                player.sendSystemMessage(BirdDroppingMessageUtil.randomComponent("message.guaniao.lucky_dropping", random));
                BirdDroppingMessageUtil.grant(serverPlayer, LUCKY_ADVANCEMENT, "lucky_hit");
            }
        }
    }

    public static void handleVillagerHit(BirdDroppingProjectileEntity projectile, Villager villager) {
        if (!(villager.level() instanceof ServerLevel level)) {
            return;
        }

        Entity owner = projectile.getOwner();
        Player responsible = owner instanceof Player player ? player : level.getNearestPlayer(villager, 12.0D);
        if (responsible != null) {
            villager.getLookControl().setLookAt(responsible, 30.0F, 30.0F);
        }

        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, villager.getX(), villager.getY(0.85D), villager.getZ(), 5, 0.25D, 0.22D, 0.25D, 0.02D);
        level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.75F, 0.85F + level.random.nextFloat() * 0.2F);

        if (owner instanceof ServerPlayer serverPlayer) {
            String key = VILLAGER_HIT_PREFIX + serverPlayer.getUUID();
            CompoundTag data = villager.getPersistentData();
            int count = data.getInt(key) + 1;
            data.putInt(key, count);
            applyTemporaryVillagerTradePenalty(level, villager, count);
            serverPlayer.displayClientMessage(BirdDroppingMessageUtil.randomComponent("message.guaniao.villager_hit_reaction", level.random), true);
            if (count >= 3) {
                BirdDroppingMessageUtil.grant(serverPlayer, VILLAGER_ADVANCEMENT, "hit_same_villager");
            }
        }
    }

    public static void handleIronGolemHit(BirdDroppingProjectileEntity projectile, IronGolem golem) {
        if (!(golem.level() instanceof ServerLevel level)) {
            return;
        }

        Entity owner = projectile.getOwner();
        Player responsible = owner instanceof Player player ? player : level.getNearestPlayer(golem, 16.0D);
        if (responsible != null) {
            golem.getLookControl().setLookAt(responsible, 30.0F, 30.0F);
        }

        int warningLevel = 1;
        if (owner instanceof ServerPlayer serverPlayer) {
            CompoundTag data = golem.getPersistentData();
            String countKey = GOLEM_HIT_PREFIX + serverPlayer.getUUID();
            String timeKey = GOLEM_HIT_TIME_PREFIX + serverPlayer.getUUID();
            long now = level.getGameTime();
            int count = now - data.getLong(timeKey) > GOLEM_MEMORY_TICKS ? 0 : data.getInt(countKey);
            count = Math.min(3, count + 1);
            data.putInt(countKey, count);
            data.putLong(timeKey, now);
            warningLevel = count;
            if (count >= 3) {
                serverPlayer.sendSystemMessage(BirdDroppingMessageUtil.randomComponent("message.guaniao.golem_warning", level.random));
                makeGolemRetaliate(golem, serverPlayer);
            }
        }

        int particles = warningLevel == 1 ? 2 : warningLevel == 2 ? 5 : 8;
        level.sendParticles(warningLevel >= 2 ? ParticleTypes.ANGRY_VILLAGER : ParticleTypes.POOF, golem.getX(), golem.getY(0.72D), golem.getZ(), particles, 0.32D, 0.28D, 0.32D, 0.02D);
        SoundEvent sound = warningLevel >= 3 ? SoundEvents.IRON_GOLEM_HURT : SoundEvents.IRON_GOLEM_REPAIR;
        level.playSound(null, golem.getX(), golem.getY(), golem.getZ(), sound, SoundSource.NEUTRAL, 0.65F, 0.75F + level.random.nextFloat() * 0.2F);
    }

    public static void tickVillagerTradePenalty(Villager villager) {
        if (!(villager.level() instanceof ServerLevel level)) {
            return;
        }

        CompoundTag data = villager.getPersistentData();
        if (!data.contains(VILLAGER_TRADE_PENALTY_UNTIL) || !data.contains(VILLAGER_TRADE_PENALTY)) {
            return;
        }

        if (level.getGameTime() < data.getLong(VILLAGER_TRADE_PENALTY_UNTIL)) {
            return;
        }

        int penalty = data.getInt(VILLAGER_TRADE_PENALTY);
        if (penalty > 0) {
            adjustVillagerTradePrices(villager, -penalty);
        }
        data.remove(VILLAGER_TRADE_PENALTY);
        data.remove(VILLAGER_TRADE_PENALTY_UNTIL);
    }

    public static void trackVillager(Villager villager) {
        if (!(villager.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag data = villager.getPersistentData();
        if (data.contains(VILLAGER_TRADE_PENALTY_UNTIL) && data.contains(VILLAGER_TRADE_PENALTY)) {
            long expiresAt = data.getLong(VILLAGER_TRADE_PENALTY_UNTIL);
            TRACKED_VILLAGERS.computeIfAbsent(level, ignored -> new HashMap<>())
                    .put(villager.getUUID(), expiresAt);
            NEXT_VILLAGER_EXPIRY.merge(level, expiresAt, Math::min);
        }
    }

    public static void tickTrackedVillagers(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Map<UUID, Long> tracked = TRACKED_VILLAGERS.get(level);
            if (tracked == null || tracked.isEmpty()) {
                continue;
            }
            long now = level.getGameTime();
            if (now < NEXT_VILLAGER_EXPIRY.getOrDefault(level, Long.MAX_VALUE)) {
                continue;
            }
            long nextExpiry = Long.MAX_VALUE;
            Iterator<Map.Entry<UUID, Long>> iterator = tracked.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Long> entry = iterator.next();
                if (now < entry.getValue()) {
                    nextExpiry = Math.min(nextExpiry, entry.getValue());
                    continue;
                }
                Entity entity = level.getEntity(entry.getKey());
                if (entity instanceof Villager villager) {
                    tickVillagerTradePenalty(villager);
                }
                iterator.remove();
            }
            if (tracked.isEmpty()) {
                TRACKED_VILLAGERS.remove(level);
                NEXT_VILLAGER_EXPIRY.remove(level);
            } else {
                NEXT_VILLAGER_EXPIRY.put(level, nextExpiry);
            }
        }
    }

    public static void forgetLevel(ServerLevel level) {
        TRACKED_VILLAGERS.remove(level);
        NEXT_VILLAGER_EXPIRY.remove(level);
    }

    public static boolean handleCakeHit(BirdDroppingProjectileEntity projectile, BlockPos pos) {
        if (!(projectile.level() instanceof ServerLevel level)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CakeBlock)) {
            return false;
        }

        Vec3 cakeTop = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.58D, pos.getZ() + 0.5D);
        PollutedCakeData.get(level).pollute(level, pos);
        level.playSound(null, cakeTop.x, cakeTop.y, cakeTop.z, SoundEvents.SLIME_BLOCK_HIT, SoundSource.NEUTRAL, 0.82F, 0.48F + level.random.nextFloat() * 0.16F);
        BirdDroppingMessageUtil.sendNearby(level, cakeTop, BirdDroppingMessageUtil.randomComponent("message.guaniao.cake_ruined", level.random));

        if (projectile.getOwner() instanceof ServerPlayer serverPlayer) {
            BirdDroppingMessageUtil.grant(serverPlayer, CAKE_ADVANCEMENT, "ruin_cake");
        }
        return true;
    }

    private static void spawnPoof(ServerLevel level, Vec3 position, int count) {
        level.sendParticles(ParticleTypes.POOF, position.x, position.y, position.z, count, 0.16D, 0.12D, 0.16D, 0.01D);
    }

    private static void playWetHit(ServerLevel level, Vec3 position, float volume) {
        level.playSound(null, position.x, position.y, position.z, SoundEvents.SLIME_BLOCK_HIT, SoundSource.NEUTRAL, volume, 0.8F + level.random.nextFloat() * 0.35F);
    }

    private static void applyTemporaryVillagerTradePenalty(ServerLevel level, Villager villager, int hitCount) {
        CompoundTag data = villager.getPersistentData();
        int previousPenalty = data.getInt(VILLAGER_TRADE_PENALTY);
        if (previousPenalty > 0) {
            adjustVillagerTradePrices(villager, -previousPenalty);
        }

        int penalty = Math.min(VILLAGER_TRADE_PENALTY_MAX, VILLAGER_TRADE_PENALTY_BASE + Math.max(0, hitCount - 1) * 2);
        if (adjustVillagerTradePrices(villager, penalty)) {
            data.putInt(VILLAGER_TRADE_PENALTY, penalty);
            data.putLong(VILLAGER_TRADE_PENALTY_UNTIL, level.getGameTime() + VILLAGER_TRADE_PENALTY_TICKS);
            trackVillager(villager);
        } else {
            data.remove(VILLAGER_TRADE_PENALTY);
            data.remove(VILLAGER_TRADE_PENALTY_UNTIL);
        }
    }

    private static boolean adjustVillagerTradePrices(Villager villager, int diff) {
        boolean changed = false;
        for (MerchantOffer offer : villager.getOffers()) {
            offer.addToSpecialPriceDiff(diff);
            changed = true;
        }
        return changed;
    }

    private static void makeGolemRetaliate(IronGolem golem, ServerPlayer player) {
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            return;
        }
        golem.setTarget(player);
        golem.setAggressive(true);
        golem.getNavigation().moveTo(player, 1.1D);
    }

    private static void playLaughingBirds(ServerLevel level, LivingEntity target, RandomSource random) {
        SoundEvent[] sounds = new SoundEvent[] {
                GuaniaoSoundEvents.SPARROW_AMBIENT.get(),
                GuaniaoSoundEvents.BUDGERIGAR_AMBIENT.get(),
                GuaniaoSoundEvents.PIGEON_AMBIENT.get()
        };
        int count = 2 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            SoundEvent sound = sounds[random.nextInt(sounds.length)];
            double x = target.getX() + (random.nextDouble() - 0.5D) * 5.0D;
            double y = target.getY(0.7D) + random.nextDouble() * 1.5D;
            double z = target.getZ() + (random.nextDouble() - 0.5D) * 5.0D;
            level.playSound(null, x, y, z, sound, SoundSource.AMBIENT, 0.55F, 0.85F + random.nextFloat() * 0.45F);
        }
    }
}
