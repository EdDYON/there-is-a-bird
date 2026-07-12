package EdDYON.guaniao.event;

import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.columbid.PigeonEntity;
import EdDYON.guaniao.content.bird.columbid.SpottedDoveEntity;
import EdDYON.guaniao.content.bird.crow.CrowEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.seagull.SeagullEntity;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.content.bird.longtailedtit.LongTailedTitEntity;
import EdDYON.guaniao.content.bird.cockatiel.CockatielEntity;
import EdDYON.guaniao.content.bird.macaw.MacawEntity;
import EdDYON.guaniao.content.dropping.BirdDroppingUtil;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid="guaniao", bus=Mod.EventBusSubscriber.Bus.MOD)
public final class GuaniaoModEvents {
    private GuaniaoModEvents() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(BirdDroppingUtil::registerCompostables);
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put((EntityType)GuaniaoEntityTypes.NIGHT_HERON.get(), NightHeronEntity.createAttributes().build());
        event.put((EntityType)GuaniaoEntityTypes.SPARROW.get(), SparrowEntity.createAttributes().build());
        event.put((EntityType)GuaniaoEntityTypes.LONG_TAILED_TIT.get(), SparrowEntity.createAttributes().build());
        event.put((EntityType)GuaniaoEntityTypes.COCKATIEL.get(), CockatielEntity.createCockatielAttributes().build());
        event.put((EntityType)GuaniaoEntityTypes.MACAW.get(), MacawEntity.createMacawAttributes().build());
        event.put((EntityType)GuaniaoEntityTypes.BUDGERIGAR.get(), BudgerigarEntity.createAttributes().build());
        event.put((EntityType)GuaniaoEntityTypes.SPOTTED_DOVE.get(), SpottedDoveEntity.createAttributes().build());
        event.put((EntityType)GuaniaoEntityTypes.PIGEON.get(), PigeonEntity.createAttributes().build());
        event.put((EntityType)GuaniaoEntityTypes.CROW.get(), CrowEntity.createAttributes().build());
        event.put((EntityType)GuaniaoEntityTypes.SEAGULL.get(), SeagullEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register((EntityType)GuaniaoEntityTypes.NIGHT_HERON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NightHeronEntity::canSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register((EntityType)GuaniaoEntityTypes.SPARROW.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SparrowEntity::canSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.LONG_TAILED_TIT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LongTailedTitEntity::canLongTailedTitSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.COCKATIEL.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CockatielEntity::canCockatielSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(GuaniaoEntityTypes.MACAW.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, MacawEntity::canMacawSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register((EntityType)GuaniaoEntityTypes.BUDGERIGAR.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BudgerigarEntity::canSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register((EntityType)GuaniaoEntityTypes.SPOTTED_DOVE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SpottedDoveEntity::canSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register((EntityType)GuaniaoEntityTypes.PIGEON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, PigeonEntity::canSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register((EntityType)GuaniaoEntityTypes.CROW.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CrowEntity::canSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register((EntityType)GuaniaoEntityTypes.SEAGULL.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SeagullEntity::canSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.SPAWN_EGGS.equals((Object)event.getTabKey())) {
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
        }
    }
}
