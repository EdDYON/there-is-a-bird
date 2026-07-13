package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.content.camera.PhotographData;
import EdDYON.guaniao.network.GuaniaoNetwork;
import EdDYON.guaniao.network.PhotographTakenPacket;
import EdDYON.guaniao.registry.GuaniaoItems;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ViewportEvent;

public final class CameraClientCapture {
    private static final int CLEAN_CAPTURE_DELAY_FRAMES = 2;
    private static final double MIN_FOCAL_LENGTH = 18.0D;
    private static final double MAX_FOCAL_LENGTH = 200.0D;
    private static final double DEFAULT_FOCAL_LENGTH = 50.0D;
    private static final double FOCAL_LENGTH_SCROLL_STEP = 4.0D;
    private static final double FULL_FRAME_SENSOR_WIDTH = 36.0D;
    private static final double MIN_VIEWFINDER_SENSITIVITY_SCALE = 0.28D;

    private static boolean viewfinderOpen;
    private static InteractionHand viewfinderHand = InteractionHand.MAIN_HAND;
    private static double focalLength = DEFAULT_FOCAL_LENGTH;

    private static boolean cleanCapturePending;
    private static int cleanCaptureDelayFrames;
    private static InteractionHand pendingCaptureHand = InteractionHand.MAIN_HAND;
    private static double pendingCaptureFov = focalLengthToFov(DEFAULT_FOCAL_LENGTH);
    private static boolean storedHideGui;
    private static CameraType storedCameraType = CameraType.FIRST_PERSON;
    private static boolean sensitivityAdjusted;
    private static double storedSensitivity;

    private CameraClientCapture() {
    }

    public static void openViewfinder(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || cleanCapturePending) {
            return;
        }

        if (viewfinderOpen) {
            closeViewfinder();
            return;
        }

        viewfinderHand = hand;
        focalLength = Mth.clamp(focalLength, MIN_FOCAL_LENGTH, MAX_FOCAL_LENGTH);
        beginSensitivityAdjustment(minecraft);
        applyFocalSensitivity(minecraft);
        viewfinderOpen = true;
        minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 1.5F);
    }

    public static boolean isViewfinderOpen() {
        return viewfinderOpen;
    }

    public static boolean shouldHideHands() {
        return viewfinderOpen || cleanCapturePending;
    }

    public static void closeViewfinder() {
        viewfinderOpen = false;
        restoreSensitivity(Minecraft.getInstance());
    }

    public static void tickViewfinder() {
        if (!viewfinderOpen) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !isCameraStillHeld(minecraft, viewfinderHand)) {
            closeViewfinder();
        }
    }

    public static boolean handleMouseScroll(double delta) {
        if (!viewfinderOpen || cleanCapturePending) {
            return false;
        }

        double scrollAmount = Math.max(1.0D, Math.abs(delta));
        double next = Mth.clamp(focalLength + (delta > 0.0D ? FOCAL_LENGTH_SCROLL_STEP : -FOCAL_LENGTH_SCROLL_STEP) * scrollAmount,
                MIN_FOCAL_LENGTH,
                MAX_FOCAL_LENGTH);
        if (Math.abs(next - focalLength) > 0.001D) {
            focalLength = next;
            Minecraft minecraft = Minecraft.getInstance();
            applyFocalSensitivity(minecraft);
            if (minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.18F, delta > 0.0D ? 1.65F : 1.1F);
            }
        }
        return true;
    }

    public static boolean handleMouseButton(int button, int action) {
        if (!viewfinderOpen || cleanCapturePending || action != 1) {
            return false;
        }

        if (button == 0) {
            beginCleanCapture(viewfinderHand, currentFov());
            return true;
        }
        if (button == 1) {
            closeViewfinder();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.25F, 0.85F);
            }
            return true;
        }

        return false;
    }

    public static void renderViewfinder(GuiGraphics graphics, float partialTick) {
        if (!viewfinderOpen || cleanCapturePending) {
            return;
        }
        CameraViewfinderOverlay.render(graphics, focalLength, currentFov());
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

    public static void onRenderTickEnd() {
        if (!cleanCapturePending) {
            return;
        }

        if (cleanCaptureDelayFrames-- > 0) {
            return;
        }

        try {
            captureAndSend(pendingCaptureHand);
        } finally {
            restoreAfterCleanCapture();
        }
    }

    public static void captureAndSend(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getMainRenderTarget() == null) {
            return;
        }

        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            int[] pixels = cropSquare(image, PhotographData.IMAGE_SIZE);
            GuaniaoNetwork.CHANNEL.sendToServer(new PhotographTakenPacket(hand, pixels));
            minecraft.player.displayClientMessage(Component.translatable("item.guaniao.nikon_d750.captured"), true);
        } catch (Exception exception) {
            minecraft.player.displayClientMessage(Component.translatable("item.guaniao.nikon_d750.capture_failed"), true);
        }
    }

    private static int[] cropSquare(NativeImage image, int size) {
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        int sourceSize = Math.min(sourceWidth, sourceHeight);
        int offsetX = (sourceWidth - sourceSize) / 2;
        int offsetY = (sourceHeight - sourceSize) / 2;
        int[] pixels = new int[size * size];

        for (int y = 0; y < size; y++) {
            int startY = offsetY + y * sourceSize / size;
            int endY = offsetY + Math.max(startY - offsetY + 1, (y + 1) * sourceSize / size);
            endY = Math.min(offsetY + sourceSize, Math.max(startY + 1, endY));
            for (int x = 0; x < size; x++) {
                int startX = offsetX + x * sourceSize / size;
                int endX = offsetX + Math.max(startX - offsetX + 1, (x + 1) * sourceSize / size);
                endX = Math.min(offsetX + sourceSize, Math.max(startX + 1, endX));
                pixels[y * size + x] = averagePixels(image, startX, startY, endX, endY);
            }
        }

        return pixels;
    }

    private static int averagePixels(NativeImage image, int startX, int startY, int endX, int endY) {
        long a = 0L;
        long b = 0L;
        long g = 0L;
        long r = 0L;
        int count = 0;

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int pixel = image.getPixelRGBA(x, y);
                a += pixel >>> 24 & 255;
                b += pixel >>> 16 & 255;
                g += pixel >>> 8 & 255;
                r += pixel & 255;
                count++;
            }
        }

        if (count <= 0) {
            return image.getPixelRGBA(startX, startY);
        }

        return ((int)(a / count) << 24)
                | ((int)(b / count) << 16)
                | ((int)(g / count) << 8)
                | (int)(r / count);
    }

    private static void beginCleanCapture(InteractionHand hand, double fov) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !isCameraStillHeld(minecraft, hand)) {
            closeViewfinder();
            return;
        }

        viewfinderOpen = false;
        restoreSensitivity(minecraft);
        cleanCapturePending = true;
        cleanCaptureDelayFrames = CLEAN_CAPTURE_DELAY_FRAMES;
        pendingCaptureHand = hand;
        pendingCaptureFov = fov;
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
        restoreSensitivity(minecraft);
        cleanCapturePending = false;
    }

    private static boolean isCameraStillHeld(Minecraft minecraft, InteractionHand hand) {
        if (minecraft.player == null) {
            return false;
        }
        ItemStack stack = minecraft.player.getItemInHand(hand);
        return stack.is(GuaniaoItems.NIKON_D750.get());
    }

    private static double currentFov() {
        return focalLengthToFov(focalLength);
    }

    private static double focalLengthToFov(double focalLength) {
        double fov = Math.toDegrees(2.0D * Math.atan(FULL_FRAME_SENSOR_WIDTH / (2.0D * focalLength)));
        return Mth.clamp(fov, 14.0D, 92.0D);
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
        double normalized = Mth.clamp((focalLength - MIN_FOCAL_LENGTH) / (MAX_FOCAL_LENGTH - MIN_FOCAL_LENGTH), 0.0D, 1.0D);
        double smooth = normalized * normalized * (3.0D - 2.0D * normalized);
        return Mth.lerp(smooth, 1.0D, MIN_VIEWFINDER_SENSITIVITY_SCALE);
    }
}
