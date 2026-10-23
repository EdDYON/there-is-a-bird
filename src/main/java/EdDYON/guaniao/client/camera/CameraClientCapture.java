package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.CameraFilter;
import EdDYON.guaniao.content.camera.CameraFocusMode;
import EdDYON.guaniao.content.camera.CameraSettingsData;
import EdDYON.guaniao.content.camera.CameraState;
import EdDYON.guaniao.content.camera.PhotographData;
import EdDYON.guaniao.content.camera.PhotoImageCodec;
import EdDYON.guaniao.network.GuaniaoNetwork;
import EdDYON.guaniao.network.SetCameraSettingsPacket;
import EdDYON.guaniao.registry.GuaniaoItems;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import org.lwjgl.glfw.GLFW;
import java.util.UUID;

public final class CameraClientCapture {
    private static final int CLEAN_CAPTURE_DELAY_FRAMES = 0;
    private static final double DEFAULT_FOCAL_LENGTH = 50.0D;
    private static final double FOCAL_LENGTH_SCROLL_STEP = 4.0D;
    private static final double FULL_FRAME_SENSOR_WIDTH = 36.0D;
    private static final double MIN_VIEWFINDER_SENSITIVITY_SCALE = 0.28D;
    private static final double MAX_AUTOFOCUS_DISTANCE = CameraState.MAX_FOCUS_DISTANCE;

    private static boolean viewfinderOpen;
    private static InteractionHand viewfinderHand = InteractionHand.MAIN_HAND;
    private static CameraState currentState = CameraState.defaults();
    private static int focusTargetId = -1;
    private static int continuousFocusTicks;

    private static boolean cleanCapturePending;
    private static int cleanCaptureDelayFrames;
    private static InteractionHand pendingCaptureHand = InteractionHand.MAIN_HAND;
    private static double pendingCaptureFov = focalLengthToFov(DEFAULT_FOCAL_LENGTH);
    private static CameraState pendingCaptureState = CameraState.defaults();
    private static float pendingCameraYRot;
    private static float pendingCameraXRot;
    private static boolean storedHideGui;
    private static CameraType storedCameraType = CameraType.FIRST_PERSON;
    private static boolean sensitivityAdjusted;
    private static double storedSensitivity;
    private static boolean cleanFramePrepared;
    private static final CameraShutterInput SHUTTER_INPUT = new CameraShutterInput();
    private static UUID pendingCaptureId;
    private static boolean captureSubmitted;

    private CameraClientCapture() {
    }

    public static void openViewfinder(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || cleanCapturePending || PhotoClientRepository.hasPendingCapture()
                || !isCameraStillHeld(minecraft, hand) || minecraft.screen != null
                || minecraft.getOverlay() != null || !minecraft.isWindowActive()) {
            return;
        }

        if (viewfinderOpen) {
            return;
        }

        if (CameraHelpScreen.showFirstUse(hand)) {
            return;
        }

        SHUTTER_INPUT.reset();
        viewfinderHand = hand;
        currentState = CameraSettingsData.state(minecraft.player.getItemInHand(hand));
        focusTargetId = -1;
        continuousFocusTicks = 0;
        beginSensitivityAdjustment(minecraft);
        applyFocalSensitivity(minecraft);
        viewfinderOpen = true;
        // Opening is a single press, not vanilla's repeated item use while the key is held.
        minecraft.options.keyUse.setDown(false);
        minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 1.5F);
    }

    public static boolean isViewfinderOpen() {
        return viewfinderOpen;
    }

    public static CameraFilter currentFilter() {
        return currentState.filter();
    }

    public static CameraState currentState() {
        return currentState;
    }

    static CameraState renderState() {
        return cleanCapturePending ? pendingCaptureState : currentState;
    }

    public static boolean shouldHideHands() {
        return viewfinderOpen || cleanCapturePending;
    }

    public static boolean isCleanCapturePending() {
        return cleanCapturePending;
    }

    public static void closeViewfinder() {
        persistCurrentState();
        viewfinderOpen = false;
        SHUTTER_INPUT.reset();
        focusTargetId = -1;
        CameraPreviewPostEffect.close();
        restoreSensitivity(Minecraft.getInstance());
    }

    public static void tickViewfinder() {
        if (!viewfinderOpen) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !isCameraStillHeld(minecraft, viewfinderHand)) {
            closeViewfinder();
            return;
        }

        refreshInput(minecraft);

        if (currentState.focusMode() == CameraFocusMode.AF_C) {
            tickContinuousFocus(minecraft);
        } else {
            focusTargetId = -1;
        }
    }

    public static boolean handleMouseScroll(double delta) {
        if (!canUseViewfinderControls() || delta == 0.0D) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        double scrollAmount = Math.max(1.0D, Math.abs(delta));
        if (currentState.focusMode() == CameraFocusMode.MANUAL && minecraft.options.keyShift.isDown()) {
            double factor = Math.pow(1.16D, scrollAmount * (delta > 0.0D ? 1.0D : -1.0D));
            currentState = currentState.withFocusDistance(currentState.focusDistance() * factor);
        } else {
            double next = currentState.focalLength()
                    + (delta > 0.0D ? FOCAL_LENGTH_SCROLL_STEP : -FOCAL_LENGTH_SCROLL_STEP) * scrollAmount;
            currentState = currentState.withFocalLength(next);
            applyFocalSensitivity(minecraft);
        }
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.18F, delta > 0.0D ? 1.65F : 1.1F);
        }
        return true;
    }

    public static boolean handleMouseButton(int button, int action) {
        return handleMouseButton(button, action, 0);
    }

    public static boolean handleMouseButton(int button, int action, int modifiers) {
        if (action == GLFW.GLFW_RELEASE) {
            SHUTTER_INPUT.release(button);
            return false;
        }
        if ((!viewfinderOpen && !cleanCapturePending) || action != GLFW.GLFW_PRESS || (button != 0 && button != 1)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.getOverlay() != null) {
            SHUTTER_INPUT.suspend(false);
            return false;
        }
        if (!minecraft.isWindowActive()) {
            suspendForExternalInput();
            return true;
        }
        if (hasShortcutModifiers(modifiers) || shortcutKeysHeld(minecraft)) {
            suspendForExternalInput();
            return true;
        }
        if (!minecraft.mouseHandler.isMouseGrabbed()) {
            // The click returning from a screenshot or another window only restores mouse capture.
            minecraft.mouseHandler.grabMouse();
            SHUTTER_INPUT.recaptured();
            return true;
        }
        if (cleanCapturePending || PhotoClientRepository.hasPendingCapture()
                || !isCameraStillHeld(minecraft, viewfinderHand) || !SHUTTER_INPUT.press(button)) {
            return true;
        }

        if (button == 0) {
            beginCleanCapture(viewfinderHand, currentFov());
            return true;
        }
        if (button == 1) {
            closeViewfinder();
            if (minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.25F, 0.85F);
            }
            return true;
        }

        return false;
    }

    public static void handleKeyInput(int key, int scanCode, int action, int modifiers) {
        if ((!viewfinderOpen && !cleanCapturePending) || action != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (key == GLFW.GLFW_KEY_PRINT_SCREEN || minecraft.options.keyScreenshot.matches(key, scanCode)
                || hasShortcutModifiers(modifiers) || isShortcutKey(key)) {
            suspendForExternalInput();
        }
    }

    public static void handleScreenshot() {
        if (viewfinderOpen || cleanCapturePending) {
            suspendForExternalInput();
        }
    }

    private static void suspendForExternalInput() {
        if (cleanCapturePending && !captureSubmitted) {
            restoreAfterCleanCapture();
        }
        SHUTTER_INPUT.suspend(true);
    }

    public static boolean canUseViewfinderControls() {
        Minecraft minecraft = Minecraft.getInstance();
        return viewfinderOpen && !cleanCapturePending && inputContextReady(minecraft)
                && !shortcutKeysHeld(minecraft) && SHUTTER_INPUT.isArmed();
    }

    private static void refreshInput(Minecraft minecraft) {
        if (!minecraft.isWindowActive() || shortcutKeysHeld(minecraft)) {
            suspendForExternalInput();
        } else if (!inputContextReady(minecraft)) {
            SHUTTER_INPUT.suspend(false);
        }
        long window = minecraft.getWindow().getWindow();
        SHUTTER_INPUT.observe(inputContextReady(minecraft) && !shortcutKeysHeld(minecraft),
                GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS,
                GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS);
    }

    private static boolean inputContextReady(Minecraft minecraft) {
        return minecraft.player != null && minecraft.level != null && minecraft.isWindowActive()
                && minecraft.screen == null && minecraft.getOverlay() == null && minecraft.mouseHandler.isMouseGrabbed();
    }

    private static boolean hasShortcutModifiers(int modifiers) {
        return (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER)) != 0;
    }

    private static boolean isShortcutKey(int key) {
        return key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL
                || key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT
                || key == GLFW.GLFW_KEY_LEFT_SUPER || key == GLFW.GLFW_KEY_RIGHT_SUPER;
    }

    private static boolean shortcutKeysHeld(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SUPER)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SUPER);
    }

    public static void disconnect() {
        closeViewfinder();
        if (cleanCapturePending) {
            restoreAfterCleanCapture();
        }
    }

    /** Local-only preview used while browsing the filter library. */
    public static boolean previewFilter(CameraFilter filter) {
        if (!viewfinderOpen || cleanCapturePending || filter == null) {
            return false;
        }
        currentState = currentState.withFilter(filter);
        return true;
    }

    /** Saves the highlighted filter and synchronizes it exactly once. */
    public static boolean commitFilter(CameraFilter filter) {
        if (!viewfinderOpen || cleanCapturePending || filter == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !isCameraStillHeld(minecraft, viewfinderHand)) {
            closeViewfinder();
            return false;
        }

        currentState = currentState.withFilter(filter);
        persistCurrentState();
        minecraft.player.playSound(
                SoundEvents.UI_BUTTON_CLICK.value(),
                0.28F,
                1.0F + currentState.filter().id() % 8 * 0.035F
        );
        return true;
    }

    /** Local-only live preview for lens, mode, and parameter browsing. */
    public static boolean previewState(CameraState state) {
        if (!viewfinderOpen || cleanCapturePending || state == null) {
            return false;
        }
        currentState = state;
        applyFocalSensitivity(Minecraft.getInstance());
        return true;
    }

    /** Saves all camera settings to this camera item and synchronizes once. */
    public static boolean commitState(CameraState state) {
        if (!previewState(state)) {
            return false;
        }
        persistCurrentState();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3F, 1.25F);
        }
        return true;
    }

    public static void restorePreviewState(CameraState state) {
        if (state != null && viewfinderOpen && !cleanCapturePending) {
            currentState = state;
            focusTargetId = -1;
            applyFocalSensitivity(Minecraft.getInstance());
        }
    }

    /** Restores the saved filter when the picker is cancelled. */
    public static void restorePreviewFilter(CameraFilter original) {
        if (original != null && viewfinderOpen && !cleanCapturePending) {
            currentState = currentState.withFilter(original);
        }
    }

    public static boolean focusAtCrosshair() {
        if (!viewfinderOpen || cleanCapturePending) {
            return false;
        }
        return updateFocusFromCrosshair(Minecraft.getInstance(), true);
    }

    public static void renderViewfinder(GuiGraphics graphics, float partialTick) {
        if (!viewfinderOpen
                || cleanCapturePending
                || Minecraft.getInstance().screen != null) {
            return;
        }
        CameraViewfinderOverlay.render(graphics, currentState);
    }

    public static void modifyFov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) {
            return;
        }

        if (viewfinderOpen) {
            event.setFOV(currentFov());
        } else if (cleanCapturePending) {
            event.setFOV(pendingCaptureFov);
        }
    }

    public static void onRenderTickStart() {
        if (!cleanCapturePending) {
            return;
        }

        if (cleanCaptureDelayFrames-- > 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!inputContextReady(minecraft) || shortcutKeysHeld(minecraft)) {
            restoreAfterCleanCapture();
            return;
        }

        // Prepare the shutter orientation before Minecraft renders the normal world frame.
        minecraft.player.setYRot(pendingCameraYRot);
        minecraft.player.yRotO = pendingCameraYRot;
        minecraft.player.setXRot(pendingCameraXRot);
        minecraft.player.xRotO = pendingCameraXRot;
        minecraft.gameRenderer.setRenderBlockOutline(false);
        cleanFramePrepared = true;
    }

    public static void onRenderTickEnd() {
        if (!cleanCapturePending || !cleanFramePrepared) {
            return;
        }

        try {
            if (inputContextReady(Minecraft.getInstance()) && !shortcutKeysHeld(Minecraft.getInstance())) {
                captureAndSend(pendingCaptureHand);
            }
        } finally {
            restoreAfterCleanCapture();
        }
    }

    public static void captureAndSend(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || pendingCaptureId == null || captureSubmitted) {
            return;
        }

        RenderTarget captureTarget = CameraPreviewPostEffect.cleanCaptureTarget(minecraft.getMainRenderTarget());
        try (NativeImage image = Screenshot.takeScreenshot(captureTarget)) {
            int[] pixels = cropSquare(image, PhotographData.IMAGE_SIZE);
            CameraImageFilters.apply(
                    pixels,
                    PhotographData.IMAGE_SIZE,
                    PhotographData.IMAGE_SIZE,
                    pendingCaptureState.filter(),
                    System.nanoTime()
            );
            byte[] jpeg = PhotoImageCodec.encodeJpeg(pixels, PhotographData.IMAGE_SIZE, PhotographData.IMAGE_SIZE);
            PhotoClientRepository.upload(pendingCaptureId, hand, jpeg);
            captureSubmitted = true;
        } catch (Exception exception) {
            GuaniaoMod.LOGGER.warn("Failed to capture photograph", exception);
        }
    }

    private static int[] cropSquare(NativeImage image, int size) {
        Minecraft minecraft = Minecraft.getInstance();
        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        CameraViewfinderFrame.Pixels crop = CameraViewfinderFrame.layout(
                guiWidth, guiHeight, minecraft.font.lineHeight).pixels(
                guiWidth, guiHeight, image.getWidth(), image.getHeight());
        int[] pixels = new int[size * size];

        for (int y = 0; y < size; y++) {
            int sampleY = crop.top() + Math.min(crop.height() - 1, (int)((y + 0.5D) * crop.height() / size));
            for (int x = 0; x < size; x++) {
                int sampleX = crop.left() + Math.min(crop.width() - 1, (int)((x + 0.5D) * crop.width() / size));
                pixels[y * size + x] = image.getPixelRGBA(sampleX, sampleY);
            }
        }

        return pixels;
    }

    private static void beginCleanCapture(InteractionHand hand, double fov) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !isCameraStillHeld(minecraft, hand)) {
            closeViewfinder();
            return;
        }

        if (!inputContextReady(minecraft) || shortcutKeysHeld(minecraft)
                || minecraft.player.getCooldowns().isOnCooldown(GuaniaoItems.NIKON_D750.get())) {
            return;
        }
        UUID captureId = PhotoClientRepository.reserveCapture();
        if (captureId == null) {
            return;
        }

        viewfinderOpen = false;
        CameraPreviewPostEffect.close();
        cleanCapturePending = true;
        pendingCaptureId = captureId;
        captureSubmitted = false;
        cleanFramePrepared = false;
        cleanCaptureDelayFrames = CLEAN_CAPTURE_DELAY_FRAMES;
        pendingCaptureHand = hand;
        pendingCaptureFov = fov;
        pendingCaptureState = currentState;
        persistCurrentState();
        pendingCameraYRot = minecraft.player.getViewYRot(1.0F);
        pendingCameraXRot = minecraft.player.getViewXRot(1.0F);
        storedHideGui = minecraft.options.hideGui;
        storedCameraType = minecraft.options.getCameraType();
        minecraft.options.hideGui = true;
        if (storedCameraType != CameraType.THIRD_PERSON_FRONT) {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        }
        minecraft.player.playSound(SoundEvents.SPYGLASS_USE, 0.45F, 1.4F);
    }

    private static void restoreAfterCleanCapture() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.hideGui = storedHideGui;
        minecraft.options.setCameraType(storedCameraType);
        minecraft.gameRenderer.setRenderBlockOutline(true);
        restoreSensitivity(minecraft);
        cleanFramePrepared = false;
        cleanCapturePending = false;
        if (!captureSubmitted) {
            PhotoClientRepository.cancelCapture(pendingCaptureId);
        }
        pendingCaptureId = null;
        captureSubmitted = false;
        SHUTTER_INPUT.reset();
    }

    private static boolean isCameraStillHeld(Minecraft minecraft, InteractionHand hand) {
        if (minecraft.player == null) {
            return false;
        }
        ItemStack stack = minecraft.player.getItemInHand(hand);
        return stack.is(GuaniaoItems.NIKON_D750.get());
    }

    private static double currentFov() {
        return focalLengthToFov(currentState.focalLength());
    }

    private static double focalLengthToFov(double focalLength) {
        double fov = Math.toDegrees(2.0D * Math.atan(FULL_FRAME_SENSOR_WIDTH / (2.0D * focalLength)));
        // Minecraft consumes this as a vertical FOV. Keep the wide-angle safeguard,
        // but preserve the full telephoto range: 150 mm and 200 mm are both below 14 degrees.
        return Math.min(fov, 110.0D);
    }

    private static void beginSensitivityAdjustment(Minecraft minecraft) {
        if (!sensitivityAdjusted) {
            storedSensitivity = minecraft.options.sensitivity().get();
            sensitivityAdjusted = true;
        }
    }

    private static void applyFocalSensitivity(Minecraft minecraft) {
        if (!sensitivityAdjusted) {
            return;
        }
        minecraft.options.sensitivity().set(storedSensitivity * focalSensitivityScale());
    }

    private static void restoreSensitivity(Minecraft minecraft) {
        if (!sensitivityAdjusted) {
            return;
        }
        minecraft.options.sensitivity().set(storedSensitivity);
        sensitivityAdjusted = false;
    }

    private static double focalSensitivityScale() {
        double normalized = Mth.clamp(
                (currentState.focalLength() - CameraState.MIN_FOCAL_LENGTH)
                        / (CameraState.MAX_FOCAL_LENGTH - CameraState.MIN_FOCAL_LENGTH),
                0.0D,
                1.0D
        );
        double smooth = normalized * normalized * (3.0D - 2.0D * normalized);
        return Mth.lerp(smooth, 1.0D, MIN_VIEWFINDER_SENSITIVITY_SCALE);
    }

    private static void tickContinuousFocus(Minecraft minecraft) {
        Entity target = minecraft.level == null || focusTargetId < 0 ? null : minecraft.level.getEntity(focusTargetId);
        if (target != null && target.isAlive() && minecraft.player != null) {
            double distance = minecraft.player.getEyePosition(1.0F).distanceTo(target.getBoundingBox().getCenter());
            if (distance <= CameraState.MAX_FOCUS_DISTANCE) {
                currentState = currentState.withFocusDistance(distance);
                return;
            }
        }

        focusTargetId = -1;
        if (++continuousFocusTicks >= 5) {
            continuousFocusTicks = 0;
            updateFocusFromCrosshair(minecraft, false);
        }
    }

    private static boolean updateFocusFromCrosshair(Minecraft minecraft, boolean feedback) {
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }

        Vec3 eye = minecraft.player.getEyePosition(1.0F);
        Vec3 look = minecraft.player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(MAX_AUTOFOCUS_DISTANCE));
        BlockHitResult blockHit = minecraft.level.clip(new ClipContext(
                eye,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                minecraft.player
        ));
        double blockDistanceSquared = blockHit.getType() == HitResult.Type.MISS
                ? MAX_AUTOFOCUS_DISTANCE * MAX_AUTOFOCUS_DISTANCE
                : eye.distanceToSqr(blockHit.getLocation());
        Vec3 entityRayEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        AABB searchArea = minecraft.player.getBoundingBox()
                .expandTowards(entityRayEnd.subtract(eye))
                .inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                minecraft.level,
                minecraft.player,
                eye,
                entityRayEnd,
                searchArea,
                entity -> !entity.isSpectator() && entity.isPickable()
        );
        HitResult hit = entityHit != null
                && eye.distanceToSqr(entityHit.getLocation()) <= blockDistanceSquared
                ? entityHit
                : blockHit;
        if (hit.getType() == HitResult.Type.MISS) {
            currentState = currentState.withFocusDistance(CameraState.MAX_FOCUS_DISTANCE);
            focusTargetId = -1;
            if (feedback) {
                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.22F, 1.45F);
            }
            return true;
        }

        double distance = eye.distanceTo(hit.getLocation());
        currentState = currentState.withFocusDistance(distance);
        if (currentState.focusMode() == CameraFocusMode.AF_C && hit instanceof EntityHitResult focusedEntityHit) {
            focusTargetId = focusedEntityHit.getEntity().getId();
        } else {
            focusTargetId = -1;
        }
        if (feedback) {
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.22F, 1.8F);
        }
        return true;
    }

    private static void persistCurrentState() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !isCameraStillHeld(minecraft, viewfinderHand)) {
            return;
        }
        ItemStack camera = minecraft.player.getItemInHand(viewfinderHand);
        CameraSettingsData.setState(camera, currentState);
        GuaniaoNetwork.sendToServer(new SetCameraSettingsPacket(viewfinderHand, currentState));
    }
}
