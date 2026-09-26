package EdDYON.guaniao.registry;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.nest.CrowNestMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Supplier;

public final class GuaniaoMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, GuaniaoMod.MOD_ID);

    public static final Supplier<MenuType<CrowNestMenu>> CROW_NEST = MENU_TYPES.register("crow_nest",
            () -> IMenuTypeExtension.create(CrowNestMenu::new));

    private GuaniaoMenuTypes() {
    }
}
