package EdDYON.guaniao.content.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;

public final class RivenPlumeEnchantment extends Enchantment {
    public RivenPlumeEnchantment() {
        super(Rarity.VERY_RARE, GuaniaoEnchantments.FEATHER_FAN,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return other != GuaniaoEnchantments.BURIAL_PLUME.get()
                && other != GuaniaoEnchantments.HUNTING_RETURN.get()
                && super.checkCompatibility(other);
    }
}
