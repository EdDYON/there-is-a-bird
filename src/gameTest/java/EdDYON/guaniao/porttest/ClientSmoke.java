package EdDYON.guaniao.porttest;

import EdDYON.guaniao.client.guide.BirdGuideScreen;
import EdDYON.guaniao.client.config.BirdConfigScreen;
import EdDYON.guaniao.config.BirdConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Development-only render exercise; excluded from the release jar. */
@EventBusSubscriber(modid = "guaniao_port_tests", value = Dist.CLIENT)
public final class ClientSmoke {
    private static int ticks;
    private static int waiting;
    private static BirdGuideScreen guide;
    private static net.minecraft.world.item.ItemStack capturedFilm;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("guaniao.portClientSmoke")) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            if (++waiting > 2400) throw new IllegalStateException("Client smoke did not enter the test world");
            return;
        }
        ticks++;
        if (ticks == 40) {
            guide = new BirdGuideScreen();
            minecraft.setScreen(guide);
        }
        if (ticks >= 50 && ticks < 690 && (ticks - 50) % 40 == 0) {
            int index = (ticks - 50) / 40;
            var method = BirdGuideScreen.class.getDeclaredMethod("selectEntry", int.class);
            method.setAccessible(true);
            method.invoke(guide, index);
            System.out.println("PORT CLIENT: render species " + index);
        }
        if (ticks >= 70 && ticks < 710 && (ticks - 70) % 40 == 0) {
            var method = BirdGuideScreen.class.getDeclaredMethod("selectPose", int.class);
            method.setAccessible(true);
            method.invoke(guide, 2);
        }
        if (ticks == 68 || ticks == 88 || ticks == 668 || ticks == 688) capture(minecraft, "guide-" + ticks + ".png");
        if (ticks == 720) minecraft.setScreen(new BirdConfigScreen(BirdConfigManager.snapshot()));
        if (ticks == 750) capture(minecraft, "config.png");
        if (ticks == 780) minecraft.setScreen(new ItemModels());
        if (ticks == 805) capture(minecraft, "item-models.png");
        if (ticks == 820) {
            minecraft.setScreen(null);
            minecraft.getSingleplayerServer().execute(() -> {
                var player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(minecraft.player.getUUID());
                player.getInventory().clearContent();
                player.getInventory().setItem(player.getInventory().selected,
                        new net.minecraft.world.item.ItemStack(EdDYON.guaniao.registry.GuaniaoItems.NIKON_D750.get()));
                player.containerMenu.broadcastChanges();
            });
        }
        if (ticks == 850) capture(minecraft, "camera-held.png");
        if (ticks == 860) EdDYON.guaniao.client.camera.CameraClientCapture.openViewfinder(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (ticks == 880) capture(minecraft, "camera-viewfinder.png");
        if (ticks == 890) EdDYON.guaniao.client.camera.CameraClientCapture.handleMouseButton(0, 1);
        if (ticks == 970) {
            var film = minecraft.player.getInventory().items.stream()
                    .filter(EdDYON.guaniao.content.camera.PhotographData::hasImage).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Camera did not return a stored photo through the network"));
            capturedFilm = film;
            minecraft.setScreen(new EdDYON.guaniao.client.camera.PhotographScreen(film));
            System.out.println("PORT CAMERA: capture and server upload succeeded: " + EdDYON.guaniao.content.camera.PhotographData.id(film));
        }
        if (ticks == 1010) capture(minecraft, "photograph.png");
        if (ticks == 1080) {
            String id = EdDYON.guaniao.content.camera.PhotographData.id(capturedFilm);
            byte[] jpeg = EdDYON.guaniao.client.camera.PhotoClientRepository.cached(id);
            var texture = EdDYON.guaniao.client.camera.PhotographTextureCache.textureFor(capturedFilm);
            System.out.println("PORT PHOTO STATE: jpeg=" + (jpeg == null ? 0 : jpeg.length) + " texture=" + texture);
            if (jpeg == null) {
                for (String field : new String[]{"DOWNLOADS", "RETRY_AFTER", "FAILURE_COUNTS", "activeRequest"}) {
                    var f = EdDYON.guaniao.client.camera.PhotoClientRepository.class.getDeclaredField(field);
                    f.setAccessible(true);
                    System.out.println("PORT PHOTO DEBUG: " + field + "=" + f.get(null));
                }
            }
            if (jpeg == null || !texture.getPath().contains("guaniao_photo")) throw new IllegalStateException("Photo download/texture upload failed");
            EdDYON.guaniao.client.camera.PhotographTextureCache.export(capturedFilm);
            capture(minecraft, "photograph.png");
        }
        if (ticks == 1120) {
            System.out.println("PORT CLIENT PASS: integrated world, 16 standing and flight previews, config rendering, item models, camera capture/upload/download");
            minecraft.stop();
        }
    }


    private static final class ItemModels extends net.minecraft.client.gui.screens.Screen {
        private final java.util.List<net.minecraft.world.item.ItemStack> items;
        ItemModels() {
            super(net.minecraft.network.chat.Component.literal("Migration item render check"));
            items = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                    .filter(item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("guaniao"))
                    .map(net.minecraft.world.item.ItemStack::new).toList();
        }
        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFFC6C6C6);
            graphics.drawString(font, "GUI / held model render check", 10, 10, 0xFF333333, false);
            for (int i = 0; i < items.size(); i++) {
                int x = 15 + i % 10 * 60, y = 35 + i / 10 * 54;
                var stack = items.get(i);
                graphics.renderItem(stack, x, y);
                graphics.pose().pushPose();
                graphics.pose().translate(x + 40, y + 12, 100);
                graphics.pose().scale(25, -25, 25);
                com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
                minecraft.getItemRenderer().renderStatic(stack, net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        graphics.pose(), graphics.bufferSource(), minecraft.level, i);
                graphics.flush();
                graphics.pose().popPose();
            }
        }
    }

    private static void capture(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(),
                message -> System.out.println("PORT CAPTURE: " + message.getString()));
    }
}
