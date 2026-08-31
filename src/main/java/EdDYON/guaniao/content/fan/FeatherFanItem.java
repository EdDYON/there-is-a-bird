package EdDYON.guaniao.content.fan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import EdDYON.guaniao.registry.GuaniaoParticleTypes;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Comparator;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public class FeatherFanItem extends Item {
    private static final int USE_DURATION_TICKS = 72000;
    private static final int MIN_CHARGE_TICKS = 5;
    private static final int MAX_CHARGE_TICKS = 30;
    private static final float CHARGE_POSE_SETTLE_TICKS = MAX_CHARGE_TICKS;
    private static final float MIN_THROW_SPEED = 0.8F;
    private static final float MAX_THROW_SPEED = 1.6F;
    private static final double MELEE_KNOCKBACK = 0.3D;
    private static final int HUNT_LOCK_DELAY_TICKS = 6;
    private static final int HUNT_LOCK_INTERVAL_TICKS = 4;
    private static final int HUNT_MAX_LOCKED_TARGETS = 7;
    private static final double HUNT_LOCK_RANGE = 18.0D;
    private static final double HUNT_LOCK_MIN_DOT = Math.cos(Math.toRadians(52.0D));

    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public FeatherFanItem(Properties properties) {
        super(properties);
        this.defaultModifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Fan damage modifier", 4.0D, AttributeModifier.Operation.ADDITION))
                .put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Fan speed modifier", -2.1D, AttributeModifier.Operation.ADDITION))
                .build();
    }

    @Override
    public Component getName(ItemStack stack) {
        if (FeatherFanEnchantments.hasHuntingReturn(stack)) {
            return Component.translatable("item.guaniao.wind_feather_fan.hunting");
        }
        if (FeatherFanEnchantments.hasRivenPlume(stack)) {
            return Component.translatable("item.guaniao.wind_feather_fan.riven");
        }
        if (FeatherFanEnchantments.hasBurialPlume(stack)) {
            return Component.translatable("item.guaniao.wind_feather_fan.burial");
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (FeatherFanEnchantments.hasHuntingReturn(stack)) {
            addSpecialFanTooltip(tooltip, "hunting", ChatFormatting.GOLD);
        } else if (FeatherFanEnchantments.hasRivenPlume(stack)) {
            addSpecialFanTooltip(tooltip, "riven", ChatFormatting.BLUE);
        } else if (FeatherFanEnchantments.hasBurialPlume(stack)) {
            addSpecialFanTooltip(tooltip, "burial", ChatFormatting.DARK_AQUA);
        }
    }

    private static void addSpecialFanTooltip(List<Component> tooltip, String variant, ChatFormatting abilityColor) {
        String key = "item.guaniao.wind_feather_fan." + variant + ".tooltip";
        tooltip.add(Component.translatable(key + ".legend")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(key + ".ability").withStyle(abilityColor));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                                   float partialTick, float equipProgress, float swingProgress) {
                InteractionHand renderedHand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                if (!player.isUsingItem()
                        || player.getUsedItemHand() != renderedHand
                        || !ItemStack.isSameItemSameTags(player.getUseItem(), itemInHand)) {
                    return false;
                }

                int usedTicks = FeatherFanItem.this.getUseDuration(itemInHand) - player.getUseItemRemainingTicks();
                float elapsedTicks = usedTicks + partialTick;
                float chargeProgress = Mth.clamp(elapsedTicks / MAX_CHARGE_TICKS, 0.0F, 1.0F);
                float chargeEase = smootherStep(chargeProgress);
                float progress = getDampedPoseProgress(elapsedTicks);
                float tensionEnvelope = 4.0F * chargeProgress * (1.0F - chargeProgress);
                float tensionWave = Mth.sin(elapsedTicks * 0.55F) * tensionEnvelope;
                float handDirection = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
                poseStack.translate(
                        Mth.lerp(progress, handDirection * 0.56F, handDirection * 0.10F)
                                + handDirection * tensionWave * 0.003F,
                        Mth.lerp(progress, -0.52F - equipProgress * 0.6F, -0.42F)
                                - chargeEase * 0.018F
                                + Mth.cos(elapsedTicks * 0.48F) * tensionEnvelope * 0.0015F,
                        Mth.lerp(progress, -0.72F, -0.70F) + chargeEase * 0.025F
                );
                float preChargeRotation = Mth.sin(elapsedTicks * 0.45F) * tensionEnvelope * 0.6F;
                float shakeRotation = 0.0F;
                if (usedTicks >= MAX_CHARGE_TICKS) {
                    float shakeTime = usedTicks - MAX_CHARGE_TICKS + partialTick;
                    poseStack.translate(
                            Mth.sin(shakeTime * 2.6F) * 0.006F,
                            Mth.sin(shakeTime * 3.3F) * 0.004F,
                            0.0F
                    );
                    shakeRotation = Mth.sin(shakeTime * 2.9F) * 1.5F;
                }
                double modelOffsetX = handDirection * 0.9D / 16.0D;
                double modelOffsetY = 3.5D / 16.0D;
                double modelOffsetZ = 1.0D / 16.0D;
                poseStack.translate(modelOffsetX, modelOffsetY, modelOffsetZ);
                poseStack.mulPose(Axis.XP.rotationDegrees(18.0F * progress + 7.0F * chargeEase));
                poseStack.mulPose(Axis.ZP.rotationDegrees(handDirection
                        * (90.0F * progress + preChargeRotation + shakeRotation)));
                poseStack.translate(-modelOffsetX, -modelOffsetY, -modelOffsetZ);
                return true;
            }
        });
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            target.knockback(MELEE_KNOCKBACK, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        }
        stack.hurtAndBreak(1, attacker, broken -> broken.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int chargeTicks = this.getUseDuration(stack) - remainingUseDuration;
        if (living instanceof Player player && FeatherFanEnchantments.hasHuntingReturn(stack)) {
            int lockCapacity = getHuntingLockCapacity(chargeTicks);
            if (lockCapacity > 0 && chargeTicks % 2 == 0) {
                List<LivingEntity> targets = findHuntingTargets(player, lockCapacity);
                for (int index = 0; index < targets.size(); index++) {
                    this.spawnHuntingLockEffect(serverLevel, targets.get(index), chargeTicks, index);
                }
                if (isHuntingLockMilestone(chargeTicks) && targets.size() >= lockCapacity) {
                    LivingEntity newestTarget = targets.get(lockCapacity - 1);
                    level.playSound(null, newestTarget.getX(), newestTarget.getY(), newestTarget.getZ(),
                            GuaniaoSoundEvents.FEATHER_FAN_HUNT_LOCK.get(),
                            SoundSource.PLAYERS, 0.52F, 0.96F + lockCapacity * 0.075F);
                }
            }
        }

        float charge = Mth.clamp(chargeTicks / (float)MAX_CHARGE_TICKS, 0.0F, 1.0F);
        if (chargeTicks > 0 && chargeTicks % 4 == 0) {
            Vec3 center = living.getEyePosition()
                    .add(living.getLookAngle().scale(0.78D))
                    .add(0.0D, -0.38D, 0.0D);
            int moteCount = 1 + Mth.floor(charge);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, center.x, center.y, center.z, moteCount, 0.08D + charge * 0.05D, 0.07D, 0.08D + charge * 0.05D, 0.008D);
            if (chargeTicks >= MAX_CHARGE_TICKS) {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, 1, 0.12D, 0.08D, 0.12D, 0.008D);
            }
        }

        if (chargeTicks == 1) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.2F, 1.45F);
        } else if (chargeTicks == 10 || chargeTicks == 20) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.35F, 1.35F - chargeTicks * 0.012F);
        } else if (chargeTicks == MAX_CHARGE_TICKS) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.55F, 1.35F);
            serverLevel.sendParticles(ParticleTypes.POOF, living.getX(), living.getEyeY() - 0.2D, living.getZ(), 2, 0.12D, 0.08D, 0.12D, 0.018D);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, living.getX(), living.getEyeY() - 0.2D, living.getZ(), 5, 0.18D, 0.12D, 0.18D, 0.012D);
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, living.getX(), living.getEyeY() - 0.2D, living.getZ(), 3, 0.14D, 0.10D, 0.14D, 0.008D);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!(living instanceof Player player) || level.isClientSide) {
            return;
        }

        int chargeTicks = this.getUseDuration(stack) - timeLeft;
        if (chargeTicks < MIN_CHARGE_TICKS) {
            return;
        }

        InteractionHand hand = player.getUsedItemHand();
        float charge = getCharge(chargeTicks);
        List<LivingEntity> huntingTargets = List.of();
        if (FeatherFanEnchantments.hasHuntingReturn(stack)) {
            huntingTargets = findHuntingTargets(player, getHuntingLockCapacity(chargeTicks));
            if (huntingTargets.isEmpty()) {
                player.getCooldowns().addCooldown(this, 6);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.35F, 0.72F);
                return;
            }
        }
        this.launchFan(stack, level, player, hand, charge, false, huntingTargets);
    }

    public static boolean isFullyCharged(LivingEntity living) {
        if (!living.isUsingItem() || !(living.getUseItem().getItem() instanceof FeatherFanItem fan)) {
            return false;
        }
        int chargeTicks = fan.getUseDuration(living.getUseItem()) - living.getUseItemRemainingTicks();
        return chargeTicks >= MAX_CHARGE_TICKS;
    }

    public boolean tryLaunchPiercing(ServerPlayer player) {
        if (player.getCooldowns().isOnCooldown(this) || !isFullyCharged(player)) {
            return false;
        }

        InteractionHand hand = player.getUsedItemHand();
        ItemStack stack = player.getUseItem();
        player.stopUsingItem();
        return this.launchFan(stack, player.level(), player, hand, 1.0F, true, List.of());
    }

    private boolean launchFan(ItemStack stack, Level level, Player player, InteractionHand hand,
                              float charge, boolean piercing, List<LivingEntity> huntingTargets) {
        boolean hunting = !piercing && !huntingTargets.isEmpty();
        LivingEntity primaryHuntingTarget = hunting ? huntingTargets.get(0) : null;
        float speed = piercing ? 2.65F
                : hunting ? FeatherFanProjectileEntity.HUNT_SPEED
                : Mth.lerp(charge, MIN_THROW_SPEED, MAX_THROW_SPEED);
        ItemStack thrownStack = stack.copy();
        thrownStack.setCount(1);
        thrownStack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
        if (thrownStack.isEmpty()) {
            stack.shrink(1);
            player.awardStat(Stats.ITEM_USED.get(this));
            return false;
        }

        FeatherFanProjectileEntity projectile = new FeatherFanProjectileEntity(level, player);
        if (piercing) {
            projectile.configurePiercing(thrownStack, hand);
        } else if (hunting) {
            projectile.configureHunting(thrownStack, hand, charge, huntingTargets);
        } else {
            projectile.configureThrow(thrownStack, hand, charge);
        }
        if (hunting) {
            Vec3 direction = primaryHuntingTarget.getBoundingBox().getCenter().subtract(projectile.position());
            if (direction.lengthSqr() > 1.0E-6D) {
                projectile.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
            }
        } else {
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, 0.0F);
        }
        if (level.addFreshEntity(projectile)) {
            stack.shrink(1);
            player.awardStat(Stats.ITEM_USED.get(this));
            player.swing(hand, true);
            if (piercing) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.55F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.85F, 1.25F);
                if (level instanceof ServerLevel serverLevel) {
                    Vec3 launch = player.getEyePosition().add(player.getLookAngle().scale(1.15D));
                    serverLevel.sendParticles(ParticleTypes.WHITE_ASH, launch.x, launch.y, launch.z,
                            6, 0.06D, 0.05D, 0.06D, 0.025D);
                }
            } else if (hunting) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        GuaniaoSoundEvents.FEATHER_FAN_HUNT_START.get(),
                        SoundSource.PLAYERS, 0.95F, 1.0F + charge * 0.12F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.45F, 1.42F);
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8F, 1.15F + charge * 0.25F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.65F, 0.85F + charge * 0.2F);
            }
            return true;
        }
        return false;
    }

    private static List<LivingEntity> findHuntingTargets(Player player, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB searchArea = player.getBoundingBox().inflate(HUNT_LOCK_RANGE);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                target -> target.isAlive()
                        && !target.isSpectator()
                        && target != player
                        && player.canAttack(target)
        );
        candidates.removeIf(target -> {
            Vec3 offset = target.getBoundingBox().getCenter().subtract(eye);
            double distance = offset.length();
            if (distance < 1.0E-3D || distance > HUNT_LOCK_RANGE) {
                return true;
            }

            double dot = look.dot(offset.scale(1.0D / distance));
            return dot < HUNT_LOCK_MIN_DOT || !player.hasLineOfSight(target);
        });
        candidates.sort(Comparator.comparingDouble(target -> huntingTargetScore(player, target)));
        return candidates.size() <= limit ? candidates : List.copyOf(candidates.subList(0, limit));
    }

    private static double huntingTargetScore(Player player, LivingEntity target) {
        Vec3 offset = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        double distance = offset.length();
        double dot = distance < 1.0E-3D
                ? 1.0D
                : player.getLookAngle().normalize().dot(offset.scale(1.0D / distance));
        return (1.0D - dot) * 5.0D + distance / HUNT_LOCK_RANGE;
    }

    private static int getHuntingLockCapacity(int chargeTicks) {
        if (chargeTicks < HUNT_LOCK_DELAY_TICKS) {
            return 0;
        }
        return Mth.clamp(1 + (chargeTicks - HUNT_LOCK_DELAY_TICKS) / HUNT_LOCK_INTERVAL_TICKS,
                1, HUNT_MAX_LOCKED_TARGETS);
    }

    private static boolean isHuntingLockMilestone(int chargeTicks) {
        return chargeTicks >= HUNT_LOCK_DELAY_TICKS
                && (chargeTicks - HUNT_LOCK_DELAY_TICKS) % HUNT_LOCK_INTERVAL_TICKS == 0;
    }

    private void spawnHuntingLockEffect(ServerLevel level, LivingEntity target, int ticks, int index) {
        Vec3 center = target.getBoundingBox().getCenter().add(0.0D, target.getBbHeight() * 0.08D, 0.0D);
        double radius = target.getBbWidth() * 0.65D + 0.38D;
        double angle = ticks * 0.18D + index * 0.78D;
        level.sendParticles(GuaniaoParticleTypes.HUNTING_MARK.get(),
                center.x + Math.cos(angle) * radius * 0.12D,
                center.y,
                center.z + Math.sin(angle) * radius * 0.12D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.WAX_ON,
                center.x, center.y, center.z,
                2, radius * 0.45D, target.getBbHeight() * 0.22D, radius * 0.45D, 0.012D);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    private static float getCharge(int chargeTicks) {
        return Mth.clamp((chargeTicks - MIN_CHARGE_TICKS) / (float)(MAX_CHARGE_TICKS - MIN_CHARGE_TICKS), 0.0F, 1.0F);
    }

    private static float getDampedPoseProgress(float elapsedTicks) {
        float time = Mth.clamp(elapsedTicks / CHARGE_POSE_SETTLE_TICKS, 0.0F, 1.0F);
        if (time >= 1.0F) {
            return 1.0F;
        }
        float decay = (float)Math.exp(-6.0F * time);
        float phase = 5.3F * time;
        return 1.0F - decay * (Mth.cos(phase) + 1.13F * Mth.sin(phase));
    }

    private static float smootherStep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * value * (value * (value * 6.0F - 15.0F) + 10.0F);
    }
}
