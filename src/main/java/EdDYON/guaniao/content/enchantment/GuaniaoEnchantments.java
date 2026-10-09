package EdDYON.guaniao.content.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class GuaniaoEnchantments {
    public static final ResourceKey<Enchantment> BURIAL_PLUME = key("burial_plume");
    public static final ResourceKey<Enchantment> RIVEN_PLUME = key("riven_plume");
    public static final ResourceKey<Enchantment> HUNTING_RETURN = key("hunting_return");
    private GuaniaoEnchantments() {}
    private static ResourceKey<Enchantment> key(String id) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("guaniao", id));
    }
    public static int level(ItemStack stack, ResourceKey<Enchantment> key) {
        for (var entry : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
            if (entry.getKey().is(key)) return entry.getIntValue();
        }
        return 0;
    }
    public static Holder<Enchantment> holder(HolderLookup.Provider registries, ResourceKey<Enchantment> key) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
