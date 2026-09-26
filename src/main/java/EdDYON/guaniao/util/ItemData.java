package EdDYON.guaniao.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Preserves the mod's existing NBT keys inside Minecraft's immutable custom-data component. */
public final class ItemData {
    private ItemData() {}
    public static CompoundTag read(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }
    public static CompoundTag copy(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
    public static void write(ItemStack stack, CompoundTag data) {
        if (data.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data.copy()));
    }
    public static boolean hasMetadata(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA) || !stack.getComponentsPatch().isEmpty();
    }
    /** Vanilla cannot data-fix item stacks nested under a mod's custom NBT keys. */
    public static ItemStack load(net.minecraft.core.HolderLookup.Provider registries, CompoundTag tag) {
        return ItemStack.parseOptional(registries, upgradeLegacyStack(tag));
    }

    public static CompoundTag upgradeLegacyStack(CompoundTag tag) {
        if (!tag.contains("Count", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC) || tag.contains("count")) return tag;
        // 3465 is the source Minecraft 1.20.1 data version. Preserve names, enchantments,
        // vanilla item data and all mod fields through the official component data fix.
        var upgraded = net.minecraft.util.datafix.DataFixers.getDataFixer().update(
                net.minecraft.util.datafix.fixes.References.ITEM_STACK,
                new com.mojang.serialization.Dynamic<>(net.minecraft.nbt.NbtOps.INSTANCE, tag.copy()),
                3465, net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
        if (!(upgraded instanceof CompoundTag result)) throw new IllegalStateException("Item data fix did not return a compound");
        if (tag.contains("Slot")) result.putByte("Slot", tag.getByte("Slot"));
        return result;
    }

    public static CompoundTag upgradeLegacyInventory(CompoundTag inventory) {
        CompoundTag result = inventory.copy();
        var items = result.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) items.set(i, upgradeLegacyStack(items.getCompound(i)));
        return result;
    }
}
