package EdDYON.guaniao.client;

import EdDYON.guaniao.client.bath.BirdBathRenderer;
import EdDYON.guaniao.client.camera.PhotographEntityRenderer;
import EdDYON.guaniao.client.cage.BirdCageRenderer;
import EdDYON.guaniao.client.nest.CrowNestRenderer;
import EdDYON.guaniao.client.nest.CrowNestScreen;
import EdDYON.guaniao.client.dropping.BirdDroppingProjectileRenderer;
import EdDYON.guaniao.client.dropping.BirdDroppingSplatRenderer;
import EdDYON.guaniao.client.entity.budgerigar.BudgerigarRenderer;
import EdDYON.guaniao.client.entity.columbid.PigeonRenderer;
import EdDYON.guaniao.client.entity.columbid.SpottedDoveRenderer;
import EdDYON.guaniao.client.entity.crow.CrowRenderer;
import EdDYON.guaniao.client.entity.nightheron.NightHeronRenderer;
import EdDYON.guaniao.client.entity.seagull.SeagullRenderer;
import EdDYON.guaniao.client.entity.sparrow.SparrowRenderer;
import EdDYON.guaniao.client.entity.longtailedtit.LongTailedTitRenderer;
import EdDYON.guaniao.client.entity.cockatiel.CockatielRenderer;
import EdDYON.guaniao.client.entity.macaw.MacawRenderer;
import EdDYON.guaniao.registry.GuaniaoBlockEntityTypes;
import EdDYON.guaniao.registry.GuaniaoBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid="guaniao", bus=Mod.EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.NIGHT_HERON.get(), NightHeronRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.SPARROW.get(), SparrowRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.LONG_TAILED_TIT.get(), LongTailedTitRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.COCKATIEL.get(), CockatielRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.MACAW.get(), MacawRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.BUDGERIGAR.get(), BudgerigarRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.SPOTTED_DOVE.get(), SpottedDoveRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.PIGEON.get(), PigeonRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.CROW.get(), CrowRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.SEAGULL.get(), SeagullRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.PHOTOGRAPH.get(), PhotographEntityRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.BIRD_DROPPING_PROJECTILE.get(), BirdDroppingProjectileRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.BIRD_DROPPING_SPLAT.get(), BirdDroppingSplatRenderer::new);
        event.registerBlockEntityRenderer(GuaniaoBlockEntityTypes.BIRD_CAGE.get(), BirdCageRenderer::new);
        event.registerBlockEntityRenderer(GuaniaoBlockEntityTypes.BIRD_BATH.get(), BirdBathRenderer::new);
        event.registerBlockEntityRenderer(GuaniaoBlockEntityTypes.CROW_NEST.get(), CrowNestRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(GuaniaoBlocks.BREADCRUMBS.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GuaniaoBlocks.BIRD_DROPPING_STAIN_LIGHT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GuaniaoBlocks.BIRD_DROPPING_STAIN_DARK.get(), RenderType.cutout());
            MenuScreens.register(GuaniaoMenuTypes.CROW_NEST.get(), CrowNestScreen::new);
        });
    }
}
