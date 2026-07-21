package EdDYON.guaniao.client.config;

import EdDYON.guaniao.config.BirdConfigData;
import net.minecraft.client.Minecraft;

public final class BirdConfigClient {
    private BirdConfigClient() {
    }

    public static boolean requestOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            return false;
        }
        minecraft.player.connection.sendCommand("guaniaoconfig");
        return true;
    }

    public static void open(BirdConfigData data) {
        Minecraft.getInstance().setScreen(new BirdConfigScreen(data));
    }
}
