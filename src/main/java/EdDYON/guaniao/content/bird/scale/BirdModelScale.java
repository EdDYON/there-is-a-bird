package EdDYON.guaniao.content.bird.scale;

import EdDYON.guaniao.config.BirdConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public final class BirdModelScale {
    public static final String NBT_KEY = "BirdModelScale";
    public static final float DEFAULT_INDIVIDUAL_SCALE = 1.0F;
    public static final float APRIL_FOOLS_RENDER_MULTIPLIER = 4.1F;

    private BirdModelScale() {
    }

    public static float randomIndividualScale(RandomSource random, BirdModelScaleProfile profile) {
        return lerp(random.nextFloat(), profile.minIndividualScale(), profile.maxIndividualScale());
    }

    public static float inheritIndividualScale(RandomSource random, float firstParentScale, float secondParentScale, BirdModelScaleProfile profile) {
        float average = (sanitize(firstParentScale, profile) + sanitize(secondParentScale, profile)) * 0.5F;
        float smallMutation = (random.nextFloat() - 0.5F) * 0.06F;
        if (random.nextFloat() < 0.12F) {
            return randomIndividualScale(random, profile);
        }
        return sanitize(average + smallMutation, profile);
    }

    public static float renderScale(BirdModelScaleProfile profile, float individualScale) {
        float scale = profile.baseRenderScale() * sanitize(individualScale, profile);
        return BirdConfigManager.aprilFoolsMode() ? scale * APRIL_FOOLS_RENDER_MULTIPLIER : scale;
    }

    public static int fitPreviewScale(int scale) {
        if (!BirdConfigManager.aprilFoolsMode()) {
            return scale;
        }
        return Math.max(1, Math.round(scale / APRIL_FOOLS_RENDER_MULTIPLIER));
    }

    public static float sanitize(float scale, BirdModelScaleProfile profile) {
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            return DEFAULT_INDIVIDUAL_SCALE;
        }
        return clamp(scale, profile.minIndividualScale(), profile.maxIndividualScale());
    }

    public static void save(CompoundTag compoundTag, float individualScale, BirdModelScaleProfile profile) {
        compoundTag.putFloat(NBT_KEY, sanitize(individualScale, profile));
    }

    public static float load(CompoundTag compoundTag, BirdModelScaleProfile profile) {
        if (!compoundTag.contains(NBT_KEY, 5)) {
            return DEFAULT_INDIVIDUAL_SCALE;
        }
        return sanitize(compoundTag.getFloat(NBT_KEY), profile);
    }

    private static float lerp(float amount, float min, float max) {
        return min + amount * (max - min);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
