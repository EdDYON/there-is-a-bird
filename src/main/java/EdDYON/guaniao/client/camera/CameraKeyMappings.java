package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyModifier;
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
            GLFW.GLFW_KEY_TAB,
            "key.categories.guaniao"
    );
    private static boolean focusMigrationChecked;

    private CameraKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_FILTER_LIBRARY);
        event.register(OPEN_CREATIVE_CONTROLS);
        event.register(FOCUS);
    }

    /** Run after Forge has loaded saved mod bindings, before the first camera input. */
    static void migrateLegacyFocusKey() {
        Minecraft minecraft = Minecraft.getInstance();
        if (focusMigrationChecked || minecraft.options == null || minecraft.getOverlay() != null
                || Arrays.stream(minecraft.options.keyMappings).noneMatch(binding -> binding == FOCUS)) return;
        focusMigrationChecked = true;
        if (!CameraClientPreferences.isFocusKeyMigrationPending()) return;

        // Only the old, unmodified default migrates. Custom keys and modifiers stay intact.
        if (FOCUS.matches(GLFW.GLFW_KEY_R, 0) && FOCUS.getKeyModifier() == KeyModifier.NONE) {
            FOCUS.setKeyModifierAndCode(KeyModifier.NONE, FOCUS.getDefaultKey());
            KeyMapping.resetMapping();
            minecraft.options.save();
            // Options.save logs failures instead of throwing. Do not mark a failed write complete.
            Path options = minecraft.gameDirectory.toPath().resolve("options.txt");
            String savedBinding = "key_" + FOCUS.getName() + ":" + FOCUS.saveString();
            try (Stream<String> lines = Files.lines(options, StandardCharsets.UTF_8)) {
                if (lines.noneMatch(savedBinding::equals)) {
                    GuaniaoMod.LOGGER.warn("Camera focus key migration was not saved; retrying next launch");
                    return;
                }
            } catch (IOException | UncheckedIOException exception) {
                GuaniaoMod.LOGGER.warn("Could not verify camera focus key migration; retrying next launch", exception);
                return;
            }
        }
        CameraClientPreferences.markFocusKeyMigrated();
    }
}
