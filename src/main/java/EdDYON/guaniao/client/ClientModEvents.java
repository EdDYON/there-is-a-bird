package EdDYON.guaniao.client;
import EdDYON.guaniao.client.entity.cockatoo.CockatooRenderer;
import EdDYON.guaniao.client.entity.kingfisher.KingfisherRenderer;
import EdDYON.guaniao.client.camera.frame.FrameWorkbenchScreen;
import EdDYON.guaniao.client.camera.CameraPreviewPostEffect;

import EdDYON.guaniao.client.bath.BirdBathItemRenderer;
import EdDYON.guaniao.client.cage.BirdCageItemRenderer;
import EdDYON.guaniao.client.camera.FilmItemRenderer;
import EdDYON.guaniao.client.camera.NikonD750ItemRenderer;
import EdDYON.guaniao.client.camera.PhotographItemRenderer;
import EdDYON.guaniao.client.nest.CrowNestItemRenderer;
import EdDYON.guaniao.client.particle.PlaceableBlockBreakEffects;
import EdDYON.guaniao.content.fan.FeatherFanItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Supplier;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import EdDYON.guaniao.client.entity.earthworm.EarthwormRenderer;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.client.bath.BirdBathRenderer;
import EdDYON.guaniao.client.camera.PhotographEntityRenderer;
import EdDYON.guaniao.client.camera.CameraOpticsShader;
import EdDYON.guaniao.client.cage.BirdCageRenderer;
import EdDYON.guaniao.client.nest.CrowNestRenderer;
import EdDYON.guaniao.client.food.BaggedFriesRenderer;
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
import EdDYON.guaniao.client.particle.PlaceableBreakFleckParticle;
import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.client.entity.budgerigar.BudgerigarRenderer;
import EdDYON.guaniao.client.entity.hummingbird.HummingbirdRenderer;
import EdDYON.guaniao.client.entity.columbid.PigeonRenderer;
import EdDYON.guaniao.client.entity.columbid.SpottedDoveRenderer;
import EdDYON.guaniao.client.entity.crow.CrowRenderer;
import EdDYON.guaniao.client.entity.nightheron.NightHeronRenderer;
import EdDYON.guaniao.client.entity.seagull.SeagullRenderer;
import EdDYON.guaniao.client.entity.kiwi.KiwiRenderer;
import EdDYON.guaniao.client.entity.myna.MynaRenderer;
import EdDYON.guaniao.client.entity.woodcock.WoodcockRenderer;
import EdDYON.guaniao.client.entity.kestrel.KestrelRenderer;
import EdDYON.guaniao.client.entity.cassowary.CassowaryRenderer;
import EdDYON.guaniao.client.entity.sparrow.SparrowRenderer;
import EdDYON.guaniao.client.entity.longtailedtit.LongTailedTitRenderer;
import EdDYON.guaniao.client.entity.cockatiel.CockatielRenderer;
import EdDYON.guaniao.client.entity.macaw.MacawRenderer;
import EdDYON.guaniao.client.entity.umbrellacockatoo.UmbrellaCockatooRenderer;
import EdDYON.guaniao.content.enchantment.GuaniaoEnchantments;
import EdDYON.guaniao.registry.GuaniaoBlockEntityTypes;
import EdDYON.guaniao.registry.GuaniaoBlocks;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoMenuTypes;
import EdDYON.guaniao.registry.GuaniaoParticleTypes;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid="guaniao", value={Dist.CLIENT})
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(PlaceableBlockBreakEffects.birdCage(),
                GuaniaoBlocks.SMALL_BIRD_CAGE.get(), GuaniaoBlocks.MEDIUM_BIRD_CAGE.get(), GuaniaoBlocks.LARGE_BIRD_CAGE.get());
        event.registerBlock(PlaceableBlockBreakEffects.birdBath(),
                GuaniaoBlocks.WOODEN_BIRD_BATH.get(), GuaniaoBlocks.STONE_BIRD_BATH.get(), GuaniaoBlocks.BIRD_BATH.get(),
                GuaniaoBlocks.WOODEN_BIRD_BATH_2.get(), GuaniaoBlocks.STONE_BIRD_BATH_2.get(), GuaniaoBlocks.BIRD_BATH_2.get());
        event.registerBlock(PlaceableBlockBreakEffects.crowNest(), GuaniaoBlocks.CROW_NEST.get());
        event.registerBlock(PlaceableBlockBreakEffects.baggedFries(), GuaniaoBlocks.BAGGED_FRIES.get());
        event.registerBlock(PlaceableBlockBreakEffects.breadcrumbs(), GuaniaoBlocks.BREADCRUMBS.get());
        event.registerBlock(PlaceableBlockBreakEffects.droppingStain(),
                GuaniaoBlocks.BIRD_DROPPING_STAIN_LIGHT.get(), GuaniaoBlocks.BIRD_DROPPING_STAIN_DARK.get());

        registerItemRenderers(event, BirdCageItemRenderer::new,
                GuaniaoItems.SMALL_BIRD_CAGE.get(), GuaniaoItems.MEDIUM_BIRD_CAGE.get(), GuaniaoItems.LARGE_BIRD_CAGE.get());
        registerItemRenderers(event, BirdBathItemRenderer::new,
                GuaniaoItems.WOODEN_BIRD_BATH.get(), GuaniaoItems.STONE_BIRD_BATH.get(), GuaniaoItems.BIRD_BATH.get(),
                GuaniaoItems.WOODEN_BIRD_BATH_2.get(), GuaniaoItems.STONE_BIRD_BATH_2.get(), GuaniaoItems.BIRD_BATH_2.get());
        registerItemRenderers(event, CrowNestItemRenderer::new, GuaniaoItems.CROW_NEST.get());
        registerItemRenderers(event, FilmItemRenderer::new, GuaniaoItems.FILM.get());
        registerItemRenderers(event, PhotographItemRenderer::new, GuaniaoItems.PHOTOGRAPH.get());
        event.registerItem(new IClientItemExtensions() {
            private NikonD750ItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new NikonD750ItemRenderer();
                }
                return this.renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                Minecraft minecraft = Minecraft.getInstance();
                if (entityLiving == minecraft.player && minecraft.options.getCameraType() == CameraType.FIRST_PERSON) {
                    return HumanoidModel.ArmPose.ITEM;
                }
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        }, GuaniaoItems.NIKON_D750.get());
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                                   float partialTick, float equipProgress, float swingProgress) {
                InteractionHand renderedHand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                if (!player.isUsingItem()
                        || player.getUsedItemHand() != renderedHand
                        || !ItemStack.isSameItemSameComponents(player.getUseItem(), itemInHand)) {
                    return false;
                }

                int usedTicks = GuaniaoItems.WIND_FEATHER_FAN.get().getUseDuration(itemInHand, player) - player.getUseItemRemainingTicks();
                float elapsedTicks = usedTicks + partialTick;
                float chargeProgress = Mth.clamp(elapsedTicks / FeatherFanItem.MAX_CHARGE_TICKS, 0.0F, 1.0F);
                float chargeEase = smootherStep(chargeProgress);
                float progress = getDampedFanPoseProgress(elapsedTicks);
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
                if (usedTicks >= FeatherFanItem.MAX_CHARGE_TICKS) {
                    float shakeTime = usedTicks - FeatherFanItem.MAX_CHARGE_TICKS + partialTick;
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
        }, GuaniaoItems.WIND_FEATHER_FAN.get());
    }

    private static void registerItemRenderers(RegisterClientExtensionsEvent event,
                                             Supplier<? extends BlockEntityWithoutLevelRenderer> factory, Item... items) {
        for (Item item : items) {
            // Each registered item keeps its own lazy renderer, including cage/bath variants.
            event.registerItem(new IClientItemExtensions() {
                private BlockEntityWithoutLevelRenderer renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (this.renderer == null) {
                        this.renderer = factory.get();
                    }
                    return this.renderer;
                }
            }, item);
        }
    }

    private static float getDampedFanPoseProgress(float elapsedTicks) {
        float time = Mth.clamp(elapsedTicks / FeatherFanItem.MAX_CHARGE_TICKS, 0.0F, 1.0F);
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

    @SubscribeEvent
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(GuaniaoMenuTypes.CROW_NEST.get(), CrowNestScreen::new);
        event.register(GuaniaoMenuTypes.FRAME_WORKBENCH.get(), FrameWorkbenchScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GuaniaoEntityTypes.EARTHWORM.get(), EarthwormRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.HUMMINGBIRD.get(), HummingbirdRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.KINGFISHER.get(), KingfisherRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.NIGHT_HERON.get(), NightHeronRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.SPARROW.get(), SparrowRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.LONG_TAILED_TIT.get(), LongTailedTitRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.COCKATIEL.get(), CockatielRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.MACAW.get(), MacawRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.BUDGERIGAR.get(), BudgerigarRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.SPOTTED_DOVE.get(), SpottedDoveRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.PIGEON.get(), PigeonRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.CROW.get(), CrowRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.SEAGULL.get(), SeagullRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.KIWI.get(), KiwiRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.MYNA.get(), MynaRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.WOODCOCK.get(), WoodcockRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.KESTREL.get(), KestrelRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.CASSOWARY.get(), CassowaryRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.UMBRELLA_COCKATOO.get(), UmbrellaCockatooRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.COCKATOO.get(), CockatooRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.PHOTOGRAPH.get(), PhotographEntityRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.BIRD_DROPPING_PROJECTILE.get(), BirdDroppingProjectileRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.BIRD_DROPPING_SPLAT.get(), BirdDroppingSplatRenderer::new);
        event.registerEntityRenderer(GuaniaoEntityTypes.FEATHER_FAN_PROJECTILE.get(), FeatherFanProjectileRenderer::new);
        event.registerBlockEntityRenderer(GuaniaoBlockEntityTypes.BIRD_CAGE.get(), BirdCageRenderer::new);
        event.registerBlockEntityRenderer(GuaniaoBlockEntityTypes.BIRD_BATH.get(), BirdBathRenderer::new);
        event.registerBlockEntityRenderer(GuaniaoBlockEntityTypes.CROW_NEST.get(), CrowNestRenderer::new);
        event.registerBlockEntityRenderer(GuaniaoBlockEntityTypes.BAGGED_FRIES.get(), BaggedFriesRenderer::new);
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
        event.registerSpriteSet(GuaniaoParticleTypes.PLACEABLE_FLECK.get(), PlaceableBreakFleckParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws java.io.IOException {
        CameraOpticsShader.register(event);
        CameraPreviewPostEffect.register(event);
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
            ItemProperties.register(
                    GuaniaoItems.WIND_FEATHER_FAN.get(),
                    ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, "burial_plume"),
                    (stack, level, entity, seed) -> GuaniaoEnchantments.level(stack, GuaniaoEnchantments.BURIAL_PLUME) > 0 ? 1.0F : 0.0F
            );
            ItemProperties.register(
                    GuaniaoItems.WIND_FEATHER_FAN.get(),
                    ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, "riven_plume"),
                    (stack, level, entity, seed) -> GuaniaoEnchantments.level(stack, GuaniaoEnchantments.RIVEN_PLUME) > 0 ? 1.0F : 0.0F
            );
            ItemProperties.register(
                    GuaniaoItems.WIND_FEATHER_FAN.get(),
                    ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, "hunting_return"),
                    (stack, level, entity, seed) -> GuaniaoEnchantments.level(stack, GuaniaoEnchantments.HUNTING_RETURN) > 0 ? 1.0F : 0.0F
            );
        });
    }
}
