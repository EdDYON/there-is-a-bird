package EdDYON.guaniao.registry;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.nest.CrowNestMenu;
import EdDYON.guaniao.content.camera.frame.FrameWorkbenchMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GuaniaoMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, GuaniaoMod.MOD_ID);

    public static final RegistryObject<MenuType<CrowNestMenu>> CROW_NEST = MENU_TYPES.register("crow_nest",
            () -> IForgeMenuType.create(CrowNestMenu::new));

    public static final RegistryObject<MenuType<FrameWorkbenchMenu>> FRAME_WORKBENCH = MENU_TYPES.register("frame_workbench",
            () -> IForgeMenuType.create(FrameWorkbenchMenu::new));

    private GuaniaoMenuTypes() {
    }
}
