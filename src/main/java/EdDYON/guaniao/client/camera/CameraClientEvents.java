package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.registry.GuaniaoItems;
import EdDYON.guaniao.config.BirdConfigManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT)
public final class CameraClientEvents {
    private CameraClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CameraClientCapture.tickViewfinder();
            while (CameraKeyMappings.CYCLE_FILTER.consumeClick()) {
                CameraClientCapture.cycleFilter();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PhotographTextureCache.pumpUploads();
            CameraClientCapture.onRenderTickEnd();
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        CameraClientCapture.modifyFov(event);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (CameraClientCapture.shouldHideHands()) {
            event.setCanceled(true);
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && holdsCamera(player)) {
            event.setCanceled(true);
            if (isCameraHand(player, event.getHand())) {
                renderFirstPersonCamera(event, player, player.getItemInHand(event.getHand()));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (CameraClientCapture.isViewfinderOpen()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        CameraClientCapture.renderViewfinder(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            CameraClientCapture.rebindOffscreenCaptureTarget();
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (CameraClientCapture.handleMouseScroll(event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (CameraClientCapture.handleMouseButton(event.getButton(), event.getAction())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && event.getKey() == GLFW.GLFW_KEY_ESCAPE && CameraClientCapture.isViewfinderOpen()) {
            CameraClientCapture.closeViewfinder();
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        CameraClientCapture.closeViewfinder();
        PhotographTextureCache.clear();
        BirdConfigManager.loadOrCreateDefaults();
    }

    private static boolean holdsCamera(LocalPlayer player) {
        return player.getMainHandItem().is(GuaniaoItems.NIKON_D750.get())
                || player.getOffhandItem().is(GuaniaoItems.NIKON_D750.get());
    }

    private static boolean isCameraHand(LocalPlayer player, InteractionHand hand) {
        return player.getItemInHand(hand).is(GuaniaoItems.NIKON_D750.get());
    }

    private static void renderFirstPersonCamera(RenderHandEvent event, LocalPlayer player, ItemStack camera) {
        PoseStack poseStack = event.getPoseStack();
        Minecraft minecraft = Minecraft.getInstance();
        float equip = equipAnimation(event);

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.11F - equip * 0.86F, -0.88F + equip * 0.12F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-5.0F + equip * 22.0F));
        minecraft.getItemRenderer().renderStatic(
                camera,
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                event.getPackedLight(),
                OverlayTexture.NO_OVERLAY,
                poseStack,
                event.getMultiBufferSource(),
                player.level(),
                player.getId()
        );
        poseStack.popPose();

        EntityRenderer<? super LocalPlayer> renderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
        if (renderer instanceof PlayerRenderer playerRenderer) {
            renderCameraArm(playerRenderer, player, poseStack, event, HumanoidArm.RIGHT);
            renderCameraArm(playerRenderer, player, poseStack, event, HumanoidArm.LEFT);
        }
    }

    private static void renderCameraArm(PlayerRenderer renderer, LocalPlayer player, PoseStack poseStack, RenderHandEvent event, HumanoidArm arm) {
        boolean right = arm == HumanoidArm.RIGHT;
        float side = right ? 1.0F : -1.0F;
        float swing = Mth.clamp(event.getSwingProgress(), 0.0F, 0.25F);
        float rootSwing = Mth.sqrt(swing);
        float swingX = -0.18F * Mth.sin(rootSwing * Mth.PI);
        float swingY = 0.14F * Mth.sin(rootSwing * (Mth.PI * 2.0F));
        float swingZ = -0.18F * Mth.sin(swing * Mth.PI);
        float equip = equipAnimation(event);

        poseStack.pushPose();
        poseStack.translate(side * (0.84F + swingX + equip * 0.14F), -0.44F + swingY - equip * 0.82F, -1F + swingZ + equip * 0.08F);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * (32.0F + equip * 14.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * (-4.0F - equip * 12.0F)));

        // Same coordinate family as vanilla first-person arms, tuned lower and wider so the camera stays visible.
        poseStack.translate(side * -1.0F, 3.45F, 3.35F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * 112.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(205.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -124.0F));
        poseStack.translate(side * 5.45F, -0.20F, 0.0F);

        if (right) {
            renderer.renderRightHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
        } else {
            renderer.renderLeftHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
        }

        poseStack.popPose();
    }

    private static float equipAnimation(RenderHandEvent event) {
        float equip = 1.0F - Mth.clamp(event.getEquipProgress(), 0.0F, 1.0F);
        return equip * equip * (3.0F - 2.0F * equip);
    }
}
