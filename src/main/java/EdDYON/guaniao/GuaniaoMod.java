package EdDYON.guaniao;

import com.mojang.logging.LogUtils;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.dropping.BirdDroppingDispenserBehavior;
import EdDYON.guaniao.registry.GuaniaoBlockEntityTypes;
import EdDYON.guaniao.registry.GuaniaoBiomeModifierSerializers;
import EdDYON.guaniao.registry.GuaniaoCreativeTabs;
import EdDYON.guaniao.registry.GuaniaoBlocks;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoFeatures;
import EdDYON.guaniao.registry.GuaniaoItems;
import EdDYON.guaniao.registry.GuaniaoMenuTypes;
import EdDYON.guaniao.registry.GuaniaoRecipeSerializers;
import EdDYON.guaniao.registry.GuaniaoSoundEvents;
import EdDYON.guaniao.network.GuaniaoNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

@Mod(value="guaniao")
public class GuaniaoMod {
    public static final String MOD_ID = "guaniao";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GuaniaoMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        GeckoLib.initialize();
        GuaniaoBlocks.BLOCKS.register(modEventBus);
        GuaniaoBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        GuaniaoItems.ITEMS.register(modEventBus);
        GuaniaoMenuTypes.MENU_TYPES.register(modEventBus);
        GuaniaoEntityTypes.ENTITY_TYPES.register(modEventBus);
        GuaniaoFeatures.FEATURES.register(modEventBus);
        GuaniaoBiomeModifierSerializers.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
        GuaniaoRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        GuaniaoSoundEvents.SOUND_EVENTS.register(modEventBus);
        GuaniaoCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        GuaniaoNetwork.register();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BirdConfigManager.loadOrCreateDefaults();
            BirdDroppingDispenserBehavior.registerAll();
        });
    }
}
