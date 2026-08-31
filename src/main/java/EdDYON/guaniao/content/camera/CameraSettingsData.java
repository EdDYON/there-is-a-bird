package EdDYON.guaniao.content.camera;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class CameraSettingsData {
    private static final String TAG_FILTER = "CameraFilter";

    private CameraSettingsData() {
    }

    public static CameraFilter filter(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? CameraFilter.NONE : CameraFilter.byId(tag.getInt(TAG_FILTER));
    }

    public static void setFilter(ItemStack stack, CameraFilter filter) {
        stack.getOrCreateTag().putInt(TAG_FILTER, filter.ordinal());
    }
}
