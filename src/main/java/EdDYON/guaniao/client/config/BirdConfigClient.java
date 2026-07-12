package EdDYON.guaniao.client.config;

import EdDYON.guaniao.config.BirdConfigData;
import net.minecraft.client.Minecraft;

public final class BirdConfigClient {
    private BirdConfigClient() {
    }

    public static void open(BirdConfigData data) {
        Minecraft.getInstance().setScreen(new BirdConfigScreen(data));
    }
}
