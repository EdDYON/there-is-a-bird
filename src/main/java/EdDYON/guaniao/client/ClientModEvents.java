package EdDYON.guaniao.client;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.client.bath.BirdBathRenderer;
import EdDYON.guaniao.client.camera.PhotographEntityRenderer;
import EdDYON.guaniao.client.camera.CameraOpticsShader;
import EdDYON.guaniao.client.cage.BirdCageRenderer;
import EdDYON.guaniao.client.nest.CrowNestRenderer;
import EdDYON.guaniao.client.nest.CrowNestScreen;
import EdDYON.guaniao.client.dropping.BirdDroppingProjectileRenderer;
import EdDYON.guaniao.client.dropping.BirdDroppingSplatRenderer;
import EdDYON.guaniao.client.fan.FeatherFanProjectileRenderer;
import EdDYON.guaniao.client.particle.BurialCycloneParticle;
import EdDYON.guaniao.client.particle.BurialWindParticle;
import EdDYON.guaniao.client.particle.KillFeatherParticle;
import EdDYON.guaniao.client.particle.RivenSplitParticle;
import EdDYON.guaniao.client.particle.RivenStreakParticle;
import EdDYON.guaniao.client.particle.HuntingMarkParticle;
import EdDYON.guaniao.client.particle.HuntingStreakParticle;
import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.client.entity.budgerigar.BudgerigarRenderer;
import EdDYON.guaniao.client.entity.columbid.PigeonRenderer;
import EdDYON.guaniao.client.entity.columbid.SpottedDoveRenderer;
import EdDYON.guaniao.client.entity.crow.CrowRenderer;
import EdDYON.guaniao.client.entity.nightheron.NightHeronRenderer;
import EdDYON.guaniao.client.entity.seagull.SeagullRenderer;
import EdDYON.guaniao.client.entity.kiwi.KiwiRenderer;
import EdDYON.guaniao.client.entity.myna.MynaRenderer;
import EdDYON.guaniao.client.entity.sparrow.SparrowRenderer;
import EdDYON.guaniao.client.entity.longtailedtit.LongTailedTitRenderer;
import EdDYON.guaniao.client.entity.cockatiel.CockatielRenderer;
import EdDYON.guaniao.client.entity.macaw.MacawRenderer;
import EdDYON.guaniao.content.enchantment.GuaniaoEnchantments;
import EdDYON.guaniao.registry.GuaniaoBlockEntityTypes;
import EdDYON.guaniao.registry.GuaniaoBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoMenuTypes;
import EdDYON.guaniao.registry.GuaniaoParticleTypes;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
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
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.KIWI.get(), KiwiRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.MYNA.get(), MynaRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.PHOTOGRAPH.get(), PhotographEntityRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.BIRD_DROPPING_PROJECTILE.get(), BirdDroppingProjectileRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.BIRD_DROPPING_SPLAT.get(), BirdDroppingSplatRenderer::new);
        event.registerEntityRenderer((EntityType)GuaniaoEntityTypes.FEATHER_FAN_PROJECTILE.get(), FeatherFanProjectileRenderer::new);
        event.registerBlockEntityRenderer(GuaniaoBlockEntityTypes.BIRD_CAGE.get(), BirdCageRenderer::new);
        event.registerBlockEntityRenderer(GuaniaoBlockEntityTypes.BIRD_BATH.get(), BirdBathRenderer::new);
        event.registerBlockEntityRenderer(GuaniaoBlockEntityTypes.CROW_NEST.get(), CrowNestRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(GuaniaoParticleTypes.KILL_FEATHER.get(), KillFeatherParticle.Provider::new);
        event.registerSpriteSet(GuaniaoParticleTypes.BURIAL_WIND.get(), BurialWindParticle.Provider::new);
        event.registerSpriteSet(GuaniaoParticleTypes.BURIAL_CYCLONE.get(), BurialCycloneParticle.Provider::new);
        event.registerSpriteSet(GuaniaoParticleTypes.RIVEN_SPLIT.get(), RivenSplitParticle.Provider::new);
        event.registerSpriteSet(GuaniaoParticleTypes.RIVEN_STREAK.get(), RivenStreakParticle.Provider::new);
        event.registerSpriteSet(GuaniaoParticleTypes.HUNTING_MARK.get(), HuntingMarkParticle.Provider::new);
        event.registerSpriteSet(GuaniaoParticleTypes.HUNTING_STREAK.get(), HuntingStreakParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws java.io.IOException {
        CameraOpticsShader.register(event);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // These spawn eggs use complete, full-color icons rather than vanilla's two tint masks.
        // Returning white prevents ForgeSpawnEggItem's base/spot colors from darkening layer0.
        event.register(
                (stack, tintIndex) -> 0xFFFFFFFF,
                GuaniaoItems.NIGHT_HERON_SPAWN_EGG.get(),
                GuaniaoItems.SPARROW_SPAWN_EGG.get(),
                GuaniaoItems.LONG_TAILED_TIT_SPAWN_EGG.get(),
                GuaniaoItems.COCKATIEL_SPAWN_EGG.get(),
                GuaniaoItems.MACAW_SPAWN_EGG.get(),
                GuaniaoItems.BUDGERIGAR_SPAWN_EGG.get(),
                GuaniaoItems.SPOTTED_DOVE_SPAWN_EGG.get(),
                GuaniaoItems.PIGEON_SPAWN_EGG.get(),
                GuaniaoItems.CROW_SPAWN_EGG.get(),
                GuaniaoItems.SEAGULL_SPAWN_EGG.get(),
                GuaniaoItems.KIWI_SPAWN_EGG.get(),
                GuaniaoItems.MYNA_SPAWN_EGG.get()
        );
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        // Mutation textures are generated lazily and cached; after an F3+T reload the texture
        // manager discards them, so the cache must be cleared or mutated birds render as missing.
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> BirdMutationTextureFactory.onResourceReload());
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(GuaniaoBlocks.BREADCRUMBS.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GuaniaoBlocks.BIRD_DROPPING_STAIN_LIGHT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GuaniaoBlocks.BIRD_DROPPING_STAIN_DARK.get(), RenderType.cutout());
            MenuScreens.register(GuaniaoMenuTypes.CROW_NEST.get(), CrowNestScreen::new);
            ItemProperties.register(
                    GuaniaoItems.WIND_FEATHER_FAN.get(),
                    new ResourceLocation(GuaniaoMod.MOD_ID, "burial_plume"),
                    (stack, level, entity, seed) -> EnchantmentHelper.getItemEnchantmentLevel(
                            GuaniaoEnchantments.BURIAL_PLUME.get(), stack) > 0 ? 1.0F : 0.0F
            );
            ItemProperties.register(
                    GuaniaoItems.WIND_FEATHER_FAN.get(),
                    new ResourceLocation(GuaniaoMod.MOD_ID, "riven_plume"),
                    (stack, level, entity, seed) -> EnchantmentHelper.getItemEnchantmentLevel(
                            GuaniaoEnchantments.RIVEN_PLUME.get(), stack) > 0 ? 1.0F : 0.0F
            );
            ItemProperties.register(
                    GuaniaoItems.WIND_FEATHER_FAN.get(),
                    new ResourceLocation(GuaniaoMod.MOD_ID, "hunting_return"),
                    (stack, level, entity, seed) -> EnchantmentHelper.getItemEnchantmentLevel(
                            GuaniaoEnchantments.HUNTING_RETURN.get(), stack) > 0 ? 1.0F : 0.0F
            );
        });
    }
}
