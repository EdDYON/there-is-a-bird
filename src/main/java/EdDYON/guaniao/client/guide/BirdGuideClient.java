package EdDYON.guaniao.client.guide;

import net.minecraft.client.Minecraft;

public final class BirdGuideClient {
    private BirdGuideClient() {
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new BirdHandbookScreen());
    }
}
