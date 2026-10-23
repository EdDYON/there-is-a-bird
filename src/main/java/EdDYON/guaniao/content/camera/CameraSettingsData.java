package EdDYON.guaniao.content.camera;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class CameraSettingsData {
    private static final String TAG_FILTER = "CameraFilter";
    private static final String TAG_LENS = "CameraLens";
    private static final String TAG_SHOOTING_MODE = "CameraShootingMode";
    private static final String TAG_FOCAL_LENGTH = "CameraFocalLength";
    private static final String TAG_APERTURE = "CameraAperture";
    private static final String TAG_FOCUS_MODE = "CameraFocusMode";
    private static final String TAG_FOCUS_DISTANCE = "CameraFocusDistance";

    private CameraSettingsData() {
    }

    public static CameraFilter filter(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? CameraFilter.NONE : CameraFilter.byId(tag.getInt(TAG_FILTER));
    }

    public static void setFilter(ItemStack stack, CameraFilter filter) {
        stack.getOrCreateTag().putInt(TAG_FILTER, filter.id());
    }

    public static CameraState state(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        CameraState defaults = CameraState.defaults();
        if (tag == null) {
            return defaults;
        }
        return new CameraState(
                CameraFilter.byId(tag.getInt(TAG_FILTER)),
                tag.contains(TAG_LENS) ? CameraLens.byId(tag.getInt(TAG_LENS)) : defaults.lens(),
                tag.contains(TAG_SHOOTING_MODE)
                        ? CameraShootingMode.byId(tag.getInt(TAG_SHOOTING_MODE))
                        : defaults.shootingMode(),
                tag.contains(TAG_FOCAL_LENGTH) ? tag.getDouble(TAG_FOCAL_LENGTH) : defaults.focalLength(),
                tag.contains(TAG_APERTURE) ? CameraAperture.byId(tag.getInt(TAG_APERTURE)) : defaults.aperture(),
                tag.contains(TAG_FOCUS_MODE) ? CameraFocusMode.byId(tag.getInt(TAG_FOCUS_MODE)) : defaults.focusMode(),
                tag.contains(TAG_FOCUS_DISTANCE) ? tag.getDouble(TAG_FOCUS_DISTANCE) : defaults.focusDistance()
        );
    }

    public static void setState(ItemStack stack, CameraState state) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_FILTER, state.filter().id());
        tag.putInt(TAG_LENS, state.lens().id());
        tag.putInt(TAG_SHOOTING_MODE, state.shootingMode().id());
        tag.putDouble(TAG_FOCAL_LENGTH, state.focalLength());
        tag.putInt(TAG_APERTURE, state.aperture().id());
        tag.putInt(TAG_FOCUS_MODE, state.focusMode().id());
        tag.putDouble(TAG_FOCUS_DISTANCE, state.focusDistance());
    }
}
