package EdDYON.guaniao.content.enchantment;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.fan.FeatherFanItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GuaniaoEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, GuaniaoMod.MOD_ID);

    public static final EnchantmentCategory FEATHER_FAN = EnchantmentCategory.create(
            "feather_fan",
            item -> item instanceof FeatherFanItem
    );

    public static final RegistryObject<Enchantment> BURIAL_PLUME = ENCHANTMENTS.register(
            "burial_plume",
            BurialPlumeEnchantment::new
    );
    public static final RegistryObject<Enchantment> RIVEN_PLUME = ENCHANTMENTS.register(
            "riven_plume",
            RivenPlumeEnchantment::new
    );
    public static final RegistryObject<Enchantment> HUNTING_RETURN = ENCHANTMENTS.register(
            "hunting_return",
            HuntingReturnEnchantment::new
    );

    private GuaniaoEnchantments() {
    }
}
