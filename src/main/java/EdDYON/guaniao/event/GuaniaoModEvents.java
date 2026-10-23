package EdDYON.guaniao.event;

import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.columbid.PigeonEntity;
import EdDYON.guaniao.content.bird.columbid.SpottedDoveEntity;
import EdDYON.guaniao.content.bird.crow.CrowEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.seagull.SeagullEntity;
import EdDYON.guaniao.content.bird.kiwi.KiwiEntity;
import EdDYON.guaniao.content.bird.myna.MynaEntity;
import EdDYON.guaniao.content.bird.woodcock.WoodcockEntity;
import EdDYON.guaniao.content.bird.kestrel.KestrelEntity;
import EdDYON.guaniao.content.bird.cassowary.CassowaryEntity;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.content.bird.longtailedtit.LongTailedTitEntity;
import EdDYON.guaniao.content.bird.cockatiel.CockatielEntity;
import EdDYON.guaniao.content.bird.macaw.MacawEntity;
import EdDYON.guaniao.content.bird.umbrellacockatoo.UmbrellaCockatooEntity;
import EdDYON.guaniao.content.bird.cockatoo.CockatooEntity;
import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
import EdDYON.guaniao.content.bird.kingfisher.KingfisherEntity;
import EdDYON.guaniao.content.dropping.BirdDroppingUtil;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="guaniao")
public final class GuaniaoModEvents {
    private GuaniaoModEvents() {
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(GuaniaoEntityTypes.NIGHT_HERON.get(), NightHeronEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.SPARROW.get(), SparrowEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.LONG_TAILED_TIT.get(), SparrowEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.COCKATIEL.get(), CockatielEntity.createCockatielAttributes().build());
        event.put(GuaniaoEntityTypes.MACAW.get(), MacawEntity.createMacawAttributes().build());
        event.put(GuaniaoEntityTypes.BUDGERIGAR.get(), BudgerigarEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.SPOTTED_DOVE.get(), SpottedDoveEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.PIGEON.get(), PigeonEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.CROW.get(), CrowEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.SEAGULL.get(), SeagullEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.KIWI.get(), KiwiEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.MYNA.get(), MynaEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.WOODCOCK.get(), WoodcockEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.KESTREL.get(), KestrelEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.CASSOWARY.get(), CassowaryEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.UMBRELLA_COCKATOO.get(), UmbrellaCockatooEntity.createUmbrellaCockatooAttributes().build());
        event.put(GuaniaoEntityTypes.HUMMINGBIRD.get(), HummingbirdEntity.createAttributes().build());
        event.put(GuaniaoEntityTypes.COCKATOO.get(), CockatooEntity.createCockatooAttributes().build());
        event.put(GuaniaoEntityTypes.KINGFISHER.get(), KingfisherEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(GuaniaoEntityTypes.HUMMINGBIRD.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, withoutChunkGeneration(HummingbirdEntity::canSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.NIGHT_HERON.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, withoutChunkGeneration(NightHeronEntity::canSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.SPARROW.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, withoutChunkGeneration(SparrowEntity::canSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.LONG_TAILED_TIT.get(), BirdSpawnPlacements.ON_GROUND_OR_LEAVES, Heightmap.Types.MOTION_BLOCKING, withoutChunkGeneration(LongTailedTitEntity::canLongTailedTitSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.COCKATIEL.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, withoutChunkGeneration(CockatielEntity::canCockatielSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.MACAW.get(), BirdSpawnPlacements.ON_GROUND_OR_LEAVES, Heightmap.Types.MOTION_BLOCKING, withoutChunkGeneration(MacawEntity::canMacawSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.BUDGERIGAR.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, withoutChunkGeneration(BudgerigarEntity::canSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.SPOTTED_DOVE.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, withoutChunkGeneration(SpottedDoveEntity::canSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.PIGEON.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, withoutChunkGeneration(PigeonEntity::canSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.CROW.get(), BirdSpawnPlacements.ON_GROUND_OR_LEAVES, Heightmap.Types.MOTION_BLOCKING, withoutChunkGeneration(CrowEntity::canSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.SEAGULL.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, withoutChunkGeneration(SeagullEntity::canSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.KIWI.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, withoutChunkGeneration(KiwiEntity::canSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.MYNA.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, withoutChunkGeneration(MynaEntity::canMynaSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.WOODCOCK.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, withoutChunkGeneration(WoodcockEntity::canWoodcockSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.KESTREL.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, withoutChunkGeneration(KestrelEntity::canSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.CASSOWARY.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, withoutChunkGeneration(CassowaryEntity::canSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.UMBRELLA_COCKATOO.get(), BirdSpawnPlacements.ON_GROUND_OR_LEAVES, Heightmap.Types.MOTION_BLOCKING, withoutChunkGeneration(UmbrellaCockatooEntity::canUmbrellaCockatooSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.COCKATOO.get(), BirdSpawnPlacements.ON_GROUND_OR_LEAVES, Heightmap.Types.MOTION_BLOCKING, withoutChunkGeneration(CockatooEntity::canCockatooSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.KINGFISHER.get(), BirdSpawnPlacements.ON_GROUND_OR_LEAVES, Heightmap.Types.MOTION_BLOCKING, withoutChunkGeneration(KingfisherEntity::canSpawn), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends Entity> SpawnPlacements.SpawnPredicate<T> withoutChunkGeneration(
            SpawnPlacements.SpawnPredicate<T> predicate) {
        // The predicate runs before SpawnPlacementCheck, so reject worldgen before reading its region.
        return (type, level, spawnType, pos, random) -> spawnType != MobSpawnType.CHUNK_GENERATION
                && predicate.test(type, level, spawnType, pos, random);
    }

    @SubscribeEvent
    public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept((ItemLike)GuaniaoItems.EARTHWORM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept((ItemLike)GuaniaoItems.NIGHT_HERON_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.SPARROW_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.LONG_TAILED_TIT_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.COCKATIEL_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.MACAW_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.BUDGERIGAR_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.SPOTTED_DOVE_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.PIGEON_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.CROW_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.SEAGULL_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.KIWI_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.MYNA_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.WOODCOCK_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.KESTREL_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.CASSOWARY_SPAWN_EGG.get());
            event.accept((ItemLike)GuaniaoItems.UMBRELLA_COCKATOO_SPAWN_EGG.get());
            event.accept(GuaniaoItems.HUMMINGBIRD_SPAWN_EGG.get());
            event.accept(GuaniaoItems.COCKATOO_SPAWN_EGG.get());
            event.accept(GuaniaoItems.KINGFISHER_SPAWN_EGG.get());
        }
    }
}
