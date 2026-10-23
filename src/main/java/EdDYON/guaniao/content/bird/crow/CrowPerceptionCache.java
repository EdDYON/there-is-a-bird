package EdDYON.guaniao.content.bird.crow;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.nest.CrowNestTreasure;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/** One bounded perception snapshot shared by the crow's food, treasure and player goals. */
final class CrowPerceptionCache {
    private long nextRefreshTick;
    private List<Integer> itemIds = List.of();
    private List<Integer> playerIds = List.of();

    ItemEntity nearestFood(CrowEntity crow) {
        this.ensureFresh(crow);
        return this.nearestItem(crow, 12.0D, true);
    }

    ItemEntity nearestShiny(CrowEntity crow) {
        this.ensureFresh(crow);
        return this.nearestItem(crow, 14.0D, false);
    }

    Player nearestPlayer(CrowEntity crow, double range) {
        Player best = null;
        double bestDistance = range * range;
        for (Player player : this.players(crow)) {
            double distance = crow.distanceToSqr(player);
            if (distance < bestDistance) {
                best = player;
                bestDistance = distance;
            }
        }
        return best;
    }

    List<Player> players(CrowEntity crow) {
        this.ensureFresh(crow);
        if (!(crow.level() instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        List<Player> players = new ArrayList<>(this.playerIds.size());
        for (int id : this.playerIds) {
            Entity entity = serverLevel.getEntity(id);
            if (entity instanceof Player player && player.isAlive() && !player.isSpectator()) {
                players.add(player);
            }
        }
        return players;
    }

    private ItemEntity nearestItem(CrowEntity crow, double range, boolean food) {
        if (!(crow.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ItemEntity best = null;
        double bestDistance = range * range;
        for (int id : this.itemIds) {
            Entity entity = serverLevel.getEntity(id);
            if (!(entity instanceof ItemEntity item) || !item.isAlive()) {
                continue;
            }
            boolean matches = food
                    ? CrowEntity.isCrowDroppedFoodCandidate(item.getItem())
                    : CrowNestTreasure.isAccepted(item.getItem());
            double distance = crow.distanceToSqr(item);
            if (matches && distance < bestDistance) {
                best = item;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void ensureFresh(CrowEntity crow) {
        if (!(crow.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        long now = serverLevel.getGameTime();
        if (now < this.nextRefreshTick) {
            return;
        }
        if (!BirdScanBudget.tryAcquire(serverLevel, crow, 2)) {
            return;
        }
        int interval = Math.max(20, Math.min(40, Math.min(
                BirdConfigManager.foodScanInterval(BirdSpecies.CROW),
                BirdConfigManager.threatScanInterval(BirdSpecies.CROW))));
        this.nextRefreshTick = now + interval;
        this.itemIds = serverLevel.getEntitiesOfClass(ItemEntity.class, crow.getBoundingBox().inflate(14.0D, 5.0D, 14.0D), ItemEntity::isAlive)
                .stream().map(Entity::getId).toList();
        this.playerIds = serverLevel.getEntitiesOfClass(Player.class, crow.getBoundingBox().inflate(14.0D),
                        player -> player.isAlive() && !player.isSpectator())
                .stream().map(Entity::getId).toList();
    }
}
