package EdDYON.guaniao.client.guide;

import EdDYON.guaniao.client.guide.BirdGuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class BirdGuideClient {
    private BirdGuideClient() {
    }

    public static void open() {
        Minecraft.getInstance().setScreen((Screen)new BirdGuideScreen());
    }
}

