package EdDYON.guaniao.util;

import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** Defers client class resolution until running on a physical client. */
public final class ClientActions {
    private ClientActions() {}

    public static void run(Supplier<Runnable> action) {
        if (FMLEnvironment.dist == Dist.CLIENT) action.get().run();
    }
}
