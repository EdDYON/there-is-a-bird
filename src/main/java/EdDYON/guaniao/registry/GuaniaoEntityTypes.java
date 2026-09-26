package EdDYON.guaniao.registry;

import EdDYON.guaniao.content.earthworm.EarthwormEntity;

import EdDYON.guaniao.content.bird.budgerigar.BudgerigarDefinition;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.columbid.PigeonDefinition;
import EdDYON.guaniao.content.bird.columbid.PigeonEntity;
import EdDYON.guaniao.content.bird.columbid.SpottedDoveDefinition;
import EdDYON.guaniao.content.bird.columbid.SpottedDoveEntity;
import EdDYON.guaniao.content.bird.crow.CrowDefinition;
import EdDYON.guaniao.content.bird.crow.CrowEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronDefinition;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.seagull.SeagullDefinition;
import EdDYON.guaniao.content.bird.seagull.SeagullEntity;
import EdDYON.guaniao.content.bird.kiwi.KiwiDefinition;
import EdDYON.guaniao.content.bird.kiwi.KiwiEntity;
import EdDYON.guaniao.content.bird.myna.MynaDefinition;
import EdDYON.guaniao.content.bird.myna.MynaEntity;
import EdDYON.guaniao.content.bird.woodcock.WoodcockDefinition;
import EdDYON.guaniao.content.bird.woodcock.WoodcockEntity;
import EdDYON.guaniao.content.bird.kestrel.KestrelDefinition;
import EdDYON.guaniao.content.bird.kestrel.KestrelEntity;
import EdDYON.guaniao.content.bird.cassowary.CassowaryDefinition;
import EdDYON.guaniao.content.bird.cassowary.CassowaryEntity;
import EdDYON.guaniao.content.bird.sparrow.SparrowDefinition;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.content.bird.longtailedtit.LongTailedTitDefinition;
import EdDYON.guaniao.content.bird.longtailedtit.LongTailedTitEntity;
import EdDYON.guaniao.content.bird.cockatiel.CockatielDefinition;
import EdDYON.guaniao.content.bird.cockatiel.CockatielEntity;
import EdDYON.guaniao.content.bird.macaw.MacawDefinition;
import EdDYON.guaniao.content.bird.macaw.MacawEntity;
import EdDYON.guaniao.content.bird.umbrellacockatoo.UmbrellaCockatooDefinition;
import EdDYON.guaniao.content.bird.umbrellacockatoo.UmbrellaCockatooEntity;
import EdDYON.guaniao.content.camera.PhotographEntity;
import EdDYON.guaniao.content.dropping.BirdDroppingProjectileEntity;
import EdDYON.guaniao.content.dropping.BirdDroppingSplatEntity;
import EdDYON.guaniao.content.fan.FeatherFanProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Supplier;

public final class GuaniaoEntityTypes {
    public static final MobCategory BIRD = MobCategory.CREATURE;
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, (String)"guaniao");
    public static final Supplier<EntityType<NightHeronEntity>> NIGHT_HERON = GuaniaoEntityTypes.registerCreature(NightHeronDefinition.ENTITY_ID, NightHeronEntity::new, NightHeronDefinition.WIDTH, NightHeronDefinition.HEIGHT);
    public static final Supplier<EntityType<SparrowEntity>> SPARROW = GuaniaoEntityTypes.registerCreature(SparrowDefinition.ENTITY_ID, SparrowEntity::new, SparrowDefinition.WIDTH, SparrowDefinition.HEIGHT);
    public static final Supplier<EntityType<LongTailedTitEntity>> LONG_TAILED_TIT = GuaniaoEntityTypes.registerCreature(LongTailedTitDefinition.ENTITY_ID, LongTailedTitEntity::new, LongTailedTitDefinition.WIDTH, LongTailedTitDefinition.HEIGHT);
    public static final Supplier<EntityType<CockatielEntity>> COCKATIEL = GuaniaoEntityTypes.registerCreature(CockatielDefinition.ENTITY_ID, CockatielEntity::new, CockatielDefinition.WIDTH, CockatielDefinition.HEIGHT);
    public static final Supplier<EntityType<MacawEntity>> MACAW = GuaniaoEntityTypes.registerCreature(MacawDefinition.ENTITY_ID, MacawEntity::new, MacawDefinition.WIDTH, MacawDefinition.HEIGHT);
    public static final Supplier<EntityType<BudgerigarEntity>> BUDGERIGAR = GuaniaoEntityTypes.registerCreature(BudgerigarDefinition.ENTITY_ID, BudgerigarEntity::new, BudgerigarDefinition.WIDTH, BudgerigarDefinition.HEIGHT);
    public static final Supplier<EntityType<SpottedDoveEntity>> SPOTTED_DOVE = GuaniaoEntityTypes.registerCreature(SpottedDoveDefinition.ENTITY_ID, SpottedDoveEntity::new, SpottedDoveDefinition.WIDTH, SpottedDoveDefinition.HEIGHT);
    public static final Supplier<EntityType<PigeonEntity>> PIGEON = GuaniaoEntityTypes.registerCreature(PigeonDefinition.ENTITY_ID, PigeonEntity::new, PigeonDefinition.WIDTH, PigeonDefinition.HEIGHT);
    public static final Supplier<EntityType<CrowEntity>> CROW = GuaniaoEntityTypes.registerCreature(CrowDefinition.ENTITY_ID, CrowEntity::new, CrowDefinition.WIDTH, CrowDefinition.HEIGHT);
    public static final Supplier<EntityType<SeagullEntity>> SEAGULL = GuaniaoEntityTypes.registerCreature(SeagullDefinition.ENTITY_ID, SeagullEntity::new, SeagullDefinition.WIDTH, SeagullDefinition.HEIGHT);
    public static final Supplier<EntityType<KiwiEntity>> KIWI = GuaniaoEntityTypes.registerCreature(KiwiDefinition.ENTITY_ID, KiwiEntity::new, KiwiDefinition.WIDTH, KiwiDefinition.HEIGHT);
    public static final Supplier<EntityType<MynaEntity>> MYNA = GuaniaoEntityTypes.registerCreature(MynaDefinition.ENTITY_ID, MynaEntity::new, MynaDefinition.WIDTH, MynaDefinition.HEIGHT);
    public static final Supplier<EntityType<WoodcockEntity>> WOODCOCK = GuaniaoEntityTypes.registerCreature(WoodcockDefinition.ENTITY_ID, WoodcockEntity::new, WoodcockDefinition.WIDTH, WoodcockDefinition.HEIGHT);
    public static final Supplier<EntityType<KestrelEntity>> KESTREL = GuaniaoEntityTypes.registerCreature(KestrelDefinition.ENTITY_ID, KestrelEntity::new, KestrelDefinition.WIDTH, KestrelDefinition.HEIGHT);
    public static final Supplier<EntityType<CassowaryEntity>> CASSOWARY = GuaniaoEntityTypes.registerCreature(CassowaryDefinition.ENTITY_ID, CassowaryEntity::new, CassowaryDefinition.WIDTH, CassowaryDefinition.HEIGHT);
    public static final Supplier<EntityType<UmbrellaCockatooEntity>> UMBRELLA_COCKATOO = GuaniaoEntityTypes.registerCreature(UmbrellaCockatooDefinition.ENTITY_ID, UmbrellaCockatooEntity::new, UmbrellaCockatooDefinition.WIDTH, UmbrellaCockatooDefinition.HEIGHT);
    public static final Supplier<EntityType<PhotographEntity>> PHOTOGRAPH = ENTITY_TYPES.register("photograph", () ->
            EntityType.Builder.<PhotographEntity>of(PhotographEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(Integer.MAX_VALUE)
                    .build(ResourceLocation.fromNamespaceAndPath("guaniao", "photograph").toString()));
    public static final Supplier<EntityType<BirdDroppingProjectileEntity>> BIRD_DROPPING_PROJECTILE = ENTITY_TYPES.register("bird_dropping_projectile", () ->
            EntityType.Builder.<BirdDroppingProjectileEntity>of(BirdDroppingProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath("guaniao", "bird_dropping_projectile").toString()));
    public static final Supplier<EntityType<BirdDroppingSplatEntity>> BIRD_DROPPING_SPLAT = ENTITY_TYPES.register("bird_dropping_splat", () ->
            EntityType.Builder.<BirdDroppingSplatEntity>of(BirdDroppingSplatEntity::new, MobCategory.MISC)
                    .sized(0.45F, 0.12F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build(ResourceLocation.fromNamespaceAndPath("guaniao", "bird_dropping_splat").toString()));
    public static final Supplier<EntityType<FeatherFanProjectileEntity>> FEATHER_FAN_PROJECTILE = ENTITY_TYPES.register("feather_fan_projectile", () ->
            EntityType.Builder.<FeatherFanProjectileEntity>of(FeatherFanProjectileEntity::new, MobCategory.MISC)
                    .sized(0.65F, 0.15F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath("guaniao", "feather_fan_projectile").toString()));

    public static final Supplier<EntityType<EarthwormEntity>> EARTHWORM = ENTITY_TYPES.register("earthworm", () ->
            EntityType.Builder.<EarthwormEntity>of(EarthwormEntity::new, MobCategory.MISC)
                    .sized(0.32F, 0.05F).clientTrackingRange(6).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath("guaniao", "earthworm").toString()));

    private GuaniaoEntityTypes() {
    }

    private static <T extends Mob> Supplier<EntityType<T>> registerCreature(String id, EntityType.EntityFactory<T> factory, float width, float height) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.of((EntityType.EntityFactory)factory, BIRD).sized(width, height).clientTrackingRange(8).build(ResourceLocation.fromNamespaceAndPath("guaniao", id).toString()));
    }
}
