package EdDYON.guaniao.content.dropping;

import EdDYON.guaniao.content.bird.seagull.SeagullEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public final class BirdDroppingInteractionHandler {
    private static final double GROUND_FOOD_SEARCH_RADIUS = 0.62D;
    private static final double SEAGULL_DOG_BARK_RADIUS = 10.0D;
    private static final float FOX_DROP_HELD_ITEM_CHANCE = 0.15F;
    private static final DustParticleOptions DIRTY_BROWN_DUST = new DustParticleOptions(new Vector3f(0.25F, 0.16F, 0.06F), 0.92F);
    private static final DustParticleOptions SICK_GREEN_DUST = new DustParticleOptions(new Vector3f(0.18F, 0.42F, 0.08F), 0.78F);

    private BirdDroppingInteractionHandler() {
    }

    public static boolean handleEntityHit(BirdDroppingProjectileEntity projectile, Entity hit) {
        if (!(projectile.level() instanceof ServerLevel level)) {
            return false;
        }

        if (hit instanceof ItemEntity itemEntity) {
            return prankFood(level, projectile, itemEntity);
        }
        if (hit instanceof Wolf wolf) {
            reactWolf(level, projectile, wolf);
            return true;
        }
        if (hit instanceof Cat cat) {
            reactCat(level, projectile, cat);
            return true;
        }
        if (hit instanceof Fox fox) {
            reactFox(level, projectile, fox);
            return true;
        }
        if (hit instanceof EnderMan enderMan) {
            reactEnderman(level, projectile, enderMan);
            return true;
        }
        if (hit instanceof AbstractSkeleton skeleton) {
            reactSkeleton(level, skeleton);
            return true;
        }
        if (hit instanceof Spider spider) {
            reactSpider(level, spider);
            return true;
        }
        if (hit instanceof Zombie zombie) {
            reactZombie(level, zombie);
            return true;
        }

        return false;
    }

    public static boolean handleNearbyFoodHit(BirdDroppingProjectileEntity projectile, Vec3 impactPosition) {
        if (!(projectile.level() instanceof ServerLevel level)) {
            return false;
        }

        AABB searchBox = new AABB(impactPosition, impactPosition).inflate(GROUND_FOOD_SEARCH_RADIUS);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchBox, item -> item.isAlive() && PrankFoodUtil.isEligibleFood(item.getItem()));
        if (items.isEmpty()) {
            return false;
        }

        ItemEntity nearest = items.get(0);
        double nearestDistance = nearest.distanceToSqr(impactPosition);
        for (int i = 1; i < items.size(); i++) {
            ItemEntity item = items.get(i);
            double distance = item.distanceToSqr(impactPosition);
            if (distance < nearestDistance) {
                nearest = item;
                nearestDistance = distance;
            }
        }
        return prankFood(level, projectile, nearest);
    }

    private static boolean prankFood(ServerLevel level, BirdDroppingProjectileEntity projectile, ItemEntity itemEntity) {
        ItemStack original = itemEntity.getItem();
        if (!PrankFoodUtil.isEligibleFood(original)) {
            return false;
        }

        ItemStack prankFood = PrankFoodUtil.makePrankFood(original, droppingStack(projectile));
        if (prankFood.isEmpty()) {
            return false;
        }

        if (original.getCount() <= 1) {
            itemEntity.setItem(prankFood);
            itemEntity.setPickUpDelay(10);
        } else {
            ItemStack remaining = original.copy();
            remaining.shrink(1);
            itemEntity.setItem(remaining);

            ItemEntity prankEntity = new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), prankFood);
            prankEntity.setDeltaMovement(itemEntity.getDeltaMovement().scale(0.35D).add(0.0D, 0.06D, 0.0D));
            prankEntity.setPickUpDelay(10);
            level.addFreshEntity(prankEntity);
        }

        spawnPrankFoodFilth(level, itemEntity.position().add(0.0D, 0.12D, 0.0D));
        return true;
    }

    private static void reactWolf(ServerLevel level, BirdDroppingProjectileEntity projectile, Wolf wolf) {
        wetSplat(level, wolf.position().add(0.0D, wolf.getBbHeight() * 0.72D, 0.0D), 7);
        wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
        wolf.playSound(SoundEvents.WOLF_WHINE, 0.8F, 0.85F + level.random.nextFloat() * 0.25F);

        LivingEntity owner = wolf.getOwner();
        if (wolf.isTame() && owner != null) {
            wolf.getLookControl().setLookAt(owner, 30.0F, 30.0F);
        } else if (projectile.getOwner() instanceof LivingEntity livingOwner) {
            wolf.getLookControl().setLookAt(livingOwner, 30.0F, 30.0F);
        }

        triggerNearbySeagullDogBark(level, wolf.position());
    }

    private static void reactCat(ServerLevel level, BirdDroppingProjectileEntity projectile, Cat cat) {
        wetSplat(level, cat.position().add(0.0D, cat.getBbHeight() * 0.72D, 0.0D), 7);
        cat.playSound(SoundEvents.CAT_HISS, 0.9F, 0.9F + level.random.nextFloat() * 0.25F);
        cat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 55, 0));
        shoveAway(cat, projectile.position(), 0.42D, 0.18D);
    }

    private static void reactFox(ServerLevel level, BirdDroppingProjectileEntity projectile, Fox fox) {
        wetSplat(level, fox.position().add(0.0D, fox.getBbHeight() * 0.66D, 0.0D), 7);
        fox.playSound(SoundEvents.FOX_HURT, 0.8F, 1.05F + level.random.nextFloat() * 0.25F);
        shoveAway(fox, projectile.position(), 0.46D, 0.22D);

        ItemStack held = fox.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!held.isEmpty() && level.random.nextFloat() < FOX_DROP_HELD_ITEM_CHANCE) {
            ItemStack dropped = held.split(1);
            fox.setItemSlot(EquipmentSlot.MAINHAND, held);

            ItemEntity itemEntity = new ItemEntity(level, fox.getX(), fox.getY(0.45D), fox.getZ(), dropped);
            itemEntity.setDeltaMovement(fox.getDeltaMovement().add((level.random.nextDouble() - 0.5D) * 0.18D, 0.12D, (level.random.nextDouble() - 0.5D) * 0.18D));
            itemEntity.setPickUpDelay(20);
            level.addFreshEntity(itemEntity);
        }
    }

    private static void reactZombie(ServerLevel level, Zombie zombie) {
        wetSplat(level, zombie.position().add(0.0D, zombie.getBbHeight() * 0.72D, 0.0D), 5);
        zombie.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 0));
        zombie.playSound(SoundEvents.ZOMBIE_HURT, 0.55F, 0.8F + level.random.nextFloat() * 0.25F);
    }

    private static void reactSkeleton(ServerLevel level, AbstractSkeleton skeleton) {
        wetSplat(level, skeleton.position().add(0.0D, skeleton.getBbHeight() * 0.72D, 0.0D), 5);
        skeleton.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
        skeleton.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0));
        skeleton.playSound(SoundEvents.SKELETON_HURT, 0.55F, 0.75F + level.random.nextFloat() * 0.25F);
    }

    private static void reactSpider(ServerLevel level, Spider spider) {
        wetSplat(level, spider.position().add(0.0D, spider.getBbHeight() * 0.45D, 0.0D), 6);
        spider.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
        spider.playSound(SoundEvents.SPIDER_HURT, 0.55F, 0.85F + level.random.nextFloat() * 0.25F);
    }

    private static void reactEnderman(ServerLevel level, BirdDroppingProjectileEntity projectile, EnderMan enderMan) {
        wetSplat(level, enderMan.position().add(0.0D, enderMan.getBbHeight() * 0.72D, 0.0D), 9);
        LivingEntity target = projectile.getOwner() instanceof LivingEntity livingOwner
                ? livingOwner
                : level.getNearestPlayer(enderMan, 16.0D);
        if (target != null) {
            enderMan.setTarget(target);
            enderMan.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        teleportEndermanAway(level, enderMan);
    }

    private static void teleportEndermanAway(ServerLevel level, EnderMan enderMan) {
        Vec3 origin = enderMan.position();
        for (int i = 0; i < 16; i++) {
            double x = origin.x + (level.random.nextDouble() - 0.5D) * 18.0D;
            double y = Mth.clamp(origin.y + level.random.nextInt(9) - 4, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 1);
            double z = origin.z + (level.random.nextDouble() - 0.5D) * 18.0D;
            if (endermanRandomTeleport(enderMan, x, y, z)) {
                return;
            }
        }
        enderMan.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
    }

    private static boolean endermanRandomTeleport(EnderMan enderMan, double x, double y, double z) {
        boolean teleported = enderMan.randomTeleport(x, y, z, true);
        if (teleported) {
            enderMan.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }
        return teleported;
    }

    private static void triggerNearbySeagullDogBark(ServerLevel level, Vec3 position) {
        AABB searchBox = new AABB(position, position).inflate(SEAGULL_DOG_BARK_RADIUS);
        List<SeagullEntity> seagulls = level.getEntitiesOfClass(SeagullEntity.class, searchBox, SeagullEntity::isAlive);
        if (seagulls.isEmpty() || level.random.nextFloat() > 0.45F) {
            return;
        }

        SeagullEntity seagull = seagulls.get(level.random.nextInt(seagulls.size()));
        level.playSound(null, seagull.getX(), seagull.getY(), seagull.getZ(), SoundEvents.WOLF_AMBIENT, SoundSource.AMBIENT, 0.75F, 1.25F + level.random.nextFloat() * 0.3F);
        level.sendParticles(ParticleTypes.NOTE, seagull.getX(), seagull.getY(0.85D), seagull.getZ(), 1, 0.08D, 0.08D, 0.08D, 0.0D);
    }

    private static void shoveAway(LivingEntity entity, Vec3 sourcePosition, double horizontalStrength, double upwardStrength) {
        Vec3 away = entity.position().subtract(sourcePosition).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(entity.getRandom().nextDouble() - 0.5D, 0.0D, entity.getRandom().nextDouble() - 0.5D);
        }
        away = away.normalize();
        entity.setDeltaMovement(entity.getDeltaMovement().add(away.scale(horizontalStrength)).add(0.0D, upwardStrength, 0.0D));
        entity.hasImpulse = true;
    }

    private static ItemStack droppingStack(BirdDroppingProjectileEntity projectile) {
        return new ItemStack(projectile.getVariant().item());
    }

    private static void wetSplat(ServerLevel level, Vec3 position, int particleCount) {
        level.playSound(null, position.x, position.y, position.z, SoundEvents.SLIME_BLOCK_HIT, SoundSource.NEUTRAL, 0.62F, 0.82F + level.random.nextFloat() * 0.28F);
        level.sendParticles(ParticleTypes.POOF, position.x, position.y, position.z, particleCount, 0.16D, 0.12D, 0.16D, 0.01D);
    }

    private static void spawnPrankFoodFilth(ServerLevel level, Vec3 position) {
        level.playSound(null, position.x, position.y, position.z, SoundEvents.SLIME_BLOCK_HIT, SoundSource.NEUTRAL, 0.78F, 0.48F + level.random.nextFloat() * 0.18F);
        level.sendParticles(ParticleTypes.ITEM_SLIME, position.x, position.y, position.z, 7, 0.20D, 0.08D, 0.20D, 0.05D);
        level.sendParticles(ParticleTypes.COMPOSTER, position.x, position.y + 0.04D, position.z, 8, 0.18D, 0.10D, 0.18D, 0.04D);
        level.sendParticles(DIRTY_BROWN_DUST, position.x, position.y + 0.06D, position.z, 9, 0.20D, 0.10D, 0.20D, 0.02D);
        level.sendParticles(SICK_GREEN_DUST, position.x, position.y + 0.10D, position.z, 5, 0.16D, 0.08D, 0.16D, 0.01D);
        level.sendParticles(ParticleTypes.POOF, position.x, position.y + 0.12D, position.z, 3, 0.12D, 0.08D, 0.12D, 0.004D);
    }
}
