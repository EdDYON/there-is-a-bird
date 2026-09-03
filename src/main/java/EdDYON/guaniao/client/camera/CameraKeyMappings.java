package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CameraKeyMappings {
    public static final KeyMapping OPEN_FILTER_LIBRARY = new KeyMapping(
            "key.guaniao.camera.cycle_filter",
            GLFW.GLFW_KEY_V,
            "key.categories.guaniao"
    );
    public static final KeyMapping OPEN_CREATIVE_CONTROLS = new KeyMapping(
            "key.guaniao.camera.open_creative_controls",
            GLFW.GLFW_KEY_C,
            "key.categories.guaniao"
    );
    public static final KeyMapping FOCUS = new KeyMapping(
            "key.guaniao.camera.focus",
            GLFW.GLFW_KEY_R,
            "key.categories.guaniao"
    );

    private CameraKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_FILTER_LIBRARY);
        event.register(OPEN_CREATIVE_CONTROLS);
        event.register(FOCUS);
    }
}
