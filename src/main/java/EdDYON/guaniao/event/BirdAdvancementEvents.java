package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.advancement.BirdAdvancements;
import EdDYON.guaniao.content.bird.crow.CrowEntity;
import EdDYON.guaniao.content.bird.longtailedtit.LongTailedTitEntity;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.List;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdAdvancementEvents {
    private static final double NEARBY_BIRD_RADIUS = 24.0D;
    private static final double TIT_LINE_RADIUS = 14.0D;
    private static final double GUIDE_OBSERVE_DISTANCE = 28.0D;

    private BirdAdvancementEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        int staggeredTick = player.tickCount + player.getId();
        if (staggeredTick % 40 == 0) {
            if (!BirdAdvancements.isDone(player, BirdAdvancements.BIRD_CONFERENCE) && hasFiveNearbyBirdSpecies(player)) {
                BirdAdvancements.grant(player, BirdAdvancements.BIRD_CONFERENCE);
            }
            if (!BirdAdvancements.isDone(player, BirdAdvancements.FLUFFY_LINE) && hasLongTailedTitLine(player)) {
                BirdAdvancements.grant(player, BirdAdvancements.FLUFFY_LINE);
            }
        }
        if (staggeredTick % 10 == 0
                && !BirdAdvancements.isDone(player, BirdAdvancements.HIGH_EYE)
                && isHoldingBirdGuide(player)) {
            CrowEntity crow = findLookedAtCrow(player);
            if (crow != null && isHighPerchedCrow(crow)) {
                BirdAdvancements.grant(player, BirdAdvancements.HIGH_EYE);
            }
        }
    }

    private static boolean hasFiveNearbyBirdSpecies(ServerPlayer player) {
        EnumSet<BirdSpecies> speciesSet = EnumSet.noneOf(BirdSpecies.class);
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(NEARBY_BIRD_RADIUS), Entity::isAlive)) {
            BirdSpecies species = BirdSpecies.from(entity);
            if (species != null) {
                speciesSet.add(species);
                if (speciesSet.size() >= 5) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasLongTailedTitLine(ServerPlayer player) {
        List<LongTailedTitEntity> tits = player.level().getEntitiesOfClass(
                LongTailedTitEntity.class,
                player.getBoundingBox().inflate(TIT_LINE_RADIUS, 6.0D, TIT_LINE_RADIUS),
                tit -> tit.isAlive() && tit.onGround() && !tit.isBirdFlightActive() && tit.getDeltaMovement().horizontalDistanceSqr() < 0.012D);
        if (tits.size() < 4) {
            return false;
        }
        for (int i = 0; i < tits.size(); i++) {
            Vec3 anchor = tits.get(i).position();
            for (int j = i + 1; j < tits.size(); j++) {
                Vec3 delta = tits.get(j).position().subtract(anchor);
                Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
                double lengthSqr = horizontal.lengthSqr();
                if (lengthSqr < 0.16D || lengthSqr > 9.0D) {
                    continue;
                }
                Vec3 direction = horizontal.normalize();
                int count = 0;
                for (LongTailedTitEntity tit : tits) {
                    Vec3 offset = tit.position().subtract(anchor);
                    if (Math.abs(offset.y) > 0.85D) {
                        continue;
                    }
                    double projection = offset.x * direction.x + offset.z * direction.z;
                    double horizontalSqr = offset.x * offset.x + offset.z * offset.z;
                    double perpendicularSqr = Math.max(0.0D, horizontalSqr - projection * projection);
                    if (Math.abs(projection) <= 3.0D && perpendicularSqr <= 0.20D) {
                        count++;
                        if (count >= 4) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isHoldingBirdGuide(ServerPlayer player) {
        return isBirdGuide(player.getMainHandItem()) || isBirdGuide(player.getOffhandItem());
    }

    private static boolean isBirdGuide(ItemStack stack) {
        return !stack.isEmpty() && stack.is(GuaniaoItems.BIRD_GUIDE.get());
    }

    private static CrowEntity findLookedAtCrow(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        CrowEntity best = null;
        double bestProjection = GUIDE_OBSERVE_DISTANCE + 1.0D;
        for (CrowEntity crow : player.level().getEntitiesOfClass(
                CrowEntity.class,
                player.getBoundingBox().inflate(GUIDE_OBSERVE_DISTANCE),
                crow -> crow.isAlive() && player.hasLineOfSight(crow))) {
            Vec3 toCrow = crow.getBoundingBox().getCenter().subtract(eye);
            double projection = toCrow.dot(look);
            if (projection <= 0.0D || projection > GUIDE_OBSERVE_DISTANCE || projection >= bestProjection) {
                continue;
            }
            Vec3 closestPoint = eye.add(look.scale(projection));
            if (crow.getBoundingBox().inflate(0.65D).distanceToSqr(closestPoint) <= 0.45D) {
                best = crow;
                bestProjection = projection;
            }
        }
        return best;
    }

    private static boolean isHighPerchedCrow(CrowEntity crow) {
        if (!crow.onGround() || crow.isBirdFlightActive()) {
            return false;
        }
        ServerLevel level = (ServerLevel) crow.level();
        BlockPos pos = crow.blockPosition();
        BlockState below = level.getBlockState(pos.below());
        if (below.is(BlockTags.LEAVES)
                || below.is(BlockTags.FENCES)
                || below.is(BlockTags.WALLS)
                || below.is(BlockTags.LOGS)
                || below.is(BlockTags.PLANKS)
                || below.is(BlockTags.SLABS)
                || below.is(BlockTags.STAIRS)) {
            return true;
        }
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return pos.getY() >= groundY + 2;
    }
}
